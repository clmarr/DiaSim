import java.io.BufferedReader;
import java.io.File; 
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap; 
import java.util.Scanner; 
import java.util.List;
import java.util.ArrayList;

/**
 * main class for diachronic derivation system
 * @author Clayton Marr
 *
 */
public class DiachronicSimulator {
	
	private static List<String> rulesByTimeInstant;
	
	private static String[] featsByIndex; 
	private static HashMap<String, Integer> featIndices;
	private static boolean feats_weighted;
	private static double[] FT_WTS; 
	
	private static HashMap<String, String> phoneSymbToFeatsMap;
	private static HashMap<String, String[]> featImplications; 
	private static LexPhon[] inputForms; 
	private static Lexicon goldOutputLexicon;
	private static int NUM_ETYMA; 
	private static int NUM_GOLD_STAGES, NUM_BLACK_STAGES;
	private static String[] goldStageNames, blackStageNames; 
	private static Lexicon[] goldStageGoldLexica; //indexes match with those of customStageNames 
		//so that each stage has a unique index where its lexicon and its name are stored at 
			// in their respective lists.
	private static int[] goldStageInstants, blackStageInstants; // i.e. the index of custom stages in the ordered rule set
	private static boolean goldStagesSet, blackStagesSet; 
	
	private static boolean goldOutput; 
	
	//to be set in command line...
	private static String runPrefix;
	private static String symbDefsLoc; 
	private static String featImplsLoc; 
	private static String cascFileLoc; 	
	private static String lexFileLoc;
	
	private static double id_wt; 
	private static boolean DEBUG_RULE_PROCESSING, DEBUG_MODE, print_changes_each_rule, stage_pause, ignore_stages, 
		no_feat_impls; 
	
	private static int goldStageInd, blackStageInd; 
	
	private static List<SChange> CASCADE;
	private static Simulation theSimulation; 
	
	private static String[] stageOrdering; 
	private static String[] initStrForms; 
	
	private static void extractSymbDefs()
	{
		System.out.println("Collecting symbol definitions...");
		
		featIndices = new HashMap<String, Integer>() ; 
		phoneSymbToFeatsMap = new HashMap<String, String>(); 
		
		List<String> symbDefsLines = UTILS.readFileLines(symbDefsLoc);
		System.out.println("Symbol definitions extracted!");
		System.out.println("Length of symbDefsLines : "+symbDefsLines.size()); 
		

		//from the first line, extract the feature list and then the features for each symbol.
		featsByIndex = symbDefsLines.get(0).replace("SYMB,", "").split(""+UTILS.FEAT_DELIM); 
		
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
		int li = 1; String nextLine;
		while (li < symbDefsLines.size()) 
		{
			nextLine = symbDefsLines.get(li).replaceAll("\\s+", ""); //strip white space and invisible characters 
			int ind1stComma = nextLine.indexOf(UTILS.FEAT_DELIM); 
			String symb = nextLine.substring(0, ind1stComma); 
			String[] featVals = nextLine.substring(ind1stComma+1).split(""+UTILS.FEAT_DELIM); 		
			
			String intFeatVals = ""; 
			for(int fvi = 0; fvi < featVals.length; fvi++)
			{
				if(featVals[fvi].equals(""+UTILS.MARK_POS))	intFeatVals+= UTILS.POS_INT; 
				else if (featVals[fvi].equals(""+UTILS.MARK_UNSPEC))	intFeatVals += UTILS.UNSPEC_INT; 
				else if (featVals[fvi].equals(""+UTILS.MARK_NEG))	intFeatVals += UTILS.NEG_INT; 
				else	throw new Error("Error: unrecognized feature value, "+featVals[fvi]+" in line "+li);
			}
			
			phoneSymbToFeatsMap.put(symb, intFeatVals);
			li++; 
		}
	}
	
	public static void extractFeatImpls()
	{
		featImplications = new HashMap<String, String[]>(); 
		
		if (no_feat_impls)	return; 
		
		System.out.println("Now extracting info from feature implications file...");
		
		List<String> featImplLines = UTILS.readFileLines(featImplsLoc);
				
		for(String filine : featImplLines)
		{
			String[] fisides = filine.split(""+UTILS.IMPLICATION_DELIM); 
			featImplications.put(fisides[0], fisides[1].split(""+UTILS.FEAT_DELIM));
		}
		
		System.out.println("Done extracting feature implications!");	
	}
	
	public static void extractCascade(SChangeFactory theFactory)
	{
		System.out.println("Now extracting diachronic sound change rules from rules file...");
		
		rulesByTimeInstant = new ArrayList<String>(); 

		String nextRuleLine;
		
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader ( 
				new FileInputStream(cascFileLoc), "UTF-8")); 
			
