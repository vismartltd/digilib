/* ImageOps -- convenience methods for images

 Digital Image Library servlet components

 Copyright (C) 2004 Robert Casties (robcast@mail.berlios.de)

 This program is free software; you can redistribute  it and/or modify it
 under  the terms of  the GNU General  Public License as published by the
 Free Software Foundation;  either version 2 of the  License, or (at your
 option) any later version.
 
 Please read license.txt for the full details. A copy of the GPL
 may be found at http://www.gnu.org/copyleft/lgpl.html

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA

 * Created on 13.10.2004
 */
package digilib.image;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.log4j.Logger;
import org.marcoschmidt.image.ImageInfo;

import digilib.io.FileOpException;
import digilib.io.FileOps;
import digilib.io.ImageFile;

/**
 * convenience methods for images
 * 
 * @author casties
 */
public class ImageOps {

	private static Logger logger = Logger.getLogger(ImageOps.class);

	/** Check and store image size and type of image in ImageFile */
	public static boolean checkFile(ImageFile imgf) throws IOException {
		// fileset to store the information
		RandomAccessFile raf = new RandomAccessFile(imgf, "r");
		// set up ImageInfo object
		ImageInfo iif = new ImageInfo();
		iif.setInput(raf);
		iif.setCollectComments(false);
		iif.setDetermineImageNumber(false);
		logger.debug("identifying (ImageInfo) " + imgf);
		// try with ImageInfo first
		if (iif.check()) {
			ImageSize d = new ImageSize(iif.getWidth(), iif.getHeight());
			imgf.setSize(d);
			imgf.setMimetype(iif.getMimeType());
			//logger.debug("  format:"+iif.getFormatName());
			raf.close();
			iif = null;
		} else {
			iif = null;
			logger.debug("identifying (ImageIO) " + imgf);
			/*
			 * else use ImageReader
			 */
			ImageInputStream istream = ImageIO.createImageInputStream(raf);
			Iterator readers = ImageIO.getImageReaders(istream);
			if ((readers == null) || (!readers.hasNext())) {
				throw new FileOpException("ERROR: unknown image file format!");
			}
			ImageReader reader = (ImageReader) readers.next();
			/* are there more readers? */
			logger.debug("ImageIO: this reader: " + reader.getClass());
			while (readers.hasNext()) {
				logger.debug("ImageIO: next reader: "
						+ readers.next().getClass());
			}
			reader.setInput(istream);
			ImageSize d = new ImageSize(reader.getWidth(0), reader.getHeight(0));
			imgf.setSize(d);
			//String t = reader.getFormatName();
			String t = FileOps.mimeForFile(imgf);
			imgf.setMimetype(t);
			//logger.debug("  format:"+t);
			// dispose the reader to free resources
			reader.dispose();
			raf.close();
			reader = null;
		}
		logger.debug("image size: " + imgf.getSize());
		return true;
	}

}
