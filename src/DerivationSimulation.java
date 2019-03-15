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


//TODO implement command line support 
//TODO metric output regularization...

/**TODO update here when decisions have been made
 * 
 * main class for diachronic derivation system
 * first takes in info from relecvant files: 
 * 		symbolDefs.csv -- gets the list of relevant features from the first row
 * 			and the definition of each phone symbol with respect to those features
 * 				from the lines below 
 *		FeatTranslations.txt and FeatImplications.txt -- for auxiliary operations, use as necessary
 *	then inputs shifts file -- saves these as is appropriate (decide how to do this, then update here
 *	and finally dataset or words entered by user -- probably use separate method for this. 
 * @author Clayton Marr
 *
 */
public class DerivationSimulation {
	
	private final static char MARK_POS = '+', MARK_NEG = '-', MARK_UNSPEC = '.', FEAT_DELIM = ','; 
	private final static int POS_INT = 2, NEG_INT = 0, UNSPEC_INT = 1;
	private final static char IMPLICATION_DELIM=':', PH_DELIM = ' '; 
	private final static char CMT_FLAG = '$'; //marks taht the text after is a comment in the sound rules file, thus doesn't read the rest of the line
	private final static char GOLD_STAGENAME_FLAG = '~', BLACK_STAGENAME_FLAG ='=';
	private final static char STAGENAME_LOC_DELIM = ':'; 
	private final static char LEX_DELIM =','; 
	private final static char STAGE_PRINT_DELIM = ',';  
	private final static String OUT_GRAPH_FILE_TYPE = ".csv"; 
	
	private static String[] featsByIndex; 
	private static HashMap<String, Integer> featIndices;
	private static HashMap<String, String> phoneSymbToFeatsMap;
	private static HashMap<String, String> phoneFeatsToSymbMap; //TODO abrogate either this or the previous class variable
	private static HashMap<String, String[]> featImplications; 
	private HashMap featTranslations; //TODO currently abrogated 
	private static List<String> rulesByTimeInstant; 
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
	private static String[] wordTrajectories; //stores trajectory(form at every time step), with stages delimited by line breaks, of each word 
	
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
	
	//TODO to be set in command line...
	private static String runPrefix;
	private static boolean DEBUG_RULE_PROCESSING = true; 
	private static int num_prob_phones_displayed = 10; //the top n phones most associated with errors... 
	
	private static int goldStageInd, blackStageInd; 
	
	public static void main(String args[])
	{
		Scanner input = new Scanner(System.in); 
		
		featIndices = new HashMap<String, Integer>() ; 
		phoneSymbToFeatsMap = new HashMap<String, String>(); 
		phoneFeatsToSymbMap = new HashMap<String, String>(); 
		featImplications = new HashMap<String, String[]>(); 
		
		System.out.println("What would you like the file output prefix for this run to be?");
		runPrefix = input.nextLine(); 
		
		System.out.println("Would you like to use the standard symbol definitions file? Please enter 'y' or 'n'.");
		String resp = input.nextLine(); 
		
		while(!resp.equalsIgnoreCase("y") && !resp.equalsIgnoreCase("n"))
		{
			System.out.println("Invalid response.");
			System.out.println("Would you like to use the standard symbol definitions file location? Please enter 'y' or 'n'. ");
			resp = input.nextLine(); 
		}
		
		String symbDefsLoc = (resp.equalsIgnoreCase("y")) ? "symbolDefs.csv" : ""; 
		if(resp.equalsIgnoreCase("n"))
		{
			System.out.println("Please enter the correct location of the symbol definitions file you would like to use:");
			symbDefsLoc = input.nextLine(); 
		}
		
		//collect task information from symbol definitions file. 
		
		//TODO debugging
		System.out.println("Symbol defs location: "+symbDefsLoc);
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

		System.out.println("Would you like to use the standard feature implications file location? Please enter 'y' or 'n'.");
		resp = input.nextLine(); 
		
		while(!resp.equalsIgnoreCase("y") && !resp.equalsIgnoreCase("n"))
		{
			System.out.println("Invalid response.");
			System.out.println("Would you like to use the standard feature implications file? Please enter 'y' or 'n'. ");
			resp = input.nextLine(); 
		}
		
		String featImplsLoc = (resp.equals("y")) ? "FeatImplications" : ""; 
		if(resp.equals("n"))
		{
			System.out.println("Please enter the correct location of the feature implications file you would like to use:");
			featImplsLoc = input.nextLine(); 
		}
		
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
		
		System.out.println("Use current default rules file location? Enter 'y' or 'n'."); 
		resp = input.nextLine(); 
		
		while(!resp.equalsIgnoreCase("y") && !resp.equalsIgnoreCase("n"))
		{
			System.out.println("Invalid response.");
			System.out.println("Use current default rules file location? Please enter 'y' or 'n'. ");
			resp = input.nextLine(); 
		}
		
		String ruleFileLoc = (resp.equalsIgnoreCase("y")) ? "MKPopeRules.txt" : ""; 
		if (resp.equalsIgnoreCase("n"))	
		{
			System.out.println("Please enter the location of your alternative rules file: ");
			ruleFileLoc = input.nextLine(); 
		}
		
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
			if( currRule.charAt(0) == GOLD_STAGENAME_FLAG )
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
			else	rli++;
		}
		
