import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class UTILS {

	public final static char MARK_POS = '+', MARK_NEG = '-', MARK_UNSPEC = '0', FEAT_DELIM = ','; 
	public final static int POS_INT = 2, NEG_INT = 0, UNSPEC_INT = 1, DESPEC_INT = 9; 
	public final static char UNSPEC_INT_CHAR = (""+UNSPEC_INT).charAt(0), DESPEC_INT_CHAR = (""+DESPEC_INT).charAt(0); 
	public final static String POLAR_FTVECT_INTS = ("" + POS_INT) + NEG_INT,
								POLAR_FTSPEC_MARKS = "" + MARK_POS + MARK_NEG; 
	public final static String ALL_FTVECT_INTS = (POLAR_FTVECT_INTS + UNSPEC_INT) + DESPEC_INT,
							ALL_FTSPEC_MARKS = ""+MARK_POS+MARK_NEG+MARK_UNSPEC;

	public final static char IMPLICATION_DELIM=':', PH_DELIM = ' ', DIACRITICS_DELIM='='; 
	public static final char RESTR_DELIM =  ','; // delimits restrictions between features inside the specification
			// ... for a FeatMatrix : i.e. if "," then the FeatMatrix will be in phonological representation
			// ... as [+A,-B,+C]
	public final static char CMT_FLAG = '$'; //marks that the text after is a comment in the sound rules file, thus doesn't read the rest of the line
	public final static char GOLD_STAGENAME_FLAG = '~', BLACK_STAGENAME_FLAG ='=';
	public final static String NULL_STAGE_INDIC = "NULLSTAGE"; 
	public final static char STAGENAME_LOC_DELIM = ':'; 
	public final static char LEX_DELIM =','; 
	public final static char STAGE_PRINT_DELIM = ',';  
	public final static char DISJUNCT_DELIM = ';'; 
	public final static String OUT_GRAPH_FILE_TYPE = ".csv"; 
	public final static String ABSENT_INDIC = "...", ABSENT_REPR = "{ABSENT}"; 
	public final static String UNATTD_GOLD_INDIC = ">*", UNATTD_GOLD_REPR = "{UNATTESTED}"; 
		// the -INDIC items are the strings used in lexicon files provided by the user and processed by the system
		// whereas the -REPR items are the internal representation within the Etymon subclasses.
			// the latter are for unattested GOLD lexicon items -- i.e. those not included in diagnostic analysis
			// i.e. NOT unattested reconstructions!
	public final static List<String> PSEUDO_ETYM_REPRS = Arrays.asList(ABSENT_REPR, UNATTD_GOLD_REPR); 	
	public final static List<String> PSEUDO_ETYM_INDICS = Arrays.asList(ABSENT_INDIC, UNATTD_GOLD_INDIC); 
	public static boolean isPseudoEtymon (Etymon e)	{	return PSEUDO_ETYM_INDICS.contains(e+""); 	}
	public final static int maxAutoCommentWidth = 150;
	public static final int PRINTERVAL = 100; 
	
	//IPA symbol and feature related variables. 
	public static HashMap<String,String[]> DIACRIT_TO_FT_MAP; 
	public static HashMap<String,String[]> FT_IMPLICATIONS; 	
	
	public static Set<String> featsWithImplications; 
	public static List<String> ordFeatNames; 
	
	public static String[] featsByIndex; 
	public static HashMap<String, Integer> featIndices;
	public static boolean feats_weighted;
	public static double[] FT_WTS; 
	public static HashMap<String, String> phoneSymbToFeatsMap;
	public static HashMap<String, String> featsToSymbMap; 
	public static HashMap<String, List<String>> featsToPossibleDiacritics; 
	
	public static final String[] illegalForPhSymbs = new String[]{"[","]","{","}","__",":",",",";"," ","+","#","@","∅","$",">","/","~","0"};
	
	public static double ID_WT; 
	public static boolean contextualize_FED; 
	
	public static boolean symbsExtracted = false , no_feat_impls = false, 
			diacriticsExtracted = false, featImplsExtracted = false;
	
	public static boolean VERBOSE; 
	public static boolean USE_FORM_ID; 
	public static final char FORM_ID_FLAG = 'ɸ'; 
		// for flagging lexical or morphological info within lines of a lexicon file -- between phonological info and the comment
	
	
	public static final int MAX_DIACRIT = 4; 
	
	public static int NUM_UNDEFINED_PHONES_USED = 0; 
	
	/**
	 * given @param i, a feature int from a feat vector 
	 * @return the surface marking in the feat spec list that corresponds 
	 * @beware, @surjective for 1 and 9 which both go to '0' (UTILS.MARK_UNSPEC)
	 */
	public static char ftIntToMark(char i)
	{
		if (!ALL_FTVECT_INTS.contains(""+i))	throw new Error("Error: invalid specification value.");
		return Integer.parseInt(""+i) == DESPEC_INT ? MARK_UNSPEC : ALL_FTSPEC_MARKS.charAt(ALL_FTVECT_INTS.indexOf(i));
	}
	
	public static void abortInvalidFtIntStr(String ftInt)
	{	abortInvalidFtIntStr(ftInt,""); 	}
	public static void abortInvalidFtIntStr(String ftInt, String qualifier) 
	{	if (detectInvalidFtIntStr(ftInt))	
			throw new Error("Error: tried to get "+qualifier+"feature int of an illegitimate feature int: "+ftInt); 
	}
	
	public static boolean detectInvalidFtIntStr (String candFI) {
		return candFI.length() != 1 ? true : !ALL_FTVECT_INTS.contains(candFI); 
	}
	
	// this will work IFF the POS_INT is still 2 and NEG_INT is still 0. 
	// gives opposite integer if it's a polar int (2,0 -> 0,2) 
	// if 9 or 1, gives just that. Else throws error. 
	public static int getOppFtInt(String ftInt) {
		abortInvalidFtIntStr(ftInt, "opposite "); 	
		
		int ftInp = Integer.parseInt(ftInt);
		return POLAR_FTVECT_INTS.contains(""+ftInt) ? 
				POS_INT - ftInp : ftInp;
	}
	
	public static boolean etymonIsPresent (Etymon etym)	
	{	return !PSEUDO_ETYM_REPRS.contains(etym.print()); 	}
	
	public static char getFeatspecMarkFromInt (int ftInt)
	{
		if (ftInt == POS_INT)	return MARK_POS;
		if (ftInt == NEG_INT)	return MARK_NEG;
		
		if (ftInt != UNSPEC_INT)
			throw new RuntimeException("Error -- invalid feature int used: "+ftInt); 
		
		return MARK_UNSPEC; 
	}
	
	public static int getFeatspecIntFromMark (char mark) 
	{
		if (!ALL_FTSPEC_MARKS.contains(""+mark))
			throw new RuntimeException("Invalid feature specification mark :"+mark+"; allowed marks are :"+ALL_FTSPEC_MARKS); 
		if (mark == MARK_POS)	return POS_INT; 
		if (mark == MARK_NEG)	return NEG_INT; 
		return UNSPEC_INT; // this last line shouldn't ever really happen. Diacritics should not be used to unspecify features. 
	}
	
	// recent auxiliary as of summer 2023 
	public static String getKeyFromValue(HashMap<String,String> map, String val)
	{
		for (String k : map.keySet()) {
	        if (val.equals(map.get(k))) {
	            return k;
	        }
	    }
	    return null;
	}

	public static String fillSpaceToN(String inp, int n)
	{
		String out = inp+"";
		while (out.length() < n)	out += " ";
		return out; 
	}
	
	//to use for checking if an entered etymon or rule id is valid. 
	// max should be the number of words in the lexicon minus 1 (for an etymon)
		// or the length of the cascade (for a rule)
	// input argument is (String form of) the index (String because pulled from cascade files, or etc.)
		// @return either (int) form of input, if it's valid, or -1 if it isn't (for usage downstream)
	public static int getValidInd(String s, int max)
	{
		int output; 
		try 		{	output = Integer.parseInt(s);	} 
		catch (NumberFormatException | NullPointerException nfe) {
	        return -1;	}
		return output <= max ? output : -1; 
	}

	public static String etymInds(Etymon[] etList, Etymon etTarg)
	{
		String output = ""; 
		for (int wli = 0; wli < etList.length; wli++)
			if(etList[wli].toString().equals(etTarg.toString()))
				output += output.equals("") ? ""+wli : ", "+wli;
		return output;
	}
	
	public static Simulation toyDerivation(Simulation ogs, List<SChange> jur)
	{
		Simulation toy = new Simulation(ogs.getInput().getWordList(), jur, ogs.getStagesOrdered()); 
		if (ogs.hasBlackStages())	toy.setBlackStages(ogs.getBlackStageNames(), ogs.getBlackStageInstants());
		if (ogs.hasGoldOutput()) toy.setGoldOutput(ogs.getGoldOutput().getWordList()); 
		if (ogs.hasColumnedStages()) 
		{
			int[] blackStageColumnedIndices = new int[ogs.hasBlackStages() ? ogs.NUM_BLACK_STAGES(): 0]; 
			if (ogs.hasBlackStages())
				for (int bsi = 0 ; bsi < ogs.NUM_BLACK_STAGES() ; bsi++)
					blackStageColumnedIndices[bsi] = ogs.columnedBlackStageBlackIndices.contains(bsi) ? bsi : -1; 
			toy.setColumnedStages(ogs.getColumnStageForms(), ogs.getColumnedStageNames(), ogs.getColumnedStageInstants(), blackStageColumnedIndices);
		}
			
		toy.setOpacity(true);
		toy.setStepPrinterval(PRINTERVAL); 
		toy.simulateToEnd();
		return toy; 
	}
	
	//auxiliary
	public static void writeToFile(String filename, String output, boolean print)
	{	try 
		{	
			int dirBreak = filename.indexOf("/");

			while (dirBreak != -1)
			{
				String curDir = filename.substring(0, dirBreak),
						rem = filename.substring(dirBreak+1); 
				
				if (!new File(curDir).exists()) 
					new File(curDir).mkdirs(); 
			
				dirBreak = !rem.contains("/") ? -1 : 
					dirBreak + 1 + rem.indexOf("/"); 			
			}
			
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), StandardCharsets.UTF_8)); 
					
			out.write(output);
			out.close();
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
	}
	

	//auxiliary method -- check if the line consists of only spaces. 
	public static boolean isJustSpace(String line)
	{	return line.replace(" ","").length() == 0;	}

	//auxiliary method -- get number of columns in lexicon file. 
	public static int countColumns(String row)
	{
		String proxy = row+"";
		if (proxy.contains(CMT_FLAG+""))
			proxy = proxy.substring(0, proxy.indexOf(CMT_FLAG+""));
		int i = proxy.indexOf(""+LEX_DELIM), c = 1 ;
		while( i > -1)
		{
			c++; 
			proxy = proxy.substring(i+1);
			i = proxy.indexOf(","); 
		}
		return c; 
	}
	
	public static String printParenMap(SequentialFilter testCont)
	{
		String output = ""; 
		String[] pm = testCont.getParenMap();
		for(String p : pm)	output += p + " "; 
		return output.trim();
	}
	
	public static String printWord(List<SequentialPhonic> word) {
		String output = "";
		for (SequentialPhonic ph : word)
			output += ph.print();
		return output;
	}
	
	public static String stdCols(int width, int[] vals)
	{
		String[] strvals = new String[vals.length] ;
		for (int vi = 0; vi < vals.length; vi++)	strvals[vi] = ""+vals[vi];
		return stdCols(width, strvals); 
	}
	
	// produce columns of a certain width (= first input argument)  
	public static String stdCols(int width, String[] vals)
	{
		String out = ""; 
		for (String val : vals)	out += "| "+fillSpaceToN(val,width);
		return out.trim();
	}
	
	public static String stdMetricHeader()
	{
		return fillSpaceToN("Stage", 24) + stdCols(24, new String[] {"Accuracy","Accuracy within 1 ph","Accuracy within 2 phs","Average PED","Average FED"});
	}
	
	public static String stdMetricReport(ErrorAnalysis ea)
	{
		return stdCols( 24, new String[] {
			""+ea.getAccuracy(), ""+ea.getPctWithin1(), ""+ea.getPctWithin2(),
			""+ea.getAvgPED(), ""+ea.getAvgFED() }); 
	}
	
	// extracts in from a line in derivation. 
	public static int extractInd(String dl)
	{
		if (dl.indexOf(" | ") == -1 || dl.indexOf(" : ") == -1)	return -1; 
		
		return Integer.parseInt( dl.substring( dl.indexOf(" | ") + 3, dl.lastIndexOf(" :")).trim());
	}
	
	// auxiliary for String[] objects where etyma with different effects between alt-hyp and baseline 
		// are stored in the cell of etymon index and others are empty
	// functions to count how many etyma have been effected. 
	public static int numFilled (String[] effs)
	{
		int c = 0; 
		for (String eff : effs)	c += (eff != null) ? 1 : 0; 
		return c;
	}
	
	
	public static boolean cmtIsStandardized(String cmt)
	{
		if(cmt.charAt(0) != CMT_FLAG)	return false; 
		
		if (!cmt.contains("\n"))
			return true; // since we now know it must start with the proper character
		String[] spl = cmt.split("\n"); 
		if(spl[0].length() > DHSWrapper.MAX_CMT_WIDTH)	return false; 
		
		int cli = 1; 
		String hangLnPrfx = DHSWrapper.HANGING_INDENT + CMT_FLAG; 
		int hlpLen = hangLnPrfx.length(); 
		
		while (cli < spl.length) {
			if (spl[cli].length() <= hlpLen)	return false; 
			if(!spl[cli].substring(0, hlpLen).equals(hangLnPrfx))	return false; 
			cli++;
		}
		return true; 
	}
	
	// impost standard format conventions on comment for a cascade~cascade file
	public static String standardizeCmt(String cmt)
	{
		String[] tokenized = cmt.substring(cmt.indexOf(" ")+1).split(" "); 
		
		String out = ""+CMT_FLAG+cmt.substring(0, cmt.indexOf(" ")); 
		for (String token : tokenized)
		{
			if (out.length() + token.length() + 1 > DHSWrapper.MAX_CMT_WIDTH)
				out += "\n"+DHSWrapper.HANGING_INDENT+CMT_FLAG+token; 
			else	out += " "+token; 
		}
		
		return out; 
		
	}
	
	
	//checker methods follow: 
	
	/** findSeqBySymb
	 * if
	 * @param toSearch, a list, contains... 
	 * @param sought, a certain sequential phonic instance's symbol ...
	 * @return its index, else return -1 .
	 */
	// else return -1. 
	public static int findSeqPhBySymb (List<SequentialPhonic> toSearch, String sought)
	{
		for (int tsi = 0; tsi < toSearch.size(); tsi++ )
			if (toSearch.get(tsi).print().equals(sought))	return tsi; 
		return -1;
	}
	
	//true if two sequences of SequentialPhonic instances are functionally equal
	public static boolean phonSeqsEqual(List<SequentialPhonic> sp1, List<SequentialPhonic> sp2) {
		if (sp1.size() != sp2.size())
			return false;
		int spn = sp1.size();
		for (int spi = 0; spi < spn; spi++)
			if (!sp1.get(spi).equals(sp2.get(spi)))
				return false;
		return true;
	}
	
	// check if two cascades, i.e. lists of sound changes, are equivalent
	public static boolean compareCascades(List<SChange> c1, List<SChange> c2)
	{
		for(int ci = 0; ci < c1.size(); ci++)
			if (!c1.get(ci).toString().equals(""+c2.get(ci)))
				return false;
		return true;
	}
	
	// print the difference between two cascades, if there is one. 
	public static String printCascDiff(List<SChange> c1, List<SChange> c2)
	{
		if (c1.size() != c2.size())	return "Different sizes!";
		
		int i = 0 ;
		while ( c1.get(i).toString().equals(""+c2.get(i)))
		{
			i++; 
			if (i == c1.size())	return "No difference!"; 
		}
		
		return "First diff at rule "+i+":\n"
				+ c1.get(i) + "\nVS\n" + c2.get(i);
	}
	
	// check if an observed word, the second argument (in current usage, the observed predicted outcome based on a cascade)...
		// is equivalent to the correct form of the word, the second argument (in current usage, the actual reflex word 
	public static boolean checkWord(Etymon correct, Etymon observed, String errMessage)
	{
		String c = correct.print(), o = observed.print(); 
		if (o.startsWith("*"))	o = o.substring(1); 
		boolean result = c.equals(o); 
		if (!result)	System.out.println(errorMessage(c,o,errMessage)); 
		return result; 
	}
	
	// same as above, but for booleans, not words -- used as auxiliary method for various debugging purposes. 
	public static boolean checkBoolean(boolean correct, boolean observed, String errMessage)
	{
		if (correct != observed)	System.out.println(errorMessage(""+correct,""+observed,errMessage)); 
		return correct == observed; 
	}
	
	public static String generateErrorMessage(SChange sc, List<SequentialPhonic> input,
			List<SequentialPhonic> expected, List<SequentialPhonic> observed) {
		return "Error in realization of this rule:\t\t" + sc + "\n\tInput was:\t" + printWord(input) 
		+ "\n\tExpected result: " + printWord(expected) + "\n\tObserved result:\t\t" + printWord(observed)+ "\n";
	}

	/**
	 * @param sc: sound change being tested
	 * @param inp: input
	 * @param exp: expected output
	 * @return whether expected output was produced (true, false)
	 */
	public static boolean runTest(SChange sc, List<SequentialPhonic> inp, List<SequentialPhonic> exp) {
		List<SequentialPhonic> obs = sc.realize(inp);
		if (phonSeqsEqual(exp, obs))
			return true;
		System.out.print(generateErrorMessage(sc, inp, exp, obs));
		return false;
	}
	
	public static String errorMessage(String cor, String obs, String msg)
	{
		return msg.replace("%c", cor).replace("%o",obs); 
	}
	
	// @param ec -- error count
	public static void errorSummary(int ec)
	{
		if (ec == 0)	System.out.println("No errors through to this point."); 
		else	System.out.println("In all "+ec+" error"+(ec != 1 ? "s" : "")+".");
	}
	
	public static boolean checkMetric(double correct, double observed, String errMessage)
	{
		if (correct != observed)	System.out.println(errorMessage(""+correct,""+observed,errMessage)); 
		return correct == observed; 
	}
	
	/**
	 * @param g -- gold words
	 * @param obs -- observed words
	 */
	public static int aggregateErrorsCheckWordLists(Etymon[] g, Etymon[] obs)
	{
		assert g.length == obs.length : "Error: tried to compare word lists of different lengths.";
		
		int tot = 0; 
		for (int i = 0 ; i < g.length; i++)
			tot += checkWord(g[i], obs[i], "Reflex mismatch: %o for %c") ? 0 : 1; 
		return tot;
	}
	
	public static boolean compare1dBoolArrs(boolean[] a1, boolean[] a2)
	{
		assert a1.length == a2.length : "Tried to compare two int arrays of different length"; 
		for (int ai = 0; ai < a1.length; ai++)	
			if (a1[ai] != a2[ai])	return false;
		return true;
	}
	
	public static boolean compare1dIntArrs (int[] a1, int[] a2)
	{
		if (a1.length != a2.length )	return false; 
		for (int ai = 0; ai < a1.length; ai++)	
			if (a1[ai] != a2[ai])	return false;
		return true;
	}
	
	public static boolean compare2dIntArrs(int[][] aa1, int[][] aa2)
	{
		assert aa1.length == aa2.length : "Tried to compare structures of different length"; 
		for (int aai = 0 ; aai < aa1.length; aai++)
			if (!compare1dIntArrs(aa1[aai],aa2[aai]))	return false;
		return true;
	}
	
	public static boolean compare1dStrArrs(String[] a1, String[] a2)
	{
		assert a1.length == a2.length : "Tried to compare two int arrays of different length"; 
		for (int ai = 0; ai < a1.length; ai++)	
			if (!strcmp(a1[ai],a2[ai]))	return false;
		return true;
	}
	
	public static boolean compare2dStrArrs(String[][] aa1, String[][] aa2)
	{
		assert aa1.length == aa2.length : "Tried to compare structures of different length"; 
		for (int aai = 0 ; aai < aa1.length; aai++)
			if (!compare1dStrArrs(aa1[aai],aa2[aai]))	return false;
		return true;
	}
	
	// return all lines of a file as a list of each as a string
	public static List<String> readFileLines(String loc) 
	{
		List<String> lns = new ArrayList<String>();
		String nextLine; 
		
		try 
		{
			File inFile = new File(loc); 
			BufferedReader in = new BufferedReader ( new InputStreamReader ( new FileInputStream(inFile), "UTF8")); 
			while((nextLine = in.readLine()) != null)		lns.add(nextLine);
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
		
		return lns;
	}
	
	// check if two files (i.e. cascade files) have the exact same contents
	public static boolean compareFiles(String loc1, String loc2)
	{
		List<String> f1lns = readFileLines(loc1), 
				f2lns = readFileLines(loc2); 
		if (f1lns.size() != f2lns.size())	return false; 
		for (int li = 0 ; li < f1lns.size(); li++)
			if(!f1lns.get(li).equals(f2lns.get(li)))	return false;
		return true;
	}
	
	public static String printFileDiff(String loc1, String loc2)
	{
		if (compareFiles(loc1, loc2))	return "identical."; 
		
		List<String> f1lns = readFileLines(loc1), 
				f2lns = readFileLines(loc2); 
		int len1 = f1lns.size(), len2 = f2lns.size(); 
		
		int li = 0 ;
		
		boolean firstLonger = len1 > len2; 

		while( f1lns.get(li).equals(f2lns.get(li)))
		{
			li++; 
			if (li == Math.min(len1, len2))
				return (firstLonger ? "First" : "Second") + " with extra rules after number "+li; 
		}
		
		return "Congruent until rule "+li+":\n"+outset(longVertDisjunction(f1lns.get(li), f2lns.get(li)))+"\n"; 
	}
	
	public static boolean strcmp (String x, String y)
	{
		if (x == null || y == null)	return (x == null) == (y == null); 
		else return x.equals(y);
	}
	
	//debugging auxiliary...
	public static String printIndexedRules(List<SChange> theCasc)
	{
		int ci = 0; String out = ""; 
		for (SChange sci : theCasc)	out += ""+(ci++)+": "+sci+"\n"; 
		return out.substring(0, out.length() - "\n".length()) ;
	}
	
	
	public static void checkForIllegalPhoneSymbols()
	{
		Set<String> phSymbols = phoneSymbToFeatsMap.keySet(); 
		for(String phSymb : phSymbols)
		{
			for(String illegal : illegalForPhSymbs)
				if(phSymb.contains(illegal))
					throw new Error("Error, the phone symbol "+phSymb+" contains an illegal part, "+illegal); 
		}
	}
	
	public static void extractSymbDefs(List<String> symbDefsLines)
	{
		//from the first line, extract the feature list and then the features for each symbol.
		phoneSymbToFeatsMap = new HashMap<String, String>(); 
		featsToSymbMap = new HashMap<String, String>(); 	
		
		featsByIndex = symbDefsLines.get(0).replace("SYMB,", "").split(""+FEAT_DELIM); 
		featIndices = new HashMap<String,Integer>(); 
		
		for(int fi = 0; fi < featsByIndex.length; fi++) featIndices.put(featsByIndex[fi], fi);

		//ordFeatNames = new ArrayList<String>(featIndices.keySet());
			// ^abrogated above -- because this apparently disorders the features!
		ordFeatNames = Arrays.asList(featsByIndex); 
		
		//Now we check if the features have weights
		if (symbDefsLines.get(1).split(",")[0].equalsIgnoreCase("FEATURE_WEIGHTS"))
		{
			feats_weighted = true; 
			FT_WTS = new double[featsByIndex.length];
			String[] weightsByIndex = symbDefsLines.get(1).substring(symbDefsLines.get(1).indexOf(",")+1).split(",");
			for(int i = 0 ; i < featsByIndex.length; i++)
				FT_WTS[i] = Double.parseDouble(weightsByIndex[i]); 
			symbDefsLines.remove(1); 
		}	
		else	feats_weighted = false;
		
		//from the rest-- extract the symbol def each represents
		int li = 1; String nextLine;
		while (li < symbDefsLines.size()) 
		{
			nextLine = stripEnds(symbDefsLines.get(li)); //strip white space and invisible characters 
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
			featsToSymbMap.put(intFeatVals, symb ); 
			li++; 
		}
		
		checkForIllegalPhoneSymbols(); 
		
		symbsExtracted = true;
	}
	
	
	/** extract order of intermediate stages so that we don't end up with ``flips'' in the relative ordering between stages
		// in the case that they end up in the same
		// chronological "moment" between rule operation steps (TODO need to clarify this a bit further maybe?) 
	// @param black_at_input -- true to execute bandaid in scenario where user declared name of input stage as a black stage. 
		// in this case, skip until have found first rule. 
	// @note does not handle columned black stages -- this is done externally. 
	 */
	public static String[] extractStageOrder(String cascLoc, boolean black_at_input)
	{
		List<String> lines = readFileLines(cascLoc); 
		int li = 0; 
		
		if (black_at_input)
		{
			boolean found_first_rule = false ;
			while (!found_first_rule) 
			{
				String ln = lines.get(li).split(""+CMT_FLAG)[0].replaceAll("^[ \t]+|[ \t]+$", ""); 
				
					//String ln = lines.get(li).split(""+CMT_FLAG)[0].strip(); 
				lines.remove(li); // can destructively do this, since rules are not being processed in this method. 
				if (ln.length() == 0 )	continue;
				if (ln.charAt(0) == GOLD_STAGENAME_FLAG)
					throw new RuntimeException("Somehow found a gold stage called '"+ln.substring(1)+"' before any rules are declared; there must be an error somewhere.");
				found_first_rule = (ln.charAt(0) != BLACK_STAGENAME_FLAG && ln.contains(">"));
			}
		}
		
		while(li < lines.size())
		{
			String ln = lines.get(li); 
			
			if (ln.equals("") == false) {
				char flag = ln.charAt(0); 
				if (flag != GOLD_STAGENAME_FLAG && flag != BLACK_STAGENAME_FLAG)
					lines.remove(li); 
				else	li++; 
			}
			else lines.remove(li);
		}
		
		String[] out = new String[lines.size()]; 
		li = 0;
		int ngi = 0, nbi = 0;
		while (li < out.length) {
			out[li] = lines.get(li).charAt(0) == GOLD_STAGENAME_FLAG ? "G"+(ngi++) : "b"+(nbi++);
			li++;
		}
		return out; 
	}
	

	public static void extractFeatImpls(String featImplsSrc)
	{
		FT_IMPLICATIONS = new HashMap<String, String[]>(); 
		
		List<String> featImplLines = readFileLines(featImplsSrc);
				
		for(String filine : featImplLines)
		{
			String[] fisides = filine.split(""+IMPLICATION_DELIM); 
			FT_IMPLICATIONS.put(fisides[0], fisides[1].split(""+FEAT_DELIM));
		}
		featImplsExtracted = true; 
		featsWithImplications = FT_IMPLICATIONS.keySet(); 

	}
	
	/**
	 * @author Clayton Marr
	 * @date August 7 2023 (but based on material moved from slightly earlier method extractDiacriticDefs from earlier in the summer of 2023) 
	 * @param diacriticDefLocation -- file to extract from
	 * @param feature_indices -- should be featIndices in DiachronicSimulator; featureIndices in phoneTester
	 * method to build diacritics map -- moved to here so it can be more easily shared between PhoneTester and DiachronicSimulator 
	 * and anywhere else, as necessary
	 * formerly, @returned diacritics map to be used in DiachronicSimulator and in PhoneTester
	 * now as of @date January 24, 2024, this is a void method that initializes the now LOCAL but PUBLIC diacritic map, which will be referenced by other classes
	 * 	but kept here in UTILS.
	 */
	public static void extractDiacriticMap(String diacriticDefLocation)
	{
		DIACRIT_TO_FT_MAP = new HashMap<String, String[]> (); 
		featsToPossibleDiacritics = new HashMap<String, List<String>>(); 
		if (VERBOSE)		System.out.println("Now extracting diacritics for segmentals symbols from file: "+diacriticDefLocation); 
		
		List<String> diacriticsLines = readFileLines(diacriticDefLocation); 
		
		for (String sdline: diacriticsLines)
		{
			int cmtFlagIndex = sdline.indexOf(CMT_FLAG);
			if (cmtFlagIndex != -1)
				sdline = sdline.substring(0, cmtFlagIndex);
			String[] sdsides = sdline.replace(" ", "").split(String.valueOf(DIACRITICS_DELIM));
			if (!sdsides[0].equals(""))
			{
				String[] diacritFeats = sdsides[1].split(","); 
				for (String df : diacritFeats)
				{
					if (!ALL_FTSPEC_MARKS.contains(""+df.charAt(0)))
						throw new RuntimeException("ERROR: symbol diacritics defs file should only have feature specifications indicated for diacritics in '+' or '-', "
								+ "but instead this one has :"+df.charAt(0)); 
					if (!featIndices.containsKey(df.substring(1)))
						throw new RuntimeException("ERROR: tried to declare a diacritic, "+sdsides[0]+" that would mark an invalid feature: "+df);
				}
				DIACRIT_TO_FT_MAP.put(sdsides[0], sdsides[1].split(","));
				
				List<String> ftpdi_val = 
						featsToPossibleDiacritics.containsKey(sdsides[1]) ? 
								featsToPossibleDiacritics.get(sdsides[1]) : new ArrayList<String>() ;
				ftpdi_val.add(sdsides[0]);
				featsToPossibleDiacritics.put(sdsides[1], ftpdi_val); 
			}
		}
		if (VERBOSE)
			System.out.println("Done extracting symbol diacritics!");	
		diacriticsExtracted = true; 
	}
	
	// printer methods follow: 
	
	/**
	 * s1, s2  -- two strings we are comparing
	 * @return the string forms after the first point where there is a difference between them. 
	 * if there is no difference, return null 
	 */
	public static String printCommonPrefix(String s1, String s2)
	{
		int i = 1;  
		int minlen = Math.min(s1.length(), s2.length());
		while (i < minlen)
		{
			if (!s1.substring(0,i).equals(s2.substring(0,i)))
				return s1.substring(0,i-1);
			i++;
		}
		if(s1.length() == s2.length())	return null;
		else	return s1.substring(0,i-1);

	}
	
	public static String print1dArr(Object[] a)
	{
		if (a.length == 0)	return "[empty array]"; 
		String out = ""; 
		for (Object ai : a)	out += ""+ai+","; 
		return out.substring(0, out.length()-1);
	}
	
	public static String print1dIntArr(int[] a)
	{
		if (a.length == 0)	return "[empty array]"; 
		
		String out = ""; 
		for (int ai : a)	out += ""+ai+","; 
		return out.substring(0, out.length()-1);
	}
	
	public static String print2dStrArray(String[][] a)
	{
		String out="\n"; 
		String DELIM = "\t"; 
		for (int i = 0 ; i < a.length; i++)
		{
			for (int j = 0 ; j < a[i].length; j++)
				out += (j == 0 ? "" : DELIM) + a[i][j];
			out += "\n";
		}
		return out; 
	}
	
	public static String print1dBoolArrAsIntArr(boolean[] b)
	{
		if (b.length == 0)	return "[empty array]"; 
		
		String out = "";
		for (boolean bi : b)	out += bi ? "1," : "0,"; 
		return out.substring(0, out.length() - 1 ); 
	}
	
	public static int findInt (int[] arr, int targ)
	{
		for(int i = 0 ; i < arr.length; i++)	if (arr[i] == targ)	return i;
		return -1; 
	}
	
	//for debugging purposes mainly in SimulationTester
	public static String stringDiff(String a, String b)
	{
		int lenA = a.length(), lenB = b.length(); 
		// handle case where one of the input arguments is empty. 
		if(lenA == 0 || lenB == 0)
			return lenA == lenB ? "No difference" : 
					"total difference (one string is empty) : "+ 
						(lenA == 0 ? lenB : lenA); 
		
		String commonPrefix = "", commonSuffix = "" ;
		
		//fill commonPrefix as a starting part of string that is shared. 
		int i = 1; 
		while ( a.substring(0, i).equals(b.substring(0, i)))
		{
			i++;
			if ( i == Math.min(lenA, lenB))
			{
				String diffSuffix = lenA > lenB ? a.substring(i) : b.substring(i); 
				return a.substring(0, i-1) +" [+"+ outset(diffSuffix);
			}
		}
		
		commonPrefix = a.substring(0, i-1); 
		
		//fill commonSuffix
		int j = 0 ; 
		while (a.substring(lenA-1-j).equals(b.substring(lenB-1-j)))
		{
			j++; 
			if ( j == -1 + Math.min(lenA,lenB));
			{
				if (lenA > lenB)
					return outset(a.substring(0, lenA - lenB)) + "+] "+ b; 
				else	return outset(b.substring(0, lenB - lenA)) + "+] " + a; 
			}
		}
		
		
		if (i < (Math.min(lenA, lenB) - 1 - j) )
		{
			String disjunction = longVertDisjunction ( a.substring(i, lenA - j), b.substring(i, lenB - j));
			return commonPrefix + outset(disjunction) + commonSuffix;
		}
		else
		{
			if (i < j) // suffix longer than prefix
			{
				String disjunction = longVertDisjunction(a.substring(0, lenA-j) , b.substring(0, lenB - j));
				return outset(disjunction) + commonSuffix; 
			}
			else
			{
				String disjunction = longVertDisjunction(a.substring(i), b.substring(i));
				return commonPrefix + outset(disjunction); 
			}
		}
		
	}
	
	public static String longVertDisjunction(String a , String b) 
	{
		return a +"\n----------------------------------------------\n "+b; 
	}
	
	public static String outset(String s)
	{
		return "{[ "+s+" ]}"; 
	}
	
	public static boolean isInt(String s)
	{
		for (int si = 0 ; si < s.length(); si++)
			if (!"0123456789".contains(s.substring(si,si+1)))	return false; 
		return true;
	}
	
	public static int getIntPrefix(String s)
	{
		for (int si = 1 ; si < s.length(); si++ )
			if (!isInt(""+s.charAt(si)))		return Integer.parseInt(s.substring(0,si));
		return Integer.parseInt(s); 
	}
	
	public static int countDisjunctContexts(String rule) {
		return !rule.contains(SChangeFactory.contextFlag+"") ? 1 : 
			getDisjunctContexts(rule.substring(rule.indexOf(SChangeFactory.contextFlag+"")+1)).size(); 
	}
	
	public static List<String> getDisjunctContexts(String impCtxt) {
		String CF = SChangeFactory.contextFlag+"", LOC = SChangeFactory.LOCUS; 
		if(impCtxt.contains(CF))	throw new RuntimeException("Error: cannot have context flag within context"); 
		if(!impCtxt.contains(LOC))	throw new RuntimeException("Error: context specification must contain a locus "+LOC);
		if(impCtxt.contains(CMT_FLAG+""))	impCtxt = impCtxt.substring(0, impCtxt.indexOf(CMT_FLAG));
		
		//check for improper brace placement with regard to locus, and other incorrect formations. 
		String[] inpSplit = impCtxt.split(LOC); 
		String inputPrior = inpSplit[0];
		boolean 
				priorSpecified = stripEnds(inputPrior).length() > 0,
				//		priorSpecified = inputPrior.strip().length() > 0, 
				postrSpecified = inpSplit.length == 1 ? false : stripEnds(inpSplit[1]).length() > 0;
		if (inpSplit.length > 2)	throw new RuntimeException("Context specification cannot have multiple locuses!");
		String inputPostr = inpSplit.length == 2 ? inpSplit[1] : ""; 
		
		if(priorSpecified)
			if (checkForBracingError(inputPrior))
				throw new RuntimeException("Invalid input for prior context stipulation: "+inputPrior); 
		if(postrSpecified)
			if(checkForBracingError(inputPostr))
				throw new RuntimeException("Invalid input for posterior context stipulation: "+inputPostr); 
		
		return getBraceDisjPossibilities(impCtxt); 
	}
	
	//error if any opener { braces are not closed by a closer } brace or vice versa.
	// @true if there is such erroneous usage. 
	public static boolean checkForBracingError(String s) 
	{
		String SD = DISJUNCT_DELIM + ""; 
		if (s.contains("{") != s.contains("}") )	return true; 
		if(!s.contains("{"))	return false; 
		int openerInd = s.indexOf("{"), lastCloserInd = s.lastIndexOf("}"); 
		int checkInd = openerInd;
		List<int[]> subDisjunctions = new ArrayList<int[]> (); // for braces delimiting the area where braceDepth == 2; 
		String singleDepthText = "";
		int braceDepth = 0; 
		do 
		{
			char curr = s.charAt(checkInd); 
			if (curr == '{') {
				if(braceDepth++ == 1)	
					subDisjunctions.add(new int[] {checkInd, -1}); 
				
				// next three indices cannot have }
				if (checkInd+3 >= lastCloserInd)	return true; 
				if (s.substring(checkInd+1, checkInd+4).contains("}")) return true; 
			}
			if (curr == '}')
			{
				if(braceDepth-- == 2)
				{
					int[] last_subdisj = subDisjunctions.get(subDisjunctions.size() - 1); 
					if (last_subdisj[1] != -1)	return true; 
					last_subdisj[1] = checkInd;
					subDisjunctions.set(subDisjunctions.size()-1, last_subdisj); 
				}
			}
			else if(braceDepth == 1)	singleDepthText += curr; 
			checkInd++; 
		} while (braceDepth > 0 && checkInd < lastCloserInd);
		
		if (checkInd == lastCloserInd && braceDepth != 1)	return true; 
		
		if (subDisjunctions.size() > 0)
			for (int[] sub_disj : subDisjunctions)
				if (checkForBracingError(s.substring(sub_disj[0], sub_disj[1])))
					return true;  
		if(!singleDepthText.contains(SD))
			return true; 
		if(checkInd < lastCloserInd)
			return checkForBracingError(s.substring(checkInd, lastCloserInd+1)); 
		return false; 
	}
	

	/**
	 * given a string of phonemes featMatrices, pseudophones etc with a disjunction (@param INP) , 
	 * @return all possible strings with each possibility of (each) disjunction clause explored 
	 */
	public static List<String> getBraceDisjPossibilities(String inp)
	{
		if(checkForBracingError(inp))	
			throw new RuntimeException("Invalid bracing in string: "+inp);
		List<String> out = new ArrayList<String>(); 
		if (!inp.contains("{"))
		{	out.add(inp); return out;	}
		
		int openerInd = inp.indexOf("{"); 
		int checkInd = openerInd + 1, braceDepth = 1;
		List<int[]> lvl2disjunctSpans = new ArrayList<int[]> (); 
		List<Integer> ddelims = new ArrayList<Integer>(); 
		while (braceDepth > 1 || inp.charAt(checkInd) != '}')
		{
			char curr = inp.charAt(checkInd); 
			if (curr == DISJUNCT_DELIM && braceDepth == 1) 
				ddelims.add(checkInd); 
			if(curr == '{' && braceDepth == 1)
				lvl2disjunctSpans.add(new int[] {checkInd, -1}); 
			if (curr == '{')	{
				if(braceDepth++ == 1)
					lvl2disjunctSpans.add(new int[] {checkInd, -1});  }
			if (curr == '}') {
				if (braceDepth-- == 2) {
					int[] l2last = lvl2disjunctSpans.get(lvl2disjunctSpans.size()-1); 
					l2last[1] = checkInd; 
					lvl2disjunctSpans.set(lvl2disjunctSpans.size()-1, l2last); 
				}
			}
			checkInd++; 
		}
		
		List<String> lvl1disjuncts = new ArrayList<String>(); 
		for (int di = 0; di <= ddelims.size(); di++)
			lvl1disjuncts.add( 
					inp.substring( 
							1 + (di == 0 ? openerInd : ddelims.get(di-1)) ,
							di == ddelims.size() ? checkInd : ddelims.get(di)));
		
		// will recurse if necessary.
		for (String disj : lvl1disjuncts)
			out.addAll( 
					getBraceDisjPossibilities(inp.substring(0, openerInd) + disj + inp.substring(checkInd+1)));
		
		return out; 
	}
	
	public static boolean suffixIs(String str, String cand)
	{
		return str.substring(str.length()-cand.length()).equals(cand); 
	}
	
	//return harmonic mean of precision and recall
	// give 0 rather than NaN if both recall and precision are zero. 
	public static double f1 (double precision, double recall)
	{
		if (precision + recall == 0)	return 0.0; 
		
		// return fB_score (precision, recall, 1) ;  -- equivalent! 
		return 2.0 * precision * recall / (precision + recall); 
		
	}
	
	// beta > 1 favors recall contributing more to the outcome of the score
		// beta = 0 is just precision
		// beta in (0, 1) favors precision 
		// very large beta starts to become just a measure of recall
	// give 0 rather than NaN if both recall and precision are zero. 
	public static double fB_score (double precision, double recall, double beta)
	{
		if (precision + recall == 0)	return 0.0; 
		
		return (1 + beta*beta) * precision * recall 
				/ ( beta * beta * precision + recall ); 
	}
	
	// return if string consists of F + a valid beta value for an f-score test
	public static boolean valid_fB (String entry)
	{
		entry = stripEnds(entry); 
		if (entry.length() < 2)	return false;  
		if (!entry.substring(0,1).equalsIgnoreCase("f"))	return false; 
		String beta = entry.substring(1); 
		if (beta.contains("."))
		{
			int dot_index = beta.indexOf("."); 
			beta = beta.substring(0,dot_index) + beta.substring(dot_index+1); 
			if (beta.contains("."))	return false; 
		}
		for (char c : beta.toCharArray())
			if (!Character.isDigit(c))	return false; 
		
		return true; 
	}
	
	// calculate MCC for a binary variable pair. Effectively equivalent to Pearson's coefficient in this binary pair case. 
	public static double phi_coeff (double n00, double n10, double n01, double n11)
	{
		double numerator = n11 * n00 - n10 * n01;
		double denominator = Math.sqrt( 
				(n11 + n10) * (n01 + n00) * (n11 + n01) * (n10 + n00));
		return numerator / denominator; 
	}

	/**
	 * Returns a multiline string consisting of a formatted accuracy report.
	 * @param ea an ErrorAnalysis
	 * @return A formatted string.
	 */
	public static String getAccuracyReport(ErrorAnalysis ea)
	{
		String accuracyReport = "";
		accuracyReport += "\nACCURACY REPORT:";

		accuracyReport += String.format("\nOverall accuracy:                        %.8f", ea.getAccuracy());
		accuracyReport += String.format("\nAccuracy within 1 phone:                 %.8f", ea.getPctWithin1());
		accuracyReport += String.format("\nAccuracy within 2 phones:                %.8f", ea.getPctWithin2());
		accuracyReport += String.format("\nAverage edit distance from gold:         %.8f", ea.getAvgPED());
		accuracyReport += String.format("\nAverage feature edit distance from gold: %.8f", ea.getAvgFED());
		

		return accuracyReport;
	}

	/**
	 * Returns a multiline string consisting of a formatted comparative accuracy report.
	 * @param ea the existing ErrorAnalysis
	 * @param newEa a candidate ErrorAnalysis
	 * @return A formatted string.
	 */
	public static String getAccuracyReport(ErrorAnalysis ea, ErrorAnalysis newEa)
	{
		String accuracyReport = "";
		accuracyReport +=               "\nCOMPARATIVE ACCURACY REPORT:             OLD:          NEW:";
		
		accuracyReport += String.format("\nOverall accuracy:                        %.8f -> %.8f",
			ea.getAccuracy(), newEa.getAccuracy());

		accuracyReport += String.format("\nAccuracy within 1 phone:                 %.8f -> %.8f",
			ea.getPctWithin1(), newEa.getPctWithin1());

		accuracyReport += String.format("\nAccuracy within 2 phones:                %.8f -> %.8f",
			ea.getPctWithin2(), newEa.getPctWithin2());

		accuracyReport += String.format("\nAverage edit distance from gold:         %.8f -> %.8f",
			ea.getAvgPED(), newEa.getAvgPED());

		accuracyReport += String.format("\nAverage feature edit distance from gold: %.8f -> %.8f",
			ea.getAvgFED(), newEa.getAvgFED());


		return accuracyReport;
	}
	
	//preempt illegal alpha symbols
	public static final String ILLEGAL_ALPHAS = "[|]/()*+ 0129-,;";
	public static void abortIllegalAlpha(char inp) {	abortIllegalAlpha(""+inp);	}
	public static void abortIllegalAlpha(String inp)	{
		if (inp.length() > 1 || ILLEGAL_ALPHAS.contains(inp))			throw new Error("Illegal string attempted to be used as alpha symbol: ' "+inp+" '");	}
	// @precondition: ordFeatNames initialized in terms of feature names operationally in use...
	public static void abortIllegalAlphaSpec(String inp) 
	{	
		inp = inp.replace(" ",""); 
		if (ALL_FTSPEC_MARKS.contains(inp.substring(0,1)))
			inp = inp.substring(1); 
		
		char candalph = inp.charAt(0); 
		
		if (!ordFeatNames.contains(inp.substring(1))
				|| ILLEGAL_ALPHAS.contains(candalph+""))
			throw new Error("Illegal attempted alpha feature specification : '"+inp+"'"); 	
		
		String char_legality_indic = preemptFeatAlphambiguation(candalph, ordFeatNames);
		
		if (!char_legality_indic .equals(""))
			throw new Error("Cannot use '"+candalph+"' as an alpha feature, because it would cause confusion between the following features: "+char_legality_indic); 
				
	}
	
	public static void abortMidgetFeatSpec(String fsp)
	{
		if (fsp.length() < 2)
			throw new Error("Error: tried to check a feature stipulation less than two characters long ('"+fsp+"')\n."
					+ " Such a feat stip shouldn't have existed in the first place...");	
	}
	
	
	/**
	 * @param str -- a string to check for the present of a feat matrix with alpha-specified features.. 
	 * @return true if there is a feat matrix with an alpha-valued feature specification present in this string 
	 * 		intended for use for inputs, destinations, and contexts... 
	 */
	public static boolean stringHasFMWithAlpha (String str)
	{
		String[] protophones = str.split(""+PH_DELIM); 
		
		for(int ppi = 0 ; ppi < protophones.length; ppi++)
		{
			String curpp = ""+protophones[ppi].trim();
			if(curpp.charAt(0) == '[')
			{
				// as of July 2024, spaces in feature matrices as written are ignored: 
				curpp = curpp.replace(" ", "");
				
				String[] specs = curpp.substring(1, curpp.indexOf(']')).split(""+FEAT_DELIM); 
				for (String spec : specs) 
					if (spec_is_alpha_marked(spec))	return true; 
			}
		}
		return false;
	}	
	public static boolean stringHasFMWithNegAlpha (String str)
	{
		String[] protophones = str.split(""+PH_DELIM); 
		
		for(int ppi = 0 ; ppi < protophones.length; ppi++)
		{
			String curpp = ""+protophones[ppi].trim();
			if(curpp.charAt(0) == '[')
			{
				// as of July 2024, spaces in feature matrices as written are ignored: 
				curpp = curpp.replace(" ", "");
				
				String[] specs = curpp.substring(1, curpp.indexOf(']')).split(""+FEAT_DELIM); 
				for (String spec : specs) 
					if (spec_is_neg_alpha_marked(spec))	return true; 
			}
		}
		return false;
	}		
	
	
	/**
	 * 
	 * @param fstr -- a list of feat specs, delimited by FEAT_DELIM, after '[' and ']' are stripped 
	 * @returnList of all alphas in it. 	
	 *   @param only_if_negated -- do the above ONLY for negated alphas. 
	 * return empty if there are none.  (no error)
	 */
	public static List<String> listAlphasInFeatString (String ftstr, boolean only_if_negated)
	{
		String fstr = fSpecsAreBracketed(ftstr) ? debracketFSpecs(ftstr) : ""+ftstr; 
		List<String> output = new ArrayList<String> (); 
		String[] specs = fstr.split(""+FEAT_DELIM); 
		for (String spec : specs) {
			if (only_if_negated ? spec_is_neg_alpha_marked(spec) : spec_is_alpha_marked(spec)) {
				String alphHere = getAlphaFromFeatSpec(spec)+""; // FEATSPEC_MARKS.contains(""+spec.charAt(0)) ? spec.substring(1,2) : spec.substring(0,1); 
				if (output.size() == 0 ? true : !output.contains(alphHere))
					output.add(alphHere); 
			}
		}
		
		return output; 
	}
	public static List<String> listNegAlphasInFeatString(String fs)	{	return listAlphasInFeatString(fs,true);	}
	
	/**
	 * @param seg a part of a rule or sequentialfilter originally delimited by ' ' from others
	 * @return the text content in it but with any recursive parenthesizing ("(), ()*, ()+") or the presence of disjunction marking -- ; ,{} -- from it
	 * @warning could cause errors if the content above constitutes all that was passed.
	 */
	public static String stripRecursionAndDisjunctionFromSegmental(String seg)
	{
		String output = seg.replace("}", ""); 
		for (int i = 0 ; i < 3 ; i++) // onset could have up to all three of { (  ; , but each no more than once (actually ; could only be first, but anyhow)
		if (";{(".contains(output.charAt(0)+"")) output = output.substring(1); 
		if (output.charAt(output.length()-1) == ';')	output=output.substring(0, output.length()-1); 
		if (output.length() < 2 ) return output;
		if (output.substring(output.length() - 2).contains(")")) // either final ), or final )* or )+
			return output.substring(0, output.lastIndexOf(")")); 
		return output; 
	}
	
	/**
	 * @param str -- a string (a rule or debugging suite filter)
	 * 		to check for the presence of a feat matrix with alpha-specified features.. 
	 * will detect and list ANY alpha-valued feature specification present in this string 
	 * 		intended for use for strings to become rules, or to become debugging suite filters 
	 * 			(for filters as contexts of rules, should be handled when rule is comprehended from strings. 
	 *  @return List of all alphas in it. 
	 *  @param only_if_negated -- do the above ONLY for negated alphas. 
	 * return empty if there are none.  (no error)
	 */
	public static List<String> listAlphasInString (String str, boolean only_if_negated)
	{
		List<String> foundAlphas = new ArrayList<String>(); 
		String[] protophones = str.strip().split(""+PH_DELIM); 
		
		//if htere's only one 'protophone' and '[' never occurs, this is likely a featSpecs list...
		if (protophones.length < 2 && !str.contains("[")) 
		{
			System.out.println("Warning:  larger scale method listAlphasInString called on a feat matrix' feat specs. Consider using listAlphasInFeatString instead.");
			return listAlphasInFeatString(str.strip(), only_if_negated); 
		}
		
		for(int ppi = 0 ; ppi < protophones.length; ppi++)
		{
			String curpp = stripRecursionAndDisjunctionFromSegmental(protophones[ppi]);
			if(curpp.charAt(0) == '[')
			{
				// as of July 2024, spaces in feature matrices as written are ignored: 
				curpp = curpp.replace(" ", "");
				
				if (curpp.indexOf("]") > 1)
					for (String afi : listAlphasInFeatString(curpp.substring(1, curpp.indexOf("]")).replace(" ",""), only_if_negated))
						if (!foundAlphas.contains(afi)) foundAlphas.add(afi); 
			}	
		}
		return foundAlphas;
	}
	public static List<String> listAlphasInString (String str)	{	return listAlphasInString(str, false);	}
	
	/**
	 * @param str -- a string (a rule or  debugging suite filter) 
	 * 		to check for the presence of a feat matrix with alpha-specified features.. 
	 * will detect and list NEGATIVE alpha-valued feature specifications present in this string 
	 * 		intended for use for strings to become rules, or to become debugging suite filters 
	 * 			(for filters as contexts of rules, should be handled when rule is comprehended from strings. 
	 *  @return List of all alphas negated in it. 
	 * return empty if there are none.  (no error)
	 */
	public static List<String> listNegatedAlphasInString (String str)
	{	return listAlphasInString(str, true); 	}
	public static boolean stringHasNegProxies (String stri)
	{	return listNegatedAlphasInString(stri).size() > 0; 	} //if there's any, it'll be in the size.
	
	private static String possibleAlphaProxies = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnmʏʠɰɥɶʁɭʟɱŋɲɳɾɽʀɹɻʑʒʃʝθðɸαɑæʊσβɣçɛøœχɩʎ"; 
	
	/**createNegProxyAlphabet
	 * 9/29/25 @author Clayton Marr
	 * @param ruleOrDebugFilt -- a string that will become a rule or a debugging filter (not a context within a rule) 
	 * @precondition ruleOrDebugFilt should have a negative alpha value in the first place!
	 * @return neg proxy alphabet to handle negative alpha values
	 * unless an error is built in to block this, if there are no negative alphas present, it will return an empty HashMap 
	 * 		but an error currently blocks it -- can remove this if necessary.
	 */
	public static HashMap<String, String> createNegProxyAlphabet (String ruleOrDebugFilt)
	{
		List<String> alphsToNegate = listNegatedAlphasInString(ruleOrDebugFilt); 
		if (alphsToNegate.size() == 0)
			throw new Error("Error: tried to detect negated alphas in a string with no alphas: "+ruleOrDebugFilt+"\n\tInspect this."); 
				
		String possProxiesLeft = ""+possibleAlphaProxies;
		// preempt danger of using a locally existing alpha symbol as a proxy. 
		for (String ahi : listAlphasInString(ruleOrDebugFilt))	possProxiesLeft.replace(ahi, ""); 
		
		HashMap<String,String> outp = new HashMap<String, String> (); 
		for (String ani : alphsToNegate) {
			outp.put(possProxiesLeft.substring(possProxiesLeft.length()-1), ani); 
			possProxiesLeft = possProxiesLeft.substring(0, possProxiesLeft.length() -1 );
		}
		return outp; 
	}
	
	
	/** 
	 * for @param str, given proxy mapping @param proxies, apply so that if Z is the neg proxy for an alpha feature A, -A is replaced with Z 
	 * 		proxies -- key is the proxy, value is the negated alpha
	 * done to enable computation of negative alphas as single characters primarily for purposes within FeatMatrix. 
	 */
	public static String applyNegalphaProxies (String str, HashMap<String, String> proxies) 
	{
		String output = str+""; 
		if (str.charAt(0) == '-' && !isValidFeatSpecList(str, true)) // unless 
			throw new Error ("Tried to apply negative alpha proxy to a rule or filter that starts with '-'."
					+ "This should never have existed in the first place. Input was:\n\t"
					+ str); 
		
		for (String pxi : proxies.keySet())
		{
			for (String ftj : ordFeatNames)
			{
				String toReplace = MARK_NEG+proxies.get(pxi)+ftj; 
				while (output.contains(toReplace))
				{
					int loc = output.indexOf(toReplace); 
					output = output.substring(0, loc) + pxi
							+ output.substring(loc + (MARK_NEG+proxies.get(pxi)).length());  
				}			// ^ latter addend SHOULD alwyas be 2 , but just in case...	
			}
		}
		return output; 
	}
	
	/**
	 * 
	 * @param specString -- string of specs (not in []), delimited by FEAT_DELIM> 
	 * @param negProxies  -- key : proxy, value: negated alpha
	 * @return
	 */
	public static String decodeNegAlphProxies(String specString, HashMap<String, String> negProxies)
	{
		String output = negProxies.containsKey(specString.substring(0,1)) ?
				MARK_NEG + negProxies.get(specString.substring(0,1)) + specString.substring(1) : ""+specString; 
		for (String pxi : negProxies.keySet())
			while (output.contains(FEAT_DELIM + pxi))
				output = output.substring(0, 1+output.indexOf(","+pxi)) + MARK_NEG + negProxies.get(pxi); 
		return output; 
	}
	
	/**
	 * @precondition ordFeatNames is initialized.
	 * @return @true if a feature specification (e.g. '+voi', 'βround', etc...) 
	 * 	 as of 10/14, no longer only if it is assigned a @POSITIVE alpha value. 
	 * @param @fspec is the string form of the feature specification. */ 
	public static boolean spec_is_alpha_marked(String fspec)
	{
		String spec = fspec.replace(" ", ""); 
		abortMidgetFeatSpec(spec); 
		char onset = spec.charAt(0); 
		if (ALL_FTSPEC_MARKS.contains(""+onset))	return spec_is_preposed_alpha_marked(spec, onset);
		abortIllegalAlphaSpec(spec);  // throws error if alpha var'd feature (substring(1)) is not a valid feature, or first char is illegal as an alpha variable
		return true; 
	}
	
	/**
	 * @precondition ordFeatNames is filled (e.g. extractSymbDefs() has been called, and feat list we're operating is functionally what it extracted.
	 * @param spec feature stipulation : e.g. -βvoi etc. 
	 * @return @true iff it is an alpha feature preposed in a certain way (+, -, 0)- - 
	 * 	e.g. '-' + alpha character + feature -- meaning the character at 1 is the alpha value.
	 */
	public static boolean spec_is_preposed_alpha_marked (String fspec, char prep)
	{
		if (!ALL_FTSPEC_MARKS.contains(prep+""))	
			throw new Error("ERROR: tried to detect a proposed alpha feature, with an invalid preposition ('"+prep+"')"); 
		String spec = fspec.replace(" ", ""); 
		abortMidgetFeatSpec(spec); 
		if (spec.charAt(0) != prep)	return false;
		
		if (ordFeatNames.contains(spec.substring(1))) //if rest after '-' is a feature name, assume it's just a negative feature stip, no alpha.
			return false;  // note that this means, if one has a feature "lng" and another feature "slng", using 's' as an alpha variable will not work. One assumes the user isn't this stupid though.
		if (spec.length() < 3)
			throw new Error("Error: tried to check for "
					+ (prep==MARK_NEG ? "negative" : prep==MARK_POS ? "positive" : "demarked")
					+ " alpha stipulation on '"+spec+"'...\n\t"
					+ "...but the symbol '"+spec.charAt(1)+"' is not a feature, and with only 2 characters it cannot be anything valid.\n"
							+ "Please check your cascade for something wrong, and if there is nothing, flag this error for investigation..."); 
		abortIllegalAlphaSpec(spec); 
		return true; 
	}
	
	public static boolean spec_is_neg_alpha_marked (String spec)
	{	return spec_is_preposed_alpha_marked(spec, MARK_NEG);	}
	public static boolean spec_is_unspec_alpha_marked (String spec)
	{	return spec_is_preposed_alpha_marked(spec, MARK_UNSPEC);	}
	
	/**
	 * @precondition ordFeatNames is filled (e.g. extractSymbDefs() has been called, and feat list we're operating is functionally what it extracted.
	 * @param fspec -- feature stipulation / specification -- must consist of '-' + alph variable + feature
	 * @return the alpha variable being negated
	 */
	public static char getNegatedAlpha (String fspec)
	{
		if (fspec.length() < 3 ? true : 
			fspec.charAt(0) != MARK_NEG || !ordFeatNames.contains(fspec.substring(2)) || ILLEGAL_ALPHAS.contains(fspec.charAt(1)+""))
			throw new Error("Tried to detect negated alpha variable for a string that cannot be a stipulation with a negated alpha: '"+fspec+"'."
					+ "\n(Should be : '"+MARK_NEG+"' + alph var + a valid feature'...)"); 
		
		return fspec.charAt(1); 
	}

	/**
	 * like the above, but for getting any alpha, not just a negated one. 
	 * @precondition ordFeatNames is filled (e.g. extractSymbDefs() has been called, and feat list we're operating is functionally what it extracted.
	 * @param fspec -- feature stipulation / specification -- must consist of '-/0/(+ || '')' + alph variable + feature
	 * @return the alpha variable being used
	 */
	public static char getAlphaFromFeatSpec(String fspec)
	{
		String spec = fspec.replace(" ", ""); 
		int alphInd = ALL_FTSPEC_MARKS.contains(spec.substring(0, 1)) ? 1 : 0; 
		if (fspec.length() < alphInd+1 ? true : 
			ILLEGAL_ALPHAS.contains(spec.charAt(alphInd)+"") ? true : 
			!ordFeatNames.contains(fspec.substring(alphInd+1)))
			throw new Error("Tried to detect negated alpha variable for a string that cannot be an alpha-featured stipulation: '"+fspec+"'."); 
		
		return spec.charAt(alphInd);
	}
	
	/**
	 * 
	 * @param inp -- String form a rule or filter
	 * @return a list of all unique feature specifications in @param inp
	 */
	public static List<String> detectAllFeatSpecs (String inp)
	{
		String inp_left = inp+""; 
		List<String> outp = new ArrayList<String>(); 
		
		while (inp_left.contains("["))
		{
			inp_left = inp_left.substring(inp_left.indexOf("[")+1); 
			if(!inp_left.contains("]"))
				throw new Error("Error: unclosed feature matrix in '"+inp+"'");
			int closingLoc = inp_left.indexOf("]"); 
			
			String thisMatrix = inp_left.substring(0, closingLoc).replace(" ", ""); 
			if (thisMatrix.contains(""+FEAT_DELIM))
				outp.addAll(Arrays.asList(thisMatrix.split(""+FEAT_DELIM)));
			else outp.add(thisMatrix); 
			inp_left = closingLoc + 1 < inp_left.length() ? inp_left.substring(closingLoc+1) : "" ; 
		}
		
		return outp;
	}
	
	/**
	 * @precondition ordFeatNames has been filled 
	 * @param inp -- a rule, or a filter
	 * 		if a filter, should be one used in isolation, e.g. in the debugging suite -- not as part of a rule
	 * @throws @error if there is a negative alpha stipulation with no positive alpha feat stip anywhere else in this rule or filter.
	 */
	public static void abortOrphanedNegAlphStip (String inp)
	{
		List<String> specsHere = detectAllFeatSpecs (inp); 
		
		String neggedAlphsLeft = ""; // all negated alpha characters present. 
		
		for (int i = 0 ; i < specsHere.size(); i++) 
		{
			String sphi = specsHere.get(i); 
			if (spec_is_neg_alpha_marked(sphi))
			{
				char neggedAlph = getNegatedAlpha(sphi); 
				if (!neggedAlphsLeft.contains(""+neggedAlph))
					neggedAlphsLeft += (""+neggedAlph); 
				
				specsHere.remove(i); 
				i--; 
			}
		}
		
		while (neggedAlphsLeft.length() > 0)
		{
			char nali = neggedAlphsLeft.charAt(0); 
			neggedAlphsLeft = neggedAlphsLeft.length() == 1 ? "" : neggedAlphsLeft.substring(1); 
			boolean safe = false; 
			
			for (String sphj : specsHere ) 
			{
				String sph = sphj.substring(sphj.charAt(0) == MARK_POS ? 1 : 0); 
				if (nali == sph.charAt(0) && ordFeatNames.contains(sph.substring(1)))
					safe = true; ; // go to bigger wild loop -- passed for this character. 
			}
			
			//if didn't match -- orphaned negative alpha spec was present! 
			if (!safe)	throw new Error("ERROR: negated alpha variable '"+nali+"' is orphaned, without a counterpart anywhere in this formulation ('"+inp+"')"); 
		}
	}
	

	/**
	 * @param pref -- a char value
	 * @return "" if there is no feature in @param establishedFeatNames that would become another feature name if this character was preposed
	 * @else @return "A,B" where A is the non-prefixed feature and B is the feature that A becomes identical once prefixed with @param pref.f 
	 * @usage -- for possible alpha and negative proxy alpha characters
	 */
	public static String preemptFeatAlphambiguation (char pref, List<String> establishedFeatNames)
	{
		for (int fi = 0; fi < establishedFeatNames.size(); fi++)
		{
			String prefixed_fni = ""+ pref+ establishedFeatNames.get(fi); 
			for (int fj = 0 ; fj < establishedFeatNames.size(); fj++)
			{
				if (fi == fj)	continue; 
				
				if(prefixed_fni.equals(establishedFeatNames.get(fj))) 
				{
					possibleAlphaProxies.replace(pref+"","");  // cannot be proxy either. 
					return prefixed_fni.substring(1)+","+establishedFeatNames.get(fj); // i.e. prefix could make one feature become another -- ILLEGAL! 
				}
			}
		}
		return ""; 
	}
	
	/**
	 * @param listOfFeatSpecs -- list of sets of featSpecs -- e.g. { [+hi,+back]; {+cont,+son]}
	 * @return list of feats for which there are conflicting specifications, delimited by RESTR_DELIM + a space 
	 * 		a conflict is any non-equivalence in specification of a feature -- including alpha spec once and a +/-/0 otherwise. 
	 * @else return "".  
	 * (9/30/25) no specific handling for alphas or neg alphas here. It is assumed that if they aren't set they won't matter.
	 */
	public static String detectFeatConflicts(List<String> listOfFeatSpecs) 
	{
		HashMap<String,String> specsMade = new HashMap<String,String> (); 
		List<String> conflictedFeats = new ArrayList<String>(); 
		
		for (String specSet : listOfFeatSpecs) 
		{
			for (String featSpec : specSet.split(""+RESTR_DELIM)) 
			{
				String spec = ""+featSpec.charAt(0), ft = featSpec.substring(1); 
				if (specsMade.containsKey(ft) && !conflictedFeats.contains(ft))
				{	if (!specsMade.get(ft).equals(spec))	conflictedFeats.add(ft); }
				else specsMade.put(ft, spec);		
			}
		}
		
		if (conflictedFeats.size() == 0)	return ""; 
		else return String.join(RESTR_DELIM+" ", conflictedFeats);
	}
	
	public static String detectDiacritFeatConflicts(List<String> diacrits, boolean applyFeatImpls)
	{
		if (diacrits.size() < 2)	return ""; 
		
		// get feat specs for each diacritic.
		List<String> featSpecSetsPerDiacrit = new ArrayList<String>(); 
		for (String dfi : diacrits)	
		{
			String toAdd = String.join(""+RESTR_DELIM, DIACRIT_TO_FT_MAP.get(dfi)); 
			if (applyFeatImpls)	toAdd = applyImplications(toAdd); 
			featSpecSetsPerDiacrit.add(toAdd); 
		}
		
		return detectFeatConflicts(featSpecSetsPerDiacrit);
	}
	
	
	/**
	 * @author Clayton Marr, @date January 24, 2024
	 * @param unseenSymb -- a previously unseen combination of a base phone symbol and diacritics
	 *  @prerequisite phoneSymbToFeatsMap has already been built (extractSymbDefs()) 
	 *  @prerequisite DIACRIT_TO_FT_MAP has also already been built (extractDiacriticMap()) 
	 * this method will attempt to parse what phonetic feature string this likely indicates
	 * 	beware, @error (currently as @warning instead) if there are multiple diacritics present AND they indicate contradictory features! 
	 * 			(maybe this isn't necessary to do?) 
	 * @return @false if there is no predefined base phone detected  [ likely triggering error in outer-nested method ]
	 * 		(in practice, a 'base phone' is one already present as a key in phoneSymbToFeatsMap 
	 * @return @true if the parse is successful, 
	 * 	 but before returning, add @param unseenSymb as a key into @classvariable phoneSymbToFeatsMap ( @destructive ) 
	 * 		with its newly detected feature string as a value 
	 * 	 also, add the feature string as a key with @param unseenSymb as a value into @classvariable featsToSymbMap ( @destructive ) 
	 * 		if there was already a symbol with that feature string, then @usurp it within featsToSymbMap
	 * 			(because this is largely used as an auxiliary to @method parseLexPhon, 
	 * 					so unseen symbols likely reflect the annotation scheme of the "gold" forms to compare against...) 
	 * 		@print a message saying as much if @verbose
	 */
	public static boolean tryParseAndDefineMarkedSymbol (String unseenSymb) 
	{
		if (!symbsExtracted)	throw new Error("Error: tried to parse an unseen marked symbol before symbol definitions were extracted in the first place!"); 
		if (!diacriticsExtracted)	throw new Error("Error: tried to parse an unseen diacritic marked symbol ( "+unseenSymb+" ) before diacritic definitions were extracted!"); 
		
		List<String> diacritsLeft = new ArrayList<String>(DIACRIT_TO_FT_MAP.keySet()),
				diacritsFound = new ArrayList<String>(); 
		String restOfPhone = ""+unseenSymb; 
		
		while (diacritsLeft.size()>0 ) 
		{
			String candDiacrit = diacritsLeft.remove(0); 
			if(restOfPhone.contains(candDiacrit))
			{
				restOfPhone = restOfPhone.replace(candDiacrit, ""); 
					// not "replaceAll" -- should only replace the first instance 
					// so there could be an error (runtime or otherwise) if the same diacritic is used twice and the existing item isn't already defined. 
					// catching that here. 
				
				if (restOfPhone.contains(candDiacrit) && !candDiacrit.equals("ː")) 
					System.out.println("Warning: redundant diacritic (' "+candDiacrit+" ') detected in hitherto unseen diacriticized symbol <"+unseenSymb+">."); 
				
				diacritsFound.add(candDiacrit);
				
				if(phoneSymbToFeatsMap.containsKey(restOfPhone))	// successful parse complete! 
				{
					if (VERBOSE)
						System.out.println("New phone symbol ' "+unseenSymb+" ' defined!"); 
					
					String newFeatVect = phoneSymbToFeatsMap.get(restOfPhone); 
				
					// get feat specs for each diacritic, deal with any induced feature conflicts. 
					List<String> featSpecSetsPerDiacrit = new ArrayList<String>(); 
					for (String dfi : diacritsFound)	featSpecSetsPerDiacrit.add(String.join(""+RESTR_DELIM, DIACRIT_TO_FT_MAP.get(dfi))); 
					
					// detect diacritics in base phone symbol -- start with all of htem and remove htem. 
					List<String> basePhDiacrits = new ArrayList<String>(DIACRIT_TO_FT_MAP.keySet()); 
					for (String diacr : diacritsFound)	basePhDiacrits.remove(diacr); 
					int bpdi = 0; 
					while (bpdi < basePhDiacrits.size())
					{
						if (restOfPhone.contains(basePhDiacrits.get(bpdi)))	bpdi++; 
						else	basePhDiacrits.remove(bpdi); 
					}
					
					List<String> totalDiacrits = new ArrayList<String>(basePhDiacrits);
					totalDiacrits.addAll(diacritsFound); 
					
					// deal with any induced feature conflicts... 
					String conflictedFeats = detectDiacritFeatConflicts(totalDiacrits,true); 
					
					if( conflictedFeats.length() != 0)  System.out.println("Warning: diacritics used in the hitherto unseen symbol "
							+ "' "+unseenSymb+" ' "
								+ "appear to involve a conflict for the specification of the following features:\n\t"
								+conflictedFeats);
							
					// apply the features. Note they are applied in the order the diacritics were detected...
					for (String fsi : featSpecSetsPerDiacrit)	
						newFeatVect = (getFeatMatrix(fsi,true)).forceTruthOnFeatVect(newFeatVect);
					
					defineFeatVect(newFeatVect, unseenSymb); 
					
					return true; 
				}	
			}
		}

		return false; 
	}
	
	
		
	/** 
	 * given String @param toLexem
	 * @return its representation as a Etymon containing a sequence of Phone instances
	 * NOTE we assume the phones are separated by ()PH_DELIM (presumably ' ') 
	 * TODO still need to debug the use of diacritics here.  (still relevant @ 9/30/25? Unclear.)
	 * TODO when do that, make sure to update the counterpart in SimulationTester.
	 * this still bears the name LexPhon in its name even though the class LexPhon was renamed Etymon on 2 July 2023  
	 * 		... because it does not yet handle parsing of morphological, semantic, or token frequency info... yetR. 
	 * 		TODO decide where that will be parsed, make changes as necessary. (still relevant @ 9/30/25? Unclear.)
	 * moved from DiachronicSimulator to UTILS on January 24, 2024.  
	 */
	public static Etymon parseLexPhon(String toLexem, boolean no_symb_diacritics)
	{
		String toLex = toLexem.trim(); 
		
		if (PSEUDO_ETYM_INDICS.contains(toLex))
			return new PseudoEtymon(toLex);

		boolean toLexIsReconstructed = false; 
		if (toLex.charAt(0) == '*') { 
			toLexIsReconstructed = true ; 
			if ( toLexem.startsWith("*"+PH_DELIM) )
				toLexem = toLexem.substring(("*"+PH_DELIM).length()); 
			else	toLexem = toLexem.substring(1); 
		}
		
		String[] toPhones = toLexem.trim().split(""+PH_DELIM);
		
		List<SequentialPhonic> phones = new ArrayList<SequentialPhonic>(); //Etymon class stores internal List of phones not an array,
			// for better ease of mutation

		for (String toPhone : toPhones)
		{
			if (toPhone.equals("#") || toPhone.equals("+"))
				phones.add(new Boundary(toPhone.equals("#") ? "word bound" : "morph bound"));
			else
			{
				if (!phoneSymbToFeatsMap.containsKey(toPhone))
				{
					
					/**
					 * if the symbol isn't present in symbolDefs but it is a diacritic-marked variant of a symbol in it, 
					 * it will be rescued here, by adding a new symbol to phoneSymbToFeatsMap
					 * 	with feats a modified version of the basis according to the feature specifications
					 * 	that are associated to that diacritic in diacriticMap
					 *  if a phone already exists with that feature set, it will simply be replaced with that one. 
					 * at present it can only have one diacritic added onto it here. 
					*/
					
					boolean invalid_phone_error = no_symb_diacritics ? 
							true : !tryParseAndDefineMarkedSymbol(toPhone);
					
					if (invalid_phone_error)
						throw new RuntimeException("ERROR: tried to declare a phone in a word in the lexicon using an invalid symbol.\n"
								+ "Symbol is : '"+toPhone+"', length = "+toPhone.length()
								+ "\nLex phon is :"+toLexem);
				}
				phones.add(new Phone(phoneSymbToFeatsMap.get(toPhone), featIndices, phoneSymbToFeatsMap));
			}
		}
		return new Etymon(phones, toLexIsReconstructed);
	}
	
	// returns list of all diacritics found in symbol. Empty string if none found. 
	public static List<String> diacritsFoundInPhoneSymb (String symb)
	{
		String symb_elements_left = ""+symb; 
		List<String> out = new ArrayList<String>(); 
		for (String diacrit : DIACRIT_TO_FT_MAP.keySet()) {
			if (symb_elements_left.contains(diacrit)) {
				out.add(diacrit); 
				symb_elements_left = symb_elements_left.replace(diacrit, ""); 
			}
		}
		
		return out; 
	}
	
	/** for simulating the change of one feature in a feature vector as used in FeatMatrix
	 * 
	 * @param fv_inp -- input feature vector
	 * @param deep_feat_ch_specs -- specifications of what to change
	 * 			use the UNDERLYING ("deep"), format 2 = positive, 0 = negative, 9 = despecified
	 * 				sorry if this is confusing! 
	 * 			separate specifications with ',', or whatever restrDelim is set as 
	 * @return result of the change 
	 */
	public static String featVectChange(String fv_inp, String deep_feat_ch_specs)
	{
		String output = ""+fv_inp;
		for (String ch : deep_feat_ch_specs.split(""+RESTR_DELIM))
		{
			int floc = UTILS.featIndices.get(ch.substring(1)); 
			output = output.substring(0,floc) + ch.substring(0,1) + output.substring(floc+1); 
		}
		return output;		
	}
	
	/** 
	 * modify the hashmaps to add a new symbol ( @param symb) for the feat vect ( @param vect ) 
	 * @error if somehow this phone is already defined 
	 */
	public static void defineFeatVect(String vect, String symb)
	{
		//add new symbol, feat vector pair to phoneSymbToFeatsMap 
		if (phoneSymbToFeatsMap.containsKey(symb))
			throw new Error("Error: tried to add newly defined phone ' "+symb+" ' as dedicated symbol for the feature vector "+vect+
					"\n...but this symbol is already dedicated to the feature vector "+phoneSymbToFeatsMap.get(symb)); 
		phoneSymbToFeatsMap.put(symb,vect); 
		
		// add feats to symb to the reverse map, potentially usurping the default symbol mapped to in doing so 
		// (most recently seen symbol is one being used in current gold set, so this makes sense). 
		if (VERBOSE && featsToSymbMap.containsKey(vect)) 
			System.out.println("The symbol ' "+featsToSymbMap.get(vect)+" ' is usurped as the default print of its feature vector by ' "+symb+"'"); 
		featsToSymbMap.put(vect,symb); 
		
		System.out.println("Defined new symbol '"+symb+"', with feat vect: "+vect+" ."); 
	}
	
	public static String spellOutFeatVect (String featVect) {
		String out = "";
		for (int fi = 0; fi < featVect.length(); fi++) {
			switch (featVect.charAt(fi)) {
				case '0':
					out += MARK_NEG;
					break;
				case '1':
					out += MARK_UNSPEC;
					break;
				case '2':
					out += MARK_POS;
					break;
				case '9':
					out += ".";
					break;
				default:
					break;
			}
			out += featsByIndex[fi] + " ";
		}
		return out;
	}

	/**
	 * @author Clayton Marr, @date January 25, 2024 
	 * @param unseenVect -- feature vector that does not yet have a phone symbol attached to it. 
	 * 		@error if it is actually defined 
	 * 		@error if phone symbols not yet extracted, or if diacritics not yet extracted. 
	 * search through diacritics for a diacritic or combination of diacritics that could be added to a base phone to generate this vector 
	 * 	this is done by detecting which diacritics are true
	 * 		in first runthrough (tries for base symbol + single diacritic): 
	 * 			for each diacritic, check if it is true of the feature vector
	 * 				if so add it to set of diacritics to consider for multiple diacritic formations (second runtrhough) 
	 * 				if there is a base symbol (existing symbol in the hashmaps) that it can be added to
	 * 					check if it would create a diacritic conflict (i.e. if the 'base' symbol is in fact already diacriticized)
	 * 					and if not, we've successfully found a new phone for this feature vect
	 * 						add it to the hashmaps and @return @true
	 * 			@note that if multiple diacritics are defined for the same features, there is a preference for those that were defined first originally!
	 * 				(their order almost certainly reflects their original order in the file diacritics were extracted from) 
	 * 			@note that in the same way there is a preference for phones that were defined earlier. 
	 * 				(which also disprefers phones that were secondarily assigned to feat vects using htis method)
	 * 		if this doesn't work for any single diacritics, use the subset of diacritics that are true to try multiple diacritic combos 
	 * 			wherein again cases with conflicting diacritics (both from the base or the ones being added) are blocked
	 * 		if neither of these worked @return @false
	 * if @param apply_ft_impls, apply feature implications. 
	 */
	public static boolean tryDefineUnseenFeatVect (String unseenVect, boolean apply_ft_impls) 
	{
		if (VERBOSE)
			System.out.println("Attempting to define new symbol for hitherto unseen feature vector: \n\t"+spellOutFeatVect(unseenVect));
		
		if (!diacriticsExtracted)	throw new Error ("Error: tried to use diacritics to define new symbol before diacritics were extracted!"); 
		if (!symbsExtracted)	throw new Error ("Error: tried to define new symbol for unseen feat vector before phone symbols were even extracted!"); 
		if (featsToSymbMap.containsKey(unseenVect))	
			throw new Error ("Error: tried to define new symbol for 'unseen' feat vector that has already been seen and defined!"); 
		
		List<String> diacritSpecSetCands = new ArrayList<String>(featsToPossibleDiacritics.keySet());
		
		int ssi = 0; 
		
		while (ssi < diacritSpecSetCands.size())
		{
			FeatMatrix curCandFM = getFeatMatrix(diacritSpecSetCands.get(ssi), apply_ft_impls);
			if (curCandFM.compareToFeatVect(unseenVect))
			{
				//then this is a valid candidate!
				
				//check if this can combine with an existing base symbol 
				for (String baseFeatVect : featsToSymbMap.keySet())
				{
					String candDiacritResult = curCandFM.forceTruthOnFeatVect
							(baseFeatVect).replace(Integer.toString(DESPEC_INT), 
									Integer.toString(UNSPEC_INT)); 
					
					String baseSymb = featsToSymbMap.get(baseFeatVect),
							suffix = featsToPossibleDiacritics.get(diacritSpecSetCands.get(ssi)).get(0); 
					List<String> diacritsInvolved = diacritsFoundInPhoneSymb(baseSymb); 
					diacritsInvolved.add(suffix);
					
					if (candDiacritResult.equals(unseenVect)
							&& detectDiacritFeatConflicts(diacritsInvolved,true).equals(""))	// then we found it!!
					{
						String newSymb = baseSymb + suffix;
		
						defineFeatVect(unseenVect,newSymb); 
						// preference for first diacritic defined for this feature set. 
						if (VERBOSE)
							System.out.println("New symbol '"+newSymb+"' defined to represent feat vect "+unseenVect+"."); 
						return true; 
					}	
				}
				
				ssi++; // move on but keep this as a candidate. 
			}
			else	//not a valid candidate
				diacritSpecSetCands.remove(ssi); 	
		}
		
		if (VERBOSE)
			System.out.println("No single diacritic alone can define it. Attempting combos..."
				+ " \n\t(Possible starting points: "+String.join("  ",diacritSpecSetCands.toArray(String[]::new))+")"); 
		
		//second run -- if reached, then above, could not get a combo with single diacritics, now trying multiple diacritic combinations 
		
		HashMap<Integer,HashMap<String,String>> combinedSpecSetCandsByDepth = new HashMap<Integer,HashMap<String,String>>(); 
			// key -- number of combined diacrits (starting with 2)
			// value -- a hashmap of (featvect, diacritcombo) pairs. 
		int depth = 2; 
		
		HashMap<String,String> depth1set = new HashMap<String,String> (); 
		for ( String dssi : diacritSpecSetCands) 
			depth1set.put(dssi, featsToPossibleDiacritics.get(dssi).get(0));  //get 0 -- if two diacritics indicate the same set of features, arbitrarily choose the first. 
		combinedSpecSetCandsByDepth.put(1,depth1set); 
				
		while (depth < diacritSpecSetCands.size() && depth <= MAX_DIACRIT && depth1set.size() > 0 && combinedSpecSetCandsByDepth.containsKey(depth-1))
		{
			HashMap<String,String> currDepthSet = new HashMap<String,String>(); 
			for ( String existingStackFeats : combinedSpecSetCandsByDepth.get(depth-1).keySet()) 
			{
				for ( String addend : diacritSpecSetCands) // for each, try adding another diacrit onto it... 
				{
					// bypass if it's already in the stack... 
					if (combinedSpecSetCandsByDepth.get(depth-1).get(existingStackFeats).contains(
							featsToPossibleDiacritics.get(addend).get(0)))
						continue;
					
					List<String> currFeatSpecCombo = new ArrayList<String>(); 
					currFeatSpecCombo.add(existingStackFeats); 
					currFeatSpecCombo.add(addend); 
					
					// bypass if there would be any feature conflicts. 
					if (!detectFeatConflicts(currFeatSpecCombo).equals(""))
						continue; 
					
					//bypass if the features being added are already included. 
					List<String> feats_to_add = Arrays.asList(addend.split(""+RESTR_DELIM)); 
					int fai= 0; 
					while (fai < feats_to_add.size()) {
						if ( existingStackFeats.contains( feats_to_add.get(fai) ) )
							feats_to_add.remove(fai); 
						else	fai++; 
					}
					if (feats_to_add.size() == 0) // bypass
						continue; 
				
					String resultFeatVect = existingStackFeats + RESTR_DELIM 
							+ (feats_to_add.size() == 1 ? feats_to_add.get(0)
									: String.join(""+RESTR_DELIM, feats_to_add)); 
					
					String comboSuffix = 
							combinedSpecSetCandsByDepth.get(depth-1).get(existingStackFeats) // existing diacritics
							+ featsToPossibleDiacritics.get(addend).get(0);  // newly added material. 
					
					FeatMatrix curCandFM = getFeatMatrix(resultFeatVect, apply_ft_impls);
					if (curCandFM.compareToFeatVect(unseenVect)) //valid candidate 
					{
						for (String baseFeatVect : featsToSymbMap.keySet())
						{
							String candDiacritResult = curCandFM.forceTruthOnFeatVect
									(baseFeatVect).replace(Integer.toString(DESPEC_INT), 
											Integer.toString(UNSPEC_INT)); 
							String baseSymb = featsToSymbMap.get(baseFeatVect);
							
							List<String> diacritsInvolved = diacritsFoundInPhoneSymb(baseSymb); 
							//diacritsInvolved.add(comboSuffix);  -- causes some errors for combined diacritics. Replaced with w. below. 
							diacritsInvolved.addAll(diacritsFoundInPhoneSymb(comboSuffix)); 
							
							if (candDiacritResult.equals(unseenVect)
									&& !detectDiacritFeatConflicts(diacritsInvolved,true).equals(""))	// then we found it!!
							{						
								String newSymb = baseSymb + comboSuffix;
								defineFeatVect(unseenVect,newSymb); 
								// preference for first diacritic defined for this feature set. 
								if (VERBOSE)
									System.out.println("New symbol '"+newSymb+"' defined to represent feat vect "+unseenVect+"."); 
								return true; 
							}
						}
					}
					
					currDepthSet.put(resultFeatVect , comboSuffix); 
				}
			}
			combinedSpecSetCandsByDepth.put(depth, currDepthSet); 
			depth++; 
		}
		
		
		if (depth >= diacritSpecSetCands.size() || depth >= MAX_DIACRIT || !combinedSpecSetCandsByDepth.containsKey(depth-1))
		{
			String newSymb = "?"+Integer.toString(NUM_UNDEFINED_PHONES_USED++); 
			System.out.println("Warning: tried to generate diacritized symbol for unseen feature spec combination, \n"
					+ spellOutFeatVect(unseenVect)+"...\n"
					+ "but failed to find an appropriate base symbol + diacritics combination.\n"
					+ "Now using the following symbol: "+newSymb); 
			defineFeatVect(unseenVect,newSymb); 
		}
		
		return false;		
	}
	
	public static String generatePhoneSymbol(String featString)
	{
		if(featsToSymbMap.containsKey(featString))	return featsToSymbMap.get(featString);
		return !diacriticsExtracted ? "?"
				: (tryDefineUnseenFeatVect(featString, true) ? featsToSymbMap.get(featString) : "?");
	}
	
	public static ErrorAnalysis setupErrorAnalysis(Lexicon currResult, Lexicon gold) 
	{
		return new ErrorAnalysis(currResult, gold,  
				feats_weighted ? new FED(featsByIndex.length, FT_WTS,ID_WT,contextualize_FED) 
						: new FED(featsByIndex.length, ID_WT,contextualize_FED));
	}
	
	public static boolean hasValidFeatSpecList(String inp)
	{
		if(isValidFeatSpecList(inp.trim()))		return true; 
		String[] protophones = inp.split(""+PH_DELIM);
		for(int ppi = 0; ppi < protophones.length; ppi++)
		{
			String curpp = ""+protophones[ppi].trim();
			if(curpp.contains("["))	curpp = curpp.substring(curpp.indexOf('[')+1);
			if(curpp.contains("]"))	curpp = curpp.substring(0, curpp.indexOf(']'));
			if(isValidFeatSpecList(curpp))	return true; 
		}
		return false; 
	}
	
	/** isValidFeatSpecList
	 * @return @true iff @param input consists of a list of valid feature specifications 
	 * 	each delimited by restrDelim
	 * @note unlike the one in SChangeFactory, this does not take into account possible negative alpha proxies. 
	 */
	public static boolean isValidFeatSpecList(String input)
	{
		String[] specs = input.split(""+RESTR_DELIM); 
		
		for(int si = 0; si < specs.length; si++)	
			if (!ordFeatNames.contains(specs[si].substring(1)))	return false;
		return true; 
	}
	
	/**
	 * @param input -- a single spec : (+)/-/0 (alpha) feat. 
	 * @note does handle neg alpha specs
	 * @return @true @iff it's valid. 
	 */
	public static boolean isValidFeatSpecInclAlphPrep(String inpspec)
	{
		if (inpspec.length() < 2)	return false; 

		// true if it's a basic spec, no alpha, or if it's simple alpha (or neg alpha!)  + feat. 
		if (UTILS.ordFeatNames.contains(inpspec.substring(1)))	return true; 
		
		if (inpspec.length() < 3  || !UTILS.ALL_FTSPEC_MARKS.contains(""+inpspec.charAt(0))) return false ; 
				
		// at this point, possibility is that it could be preposed alpha... -- proxy or not doens't really matter. 
		return UTILS.ALL_FTSPEC_MARKS.contains(""+inpspec.charAt(0)) && UTILS.ordFeatNames.contains(inpspec.substring(2)); 
	}
	
	/** isValidFeatSpecList
	 * @return @true iff @param input consists of a list of valid feature specifications 
	 * 	each delimited by restrDelim
	 */
	public static boolean isValidFeatSpecList(String input, boolean allowPreposedAlpha)
	{
		if (!allowPreposedAlpha)	return isValidFeatSpecList(input); 
		
		String[] specs = input.split(""+UTILS.RESTR_DELIM); 
		
		for(int si = 0; si < specs.length; si++)	
			if (!isValidFeatSpecInclAlphPrep(specs[si]))
				return false;
		return true; 
	}
	
	/**
	 * if @param featSpecs is not already bracketed, @return it [bracketed]
	 */
	public static String bracketFSpecs (String featSpecs)
	{	return fSpecsAreBracketed(featSpecs) ? featSpecs: "[" + featSpecs +"]" ; 	}
	public static String debracketFSpecs (String featSpecs)
	{	return fSpecsAreBracketed(featSpecs) ? featSpecs.substring(featSpecs.indexOf("[")+1, featSpecs.indexOf("]")) :  featSpecs; }
	public static boolean fSpecsAreBracketed (String specs)
	{
		if (specs.strip().charAt(0) != '[')	return false; 
		if (!specs.contains("]"))	return false;
		if (specs.indexOf("]") == specs.length() -1 )	return true;
		return specs.substring(specs.indexOf("]")+1).strip().equals("");
	}
	
	//derives FeatMatrix object instance from String of featSpec instances
	public FeatMatrix getFeatMatrix(String featSpecs)
	{	return getFeatMatrix(featSpecs, false, new HashMap<String, String>());	}
	public static FeatMatrix getFeatMatrix(String featSpecs, boolean apply_ft_impls)
	{	return getFeatMatrix(featSpecs, apply_ft_impls, new HashMap<String, String>());	}
	// if negProxyAlphs is empty, functionally there are none.
	public static FeatMatrix getFeatMatrix(String featSpecs, boolean apply_ft_impls, HashMap<String, String> negProxyAlphs)
	{
		if(! isValidFeatSpecList(featSpecs) ) // however this will throw a negative due to neg prox usage so need to handle that pre this method call 
			throw new RuntimeException("Error : preempted attempt to get FeatMatrix from an invalid list of feature specifications."
					+ "\nAttempted feat specs: "+featSpecs); 
		
		String theFeatSpecs = apply_ft_impls ? applyImplications(featSpecs) : featSpecs+"";
		
		if(theFeatSpecs.contains("0") && !apply_ft_impls)
			throw new RuntimeException(
			"Error : despecification used for a FeatMatrix that is not in the destination -- this is inappropriate."); 
		
		theFeatSpecs = applyNegalphaProxies("["+theFeatSpecs+"]", negProxyAlphs).substring(1, theFeatSpecs.length()+1); 
		
		return negProxyAlphs.size() == 0 ? new FeatMatrix(theFeatSpecs, ordFeatNames) : 
			new FeatMatrix(theFeatSpecs, ordFeatNames, negProxyAlphs);
	}
	
	/**	applyImplications
	 * modifies a list of features which will presumably be used to define a FeatMatrix 
	 * so that the implications regarding the specification or non-specifications of certain features are adhered to 
	 * @param featSpecs, feature specifications before application of the stored implications
	 */
	public static String applyImplications (String featSpecs) 
	{
		if (! isValidFeatSpecList(featSpecs) )
			throw new RuntimeException("Error : preempted attempt to apply implications to an invalid list of feature specifications"); 
		String[] theFeatSpecs = featSpecs.trim().split(""+RESTR_DELIM); 
		String output = ""+featSpecs; 
		
		for(int fsi = 0 ; fsi < theFeatSpecs.length; fsi++)
		{
			String currSpec = theFeatSpecs[fsi]; 
			
			if(featsWithImplications.contains(currSpec)) 
			{
				String[] implications = FT_IMPLICATIONS.get(currSpec); 
				for (int ii = 0; ii < implications.length; ii++)
				{	if (output.contains(implications[ii].substring(1)) == false)
					{	
						output += RESTR_DELIM + implications[ii];
						theFeatSpecs = output.trim().split(""+RESTR_DELIM); 		
			}}}
			
			if("+-".contains(currSpec.substring(0,1)))
			{
				if(featsWithImplications.contains(currSpec.substring(1)))
				{
					String[] implications = FT_IMPLICATIONS.get(currSpec.substring(1)); 
					for (int ii=0; ii < implications.length; ii++)
					{	if(output.contains(implications[ii]) == false)
						{
							output += RESTR_DELIM + implications[ii]; 
							theFeatSpecs = output.trim().split(""+RESTR_DELIM); 
						}
			}}}
		}
		
		return output; 
	}
	
	public static String stripEnds(String inp)
	{
		return inp.replaceAll("^[ \t]+|[ \t]+$","");
	}
	
	public static String append_space_to_x (String in, int x)
	{
		if (in.length() >= x)	return in;
		String out = in + " ";
		while (out.length() < x)	out += " ";
		return out;
	}
		
	public static boolean isNumeric (String s)
	{
		try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
	}
}
