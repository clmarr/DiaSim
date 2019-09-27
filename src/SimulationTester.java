import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * for ensuring functionality of DHSAnalysis, ErrorAnalysis, FED, Simulation, and related classes, ultimately DiachronicSimulator as well. 
 * @author Clayton Marr
 *
 */

public class SimulationTester {

	private static final String DBG_WRKG_CASC = "DebugWorkingCasc",
			DBG_GOLD_CASC = "DebugGoldCasc" , DBG_START_CASC = "DebugStartCasc"; 
	private static final String SYMBS_LOC = "symbolDefs.csv";
	private static final String LEX_LOC = "DebugDummyLexicon.txt"; 
	private static final String FI_LOC = "FeatImplications"; 
	private static double ID_WT = 0.5; 
	
	private static String[] featsByIndex; 
	private static HashMap<String, Integer> featIndices;

	private static boolean feats_weighted;
	private static double[] FT_WTS; 
	
	private static HashMap<String, String> phoneSymbToFeatsMap;
	private static HashMap<String, String[]> featImplications; 
	
	private static boolean goldStagesSet, blackStagesSet; 
	private static int NUM_ETYMA, NUM_GOLD_STAGES, NUM_BLACK_STAGES; 
	private static LexPhon[] inputForms; 
	private static Lexicon goldOutputLexicon;

	private static boolean goldOutput; 
	private static String[] goldStageNames, blackStageNames; 
	private static Lexicon[] goldStageGoldLexica; //indexes match with those of customStageNames 
		//so that each stage has a unique index where its lexicon and its name are stored at 
			// in their respective lists.
	private static int[] goldStageInstants, blackStageInstants; // i.e. the index of custom stages in the ordered rule set

	
	
	private static List<SChange> CASCADE;

	
	public static void main(String args[])
	{
		initWorkingCascFile(); 

		extractSymbDefs(); 
		extractFeatImpls();
		
		System.out.println("Creating SChangeFactory...");
		SChangeFactory theFactory = new SChangeFactory(phoneSymbToFeatsMap, featIndices, featImplications); 
		
		extractCascAndLex(theFactory, DBG_GOLD_CASC); 
			// first extracting from gold casc so that we do initial sanity test of the correct cascade leading to correct forms
				// with all metrics agreeing with this etc etc. 
		
				
		System.out.println("Lexicon extracted. Now debugging.");
		
		System.out.println("First -- checking agreement of gold cascade with gold lexicon."); 
		System.out.println("Sanity check -- input forms should be 100% correct checked against input forms."); 
		
		//TODO check stage forms
		
		//TODO check final forms
		
		//TODO use working casc now 
		//TODO and check different diagnostics
		//TODO including by making edits to the Working casc file. 
		
		

	}
	
