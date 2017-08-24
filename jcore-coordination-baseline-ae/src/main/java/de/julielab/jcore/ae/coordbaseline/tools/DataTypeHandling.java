/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 */

package de.julielab.jcore.ae.coordbaseline.tools;

import java.util.ArrayList;

/**
 * This class provides methods which deal with data type conversion. 
 * 
 * @author lichtenwald
 *
 */
public class DataTypeHandling 
{

	/**
	 * This method is used to reverse the order of the elements within the input ArrayList. The reversed elements 
	 * will be stored in the output ArrayList.
	 * 
	 * @param inputList the list which content will be reversed and then put into the output list
	 * 
	 * @return outputList the list which will be returned after being filled with the reversed content of the input list
	 */
	public static ArrayList reverseList(ArrayList inputList)
	{
		ArrayList outputList = new ArrayList();
		for (int i=inputList.size()-1; i>=0; i--)
		{
			outputList.add(inputList.get(i));
		} // of for
		return outputList;
	} // of reverseList
	
	/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to simply copy or clone two lists. In other words, 
	 * the elements of the inputList will be copied to the outputList.	
	 * 
	 * @param inputList the list which content will be copied to the output list.
	 * 
	 * @return outputList the list which will be returned after being filled with the content of the input list				
	 *
	 */
	public static ArrayList cloneList(ArrayList inputList)
	{
		ArrayList outputList = new ArrayList();
		for (int i = 0; i < inputList.size(); i++)
		{
			outputList.add(inputList.get(i));
		} // of for
		return outputList;
	} // of cloneList
	
/*------------------------------------------------------------------------------------------*/	
	/**
	 * This method is used to convert a string array to string array list.
	 * 
	 * @param array String array which will be converted to a string array list
	 * @return list ArrayList which is the result of the conversion
	 */
	public static ArrayList<String> arrayToArrayList(String[] array)
	{
		ArrayList<String> list = new ArrayList<String>();
		
		for (int i = 0; i < array.length; i++)
		{
			list.add(array[i]);
		} // of for
		return list;
	} // of fillList
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to convert an ArrayList of Strings to a String using the separator 
	 * to separate the list elements within the String.
	 * 
	 * @param inputList ArrayList which elements will be put into the output string
	 * @param separator String which will separate the list elements within the string
	 * @return outputString String which will contain the list elements separated by the separator
	 */
	public static String arrayListToString(ArrayList<String> inputList, String separator)
	{
		String outputString = "";
		
		for (int i = 0; i < inputList.size(); i++)
		{
			outputString = outputString + inputList.get(i) + separator;
		} // of for
		
		return outputString;
	} // of arrayListToString
	
/*------------------------------------------------------------------------------------------*/
	
	/**
	 * This method is used to append elements of one ArrayList of Strings to another. Every element of
	 * the list2 will be stuck to the corresponging element of the list1 using the separator between them. 
	 * The output element will be (list1 element)(separator)(list2 element). 
	 * 
	 * @param list1 ArrayList to which elements the elements of the second list will be added
	 * @param list2 ArrayList which elements will be added to the elements of the first list
	 * @param separator String which will be put between the elements of the first and the second list to separate them
	 * @return outputList ArrayList which will contain the elements of both input lists according to the computation
	 */
	public static ArrayList<String> appendElements(ArrayList<String> list1, ArrayList<String>list2, String separator)  // FIXME throws UnequalListSizeException
	{
		ArrayList<String> outputList = new ArrayList<String>();
		
		/*------------------------------------------------------------------*/
		/* If the two list sizes aren't equal, an exception will be thrown.	*/
		/*------------------------------------------------------------------*/
		if (list1.size() != list2.size()) 
		{
			// throw UnequalListSizeException
		} // of if
		
		for (int i = 0; i < list1.size(); i++)
		{
			outputList.add(list1.get(i) + separator + list2.get(i));
		} // of for
		
		return outputList;
	} // of appendElements
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to initialize a given boolean array with a boolean value
	 * 
	 * @param array boolean array which will be initialized and then returned
	 * @param value boolean variable which the array will be initialized with
	 * @return array boolean array which was initialized with the boolean value
	 */
	public static boolean[] initializeArray(boolean[] array, boolean value)
	{
		for (int i = 0; i < array.length; i++)
		{
			array[i] = value;
		} // of for
		
		return array;
	} // of initializeArray
	
	
	/**
	 * This method is used to initialize a given String array with a given String value
	 * 
	 * @param array String array which will be initialized and then returned
	 * @param value String variable which the array will be initialized with
	 * @return array String array which was initialized with the String value
	 */
	public static String[] initializeArray(String[] array, String value)
	{
		for (int i = 0; i < array.length; i++)
		{
			array[i] = value;
		} // of for
		
		return array;
	} // of initializeArray
	
	
} // of DataTypeHandling
