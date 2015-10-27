///////////////////////////////////////////////////////////////////////////////
// Copyright (C) 2007 University of Texas at Austin and (C) 2005
// University of Pennsylvania and Copyright (C) 2002, 2003 University
// of Massachusetts Amherst, Department of Computer Science.
//
// This software is licensed under the terms of the Common Public
// License, Version 1.0 or (at your option) any subsequent version.
// 
// The license is approved by the Open Source Initiative, and is
// available from their website at http://www.opensource.org.
///////////////////////////////////////////////////////////////////////////////

package edu.upenn.seas.mstparser.io;

import java.io.*;

/**
 * A class that defines common behavior and abstract methods for
 * readers for different formats.
 *
 * <p>
 * Created: Sat Nov 10 15:25:10 2001
 * </p>
 *
 * @author Jason Baldridge
 * @version $Id: DependencyReader.java 112 2007-03-23 19:19:28Z jasonbaldridge $
 */
public abstract class DependencyReader {

	protected BufferedReader inputReader;
	protected boolean labeled = true;

	public static DependencyReader createDependencyReader (String format, 
			boolean discourseMode) 
	throws IOException {

		if (format.equals("MST")) {
			return new MSTReader();
		} else if (format.equals("CONLL")) {
			return new CONLLReader(discourseMode);
		} else {
			System.out.println("!!!!!!!  Not a supported format: " + format);
			System.out.println("********* Assuming CONLL format. **********");
			return new CONLLReader(discourseMode);
		}
	}

	public static DependencyReader createDependencyReader (String format)
	throws IOException {

		return createDependencyReader(format, false);
	}


	public boolean startReading (String content,boolean fileAccess) throws IOException {

		labeled = fileContainsLabels(content,fileAccess);
		inputReader = new BufferedReader(this.getReader(content,fileAccess));
		
		return labeled;
	}

	protected Reader getReader(String content,boolean fileAccess) throws IOException {
		if(fileAccess){
			
			return new InputStreamReader(new FileInputStream(content),"UTF8");
			
		}
		else
			return new StringReader(content);
	}

	public boolean isLabeled() {
		return labeled;
	}

	public abstract edu.upenn.seas.mstparser.DependencyInstance getNext() throws IOException;

	protected abstract boolean fileContainsLabels(String filename, boolean fileAccess) throws IOException;


	protected String normalize (String s) {
		if(s.matches("[0-9]+|[0-9]+\\.[0-9]+|[0-9]+[0-9,]+"))
			return "<num>";

		return s;
	}	

}
