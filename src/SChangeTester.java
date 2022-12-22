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
		
		SChangeFeat scfTest = new SChangeFeat(Arrays.asList(featsByIndex), "-voi", "+voi","DEBUG",featImplications); 
		scfTest.setPostContext(testFactory.parseNewSeqFilter("[+voi]", false));
		
		int numCorrect = 0 ; 
		
		numCorrect += runTest(scfTest, testFactory.parseSeqPhSeg("a"+PH_DELIM+"s"+PH_DELIM+"t"+PH_DELIM+"a"), 
				testFactory.parseSeqPhSeg("a"+PH_DELIM+"s"+PH_DELIM+"d"+PH_DELIM+"a")) ? 1 : 0; 
		
		scfTest = new SChangeFeat(Arrays.asList(featsByIndex), "-nas", "+nas", "DEBUG", featImplications);
		scfTest.setPriorContext(testFactory.parseNewSeqFilter("+nas", false)); 
		numCorrect += runTest(scfTest, testFactory.parseSeqPhSeg("n a b a n a"),
				testFactory.parseSeqPhSeg("n ã b a n ã")) ? 1 : 0;
		
		scfTest = new SChangeFeat(new FeatMatrix("+syl,-cons", Arrays.asList(featsByIndex), featImplications), new NullPhone(), "DEBUG");
		scfTest.setPriorContext(testFactory.parseNewSeqFilter("+son", false));
		numCorrect += runTest(scfTest, testFactory.parseSeqPhSeg("r e a l e a"), 
				testFactory.parseSeqPhSeg("r a l a")) ? 1 : 0; 
		
		scfTest = new SChangeFeat(new FeatMatrix("+syl", Arrays.asList(featsByIndex), featImplications), new NullPhone(), "DEBUG"); 
		scfTest.setPriorContext(testFactory.parseNewSeqFilter("+syl", boundsMatter));
		numCorrect += runTest(scfTest, testFactory.parseSeqPhSeg("r e a l e a"), 
				testFactory.parseSeqPhSeg("r e l e")) ? 1 : 0;
		
		scfTest = new SChangeFeat(Arrays.asList(featsByIndex), "-cont,-nas,-lat,-delrel","-voi","DEBUG", featImplications); 
		numCorrect += runTest(scfTest, testFactory.parseSeqPhSeg("d i d e ð l a d d o n u r"),
				testFactory.parseSeqPhSeg("t i t e ð l a t t o n u r")) ? 1 : 0; 
		
		scfTest = new SChangeFeat(new FeatMatrix("-cont,-nas,-lat,-delrel", Arrays.asList(featsByIndex), featImplications),
				new Phone(phoneSymbToFeatsMap.get("q"), featIndices, phoneSymbToFeatsMap),"DEBUG");
		numCorrect += runTest(scfTest, testFactory.parseSeqPhSeg("d i d e ð l a d d o n u r"),
				testFactory.parseSeqPhSeg("q i q e ð l a q q o n u r")) ? 1 : 0 ;
		
		scfTest = new SChangeFeat(Arrays.asList(featsByIndex), "-cont", "+nas,+son,.delrel,+cont",
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
		numCorrect += runTest(scftpTest, testFactory.parseSeqPhSeg("# f ə ɡ ˈɛ t # ə b ˈa w t # ɪ t #"),
				testFactory.parseSeqPhSeg("# f ə ɡ ˈɛ ɾ ə b ˈa w ɾ ə t #")) ? 1 : 0; 
		
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
		System.out.println("First : testing alpha variable functionality of FeatMatrices and no alpha feats specified"); 
		FeatMatrix fmtest = new FeatMatrix("+prim,+stres",Arrays.asList(featsByIndex),featImplications); 
		
		numCorrect += UTILS.checkBoolean(false, fmtest.has_alpha_specs(), 
				"Error: system believes there to be alpha specs when there are none.") ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(true, fmtest.first_unset_alpha() == '0', 
				"Error: first_unset_alpha() should return '0' but instead we get "+fmtest.first_unset_alpha()) ? 1 : 0 ;
		
		SequentialPhonic e_tense = testFactory.parseSeqPh("e"); 
		
		String prevFeatVect = fmtest.getFeatVect(); 
		
		int n_feats_extracted = fmtest.extractAndApplyAlphaValues(e_tense).keySet().size(); 
		numCorrect += UTILS.checkBoolean(true, 
				n_feats_extracted == 0, 
				"Error: there should be zero features extracted since there are no alpha feats specified to begin with, "
				+ "but "+n_feats_extracted+" were extracted!" ) ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, 
				prevFeatVect.equals(fmtest.getFeatVect()),
				"Error: the feat vect should have been unchanged but it has changed from\n"+prevFeatVect+"\nto\n"+fmtest.getFeatVect()) 
				? 1 : 0; 
		System.out.println("In this mode, got "+numCorrect+" correct out of 4.");
		numCorrect = 0; 
				
		System.out.println("\nNow for a feature matrix with one alpha value, without any feature implications (-tense,βhi)..."); 
		fmtest = new FeatMatrix("-tense,βhi", Arrays.asList(featsByIndex), featImplications); 
		numCorrect += UTILS.checkBoolean(true, fmtest.getLocalAlphabet().equals("β"), 
				"Error: the local alphabet should be 'β' but instead it is '"+fmtest.getLocalAlphabet()+"'") ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(true, fmtest.has_alpha_specs(),
				"Error: system believes there are no alpha specs, but there is one.") ? 1 : 0 ; 
		char fua = fmtest.first_unset_alpha(); 
		numCorrect += UTILS.checkBoolean(true, fua == 'β',
				"Error: first unset alpha should be 'β', but it is '"+fua+"'") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(false, fmtest.has_multifeat_alpha(), 
				"Error: system detects an alpha variable specified for multiple features, but there is none") ? 1 : 0; 
		
		//testing whether featVect is stored properly in the FeatMatrix object instance 
		String corrFeatVect = ""; 
		for(int i = 0; i < featsByIndex.length; i++)	corrFeatVect += "1";
		int hi_loc = featIndices.get("hi"), tense_loc = featIndices.get("tense"); 
		corrFeatVect = corrFeatVect.substring(0, hi_loc) + "β" + corrFeatVect.substring(hi_loc+1); 
		corrFeatVect = corrFeatVect.substring(0, tense_loc) + "0" + corrFeatVect.substring(tense_loc+1);
		prevFeatVect = fmtest.getFeatVect(); 
		numCorrect += UTILS.checkBoolean(true, corrFeatVect.equals(prevFeatVect), 
				"Error: the feature vector should be\n"+corrFeatVect+"\nbut it is\n"+fmtest.getFeatVect()) ? 1 : 0 ; 
		
		SequentialPhonic dummyPhone = testFactory.parseSeqPh("m"); // which is -hi, 0tense.
		String initSpecs = ""+fmtest;
		HashMap<String, String> alph_feats_extrd = fmtest.extractAndApplyAlphaValues(dummyPhone); 
		n_feats_extracted = alph_feats_extrd.keySet().size(); 
		numCorrect += UTILS.checkBoolean(true, 
				n_feats_extracted == 0, 
				"Error: there should be zero features extracted from ["+dummyPhone.print()+"] since tense is not specified for consonantals, "
				+ "but "+n_feats_extracted+" were extracted!" ) ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, 
				prevFeatVect.equals(fmtest.getFeatVect()),
				"Error: the feat vect should have been unchanged but it has changed from\n"+prevFeatVect+"\nto\n"+fmtest.getFeatVect()) 
				? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, initSpecs.equals(""+fmtest), 
				"Error: feat specs should have been unchanged but it was changed from\n"+initSpecs+"\nto\n"+fmtest) ? 1 : 0; 
		
		dummyPhone = testFactory.parseSeqPh("x"); //+hi, 0tense
		alph_feats_extrd = fmtest.extractAndApplyAlphaValues(dummyPhone); 
		n_feats_extracted = alph_feats_extrd.keySet().size(); 
		numCorrect += UTILS.checkBoolean(true, 
				n_feats_extracted == 0, 
				"Error: there should be zero features extracted from ["+dummyPhone.print()+"] since tense is not specified for consonantals, "
				+ "but "+n_feats_extracted+" were extracted!" ) ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, 
				prevFeatVect.equals(fmtest.getFeatVect()),
				"Error: the feat vect should have been unchanged but it has changed from\n"+prevFeatVect+"\nto\n"+fmtest.getFeatVect()) 
				? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, initSpecs.equals(""+fmtest), 
				"Error: feat specs should have been unchanged but it was changed from\n"+initSpecs+"\nto\n"+fmtest) ? 1 : 0; 
		
		dummyPhone = testFactory.parseSeqPh("y"); //+hi, 0tense
		alph_feats_extrd = fmtest.extractAndApplyAlphaValues(dummyPhone); 
		n_feats_extracted = alph_feats_extrd.keySet().size(); 
		numCorrect += UTILS.checkBoolean(true, 
				n_feats_extracted == 0, 
				"Error: there should be zero features extracted from [y] since [y] is tense, "
				+ "but "+n_feats_extracted+" were extracted!" ) ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, 
				prevFeatVect.equals(fmtest.getFeatVect()),
				"Error: the feat vect should have been unchanged but it has changed from\n"+prevFeatVect+"\nto\n"+fmtest.getFeatVect()) 
				? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, initSpecs.equals(""+fmtest), 
				"Error: feat specs should have been unchanged but it was changed from\n"+initSpecs+"\nto\n"+fmtest) ? 1 : 0; 
		
		alph_feats_extrd = fmtest.extractAndApplyAlphaValues(e_tense); 
		n_feats_extracted = alph_feats_extrd.keySet().size(); 
		numCorrect += UTILS.checkBoolean(true, 
				n_feats_extracted == 0, 
				"Error: there should be zero features extracted from [e] since [e] is tense, "
				+ "but "+n_feats_extracted+" were extracted!" ) ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, 
				prevFeatVect.equals(fmtest.getFeatVect()),
				"Error: the feat vect should have been unchanged but it has changed from\n"+prevFeatVect+"\nto\n"+fmtest.getFeatVect()) 
				? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, initSpecs.equals(""+fmtest), 
				"Error: feat specs should have been unchanged but it was changed from\n"+initSpecs+"\nto\n"+fmtest) ? 1 : 0; 
		
		numCorrect += UTILS.checkBoolean(true, fmtest.first_unset_alpha() == 'β', 
				"Error: first_unset_alpha() should return 'β' (it still isn't set),"
				+ " but instead we get "+fmtest.first_unset_alpha()) ? 1 : 0 ;
		
		dummyPhone = testFactory.parseSeqPh("ʌ"); //-hi, -tense 
		alph_feats_extrd = fmtest.extractAndApplyAlphaValues(dummyPhone); 
		n_feats_extracted = alph_feats_extrd.keySet().size(); 
		numCorrect += UTILS.checkBoolean(true, 
				n_feats_extracted == 1, 
				"Error: there should be one feature extracted from ["+dummyPhone.print()+"] as it is [-tense] and [-hi], "
				+ "but "+n_feats_extracted+" features were extracted!" ) ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, fmtest.first_unset_alpha() == '0', 
				"Error: first_unset_alpha() should return '0' (we just set the only unset one),"
				+ " but instead we get "+fmtest.first_unset_alpha()) ? 1 : 0 ;
		 
		numCorrect += UTILS.checkBoolean(false, prevFeatVect.equals(fmtest.getFeatVect()), 
				"Error: the feature vector should have changed after alpha value extraction, yet it hasn't!") ? 1 : 0 ; 
		corrFeatVect = corrFeatVect.substring(0, hi_loc) + "0" // because wedge is not hi
				+ corrFeatVect.substring(hi_loc+1); 
		numCorrect += UTILS.checkBoolean(true, corrFeatVect.equals(fmtest.getFeatVect()), 
				"Error: the feature vector should be\n"+corrFeatVect+"\nbut it is\n"+fmtest.getFeatVect()) ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(false, initSpecs.equals(""+fmtest), 
				"Error: feat specs has erroneously not been changed after alpha value extraction!") ? 1 : 0; 
		
		String corrSpecs = ""+initSpecs; corrSpecs = corrSpecs.replace('β', '-'); 
		numCorrect += UTILS.checkBoolean(true, corrSpecs.equals(""+fmtest), 
				"Error: feat specs should be\n"+corrSpecs+"\nbut it is\n"+fmtest) ? 1 : 0; 
		
		//testing the compare methods now that the feature is extracted
		numCorrect += UTILS.checkBoolean(false, fmtest.compare(e_tense), 
				"Error: [e] should be false for [-tense,(β=-)hi].compare() but it is trueǃ") ? 1 : 0 ;
		numCorrect += UTILS.checkBoolean(false, fmtest.compare(testFactory.parseSeqPh("h")), 
				"Error: [h] should be false for [-tense,(β=-)hi].compare() but it is trueǃ") ? 1 : 0 ;
		numCorrect += UTILS.checkBoolean(false, fmtest.compare(testFactory.parseSeqPh("k")), 
				"Error: [k] should be false for [-tense,(β=-)hi].compare() but it is trueǃ") ? 1 : 0 ;
		numCorrect += UTILS.checkBoolean(false, fmtest.compare(testFactory.parseSeqPh("u")), 
				"Error: [u] should be false for [-tense,(β=-)hi].compare() but it is trueǃ") ? 1 : 0 ;
		numCorrect += UTILS.checkBoolean(true, fmtest.compare(testFactory.parseSeqPh("æ")), 
				"Error: [æ] should be true for [-tense,(β=-)hi].compare() but it is falseǃ") ? 1 : 0 ;
		numCorrect += UTILS.checkBoolean(true, fmtest.compare(testFactory.parseSeqPh("ɔ")), 
				"Error: [ɔ] should be true for [-tense,(β=-)hi].compare() but it is falseǃ") ? 1 : 0 ;
		
		//checking application of these feature values to different matrices 
		// i.e. that the alph_feats_extrd apply to another FeatMatrix well via its own applyAlphaFeats method
		// application to a FeatMatrix instance without any alpha values isn't necessary as this is caught at the beginning of the method applyAlphaValues
			// and triggers the end of the method with a blank return statement. 
		
		
		// testing application to a FeatMatrix with +tense (vs. -tense as fmtest currently has), +front, and β hi
		FeatMatrix dummyFM = newFM("+tense,+front,βhi"); 
		String dfm_og_vect = dummyFM.getFeatVect(), dfm_og_specs = ""+dummyFM; 
		dummyFM.applyAlphaValues(alph_feats_extrd);
		
		numCorrect += UTILS.checkBoolean(true, dummyFM.first_unset_alpha() == '0', 
				"Error: after application of alpha values to only alpha value, it erroneously does not count as unset") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(false, 
				dfm_og_vect.equals(dummyFM.getFeatVect()), 
				"Error: feature vector remained unchanged after application of alpha values.") ?  1 : 0; 
		String corr_dfm_vect = ""+dfm_og_vect; 
		
		corr_dfm_vect = corr_dfm_vect.substring(0, hi_loc) + "0" + corr_dfm_vect.substring(hi_loc+1); 
		
		numCorrect += UTILS.checkBoolean(true, corr_dfm_vect.equals(dummyFM.getFeatVect()), 
				"Error: the feature vector after alpha feature filling should be\n"+corr_dfm_vect+
				"\nbut it is\n"+dummyFM.getFeatVect()) ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(false, dfm_og_specs.equals(""+dummyFM), 
				"Error: feature specs remained unchanged after application of alpha values.") ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(true, dummyFM.toString().equals(""+(newFM("+tense,+front,-hi"))), 
				"Error: feature specs should be [+tense,+front,-hi], but it is "+dummyFM) ? 1 : 0 ; 
				
		// check for application to case where ɣ is specified for [hi] and it is matchingly [-tense]
		// applyAlphaValues should not fill any alpha values, as they're different ones... test here is to see if this is truly the case. 
		dummyFM = newFM("-tense,ɣhi"); 
		dfm_og_vect = ""+dummyFM.getFeatVect(); dfm_og_specs = ""+dummyFM; 
		dummyFM.applyAlphaValues(alph_feats_extrd);
		
		numCorrect += UTILS.checkBoolean(true, dummyFM.first_unset_alpha() == 'ɣ', 
				"Error: after application of alpha value β to a FeatMatrix with only ɣ as an alpha value, β"
				+ " is now erroneously detected as set, or detected as erroneously set") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, dfm_og_specs.equals(""+dummyFM), 
				"Error: feature specs should be unchanged after attempt to fill value for the wrong alpha symbol, yet it has changed from\n"
				+ dfm_og_specs +"\nto: "+dummyFM) ? 1 : 0 ;
		numCorrect += UTILS.checkBoolean(true, dfm_og_vect.equals(dummyFM.getFeatVect()), 
				"Error: feature vector should be unchanged after attempt to fill value for the wrong alpha symbol, yet it has changed from\n"
				+ dfm_og_vect +"\nto: "+dummyFM.getFeatVect()) ? 1 : 0 ;
		
		
		
		// one with β for tense, which will show handling of downstream feature implications 
				
				
		// one with β for hi, and ɑ for tense... 
				
		// one with β for hi and for nas
				
		
		/*
		 * now checking force truth methods 
		 *for now, not dealing with implications not already stored -- for example, that fmtest forcing a value on something should 
		* make it non-consonantal, given that it is specified for tense
		* those should really be given to the FeatMatrix constructor
		* and in practice, it is done when the FeatMatrix constructor is made via SChangeFactory
		* ... though (TODO) it might be a good idea to check this. 
		* 
		* anyhow, recall that fmtest should now be [-tense, [β=-]hi]
		*/ 
		SequentialPhonic dummyPhone2 = testFactory.parseSeqPh("y"); //initially +tense, +hi
		Phone modDP2 = fmtest.forceTruth(new Phone(dummyPhone2)); 
		String correct_modified_dp2_str = dummyPhone2.getFeatString();
		correct_modified_dp2_str = correct_modified_dp2_str.substring(0,hi_loc) + "0" + correct_modified_dp2_str.substring(hi_loc+1); 
		correct_modified_dp2_str = "œ:"+correct_modified_dp2_str.substring(0,tense_loc) + "0" + correct_modified_dp2_str.substring(tense_loc+1); 
		
		numCorrect += UTILS.checkBoolean(false, (""+modDP2).equals(""+dummyPhone2), 
				"Error: FeatMatrix.forceTruth() does not effect any change upon a valid phone to operate on!") ? 1 :0; 
		
		numCorrect += UTILS.checkBoolean(true, correct_modified_dp2_str.equals(""+modDP2), 
				"Error: ["+dummyPhone2.print()+"], after modification by FeatMatrix "+fmtest+", should become\n"
						+ correct_modified_dp2_str+"\n but instead it is\n"+modDP2) ? 1 : 0 ;

		dummyPhone2 = testFactory.parseSeqPh("ə"); //initially 0tense, -hi
		modDP2 = fmtest.forceTruth(new Phone(dummyPhone2)); 
		correct_modified_dp2_str = dummyPhone2.getFeatString();
		correct_modified_dp2_str = correct_modified_dp2_str.substring(0,hi_loc) + "0" + correct_modified_dp2_str.substring(hi_loc+1); 
		correct_modified_dp2_str = "ɜ:"+correct_modified_dp2_str.substring(0,tense_loc) + "0" + correct_modified_dp2_str.substring(tense_loc+1); 
		
		numCorrect += UTILS.checkBoolean(false, (""+modDP2).equals(""+dummyPhone2), 
				"Error: FeatMatrix.forceTruth() does not effect any change upon a valid phone to operate on!") ? 1 :0; 
		
		numCorrect += UTILS.checkBoolean(true, correct_modified_dp2_str.equals(""+modDP2), 
				"Error: ["+dummyPhone2.print()+"], after modification by FeatMatrix "+fmtest+", should become\n"
						+ correct_modified_dp2_str+"\n but instead it is\n"+modDP2) ? 1 : 0 ;

		dummyPhone2 = testFactory.parseSeqPh("ʊ"); //initially -tense, +hi
		modDP2 = fmtest.forceTruth(new Phone(dummyPhone2)); 
		correct_modified_dp2_str = dummyPhone2.getFeatString();
		correct_modified_dp2_str = correct_modified_dp2_str.substring(0,hi_loc) + "0" + correct_modified_dp2_str.substring(hi_loc+1); 
		correct_modified_dp2_str = "ɔ:"+correct_modified_dp2_str.substring(0,tense_loc) + "0" + correct_modified_dp2_str.substring(tense_loc+1); 
		
		numCorrect += UTILS.checkBoolean(false, (""+modDP2).equals(""+dummyPhone2), 
				"Error: FeatMatrix.forceTruth() does not effect any change upon a valid phone to operate on!") ? 1 :0; 
		
		numCorrect += UTILS.checkBoolean(true, correct_modified_dp2_str.equals(""+modDP2), 
				"Error: ["+dummyPhone2.print()+"], after modification by FeatMatrix "+fmtest+", should become\n"
						+ correct_modified_dp2_str+"\n but instead it is\n"+modDP2) ? 1 : 0 ;

		dummyPhone2 = testFactory.parseSeqPh("ˈʌ"); //initially -tense, -hi
		modDP2 = fmtest.forceTruth(new Phone(dummyPhone2)); 
		correct_modified_dp2_str = ""+dummyPhone2;
		numCorrect += UTILS.checkBoolean(true, (""+modDP2).equals(""+dummyPhone2), 
				"Error: FeatMatrix.forceTruth() should not effect any change upon a phone that already adheres to its stipulations, yet it does!") ? 1 :0; 

		//testing with the List<SequentialPhonic> version of forceTruth()
		List<SequentialPhonic> dummyList = testFactory.parseSeqPhSeg("ø ˈɯ"); 
		// correct_modified_dp2_str will not change, as it should become [ˈʌ] 
		
		List<SequentialPhonic> modDummyList = fmtest.forceTruth(dummyList, 1); 
		
		numCorrect += UTILS.checkBoolean(true, dummyList.get(0).equals(modDummyList.get(0)), 
				"Error: FeatMatrix.forceTruth(List<SequentialPhonic>) seems to have changed a phone at the wrong index!") ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(false, dummyList.get(1).equals(modDummyList.get(1)), 
				"Error: FeatMatrix.forceTruth(List<SequentialPhonic>) does not effect any change upon a valid phone to operate on, "
				+ "or failed to access the index of the list!") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, correct_modified_dp2_str.equals(""+modDummyList.get(1)), 
				"Error: ["+dummyList.get(1).print()+"], after forceTruth() by FeatMatrix "+fmtest+", should become\n"
						+ correct_modified_dp2_str+"\n but instead it is\n"+modDP2) ? 1 : 0 ;
		
		System.out.println("Done testing alpha comprehension in this mode. Got "+numCorrect+" correct "
				+ "out of 48"); 
		
		//TODO finish testing here... 
		
		System.out.println("\nNow for a feat matrix with one alpha value, with a redundant feature implication...");
		fmtest = new FeatMatrix("ɑstres,+syl",Arrays.asList(featsByIndex),featImplications); 

		numCorrect += UTILS.checkBoolean(true, fmtest.has_alpha_specs(),
				"Error: system believes there are no alpha specs, but there is one.") ? 1 : 0 ; 
		fua = fmtest.first_unset_alpha(); 
		numCorrect += UTILS.checkBoolean(true, fua == 'ɑ',
				"Error: first unset alpha should be 'ɑ', but it is '"+fua+"'") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(false, fmtest.has_multifeat_alpha(), 
				"Error: system detects an alpha variable specified for multiple features, but there is none") ? 1 : 0; 
		
		List<SequentialPhonic> actOn = testFactory.parseSeqPhSeg("ˈo");
		
		boolean caught = false; 
		try {	fmtest.forceTruth(actOn,0); 		}
		catch(UnsetAlphaError e)	{	caught = true;		}
		numCorrect += UTILS.checkBoolean(true, caught, "Error: Trying to forceTruth"
				+ " without initializing the alpha value should cause an UnsetAlphaError,"
				+ " but none was detected!") ? 1 : 0;
		
		HashMap<String,String> toApply = fmtest.extractAndApplyAlphaValues(e_tense); 
		fmtest.applyAlphaValues(toApply);
		
		numCorrect += UTILS.checkBoolean(true, fmtest.has_alpha_specs(),
				"Error: system believes there are no alpha specs, but there is one.") ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(true, fmtest.first_unset_alpha() == '0', 
				"Error: first_unset_alpha() should return '0' (we just set the last unset one),"
				+ " but instead we get "+fmtest.first_unset_alpha()) ? 1 : 0 ;
		numCorrect += UTILS.checkBoolean(false, fmtest.has_multifeat_alpha(), 
				"Error: system detects a multispecified alpha variable where there is none") ? 1 : 0; 
		//System.out.println("Action on "+actOn.get(0)+" ...\n\t"+fmtest.forceTruth(actOn,0).get(0));
		List<SequentialPhonic> result = fmtest.forceTruth(actOn,0); 
		
		numCorrect += UTILS.checkBoolean(true,
				UTILS.phonSeqsEqual(result, testFactory.parseSeqPhSeg("o")),
				"Error: result of fm.forceTruth using "+fmtest+" with ɑ set to (-) should be 'o' but it is "
						+ UTILS.printWord(result)) ? 1 : 0; 
	}

	private static String generateErrorMessage(SChange sc, List<SequentialPhonic> input,
			List<SequentialPhonic> expected, List<SequentialPhonic> observed) {
		return "Error in realization of this rule:\t\t" + sc + "\n\tInput was:\t" + UTILS.printWord(input)
				+ "\n\tExpected result: " + UTILS.printWord(expected) + "\n\tObserved result:\t\t" + UTILS.printWord(observed)
				+ "\n";
	}

	/**
	 * @param sc: sound change being tested
	 * @param inp: input
	 * @param exp: expected output
	 * @return whether expected output was produced (true, false)
	 */
	private static boolean runTest(SChange sc, List<SequentialPhonic> inp, List<SequentialPhonic> exp) {
		List<SequentialPhonic> obs = sc.realize(inp);
		if (UTILS.phonSeqsEqual(exp, obs))
			return true;
		System.out.print(generateErrorMessage(sc, inp, exp, obs));
		return false;
	}
	
	private static FeatMatrix newFM(String specs)
	{
		return new FeatMatrix(specs, Arrays.asList(featsByIndex),featImplications);
	}

}
