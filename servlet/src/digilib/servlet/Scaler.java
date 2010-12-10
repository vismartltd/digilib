package digilib.servlet;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import digilib.auth.AuthOpException;
import digilib.auth.AuthOps;
import digilib.image.DocuImage;
import digilib.image.ImageOpException;
import digilib.io.DocuDirCache;
import digilib.io.DocuDirectory;
import digilib.io.DocuDirent;
import digilib.io.FileOps;
import digilib.io.ImageFile;

// TODO digilibError is not used anymore and may need to get reintegrated

@SuppressWarnings("serial")
public class Scaler extends RequestHandler {

    /** digilib servlet version (for all components) */
    public static final String dlVersion = "1.9.0a";

    /** general error code */
    public static final int ERROR_UNKNOWN = 0;

    /** error code for authentication error */
    public static final int ERROR_AUTH = 1;

    /** error code for file operation error */
    public static final int ERROR_FILE = 2;

    /** error code for image operation error */
    public static final int ERROR_IMAGE = 3;

    /** DocuDirCache instance */
    DocuDirCache dirCache;

    /** Image executor */
    DigilibJobCenter<DocuImage> imageJobCenter;

    /** authentication error image file */
    File denyImgFile;

    /** image error image file */
    File errorImgFile;

    /** not found error image file */
    File notfoundImgFile;

    /** send files as is? */
    boolean sendFileAllowed = true;

    /** DigilibConfiguration instance */
    DigilibConfiguration dlConfig;

    /** use authorization database */
    boolean useAuthorization = true;

    /** AuthOps instance */
    AuthOps authOp;

    /**
     * Initialisation on first run.
     * 
     * @throws ServletException
     * 
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        System.out
                .println("***** Digital Image Library Image Scaler Servlet (version "
                        + dlVersion + ") *****");
        // say hello in the log file
        logger.info("***** Digital Image Library Image Scaler Servlet (version "
                + dlVersion + ") *****");

        // get our ServletContext
        ServletContext context = config.getServletContext();
        // see if there is a Configuration instance
        dlConfig = (DigilibConfiguration) context
                .getAttribute("digilib.servlet.configuration");
        if (dlConfig == null) {
            // no Configuration
            throw new ServletException("No Configuration!");
        }
        // set our AuthOps
        useAuthorization = dlConfig.getAsBoolean("use-authorization");
        authOp = (AuthOps) dlConfig.getValue("servlet.auth.op");

        // DocuDirCache instance
        dirCache = (DocuDirCache) dlConfig.getValue("servlet.dir.cache");

        // Executor
        imageJobCenter = (DigilibJobCenter<DocuImage>) dlConfig
                .getValue("servlet.worker.imageexecutor");

        denyImgFile = ServletOps.getFile(
                (File) dlConfig.getValue("denied-image"), config);
        errorImgFile = ServletOps.getFile(
                (File) dlConfig.getValue("error-image"), config);
        notfoundImgFile = ServletOps.getFile(
                (File) dlConfig.getValue("notfound-image"), config);
        sendFileAllowed = dlConfig.getAsBoolean("sendfile-allowed");
    }

    /** Returns modification time relevant to the request.
     * 
     * @see javax.servlet.http.HttpServlet#getLastModified(javax.servlet.http.HttpServletRequest)
     */
    protected long getLastModified(HttpServletRequest request) {
        accountlog.debug("GetLastModified from " + request.getRemoteAddr()
                + " for " + request.getQueryString());
        long mtime = -1;
        // create new request
        DigilibRequest dlReq = new DigilibRequest(request);
		// find the file(set)
		DocuDirent f = dirCache.getFile(dlReq.getFilePath(),
		        dlReq.getAsInt("pn"), FileOps.CLASS_IMAGE);
        // find the requested file
        if (f != null) {
            DocuDirectory dd = (DocuDirectory) f.getParent();
            mtime = dd.getDirMTime() / 1000 * 1000;
        }
        return mtime;
    }


