import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File; 
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap; 
import java.util.Scanner; 
import java.util.List;
import java.util.ArrayList;
import java.util.Collections; 

/**
 * main class for diachronic derivation system
 * @author Clayton Marr
 *
 */
public class DerivationSimulation {
	
	public final static char MARK_POS = '+', MARK_NEG = '-', MARK_UNSPEC = '0', FEAT_DELIM = ','; 
	public final static int POS_INT = 2, NEG_INT = 0, UNSPEC_INT = 1;
	public final static char IMPLICATION_DELIM=':', PH_DELIM = ' '; 
	private final static char CMT_FLAG = '$'; //marks taht the text after is a comment in the sound rules file, thus doesn't read the rest of the line
	private final static char GOLD_STAGENAME_FLAG = '~', BLACK_STAGENAME_FLAG ='=';
	private final static char STAGENAME_LOC_DELIM = ':'; 
	private final static char LEX_DELIM =','; 
	private static List<String> rulesByTimeInstant;
	private final static char STAGE_PRINT_DELIM = ',';  
	private final static String OUT_GRAPH_FILE_TYPE = ".csv"; 
	private final static String ABSENT_PH_INDIC = "...";
	
	private static String[] featsByIndex; 
	private static HashMap<String, Integer> featIndices;
	private static HashMap<String, String> phoneSymbToFeatsMap;
	private static HashMap<String, String> phoneFeatsToSymbMap; //TODO abrogate either this or the previous class variable
	private static HashMap<String, String[]> featImplications; 
	private static Lexicon initLexicon, testResultLexicon, goldResultLexicon;
	private static int NUM_ETYMA; 
	private static int NUM_GOLD_STAGES, NUM_BLACK_STAGES;
	private static String[] goldStageNames, blackStageNames; 
	private static Lexicon[] goldStageGoldLexica; //indexes match with those of customStageNames 
		//so that each stage has a unique index where its lexicon and its name are stored at 
			// in their respective lists.
	private static Lexicon[] goldStageResultLexica, blackStageResultLexica; 
	private static int[] goldStageTimeInstants, blackStageTimeInstants; // i.e. the index of custom stages in the ordered rule set
	private static boolean goldStagesSet, blackStagesSet; 
	private static String[] wordTrajectories; //stores derivation (form at every time step), with stages delimited by line breaks, of each word 
	
	private static int[] finLexLD; //if gold is input: Levenshtein distance between gold and testResult for each word.
	private static double[] finLexLak; //Lev distance between gold and testResult for each etymon divided by init form phone length for that etymon
	private static boolean[] finMissInds; //each index true if the word for this index
		// resulted in a missmatch between the gold and the test result
	
	private static List<int[]> stageLexLDs; //lexical Levenshtein Distance per stage.
	private static List<double[]> stageLexLaks;
	private static List<boolean[]> stageMissInds; 
	
	private static boolean goldOutput; 
	
	private static double PERFORMANCE; // for the final score of Levenshtein Distance / #phones, avgd over words = "Lakation" for now. 
	private static double ACCURACY; 
	private static double NEAR_ACCURACY; 
	private static int numCorrectEtyma; //number of words in final result correct.
	
	//to be set in command line...
	private static String runPrefix;
	private static String symbDefsLoc; 
	private static String featImplsLoc; 
	private static String ruleFileLoc; 
	private static String lexFileLoc;
	
	private static double id_wt; 
	private static boolean DEBUG_RULE_PROCESSING, DEBUG_MODE, print_changes_each_rule, stage_pause, ignore_stages; 
	private static int num_prob_phones_displayed = 10; //the top n phones most associated with errors... 
	
	private static int goldStageInd, blackStageInd; 
	
	private static boolean feats_weighted;
	private static double[] FT_WTS; 
	
	private static List<SChange> CASCADE;
	
