import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File; 
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class DiachronicSimulator {
	
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
	
	//TODO perhaps abrogate these if they're permanently no longer in use. 
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
	private static String cascFileLoc; 	
	private static String lexFileLoc;
	
	private static double id_wt; 
	private static boolean DEBUG_RULE_PROCESSING, DEBUG_MODE, print_changes_each_rule, stage_pause, ignore_stages; 
	private static int num_prob_phones_displayed = 10; //the top n phones most associated with errors... 
	
	private static int goldStageInd, blackStageInd; 
	
	private static List<SChange> CASCADE;
	private static Simulation theSimulation; 
	
	private static final int PRINTERVAL = 100; 
	
	private static void extractSymbDefs()
	{
		System.out.println("Collecting symbol definitions...");
		
		featIndices = new HashMap<String, Integer>() ; 
		phoneSymbToFeatsMap = new HashMap<String, String>(); 
		
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
		goldStageNames = new String[NUM_GOLD_STAGES];
		blackStageNames = new String[NUM_BLACK_STAGES];
		goldStageInstants = new int[NUM_GOLD_STAGES]; 
		blackStageInstants = new int[NUM_BLACK_STAGES]; 
		
		// parse the rules
		CASCADE = new ArrayList<SChange>();
		
		int cri = 0, gsgi =0 , bsgi = 0, next_gold = -1, next_black = -1;
		if (goldStagesSet)	next_gold = Integer.parseInt(goldStageNameAndLocList.get(gsgi).split(""+STAGENAME_LOC_DELIM)[1]);
		if (blackStagesSet)	next_black = Integer.parseInt(blackStageNameAndLocList.get(bsgi).split(""+STAGENAME_LOC_DELIM)[1]);
		
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
			
			if(goldStagesSet)
			{
				if (cri == next_gold)
				{
					goldStageNames[gsgi] = goldStageNameAndLocList.get(gsgi).split(""+STAGENAME_LOC_DELIM)[0];
					goldStageInstants[gsgi] = CASCADE.size();		
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
					blackStageInstants[bsgi] = CASCADE.size();
					bsgi += 1;
					if (bsgi < NUM_BLACK_STAGES)
						next_black = Integer.parseInt(blackStageNameAndLocList.get(bsgi).split(""+STAGENAME_LOC_DELIM)[1]);
				}
			}
			
			cri++; 
		}
		
		System.out.println("Diachronic rules extracted. "); 
		
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
		String[] initStrForms = new String[NUM_ETYMA]; 
		
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
		
		inputForms = new LexPhon[NUM_ETYMA];
		LexPhon[] goldResults = new LexPhon[NUM_ETYMA];  
		LexPhon[][] goldForms = new LexPhon[NUM_GOLD_STAGES][NUM_ETYMA];

		int lfli = 0 ; //"lex file line index"
		
		while(lfli < NUM_ETYMA)
		{
			theLine = lexFileLines.get(lfli);
			
			initStrForms[lfli] = justInput ? theLine : theLine.split(""+LEX_DELIM)[0]; 
			inputForms[lfli] = parseLexPhon(initStrForms[lfli]);
			if (!justInput)
			{
				String[] forms = theLine.split(""+LEX_DELIM); 
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
		
		System.out.println("Lexicon extracted.");

		System.out.println("Now preparing simulation.");
		
		theSimulation = new Simulation(inputForms, CASCADE, initStrForms); 
		if (blackStagesSet)  theSimulation.setBlackStages(blackStageNames, blackStageInstants);
		if (goldOutput)	theSimulation.setGold(goldResults);
		if (goldStagesSet)	theSimulation.setGoldStages(goldForms, goldStageNames, goldStageInstants);
		theSimulation.setStepPrinterval(PRINTERVAL); 
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
				else //hit black
				{
					System.out.println("Hit black stage "+blackStageInd+": "+blackStageNames[blackStageInd]); 
					System.out.println("Error analysis at black stages is not currently supported."); //TODO make it supported...
					System.out.println("Print latest developments from last stage? Please enter 'y' or 'n'.");
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
		
		//make trajectories files.
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
		String filename = runPrefix + "_output_graph"+ OUT_GRAPH_FILE_TYPE; 
		writeToFile(filename, theSimulation.outgraph()); 
	}
	
	private static void makeRulesLog(List<SChange> theShiftsInOrder) {
		String filename = runPrefix + "_rules_log.txt"; 
		String output = "";
		for (SChange thisShift : theShiftsInOrder)
			output += ""+thisShift + (DEBUG_RULE_PROCESSING ? "| ORIG : "+thisShift.getOrig(): "") + "\n"; 
		writeToFile(filename, output); 
	}

	private static void makeDerivationFiles()
	{
		File derdir = new File(runPrefix,"derivations"); 
		derdir.mkdir(); 
	
		for( int wi =0; wi < NUM_ETYMA; wi ++) 
		{
			String filename = new File(runPrefix, new File("derivation","etym"+wi+".txt").toString()).toString(); 
			String output = "Derivation file for run '"+runPrefix+"'; etymon number :"+wi+":\n"
				+	inputForms[wi]+" >>> "+theSimulation.getCurrentForm(wi)
				+ (goldOutput ? " ( GOLD : "+goldOutputLexicon.getByID(wi)+") :\n"  : ":\n")
					+theSimulation.getDerivation(wi)+"\n";
			writeToFile(filename, output); 
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
	
	//auxiliary
	private static void writeToFile(String filename, String output)
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
	
	//TODO is this doing what its supposed to? If so rename.
	// @param (cutoff) -- rule number that the black stage must be BEFORE.
	private static void printTheseBlackStages(int first, int last, boolean prepend)
	{
		if(blackStagesSet)
			for(int bsi = first; bsi < last + 1; bsi++)
				System.out.println(bsi+": "+(prepend ? "b":"")+
					blackStageNames[bsi]+" (@rule #: "+blackStageInstants[bsi]+")");
	}

	//TODO is this doing what its supposed to? If so rename.
	private static void printTheseGoldStages(int firstToPrint, int lastToPrint)
	{
		if(goldStagesSet)
			for(int gsi = firstToPrint; gsi < lastToPrint + 1; gsi++)
				System.out.println(gsi+": "+
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
	
	//TODO fix this ! 
	// @param curr_stage : -1 if at final result point, otherwise valid index of stage in goldStage(Gold/Result)Lexica
	private static void haltMenu(int curSt, Scanner inpu, SChangeFactory fac)
	{	
		Lexicon r = theSimulation.getCurrentResult();
		Lexicon g = (curSt == -1) ? goldOutputLexicon : goldStageGoldLexica[curSt]; 
		
		ErrorAnalysis ea = new ErrorAnalysis(r, g, featsByIndex, 
				feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));

		System.out.println("Overall accuracy : "+ea.getPercentAccuracy());
		System.out.println("Accuracy within 1 phone: "+ea.getPct1off());
		System.out.println("Accuracy within 2 phone: "+ea.getPct2off());
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
							focPtLex = toyDerivation(inputForms,CASCADE.subList(0, focPtLoc)).getCurrentResult();
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
							else	focPtLex = resp.equals("In") ? theSimulation.getInput() : 
								resp.equals("Out") ? theSimulation.getCurrentResult() :
									(curSt == -1) ? goldOutputLexicon : goldStageGoldLexica[curSt];
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
							LexPhon[] wl = inputForms;
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
							else if(resp.equals("1"))	System.out.println(inputForms[theID]); 
							else 	System.out.println(""+theSimulation.getDerivation(theID));
						}
					}
					else if(resp.equals("2"))
					{
						System.out.println("etymID"+STAGE_PRINT_DELIM+"Input"+STAGE_PRINT_DELIM+"Gold");
						for (int i = 0 ; i < r.getWordList().length ; i++)
							System.out.println(""+i+STAGE_PRINT_DELIM+inputForms[i]+STAGE_PRINT_DELIM+goldOutputLexicon.getByID(i));
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
						ea.printFourColGraph(theSimulation.getInput());	
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
			else if(resp.equals("7")) //forking test for proposed changes to cascade. 
			{
				String errorMessage = "Invalid response. Please enter a valid response. Returning to forking test menu.";
				
				List<SChange> hypCASCADE = new ArrayList<SChange>(CASCADE); 
					// new cascade that we are comparing against the "baseline", @varbl CASCADE
				
				List<String[]> proposedChanges = new ArrayList<String[]>(); 
					//TODO important variable here, explanation follows
					// each indexed String[] is form [curr time step, operation details]
					// this object is *kept sorted* by current form index
						// (IMPORTANT: equivalent to iterator hci, for hypCASCADE later)
					// operation may be either deletion or insertion 
					// both relocdation and modification are handled as deletion then insertion pairs. 
					// for deletion, the second slot simply holds the string "deletion"
					// whereas for insertion, the second index holds the string form of the SChange 
						// that is inserted there in hypCASCADE. 
				
				List<String> propChNotes = new ArrayList<String>(); 
					// will be used to keep notes on changes that will be used in case they are "finalized" 
						// i.e. using automatic modification of the cascade file. 
				
				int[] RULE_IND_MAP = new int[CASCADE.size()+1], //easy access maps indices of CASCADE to those in hypCASCADE.
								// -1 -- deleted. 
						propGoldLocs = new int[NUM_GOLD_STAGES], propBlackLocs = new int[NUM_BLACK_STAGES]; 
				int originalLastMoment = CASCADE.size(); 
				
				for (int i = 0; i < CASCADE.size()+1; i++)	RULE_IND_MAP[i] = i; //initialize each.
				for (int i = 0; i < NUM_GOLD_STAGES; i++)	propGoldLocs[i] = goldStageInstants[i];
				for (int i = 0; i < NUM_BLACK_STAGES; i++)	propBlackLocs[i] = blackStageInstants[i];
				
				boolean subcont = true; 
				while(subcont) {
					
					int forkAt = -1; 
					
					String currRuleOptions = "\t\t\t'get curr rule X', to get the index of any rules containing an entered string replacing <X>.\n"
							+ "\t\t\t'get curr rule at X', to get the rule at the original index number <X>.\n" 
							+ "\t\t\t'get curr rule effect X', to get all changes from a rule by the index <X>.\n";
					
					while(forkAt == -1 && subcont) {
					
						//TODO is this confusing? 
						System.out.print("At what current rule number would you like to modify cascade? Please type the number.\n"
								+ "You may also enter:\t'quit', to return to the main menu\n"
								+ "\t\t\t'get rule X', to get the index of any rules containing an entered string replacing <X>.\n"
								+ "\t\t\t'get rule at X', to get the rule at the original index number <X>.\n" 
								+ "\t\t\t'get rule effect X', to get all changes from a rule by the index <X>.\n"
								+ "\t\t\t'get cascade', to print all rules with their original/new indices.\n"
								+ (proposedChanges.size() >= 1 ? currRuleOptions : "")
								+ "\t\t\t'get etym X', to print the index of the INPUT form etyma entered <X>.\n"
								+ "\t\t\t:'get etym at X', to get the etymon at index <X>.\n"
								+ "\t\t\t:'get etym derivation X', to get the full derivation of etymon with index <X>.\n"
								+ "\t\t\t:'get lexicon', print entire lexicon with etyma mapped to inds.\n"); 
						resp = inpu.nextLine().replace("\n",""); 
						forkAt = getValidInd(resp, CASCADE.size()) ;
						
						if (resp.equals("quit"))	subcont = false;
						else if(forkAt > -1)	subcont = true; //NOTE dummy stmt --  do nothing but continue on to next stage. 
						else if(getValidInd(resp, 99999) > -1)
							System.out.println(errorMessage+". There are only "+CASCADE.size()+1+" timesteps."); 
						else if(!resp.contains("get ") || resp.length() < 10)	System.out.println(errorMessage); 
						else if(resp.equals("get cascade"))
						{
							int ci = 0 , gsi = 0, bsi = 0,
									firstFork = proposedChanges.size() > 0 ? 
											originalLastMoment : Integer.parseInt(proposedChanges.get(0)[0]); 
							
							while ( ci < firstFork) 
							{
								if (gsi < NUM_GOLD_STAGES)
								{	if (propGoldLocs[gsi] == ci)
									{	System.out.println("Gold stage "+gsi+": "+goldStageNames[gsi]); gsi++; }}
								else if (bsi < NUM_BLACK_STAGES)
								{	if (propBlackLocs[bsi] == ci)
									{	System.out.println("Black stage "+bsi+": "+blackStageNames[bsi]); bsi++; }}
								
								System.out.println(ci+" : "+CASCADE.get(ci)); 
								ci += 1; 	
							}
							
							int pci = 0, hci = ci; 
							int nextFork = pci < proposedChanges.size() ? 
									Integer.parseInt(proposedChanges.get(pci)[0]) : originalLastMoment; 
							
							while (pci < proposedChanges.size())
							{
								assert hci == nextFork : "Error : should be at next fork moment but we are not"; 
								
								String currMod = proposedChanges.get(pci)[1]; 
								if (currMod.equals("deletion"))	
								{
									System.out.println("[DELETED RULE : "+CASCADE.get(ci).toString()); 
									ci++; 
								}
								else //insertion
								{
									System.out.println(hci+" [INSERTED] : "+hypCASCADE.get(hci));
									hci++; 
								}
								
								//then print all the rest until the next stopping point. 
								pci++; 
								nextFork = pci < proposedChanges.size() ? 
										Integer.parseInt(proposedChanges.get(pci)[0]) : originalLastMoment; 
								
								while (Math.max(ci, hci) < nextFork)
								{
									if (gsi < NUM_GOLD_STAGES)
									{	if (propGoldLocs[gsi] == hci)
										{	System.out.println("Gold stage "+gsi+": "+goldStageNames[gsi]); gsi++; }}
									else if (bsi < NUM_BLACK_STAGES)
									{	if (propBlackLocs[bsi] == hci)
										{	System.out.println("Black stage "+bsi+": "+blackStageNames[bsi]); bsi++; }}
									
									System.out.println(ci
											+(ci==hci ? "" : "->"+hci)
											+" : "+CASCADE.get(ci)); 
									ci++; hci++; 
								}	
							}
							
							//then must be at last moment. 
							System.out.println(ci
									+ (ci == hci ? "->"+hci : "")
									+ ": Last moment, after final rule and before output.") ;
						}
						else if(resp.equals("get lexicon"))
						{
							System.out.println("etymID"+STAGE_PRINT_DELIM+"Input"+STAGE_PRINT_DELIM+"Gold");
							for (int i = 0 ; i < r.getWordList().length ; i++)
								System.out.println(""+i+STAGE_PRINT_DELIM+inputForms[i]+STAGE_PRINT_DELIM+goldOutputLexicon.getByID(i));
						}
						else
						{
							int cutPoint = 9; 
							if(resp.substring(0,9).equals("get rule ")) {
								if (resp.length() >= 13)
								{	if (resp.substring(9,12).equals("at "))	cutPoint = 12;
									else if (resp.length() < 17 ? false : resp.substring(9,16).equals("effect "))
										cutPoint = 16;
								}
								String entry = resp.substring(cutPoint); 
								if (cutPoint > 9)
								{
									int theInd = getValidInd(entry, CASCADE.size());
									if(cutPoint == 12)	printRuleAt(theInd);
									else /*curPoint == 16*/	if (theInd > -1)	theSimulation.getRuleEffect(theInd); 
								}
								else
								{	boolean noMatches = true; 
									for(int ci = 0; ci < CASCADE.size(); ci++)
										if (CASCADE.get(ci).toString().contains(entry))
											System.out.println(""+ci+" : "+CASCADE.get(ci).toString());
									if(noMatches)	System.out.println("No matches found.");
								}
							}
							if(resp.substring(0,9).equals("get etym ")) {
								if (resp.length() >= 13)
								{	if (resp.substring(9,12).equals("at "))	cutPoint = 12;
									else if (resp.length() < 21 ? false : resp.substring(9,20).equals("derivation "))
										cutPoint = 20;
								}
								String entry = resp.substring(cutPoint); 
								if (cutPoint > 9)
								{
									int theInd = getValidInd(entry, NUM_ETYMA - 1); 
									if (cutPoint == 12 && theInd > -1)	System.out.println(inputForms[theInd]);
									else /* cutPoint == 20 */ if (theInd > -1)	System.out.println(theSimulation.getDerivation(theInd)); 
									else System.out.println("Error: invalid etymon index; there are only "+NUM_ETYMA+" etyma.\nReturning to forking test menu."); 
								}
								else
								{	LexPhon query = null; boolean validLexPhon = true;
									try {	query = new LexPhon(fac.parseSeqPhSeg(resp));	}
									catch (Exception e){
										System.out.println("Error: could not parse entered phone string. Returning to forking menu.");
										validLexPhon = false;
									}
									if(validLexPhon)
									{
										String inds = etymInds(inputForms, query);
										System.out.println("Ind(s) with this word as input : "+inds);  
									}
								}
							}
							else	System.out.println(errorMessage); 
						}				
					}
					
					boolean toSetBehavior = true;
					while (toSetBehavior) {
						System.out.println("Operating on rule "+forkAt+": "+hypCASCADE.get(forkAt).toString()) ;
						System.out.print("What would you like to do?\n"
								+ "0: Insertion -- insert a rule at "+forkAt+".\n"
								+ "1: Deletion -- delete the rule at "+forkAt+".\n"
								+ "2: Modification -- change the rule at "+forkAt+".\n"
								+ "3: 'Relocdation' -- move the rule at "+forkAt+" to a different moment in time.\n"
								+ "8: Go back to previous option (setting fork point, querying options available).\n"
								+ "9: Cancel entire forking test and return to main menu.\n"); 
						resp = inpu.nextLine().substring(0,1);
						
						if(!"012389".contains(resp))
							System.out.println("Invalid entry. Please enter the valid number for what you want to do."); 
						else if(resp.equals("9"))
							subcont = false; 
						else if(resp.equals("8"))
							System.out.println("Returning to prior menu.");
						else
						{
							int deleteAt = -1; 
							List<String[]> insertions = new ArrayList<String[]>(); 
							List<String> insertionNotes = new ArrayList<String>();
							String deletionNotes = ""; 
							
							if("123".contains(resp)) // all the operations that involve deletion.
							{
								deleteAt = forkAt; 
								SChange removed = hypCASCADE.remove(deleteAt); 
								
								if(resp.equals("1"))	deletionNotes = "Former rule "+deleteAt+" [ "+removed.toString()+" ] simply removed."; 
								else if (resp.equals("2")) deletionNotes = "Former rule "+deleteAt+" [ "+removed.toString()+" ]"; // will have specific modification appended later.  
								else if(resp.equals("3"))
								{
									int relocdate = -1; 
									while(relocdate == -1)
									{
										System.out.println("Enter the moment (rule index) would you like to move this rule to:");

										int candiDate = getValidInd(inpu.nextLine().replace("\n",""), hypCASCADE.size());
										if (candiDate == -1)
											System.out.println("Invalid rule index entered. There are currently "+hypCASCADE.size()+" rules."); 
										else
										{
											System.out.println("Rule before moment "+candiDate+": "
													+ (candiDate == 0 ? "BEGINNING" : hypCASCADE.get(candiDate - 1)) );
											System.out.println("Rule after moment "+candiDate+": "
													+ (candiDate == hypCASCADE.size() ? "END" : hypCASCADE.get(candiDate)) ); 
											System.out.println("Are you sure you want to move rule "+forkAt+" to here?"); 
											char conf = '0';
											while (!"yn".contains(conf+""))
											{
												System.out.println("Please enter 'y' or 'n' to confirm or not."); 
												conf = inpu.nextLine().toLowerCase().charAt(0); 
											}
											if (conf == 'y')	relocdate = candiDate; 
										}
									}
									
									// unnecessary -- handled implicitly. 
									//if ( deleteAt > relocdate ) deleteAt--;
									//else	relocdate--; 
									deletionNotes = "Former rule "+deleteAt+" [ "+removed.toString()+" ] relocdated\n\tmoved to "+relocdate; 
							
									insertions.add(new String[] {""+relocdate, removed.toString()} );
									insertionNotes.add("Moved, originally at "+deleteAt); 
									hypCASCADE.add( relocdate , removed);
								}
							}
							if ("02".contains(resp)) // all the operations that involve insertion of a NEW rule.
							{
								
								List<SChange> propShifts = null;
								String propRule = ""; 
								while(toSetBehavior) {
									System.out.println("Please enter the new rule:");
									propRule = inpu.nextLine().replace("\n", ""); 
									toSetBehavior = false;

									try
									{	propShifts = fac.generateSoundChangesFromRule(propRule); 	}
									catch (Error e)
									{
										toSetBehavior = true;
										System.out.println("Preempted error : "+e); 
										System.out.println("You entered an invalid rule, clearly. All rules must be of form A -> B / X __ Y.");
										System.out.println("Valid notations include [] () ()* ()+ {;} # @ ,");
									}
								}
																
								//actual insertions here.
								// insert "backwards" so that intended order is preserved
								while(propShifts.size() > 0)
								{
									SChange curr = propShifts.remove(propShifts.size()-1); 
									insertions.add(new String[] {  "" +(forkAt + propShifts.size() ) ,
											curr.toString() } );
									if (resp.equals("0"))
										insertionNotes.add(""); //no notes for simple insertion. 
									else	//modification
										insertionNotes.add("Part of replacement of "+deletionNotes.substring(12)); 
									hypCASCADE.add(forkAt,curr);
								}

								if(resp.equals("2"))
									deletionNotes += " modified\nto "+propRule; 
								
								
							}
							
							// data structure manipulation
							
							if(resp.equals("3")) //relocdation -- handled separately from others. 
							{
								int relocdate = Integer.parseInt(insertions.get(0)[0]); 
								boolean back = forkAt > relocdate;

								RULE_IND_MAP[forkAt] = relocdate; 
								
								for (int rimi = 0 ; rimi < RULE_IND_MAP.length; rimi++)
								{
									int curm = RULE_IND_MAP[rimi];
									if (back && rimi != forkAt)
									{	if ( curm >= relocdate && curm < forkAt )
											RULE_IND_MAP[rimi] = curm + 1 ; }
									else if (curm > forkAt && curm <= relocdate )
										RULE_IND_MAP[rimi] = curm - 1; 
								}
								
								if (NUM_GOLD_STAGES > 0)
									for (int gsi = 0 ; gsi < NUM_GOLD_STAGES; gsi++)
										if (propGoldLocs[gsi] > Math.min(relocdate, forkAt))
											if (propGoldLocs[gsi] < Math.max(relocdate, forkAt))
												propGoldLocs[gsi] = propGoldLocs[gsi] + (back?-1:1); 

								if (NUM_BLACK_STAGES > 0)
									for (int bsi = 0 ; bsi < NUM_BLACK_STAGES; bsi++)
										if (propBlackLocs[bsi] > Math.min(relocdate, forkAt))
											if (propBlackLocs[bsi] < Math.max(relocdate, forkAt))
												propBlackLocs[bsi] = propBlackLocs[bsi] + (back?-1:1); 
								
							}
							else if (resp.equals("2") && insertions.size() == 1) //single replacement modification
							{	// no change in rule ind map or prop(Gold/Black)Locs -- dummy condition.
							}
							else // simple insertion or deletion, or non one to one replacement modification
							{
								if(deleteAt != -1)
								{
									RULE_IND_MAP[deleteAt] = -1;
									for (int rimi = 0 ; rimi < RULE_IND_MAP.length; rimi++)
										if (RULE_IND_MAP[rimi] > deleteAt)
											RULE_IND_MAP[rimi] = RULE_IND_MAP[rimi] - 1; 
									
									if (NUM_GOLD_STAGES > 0)
										for (int gsi = 0 ; gsi < NUM_GOLD_STAGES; gsi++)
											if (propGoldLocs[gsi] >= deleteAt)
												propGoldLocs[gsi] -= 1; 

									if (NUM_BLACK_STAGES > 0)
										for (int bsi = 0 ; bsi < NUM_BLACK_STAGES; bsi++)
											if (propBlackLocs[bsi] >= deleteAt)
												propBlackLocs[bsi] -= 1; 
								}
								if (insertions.size() > 0)
								{
									int insertLoc = Integer.parseInt(insertions.get(0)[0]),
											increment = insertions.size(); 
									for (int rimi = 0 ; rimi < RULE_IND_MAP.length; rimi++)
										if (RULE_IND_MAP[rimi] > insertLoc)
											RULE_IND_MAP[rimi] = RULE_IND_MAP[rimi] + increment; 
									
									if (NUM_GOLD_STAGES > 0)
										for (int gsi = 0 ; gsi < NUM_GOLD_STAGES; gsi++)
											if (propGoldLocs[gsi] >= insertLoc)
												propGoldLocs[gsi] += increment; 

									if (NUM_BLACK_STAGES > 0)
										for (int bsi = 0 ; bsi < NUM_BLACK_STAGES; bsi++)
											if (propBlackLocs[bsi] >= insertLoc)
												propBlackLocs[bsi] += increment; 
								}											
							}
								
							// update proposedChanges while keeping it sorted by index of operation
								// if there is a "tie" in index, we always list deletions first, then insertions.
							 
							// only way that an insertion could come before a deletion is in relocdation
								// if we relocdate to an earlier date. 
							
							if("123".contains(resp)) //handling deletion for @varbl proposedChanges
							{
								int pcplace = proposedChanges.size(); 
								boolean foundTargSpot = false; 
								while (pcplace == 0 ? false : foundTargSpot)
								{
									
									String[] prev = proposedChanges.get(pcplace - 1); 
									int prevLoc = Integer.parseInt(prev[0]); 
									if (deleteAt < prevLoc)	foundTargSpot = true; 
									else if (deleteAt == prevLoc)
										foundTargSpot = "deletion".equals(prev[1]); 
									
									if (!foundTargSpot)
									{	
										pcplace--; 
										proposedChanges.set(pcplace, 
												new String[] { ""+(prevLoc-1) , prev[1] } );
									}
								}
								
								if (pcplace == proposedChanges.size())	
								{	proposedChanges.add(new String[] {""+deleteAt, "deletion"});
									propChNotes.add(deletionNotes);
								}
								else
								{
									proposedChanges.add(pcplace, new String[] {""+deleteAt, "deletion"});	
									propChNotes.add(pcplace, deletionNotes); 
								}
							}
							
							if("023".contains(resp))
							{
								int insertLoc = Integer.parseInt(insertions.get(0)[0]);
								int pcplace = proposedChanges.size(); //because we iterate backwards. 
								boolean foundTargSpot = false; 
								int increment = insertions.size(); 
								while (pcplace == 0 ? false : foundTargSpot)
								{
									String[] prev = proposedChanges.get(pcplace - 1); 
									int prevLoc = Integer.parseInt(prev[0]); 
									if (insertLoc <prevLoc ) foundTargSpot = true; 
									else if (insertLoc == prevLoc)	foundTargSpot = "deletion".equals(prev[1]); 
									
									if (!foundTargSpot)
									{	
										pcplace--; 
										proposedChanges.set(pcplace, 
												new String[] { ""+(prevLoc+increment) , prev[1] } );
									}
								}
								if (pcplace == proposedChanges.size())
								{
									proposedChanges.addAll(insertions); 
									propChNotes.addAll(insertionNotes); 
								}
								else	
								{
									proposedChanges.addAll(pcplace, insertions);
									propChNotes.addAll(pcplace, insertionNotes); 
								}
							}
							
							System.out.println("Would you like to make another change at this time? Enter 'y' or 'n'."); 
							char conf = inpu.nextLine().toLowerCase().charAt(0); 
							while (!"yn".contains(conf+""))
							{
								System.out.println("Please enter 'y' or 'n' to confirm or not."); 
								conf = inpu.nextLine().toLowerCase().charAt(0); 
							}
							toSetBehavior = (conf == 'y'); 
						}
					}
					
					//actual hypothesis test simulation of CASCADE and hypCASCADE... results, etc. 
					Simulation hypEmpiricized = new Simulation (theSimulation, hypCASCADE); 
					
					//TODO iteration of @varbl hypEmpiricized 
					boolean gsstops; 
					if (goldStagesSet)
					{
						System.out.println("Halt at gold stages? Enter 'y' or 'n'."); 
						char conf = inpu.nextLine().toLowerCase().charAt(0); 
						while (!"yn".contains(conf+""))
						{
							System.out.println("Please enter 'y' or 'n' to confirm stage stopping or not."); 
							conf = inpu.nextLine().toLowerCase().charAt(0); 
						}
						gsstops = (conf == 'y'); 
					}
					else	gsstops = false; 
					
					if(gsstops)
					{
						int gssi = 0; 
						while(!hypEmpiricized.isComplete())
						{
							hypEmpiricized.simulateToNextStage();
							while (!hypEmpiricized.justHitGoldStage() && !hypEmpiricized.isComplete()) //TODO check this. 
								hypEmpiricized.simulateToNextStage();
							
							
							ErrorAnalysis hsea = new ErrorAnalysis(hypEmpiricized.getCurrentResult(), goldStageGoldLexica[gssi], featsByIndex, 
									feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt)),
									bsea = new ErrorAnalysis(theSimulation.getStageResult(true, gssi), goldStageGoldLexica[gssi], featsByIndex, 
											feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));
							
							System.out.println("Hit gold stage "+gssi+": "+goldStageNames[gssi]); 
							gssi++; 
							
							double[] pctAccs = new double[] { bsea.getPercentAccuracy(), hsea.getPercentAccuracy() },
									pct1offs = new double[] { bsea.getPct1off(), hsea.getPct1off() },
									avgFEDs = new double[] { bsea.getAvgFED(), hsea.getAvgFED() };
							if (pctAccs[0] != pctAccs[1] || pct1offs[0] != pct1offs[1] || avgFEDs[0] != avgFEDs[1])
							{
								System.out.println("Overall accuracy : "+pctAccs[0]+" >>> "+pctAccs[1]);
								System.out.println("Accuracy within 1 phone: "+pct1offs[0]+" >>> "+pct1offs[1]);
								System.out.println("Accuracy within 2 phone: "+bsea.getPct2off()+" >>> "+hsea.getPct2off());
								System.out.println("Average edit distance per from gold phone: "+bsea.getAvgPED()+" >>> "+hsea.getAvgPED());
								System.out.println("Average feature edit distance from gold: "+avgFEDs[0]+" >>> "+avgFEDs[1]); 
								System.out.println("Press anything to continue."); 
								char dum = inpu.nextLine().charAt(0);
								//TODO possibly enable further user interaction here? 

							}
							else	System.out.println("No divergence yet."); 
							
						}
					}
					else	hypEmpiricized.simulateToEnd();
					
					DHSAnalysis DHScomp = new DHSAnalysis(theSimulation, hypEmpiricized, RULE_IND_MAP , proposedChanges ); 
					
					// recall -- ErrorAnalysis ea is the ErrorAnalysis object for the baseline simulation. 
					ErrorAnalysis hea = new ErrorAnalysis(hypEmpiricized.getCurrentResult(), goldOutputLexicon, featsByIndex,
							feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));
					System.out.println("Final output comparison for hypothesis simulation"); 
					DHScomp.printBasicResults();
					System.out.println("Overall accuracy : "+ea.getPercentAccuracy()+" >>> "+hea.getPercentAccuracy());
					System.out.println("Accuracy within 1 phone: "+ea.getPct1off()+" >>> "+hea.getPct1off());
					System.out.println("Accuracy within 2 phone: "+ea.getPct2off()+" >>> "+hea.getPct2off());
					System.out.println("Average edit distance per from gold phone: "+ea.getAvgPED()+" >>> "+hea.getAvgPED());
					System.out.println("Average feature edit distance from gold: "+ea.getAvgFED()+" >>> "+hea.getAvgFED());

					System.out.println("What would you like to do? Please enter the appropriate number:"); 
					char choice = 'a';
					while (choice != '9')
					{
						System.out.println(
								  "| 0 : Print parallel derivations for one word (by index) ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|\n"
								+ "| 1 : Print all etyma by index                                                        |\n"
								+ "| 2 : Automatically finalize these changes in the cascade [exits interface]           |\n"
								+ "| 3 : Automatically comment where changes should be made to assist manual editing.    |\n"
								+ "| 4 : Display results again                                                           |\n"
								+ "| 9 : Return to main menu.____________________________________________________________|\n"); 
						choice = inpu.nextLine().charAt(0);
						//TODO implement options...

						if (choice == '4')
						{	System.out.println("Final output comparison for hypothesis simulation"); 
							DHScomp.printBasicResults();
							System.out.println("Overall accuracy : "+ea.getPercentAccuracy()+" >>> "+hea.getPercentAccuracy());
							System.out.println("Accuracy within 1 phone: "+ea.getPct1off()+" >>> "+hea.getPct1off());
							System.out.println("Accuracy within 2 phone: "+ea.getPct2off()+" >>> "+hea.getPct2off());
							System.out.println("Average edit distance per from gold phone: "+ea.getAvgPED()+" >>> "+hea.getAvgPED());
							System.out.println("Average feature edit distance from gold: "+ea.getAvgFED()+" >>> "+hea.getAvgFED());
							System.out.println("Enter anything to continue.");
							char dum = inpu.nextLine().charAt(0); 
							System.out.println("\nWould you like to do anything else?"); 
						}
						else if (choice == '2' || choice == '3')
						{
							String toFileOut = ""; 
							
							
							while (toFileOut.length() == 0) {
								try 
								{
									if(choice == '2')
										System.out.println("Automatically implementing proposed changes to cascade..."); 
									else // choice == 3
										System.out.println("Placing comments to facilitate manual editing of cascade..."); 
									System.out.println("First, explanatory comments must be entered for the changes..."); 
									
									List<String> editComments = new ArrayList<String>();
									
									for (int pci = 0; pci < proposedChanges.size(); pci++) {
										String[] ipc = proposedChanges.get(pci); 
										
										
										//note: no comments are entered for the insertion part of rule modification, 
											// which, unlike simple deletion, implies a non-empty corresponding entry in propChNotes
											// in this way, the system will be able to recognize such cases due to the explanatory comment 
													//being empty
												// in all other cases, empty explanations are strictly forbidden
										String justification = ""; 
		
										if(ipc[1].equals("deletion") || propChNotes.get(0).length() == 0)
										{	
											while (justification.equals(""))
											{
												System.out.println("Please enter an explanatory comment for this change : ");
												
												if(ipc[1].equals("deletion"))
													System.out.println(propChNotes.get(0)); 
												else if (propChNotes.get(0).length() == 0)
													System.out.println("Simple insertion of "+ipc[1]); 
												justification = inpu.nextLine().replace("\n",""); 
												if (justification.equals(""))
													System.out.println("You must enter a comment to describe your change."); 
											}
											
											//now perform line breaks as appropriate for long comments...
											justification = commentJustify(justification); 
										}
										editComments.add(justification); 
									}
									
									toFileOut = modCascFileText ( proposedChanges, editComments, choice == '3'); 
								}
								catch (MidDisjunctionEditException e ) 
								{
									System.out.print(e.getMessage());
									System.out.println("Instead, we are making file with comments of cues for where and how to make these changes."); 
										//TODO in future, come up with better behavior to handle this situation... 
									choice = '3'; 
								}
							
							}
							
							String fileDest = ""; 
							while (fileDest.equals("")) {
								System.out.println("Please enter what you want to save your new cascade as:");
								fileDest = inpu.nextLine().replace("\n",""); 
								if (fileDest.equals("") || fileDest.equals(cascFileLoc))
									System.out.println("You must enter a file destination, and it must be distinct from the original cascade's location."); 
										//TODO once we have finished debugging, allow initial file's location to be used.
							}
							writeToFile(fileDest, toFileOut); 
						}
						else if (choice == '1')
						{
							System.out.println("Printing all etyma as ID#, INPUT, BASELINE RESULT, HYP SIM RESULT, GOLD RESULT");
							for (int eti = 0 ; eti < NUM_ETYMA ; eti++)
								System.out.println(""+eti+", "+inputForms[eti]+", "+theSimulation.getCurrentForm(eti)+", "
										+ hypEmpiricized.getCurrentForm(eti)+", "+goldOutputLexicon.getByID(eti)); 
							System.out.println("Enter anything to continue.");
							char dum = inpu.nextLine().charAt(0); 
							System.out.println("\nWould you like to do anything else?"); 
						}
						else if (choice == '0')
						{
							int theID = -1; 
							while(theID == -1)
							{
								System.out.println("Please enter the index of the etymon that you would like to query:"); 
								String idstr = inpu.nextLine(); 
								theID = getValidInd(idstr, NUM_ETYMA - 1) ; 
								if (theID == -1)	
									System.out.println("Error -- there are only "+NUM_ETYMA+" etyma. Returning to query menu."); 
							}
							System.out.println(DHScomp.getDifferentialDerivation(theID)); 
							System.out.println("Enter anything to continue.");
							char dum = inpu.nextLine().charAt(0); 
							System.out.println("\nWould you like to do anything else?"); 
						}
						else if (choice != '9') //must have been not one of the listed numbers. 
							System.out.println("Invalid entry, please enter one of the listed numbers:"); 
					}
					
					
				}
			}
			else if(resp.equals("9")) {
				System.out.println("Ending"); cont = false; 
			}
			else	System.out.println("Invalid response. Please enter one of the listed numbers"); 
		}
	}
	
	//makes  EA object on subset of gold/res pairs that have a specified sequence in either the gold or res as flagged by boolean second param
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
		cascFileLoc = "DiaCLEF"; 
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
	
	//TODO remake this. 
	private static Simulation toyDerivation(LexPhon[] inps, List<SChange> ruleCascade )
	{
		Simulation toy = new Simulation(inps, ruleCascade); 
		toy.simulateToEnd();
		return toy; 
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
	
	// @param skipCode -- "g2" -- i.e. "skip 2 gold stages"
	// @param aggRemTxt -- aggregrate remaining text
	// @return break point to skip those stages
	private static int brkPtForStageSkip(String aggRemTxt, String skipCode)
	{
		boolean isGold = skipCode.charAt(0) == 'g'; 
		char SN = isGold ? GOLD_STAGENAME_FLAG : BLACK_STAGENAME_FLAG; 
		int skips_left = Integer.parseInt(skipCode.substring(1));  
		if (aggRemTxt.charAt(0) == SN)	skips_left--; 
		String dummyTxt = aggRemTxt + "";
		int brkpt = 0; 
		String breaker = "\n"+SN; 
		
		while (skips_left > 0)
		{
			int nextbreak = dummyTxt.indexOf(breaker) + breaker.length(); 
			
			//then go to the end of that line, since it will just be the stage declaration
			nextbreak += dummyTxt.substring(nextbreak).indexOf("\n")+"\n".length(); 
			
			brkpt += nextbreak; 
			dummyTxt = dummyTxt.substring(nextbreak); 
			
			skips_left--; 
		}
		
		return brkpt; 
	}
	
	//auxiliary
	private static boolean isJustSpace(String line)
	{
		return line.replace(" ","").length() == 0;
	}
	
	
	private final static String HANGING_INDENT = "      "; 
	//auxiliary for use in implementing DHS changes 
	/** commentJustify
	 * @return @param ogcmt with line breaks inserted such that no line is longer than maxAutoCommentWidth,
	 * 		each line starts with CMT_FLAG,
	 * 		and lines after the first have hanging indentation
	 */
	private static String commentJustify(String ogcmt)
	{
		String[] tokens = ogcmt.split(" "); 
		String out = ""+CMT_FLAG, lineStarter = "\n"+CMT_FLAG+HANGING_INDENT; 
		int currLineWidth = 1; 
		
		for (int ti = 0 ; ti < tokens.length ; ti ++)
		{
			
			if (currLineWidth + 1 + tokens[ti].length() < maxAutoCommentWidth)
			{
				currLineWidth += 1 + tokens[ti].length(); 
				out += " " + tokens[ti];
			}
			else
			{
				out += lineStarter+tokens[ti]; 
				currLineWidth = lineStarter.length() + tokens[ti].length(); 
			}
		}
		return out; 
	}
	
	
	/** modCascFileText
	 * gets text from @global cascFileLoc
	 * @return casc text as appropriately modified. 
	 * @param propChs -- proposedChanges, of form [index, description] -- see above in comments about proposedChanges @varbl for more info
	 * @param comments -- comments to be inserted for each proposed change 
	 * 		(note that the insertion part of modification is bidirectionally indicated with the lack of such a comment)
	 * @param justPlaceHolders -- if true, we are only placing comments to indicate where user should edit cascade file
	 * 		otherwise we are actually carrying out the edits. 
	 * @return
	 */
	
	private static String modCascFileText( List<String[]> propChs, List<String> comments, boolean justPlaceHolders ) throws MidDisjunctionEditException
	{
		SChangeFactory tempFac = new SChangeFactory(phoneSymbToFeatsMap, featIndices, featImplications); 
		String STAGEFLAGS = ""+GOLD_STAGENAME_FLAG+BLACK_STAGENAME_FLAG;
		int linesPassed = 0; 
		String readIn = ""; 
		
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader ( 
				new FileInputStream(cascFileLoc), "UTF-8")); 
			String nextLine = ""; 
		
			while((nextLine = in.readLine()) != null)
				readIn += nextLine.replace("\n", "")+"\n"; 
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
		
		int igs = -1, ibs = -1; 
		int nextRuleInd = 0 ; 
		String out = ""; 
		
		//iterate over each proposed change. 
		for (int pci = 0 ; pci < propChs.size() ; pci++)
		{
			int nextChangeRuleInd = Integer.parseInt(propChs.get(pci)[0]); 
			boolean isDelet = propChs.get(pci)[1].equals("deletion"); 
			// will be used to determine where we place new content with respect to comment blocks
			String stagesToSkip = ""; 
			int prev_igs = igs , prev_ibs = ibs; 
			
			if(goldStagesSet)
				while(igs == NUM_GOLD_STAGES - 1 ? false : goldStageInstants[igs] < nextChangeRuleInd)
					igs++; 
			if(blackStagesSet)
				while(ibs == NUM_BLACK_STAGES -1 ? false : blackStageInstants[ibs] < nextChangeRuleInd)
					ibs++;

			if(igs > prev_igs || ibs > prev_ibs)
			{
				if ( (goldStagesSet ? goldStageInstants[igs] : -1)
						> (blackStagesSet ? blackStageInstants[ibs] : -1))
					stagesToSkip = "g"+(prev_igs-igs); 
				else	stagesToSkip = "b"+(prev_ibs-ibs); 
			}
			
			if(!stagesToSkip.equals(""))
			{
				int break_pt = brkPtForStageSkip(readIn, stagesToSkip); // should end in "\n", at least as of September 17 2019 
				String hop = readIn.substring(0, break_pt); 
				linesPassed += hop.split("\n").length - 1; //-1 because of the final \n 
				readIn = readIn.substring(break_pt); 
				nextRuleInd = (stagesToSkip.charAt(0) == 'g') ? goldStageInstants[igs] : blackStageInstants[ibs]; 
			}
			
			while (nextRuleInd <= nextChangeRuleInd)
			{
				// first - skip any leading blankj lines or stage declaration lines
				while (STAGEFLAGS.contains(readIn.substring(0,1)) || isJustSpace(readIn.substring(0, readIn.indexOf("\n"))))
				{
					int brkpt = readIn.indexOf("\n") + "\n".length(); 
					linesPassed ++; 
					out += readIn.substring(0, brkpt);
					readIn = readIn.substring(brkpt); 
				}
				
				String commentBlock = ""; 
				// case of if the next line is headed by the comment flag. 
				// absorb all consecutive comment lines in @varbl commentBlock
				while (readIn.charAt(0) == CMT_FLAG ) {
					int brkpt = readIn.indexOf("\n") + "\n".length(); 
					commentBlock += readIn.substring(0, brkpt); 
					readIn = readIn.substring(brkpt); 
					linesPassed++; 
				}
				

				if (!commentBlock.equals("") && isJustSpace(readIn.substring(0,readIn.indexOf("\n"))))
				{
					int brkpt = readIn.indexOf("\n") + "\n".length(); 
					commentBlock += readIn.substring(0, brkpt); 
					readIn = readIn.substring(brkpt); 
					linesPassed++; 
				}
				
				//if next line is either another blank line, another comment after blank line, 
				// or a stage, this iteration of loop is over
				if ((STAGEFLAGS + CMT_FLAG).contains(readIn.substring(0,1)) || isJustSpace(readIn.substring(0, readIn.indexOf("\n"))) )
					out += commentBlock; 
				else // i.e. we are handling a line holding a rule.
				{

					//on the other hand, if a rule comes after this block, we consider the comment block to have been
						// the explanation or justification for the rule, and will then operate on the rule.
					// if the comment block is empty, nothing explicitly differs in code, so both are handled here. 
					int brkpt = readIn.indexOf("\n"); 
					String ruleLine = readIn.substring(0, brkpt); 
					List<SChange> dummyShifts = tempFac.generateSoundChangesFromRule(ruleLine.substring(0, brkpt - "\n".length())); 
					
					assert dummyShifts.get(0).toString().equals(CASCADE.get(nextRuleInd).toString()) : 
						"Error : misalignment in saved CASCADE and its source file"; //TODO debugging likely necessary 
					readIn = readIn.substring(brkpt+"\n".length());
					
					if (nextRuleInd - 1 + dummyShifts.size() < nextChangeRuleInd) // then we can simply absorb it into @varbl out as usual.
					{
						out += commentBlock + ruleLine + "\n"; 
						nextRuleInd += dummyShifts.size(); 
						linesPassed++; 
					}
					else //perform proper file text modification behavior according to proposed change and whether we are automodification or merely commenting mode. 
					{
						//TODO we are assuming all rule indexing is correct as supplied by @param propChs
						// ... may need to check this. 
						
						if (dummyShifts.size() == 1 ) { 
							if(isDelet) 
							{	// then we add comments AFTER prior block
								out += commentBlock; 
								
								out += comments.get(pci); 
								if (!out.substring(out.length() - "\n".length()).equals("\n"))
									out += "\n"; 
								
								if (justPlaceHolders)
									out += CMT_FLAG+"AUTOGENERATED CORRECTION CUE : delete following rule.\n"
											+ ruleLine + "\n"; 
								else//  comment out the rule that we are deleting... 
									out += CMT_FLAG + ruleLine + "\n"; 
								linesPassed ++; 
								nextRuleInd++; 
							}
							else // we are dealing with an insertion then.
							{	
								// and thus comments and insertion come before next rule's preceding comment block
								out += comments.get(pci); //will do nothing if this is part of a modification. 
								if (!out.substring(out.length() - "\n".length()).equals("\n"))
									out += "\n"; 
								if (justPlaceHolders)
									out += CMT_FLAG+"AUTOGENERATED CORRECTION CUE: insert following rule to replace this comment\n"
											+CMT_FLAG;
								out += propChs.get(pci)[1] + "\n"; 
								//nextRuleInd or linesPassed do not increment since we are only inserting new content. 
							
								//restore commentBlock and ruleLine to readIn
									// because later rules may operate on them. 
								readIn = commentBlock + ruleLine + "\n" + readIn; 
								//track back on linesPassed as appropriate. 
								linesPassed -= (commentBlock+ruleLine).split("\n").length; 
							}
						}
						else //then there is a disjunction -- whether we can pass without error determined by value of justPlaceHolders
						{
							if(justPlaceHolders)
							{
								if (isDelet) {
									out += commentBlock; 
									out += comments.get(pci); 
									if (!out.substring(out.length() - "\n".length()).equals("\n"))
										out += "\n"; 
									out += CMT_FLAG+"AUTOGENERATED CORRECTION CUE: note, deletion targets one of many rules generated from "
											+ "\n"+HANGING_INDENT+CMT_FLAG+"a rule form with a {} disjucntion:\n"
											+ HANGING_INDENT+"\t"+CMT_FLAG+ruleLine+"\n"+HANGING_INDENT+CMT_FLAG
											+ "The one targeted for deletion is "+dummyShifts.get(nextChangeRuleInd- nextRuleInd)
											+ "\n"+HANGING_INDENT+CMT_FLAG+"Manually edit as is appropriate below.\n"
											+ruleLine+"\n"; 
									nextRuleInd += dummyShifts.size(); 
									linesPassed++; 
								}
								else // insertion with justPlaceHolders = true and with a disjunction in the rule form. 
								{
									// if we are inserting before the first rule generated by the disjunction, this is just like the normal case 
										// without the issues arising from the disjunction.
									if( nextRuleInd == nextChangeRuleInd) 
									{
										// and thus comments and insertion come before next rule's preceding comment block
										out += comments.get(pci); //will do nothing if this is part of a modification. 
										if (!out.substring(out.length() - "\n".length()).equals("\n"))
											out += "\n"; 
										out += CMT_FLAG+"AUTOGENERATED CORRECTION CUE: insert following rule to replace this comment\n"
													+CMT_FLAG+propChs.get(pci)[1] + "\n"; 
										
									}
									else
									{
										out += comments.get(pci); 
										if (!out.substring(out.length() - "\n".length()).equals("\n"))
											out += "\n"; 
										out += CMT_FLAG + "AUTOGENERATED CORRECTION CUE: note, an insertion of a new rule was placed here\n"
												+ HANGING_INDENT+CMT_FLAG+"between two rules that were drawn from a rule form with a {} disjunction:\n"
														+ HANGING_INDENT+"\t"+CMT_FLAG+ruleLine+"\n"
												+ HANGING_INDENT+CMT_FLAG+"Specifically between the two generated rules,\n"
														+ HANGING_INDENT+"\t"+CMT_FLAG+dummyShifts.get(nextChangeRuleInd - nextRuleInd - 1)+"\n"
																+ HANGING_INDENT+"\t\t\t"+CMT_FLAG+"and\n"
														+ HANGING_INDENT+"\t"+CMT_FLAG+dummyShifts.get(nextChangeRuleInd - nextRuleInd)+"\n"
														+CMT_FLAG+"AUTOGENERATED CORRECTION CUE: insert following rule to replace this comment\n"
														+CMT_FLAG+propChs.get(pci)[1]+"\n"; 
									}

									//nextRuleInd or linesPassed do not increment since we are only inserting new content. 
								
									//restore commentBlock and ruleLine to readIn
										// because later rules may operate on them. 
									readIn = commentBlock + ruleLine + "\n" + readIn; 
									//track back on linesPassed as appropriate. 
									linesPassed -= (commentBlock+ruleLine).split("\n").length; 
								}
									
							}
							else
							{
								String errorMessage = "MidDisjunctionException : Currently, you cannot ";
								//TODO check for proper error message generation here .... 
								
								if (isDelet)
									errorMessage += "delete only one";
								else	errorMessage += "insert a rule between two"; 
								errorMessage += " of the sound changes derived from rule written in the original cascade file "
											+ "with a {} disjunction in its context stipulations.\n"
											+ "The disjunct context rule in question was "+ruleLine+"\n"
											+ "It is on line "+linesPassed+" of the original cascade file, "+cascFileLoc+"\n";
								
								if (isDelet) errorMessage +=  "You tried to delete this derived rule : "+dummyShifts.get(nextChangeRuleInd - nextRuleInd);
								else	errorMessage += "You tried to insert this rule : "+propChs.get(pci)[1]+"\n"
										+ "between this derived rule : "+dummyShifts.get(nextChangeRuleInd - nextRuleInd - 1)+"\n"
												+ "and this one : "+dummyShifts.get(nextChangeRuleInd - nextRuleInd); 
								
								throw new MidDisjunctionEditException(errorMessage+ "\nInstead, you should manually make this change at the specified line number yourself."); 
							}
						}
					}
				}
			}
			
		}

		//TODO make sure this final append behavior is carried out correctly.
		return out + readIn; 
		
	}
}


