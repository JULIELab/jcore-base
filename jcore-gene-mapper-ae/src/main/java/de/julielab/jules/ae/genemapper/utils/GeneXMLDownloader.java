package de.julielab.jules.ae.genemapper.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The gene XML format contains the following elements
 * <ul>
 * <li>Entrezgene-Set</li>
 * <li>Entrezgene</li>
 * <li>Entrezgene_track-info</li>
 * <li>Gene-track</li>
 * <li>Gene-track_geneid</li>
 * <li>Gene-track_create-date</li>
 * <li>Date</li>
 * <li>Date_std</li>
 * <li>Date-std</li>
 * <li>Date-std_year</li>
 * <li>Date-std_month</li>
 * <li>Date-std_day</li>
 * <li>Date-std_hour</li>
 * <li>Date-std_minute</li>
 * <li>Date-std_second</li>
 * <li>Gene-track_update-date</li>
 * <li>Date</li>
 * <li>Date_std</li>
 * <li>Date-std</li>
 * <li>Date-std_year</li>
 * <li>Date-std_month</li>
 * <li>Date-std_day</li>
 * <li>Date-std_hour</li>
 * <li>Date-std_minute</li>
 * <li>Date-std_second</li>
 * <li>Entrezgene_type</li>
 * <li>Entrezgene_source</li>
 * <li>BioSource</li>
 * <li>BioSource_genome</li>
 * <li>BioSource_org</li>
 * <li>Org-ref</li>
 * <li>Org-ref_taxname</li>
 * <li>Org-ref_db</li>
 * <li>Dbtag</li>
 * <li>Dbtag_db</li>
 * <li>Dbtag_tag</li>
 * <li>Object-id</li>
 * <li>Object-id_id</li>
 * <li>Org-ref_orgname</li>
 * <li>OrgName</li>
 * <li>OrgName_name</li>
 * <li>OrgName_name_binomial</li>
 * <li>BinomialOrgName</li>
 * <li>BinomialOrgName_genus</li>
 * <li>BinomialOrgName_species</li>
 * <li>OrgName_lineage</li>
 * <li>OrgName_gcode</li>
 * <li>OrgName_mgcode</li>
 * <li>OrgName_div</li>
 * <li>Entrezgene_gene</li>
 * <li>Gene-ref</li>
 * <li>Gene-ref_locus</li>
 * <li>Gene-ref_locus-tag</li>
 * <li>Entrezgene_rna</li>
 * <li>RNA-ref</li>
 * <li>RNA-ref_type</li>
 * <li>RNA-ref_ext</li>
 * <li>RNA-ref_ext_tRNA</li>
 * <li>Trna-ext</li>
 * <li>Trna-ext_aa</li>
 * <li>Trna-ext_aa_ncbieaa</li>
 * <li>Entrezgene_gene-source</li>
 * <li>Gene-source</li>
 * <li>Gene-source_src</li>
 * <li>Gene-source_src-int</li>
 * <li>Gene-source_src-str1</li>
 * <li>Gene-source_src-str2</li>
 * <li>Entrezgene_locus</li>
 * <li>Gene-commentary</li>
 * <li>Gene-commentary_type</li>
 * <li>Gene-commentary_accession</li>
 * <li>Gene-commentary_version</li>
 * <li>Gene-commentary_seqs</li>
 * <li>Seq-loc</li>
 * <li>Seq-loc_int</li>
 * <li>Seq-interval</li>
 * <li>Seq-interval_from</li>
 * <li>Seq-interval_to</li>
 * <li>Seq-interval_strand</li>
 * <li>Na-strand</li>
 * <li>Seq-interval_id</li>
 * <li>Seq-id</li>
 * <li>Seq-id_gi</li>
 * <li>Gene-commentary_products</li>
 * <li>Gene-commentary</li>
 * <li>Gene-commentary_type</li>
 * <li>Gene-commentary_label</li>
 * <li>Gene-commentary_genomic-coords</li>
 * <li>Seq-loc</li>
 * <li>Seq-loc_mix</li>
 * <li>Seq-loc-mix</li>
 * <li>Seq-loc</li>
 * <li>Seq-loc_int</li>
 * <li>Seq-interval</li>
 * <li>Seq-interval_from</li>
 * <li>Seq-interval_to</li>
 * <li>Seq-interval_strand</li>
 * <li>Na-strand</li>
 * <li>Seq-interval_id</li>
 * <li>Seq-id</li>
 * <li>Seq-id_gi</li>
 * <li>Seq-loc</li>
 * <li>Seq-loc_int</li>
 * <li>Seq-interval</li>
 * <li>Seq-interval_from</li>
 * <li>Seq-interval_to</li>
 * <li>Seq-interval_strand</li>
 * <li>Na-strand</li>
 * <li>Seq-interval_id</li>
 * <li>Seq-id</li>
 * <li>Seq-id_gi</li>
 * <li>Gene-commentary_comment</li>
 * <li>Gene-commentary</li>
 * <li>Gene-commentary_type</li>
 * <li>Gene-commentary_text</li>
 * <li>Entrezgene_comments</li>
 * <li>Gene-commentary</li>
 * <li>Gene-commentary_type</li>
 * <li>Gene-commentary_heading</li>
 * <li>Gene-commentary_text</li>
 * <li>Entrezgene_unique-keys</li>
 * <li>Entrezgene_xtra-iq</li>
 * <li>Entrezgene_non-unique-keys</li>
 * <li>Dbtag</li>
 * <li>Dbtag_db</li>
 * <li>Dbtag_tag</li>
 * <li>Object-id</li>
 * <li>Object-id_id</li>
 * </ul>
 * 
 * @author faessler
 *
 */