		NUM_GOLD_STAGES = goldStageNameAndLocList.size(); 
		NUM_BLACK_STAGES =blackStageNameAndLocList.size();
		
		System.out.println("Using "+NUM_GOLD_STAGES+" custom stages."); 
		
		goldStageGoldLexica = new Lexicon[NUM_GOLD_STAGES];
		goldStageResultLexica = new Lexicon[NUM_GOLD_STAGES];
		blackStageResultLexica =new Lexicon[NUM_BLACK_STAGES];
		goldStageNames = new String[NUM_GOLD_STAGES];
		blackStageNames = new String[NUM_BLACK_STAGES];
		goldStageTimeInstants = new int[NUM_GOLD_STAGES]; 
		blackStageTimeInstants = new int[NUM_BLACK_STAGES]; 
		
		if(goldStagesSet)
		{
			for(int csi = 0; csi < NUM_GOLD_STAGES; csi++)
			{
				//TODO debugging
				System.out.println("Stage name and loc : "+goldStageNameAndLocList.get(csi));
				
				String[] stageNameAndLoc = goldStageNameAndLocList.get(csi).split(""+STAGENAME_LOC_DELIM);
				goldStageNames[csi] = stageNameAndLoc[0]; 
				
				//TODO debuggging
				System.out.println("gold stage : "+goldStageNames[csi]);
				
				goldStageTimeInstants[csi] = Integer.parseInt(stageNameAndLoc[1]); 
			}
		}
		if(blackStagesSet)
		{
			for(int csi = 0; csi < NUM_BLACK_STAGES; csi++)
			{
				//TODO debugging
				System.out.println("Stage name and loc : "+blackStageNameAndLocList.get(csi));
				
				String[] stageNameAndLoc = blackStageNameAndLocList.get(csi).split(""+STAGENAME_LOC_DELIM);
				blackStageNames[csi] = stageNameAndLoc[0]; 
				
				//TODO debuggging
				System.out.println("black stage : "+blackStageNames[csi]);
				
				blackStageTimeInstants[csi] = Integer.parseInt(stageNameAndLoc[1]); 
			}
		}
		
		// parse the rules
		List<SChange> theShiftsInOrder = new ArrayList<SChange>();
		
		int cri = 0; 
		
		for(String currRule : rulesByTimeInstant)
		{
			//TODO debugging
			System.out.println("Generating rules for rule number "+cri+" : "+currRule);
			
			List<SChange> newShifts = theFactory.generateSoundChangesFromRule(currRule); 
			for(SChange newShift : newShifts)
				System.out.println("SChange generated : "+newShift+", with type"+newShift.getClass());
			
			theShiftsInOrder.addAll(theFactory.generateSoundChangesFromRule(currRule));
			cri++; 
		}
		
		System.out.println("Diachronic rules extracted. "); 
		
		System.out.println("Do you wish to print words changed for each rule? Enter 'y' or 'n'");
		resp = input.nextLine();
		while(!resp.equalsIgnoreCase("y") && !resp.equalsIgnoreCase("n"))
		{
			System.out.println("Invalid response.");
			System.out.println("Do you wish to print words changed for each rule? Please enter 'y' or 'n'. ");
			resp = input.nextLine(); 
		}
		boolean print_changes_each_rule = (resp.equalsIgnoreCase("y"));
		
