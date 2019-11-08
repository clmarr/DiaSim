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
	
	private static String[] SO; //ordering of stages. 
	
	public static void main(String args[]) throws MidDisjunctionEditException
	{
		initWorkingCascFile(); 

		extractSymbDefs(); 
		extractFeatImpls();
		
		System.out.println("Creating SChangeFactory...");
		theFactory = new SChangeFactory(phoneSymbToFeatsMap, featIndices, featImplications); 
		
		SO = UTILS.extractStageOrder(DBG_START_CASC); 
		extractCascAndLex(theFactory, DBG_GOLD_CASC); 
			// first extracting from gold casc so that we do initial sanity test of the correct cascade leading to correct forms
				// with all metrics agreeing with this etc etc. 
		
		System.out.println("Lexicon extracted. Now debugging.");
		
		Simulation testSimul = new Simulation (inputForms, CASCADE, SO);

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
		
		UTILS.errorSummary(errorCount); 
		
		testSimul.simulateToNextStage();
		totalErrorCount += errorCount; 
		errorCount = 0; 
		
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, testSimul.justHitGoldStage(), "ERROR: gold stage erroneously not detected"); 
		checker = standardChecker(testSimul.getStageResult(true, 0), testSimul.getGoldStageGold(0)); 

		errorCount +=UTILS.checkMetric(1.0, checker.getAccuracy(), "ERROR: accuracy of only %o at first gold waypoint compared to stored result lexicon at that point.") ? 0 : 1 ; 
		errorCount +=UTILS.aggregateErrorsCheckWordLists(goldStageGoldWordlists[0], testSimul.getCurrentResult().getWordList()); 

		//TODO check modification methods in DiachronicSimulator -- or do this later? 
				
		testSimul.simulateToNextStage();
		//TODO check going to first black
		totalErrorCount += errorCount; 
		errorCount = 0 ;
		System.out.println("Checking Waypoint 1 (black box mode)"); 
		errorCount +=chBoolPrIncIfError(getLineNumber(), false, testSimul.justHitGoldStage(), "ERROR: gold stage erroneously detected at point of a black box stage."); 
		
		//TODO other shit here... 
		
		UTILS.errorSummary(errorCount); 
		
		testSimul.simulateToNextStage();
		totalErrorCount += errorCount; 
		errorCount = 0; 
		
		// TODO checks after skipping final gold stage before the end
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
		testSimul = new Simulation(inputForms, CASCADE, SO); 
		testSimul.setBlackStages(blackStageNames, blackStageInstants);
		testSimul.setGold(goldOutputLexicon.getWordList());
		testSimul.setGoldStages(goldStageGoldWordlists, goldStageNames, goldStageInstants);
		testSimul.simulateToEnd(); 

		String bittenCorrectBaselineDeriv = "/bˈɪtən/\n" + 
				"#bˈɪɾən# | 0 : [+cor,-delrel] > ɾ / [-cons] __ [-stres]\n" + 
				"#bˈɪɾə̃n# | 2 : [-cons] > [+nas,+son,0delrel] / __ n\n" + 
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
		System.out.println("----------------\n\nFirst test: insertion of l-darkening rule at the beginning of the cascade\n"
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
		bcdlines[2] = bcdlines[2].substring(0, colloc-2) + "3"+bcdlines[2].substring(colloc-1); 
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
		errorCount +=chBoolPrIncIfError(getLineNumber(), true, theDHS.getGlobalizedDerivation(0 , false).equals(bittenCorrectBaselineDeriv.replace("0 :","1 :" ).replace("2 :", "3 :")),
				"ERROR: malformation of globalized derivation in baseline for 'bitten'"); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getGlobalizedDerivation(26 , false).equals(mbdGlobCor), 
				"ERROR: malformation of globalized derivation in baseline for 'molted'"); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, theDHS.getGlobalizedDerivation(26 , true).equals(mhdCor), 
				"ERROR: malformation of proposed hypothesis' predicted derivation in baseline for 'molted'"); 
	
		//checking DHS.prChLocs
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
		System.out.println("-----------------\nSecond: Testing comprehension of simple deletion (in this case, of a derhotacization rule).");
		System.out.println("Deleting derhotacization rule at index 7.\n----------------\n");

		DHSW.processSingleCh(7,"we're Yankees", -1, "", null, "");
		curHC = DHSW.getHypCASC(); dumCasc = new ArrayList<SChange>(CASCADE); 

		//testing realization in the cascade structures. 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compareCascades(dumCasc, DHSW.getBaseCASC()),
			"ERROR: base cascade appears to have been corrupted during comprehension of a deletion operation."); 
			
		dumCasc.remove(7); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compareCascades(dumCasc, curHC),
			"ERROR: malformed comprehension of simple deletion operation."); 
		
		//testing base to hyp rule in map in DHSWrapper 
			// -- before this operation there were 11 rules, and we are deleting the 8th. 
		int[] corrBhRIM = new int[] {0, 1, 2, 3, 4, 5, 6, -1, 7, 8, 9, 10} ;
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
		System.out.println("\n-------------\nThird: Testing comprehension of forward relocdation: moving the flapping rule that is at index 1 to index 6.\n----------------\n"); 
		
		DHSW.processSingleCh(1,"Relocdated to after first waypoint",6,"",null,"Relocdated from former step 1");
		
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
		corrBhRIM = new int[]{0, 6, 1, 2, 3, 4, 5, -1, 7, 8, 9, 10};
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(corrBhRIM, DHSW.getBaseHypRuleIndMap()),
			"ERROR: forward relocdation is not handled correctly in base-hyp rule ind map.\n"
			+ "Correct : "+UTILS.print1dIntArr(corrBhRIM)+
			"\nObserved : "+UTILS.print1dIntArr(DHSW.getBaseHypRuleIndMap()) ) ;
	
		//and same for hyp to base
		int[] corrHbRIM = new int[] {0, 2, 3, 4, 5, 6 , 1, 8, 9, 10, 11}; 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, 
				UTILS.compare1dIntArrs(DHSW.getHypBaseRuleIndMap(), corrHbRIM), 
				"ERROR: forward relocdation not handled correctly in hyp-base rule ind map\n"
				+ "Correct: "+UTILS.print1dIntArr(corrHbRIM)+"\nObserved: "+UTILS.print1dIntArr(DHSW.getHypBaseRuleIndMap())) ;
		
		//test DHSWrapper.hypGoldLocs
		errorCount += chBoolPrIncIfError(getLineNumber(), true, UTILS.compare1dIntArrs(new int[] {5, 7}, DHSW.getHypGoldLocs()),
			"ERROR: update on hypGoldLocs for forward relocdation following a not-yet-accepted simple deletion hyp not executed properly." ) ; 
		
		//test DHSW.proposedChanges
		// second to last -- is clearly after the deletion @ index 1
			// since deletion is processed first, this moves the previous propCh to index 6
			// meaning it ties with the insertion
			// in case of a tie we add the new proposed change after the old
				// so index is 1
		thepc = DHSW.getProposedChanges().get(1); 
			// should still be as before. 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, "6".equals(thepc[0]) && "deletion".equals(thepc[1]) && "we're Yankees".equals(thepc[2]),
			"ERROR: earlier not-yet-accepted hypothesis change is corrupted by processing of a new change!") ;
		
		//now test processing of the second change, which should consist of one deletion and one insertion.
		// first test the deletion.
		thepc = DHSW.getProposedChanges().get(0); 
		errorCount += chBoolPrIncIfError(getLineNumber(), true, "1".equals(thepc[0]) && "deletion".equals(thepc[1]) && "Relocdated to after first waypoint".equals(thepc[2]) ,
			"ERROR: deletion part of update on proposedChanges for forward relocdation handled incorrectly!") ;
		// and then the insertion phase
		
		thepc = DHSW.getProposedChanges().get(2);
		errorCount += chBoolPrIncIfError(getLineNumber(),  true , "6".equals(thepc[0]) && "[+cor,-delrel] > ɾ / [-cons] __ [-stres]".equals(thepc[1]) && 
			"Relocdated from former step 1".equals(thepc[2]), "ERROR: processing of insertion phase of update on proposedChanges for forward relocdation "
				+"executed incorrectly!"); 
				
		theDHS = DHSW.generateDHS(); 
		
		//checking DHS.ruleCorrespondences
		corrRC = new int[][] { {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, {0, 6, 1, 2, 3, 4, 5, -1, 7, 8, 9}} ; 
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
		int[] corHTG = new int[] {0, 2, 3, 4, 5, 6, 1, 8, 9, 10 }; 
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
				"6[-1|5] : fed or inserted | #bˈʌtə˞# > #bˈʌɾə˞#\n" + 
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
		
		//checking DHS.changedRuleEffects
		//TODO may need to expand coverage to match what we have for previous tests... 
		CREs = theDHS.getChangedRuleEffects();
		errorCount += chBoolPrIncIfError(getLineNumber(), true, CREs.keySet().size() == 4, 
				"ERROR : size of hashmap changedRuleEffects should be 4 but it is"+CREs.keySet().size()) ;
		for (int ri : new int[] {1,2,6,7})
			errorCount += chBoolPrIncIfError(getLineNumber(), true, CREs.containsKey(ri), "ERROR: changedRuleEffects should have a key for global rule "+ri); 
		
		UTILS.errorSummary(errorCount);
		totalErrorCount += errorCount; 
		errorCount = 0; 
		
		//usurp. 
		DHSW.setHypOutLoc(DBG_WRKG_CASC); 
		DHSW.acceptHypothesis(false);
		CASCADE = curHC; 
		curHC = null; dumCasc = null;
		
		//now we we will do the next three rules before usurping
		
		//TODO // complex modification: change t > ʔ / __ ə to : 
			// t > ʔ / [+son] __ {# ; [-cons,-lo,-stres]}
		
		
		
		//TODO add rule processing and debug comprehension of the following
		// again relocate the flapping rule to after waypoint 2 
		// copmlex inserton to right before s > ts / n__ : 
				// n > null / [-cons,+nas] __ {[-son,-cor],[+cons,+son]}
		// finally all things between waypoitns 2 and 3 insert
		// and then check that results are all correct.
	
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
					assert !currRule.contains(""+UTILS.GOLD_STAGENAME_FLAG): 
						"ERROR: stage name flag <<"+UTILS.GOLD_STAGENAME_FLAG+">> occuring in a place besides the first character in the rule line -- this is illegal: \n"+currRule; 
					assert !currRule.contains(UTILS.STAGENAME_LOC_DELIM+""):
						"ERROR: illegal character found in name for custom stage -- <<"+UTILS.STAGENAME_LOC_DELIM+">>";  
					goldStageNameAndLocList.add(""+currRule+UTILS.STAGENAME_LOC_DELIM+rli);
					rulesByTimeInstant.remove(rli);  
				}
				else if (currRule.charAt(0) == UTILS.BLACK_STAGENAME_FLAG)
				{
					blackStagesSet =true;
					currRule = currRule.substring(1); 
					assert !currRule.contains(UTILS.STAGENAME_LOC_DELIM+""):
						"ERROR: illegal character found in name for custom stage -- <<"+UTILS.STAGENAME_LOC_DELIM+">>";  
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
			assert numCols == NUM_GOLD_STAGES + 1: "ERROR: mismatch between number of columns in lexicon file and number of gold stages declared in rules file (plus 1)\n"
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
					"ERROR: tried to declare a phone in a word in the lexicon using an invalid symbol.\nSymbol is : '"+toPhone+"', length = "+toPhone.length(); 
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
		
		return (bi == -1 ? targ.substring(splint) : targ.substring(0,splint)).trim(); 
	}


	private static int chBoolPrIncIfError(int lnNum, boolean targ, boolean obs, String bareErrMsg)
	{
		return UTILS.checkBoolean(targ, obs, "@l"+lnNum+": "+bareErrMsg) ? 0 : 1; 
	}
	
	public static int getLineNumber() {
	    return Thread.currentThread().getStackTrace()[2].getLineNumber();
	}
	
}
