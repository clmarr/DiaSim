import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

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
	
	public static Simulation toyDerivation(LexPhon[] inps, List<SChange> ruleCascade )
	{
		Simulation toy = new Simulation(inps, ruleCascade); 
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
	
	//TODO for debugging purposes
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
	
	public static boolean compareCascades(List<SChange> c1, List<SChange> c2)
	{
		assert c1.size() == c2.size() : "Error: tried to compare two cascades of different lengths..."; 
		for(int ci = 0; ci < c1.size(); ci++)
			if (!c1.get(ci).toString().equals(""+c2.get(ci)))
				return false;
		return true;
	}
	
	//should ONLY be called after the derivation has been globalized by means of DifferentialHypothesisSimulator.derivationToGlobalInds()
	public static int extractGlobalInd(String dl)
	{
		return Integer.parseInt( dl.substring( dl.indexOf("[") + 1, dl.indexOf("]"))); 
	}
}
