/*
 * FileOps -- Utility class for file operations
 * 
 * Digital Image Library servlet components
 * 
 * Copyright (C) 2001, 2002 Robert Casties (robcast@mail.berlios.de)
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
 */

package digilib.io;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class FileOps {

	private static Logger logger = Logger.getLogger(FileOps.class);
	
	/**
	 * Array of file extensions and corresponding mime-types.
	 */
	private static String[][] ft = { { "jpg", "image/jpeg" },
			{ "jpeg", "image/jpeg" }, { "jp2", "image/jp2" },
			{ "png", "image/png" }, { "gif", "image/gif" },
			{ "tif", "image/tiff" }, { "tiff", "image/tiff" },
			{ "txt", "text/plain" }, { "html", "text/html" },
			{ "htm", "text/html" }, { "xml", "text/xml" },
			{ "svg", "image/svg+xml" } };

	public static Map fileTypes;

	public static List imageExtensions;

	public static List textExtensions;

	public static List svgExtensions;

	public static final int CLASS_NONE = -1;

	public static final int CLASS_IMAGE = 0;

	public static final int CLASS_TEXT = 1;

	public static final int CLASS_SVG = 2;

	public static final int NUM_CLASSES = 3;

	public static int[] fileClasses = {};
	
	public static int[] fcIndexes = {};

	public static final Integer HINT_BASEDIRS = new Integer(1);

	public static final Integer HINT_FILEEXT = new Integer(2);

	public static final Integer HINT_DIRS = new Integer(3);

	public static File[] baseDirs = {};
	
	public static DocuDirCache cache;

	/**
	 * static initializer for FileOps
	 */
	static {
		fileTypes = new HashMap();
		imageExtensions = new ArrayList();
		textExtensions = new ArrayList();
		svgExtensions = new ArrayList();
		// iterate through file types in ft and fill the Map and Lists
		for (int i = 0; i < ft.length; i++) {
			String ext = ft[i][0];
			String mt = ft[i][1];
			fileTypes.put(ext, mt);
			if (classForMimetype(mt) == CLASS_IMAGE) {
				imageExtensions.add(ext);
			} else if (classForMimetype(mt) == CLASS_TEXT) {
				textExtensions.add(ext);
			} else if (classForMimetype(mt) == CLASS_SVG) {
				svgExtensions.add(ext);
			}
		}
	}

	/** sets the array of actually used file classes */
	public static void setFileClasses(int[] fc) {
		fileClasses = fc;
		fcIndexes = new int[NUM_CLASSES];
		for (int i = 0; i < fc.length; i++) {
			fcIndexes[fc[i]] = i;
		}
	}

	/** returns the array of actually used file classes */
	public static int[] getFileClasses() {
		return fileClasses;
	}

	/** returns an element from the array of actally used file classes */
	public static int getFileClass(int idx) {
		try {
			return fileClasses[idx];
		} catch (Exception e) {
		}
		return CLASS_NONE;
	}

	/** Returns the index number for the given file class.
	 * @param fc
	 * @return
	 */
	public static int getFCindex(int fc) {
		try {
			return fcIndexes[fc];
		} catch (Exception e) {
		}
		return -1;
	}
	
	/**
	 * returns the file class for a mime-type
	 * 
	 * @param mt
	 * @return
	 */
	public static int classForMimetype(String mt) {
		if (mt == null) {
			return CLASS_NONE;
		}
		if (mt.startsWith("image/svg")) {
			return CLASS_SVG;
		} else if (mt.startsWith("image")) {
			return CLASS_IMAGE;
		} else if (mt.startsWith("text")) {
			return CLASS_TEXT;
		}
		return CLASS_NONE;
	}

	/**
	 * get the mime type for a file format (by extension)
	 */
	public static String mimeForFile(File f) {
		return (String) fileTypes.get(extname(f.getName().toLowerCase()));
	}

	/**
	 * get the file class for the filename (by extension)
	 * 
	 * @param fn
	 * @return
	 */
	public static int classForFilename(String fn) {
		String mt = (String) fileTypes.get(extname(fn).toLowerCase());
		return classForMimetype(mt);
	}

	/**
	 * get the file class for the file (by extension)
	 * 
	 * @param fn
	 * @return
	 */
	public static int classForFile(File f) {
		return classForFilename(f.getName());
	}

	public static Iterator getImageExtensionIterator() {
		return imageExtensions.iterator();
	}

	public static Iterator getTextExtensionIterator() {
		return textExtensions.iterator();
	}

	public static Iterator getSVGExtensionIterator() {
		return svgExtensions.iterator();
	}

	/**
	 * convert a string with a list of pathnames into an array of strings using
	 * the system's path separator string
	 */
	public static String[] pathToArray(String paths) {
		// split list into directories
		StringTokenizer dirs = new StringTokenizer(paths, File.pathSeparator);
		int n = dirs.countTokens();
		if (n < 1) {
			return null;
		}
		// add directories into array
		String[] pathArray = new String[n];
		for (int i = 0; i < n; i++) {
			String s = dirs.nextToken();
			// make shure the dir name ends with a directory separator
			if (s.endsWith(File.separator)) {
				pathArray[i] = s;
			} else {
				pathArray[i] = s + File.separator;
			}
		}
		return pathArray;
	}

	/**
	 * Extract the base of a file name (sans extension).
	 * 
	 * Returns the filename without the extension. The extension is the part
	 * behind the last dot in the filename. If the filename has no dot the full
	 * file name is returned.
	 * 
	 * @param fn
	 * @return
	 */
	public static String basename(String fn) {
		if (fn == null) {
			return null;
		}
		int i = fn.lastIndexOf('.');
		if (i > 0) {
			return fn.substring(0, i);
		}
		return fn;
	}

	/**
	 * Extract the base of a file name (sans extension).
	 * 
	 * Returns the filename without the extension. The extension is the part
	 * behind the last dot in the filename. If the filename has no dot the full
	 * file name is returned.
	 * 
	 * @param f
	 * @return
	 */
	public static String basename(File f) {
		if (f == null) {
			return null;
		}
		return basename(f.getName());
	}

	/**
	 * Extract the extension of a file name.
	 * 
	 * Returns the extension of a file name. The extension is the part behind
	 * the last dot in the filename. If the filename has no dot the empty string
	 * is returned.
	 * 
	 * @param fn
	 * @return
	 */
	public static String extname(String fn) {
		if (fn == null) {
			return null;
		}
		int i = fn.lastIndexOf('.');
		if (i > 0) {
			return fn.substring(i + 1);
		}
		return "";
	}

	/**
	 * Extract the parent directory of a (digilib) path name.
	 * 
	 * Returns the parent directory of a path name. The parent is the part
	 * before the last slash in the path name. If the path name has no slash the
	 * empty string is returned.
	 * 
	 * @param fn
	 * @return
	 */
	public static String dlParent(String fn) {
		if (fn == null) {
			return null;
		}
		int i = fn.lastIndexOf('/');
		if (i > 0) {
			return fn.substring(0, i);
		}
		return "";
	}

	/**
	 * Extract the dir/file name of a (digilib) path name.
	 * 
	 * The file/dir name is the part after the last slash in the path name. If
	 * the path name has no slash the same string is returned.
	 * 
	 * @param path
	 * @return
	 */
	public static String dlName(String path) {
		if (path == null) {
			return null;
		}
		int i = path.lastIndexOf('/');
		if (i > 0) {
			return path.substring(i+1);
		}
		return path;
	}

	/**
	 * Normalize a path name.
	 * 
	 * Removes leading and trailing slashes. Returns null if there is other
	 * unwanted stuff in the path name.
	 * 
	 * @param pathname
	 * @return
	 */
	public static String normalName(String pathname) {
		if (pathname == null) {
			return null;
		}
		// upper-dir references are unwanted
		if (pathname.indexOf("../") >= 0) {
			return null;
		}
		int a = 0;
		int e = pathname.length() - 1;
		if (e < 0) {
			return pathname;
		}
		// leading and trailing "/" are removed
		while ((a <= e) && (pathname.charAt(a) == '/')) {
			a++;
		}
		while ((a < e) && (pathname.charAt(e) == '/')) {
			e--;
		}
		return pathname.substring(a, e + 1);
	}

	public static StringTokenizer dlPathIterator(String path) {
		return new StringTokenizer(path, "/");
	}
	
	
	/**
	 * FileFilter for general files
	 */
	static class ReadableFileFilter implements FileFilter {

		public boolean accept(File f) {
			return f.canRead();
		}
	}

	/**
	 * FileFilter for image types (helper class for getFile)
	 */
	static class ImageFileFilter implements FileFilter {

		public boolean accept(File f) {
			return (classForFilename(f.getName()) == CLASS_IMAGE);
		}
	}

	/**
	 * FileFilter for text types (helper class for getFile)
	 */
	static class TextFileFilter implements FileFilter {

		public boolean accept(File f) {
			return (classForFilename(f.getName()) == CLASS_TEXT);
		}
	}

	/**
	 * FileFilter for svg types (helper class for getFile).
	 *  
	 */
	static class SVGFileFilter implements FileFilter {

		public boolean accept(File f) {
			return (classForFilename(f.getName()) == CLASS_SVG);
		}
	}

	/**
	 * Factory for FileFilters (image or text).
	 * 
	 * @param fileClass
	 * @return
	 */
	public static FileFilter filterForClass(int fileClass) {
		if (fileClass == CLASS_IMAGE) {
			return new ImageFileFilter();
		}
		if (fileClass == CLASS_TEXT) {
			return new TextFileFilter();
		}
		if (fileClass == CLASS_SVG) {
			return new SVGFileFilter();
		}
		return null;
	}

	/**
	 * Factory for DocuDirents based on file class.
	 * 
	 * Returns an ImageFileset, TextFile or SVGFile.
	 * 
	 * @param fileClass
	 * @param file
	 * @param parent
	 * @param hints
	 *            optional additional parameters
	 * @return
	 */
	public static DigiDirent fileForClass(int fileClass, File file,
			DigiDirectory parent, Map hints) {
		// what class of file do we have?
		if (fileClass == CLASS_IMAGE) {
			// image file
			return new ImageFileset(file, parent, hints);
		} else if (fileClass == CLASS_TEXT) {
			// text file
			return new TextFile(file, parent);
		} else if (fileClass == CLASS_SVG) {
			// text file
			return new SVGFile(file, parent);
		}
		// anything else is a generic dir or file
		if (file.isDirectory()) {
			return getCachedDirectory(file, null, parent);
		}
		return new DigiDirent(file.getName(), parent);
	}

	/**
	 * Filters a list of Files through a FileFilter.
	 * 
	 * @param files
	 * @param filter
	 * @return
	 */
	public static File[] listFiles(File[] files, FileFilter filter) {
		if (files == null) {
			return null;
		}
		File[] ff = new File[files.length];
		int ffi = 0;
		for (int i = 0; i < files.length; i++) {
			if (filter.accept(files[i])) {
				ff[ffi] = files[i];
				ffi++;
			}
		}
		File[] fff = new File[ffi];
		System.arraycopy(ff, 0, fff, 0, ffi);
		return fff;
	}

	/**
	 * Returns the closest matching file out of an array.
	 * 
	 * Compares the files sans extensions if no direct match is found. Returns
	 * null if no match is found.
	 * 
	 * @param fn
	 * @param files
	 * @return
	 */
	public static File findFile(File fn, File[] files) {
		// try the same filename as the original
		int fileIdx = Arrays.binarySearch(files, fn);
		if (fileIdx >= 0) {
			return files[fileIdx];
		} else {
			// try closest matches without extension
			String fb = FileOps.basename(fn);
			fileIdx = -fileIdx - 1;
			if ((fileIdx < files.length)
					&& (FileOps.basename(files[fileIdx]).equals(fb))) {
				// idx ok
				return files[fileIdx];
			} else if ((fileIdx > 0)
					&& (FileOps.basename(files[fileIdx - 1]).equals(fb))) {
				// idx-1 ok
				return files[fileIdx - 1];
			} else if ((fileIdx + 1 < files.length)
					&& (FileOps.basename(files[fileIdx + 1]).equals(fb))) {
				// idx+1 ok
				return files[fileIdx + 1];
			}
		}
		// unknown
		return null;
	}

	/**
	 * Returns the closest matching file out of an array.
	 * 
	 * Compares the files sans extensions if no direct match is found. Returns
	 * null if no match is found.
	 * 
	 * @param fn
	 * @param files
	 * @return
	 */
	public static String findFilename(String fn, String[] files) {
		// try the same filename as the original
		int fileIdx = Arrays.binarySearch(files, fn);
		if (fileIdx >= 0) {
			return files[fileIdx];
		} else {
			// try closest matches without extension
			String fb = FileOps.basename(fn);
			fileIdx = -fileIdx - 1;
			if ((fileIdx < files.length)
					&& (FileOps.basename(files[fileIdx]).equals(fb))) {
				// idx ok
				return files[fileIdx];
			} else if ((fileIdx > 0)
					&& (FileOps.basename(files[fileIdx - 1]).equals(fb))) {
				// idx-1 ok
				return files[fileIdx - 1];
			} else if ((fileIdx + 1 < files.length)
					&& (FileOps.basename(files[fileIdx + 1]).equals(fb))) {
				// idx+1 ok
				return files[fileIdx + 1];
			}
		}
		// unknown
		return null;
	}
	/**
	 * Returns a File for a base directory and a digilib-path.
	 * 
	 * @param basedir
	 * @param dlpath
	 * @return
	 */
	public static File getRealFile(File basedir, String dlpath) {
		// does this work on all platforms??
		return new File(basedir, dlpath);
	}

	/** Returns a File for a digilib-path.
	 * 
	 * The file is assumed to be in the first base directory.
	 * 
	 * @param dlpath
	 * @return
	 */
	public static File getRealFile(String dlpath) {
		// does this work on all platforms??
		return new File(baseDirs[0], dlpath);
	}

	/**
	 * Creates a new empty hints Map.
	 * 
	 * @return
	 */
	public static Map newHints() {
		Map m = new HashMap();
		return m;
	}

	/**
	 * Creates a new hints Map with the given first element.
	 * 
	 * @param type
	 * @param value
	 * @return
	 */
	public static Map newHints(Integer type, Object value) {
		Map m = new HashMap();
		if (type != null) {
			m.put(type, value);
		}
		return m;
	}

	/**
	 * @return Returns the baseDirs.
	 */
	public static File[] getBaseDirs() {
		return baseDirs;
	}

	/**
	 * @param baseDirs
	 *            The baseDirs to set.
	 */
	public static void setBaseDirs(File[] baseDirs) {
		FileOps.baseDirs = baseDirs;
	}

	
	/**
	 * Returns a DigiDirectory instance that is guaranteed to be unique in the
	 * cache.
	 * 
	 * @param dir
	 * @param parent
	 * @return
	 */
	public static DigiDirectory getCachedDirectory(File dir, String dlpath, DigiDirectory parent) {
		if (dir == null) {
			dir = FileOps.getRealFile(dlpath);
		}
		DigiDirectory dd = null;
		if (parent == null) {
			// create a new parent by starting at the root
			StringBuffer ps = new StringBuffer();
			DigiDirectory p = cache.getRootDir();
			// walk the path
			for (StringTokenizer i = dlPathIterator(dlpath); i.hasMoreTokens();) {
				p.check();
				String dn = i.nextToken();
				ps.append("/");
				ps.append(dn);
				DigiDirectory d = cache.get(dn);
				if (d == null) {
					dd = new DigiDirectory(FileOps.getRealFile(dn), ps.toString(), p);
				}
				if (d.getParent() != p) {
					logger.warn("digidirectory "+d.getDLPath()+" has wrong parent: "+p.getDLPath());
				}
				p = d;
			}
		} else {
			if (dlpath == null) {
				dlpath = parent.getDLPath() + "/" + dir.getName();
			}
			dd = cache.get(dlpath);
			if (dd == null) {
				dd = new DigiDirectory(dir, dlpath, parent);
			} else {
				logger.debug("reusing directory:" + dlpath);
			}
		}
		return dd;
	}
	
	/**
	 * @return Returns the cache.
	 */
	public static DocuDirCache getCache() {
		return cache;
	}
	/**
	 * @param cache The cache to set.
	 */
	public static void setCache(DocuDirCache cache) {
		FileOps.cache = cache;
	}
}