public class GeneXMLDownloader {

	private static final Logger log = LoggerFactory.getLogger(GeneXMLDownloader.class);

	public static String EUTILS = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/";

	public static final File FILE_SUMMARIES = new File("eg2summary");
	public static final File FILE_PROT = new File("gene2entrezgene_prot");

	public static void main(String[] args) throws IOException, XMLStreamException {
		if (args.length < 1) {
			System.err.println("Usage: " + GeneXMLDownloader.class.getSimpleName()
					+ " <taxonomy ids file> [XML directory] [gene_info file]");
			System.err.println(
					"If XML directory is given but empty, XML files with the downloaded batches will be stored there for later use. If the files already exist, no download will happen but the existing files will be read.");
			System.err.println(
					"If the gene_info file is given, its creation date will be compared to the oldest XML file in the XML directory, if not empty. If the gene_info file is newer than the existing XML files, the files will be refreshed by downloading them.");
			System.exit(0);
		}

		String taxIdFile = args[0];
		File xmlDirectory = args.length > 1 && !StringUtils.isBlank(args[1]) ? new File(args[1]) : null;
		File gene_info = args.length > 2 && !StringUtils.isBlank(args[2]) ? new File(args[2]) : null;

		log.info("Taxonomy ID file: {}", taxIdFile);
		log.info("XML directory to store/read XML from to/from: {}", xmlDirectory);
		log.info("gene_info file to compare timestamp to: {}", gene_info);
		List<String> taxIds = FileUtils.readLines(new File(taxIdFile), "UTF-8");
		for (String taxId : taxIds) {
			// for (int i = 0; i < taxIds.size(); ++i)
			// taxIds.set(i, taxIds.get(i) + "[taxid]");

			boolean geneInfoNewerThanXmlFiles = isGeneInfoNewerThanXmlFiles(xmlDirectory, taxId, gene_info);
			File[] xmlFilesInDirectory = getXmlFilesInDirectoryForTaxId(xmlDirectory, taxId);
			log.info("Found {} XML files for organism with taxonomy ID {} in directory {}",
					new Object[] { xmlFilesInDirectory.length, taxId, xmlDirectory });
			if (xmlFilesInDirectory.length == 0 || geneInfoNewerThanXmlFiles) {
				boolean storeXml = (null != xmlDirectory && xmlFilesInDirectory.length == 0)
						|| geneInfoNewerThanXmlFiles;
				if (geneInfoNewerThanXmlFiles)
					clearXmlFilesForTaxId(xmlDirectory, taxId);
				if (!xmlDirectory.exists()) {
					log.info("XML directory {} does not exist and is created.", xmlDirectory);
					xmlDirectory.mkdirs();
				}
				// this request will only give a query key to get the actual
				// results
				// batch wise
				URL downloadHandleUrl = new URL(EUTILS + "esearch.fcgi?db=gene&retmax=1&usehistory=y&term=" + taxId
						+ "[taxid]+AND+alive[properties]");
				log.trace("Request for download handle: {}", downloadHandleUrl);
				InputStream responseStream = downloadHandleUrl.openConnection().getInputStream();
				log.info("Contacting E-Utils for download of XML gene information for taxonomy ID {}...", taxId);
				DownloadHandle downloadHandle = readDownloadHandleXml(responseStream);

				log.info("Got a download handle for a search result of {} entries", downloadHandle.count);

				if (downloadHandle.count == 0) {
					log.info(
							"Did not receive any entries for taxonomy ID {}. This could point to an error or just no available entries. This taxonomy ID is skipped. The request URL was {}",
							taxId, downloadHandleUrl);
					continue;
				}

				log.info("Downloading Gene XML data to {}. This will take a few hours.", xmlDirectory);

				try (OutputStream osSummaries = new FileOutputStream(FILE_SUMMARIES);
						OutputStream osProtnames = new FileOutputStream(FILE_PROT)) {
					// Now download the gene XML batch-wise. The batches must
					// not be
					// set
					// too
					// large because then timeouts could occur.
					int retmax = 500;
					for (int retstart = 0; retstart < downloadHandle.count; retstart += retmax) {
						log.debug("Downloading gene XML records for taxonomy ID {}: {}", taxId,
								retstart + " - " + Math.min(retstart + retmax - 1, downloadHandle.count));
						String format = String.format(
								EUTILS + "efetch.fcgi?rettype=xml&retmode=text&retstart=%s&retmax=%s&"
										+ "db=gene&query_key=%s&WebEnv=%s",
								retstart, retmax, downloadHandle.queryKey, downloadHandle.webEnv);
						log.trace("Request URL: {}", format);
						URL batchUrl = new URL(format);

						log.debug(
								"Reading stream response and parsing the respective XML (this is the download step and will take a while)");
						InputStream is = batchUrl.openStream();
						if (storeXml) {
							String xml = IOUtils.toString(is, "UTF-8");
							try (OutputStream os = new GZIPOutputStream(
									new FileOutputStream(new File(xmlDirectory.getAbsolutePath() + File.separator
											+ "genes-taxid" + taxId + "-" + retstart + "-"
											+ Math.min(retstart + retmax - 1, downloadHandle.count) + ".xml.gz")))) {
								IOUtils.copy(new StringReader(xml), os, "UTF-8");
							}
							is = new ReaderInputStream(new StringReader(xml), "UTF-8");
						}
						extractAndWriteGeneInfoToFile(osSummaries, osProtnames, is);
					}
				}
			} else {
				log.info("Reading existing gene XML data from {}", xmlDirectory);
				try (OutputStream osSummaries = new FileOutputStream(FILE_SUMMARIES);
						OutputStream osProtnames = new FileOutputStream(FILE_PROT)) {
//					int i = 0;
//					if (!log.isTraceEnabled())
//						System.out.print("Processed: ");

					Stream.of(xmlFilesInDirectory).parallel().map(xmlFile -> {
						try {
							InputStream is = new GZIPInputStream(new FileInputStream(xmlFile));
							log.trace("Reading XML file {}", xmlFile);
							return extractGeneInfoFromXml(is);
						} catch (IOException | XMLStreamException e) {
							e.printStackTrace();
						}
						return null;
					}).forEach(extractList -> {
						try {
							writeGeneInfoToFile(extractList, osSummaries, osProtnames);
						} catch (XMLStreamException | IOException e) {
							e.printStackTrace();
						}
					});

//					for (File xmlFile : xmlFilesInDirectory) {
//						log.trace("Reading XML file {}", xmlFile);
//						try (InputStream is = new GZIPInputStream(new FileInputStream(xmlFile))) {
//							extractAndWriteGeneInfoToFile(osSummaries, osProtnames, is);
//						}
//						if (!log.isTraceEnabled()) {
//							// If we already printed something, set the cursor
//							// back to erase the current number
//							if (i > 0) {
//								int length = String.valueOf(i).length();
//								for (int j = 0; j < length; j++)
//									System.out.print("\b");
//							}
//							System.out.print(++i);
//						}
//					}
				}
			}
			log.info("Done extracting gene data from XML and writing result files for taxonomy ID {}.", taxId);
		}
	}