    public void processRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException,
            ImageOpException {

        if (dlConfig == null) {
            throw new ServletException("ERROR: No Configuration!");
        }

        accountlog.debug("request: " + request.getQueryString());
        logger.debug("request: " + request.getQueryString());
        long startTime = System.currentTimeMillis();

        // parse request
        DigilibRequest dlRequest = new DigilibRequest(request);
        // extract the job information
        ImageJobDescription jobTicket = new ImageJobDescription(dlRequest, dlConfig);

        ImageWorker job = null;
        try {
        	/*
        	 *  check if we can fast-track without scaling
        	 */
            ImageFile fileToLoad = jobTicket.getFileToLoad();

            // check permissions
            if (useAuthorization) {
                // get a list of required roles (empty if no restrictions)
                List<String> rolesRequired = authOp.rolesForPath(
                        jobTicket.getFilePath(), request);
                if (rolesRequired != null) {
                    authlog.debug("Role required: " + rolesRequired);
                    authlog.debug("User: " + request.getRemoteUser());
                    // is the current request/user authorized?
                    if (!authOp.isRoleAuthorized(rolesRequired, request)) {
                        // send deny answer and abort
                        throw new AuthOpException();
                    }
                }
            }

            // if requested, send image as a file
            if (sendFileAllowed && jobTicket.getSendAsFile()) {
                String mt = null;
                if (jobTicket.hasOption("rawfile")) {
                    mt = "application/octet-stream";
                }
                logger.debug("Sending RAW File as is.");
                ServletOps.sendFile(fileToLoad.getFile(), mt, response);
                logger.info("Done in " + (System.currentTimeMillis() - startTime) + "ms");
                return;
            }

            // if possible, send the image without actually having to transform it
            if (! jobTicket.isTransformRequired()) {
                logger.debug("Sending File as is.");
                ServletOps.sendFile(fileToLoad.getFile(), null, response);
                logger.info("Done in " + (System.currentTimeMillis() - startTime) + "ms");
                return;
            }

            // check load of workers
            if (imageJobCenter.isBusy()) {
                logger.error("Servlet overloaded!");
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
            // create job
            job = new ImageWorker(dlConfig, jobTicket);
            // submit job
            Future<DocuImage> jobResult = imageJobCenter.submit(job);
            // wait for result
            DocuImage img = jobResult.get();
            // send image
            ServletOps.sendImage(img, null, response);
            logger.debug("Job Processing Time: "
                    + (System.currentTimeMillis() - startTime) + "ms");

        } catch (IOException e) {
            logger.error(e.getClass() + ": " + e.getMessage());
            // response.sendError(1);
        } catch (AuthOpException e) {
            logger.error(e.getClass() + ": " + e.getMessage());
            // response.sendError(1);
        } catch (InterruptedException e) {
            logger.error(e.getClass() + ": " + e.getMessage());
        } catch (ExecutionException e) {
            logger.error(e.getClass() + ": " + e.getMessage());
            logger.error("caused by: " + e.getCause().getMessage());
        }

    }

    /**
     * Sends an error to the client as text or image.
     * 
     * @param asHTML
     * @param type
     * @param msg
     * @param response
     */
    public void digilibError(boolean asHTML, int type, String msg,
            HttpServletResponse response) {
        try {
            File img = null;
            if (type == ERROR_AUTH) {
                if (msg == null) {
                    msg = "ERROR: Unauthorized access!";
                }
                img = denyImgFile;
            } else if (type == ERROR_FILE) {
                if (msg == null) {
                    msg = "ERROR: Image file not found!";
                }
                img = notfoundImgFile;
            } else {
                if (msg == null) {
                    msg = "ERROR: Other image error!";
                }
                img = this.errorImgFile;
            }
            if (asHTML && (img != null)) {
                ServletOps.htmlMessage(msg, response);
            } else {
                ServletOps.sendFile(img, null, response);
            }
        } catch (IOException e) {
            logger.error("Error sending error!", e);
        }

    }

    public static String getVersion() {
        return dlVersion;
    }

}
