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
		
		scfTest = new SChangeFeat(new FeatMatrix("+syl", featIndices), new NullPhone()); 
		scfTest.setPriorContext(testFactory.parseNewContext("+syl", boundsMatter));
		System.out.println("Now testing : "+scfTest);
		testWord = scfTest.realize(testFactory.parseSeqPhSeg("r e a l e a"));
		System.out.println("Result should be r e l e   :    "+printWord(testWord));
		
		scfTest = new SChangeFeat(featIndices, "-cont,-nas,-lat,-delrel","-voi"); 
		System.out.println("Now testing : "+scfTest);
		testWord = scfTest.realize(testFactory.parseSeqPhSeg("d i d e ð l a d d o n u r")); 
		System.out.println("Output should be t i t e ð l a t t o n u r : "+printWord(testWord));
		
		scfTest = new SChangeFeat(new FeatMatrix("-cont,-nas,-lat,-delrel", featIndices),
				new Phone(phoneSymbToFeatsMap.get("q"), featIndices, phoneSymbToFeatsMap));
		System.out.println("Now testing : "+scfTest);
		testWord = scfTest.realize(testFactory.parseSeqPhSeg("d i d e ð l a d d o n u r")); 
		System.out.println("Output should be q i q e ð l a q q o n u r : "+printWord(testWord));
		
		scfTest = new SChangeFeat(featIndices, "-cont", "+nas,+son,.delrel,+cont",
				testFactory.parseNewContext("+nas,-syl", false), testFactory.parseNewContext("+syl", false));
		System.out.println("Now testing : "+scfTest);
		testWord = scfTest.realize(testFactory.parseSeqPhSeg("b i m b d e n d o")); 
		System.out.println("Output should be b i m b d e n n o : "+printWord(testWord));
		
		System.out.println("Done testing SChangeFeat. Note that testing context processing should be "
				+ "done in SChangeContextTester, \n...while this class also doesn't handle the effect of an entered "
				+ "feature implications file.");
		
		System.out.println("Now testing SChangeFeatToPhone");
		
		SChangeFeatToPhone scftpTest = new SChangeFeatToPhone(featIndices,
				testFactory.parseRestrictPhoneSequence("l"+PH_DELIM+"[+hi,+front]"),
				testFactory.parsePhoneSequenceForDest("ʎ"));  
		System.out.println("Now testing : "+scftpTest);
		testWord = scftpTest.realize(testFactory.parseSeqPhSeg("a l j a"));
		System.out.println("Output should be a ʎ a :  "+printWord(testWord));
		testWord = scftpTest.realize(testFactory.parseSeqPhSeg("a l i e l j")); 
		System.out.println("Output should be a ʎ e ʎ : "+printWord(testWord));
		
		scftpTest = new SChangeFeatToPhone(featIndices, 
				testFactory.parseRestrictPhoneSequence("[+hi,+front,-syl]"), 
				testFactory.parsePhoneSequenceForDest("j ɟ ʝ")); 
		System.out.println("Now testing : " + scftpTest);
		testWord = scftpTest.realize(testFactory.parseSeqPhSeg("a c i a j o")); 
		System.out.println("Output should be :  a j ɟ ʝ i a j ɟ ʝ o : "+ printWord(testWord));
		
		scftpTest = new SChangeFeatToPhone(featIndices, 
				testFactory.parseRestrictPhoneSequence("[-cons,+front,+hi] [+syl,+back] [-cont,+hi]"),
				testFactory.parsePhoneSequenceForDest("ʝ o w j")); 
		scftpTest.setPostContext(testFactory.parseNewContext("+syl", false));
		System.out.println("Now testing : "+scftpTest);
		testWord = scftpTest.realize(testFactory.parseSeqPhSeg("j o c o k a"));
		System.out.println("Output should be ʝ o w j o k a : "+printWord(testWord));

		scftpTest = new SChangeFeatToPhone(featIndices,
				testFactory.parseRestrictPhoneSequence("[-cont,+cor,-voi] # j [+syl]"), 
				testFactory.parsePhoneSequenceForDest("t͡ʃ j ə")); 
		System.out.println("Now testing : "+scftpTest);
		scftpTest.setPostContext(testFactory.parseNewContext("#", true));
		System.out.println("Output should be # ɡ `ɑ t͡ʃ j ə # : " 
				+printWord(scftpTest.realize(testFactory.parseSeqPhSeg("# ɡ `ɑ t # j 'u"))));
		System.out.println("Output should be # kʰ `ɛ t͡ʃ j ə # : "
				+printWord(scftpTest.realize(testFactory.parseSeqPhSeg("# kʰ `ɛ t͡ʃ # j 'u #"))));
		System.out.println("Output should be # ɡ `ɑ t # j `æ̃ː m z # : "
				+ printWord(scftpTest.realize(testFactory.parseSeqPhSeg("# ɡ `ɑ t # j `æ̃ː m z #"))));
		
		scftpTest = new SChangeFeatToPhone(featIndices,
				testFactory.parseRestrictPhoneSequence("[-cont,+cor] # [+syl,-prim]"),
				testFactory.parsePhoneSequenceForDest("ɾ ə"));
		System.out.println("Now testing : "+scftpTest);
		System.out.println("Output should be 'forgetaboutit', #f ə ɡ `ɛ ɾ ə b 'a w ɾ ə t# : \n"
				+ printWord(scftpTest.realize(testFactory.parseSeqPhSeg("# f ə ɡ `ɛ t # ə b 'a w t # ɪ t #"))));
		
		System.out.println("Done testing SChangeFeatToPhone. Now testing SChangePhone"); 
		
		SChangePhone scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs("h"), testFactory.parseSeqPhDisjunctSegs(""));
		System.out.println("Now testing : "+scpTest);
		System.out.println("Output should be a m e a m e a : "+
				printWord(scpTest.realize(testFactory.parseSeqPhSeg("h a m e h a m e h a h"))));
		
		
		scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs("a j ; eː ; iː"),
				testFactory.parseSeqPhDisjunctSegs("e j ; iː ; a j")); 
		System.out.println("Now testing : "+scpTest);
		System.out.println("Output should be : l iː w e j : \t"
				+ printWord(scpTest.realize(testFactory.parseSeqPhSeg("l eː w a j"))));
		System.out.println("Output should be tʰ iː tʰ a j m : \t"
				+ printWord(scpTest.realize(testFactory.parseSeqPhSeg("tʰ eː tʰ iː m"))));
		
		scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs("l j"), testFactory.parseSeqPhDisjunctSegs("ʎ")); 
		System.out.println("Now testing : "+scpTest);
		System.out.println("Output should be ʎ u ʎ : \t "
				+printWord(scpTest.realize(testFactory.parseSeqPhSeg("l j u l j"))));
		
		scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs(""), testFactory.parseSeqPhDisjunctSegs("i"),
				testFactory.parseNewContext("#", true), testFactory.parseNewContext("s [+cons]", true));
		System.out.println("Now testing : "+scpTest);
		System.out.println("Output should be # i s p a t a # :\t"
				+ printWord(scpTest.realize(testFactory.parseSeqPhSeg("# s p a t a #"))));
		System.out.println("Output should be s p a t a # :\t "
				+ printWord(scpTest.realize(testFactory.parseSeqPhSeg("s p a t a #"))));
		System.out.println("Output should be # s j a t a # : \t "
				+ printWord(scpTest.realize(testFactory.parseSeqPhSeg("# s j a t a #"))));
		
		scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs("{p;b;k;ɡ}"),
				new ArrayList<RestrictPhone>(testFactory.parseRestrictPhoneSequence("+front,+hi,-back")));
		scpTest.setPostContext(testFactory.parseNewContext("#", true));
		System.out.println("Now testing : "+scpTest);
		System.out.println("Output should be # s a c # :\t"
				+ printWord(scpTest.realize(testFactory.parseSeqPhSeg("# s a k #"))));
		System.out.println("Output should be s a k :\t"
				+ printWord(scpTest.realize(testFactory.parseSeqPhSeg("s a k"))));
		System.out.println("Output should be # s u pʲ # :\t"
				+ printWord(scpTest.realize(testFactory.parseSeqPhSeg("# s u p #"))));
						
		scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs("t u"),
				testFactory.parseSeqPhDisjunctSegs("n ə"),
				testFactory.parseNewContext("[+nas] ([-cont,-delrel]) #", true),
				testFactory.parseNewContext("#", true));
		System.out.println("Now testing : "+scpTest);
		System.out.println("Output should be # ɡ o w ɪ ŋ # n ə #:\t"
				+ printWord(scpTest.realize(testFactory.parseSeqPhSeg("# ɡ o w ɪ ŋ # t u #"))));
		System.out.println("Output should be # t u # w ɑ̃ t # n ə # :\t"
				+ printWord(scpTest.realize(testFactory.parseSeqPhSeg("# t u # w ɑ̃ t # t u #"))));
		System.out.println("Output should be # n o w ɪ ŋ # t u v ə n #:\t"
				+ printWord(scpTest.realize(testFactory.parseSeqPhSeg("# n o w ɪ ŋ # t u v ə n #"))));
		
		scpTest = new SChangePhone(
				testFactory.parseSeqPhDisjunctSegs("{b;ɡ}"),
				new ArrayList<RestrictPhone>( 
						testFactory.parseRestrictPhoneSequence("[+cont]", true)));
		scpTest.setPriorContext(testFactory.parseNewContext("[+syl] (#)", true));
		System.out.println("Now testing : "+scpTest);
		System.out.println("output should be # a ɣ a r a # :\t"
				+printWord(scpTest.realize(testFactory.parseSeqPhSeg("# a ɡ a r a #"))));
		System.out.println("output should be # a # β i t u # :\t"
				+printWord(scpTest.realize(testFactory.parseSeqPhSeg("# a # b i t u #"))));
		System.out.println("output should be # b i t u # :\t"
				+ printWord(scpTest.realize(testFactory.parseSeqPhSeg("# b i t u #"))));
		System.out.println("output should be # k o m # b i t u # : \t"
				+ printWord(scpTest.realize(testFactory.parseSeqPhSeg("# k o m b i t u #"))));
	}
	
	private static String printWord(List<SequentialPhonic> word)
	{
		String output = ""; 
		for (SequentialPhonic ph : word)
			output+=ph.print()+PH_DELIM; 
		return output.substring(0, output.lastIndexOf(PH_DELIM));
	}
}
