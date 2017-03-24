package de.hu.wbi.indexing;
import jargs.gnu.CmdLineParser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/*
 * 
 	export JAVA_HOME="/vol/home-vol3/wbi/solt/etc/jdk1.6.0_20"
	export PATH="$JAVA_HOME/bin:$PATH"
	time java -cp /vol/home-vol3/wbi/thomas/workspace/iat/lib/lucene-core-2.9.1.jar:/vol/home-vol3/wbi/thomas/workspace/iat/lib/opencsv-2.1.jar:/vol/home-vol3/wbi/thomas/workspace/iat/bin de.hu.wbi.indexing.NXMLParser /local/colonet/colonet/bc3/xml/mixed/ | tee sections.txt
	time java -cp lib/lucene-core-2.9.3.jar:lib/opencsv-2.1.jar:lib/lingpipe-4.0.0.jar:bin de.hu.wbi.indexing.NXMLParser /tmp/bc3xml/mixed/ | tee sections.txt
 *  
 *  real    1833m41.829s
user    754m21.665s
sys     26m12.858s


Usage sample: 

String filename = "path/to/file.nxml"

// fetch single string values
String[] get_selectors = new String[]{"title", "year", "month", "day", "journal", "volume", "issue", "abstract"};
for (int i = 0; i = get_selectors.length; i++) {
    String result = NXMLParser.get(get_selectors[i], filename);
}

// fetch string list values
String[] list_selectors = new String[]{"sections", "authors"};
for (int i = 0; i = list_selectors.length; i++) {
    String[] result = NXMLParser.list(list_selectors[i], filename);
}

// _after_ fetching list of sections use indices of array to fetch section contents
String[] section_titles = NXMLParser.list("sections", filename);
String[] section_contents = new String[section_titles.length];
for (int i = 0; i = section_titles.length; i++) {
    section_contents[i] = NXMLParser.get("section", i, filename);
}

String[] caption_numbers = nxmlparser.list("captions", nxmlfile);
for(int i = 0; i < caption_numbers.length;i++) {
    System.out.println("Caption "+i);
    System.out.println(nxmlparser.get("caption",i+1,nxmlfile);
}

String issn_number = nxmlparser.get("issn", nxmlfile);


*/

public class NXMLParser {
	static Pattern p1 = Pattern.compile("\\s\\s+");
	static Pattern p2 = Pattern.compile("\\n");	//Remove line breaks 
	static String indexDir= "/local/colonet/colonet/bc3/index/";
	static String pm2PMC = "/local/colonet/colonet/bc3/PMC-ids.csv.gz";
	static String xmlDir = "/local/colonet/colonet/bc3/xml/mixed/";
	static boolean create = false;	//Shall we create a new Index?
	
	
	private static HashMap<Integer, Integer> pmcTopmid;  
    private static IndexWriter iw=null;
    static Map<String, String> xpaths = null;
     

    public static String get(String what, String pmcid) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        
    	// for pmid, title, year, month, day, journal, volume, issue, page, abstract, fulltext
        if (xpaths == null) {
            xpaths = new HashMap<String, String>();
            xpaths.put("title", "//front/article-meta/title-group/article-title");
            xpaths.put("year", "//front/article-meta/pub-date[@pub-type='epub']/year");
            xpaths.put("month", "//front/article-meta/pub-date[@pub-type='epub']/month"); 
            xpaths.put("day",  "//front/article-meta/pub-date[@pub-type='epub']/day");
            xpaths.put("journal", "//front/journal-meta/journal-title");
            xpaths.put("volume",  "//front/article-meta/volume");
            xpaths.put("issue", "//front/article-meta/issue");
            xpaths.put("abstract", "//front/article-meta/abstract");
            xpaths.put("issn", "//front/journal-meta/issn[@pub-type='ppub']");
        }

        if (what.matches("fulltext")) {
            String[] sections = list("sections", pmcid); 
            String fulltext = "";
            for(int i = 0; i < sections.length; i++) {
                fulltext = fulltext + sections[i] + get("section", i+1, pmcid);
            }
            return fulltext;
        }


        if (xpaths.get(what) == null) {
            return null;
        }
        String xpath_expr = (String) xpaths.get(what);
        NodeList nodes = getNodeList(pmcid, xpath_expr);
        String output = "";
        for (int i = 0; i < nodes.getLength(); i++) {
            output = output + nodes.item(i).getTextContent();
        }