	private static void extractSymbDefs()
	{

		//TODO debugging
		System.out.println("Collecting symbol definitions...");
		
		List<String> symbDefsLines = new ArrayList<String>();
		String nextLine; 
		
		try 
		{	File inFile = new File(symbDefsLoc); 
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

	}
	
	public static void main(String args[])
	{
		parseArgs(args); 
		
		featIndices = new HashMap<String, Integer>() ; 
		phoneSymbToFeatsMap = new HashMap<String, String>(); 
		phoneFeatsToSymbMap = new HashMap<String, String>(); 
		featImplications = new HashMap<String, String[]>(); 
		
		//collect task information from symbol definitions file. 
		
		extractSymbDefs(); 
		String nextLine; 
		
		
		//TODO debugging
		System.out.println("Now extracting info from feature implications file...");
		
		List<String> featImplLines = new ArrayList<String>(); 
		
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
		System.out.println("Creating SChangeFactory...");
		SChangeFactory theFactory = new SChangeFactory(phoneSymbToFeatsMap, featIndices, featImplications); 
		
		
		System.out.println("Now extracting diachronic sound change rules from rules file...");
		
		rulesByTimeInstant = new ArrayList<String>(); 

		String nextRuleLine;
		
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader ( 
				new FileInputStream(ruleFileLoc), "UTF-8")); 
			
			while((nextRuleLine = in.readLine()) != null)
			{
				String lineWithoutComments = ""+nextRuleLine; 
				if (lineWithoutComments.contains(""+CMT_FLAG))
						lineWithoutComments = lineWithoutComments.substring(0,
								lineWithoutComments.indexOf(""+CMT_FLAG));
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
			
			if ( (""+GOLD_STAGENAME_FLAG+BLACK_STAGENAME_FLAG).contains(""+currRule.charAt(0)))
			{
				if (ignore_stages)	rulesByTimeInstant.remove(rli); 
				else if ( currRule.charAt(0) == GOLD_STAGENAME_FLAG)
				{
					goldStagesSet = true; 
					assert rli != 0: "Error: Stage set at the first line -- this is useless, redundant with the initial stage ";
					
					currRule = currRule.substring(1); 
					assert !currRule.contains(""+GOLD_STAGENAME_FLAG): 
						"Error: stage name flag <<"+GOLD_STAGENAME_FLAG+">> occuring in a place besides the first character in the rule line -- this is illegal: \n"+currRule; 
					assert !currRule.contains(STAGENAME_LOC_DELIM+""):
						"Error: illegal character found in name for custom stage -- <<"+STAGENAME_LOC_DELIM+">>";  
					goldStageNameAndLocList.add(""+currRule+STAGENAME_LOC_DELIM+rli);
					rulesByTimeInstant.remove(rli);  
				}
				else if (currRule.charAt(0) == BLACK_STAGENAME_FLAG)
				{
					blackStagesSet =true;
					currRule = currRule.substring(1); 
					assert !currRule.contains(STAGENAME_LOC_DELIM+""):
						"Error: illegal character found in name for custom stage -- <<"+STAGENAME_LOC_DELIM+">>";  
					blackStageNameAndLocList.add(""+currRule+STAGENAME_LOC_DELIM+rli);
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
				System.out.print(gs.substring(0,gs.indexOf(STAGENAME_LOC_DELIM))+",");
			System.out.println(""); 
		}
		  
		if (NUM_BLACK_STAGES > 0)
		{
			System.out.print("Black stages:");
			for (String bs : blackStageNameAndLocList)
				System.out.print(bs.substring(0,bs.indexOf(STAGENAME_LOC_DELIM))+",");
			System.out.println(""); 
		}
		
		goldStageGoldLexica = new Lexicon[NUM_GOLD_STAGES];
		goldStageResultLexica = new Lexicon[NUM_GOLD_STAGES];
		blackStageResultLexica =new Lexicon[NUM_BLACK_STAGES];
		goldStageNames = new String[NUM_GOLD_STAGES];
		blackStageNames = new String[NUM_BLACK_STAGES];
		goldStageTimeInstants = new int[NUM_GOLD_STAGES]; 
		blackStageTimeInstants = new int[NUM_BLACK_STAGES]; 
		
		// parse the rules
		CASCADE = new ArrayList<SChange>();
		
		int cri = 0, gsgi =0 , bsgi = 0, next_gold = -1, next_black = -1;
		if (goldStagesSet)	next_gold = Integer.parseInt(goldStageNameAndLocList.get(gsgi).split(""+STAGENAME_LOC_DELIM)[1]);
		if (blackStagesSet)	next_black = Integer.parseInt(blackStageNameAndLocList.get(bsgi).split(""+STAGENAME_LOC_DELIM)[1]);
		
		for(String currRule : rulesByTimeInstant)
		{

			List<SChange> newShifts = theFactory.generateSoundChangesFromRule(currRule); 
			
			if(DEBUG_RULE_PROCESSING)
			{	System.out.println("Generating rules for rule number "+cri+" : "+currRule);
				for(SChange newShift : newShifts)
					System.out.println("SChange generated : "+newShift+", with type"+newShift.getClass());
			}
			CASCADE.addAll(theFactory.generateSoundChangesFromRule(currRule));
			
			if(goldStagesSet)
			{
				if (cri == next_gold)
				{
					goldStageNames[gsgi] = goldStageNameAndLocList.get(gsgi).split(""+STAGENAME_LOC_DELIM)[0];
					goldStageTimeInstants[gsgi] = CASCADE.size();		
					gsgi += 1;
					if ( gsgi < NUM_GOLD_STAGES)
						next_gold = Integer.parseInt(goldStageNameAndLocList.get(gsgi).split(""+STAGENAME_LOC_DELIM)[1]);
				}
			}
			
			if(blackStagesSet)
			{
				if (cri == next_black)
				{
					blackStageNames[bsgi] = blackStageNameAndLocList.get(bsgi).split(""+STAGENAME_LOC_DELIM)[0];
					blackStageTimeInstants[bsgi] = CASCADE.size();
					bsgi += 1;
					if (bsgi < NUM_BLACK_STAGES)
						next_black = Integer.parseInt(blackStageNameAndLocList.get(bsgi).split(""+STAGENAME_LOC_DELIM)[1]);
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
		
		List<String> lexFileLines = new ArrayList<String>(); 
		
		try 
		{	File inFile = new File(lexFileLoc); 
			BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream(inFile), "UTF8"));
			while((nextLine = in.readLine()) != null)	
			{	if (nextLine.contains(CMT_FLAG+""))
					nextLine = nextLine.substring(0,nextLine.indexOf(CMT_FLAG)).trim(); 
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
		wordTrajectories = new String[NUM_ETYMA]; 
				
		String theLine =lexFileLines.get(0); 
		String firstlineproxy = ""+theLine; 
		int numCols = 1; 
		while (firstlineproxy.contains(""+LEX_DELIM))
		{	numCols++; 
			firstlineproxy = firstlineproxy.substring(firstlineproxy.indexOf(""+LEX_DELIM)+1);
		}
		goldOutput =false; 
		if(numCols == NUM_GOLD_STAGES + 2)
			goldOutput = true; 
		else
			assert numCols == NUM_GOLD_STAGES + 1: "Error: mismatch between number of columns in lexicon file and number of gold stages declared in rules file (plus 1)\n"
					+ "# stages in rules file : "+NUM_GOLD_STAGES+"; # cols : "+numCols;
		
		boolean justInput = (numCols == 0); 
		
		LexPhon[] inputs = new LexPhon[NUM_ETYMA];
		LexPhon[] goldResults = new LexPhon[NUM_ETYMA];  //TODO may be unnecessary, if so delete. 
		List<LexPhon[]> goldForms = new ArrayList<LexPhon[]>(); //TODO may be unnecessary, if so delete.
		if (NUM_GOLD_STAGES >0)
			for (int gsi = 0 ; gsi<NUM_GOLD_STAGES; gsi++)	goldForms.add(new LexPhon[NUM_ETYMA]);
		
		int lfli = 0 ; //"lex file line index"
		
		while(lfli < NUM_ETYMA)
		{
			theLine = lexFileLines.get(lfli);
			
			wordTrajectories[lfli] = justInput ? theLine : theLine.split(""+LEX_DELIM)[0]; 
			inputs[lfli] = parseLexPhon(wordTrajectories[lfli]);
			if (!justInput)
			{
				String[] forms = theLine.split(""+LEX_DELIM); 
				if(NUM_GOLD_STAGES > 0)
					for (int gsi = 0 ; gsi < NUM_GOLD_STAGES ; gsi++)
						goldForms.get(gsi)[lfli] = parseLexPhon(forms[gsi+1]);
				if (goldOutput)
					goldResults[lfli] = parseLexPhon(forms[NUM_GOLD_STAGES+1]);
			}
			lfli++;
			if(lfli <NUM_ETYMA)
				assert numCols == colCount(theLine): "ERROR: incorrect number of columns in line "+lfli;
		}
		
		initLexicon = new Lexicon(inputs); 
		testResultLexicon = new Lexicon(inputs); // this one will "evolve" with "time" 
		
		if(NUM_GOLD_STAGES > 0)
			for (int gsi = 0 ; gsi < NUM_GOLD_STAGES; gsi++)
				goldStageGoldLexica[gsi] = new Lexicon(goldForms.get(gsi)); 
		
		if(goldOutput)	
			goldResultLexicon = new Lexicon(goldResults); 
		
		System.out.println("Lexicon extracted.");
		System.out.println("Now running simulation...");

		goldStageInd = 0; blackStageInd=0;
			//index IN THE ARRAYS that the next stage to look for will be at .
		int ri = 0, numRules = CASCADE.size(); //for iteration.
		
		makeRulesLog(CASCADE);
		
		String resp; 
		
		Scanner inp = new Scanner(System.in);
		
		while (ri < numRules)
		{
			if(ri % 100 == 0)	System.out.println("On rule number "+ri);
				
			SChange thisShift =  CASCADE.get(ri);
			
			boolean goldhere = false; 
			if(goldStageInd < NUM_GOLD_STAGES)
			{
				if ( ri  + 1 == goldStageTimeInstants[goldStageInd])
				{
					goldhere = true; 
					testResultLexicon.updateAbsence(goldStageGoldLexica[goldStageInd].getWordList());
					goldStageResultLexica[goldStageInd] = new Lexicon(testResultLexicon.getWordList());
					
					if (stage_pause)
					{						
						System.out.println("Pausing at gold stage "+goldStageInd+", "+goldStageNames[goldStageInd]); 
						System.out.println("Run accuracy analysis here? Enter 'y' or 'n'"); 
						resp = inp.nextLine().substring(0,1); 
						while(!resp.equalsIgnoreCase("y") && !resp.equalsIgnoreCase("n"))
						{
							System.out.println("Invalid response. Do you want to run accuracy analysis here? Please enter 'y' or 'n'.");
							resp = inp.nextLine().substring(0,1); 
						}
						if(resp.equalsIgnoreCase("y"))	
							haltMenu(goldStageInd, inp, theFactory);
					}
					goldStageInd++;
				}
			}
			if(blackStageInd<NUM_BLACK_STAGES && !goldhere)
			{
				if(ri + 1 == blackStageTimeInstants[blackStageInd])
				{
					blackStageResultLexica[blackStageInd] = new Lexicon(testResultLexicon.getWordList());
					blackStageInd++; 
				}
			}
			
			boolean[] wordsChanged = testResultLexicon.applyRuleAndGetChangedWords(thisShift);
			for(int wi = 0 ; wi < NUM_ETYMA ; wi++)
				if (wordsChanged[wi])
					wordTrajectories[wi]+= "\n"+testResultLexicon.getByID(wi)+" | Rule "+ri+" : "+thisShift;
			
			if(print_changes_each_rule)
			{	
				System.out.println("Words changed for rule "+ri+" "+thisShift); 
				for (int wi = 0 ; wi < NUM_ETYMA ;  wi++)
					if (wordsChanged[wi])
					{	
						System.out.println("etym "+wi+" is now : "+testResultLexicon.getByID(wi)+"\t\t[ "+initLexicon.getByID(wi)+" >>> "+goldResultLexicon.getByID(wi)+" ]");
					}
			}
			ri++;
		}
		
		System.out.println("Simulation complete.");
			
		File dir = new File(""+runPrefix); 
		dir.mkdir(); 
		
		System.out.println("making trajectories file in "+dir);
		
		//make trajectories files.
		makeTrajectoryFiles(); 	
		
		//make output graphs file
		System.out.println("making output graph file in "+dir);
		makeOutGraphFile(); 
		
		System.out.println("Simulation complete!");
		
		//TODO fix this here to remove new bug.
		if(goldOutput)
		{
			haltMenu(-1, inp,theFactory);
			
			System.out.println("Writing analysis files...");
			//TODO -- enable analysis on "influence" of black stages and init stage... 
			
			ErrorAnalysis ea = new ErrorAnalysis(testResultLexicon, goldResultLexicon, featsByIndex, 
					feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));
			ea.makeAnalysisFile("testResultAnalysis.txt", false, testResultLexicon);
			ea.makeAnalysisFile("goldAnalysis.txt",true,goldResultLexicon);
			
			if(goldStagesSet)
			{	
				for(int gsi = 0; gsi < NUM_GOLD_STAGES - 1 ; gsi++)
				{	
					ErrorAnalysis eap = new ErrorAnalysis(goldStageResultLexica[gsi], goldStageGoldLexica[gsi], featsByIndex,
							feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));
					eap.makeAnalysisFile(goldStageNames[gsi].replaceAll(" ", "")+"ResultAnalysis.txt",
							false, goldStageResultLexica[gsi]);
				}
			}
		}
		System.out.println("Thank you for using DiaSim"); 
		
		inp.close();
		
	}

	private static String printStageOutsForEtymon( int etymID)
	{
		String toRet = ""; 
		for ( int i = 0 ; i < NUM_GOLD_STAGES ; i++)	
			toRet += ""+goldStageResultLexica[i].getByID(etymID) + STAGE_PRINT_DELIM; 
		return toRet.substring(0, (""+STAGE_PRINT_DELIM).length() );
	}
	
	private static void makeOutGraphFile()
	{
		String toFile = "etymID" + STAGE_PRINT_DELIM + "initial forms" + STAGE_PRINT_DELIM; 
		if(NUM_GOLD_STAGES > 0 )
		{	for (int i = 0 ; i < NUM_GOLD_STAGES ; i++)
				toFile += goldStageNames[i] + " RES" +  
						(goldOutput ? (STAGE_PRINT_DELIM+goldStageNames[i] + " GOLD") : "") +  STAGE_PRINT_DELIM; }
		toFile += "result" + (goldOutput ? STAGE_PRINT_DELIM + " gold" : "") ; 	
		
		for (int i = 0 ; i < NUM_ETYMA ; i++)
		{
			toFile += "\n"+i+STAGE_PRINT_DELIM+initLexicon.getByID(i) ;  
			if(NUM_GOLD_STAGES > 0 )	toFile += printStageOutsForEtymon(i); 
			toFile += ""+STAGE_PRINT_DELIM + testResultLexicon.getByID(i) + (goldOutput ? ""+STAGE_PRINT_DELIM+ goldResultLexicon.getByID(i) : "") ;
		}

		//TODO debugging
		System.out.println("Out graph...\n"+toFile);
		
		
		String filename = runPrefix + "_output_graph"+ OUT_GRAPH_FILE_TYPE; 
		writeToFile(filename, toFile); 
	}
	
	private static void makeRulesLog(List<SChange> theShiftsInOrder) {
		String filename = runPrefix + "_rules_log.txt"; 
		String output = "";
		for (SChange thisShift : theShiftsInOrder)
			output += ""+thisShift + (DEBUG_RULE_PROCESSING ? "| ORIG : "+thisShift.getOrig(): "") + "\n"; 
		writeToFile(filename, output); 
	}

	private static void makeTrajectoryFiles()
	{
		File trajdir = new File(runPrefix,"trajectories"); 
		trajdir.mkdir(); 
	
		for( int wi =0; wi < NUM_ETYMA; wi ++) 
		{
			String filename = new File(runPrefix, new File("trajectories","etym"+wi+".txt").toString()).toString(); 
			String output = "Derivation file for run '"+runPrefix+"'; etymon number :"+wi+":\n"
				+	initLexicon.getByID(wi)+" >>> "+testResultLexicon.getByID(wi)
				+ (goldOutput ? " ( Correct : "+goldResultLexicon.getByID(wi)+") :\n"  : ":\n")
					+wordTrajectories[wi]+"\n";
			writeToFile(filename, output); 
		}
	}
	
	// missLocations are the indices of words that ultimately resulted in a miss between the testResult and the gold
	// outputs the scores for each phone in the word in the lexicon
	public static HashMap<Phone,Double> missLikelihoodPerPhone (Lexicon lexic)
	{
		LexPhon[] lexList = lexic.getWordList(); //indices should correspond to those in missLocations
		int lexSize = lexList.length; 
		assert NUM_ETYMA == finMissInds.length: "Error : mismatch between size of locMissed array and word list in lexicon"; 
		
		Phone[] phonemicInventory = lexic.getPhonemicInventory(); 
		int inventorySize = phonemicInventory.length; 
		
		HashMap<String,Integer> phonemeIndices = new HashMap<String,Integer>();
			//maps print symbols of phoneme onto its index in phonemicInventory array
		for(int phii = 0 ; phii < phonemicInventory.length; phii++)
			phonemeIndices.put( phonemicInventory[phii].print(), phii); 
			
		int[] phoneFreqForMisses = new int[inventorySize]; 
			//indices correspond to those in phonemicInventory 
		
		for(int li = 0 ; li < lexSize; li++)
		{
			if(finMissInds[li])
			{
				String phonesSeenInWord = ""; 
				List<SequentialPhonic> phs = lexList[li].getPhonologicalRepresentation(); 
				
				for(SequentialPhonic ph : phs)
				{
					if(ph.getType().equals("phone"))
					{
						if(!phonesSeenInWord.contains(ph.print()))
						{
							phonesSeenInWord += ph.print() + ","; 
							phoneFreqForMisses[phonemeIndices.get(ph.print())] += 1; 
						}
					}
				}
			}
		}
		
		HashMap<String,Integer> phoneFreqsByWordInLex = lexic.getPhoneFrequenciesByWord(); 
		//note that the keys for this are the feature vects, not the toString() or print() statements
		
		HashMap<Phone,Double> output = new HashMap<Phone,Double>(); 
		for (int pi = 0; pi < inventorySize ; pi++)
			output.put(phonemicInventory[pi], 
				(double)phoneFreqForMisses[pi] /
				(double)phoneFreqsByWordInLex.get(phonemicInventory[pi].getFeatString()));
		
		return output; 
	}
	
	/**
	 * given String @param toLex
	 * @return its representation as a LexPhon containing a sequence of Phone instances
	 * TODO note we assume the phones are separated by PH_DELIM (presumably ' ') 
	 */
	private static LexPhon parseLexPhon(String toLex)
	{
		if (toLex.contains(ABSENT_PH_INDIC))
		{	return new AbsentLexPhon();	}
		
		String[] toPhones = toLex.trim().split(""+PH_DELIM);
		
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
		int i = proxy.indexOf(""+LEX_DELIM), c = 1 ;
		while( i > -1)
		{
			c++; 
			proxy = proxy.substring(i+1);
			i = proxy.indexOf(","); 
		}
		return c; 
	}
	
	private static int numFalse (boolean[] boolarray)
	{
		//TODO debugging
		System.out.println("bool array len : " + boolarray);
		
		int count = 0; 
		for (int i = 0 ; i < boolarray.length; i++)
			count += boolarray[i] ? 0 : 1 ;
		return count; 
	}
	
	private static void writeToFile(String filename, String output)
	{	try 
		{	FileWriter outFile = new FileWriter(filename); 
			BufferedWriter out = new BufferedWriter(outFile); 
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
	
	
	// @param (cutoff) -- rule number that the black stage must be BEFORE.
	private static void printTheseBlackStages(int first, int last, boolean prepend)
	{
		if(blackStagesSet)
			for(int bsi = first; bsi < last + 1; bsi++)
				System.out.println(bsi+": "+(prepend ? "b":"")+
					blackStageNames[bsi]+" (@rule #: "+blackStageTimeInstants[bsi]+")");
	}
	
	private static void printTheseGoldStages(int firstToPrint, int lastToPrint)
	{
		if(goldStagesSet)
			for(int gsi = firstToPrint; gsi < lastToPrint + 1; gsi++)
				System.out.println(gsi+": "+
					goldStageNames[gsi]+" gold forms (@rule #: "+goldStageTimeInstants[gsi]+")");
	}
	
	private static List<String> validBlackStageOptions(int first, int last, boolean prepend)
	{
		List<String> out = new ArrayList<String>();
		if (blackStagesSet)
			for (int oi = first; oi < last+1; oi++)	out.add((prepend ? "b":"")+oi);
		return out;
	}
	
	private static List<String> validGoldStageOptions(int first, int last, boolean prepend)
	{
		List<String> out = new ArrayList<String>();
		if (goldStagesSet)
			for (int oi = first; oi < last+1; oi++)	out.add((prepend ? "g":"")+oi);
		return out;
	}
	
	//TODO fix this ! 
	// @param curr_stage : -1 if at final result point, otherwise valid index of stage in goldStage(Gold/Result)Lexica
	private static void haltMenu(int curSt, Scanner inpu, SChangeFactory fac)
	{	
		Lexicon r = testResultLexicon;
		Lexicon g = (curSt == -1) ? goldResultLexicon : goldStageGoldLexica[curSt]; 
		
		ErrorAnalysis ea = new ErrorAnalysis(r, g, featsByIndex, 
				feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));

		System.out.println("Overall accuracy : "+ea.getPercentAccuracy());
		System.out.println("Accuracy within 1 phone: "+ea.getPct1off());
		System.out.println("Accuracy within 2 phone: "+ea.getPct2off());
		System.out.println("Average edit distance per from gold phone: "+ea.getAvgPED());
		System.out.println("Average feature edit distance from gold: "+ea.getAvgFED());
		
		int lastGoldOpt = (curSt == -1 ? NUM_GOLD_STAGES : curSt) - 1;
		int lastBlkOpt = NUM_BLACK_STAGES - 1;
		while((lastBlkOpt < 0 || curSt < 0) ? false : blackStageTimeInstants[lastBlkOpt] > goldStageTimeInstants[curSt])
			lastBlkOpt--;
		
		boolean cont = true; 
		int evalStage = curSt; 
		SequentialFilter filterSeq = new SequentialFilter(new ArrayList<RestrictPhone>(), new String[] {}); 
		Lexicon focPtLex = null;
		String focPtName = ""; 
		int focPtLoc = -1; 
		boolean focPtSet = false, filterIsSet = false;
		
		while(cont)
		{
			System.out.print("What would you like to do? Please enter the appropriate number below:\n"
					+ "| 0 : Set evaluation point ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|\n"
					+ "| 1 : Set focus point                                                                 |\n"
					+ "| 2 : Set filter sequence                                                             |\n"
					+ "| 3 : Query                                                                           |\n"
					+ "| 4 : Standard prognosis at evaluation point                                          |\n"
					+ "| 5 : Run autopsy for (at evaluation point) (for subset lexicon if specified)         |\n"
					+ "| 6 : Review results (stats, word forms) at evaluation point (goes to submenu)        |\n"
					+ "| 7 : Test full effects of a proposed change to the cascade                           |\n"
					+ "| 9 : End this analysis.______________________________________________________________|\n");
			String resp = inpu.nextLine().substring(0,1);
			
			if (resp.equals("0")) //set evaluation point
			{
				if (!goldStagesSet)	System.out.println("Cannot change evaluation stage: no intermediate gold stages are set."); 
				else
				{
					System.out.println("Changing point of evaluation (comparing result against gold)");
					System.out.print("Current evaluation point: ");
					if (evalStage == curSt)
					{
						if (evalStage == -1)	System.out.print("final result\n");
						else	System.out.print("current forms at stage "+evalStage+": "+goldStageNames[evalStage]+"\n");
					}
					else	System.out.println("intermediate stage "+evalStage+": "+goldStageNames[evalStage]); 
				
					List<String> validOptions = validGoldStageOptions(0,lastGoldOpt,false); 
					validOptions.add("F"); 
					boolean chosen = false;
					
					while (!chosen)
					{
						System.out.println("Available options for evaluation stage: ");
						printTheseGoldStages(0, lastGoldOpt); 
						System.out.println("F : "+ (curSt == -1 ? "final forms" : "current forms at stage "+curSt));
						System.out.println("Please enter the indicator for the stage you desire"); 
						resp = inpu.nextLine().substring(0,1);
						chosen = validOptions.contains(resp);
						if(!chosen)	System.out.println("Invalid response. Please choose a valid indicator for the new evaluation stage."); 
					}
					
					evalStage = resp.equals("F") ? curSt : Integer.parseInt(resp); 
					r = resp.equals("F") ? testResultLexicon : goldStageResultLexica[evalStage];
					g = (curSt == -1 && resp.equals("F") ) ? goldResultLexicon : goldStageGoldLexica[evalStage];
					boolean filtered = ea.isFiltSet();
					boolean focused = ea.isFocSet(); 
					
					ea = new ErrorAnalysis(r, g, featsByIndex, 
							feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));
					if (focused) 	ea.setFocus(focPtLex, focPtName);
					if (filtered) 	ea.setFilter(filterSeq, focPtName);
				}
			}
			else if (resp.equals("1"))
			{
				System.out.println("Setting focus point -- extra stage printed for word list, and point at which we filter to make subsets."); 
				System.out.println("Current focus point lexicon: "+(focPtSet ? focPtName : "undefined"));
				System.out.println("Current filter : "+(filterIsSet ? filterSeq.toString() : "undefined")); 
				
				boolean chosen = false; 
				while(!chosen)
				{
					System.out.println("Available options for focus point:");
					printTheseGoldStages(0, lastGoldOpt); printTheseBlackStages(0, lastBlkOpt, true); 
					System.out.print("In: delete & filter by input\nOut: delete & filter at current output\nGold: delete & filter by current gold"
							+ "\nU: delete and also delete filter\nR#: right before rule with index number <#> (you can find rule indices with option 3 to query on the main menu)\n"); 
					List<String> validOptions = validGoldStageOptions(0,lastGoldOpt,true);
					validOptions.addAll(validBlackStageOptions(0,lastBlkOpt,true));
					validOptions.add("In"); validOptions.add("Out"); validOptions.add("U"); validOptions.add("Gold");
					
					for(int ri = 1; ri < CASCADE.size(); ri++)	
						validOptions.add("R"+ri);
					resp = inpu.nextLine();
					resp.replace("\n", ""); 
					chosen = validOptions.contains(resp); 
					if(!chosen)
					{
						if(resp.equals("R0"))	System.out.println("'R0' is not a valid option -- instead choose 'In' "
								+ "to delete focus point and use the input for filtering");
						else if (resp.charAt(0) == 'R')
							System.out.println("'"+resp+"' is not a valid option: the last rule is number "+(CASCADE.size()-1));
						else	System.out.println("Invalid input : '"+resp+"'\nPlease select a valid option listed below:");
					}
					else
					{
						focPtSet = true;
						if(resp.charAt(0) == 'g')
						{
							int si = Integer.parseInt(resp.substring(1));
							focPtLex = goldStageGoldLexica[si]; 
							focPtLoc = goldStageTimeInstants[si];
							focPtName = goldStageNames[si]+" [r"+focPtLoc+"]";
							ea.setFocus(focPtLex, focPtName); 
						}
						else if (resp.charAt(0) == 'b')
						{
							int si = Integer.parseInt(resp.substring(1));
							focPtLex = blackStageResultLexica[si]; 
							focPtLoc = blackStageTimeInstants[si];
							focPtName = blackStageNames[si]+" [r"+focPtLoc+"]";
							ea.setFocus(focPtLex, focPtName); 
						}
						else if (resp.charAt(0) == 'R')
						{
							focPtLoc = Integer.parseInt(resp.substring(1)); 
							focPtLex = toyDerivationResults(initLexicon.getWordList(),CASCADE.subList(0, focPtLoc));
							focPtName = "pivot@R"+focPtLoc; 
							ea.setFocus(focPtLex, focPtName); 
						}
						else
						{
							focPtLoc = -1; focPtLex = null; focPtName = ""+resp;
							if(resp.equals("U"))
							{	filterSeq = new SequentialFilter(new ArrayList<RestrictPhone>(), new String[] {});
								filterIsSet = false; 
								focPtName = "";
								focPtSet = false; 
							}
							else	focPtLex = resp.equals("In") ? initLexicon : 
								resp.equals("Out") ? testResultLexicon :
									(curSt == -1) ? goldResultLexicon : goldStageGoldLexica[curSt];
							ea = new ErrorAnalysis(r, g, featsByIndex, 
									feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));
							ea.setFocus(focPtLex,focPtName);
						}
					}
							
				}
				
			}
			else if (resp.equals("2") && !focPtSet)
				System.out.println("Error: cannot set a filter sequence without first setting a focus point.\nUse option '1' on the menu.");
			else if (resp.equals("2"))
			{
				boolean fail = true; 
				
				System.out.println("Setting filter sequence to define lexicon subsample.");
				System.out.println("[Filtering from "+focPtName+"]"); 
				
				while(fail)
				{	
					System.out.println("Enter the phoneme sequence filter, delimiting phones with '"+PH_DELIM+"'");
					
					resp = inpu.nextLine().replace("\n",""); 
					
					try {
						filterSeq = fac.parseNewSeqFilter(resp, true);
						fail = false;
					}
					catch (Exception e)
					{
						System.out.println("That is not a valid filter.\nTry again and double check spelling of any feature names, and that the proper delimitation is used...");
					}
					
					if(!fail)
					{
						System.out.println("Success: now making subsample with filter "+filterSeq.toString());
						System.out.println("(Pivot moment name: "+focPtName+")");
						
						//TODO debugging
						System.out.println("Filter seq : "+filterSeq);
						
						ea.setFilter(filterSeq,focPtName);
					}
				}
			}
			else if(resp.equals("3"))
			{
				boolean prompt = true; 
				while(prompt)
				{	System.out.print("What is your query? Enter the corresponding indicator:\n"
							+ "0 : get ID of an etymon by input form\n"
							+ "1 : get etymon's input form by ID number\n"
							+ "2 : print all etyma by ID\n"
							+ "3 : get derivation up to this point for etymon by its ID\n"
							+ "4 : get rule by time step\n"
							+ "5 : get time step(s) of any rule whose string form contains the submitted string\n"
							+ "6 : print all rules by time step.\n"
							+ "9 : return to main menu.\n"); 
					resp = inpu.nextLine().replace("\n",""); 
					prompt = false;
					if( !"01234569".contains(resp) || resp.length() > 1 ) {
						System.out.println("Error : '"+resp+"' is not in the list of valid indicators. Please try again.");
						prompt = true;
					}
					else if (resp.equals("9"))	prompt = false;
					else if (resp.equals("0")) {
						System.out.println("Enter the input form, separating phones by "+PH_DELIM);
						resp = inpu.nextLine().replace("\n",""); 
						LexPhon query = null;
						try {
							query = new LexPhon(fac.parseSeqPhSeg(resp));
						}
						catch (Exception e){
							System.out.println("Error: could not parse entered phone string. Returning to query menu.");
							prompt = true;
						}
						if(!prompt)
						{
							LexPhon[] wl = initLexicon.getWordList();
							String inds = etymInds(wl, query);
							System.out.println("Ind(s) with this word as input : "+inds);  
						}
					}
					else if(resp.equals("1")||resp.equals("3") || resp.equals("4"))
					{
						System.out.println("Enter the ID to query:");
						String idstr = inpu.nextLine(); 
						boolean queryingRule = resp.equals("4"); //otherwise we're querying an etymon.
						int theID = getValidInd(idstr, queryingRule ? CASCADE.size() : NUM_ETYMA - 1) ; 
						if (theID == -1){
							prompt =true;
						}
						else if(queryingRule)
						{
							prompt = theID < 0 || theID >= CASCADE.size();
							if(prompt)	System.out.println("Error -- there are only "+CASCADE.size()+"rules. Returning to query menu."); 
							else	printRuleAt(theID); 
						}
						else
						{
							if(prompt)	System.out.println("Error -- there are only "+NUM_ETYMA+" etyma. Returning to query menu."); 
							else if(resp.equals("1"))	System.out.println(initLexicon.getByID(theID)); 
							else 	System.out.println(""+wordTrajectories[theID]);
						}
					}
					else if(resp.equals("2"))
					{
						System.out.println("etymID"+STAGE_PRINT_DELIM+"Input"+STAGE_PRINT_DELIM+"Gold");
						for (int i = 0 ; i < r.getWordList().length ; i++)
							System.out.println(""+i+STAGE_PRINT_DELIM+initLexicon.getByID(i)+STAGE_PRINT_DELIM+goldResultLexicon.getByID(i));
					}
					else if(resp.equals("5"))
					{
						System.out.println("Enter the string you want to query with.\n");
						resp = inpu.nextLine().replace("\n", ""); 
						String out = "";
						boolean noMatches = true; 
						for(int ci = 0; ci < CASCADE.size(); ci++)
						{
							if (CASCADE.get(ci).toString().contains(resp))
								System.out.println(""+ci+" : "+CASCADE.get(ci).toString()); 
						}
						if(noMatches)	System.out.println("No matches found."); 
					}
					else //"6"
					{
						for(int ci = 0 ; ci < CASCADE.size(); ci++) 
							System.out.println(""+ci+": "+CASCADE.get(ci)); 
					}
				}
			}
			else if(resp.equals("4"))	ea.confusionPrognosis(true);
			else if(resp.equals("5"))
			{
				if(!ea.isFiltSet())
					System.out.println("Error: tried to do context autopsy without beforehand setting filter stipulations: You can do this with 2.");
				else if (!ea.isFocSet()) System.out.println("Error: can't do context autopsy without first setting focus point. Use option 1.");
				else	ea.contextAutopsy();				
			}
			else if(resp.equals("6"))
			{
				boolean subcont = true; 
				
				while(subcont) {
				
					System.out.print("What results would you like to check? Please enter the appropriate number:\n"
						+ "| 0 : Print stats (at evaluation point) (for subset lexicon if specified)~~~~~~~~~~~~~|\n"
						+ "| 1 : Print all corresponding forms (init(,focus),res,gold) (for subset if specified) |\n"
						+ "| 2 : Print all mismatched forms at evaluation point (for subset if specified)        |\n"
						+ "| 9 : Exit this menu._________________________________________________________________|\n");  
					
					resp = inpu.nextLine().substring(0,1);
					
					if(resp.equals("0"))
					{
						System.out.println("Printing stats:"+ (ea.isFiltSet() ? " for filter "+filterSeq.toString()+ " at "+focPtName : "" ));
						
						System.out.println("Overall accuracy : "+ea.getPercentAccuracy());
						System.out.println("Accuracy within 1 phone: "+ea.getPct1off());
						System.out.println("Accuracy within 2 phone: "+ea.getPct2off());
						System.out.println("Average edit distance per from gold phone: "+ea.getAvgPED());
						System.out.println("Average feature edit distance from gold: "+ea.getAvgFED());
					}
					else if(resp.equals("1"))
					{
						System.out.println("Printing all etyma: Input," + (ea.isFocSet() ? focPtName+"," : "")+"Result, Gold"); 
						ea.printFourColGraph(initLexicon);	
					}
					else if(resp.equals("2"))
					{
						System.out.println("Printing all mismatched etyma" + (ea.isFiltSet() ? " for filter "+filterSeq.toString()+" at "+focPtName : "" ));
						System.out.println("Res : Gold");
						List<LexPhon[]> mms = ea.getCurrMismatches(new ArrayList<SequentialPhonic>(), true);
						for (LexPhon[] mm : mms)
							System.out.println(mm[0].print()+" : "+mm[1].print());
					}
					else if (resp.equals("9"))
					{
						System.out.println("Going back to main menu"); 
						subcont = false;
					}
					else	System.out.println("Invalid response. Please enter one of the listed numbers"); 
				}
			}
			else if(resp.equals("7")) //forking test
			{
				String errorMessage = "Invalid response. Please enter a valid response. Returning to forking test menu."
				
				
				boolean subcont = true; 
				while(subcont) {
					System.out.print("At what rule number would you like to modify cascade? Please type the number.\n"
							+ "You may also enter:\t'quit', to return to the main menu\n"
							+ "\t\t\t'get rule X', to get the index of any rules containing an entered string replacing <X>.\n"
							+ "\t\t\t'get rule at X', to get the rule at the index number <X>.\n" 
							+ "\t\t\t'get rule effect X', to get all changes from a rule by the index <X>.\n"
							+ "\t\t\t'get cascade', to print all rules with their original indices.\n"
							+ "\t\t\t'get etym X', to print the index of the INPUT form etyma entered <X>.\n"
							+ "\t\t\t:'get etym at X', to get the etymon at index <X>.\n"
							+ "\t\t\t:'get etym derivation X', to get the full derivation of etymon with index <X>.\n"
							+ "\t\t\t:'get lexicon', print entire lexicon with etyma mapped to inds.\n"); 
					resp = inpu.nextLine().replace("\n",""); 
					if (resp.equals("quit"))	subcont = false;
					else if(isValidInd(resp, CASCADE.size()))
					{
						//TODO here. 
					}
					else if(!resp.contains("get ") || resp.length() < 10)	System.out.println(errorMessage); 
					else if(resp.equals("get cascade"))
					{
						for(int ci = 0 ; ci < CASCADE.size(); ci++) 
							System.out.println(""+ci+": "+CASCADE.get(ci)); 
					}
					else if(resp.equals("get lexicon"))
					{
						System.out.println("etymID"+STAGE_PRINT_DELIM+"Input"+STAGE_PRINT_DELIM+"Gold");
						for (int i = 0 ; i < r.getWordList().length ; i++)
							System.out.println(""+i+STAGE_PRINT_DELIM+initLexicon.getByID(i)+STAGE_PRINT_DELIM+goldResultLexicon.getByID(i));
					}
					else if(resp.substring(0,9).equals("get rule "))
					{
						String entry = resp.substring(9); 
						if(entry.length() >= 4) {
							if(entry.substring(0,3).equals("at ")) {
								int theInd = getValidInd(entry.substring(4), CASCADE.size() );
								if (theInd > -1) printRuleAt(theInd); 
							}
						}
					}
					else if(resp.substring(0,9).equals("get etym "))
					{
						
					}
					else	System.out.println(errorMessage); 
							

					
				}
			}
			else if(resp.equals("9")) {
				System.out.println("Ending"); cont = false; 
			}
			else	System.out.println("Invalid response. Please enter one of the listed numbers"); 
		}
	}
	
	//makes EA object on subset of gold/res pairs that have a specified sequence in either the gold or res as flagged by boolean second param
	public static ErrorAnalysis analyze_subset_with_seq (Lexicon ogRes, Lexicon ogGold, List<SequentialPhonic> targSeq, boolean look_in_gold)
	{
		if (targSeq.size() == 0)	throw new Error("Can't make subset based on empty sequence"); 
		List<Integer> indsInSubset = new ArrayList<Integer>(); 
		Lexicon toCheck = look_in_gold ? ogGold : ogRes;
		
		for (int i = 0 ; i < NUM_ETYMA; i++)
			if (Collections.indexOfSubList( toCheck.getByID(i).getPhonologicalRepresentation(), targSeq) != -1)
				indsInSubset.add(i);
		
		int subset_size = indsInSubset.size();
		
		LexPhon[] subRes = new LexPhon[subset_size], subGold = new LexPhon[subset_size]; 
		
		for (int j = 0 ; j < subset_size; j++)
		{
			int k = indsInSubset.remove(0); 
			subRes[j] = ogRes.getByID(k); 
			subGold[j] = ogGold.getByID(k); 	
		}
		 
		return new ErrorAnalysis(new Lexicon(subRes), new Lexicon(subGold), featsByIndex, 
				feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt)); 
		
	}
	
	// for the lexicon of any given stage, passed as parameter, 
	// outputs hashmap where the value for each key Phone instance
	// is the average Levenshtein distance for words containing that phone 
	// normalized for length of the word
	// counted for the number of times the phone actually occurs in that word out of total phones in the word, each time. 
	public static HashMap<Phone,Double> avgLDForWordsWithPhone (Lexicon lexic)
	{
		LexPhon[] lexList = lexic.getWordList(); //indices should correspond to those in missLocations
		int lexSize = lexList.length; 

		Phone[] phonemicInventory = lexic.getPhonemicInventory(); 
		int inventorySize = phonemicInventory.length; 
		HashMap<String,Integer> phonemeIndices = new HashMap<String,Integer>();
			//maps print symbols of phoneme onto its index in phonemicInventory array
		for(int phii = 0 ; phii < phonemicInventory.length; phii++)
			phonemeIndices.put( phonemicInventory[phii].print(), phii); 
		
		int[] totalLevenshtein = new int[inventorySize]; //total levenshtein edit distance 
			// of words with each phone
		
		for(int li = 0; li < lexSize; li++)
		{
			List<SequentialPhonic> phs = lexList[li].getPhonologicalRepresentation();
			for (SequentialPhonic ph : phs)
			{
				if(ph.getType().equals("phone"))
				{					
					totalLevenshtein[phonemeIndices.get(ph.print())] += 
							ErrorAnalysis.levenshteinDistance(testResultLexicon.getByID(li),
									goldResultLexicon.getByID(li)) / (double)goldResultLexicon.getByID(li).getNumPhones() ;
				}
			}
		}
		
		HashMap<String,Integer> phoneFreqsByWordInLex = lexic.getPhoneFrequenciesByWord(); 

		HashMap<Phone,Double> output = new HashMap<Phone,Double>(); 
		for(int phi = 0; phi < inventorySize; phi++)
		{
			output.put(phonemicInventory[phi], 
					(double)totalLevenshtein[phi] / 
					(double)phoneFreqsByWordInLex.get(phonemicInventory[phi].getFeatString()));
		}
		return output;
	}
	
	public static HashMap<Phone,Double> avgFEDForWordsWithPhone (Lexicon lexic)
	{
		LexPhon[] lexList = lexic.getWordList(); //indices should correspond to those in missLocations
		int lexSize = lexList.length; 

		Phone[] phonemicInventory = lexic.getPhonemicInventory(); 
		int inventorySize = phonemicInventory.length; 
		HashMap<String,Integer> phonemeIndices = new HashMap<String,Integer>();
			//maps print symbols of phoneme onto its index in phonemicInventory array
		for(int phii = 0 ; phii < phonemicInventory.length; phii++)
			phonemeIndices.put( phonemicInventory[phii].print(), phii); 
		
		int[] totalFED = new int[inventorySize]; //total feature edit distance 
			// of words with this phone
		
		FED distMeasure = feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt); 
		
		for(int li = 0 ; li < lexSize ; li++)
		{
			List<SequentialPhonic> phs = lexList[li].getPhonologicalRepresentation();
			for (SequentialPhonic ph : phs)
			{
				if(ph.getType().equals("phone"))
				{
					distMeasure.compute(testResultLexicon.getByID(li),
							goldResultLexicon.getByID(li));
					totalFED[phonemeIndices.get(ph.print())] += distMeasure.getFED();
				}
			}
		}
		
		HashMap<String,Integer> phoneFreqsByWordInLex = lexic.getPhoneFrequenciesByWord(); 

		HashMap<Phone,Double> output = new HashMap<Phone,Double>(); 
		for(int phi = 0; phi < inventorySize; phi++)
		{
			output.put(phonemicInventory[phi], 
					(double)totalFED[phi] / 
					(double)phoneFreqsByWordInLex.get(phonemicInventory[phi].getFeatString()));
		}
		return output;
		
	}
	
