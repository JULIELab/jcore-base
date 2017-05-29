/** 
 * DefaultFileFilter.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: tomanek
 * 
 * Current version: 1.6	
 * Since version:   1.0
 *
 * Creation date: Aug 01, 2006 
 * 
 * A default file filter that accepts all files.
 **/

package de.julielab.jcore.ae.jsbd;

import java.io.File;
import java.io.FileFilter;

class DefaultFileFilter  implements FileFilter {

	public DefaultFileFilter() {
		
	}
  public boolean accept( File f ) {
    return true;
  }
}