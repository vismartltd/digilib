/*
 * DocuDirCache.java
 * 
 * Digital Image Library servlet components
 * 
 * Copyright (C) 2003 Robert Casties (robcast@mail.berlios.de)
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
 * Created on 03.03.2003
 */

package digilib.io;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Cache of digilib directories.
 * 
 * @author casties
 */
public class DocuDirCache {

	/** general logger for this class */
	protected static Logger logger = Logger.getLogger(DocuDirCache.class);

	/** Map of directories */
	Map map = null;

	/** number of files in the whole cache (approximate) */
	long numFiles = 0;

	/** number of cache hits */
	long hits = 0;

	/** number of cache misses */
	long misses = 0;

	/** the root directory element */
	public DigiDirectory rootDir = null;

	/**
	 * Default constructor.
	 */
	public DocuDirCache() {
		map = new HashMap();
		// add root directory
		rootDir = new DigiDirectory(FileOps.getBaseDirs()[0], "", null);
		map.put("", rootDir);
	}

	/**
	 * The number of directories in the cache.
	 * 
	 * @return
	 */
	public int size() {
		return (map != null) ? map.size() : 0;
	}

	/**
	 * Add a DocuDirectory to the cache.
	 * 
	 * @param newdir
	 */
	public void put(DigiDirectory newdir) {
		String s = newdir.getDLPath();
		Object oldkey = map.put(s, newdir);
		if (oldkey != null) {
			logger.warn("Duplicate key in DocuDirCache.put -- overwritten!");
		}
		numFiles += newdir.getSize();
	}

	/**
	 * Add a directory to the cache and check its parents.
	 * 
	 * @param newDir
	 */
	public synchronized void putDir(DigiDirectory newDir) {
		put(newDir);
	}

	/**
	 * Returns a DigiDirectory from the cache.
	 * 
	 * @param path
	 * @return
	 */
	public DigiDirectory get(String path) {
		return (DigiDirectory) map.get(path);
	}

	/**
	 * Returns the DigiDirectory indicated by the pathname <code>fn</code>.
	 * 
	 * If <code>fn</code> is a file then its parent directory is returned.
	 * 
	 * @param fn
	 *            digilib pathname
	 * @return
	 */
	public DigiDirectory getDirectory(String pn) {
		/*
		 * first, assume pn is a directory and look in the cache
		 */
		DigiDirectory dd = (DigiDirectory) map.get(pn);
		if (dd != null) {
			// cache hit
			hits++;
			return dd;
		} else {
			/*
			 * maybe it's a file? try the parent directory
			 */
			String dn = FileOps.dlParent(pn);
			// try it in the cache
			dd = (DigiDirectory) map.get(dn);
			if (dd != null) {
				// cache hit
				hits++;
				return dd;
			} else {
				// cache miss
				misses++;
				/*
				 * try to read from disk
				 */
				File f = FileOps.getRealFile(pn);
				/*
				 * is it a directory?
				 */
				if (f.isDirectory()) {
					dd = new DigiDirectory(f, pn, null);
					// add to the cache
					putDir(dd);
					return dd;
				} else {
					/*
					 * then maybe a file? try the parent as a directory
					 */
					File d = FileOps.getRealFile(dn);
					if (d.isDirectory()) {
						dd = new DigiDirectory(d, dn, null);
						// add to the cache
						putDir(dd);
						return dd;
					}
				}
			}
		}
		/*
		 * otherwise it's crap
		 */
		return null;
	}

	/**
	 * Returns the DigiDirent with the pathname <code>fn</code> and the index
	 * <code>in</code> and the class <code>fc</code>.
	 * 
	 * If <code>fn</code> is a file then the corresponding DocuDirent is
	 * returned and the index is ignored.
	 * 
	 * @param pn
	 *            digilib pathname
	 * @param in
	 *            file index
	 * @param fc
	 *            file class
	 * @return
	 */
	public DigiDirent getFile(String pn, int in, int fc) {
		// file number is 1-based, vector index is 0-based
		int n = in - 1;
		// get the (parent) directory
		DigiDirectory dd = getDirectory(pn);
		if (dd != null) {
			// get the directory's name
			String dn = dd.getDLPath();
			if (dn.equals(pn)) {
				// return the file at the index
				return dd.get(n, fc);
			} else {
				// then the last part must be the filename
				String fn = FileOps.dlName(pn);
				return dd.get(fn);
			}
		}
		return null;
	}

	/**
	 * @return long
	 */
	public long getNumFiles() {
		return numFiles;
	}

	/**
	 * @return long
	 */
	public long getHits() {
		return hits;
	}

	/**
	 * @return long
	 */
	public long getMisses() {
		return misses;
	}

	/**
	 * @return Returns the rootDir.
	 */
	public DigiDirectory getRootDir() {
		return rootDir;
	}
	/**
	 * @param rootDir The rootDir to set.
	 */
	public void setRootDir(DigiDirectory rootDir) {
		this.rootDir = rootDir;
	}
}
