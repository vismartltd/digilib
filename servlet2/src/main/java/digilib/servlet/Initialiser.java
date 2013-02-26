package digilib.servlet;

/*
 * #%L
 * 
 * Initialiser.java -- initalisation servlet for setup tasks
 * 
 * Digital Image Library servlet components
 * 
 * %%
 * Copyright (C) 2004 - 2013 MPIWG Berlin
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 * Author: Robert Casties (robcast@berlios.de)
 * 
 * Created on 18.10.2004
 */

import java.io.File;
import java.io.OutputStream;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import digilib.auth.AuthOps;
import digilib.auth.XMLAuthOps;
import digilib.image.DocuImage;
import digilib.io.AliasingDocuDirCache;
import digilib.io.DocuDirCache;
import digilib.io.FileOps.FileClass;
import digilib.util.DigilibJobCenter;

/**
 * Singleton initalisation listener for setup tasks and resources.
 * 
 * @author casties
 *  
 */
public class Initialiser implements ServletContextListener {


	/** servlet version */
	public static final String version = "0.3";

	/** general logger for this class */
	private static Logger logger = Logger.getLogger("digilib.init");

	/** DocuDirCache instance */
	DocuDirCache dirCache;

	/** DigilibConfiguration instance */
	DigilibServletConfiguration dlConfig;

	/** Executor for digilib image jobs (AsyncServletWorker doesn't return anything) */
	DigilibJobCenter<DocuImage> imageEx;
	
	/** Executor for PDF jobs */
	DigilibJobCenter<OutputStream> pdfEx;
	
	/** Executor for PDF image jobs */
	DigilibJobCenter<DocuImage> pdfImageEx;
	
	/**
	 * Initialisation on first run.
	 */
    public void contextInitialized(ServletContextEvent cte) {
        ServletContext context = cte.getServletContext();
        context.log("***** Digital Image Library Initialiser (version "
                + version + ") *****");
		System.out.println("***** Digital Image Library Initialiser (version "
						+ version + ") *****");

		// see if there is a Configuration instance
		dlConfig = (DigilibServletConfiguration) context.getAttribute("digilib.servlet.configuration");
		if (dlConfig == null) {
			// create new Configuration
			try {
				dlConfig = new DigilibServletConfiguration(context);

				/*
				 * further initialization
				 */

				// set up the logger
				File logConf = ServletOps.getConfigFile((File) dlConfig
						.getValue("log-config-file"), context);
				DOMConfigurator.configure(logConf.getAbsolutePath());
				dlConfig.setValue("log-config-file", logConf);
				// say hello in the log file
				logger
						.info("***** Digital Image Library Initialiser (version "
								+ version + ") *****");
				// directory cache
				String[] bd = (String[]) dlConfig.getValue("basedir-list");
				FileClass[] fcs = { FileClass.IMAGE, FileClass.TEXT };
				if (dlConfig.getAsBoolean("use-mapping")) {
					// with mapping file
					File mapConf = ServletOps.getConfigFile((File) dlConfig
							.getValue("mapping-file"), context);
					dirCache = new AliasingDocuDirCache(bd, fcs, mapConf,
							dlConfig);
					dlConfig.setValue("mapping-file", mapConf);
				} else {
					// without mapping
					dirCache = new DocuDirCache(bd, fcs, dlConfig);
				}
				dlConfig.setValue("servlet.dir.cache", dirCache);
				// useAuthentication
				if (dlConfig.getAsBoolean("use-authorization")) {
					// DB version
					//authOp = new DBAuthOpsImpl(util);
					// XML version
					File authConf = ServletOps.getConfigFile((File) dlConfig
							.getValue("auth-file"), context);
					AuthOps authOp = new XMLAuthOps(authConf);
					dlConfig.setValue("servlet.auth.op", authOp);
					dlConfig.setValue("auth-file", authConf);
				}
				// DocuImage class
				DocuImage di = DigilibServletConfiguration.getDocuImageInstance();
				dlConfig.setValue("servlet.docuimage.class", di.getClass().getName());
				// disk cache for image toolkit
				boolean dc = dlConfig.getAsBoolean("img-diskcache-allowed");
				// TODO: methods for all toolkits?
				ImageIO.setUseCache(dc);
				// digilib worker threads
				int nt = dlConfig.getAsInt("worker-threads");
                int mt = dlConfig.getAsInt("max-waiting-threads");
				imageEx = new DigilibJobCenter<DocuImage>(nt, mt, false, "servlet.worker.imageexecutor");
                dlConfig.setValue("servlet.worker.imageexecutor", imageEx);				
				// PDF worker threads
				int pnt = dlConfig.getAsInt("pdf-worker-threads");
                int pmt = dlConfig.getAsInt("pdf-max-waiting-threads");
				pdfEx = new DigilibJobCenter<OutputStream>(pnt, pmt, false, "servlet.worker.pdfexecutor");
                dlConfig.setValue("servlet.worker.pdfexecutor", pdfEx);				
				// PDF image worker threads
				int pint = dlConfig.getAsInt("pdf-image-worker-threads");
                int pimt = dlConfig.getAsInt("pdf-image-max-waiting-threads");
				pdfImageEx = new DigilibJobCenter<DocuImage>(pint, pimt, false, "servlet.worker.pdfimageexecutor");
                dlConfig.setValue("servlet.worker.pdfimageexecutor", pdfImageEx);				
				// set as the servlets main config
				context.setAttribute("digilib.servlet.configuration", dlConfig);

			} catch (Exception e) {
				logger.error("Error in initialisation: ", e);
			}
		} else {
			// say hello in the log file
			logger.info("***** Digital Image Library Initialiser (version "
							+ version + ") *****");
			logger.warn("Already initialised!");
		}
	}

    /** clean up local resources
     * 
     */
    public void contextDestroyed(ServletContextEvent arg0) {
        logger.info("Initialiser shutting down.");
        if (dirCache != null) {
            // shut down dirCache?
            dirCache = null;
        }
        if (imageEx != null) {
            // shut down image thread pool
            List<Runnable> rj = imageEx.shutdownNow();
            int nrj = rj.size();
            if (nrj > 0) {
                logger.error("Still running threads when shutting down image job queue: "+nrj);
            }
        }
        if (pdfEx != null) {
            // shut down pdf thread pool
            List<Runnable> rj = pdfEx.shutdownNow();
            int nrj = rj.size();
            if (nrj > 0) {
                logger.error("Still running threads when shutting down PDF job queue: "+nrj);
            }
        }
        if (pdfImageEx != null) {
            // shut down pdf image thread pool
            List<Runnable> rj = pdfImageEx.shutdownNow();
            int nrj = rj.size();
            if (nrj > 0) {
                logger.error("Still running threads when shutting down PDF-image job queue: "+nrj);
            }
        }
    }

}
