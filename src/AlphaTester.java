import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
		
		initTestBatch(); 
		System.out.println("Testing a feature matrix with one alpha value, without any feature implications (-tense,βhi)..."); 
		FeatMatrix fmtest = new FeatMatrix("-tense,βhi", Arrays.asList(UTILS.featsByIndex)); 
		numCorrect += UTILS.checkBoolean(true, fmtest.getLocalAlphabet().equals("β"), 
				"Error: the local alphabet should be 'β' but instead it is '"+fmtest.getLocalAlphabet()+"'") ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(true, fmtest.has_alpha_specs(),
				"Error: system believes there are no alpha specs, but there is one.") ? 1 : 0 ; 
		char fua = fmtest.first_unset_alpha(); 
		numCorrect += UTILS.checkBoolean(true, fua == 'β',
				"Error: first unset alpha should be 'β', but it is '"+fua+"'") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(false, fmtest.has_multifeat_alpha(), 
				"Error: system detects an alpha variable specified for multiple features, but there is none") ? 1 : 0; 
		
		SChangeFactory testFactory = new SChangeFactory(UTILS.phoneSymbToFeatsMap, UTILS.featIndices); 

		//testing whether featVect is stored properly in the FeatMatrix object instance 
		String corrFeatVect = ""; 
		for(int i = 0; i < UTILS.featsByIndex.length; i++)	corrFeatVect += "1";
		int hi_loc = UTILS.featIndices.get("hi"), tense_loc = UTILS.featIndices.get("tense"); 
		corrFeatVect = corrFeatVect.substring(0, hi_loc) + "β" + corrFeatVect.substring(hi_loc+1); 
		corrFeatVect = corrFeatVect.substring(0, tense_loc) + "0" + corrFeatVect.substring(tense_loc+1);
		String prevFeatVect = fmtest.getFeatVect(); 
		numCorrect += UTILS.checkBoolean(true, corrFeatVect.equals(prevFeatVect), 
				"Error: the feature vector should be\n"+corrFeatVect+"\nbut it is\n"+fmtest.getFeatVect()) ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(false,
				fmtest.comparePreUnsetAlpha(testFactory.parseSeqPh("e")), 
						"Error @"+getLineNumber()+": FM.comparePreUnsetAlpha for [e] should be false for "+fmtest.print()+" but it is mishandled as true.") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true,
				fmtest.comparePreUnsetAlpha(testFactory.parseSeqPh("ɛ")), 
						"Error @"+getLineNumber()+": FM.comparePreUnsetAlpha for [ɛ] should be true for "+fmtest.print()+" but it is mishandled as false.") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(false, fmtest.check_for_alpha_conflict(testFactory.parseSeqPh("w")),
				"Error: [w] should have no alpha conflict, no alph values are set yet, but a conflict is detected") ? 1 : 0; 
		SequentialPhonic dummyPhone = testFactory.parseSeqPh("m"); // which is -hi, 0tense.
		String initSpecs = ""+fmtest;
		HashMap<String, String> alph_feats_extrd = fmtest.extractAndApplyAlphaValues(dummyPhone); 
		int n_feats_extracted = alph_feats_extrd.keySet().size(); 
		numCorrect += UTILS.checkBoolean(true, 
				n_feats_extracted == 0, 
				"Error: there should be zero features extracted from ["+dummyPhone.print()+"] since tense is not specified for consonantals, "
				+ "but "+n_feats_extracted+" were extracted!" ) ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, 
				prevFeatVect.equals(fmtest.getFeatVect()),
				"Error: the feat vect should have been unchanged but it has changed from\n"+prevFeatVect+"\nto\n"+fmtest.getFeatVect()) 
				? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, initSpecs.equals(""+fmtest), 
				"Error: feat specs should have been unchanged but it was changed from\n"+initSpecs+"\nto\n"+fmtest) ? 1 : 0; 
		HashMap<String,String> dummyHM = new HashMap<String, String>(); 
		dummyHM.put("β",  UTILS.MARK_NEG+"");  
		fmtest.applyAlphaValues(dummyHM); 
		numCorrect += UTILS.checkBoolean(true, fmtest.getFeatVect().equals(corrFeatVect.substring(0, hi_loc) + UTILS.NEG_INT + corrFeatVect.substring(hi_loc+1)), 
				"Error @"+getLineNumber()+" FM.applyAlphaValues() did not produce right change in feature vector") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(false,
				fmtest.comparePreUnsetAlpha(testFactory.parseSeqPh("ɛ")), 
						"Error @"+getLineNumber()+": FM.comparePreUnsetAlpha for [ɛ should now (tangentially) be false for "+fmtest.print()+" given that β was set to [-] but it is mishandled as true.") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true,
				fmtest.check_for_alpha_conflict(testFactory.parseSeqPh("ɛ")), 
						"Error @"+getLineNumber()+": FM.check_for_alpha_conflict for [ɛ] should now be true for "+fmtest.print()+" given that β was set to [-] but it is mishandled as false.") ? 1 : 0; 
		
		fmtest.resetAlphVal('β'); 
		numCorrect += UTILS.checkBoolean(true, fmtest.getFeatVect().equals(corrFeatVect), 
				"Error @"+getLineNumber()+" FM.resetAlphVal did not produce right change in feature vector") ? 1 : 0; 
		fmtest.setAlphaValue("β", ""+UTILS.MARK_POS); 
		numCorrect += UTILS.checkBoolean(true, fmtest.getFeatVect().equals(corrFeatVect.substring(0, hi_loc) + UTILS.POS_INT + corrFeatVect.substring(hi_loc+1)), 
				"Error @"+getLineNumber()+" FM.setAlphaValue() did not produce right change in feature vector") ? 1 : 0; 
		fmtest.resetAlphaValues(); 
		numCorrect += UTILS.checkBoolean(true, fmtest.getFeatVect().equals(corrFeatVect), 
				"Error @"+getLineNumber()+" FM.resetAlphVal did not produce right feature vector") ? 1 : 0; 
		concludeTestBatch(); 

		System.out.println("Test FM w single alph feat w implications (check that code-commented below is covered: "); 
		System.out.println("Now testing an FM wit implicaitons...");		
		// now testing application of alpha feature filling to a FeatMatrix with [βtense], which will show handling of downstream feature implications 
			// namely: tense:-cons (an any-specification scenario)
				// [-cons] has downstream implications: -lat,+cont
					// [+cont] itself has a downstream implication: [0delrel]
		FeatMatrix dummyFM = new FeatMatrix("βtense", Arrays.asList(UTILS.featsByIndex)); 
		String dfm_og_vect = ""+dummyFM.getFeatVect(), dfm_og_specs = ""+dummyFM; 
		
		//TODO debugging
		System.out.println("vect : "+dfm_og_vect);
		System.out.println("feats extr'd : "+alph_feats_extrd);
		
		dummyFM.applyAlphaValues(alph_feats_extrd);

		numCorrect += UTILS.checkBoolean(true, dummyFM.first_unset_alpha() == '0', 
				"Error: after application of alpha values to only alpha value, it erroneously does not count as unset") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(false, 
				dfm_og_vect.equals(dummyFM.getFeatVect()), 
				"Error: feature vector remained unchanged after application of alpha values.") ?  1 : 0; 

		corr_dfm_vect = featVectChange(""+dfm_og_vect, "0tense,0cons,0lat,2cont,9delrel"); 
		numCorrect += UTILS.checkBoolean(true, corr_dfm_vect.equals(dummyFM.getFeatVect()), 
				"Error: the feature vector after alpha feature filling should be\n"+corr_dfm_vect+
				"\nbut it is\n"+dummyFM.getFeatVect()) ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(false, dfm_og_specs.equals(""+dummyFM), 
				"Error: feature specs remained unchanged after application of alpha values.") ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(true, dummyFM.toString().equals(""+(newFM("-tense"))), 
				"Error: feature specs should be [-tense], but it is "+dummyFM) ? 1 : 0 ; 

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
		pointTest("a", String.join(";", filtTester.getLocalAlphSpecs().keySet()), "(@line "+getLineNumber()+") alphas should include %c but detected { %o }"); 
		pointTest("a", String.join(";", filtTester.getLocalAlphLocs().keySet()), "(@line "+getLineNumber()+") localAlphLocs keys should include %c but detected { %o }"); 
		pointTest("1,3",
				filtTester.getLocalAlphLocs().get("a"),
				"(@line "+getLineNumber()+") a's locations should include %c but we see [ %o ]"); 
		pointTest("1,2",
				filtTester.getPlaceRestrLocsWithAlpha("a"), 
				"(@line "+getLineNumber()+") a's locations in placeRestrs should include %c but we see [ %o ]"); 
		
		pointTest(true, filtTester.hasParenthesizedAlpha(), "(@line "+getLineNumber()+") hasParenthesizedAlpha should be %c but isn't"); 
		pointTest("a", String.join(";", filtTester.getParenthesizedAlphas()), "(@line "+getLineNumber()+") parenthesized alphas should be just %c but detected %o"); 
		pointTest(",a,,(a,,",
				filtTester.getParenAlphaMap(),
				"(@line "+getLineNumber()+") parenAlphaMap should be [ %c ] but it is [ %o ]"); 
		pointTest(false, 
				filtTester.alphaOnlyInParentheses("a"),
				"(@line "+getLineNumber()+") a is not only stipulated in parenthesized locations, but it is errantly detected as such"); 
		
		
		pointTest(true, filtTester.has_unset_alphas(), "(@line "+getLineNumber()+") errantly thought alphas prematurely set"); 
		pointTest(true, filtTester.has_unset_paren_alphas(), "(@line "+getLineNumber()+") errantly thought parenthesized alphas prematurely set"); 
		pointTest("["+SequentialFilter.UNSET_ALPHVAL+"]","["+String.join(",", filtTester.getLocalAlphSpecs().values())+"]",
				"(@line "+getLineNumber()+") localAlphSpecs should be unset but instead we see %o"); 
		
		List<SequentialPhonic> shouldPass1 = testFactory.parseSeqPhSeg("# ɑ ɛ #"); 
		List<SequentialPhonic> shouldPass2 = testFactory.parseSeqPhSeg("# u #"); 
		List<SequentialPhonic> shouldPass3 = testFactory.parseSeqPhSeg("# ˈo #"); 
		List<SequentialPhonic> shouldPass4 = testFactory.parseSeqPhSeg("# ˈo ˌa #"); 

		HashMap<String,String> extraction = (new FeatMatrix("+back,astres",featNames)).extractAndApplyAlphaValues(shouldPass1.get(1)); 
		pointTest("a", String.join(",", extraction.keySet()), "should have extracted for alph feat a but instead we see : %o"); 
		pointTest("0", extraction.get("a"), "extracted value should be %c but we see %o"); 
		filtTester.applyAlphaValues(extraction);
		pointTest(false, filtTester.has_unset_alphas(), "(@line "+getLineNumber()+") errantly thought alphas still unset after being applied"); 
		pointTest(false, filtTester.has_unset_paren_alphas(), "(@line "+getLineNumber()+") errantly thought parenthesized alphas unset after being applied"); 
		pointTest("[0]","["+String.join(",", filtTester.getLocalAlphSpecs().values())+"]",
				"(@line "+getLineNumber()+") localAlphSpecs should be %c but instead we see %o"); 
		
		filtTester.resetAllAlphaValues(); 
		pointTest(true, filtTester.has_unset_alphas(), "(@line "+getLineNumber()+") errantly thought alphas set after being reset"); 
		pointTest(true, filtTester.has_unset_paren_alphas(), "(@line "+getLineNumber()+") errantly thought parenthesized alphas set after being reset"); 
		pointTest("["+SequentialFilter.UNSET_ALPHVAL+"]","["+String.join(",", filtTester.getLocalAlphSpecs().values())+"]",
				"(@line "+getLineNumber()+") localAlphSpecs should be %c after reset but instead we see %o"); 
		
		extraction = filtTester.getPlaceRestrs().get(1).extractAndApplyAlphaValues(shouldPass4.get(1)); 
		pointTest("a", String.join(",", extraction.keySet()), "should have extracted for alph feat a but instead we see : %o"); 
		pointTest("2", extraction.get("a"), "extracted value should be %c but we see %o"); 
		filtTester.applyAlphaValues(extraction);
		pointTest(false, filtTester.has_unset_alphas(), "(@line "+getLineNumber()+") errantly thought alphas still unset after being applied"); 
		pointTest(false, filtTester.has_unset_paren_alphas(), "(@line "+getLineNumber()+") errantly thought parenthesized alphas unset after being applied"); 
		pointTest("[2]","["+String.join(",", filtTester.getLocalAlphSpecs().values())+"]",
				"(@line "+getLineNumber()+") localAlphSpecs should be %c but instead we see %o"); 
		filtTester.resetTheseAlphaValues(Arrays.asList(new String[] {"a"})); 
		pointTest(true, filtTester.has_unset_alphas(), "(@line "+getLineNumber()+") errantly thought alphas set after being reset"); 
		pointTest(true, filtTester.has_unset_paren_alphas(), "(@line "+getLineNumber()+") errantly thought parenthesized alphas set after being reset"); 
		pointTest("["+SequentialFilter.UNSET_ALPHVAL+"]","["+String.join(",", filtTester.getLocalAlphSpecs().values())+"]",
				"(@line "+getLineNumber()+") localAlphSpecs should be %c after reset but instead we see %o"); 
		
		filtCheckCheck(filtTester, shouldPass1, true, getLineNumber()); 
		filtCheckCheck(filtTester, shouldPass2, true, getLineNumber()); 
		filtCheckCheck(filtTester, shouldPass3, true, getLineNumber()); 
		filtCheckCheck(filtTester, shouldPass4, true, getLineNumber()); 
		
		List<SequentialPhonic> shouldFail1 = testFactory.parseSeqPhSeg("u b u #"); 
		List<SequentialPhonic> shouldFail2 = testFactory.parseSeqPhSeg("# u ˈa #"); 
		List<SequentialPhonic> shouldFail3 = testFactory.parseSeqPhSeg("# i #"); 
		List<SequentialPhonic> shouldFail4 = testFactory.parseSeqPhSeg("# u ˈa"); 
		List<SequentialPhonic> shouldFail5 = testFactory.parseSeqPhSeg("# ˈu o #"); 
		
		filtCheckCheck(filtTester, shouldFail1, false, getLineNumber()); 
		filtCheckCheck(filtTester, shouldFail2, false, getLineNumber()); 
		filtCheckCheck(filtTester, shouldFail3, false, getLineNumber()); 
		filtCheckCheck(filtTester, shouldFail4, false, getLineNumber()); 
		filtCheckCheck(filtTester, shouldFail5, false, getLineNumber()); 


		// using shouldPass# variables... 
			// filtCheck
			// filtCheckHelper
		concludeTestBatch(); 
		
		initTestBatch();
		System.out.println("Testing parenthesisLocalAlphas..."); 
		filtTester =  testFactory.parseNewSeqFilter("# [astres,+syl,dhi] ([acons,bround] [bround,chi] ([chi,flab,fround])) @ ([dcor,elat,econt])* m #", true); 
		pointTest("b,c", String.join(",", filtTester.parenthesisLocalAlphas(2)), "(@line "+getLineNumber()+") local parens here should be %c but we get %o");
		pointTest("b,c", String.join(",", filtTester.parenthesisLocalAlphas(8)), "(@line "+getLineNumber()+") local parens here should be %c but we get %o");
		pointTest("f", String.join(",", filtTester.parenthesisLocalAlphas(5)), "(@line "+getLineNumber()+") local parens here should be %c but we get %o");
		pointTest("f", String.join(",", filtTester.parenthesisLocalAlphas(7)), "(@line "+getLineNumber()+") local parens here should be %c but we get %o");
		pointTest("e", String.join(",", filtTester.parenthesisLocalAlphas(10)), "(@line "+getLineNumber()+") local parens here should be %c but we get %o");
		pointTest("e", String.join(",", filtTester.parenthesisLocalAlphas(12)), "(@line "+getLineNumber()+") local parens here should be %c but we get %o");
		
		shouldPass1 = testFactory.parseSeqPhSeg("# ˈa kʷ w a p k m #"); 
		shouldPass2 = testFactory.parseSeqPhSeg("# ˈa kʷ w i a p k m #"); 
		shouldPass3 = testFactory.parseSeqPhSeg("# a ɥ w a p k m #"); 
		List<SequentialPhonic> shouldFail0 = testFactory.parseSeqPhSeg("# ˈa kʷ w a p k a m # ");
		shouldFail1 = testFactory.parseSeqPhSeg("# ˈa kʷ w a p k m"); 
		shouldFail2 = testFactory.parseSeqPhSeg("# a kʷ w a p k m #");
		shouldFail3 = testFactory.parseSeqPhSeg("# ˈa k w a p k m #");
		shouldFail4 = testFactory.parseSeqPhSeg("# ˈa kʷ w a p t m #"); 
		shouldPass4 = testFactory.parseSeqPhSeg("# ˈu t r e a l ɾ l m #"); 
		shouldFail5 = testFactory.parseSeqPhSeg("# ˈu t r e a l s l m #"); 
		List<SequentialPhonic> shouldFail6 = testFactory.parseSeqPhSeg("# ˈu t r b a l ɾ l m #"); 
		List<SequentialPhonic> shouldPass6 = testFactory.parseSeqPhSeg("# ˈu t r b # a l m #"); 


		filtCheckCheck(filtTester, shouldPass1, true, getLineNumber()); 
		filtCheckCheck(filtTester, shouldPass2, true, getLineNumber()); 
		filtCheckCheck(filtTester, shouldPass3, true, getLineNumber()); 
		filtCheckCheck(filtTester, shouldPass4, true, getLineNumber()); 
		filtCheckCheck(filtTester, shouldPass6, true, getLineNumber()); 

		filtCheckCheck(filtTester, shouldFail0, false, getLineNumber()); 
		filtCheckCheck(filtTester, shouldFail1, false, getLineNumber()); 
		filtCheckCheck(filtTester, shouldFail2, false, getLineNumber()); 
		filtCheckCheck(filtTester, shouldFail3, false, getLineNumber()); 
		filtCheckCheck(filtTester, shouldFail4, false, getLineNumber()); 
		filtCheckCheck(filtTester, shouldFail5, false, getLineNumber()); 
		filtCheckCheck(filtTester, shouldFail6, false, getLineNumber());
		filtCheckCheck(filtTester, testFactory.parseSeqPhSeg("ˈa kʷ w a p k a a a a a m # "), false, getLineNumber());
		
		concludeTestBatch(); 
		
		
		// ------ neg alpha testing begins here -------
		System.out.println("Beginning testing of neg and other alpha coverage in UTILS..."); 
		
		System.out.println("Testing spec alpha detection...");
		initTestBatch(); 
		
		String spuriousAlphMsg = "Spurious detection of alpha marking on feature" , undetectedAlphMsg="Failed to detect alpha marking on feature"; 
		pointTest(false,UTILS.spec_is_alpha_marked("+voi"), "Spurious detection of alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_alpha_marked("-son"), "Spurious detection of alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_alpha_marked("0sg"), "Spurious detection of alpha marking on feature"); 
		pointTest(true,UTILS.spec_is_alpha_marked("βvoi"), "Failed to detect alpha marking on feature"); 
		pointTest(true,UTILS.spec_is_alpha_marked("+ɣhi"), "Failed to detect alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_preposed_alpha_marked("+voi", '+'), "Spurious detection of alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_preposed_alpha_marked("-son",'-'), "Spurious detection of alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_preposed_alpha_marked("0sg",'0'), "Spurious detection of alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_preposed_alpha_marked("βnas",'+'), "Spurious detection of alpha marking on feature"); 
		pointTest(true,UTILS.spec_is_preposed_alpha_marked("+ɣhi",'+'), "Failed to detect alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_preposed_alpha_marked("+ɣhi",'-'), spuriousAlphMsg); 
		pointTest(true,UTILS.spec_is_preposed_alpha_marked("-ðcons", '-'), undetectedAlphMsg); 
		pointTest(true,UTILS.spec_is_preposed_alpha_marked("0ðdistr", '0'), undetectedAlphMsg); 
		pointTest(true,UTILS.spec_is_neg_alpha_marked("-ðcons"), undetectedAlphMsg); 
		pointTest(false,UTILS.stringHasFMWithAlpha("h > ∅ / __ #"), "spurious detection of alpha in string"); 
		pointTest(false,UTILS.stringHasFMWithNegAlpha("h > ∅ / __ #"), "spurious detection of neg alpha in string @"+getLineNumber()); 
		pointTest(false,UTILS.stringHasFMWithAlpha("h > ∅ / __ [-cons] "), "spurious detection of alpha in string @"+getLineNumber());  
		pointTest(false,UTILS.stringHasFMWithNegAlpha("h > ∅ / __ [-cons] "), "spurious detection of neg alpha in string @"+getLineNumber());  
		pointTest(true,UTILS.stringHasFMWithAlpha("h > [avoi] / [-cons] __ [acons] "), "missed detection of alpha in string @"+getLineNumber());  
		pointTest(false,UTILS.stringHasFMWithNegAlpha("h > [avoi] / [-cons] __ [acons] "), "spurious detection of neg alpha in string @"+getLineNumber());  
		pointTest(true,UTILS.stringHasFMWithNegAlpha("h > b ɹ ʌː / [-acons] __ [acons] "), "missed detection of neg alpha in string @"+getLineNumber());  
		pointTest(true,UTILS.stringHasFMWithNegAlpha("h > b ɹ ʌː / [-acons] __ [+cons] "), "missed detection of neg alpha in string @"+getLineNumber());  

		concludeTestBatch(); 
		
		
		

	}
	
	private static void initTestBatch()
	{	numCorrect = 0; totalChecks = 0; 	}
	private static void concludeTestBatch()
	{
		UTILS.errorSummary(totalChecks - numCorrect);
		numCorrect = 0; totalChecks = 0; 
	}

	private static void filtCheckCheck(SequentialFilter filt, List<SequentialPhonic> candidate, boolean passable, int ln)
	{
		pointTest(passable, 
				filt.filtCheck(candidate, true),
				"(@line "+ln+") "+UTILS.printWord(candidate)+" should "+(passable ? "pass" : "fail")+" but it doesn't."); 
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
	private static void pointTest(List<Integer> corr, List<Integer> obs, String msg)
	{	pointTest(corr.toArray(), obs.toArray(), msg); }
	private static void pointTest(String corr, List<Integer> obs, String msg) // corr format [#,#,# ..]
	{	
		String[] corr_spl = corr.split(","); 
		
		List<Integer> corrToPass = new ArrayList<Integer>(); 
		for (int csi = 0 ; csi < corr_spl.length; csi++) 
			corrToPass.add(Integer.parseInt(corr_spl[csi])); 
		pointTest(corrToPass, obs, msg); }
	
	// @corr format: cellA,cellB... 
	private static void pointTest(String corr, String[] obs, String msg)
	{	pointTest("["+corr+"]", "["+String.join(",", obs)+"]", msg); 	}
	
	private static void pointTest(Object[] corr, Object[] obs, String msg)
	{	pointTest("["+UTILS.print1dArr(corr)+"]", "["+UTILS.print1dArr(obs)+"]", msg); 	}
	private static void pointTest(int[] corr, int[] obs, String msg)
	{	pointTest("["+UTILS.print1dIntArr(corr)+"]", "["+UTILS.print1dIntArr(obs)+"]", msg); 	}
	// corr format: #,# 
	private static void pointTest(String corr, int[] obs, String msg)
	{	String[] corr_spl = corr.split(","); 
		
		int[] corrToPass = new int[corr_spl.length]; 
		for (int csi = 0 ; csi < corr_spl.length; csi++) corrToPass[csi] = Integer.parseInt(corr_spl[csi]); 
	
		pointTest(corrToPass, obs, msg); }
	
	public static int getLineNumber() {
	    return Thread.currentThread().getStackTrace()[2].getLineNumber();
	}
	
	
}
