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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import io.bdrc.ewtsconverter.EwtsConverter;

public class Convert2OutlineXML {
	private static String VERSION = "1.8.0";
	static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	
	static boolean debug = false;
    static boolean verbose = false;
    static public boolean extended = false;
	
	private static StringBuilder sb = new StringBuilder();
	private static PrintWriter out = null;
	
	private static String oRid = "";
	private static int nodeCounter = 1;
	
	static EwtsConverter tibConverter = new EwtsConverter(true, true, false, false);
	
	static HttpClient client = HttpClientBuilder.create().build();
	static Map<String, String> rids2names = new HashMap<>();
	
	private static String getName(String rid) {
		rid = rid.trim();
	    String name = rids2names.get(rid);
        
	    if (name == null) {
	        HttpGet get=new HttpGet("https://www.tbrc.org/public?module=any&query=name&args="+rid);
	        try {
	            HttpResponse resp = client.execute(get);
	            HttpEntity entity = resp.getEntity();
	            if (entity == null)
	            	System.err.println("error: cannot find name for "+rid);
	            name = EntityUtils.toString(entity).trim();
	            if (name.isEmpty()) {
	            	System.err.println("error: cannot find name for "+rid);
	            	name = "no-name";
	            }
	            rids2names.put(rid, name);
	        } catch (Exception ex) {
	        	System.err.println("error: cannot find name for "+rid);
	            return "no-name";
	        }
	    }
	    
        return name;
	}
	
	private static void setOutlineRid(String outlineRid) {
		oRid = outlineRid;
	}
	
	private static void writeNodeRid() {
		String counter = String.format("%04d", nodeCounter);
		sb.append(" RID='");
		sb.append(oRid);
		sb.append("C2O");
		sb.append(counter);
		sb.append("'");
		nodeCounter++;
	}
	
	private static void flush() {
		out.write(sb.toString());
		sb.delete(0, sb.length());
	}

	private static void writeHeader(String oRid, String wRid, String type, String who, String title) {
		
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r");
		sb.append("<o:outline xmlns:o='http://www.tbrc.org/models/outline#'");
		sb.append(" RID='"); sb.append(oRid); sb.append("' status='provisional' pagination='absolute'>\r");

		sb.append("  <o:name lang='tibetan' encoding='extendedWylie'>"); sb.append(title); sb.append("</o:name>\r");
		
		sb.append("  <o:isOutlineOf type='"); sb.append(type); 
					   sb.append("' work='"); sb.append(wRid); sb.append("'>"); 
					   sb.append(title); sb.append("</o:isOutlineOf>\r");
		
		sb.append("  <o:creator type='hasScribe'>"); sb.append(who); sb.append(" via TBRC Convert2OutlineXml version: "); sb.append(VERSION); sb.append("</o:creator>\r");
		flush();
	}

