/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.reader.xmlmapper;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import com.ximpleware.AutoPilot;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

public class EncodingTest {
	@Test
	public void testEmoji() throws Exception {
	    String xmlDocument = "";
	    xmlDocument += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	    xmlDocument += "<root>";
	    xmlDocument += "<token>emoji\uD83C\uDDE7</token>";
//	    xmlDocument += "<token>"+new String(Character.toChars(0x1F1E7))+"</token>";
	    xmlDocument += "</root>";
	    VTDGen vg = new VTDGen();
	    vg.setDoc(xmlDocument.getBytes("UTF-8"));
	    vg.parse(true);
	    VTDNav vn = vg.getNav();
	    AutoPilot ap = new AutoPilot(vn);
	    ap.selectElementNS("","token");
	    while(ap.iterate()) {
	        int t = vn.getText();
	        if (t != -1) {
	            String value = vn.toRawString(t);
	            System.out.println("emoji\uD83C\uDDE7");
	            System.out.println(Character.codePointAt(value.toCharArray(), 0));
//	            assertEquals(value, "emoji\uD83C\uDDE7");
	            assertEquals(value, "emoji"+new String(Character.toChars(0x1F1E7)));
	        }
	    }
	}
}
