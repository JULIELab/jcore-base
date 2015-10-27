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

/**
 * A writer to create files in CONLL format.
 *
 * <p>
 * Created: Sat Nov 10 15:25:10 2001
 * </p>
 *
 * @author Jason Baldridge
 * @version $Id: CONLLWriter.java 94 2007-01-17 17:05:12Z jasonbaldridge $
 * @see edu.upenn.seas.mstparser.io.DependencyWriter
 */
public class CONLLWriter extends DependencyWriter {

    public CONLLWriter (boolean labeled) {
	this.labeled = labeled;
    }

    public String write(DependencyInstance instance,boolean fileAccess) throws IOException {
	StringBuilder b = new StringBuilder();
	for (int i=0; i<instance.length(); i++) {
	    b.append(Integer.toString(i+1)).append('\t');
	    b.append(instance.forms[i]).append('\t');
	    b.append(instance.forms[i]).append('\t');
	    //writer.write(instance.cpostags[i]);                 writer.write('\t');
	    b.append(instance.postags[i]).append('\t');
	    b.append(instance.postags[i]).append('\t');
	    b.append("-").append('\t');
	    b.append(Integer.toString(instance.heads[i])).append('\t');
	    b.append(instance.deprels[i]).append('\t');
	    b.append("-\t-");
	    b.append('\n');
	}
	b.append('\n');
	String result=b.toString();
	if(fileAccess){
		writer.write(result);
	}
	return result;
    }


}
