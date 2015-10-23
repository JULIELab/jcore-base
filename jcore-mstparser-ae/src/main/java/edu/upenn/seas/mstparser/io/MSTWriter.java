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

import edu.upenn.seas.mstparser.DependencyInstance;
import edu.upenn.seas.mstparser.Util;

/**
 * A writer to create files in MST format.
 *
 * <p>
 * Created: Sat Nov 10 15:25:10 2001
 * </p>
 *
 * @author Jason Baldridge
 * @version $Id: MSTWriter.java 94 2007-01-17 17:05:12Z jasonbaldridge $
 * @see edu.upenn.seas.mstparser.io.DependencyWriter
 */
public class MSTWriter extends DependencyWriter {

    public MSTWriter (boolean labeled) {
	this.labeled = labeled;
    }

    public String write(DependencyInstance instance,boolean fileAccess) throws IOException {
    	StringBuilder b = new StringBuilder();
    	b.append(Util.join(instance.forms, '\t')).append("\n");
    	b.append(Util.join(instance.postags, '\t')).append("\n");
		if (labeled){
			b.append(Util.join(instance.deprels, '\t')).append("\n");
		}
		b.append(Util.join(instance.heads, '\t')).append("\n\n");
		String outString = b.toString();
		if(fileAccess) {
			writer.write(outString);
		}
		return outString;
    }

}
