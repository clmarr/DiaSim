import java.util.Arrays;
import java.util.List;

public class AlphaTester {

	private static String featImplsLoc = "FeatImplications", symbDefsLoc = "symbolDefs.csv", lexFileLoc="DebugDummyLexicon.txt";
	private static List<String> featNames; 

	private static int numCorrect, totalChecks; 
	
	public static void main (String args[])
	{
		
		System.out.println("Collecting symbol definitions...");
		List<String> symbDefsLines = UTILS.readFileLines(symbDefsLoc);
		UTILS.extractSymbDefs(symbDefsLines); 
		featNames = Arrays.asList(UTILS.featsByIndex);
		System.out.println("Now extracting info from feature implications file...");
		UTILS.extractFeatImpls(featImplsLoc);
		System.out.println("Done extracting feature implications!");
		
		System.out.println("----------------------");
		String[] feats = UTILS.featsByIndex; 

		FeatMatrix nasalStop = new FeatMatrix("+nas,+cont,+son,0delrel", Arrays.asList(feats)); 


		System.out.println("Now testing functionality of alpha feature handling within FeatMatrix, without any alpha feature specification...");
		initTestBatch(); 
		
		System.out.println("(no alpha feature specification, testing for errant detection thereof)"); 
		
		pointTest(false, 
				nasalStop.has_alpha_specs(), 
				"FeatMatrix.has_alpha_specs() should be %c with no alpha specs but it gives %o"); 
		pointTest(false, 
				nasalStop.has_multifeat_alpha(), 
				"FeatMatrix.has_multifeat_alpha() should be %c with no alpha specs but it gives %o"); 
		pointTest("0", ""+nasalStop.first_unset_alpha(), 
				"FeatMatrix.first_unset_alpha should give '0' w/o alpha setting, but it gives '%o'");
		pointTest("0", ""+nasalStop.getAlphaVars().size(),  
				"FeatMatrix.getAlphaVars() should have size %c w/o alpha setting, but we see %o (local alphabet : "+nasalStop.getLocalAlphabet()+")"); 
		
		concludeTestBatch(); 
		
		//TODO draw from SChangeTester , SChangeContextTester as appropriate
		System.out.println("//TODO draw from SChangeTester , SChangeContextTester as appropriate"); 
		System.out.println("Test FM w single alph feat for the code-commented below: "); 
			// has_alpha_specs
			// has_multifeat_alpha
			// first_unset_alpha
			// getAlphaVars
			// comparePreAlpha
			// resetAlphaValues
			// resetAlphVal
			// setAlphaValue
			// applyAlphaValues
			// check_for_alpha_conflict
			// extractAndApplyAlphaVlaues
		System.out.println("Test FM w single alph feat w implications for the code-commented below: "); 
					// has_alpha_specs
					// has_multifeat_alpha
					// first_unset_alpha
					// getAlphaVars
					// comparePreAlpha
					// resetAlphaValues
					// resetAlphVal
					// setAlphaValue
					// applyAlphaValues
					// check_for_alpha_conflict
					// extractAndApplyAlphaVlaues
		System.out.println("Test FM w multiple separate variable alph feats (w implications) for the code-commented below: "); 
					// has_alpha_specs
					// has_multifeat_alpha
					// first_unset_alpha
					// getAlphaVars
					// comparePreAlpha
					// resetAlphaValues
					// resetAlphVal
					// setAlphaValue
					// applyAlphaValues
					// check_for_alpha_conflict
					// extractAndApplyAlphaVlaues
		System.out.println("Test FM w multiple-specified single alpha feat (w implications) for the code-commented below: "); 

			// has_alpha_specs
			// has_multifeat_alpha
			// first_unset_alpha
			// getAlphaVars
			// comparePreAlpha
			// resetAlphaValues
			// resetAlphVal
			// setAlphaValue
			// applyAlphaValues
			// check_for_alpha_conflict
			// extractAndApplyAlphaVlaues
		
		SChangeFactory testFactory = new SChangeFactory(UTILS.phoneSymbToFeatsMap, UTILS.featIndices); 

		System.out.println("TODO testing SequentialFitler"); 
		// TODO SequentialFilter testing...  method: 
			// hasAlphaSpecs
			// hasParenthesizedAlpha
			// parenthesizedAlphas
			// parenAlphaMap
			// localAlphSpecs keys
			// localAlphLocs
			// getPlaceRestrLocsWithAlpha
			// alphasOnlyInParentheses(String alph)
		
			// before setting alphas ...  -- i.e. after initAlpha()
				// has_unset_alphas
				// has_unset_paren_alphas 
				// localAlphSpecs values
			
			// applyAlphaValues, then afterward ... 
				// has_unset_alphas
				// has_unset_paren_alphas
				// localAlphSpecs values
		
			// after resetAllAlphaValues
				// has_unset_alphas
				// has_unset_paren_alphas 
				// localAlphSpecs values
		
			// after resetTheseAlphaValues
				// has_unset_alphas
				// has_unset_paren_alphas 
				// localAlphSpecs values
		
			// filtCheck
			// filtCheckHelper
		
		initTestBatch(); 
		System.out.println("Testing: # [+back,astres] ([astres]) #");
		SequentialFilter filtTester =  testFactory.parseNewSeqFilter("# [+back,astres] ([astres]) #", false);
		pointTest(true, filtTester.hasAlphaSpecs(), 
				"(@line "+getLineNumber()+") hasAlphaSpecs should be %c for this SeqFilt but it isn't"); 
		pointTest(true, filtTester.hasParenthesizedAlpha(), "(@line "+getLineNumber()+") hasParenthesizedAlpha should be %c but isn't"); 
		pointTest("a", String.join(";", filtTester.getParenthesizedAlphas()), "(@line "+getLineNumber()+") parenthesized alphas should be just %c but detected %o"); 
		
		pointTest(true, filtTester.has_unset_alphas(), "(@line "+getLineNumber()+") errantly thought alphas prematurely set"); 
		pointTest(true, filtTester.has_unset_paren_alphas(), "(@line "+getLineNumber()+") errantly thought parenthesized alphas prematurely set"); 
		

			// localAlphLocs
			// getPlaceRestrLocsWithAlpha
			// alphasOnlyInParentheses(String alph)
		
			// before setting alphas ...  -- i.e. after initAlpha()
				// localAlphSpecs
			
			// applyAlphaValues, then afterward ... 
				// has_unset_alphas
				// has_unset_paren_alphas
				// localAlphSpecs values
		
			// after resetAllAlphaValues
				// has_unset_alphas
				// has_unset_paren_alphas 
				// localAlphSpecs values
		
			// after resetTheseAlphaValues
				// has_unset_alphas
				// has_unset_paren_alphas 
				// localAlphSpecs values
		
			// filtCheck
			// filtCheckHelper
		concludeTestBatch(); 

	}
	
	private static void initTestBatch()
	{	numCorrect = 0; totalChecks = 0; 	}
	private static void concludeTestBatch()
	{
		UTILS.errorSummary(totalChecks - numCorrect);
		numCorrect = 0; totalChecks = 0; 
	}

	// string equation vsn. 
	private static void pointTest(String corr, String obs, String msg) 
	{
		totalChecks++; 
		numCorrect += UTILS.checkBoolean(true, corr.equals(obs), UTILS.errorMessage(corr, obs, msg))
				? 1 : 0;	
	}
	private static void pointTest(boolean corr, boolean obs, String msg) 
	{
		totalChecks++; 
		numCorrect += UTILS.checkBoolean(corr, obs, UTILS.errorMessage(""+corr, ""+obs, msg))
				? 1 : 0;	
	}
	
	public static int getLineNumber() {
	    return Thread.currentThread().getStackTrace()[2].getLineNumber();
	}
	
	
}
