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

	private final static char MARK_POS = '+', MARK_NEG = '-', MARK_UNSPEC = '0', FEAT_DELIM = ',';
	private final static char IMPLICATION_DELIM = ':', PH_DELIM = ' ';
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
		
		SChangeFeat scfTest = new SChangeFeat(featIndices, "-voi", "+voi","DEBUG",featImplications); 
		scfTest.setPostContext(testFactory.parseNewSeqFilter("[+voi]", false));
		
		int numCorrect = 0 ; 
		
		numCorrect += runTest(scfTest, testFactory.parseSeqPhSeg("a"+PH_DELIM+"s"+PH_DELIM+"t"+PH_DELIM+"a"), 
				testFactory.parseSeqPhSeg("a"+PH_DELIM+"s"+PH_DELIM+"d"+PH_DELIM+"a")) ? 1 : 0; 
		
		scfTest = new SChangeFeat(featIndices, "-nas", "+nas", "DEBUG", featImplications);
		scfTest.setPriorContext(testFactory.parseNewSeqFilter("+nas", false)); 
		numCorrect += runTest(scfTest, testFactory.parseSeqPhSeg("n a b a n a"),
				testFactory.parseSeqPhSeg("n ã b a n ã")) ? 1 : 0;
		
		scfTest = new SChangeFeat(new FeatMatrix("+syl,-cons", featIndices, featImplications), new NullPhone(), "DEBUG");
		scfTest.setPriorContext(testFactory.parseNewSeqFilter("+son", false));
		numCorrect += runTest(scfTest, testFactory.parseSeqPhSeg("r e a l e a"), 
				testFactory.parseSeqPhSeg("r a l a")) ? 1 : 0; 
		
		scfTest = new SChangeFeat(new FeatMatrix("+syl", featIndices, featImplications), new NullPhone(), "DEBUG"); 
		scfTest.setPriorContext(testFactory.parseNewSeqFilter("+syl", boundsMatter));
		numCorrect += runTest(scfTest, testFactory.parseSeqPhSeg("r e a l e a"), 
				testFactory.parseSeqPhSeg("r e l e")) ? 1 : 0;
		
		scfTest = new SChangeFeat(featIndices, "-cont,-nas,-lat,-delrel","-voi","DEBUG", featImplications); 
		numCorrect += runTest(scfTest, testFactory.parseSeqPhSeg("d i d e ð l a d d o n u r"),
				testFactory.parseSeqPhSeg("t i t e ð l a t t o n u r")) ? 1 : 0; 
		
		scfTest = new SChangeFeat(new FeatMatrix("-cont,-nas,-lat,-delrel", featIndices, featImplications),
				new Phone(phoneSymbToFeatsMap.get("q"), featIndices, phoneSymbToFeatsMap),"DEBUG");
		numCorrect += runTest(scfTest, testFactory.parseSeqPhSeg("d i d e ð l a d d o n u r"),
				testFactory.parseSeqPhSeg("q i q e ð l a q q o n u r")) ? 1 : 0 ;
		
		scfTest = new SChangeFeat(featIndices, "-cont", "+nas,+son,.delrel,+cont",
				testFactory.parseNewSeqFilter("+nas,-syl", false), testFactory.parseNewSeqFilter("+syl", false), "DEBUG", featImplications);
		numCorrect += runTest(scfTest, testFactory.parseSeqPhSeg("b i m b d e n n o"),
				testFactory.parseSeqPhSeg("b i m b d e n n o")) ? 1 : 0; 
		
		System.out.println("Done testing SChangeFeat. Got "+numCorrect+" tests correct out of 7.\n"
				+ "Note that testing context processing should be "
				+ "done in SChangeContextTester, \n...while this class also doesn't handle the effect of an entered "
				+ "feature implications file.");
		
		System.out.println("Now testing SChangeFeatToPhone");
		
		SChangeFeatToPhone scftpTest = new SChangeFeatToPhone(featIndices,
				testFactory.parseRestrictPhoneSequence("l"+PH_DELIM+"[+hi,+front]"),
				testFactory.parsePhoneSequenceForDest("ʎ"), "DEBUG");  
		numCorrect = runTest(scftpTest, testFactory.parseSeqPhSeg("a l j a"), testFactory.parseSeqPhSeg("a ʎ a")) ? 1 : 0; 
		numCorrect += runTest(scftpTest, testFactory.parseSeqPhSeg("a l i e l j"), testFactory.parseSeqPhSeg(" a ʎ e ʎ")) ? 1 : 0; 
		
		scftpTest = new SChangeFeatToPhone(featIndices, 
				testFactory.parseRestrictPhoneSequence("[+hi,+front,-syl]"), 
				testFactory.parsePhoneSequenceForDest("j ɟ ʝ"), "DEBUG"); 
		numCorrect += runTest(scftpTest, testFactory.parseSeqPhSeg("a c i a j o"), testFactory.parseSeqPhSeg("a j ɟ ʝ i a j ɟ ʝ o")) ? 1 : 0 ;
		
		scftpTest = new SChangeFeatToPhone(featIndices, 
				testFactory.parseRestrictPhoneSequence("[-cons,+front,+hi] [+syl,+back] [-cont,+hi]"),
				testFactory.parsePhoneSequenceForDest("ʝ o w j"), "DEBUG"); 
		numCorrect += runTest(scftpTest, testFactory.parseSeqPhSeg("j o c o k a"), testFactory.parseSeqPhSeg("ʝ o w j o k a ")) ? 1 : 0 ;
		
		scftpTest = new SChangeFeatToPhone(featIndices,
				testFactory.parseRestrictPhoneSequence("[-cont,+cor,-voi] # j [+syl]"), 
				testFactory.parsePhoneSequenceForDest("t͡ʃ j ə"), "DEBUG"); 
		scftpTest.setPostContext(testFactory.parseNewSeqFilter("#", true)); 
		numCorrect += runTest(scftpTest, testFactory.parseSeqPhSeg("# ɡ ˈɑ t # j ˈu #"), testFactory.parseSeqPhSeg("# ɡ ˈɑ t͡ʃ j ə #")) ? 1 : 0 ; 
		numCorrect += runTest(scftpTest, testFactory.parseSeqPhSeg("# kʰ ˈɛ t͡ʃ # j ˈu #"), testFactory.parseSeqPhSeg("# kʰ ˈɛ t͡ʃ j ə #")) ? 1 : 0; 
		numCorrect += runTest(scftpTest, testFactory.parseSeqPhSeg("# ɡ ˈɑ t # j ˈæ̃ː m z #"), testFactory.parseSeqPhSeg("# ɡ ˈɑ t # j ˈæ̃ː m z #")) ? 1 : 0; 
		
		scftpTest = new SChangeFeatToPhone(featIndices,
				testFactory.parseRestrictPhoneSequence("[-cont,+cor] # [+syl,-prim]"),
				testFactory.parsePhoneSequenceForDest("ɾ ə"), "DEBUG");
		numCorrect += runTest(scftpTest, testFactory.parseSeqPhSeg("# f ə ɡ ˈɛ t # ə b 'a w t # ɪ t #"),
				testFactory.parseSeqPhSeg("# f ə ɡ ˈɛ ɾ ə b 'a w ɾ ə t #")) ? 1 : 0; 
		
		System.out.println("Done testing SChangeFeatToPhone. Got "+numCorrect+" out of 8 tests correct. Now testing SChangePhone"); 
		numCorrect = 0; 
		
		SChangePhone scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs("h"), testFactory.parseSeqPhDisjunctSegs(""), "DEBUG");
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("h a m e h a m e h a h"), 
				testFactory.parseSeqPhSeg("a m e a m e a")) ? 1 : 0 ;
		
		scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs("a j ; eː ; iː"),
				testFactory.parseSeqPhDisjunctSegs("e j ; iː ; a j"), "DEBUG");
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("l eː w a j"),
				testFactory.parseSeqPhSeg("l iː w e j")) ? 1 : 0; 
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("tʰ eː tʰ iː m"),
				testFactory.parseSeqPhSeg("tʰ iː tʰ a j m")) ? 1 : 0; 
		
		scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs("l j"), testFactory.parseSeqPhDisjunctSegs("ʎ"), "DEBUG"); 
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("l j u l j"), 
				testFactory.parseSeqPhSeg("ʎ u ʎ")) ? 1 : 0 ; 
		
		scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs(""), testFactory.parseSeqPhDisjunctSegs("i"),
				testFactory.parseNewSeqFilter("#", true), testFactory.parseNewSeqFilter("s [+cons]", true), "DEBUG");
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("# s p a t a #"), testFactory.parseSeqPhSeg("# i s p a t a #")) ? 1 : 0;
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("s p a t a #"), testFactory.parseSeqPhSeg("s p a t a #")) ? 1 : 0;
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("# s j a t a #"), testFactory.parseSeqPhSeg("# s j a t a #")) ? 1 : 0;
		
		scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs("{p;b;k;ɡ}"),
				new ArrayList<RestrictPhone>(testFactory.parseRestrictPhoneSequence("+front,+hi,-back")), "DEBUG");
		scpTest.setPostContext(testFactory.parseNewSeqFilter("#", true));
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("# s a k #"), testFactory.parseSeqPhSeg("# s a c #")) ? 1 : 0 ;
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("s a k"), testFactory.parseSeqPhSeg("s a k")) ? 1 : 0 ;
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("# s u p #"), testFactory.parseSeqPhSeg("# s u pʲ #")) ? 1 : 0 ;

		scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs("t u"),
				testFactory.parseSeqPhDisjunctSegs("n ə"),
				testFactory.parseNewSeqFilter("[+nas] ([-cont,-delrel]) #", true),
				testFactory.parseNewSeqFilter("#", true), "DEBUG");
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("# ɡ o w ɪ ŋ # t u #"), testFactory.parseSeqPhSeg("# ɡ o w ɪ ŋ # n ə #")) ? 1 : 0; 
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("# t u # w ɑ̃ t # t u #"), testFactory.parseSeqPhSeg("# t u # w ɑ̃ t # n ə #")) ? 1 : 0; 
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("# n o w ɪ ŋ # t u v ə n #"), testFactory.parseSeqPhSeg("# n o w ɪ ŋ # t u v ə n #")) ? 1 : 0;
		
		scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs("{b;ɡ}"),
				new ArrayList<RestrictPhone>( 
						testFactory.parseRestrictPhoneSequence("[+cont]", true)), "DEBUG");
		scpTest.setPriorContext(testFactory.parseNewSeqFilter("[+syl] (#)", true));
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("# a ɡ a r a #"), testFactory.parseSeqPhSeg("# a ɣ a r a #")) ? 1 : 0 ; 
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("# a # b i t u #"), testFactory.parseSeqPhSeg("# a # β i t u #")) ? 1 : 0; 
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("# b i t u #"), testFactory.parseSeqPhSeg("# b i t u #")) ? 1 : 0;
		numCorrect += runTest(scpTest, testFactory.parseSeqPhSeg("# k o m # b i t u #"), testFactory.parseSeqPhSeg("# k o m # b i t u #")) ? 1 : 0 ;
		
		System.out.println("Done testing SChangePhone. Got "+numCorrect+" correct out of 17.");
		
		System.out.println("Now testing SChangeSeqToSeq");
		numCorrect = 0;
		SChangeSeqToSeq scsqTest = new SChangeSeqToSeq(featIndices, phoneSymbToFeatsMap, 
				testFactory.parseRestrictPhoneSequence("[+hi,+tense,+long] ∅"), testFactory.parseRestrictPhoneSequence("[-hi,+lo,-long] j",true), "DEBUG");
		numCorrect += runTest(scsqTest, testFactory.parseSeqPhSeg("# t ˈiː m #"), testFactory.parseSeqPhSeg("# t ˈa j m #")) ? 1 : 0;
		numCorrect += runTest(scsqTest, testFactory.parseSeqPhSeg("# t iː m #"), testFactory.parseSeqPhSeg("# t a j m #")) ? 1 : 0 ;
		numCorrect += runTest(scsqTest, testFactory.parseSeqPhSeg("# t ˌiː iː #" ), testFactory.parseSeqPhSeg("# t ˌa j a j #")) ? 1 : 0 ;
		
		scsqTest = new SChangeSeqToSeq ( featIndices, phoneSymbToFeatsMap,
				testFactory.parseRestrictPhoneSequence("[-lo] [+round]"), testFactory.parseRestrictPhoneSequence("[+round,+long,+lab] ∅", true), "DEBUG"); 
		numCorrect += runTest(scsqTest, testFactory.parseSeqPhSeg("l ˌe w u"), testFactory.parseSeqPhSeg("l ˌøː u")) ? 1 : 0;
		
		scsqTest = new SChangeSeqToSeq ( featIndices, phoneSymbToFeatsMap, 
				testFactory.parseRestrictPhoneSequence("∅ [+front]"), testFactory.parseRestrictPhoneSequence("e̯ [+hi,+tense]", true), "DEBUG"); 
		numCorrect += runTest(scsqTest, testFactory.parseSeqPhSeg("eː n r a"), testFactory.parseSeqPhSeg("e̯ iː n r a")) ? 1 : 0 ;
		
		scsqTest = new SChangeSeqToSeq( featIndices, phoneSymbToFeatsMap, 
				testFactory.parseRestrictPhoneSequence("[+hi,+son] [+lab]"), testFactory.parseRestrictPhoneSequence("[-tense] [+cont]", true), "DEBUG");
		scsqTest.setPostContext(testFactory.parseNewSeqFilter("[+cons]",false));
		numCorrect += runTest(scsqTest, testFactory.parseSeqPhSeg("h u p p u p i p k"), testFactory.parseSeqPhSeg("h ʊ ɸ p u p ɪ ɸ k")) ? 1 : 0 ; 
		
		scsqTest = new SChangeSeqToSeq( featIndices, phoneSymbToFeatsMap, 
				testFactory.parseRestrictPhoneSequence("[+lab] [+lab] [+hi,+front,-syl]"),
				testFactory.parseRestrictPhoneSequence("∅ [-lab,-ant,+hi,+front] ∅", true), "DEBUG");
		numCorrect += runTest(scsqTest, testFactory.parseSeqPhSeg("a w β j p j o w p c y"), 
				testFactory.parseSeqPhSeg("a ʝ p j o c y")) ? 1 : 0 ;
				
		scsqTest = new SChangeSeqToSeq(featIndices, phoneSymbToFeatsMap, 
				testFactory.parseRestrictPhoneSequence("[+back] [+back]"), testFactory.parseRestrictPhoneSequence("∅ [+long]", true), "DEBUG"); 
		scsqTest.setPriorContext(testFactory.parseNewSeqFilter("#", true));
		
		numCorrect += runTest(scsqTest, testFactory.parseSeqPhSeg("# ɣ ɑ kʷ ɔ x #"), testFactory.parseSeqPhSeg("# ɑː kʷ ɔ x #")) ? 1 : 0 ; 
		
		System.out.println("Done testing SChangeSeqToSeq. Got "+numCorrect+" correct out of 8.");
		
		numCorrect = 0; 
		System.out.println("\nNow testing alpha variable functionality."); 
		System.out.println("First : testing alpha variable functionality of FeatMatrices."); 
		System.out.println("Default -- no alpha features"); 
		FeatMatrix fmtest = new FeatMatrix("+prim,+stres",featIndices,featImplications); 
		
		System.out.println("init_chArr : "+fmtest.getStrInitChArr()); 
		System.out.println("feat vect : "+fmtest.getFeatVect()); 
		System.out.println("specs : "+fmtest); 
		System.out.println("has alpha specs? Should be false: "+fmtest.has_alpha_specs());
		System.out.println("first unset alpha should be '0': "+fmtest.first_unset_alpha()); 
		SequentialPhonic pfm = testFactory.parseSeqPh("e"); 
		System.out.println("features extracted should be 0 : "+fmtest.extract_alpha_values(pfm).keySet().size()); 
		
		System.out.println("\nNow for a feat matrix with one alpha value...");
		fmtest = new FeatMatrix("ɑstres,+syl",featIndices,featImplications); 
		System.out.println("init_chArr : "+fmtest.getStrInitChArr()); 
		System.out.println("feat vect : "+fmtest.getFeatVect()); 
		System.out.println("specs : "+fmtest); 
		System.out.println("has alpha specs? Should be true: "+fmtest.has_alpha_specs());
		System.out.println("first unset alpha should be 'ɑ': "+fmtest.first_unset_alpha()); 
		System.out.println("Has multi-used alpha symbol? Should be false: "+fmtest.has_multispec_alph()); 

		List<SequentialPhonic> actOn = testFactory.parseSeqPhSeg("ˈo");
		System.out.println("Trying to forceTruth without initializing the alpha value should result in a caught UnsetAlphaError"); 
		
		boolean caught = false; 
		try {	fmtest.forceTruth(actOn,0); 		}
		catch(UnsetAlphaError e)	{	System.out.println("UnsetAlphaError caught"); caught = true;		}
		if (!caught)	System.out.println("Uh oh: forceTruth failed to trigger an error!");
		
		System.out.println("\nNow extract from : "+pfm); 
		HashMap<String,String> toApply = fmtest.extract_alpha_values(pfm); 
		fmtest.applyAlphaValues(toApply);
		
		System.out.println("init_chArr : "+fmtest.getStrInitChArr()); 
		System.out.println("feat vect : "+fmtest.getFeatVect()); 
		System.out.println("specs : "+fmtest); 
		System.out.println("has alpha specs? Should be true: "+fmtest.has_alpha_specs());
		System.out.println("first unset alpha should be '0': "+fmtest.first_unset_alpha()); 
		System.out.println("Has multi-used alpha symbol? Should be false: "+fmtest.has_multispec_alph()); 
		System.out.println("Action on "+actOn.get(0)+" ...\n\t"+fmtest.forceTruth(actOn,0).get(0)); 
		
	}

	


	private static String generateErrorMessage(SChange sc, List<SequentialPhonic> input,
			List<SequentialPhonic> expected, List<SequentialPhonic> observed) {
		return "Error in realization of this rule:\t\t" + sc + "\n\tInput was:\t" + UTILS.printWord(input)
				+ "\n\tExpected result: " + UTILS.printWord(expected) + "\n\tObserved result:\t\t" + UTILS.printWord(observed)
				+ "\n";
	}

	private static boolean runTest(SChange sc, List<SequentialPhonic> inp, List<SequentialPhonic> exp) {
		List<SequentialPhonic> obs = sc.realize(inp);
		if (UTILS.phonSeqsEqual(exp, obs))
			return true;
		System.out.print(generateErrorMessage(sc, inp, exp, obs));
		return false;
	}

}
