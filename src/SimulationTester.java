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

	//finals
	private static final String DBG_WRKG_CASC = "DebugWorkingCasc",
			DBG_GOLD_CASC = "DebugGoldCasc" , DBG_START_CASC = "DebugStartCasc"; 
	private static final String SYMBS_LOC = "symbolDefs.csv";
	private static final String LEX_LOC = "DebugDummyLexicon.txt"; 
	private static final String FI_LOC = "FeatImplications"; 
	private static final double ID_WT = 0.5; 
	
	//constant once set.
	private static SChangeFactory theFactory; 
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
	
	//track
	private static int[] goldStageInstants, blackStageInstants; // i.e. the index of custom stages in the ordered rule set
	private static List<SChange> CASCADE;
	
	private static String[] STAGE_ORDER; //ordering of stages. 
	
	public static void main(String args[]) throws MidDisjunctionEditException
	{
		initWorkingCascFile(); 

		extractSymbDefs(); 
		extractFeatImpls();
		
		System.out.println("Creating SChangeFactory...");
		theFactory = new SChangeFactory(phoneSymbToFeatsMap, featIndices, featImplications); 
		
		STAGE_ORDER = UTILS.extractStageOrder(DBG_START_CASC); 
		extractCascAndLex(theFactory, DBG_GOLD_CASC); 
			// first extracting from gold casc so that we do initial sanity test of the correct cascade leading to correct forms
				// with all metrics agreeing with this etc etc. 
		
		System.out.println("Lexicon extracted. Now debugging.");
		
		Simulation testSimul = new Simulation (inputForms, CASCADE, STAGE_ORDER);

		System.out.println("Sanity check -- proper initialization and comprehension of gold and black stage initialization in Simulation class."); 
		
		int errorCount = 0, totalErrorCount = 0;
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dStrArrs(testSimul.getStagesOrdered(), new String[] {"g0","b0","g1"} ), 
				"ERROR: Simulation.stagesOrdered not constructed properly.") ;
		// first -- ensure that path is not immediately considered complete by class Simulation. 
		
		errorCount += chBoolPrIncIfError(getLineNumber(), false, testSimul.isComplete(), "ERROR: simulation with non empty cascade considered complete before any steps");
		// Simulation class should not think it just hit a gold stage
		errorCount += chBoolPrIncIfError(getLineNumber(), false, testSimul.justHitGoldStage(), "ERROR: gold stage erroneously detected at beginning of simulation.");
		// check number of words
		errorCount += chBoolPrIncIfError(getLineNumber(), true, NUM_ETYMA == testSimul.NUM_ETYMA(), "ERROR: number of input forms not consistent after initialization");
		
		testSimul.setBlackStages(blackStageNames, blackStageInstants);
		testSimul.setGold(goldOutputLexicon.getWordList());
		testSimul.setGoldStages(goldStageGoldWordlists, goldStageNames, goldStageInstants);
		testSimul.setStepPrinterval(UTILS.PRINTERVAL); 
		// for debugging purposes opacity is fine. 
		
		//check these to make sure no initialization mutator function messed with them. 
		errorCount += chBoolPrIncIfError(getLineNumber(), false, testSimul.isComplete(), "ERROR: simulation with non empty cascade considered complete before any steps");
		// Simulation class should not think it just hit a gold stage
		errorCount += chBoolPrIncIfError(getLineNumber(), false, testSimul.justHitGoldStage(), "ERROR: gold stage erroneously detected at beginning of simulation.");
		// check number of stages
		errorCount += chBoolPrIncIfError(getLineNumber(), true, NUM_GOLD_STAGES == testSimul.NUM_GOLD_STAGES(), "ERROR: inconsistent calculation of number of gold stages");
		errorCount += chBoolPrIncIfError(getLineNumber(), true, NUM_BLACK_STAGES == testSimul.NUM_BLACK_STAGES(), "ERROR: inconsistent calculation of number of black stages");
				
		System.out.println("Sanity check -- input forms should be 100% correct checked against input forms."); 
		
		//ErrorAnalysis for input lexicon against... itself... should have perfect scores for everything.
		ErrorAnalysis checker = standardChecker(testSimul.getCurrentResult(), new Lexicon(inputForms)); 

		//check that average distance metrics are all 0
		errorCount +=UTILS.checkMetric(0.0, checker.getAvgFED(), "ERROR: avg FED should be 0.0 but it is %o") ? 0 : 1 ; 
		errorCount +=UTILS.checkMetric(0.0, checker.getAvgPED(), "ERROR: avg PED should be 0.0 but it is %o") ? 0 : 1 ;
		errorCount +=UTILS.checkMetric(1.0, checker.getAccuracy(), "ERROR: initial accuracy should be 1.0 but it is %o") ? 0 : 1;
		errorCount +=UTILS.checkMetric(1.0, checker.getPctWithin1(), "ERROR: initial accuracy within 1 phone should be 1.0 but it is %o") ? 0 : 1 ; 
		errorCount +=UTILS.checkMetric(1.0, checker.getPctWithin2(), "ERROR: initial accuracy within 2 phones should be 1.0 but it is %o") ? 0 : 1 ;
		
		UTILS.errorSummary(errorCount); 
		
		System.out.println("Checking after one iteration step."); 
		totalErrorCount += errorCount;
		errorCount = 0; 
		
		testSimul.iterate();
		
		System.out.println("Checking integrity of stored input forms after step."); 
		
		checker = standardChecker(testSimul.getInput(), new Lexicon(inputForms)); 

		//check that average distance metrics are all 0
		errorCount +=UTILS.checkMetric(0.0, checker.getAvgFED(), "ERROR: avg FED should be 0.0 but it is %o") ? 0 : 1 ;
		errorCount +=UTILS.checkMetric(0.0, checker.getAvgPED(), "ERROR: avg PED should be 0.0 but it is %o") ? 0 : 1 ;
		errorCount +=UTILS.checkMetric(1.0, checker.getAccuracy(), "ERROR: initial accuracy should be 1.0 but it is %o") ? 0 : 1 ; 
		errorCount +=UTILS.checkMetric(1.0, checker.getPctWithin1(), "ERROR: initial accuracy within 1 phone should be 1.0 but it is %o") ? 0 : 1 ; 
		errorCount +=UTILS.checkMetric(1.0, checker.getPctWithin2(), "ERROR: initial accuracy within 2 phones should be 1.0 but it is %o") ? 0 : 1 ;
				
		System.out.println("Check that metrics of difference between form after one step and init forms are calculated correctly."); 
		checker = standardChecker(testSimul.getCurrentResult(), testSimul.getInput()); 
		
		errorCount +=UTILS.checkMetric(0.925, checker.getAccuracy(), "ERROR: accuracy after the first step should be 0.925 but it is %o") ? 0 : 1 ; 
		errorCount +=UTILS.checkMetric(1.0, checker.getPctWithin1(), "ERROR: accuracy within 1 phone after first step should be 1.0 but it is %o") ? 0 : 1; 
		errorCount +=UTILS.checkMetric(1.0, checker.getPctWithin2(), "ERROR: accuracy within 2 phones after first step should be 1.0 but it is %o") ? 0 : 1 ; 
		errorCount +=UTILS.checkMetric(5.0/504.0, checker.getAvgPED(), "ERROR: avg PED after first step should be "+5.0/504.0+" but it is %o") ? 0 : 1 ;
		errorCount +=UTILS.checkMetric(3.0/520.0, checker.getAvgFED(), "ERROR: avg FED after first step should be "+3.0/520.0+" but it is %o") ? 0 : 1 ; 
		
		//TODO check other methods in ErrorAnalysis -- i.e. diagnostics. 
			// or do this later? 
			// TODO make sure this was done... (Dec 2022) 
		UTILS.errorSummary(errorCount); 
		
		testSimul.simulateToNextStage();
		totalErrorCount += errorCount; 
		errorCount = 0; 
		
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, testSimul.justHitGoldStage(), "ERROR: gold stage erroneously not detected"); 
		checker = standardChecker(testSimul.getStageResult(true, 0), testSimul.getGoldStageGold(0)); 

		errorCount +=UTILS.checkMetric(1.0, checker.getAccuracy(), "ERROR: accuracy of only %o at first gold waypoint compared to stored result lexicon at that point.") ? 0 : 1 ; 
		errorCount +=UTILS.aggregateErrorsCheckWordLists(goldStageGoldWordlists[0], testSimul.getCurrentResult().getWordList()); 

		//TODO check modification methods in DiachronicSimulator -- or do this later? 
				//TODO make sure this was/is done. (Dec 2022) 
		testSimul.simulateToNextStage();
		//TODO check going to first black // TODO check this was/is done (Dec 2022) 
		totalErrorCount += errorCount; 
		errorCount = 0 ;
		System.out.println("Checking Waypoint 1 (black box mode)"); 
		errorCount +=chBoolPrIncIfError(getLineNumber(), false, testSimul.justHitGoldStage(), "ERROR: gold stage erroneously detected at point of a black box stage."); 
		
		UTILS.errorSummary(errorCount); 
		
		testSimul.simulateToNextStage();
		totalErrorCount += errorCount; 
		errorCount = 0; 
		
		// TODO checks after skipping final gold stage before the end
			//TODO check this was/is implemented (Dec 2022) 
		System.out.println("Checking at final waypoint, a gold stage."); 
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, testSimul.justHitGoldStage(), "ERROR: gold stage erroneously not detected"); 
		checker = new ErrorAnalysis(testSimul.getStageResult(true, 1), testSimul.getGoldStageGold(1), featsByIndex, 
				feats_weighted ? new FED(featsByIndex.length, FT_WTS, ID_WT) : new FED(featsByIndex.length, ID_WT));
		errorCount +=UTILS.checkMetric(1.0, checker.getAccuracy(), "ERROR: accuracy of only %o at second gold waypoint compared to stored result lexicon at that point.") ? 0 : 1 ; 
		errorCount +=UTILS.aggregateErrorsCheckWordLists(goldStageGoldWordlists[1], testSimul.getCurrentResult().getWordList()); 

		UTILS.errorSummary(errorCount); 

		System.out.println("checking agreement of results via gold cascade with the gold lexicon."); 
		
		testSimul.simulateToEnd();
		
		totalErrorCount += errorCount; 
		errorCount = 0; 
		checker = standardChecker(testSimul.getCurrentResult(), goldOutputLexicon); 
		errorCount += UTILS.checkMetric(1.0, checker.getAccuracy(), "ERROR: final accuracy should be 1.0 but it is %o") ? 0 : 1 ; 
		errorCount += UTILS.checkMetric(1.0, checker.getPctWithin1(), "ERROR: final accuracy within 1 phone should be 1.0 but it is %o") ? 0 : 1; 
		errorCount += UTILS.checkMetric(1.0, checker.getPctWithin2(), "ERROR: final accuracy within 2 phones should be 1.0 but it is %o") ? 0 : 1 ; 
		errorCount += UTILS.checkMetric(0.0, checker.getAvgPED(), "ERROR: final avg PED should be "+0.0+" but it is %o") ? 0 : 1 ;
		errorCount += UTILS.checkMetric(0.0, checker.getAvgFED(), "ERROR: final avg FED should be "+0.0+" but it is %o") ? 0 : 1 ; 
		errorCount += UTILS.aggregateErrorsCheckWordLists(goldOutputLexicon.getWordList(), testSimul.getCurrentResult().getWordList()); 
		
		UTILS.errorSummary(errorCount); 
		
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
		
		initWorkingCascFile(); 
		resetToWorkingCasc(theFactory); 
		testSimul = new Simulation(inputForms, CASCADE, STAGE_ORDER); 
		testSimul.setBlackStages(blackStageNames, blackStageInstants);
		testSimul.setGold(goldOutputLexicon.getWordList());
		testSimul.setGoldStages(goldStageGoldWordlists, goldStageNames, goldStageInstants);
		testSimul.setStepPrinterval(UTILS.PRINTERVAL); //reinserted. Necessary? Not sure.  
		testSimul.simulateToEnd(); 

		String bittenCorrectBaselineDeriv = "/bˈɪtən/\n" + 
				"#bˈɪɾən# | 0 : [+cor,-delrel] > ɾ / [-cons] __ [-stres]\n" + 
				"#bˈɪɾə̃n# | 1 : [-cons] > [+nas,+son,0delrel] / __ n\n" + 
				"Waypoint 1 Gold stage form : #bˈɪɾə̃n#\n" + 
				"Waypoint 2 Black stage form : #bˈɪɾə̃n#\n" + 
				"Waypoint 3 Gold stage form : #bˈɪɾə̃n#\n" + 
				"Final form : #bˈɪɾə̃n#";
		String observedDerBitten = testSimul.getDerivation(0); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, bittenCorrectBaselineDeriv.equals(observedDerBitten), "ERROR: baseline derivation for 'bitten' not matched."
				+ "correct:\n"+bittenCorrectBaselineDeriv+"\nobserved:\n"+observedDerBitten) ;
		
		System.out.print("Performance of baseline cascade before edits...\n"
				+ UTILS.stdMetricHeader()+"\n"); 
		
		for (int gsi = 0 ; gsi < NUM_GOLD_STAGES ; gsi++)
		{
			System.out.print(goldStageNames[gsi] +"\t\t");
			
			checker = standardChecker(testSimul.getStageResult(true, gsi), testSimul.getGoldStageGold(gsi)); 
			System.out.println(UTILS.stdMetricReport(checker)); 
		}
		checker = standardChecker(testSimul.getCurrentResult(), goldOutputLexicon); 
		System.out.println(UTILS.fillSpaceToN("Output",24)+UTILS.stdMetricReport(checker)); 
		
		DHSWrapper DHSW = newDHS(testSimul); 
		errorCount = totalErrorCount = 0; 
		System.out.println("----------------\n\nFirst test (line "+getLineNumber()+"): insertion of l-darkening rule at the beginning of the cascade\n"
				+ "\tThis should increase accuracy at each gold stage and the output by 0.075\n---------\n"); 
		
		String nextLaw = "l > lˠ / __ [+cons]"; 
		String nextCmt = "L-darkening as evidenced by mˈowlˠɾəd, bɨhˈowlˠɾə̃n, mˈowlˠʔə̃n"; 
		
		DHSW.processSingleCh(-1, "", 0, nextLaw, 
				theFactory.generateSoundChangesFromRule(nextLaw), nextCmt);
		
		List<SChange> curHC = DHSW.getHypCASC(),
				dumCasc = new ArrayList<SChange>(CASCADE);
		
		dumCasc.addAll(0, theFactory.generateSoundChangesFromRule(nextLaw)); 

		//test DHSWrapper.hypCASC (and ~.baseCASC)  
		errorCount +=chBoolPrIncIfError(getLineNumber(), true , curHC.get(0).toString().equals(nextLaw), "ERROR: first instance does not have the correct rule."); 
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, UTILS.compareCascades(curHC.subList(1, curHC.size()), DHSW.getBaseCASC()),
				"ERROR: 2nd rule onward for hypCASC should be equal to baseCASC, but apparently it is not.");
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, UTILS.compareCascades(curHC.subList(1, curHC.size()), CASCADE), 
				"ERROR: 2nd rule onward for hypCASC should be equal to SimulationTester.CASCADE, but apparently it is not.");
		
		//test DHSWrapper's rule ind maps.
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 3  == DHSW.getBaseHypRuleIndMap()[2], "ERROR: increment not realized in baseHypRuleIndMap properly."); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 1 == DHSW.getHypBaseRuleIndMap()[2] , "ERROR: increment not realized in hypBaseRuleIndMap properly.\n"
				+ "Correct: "+UTILS.print1dIntArr(new int[] { -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})+"\n"
						+ "Observed: "+UTILS.print1dIntArr(DHSW.getHypBaseRuleIndMap())); 
		
		//test DHSWrapper.hypGoldLocs
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, 6 == DHSW.getHypGoldLocs()[0], "ERROR: increment on hypGoldLocs not done correctly"); 
		
		//test DHSWrapper.proposedChanges
		String[] thepc = DHSW.getProposedChanges().get(0); 
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, "0".equals(thepc[0]) && nextLaw.equals(thepc[1]) && nextCmt.equals(thepc[2]),  
				"ERROR: update on proposedChanges for simple insertion not carried out properly"); 
		
		//test RIM_BH and RIM_HB
		int[] corrBhRIM = new int[] {1,2,3,4,5,6,7,8,9,10,11}, corrHbRIM = new int[] {-1,0,1,2,3,4,5,6,7,8,9,10},
				obsBhRIM = DHSW.getBaseHypRuleIndMap(), obsHbRIM = DHSW.getHypBaseRuleIndMap();
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare1dIntArrs(corrBhRIM, obsBhRIM),
				"ERROR: RIM_BH should be "+UTILS.print1dIntArr(corrBhRIM)+"\nbut it is : "+UTILS.print1dIntArr(obsBhRIM)); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true,
				UTILS.compare1dIntArrs(corrHbRIM, obsHbRIM),
				"ERROR: RIM_HB should be "+UTILS.print1dIntArr(corrHbRIM)+"\nbut it is : "+UTILS.print1dIntArr(obsHbRIM)); 
		
		DifferentialHypothesisSimulator theDHS = DHSW.generateDHS(); 
		
		//checking DHS.ruleCorrespondences
		int[][] corrRC = new int[][] { {-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10} }; 
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare2dIntArrs(theDHS.getRuleCorrespondences(), corrRC),
				"ERROR: DifferentialHypothesisSimulator.ruleCorrespondences appears to have been malformed.\n"
						+ "Correct :\n"+UTILS.print1dIntArr(corrRC[0])+"\n"+UTILS.print1dIntArr(corrRC[1])+
						"\nObserved : \n"+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[0])+"\n"+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[1])); 
					
		//test DifferentialHypothesisSimulator.baseRuleIndsToGlobal and ~.hypRuleIndsToGlobal
		int[] btg = theDHS.getBaseIndsToGlobal(), htg = theDHS.getHypIndsToGlobal(); 
		
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, btg.length == 10 , "ERROR: base to global ind mapper has wrong dimensions"); 
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, htg.length == 11 , "ERROR: hyp to global ind mapper has wrong dimensions"); 
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, btg[0] == 1 && btg[2] == 3, "ERROR: base to global ind mapper is malformed"); 
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, htg[0] == 0 && htg[2] == 2, "ERROR: hyp to global ind mapper is malformed"); 
		
		//test DifferentialHypothesisSimulator.divergencePoint 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDivergencePoint() == 0, 
				"ERROR: divergence point should be 0 but it is "+theDHS.getDivergencePoint()); 
		
		// testing lexical effects. 'bitten' should not be effected, 'molted' should.
		String[] bcdlines = bittenCorrectBaselineDeriv.split("\n"); 
		
		int colloc = bcdlines[1].indexOf(":"); 
		bcdlines[1] = bcdlines[1].substring(0, colloc-2) + "1"+bcdlines[1].substring(colloc-1); 
		colloc = bcdlines[2].indexOf(":");
		bcdlines[2] = bcdlines[2].substring(0, colloc-2) + "2"+bcdlines[2].substring(colloc-1); 
		String bittenCorrDerivAfterCh1 = String.join("\n", bcdlines); 
		
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, theDHS.hypCascSim.getDerivation(0).equals(bittenCorrDerivAfterCh1),
				"ERROR: malformed derivation of 'bitten' for hypothesis cascade after 1 change");  
		
		String mhdCor = 
				"/mˈowltəd/\n" + 
				"#mˈowlˠtəd# | 0 : l > lˠ / __ [+cons]\n" + 
				"Waypoint 1 Gold stage form : #mˈowlˠtəd#\n" + 
				"#mˈowlˠʔəd# | 6 : t > ʔ / __ ə\n" + 
				"Waypoint 2 Black stage form : #mˈowlˠʔəd#\n" + 
				"Waypoint 3 Gold stage form : #mˈowlˠʔəd#\n" + 
				"Final form : #mˈowlˠʔəd#";

		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.hypCascSim.getDerivation(26).equals(mhdCor), 
				"ERROR: malformed derivation of 'molted' for hypothesis cascade after 1 change");  
		
		String mbdGlobCor = "/mˈowltəd/\n" + 
				"Waypoint 1 Gold stage form : #mˈowltəd#\n" + 
				"#mˈowlʔəd# | 6 : t > ʔ / __ ə\n" + 
				"Waypoint 2 Black stage form : #mˈowlʔəd#\n" + 
				"Waypoint 3 Gold stage form : #mˈowlʔəd#\n" + 
				"Final form : #mˈowlʔəd#";
		//checking globalization of derivation
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, theDHS.getGlobalizedDerivation(0 , false).equals(bittenCorrectBaselineDeriv.replace("1 :","2 :" ).replace("0 :", "1 :")),
				"ERROR: malformation of globalized derivation in baseline for 'bitten'"); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getGlobalizedDerivation(26 , false).equals(mbdGlobCor), 
				"ERROR: malformation of globalized derivation in baseline for 'molted'"); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getGlobalizedDerivation(26 , true).equals(mhdCor), 
				"ERROR: malformation of proposed hypothesis' predicted derivation in baseline for 'molted'"); 
	
		//checking DHS.locHasPrCh
		boolean[] corrPCLs = new boolean[11];
		corrPCLs[0] = true; 
		errorCount += chBoolPrIncIfError(getLineNumber(),  true, UTILS.compare1dBoolArrs(corrPCLs, theDHS.getPrChLocs()), 
				"ERROR: DifferentialHypothesisSimulator.prChLocs is malformed\n"
				+ "Correct : "+UTILS.print1dBoolArrAsIntArr(corrPCLs)+"\nObserved : "
						+ UTILS.print1dBoolArrAsIntArr(theDHS.getPrChLocs())); 

		//first check syntax of differential derivations, before separately checking DHS.changedDerivations 
		//ensure that words with no difference should have a differential derivation of "". 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(0).equals(""),
				"ERROR: differential derivation for unaffected lexeme 'bitten' should be an empty string, but it is:\n"
				+ theDHS.getDifferentialDerivation(0)); 
		//now check syntax of differential derivation of a word that was indeed changed. 
		String corDD = "/mˈowltəd/\n" 
				+ "CONCORDANT UNTIL RULE : 0\n"
				+ "0[-1|0] : fed or inserted | #mˈowltəd# > #mˈowlˠtəd#\n"
				+ "Waypoint 1 Gold : #mˈowltəd# | #mˈowlˠtəd#\n"
				+ "6[5|6] : #mˈowltəd# > #mˈowlʔəd# | #mˈowlˠtəd# > #mˈowlˠʔəd#\n"
				+ "Waypoint 2 Black : #mˈowlʔəd# | #mˈowlˠʔəd#\n"
				+ "Waypoint 3 Gold : #mˈowlʔəd# | #mˈowlˠʔəd#\n"
				+ "Final forms : #mˈowlʔəd# | #mˈowlˠʔəd#";
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(26).equals(corDD), 
				"ERROR: differential derivation for 'molted' is malformed"); 
		
		//checking DHS.changedDerivations
			// we don't need to check the exact syntax since we have effectively already done that above.
			// instead we only need to check the specific keys.
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare1dIntArrs(theDHS.getEtsWithChangedDerivations(), new int[] {15, 26, 28}), 
				"ERROR: wrong etyma effected by l darkening change..."); 
		
		//now finally checking DHS.changedRuleEffects
			// for this rule there is no feeding or bleeding, so the HashMap DHS.changedRuleEffects should have only one key, 0. 
			// and it should contain three specific feedings, for molten, molded and beholden (et ids 15,26,28)
		HashMap<Integer,String[][]> CREs = theDHS.getChangedRuleEffects(); 
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				CREs.keySet().size() == 1 && CREs.containsKey(0),
				"ERROR: incorrect comprehension effects of insertion of l-darkening rule ");
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true,
				UTILS.numFilled(theDHS.getEffectsBlocked(0)) == 0,
				"ERROR: false positive detection of blocking effects of l-darkening when there are none."); 
		
		String[] darkened = new String[40]; 
		darkened[15] = "#mˈowltən# > #mˈowlˠtən#"; 
		darkened[26] = "#mˈowltəd# > #mˈowlˠtəd#";
		darkened[28] = "#bihˈowldən# > #bihˈowlˠdən#"; 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dStrArrs(CREs.get(0)[1], darkened),
				"ERROR: incorrect comprehension of effects by caused by the insertion of l-darkening");
				
		DHSW.setHypOutLoc(DBG_WRKG_CASC);
		DHSW.acceptHypothesis(false); 

		UTILS.errorSummary(errorCount);
		totalErrorCount += errorCount;
		errorCount = 0;
		
		//i.e. rebasing and usurpation occurs -- now, just this one time, we need to test that all major variables are intact.
		System.out.println("Now testing integrity of DHSW after usurpation of baseline for hypothesis acceptance."); 
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compareCascades(DHSW.getBaseCASC(), curHC),
				"ERROR: hypothesis acceptance did not usurp the baseline correctly.");
		CASCADE = curHC; 
		
		int[] bhRIM = new int[curHC.size()+1];
		curHC = null; dumCasc = null;
		
		for (int rimi = 0 ; rimi < bhRIM.length; rimi++)	bhRIM[rimi] = rimi;
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare1dIntArrs(DHSW.getBaseHypRuleIndMap(), bhRIM),
				"ERROR: hypothesis acceptance did not reinitialize base to hyp rule in correctly"); 
		
		//at this point the base and hyp rule maps should still be identical... 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare1dIntArrs(DHSW.getHypBaseRuleIndMap(), bhRIM),
				"ERROR: hypothesis acceptance did not reinitialize hyp to base rule in correctly"); 
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, DHSW.getProposedChanges().size() == 0,
				"ERROR: hypothesis acceptance did not reinitialize proposedChanges correctly"); 
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compareFiles("DebugCheckerCascAfterDarkening",DBG_WRKG_CASC),
				"ERROR: modification of DBG_WRKG_CASC not carried out properly during hypothesis acceptance.");
		
		UTILS.errorSummary(errorCount);
		totalErrorCount += errorCount; 
		errorCount = 0; 
		
		//now we will do two changes before accepting the hypothesis. 
		System.out.println("-----------------\nSecond (at line "+getLineNumber()+"): Testing comprehension of simple deletion (in this case, of a derhotacization rule).");
		
		System.out.println("Deleting derhotacization rule at index 7.\n----------------\n");

		DHSW.processSingleCh(7,"we're Yankees", -1, "", null, "");
		curHC = DHSW.getHypCASC(); dumCasc = new ArrayList<SChange>(CASCADE); 

		//testing realization in the cascade structures. 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compareCascades(dumCasc, DHSW.getBaseCASC()),
			"ERROR: base cascade appears to have been corrupted during comprehension of a deletion operation."); 
			
		dumCasc.remove(7); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compareCascades(dumCasc, curHC),
			"ERROR: malformed comprehension of simple deletion operation."); 
		
		//testing base to hyp rule ind map in DHSWrapper 
			// -- before this operation there were 11 rules, and we are deleting the 8th. 
		corrBhRIM = new int[] {0, 1, 2, 3, 4, 5, 6, -1, 7, 8, 9, 10} ;
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(corrBhRIM, DHSW.getBaseHypRuleIndMap()),
			"ERROR: Handling of simple deletion in base-hyp rule ind map not realized correctly."); 
		
		// and the same for hyp to base
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare1dIntArrs(DHSW.getHypBaseRuleIndMap(), new int[] {0, 1, 2, 3, 4, 5, 6, 8, 9, 10, 11}),
				"ERROR: Handling of simple deletion in hyp-base rule ind map not realized correctly."); 
		
		//test DHSWrapper.hypGoldLocs -- since hypBlackLocs is updated the same way so it is implicitly also being checked.
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(new int[]{6,7}, DHSW.getHypGoldLocs()), 
			"ERROR: simple deletion not handled by correct update in DHSW.hypGoldLocs -- should have changed second gold stage from spot 8 to 7."); 
			
		//test DHSWrapper.proposedChanges
		thepc = DHSW.getProposedChanges().get(0); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, "7".equals(thepc[0]) && "deletion".equals(thepc[1]) && "we're Yankees".equals(thepc[2]),
			"ERROR: update on proposedChanges for simple deletion not executed properly") ; 

		theDHS = DHSW.generateDHS(); 
		
		//checking DHS.ruleCorrespondences
		corrRC = new int[][] { {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, {0, 1, 2, 3, 4, 5, 6, -1, 7, 8, 9}}; 
		
		errorCount += UTILS.checkBoolean ( true, 
			UTILS.compare2dIntArrs( theDHS.getRuleCorrespondences(), corrRC),
			"ERROR: DifferentialHypothesisSimulator.ruleCorrespondences appears to have been malformed.\n"
			+ "Correct :\n"+UTILS.print1dIntArr(corrRC[0])+"\n"+UTILS.print1dIntArr(corrRC[1])+
			"\nObserved : \n"+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[0])+"\n"+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[1])) ? 0 : 1; 
						
		btg = theDHS.getBaseIndsToGlobal(); htg = theDHS.getHypIndsToGlobal(); 

		errorCount += chBoolPrIncIfError(getLineNumber(), true, btg.length == 11, "ERROR: base to global ind mapper has wrong dimensions"); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, htg.length == 10, "ERROR: hyp to global ind mapper has wrong dimensions"); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true,
			UTILS.compare1dIntArrs( btg, new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}),
			"ERROR: base to global ind mapper is malformed");
		errorCount += chBoolPrIncIfError(getLineNumber(), true,
			UTILS.compare1dIntArrs( htg, new int[] {0, 1, 2, 3, 4, 5, 6, 8, 9, 10}),
			"ERROR: hyp to global ind mapper is malformed"); 

		//test divergence point.
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDivergencePoint() == 7,
			"ERROR: divergence point should be 7 but it is "+theDHS.getDivergencePoint()) ; 
		
		//test lexical effects -- 'bitten' (et0) should be unaffected, but butter (et22) should be effected
			// also testing differential derivation generation for case of a deletion in this block.
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(0).equals(""),
			"ERROR: differential derivation for unaffected lexeme 'bitten' should be an empty string, but it is:\n"
			+ theDHS.getDifferentialDerivation(0)) ;
		corDD = "/bˈʌtə˞/\n" 
			+ "CONCORDANT UNTIL RULE : 7\n"
			+ "7[7|-1] : #bˈʌɾə˞# > #bˈʌɾə# | bled or deleted\n"
			+ "Waypoint 3 Gold : #bˈʌɾə# | #bˈʌɾə˞#\n"
			+ "Final forms : #bˈʌɾə# | #bˈʌɾə˞#";
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(22).equals(corDD),
			"ERRORː differential derivation for 'butter' is malformed"); 
			
		//checking DHS.locHasPrCh
		corrPCLs = new boolean[11];
		corrPCLs[7] = true; 
		errorCount += chBoolPrIncIfError(getLineNumber(),  true, UTILS.compare1dBoolArrs(corrPCLs, theDHS.getPrChLocs()), 
				"ERROR: DifferentialHypothesisSimulator.prChLocs is malformed\n"
				+ "Correct : "+UTILS.print1dBoolArrAsIntArr(corrPCLs)+"\nObserved : "
						+ UTILS.print1dBoolArrAsIntArr(theDHS.getPrChLocs())) ;

		//check DHS.changedDerivations
		Phone er = new Phone(phoneSymbToFeatsMap.get("ə˞"), featIndices, phoneSymbToFeatsMap); 
		int nFs = 0; 
		for (LexPhon ifi : inputForms)	nFs += ifi.findPhone(er) == -1 ? 0 : 1 ; 
		int[] efds = new int[nFs] ; int nfi = 0; 
		for(int ifii = 0; nfi < nFs ; ifii++)
			if (inputForms[ifii].findPhone(er) != -1)	efds[nfi++] = ifii; 

		errorCount += chBoolPrIncIfError(getLineNumber(),  true, 
			UTILS.compare1dIntArrs(efds, theDHS.getEtsWithChangedDerivations()),
			"ERROR: wrong etyma effected by deletion of derhotacization"); 
			
		//check DHS.changedRuleEffects
		String[] rhota = new String[NUM_ETYMA];
		for (int efdi : efds)	rhota[efdi] = getRegRuleEffect(theDHS.getDifferentialDerivation(efdi), 7, 7, -1); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dStrArrs(rhota, theDHS.getChangedRuleEffects().get(7)[0]), 
				"ERROR: construction of changedRuleEffects after simple deletion of derhotacization rule did not represent lost effects properly");  
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dStrArrs(new String[NUM_ETYMA], theDHS.getChangedRuleEffects().get(7)[1]),
				"ERROR: construction of changedRuleEffects after deletion of derhotacization rule thought there were gained effects when there were none");
		
		CREs = theDHS.getChangedRuleEffects(); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, CREs.keySet().size() == 1 , "ERROR: incorrect comprehension of effects of removing derhotacism");
		errorCount += chBoolPrIncIfError(getLineNumber(), true, CREs.containsKey(7), "ERROR: incorrect construction of changedRuleEffects after removing derhotacism"); 

		UTILS.errorSummary(errorCount);
		totalErrorCount += errorCount; 
		errorCount = 0; 
		
		//relocdation of flapping rule to after first waypoint
		System.out.println("\n-------------\nThird (line "+getLineNumber()+"): Testing comprehension of forward relocdation: "
				+ "moving the flapping rule that is at index 1 to index 5,"
				+ " after the first waypoint.\n----------------\n"); 
		
		DHSW.processChWithAddNearWaypoint(false, "g1", 
				1, "relocdated from 1 to 5, after first waypoint", "", null, "relocdated from 1 to 5, after first waypoint");; 
		
		//DHSW.processSingleCh(1,"relocdated from 1 to 6",6,"",null,
		//		"relocdated from 1 to 6");
		
		//testing realization in the cascade structures. 
		curHC = DHSW.getHypCASC();
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compareCascades(CASCADE, DHSW.getBaseCASC()),
			"ERROR: base cascade appears to have been corrupted during comprehension of a forward relocdation operation.");

		dumCasc.add(5,dumCasc.remove(1)); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compareCascades(dumCasc, curHC),
				"ERROR: malformed comprehension of forward relocdation operation."); 
		
		//test lengths
		errorCount += chBoolPrIncIfError(getLineNumber(), true, DHSW.getBaseCASC().size() == 11, "ERROR: wrong length assigned to baseline CASCADE."); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, DHSW.getHypCASC().size() == 10, "ERROR: wrong length assigned to hypothesis CASCADE."); 
		
		//testing DHSWrapper's base to hyp rule ind map 
		// prev RIM :  {0, 1, 2, 3, 4, 5, 6, -1, 7, 8, 9, 10} 
		corrBhRIM = new int[]{0, 5, 1, 2, 3, 4, 6, -1, 7, 8, 9, 10};
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(corrBhRIM, DHSW.getBaseHypRuleIndMap()),
			"ERROR: forward (originally) relocdation is not handled correctly in base-hyp rule ind map.\n"
			+ "Correct : "+UTILS.print1dIntArr(corrBhRIM)+
			"\nObserved : "+UTILS.print1dIntArr(DHSW.getBaseHypRuleIndMap()) ) ;
	
		//and same for hyp to base
		corrHbRIM = new int[] {0, 2, 3, 4, 5, 1 , 6, 8, 9, 10, 11}; 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare1dIntArrs(DHSW.getHypBaseRuleIndMap(), corrHbRIM), 
				"ERROR: forward (originally) relocdation not handled correctly in hyp-base rule ind map\n"
				+ "Correct: "+UTILS.print1dIntArr(corrHbRIM)+"\nObserved: "+UTILS.print1dIntArr(DHSW.getHypBaseRuleIndMap())) ;
		
		//test DHSWrapper.hypGoldLocs
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(new int[] {5, 7}, DHSW.getHypGoldLocs()),
			"ERROR: update on hypGoldLocs for forward relocdation (following a not-yet-accepted simple deletion hyp) not executed properly."
			+ "\nGold locs detected as "+UTILS.print1dIntArr(DHSW.getHypGoldLocs()) ) ; 
		
		//test DHSW.proposedChanges
		thepc = DHSW.getProposedChanges().get(2); 
			// should still be as before. 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, "7".equals(thepc[0]) && "deletion".equals(thepc[1]) && "we're Yankees".equals(thepc[2]),
			"ERROR: earlier not-yet-accepted hypothesis change is corrupted by processing of a new change!") ;
		
		//now test processing of the second change, which should consist of one deletion and one insertion.
		// first test the deletion.
		thepc = DHSW.getProposedChanges().get(0); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, "1".equals(thepc[0]) && "deletion".equals(thepc[1]) && "relocdated from 1 to 5, after first waypoint".equals(thepc[2]) ,
			"ERROR: at index 0 should be deletion part of update on proposedChanges for forward relocdation;\n"
			+ "construction of proposedChs handled incorrectly!") ;
		
		// and then the insertion phase
		thepc = DHSW.getProposedChanges().get(1);
		errorCount += chBoolPrIncIfError(getLineNumber(),  true , "5".equals(thepc[0]) && "[+cor,-delrel] > ɾ / [-cons] __ [-stres]".equals(thepc[1]) && "relocdated from 1 to 5, after first waypoint".equals(thepc[2]), 
				"ERROR: at index 1 should be processing of insertion phase of update on proposedChanges for forward relocdation, building of proposedChs executed incorrectly!\n"
				+ "thepc[0] = "+thepc[0]); 
		
		theDHS = DHSW.generateDHS(); 
		
		//checking DHS.ruleCorrespondences
		corrRC = new int[][] { {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, {0, 5, 1, 2, 3, 4, 6, -1, 7, 8, 9}} ; 
		errorCount += UTILS.checkBoolean ( true, 
			UTILS.compare2dIntArrs( theDHS.getRuleCorrespondences(), corrRC),
			"ERROR: DifferentialHypothesisSimulator.ruleCorrespondences appears to have been malformed"
			+ "\nCorrect:\n"+UTILS.print1dIntArr(corrRC[0])+"\n"+UTILS.print1dIntArr(corrRC[1]) + "\n"
			+ "Observed:\n"+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[0])+"\n"+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[1])) ? 0 : 1; 
		
		btg = theDHS.getBaseIndsToGlobal(); htg = theDHS.getHypIndsToGlobal(); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, btg.length == 11, "ERROR: base to global ind mapper has wrong dimensions"); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, htg.length == 10, "ERROR: hyp to global ind mapper has wrong dimensions"); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true,
			UTILS.compare1dIntArrs( btg, new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}),
			"ERROR: base to global ind mapper is malformed");
		int[] corHTG = new int[] {0, 2, 3, 4, 5, 1, 6, 8, 9, 10 }; 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs( htg, corHTG),
			"ERROR: hyp to global ind mapper is malformed\nCorrect: "+UTILS.print1dIntArr(corHTG)+"\n"+UTILS.print1dIntArr(htg)) ;

		//test divergence point.
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDivergencePoint() == 1,
			"ERROR: divergence point should be 1 but it is "+theDHS.getDivergencePoint()) ;
		
		//test lexical effects -- butter (et22) should be effected
		// fountain (et4) should not be effected
		// also testing differential derivation generation for case of a forward relocdation in this block.
		corDD = "/bˈʌtə˞/\n" + 
				"CONCORDANT UNTIL RULE : 1\n" + 
				"1[1|-1] : #bˈʌtə˞# > #bˈʌɾə˞# | bled or deleted\n" + 
				"Waypoint 1 Gold : #bˈʌɾə˞# | #bˈʌtə˞#\n" + 
				"1[-1|5] : fed or inserted | #bˈʌtə˞# > #bˈʌɾə˞#\n" + 
				"Waypoint 2 Black : #bˈʌɾə˞# | #bˈʌɾə˞#\n" + 
				"7[7|-1] : #bˈʌɾə˞# > #bˈʌɾə# | bled or deleted\n" + 
				"Waypoint 3 Gold : #bˈʌɾə# | #bˈʌɾə˞#\n" + 
				"Final forms : #bˈʌɾə# | #bˈʌɾə˞#"; 
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(22).equals(corDD),
				"ERRORː differential derivation for 'butter' is malformed\n"
				+ "Correct : "+corDD+"\nObserved : "+theDHS.getDifferentialDerivation(22)); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(4).equals(""),
				"ERROR: differential derivation for unaffected lexeme 'fountain' should be an empty string, but it is:\n"
				+ theDHS.getDifferentialDerivation(0)) ;
		
		//checking DHS.prChLocs
		corrPCLs = new boolean[11];
		corrPCLs[7] = true; 
		corrPCLs[1] = true; 
		errorCount += chBoolPrIncIfError(getLineNumber(),  true, UTILS.compare1dBoolArrs(corrPCLs, theDHS.getPrChLocs()), 
				"ERROR: DifferentialHypothesisSimulator.prChLocs is malformed\n"
				+ "Correct : "+UTILS.print1dBoolArrAsIntArr(corrPCLs)+"\nObserved : "
						+ UTILS.print1dBoolArrAsIntArr(theDHS.getPrChLocs())) ;

		//TODO when have time, find good way to test theDHS.changedDerivations here. 
			//TODO check this was/is done (Dec 2022) 
		
		//checking DHS.changedRuleEffects
		//TODO may need to expand coverage to match what we have for previous tests... 
			//TODO check this was/is done (Dec 2022) 
		CREs = theDHS.getChangedRuleEffects();
		errorCount += chBoolPrIncIfError(getLineNumber(), true, CREs.keySet().size() == 2,  
				"ERROR : size of hashmap changedRuleEffects should be 2 but it is"+CREs.keySet().size()) ;
		for (int ri : new int[] {5,7})
			errorCount += chBoolPrIncIfError(getLineNumber(), true, CREs.containsKey(ri), "ERROR: changedRuleEffects should have a key for global rule "+ri); 
		
		UTILS.errorSummary(errorCount);
		totalErrorCount += errorCount; 
		errorCount = 0; 
		
		//usurp. 
		DHSW.setHypOutLoc(DBG_WRKG_CASC); 
		DHSW.acceptHypothesis(false);
		CASCADE = curHC; 
		curHC = null; dumCasc = null;
		
		System.out.println("\n-------------\nFourth (line "+getLineNumber()+"): Now processing three changes for hypothesis, before usurping baseline.\n---------------\n"); 
		System.out.println("First in group, fourth overall -- complex modification of contexts of glottalization\n"
                        + "\t\tfrom __ ə\n"
                        + "\tto [+son] __ {# ; [-stres,-back,+nas]}\n");

		nextLaw =  "t > ʔ / [+son] __ {# ; [-stres,-back,+nas]}";
		
		String[] ch4InsPCform = new String[] {"6", nextLaw, "Insertion of T-glottalization with refined contexts"},
		ch4DelPCform = new String[] {"8", "deletion", "T-glottalization contexts refined, old form here removed"};
		DHSW.processSingleCh( 6, ch4DelPCform[2], 6, nextLaw,
				theFactory.generateSoundChangesFromRule(nextLaw), ch4InsPCform[2]);
		
		curHC = DHSW.getHypCASC(); dumCasc = new ArrayList<SChange>(CASCADE);
		
		//test realization in cascade structures
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compareCascades(dumCasc, DHSW.getBaseCASC()),
		                "ERROR: base cascade appears to have been corrupted during comprehension of a complex modification operation.");
		dumCasc.remove(6);
		dumCasc.addAll(6, theFactory.generateSoundChangesFromRule(nextLaw));
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compareCascades(dumCasc, curHC),
		"ERROR: malformed comprehension of complex modification operation");
		
		//testing base to hyp rule ind map
		corrBhRIM = new int[] {0, 1, 2, 3, 4, 5, -1, 8, 9, 10, 11};
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(corrBhRIM, DHSW.getBaseHypRuleIndMap()),
				"ERROR: Handling of complex modification in base to hyp rule ind map not realized correctly.\n"
				+ "correct : "+UTILS.print1dIntArr(corrBhRIM)+"\nObserved: "+UTILS.print1dIntArr(DHSW.getBaseHypRuleIndMap()));
		
		//and hyp to base
		corrHbRIM = new int[] {0, 1, 2, 3, 4, 5, -1, -1, 7, 8 ,9, 10};
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(corrHbRIM, DHSW.getHypBaseRuleIndMap()),
				"ERROR: Handling of complex modification in hyp to base rule ind map not realized correctly.");
		
		//test hypGoldLocs -- hypBlackLocs functionality is implied this way
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(new int[]{5,8}, DHSW.getHypGoldLocs()),
				"ERROR: complex modification not handled by correct update in DHSW.hypGoldLocs\n\t"
				+ "-- second gold stage should have been moved from instant 7 to 8.\n"
				+ "hyp gold locs observed : "+UTILS.print1dIntArr(DHSW.getHypGoldLocs()));
		
		//test DHSWrapper.proposedChanges
		//first should be the insertion.
		errorCount += calcPCerrs(getLineNumber(), 0, ch4InsPCform, DHSW.getProposedChanges().get(0),
				"insertion aspect of complex modification of T-glottalization rule");
		errorCount += calcPCerrs(getLineNumber(), 1, ch4DelPCform, DHSW.getProposedChanges().get(1),
				"deletion aspect of complex modification of T-glottalization rule");
		
		theDHS = DHSW.generateDHS();
		
		//checking DHS.ruleCorrespondences...
		corrRC = new int[][] { {0, 1, 2, 3, 4, 5, -1, -1, 6, 7, 8, 9},
			{0, 1, 2, 3, 4, 5, 6, 7, -1, 8, 9, 10}};
		errorCount += chBoolPrIncIfError ( getLineNumber(), true,
			UTILS.compare2dIntArrs( theDHS.getRuleCorrespondences(), corrRC),
				"ERROR: DifferentialHypothesisSimulator.ruleCorrespondences appears to have been malformed.\n"
						+ "Correct :\n"+UTILS.print1dIntArr(corrRC[0])+"\n"+UTILS.print1dIntArr(corrRC[1])+
						"\nObserved : \n"+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[0])+"\n"
						+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[1])) ;
		
		btg = theDHS.getBaseIndsToGlobal(); htg = theDHS.getHypIndsToGlobal();
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, btg.length == 10, "ERROR: base to global ind mapper has wrong dimensions");
		errorCount += chBoolPrIncIfError(getLineNumber(), true, htg.length == 11, "ERROR: hyp to global ind mapper has wrong dimensions");
		errorCount += chBoolPrIncIfError(getLineNumber(), true,
				UTILS.compare1dIntArrs( btg, new int[] {0, 1, 2, 3, 4, 5, 8, 9, 10, 11}),
				"ERROR: base to global ind mapper is malformed");
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare1dIntArrs( htg, new int[] {0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11}),
				"ERROR: hyp to global ind mapper is malformed");
		
		//test divergence point.
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDivergencePoint() == 6,
				"ERROR: divergence point should be 6 but it is "+theDHS.getDivergencePoint()) ;
		
		//test lexical effects. 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(29).equals(""),
				"ERROR: differential derivation for unaffected lexeme 'bidden' should be an empty string, but it is:\n"
				+ theDHS.getDifferentialDerivation(29)); 
		//now check syntaxes for cases that were changed.
		// case of word 0 bitten -- bleeding should mean that there is (erroneously) no change in differential derivation -- yet. 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(0).equals(""),
				"ERROR: differential derivation for lexeme 'bitten' should be an empty string\n"+
				"because at this point the correction is being bled but it is:\n" + theDHS.getDifferentialDerivation(0)); 
		//case of word 4 fountain -- here we see a real change. 
		corDD = "/fˈæwntən/\n"
			+ "CONCORDANT UNTIL RULE : 7\n"
			+ "7[-1|7] : fed or inserted | #fˈæ̃w̃ntə̃n# > #fˈæ̃w̃nʔə̃n#\n"
			+ "Waypoint 2 Black : #fˈæ̃w̃ntə̃n# | #fˈæ̃w̃nʔə̃n#\n"
			+ "Waypoint 3 Gold : #fˈæ̃w̃ntə̃n# | #fˈæ̃w̃nʔə̃n#\n"
			+ "Final forms : #fˈæ̃w̃ntə̃n# | #fˈæ̃w̃nʔə̃n#";			
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(4).equals(corDD),
				"ERROR: derivation of fountain is malformed"); 
		String fountainDDr4 = ""+corDD; 
		
		//test DHS.locHasPrCh
		corrPCLs = new boolean[12]; 
		for (int cpi = 6; cpi < 9;cpi++) corrPCLs[cpi] = true; 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dBoolArrs(corrPCLs, theDHS.getPrChLocs()),
				"ERROR: locHasPrCh malformed."); 
		
		//check DHS.changedDerivations
		errorCount += chBoolPrIncIfError(getLineNumber(), true,
			UTILS.compare1dIntArrs(new int[] {4, 8, 13, 15, 26, 30, 31},
				theDHS.getEtsWithChangedDerivations()),
			"ERROR: wrong etyma effected by complex modification of t-glottalization"); 

		//check DHS.changedRuleEffects
		CREs = theDHS.getChangedRuleEffects();
		
		// all three rules added or subtracted should have changes associated--
			// 6: important[8], cadet[30], mitigate[31]
			// 7: fountain[4], mountain[13], molten[15]
			// 8: molted [26] 
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, CREs.keySet().size() == 3 , "ERROR: incorrect comprehension of effects of fixing contexts for /t/-glottalization");

		errorCount += chBoolPrIncIfError(getLineNumber(), true, CREs.containsKey(6), "ERROR: incorrect comprehension of effects of fixing contexts for /t/-glottalization");
		errorCount += chBoolPrIncIfError(getLineNumber(), true, CREs.containsKey(7), "ERROR: incorrect comprehension of effects of fixing contexts for /t/-glottalization");
		errorCount += chBoolPrIncIfError(getLineNumber(), true, CREs.containsKey(8), "ERROR: incorrect comprehension of effects of fixing contexts for /t/-glottalization");
		
		int[][] corrEffs = new int[][] { {8,30,31}, {4,13,15}, {26}}; 
		String[] descrs = new String[] { "first insertion", "second insertion", "deletion"};
		
		for (int cei = 0 ; cei < 3; cei++)
		{
			String[] lambda = new String[NUM_ETYMA]; 
			for (int ldi : corrEffs[cei])
				lambda[ldi] = getRegRuleEffect(theDHS.getDifferentialDerivation(ldi), 
						6+cei, cei < 2 ? -1 : 6, cei < 2 ? 6+cei : -1); 
			errorCount += chBoolPrIncIfError(getLineNumber(), true, 
					UTILS.compare1dStrArrs(lambda, CREs.get(cei+6)[cei < 2 ? 1 : 0]),
					"ERROR: changes from "+descrs[cei]+" aspect of t-glottalization context reform not properly processed!") ;
		}

		UTILS.errorSummary(errorCount);
		totalErrorCount += errorCount; 
		errorCount = 0; 
		
		System.out.println("--------------\nSecond rule in group, fifth overall (line "+getLineNumber()+"): backward relocdation of American raising to become second rule."); 
		String[] backRlcIns = new String[] {"1", ""+CASCADE.get(4), "relocdated from 4 to 1"},
				backRlcDel = new String[] {"5", "deletion", "relocdated from 4 to 1"};
		DHSW.processSingleCh(4, backRlcDel[2], 1, "", null, backRlcIns[2]); 
		
		//test realization in cascade structures.
		curHC = DHSW.getHypCASC(); 
		dumCasc.add(1,dumCasc.remove(4)); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compareCascades(dumCasc, curHC), 
			"ERROR: malformed comprehension of backward relocdation operation."); 
		
		//can skip testing lengths at this point, as well as making sure baseCASC was uncorrupted. If no error appeared before it won't happen here.  
		
		//update checker structures concerning mappings
		for(int ri = 1; ri < 4; ri++)	
		{	
			corrBhRIM[ri]++; 
			corrHbRIM[ri+1]--;
			corrRC[1][ri]++;
		}
		corrBhRIM[4] = 1;  corrHbRIM[1] = 4; 
		corrRC[1][4] = 1; 
		
		//testing DHSW's rule maps
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(corrBhRIM, DHSW.getBaseHypRuleIndMap()),
						"ERROR: Handling of backward relocdation in base to hyp rule ind map not realized correctly.\n"
						+ "correct : "+UTILS.print1dIntArr(corrBhRIM)+"\nObserved: "+UTILS.print1dIntArr(DHSW.getBaseHypRuleIndMap()));		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(corrHbRIM, DHSW.getHypBaseRuleIndMap()),
				"ERROR: Handling of backward relocdation in hyp to base rule ind map not realized correctly.");

		//skip testing hypGoldLocs -- should be unchanged
		
		//test DHSW.proposedChanges
		errorCount += calcPCerrs(getLineNumber(), 0, backRlcIns, DHSW.getProposedChanges().get(0), 
			"insertion aspect of backward relocdation of American raising.");
		errorCount += calcPCerrs(getLineNumber(), 1, backRlcDel, DHSW.getProposedChanges().get(1), 
			"deletion aspect of backward relocdation of American raising.");
		errorCount += calcPCerrs(getLineNumber(), 2, ch4InsPCform, DHSW.getProposedChanges().get(2),
				"insertion aspect of complex modification of T-glottalization rule");
		errorCount += calcPCerrs(getLineNumber(), 3, ch4DelPCform, DHSW.getProposedChanges().get(3),
				"deletion aspect of complex modification of T-glottalization rule");

		theDHS = DHSW.generateDHS(); 

		//checking DHS.ruleCorrespondences

		errorCount += chBoolPrIncIfError ( getLineNumber(), true,
			UTILS.compare2dIntArrs( theDHS.getRuleCorrespondences(), corrRC),
				"ERROR: DifferentialHypothesisSimulator.ruleCorrespondences appears to have been malformed.\n"
						+ "Correct :\n"+UTILS.print1dIntArr(corrRC[0])+"\n"+UTILS.print1dIntArr(corrRC[1])+
						"\nObserved : \n"+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[0])+"\n"
						+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[1])) ;
		
		btg = theDHS.getBaseIndsToGlobal(); htg = theDHS.getHypIndsToGlobal();
		
		// can skip checking btg and htg dimensions -- should be unchanged. 
		errorCount += chBoolPrIncIfError(getLineNumber(), true,
				UTILS.compare1dIntArrs( btg, new int[] {0, 1, 2, 3, 4, 5, 8, 9, 10, 11}),
				"ERROR: base to global ind mapper is malformed");
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare1dIntArrs( htg, new int[] {0, 4, 1, 2, 3, 5, 6, 7, 9, 10, 11}),
				"ERROR: hyp to global ind mapper is malformed");
				
		//test divergence point
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDivergencePoint() == 1,
				"ERROR: divergence point should be 1 but it is "+theDHS.getDivergencePoint()) ;
		
		//test lexical effects
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(29).equals(""),
				"ERROR: differential derivation for unaffected lexeme 'bidden' should be an empty string, but it is:\n"
				+ theDHS.getDifferentialDerivation(29)); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(4).equals(fountainDDr4),
				"ERROR: derivation of fountain is malformed after backward relocdation of American raising, which should have "
				+ "had no effect beyond that of the previous rule (which appears to have been correctly formed)."); 
		corDD = "/hˈajtən/\n" + 
				"CONCORDANT UNTIL RULE : 1\n" + 
				"1[1|-1] : #hˈajtən# > #hˈajtə̃n# | bled or deleted\n" + 
				"4[4|1] : #hˈajtə̃n# > #hˈʌjtə̃n# | #hˈajtən# > #hˈʌjtən#\n" + 
				"1[-1|2] : fed or inserted | #hˈʌjtən# > #hˈʌjtə̃n#\n" + 
				"Waypoint 1 Gold : #hˈʌjtə̃n# | #hˈʌjtə̃n#\n" + 
				"5[5|5] : #hˈʌjtə̃n# > #hˈʌjɾə̃n# | #hˈʌjtə̃n# > #hˈʌjɾə̃n#\n" + 
				"Waypoint 2 Black : #hˈʌjɾə̃n# | #hˈʌjɾə̃n#\n" + 
				"Waypoint 3 Gold : #hˈʌjɾə̃n# | #hˈʌjɾə̃n#\n" + 
				"Final forms : #hˈʌjɾə̃n# | #hˈʌjɾə̃n#";
			// TODO maybe this one should not really be different between the basline and hyp at all... 
					// since there is no material difference really, just the same things happening in a different order
						// to obtain the same results by the next waypoint... 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(7).equals(corDD),
				"ERROR: derivation of 'heighten' is malformed:"
				+ theDHS.getDifferentialDerivation(7)); 

		//test DHS.locHasPrCh
		corrPCLs[1] = true; 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dBoolArrs(corrPCLs,  theDHS.getPrChLocs()),
				"ERROR: locHasPrCh malformed.\nCorr : "+UTILS.print1dBoolArrAsIntArr(corrPCLs)+"\n"+UTILS.print1dBoolArrAsIntArr(theDHS.getPrChLocs()));
		
		//check DHS.changedDerivations
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(new int[] {2, 3, 4, 7, 8, 13, 15, 19, 26, 30, 31},
				theDHS.getEtsWithChangedDerivations()), "ERROR: wrong etyma effected by the last rule plus the movement of Canadian raising"); 
			// case of number 21, "item" -- although the exact number for Canadian raising has changed
				// the ordering of different forms has not
				// nor has the alignment
				// so this is not included
				// for enlighten(er) and heighten, Canadian raising now newly precedes nasalization of the /ə/
				// that is the difference.

		//check DHS.changedRuleEffects -- should be unchanged, as we simply moved when the effect happened. 
		CREs = theDHS.getChangedRuleEffects();
		errorCount += chBoolPrIncIfError(getLineNumber(), true, CREs.keySet().size() == 3, 
				"ERROR: incorrect comprehension of t-glot change plus Canadian raising for changedRuleEffects"); 
				
		System.out.println("-------------\nThird rule in group, sixth overall (line "+getLineNumber()+"): relocating flapping rule to just before third waypoint.");
		
		String currNote = "relocdated from 5 to 7, just before third waypoint";
		
		String[] ch6DelPCform = new String[] {"5", "deletion", currNote},
				ch6InsPCform = new String[] {"7", "[+cor,-delrel] > ɾ / [-cons] __ [-stres]", currNote}; 
		
		DHSW.processChWithAddNearWaypoint(true, "g2", 
				5, currNote, "", null, currNote);
		 
		//test realization in cascade structures
		curHC = DHSW.getHypCASC(); 
		dumCasc.add(7, dumCasc.remove(5)); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compareCascades(dumCasc, curHC),
				"ERROR: malformed comprehension of forward relocdation operation targeting spot just before a waypoint."); 

		//can skip testing lengths at this point, as well as making sure baseCASC was uncorrupted. If no error appeared before it won't happen here.  
		//update checker structures concerning mappings
		corrBhRIM[5] = 7; 
		corrHbRIM[5] = -1; //[6] already is -1.
		corrHbRIM[7] = 5; 
		//testing DHSW's rule maps. 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(corrBhRIM, DHSW.getBaseHypRuleIndMap()),
				"ERROR: Handling of forward relocdation in base to hyp rule ind map not realized correctly.\n"
						+ "correct : "+UTILS.print1dIntArr(corrBhRIM)+"\nObserved: "  
								+UTILS.print1dIntArr(DHSW.getBaseHypRuleIndMap()));	
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(corrHbRIM, DHSW.getHypBaseRuleIndMap()),
				"ERROR: Handling of forward relocdation in hyp to base rule ind map not realized correctly.\n"
						+ "correct : "+UTILS.print1dIntArr(corrHbRIM)+"\nObserved: "  
						+UTILS.print1dIntArr(DHSW.getHypBaseRuleIndMap()));	
		
		//testing hypGoldLocs and hypBlackLocs
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare1dIntArrs(new int[] {5, 8}, DHSW.getHypGoldLocs()),
				"ERROR: forward relocdation to before waypoint not handled correctly in DHSW.hypGoldLocs (which shouldn't change)."
				+ "\nObserved hyp gold locs "+UTILS.print1dIntArr(DHSW.getHypGoldLocs()));  
		errorCount += chBoolPrIncIfError(getLineNumber(), true,
				UTILS.compare1dIntArrs(new int[] {7}, DHSW.getHypBlackLocs()), 
				"ERROR: forward relocdation to before waypoint not handled correctly in DHSW.hypBlackLocs (should now be 7)"); 
		
		ch4InsPCform[0] = ""+(Integer.parseInt(ch4InsPCform[0])-1); 
		ch4DelPCform[0] = ""+(Integer.parseInt(ch4DelPCform[0])-1);
		
		// testing DHSW.proposedChanges
		errorCount += calcPCerrs(getLineNumber(), 0, backRlcIns, DHSW.getProposedChanges().get(0), 
				"insertion aspect of backward relocdation of American raising.");
		errorCount += calcPCerrs(getLineNumber(), 1, backRlcDel, DHSW.getProposedChanges().get(1), 
			"deletion aspect of backward relocdation of American raising.");
		errorCount += calcPCerrs(getLineNumber(), 2, ch6DelPCform, DHSW.getProposedChanges().get(2),
				"deletion aspect of forward relocdation of flapping."); 
		errorCount += calcPCerrs(getLineNumber(), 3, ch4InsPCform, DHSW.getProposedChanges().get(3),
				"insertion aspect of complex modification of T-glottalization rule");
		errorCount += calcPCerrs(getLineNumber(), 4, ch4DelPCform, DHSW.getProposedChanges().get(4),
				"deletion aspect of complex modification of T-glottalization rule");
		errorCount += calcPCerrs(getLineNumber(), 5, ch6InsPCform, DHSW.getProposedChanges().get(5),
				"insertion aspect of forward relocdation of flapping"); 
		
		theDHS = DHSW.generateDHS(); 
		
		//checking DHS.ruleCorrespondences
		corrRC[1][5] = 7;
		corrRC[1][6] = 5; 
		corrRC[1][7] = 6; 
		
		errorCount += chBoolPrIncIfError ( getLineNumber(), true,
				UTILS.compare2dIntArrs( theDHS.getRuleCorrespondences(), corrRC),
					"ERROR: DifferentialHypothesisSimulator.ruleCorrespondences appears to have been malformed.\n"
							+ "Correct :\n"+UTILS.print1dIntArr(corrRC[0])+"\n"+UTILS.print1dIntArr(corrRC[1])+
							"\nObserved : \n"+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[0])+"\n"
							+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[1])) ;
		
		btg = theDHS.getBaseIndsToGlobal(); htg = theDHS.getHypIndsToGlobal();
		// can skip checking btg and htg dimensions -- should be unchanged. 
		
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare1dIntArrs(btg, new int[] {0, 1, 2, 3, 4, 5, 8, 9, 10, 11}), 
				"ERROR: base to global ind mapper is malformed"); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare1dIntArrs(htg, new int[] {0, 4, 1, 2, 3, 6, 7, 5, 9, 10, 11 }),
				"ERROR: hyp to global ind mapper is malformed"); 
		
		//test divergence point
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDivergencePoint() == 1,
				"ERROR: divergence point should be 1 but it is "+theDHS.getDivergencePoint()) ;
		
		//test lexical effects
		
		// 25 | maintain -- not affected by any of the added rules. 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(25).equals(""),
				"ERROR: differential derivation for unaffected lexeme 'maintain' should be an empty string, but it is:\n"
				+ theDHS.getDifferentialDerivation(25)); 
		// 8 | heighten -- affected by all three test rules. 
		corDD = "/hˈajtən/\n" + 
				"CONCORDANT UNTIL RULE : 1\n" + 
				"1[1|-1] : #hˈajtən# > #hˈajtə̃n# | bled or deleted\n" + 
				"4[4|1] : #hˈajtə̃n# > #hˈʌjtə̃n# | #hˈajtən# > #hˈʌjtən#\n" + 
				"1[-1|2] : fed or inserted | #hˈʌjtən# > #hˈʌjtə̃n#\n" + 
				"Waypoint 1 Gold : #hˈʌjtə̃n# | #hˈʌjtə̃n#\n" + 
				"5[5|-1] : #hˈʌjtə̃n# > #hˈʌjɾə̃n# | bled or deleted\n" +
				"7[-1|6] : fed or inserted | #hˈʌjtə̃n# > #hˈʌjʔə̃n#\n" +
				"Waypoint 2 Black : #hˈʌjɾə̃n# | #hˈʌjʔə̃n#\n" + 
				"Waypoint 3 Gold : #hˈʌjɾə̃n# | #hˈʌjʔə̃n#\n" + 
				"Final forms : #hˈʌjɾə̃n# | #hˈʌjʔə̃n#";
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(7).equals(corDD),
				"ERROR: differential derivation for 'heighten' is malformed:\n"+theDHS.getDifferentialDerivation(7)); 
		// 29 | bidden -- affected only by test rule 6 (moving flapping across a waypoint) 
		corDD = "/bˈɪdən/\n" + 
				"CONCORDANT UNTIL RULE : 5\n" + 
				"5[5|-1] : #bˈɪdə̃n# > #bˈɪɾə̃n# | bled or deleted\n" + 
				"Waypoint 2 Black : #bˈɪɾə̃n# | #bˈɪdə̃n#\n" + 
				"5[-1|7] : fed or inserted | #bˈɪdə̃n# > #bˈɪɾə̃n#\n" + 
				"Waypoint 3 Gold : #bˈɪɾə̃n# | #bˈɪɾə̃n#\n" + 
				"Final forms : #bˈɪɾə̃n# | #bˈɪɾə̃n#";
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(29).equals(corDD),
				"ERROR: differential derivation for 'bidden' is malformed:\n"
				+ theDHS.getDifferentialDerivation(29)); 
		// 31 | mitigate -- affected by test rules 4 and 6 specifically
		corDD = "/mˈɪtɪɡɛjt/\n"
				+ "CONCORDANT UNTIL RULE : 5\n" 
				+ "5[5|-1] : #mˈɪtɪɡɛjt# > #mˈɪɾɪɡɛjt# | bled or deleted\n"
				+ "6[-1|5] : fed or inserted | #mˈɪtɪɡɛjt# > #mˈɪtɪɡɛjʔ#\n"
				+ "Waypoint 2 Black : #mˈɪɾɪɡɛjt# | #mˈɪtɪɡɛjʔ#\n"
				+ "5[-1|7] : fed or inserted | #mˈɪtɪɡɛjʔ# > #mˈɪɾɪɡɛjʔ#\n"
				+ "Waypoint 3 Gold : #mˈɪɾɪɡɛjt# | #mˈɪɾɪɡɛjʔ#\n"
				+ "10[8|9] : #mˈɪɾɪɡɛjt# > #mˈɪɾɨɡɛjt# | #mˈɪɾɪɡɛjʔ# > #mˈɪɾɨɡɛjʔ#\n"
				+ "Final forms : #mˈɪɾɨɡɛjt# | #mˈɪɾɨɡɛjʔ#";
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(31).contentEquals(corDD), 
				"ERROR: differential derivation for 'mitigate' is malformed:\n" + theDHS.getDifferentialDerivation(31));
		
		// test DHS.locHasPrCh
		corrPCLs[5] = true; 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dBoolArrs(corrPCLs,  theDHS.getPrChLocs()),
				"ERROR: locHasPrCh malformed.\nCorr : "+UTILS.print1dBoolArrAsIntArr(corrPCLs)+"\n"+UTILS.print1dBoolArrAsIntArr(theDHS.getPrChLocs()));
		
		//check DHS.changedDerivations
		errorCount += chBoolPrIncIfError( getLineNumber(), true, 
				UTILS.compare1dIntArrs(theDHS.getEtsWithChangedDerivations() ,
						new int[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,26,29,30,31,33,35,36}), 
				"ERROR: wrong etyma effected by last three rules in aggregate") ; 

		//check DHS.changedRuleEffects -- should be unchanged, as we simply moved when the effect happened. 
		CREs = theDHS.getChangedRuleEffects();
		errorCount += chBoolPrIncIfError(getLineNumber(), true, CREs.keySet().size() == 4,
				"ERROR: incorrect comprehension of t-glot change plus Canadian raising for changedRuleEffects"); 
	
		UTILS.errorSummary(errorCount);
		totalErrorCount += errorCount; 
		errorCount = 0; 
	
		//usurp. 
		DHSW.setHypOutLoc(DBG_WRKG_CASC); 
		DHSW.acceptHypothesis(false);
		CASCADE = curHC; 
		curHC = null; dumCasc = null;		
		
		//remaining fixes bundled into one hypothesis. 
		System.out.println("-----------------\nRemaining fixes bundled into one hypothesis....\n-----------------------");
		
		String nx_law = "[+cor,-delrel] > ɾ / [-cons] __ [-cons,-lo,-stres]";
		
		DHSW.processSingleCh(7, "Flapping contexts revised", 7, nx_law, 
				theFactory.generateSoundChangesFromRule(nx_law), "Flapping contexts revised");
		
		nx_law = "d > ɾ / [-cons] __ [-cons]";
		DHSW.processChWithAddNearWaypoint(true, "g2", -1, "",
				nx_law, theFactory.generateSoundChangesFromRule(nx_law), "d flapping between nonconsonantals.");
		
		nx_law = "d > ɾ / [+son] __ [+syl,-stres]"; 
		DHSW.processChWithAddNearWaypoint(true, "g2", -1, "",
				nx_law, theFactory.generateSoundChangesFromRule(nx_law), "d flapping between sonorant cons and unstressed vowel.");
		
		nx_law = "[+syl,-stres] > [-nas] / __ n [+syl]"; 
		DHSW.processChWithAddNearWaypoint(false, "g2", -1, "", nx_law, theFactory.generateSoundChangesFromRule(nx_law), "denasalization before intervocalic /n/");
		
		nx_law = "n > ∅ / [-cons,+nas] __ [-son,-cor]";
		DHSW.processSingleCh(-1, "", 14, nx_law, theFactory.generateSoundChangesFromRule(nx_law), "/n/ effaced between nasal vowel and non-coronal obstruent.");

		nx_law = "n > ∅ / [-cons,+nas] __ [+cons,+son]"; 
		DHSW.processSingleCh(-1, "", 15, nx_law, theFactory.generateSoundChangesFromRule(nx_law), "presonorant /n/ effaced after nasal vowel");

		//testing base to hyp rule ind map in DHSWrapper. 
		corrBhRIM = new int[] {0,1,2,3,4,5,6,7,11,12,13,16}; //recall each index is about the spot *before* rules, the last being the end state. 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(corrBhRIM, DHSW.getBaseHypRuleIndMap()),
				"ERROR: Handling of simple deletion in base-hyp rule ind map not realized correctly... \ngot: "+UTILS.print1dIntArr(DHSW.getBaseHypRuleIndMap())); 
		
		//likewise for hyp to base
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(DHSW.getHypBaseRuleIndMap(), 
				new int[] {0,1,2,3,4,5,6,7,-1,-1,-1,8,9,10,-1,-1,11}), "ERROR: Handling of simple deletion in hyp-base rule ind map not realized correctly..."
						+ "got: "+UTILS.print1dIntArr(DHSW.getHypBaseRuleIndMap())); 
		
		// test DHSWrapper.hypGoldLocs
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(new int[] {5, 10}, DHSW.getHypGoldLocs() ),
				"ERROR: hyp gold locs are wrong: "+UTILS.print1dIntArr(DHSW.getHypGoldLocs()));
		// we can induce hypBlackLocs is fine if this is.
		
		//not necessary to test DHSWrapper.proposedChanges here. 
		
		theDHS = DHSW.generateDHS(); 
		
		//check DHSW.ruleCorrespondences
		corrRC = new int[][] {{0, 1, 2, 3, 4, 5, 6, 7, -1, -1, -1, 8, 9, 10, -1, -1}, {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}};
		errorCount += UTILS.checkBoolean (true, UTILS.compare2dIntArrs(theDHS.getRuleCorrespondences(),corrRC),
				"ERROR: ruleCorrespondences malformed:\n"
				+ "Observed:\n"+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[0])
				+ "\n"+UTILS.print1dIntArr(theDHS.getRuleCorrespondences()[1])
				+ "\nCorrect:\n"+UTILS.print1dIntArr(corrRC[0])+"\n"+UTILS.print1dIntArr(corrRC[1])+"\n") ? 0 : 1; 
		
		btg = theDHS.getBaseIndsToGlobal(); htg= theDHS.getHypIndsToGlobal(); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, btg.length == 11, "ERROR: base to global ind mapper has wrong dimensions"); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, htg.length == 16, "ERROR: hyp to global ind mapper has wrong dimensions"); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(btg, new int[] {0,1,2,3,4,5,6,7,11,12,13}), "ERROR: base to global ind mapper is malformed. Observed: "+UTILS.print1dIntArr(btg)); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(htg, new int[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}), "ERROR: hyp to global ind mapper is malformed. Observed: "+UTILS.print1dIntArr(htg)); 
		
		//test divergence point.
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDivergencePoint() == 7,
			"ERROR: divergence point should be 7 but it is "+theDHS.getDivergencePoint()) ; 

		// testing lexical effects
		// butter (et22) should be unaffected.
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(0).equals(""),
				"ERROR: differential derivation for unaffected lexeme 'butter' should be an empty string but it is:\n"+theDHS.getDifferentialDerivation(22));
		
		corDD = "/fˈutɑn/\n" + 
				"CONCORDANT UNTIL RULE : 7\n" + 
				"7[7|-1] : #fˈutɑ̃n# > #fˈuɾɑ̃n# | bled or deleted\n" + 
				"Waypoint 3 Gold : #fˈuɾɑ̃n# | #fˈutɑ̃n#\n" + 
				"Final forms : #fˈuɾɑ̃n# | #fˈutɑ̃n#";
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(24).equals(corDD),
				"ERROR: differential derivation of 'futan' is malformed:\n"+theDHS.getDifferentialDerivation(24)) ;
		corDD = "/ɹˈɛjdɑn/\n" + 
				"CONCORDANT UNTIL RULE : 7\n" + 
				"7[7|-1] : #ɹˈɛjdɑ̃n# > #ɹˈɛjɾɑ̃n# | bled or deleted\n" + 
				"8[-1|8] : fed or inserted | #ɹˈɛjdɑ̃n# > #ɹˈɛjɾɑ̃n#\n" + 
				"Waypoint 3 Gold : #ɹˈɛjɾɑ̃n# | #ɹˈɛjɾɑ̃n#\n" + 
				"Final forms : #ɹˈɛjɾɑ̃n# | #ɹˈɛjɾɑ̃n#"; 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(33).equals(corDD),
				"ERROR: differential derivation for 'radon' is malformed:\n"+theDHS.getDifferentialDerivation(33)); 
		corDD = "/bihˈowldən/\n" + 
				"CONCORDANT UNTIL RULE : 9\n" + 
				"9[-1|9] : fed or inserted | #bihˈowlˠdə̃n# > #bihˈowlˠɾə̃n#\n" + 
				"Waypoint 3 Gold : #bihˈowlˠdə̃n# | #bihˈowlˠɾə̃n#\n" + 
				"12[9|12] : #bihˈowlˠdə̃n# > #bɨhˈowlˠdə̃n# | #bihˈowlˠɾə̃n# > #bɨhˈowlˠɾə̃n#\n" + 
				"Final forms : #bɨhˈowlˠdə̃n# | #bɨhˈowlˠɾə̃n#"; 

		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(28).equals(corDD),
				"ERROR: differential derivation for 'beholden' is malformed:\n"+theDHS.getDifferentialDerivation(28));
		
		corDD = "/pˈændə˞/\n" + 
				"CONCORDANT UNTIL RULE : 9\n" + 
				"9[-1|9] : fed or inserted | #pʰˈæ̃ndə˞# > #pʰˈæ̃nɾə˞#\n" + 
				"Waypoint 3 Gold : #pʰˈæ̃ndə˞# | #pʰˈæ̃nɾə˞#\n" + 
				"15[-1|15] : fed or inserted | #pʰˈæ̃nɾə˞# > #pʰˈæ̃ɾə˞#\n" + 
				"Final forms : #pʰˈæ̃ndə˞# | #pʰˈæ̃ɾə˞#"; 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(32).equals(corDD), 
				"ERROR: differential derivation for 'pander' is malformed:\n" + theDHS.getDifferentialDerivation(32)); 
		
		corDD = "/mjˈutɪni/\n" + 
				"CONCORDANT UNTIL RULE : 10\n" + 
				"10[-1|10] : fed or inserted | #mjˈuʔɪ̃ni# > #mjˈuʔɪni#\n" + 
				"11[8|-1] : #mjˈuʔɪ̃ni# > #mjˈuʔə̃ni# | bled or deleted\n" + 
				"12[-1|12] : fed or inserted | #mjˈuʔɪni# > #mjˈuʔɨni#\n" + 
				"Final forms : #mjˈuʔə̃ni# | #mjˈuʔɨni#";
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(14).equals(corDD), 
				"ERROR: differential derivation for 'mutiny' is malformed:\n" + theDHS.getDifferentialDerivation(14)); 
		
		corDD = "/fˈæwntən/\n"
				+ "CONCORDANT UNTIL RULE : 14\n"
				+ "14[-1|14] : fed or inserted | #fˈæ̃w̃nʔə̃n# > #fˈæ̃w̃ʔə̃n#\n" 
				+ "Final forms : #fˈæ̃w̃nʔə̃n# | #fˈæ̃w̃ʔə̃n#";			
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getDifferentialDerivation(4).equals(corDD), 
				"ERROR: differential derivation for 'fountain' is malformed:\n"+theDHS.getDifferentialDerivation(4));
		
		// check DHS.locHasPrCh
		corrPCLs = new boolean[16]; 
		corrPCLs[7] = true;
		corrPCLs[8] = true; 
		corrPCLs[9] = true; 
		corrPCLs[10] = true; 
		corrPCLs[14] = true; 
		corrPCLs[15] = true; 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare1dBoolArrs(corrPCLs, theDHS.getPrChLocs()), "ERROR: prCHLcos is malformed:\n"
						+ "Correct: "+UTILS.print1dBoolArrAsIntArr(corrPCLs)+"\nObserved: "+UTILS.print1dBoolArrAsIntArr(theDHS.getPrChLocs())); 
		
		errorCount += chBoolPrIncIfError( getLineNumber(), true, 
				UTILS.compare1dIntArrs(theDHS.getEtsWithChangedDerivations(),
						new int[] {2, 3, 4, 5, 8, 13, 14, 16, 17, 19, 24, 28, 30, 32, 33, 34, 39}), 
				"ERROR : wrong etyma affected by last rule bundle...\nObserved: "+UTILS.print1dIntArr(theDHS.getEtsWithChangedDerivations()));
		
		CREs = theDHS.getChangedRuleEffects();
		errorCount += chBoolPrIncIfError(getLineNumber(), true, CREs.keySet().size() == 8,
				"ERROR: incorrect comprehension of changedRuleEffects"); 
		
		UTILS.errorSummary(errorCount);
		totalErrorCount += errorCount; 
		errorCount = 0; 
		
		DHSW.setHypOutLoc(DBG_WRKG_CASC); 
		DHSW.acceptHypothesis(false);
		CASCADE = curHC; 
		curHC = null; dumCasc = null;
	}
	
	
	private static DHSWrapper newDHS(Simulation sim)
	{	return new DHSWrapper(sim, feats_weighted, featsByIndex, FT_WTS, ID_WT, DBG_WRKG_CASC,theFactory); 
	}
	
	private static void extractSymbDefs()
	{
		System.out.println("Collecting symbol definitions...");
		
		featIndices = new HashMap<String, Integer>() ; 
		phoneSymbToFeatsMap = new HashMap<String, String>(); 
		
		List<String> symbDefsLines = UTILS.readFileLines(SYMBS_LOC);
		
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
				else	throw new Error("ERROR: unrecognized feature value, "+featVals[fvi]+" in line "+li);
			}
			
			phoneSymbToFeatsMap.put(symb, intFeatVals);
			li++; 
		}
	}
	
	public static void extractFeatImpls()
	{
		featImplications = new HashMap<String, String[]>(); 
		
		List<String> featImplLines = UTILS.readFileLines(FI_LOC);
		
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
					assert rli != 0: "ERROR: Stage set at the first line -- this is useless, redundant with the initial stage ";
					
					currRule = currRule.substring(1); 
					if (currRule.contains(""+UTILS.GOLD_STAGENAME_FLAG)) throw new RuntimeException(
						"ERROR: stage name flag <<"+UTILS.GOLD_STAGENAME_FLAG+">> occuring "
								+ "in a place besides the first character in the rule line -- this is illegal: \n"+currRule); 
					if(currRule.contains(UTILS.STAGENAME_LOC_DELIM+"")) throw new RuntimeException(
						"ERROR: illegal character found in name for custom stage -- <<"+UTILS.STAGENAME_LOC_DELIM+">>");  
					goldStageNameAndLocList.add(""+currRule+UTILS.STAGENAME_LOC_DELIM+rli);
					rulesByTimeInstant.remove(rli);  
				}
				else if (currRule.charAt(0) == UTILS.BLACK_STAGENAME_FLAG)
				{
					blackStagesSet =true;
					currRule = currRule.substring(1); 
					if(currRule.contains(UTILS.STAGENAME_LOC_DELIM+""))	throw new RuntimeException(
						"ERROR: illegal character found in name for custom stage -- <<"+UTILS.STAGENAME_LOC_DELIM+">>"); 
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
		
		System.out.println(CASCADE.size()+" diachronic rules extracted. "); 
		
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
		else if (numCols != NUM_GOLD_STAGES + 1) 
			throw new RuntimeException("ERROR: mismatch between number of columns in lexicon file and number of gold stages declared in rules file (plus 1)\n"
					+ "# stages in rules file : "+NUM_GOLD_STAGES+"; # cols : "+numCols); 
		
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
			if(lfli <NUM_ETYMA && numCols != UTILS.colCount(theLine))
				throw new RuntimeException("ERROR: incorrect number of columns in line "+lfli);
		}		

		if(goldOutput)	
			goldOutputLexicon = new Lexicon(goldResults); 
		
	}

	/** auxiliary.
	 * given String @param toLex
	 * @return its representation as a LexPhon containing a sequence of Phone instances
	 * TODO note we assume the phones are separated by PH_DELIM (presumably ' ') 
	 */
	private static LexPhon parseLexPhon(String toLexem)
	{
		String toLex = toLexem.trim(); 
		
		if (toLex.equals(UTILS.ABSENT_PH_INDIC))
		{	return new AbsentLexPhon();	}
		else if (toLex.equals(UTILS.UNATTESTED_PH_INDIC))
			return new UnattestedLexPhon(); 
		
		String[] toPhones = toLex.split(""+UTILS.PH_DELIM);
		
		List<SequentialPhonic> phones = new ArrayList<SequentialPhonic>(); //LexPhon class stores internal List of phones not an array,
			// for better ease of mutation

		for (String toPhone : toPhones)
		{
			if (toPhone.equals("#") || toPhone.equals("+"))
				phones.add(new Boundary(toPhone.equals("#") ? "word bound" : "morph bound"));
			else
			{
				if(! phoneSymbToFeatsMap.containsKey(toPhone))	throw new RuntimeException(
					"ERROR: tried to declare a phone in a word in the lexicon using an invalid symbol.\n"
					+ "Symbol is : '"+toPhone+"', length = "+toPhone.length()); 
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
	
	
	/** 
	 * @param dd -- differential derivation
	 * @param gi -- global ind
	 * @param bi -- base casc ind 
	 * @param hi -- hyp casc ind
	 * @return form resulting from the rule
	 */
	private static String getRegRuleEffect(String dd, int gi, int bi, int hi)
	{
		String breaker = ""+gi+"["+bi+"|"+hi+"] : "; 
		String targ = dd.substring(dd.indexOf(breaker)+breaker.length()); 
		targ = targ.substring(0, targ.indexOf("\n")); 
		int splint = targ.indexOf("|");
		return (bi == -1 ? targ.substring(splint+1) : targ.substring(0,splint)).trim(); 
	}


	private static int chBoolPrIncIfError(int lnNum, boolean targ, boolean obs, String bareErrMsg)
	{
		return UTILS.checkBoolean(targ, obs, "@line"+lnNum+": "+bareErrMsg) ? 0 : 1; 
	}
	
	public static int getLineNumber() {
	    return Thread.currentThread().getStackTrace()[2].getLineNumber();
	}
	
	//for error printing and counting of errors regarding DHSW.proposedChanges
	private static int calcPCerrs(int lineNum, int index, String[] corrPC, String[] obsPC, String description)
	{
	        int errs = 0; 
	        String prefix = "@"+lineNum+" ERROR: at pc index "+index+" we should have "+description;
	        errs += chBoolPrIncIfError(lineNum, true, corrPC[0].equals(obsPC[0]),
	                prefix+" indexed to "+corrPC[0]+" but instead we see it is indexed to "+obsPC[0]);
	        errs += chBoolPrIncIfError(lineNum, true, corrPC[1].equals(obsPC[1]),
	                prefix+" described as "+corrPC[1]+" but instead we observe description: "+obsPC[1]); 
	        errs += chBoolPrIncIfError(lineNum, true, corrPC[2].equals(obsPC[2]),
	                prefix+" commented with "+corrPC[2]+" but instead comment is "+obsPC[2]); 
	        return errs; 
	}

	
}