	private static void writeGeneInfoToFile(List<GeneXmlExtract> geneExtractList, OutputStream osSummaries,
			OutputStream osProtnames) throws XMLStreamException, IOException {
		log.trace("Writing gene summaries of current XML batch to file {}", FILE_SUMMARIES);
		for (GeneXmlExtract extract : geneExtractList) {
			IOUtils.write(extract.geneId + "\t" + extract.summary + "\n", osSummaries, "UTF-8");
		}

		log.trace("Writing entrezgene_prot names of current XML batch to file {}", FILE_PROT);
		for (GeneXmlExtract extract : geneExtractList) {
			if (extract.entrezgeneProt != null) {
				if (extract.entrezgeneProt.protrefName != null) {
					for (String protName : extract.entrezgeneProt.protrefName) {
						IOUtils.write(extract.geneId + "\t" + protName + "\n", osProtnames, "UTF-8");
					}
				}
				if (null != extract.entrezgeneProt.protrefDesc)
					IOUtils.write(extract.geneId + "\t" + extract.entrezgeneProt.protrefDesc + "\n", osProtnames,
							"UTF-8");
			}
		}
	}
	
	private static List<GeneXmlExtract> extractAndWriteGeneInfoToFile(OutputStream osSummaries,
			OutputStream osProtnames, InputStream is) throws XMLStreamException, IOException {
		List<GeneXmlExtract> geneExtractList = extractGeneInfoFromXml(is);
		writeGeneInfoToFile(geneExtractList, osSummaries, osProtnames);
		return geneExtractList;
	}

