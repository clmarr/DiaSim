import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class UTILS {

	public final static char MARK_POS = '+', MARK_NEG = '-', MARK_UNSPEC = '0', FEAT_DELIM = ','; 
	public final static String FEATSPEC_MARKS = ""+MARK_POS+MARK_NEG+MARK_UNSPEC;
	public final static int POS_INT = 2, NEG_INT = 0, UNSPEC_INT = 1;
	public final static char IMPLICATION_DELIM=':', PH_DELIM = ' ', DIACRITICS_DELIM='='; 
	public final static char CMT_FLAG = '$'; //marks that the text after is a comment in the sound rules file, thus doesn't read the rest of the line
	public final static char GOLD_STAGENAME_FLAG = '~', BLACK_STAGENAME_FLAG ='=';
	public final static char STAGENAME_LOC_DELIM = ':'; 
	public final static char LEX_DELIM =','; 
	public final static char STAGE_PRINT_DELIM = ',';  
	public final static String OUT_GRAPH_FILE_TYPE = ".csv"; 
	public final static String ABSENT_PH_INDIC = "...";
	public final static int maxAutoCommentWidth = 150;
	public static final int PRINTERVAL = 100; 

	public static int getFeatspecIntFromMark (char mark) 
	{
		if (!FEATSPEC_MARKS.contains(""+mark))
			throw new RuntimeException("Invalid feature specification mark :"+mark+"; allowed marks are :"+FEATSPEC_MARKS); 
		if (mark == MARK_POS)	return POS_INT; 
		if (mark == MARK_NEG)	return NEG_INT; 
		return UNSPEC_INT; // this last line shouldn't ever really happen. Diacritics should not be used to unspecify features. 
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

	public static String etymInds(LexPhon[] etList, LexPhon etTarg)
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
		if (ogs.hasGoldOutput()) toy.setGold(ogs.getGoldOutput().getWordList()); 
		if (ogs.hasGoldStages()) toy.setGoldStages(ogs.getGoldStageGoldForms(), ogs.getGoldStageNames(), ogs.getGoldStageInstants());
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
			
			BufferedWriter out = new BufferedWriter(new FileWriter(filename)); 
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
	{
		return line.replace(" ","").length() == 0;
	}
	

	//auxiliary method -- get number of columns in lexicon file. 
	public static int colCount(String str)
	{
		String proxy = str+"";
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
	public static boolean checkWord(LexPhon correct, LexPhon observed, String errMessage)
	{
		String c = correct.print(), o = observed.print(); 
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
	
	public static String errorMessage(String cor, String obs, String msg)
	{
		return msg.replace("%c", cor).replace("%o",obs); 
	}
	
	// @param ec -- error count
	public static void errorSummary(int ec)
	{
		if (ec == 0)	System.out.println("No errors yet at this point."); 
		else	System.out.println("In all "+ec+" errors.");
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
	public static int aggregateErrorsCheckWordLists(LexPhon[] g, LexPhon[] obs)
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
	
	//extract order of stages so that we don't end up with ``flips'' in the relative ordering between stages
		// in the case that they end up in the same
		// chronological "moment" between rule operation steps (TODO need to clarify this a bit further maybe?) 
	public static String[] extractStageOrder(String cascLoc)
	{
		List<String> lines = readFileLines(cascLoc); 
		int li = 0; 
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
			out[li] = lines.get(li).charAt(0) == GOLD_STAGENAME_FLAG ? "g"+(ngi++) : "b"+(nbi++);
			li++;
		}
		return out; 
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
		boolean priorSpecified = inputPrior.strip().length() > 0, 
				postrSpecified = inpSplit.length == 1 ? false : inpSplit[1].strip().length() > 0;
		if (inpSplit.length > 2)	throw new RuntimeException("Context specification cannot have multiple locuses!");
		String inputPostr = inpSplit.length == 2 ? inpSplit[1] : ""; 
		
		if(priorSpecified)
			if (checkForBracingError(inputPrior))
				throw new RuntimeException("Invalid input for prior context stipulation: "+inputPrior); 
		if(postrSpecified)
			if(checkForBracingError(inputPostr))
				throw new RuntimeException("Invalid input for posterior context stipulation: "+inputPostr); 
		
		return getBraceDisjunctions(impCtxt); 
	}
	
	//error if any opener { braces are not closed by a closer } brace or vice versa.
	// @true if there is such erroneous usage. 
	public static boolean checkForBracingError(String s) 
	{
		String SD = SChangeFactory.segDelim + ""; 
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
	
	public static List<String> getBraceDisjunctions(String inp)
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
			if (curr == SChangeFactory.segDelim && braceDepth == 1) 
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
					getBraceDisjunctions(inp.substring(0, openerInd) + disj + inp.substring(checkInd+1)));
		
		return out; 
	}
	
	public static boolean suffixIs(String str, String cand)
	{
		return str.substring(str.length()-cand.length()).equals(cand); 
	}
}
