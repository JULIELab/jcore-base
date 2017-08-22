/** 
 * ExtensionFileFilter.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.6 	
 * Since version:   1.6
 *
 * Creation date: Jan 28, 2008 
 * 
 **/

package de.julielab.jules.ae.genemapper.utils;

import java.io.File;
import java.io.FilenameFilter;

public class ExtensionFileFilter  implements FilenameFilter {
	String ext = ""; // the extension to use, e.g. "xmi"

	public ExtensionFileFilter(String ext) {
		this.ext = ext;
	}

	public boolean accept(File f, String s) {
		return s.toLowerCase().endsWith("." + ext);
	}
}