	private static void clearXmlFilesForTaxId(File xmlDirectory, String taxId) {
		File[] xmlFilesInDirectory = getXmlFilesInDirectoryForTaxId(xmlDirectory, taxId);
		log.debug("Deleting {} XML files in directory {}", xmlFilesInDirectory.length, xmlDirectory);
		for (File xmlFile : xmlFilesInDirectory)
			xmlFile.delete();
	}

	private static boolean isGeneInfoNewerThanXmlFiles(File xmlDirectory, String taxId, File gene_info) {
		if (xmlDirectory == null || !xmlDirectory.exists() || gene_info == null)
			return false;
		File[] xmlFiles = getXmlFilesInDirectoryForTaxId(xmlDirectory, taxId);
		long latestXmlModification = Long.MAX_VALUE;
		for (File xmlFile : xmlFiles) {
			if (xmlFile.lastModified() < latestXmlModification)
				latestXmlModification = xmlFile.lastModified();
		}
		boolean geneInfoIsNewer = gene_info.lastModified() > latestXmlModification;
		log.debug("gene_info file at {} is {} than the oldest XML file for taxonomy ID {} in {}",
				new Object[] { gene_info, geneInfoIsNewer ? "newer" : "older", taxId, xmlDirectory });
		return geneInfoIsNewer;
	}