			while((nextRuleLine = in.readLine()) != null)
			{
				String lineWithoutComments = ""+nextRuleLine; 
				if (lineWithoutComments.contains(""+UTILS.CMT_FLAG))
						lineWithoutComments = lineWithoutComments.substring(0,
								lineWithoutComments.indexOf(""+UTILS.CMT_FLAG));
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
		List<String> blackStageNameAndLocList = new ArrayList<String>();
			// same as above, but will not be compared to gold. 
		
		goldStagesSet = false; blackStagesSet=false;  
				
		int rli = 0; 
		
		while (rli < rulesByTimeInstant.size())
		{
			String currRule = rulesByTimeInstant.get(rli); 
			
			if ( (""+UTILS.GOLD_STAGENAME_FLAG+UTILS.BLACK_STAGENAME_FLAG).contains(""+currRule.charAt(0)))
			{
				if (ignore_stages)	rulesByTimeInstant.remove(rli); 
				else if ( currRule.charAt(0) == UTILS.GOLD_STAGENAME_FLAG)
				{
					goldStagesSet = true; 
					
					assert rli != 0: "Error: Stage set at the first line -- this is useless, redundant with the initial stage ";
						// this assertion can stay as it does actually fulfill the role of an assertion... 
					
					currRule = currRule.substring(1); 
					
					if (currRule.contains(""+UTILS.GOLD_STAGENAME_FLAG))
						throw new RuntimeException("Error: stage name flag <<"+UTILS.GOLD_STAGENAME_FLAG+">> "
								+ "occuring in a place besides the first character in the rule line -- this is illegal: \n"+currRule);
					if (currRule.contains(UTILS.STAGENAME_LOC_DELIM+""))
						throw new RuntimeException("Error: illegal character found in name for custom stage -- <<"
								+UTILS.STAGENAME_LOC_DELIM+">>");  
					goldStageNameAndLocList.add(""+currRule+UTILS.STAGENAME_LOC_DELIM+rli);
					rulesByTimeInstant.remove(rli);  
				}
				else if (currRule.charAt(0) == UTILS.BLACK_STAGENAME_FLAG)
				{
					blackStagesSet =true;
					currRule = currRule.substring(1); 
					if (currRule.contains(UTILS.STAGENAME_LOC_DELIM+""))
						throw new RuntimeException("Error: illegal character found in name for custom stage -- <<"+UTILS.STAGENAME_LOC_DELIM+">>");  
					blackStageNameAndLocList.add(""+currRule+UTILS.STAGENAME_LOC_DELIM+rli);
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
				System.out.print(gs.substring(0,gs.indexOf(UTILS.STAGENAME_LOC_DELIM))+",");
			System.out.println(""); 
		}
		  
		if (NUM_BLACK_STAGES > 0)
		{
			System.out.print("Black stages:");
			for (String bs : blackStageNameAndLocList)
				System.out.print(bs.substring(0,bs.indexOf(UTILS.STAGENAME_LOC_DELIM))+",");
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
		if (goldStagesSet)	next_gold = Integer.parseInt(goldStageNameAndLocList.get(gsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[1]);
		if (blackStagesSet)	next_black = Integer.parseInt(blackStageNameAndLocList.get(bsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[1]);
		
		for(String currRule : rulesByTimeInstant)
		{
			if(DEBUG_RULE_PROCESSING)
			{	
				List<SChange> newShifts = theFactory.generateSoundChangesFromRule(currRule); 

				System.out.println("Generating rules for rule number "+cri+" : "+currRule);
				for(SChange newShift : newShifts)
					System.out.println("SChange generated : "+newShift+", with type"+newShift.getClass());
			}
			CASCADE.addAll(theFactory.generateSoundChangesFromRule(currRule));
			
			cri++; 
			
			if(goldStagesSet)
			{
				if (cri == next_gold)
				{
					goldStageNames[gsgi] = goldStageNameAndLocList.get(gsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[0];
					goldStageInstants[gsgi] = CASCADE.size();		
					gsgi += 1;
					if ( gsgi < NUM_GOLD_STAGES)
						next_gold = Integer.parseInt(goldStageNameAndLocList.get(gsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[1]);
				}
			}
			
			if(blackStagesSet)
			{
				if (cri == next_black)
				{
					blackStageNames[bsgi] = blackStageNameAndLocList.get(bsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[0];
					blackStageInstants[bsgi] = CASCADE.size();
					bsgi += 1;
					if (bsgi < NUM_BLACK_STAGES)
						next_black = Integer.parseInt(blackStageNameAndLocList.get(bsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[1]);
				}
			}
		}
		
		System.out.println("Diachronic rules extracted. "); 
		
		stageOrdering = UTILS.extractStageOrder(cascFileLoc); 
		
	}
	
	//Behavior based on stipulations on gold stages (or lack of stipulations) in lexicon file and cascade file: 
	// if there is no lexicon header : count number of columns
		// if there is only one column, obviously it is just the input
		// otherwise -- first is input, last is output, any in between are gold stages
	// if there is a lexicon header 
		// lexicon header is identified by being flagged by ~ 
			// i.e. UTILS.GOLD_STAGENAME_FLAG
				// (delimiter is still , i.e. LEX_DELIM )
		// first stage regardless of name is still "in(put)" 
		// for the others the names are saved,
			// and need to be matched...
				// if any are not matched -- throw errro 
				// if last is "Out(put)" or "Res(ult)" it does not need to be matched
					// and is interpreted to be the output gold stage.
	// to be called AFTER extractCascade is. 
		// goldOutput -- determined here. 
	private static void processLexFileHeader(String firstlineproxy)
	{
		System.out.println("Processing lexicon stipulations for gold stages..."); 

		int numCols = firstlineproxy.contains(""+UTILS.LEX_DELIM) ? 
			firstlineproxy.split(""+UTILS.LEX_DELIM).length : 1	;
		System.out.println("Lexicon file has "+numCols+" columns!"); 
		System.out.println("First column assumed to be input."); 
			
		boolean lexiconHasHeader = firstlineproxy.charAt(0) == UTILS.GOLD_STAGENAME_FLAG; 
		if(lexiconHasHeader)
		{
			int coli = 1;
			int numGoldStagesConfirmed = 0; 
			while (coli < numCols - 1)
			{
				String stipName = firstlineproxy.split(""+UTILS.LEX_DELIM)[coli].trim(); 
				int curgs = numGoldStagesConfirmed; 
				while ( coli == NUM_GOLD_STAGES ? false : stipName.equals(goldStageNames[curgs]) )
					blackenGoldStage(coli); 
				if (coli >= NUM_GOLD_STAGES)
					throw new RuntimeException("Error: Failed to find gold stage that was stipulated in lexicon file header : "+stipName);
				numGoldStagesConfirmed++; 
				coli++; 
			}
			// coli == numCols - 1 here.
			String stipName = firstlineproxy.split(""+UTILS.LEX_DELIM)[coli].trim(); 
			
			if (stipName.equalsIgnoreCase("output") && stipName.equalsIgnoreCase("out") && stipName.equalsIgnoreCase("res") && stipName.equalsIgnoreCase("result"))
			{
				System.out.println("Final column stipulated as gold output.");
				goldOutput = true; 
			}
			else
			{
				System.out.println("Final column is a gold stage, not the result column!"); 
				goldOutput = false; 
				int curgs = numGoldStagesConfirmed; 
				while ( coli > NUM_GOLD_STAGES ? false : stipName.equals(goldStageNames[curgs]) )
					blackenGoldStage(coli); 
				if (coli > NUM_GOLD_STAGES)
					throw new RuntimeException("Error: Failed to find gold stage that was stipulated in lexicon file header : "+stipName);
				numGoldStagesConfirmed++; 	
			}
			if (numGoldStagesConfirmed < NUM_GOLD_STAGES)
			{
				System.out.println("Blackening remaining uncorfirmed gold stages that were declared in cascade file!"); 
				while (numGoldStagesConfirmed < NUM_GOLD_STAGES)
					blackenGoldStage(numGoldStagesConfirmed);
			}
		}
		else
		{
			System.out.println("No explicit header declared in lexicon file."); 
			if(numCols == NUM_GOLD_STAGES + 1)
			{
				System.out.println("Each gold stage properly identified if we assume no output!"); 
				
				//TODO debugging
				System.out.println("NUM_GOLD_STAGES : "+NUM_GOLD_STAGES);
				
				goldOutput = false; 
			}
			else if(numCols == NUM_GOLD_STAGES + 2)
			{
				System.out.println("Each gold stage properly identified if we assume last is output!"); 

				//TODO debugging
				System.out.println("NUM_GOLD_STAGES : "+NUM_GOLD_STAGES);
				System.out.println("numCols : "+numCols);
				
				goldOutput = true; 
			}
			else 
			{
				if (numCols != 2) 
					throw new RuntimeException("ERROR: invalid number of columns given that we have "+NUM_GOLD_STAGES+" gold stages as specified in cascade file!"); 
				goldOutput = true; 
				System.out.println("Last column assumed to be output!"); 
				if(NUM_GOLD_STAGES > 0)	System.out.println("Therefore, blackening all gold stages!"); 
				while(NUM_GOLD_STAGES > 0)	blackenGoldStage(0); 
			}
		}
		
	}
	
	// changes one gold stage to a black stage
		// modifying global variables and data structures as appropriate. 
	// int gsi -- the index in data structures of the stage we are blackening. 
	private static void blackenGoldStage(int gsi)
	{
		System.out.println("Blackening gold stage "+gsi+" : "+goldStageNames[gsi]+" at "+goldStageInstants[gsi]); 
	
		int[] oldGoldStageInstants, oldBlackStageInstants; 
		String[] oldGoldStageNames, oldBlackStageNames; 
		
		
		oldGoldStageInstants =  new int[NUM_GOLD_STAGES];
		oldGoldStageNames = new String[NUM_GOLD_STAGES];
		oldBlackStageInstants = new int[NUM_BLACK_STAGES]; 
		oldBlackStageNames = new String[NUM_BLACK_STAGES];
		if (goldStagesSet)
		{
			
			for (int gi = 0; gi < NUM_GOLD_STAGES; gi++)
			{
				oldGoldStageInstants[gi] = goldStageInstants[gi]; 
				oldGoldStageNames[gi] = goldStageNames[gi]; 	
			}
		}
		if (blackStagesSet)
		{ 	
			for (int bi = 0; bi < NUM_BLACK_STAGES; bi++)
			{
				oldBlackStageInstants[bi] = blackStageInstants[bi];
				oldBlackStageNames[bi] = blackStageNames[bi];
			}
		}
		
		int instantToBlacken = goldStageInstants[gsi];
		String nameToBlacken = goldStageNames[gsi];
		
		NUM_GOLD_STAGES--;
		NUM_BLACK_STAGES++;
		if (NUM_BLACK_STAGES == 1)	blackStagesSet = true;
		if (NUM_GOLD_STAGES == 0)	goldStagesSet = false; 
		goldStageGoldLexica = new Lexicon[NUM_GOLD_STAGES];
		
		goldStageNames = new String[NUM_GOLD_STAGES];
		goldStageInstants = new int[NUM_GOLD_STAGES]; 
		blackStageNames = new String[NUM_BLACK_STAGES];
		blackStageInstants = new int[NUM_BLACK_STAGES]; 
		
		int soi = 0, bsloc = 0; 
		while ( !stageOrdering[soi].equals("g"+gsi) ) 
		{
			if(stageOrdering[soi].charAt(0) == 'g')
			{
				int curgi = Integer.parseInt(stageOrdering[soi].substring(1)); 
				goldStageNames[curgi] = oldGoldStageNames[curgi];
				goldStageInstants[curgi] = oldGoldStageInstants[curgi];  
			}
			else
			{
				if (stageOrdering[soi].charAt(0) != 'b')	throw new RuntimeException("Global variable stageOrdering misconstructed!"); 
				int curbi = Integer.parseInt(stageOrdering[soi].substring(1)); 
				if (curbi != bsloc) throw new RuntimeException("Error: a black stage was skipped in stageOrdering!"); 
				blackStageNames[curbi] = oldBlackStageNames[curbi]; 
				blackStageInstants[curbi] = oldBlackStageInstants[curbi]; 
				bsloc++; 
			}
			soi++; 
			if (soi >= stageOrdering.length)	
				throw new RuntimeException("ERROR: the stage we are blackening was never found in stageOrdering!") ;
		}
		blackStageNames[bsloc] = oldGoldStageNames[gsi] ; 
		blackStageInstants[bsloc] = oldGoldStageInstants[gsi] ;
		stageOrdering[soi] = "b"+bsloc;
	
		int isg = gsi; 
		while(isg < NUM_GOLD_STAGES)
		{
			goldStageNames[isg] = oldGoldStageNames[isg+1]; 
			goldStageInstants[isg] = oldGoldStageInstants[isg+1]; 
			isg++; 
		}
		bsloc++;		
		while(bsloc < NUM_BLACK_STAGES)
		{ 
			blackStageNames[bsloc] = oldBlackStageNames[bsloc-1];
			blackStageInstants[bsloc] = oldBlackStageInstants[bsloc-1];
			bsloc++; 
		}
		
		soi++;
		while (soi < stageOrdering.length)
		{
			if (stageOrdering[soi].charAt(0) == 'g')
				stageOrdering[soi] = "g"+(-1 + Integer.parseInt(stageOrdering[soi].substring(1)));
			soi++; 
		}
	}		
	
	public static void main(String args[])
	{
		parseArgs(args); 
		
		//collect task information from symbol definitions file. 
		extractSymbDefs(); 
		extractFeatImpls(); 
				
		System.out.println("Creating SChangeFactory...");
		SChangeFactory theFactory = new SChangeFactory(phoneSymbToFeatsMap, featIndices, featImplications); 
		
		extractCascade(theFactory);

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
		{	File inFile = new File(lexFileLoc); 
			BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream(inFile), "UTF8"));
			while((nextLine = in.readLine()) != null)	
			{	if (nextLine.contains(UTILS.CMT_FLAG+""))
					nextLine = nextLine.substring(0,nextLine.indexOf(UTILS.CMT_FLAG)).trim(); 
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
		
		String firstlineproxy = ""+lexFileLines.get(0); 
		int numCols = firstlineproxy.contains(""+UTILS.LEX_DELIM) ? firstlineproxy.split(""+UTILS.LEX_DELIM).length : 1 ; 
		NUM_ETYMA = lexFileLines.size() - (firstlineproxy.charAt(0) == UTILS.GOLD_STAGENAME_FLAG ? 1 : 0); 
		initStrForms = new String[NUM_ETYMA]; 
		processLexFileHeader(firstlineproxy); 
		
		//TODO debugging
		System.out.println("N ET "+NUM_ETYMA);
		
		boolean justInput = !goldOutput && !goldStagesSet; 
		
		inputForms = new LexPhon[NUM_ETYMA];
		LexPhon[] goldResults = new LexPhon[NUM_ETYMA];  
		LexPhon[][] goldForms = new LexPhon[NUM_GOLD_STAGES][NUM_ETYMA];

		int lfli = 0 ; //"lex file line index"
		
		while(lfli < NUM_ETYMA)
		{
			String theLine = lexFileLines.get(lfli);
			
			initStrForms[lfli] = justInput ? theLine : theLine.split(""+UTILS.LEX_DELIM)[0]; 
			inputForms[lfli] = parseLexPhon(initStrForms[lfli]);
			if (!justInput)
			{
				String[] forms = theLine.split(""+UTILS.LEX_DELIM); 
				if(NUM_GOLD_STAGES > 0)
					for (int gsi = 0 ; gsi < NUM_GOLD_STAGES ; gsi++)
						goldForms[gsi][lfli] = parseLexPhon(forms[gsi+1]);
				if (goldOutput)
					goldResults[lfli] = parseLexPhon(forms[NUM_GOLD_STAGES+1]);
			}
			lfli++;
			if(lfli <NUM_ETYMA && numCols != colCount(theLine))
				throw new RuntimeException("ERROR: incorrect number of columns in line "+lfli);
		}		

		//NOTE keeping gold lexica around solely for purpose of initializing Simulation objects at this point.
		if(NUM_GOLD_STAGES > 0)
			for (int gsi = 0 ; gsi < NUM_GOLD_STAGES; gsi++)
				goldStageGoldLexica[gsi] = new Lexicon(goldForms[gsi]); 
		
		if(goldOutput)	
			goldOutputLexicon = new Lexicon(goldResults); 
		
		System.out.println("Lexicon extracted.");

		System.out.println("Now preparing simulation.");
		
		/** former debugging
		*System.out.println("stageOrdering.length : "+stageOrdering.length); 
		*for(String stoi : stageOrdering)	System.out.println(""+stoi);
		*/ 
		
		theSimulation = new Simulation(inputForms, CASCADE, initStrForms, stageOrdering); 
		if (blackStagesSet)  theSimulation.setBlackStages(blackStageNames, blackStageInstants);
		if (goldOutput)	theSimulation.setGold(goldResults);
		if (goldStagesSet)	theSimulation.setGoldStages(goldForms, goldStageNames, goldStageInstants);
		theSimulation.setStepPrinterval(UTILS.PRINTERVAL); 
		theSimulation.setOpacity(!print_changes_each_rule);

		goldStageInd = 0; blackStageInd=0;
			//index IN THE ARRAYS that the next stage to look for will be at .
		
		makeRulesLog(CASCADE);
		
		String resp; 		
		Scanner inp = new Scanner(System.in);
		
		System.out.println("Now running simulation...");

		while (!theSimulation.isComplete())
		{	
			if(stage_pause){
				theSimulation.simulateToNextStage();
				if(theSimulation.justHitGoldStage())
				{
					System.out.println("Pausing at gold stage "+goldStageInd+": "+goldStageNames[goldStageInd]); 
					System.out.println("Run accuracy analysis here? Enter 'y' or 'n'"); 
					resp = inp.nextLine().substring(0,1); 
					while(!resp.equalsIgnoreCase("y") && !resp.equalsIgnoreCase("n"))
					{
						System.out.println("Invalid response. Do you want to run accuracy analysis here? Please enter 'y' or 'n'.");
						resp = inp.nextLine().substring(0,1); 
					}
					if(resp.equalsIgnoreCase("y"))	
						haltMenu(goldStageInd, inp, theFactory);
					goldStageInd++; 
				}
				else if (!theSimulation.isComplete())//hit black
				{
					System.out.println("Hit black stage "+blackStageInd+": "+blackStageNames[blackStageInd]); 
					System.out.println("Error analysis at black stages is not currently supported."); //TODO make it supported...
					System.out.println("Print latest developments from latest stage? Please enter 'y' or 'n'.");
					resp = inp.nextLine().substring(0,1); 

					while(!resp.equalsIgnoreCase("y") && !resp.equalsIgnoreCase("n"))
					{
						System.out.println("Invalid response. Do you want to run accuracy analysis here? Please enter 'y' or 'n'.");
						resp = inp.nextLine().substring(0,1); 
					}
					if(resp.equalsIgnoreCase("y"))	
					{
						Lexicon prevLex = theSimulation.getInput(); 
						String prstname = "Input";
						
						if (goldStageInd + blackStageInd > 0)
						{
							boolean lastWasBlack = (goldStageInd > 0 && blackStageInd > 0) ? 
									(goldStageInstants[goldStageInd-1] < blackStageInstants[blackStageInd-1])
									: blackStageInd > 0; 
							prevLex = theSimulation.getStageResult(!lastWasBlack, (lastWasBlack ? blackStageInd : goldStageInd) - 1);
							prstname = lastWasBlack ? blackStageNames[blackStageInd-1] : goldStageNames[goldStageInd -1]; 
						}
						
						String bd = "\t,\t"; 
						System.out.println("etymID"+bd+"Input"+bd+"Last stage: "+prstname+""+bd+"Curr stage: "+blackStageNames[blackStageInd]);
						for (int i = 0 ; i < NUM_ETYMA ; i++)
							System.out.println(i+bd+inputForms[i]+bd+prevLex.getByID(i)+bd+theSimulation.getCurrentForm(i));
					}
					blackStageInd++; 
				}
			}
			else	theSimulation.simulateToEnd(); 
		}
		
		System.out.println("Simulation complete.");
			
		File dir = new File(""+runPrefix); 
		dir.mkdir(); 
		
		System.out.println("making derivation files in "+dir);
		
		//make derivation files.
		makeDerivationFiles(); 	
		
		//make output graphs file
		System.out.println("making output graph file in "+dir);
		makeOutGraphFile(); 
				
		if(goldOutput)
		{
			haltMenu(-1, inp,theFactory);
			
			System.out.println("Writing analysis files...");
			//TODO -- enable analysis on "influence" of black stages and init stage... 
			
			//TODO figure out what we want to do here...
			ErrorAnalysis ea = new ErrorAnalysis(theSimulation.getCurrentResult(), goldOutputLexicon, featsByIndex, 
					feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));
			ea.makeAnalysisFile("testResultAnalysis.txt", false, theSimulation.getCurrentResult());
			ea.makeAnalysisFile("goldAnalysis.txt",true,goldOutputLexicon);
			
			if(goldStagesSet)
			{	
				for(int gsi = 0; gsi < NUM_GOLD_STAGES - 1 ; gsi++)
				{	
					ErrorAnalysis eap = new ErrorAnalysis(theSimulation.getStageResult(true, gsi), goldStageGoldLexica[gsi], featsByIndex,
							feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));
					eap.makeAnalysisFile(goldStageNames[gsi].replaceAll(" ", "")+"ResultAnalysis.txt",
							false, theSimulation.getStageResult(true, gsi));
				}
			}
		}
		System.out.println("Thank you for using DiaSim"); 
		inp.close();
	}

	private static void makeOutGraphFile()
	{	
		String filename = runPrefix + "_output_graph"+ UTILS.OUT_GRAPH_FILE_TYPE; 
		UTILS.writeToFile(filename, theSimulation.outgraph(),true); 
	}
	
	private static void makeRulesLog(List<SChange> theShiftsInOrder) {
		String filename = runPrefix + "_rules_log.txt"; 
		String output = "";
		for (SChange thisShift : theShiftsInOrder)
			output += ""+thisShift + (DEBUG_RULE_PROCESSING ? "| ORIG : "+thisShift.getOrig(): "") + "\n"; 
		UTILS.writeToFile(filename, output,false); 
	}

	private static void makeDerivationFiles()
	{
		File derdir = new File(runPrefix,"derivation"); 
		derdir.mkdir(); 
	
		for( int wi =0; wi < NUM_ETYMA; wi ++) 
		{
			String filename = new File(runPrefix, new File("derivation","etym"+wi+".txt").toString()).toString(); 
			String output = "Derivation file for run '"+runPrefix+"'; etymon number :"+wi+":\n"
				+	inputForms[wi]+" >>> "+theSimulation.getCurrentForm(wi)
				+ (goldOutput ? " ( GOLD : "+goldOutputLexicon.getByID(wi)+") :\n"  : ":\n")
					+theSimulation.getDerivation(wi)+"\n";
			UTILS.writeToFile(filename, output, false); 
		}
	}
	
	// missLocations are the indices of words that ultimately resulted in a miss between the testResult and the gold
	// outputs the scores for each phone in the word in the lexicon
	/** TODO abrogated -- method currently unused.
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
	}**/
	
	/** auxiliary.
	 * given String @param toLex
	 * @return its representation as a LexPhon containing a sequence of Phone instances
	 * TODO note we assume the phones are separated by PH_DELIM (presumably ' ') 
	 */
	private static LexPhon parseLexPhon(String toLex)
	{
		if (toLex.contains(UTILS.ABSENT_PH_INDIC))
		{	return new AbsentLexPhon();	}
		
		String[] toPhones = toLex.trim().split(""+UTILS.PH_DELIM);
		
		List<SequentialPhonic> phones = new ArrayList<SequentialPhonic>(); //LexPhon class stores internal List of phones not an array,
			// for better ease of mutation

		for (String toPhone : toPhones)
		{
			if (toPhone.equals("#") || toPhone.equals("+"))
				phones.add(new Boundary(toPhone.equals("#") ? "word bound" : "morph bound"));
			else
			{
				if (!phoneSymbToFeatsMap.containsKey(toPhone))
					throw new RuntimeException("ERROR: tried to declare a phone in a word in the lexicon using an invalid symbol.\n"
					+ "Symbol is : '"+toPhone+"', length = "+toPhone.length());
				phones.add(new Phone(phoneSymbToFeatsMap.get(toPhone), featIndices, phoneSymbToFeatsMap));
			}
		}
		return new LexPhon(phones);
	}
	
	//auxiliary method -- get number of columns in lexicon file. 
	private static int colCount(String str)
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
	
	// @param (cutoff) -- rule number that the black stage must be BEFORE.
	private static void printIncludedBlackStages(int first, int last)
	{
		if(blackStagesSet)
			for(int bsi = first; bsi < last + 1; bsi++)
				System.out.println("b"+bsi+": "+
					blackStageNames[bsi]+" (@rule #: "+blackStageInstants[bsi]+")");
	}

	private static void printIncludedGoldStages(int firstToPrint, int lastToPrint)
	{
		if(goldStagesSet)
			for(int gsi = firstToPrint; gsi < lastToPrint + 1; gsi++)
				System.out.println("g"+gsi+": "+
					goldStageNames[gsi]+" gold forms (@rule #: "+goldStageInstants[gsi]+")");
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
	
	// @param curr_stage : -1 if at final result point, otherwise valid index of stage in goldStage(Gold/Result)Lexica
	private static void haltMenu(int curSt, Scanner inpu, SChangeFactory fac)
	{	
		Lexicon r = theSimulation.getCurrentResult();
		Lexicon g = (curSt == -1) ? goldOutputLexicon : goldStageGoldLexica[curSt]; 
		
		ErrorAnalysis ea = new ErrorAnalysis(r, g, featsByIndex, 
				feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));

		System.out.println("Overall accuracy : "+ea.getAccuracy());
		System.out.println("Accuracy within 1 phone: "+ea.getPctWithin1());
		System.out.println("Accuracy within 2 phones: "+ea.getPctWithin2());
		System.out.println("Average edit distance per from gold phone: "+ea.getAvgPED());
		System.out.println("Average feature edit distance from gold: "+ea.getAvgFED());
		
		int lastGoldOpt = (curSt == -1 ? NUM_GOLD_STAGES : curSt) - 1;
		int lastBlkOpt = NUM_BLACK_STAGES - 1;
		while((lastBlkOpt < 0 || curSt < 0) ? false : blackStageInstants[lastBlkOpt] > goldStageInstants[curSt])
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
					+ "| 1 : Set focus point (upon which actions are conditioned, incl. filtering [2])       |\n"
					+ "| 2 : Set filter sequence                                                             |\n"
					+ "| 3 : Query                                                                           |\n"
					+ "| 4 : Confusion diagnosis at evaluation point                                         |\n"
					+ "| 5 : Run autopsy for (at evaluation point) (for subset lexicon if specified)         |\n"
					+ "| 6 : Review results (stats, word forms, errors) at evaluation point (goes to submenu)|\n"
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
						printIncludedGoldStages(0, lastGoldOpt); 
						System.out.println("F : "+ (curSt == -1 ? "final forms" : "current forms at stage "+curSt));
						System.out.println("Please enter the indicator for the stage you desire"); 
						resp = inpu.nextLine().substring(0,1);
						chosen = validOptions.contains(resp);
						if(!chosen)	System.out.println("Invalid response. Please choose a valid indicator for the new evaluation stage."); 
					}
					
					evalStage = resp.equals("F") ? curSt : Integer.parseInt(resp); 
					r = resp.equals("F") ? theSimulation.getCurrentResult() : theSimulation.getStageResult(true, evalStage);
					g = (curSt == -1 && resp.equals("F") ) ? goldOutputLexicon : goldStageGoldLexica[evalStage];
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
				System.out.println("Current focus point lexicon: "+(focPtSet ? focPtName : "not (yet) defined"));
				System.out.println("Current filter : "+(filterIsSet ? filterSeq.toString() : "not (yet) defined")); 
				
				boolean chosen = false; 
				while(!chosen)
				{
					System.out.println("Available options for focus point:");
					printIncludedGoldStages(0, lastGoldOpt); printIncludedBlackStages(0, lastBlkOpt); 
					System.out.println("In: delete focus pt & filter by input forms"
							+ "\nOut: delete it & filter by generated output forms"
							+ "\nGold: delete it & filter by correct (gold) forms for output (last gold stage if halted before end)"
							+ "\nU: delete it, and also delete filter (return to scoping over whole lexicon)"
							+ "\nR#: right before rule with index number <#>"
							+ "(you can find rule indices with option 3 to query on the main menu)\n"
							+ "Please enter the appropriate indicator."); 
					
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
						if(resp.equals("R0"))	System.out.println("Invalid input: 'R0' is not a valid option -- instead choose 'In' "
								+ "to delete focus point and use the input for filtering");
						else if (resp.charAt(0) == 'g' && !goldStagesSet)
							System.out.println("Invalid input: cannot use 'g' when no gold stages are set!"); 
						else if (resp.charAt(0) == 'b' && !blackStagesSet)
							System.out.println("Invalid input: cannot use 'g' when no b stages are set!"); 
						else if ("bgR".contains(""+resp.charAt(0)) && !UTILS.isInt(resp.substring(1)))
							System.out.println("Invalid input: 'R', 'g', and 'b' require a valid integer after them, but '"+resp.substring(1)+"' is not!"); 
						else if (resp.charAt(0) == 'R')
							System.out.println("'"+resp+"' is not a valid option: the last rule is number "+(CASCADE.size()-1));
						else if (resp.charAt(0) == 'g')
							System.out.println("'"+resp+"' is not a valid option, the last computed gold stage is number "+lastGoldOpt); 
						else if (resp.charAt(0) == 'b')
							System.out.println("'"+resp+"' is not a valid option, the last computed black stage is number "+lastBlkOpt); 
						else	System.out.println("Invalid input : '"+resp+"'\nPlease select a valid option listed below:");
					}
					else
					{
						focPtSet = true;
						if(resp.length() < 4 ? false : resp.substring(0,4).toLowerCase().equals("gold")) 
							resp = "Gold";// preempt dumb capitalization stuff that could cause errors because g# is used to grab gold stage inds. 
						if(resp.charAt(0) == 'g')
						{
							int si = Integer.parseInt(resp.substring(1));
							focPtLex = goldStageGoldLexica[si]; 
							focPtLoc = goldStageInstants[si];
							focPtName = goldStageNames[si]+" [r"+focPtLoc+"]";
							ea.setFocus(focPtLex, focPtName); 
						}
						else if (resp.charAt(0) == 'b')
						{
							int si = Integer.parseInt(resp.substring(1));
							focPtLex = theSimulation.getStageResult(false, si);
							focPtLoc = blackStageInstants[si];
							focPtName = blackStageNames[si]+" [r"+focPtLoc+"]";
							ea.setFocus(focPtLex, focPtName); 
						}
						else if (resp.charAt(0) == 'R')
						{
							focPtLoc = Integer.parseInt(resp.substring(1)); 
							focPtLex = UTILS.toyDerivation(theSimulation,CASCADE.subList(0, focPtLoc)).getCurrentResult();
							focPtName = "pivot@R"+focPtLoc; 
							ea.setFocus(focPtLex, focPtName); 
						}
						else
						{
							focPtLoc = -1; focPtLex = null; focPtName = ""+resp;
							ea = new ErrorAnalysis(r, g, featsByIndex, 
									feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));
							
							if(resp.equals("U"))
							{	filterSeq = new SequentialFilter(new ArrayList<RestrictPhone>(), new String[] {});
								filterIsSet = false; 
								focPtName = "";
								focPtSet = false; 
							}
							else
							{	
								focPtLex = resp.equals("In") ? theSimulation.getInput() : 
								resp.equals("Out") ? theSimulation.getCurrentResult() : 
									(curSt == -1) ? goldOutputLexicon : goldStageGoldLexica[curSt];
								ea.setFocus(focPtLex,focPtName);
							}
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
					System.out.println("Enter the phoneme sequence filter, delimiting phones with '"+UTILS.PH_DELIM+"'");
					
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
						System.out.println("Enter the input form, separating phones by '"+UTILS.PH_DELIM+"'");
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
							LexPhon[] wl = inputForms;
							String inds = UTILS.etymInds(wl, query);
							System.out.println("Ind(s) with this word as input : "+inds);  
						}
					}
					else if(resp.equals("1")||resp.equals("3") || resp.equals("4"))
					{
						System.out.println("Enter the ID to query:");
						String idstr = inpu.nextLine(); 
						boolean queryingRule = resp.equals("4"); //otherwise we're querying an etymon.
						int theID = UTILS.getValidInd(idstr, queryingRule ? CASCADE.size() : NUM_ETYMA - 1) ; 
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
							else if(resp.equals("1"))	System.out.println(inputForms[theID]); 
							else 	System.out.println(""+theSimulation.getDerivation(theID));
						}
					}
					else if(resp.equals("2"))
					{
						System.out.println("etymID"+UTILS.STAGE_PRINT_DELIM+"Input"+UTILS.STAGE_PRINT_DELIM+"Gold");
						for (int i = 0 ; i < r.getWordList().length ; i++)
							System.out.println(""+i+UTILS.STAGE_PRINT_DELIM+inputForms[i]+UTILS.STAGE_PRINT_DELIM+goldOutputLexicon.getByID(i));
					}
					else if(resp.equals("5"))
					{
						System.out.println("Enter the string you want to query with.\n");
						resp = inpu.nextLine().replace("\n", "");
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
			else if(resp.equals("4"))	ea.confusionDiagnosis(true);
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
						+ "| 2 : Print all corresponding forms as above for all mismatched etyma                 |\n"
						+ "| 3 : Print all mismatched forms only at eval point (for subset if specified)        |\n"
						+ "| 9 : Exit this menu._________________________________________________________________|\n");  
					
					resp = inpu.nextLine().substring(0,1);
					
					if(resp.equals("0"))
					{
						System.out.println("Printing stats:"+ (ea.isFiltSet() ? " for filter "+filterSeq.toString()+ " at "+focPtName : "" ));
						
						System.out.println("Overall accuracy : "+ea.getAccuracy());
						System.out.println("Accuracy within 1 phone: "+ea.getPctWithin1());
						System.out.println("Accuracy within 2 phones: "+ea.getPctWithin2());
						System.out.println("Average edit distance per from gold phone: "+ea.getAvgPED());
						System.out.println("Average feature edit distance from gold: "+ea.getAvgFED());
					}
					else if("12".contains(resp))
					{
						boolean is2 = "2".equals(resp); 
						System.out.println("Printing all "+(is2 ? "mismatched ":"")+
								"etyma: Input, " + (ea.isFocSet() ? "FOC: "+focPtName+"," : "")
								+"Result, Gold"); 
						ea.printFourColGraph(theSimulation.getInput(), is2);	
					}
					else if(resp.equals("3"))
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
			else if(resp.equals("7")) //forking test for proposed changes to cascade. 
			{
				DHSWrapper DHSinterface = new DHSWrapper(theSimulation, feats_weighted, featsByIndex, FT_WTS, id_wt, cascFileLoc, fac); 
				DHSinterface.queryProposedChanges(inpu); 
			}
			else if(resp.equals("9")) {
				System.out.println("Ending"); cont = false; 
			}
			else	System.out.println("Invalid response. Please enter one of the listed numbers"); 
		}
	}
	
	//TODO below is abrogated as it is not in use. 
	//makes  EA object on subset of gold/res pairs that have a specified sequence in either the gold or res as flagged by boolean second param
	/**public static ErrorAnalysis analyze_subset_with_seq (Lexicon ogRes, Lexicon ogGold, List<SequentialPhonic> targSeq, boolean look_in_gold)
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
	**/
	
	// for the lexicon of any given stage, passed as parameter, 
	// outputs hashmap where the value for each key Phone instance
	// is the average Levenshtein distance for words containing that phone 
	// normalized for length of the word
	// counted for the number of times the phone actually occurs in that word out of total phones in the word, each time. 
	/** TODO currently abrogated, as not in use. 
	private static HashMap<Phone,Double> avgLDForWordsWithPhone (Lexicon lexic)
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
							ErrorAnalysis.levenshteinDistance(theSimulation.getCurrentResult().getByID(li),
									goldOutputLexicon.getByID(li)) / (double)goldOutputLexicon.getByID(li).getNumPhones() ;
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
	}**/ 
	
	
	/** TODO currently abrogated, as not in use. 
	private static HashMap<Phone,Double> avgFEDForWordsWithPhone (Lexicon lexic)
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
					distMeasure.compute(theSimulation.getCurrentResult().getByID(li),
							goldOutputLexicon.getByID(li));
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
		
	}**/ 
	
	// required : runPrefix must be specified 
	// flags: -r : debug rule processing
	//		  -d : debugging mode -- TODO implement
	//		  -p : print words every time they are changed by a rule
	//		  -e : (explicit) do not use feature implications
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
		cascFileLoc = "DiaCLEF"; 
		featImplsLoc = "FeatImplications"; 
		id_wt = 0.5; 
		
		DEBUG_RULE_PROCESSING = false; 
		DEBUG_MODE = false; 
		print_changes_each_rule = false;
		no_feat_impls = false; 
		
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
				if (i < args.length)	cascFileLoc = args[i++];
				else	System.err.println("-rules requires a location for ruleset file.");
				if (vflag)	System.out.println("ruleset file location: "+cascFileLoc);
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
						case 'e': 
							no_feat_impls = true; 
							if (vflag)	System.out.println("Ignoring any feature implications.");
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
            throw new Error("Usage: DerivationSimulation [-verbose] [-redphi] [-idcost cost] [-rules afile] [-lex afile] [-symbols afile] [-impl afile] -out prefix"); 	
	}
	
	private static void printRuleAt(int theInd)
	{
		if (theInd == CASCADE.size())
			System.out.println("Ind "+theInd+" is right after the realization of the last rule.");
		else System.out.println(CASCADE.get(theInd)); 
	}
}


