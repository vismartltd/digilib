/* JAIImageLoaderDocuImage -- Image class implementation using JAI's ImageLoader Plugin

  Digital Image Library servlet components

  Copyright (C) 2002, 2003 Robert Casties (robcast@mail.berlios.de)

  This program is free software; you can redistribute  it and/or modify it
  under  the terms of  the GNU General  Public License as published by the
  Free Software Foundation;  either version 2 of the  License, or (at your
  option) any later version.
   
  Please read license.txt for the full details. A copy of the GPL
  may be found at http://www.gnu.org/copyleft/lgpl.html

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

*/

package digilib.image;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.servlet.ServletException;

import digilib.io.FileOpException;
import digilib.io.ImageFile;

/** DocuImage implementation using the Java Advanced Imaging API and the ImageLoader
 * API of Java 1.4.
 */
public class JAIImageLoaderDocuImage extends JAIDocuImage {

	/** ImageIO image reader */
	protected ImageReader reader;
	/** current image file */
	protected File imgFile;

	/* loadSubimage is supported. */
	public boolean isSubimageSupported() {
		return true;
	}

    /* returns the size of the current image */
    public ImageSize getSize() {
        ImageSize is = null;
        // TODO: can we cache imageSize?
        int h = 0;
        int w = 0;
        try {
            if (img == null) {
                // get size from ImageReader
                h = reader.getHeight(0);
                w = reader.getWidth(0);
            } else {
                // get size from image
                h = img.getHeight();
                w = img.getWidth();
            }
            is = new ImageSize(w, h);
        } catch (IOException e) {
            logger.debug("error in getSize:", e);
        }
        return is;
    }


	/* Load an image file into the Object. */
	public void loadImage(ImageFile f) throws FileOpException {
		logger.debug("loadImage: "+f.getFile());
		//System.gc();
		img = JAI.create("ImageRead", f.getFile().getAbsolutePath());
		if (img == null) {
			throw new FileOpException("Unable to load File!");
		}
        mimeType = f.getMimetype();
	}

	/* Get an ImageReader for the image file. */
	public ImageReader getReader(ImageFile f) throws IOException {
		logger.debug("preloadImage: "+f.getFile());
		//System.gc();
		RandomAccessFile rf = new RandomAccessFile(f.getFile(), "r");
		ImageInputStream istream = new FileImageInputStream(rf);
		//Iterator readers = ImageIO.getImageReaders(istream);
		Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(f.getMimetype());
		if (! readers.hasNext()) {
			throw new FileOpException("Unable to load File!");
		}
		reader = readers.next();
		logger.debug("JAIImageIO: this reader: " + reader.getClass());
		while (readers.hasNext()) {
			logger.debug("  next reader: " + readers.next().getClass());
		}
		reader.setInput(istream);
		return reader;
	}

	/* Load an image file into the Object. */
	public void loadSubimage(ImageFile f, Rectangle region, int prescale)
		throws FileOpException {
		logger.debug("loadSubimage: "+f.getFile());
		//System.gc();
		try {
			if ((reader == null) || (imgFile != f.getFile())) {
				getReader(f);
			}
			ImageReadParam readParam = reader.getDefaultReadParam();
			readParam.setSourceRegion(region);
			readParam.setSourceSubsampling(prescale, prescale, 0, 0);
			img = reader.read(0, readParam);
			/* JAI imageread seems to ignore the readParam :-(
			ImageInputStream istream = (ImageInputStream) reader.getInput();
			ParameterBlockJAI pb = new ParameterBlockJAI("imageread");
			pb.setParameter("Input", istream);
			pb.setParameter("ReadParam", readParam);
			pb.setParameter("Reader", reader);
			img = JAI.create("imageread", pb);
			*/
		} catch (IOException e) {
			throw new FileOpException("Unable to load File!");
		}
		if (img == null) {
			throw new FileOpException("Unable to load File!");
		}
		imgFile = f.getFile();
        mimeType = f.getMimetype();
	}


	/* Write the current image to an OutputStream. */
	public void writeImage(String mt, OutputStream ostream)
		throws ImageOpException, ServletException {
		logger.debug("writeImage");
		try {
			// setup output
			ParameterBlock pb3 = new ParameterBlock();
			pb3.addSource(img);
			pb3.add(ostream);
			if (mt == "image/jpeg") {
				pb3.add("JPEG");
			} else if (mt == "image/png") {
				pb3.add("PNG");
			} else {
				// unknown mime type
				throw new ImageOpException("Unknown mime type: " + mt);
			}
			// render output
			JAI.create("ImageWrite", pb3);
		} catch (RuntimeException e) {
			throw new ServletException("Error writing image.");
		}
	}

	@Override
    public Image getAwtImage() {
        // TODO Auto-generated method stub
        return (Image) img;
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		dispose();
		super.finalize();
	}

	public void dispose() {
		// we must dispose the ImageReader because it keeps the filehandle open!
		reader.dispose();
		reader = null;
		img = null;
	}

}