        return output;

    }

    public static String get(String what, int which, String pmcid) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException{
        // for section && captions
        String res = "";
        if (what.equals("section")) {
            NodeList nodes = getNodeList(pmcid, "//body/sec["+which+"]");
            for (int i = 0; i < nodes.getLength(); i++) {
                NodeList children = nodes.item(i).getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                	if (j == 0 && children.item(0).getLocalName() == null  ){
                		continue;
                	}
                    if (j == 0 && children.item(0).getLocalName().equals("title")) {
                        continue;
                    }
                    res = res + " " + children.item(j).getTextContent();
                }
            }
        } else if (what.equals("caption")) {
            NodeList nodes = getNodeList(pmcid, "//caption");
            res = nodes.item(which-1).getTextContent();
        }

        return res;
    }


    public static String[] list(String what, String pmcid) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException{
        // for authors && sections && captions && citations
        String[] res = null;
        if (what.equals("authors")) {
            NodeList nodes = getNodeList(pmcid, "//front/article-meta/contrib-group/contrib[@contrib-type='author']/name"); 
            res = new String[nodes.getLength()];
            for (int i = 0; i < nodes.getLength(); i++) {
                Node name_node = nodes.item(i);
                String name = ""; 
                NodeList children = name_node.getChildNodes();
                for (int j = children.getLength() - 1; j >= 0; j--) {
                    name = name + " " + children.item(j).getTextContent();
                }
                res[i] = name;
            }
        } else if (what.equals("sections")) {
            NodeList nodes = getNodeList(pmcid, "//body/sec");
            res = new String[nodes.getLength()];
            for (int i = 0; i < nodes.getLength(); i++) {
                res[i] = nodes.item(i).getFirstChild().getTextContent();
            }
        } else if (what.equals("captions")) {
            NodeList nodes = getNodeList(pmcid, "//caption");
            res = new String[nodes.getLength()];
            for (int i = 0; i < nodes.getLength(); i++) {
                res[i] = ""+i;
            }
        }
        return res;
    }

    public static NodeList getNodeList(String pmcid, String xpath_expr) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {

        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true); 
        domFactory.setValidating(false); 
        domFactory.setAttribute("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        domFactory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        Document doc = builder.parse(pmcid);

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        XPathExpression expr = xpath.compile(xpath_expr);
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        return nodes;

    }

    public static void main(String[] args) throws Exception {
    	  
    	parseArgs(args);    	    
    	pmcTopmid = PMCMapping.readMapping(pm2PMC);
    	
        Directory directory = FSDirectory.open(new File(indexDir));
        iw = new IndexWriter(directory,  new StandardAnalyzer(Version.LUCENE_29), 
        		create, IndexWriter.MaxFieldLength.UNLIMITED);
        iw.setRAMBufferSizeMB(512);
        IndexSearcher is = new IndexSearcher(directory, true);
        Query query;
        
        File f = new File(xmlDir);
        if(f.isFile()){
			try {
				int pmc= getPMCFromFileName(f.getAbsolutePath());
				int pmid=0;
				if(pmcTopmid.get(pmc) != null)
					pmid=pmcTopmid.get(pmc);

				
				System.out.println("Indexing " +f.toString());
				System.out.println("PMC: "+pmc);
				System.out.println("PMID: " +pmid);
				
				
				indexFile(f.getAbsolutePath(), pmid);
			} catch (XPathExpressionException e1) {
				System.err.println("An error occured during parsing");
				e1.printStackTrace();
			} catch (ParserConfigurationException e1) {
				System.err.println("An error occured during parsing");
				e1.printStackTrace();
			} catch (SAXException e1) {
				System.err.println("An error occured during parsing");
				e1.printStackTrace();
			} catch (IOException e1) {
				System.err.println("An error occured during parsing");
				e1.printStackTrace();
			}
        }
		else {
			int indexed=0;
        	int fileNum=0;
        	
        	forloop:for(File file:f.listFiles()){
        		if(file.isFile() && file.getAbsolutePath().endsWith(".nxml")){			//No recoursive indexing at the moment; (e.g. Smallsamples folder?) TODO
        			        			
        			try{        				
        				//Check first if the pmc-article is already indexed
        				int pmc= getPMCFromFileName(file.getAbsolutePath());
        				query = new TermQuery(new Term(Fields.pmc, Integer.toString(pmc) ));
        				TopDocs  td = is.search(query, 10);
        				if(td.totalHits > 0){
        					System.err.println("Skipping pmc " +pmc +" found " +td.totalHits +" hits/" +file.getName());
        					continue forloop;
        				}
        				
						int pmid=0;
						if(pmcTopmid.get(pmc) != null)
							pmid=pmcTopmid.get(pmc);
				        	

	       				try {
	       					indexFile(file.getAbsolutePath(), pmid);
							indexed++;
						} catch (XPathExpressionException e) {
	        				System.err.println("An error occured during parsing");
							e.printStackTrace();
							continue forloop;
						} catch (ParserConfigurationException e) {
	        				System.err.println("An error occured during parsing");
							e.printStackTrace();
							continue forloop;
						} catch (SAXException e) {
	        				System.err.println("An error occured during parsing");
							e.printStackTrace();
							continue forloop;
						} catch (IOException e) {
	        				System.err.println("An error occured during parsing");
							e.printStackTrace();
							continue forloop;
						}
	        				        			
	        			if(fileNum++==1000){	 
	        				fileNum=0;
	        				iw.commit(); //Commit after all 500 documents, haha
	        			}
        			}catch(NullPointerException np){//Is thrown when we do not have an approriate PMID-ID..
        			//TODO check this when more time is available
//        				/local/for_colonet/colonet/IAT/fulltext/Mol_Syst_Biol-6--2858443.nxml
        			
//        				TODO warum wird das unten genannte Artikel in PMID 0 umgewandelt?
//        				/local/colonet/colonet/IAT/fulltext/Aging_(Albany_NY)-2-4-2880708.nxml
        			}
        			catch(ArrayIndexOutOfBoundsException ad){
        				//TODO check for /local/colonet/colonet/IAT/fulltext/Ann_Thorac_Med-3-4-2700453.nxml, why the Exception is thrown
        				//Seems to be a problem in getAbstract
//        				Same for /local/for_colonet/colonet/IAT/fulltext/J_Cell_Biol-184-2-2654307.nxml
        				
        			}
        		}
        	}
            System.out.println(indexed +" elements indexed");
       }
        
       iw.commit();
       iw.optimize();
       iw.close(); 
       
       if(!UniqueEntries.isUnique(indexDir))
    	   System.err.println(UniqueEntries.getDuplicates(indexDir).size() +" duplicates found");
       else
    	   System.err.println("No duplicates found");
       
//       UniqueEntries.removeDuplicates(Fields.pmid, indexDir);
    }
   
    
    /**
     * Method extracts the PMC-ID from the Filename /home/philippe/workspace/IAT/data/96689.nxml -> 96689
     * @param filename
     * @return
     */
    private static int getPMCFromFileName(String filename){
    	
    	
    	String pmc= filename.substring(filename.lastIndexOf(File.separatorChar)+1, filename.lastIndexOf(".nxml")); 
    	
    	//The original pmc-Articles were provided as 1234.nxml; the real ones are provided as Environ_Health_Perspect-117-5-2685850.nxml
    	if(pmc.contains("-"))
    		pmc = pmc.substring(pmc.lastIndexOf("-")+1);    	
    	
    	return Integer.parseInt(pmc);
    }
    
    /**
     * Indexes the nxml file
     * @param filename
     * @throws IOException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws XPathExpressionException 
     * @throws Exception
     */
    private static StringBuilder indexFile(String filename, int pmid) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException{

    	
    	System.out.println(filename);
        org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();          
    	String prop;
    	get("init", filename);
    	String title="";
    	String abstr="";

        for (Object thing : xpaths.keySet()) {
             prop = (String) thing;
             if (thing.equals("section")) continue;

             String out = get(prop, filename);
             if(prop.equals("abstract")){
            	 abstr=cleanString(out);
            	 doc.add(new Field(Fields.abstr, abstr, Field.Store.YES, Field.Index.ANALYZED));            	 
             }
             else if(prop.equals("title")){
            	 title=cleanString(out);
            	 doc.add(new Field(Fields.title, title, Field.Store.YES, Field.Index.ANALYZED));
             }
             
             else if(prop.equals("journal"))
             	doc.add(new Field(Fields.journal, out, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));             
             
             else if(prop.equals("issue"))
             	doc.add(new Field(Fields.issue, out, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
             else if(prop.equals("volume"))
             	doc.add(new Field(Fields.volume, out, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
             
             else if(prop.equals("year"))
             	doc.add(new Field(Fields.year, out, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS)); //Can be ""
             else if(prop.equals("month"))
             	doc.add(new Field(Fields.month, out, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
             else if(prop.equals("day"))
             	doc.add(new Field(Fields.day, out, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
             else if(prop.equals("issn"))
            	 doc.add(new Field(Fields.issn, out, Field.Store.YES, Field.Index.ANALYZED));
             else
             	System.err.println("Other " +prop +"/" +out);
         }
        
         String[] out = list("authors", filename);
         for(int i = 0; i < out.length; i++) {    
         	doc.add(new Field(Fields.authors, out[i], Field.Store.YES, Field.Index.ANALYZED));
         }

         StringBuilder sb = new StringBuilder(Fields.title +"\t\t" +title 
        		 							+"\n" +Fields.abstr +"\t\t" +abstr +"\n");
         
         out = list("sections", filename);
         for(int i = 0; i < out.length; i++) {
        	String sectionNotNorm = cleanString(out[i].trim());	//These strings might have malicient contents; Therefore clean
         	String sectionNorm= Fields.getMapping(sectionNotNorm.toUpperCase());         	
         	System.out.println(sectionNotNorm.toUpperCase() +"\t\t" +sectionNorm);
         	
         	String sectionContent = cleanString(get("section", i+1, filename));
         	sb.append(sectionNotNorm +"\t\t" +sectionContent +"\n");
         	
         	if(sectionNorm != null)
         		doc.add(new Field(sectionNorm, sectionContent, Field.Store.YES, Field.Index.ANALYZED));
         	else
         		doc.add(new Field(Fields.other, sectionContent, Field.Store.YES, Field.Index.ANALYZED));
         }
         
     	String[] caption_numbers = list("captions", filename);
    	for(int i = 0; i < caption_numbers.length;i++) {
    		String caption= cleanString(get("caption",i+1,filename));
    	    doc.add(new Field(Fields.caption, caption, Field.Store.YES, Field.Index.ANALYZED));
    	    sb.append(Fields.caption +"\t\t" +caption +"\n");
    	}
   	
    	if(sb.charAt(sb.length()-1) =='\n')
    		sb.deleteCharAt(sb.length()-1); //Removes the last caracter, which is always an \n
         
         if(sb.toString().startsWith("empty"))
        	 System.err.println("Error parsing PMC:" +getPMCFromFileName(filename));
         doc.add(new Field(Fields.full,sb.toString(),Field.Store.YES, Field.Index.ANALYZED ));
         doc.add(new Field(Fields.pmc, Integer.toString(getPMCFromFileName(filename)), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));   
         doc.add(new Field(Fields.pmid, Integer.toString(pmid), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
         
         iw.addDocument(doc);
         return sb;
    }
    
    public static String cleanString(String string){
    	
    	Matcher matcher = p2.matcher(string);
    	string = matcher.replaceAll(" ");
    	
    	matcher = p1.matcher(string);
    	return matcher.replaceAll(" ");
    	
    }

    
    private static void parseArgs(String args[]){
		CmdLineParser parser = new CmdLineParser();
		CmdLineParser.Option luceneOption  = parser.addStringOption('i', "index");
		CmdLineParser.Option pmc2pmidOption  = parser.addStringOption('p', "pmc2pmid");
		CmdLineParser.Option xmlOption  = parser.addStringOption('x', "xml");
		CmdLineParser.Option createOption  = parser.addBooleanOption('c', "create");
		
		CmdLineParser.Option helpOption = parser.addBooleanOption('h', "help");
		
		try {
            parser.parse(args);
        }
        catch (CmdLineParser.OptionException e ) {
            printUsage();
            System.exit(2);
        }
        Boolean helpValue = (Boolean)parser.getOptionValue(helpOption);
        if(helpValue != null && helpValue == true){
        	printUsage();
            System.exit(2);
        }
        
        helpValue = (Boolean)parser.getOptionValue(createOption);
        if(helpValue != null && helpValue == true)
        	create= true;
        
        	
        String tmp = (String) parser.getOptionValue(luceneOption);
        if(tmp != null)        
        	indexDir =    tmp;
        
        tmp = (String) parser.getOptionValue(pmc2pmidOption);
        if(tmp != null)
        	pm2PMC= tmp;
        
        tmp = (String) parser.getOptionValue(xmlOption);
        if(tmp != null)
        	xmlDir= tmp;

        System.err.println("Parsing: '" +xmlDir +"'\n" 
        				+"Into:  '" + indexDir +"'\n" 
        				+"PMC2PMID '" +pm2PMC +"'\n" 
        				+"Create '" +create +"'\n");
        
	}
	
	private static void printUsage() {
        System.err.println(
        		"Parses a folder of PMC-XML files; Stores them in lucene Dir and a seperate folder.." +
        		"Usage:\n" +
        		"Lucene Index Folder [-i,--index]\n" +
        		"PM2PMC [-p,pmc2pmid]\n"+
        		"XML [-x,--xml]");
	}
}


