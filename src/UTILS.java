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
	public final static int POS_INT = 2, NEG_INT = 0, UNSPEC_INT = 1;
	public final static char IMPLICATION_DELIM=':', PH_DELIM = ' '; 
	public final static char CMT_FLAG = '$'; //marks taht the text after is a comment in the sound rules file, thus doesn't read the rest of the line
	public final static char GOLD_STAGENAME_FLAG = '~', BLACK_STAGENAME_FLAG ='=';
	public final static char STAGENAME_LOC_DELIM = ':'; 
	public final static char LEX_DELIM =','; 
	public final static char STAGE_PRINT_DELIM = ',';  
	public final static String OUT_GRAPH_FILE_TYPE = ".csv"; 
	public final static String ABSENT_PH_INDIC = "...";
	public final static int maxAutoCommentWidth = 150;
	public static final int PRINTERVAL = 100; 


	public static String fillSpaceToN(String inp, int n)
	{
		String out = inp+"";
		while (out.length() < n)	out += " ";
		return out; 
	}
	
	//to use for checking if an entered etymon or rule id is valid. 
	// max should be the number of words in the lexicon minus 1 (for an etymon)
		// or the length of the cascade (for a rule)
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
	
	public static Simulation toyDerivation(LexPhon[] inps, List<SChange> ruleCascade, String[] stageOrdering)
	{
		Simulation toy = new Simulation(inps, ruleCascade,stageOrdering); 
		toy.simulateToEnd();
		return toy; 
	}
	

	//auxiliary
	public static void writeToFile(String filename, String output)
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
			
			System.out.println("out loc : "+filename); 
			
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
	

	//auxiliary
	public static boolean isJustSpace(String line)
	{
		return line.replace(" ","").length() == 0;
	}
	

	//auxiliary method -- get number of columns in lexicon file. 
	public static int colCount(String str)
	{
		String proxy = str+"";
		int i = proxy.indexOf(""+UTILS.LEX_DELIM), c = 1 ;
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

	public static boolean phonSeqsEqual(List<SequentialPhonic> sp1, List<SequentialPhonic> sp2) {
		if (sp1.size() != sp2.size())
			return false;
		int spn = sp1.size();
		for (int spi = 0; spi < spn; spi++)
			if (!sp1.get(spi).equals(sp2.get(spi)))
				return false;
		return true;
	}
	
	public static String stdCols(int width, int[] vals)
	{
		String[] strvals = new String[vals.length] ;
		for (int vi = 0; vi < vals.length; vi++)	strvals[vi] = ""+vals[vi];
		return stdCols(width, strvals); 
	}
	
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
			return true; // since we now know it must start with hte proper char.
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
	//checker methods
	public static boolean compareCascades(List<SChange> c1, List<SChange> c2)
	{
		assert c1.size() == c2.size() : "Error: tried to compare two cascades of different lengths..."; 
		for(int ci = 0; ci < c1.size(); ci++)
			if (!c1.get(ci).toString().equals(""+c2.get(ci)))
				return false;
		return true;
	}
	
	public static boolean checkWord(LexPhon correct, LexPhon observed, String errMessage)
	{
		String c = correct.print(), o = observed.print(); 
		boolean result = c.equals(o); 
		if (!result)	System.out.println(errorMessage(c,o,errMessage)); 
		return result; 
	}
	
	public static boolean checkBoolean(boolean correct, boolean observed, String errMessage)
	{
		if (correct != observed)	System.out.println(errorMessage(""+correct,""+observed,errMessage)); 
		return correct == observed; 
	}
	
	public static String errorMessage(String cor, String obs, String msg)
	{
		return msg.replace("%c", cor).replace("%o",obs); 
	}
	
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
	
	public static boolean compareFiles(String loc1, String loc2)
	{
		List<String> f1lns = readFileLines(loc1), 
				f2lns = readFileLines(loc2); 
		if (f1lns.size() != f2lns.size())	return false; 
		for (int li = 0 ; li < f1lns.size(); li++)
			if(!f1lns.get(li).equals(f2lns.get(li)))	return false;
		return true;
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
	
	//extract order of stages so that we don't end up wiht switches in the case that they end up in the same
		// "moment" between rule operation steps 
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
	
	// auxiliary
	public static String print1dIntArr(int[] a)
	{
		String out = ""; 
		for (int ai : a)	out += ""+ai+","; 
		return out.substring(0, out.length()-1);
	}
	
	public static String print1dBoolArrAsIntArr(boolean[] b)
	{
		String out = "";
		for (boolean bi : b)	out += bi ? "1," : "0,"; 
		return out.substring(0, out.length() - 1 ); 
	}
	
	public static int findInt (int[] arr, int targ)
	{
		for(int i = 0 ; i < arr.length; i++)	if (arr[i] == targ)	return i;
		return -1; 
	}
	
}
