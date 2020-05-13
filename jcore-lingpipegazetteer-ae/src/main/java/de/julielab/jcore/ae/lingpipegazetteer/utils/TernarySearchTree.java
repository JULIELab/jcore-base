/** 
 *
 * Author: kampe
 * 
 * Creation date: 01.12.2008
 **/

package de.julielab.jcore.ae.lingpipegazetteer.utils;


/**
 * Java implementation of a data structure by Jon Bentley and Bob Sedgewick.

 * @author kampe
 */
public class TernarySearchTree {
	
	private TSTNode root;
	
	public TernarySearchTree() {
		this.root = new TSTNode();
	}
	
	public boolean contains(String s) {
		TSTNode node = root;
		int index = 0;
		while (node != null) {
			if (s.charAt(index) < node.splitchar) {
				node = node.loKID;
			}
			else if (s.charAt(index) == node.splitchar) {
				if (++index == s.length()) {
					return true;
				}
				node = node.eqKID;
			} else {
				node = node.hiKID;
			}
		}
		return false;
	}
	
	public TSTNode add(TSTNode node, String str, int index) {
		while (str.length() > index){
			if (node == null) {
				node = new TSTNode();
				node.splitchar = str.charAt(index);
				System.out.println(node.splitchar);
			}
			if (str.charAt(index) < node.splitchar) {
				node.loKID = add(node.loKID, str, index);
			}
			else if (str.charAt(index) == node.splitchar) {		
					node.eqKID = add(node.eqKID, str, ++index);
				
			}

			else {
				node.hiKID = add(node.hiKID, str, index);
			}
		return node;
		}
		return null;
	}
	
	public boolean add(String str) {
		TSTNode node = root;
		int index = 0;
		while (str.length() > index){
			if (node == null) {
				node = new TSTNode();
				node.splitchar = str.charAt(index);
				System.out.println(node.splitchar);
			}
			if (str.charAt(index) < node.splitchar) {
				node = node.loKID;				
			} else if (str.charAt(index) == node.splitchar) {
				++index;
				node = node.eqKID;
//				continue;
			} else {
				node.hiKID = add(node.hiKID, str, index);
//				node = node.hiKID;
			}
		return true;
		}
		return false;
	}
	
	
//	boolean insert3(String s){
//		int index = 0;
////	{   int d;
//		int d;
////	    char *instr = s;
//		String instr = s;
////	    Tptr pp, *p;
//		TSTNode pp;
////	    p = &root;
//		TSTNode node = root;
////	    while (pp = *p) {
//		while ((pp = node) != null) {
//			System.out.println("s: " + s.charAt(index));
//			System.out.println("splitchar: " + node.splitchar);
////	        if ((d = *s - pp->splitchar) == 0) {
//			if ((d = s.charAt(index) - node.splitchar) == 0){	
//				System.out.println(s.charAt(index));
////	            if (*s++ == 0) return;
//				if (++index == s.length()) {
//					return false;
//				}
////	            p = &(pp->eqkid);
//				node = pp.eqKID;
////	        }
//			}
////				else if (d < 0)
//			else if (d < 0) {
////	            p = &(pp->lokid);
//				node = pp.loKID;
//			}
////	        else
//			else {
////	            p = &(pp->hikid);
//				node = pp.hiKID;
//			}
////	    }
//		}
////	    for (;;) {
//		for (;;) {
////	        /* *p = (Tptr) malloc(sizeof(Tnode)); */
////	          if (bufn-- == 0) {
////	              buf = (Tptr) malloc(BUFSIZE *
////	                              sizeof(Tnode));
////	              freearr[freen++] = (void *) buf;
////	              bufn = BUFSIZE-1;
////	          }
////	          *p = buf++;
//			node = new TSTNode();
////	        pp = *p;
//			pp = node;
////	        pp->splitchar = *s;
//			pp.splitchar = s.charAt(index);
////	        pp->lokid = pp->eqkid = pp->hikid = 0;
////	        if (*s++ == 0) {
//			if (++index == s.length()) {
////	            pp->eqkid = (Tptr) instr;
//				pp.eqKID = new TSTNode(instr.charAt(0));
////	            return;
//				return true;
////	        }
//			}
////	        p = &(pp->eqkid);
//			node = pp.eqKID;
////	    }
//		}	
//		
//	}
	
	private class TSTNode {
		char splitchar;
		TSTNode loKID;
		TSTNode eqKID;
		TSTNode hiKID;
		
		TSTNode() {
			splitchar = 0;
			loKID = null;
			eqKID = null;
			hiKID = null;
		}
		
//		TSTNode(char c) {
//			splitchar = c;
//			loKID = null;
//			eqKID = null;
//			hiKID = null;
//		}
	}
	
	public static void main(String[] args) {
		TernarySearchTree TST = new TernarySearchTree();
		System.out.println(TST.contains("banane"));
		TST.add("banane");
		TST.add("bonobo");
		TST.add("");
//		TST.insert3("banane");
//		TST.insert3("bonobo");
		System.out.println(TST.contains("banane"));
		System.out.println(TST.contains("barnabas"));
		System.out.println(TST.contains("bonobo"));
		
	}
}

