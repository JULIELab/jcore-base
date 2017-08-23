/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
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
