/* DigiDirent.java -- an entry in a directory
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
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * an entry in a directory
 * 
 * @author casties
 *  
 */
public class DigiDirent implements Comparable {

	protected static Logger logger = Logger.getLogger(DigiDirent.class);
	
	protected static int fileClass = FileOps.CLASS_NONE;
	
	protected static boolean isFile = true;

	protected static boolean isDirectory = false;

	protected String name;

	protected DigiDirectory parent;

	protected Map meta;

	protected boolean isMetaRead = false;

	/**
	 *  
	 */
	public DigiDirent() {
		super();
	}

	/**
	 * @param name
	 * @param parent
	 */
	public DigiDirent(String name) {
		super();
		this.name = name;
	}

	/**
	 * @param name
	 * @param parent
	 */
	public DigiDirent(String name, DigiDirectory parent) {
		super();
		this.name = name;
		this.parent = parent;
	}

	public boolean isFile() {
		return isFile;
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	/**
	 * @return Returns the file class.
	 */
	public int getFileClass() {
		return fileClass;
	}
	
	public String getName() {
		return name;
	}

	/**
	 * @return Returns the parent.
	 */
	public DigiDirectory getParent() {
		return parent;
	}

	/**
	 * @param parent
	 *            The parent to set.
	 */
	public void setParent(DigiDirectory parent) {
		this.parent = parent;
	}

	/**
	 * @return Returns the meta.
	 */
	public Map getMeta() {
		return meta;
	}

	/**
	 * @param meta
	 *            The meta to set.
	 */
	public void setMeta(Map meta) {
		this.meta = meta;
	}

	/**
	 * Adds metadata to this files metadata.
	 * 
	 * @param meta
	 */
	public void addMeta(Map meta) {
		if (meta == null) {
			return;
		}
		if ((this.meta == null) || (this.meta.isEmpty())) {
			this.meta = meta;
		} else {
			this.meta.putAll(meta);
		}
	}

	/**
	 * Reads meta-data for this file if there is any.
	 *  
	 */
	public void readMeta() {
		if (isMetaRead || (parent == null) || (!parent.exists())) {
			// there is already metadata or there is no file
			return;
		}
		// metadata is in the file {filename}.meta
		String fn = parent.getDir().getAbsolutePath();
		File mf = new File(fn, name + ".meta");
		if (mf.canRead()) {
			XMLMetaLoader ml = new XMLMetaLoader();
			try {
				// read meta file
				Map fileMeta = ml.loadURL(mf.getAbsolutePath());
				// meta for this file is in an entry with its name
				addMeta((Map)fileMeta.get(name));
			} catch (Exception e) {
				Logger.getLogger(this.getClass()).warn(
						"error reading file .meta", e);
			}
		}
		isMetaRead = true;
	}
	
	/**
	 * Checks metadata.
	 *  
	 */
	public void check() {
		if (isMetaRead) {
			return;
		}
		// read the metadata file
		readMeta();
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return (getName().compareTo(o));
	}

}