	private static void extractSymbDefs()
	{
		System.out.println("Collecting symbol definitions...");
		
		List<String> symbDefsLines = new ArrayList<String>();
		String nextLine; 
		
		try 
		{	File inFile = new File(SYMBS_LOC); 
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
		
		System.out.println("Symbol definitions extracted!");
		System.out.println("Length of symbDefsLines : "+symbDefsLines.size()); 
		

		//from the first line, extract the feature list and then the features for each symbol.
		featsByIndex = symbDefsLines.get(0).replace("SYMB,", "").split(""+DiachronicSimulator.FEAT_DELIM); 
		
		for(int fi = 0; fi < featsByIndex.length; fi++) featIndices.put(featsByIndex[fi], fi);
		
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
		int li = 1; 
		while (li < symbDefsLines.size()) 
		{
			nextLine = symbDefsLines.get(li).replaceAll("\\s+", ""); //strip white space and invisible characters 
			int ind1stComma = nextLine.indexOf(DiachronicSimulator.FEAT_DELIM); 
			String symb = nextLine.substring(0, ind1stComma); 
			String[] featVals = nextLine.substring(ind1stComma+1).split(""+DiachronicSimulator.FEAT_DELIM); 		
			
			String intFeatVals = ""; 
			for(int fvi = 0; fvi < featVals.length; fvi++)
			{
				if(featVals[fvi].equals(""+DiachronicSimulator.MARK_POS))	intFeatVals+= DiachronicSimulator.POS_INT; 
				else if (featVals[fvi].equals(""+DiachronicSimulator.MARK_UNSPEC))	intFeatVals += DiachronicSimulator.UNSPEC_INT; 
				else if (featVals[fvi].equals(""+DiachronicSimulator.MARK_NEG))	intFeatVals += DiachronicSimulator.NEG_INT; 
				else	throw new Error("Error: unrecognized feature value, "+featVals[fvi]+" in line "+li);
			}
			
			phoneSymbToFeatsMap.put(symb, intFeatVals);
			li++; 
		}
	}
	
	public static void extractFeatImpls()
	{
		featImplications = new HashMap<String, String[]>(); 
		
		String nextLine; 
		
		System.out.println("Now extracting info from feature implications file...");
		
		List<String> featImplLines = new ArrayList<String>(); 
		
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream(FI_LOC), "UTF-8")); 
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
			String[] fisides = filine.split(""+DiachronicSimulator.IMPLICATION_DELIM); 
			featImplications.put(fisides[0], fisides[1].split(""+DiachronicSimulator.FEAT_DELIM));
		}
		
		System.out.println("Done extracting feature implications!");	
	}
	

	public static void extractCascAndLex(SChangeFactory theFactory, String CASC_LOC)
	{
		System.out.println("Now extracting diachronic sound change rules from rules file...");
		
		List<String> rulesByTimeInstant = new ArrayList<String>(); 

		String nextRuleLine;
		
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader ( 
				new FileInputStream(CASC_LOC), "UTF-8")); 
			
			while((nextRuleLine = in.readLine()) != null)
			{
				String lineWithoutComments = ""+nextRuleLine; 
				if (lineWithoutComments.contains(""+DiachronicSimulator.CMT_FLAG))
						lineWithoutComments = lineWithoutComments.substring(0,
								lineWithoutComments.indexOf(""+DiachronicSimulator.CMT_FLAG));
				if(!lineWithoutComments.trim().equals(""))	rulesByTimeInstant.add(lineWithoutComments); 
			}
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
		
		//now filter out the stage name declaration lines.
		
		List<String> goldStageNameAndLocList = new ArrayList<String>(); //to be collected 
		//until the end of collection, at which point the appropriate arrays for the custom
		// stages will be created using this List. These ones will be compared to gold.
		List<String>blackStageNameAndLocList = new ArrayList<String>();
			// same as above, but will not be compared to gold. 
		
		goldStagesSet = false; blackStagesSet=false;  
				
		int rli = 0; 
		
		while (rli < rulesByTimeInstant.size())
		{
			String currRule = rulesByTimeInstant.get(rli); 
			
			if ( (""+DiachronicSimulator.GOLD_STAGENAME_FLAG+DiachronicSimulator.BLACK_STAGENAME_FLAG).contains(""+currRule.charAt(0)))
			{
				if ( currRule.charAt(0) == DiachronicSimulator.GOLD_STAGENAME_FLAG)
				{
					goldStagesSet = true; 
					assert rli != 0: "Error: Stage set at the first line -- this is useless, redundant with the initial stage ";
					
					currRule = currRule.substring(1); 
					assert !currRule.contains(""+DiachronicSimulator.GOLD_STAGENAME_FLAG): 
						"Error: stage name flag <<"+DiachronicSimulator.GOLD_STAGENAME_FLAG+">> occuring in a place besides the first character in the rule line -- this is illegal: \n"+currRule; 
					assert !currRule.contains(DiachronicSimulator.STAGENAME_LOC_DELIM+""):
						"Error: illegal character found in name for custom stage -- <<"+DiachronicSimulator.STAGENAME_LOC_DELIM+">>";  
					goldStageNameAndLocList.add(""+currRule+DiachronicSimulator.STAGENAME_LOC_DELIM+rli);
					rulesByTimeInstant.remove(rli);  
				}
				else if (currRule.charAt(0) == DiachronicSimulator.BLACK_STAGENAME_FLAG)
				{
					blackStagesSet =true;
					currRule = currRule.substring(1); 
					assert !currRule.contains(DiachronicSimulator.STAGENAME_LOC_DELIM+""):
						"Error: illegal character found in name for custom stage -- <<"+DiachronicSimulator.STAGENAME_LOC_DELIM+">>";  
					blackStageNameAndLocList.add(""+currRule+DiachronicSimulator.STAGENAME_LOC_DELIM+rli);
					rulesByTimeInstant.remove(rli); 
				}
				else	rulesByTimeInstant.remove(rli); 
			}
			else	rli++;
		}
		
		NUM_GOLD_STAGES = goldStageNameAndLocList.size(); 
		NUM_BLACK_STAGES =blackStageNameAndLocList.size();
		
		System.out.println("Using "+NUM_GOLD_STAGES+" custom stages."); 
		
		if (NUM_GOLD_STAGES > 0)
		{
			System.out.print("Gold stages: ");
			for (String gs : goldStageNameAndLocList)
				System.out.print(gs.substring(0,gs.indexOf(DiachronicSimulator.STAGENAME_LOC_DELIM))+",");
			System.out.println(""); 
		}
		  
		if (NUM_BLACK_STAGES > 0)
		{
			System.out.print("Black stages:");
			for (String bs : blackStageNameAndLocList)
				System.out.print(bs.substring(0,bs.indexOf(DiachronicSimulator.STAGENAME_LOC_DELIM))+",");
			System.out.println(""); 
		}
		
		goldStageGoldLexica = new Lexicon[NUM_GOLD_STAGES]; 
		goldStageNames = new String[NUM_GOLD_STAGES];
		blackStageNames = new String[NUM_BLACK_STAGES];
		goldStageInstants = new int[NUM_GOLD_STAGES]; 
		blackStageInstants = new int[NUM_BLACK_STAGES]; 
		
		// parse the rules
		CASCADE = new ArrayList<SChange>();
		
		int cri = 0, gsgi =0 , bsgi = 0, next_gold = -1, next_black = -1;
		if (goldStagesSet)	next_gold = Integer.parseInt(goldStageNameAndLocList.get(gsgi).split(""+DiachronicSimulator.STAGENAME_LOC_DELIM)[1]);
		if (blackStagesSet)	next_black = Integer.parseInt(blackStageNameAndLocList.get(bsgi).split(""+DiachronicSimulator.STAGENAME_LOC_DELIM)[1]);
		
		for(String currRule : rulesByTimeInstant)
		{
			CASCADE.addAll(theFactory.generateSoundChangesFromRule(currRule));
			
			if(goldStagesSet)
			{
				if (cri == next_gold)
				{
					goldStageNames[gsgi] = goldStageNameAndLocList.get(gsgi).split(""+DiachronicSimulator.STAGENAME_LOC_DELIM)[0];
					goldStageInstants[gsgi] = CASCADE.size();		
					gsgi += 1;
					if ( gsgi < NUM_GOLD_STAGES)
						next_gold = Integer.parseInt(goldStageNameAndLocList.get(gsgi).split(""+DiachronicSimulator.STAGENAME_LOC_DELIM)[1]);
				}
			}
			
			if(blackStagesSet)
			{
				if (cri == next_black)
				{
					blackStageNames[bsgi] = blackStageNameAndLocList.get(bsgi).split(""+DiachronicSimulator.STAGENAME_LOC_DELIM)[0];
					blackStageInstants[bsgi] = CASCADE.size();
					bsgi += 1;
					if (bsgi < NUM_BLACK_STAGES)
						next_black = Integer.parseInt(blackStageNameAndLocList.get(bsgi).split(""+DiachronicSimulator.STAGENAME_LOC_DELIM)[1]);
				}
			}
			
			cri++; 
		}
		
		System.out.println("Diachronic rules extracted. "); 
		
		//now input lexicon 
		//collect init lexicon ( and gold for stages or final output if so specified) 
		//copy init lexicon to "evolving lexicon" 
		//each time a custom stage time step loc (int in the array goldStageTimeInstantLocs or blackStageTimeInstantLocs) is hit, save the 
		// evolving lexicon at that point by copying it into the appropriate slot in the customStageLexica array
		// finally when we reach the end of the rule list, save it as testResultLexicon
		
		System.out.println("Now extracting lexicon...");
		String nextLine; 
		
		List<String> lexFileLines = new ArrayList<String>(); 
		
		try 
		{	File inFile = new File(LEX_LOC); 
			BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream(inFile), "UTF8"));
			while((nextLine = in.readLine()) != null)	
			{	if (nextLine.contains(DiachronicSimulator.CMT_FLAG+""))
					nextLine = nextLine.substring(0,nextLine.indexOf(DiachronicSimulator.CMT_FLAG)).trim(); 
				if (!nextLine.equals("")) 	lexFileLines.add(nextLine); 		
			}
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

		// now extract 
		NUM_ETYMA = lexFileLines.size(); 
		String[] initStrForms = new String[NUM_ETYMA]; 
		
		String theLine =lexFileLines.get(0); 
		String firstlineproxy = ""+theLine; 
		int numCols = 1; 
		while (firstlineproxy.contains(""+DiachronicSimulator.LEX_DELIM))
		{	numCols++; 
			firstlineproxy = firstlineproxy.substring(firstlineproxy.indexOf(""+DiachronicSimulator.LEX_DELIM)+1); 
		}
		goldOutput =false; 
		if(numCols == NUM_GOLD_STAGES + 2)
			goldOutput = true; 
		else
			assert numCols == NUM_GOLD_STAGES + 1: "Error: mismatch between number of columns in lexicon file and number of gold stages declared in rules file (plus 1)\n"
					+ "# stages in rules file : "+NUM_GOLD_STAGES+"; # cols : "+numCols;
		
		boolean justInput = (numCols == 0); 
		
		inputForms = new LexPhon[NUM_ETYMA];
		LexPhon[] goldResults = new LexPhon[NUM_ETYMA];  
		LexPhon[][] goldForms = new LexPhon[NUM_GOLD_STAGES][NUM_ETYMA];

		int lfli = 0 ; //"lex file line index"
		
		while(lfli < NUM_ETYMA)
		{
			theLine = lexFileLines.get(lfli);
			
			initStrForms[lfli] = justInput ? theLine : theLine.split(""+DiachronicSimulator.LEX_DELIM)[0]; 
			inputForms[lfli] = parseLexPhon(initStrForms[lfli]);
			if (!justInput)
			{
				String[] forms = theLine.split(""+DiachronicSimulator.LEX_DELIM); 
				if(NUM_GOLD_STAGES > 0)
					for (int gsi = 0 ; gsi < NUM_GOLD_STAGES ; gsi++)
						goldForms[gsi][lfli] = parseLexPhon(forms[gsi+1]);
				if (goldOutput)
					goldResults[lfli] = parseLexPhon(forms[NUM_GOLD_STAGES+1]);
			}
			lfli++;
			if(lfli <NUM_ETYMA)
				assert numCols == colCount(theLine): "ERROR: incorrect number of columns in line "+lfli;
		}		

		//NOTE keeping gold lexica around solely for purpose of initializing Simulation objects at this point.
		if(NUM_GOLD_STAGES > 0)
			for (int gsi = 0 ; gsi < NUM_GOLD_STAGES; gsi++)
				goldStageGoldLexica[gsi] = new Lexicon(goldForms[gsi]); 
		
		if(goldOutput)	
			goldOutputLexicon = new Lexicon(goldResults); 

		
	}

	/** auxiliary.
	 * given String @param toLex
	 * @return its representation as a LexPhon containing a sequence of Phone instances
	 * TODO note we assume the phones are separated by PH_DELIM (presumably ' ') 
	 */
	private static LexPhon parseLexPhon(String toLex)
	{
		if (toLex.contains(DiachronicSimulator.ABSENT_PH_INDIC))
		{	return new AbsentLexPhon();	}
		
		String[] toPhones = toLex.trim().split(""+DiachronicSimulator.PH_DELIM);
		
		List<SequentialPhonic> phones = new ArrayList<SequentialPhonic>(); //LexPhon class stores internal List of phones not an array,
			// for better ease of mutation

		for (String toPhone : toPhones)
		{
			if (toPhone.equals("#") || toPhone.equals("+"))
				phones.add(new Boundary(toPhone.equals("#") ? "word bound" : "morph bound"));
			else
			{
				assert phoneSymbToFeatsMap.containsKey(toPhone): 
					"Error: tried to declare a phone in a word in the lexicon using an invalid symbol.\nSymbol is : '"+toPhone+"', length = "+toPhone.length(); 
				phones.add(new Phone(phoneSymbToFeatsMap.get(toPhone), featIndices, phoneSymbToFeatsMap));
			}
		}
		return new LexPhon(phones);
	}
	
	//auxiliary method -- get number of columns in lexicon file. 
	private static int colCount(String str)
	{
		String proxy = str+"";
		int i = proxy.indexOf(""+DiachronicSimulator.LEX_DELIM), c = 1 ;
		while( i > -1)
		{
			c++; 
			proxy = proxy.substring(i+1);
			i = proxy.indexOf(","); 
		}
		return c; 
	}
	
	/**
	 * reset DBG_WRKG_CASC to be a copy of DBG_START_CASC
	 */
	private static void initWorkingCascFile()
	{
		InputStream is = null; 
		OutputStream os = null; 
		try {
			is = new FileInputStream(DBG_START_CASC); 
			os = new FileOutputStream(DBG_WRKG_CASC); 
			byte[] buffer = new byte[1024]; 
			int length;
			while ((length = is.read(buffer)) > 0 ) { 
				os.write(buffer, 0 , length);
			}
			is.close();
			os.close(); 
		}
		catch (IOException e) {
			System.out.println("IO Exception!");
			e.printStackTrace();
		}
	}
	
}
