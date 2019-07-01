package io.bdrc.tools.outlines;
/*
 * This program converts a variety of file formats into Unicode Tibetan XML for inclusion into the TBRC Library.
 * Copyright (c) 2010 TBRC
 * Author: Chris Tomlinson
 *
  *
 * For more information, please contact TBRC, support@tbrc.org
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.bdrc.ewtsconverter.EwtsConverter;
//import org.tbrc.common.shared.Converter;

public class Convert2OutlineXML {
	private static String VERSION = "1.6.0";
	static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	
	static boolean debug = false;
	static boolean verbose = false;
	
	private StringBuffer sb = new StringBuffer();
	private PrintWriter out = null;
	
	private String oRid = "";
	private int nodeCounter = 1;
	
	private Convert2OutlineXML() {

	}
	
	EwtsConverter tibConverter = new EwtsConverter(true, true, true, false);
	
	private void setOutlineRid(String outlineRid) {
		oRid = outlineRid;
	}
	
	private void writeNodeRid() {
		String counter = String.format("%04d", nodeCounter);
		sb.append(" RID='");
		sb.append(oRid);
		sb.append("C2O");
		sb.append(counter);
		sb.append("'");
		nodeCounter++;
	}
	
	private void flush() {
		out.write(sb.toString());
		sb.delete(0, sb.length());
	}

	private void writeHeader(String oRid, String wRid, String type, String who, String title) {
		
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r");
		sb.append("<o:outline xmlns:o='http://www.tbrc.org/models/outline#'");
		sb.append(" RID='"); sb.append(oRid); sb.append("' status='provisional' pagination='absolute' webAccess='fullAccess'>\r");

		sb.append("  <o:name lang='tibetan' encoding='extendedWylie'>"); sb.append(title); sb.append("</o:name>\r");
		
		sb.append("  <o:isOutlineOf type='"); sb.append(type); 
					   sb.append("' work='"); sb.append(wRid); sb.append("'>"); 
					   sb.append(title); sb.append("</o:isOutlineOf>\r");
		
		sb.append("  <o:creator type='hasScribe'>"); sb.append(who); sb.append(" via TBRC Convert2OutlineXml version: "); sb.append(VERSION); sb.append("</o:creator>\r");
		flush();
	}

	private void writeCloseOutline(String who) {
		// close the last volume node
		sb.append("  </o:node>\r");
		
		// write audit log
		sb.append("  <o:log>\r");

		Date nowMillis = new Date();
		String now = dateFormatter.format(nowMillis);
		sb.append("    <entry when='"); sb.append(now); 
				sb.append("' who='"); sb.append(who); 
				sb.append("'>Convert2Outline version "); sb.append(VERSION); sb.append("</entry>\r");
		
		sb.append("  </o:log>\r");
		
		// close document
		sb.append("</o:outline>\r");
		
		flush();
		
		out.close();
	}
	
	private void writeOpenVolume(String volume) {
		sb.append("  <o:node type='volume'");
		writeNodeRid();
		sb.append(">\r");
		
		sb.append("    <o:name lang='tibetan' encoding='extendedWylie'>pod_");
		sb.append(volume);
		sb.append("</o:name>\r");
	}
	
	private void writeCloseVolume() {
		sb.append("  </o:node>\r");
		flush();
	}
	
	private String normalizeTitle(String title) {
		if (title != null) {
			title = title.replace(" zhes bya ba bzhugs so:", "");
			title = title.replace(" zhes bya ba bzhugs:", "");
			title = title.replace(" ces bya ba bzhugs so:", "");
			title = title.replace(" ces bya ba bzhugs:", "");
			title = title.replace(" bzhugs so:", "");
			title = title.replace(" bzhugs:", "");
			title = title.replace(" zhes bya ba bzhugs so/", "");
			title = title.replace(" zhes bya ba bzhugs/", "");
			title = title.replace(" ces bya ba bzhugs so/", "");
			title = title.replace(" ces bya ba bzhugs/", "");
			title = title.replace(" bzhugs so/", "");
			title = title.replace(" bzhugs/", "");
		}
		
		return title;
	}
	
	private void writeLocationDescription(String aStart, String iStart, String aEnd, String iEnd, String folio, String folioStart, String folioEnd) {
		if (folio.equals("book")) {
			sb.append("      <o:description type='location'>pp. ");
			sb.append(iStart);
			sb.append("-");
			sb.append(iEnd);
			sb.append("</o:description>\r");
		} else if (folio.equals("text")) {
			int s = 0;
			int e = 0;
			int ff = 0;
			try {
				s = Integer.parseInt(aStart);
				e = Integer.parseInt(aEnd);
				ff = (e - s + 2)/2;
			} catch (Exception ex) { }

			if (ff > 0) {
				sb.append("      <o:description type='location'>");
				sb.append(ff);
				sb.append(" ff. (pp. ");
				sb.append(iStart);
				sb.append("-");
				sb.append(iEnd);
				sb.append(")</o:description>\r");
			} else {
				sb.append("      <o:description type='location'>");
				sb.append("pp. ");
				sb.append(iStart);
				sb.append("-");
				sb.append(iEnd);
				sb.append("</o:description>\r");
			}
		} else if (folio.equals("cont")) {
			sb.append("      <o:description type='location'>ff. ");
			sb.append(folioStart);
			sb.append("-");
			sb.append(folioEnd);
			sb.append(" (pp. ");
			sb.append(iStart);
			sb.append("-");
			sb.append(iEnd);
			sb.append(")</o:description>\r");
		}
	}

	private void writeTextNode(String title, String volume, String aStart, String iStart, String aEnd, String iEnd, String folio, String folioStart, String folioEnd) 
	{
		sb.append("    <o:node type='text'");
		writeNodeRid();
		sb.append(">\r");
		
		sb.append("      <o:title lang='tibetan' encoding='extendedWylie' type='bibliographicalTitle'>");
		sb.append(normalizeTitle(title));
		sb.append("</o:title>\r");
		
		sb.append("      <o:location type='page' vol='");
		sb.append(volume);
		sb.append("' page='");
		sb.append(aStart);
		sb.append("'/>\r");
		
		sb.append("      <o:location type='page' vol='");
		sb.append(volume);
		sb.append("' page='");
		sb.append(aEnd);
		sb.append("'/>\r");
		
		writeLocationDescription(aStart, iStart, aEnd, iEnd, folio, folioStart, folioEnd);
		
		sb.append("    </o:node>\r");
	}
	
	private boolean fieldsCheck(String folio, int num) {
		if (folio.equals("cont") && (num == 10 || num == 9)) {
			return true;
		} else if (num == 7 || num == 8) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean tooFew(String folio, int num) {
		if (folio.equals("cont") && num < 9) {
			return true;
		} else if (num < 7) {
			return true;
		} else {
			return false;
		}
	}
	
	private int getOffset(String folio, int num) {
		if (folio.equals("cont") && num == 9) {
			return 0;
		} else if (folio.equals("cont") && num == 10) {
			return 1;
		} else if (num == 7) {
			return 0;
		} else {
			return 1;
		}
	}

	private void process(String inFileName, String outFileName, String type, String folio, String who, String workTitle) 
	throws Exception {
		
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(inFileName), "UTF-8"));
		out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFileName), "UTF-8")));
		
		// read in file and write out the Volume and Text nodes
		String line = in.readLine();
		int lineNum = 1;
		
		if (line != null) {
			// skip heading line in csv file
			line = in.readLine();
			
			boolean firstTime = true;
			String wRid = "";
			String volume = "";

			while (line != null) {
				String[] fields = line.split("[\\|]");
				
				int num = fields.length;

				if (! fieldsCheck(folio, num)) {
					System.err.println("Warning Outline CSV contains " + fields.length + " fields on line " + lineNum);
				}
				
				if (tooFew(folio, num)) {
					System.err.println("Outline CSV contains too few fields on line. Skipping.");

					line = in.readLine();
					lineNum++;
				} else {
					if (firstTime) {
						if (fields[0].isEmpty()) {
							System.err.println("Outline CSV does not contain work Rid on first line of data!!");
							System.exit(2);
						}

						wRid = fields[0].trim();

						writeHeader(oRid, wRid, type, who, workTitle);
					}

					if (! fields[1].isEmpty()) {
						volume = fields[1];

						if (! firstTime) {
							writeCloseVolume();
						}

						writeOpenVolume(volume);
					}

					// now write the Text node
					String title = fields[2];
					List<String> warnings = new ArrayList<String>();
					title = tibConverter.toWylie(title, warnings, false);
					if (verbose && !warnings.isEmpty()) {
						for (String warning : warnings) {
							System.err.println("toWylie: " + warning);
						}
					}
					
					int offset = getOffset(folio, num);
					
					// skip the biblio title field
					String aStart = fields[3+offset];
					String iStart = fields[4+offset];
					String aEnd = fields[5+offset];
					String iEnd = fields[6+offset];
					String fStart = (folio.equals("cont") ? fields[7+offset] : null);
					String fEnd = (folio.equals("cont") ? fields[8+offset] : null);

					writeTextNode(title, volume, aStart, iStart, aEnd, iEnd, folio, fStart, fEnd);

					firstTime = false;
					line = in.readLine();
					lineNum++;
				}
			}
		}

		writeCloseOutline(who);

		in.close();

		System.err.println(outFileName + " written");
	}
	
	/**
	 * converts a tab separated csv file to XML Outline.
	 * If outNm is null then it is computed based on context of collection, volume and index
	 * 
	 * @param inNm path of input csv file
	 * @param outNm if non-null path desired path of resulting XML file
	 * @param outBase if non-null the base directory for storing the XML file
	 * @throws Exception
	 */
	private static void convertFile(String docNm, String outDir, String type, String folio, String who, String workTitle) 
	throws Exception {
		File f = new File(docNm);
		if (!f.exists()) {
			System.err.println("Specified file " + docNm + " doesn't exist");
			System.exit(1);
		}
		
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
		in.readLine(); // skip header line
		String first = in.readLine();
		if (first == null) {
			System.err.println("Specified file " + docNm + " has TOO few lines.");
			System.exit(1);
		}
		
		String[] fields = first.split("[\\|]");
		
		while (fields.length == 0) {
			first = in.readLine();
			fields = first.split("[\\|]");
		}
		
		String wRid = fields[0].trim();
		
		String outlineRid = wRid.replaceFirst("W", "O");
		
		in.close();
		
		Convert2OutlineXML converter = new Convert2OutlineXML();
		
		converter.setOutlineRid(outlineRid);
		
		if (docNm.endsWith("-b.csv")) {
			folio = "book";
		} else if (docNm.endsWith("-t.csv")) {
			folio = "text";
		} else if (docNm.endsWith("-c.csv")) {
			folio = "cont";
		}
		
		String outNm = outDir + "/" + outlineRid + ".xml";
		
		if (verbose) {
			System.err.println("In File: " + docNm + "\rOut File: " + outNm);
		}
		
		converter.process(docNm, outNm, type, folio, who, workTitle);
	}
	
	private static void convertFiles(String docDirNm, String outDir, String type, String folio, String who, String workTitle) 
	throws Exception {
		File dir = new File(docDirNm);
		if (dir.isDirectory()) {
			class CsvFilter implements FilenameFilter {
				public boolean accept(File dir, String fileName) {
					return !fileName.startsWith(".") && fileName.endsWith("csv");
				}
			}

			FilenameFilter filter = new CsvFilter();
			String[] docs = dir.list(filter);
			Arrays.sort(docs);

			for (int i = 0; i < docs.length; i++) {
				String docNm = docDirNm + "/" + docs[i];
				System.err.println("CONVERTING " + docs[i]);
				convertFile(docNm, outDir, type, folio, who, workTitle);
			}
		} else {
			System.err.println("Specified directory " + docDirNm + " doesn't exist or isn't a directory");
			System.exit(1);
		}
	}
    
    private static void printHelp() {
        System.err.print("java -jar csv2outline.jar (-doc <pathname> | -docdir <pathname>) -outdir <pathname> -who <name> -title <work title>\r\n\r\n"
                + "-help - print this message and exits\r\n"
                + "-version - prints the version and exits\r\n"
                + "-doc <pathname> - path to dkar chag in vertical bar, '|', delimited csv\r\n"
                + "-docdir <pathname> - path to directory of dkar chag in tab delimited csv\r\n"
                + "-outdir <pathname> - path to base directory in which Outline XML will be written - no trailing slash '/'\r\n"
                + "-type <outline type> - outline type name. Defaults to 'subjectCollection'\r\n"
                + "-who <outline creator name> - name of the person who created the outline\r\n"
                + "-title <work title> - title of the work that the outline is for\r\n"
                + "-folio <book|text|cont> - form of folio information. Defaults to text\r\n"
                + "-verbose - prints basic processing information\r\n"
                + "-debug - prints diagnostic information useful in debugging format problems\r\n"
                + "-trace - prints each token\r\n"
                + "\r\nConvert2Outline2 version: " + VERSION + "\r\n"
                );
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String docNm = null;
			String docDirNm = null;
			String outDir = null;
			String who = "anon";
			String workTitle = "not specified";
			String type = "subjectCollection";
			String folio = "text";

			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				if (arg.equals("-outdir")) {
					outDir = (++i < args.length ? args[i] : null);
				} else if (arg.equals("-doc")) {
					docNm = (++i < args.length ? args[i] : null);
				} else if (arg.equals("-docdir")) {
					docDirNm = (++i < args.length ? args[i] : null);
				} else if (arg.equals("-type")) {
					type = (++i < args.length ? args[i] : null);
				} else if (arg.equals("-who")) {
					who = (++i < args.length ? args[i] : null);
				} else if (arg.equals("-title")) {
					workTitle = (++i < args.length ? args[i] : null);
				} else if (arg.equals("-folio")) {
					folio = (++i < args.length ? args[i] : null);
				} else if (arg.equals("-debug")) {
					debug = true;
				} else if (arg.equals("-verbose")) {
					verbose = true;
				} else if (arg.equals("-help")) {
					printHelp();
					System.exit(0);
				} else if (arg.equals("-version")) {
					System.err.println("Convert2Outline version: " + VERSION);
					System.exit(0);
				}
			}
			
			
			if (docDirNm != null) {
				
				convertFiles(docDirNm, outDir, type, folio, who, workTitle);
			
			} else if (docNm != null){
				
				convertFile(docNm, outDir, type, folio, who, workTitle);
			
			} else {
				System.err.println("Please provide a pathname for either an input -doc or directory -docDir.\r\n");
				printHelp();
				System.exit(1);
			}
			
			System.exit(0);
		} catch (Exception ex) {
			System.err.println(ex.getMessage());
			ex.printStackTrace();
			System.exit(1);
		}
	}
}