	private static File[] getXmlFilesInDirectoryForTaxId(File xmlDirectory, final String taxId) {
		if (!xmlDirectory.exists())
			return new File[0];
		File[] xmlFiles = xmlDirectory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().contains("taxid" + taxId) && name.toLowerCase().endsWith("xml.gz");
			}
		});
		if (xmlFiles != null && xmlFiles.length > 0) {
			Arrays.sort(xmlFiles, new Comparator<File>() {

				@Override
				public int compare(File o1, File o2) {
					// file names have the form
					// genes-taxidXXXX-from-to.xml.gz,
					// e.g.
					// genes-taxid9606-20000-20499.xml.gz
					Integer start1 = Integer.parseInt(o1.getName().split("-")[2]);
					Integer start2 = Integer.parseInt(o2.getName().split("-")[2]);
					return start1.compareTo(start2);
				}
			});
		}
		return xmlFiles == null ? new File[0] : xmlFiles;
	}

	private static List<GeneXmlExtract> extractGeneInfoFromXml(InputStream openStream) throws XMLStreamException, IOException {
		List<GeneXmlExtract> geneExtractList = new ArrayList<>();

		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader parser = factory.createXMLStreamReader(openStream);
		String currentTag = null;
		GeneXmlExtract currentXmlExtract = null;
		while (parser.hasNext()) {

			switch (parser.getEventType()) {

			case XMLStreamConstants.START_ELEMENT:
				currentTag = parser.getLocalName();
				switch (currentTag) {
				case "Entrezgene":
					if (currentXmlExtract != null)
						geneExtractList.add(currentXmlExtract);
					currentXmlExtract = new GeneXmlExtract();
					break;
				case "Entrezgene_prot":
					currentXmlExtract.entrezgeneProt = readEntrezgeneProtFromXml(parser);
					break;
				}
				break;
			case XMLStreamConstants.CHARACTERS:
				switch (currentTag) {
				case "Gene-track_geneid":
					currentXmlExtract.geneId = parser.getText();
					break;
				case "Entrezgene_summary":
					currentXmlExtract.summary = parser.getText();
					break;
				}
			default:
				break;
			}
			parser.next();
		}
		openStream.close();
		return geneExtractList;
	}

	private static EntrezgeneProt readEntrezgeneProtFromXml(XMLStreamReader parser) throws XMLStreamException {
		EntrezgeneProt prot = new EntrezgeneProt();

		String currentTag = parser.getLocalName();
		if (!currentTag.equals("Entrezgene_prot"))
			throw new IllegalStateException(
					"Expected the tag Entrezgene_prot to begin reading protein names but got " + currentTag);
		do {
			parser.next();
			switch (parser.getEventType()) {
			case XMLStreamConstants.START_ELEMENT:
				currentTag = parser.getLocalName();
				break;
			case XMLStreamConstants.END_ELEMENT:
				currentTag = parser.getLocalName();
				break;
			case XMLStreamConstants.CHARACTERS:
				switch (currentTag) {
				case "Prot-ref_name_E":
					prot.addProtrefName(parser.getText());
					break;
				case "Prot-ref_desc":
					prot.protrefDesc = parser.getText();
					break;
				}
				break;
			}
		} while (parser.getEventType() != XMLStreamConstants.END_ELEMENT || !currentTag.equals("Entrezgene_prot"));
		return prot;
	}

	private static DownloadHandle readDownloadHandleXml(InputStream downloadHandleResponse) throws XMLStreamException {
		DownloadHandle downloadHandle = new DownloadHandle();

		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader parser = factory.createXMLStreamReader(downloadHandleResponse);
		final String countTag = "Count";
		final String queryKeyTag = "QueryKey";
		final String webEnvTag = "WebEnv";
		String currentTag = null;
		try {
			while (parser.hasNext()) {

				switch (parser.getEventType()) {

				case XMLStreamConstants.START_ELEMENT:
					currentTag = parser.getLocalName();
					break;
				case XMLStreamConstants.CHARACTERS:
					switch (currentTag) {
					case countTag:
						// we are only interested in the first occurrence of a
						// count
						// element
						if (downloadHandle.count == 0)
							downloadHandle.count = Integer.parseInt(parser.getText());
						break;
					case queryKeyTag:
						downloadHandle.queryKey = parser.getText();
						break;
					case webEnvTag:
						downloadHandle.webEnv = parser.getText();
						break;
					}
					currentTag = null;
					break;

				default:
					break;
				}
				parser.next();
			}
		} catch (XMLStreamException e) {
			log.error("Caught error while trying to parse download handle info", e);
		}
		return downloadHandle;
	}

	private static class GeneXmlExtract {
		String geneId;
		String summary;
		EntrezgeneProt entrezgeneProt;

		@Override
		public String toString() {
			return "GeneXmlExtract [geneId=" + geneId + ", summary=" + summary + ", entrezgeneProt=" + entrezgeneProt
					+ "]";
		}

	}

	/**
	 * Represents the Entrezgene_prot element of the gene XML format.
	 * 
	 * @author faessler
	 *
	 */
	private static class EntrezgeneProt {
		List<String> protrefName;
		String protrefDesc;

		public void addProtrefName(String name) {
			if (null == protrefName)
				protrefName = new ArrayList<>();
			protrefName.add(name);
		}

		@Override
		public String toString() {
			return "EntrezgeneProt [protrefName=" + protrefName + ", protrefDesc=" + protrefDesc + "]";
		}

	}

	private static class DownloadHandle {
		int count;
		String queryKey;
		String webEnv;

		@Override
		public String toString() {
			return "DownloadHandle [count=" + count + ", queryKey=" + queryKey + ", webEnv=" + webEnv + "]";
		}
	}
}