	// required : runPrefix must be specified 
	// flags: -r : debug rule processing
	//		  -d : debugging mode -- TODO implement
	//		  -p : print words every time they are changed by a rule
	//		  -e : (explicit) do not use feature implications -- TODO implement
	//		  -h : halt at stage checkpoints
	//		  -i : ignore stages
	private static void parseArgs(String[] args)
	{
		int i = 0, j; 
		String arg;
		char flag; 
		boolean vflag = false;
		
		boolean no_prefix = true; 
		
		//defaults
		symbDefsLoc = "symbolDefs.csv";
		lexFileLoc = "FLLex.txt";
		ruleFileLoc = "DiaCLEF"; 
		featImplsLoc = "FeatImplications"; 
		id_wt = 0.5; 
		
		DEBUG_RULE_PROCESSING = false; 
		DEBUG_MODE = false; 
		print_changes_each_rule = false;
		
		while (i < args.length && args[i].startsWith("-"))	
		{
			arg = args[i++];
			
			if (arg.equals("-verbose"))	vflag = true; 
			
			//variable setters
			
			// output prefix -- required ultimately
			else if (arg.contains("-out"))
			{
				if (i < args.length)
					runPrefix = args[i++]; 
				else	System.err.println("Output prefix specification requires a string");
				if (vflag)	System.out.println("output prefix: "+runPrefix);
				no_prefix = false; 
			}
			
			// symbol definitions file location
			else if (arg.equals("-symbols"))
			{
				if (i < args.length)	symbDefsLoc = args[i++];
				else	System.err.println("-symbols requires a location");
				if (vflag)	System.out.println("symbol definitions location: "+symbDefsLoc);
			}
			
			//feature implications file location
			else if (arg.contains("-impl"))
			{
				if (i < args.length)	featImplsLoc = args[i++]; 
				else	System.err.println("-impl requires a location for feature implications location.");
				if (vflag)	System.out.println("feature implications location: "+featImplsLoc);
			}
			
			//ruleset file location
			else if (arg.contains("-rules"))
			{
				if (i < args.length)	ruleFileLoc = args[i++];
				else	System.err.println("-rules requires a location for ruleset file.");
				if (vflag)	System.out.println("ruleset file location: "+ruleFileLoc);
			}
			
			//lexicon location
			else if (arg.contains("-lex"))
			{
				if (i < args.length)	lexFileLoc = args[i++];
				else	System.err.println("-lex requires a location for lexicon file location.");
				if (vflag)	System.out.println("lexicon file location: "+lexFileLoc);
			}
			
			//insertion/deletion cost
			else if (arg.equals("-idcost"))
			{
				if (i < args.length)	id_wt = Double.parseDouble(args[i++]);
				else	System.err.println("-idcost requires a double for ratio of insertion/deletion cost to substitution");
				if (vflag)	System.out.println("insertion/deletion cost ratio to substitution: "+id_wt); 
			}
			
			//flag args
			else
			{
				for (j = 1; j < arg.length(); j++)
				{	flag = arg.charAt(j);
					switch(flag)	{
						case 'r':
							DEBUG_RULE_PROCESSING = true;
							if (vflag)	System.out.println("Debugging rule processing.");
							break; 
						case 'd':
							DEBUG_MODE = true;
							if (vflag)	System.out.println("Debugging mode on.");
							break; 
						case 'p':
							print_changes_each_rule = true;
							if (vflag)	System.out.println("Printing words changed for each rule.");
							break;
						case 'h':
							stage_pause = true; 
							if (vflag)	System.out.println("Halting for analysis at stage checkpoints.");
							break; 
						case 'i':
							ignore_stages = true; 
							if (vflag)	System.out.println("Ignoring all stages.");
							break;
						default:
							System.err.println("Illegal flag : "+flag);
							break;
					}	
				}
			}
		}
		if (i != args.length || no_prefix)
            throw new Error("Usage: DerivationSimulation [-verbose] [-rdphi] [-idcost cost] [-rules afile] [-lex afile] [-symbols afile] [-impl afile] -out prefix"); 	
	}
	
