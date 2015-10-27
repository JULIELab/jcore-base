/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 */

package de.julielab.jcore.ae.coordbaseline.tools;

import java.util.ArrayList;

/**
 * 
 * @author lichtenwald
 *
 * This class contains methods which are used to output some data and hence are useful for debugging purposes
 */
public class Output 
{		
	
/*--------------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to simply output a string array list to the console for debugging purposes
	 * 
	 * @param list ArrayList which elements will be output to the console
	 */
	public static void StringArrayList(ArrayList<String> list)
	{
		for (int i = 0; i < list.size(); i++)
		{
			System.out.println(list.get(i));
		} // of for
	} // of StringArrayList
	
/*--------------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to simply output a boolean array to the console for debugging purposes
	 * 
	 * @param array boolean array which elements will be output to the console
	 */
	public static void booleanArray(boolean [] array)
	{
		for (int i = 0; i < array.length; i++)
		{
			System.out.println(array[i]);
		} // of for
	} // of booleanArray
	
/*--------------------------------------------------------------------------------------------------*/
	
} // of Output
