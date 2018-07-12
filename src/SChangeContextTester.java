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

public class SChangeContextTester {

	//excludes all material dealing with feature implications 

	private final static char MARK_POS = '+', MARK_NEG = '-', MARK_UNSPEC = '.', FEAT_DELIM = ',';
	private final static char IMPLICATION_DELIM=':', PH_DELIM = ' '; 
	private final static char restrDelim = ',';
	private final static int POS_INT = 2, NEG_INT = 0, UNSPEC_INT = 1;
	private static String[] featsByIndex; 
	private static HashMap<String, Integer> featIndices;
	private static HashMap<String, String> phoneSymbToFeatsMap;
	private static HashMap<String, String> phoneFeatsToSymbMap; //TODO abrogate either this or the previous class variable
	private static HashMap<String, String[]> featImplications; 
	private static Set<String> featNames; 
	private static String featImplsLoc = "FeatImplications"; 
	private static Phone dummiePhone; 
	public static void main(String args[])
	{
		featIndices = new HashMap<String, Integer>(); 
		phoneSymbToFeatsMap = new HashMap<String, String>(); 
		phoneFeatsToSymbMap = new HashMap<String, String>(); 
		
		String dummyPhoneSymb = ""; 
		
		System.out.println("Collecting symbol definitions...");
		
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
		
		String firstline = symbDefsLines.get(1).replaceAll("\\s+", "");
		dummiePhone = new Phone (phoneSymbToFeatsMap.get(firstline.substring(0,firstline.indexOf(FEAT_DELIM))), 
				featIndices, phoneSymbToFeatsMap);
		
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
		
		System.out.println("Beginning test of context functions");
		
		String W = "[-"+featsByIndex[0]+"]", X = "[+"+featsByIndex[0]+"]";
		String Y = "[-"+featsByIndex[1]+"]", Z = "[+"+featsByIndex[1]+"]";
		
		String testString0 = W+"( "+X+" ("+Y+")( "+Z+")+("+W+" "+Y+")*)";
		System.out.println("Testing the method forceParenCloseConsistency()"); 
		System.out.println("The following should be : ");
		System.out.println(W+" ( "+X+" ( "+Y+" ) ( "+Z+" )+ ( "+W+" "+Y+" )* )");
		
		SChangeFactory testFactory = new SChangeFactory(phoneSymbToFeatsMap, featIndices, featImplications); 
		System.out.println("And it is ...\n"+testFactory.forceParenSpaceConsistency(testString0));
		
		SChangeContext testInstance0 = testFactory.parseNewContext(
				testFactory.forceParenSpaceConsistency(testString0), false); 
		System.out.println("Testing parseNewContext and associated methods");
		System.out.println("The following should be 1: "+testInstance0.generateMinSize()); 
				
		System.out.println("The following should be : ");
		System.out.println(W+" "+X+" "+Y+" "+Z+" "+W+" "+Y+" ");
		List<RestrictPhone> prstrs = testInstance0.getPlaceRestrs(); 
		for (RestrictPhone prstr : prstrs)
			System.out.print(prstr+" ");
		
		System.out.println("The following should be ...");
		System.out.println("i0 (:13,2 i1 (:5,1 i2 ):3,1 +(:8,1 i3 )+:6,1 *(:12,3 i4 i5 )*:9,3 ):1,2");
		
		String[] parenMap = testInstance0.getParenMap();
		for(String pl : parenMap)	System.out.print(pl+" ");
		System.out.println("");

		System.out.println("Testing isPriorMatch and isPosteriorMatch");
		
		//TODO: A is ++, B +-, C -+ and D --
		List<SequentialPhonic> dummyTestList = new ArrayList<SequentialPhonic>(); 
		Phone A = new Phone(dummiePhone), B = new Phone(dummiePhone), C = new Phone(dummiePhone), D = new Phone(dummiePhone); 
		String ft1 = featsByIndex[0], ft2 = featsByIndex[1];
		A.set(ft1,2); B.set(ft1, 2); C.set(ft1, 0); D.set(ft1, 0);
		A.set(ft2, 2); B.set(ft2, 0); C.set(ft2,2); D.set(ft2,0);
		
		SChangeContext testContext = testFactory.parseNewContext("A", false);
		dummyTestList.add(A); dummyTestList.add(C); 
		
		System.out.println("Testing A __ as prior context");
		System.out.println("A __ should be TRUE: " + testContext.isPriorMatch(dummyTestList,1));
		dummyTestList.set(0, B);
		System.out.println("B __ should be FALSE: " + testContext.isPriorMatch(dummyTestList, 1)); 
		dummyTestList = new ArrayList<SequentialPhonic>(); dummyTestList.add(C); dummyTestList.add(A); 
		System.out.println("Testing __ B as prior context");
		System.out.println(" __ A should be TRUE : "+ testContext.isPosteriorMatch(dummyTestList, 1)); 
		
		
		//TODO below is abrogated
		//TODO finish this 
		/**
		System.out.println("Testing this context : [-"+firstFeat+"] [+"+firstFeat+"] __");
		SChangeContext testX = testFactory.parseNewContext("[-"+firstFeat+"] [+"+firstFeat+"]", false); 
		System.out.println("The following should be [-"+firstFeat+"] [+"+firstFeat+"] :"
				+ testX.getPlaceRestrs().get(0)+" "+testX.getPlaceRestrs().get(1));
		System.out.println("The following should be i0 i1: "+testX.getParenMap()[0]+" "+testX.getParenMap()[1]);
		System.out.println("The following should be 2: "+testX.generateMinSize());
		dummyTestList.add(dummiePhone);
		System.out.println("The following should be true : "+testX.isPriorMatch(dummyTestList, 2)); 
		
		// TODO below is abrogated 
		
		
		SChangeContext testXX = testFactory.parseNewContext(X+" "+X, false); 
		System.out.println("The following should be 2\n"+testXX.generateMinSize());
		
		SChangeContext testCX7 = testFactory.parseNewContext("( "+X+" )", false);
		System.out.println("The following should be 0\n"+testCX7.generateMinSize());

		SChangeContext testCXX7 = testFactory.parseNewContext("("+X+" "+X+")", false); 
		System.out.println("The following should be 0\n"+testCXX7.generateMinSize()); 
		
		SChangeContext testXCX7t = testFactory.parseNewContext(X+" ("+X+")+", false);
		System.out.println("The following should be 2\n"+testXCX7t.generateMinSize());
		
		SChangeContext testXCX7i = testFactory.parseNewContext(X+" ("+X+")*", false); 
		System.out.println("The following should be 1\n"+testXCX7i.generateMinSize());
		
		SChangeContext testCXCX7i7t = testFactory.parseNewContext("("+X+" ("+X+")* )+",false);
		System.out.println("The following should be 1\n"+testCXCX7i7t.generateMinSize()); 
		 **/
	}
		
	
}
