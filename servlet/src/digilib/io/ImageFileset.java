/* ImageFileset -- digilib image file info class.  
 * Digital Image Library servlet components  
 * Copyright (C) 2003 Robert Casties (robcast@mail.berlios.de)  
 * 
 * This program is free software; you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.  
 * 
 * Please read license.txt for the full details. A copy of the GPL may be 
 * found at http://www.gnu.org/copyleft/lgpl.html  
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA  
 */

package digilib.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import digilib.image.ImageOps;
import digilib.image.ImageSize;

/**
 * @author casties
 */
public class ImageFileset extends DigiDirent {

	/** this is an image file */
	protected static int FILE_CLASS = FileOps.CLASS_IMAGE;

	/** list of files (ImageFile) */
	private List files;

	/** resolution of the biggest image (DPI) */
	private float resX = 0;

	/** resolution of the biggest image (DPI) */
	private float resY = 0;

	/**
	 * Constructor with a file and hints.
	 * 
	 * The hints are expected to contain 'basedirs' and 'scaledfilext' keys.
	 * 
	 * @param file
	 * @param hints
	 */
	public ImageFileset(File file, DigiDirectory parent, Map hints) {
		super(file.getName(), parent);
		files = new ArrayList(FileOps.getBaseDirs().length);
		fill(file, hints);
	}

	/**
	 * Gets the default File.
	 * 
	 */
	public ImageFile getFile() {
		return (files != null) ? (ImageFile) files.get(0) : null;
	}

	/**
	 * Get the ImageFile at the index.
	 * 
	 * 
	 * @param index
	 * @return
	 */
	public ImageFile get(int index) {
		return (files != null) ? (ImageFile) files.get(index) : null;
	}

	/**
	 * Get the next smaller ImageFile than the given size.
	 * 
	 * Returns the ImageFile from the set that has a width and height smaller or
	 * equal the given size. Returns null if there isn't any smaller image.
	 * Needs DocuInfo instance to checkFile().
	 * 
	 * 
	 * @param size
	 * @param info
	 * @return
	 */
	public ImageFile getNextSmaller(ImageSize size) {
		for (Iterator i = getHiresIterator(); i.hasNext();) {
			ImageFile f = (ImageFile) i.next();
			try {
				ImageOps.checkFile(f);
				if (f.getSize().isTotallySmallerThan(size)) {
					return f;
				}
			} catch (IOException e) {
			}
		}
		return null;
	}

	/**
	 * Get the next bigger ImageFile than the given size.
	 * 
	 * Returns the ImageFile from the set that has a width or height bigger or
	 * equal the given size. Returns null if there isn't any bigger image. Needs
	 * DocuInfo instance to checkFile().
	 * 
	 * 
	 * @param size
	 * @param info
	 * @return
	 */
	public ImageFile getNextBigger(ImageSize size) {
		for (ListIterator i = getLoresIterator(); i.hasPrevious();) {
			ImageFile f = (ImageFile) i.previous();
			try {
				ImageOps.checkFile(f);
				if (f.getSize().isBiggerThan(size)) {
					return f;
				}
			} catch (IOException e) {
			}
		}
		return null;
	}

	/**
	 * Returns the biggest ImageFile in the set.
	 * 
	 * 
	 * @return
	 */
	public ImageFile getBiggest() {
		return (ImageFile) files.get(0);
	}

	/**
	 * Returns the biggest ImageFile in the set.
	 * 
	 * 
	 * @return
	 */
	public ImageFile getSmallest() {
		return (ImageFile) files.get(files.size() - 1);
	}

	/**
	 * Get an Iterator for this Fileset starting at the highest resolution
	 * images.
	 * 
	 * 
	 * @return
	 */
	public ListIterator getHiresIterator() {
		return files.listIterator();
	}

	/**
	 * Get an Iterator for this Fileset starting at the lowest resolution
	 * images.
	 * 
	 * The Iterator starts at the last element, so you have to use it backwards
	 * with hasPrevious() and previous().
	 * 
	 * 
	 * @return
	 */
	public ListIterator getLoresIterator() {
		return files.listIterator(files.size());
	}