		System.out.println("Do you wish to pause when a stage checkpoint flag is hit? Enter 'y' or 'n'");
		resp = input.nextLine();
		while(!resp.equalsIgnoreCase("y") && !resp.equalsIgnoreCase("n"))
		{
			System.out.println("Invalid response.");
			System.out.println("Do you wish to pause when a stage checkpoint flag is hit? Please enter 'y' or 'n'. ");
			resp = input.nextLine(); 
		}
		boolean stage_pause = (resp.equalsIgnoreCase("y"));
		
		//TODO fix this... how are we going to use? 
		
		//now input lexicon 
		//collect init lexicon ( and gold for stages or final output if so specified) 
		//copy init lexicon to "evolving lexicon" 
		//each time a custom stage time step loc (int in the array goldStageTimeInstantLocs or blackStageTimeInstantLocs) is hit, save the 
		// evolving lexicon at that point by copying it into the appropriate slot in the customStageLexica array
		// finally when we reach the end of the rule list, save it as testResultLexicon
	
		
		System.out.println("Do you wish to use the default location for the lexicon input file? Enter 'y' or 'n'"); 
		resp = input.nextLine();
		while(!resp.equalsIgnoreCase("y") && !resp.equalsIgnoreCase("n"))
		{
			System.out.println("Invalid response.");
			System.out.println("Do you wish to use the default location for the lexicon input file? Please enter 'yes' or 'no'. ");
			resp = input.nextLine(); 
		}
		String lexFileLoc = (resp.equalsIgnoreCase("y")) ? "LatinLexFileForMKPopeTester.txt" : "";
		if(resp.equalsIgnoreCase("n")) 
		{
			System.out.println("Please enter the correct location of the symbol definitions file you would like to use:");
			lexFileLoc = input.nextLine(); 
		}
		
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
			
			System.out.println("Lex "+lfli+" : "+theLine);
			
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
		int ri = 0, numRules = theShiftsInOrder.size(); //for iteration.
		
		makeRulesLog(theShiftsInOrder); 
		
