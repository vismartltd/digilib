/* DigiDirectory.java -- digilib directory object
 * 
 * Digital Image Library servlet components
 * 
 * Copyright (C) 2004 Robert Casties (robcast@mail.berlios.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * Please read license.txt for the full details. A copy of the GPL may be found
 * at http://www.gnu.org/copyleft/lgpl.html
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *  
 * Created on 03.11.2004
 */
package digilib.io;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.xml.sax.SAXException;

/**
 * digilib directory object
 * 
 * @author casties
 *  
 */
public class DigiDirectory extends DigiDirent {

	protected static boolean isFile = false;

	protected static boolean isDirectory = true;

	protected DigiDirent[] entries;

	protected int[][] indexes;

	protected File dir;

	/** directory name (digilib canonical form) */
	protected String dlPath = null;

	protected long mtime = 0;

	protected boolean isDirRead = false;

	protected Map unresolvedMeta;

	/**
	 * @param parent
	 * @param dir
	 */
	public DigiDirectory(String dlPath) {
		super(FileOps.dlName(dlPath), null);
	}

	/**
	 * @param dir
	 * @param parent
	 */
	public DigiDirectory(File dir, DigiDirectory parent) {
		super(dir.getName(), parent);
		this.dir = dir;
		this.dlPath = parent.getDLPath() + "/" + dir.getName();
	}

	/**
	 * @param dir
	 * @param dlpath
	 * @param parent
	 */
	public DigiDirectory(File dir, String dlpath, DigiDirectory parent) {
		super(dir.getName(), parent);
		this.dlPath = dlpath;
		this.dir = dir;
	}

	public boolean exists() {
		return ((dir != null) && (dir.isDirectory()));
	}