	/**
	 * Fill the ImageFileset with files from different base directories.
	 * 
	 * 
	 * @param dirs
	 *            list of base directories
	 * @param imf
	 *            file (from first base dir)
	 * @param hints
	 * 
	 */
	void fill(File imf, Map hints) {
		File[][] scaledirs = (File[][]) hints.get(FileOps.HINT_BASEDIRS);
		File[] bd = FileOps.getBaseDirs();
		int nb = bd.length;
		if (scaledirs == null) {
			// read all scaled directories
			scaledirs = new File[nb][];
			for (int i = 1; i < nb; i++) {
				// check basedir + digilib path
				File d = FileOps.getRealFile(bd[i], parent.getDLPath());
				scaledirs[i] = d.listFiles();
			}
			hints.put(FileOps.HINT_BASEDIRS, scaledirs);
		}
		// add the first ImageFile to the ImageFileset
		files.add(new ImageFile(imf));
		// iterate the remaining base directories
		for (int dirIdx = 1; dirIdx < nb; dirIdx++) {
			if (scaledirs[dirIdx] == null) {
				continue;
			}
			// find the file in the directory
			File fn = FileOps.findFile(imf, scaledirs[dirIdx]);
			if (fn == null) {
				continue;
			}
			if (FileOps.classForFile(fn) == FileOps.CLASS_IMAGE) {
				// add to the fileset
				files.add(new ImageFile(fn));
			}
		}
	}

	/**
	 * Reads metadata and sets resolution in resX and resY.
	 * 
	 */
	public void readMeta() {
		if (isMetaRead) {
			return;
		}
		// read the metadata file
		super.readMeta();
		if (meta == null) {
			// try directory metadata
			meta = parent.getMeta();
			if (meta == null) {
				// no metadata available
				isMetaRead = true;
				return;
			}
		}
		isMetaRead = true;
		String s;
		float dpi = 0;
		float dpix = 0;
		float dpiy = 0;
		float sizex = 0;
		float sizey = 0;
		float pixx = 0;
		float pixy = 0;
		// DPI is valid for X and Y
		if (meta.containsKey("original-dpi")) {
			try {
				dpi = Float.parseFloat((String) meta.get("original-dpi"));
			} catch (NumberFormatException e) {
			}
			if (dpi != 0) {
				resX = dpi;
				resY = dpi;
				return;
			}
		}
		// DPI-X and DPI-Y
		if (meta.containsKey("original-dpi-x")
				&& meta.containsKey("original-dpi-y")) {
			try {
				dpix = Float.parseFloat((String) meta.get("original-dpi-x"));
				dpiy = Float.parseFloat((String) meta.get("original-dpi-y"));
			} catch (NumberFormatException e) {
			}
			if ((dpix != 0) && (dpiy != 0)) {
				resX = dpix;
				resY = dpiy;
				return;
			}
		}
		// SIZE-X and SIZE-Y and PIXEL-X and PIXEL-Y
		if (meta.containsKey("original-size-x")
				&& meta.containsKey("original-size-y")
				&& meta.containsKey("original-pixel-x")
				&& meta.containsKey("original-pixel-y")) {
			try {
				sizex = Float.parseFloat((String) meta.get("original-size-x"));
				sizey = Float.parseFloat((String) meta.get("original-size-y"));
				pixx = Float.parseFloat((String) meta.get("original-pixel-x"));
				pixy = Float.parseFloat((String) meta.get("original-pixel-y"));
			} catch (NumberFormatException e) {
			}
			if ((sizex != 0) && (sizey != 0) && (pixx != 0) && (pixy != 0)) {
				resX = pixx / (sizex * 100 / 2.54f);
				resY = pixy / (sizey * 100 / 2.54f);
				return;
			}
		}
	}

	/**
	 * Returns the aspect ratio of the images.
	 * 
	 * @return
	 */
	public float getAspect() {
		for (Iterator i = files.iterator(); i.hasNext();) {
			ImageFile f = (ImageFile) i.next();
			ImageSize s = f.getSize();
			if (s != null) {
				return s.getAspect();
			}
		}
		return 0f;
	}

	/**
	 * @return
	 */
	public float getResX() {
		return resX;
	}

	/**
	 * @return
	 */
	public float getResY() {
		return resY;
	}

}