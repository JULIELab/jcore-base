/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 */

package de.julielab.jcore.ae.coordbaseline.tools;

/**
 * 
 * @author lichtenwald
 *
 * This class contains some data types which are used by other classes. 
 */
public class DataTypes 
{
	/*----------------------------------------------------------*/
	/* This String array contains Strings that represent the 	*/
	/* coordinations which can occur in sentences.				*/
	/*----------------------------------------------------------*/
	private static String [] coordinationArray = {
			"and",
			"or",
			"but", 
			"nor", 
			"and/or", 
			"yet", 
			"both", 
			"either", 
			"neither",
			",", 
			"/",
			"+/-",
			"versus", 
			"vs",
			"vs."
			};
	
	/* get-Methods	*/
	public static String[] getCoordinationArray() {return coordinationArray;}
	
	
	/* set-Methods	*/
	public static void setCoordinationArray(){}
} // of DataTypes