	/**
	 * Returns the file of the class at the index.
	 * 
	 * @param index
	 * @param fc
	 *            fileClass
	 * @return
	 */
	public DigiDirent get(int index, int fc) {
		if (!isDirRead) {
			// read directory now
			if (readDir() < 1) {
				return null;
			}
		}
		try {
			return (DigiDirent) entries[indexes[fc][index]];
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Returns the file with the name <code>fn</code>.
	 * 
	 * @param fn
	 *            filename
	 * @return
	 */
	public DigiDirent get(String fn) {
		if (!isDirRead) {
			// read directory now
			if (readDir() < 1) {
				return null;
			}
		}
		// search for exact match
		int idx = Arrays.binarySearch(entries, fn);
		if (idx >= 0) {
			return entries[idx];
		} else {
			// try closest matches without extension
			idx = -idx - 1;
			int imax = entries.length;
			String fb = FileOps.basename(fn);
			if ((idx < imax)
					&& (FileOps.basename(entries[idx].getName()).equals(fb))) {
				// idx matches
				return entries[idx];
			} else if ((idx > 0)
					&& (FileOps.basename(entries[idx - 1].getName()).equals(fb))) {
				// idx-1 matches
				return entries[idx - 1];
			} else if ((idx + 1 < imax)
					&& (FileOps.basename(entries[idx + 1].getName()).equals(fb))) {
				// idx+1 matches
				return entries[idx + 1];
			}
		}
		return null;
	}

	
	/**
	 * Reads the names of the files in the directory. Fills the filenames array.
	 * Returns the number of files.
	 * 
	 * @return
	 */
	public int readDir() {
		if (!exists()) {
			return -1;
		}
		File[] allFiles = null;
		// list all files in the directory
		allFiles = dir.listFiles();
		if (allFiles == null) {
			// not a directory
			isDirRead = true;
			return -1;
		}
		Arrays.sort(allFiles);
		int nfiles = allFiles.length;
		entries = new DigiDirent[nfiles];
		// create new index lists for all file classes
		int nfc = FileOps.NUM_CLASSES;
		indexes = new int[nfc][nfiles];
		// index pointers for the file classes
		int[] lastidx = new int[nfc];
		Map hints = FileOps.newHints();
		// go through all files
		for (int dirIdx = 0; dirIdx < nfiles; dirIdx++) {
			File f = allFiles[dirIdx];
			String fn = f.getName();
			int fc = FileOps.classForFilename(fn);
			// create the right kind of Dirent
			DigiDirent df = FileOps.fileForClass(fc, f, this, hints);
			// add the file to our list
			entries[dirIdx] = df;
			// add to the indexes
			if (fc >= 0) {
				indexes[fc][lastidx[fc]++] = dirIdx;
			}
		}
		// copy out the index arrays
		for (int i = 0; i < nfc; i++) {
			int[] idxs = new int[lastidx[i]];
			System.arraycopy(indexes[i], 0, idxs, 0, lastidx[i]);
			indexes[i] = idxs;
		}
		// update modification time
		mtime = dir.lastModified();
		// read metadata as well
		readMeta();
		isDirRead = true;
		return nfiles;
	}

	/**
	 * Check to see if the directory has been modified and reread if necessary.
	 * 
	 * @return boolean the directory is valid
	 */
	public void check() {
		if (isDirRead && (dir.lastModified() > mtime)) {
			// on-disk modification time is more recent
			readDir();
		} else if (!isDirRead) {
			readDir();
		}
	}

	/**
	 * Read directory metadata.
	 *  
	 */
	public void readMeta() {
		// check for directory metadata...
		File mf = new File(dir, "index.meta");
		if (mf.canRead()) {
			XMLMetaLoader ml = new XMLMetaLoader();
			try {
				// read directory meta file
				Map fileMeta = ml.loadURL(mf.getAbsolutePath());
				if (fileMeta == null) {
					throw new IOException("XMLMetaloader returned no data!");
				}
				// meta for the directory itself is in the "" bin
				meta = (Map) fileMeta.remove("");
				// read meta for files in this directory
				storeFileMeta(fileMeta, null);
				// is there meta for other files left?
				if (fileMeta.size() > 0) {
					unresolvedMeta = fileMeta;
				}
			} catch (SAXException e) {
				logger.warn("error parsing index.meta", e);
			} catch (IOException e) {
				logger.warn("error reading index.meta", e);
			}
		}
		readParentMeta();
		isMetaRead = true;
	}

	/**
	 * Read metadata from all known parent directories.
	 *  
	 */
	public void readParentMeta() {
		// check the parent directories for additional file meta
		DigiDirectory dd = getParent();
		String path = dir.getName();
		while (dd != null) {
			if (dd.hasUnresolvedMeta()) {
				storeFileMeta(dd.getUnresolvedMeta(), path);
			}
			// prepend parent dir path
			path = dd.getDir().getName() + "/" + path;
			// become next parent
			dd = dd.getParent();
		}
	}

	/**
	 * Stores metadata in the files in this directory.
	 * 
	 * Takes a Map with meta-information, adding the relative path before the
	 * lookup.
	 * 
	 * @param fileMeta
	 * @param relPath
	 * @param fc
	 *            fileClass
	 */
	protected void storeFileMeta(Map fileMeta, String relPath) {
		if (entries == null) {
			// there are no files
			return;
		}
		String path = (relPath != null) ? (relPath + "/") : "";
		// iterate through the list of files in this directory
		for (int i = 0; i < entries.length; i++) {
			DigiDirent f = entries[i];
			// prepend path to the filename
			String fn = path + f.getName();
			// look up meta for this file
			if (relPath == null) {
				// remove from map the same directory
				f.addMeta((Map) fileMeta.remove(fn));
			} else {
				// copy from map in other directories
				f.addMeta((Map) fileMeta.get(fn));
			}
		}
	}

	/*
	 * boring getters and setters
	 */

	public boolean hasUnresolvedMeta() {
		return ((unresolvedMeta != null) && unresolvedMeta.isEmpty());
	}

	/**
	 * @return Returns the unresolvedMeta.
	 */
	public Map getUnresolvedMeta() {
		return unresolvedMeta;
	}

	/**
	 * @return Returns the dir.
	 */
	public File getDir() {
		return dir;
	}

	/**
	 * @param dir
	 *            The dir to set.
	 */
	public void setDir(File dir) {
		this.dir = dir;
	}

	/**
	 * @return Returns the dlPath.
	 */
	public String getDLPath() {
		return dlPath;
	}

	/**
	 * @param dlPath
	 *            The dlPath to set.
	 */
	public void setDlPath(String dlPath) {
		this.dlPath = dlPath;
	}

	/**
	 * The number of files in this directory.
	 * 
	 * @return
	 */
	public int getSize() {
		return (entries != null) ? entries.length : 0;
	}

	/**
	 * The number of files of a file class in this directory.
	 * 
	 * @return
	 */
	public int getSize(int fc) {
		try {
			return indexes[fc].length;
		} catch (Exception e) {
		}
		return 0;
	}

	/**
	 * @return Returns the mtime.
	 */
	public long getMtime() {
		return mtime;
	}
}