	private static void writeCloseOutline(String who) {
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
	
	private static void writeOpenVolume(String volume) {
		sb.append("  <o:node type='volume'");
		writeNodeRid();
		sb.append(">\r");
		
		sb.append("    <o:name lang='tibetan' encoding='extendedWylie'>pod_");
		sb.append(volume);
		sb.append("</o:name>\r");
	}
	
	private static void writeCloseVolume() {
		sb.append("  </o:node>\r");
		flush();
	}
	
	private static String normalizeTitle(String title) {
		if (title != null) {
			title = title.replace(" bzhugs so:", "");
			title = title.replace(" bzhugs:", "");
			title = title.replace(" bzhugs so/", "");
			title = title.replace(" bzhugs/", "");
		}
		return title;
	}
	
	static String[] noStrs = new String[] { };
	
	static final Map<Character, String> prefixToSubjectType = new HashMap<>();
	static {
		prefixToSubjectType.put('P', "isAboutPerson");
		prefixToSubjectType.put('C', "isAboutCorporation");
		prefixToSubjectType.put('G', "isAboutPlace");
		prefixToSubjectType.put('T', "isAboutUncontrolled");
		prefixToSubjectType.put('W', "isAboutText");
	}
	
	private static void writeExtended(String[] fields) {
	    int len = fields.length;
	    String[] authors = len > 7 ? fields[7].split(",") : noStrs;
	    String[] subjects = len > 8 ? fields[8].split(",") : noStrs;
	    String note = len > 9 ? fields[9] : "";
	    
	    for (String auth : authors) {
	        if (!auth.isEmpty()) {
	            sb.append("      <o:creator person='");
	            sb.append(auth.trim());
	            sb.append("'>"+getName(auth)+"</o:creator>\r");
	        }
	    }
	    
	    for (String subj : subjects) {
	        if (!subj.isEmpty()) {
	        	subj = subj.trim();
	        	String subjectType = prefixToSubjectType.getOrDefault(subj.charAt(0), "isAboutUncontrolled");
                sb.append("      <o:subject type='"+subjectType+"' class='");
                sb.append(subj);
                sb.append("'>"+getName(subj)+"</o:subject>\r");
	        }
	    }
	    
	    if (!note.isEmpty()) {
	        sb.append("      <o:note>");
	        sb.append(note.trim());
	        sb.append("</o:note>\r");
	    }
	}
	
	private static void writeLocationDescription(String aStart, String iStart, String aEnd, String iEnd, String folio, String folioStart, String folioEnd) {
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

	private static void writeTextNode(String title, String volume, String aStart, String iStart, String aEnd, String iEnd, String folio, String folioStart, String folioEnd, String[] fields) 
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
		
		if (extended) {
		    writeExtended(fields);
		}
		
		sb.append("    </o:node>\r");
	}
	
	private static boolean fieldsCheck(String folio, int num) {
	    if (extended && num >= 8 && num <= 10 && folio.equals("text")) {
	        return true;
	    } else if (folio.equals("cont") && (num == 10 || num == 9)) {
			return true;
		} else if (num == 7 || num == 8) {
			return true;
		} else {
			return false;
		}
	}
	
	private static boolean tooFew(String folio, int num) {
		if (extended && num < 7 && folio.equals("text")) {
            return true;
        } else if (folio.equals("cont") && num < 9) {
			return true;
		} else if (num < 7) {
			return true;
		} else {
			return false;
		}
	}
	
	private static int getOffset(String folio, int num) {
		if (extended && num <= 10 && folio.equals("text")) {
            return 0;
        } else if (folio.equals("cont") && num == 9) {
			return 0;
		} else if (folio.equals("cont") && num == 10) {
			return 1;
		} else if (num == 7) {
			return 0;
		} else {
			return 1;
		}
	}

	private static void process(InputStream inputStream, String type, String folio, String who) throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        reader = new CSVReaderBuilder(in)
                .withCSVParser(parser)
                .build();
        String[] line = reader.readNext();
        // ignoring first line
        line = reader.readNext();
        int lineNum = 1;
        boolean firstTime = true;
		String wRid = "";
		String volume = "";

		// check if there are several volumes by looking at all the lines
		List<String[]> lines = new ArrayList<>();
		
		boolean hasMultipleVolumes = false;
		String lastVol = null;
		while (line != null) {
			lines.add(line);
			if (! line[1].isEmpty()) {
				if (lastVol != null && !line[1].equals(lastVol)) {
					hasMultipleVolumes = true;
				}
				lastVol = line[1];
			}
			line = reader.readNext();
		}

		for (String[] siline : lines) {
			int num = siline.length;

			if (! fieldsCheck(folio, num)) {
				System.err.println("Warning Outline CSV contains " + siline.length + " fields on line " + lineNum);
			}
			
			if (tooFew(folio, num)) {
				System.err.println("Outline CSV contains too few fields on line. Skipping.");
				lineNum += 1;
				continue;
			} else {
				if (firstTime) {
					if (siline[0].isEmpty()) {
						System.err.println("Outline CSV does not contain work Rid on first line of data!!");
						System.exit(2);
					}

					wRid = siline[0].trim();
					String title = getName(wRid);
					writeHeader(oRid, wRid, type, who, title);
				}

				if (! siline[1].isEmpty() && !siline[1].equals(volume)) {
					volume = siline[1];
					if (hasMultipleVolumes) {
						if (! firstTime) {
							writeCloseVolume();
						}
						writeOpenVolume(volume);
					}
				}

				// now write the Text node
				String title = siline[2];
				List<String> warnings = new ArrayList<String>();
				title = tibConverter.toWylie(title, warnings, false);
				if (verbose && !warnings.isEmpty()) {
					for (String warning : warnings) {
						System.err.println("toWylie: " + warning);
					}
				}
				
				int offset = getOffset(folio, num);
				
				// skip the biblio title field
				String aStart = siline[3+offset];
				String iStart = siline[4+offset];
				String aEnd = siline[5+offset];
				String iEnd = siline[6+offset];
				String fStart = (folio.equals("cont") ? siline[7+offset] : null);
				String fEnd = (folio.equals("cont") ? siline[8+offset] : null);

				writeTextNode(title, volume, aStart, iStart, aEnd, iEnd, folio, fStart, fEnd, siline);

				firstTime = false;
				lineNum += 1;
			}
		}

		writeCloseOutline(who);
	}

