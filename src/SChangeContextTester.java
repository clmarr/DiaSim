import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class SChangeContextTester {

	//excludes all material dealing with feature implications 

	private final static char MARK_POS = '+', MARK_NEG = '-', MARK_UNSPEC = '0', FEAT_DELIM = ',';
	private final static char IMPLICATION_DELIM=':', PH_DELIM = ' '; 
	private final static char restrDelim = ',';
	private final static int POS_INT = 2, NEG_INT = 0, UNSPEC_INT = 1;
	private static String[] featsByIndex; 
	private static List<String> featNames; 
	private static HashMap<String, Integer> featIndices;
	private static HashMap<String, String> phoneSymbToFeatsMap;
	private static HashMap<String, String> phoneFeatsToSymbMap; //TODO abrogate either this or the previous class variable
	private static HashMap<String, String[]> featImplications; 
	private static String featImplsLoc = "FeatImplications"; 
	private static Phone dummiePhone; 
	public static void main(String args[])
	{
		boolean boundsMatter = false; 
		
		featIndices = new HashMap<String, Integer>(); 
		phoneSymbToFeatsMap = new HashMap<String, String>(); 
		phoneFeatsToSymbMap = new HashMap<String, String>(); 
				
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
		
		//from the first line, extract the feature list and then the features for each symbol.
		featsByIndex = symbDefsLines.get(0).replace("SYMB,", "").split(""+FEAT_DELIM); 
		
		for(int fi = 0; fi < featsByIndex.length; fi++) 
			featIndices.put(featsByIndex[fi], fi);
		
		featNames = Arrays.asList(featsByIndex);
		
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
		
		System.out.println("After expanding the plus parens, it should now be : "); 
		System.out.println(W+" ( "+X+" ( "+Y+" ) "+Z+" ( "+Z+" )* ( "+W+" "+Y+" )* )");
		
		System.out.println("And it is ...\n"+testFactory.expandOutAllPlusses(testFactory.forceParenSpaceConsistency(testString0)));
				
		SequentialFilter testInstance0 = testFactory.parseNewSeqFilter(
				testFactory.forceParenSpaceConsistency(testString0), false); 
		System.out.println("Testing parseNewContext and associated methods");
		System.out.println("The following should be 1: "+testInstance0.generateMinSize()); 
				
		System.out.println("The following should be : ");
		System.out.println(W+" "+X+" "+Y+" "+Z+" "+W+" "+Y+" ");
		List<RestrictPhone> prstrs = testInstance0.getPlaceRestrs(); 
		for (RestrictPhone prstr : prstrs)
			System.out.print(prstr+" ");
		
		System.out.println("The following should be ...");
		System.out.println("i0 (:14,2 i1 (:5,1 i2 ):3,1 i3 *(:9,1 i4 )*:7,1 *(:13,2 i5 i6 )*:10,2 ):1,2 ");
		
		String[] parenMap = testInstance0.getParenMap();
		for(String pl : parenMap)	System.out.print(pl+" ");
		System.out.println("");

		System.out.println("Testing isPriorMatch and isPosteriorMatch");
		
		//TODO: A is ++, B +-, C -+ and D --
		List<SequentialPhonic> dummyTestList = new ArrayList<SequentialPhonic>(); 
		Phone Aph = new Phone(dummiePhone), Bph = new Phone(dummiePhone), Cph = new Phone(dummiePhone), Dph = new Phone(dummiePhone); 
		String ft1 = featsByIndex[0], ft2 = featsByIndex[1];
		Aph.set(ft1,2); Bph.set(ft1, 2); Cph.set(ft1, 0); Dph.set(ft1, 0);
		Aph.set(ft2, 2); Bph.set(ft2, 0); Cph.set(ft2,2); Dph.set(ft2,0);
		FeatMatrix Afm = new FeatMatrix("+"+ft1+FEAT_DELIM+"+"+ft2, featNames, featImplications),
				Bfm = new FeatMatrix("+"+ft1+FEAT_DELIM+"-"+ft2, featNames, featImplications),
				Cfm = new FeatMatrix("-"+ft1+FEAT_DELIM+"+"+ft2, featNames, featImplications), 
				Dfm = new FeatMatrix("-"+ft1+FEAT_DELIM+"-"+ft2, featNames, featImplications); 
		
		SequentialFilter testContext = testFactory.parseNewSeqFilter(Afm+"", boundsMatter);
		dummyTestList.add(Aph); dummyTestList.add(Cph); 
		
		System.out.println("\nTesting A __ as prior context");
		System.out.println("Min size should be 1 : "+testContext.generateMinSize()); 
		System.out.println("Paren map should be i0 : "+UTILS.printParenMap(testContext));
		System.out.println("A __ should be TRUE: " + testContext.isPriorMatch(dummyTestList,1));
		dummyTestList.set(0, Bph);
		System.out.println("B __ should be FALSE: " + testContext.isPriorMatch(dummyTestList, 1)); 
		dummyTestList.clear(); dummyTestList.add(Cph); dummyTestList.add(Aph); 
		System.out.println("\nTesting __ A as posterior context");
		System.out.println(" __ A should be TRUE : "+ testContext.isPosteriorMatch(dummyTestList, 1)); 
		dummyTestList.set(1, Bph); 
		System.out.println(" __ B should be FALSE : " + testContext.isPosteriorMatch(dummyTestList, 1)); 
		
		System.out.println("\nTesting A B __ and __ A B");
		testContext = testFactory.parseNewSeqFilter(Afm+" "+Bfm, boundsMatter);
		System.out.println("Min size should be 2 : "+testContext.generateMinSize());
		System.out.println("Paren map should be i0 i1 : "+UTILS.printParenMap(testContext));
		dummyTestList.clear(); dummyTestList.add(Aph); dummyTestList.add(Bph); dummyTestList.add(Cph); 
		System.out.println("A B __ should be TRUE for A B __ : "
				+ testContext.isPriorMatch(dummyTestList, 2));
		dummyTestList.clear(); dummyTestList.add(Cph); dummyTestList.add(Aph); dummyTestList.add(Bph);
		System.out.println("__ A B should be TRUE for __ A B : "
				+ testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.set(1,Bph); dummyTestList.set(2, Cph);
		System.out.println("C B __ should be FALSE for A B __ : "+ testContext.isPriorMatch(dummyTestList, 2));
		dummyTestList.set(1,Cph); dummyTestList.set(2, Bph);
		System.out.println(" __ C B should be FALSE for __ A B : "+ testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.clear(); dummyTestList.add(Aph); dummyTestList.add(Cph); dummyTestList.add(Cph);
		System.out.println("A C __ should be FALSE for A B __ : "+ testContext.isPriorMatch(dummyTestList, 2));
		dummyTestList.set(1, Aph);
		System.out.println("__ A C should be FALSE for __ A B : "+ testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.set(0, Bph); dummyTestList.set(1, Aph); 
		System.out.println("B A __ should be FALSE for A B __ : "+ testContext.isPriorMatch(dummyTestList, 2));
		dummyTestList.set(1, Bph); dummyTestList.set(2,Aph); 
		System.out.println("__ B A should be FALSE for __ A B : "+ testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.clear(); dummyTestList.add(Aph); 
		dummyTestList.add(new Boundary("word bound")); dummyTestList.add(Bph); dummyTestList.add(Cph);
		System.out.println("A # B __ should be "+(""+!boundsMatter).toUpperCase()+" for A B __, in accordance with the value of"
				+ " boundsMatter being "+boundsMatter+": " + testContext.isPriorMatch(dummyTestList, 3));
		dummyTestList.remove(3); dummyTestList.add(0, Cph);
		System.out.println("__ A # B should be "+(""+!boundsMatter).toUpperCase()+" for __ A B, in accordance with the value of "+
				"boundsMatter being "+boundsMatter+": " + testContext.isPosteriorMatch(dummyTestList, 1));
		
		testContext = testFactory.parseNewSeqFilter(""+Afm+PH_DELIM+"("+Bfm+")", boundsMatter);
		dummyTestList.remove(2); dummyTestList.remove(0); dummyTestList.add(Cph); 
		System.out.println("\nTesting A (B) __");
		System.out.println("Min size should be 1 "+testContext.generateMinSize());
		System.out.println("Paren map should be i0 (:3,1 i1 ):1,1 --- "+UTILS.printParenMap(testContext)); 
		System.out.println("A B __ should be TRUE : "+testContext.isPriorMatch(dummyTestList, 2));
		dummyTestList.remove(1); 
		System.out.println("A __ should be TRUE : "+testContext.isPriorMatch(dummyTestList, 1)); 
		dummyTestList.clear(); dummyTestList.add(Cph); dummyTestList.add(Bph); dummyTestList.add(Cph); 
		System.out.println("C B __ should be FALSE : "+testContext.isPriorMatch(dummyTestList, 2));
		
		testContext = testFactory.parseNewSeqFilter("("+Afm+")"+PH_DELIM+Bfm, boundsMatter);
		dummyTestList.set(1, Aph); dummyTestList.set(2, Bph);
		System.out.println("\nTesting __ (A) B");
		System.out.println("Min size should be 1 : "+testContext.generateMinSize());
		System.out.println("ParenMap should be (:2,1 i0 ):0,1 i1 : "+UTILS.printParenMap(testContext));
		System.out.println("__ A B should be TRUE : "+testContext.isPosteriorMatch(dummyTestList, 1)); 
		dummyTestList.remove(1);
		System.out.println(" __ B should be TRUE : "+testContext.isPosteriorMatch(dummyTestList, 1)); 
		dummyTestList.clear(); dummyTestList.add(Cph); dummyTestList.add(Cph); dummyTestList.add(Bph); 
		System.out.println(" __ C B should be FALSE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		
		System.out.println("\nTesting # A __  and __ # A");
		testContext = testFactory.parseNewSeqFilter("#"+PH_DELIM+Afm, boundsMatter);
		dummyTestList.clear(); dummyTestList.add(new Boundary("word bound"));
		dummyTestList.add(Aph); dummyTestList.add(Cph); 
		System.out.println(" # A __ should be TRUE (prior) : "+testContext.isPriorMatch(dummyTestList, 2)); 
		dummyTestList.add(0, dummyTestList.remove(2)); 
		System.out.println(" __ # A should be TRUE (posterior) : "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.add(dummyTestList.remove(0)); dummyTestList.add(1,new Boundary("morph bound")); 
		System.out.println("# + A __ should be "+(!boundsMatter+"").toUpperCase()+" according to boundsMatter "
				+ ": "+testContext.isPriorMatch(dummyTestList, 3)); 
		dummyTestList.remove(3); dummyTestList.add(0, Cph); 
		System.out.println("__ # + A should be "+(!boundsMatter+"").toUpperCase()+" according to boundsMatter "
				+ ": "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.clear(); dummyTestList.add(Bph); dummyTestList.add(Aph); dummyTestList.add(Cph);  
		System.out.println("B A __ should be FALSE : "+testContext.isPriorMatch(dummyTestList, 2));
		dummyTestList.add(0, dummyTestList.remove(2)); 
		dummyTestList.add(dummyTestList.remove(1)); 
		System.out.println("__ A B should be FALSE : "+testContext.isPosteriorMatch(dummyTestList,1));
		dummyTestList.clear(); dummyTestList.add(Bph); dummyTestList.add(new Boundary("morph bound")); 
		dummyTestList.add(Aph); dummyTestList.add(Cph); 
		System.out.println("B + A __ should be FALSE : "+testContext.isPriorMatch(dummyTestList, 3));
		dummyTestList.add(0, dummyTestList.remove(3));
		System.out.println("__ B + A should be FALSE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		
		testContext = testFactory.parseNewSeqFilter("@"+PH_DELIM+Afm, boundsMatter); 
		System.out.println("\nTesting @ A __ and __ @ A ");
		dummyTestList.clear(); dummyTestList.add(Bph); dummyTestList.add(Aph); dummyTestList.add(Cph); 
		System.out.println("B A __ should be TRUE : "+testContext.isPriorMatch(dummyTestList, 2));
		dummyTestList.add(0, dummyTestList.remove(2));
		System.out.println("__ B A should be TRUE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.add(dummyTestList.remove(0)); dummyTestList.add(2, new Boundary("morph bound")); 
		System.out.println("B + A __ should be TRUE : "+testContext.isPriorMatch(dummyTestList, 3)); 
		dummyTestList.add(0, dummyTestList.remove(3));
		System.out.println("__ B + A should be TRUE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.clear(); dummyTestList.add(new Boundary("word bound")); dummyTestList.add(Aph); 
		dummyTestList.add(Cph);
		System.out.println("# A __ should be FALSE : "+testContext.isPriorMatch(dummyTestList, 2)); 
		dummyTestList.add(0, dummyTestList.remove(2)); 
		System.out.println("__ # A should be FALSE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.add(dummyTestList.remove(0)); dummyTestList.add(0, Bph);
		System.out.println("B # A __ should be FALSE : "+testContext.isPriorMatch(dummyTestList, 3));
		dummyTestList.clear(); dummyTestList.add(Cph); dummyTestList.add(new Boundary("word bound")); 
		dummyTestList.add(Aph); dummyTestList.add(Bph); 
		System.out.println("__ # A B should be FALSE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		
		testContext = testFactory.parseNewSeqFilter(""+Afm+PH_DELIM+"("+Bfm+")*", boundsMatter);
		dummyTestList.clear(); dummyTestList.add(Aph); dummyTestList.add(Cph);
		System.out.println("\nTesting A (B)* __ and __ A (B)* ");
		System.out.println("Min size should be 1 : "+testContext.generateMinSize());
		System.out.println("Paren map should be i0 | *(:3,1 | i1 | )*:1,1 : "+UTILS.printParenMap(testContext));
		System.out.println("A __ should be TRUE : "+testContext.isPriorMatch(dummyTestList, 1));
		dummyTestList.clear(); dummyTestList.add(Cph); dummyTestList.add(Aph);
		System.out.println("__ A should be TRUE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.clear(); dummyTestList.add(Aph); dummyTestList.add(Bph); dummyTestList.add(Bph); 
		dummyTestList.add(Bph); dummyTestList.add(Cph);
		System.out.println("A B B B __ should be TRUE : "+testContext.isPriorMatch(dummyTestList, 4)); 
		dummyTestList.clear(); dummyTestList.add(Cph); dummyTestList.add(Aph); dummyTestList.add(Bph);
		dummyTestList.add(Bph); dummyTestList.add(Bph);
		System.out.println("__ A B B B should be TRUE : "+testContext.isPosteriorMatch(dummyTestList,1));
		
		System.out.println("\nTesting (A)* B __ and __ (A)* B"); 
		testContext = testFactory.parseNewSeqFilter("("+Afm+")*"+PH_DELIM+Bfm, boundsMatter);
		dummyTestList.clear(); dummyTestList.add(Bph); dummyTestList.add(Cph); 
		System.out.println("B __ should be TRUE : "+testContext.isPriorMatch(dummyTestList, 1));
		dummyTestList.clear(); dummyTestList.add(Cph); dummyTestList.add(Bph); 
		System.out.println("__ B should be TRUE : "+testContext.isPosteriorMatch(dummyTestList,1)); 
		dummyTestList.clear(); dummyTestList.add(Aph); dummyTestList.add(Aph); dummyTestList.add(Aph); 
		dummyTestList.add(Bph); dummyTestList.add(Cph);
		System.out.println("A A A B __ should be TRUE : "+testContext.isPriorMatch(dummyTestList, 4));
		dummyTestList.add(3,Bph); 
		System.out.println("A A A B B __ should be TRUE : "+testContext.isPriorMatch(dummyTestList, 5));
		dummyTestList.clear(); dummyTestList.add(Cph); dummyTestList.add(Aph); dummyTestList.add(Aph); 
		dummyTestList.add(Aph); dummyTestList.add(Bph);
		System.out.println(" __ A A A B should be TRUE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.add(Bph);
		System.out.println(" __ A A A B B should be TRUE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		
		testContext = testFactory.parseNewSeqFilter("#"+PH_DELIM+"("+Cfm+PH_DELIM+Bfm+")*", boundsMatter); 
		System.out.println("\nTesting # (C B)* __"); 
		dummyTestList.clear(); dummyTestList.add(new Boundary("word bound"));
		dummyTestList.add(Cph); dummyTestList.add(Bph); dummyTestList.add(Cph); dummyTestList.add(Bph);
		dummyTestList.add(Dph);
		System.out.println("# C B C B __ should be TRUE : "+testContext.isPriorMatch(dummyTestList, 5));
		dummyTestList.remove(0); 
		System.out.println("C B C B __ should be FALSE : "+testContext.isPriorMatch(dummyTestList, 4));
		dummyTestList.add(0, new Boundary("word bound")); 
		dummyTestList.remove(4); dummyTestList.remove(1); 
		System.out.println("# B C __ should be FALSE : "+testContext.isPriorMatch(dummyTestList, 4));
		
		testContext = testFactory.parseNewSeqFilter("("+Cfm+PH_DELIM+Bfm+")*"+PH_DELIM+"#", boundsMatter);
		System.out.println("\nTesting __ (C B)* #");
		dummyTestList.clear();
		dummyTestList.add(Dph); dummyTestList.add(Cph); dummyTestList.add(Bph); dummyTestList.add(Cph);
		dummyTestList.add(Bph); dummyTestList.add(new Boundary("word bound")); 
		System.out.println("__ C B C B # should be TRUE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.remove(4); dummyTestList.remove(1); 
		System.out.println("__ B C # should be FALSE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.set(3, Bph); 
		System.out.println("__ B C B should be FALSE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		
		testContext = testFactory.parseNewSeqFilter("#"+PH_DELIM+"("+Cfm+PH_DELIM+Bfm+"("+Dfm+"))*", boundsMatter);
		System.out.println("\nTesting # ( C B (D) )* __" );
		dummyTestList.clear(); dummyTestList.add(new Boundary("word bound")); 
		System.out.println("# __ should be TRUE : "+testContext.isPriorMatch(dummyTestList,1)); 
		dummyTestList.add(Cph); dummyTestList.add(Bph);
		System.out.println("# C B __ should be TRUE : "+testContext.isPriorMatch(dummyTestList,3));
		dummyTestList.add(3,Dph); 
		System.out.println("# C B D __ should be TRUE : "+testContext.isPriorMatch(dummyTestList, 4));
		dummyTestList.add(1,Dph); 
		System.out.println("# D C B D __ should be FALSE : "+testContext.isPriorMatch(dummyTestList, 5));
		dummyTestList.add(1,Bph); dummyTestList.add(1, Cph);
		System.out.println("# C B D C B D __ should be TRUE : "+testContext.isPriorMatch(dummyTestList, 7));
		dummyTestList.add(3, Dph);
		System.out.println("# C B D D C B D __ should be FALSE : "+testContext.isPriorMatch(dummyTestList, 8)); 
		dummyTestList.remove(3); dummyTestList.remove(2); 
		System.out.println("# C D C B D __ should be FALSE : "+testContext.isPriorMatch(dummyTestList, 6));
		dummyTestList.set(2, Bph); 
		System.out.println("# C B C B D __ should be TRUE : "+testContext.isPriorMatch(dummyTestList, 6));
		
		
		testContext = testFactory.parseNewSeqFilter("("+Cfm+PH_DELIM+Bfm+PH_DELIM+"("+Dfm+"))*"+PH_DELIM+"#", boundsMatter); 
		System.out.println("\nTesting __ (C B (D) )* # :") ;
		dummyTestList.clear(); dummyTestList.add(Aph); dummyTestList.add(new Boundary("word bound"));
		System.out.println("__ # should be TRUE : "+testContext.isPosteriorMatch(dummyTestList, 1)); 
		dummyTestList.add(1, Bph); dummyTestList.add(1, Cph); 
		System.out.println("__ C B # should be TRUE : "+testContext.isPosteriorMatch(dummyTestList, 1)); 
		dummyTestList.add(3, Dph); 
		System.out.println("__ C B D # should be TRUE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.add(1, Dph); 
		System.out.println("__ D C B D # should be FALSE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.add(1, Bph); dummyTestList.add(1, Cph); 
		System.out.println("__ C B D C B D # should be TRUE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.remove(6); 
		System.out.println("__ C B D C B # should be TRUE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.add(1,Dph); 
		System.out.println("__ D C B D C B # should be FALSE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		dummyTestList.clear(); dummyTestList.add(Cph); dummyTestList.add(Bph); dummyTestList.add(Dph); dummyTestList.add(Dph);
		dummyTestList.add(new Boundary("word bound")); 
		System.out.println("__ C B D D # should be FALSE : "+testContext.isPosteriorMatch(dummyTestList, 1));
		
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
