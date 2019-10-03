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
	private static LexPhon[][] goldStageGoldWordlists; //outer nested indices match with those of customStageNames 
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
		
		Simulation testSimul = new Simulation (inputForms, CASCADE);

		System.out.println("Sanity check -- proper initialization and comprehension of gold and black stage initialization in Simulation class."); 
		
		int errorCount = 0, totalErrorCount = 0;
		// first -- ensure that path is not immediately considered complete by class Simulation. 
		errorCount += checkBoolean(false, testSimul.isComplete(), "Error: simulation with non empty cascade considered complete before any steps") ? 0 : 1;
		// Simulation class should not think it just hit a gold stage
		errorCount += checkBoolean(false, testSimul.justHitGoldStage(), "Error: gold stage erroneously detected at beginning of simulation.") ? 0 : 1;
		// check number of words
		errorCount += checkBoolean(true, NUM_ETYMA == testSimul.NUM_ETYMA(), "Error : number of input forms not consistent after initialization") ? 0 : 1;
		
		testSimul.setBlackStages(blackStageNames, blackStageInstants);
		testSimul.setGold(goldOutputLexicon.getWordList());
		testSimul.setGoldStages(goldStageGoldWordlists, goldStageNames, goldStageInstants);
		testSimul.setStepPrinterval(UTILS.PRINTERVAL); 
		// for debugging purposes opacity is fine. 
		
		//check these to make sure no initialization mutator function messed with them. 
		errorCount += checkBoolean(false, testSimul.isComplete(), "Error: simulation with non empty cascade considered complete before any steps") ? 0 : 1;
		// Simulation class should not think it just hit a gold stage
		errorCount += checkBoolean(false, testSimul.justHitGoldStage(), "Error: gold stage erroneously detected at beginning of simulation.") ? 0 : 1;
		// check number of stages
		errorCount += checkBoolean(true, NUM_GOLD_STAGES == testSimul.NUM_GOLD_STAGES(), "Error: inconsistent calculation of number of gold stages") ? 0 : 1;
		errorCount += checkBoolean(true, NUM_BLACK_STAGES == testSimul.NUM_BLACK_STAGES(), "Error: inconsistent calculation of number of black stages") ? 0 : 1;
				
		System.out.println("Sanity check -- input forms should be 100% correct checked against input forms."); 
		
		//ErrorAnalysis for input lexicon against... itself... should have perfect scores for everything.
		ErrorAnalysis checker = standardChecker(testSimul.getCurrentResult(), new Lexicon(inputForms)); 

		//check that average distance metrics are all 0
		errorCount += checkMetric(0.0, checker.getAvgFED(), "Error: avg FED should be 0.0 but it is %o") ? 0 : 1 ; 
		errorCount += checkMetric(0.0, checker.getAvgPED(), "Error: avg PED should be 0.0 but it is %o") ? 0 : 1 ;
		errorCount += checkMetric(1.0, checker.getAccuracy(), "Error: initial accuracy should be 1.0 but it is %o") ? 0 : 1; 
		errorCount += checkMetric(1.0, checker.getPctWithin1(), "Error: initial accuracy within 1 phone should be 1.0 but it is %o") ? 0 : 1 ; 
		errorCount += checkMetric(1.0, checker.getPctWithin2(), "Error: initial accuracy within 2 phones should be 1.0 but it is %o") ? 0 : 1 ;
		
		errorSummary(errorCount); 
		
		System.out.println("Checking after one iteration step."); 
		totalErrorCount += errorCount;
		errorCount = 0; 
		
		testSimul.iterate();
		
		System.out.println("Checking integrity of stored input forms after step."); 
		
		checker = standardChecker(testSimul.getInput(), new Lexicon(inputForms)); 

		//check that average distance metrics are all 0
		errorCount += checkMetric(0.0, checker.getAvgFED(), "Error: avg FED should be 0.0 but it is %o") ? 0 : 1 ;
		errorCount += checkMetric(0.0, checker.getAvgPED(), "Error: avg PED should be 0.0 but it is %o") ? 0 : 1 ;
		errorCount += checkMetric(1.0, checker.getAccuracy(), "Error: initial accuracy should be 1.0 but it is %o") ? 0 : 1 ; 
		errorCount += checkMetric(1.0, checker.getPctWithin1(), "Error: initial accuracy within 1 phone should be 1.0 but it is %o") ? 0 : 1 ; 
		errorCount += checkMetric(1.0, checker.getPctWithin2(), "Error: initial accuracy within 2 phones should be 1.0 but it is %o") ? 0 : 1 ;
				
		System.out.println("Check that metrics of difference between form after one step and init forms are calculated correctly."); 
		checker = standardChecker(testSimul.getCurrentResult(), testSimul.getInput()); 
		
		errorCount += checkMetric(0.925, checker.getAccuracy(), "Error: accuracy after the first step should be 0.925 but it is %o") ? 0 : 1 ; 
		errorCount += checkMetric(1.0, checker.getPctWithin1(), "Error: accuracy within 1 phone after first step should be 1.0 but it is %o") ? 0 : 1; 
		errorCount += checkMetric(1.0, checker.getPctWithin2(), "Error: accuracy within 2 phones after first step should be 1.0 but it is %o") ? 0 : 1 ; 
		errorCount += checkMetric(5.0/504.0, checker.getAvgPED(), "Error: avg PED after first step should be "+5.0/504.0+" but it is %o") ? 0 : 1 ;
		errorCount += checkMetric(3.0/520.0, checker.getAvgFED(), "Error : avg FED after first step should be "+3.0/520.0+" but it is %o") ? 0 : 1 ; 
		
		//TODO check other methods in ErrorAnalysis -- i.e. diagnostics. 
			// or do this later? 
		
		errorSummary(errorCount); 
		
		testSimul.simulateToNextStage();
		totalErrorCount += errorCount; 
		errorCount = 0; 
		
		errorCount += checkBoolean(true, testSimul.justHitGoldStage(), "Error: gold stage erroneously not detected") ? 0 : 1; 
		checker = standardChecker(testSimul.getStageResult(true, 0), testSimul.getGoldStageGold(0)); 

		errorCount += checkMetric(1.0, checker.getAccuracy(), "Error: accuracy of only %o at first gold waypoint compared to stored result lexicon at that point.") ? 0 : 1 ; 
		errorCount += aggregateErrorsCheckWordLists(goldStageGoldWordlists[0], testSimul.getCurrentResult().getWordList()); 

		//TODO check modification methods in DiachronicSimulator -- or do this later? 
				
		testSimul.simulateToNextStage();
		//TODO check going to first black
		totalErrorCount += errorCount; 
		errorCount = 0 ;
		System.out.println("Checking Waypoint 1 (black box mode)"); 
		errorCount += checkBoolean(false, testSimul.justHitGoldStage(), "Error: gold stage erroneously detected at point of a black box stage.") ? 0 : 1; 
		
		//TODO other shit here... 
		
		errorSummary(errorCount); 
		
		testSimul.simulateToNextStage();
		totalErrorCount += errorCount; 
		errorCount = 0; 
		
		// TODO checks after skipping final gold stage before the end
		System.out.println("Checking at final waypoint, a gold stage."); 
		errorCount += checkBoolean(true, testSimul.justHitGoldStage(), "Error: gold stage erroneously not detected") ? 0 : 1; 
		checker = new ErrorAnalysis(testSimul.getStageResult(true, 1), testSimul.getGoldStageGold(1), featsByIndex, 
				feats_weighted ? new FED(featsByIndex.length, FT_WTS, ID_WT) : new FED(featsByIndex.length, ID_WT));
		errorCount += checkMetric(1.0, checker.getAccuracy(), "Error: accuracy of only %o at second gold waypoint compared to stored result lexicon at that point.") ? 0 : 1 ; 
		errorCount += aggregateErrorsCheckWordLists(goldStageGoldWordlists[1], testSimul.getCurrentResult().getWordList()); 

		errorSummary(errorCount); 

		System.out.println("checking agreement of results via gold cascade with the gold lexicon."); 
		
		testSimul.simulateToEnd();
		
		totalErrorCount += errorCount; 
		errorCount = 0; 
		checker = standardChecker(testSimul.getCurrentResult(), goldOutputLexicon); 
		errorCount += checkMetric(1.0, checker.getAccuracy(), "Error: final accuracy should be 1.0 but it is %o") ? 0 : 1 ; 
		errorCount += checkMetric(1.0, checker.getPctWithin1(), "Error: final accuracy within 1 phone should be 1.0 but it is %o") ? 0 : 1; 
		errorCount += checkMetric(1.0, checker.getPctWithin2(), "Error: final accuracy within 2 phones should be 1.0 but it is %o") ? 0 : 1 ; 
		errorCount += checkMetric(0.0, checker.getAvgPED(), "Error: final avg PED should be "+0.0+" but it is %o") ? 0 : 1 ;
		errorCount += checkMetric(0.0, checker.getAvgFED(), "Error : final avg FED should be "+0.0+" but it is %o") ? 0 : 1 ; 
		errorCount += aggregateErrorsCheckWordLists(goldOutputLexicon.getWordList(), testSimul.getCurrentResult().getWordList()); 
		
		errorSummary(errorCount); 
		
		System.out.print("\nPerformance of gold cascade ...\n"
				+ UTILS.stdMetricHeader()+"\n"); 
		
		for (int gsi = 0 ; gsi < NUM_GOLD_STAGES ; gsi++)
		{
			System.out.print(goldStageNames[gsi] +"\t\t");
			
			checker = standardChecker(testSimul.getStageResult(true, gsi), testSimul.getGoldStageGold(gsi)); 
			System.out.println(UTILS.stdMetricReport(checker)); 
		}
		checker = standardChecker(testSimul.getCurrentResult(), goldOutputLexicon); 
		System.out.println(UTILS.fillSpaceToN("Output",24)+UTILS.stdMetricReport(checker)); 
		
		
		totalErrorCount += errorCount;
		errorCount = 0; 
		System.out.println("In all, there were "+totalErrorCount+" errors checking the debugging set using the debugging gold cascade\n"
				+ "Now testing cascade editing functionalities..."); 
		
		resetToWorkingCasc(theFactory); 
		testSimul.initialize(inputForms, CASCADE);
		testSimul.setBlackStages(blackStageNames, blackStageInstants);
		testSimul.setGold(goldOutputLexicon.getWordList());
		testSimul.setGoldStages(goldStageGoldWordlists, goldStageNames, goldStageInstants);
		testSimul.simulateToEnd(); 

		System.out.print("\nPerformance of baseline cascade before edits...\n"
				+ UTILS.stdMetricHeader()+"\n"); 
		
		for (int gsi = 0 ; gsi < NUM_GOLD_STAGES ; gsi++)
		{
			System.out.print(goldStageNames[gsi] +"\t\t");
			
			checker = standardChecker(testSimul.getStageResult(true, gsi), testSimul.getGoldStageGold(gsi)); 
			System.out.println(UTILS.stdMetricReport(checker)); 
		}
		checker = standardChecker(testSimul.getCurrentResult(), goldOutputLexicon); 
		System.out.println(UTILS.fillSpaceToN("Output",24)+UTILS.stdMetricReport(checker)); 
		

		
		

	}
	
	private static void extractSymbDefs()
	{
		System.out.println("Collecting symbol definitions...");
		
		featIndices = new HashMap<String, Integer>() ; 
		phoneSymbToFeatsMap = new HashMap<String, String>(); 
		
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
		int li = 1; 
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
			String[] fisides = filine.split(""+UTILS.IMPLICATION_DELIM); 
			featImplications.put(fisides[0], fisides[1].split(""+UTILS.FEAT_DELIM));
		}
		
		System.out.println("Done extracting feature implications!");	
	}
	

	public static void extractCascAndLex(SChangeFactory theFactory, String CASC_LOC)
	{
		System.out.println("Now extracting diachronic sound change rules from rules file...");
		
		List<String> rulesByTimeInstant = extractCascRulesByStep(theFactory, CASC_LOC); 
		
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
			
			if ( (""+UTILS.GOLD_STAGENAME_FLAG+UTILS.BLACK_STAGENAME_FLAG).contains(""+currRule.charAt(0)))
			{
				if ( currRule.charAt(0) == UTILS.GOLD_STAGENAME_FLAG)
				{
					goldStagesSet = true; 
					assert rli != 0: "Error: Stage set at the first line -- this is useless, redundant with the initial stage ";
					
					currRule = currRule.substring(1); 
					assert !currRule.contains(""+UTILS.GOLD_STAGENAME_FLAG): 
						"Error: stage name flag <<"+UTILS.GOLD_STAGENAME_FLAG+">> occuring in a place besides the first character in the rule line -- this is illegal: \n"+currRule; 
					assert !currRule.contains(UTILS.STAGENAME_LOC_DELIM+""):
						"Error: illegal character found in name for custom stage -- <<"+UTILS.STAGENAME_LOC_DELIM+">>";  
					goldStageNameAndLocList.add(""+currRule+UTILS.STAGENAME_LOC_DELIM+rli);
					rulesByTimeInstant.remove(rli);  
				}
				else if (currRule.charAt(0) == UTILS.BLACK_STAGENAME_FLAG)
				{
					blackStagesSet =true;
					currRule = currRule.substring(1); 
					assert !currRule.contains(UTILS.STAGENAME_LOC_DELIM+""):
						"Error: illegal character found in name for custom stage -- <<"+UTILS.STAGENAME_LOC_DELIM+">>";  
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
				System.out.print(gs+",");
			System.out.println(""); 
		}
		  
		if (NUM_BLACK_STAGES > 0)
		{
			System.out.print("Black stages:");
			for (String bs : blackStageNameAndLocList)
				System.out.print(bs+",");
			System.out.println(""); 
		}
		
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
			CASCADE.addAll(theFactory.generateSoundChangesFromRule(currRule));

			cri++; 
			
			if (cri == next_gold)
			{
				goldStageNames[gsgi] = goldStageNameAndLocList.get(gsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[0];
				goldStageInstants[gsgi] = CASCADE.size();		
				gsgi += 1;
				if ( gsgi < NUM_GOLD_STAGES)
					next_gold = Integer.parseInt(goldStageNameAndLocList.get(gsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[1]);
			}
			
			if (cri == next_black)
			{
				blackStageNames[bsgi] = blackStageNameAndLocList.get(bsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[0];
				blackStageInstants[bsgi] = CASCADE.size();
				bsgi += 1;
				if (bsgi < NUM_BLACK_STAGES)
					next_black = Integer.parseInt(blackStageNameAndLocList.get(bsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[1]);
			}
			
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
		NUM_ETYMA = lexFileLines.size(); 
		goldStageGoldWordlists = new LexPhon[NUM_GOLD_STAGES][NUM_ETYMA]; 

		String[] initStrForms = new String[NUM_ETYMA]; 
		
		String theLine =lexFileLines.get(0); 
		String firstlineproxy = ""+theLine; 
		int numCols = 1; 
		while (firstlineproxy.contains(""+UTILS.LEX_DELIM))
		{	numCols++; 
			firstlineproxy = firstlineproxy.substring(firstlineproxy.indexOf(""+UTILS.LEX_DELIM)+1); 
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

		int lfli = 0 ; //"lex file line index"
		
		while(lfli < NUM_ETYMA)
		{
			theLine = lexFileLines.get(lfli);
			
			initStrForms[lfli] = justInput ? theLine : theLine.split(""+UTILS.LEX_DELIM)[0]; 
			inputForms[lfli] = parseLexPhon(initStrForms[lfli]);
			if (!justInput)
			{	
				String[] forms = theLine.split(""+UTILS.LEX_DELIM); 
				if(NUM_GOLD_STAGES > 0)
					for (int gsi = 0 ; gsi < NUM_GOLD_STAGES ; gsi++)
						goldStageGoldWordlists[gsi][lfli] = parseLexPhon(forms[gsi+1]);
				if (goldOutput)
					goldResults[lfli] = parseLexPhon(forms[NUM_GOLD_STAGES+1]);
			}
			lfli++;
			if(lfli <NUM_ETYMA)
				assert numCols == UTILS.colCount(theLine): "ERROR: incorrect number of columns in line "+lfli;
		}		

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
				assert phoneSymbToFeatsMap.containsKey(toPhone): 
					"Error: tried to declare a phone in a word in the lexicon using an invalid symbol.\nSymbol is : '"+toPhone+"', length = "+toPhone.length(); 
				phones.add(new Phone(phoneSymbToFeatsMap.get(toPhone), featIndices, phoneSymbToFeatsMap));
			}
		}
		return new LexPhon(phones);
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
	
	private static boolean checkMetric(double correct, double observed, String errMessage)
	{
		if (correct != observed)	System.out.println(errorMessage(""+correct,""+observed,errMessage)); 
		return correct == observed; 
	}
	
	private static int aggregateErrorsCheckWordLists(LexPhon[] g, LexPhon[] obs)
	{
		assert g.length == obs.length : "Error: tried to compare word lists of different lengths.";
		
		int tot = 0; 
		for (int i = 0 ; i < g.length; i++)
			tot += checkWord(g[i], obs[i], "Reflex mismatch: %o for %c") ? 0 : 1; 
		return tot;
	}
	
	private static boolean checkWord(LexPhon correct, LexPhon observed, String errMessage)
	{
		String c = correct.print(), o = observed.print(); 
		boolean result = c.equals(o); 
		if (!result)	System.out.println(errorMessage(c,o,errMessage)); 
		return result; 
	}
	
	private static boolean checkBoolean(boolean correct, boolean observed, String errMessage)
	{
		if (correct != observed)	System.out.println(errorMessage(""+correct,""+observed,errMessage)); 
		return correct == observed; 
	}
	
	private static String errorMessage(String cor, String obs, String msg)
	{
		return msg.replace("%c", cor).replace("%o",obs); 
	}
	
	private static void errorSummary(int ec)
	{
		if (ec == 0)	System.out.println("No errors yet at this point."); 
		else	System.out.println("In all "+ec+" errors.");
	}
	
	private static ErrorAnalysis standardChecker(Lexicon res, Lexicon gold)
	{
		return new ErrorAnalysis( res, gold, featsByIndex, feats_weighted ? new FED(featsByIndex.length, FT_WTS, ID_WT) : new FED(featsByIndex.length, ID_WT));

	}
	
	private static List<String> extractCascRulesByStep(SChangeFactory fac, String CASC_FILE_LOC)
	{
		List<String> out = new ArrayList<String>(); 
		String nextRuleLine; 
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader ( 
				new FileInputStream(CASC_FILE_LOC), "UTF-8")); 
			
			while((nextRuleLine = in.readLine()) != null)
			{
				String lineWithoutComments = ""+nextRuleLine; 
				if (lineWithoutComments.contains(""+UTILS.CMT_FLAG))
						lineWithoutComments = lineWithoutComments.substring(0,
								lineWithoutComments.indexOf(""+UTILS.CMT_FLAG));
				if(!lineWithoutComments.trim().equals(""))	out.add(lineWithoutComments); 
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
		return out; 
	}
	
	public static void resetToWorkingCasc(SChangeFactory fac)
	{
		CASCADE = new ArrayList<SChange>(); 
		List<String> rulesByStep = extractCascRulesByStep(fac, DBG_WRKG_CASC); 
		int rli = 0, gsi = 0, bsi = 0; 
		int[] goldStageTempLocs = new int[NUM_GOLD_STAGES], 
			blackStageTempLocs = new int[NUM_BLACK_STAGES]; 
		
		while(rli < rulesByStep.size())
		{
			String currRule = rulesByStep.get(rli); 
			if ( (""+UTILS.GOLD_STAGENAME_FLAG+UTILS.BLACK_STAGENAME_FLAG).contains(""+currRule.charAt(0)))
			{
				if  ( currRule.charAt(0) == UTILS.GOLD_STAGENAME_FLAG)
				{
					goldStageTempLocs[gsi] = rli;
					gsi++; 
				}
				else // black
				{
					blackStageTempLocs[bsi] = rli;
					bsi++; 
				}
				rulesByStep.remove(rli);
			}
			else	rli++; 
		}
		
		int cri = 0 , gsgi = 0, bsgi = 0,
			next_gold = goldStagesSet ? goldStageTempLocs[0] : -1,
			next_black = blackStagesSet ? blackStageTempLocs[0] : -1; 
		
		for (String currRule : rulesByStep)
		{
			CASCADE.addAll(fac.generateSoundChangesFromRule(currRule)); 
			
			cri++; 
			
			if(next_gold == cri)
			{
				goldStageInstants[gsgi] = CASCADE.size(); 
				gsgi++; 
				next_gold = (gsgi == NUM_GOLD_STAGES) ? -1 : goldStageTempLocs[gsgi]; 
			}
			if(next_black == cri)
			{
				blackStageInstants[bsgi] = CASCADE.size();
				bsgi++; 
				next_black = (bsgi == NUM_BLACK_STAGES) ? -1 : blackStageTempLocs[bsgi]; 
			}
		}
	}
	
	
}