	// for tests
	public static void process(InputStream inputStream, String oRid, OutputStream outArg, String type, String folio, String who) throws IOException {
		out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outArg, "UTF-8")));
		setOutlineRid(oRid);
		process(inputStream, type, folio, who);
	}
	
	private static void process(String inFileName, String outFileName, String type, String folio, String who) 
	throws Exception {
		
		InputStream in = new FileInputStream(inFileName);
		out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFileName), "UTF-8")));
		
		process(in, type, folio, who);
		
	}
	
	/**
	 * converts a '|' separated csv file to XML Outline.
	 * If outNm is null then it is computed based on context of collection, volume and index
	 * 
	 * @param fileName path of input csv file
	 * @param outDir if non-null path desired path of resulting XML file
	 * @param outBase if non-null the base directory for storing the XML file
	 * @throws Exception
	 */
	private static void convertFile(String fileName, String outDir, String type, String folio, String who) 
	throws Exception {
		File f = new File(fileName);
		if (!f.exists()) {
			System.err.println("Specified file " + fileName + " doesn't exist");
			System.exit(1);
		}
		
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
		in.readLine(); // skip header line
		String first = in.readLine();
		if (first == null) {
			System.err.println("Specified file " + fileName + " has TOO few lines.");
			System.exit(1);
		}
		
		CSVParser parser = new CSVParser();
		String[] fields = parser.parseLine(first);
		
		while (fields.length == 0) {
			first = in.readLine();
			fields = parser.parseLine(first);
		}
		
		String wRid = fields[0].trim();
		
		String outlineRid = wRid.replaceFirst("W", "O");
		
		in.close();
		
		setOutlineRid(outlineRid);
		
		if (fileName.endsWith("-b.csv")) {
			folio = "book";
		} else if (fileName.endsWith("-t.csv")) {
			folio = "text";
		} else if (fileName.endsWith("-c.csv")) {
			folio = "cont";
		}
		
		String outNm = outDir + "/" + outlineRid + ".xml";
		
		if (verbose) {
			System.err.println("In File: " + fileName + "\rOut File: " + outNm);
		}

		process(fileName, outNm, type, folio, who);
	}
	
	private static void convertFiles(String docDirNm, String outDir, String type, String folio, String who) 
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
				convertFile(docNm, outDir, type, folio, who);
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
                + "-folio <book|text|cont> - form of folio information. Defaults to text\r\n"
                + "-extended - indicates an extended csv with up to 4 additional columns\r\n"
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
				} else if (arg.equals("-folio")) {
					folio = (++i < args.length ? args[i] : null);
				} else if (arg.equals("-debug")) {
					debug = true;
				} else if (arg.equals("-extended")) {
                    extended = true;
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
				
				convertFiles(docDirNm, outDir, type, folio, who);
			
			} else if (docNm != null){
				
				convertFile(docNm, outDir, type, folio, who);
			
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
