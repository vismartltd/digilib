/* ImageFile.java -- digilib image file class.

  Digital Image Library servlet components

  Copyright (C) 2003 Robert Casties (robcast@mail.berlios.de)

  This program is free software; you can redistribute  it and/or modify it
  under  the terms of  the GNU General  Public License as published by the
  Free Software Foundation;  either version 2 of the  License, or (at your
  option) any later version.
   
  Please read license.txt for the full details. A copy of the GPL
  may be found at http://www.gnu.org/copyleft/lgpl.html

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 * Created on 25.02.2003
 */
 
package digilib.io;

import java.io.File;
import java.io.IOException;

import digilib.image.ImageOps;
import digilib.image.ImageSize;

/**
 * @author casties
 */
public class ImageFile extends File {
	
	private static final long serialVersionUID = 1L;
	
	/** file mime-type */
	private String mimetype = null;
	
	/** image size in pixels */
	private ImageSize pixelSize = null;

	/**
	 * @param pathname
	 */
	public ImageFile(String pathname) {
		super(pathname);
	}

	/**
	 * @param file
	 */
	public ImageFile(File file) {
		super(file.getPath());
	}

	/**
	 * @return ImageSize
	 */
	public ImageSize getSize() {
		return pixelSize;
	}

	/**
	 * Sets the imageSize.
	 * @param imageSize The imageSize to set
	 */
	public void setSize(ImageSize imageSize) {
		this.pixelSize = imageSize;
	}

	/**
	 * @return String
	 */
	public String getMimetype() {
		return mimetype;
	}

	/**
	 * Sets the mimetype.
	 * @param mimetype The mimetype to set
	 */
	public void setMimetype(String filetype) {
		this.mimetype = filetype;
	}

	/**
	 * Checks image size.
	 * @throws IOException
	 *  
	 */
	public void check() throws IOException {
		if (pixelSize != null) {
			return;
		}
		ImageOps.checkFile(this);
	}
	
}
