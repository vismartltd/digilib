/*
 * AliasingDocuDirCache -- DocuDirCache using alias entries from config file
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
 * Created on 04.11.2003
 */

package digilib.io;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

/**
 * @author casties
 *  
 */
public class AliasingDocuDirCache extends DocuDirCache {

	/**
	 * @param baseDirs
	 * @param fileClasses
	 * @param confFileName
	 * @throws FileOpException
	 */
	public AliasingDocuDirCache(File confFile) throws FileOpException {
		// create standard DocuDirCache
		super();
		Map pathMap = null;
		// read alias config file
		try {
			// load into pathMap
			XMLListLoader mapLoader = new XMLListLoader("digilib-aliases",
					"mapping", "link", "dir");
			pathMap = mapLoader.loadURL(confFile.toURL().toString());
		} catch (Exception e) {
			throw new FileOpException("ERROR loading mapping file: " + e);
		}
		if (pathMap == null) {
			throw new FileOpException("ERROR: unable to load mapping file!");
		}

		/*
		 * load map entries into cache
		 */

		for (Iterator i = pathMap.keySet().iterator(); i.hasNext();) {
			String link = FileOps.normalName((String) i.next());
			String dn = (String) pathMap.get(link);
			File dir = FileOps.getRealFile(dn);
			if (dir.isDirectory()) {
				logger.debug("Aliasing dir: " + link);
				DigiDirectory destDir = new DigiDirectory(dir, dn, null);
				// add the alias name
				putName(link, destDir);
				// add the real dir
				putDir(destDir);
			}
		}
	}

	/**
	 * Adds a DocuDirectory under another name to the cache.
	 * 
	 * @param name
	 * @param newdir
	 */
	public void putName(String name, DigiDirectory newdir) {
		Object oldkey = map.put(name, newdir);
		if (oldkey != null) {
			logger
					.warn("Duplicate key in AliasingDocuDirCache.put -- replaced!");
		}
	}

}
