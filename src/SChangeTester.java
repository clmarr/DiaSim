import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

//TODO figure out how to get the console to support IPA symbols 

public class SChangeTester {

	private final static char MARK_POS = '+', MARK_NEG = '-', MARK_UNSPEC = '.', FEAT_DELIM = ',';
	private final static char IMPLICATION_DELIM=':', PH_DELIM = ' '; 
	private final static char restrDelim = ',';
	private final static int POS_INT = 2, NEG_INT = 0, UNSPEC_INT = 1;
	private static String[] featsByIndex; 
	private static HashMap<String, Integer> featIndices;
	private static HashMap<String, String> phoneSymbToFeatsMap;
	private static HashMap<String, String> phoneFeatsToSymbMap; 
	private static HashMap<String, String[]> featImplications; 
	private static Set<String> featNames; 
	private static String featImplsLoc = "FeatImplications"; 

	
	public static void main(String args[])
	{
		System.out.println("Beginning test of SChange subclasses. Note that "
				+ "context classes are tested in their own class, SChangeContextTester.");
		System.out.println("Please note that in order to properly test the system, the default "
				+ "symbol definitions and feature implication definitions must be at the right"
				+ " file locations");
		System.out.println("Collecting symbol defs and feat indices... ");
		
		boolean boundsMatter = false; 
		
		featIndices = new HashMap<String, Integer>(); 
		phoneSymbToFeatsMap = new HashMap<String, String>(); 
		phoneFeatsToSymbMap = new HashMap<String, String>(); 	
		
		List<String> symbDefsLines = new ArrayList<String>();
		String nextLine; 
		
		try 
		{	File inFile = new File("symbolDefs.csv"); 
			BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream(inFile), "UTF8")); 
			while((nextLine = in.readLine()) != null)	
				symbDefsLines.add(nextLine); 		
			in.close(); 
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("Encoding unsupported!");
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO Exception!");
			e.printStackTrace();
		}
		
		//TODO debugging
		System.out.println("Symbol definitions extracted!");
		System.out.println("Length of symbDefsLines : "+symbDefsLines.size()); 
		
		//from the first line, extract the feature list and then the features for each symbol.
		featsByIndex = symbDefsLines.get(0).replace("SYMB,", "").split(""+FEAT_DELIM); 
		
		for(int fi = 0; fi < featsByIndex.length; fi++) 
			featIndices.put(featsByIndex[fi], fi);
		
		featNames = featIndices.keySet();

		//from the rest-- extract the symbol def each represents
		int li = 1; 
		while (li < symbDefsLines.size()) 
		{
			nextLine = symbDefsLines.get(li).replaceAll("\\s+", ""); //strip white space and invisible characters 
			int ind1stComma = nextLine.indexOf(FEAT_DELIM); 
			String symb = nextLine.substring(0, ind1stComma); 
			String[] featVals = nextLine.substring(ind1stComma+1).split(""+FEAT_DELIM); 		
			
			String intFeatVals = ""; 
			for(int fvi = 0; fvi < featVals.length; fvi++)
			{
				if(featVals[fvi].equals(""+MARK_POS))	intFeatVals+= POS_INT; 
				else if (featVals[fvi].equals(""+MARK_UNSPEC))	intFeatVals += UNSPEC_INT; 
				else if (featVals[fvi].equals(""+MARK_NEG))	intFeatVals += NEG_INT; 
				else	throw new Error("Error: unrecognized feature value, "+featVals[fvi]+" in line "+li);
			}
			
			phoneSymbToFeatsMap.put(symb, intFeatVals);
			phoneFeatsToSymbMap.put(intFeatVals, symb);
			li++; 
		}
		
		//TODO debugging
		System.out.println("Now extracting info from feature implications file...");
		
		List<String> featImplLines = new ArrayList<String>(); 
		featImplications = new HashMap<String, String[]>(); 
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream(featImplsLoc), "UTF-8")); 
			while((nextLine = in.readLine()) != null)	featImplLines.add(nextLine); 		
			in.close(); 
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("Encoding unsupported!");
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO Exception!");
			e.printStackTrace();
		}
		
		for(String filine : featImplLines)
		{
			String[] fisides = filine.split(""+IMPLICATION_DELIM); 
			featImplications.put(fisides[0], fisides[1].split(""+FEAT_DELIM));
		}
		
		//TODO debugging
		System.out.println("Done extracting feature implications!");
		
		System.out.println("Beginning test of SChangeFeat");
		
		SChangeFactory testFactory = new SChangeFactory(phoneSymbToFeatsMap, featIndices, featImplications); 
		
		SChangeFeat scfTest = new SChangeFeat(featIndices, "-voi", "+voi"); 
		scfTest.setPostContext(testFactory.parseNewContext("[+voi]", false));
		System.out.println("Now testing : "+scfTest);
		List<SequentialPhonic> testWord = new ArrayList<SequentialPhonic>(
				testFactory.parseSeqPhSeg("a"+PH_DELIM+"s"+PH_DELIM+"t"+PH_DELIM+"a"));
		testWord = scfTest.realize(testWord); 
		System.out.println("result should be a s d a  : "+ printWord(testWord));
		testWord = testFactory.parseSeqPhSeg("k a s l o");
		testWord = scfTest.realize(testWord); 
		System.out.println("result should be g a z l o : " +printWord(testWord));
		
		scfTest = new SChangeFeat(featIndices, "-nas", "+nas");
		scfTest.setPriorContext(testFactory.parseNewContext("+nas", false)); 
		System.out.println("Now testing : "+scfTest);
		testWord = scfTest.realize(testFactory.parseSeqPhSeg("n a b a n a")); 
		System.out.println("Result should be n ã b a n ã : "+printWord(testWord));
		
		scfTest = new SChangeFeat(new FeatMatrix("+syl,-cons", featIndices), new NullPhone());
		scfTest.setPriorContext(testFactory.parseNewContext("+son", false));
		System.out.println("Now testing : "+scfTest); 
		testWord = scfTest.realize(testFactory.parseSeqPhSeg("r e a l e a"));
		System.out.println("Result should be r a l a : "+printWord(testWord)); 
		
		
	}
	
	private static String printWord(List<SequentialPhonic> word)
	{
		String output = ""; 
		for (SequentialPhonic ph : word)
			output+=ph.print()+PH_DELIM; 
		return output.substring(0, output.lastIndexOf(PH_DELIM));
	}
}
