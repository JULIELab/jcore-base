/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the  Eclipse Public License (EPL) v3.0
 */

package edu.upenn.seas.mstparser;

import java.io.*;

public class RelationalFeature implements Serializable {

    public String name;
    public String[][] values;

    public RelationalFeature(int size, String declaration, BufferedReader br) throws IOException {
	values = new String[size][size];
	String[] declist = declaration.split(" ");
	name = declist[2];
	for (int i=0; i<size; i++) {
	    values[i] = br.readLine().substring(2).split(" ");
	}
    }

    public String getFeature(int firstIndex, int secondIndex) {
	if (firstIndex == 0 || secondIndex == 0)
	    return name+"=NULL";
	else
	    //System.out.println(values.length + "** " + name+"="+values[firstIndex-1][secondIndex-1]);
	    return name+"="+values[firstIndex-1][secondIndex-1];
    }

    private void writeObject (ObjectOutputStream out) throws IOException {
	out.writeObject(name);
	out.writeObject(values);
    }


    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
	name = (String)in.readObject();
	values = (String[][])in.readObject();
    }


}