		while (ri < numRules)
		{
			if(ri % 100 == 0)	System.out.println("On rule number "+ri);
				
			SChange thisShift =  theShiftsInOrder.get(ri);
			
			if(goldStageInd < NUM_GOLD_STAGES)
			{
				if ( ri == goldStageTimeInstants[goldStageInd])
				{
					testResultLexicon.updateAbsence(goldStageGoldLexica[goldStageInd].getWordList());
					goldStageResultLexica[goldStageInd] = new Lexicon(testResultLexicon.getWordList());
					
					if (stage_pause)
					{
						System.out.println("Pausing at gold stage "+goldStageInd+", "+goldStageNames[goldStageInd]); 
						System.out.println("Run accuracy analysis here? Enter 'y' or 'n'"); 
						resp = input.nextLine(); 
						while(!resp.equalsIgnoreCase("y") && !resp.equalsIgnoreCase("n"))
						{
							System.out.println("Invalid response. Do you want to run accuracy analysis here? Please enter 'y' or 'n'.");
							resp = input.nextLine(); 
						}
						if(resp.equalsIgnoreCase("y"))
						{
							ErrorAnalysis ea = new ErrorAnalysis(testResultLexicon, goldStageGoldLexica[goldStageInd], featsByIndex); 
							
						}
					}
					goldStageInd++;
				}
			}
			if(blackStageInd<NUM_BLACK_STAGES)
			{
				if(ri == blackStageTimeInstants[blackStageInd])
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
						System.out.println("etym "+wi+" is now : "+testResultLexicon.getByID(wi)+"\t\t[ "+initLexicon.getByID(wi)+" >>> "+goldResultLexicon.getByID(wi)+" ]");
				ri++;
			}
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
		
		if(goldOutput)
		{
			double[] PERFORMANCE_arr = analyzeLDAccAndLakation(finLexLD, finLexLak, finMissInds, testResultLexicon, goldResultLexicon); 
			PERFORMANCE = PERFORMANCE_arr[0] ; 
			ACCURACY = PERFORMANCE_arr[1]; 
			NEAR_ACCURACY = PERFORMANCE_arr[3];
			
			System.out.println("FINAL OVERALL LAKATION : "+PERFORMANCE); 
			System.out.println("FINAL OVERALL ACCURACY : "+ACCURACY); 
			System.out.println("ACCURACY WITHIN 1 PHONE: "+PERFORMANCE_arr[2]);
			System.out.println("ACCURACY WITHIN 2 PHONES : "+NEAR_ACCURACY);
			System.out.println(numFalse(finMissInds)+" misses out of "+NUM_ETYMA+" etyma.");
			if( NUM_GOLD_STAGES > 0 )
			{
				for (int i = 0 ; i < NUM_GOLD_STAGES; i++)
				{	System.out.println(goldStageNames[i] +" overall lakation : "
							+analyzeLDAccAndLakation(stageLexLDs.get(i), stageLexLaks.get(i), stageMissInds.get(i), goldStageResultLexica[i], goldStageGoldLexica[i]));
					System.out.println(numFalse(stageMissInds.get(i))+" misses out of "+NUM_ETYMA+" etyma.");
				}
			}
			//TODO IMPORTANT, print here or to file, by phone analysis....  
		}
		//TODO ABROGATED BELOW
		if(goldOutput &&  false) //TODO implement this, remove "&& false" 
		{	
			PERFORMANCE = getLDErrorAvgdOverWordLengthInPhones(); 
			System.out.println("PERFORMANCE ON GOLD RESULT SET = "+PERFORMANCE);
			numCorrectEtyma = 0;
			
			finMissInds = new boolean[NUM_ETYMA]; 
			for(int i = 0; i < NUM_ETYMA; i++)
			{
				finLexLD[i] = levenshteinDistance(testResultLexicon.getByID(i), 
						goldResultLexicon.getByID(i)); 
				finMissInds[i] = (finLexLD[i] != 0);
				numCorrectEtyma += (finLexLD[i] == 0) ? 0 : 1;
				wordTrajectories[i] = wordTrajectories[i].substring(0, wordTrajectories[i].indexOf("\n")) +
					(finMissInds[i] ? " MISS, edit distance: "+ finLexLD[i]:" HIT") +
					wordTrajectories[i].substring(wordTrajectories[i].indexOf("\n"));	
			}
			
			System.out.println("Writing analysis files...");//TODO maybe need more print statements here. 
			
			makeAnalysisFile("initialFormInfluenceAnalysis.txt", "Initial", initLexicon); 
			makeAnalysisFile("testResultAnalysis.txt", "Test Result", testResultLexicon); 
			makeAnalysisFile("goldAnalysis.txt","Gold",goldResultLexicon); 
			
			if(goldStagesSet)
			{	
				for(int gsi = 0; gsi < NUM_GOLD_STAGES ; gsi++)
					makeAnalysisFile(goldStageNames[gsi].replaceAll(" ", "")+"ResultAnalysis.txt",
							goldStageNames[gsi]+" Result", goldStageResultLexica[gsi]);
			}
			if (blackStagesSet)
			{	
				for(int bsi = 0; bsi < NUM_BLACK_STAGES ; bsi++)
					makeAnalysisFile(blackStageNames[bsi].replaceAll(" ", "")+"ResultAnalysis.txt",
							blackStageNames[bsi]+" Result", blackStageResultLexica[bsi]);
			}
				
			System.out.println("Analysis files written!");
			
		}
		
		//TODO make the calculations and output the files! 
		
		input.close();
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

	/**
	 * @precondition: @param outForms and @param goldForms have been filled as part of a completed simulation
	 * @precondition: lexLD and lexLak are static
	 * @note : DESTRUCTIVE! Intended to modify static variables
	 * fills lexLD and lexLak and isHit
	 * Lakation -- level of quasisynchronic distortion that arose diachronically
	 * 	here it is measured for each lexeme by Levenshtein distance divided by phone length of gold form for the etymon
	 * @returns two-item array of doubles where the first element is the average lexical lakation
	 * 		and the second is the percent accuracy 0 to 100 
	 * 		third is accuracy within two phones. 
	 */
	private static double[] analyzeLDAccAndLakation(int[] lexLD, double[] lexLak, boolean[] isHit, Lexicon outForms, Lexicon goldForms)
	{
		lexLD = new int[NUM_ETYMA];
		lexLak = new double[NUM_ETYMA]; 
		isHit = new boolean[NUM_ETYMA]; 
		double totLexQuotients = 0.0, numHits= 0.0, numAlmostHits = 0.0, numNearHits = 0.0;
		for (int i = 0 ; i < NUM_ETYMA; i++)
		{
			int numPhonesInGoldWord = getNumPhones(goldForms.getByID(i).getPhonologicalRepresentation());
			lexLD[i] = levenshteinDistance(outForms.getByID(i), goldForms.getByID(i));
			isHit[i] = (lexLD[i] == 0);
			numHits += (lexLD[i] == 0) ? 1 : 0; 
			numAlmostHits += (lexLD[i] <= 1) ? 1 : 0; 
			numNearHits += (lexLD[i] <= 2) ? 1 : 0; 
			double lakation = (double)lexLD[i] / (double) numPhonesInGoldWord; 
			lexLak[i] = lakation;
			totLexQuotients += lakation; 
		}
		
		double[] output = new double[4]; 
		output[0] = totLexQuotients / (double) NUM_ETYMA * 100.0; 
		output[1] = numHits / (double)NUM_ETYMA * 100.0; 
		output[2] = numAlmostHits / (double)NUM_ETYMA * 100.0; 
		output[3] = numNearHits / (double)NUM_ETYMA * 100.0;
		return output; 
	}
	
	private static void makeTrajectoryFiles()
	{
		File trajdir = new File(""+runPrefix+"\\trajectories"); 
		trajdir.mkdir(); 
	
		for( int wi =0; wi < NUM_ETYMA; wi ++) 
		{
			String filename = runPrefix + "\\trajectories\\etym"+wi+".txt"; 
			String output = "Trajectory file for run '"+runPrefix+"'; etymon number :"+wi+":\n"
				+	initLexicon.getByID(wi)+" >>> "+testResultLexicon.getByID(wi)
				+ (goldOutput ? " ( Correct : "+goldResultLexicon.getByID(wi)+") :\n"  : ":\n")
					+wordTrajectories[wi]+"\n";
			writeToFile(filename, output); 
		}
	}
	
	//TODO abrogated! 
	private static void makeAnalysisFile(String fileName, String lexicName, Lexicon lexic)
	{
		String output = "Analysis for "+lexicName+"/n";
		output += "Overall performance in average derivational distance : "+PERFORMANCE+"\n"; //TODO come up with better name for this 
		output += "Percent of derivation results that match correct forms : "+ACCURACY+"%\n"; 
		output += "Performance associated with each phone in "+lexicName+"\n"; 
		output += "Phone in "+lexicName+"\tAssociated lakation\tMean Associated Normalized Lev.Dist.\n";
		
		HashMap<Phone, Double> missLikByPhone = missLikelihoodPerPhone(lexic);
		HashMap<Phone, Double> avgAssocdLDs = avgLDForWordsWithPhone(lexic); 
		Phone[] phonInv = lexic.getPhonemicInventory(); 
		for(Phone ph : phonInv)
			output += ph.print()+"\t|\t"+missLikByPhone.get(ph)+"\t|\t"+avgAssocdLDs.get(ph)+"\n"; 
		
		writeToFile(fileName, output); 
	}
	
	//on the word level, it is divided by the number of phones in the INITIAL lexicon's version of the word! 
	//TODO abrogated! 
	private static double getLDErrorAvgdOverWordLengthInPhones()
	{
		double totLexQuotients = 0.0; 
		for(int i = 0; i < NUM_ETYMA; i++)
		{
			int numPhonesInInitWord = 0; 
			List<SequentialPhonic> initWordPhSeq  = initLexicon.getByID(i).getPhonologicalRepresentation(); 
			for(SequentialPhonic ph : initWordPhSeq)
				if(ph.getType().equals("phone"))	numPhonesInInitWord++; 
			
			totLexQuotients += (double)levenshteinDistance(testResultLexicon.getByID(i), goldResultLexicon.getByID(i))
					/	(double)numPhonesInInitWord; 
		}
		return totLexQuotients / (double)NUM_ETYMA; 
				
	}
	
	// missLocations are the indices of words that ultimately resulted in a miss between the testResult and the gold
	// outputs the scores for each phone in the word in the lexicon
	//TODO abrogated
	private static HashMap<Phone,Double> missLikelihoodPerPhone (Lexicon lexic)
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
	
	// for the lexicon of any given stage, passed as parameter, 
	// outputs hashmap where the value for each key Phone instance
	// is the average Levenshtein distance for words containing that phone 
	// normalized for length of the word
	// counted for the number of times the phone actually occurs in that word out of total phones in the word, each time. 
	//TODO abrogated!
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
			// of words with this phone
		
		for(int li = 0; li < lexSize; li++)
		{
			List<SequentialPhonic> phs = lexList[li].getPhonologicalRepresentation();
			for (SequentialPhonic ph : phs)
			{
				if(ph.getType().equals("phone"))
				{					
					totalLevenshtein[phonemeIndices.get(ph.print())] += 
							normalizedLevenshtein(testResultLexicon.getByID(li),
									goldResultLexicon.getByID(li));
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
	/**
	 * given String @param toLex
	 * @return its representation as a LexPhon containing a sequence of Phone instances
	 * TODO note we assume the phones are separated by PH_DELIM (presumably ' ') 
	 */
	private static LexPhon parseLexPhon(String toLex)
	{
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
	
	//as formulated here : https://people.cs.pitt.edu/~kirk/cs1501/Pruhs/Spring2006/assignments/editdistance/Levenshtein%20Distance.htm
	//under this definition of Levenshtein Edit Distance,
	// substitution has a cost of 1, the same as a single insertion or as a single deletion 
	private static int levenshteinDistance(LexPhon s, LexPhon t)
	{
		List<SequentialPhonic> sPhons = s.getPhonologicalRepresentation(), 
				tPhons = t.getPhonologicalRepresentation(); 
		int n = sPhons.size(), m = tPhons.size(); 
		
		String[] sPhonStrs = new String[n], tPhonStrs = new String[m];
	
		for(int i = 0; i < n; i++)	sPhonStrs[i] = sPhons.get(i).print(); 
		for(int i = 0; i < m; i++)	tPhonStrs[i] = tPhons.get(i).print(); 
		
		int[][] distMatrix = new int[n][m], costMatrix = new int[n][m]; 
	
		//first we fill it with the base costs
		for(int i = 0; i < n; i++)
		{
			for(int j = 0; j < m; j++)
			{
				if( sPhonStrs[i].equals(tPhonStrs[j]) )	costMatrix[i][j] = 0; 
				else	costMatrix[i][j] = 1; 
			}
		}
		
		//then accumulate the Levenshtein Distance across the graph toward the bottom right
		//arbitrarily, do this top-right then down row by row (could also be done up-dwon then right)
		
		for(int i = 0 ;  i < n ; i++)	distMatrix[i][0] = i ;
		for(int j = 0 ; j < m ; j++)	distMatrix[0][j] = j;
		
		for(int i=1; i < n; i++)
		{
			for(int j = 1; j < m; j++)
			{
				distMatrix[i][j] = Math.min(distMatrix[i-1][j-1]+costMatrix[i-1][j-1],
						1 + Math.min(distMatrix[i-1][j], distMatrix[i][j-1])); 
			}
		}
		
		return distMatrix[n-1][m-1]; 
	}

	//normalized by phonetic length of the gold form (t) 
	//TODO abrogated!
	private static double normalizedLevenshtein(LexPhon s, LexPhon t)
	{
		int nphons = getNumPhones(t.getPhonologicalRepresentation()); 
		return ((double)levenshteinDistance(s,t)) / (double)nphons; 
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

	//count number of actual Phones in list of SequentialPhonic objects 
	private static int getNumPhones(List<SequentialPhonic> splist)
	{
		int count = 0 ;
		for (SequentialPhonic sp :  splist)
		{
			if(sp.getType().equals("phone"))
			{
				count++; 
			}
		}
		return count; 
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
}