	private static String append_space_to_x (String in, int x)
	{
		if (in.length() >= x)	return in;
		String out = in + " ";
		while (out.length() < x)	out += " ";
		return out;
	}
	
	public static Lexicon toyDerivationResults(LexPhon[] inps, List<SChange> ruleCascade )
	{
		Lexicon out = new Lexicon(inps);
		for (SChange rule : ruleCascade)
		{	boolean[] etymsAff = out.applyRuleAndGetChangedWords(rule);	} 
		return out; 
	}
	
	private static String etymInds(LexPhon[] etList, LexPhon etTarg)
	{
		String output = ""; 
		for (int wli = 0; wli < etList.length; wli++)
			if(etList[wli].toString().equals(etTarg.toString()))
				output += output.equals("") ? ""+wli : ", "+wli;
		return output;
	}
	
	
	//to use for checking if an entered etymon or rule id is valid. 
	// max should be the number of words in the lexicon minus 1 (for an etymon)
		// or the length of the cascade (for a rule)
	private static int getValidInd(String s, int max)
	{
		int output; 
		try 		{	output = Integer.parseInt(s);	} 
		catch (NumberFormatException | NullPointerException nfe) {
	        return -1;	}
		return output <= max ? output : -1; 
	}
	
	private static void printRuleAt(int theInd)
	{
		if (theInd == CASCADE.size())
			System.out.println("Ind "+theInd+" is right after the realization of the last rule.");
		else System.out.println(CASCADE.get(theInd)); 
	}
}


