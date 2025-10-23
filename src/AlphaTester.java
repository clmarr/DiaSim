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

		UTILS.extractDiacriticMap("currentSymbolDiacriticDefs.txt");

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
		pointTest("", ""+nasalStop.getLocalAlphabet(), 
				"FeatMatrix.getLocalAlphabet() should be empty as there are no alphas, but it is "+nasalStop.getLocalAlphabet()); 
		pointTest(false, nasalStop.hasNegProxyAlphs(), 
				"FeatMatrix.hasNegProxyAlphs errantly detected as true when there are no alphas at all!"); 
		pointTest(true, nasalStop.getNegProxyAlphs().size()==0, 
				"FeatMatrix.getNegProxyAlphs accidentally filled but it should be empty as there aren't even any alphas !"); 
		
		concludeTestBatch(); 
		
		initTestBatch(); 
		System.out.println("Testing a feature matrix with one alpha value, without any feature implications (-tense,βhi)..."); 
		FeatMatrix fmtest = new FeatMatrix("-tense,βhi", Arrays.asList(UTILS.featsByIndex)); 
		pointTest(true, fmtest.getLocalAlphabet().equals("β"), 
				"Error: the local alphabet should be 'β' but instead it is '"+fmtest.getLocalAlphabet()+"'"); 
		pointTest(true, fmtest.has_alpha_specs(),
				"Error: system believes there are no alpha specs, but there is one."); 
		char fua = fmtest.first_unset_alpha(); 
		pointTest(true, fua == 'β',
				"Error: first unset alpha should be 'β', but it is '"+fua+"'"); 
		pointTest(false, fmtest.has_multifeat_alpha(), 
				"Error: system detects an alpha variable specified for multiple features, but there is none"); 
		pointTest(false, fmtest.hasNegProxyAlphs(), 
				"FeatMatrix.hasNegProxyAlphs errantly detected as true when there are no neg alphas!"); 
		pointTest(true, fmtest.getNegProxyAlphs().size()==0, 
				"FeatMatrix.getNegProxyAlphs accidentally filled but it should be empty as there aren't any neg alphas !"); 
		
		SChangeFactory testFactory = new SChangeFactory(UTILS.phoneSymbToFeatsMap, UTILS.featIndices); 

		//testing whether featVect is stored properly in the FeatMatrix object instance 
		String corrFeatVect = ""; 
		for(int i = 0; i < UTILS.featsByIndex.length; i++)	corrFeatVect += "1";
		int hi_loc = UTILS.featIndices.get("hi"), tense_loc = UTILS.featIndices.get("tense"); 
		corrFeatVect = corrFeatVect.substring(0, hi_loc) + "β" + corrFeatVect.substring(hi_loc+1); 
		corrFeatVect = corrFeatVect.substring(0, tense_loc) + "0" + corrFeatVect.substring(tense_loc+1);
		String prevFeatVect = fmtest.getFeatVect(); 
		pointTest(true, corrFeatVect.equals(prevFeatVect), 
				"Error: the feature vector should be\n"+corrFeatVect+"\nbut it is\n"+fmtest.getFeatVect()); 
		pointTest(false,
				fmtest.comparePreUnsetAlpha(testFactory.parseSeqPh("e")), 
						"Error @"+getLineNumber()+": FM.comparePreUnsetAlpha for [e] should be false for "+fmtest+" but it is mishandled as true."); 
		pointTest(true,
				fmtest.comparePreUnsetAlpha(testFactory.parseSeqPh("ɛ")), 
						"Error @"+getLineNumber()+": FM.comparePreUnsetAlpha for [ɛ] should be true for "+fmtest+" but it is mishandled as false.");
		pointTest(false, fmtest.check_for_alpha_conflict(testFactory.parseSeqPh("w")),
				"Error: [w] should have no alpha conflict, no alph values are set yet, but a conflict is detected");
		SequentialPhonic dummyPhone = testFactory.parseSeqPh("m"); // which is -hi, 0tense.
		String initSpecs = ""+fmtest;
		HashMap<String, String> alph_feats_extrd = fmtest.extractAndApplyAlphaValues(dummyPhone); 
		int n_feats_extracted = alph_feats_extrd.keySet().size(); 
		pointTest(true, 
				n_feats_extracted == 0, 
				"Error: there should be zero features extracted from ["+dummyPhone.print()+"] since tense is not specified for consonantals, "
				+ "but "+n_feats_extracted+" were extracted!" ); 
		pointTest(true, 
				prevFeatVect.equals(fmtest.getFeatVect()),
				"Error: the feat vect should have been unchanged but it has changed from\n"+prevFeatVect+"\nto\n"+fmtest.getFeatVect()); 
		pointTest(true, initSpecs.equals(""+fmtest), 
				"Error: feat specs should have been unchanged but it was changed from\n"+initSpecs+"\nto\n"+fmtest); 
		HashMap<String,String> dummyHM = new HashMap<String, String>(); 
		dummyHM.put("β", UTILS.NEG_INT+"");  
		fmtest.applyAlphaValues(dummyHM); 
		pointTest(true, fmtest.getFeatVect().equals(corrFeatVect.substring(0, hi_loc) + UTILS.NEG_INT + corrFeatVect.substring(hi_loc+1)), 
				"Error @"+getLineNumber()+" FM.applyAlphaValues() did not produce right change in feature vector") ; 
		pointTest(false,
				fmtest.comparePreUnsetAlpha(testFactory.parseSeqPh("i")), 
						"Error @"+getLineNumber()+": FM.comparePreUnsetAlpha for [i] should now (tangentially) be false for "+fmtest+" given that β was set to [-] but it is mishandled as true.") ; 
		pointTest(false,
				fmtest.comparePreUnsetAlpha(testFactory.parseSeqPh("ɪ")), 
						"Error @"+getLineNumber()+": FM.comparePreUnsetAlpha for [ɪ] should now (tangentially) be false for "+fmtest+" given that β was set to [-] but it is mishandled as true.") ; 
		
		fmtest.resetAlphVal('β'); 
		pointTest(true, fmtest.getFeatVect().equals(corrFeatVect), 
				"Error @"+getLineNumber()+" FM.resetAlphVal did not produce right change in feature vector") ; 
		fmtest.setAlphaValue("β", ""+UTILS.POS_INT); 
		pointTest(true, fmtest.getFeatVect().equals(corrFeatVect.substring(0, hi_loc) + UTILS.POS_INT + corrFeatVect.substring(hi_loc+1)), 
				"Error @"+getLineNumber()+" FM.setAlphaValue() did not produce right change in feature vector") ; 
		fmtest.resetAlphaValues(); 
		pointTest(true, fmtest.getFeatVect().equals(corrFeatVect), 
				"Error @"+getLineNumber()+" FM.resetAlphVal did not produce right feature vector") ; 

		
		System.out.println("Now testing an FM with implications...");		
		System.out.println("Test FM w single alph feat [βtense] w implications (check that code-commented below is covered: "); 
		// now testing application of alpha feature filling to a FeatMatrix with [βtense], which will show handling of downstream feature implications 
			// namely: tense:-cons (an any-specification scenario)
				// [-cons] has downstream implications: -lat,+cont
					// [+cont] itself has a downstream implication: [0delrel]
		FeatMatrix dummyFM = new FeatMatrix("βtense", Arrays.asList(UTILS.featsByIndex)); 
		String dfm_og_vect = ""+dummyFM.getFeatVect(), dfm_og_specs = ""+dummyFM; 
		
		alph_feats_extrd = fmtest.extractAndApplyAlphaValues(testFactory.parseSeqPh("ʌ")); //(β>-)hi, -tense 
		
		dummyFM.applyAlphaValues(alph_feats_extrd);
		pointTest(false, fmtest.has_multifeat_alpha(), 
				"Error: system detects an alpha variable specified for multiple features, but there is none") ; 
		pointTest(true, dummyFM.first_unset_alpha() == '0', 
				"Error: after application of alpha values to only alpha value, it erroneously does not count as unset") ; 
		pointTest(false, 
				dfm_og_vect.equals(dummyFM.getFeatVect()), 
				"Error: feature vector remained unchanged after application of alpha values.") ;
		String corr_dfm_vect = UTILS.featVectChange(""+dfm_og_vect, "0tense,0cons,0lat,2cont,9delrel"); 
		pointTest(true, corr_dfm_vect.equals(dummyFM.getFeatVect()), 
				"Error: the feature vector after alpha feature filling should be\n"+corr_dfm_vect+
				"\nbut it is\n"+dummyFM.getFeatVect())  ; 
		pointTest(false, dfm_og_specs.equals(""+dummyFM), 
				"Error: feature specs remained unchanged after application of alpha values.")  ; 
		pointTest(true, dummyFM.toString().equals(""+(SChangeTester.newFM("-tense"))), 
				"Error @"+getLineNumber()+": feature specs should be [-tense], but it is "+dummyFM)  ; 
					// resetAlphaValues
					// resetAlphVal
					// setAlphaValue
		
		// now testing effect on feature matrix with two different alpha symbols, each specifying one feature: 
		//  β for hi, and ɸ for tense... 
		System.out.println("Now testing feature matrix with two diff alpha symbols, each specifying one feature, one of which with implications. ");
		dummyFM = SChangeTester.newFM("βhi,ɸtense"); 
		dfm_og_vect = ""+dummyFM.getFeatVect(); dfm_og_specs = ""+dummyFM; 
		dummyFM.applyAlphaValues(alph_feats_extrd);
		

		pointTest(false, fmtest.has_multifeat_alpha(), 
				"Error: system detects an alpha variable specified for multiple features, but there is none") ; 
		pointTest(true, dummyFM.first_unset_alpha() == 'ɸ', 
				"Error: after application of filling alpha value β to feat matrix with β and ɸ in its alphabet, "
				+ "the first (and only) unset alpha should be 'ɸ' but it is "+dummyFM.first_unset_alpha())  ;
		pointTest(false, dummyFM.first_unset_alpha() == '0', 
				"Error: after application of filling alpha value β to feat matrix with β and ɸ in its alphabet, "
				+ "the system erroneously believes all alpha symbols are now set!")  ;
		corr_dfm_vect = UTILS.featVectChange(""+dfm_og_vect, "0hi"); 
		pointTest(false, 
				dfm_og_vect.equals(dummyFM.getFeatVect()), 
				"Error: feature vector remained unchanged after application of alpha values."); 
		pointTest(true, corr_dfm_vect.equals(dummyFM.getFeatVect()), 
				"Error: the feature vector after alpha feature filling should be\n"+corr_dfm_vect+
				"\nbut it is\n"+dummyFM.getFeatVect())  ; 
		pointTest(false, dfm_og_specs.equals(""+dummyFM), 
				"Error: feature specs remained unchanged after application of alpha values.")  ; 
		pointTest(true, dummyFM.toString().equals(""+(SChangeTester.newFM("-hi,ɸtense"))), 
				"Error @"+getLineNumber()+": feature specs should be [-tense], but it is "+dummyFM)  ; 
		pointTest(false, fmtest.hasNegProxyAlphs(), 
				"FeatMatrix.hasNegProxyAlphs errantly detected as true when there are no neg alphas!"); 
		pointTest(true, fmtest.getNegProxyAlphs().size()==0, 
				"FeatMatrix.getNegProxyAlphs accidentally filled but it should be empty as there aren't any neg alphas !"); 
		
				// comparePreAlpha
					// resetAlphaValues
					// resetAlphVal
					// setAlphaValue
					// applyAlphaValues
					// check_for_alpha_conflict
					// extractAndApplyAlphaVlaues
		
		System.out.println("Now testing FM w multiple-specified single alpha feat (w implications)"); 
		// now testing effect for one with β for hi AND for nas (multifeature alpha symbol!), and ɸ for tense
					// note that -stres has downstream implications: -prim,+syl
						// in turn implying +son,0delrel
		dummyFM = SChangeTester.newFM("βhi,βstres,ɸtense"); 
		
		pointTest(true, dummyFM.has_multifeat_alpha(), 
				"Error: failure to detect situation multiple features assigned same alpha symbol as value") ; 
		dfm_og_vect = ""+dummyFM.getFeatVect(); dfm_og_specs = ""+dummyFM; 
		dummyFM.applyAlphaValues(alph_feats_extrd);
		pointTest(true, dummyFM.first_unset_alpha() == 'ɸ', 
				"Error: after application of filling alpha value β to feat matrix with β (x2) and ɸ in its alphabet, "
				+ "the first (and only) unset alpha should be 'ɸ' but it is "+dummyFM.first_unset_alpha())  ;
		corr_dfm_vect = UTILS.featVectChange(""+dfm_og_vect, "0hi,0stres,0prim,2syl,2son,9delrel"); 
		pointTest(false, 
				dfm_og_vect.equals(dummyFM.getFeatVect()), 
				"Error: feature vector remained unchanged after application of alpha values.") ;
		pointTest(true, corr_dfm_vect.equals(dummyFM.getFeatVect()), 
				"Error: the feature vector after alpha feature filling should be\n"+corr_dfm_vect+
				"\nbut it is\n"+dummyFM.getFeatVect())  ; 
		pointTest(false, dfm_og_specs.equals(""+dummyFM), 
				"Error: feature specs remained unchanged after application of alpha values.")  ; 
		pointTest(true, dummyFM.toString().equals(""+(SChangeTester.newFM("-hi,-stres,ɸtense"))), 
				"Error @"+getLineNumber()+": feature specs should be [-tense], but it is "+dummyFM)  ; 
		pointTest(false, fmtest.hasNegProxyAlphs(), 
				"FeatMatrix.hasNegProxyAlphs errantly detected as true when there are no neg alphas!"); 
		pointTest(true, fmtest.getNegProxyAlphs().size()==0, 
				"FeatMatrix.getNegProxyAlphs accidentally filled but it should be empty as there aren't any neg alphas !"); 
			// getAlphaVars
			// comparePreAlpha
			// resetAlphVal
			// setAlphaValue
			// applyAlphaValues
		
		/*
		 * now checking force truth methods 
		 *for now, not dealing with implications not already stored -- for example, that fmtest forcing a value on something should 
		* make it non-consonantal, given that it is specified for tense
		* those should really be given to the FeatMatrix constructor
		* and in practice, it is done when the FeatMatrix constructor is made via SChangeFactory
		* ... though (TODO) it might be a good idea to check this. 
		* 
		* anyhow, recall that fmtest should now be [-tense, [β=-]hi]
		*/ 
		SequentialPhonic dummyPhone2 = testFactory.parseSeqPh("y"); //initially +tense, +hi
		Phone modDP2 = fmtest.forceTruth(new Phone(dummyPhone2)); 
		String correct_modified_dp2_str = dummyPhone2.getFeatString();
		correct_modified_dp2_str = correct_modified_dp2_str.substring(0,hi_loc) + "0" + correct_modified_dp2_str.substring(hi_loc+1); 
		correct_modified_dp2_str = "œ:"+correct_modified_dp2_str.substring(0,tense_loc) + "0" + correct_modified_dp2_str.substring(tense_loc+1); 
		
		pointTest(false, (""+modDP2).equals(""+dummyPhone2), 
				"Error: FeatMatrix.forceTruth() does not effect any change upon a valid phone to operate on!") ;
		
		pointTest(true, correct_modified_dp2_str.equals(""+modDP2), 
				"Error: ["+dummyPhone2.print()+"], after modification by FeatMatrix "+fmtest+", should become\n"
						+ correct_modified_dp2_str+"\n but instead it is\n"+modDP2)  ;

		dummyPhone2 = testFactory.parseSeqPh("ə"); //initially 0tense, -hi
		modDP2 = fmtest.forceTruth(new Phone(dummyPhone2)); 
		correct_modified_dp2_str = dummyPhone2.getFeatString();
		correct_modified_dp2_str = correct_modified_dp2_str.substring(0,hi_loc) + "0" + correct_modified_dp2_str.substring(hi_loc+1); 
		correct_modified_dp2_str = "ɜ:"+correct_modified_dp2_str.substring(0,tense_loc) + "0" + correct_modified_dp2_str.substring(tense_loc+1); 
		
		pointTest(false, (""+modDP2).equals(""+dummyPhone2), 
				"Error: FeatMatrix.forceTruth() does not effect any change upon a valid phone to operate on!") ;
		
		pointTest(true, correct_modified_dp2_str.equals(""+modDP2), 
				"Error: ["+dummyPhone2.print()+"], after modification by FeatMatrix "+fmtest+", should become\n"
						+ correct_modified_dp2_str+"\n but instead it is\n"+modDP2)  ;

		dummyPhone2 = testFactory.parseSeqPh("ʊ"); //initially -tense, +hi
		modDP2 = fmtest.forceTruth(new Phone(dummyPhone2)); 
		correct_modified_dp2_str = dummyPhone2.getFeatString();
		correct_modified_dp2_str = correct_modified_dp2_str.substring(0,hi_loc) + "0" + correct_modified_dp2_str.substring(hi_loc+1); 
		correct_modified_dp2_str = "ɔ:"+correct_modified_dp2_str.substring(0,tense_loc) + "0" + correct_modified_dp2_str.substring(tense_loc+1); 
		
		pointTest(false, (""+modDP2).equals(""+dummyPhone2), 
				"Error: FeatMatrix.forceTruth() does not effect any change upon a valid phone to operate on!");
		
		pointTest(true, correct_modified_dp2_str.equals(""+modDP2), 
				"Error: ["+dummyPhone2.print()+"], after modification by FeatMatrix "+fmtest+", should become\n"
						+ correct_modified_dp2_str+"\n but instead it is\n"+modDP2)  ;

		dummyPhone2 = testFactory.parseSeqPh("ˈʌ"); //initially -tense, -hi
		modDP2 = fmtest.forceTruth(new Phone(dummyPhone2)); 
		correct_modified_dp2_str = ""+dummyPhone2;
		pointTest(true, (""+modDP2).equals(""+dummyPhone2), 
				"Error: FeatMatrix.forceTruth() should not effect any change upon a phone that already adheres to its stipulations, yet it does!") ;

		//testing with the List<SequentialPhonic> version of forceTruth()
		List<SequentialPhonic> dummyList = testFactory.parseSeqPhSeg("ø ˈɯ"); 
		// correct_modified_dp2_str will not change, as it should become [ˈʌ] 
		
		List<SequentialPhonic> modDummyList = fmtest.forceTruth(dummyList, 1); 
		
		pointTest(true, dummyList.get(0).equals(modDummyList.get(0)), 
				"Error: FeatMatrix.forceTruth(List<SequentialPhonic>) seems to have changed a phone at the wrong index!")  ; 
		pointTest(false, dummyList.get(1).equals(modDummyList.get(1)), 
				"Error: FeatMatrix.forceTruth(List<SequentialPhonic>) does not effect any change upon a valid phone to operate on, "
				+ "or failed to access the index of the list!") ; 
		pointTest(true, correct_modified_dp2_str.equals(""+modDummyList.get(1)), 
				"Error: ["+dummyList.get(1).print()+"], after forceTruth() by FeatMatrix "+fmtest+", should become\n"
						+ correct_modified_dp2_str+"\n but instead it is\n"+modDP2)  ;
		
		System.out.println("Done testing in this mode...");
		concludeTestBatch();
		
		System.out.println("\nNow for a feat matrix with one alpha value, with a redundant feature implication; also testing UnsetAlphaError and the reset function here...");
		fmtest = new FeatMatrix("ɑstres,-prim,+syl",Arrays.asList(UTILS.featsByIndex)); 

		numCorrect += UTILS.checkBoolean(true, fmtest.has_alpha_specs(),
				"Error: system believes there are no alpha specs, but there is one.") ? 1 : 0 ; 
		fua = fmtest.first_unset_alpha(); 
		numCorrect += UTILS.checkBoolean(true, fua == 'ɑ',
				"Error: first unset alpha should be 'ɑ', but it is '"+fua+"'") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(false, fmtest.has_multifeat_alpha(), 
				"Error: system detects an alpha variable specified for multiple features, but there is none") ? 1 : 0; 
		
		List<SequentialPhonic> actOn = testFactory.parseSeqPhSeg("ˈo");
		
		boolean caught = false; 
		try {	fmtest.forceTruth(actOn,0); 		}
		catch(UnsetAlphaError e)	{	caught = true;		}
		numCorrect += UTILS.checkBoolean(true, caught, "Error: Trying to forceTruth"
				+ " without initializing the alpha value should cause an UnsetAlphaError,"
				+ " but none was detected!") ? 1 : 0;
		
		alph_feats_extrd = fmtest.extractAndApplyAlphaValues(testFactory.parseSeqPh("e")); // i.e. -stres
		
		numCorrect += UTILS.checkBoolean(true, alph_feats_extrd.keySet().size() == 1, 
				"Error: one alpha variable was extracted, but "+alph_feats_extrd.keySet().size()+" were detected.") ? 1 :0 ; 
		numCorrect += UTILS.checkBoolean(true, fmtest.has_alpha_specs(),
				"Error: system believes there are no alpha specs, but there is one.") ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(true, fmtest.first_unset_alpha() == '0', 
				"Error: first_unset_alpha() should return '0' (we just set the last unset one),"
				+ " but instead we get "+fmtest.first_unset_alpha()) ? 1 : 0 ;
		numCorrect += UTILS.checkBoolean(false, fmtest.has_multifeat_alpha(), 
				"Error: system detects a multispecified alpha variable where there is none") ? 1 : 0; 
		//System.out.println("Action on "+actOn.get(0)+" ...\n\t"+fmtest.forceTruth(actOn,0).get(0));
		List<SequentialPhonic> result = fmtest.forceTruth(actOn,0); 
		
		numCorrect += UTILS.checkBoolean(true,
				UTILS.phonSeqsEqual(result, testFactory.parseSeqPhSeg("o")),
				"Error: result of fm.forceTruth using "+fmtest+" with ɑ set to (-) should be 'o' (unstressed) but it is "
						+ UTILS.printWord(result)) ? 1 : 0; 
		
		// after resetting, does it behave like a new feat matrix? 
		fmtest.resetAlphaValues(); 
		caught = false; 
		try {	fmtest.forceTruth(actOn,0); 		}
		catch(UnsetAlphaError e)	{	caught = true;		}
		numCorrect += UTILS.checkBoolean(true, caught, "Error: Trying to forceTruth"
				+ " without initializing the alpha value should cause an UnsetAlphaError,"
				+ " but none was detected!") ? 1 : 0;
		
		actOn.add(testFactory.parseSeqPh("j")); 
		
		alph_feats_extrd = fmtest.extractAndApplyAlphaValues(testFactory.parseSeqPh("ˌʌ")); // i.e. +stres, primary in fact. 
		result = fmtest.forceTruth(actOn, 1); 
		numCorrect += UTILS.checkBoolean(true,
				UTILS.phonSeqsEqual(result.subList(1, 2), testFactory.parseSeqPhSeg("ˌi")),
				"Error: result of fm.forceTruth  on [j] using "+fmtest+" with stress (but not primary stress) feature extracted from [ˌʌ] "
						+ "should be\n"+testFactory.parseSeqPh("ˌi")+"\nbut it is\n"
						+ result.get(1)) ? 1 : 0; 
		
		System.out.println("Done testing in this mode; got "+numCorrect+" correct out of 11."); 
		numCorrect = 0 ; 
		
		
		System.out.println("\nNow testing a scenario where two alpha symbols specify five features, and the two specified by the latter have potential mutual feature implications"); 
		//now testing a condition where the FeatMatrix has two alpha variable symbols, each with multiple features, and one non-alpha symbol
			// the non alpha symbol is +hi 
			// one pair with no implications -- lab, back, round
			// and the other with a potential shared implication -- nas and syl 
				// i.e. in this theoretical scenario, all +hi syllabic things are nasalized -- i.e. like some dialects of American English? though not the whole language, just this feature matrix 
					// (though also, all +back things are round, and all -back things aren't) 
		fmtest = SChangeTester.newFM("+hi,βlab,βback,βround,ɣnas,ɣsyl");
		
		numCorrect += UTILS.checkBoolean(true, fmtest.has_alpha_specs(),
				"Error: system believes there are no alpha symbol specifications used, but there are two, specifying five features.") ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(true, fmtest.first_unset_alpha() == 'β', 
				"Error: first_unset_alpha() should return 'β',"
				+ " but instead we get "+fmtest.first_unset_alpha()) ? 1 : 0 ;
		numCorrect += UTILS.checkBoolean(true, fmtest.has_multifeat_alpha(), 
				"Error: system fails to detects a multispecified alpha variable but there are twoǃ") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, fmtest.getLocalAlphabet().equals("βɣ"), 
				"Error: local alphabet should be 'βɣ', but it is "+fmtest.getLocalAlphabet()) ? 1 : 0 ; 
		
		//now testing functionality of FeatMatrix.check_for_alpha_conflict()... 
		//should have no conflict -- w ; c ; ã; õ
		numCorrect += UTILS.checkBoolean(false, fmtest.check_for_alpha_conflict(testFactory.parseSeqPh("w")),
				"Error: [w] should have no alpha conflict but one is detected") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(false, fmtest.check_for_alpha_conflict(testFactory.parseSeqPh("c")),
				"Error: [c] should have no alpha conflict but one is detected") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(false, fmtest.check_for_alpha_conflict(testFactory.parseSeqPh("ã")), 
				"Error: [ã] should have no alpha conflict but one is detected") ? 1 : 0; 
		SequentialPhonic o_tense_nas = testFactory.parseSeqPh("õ"); 
		numCorrect += UTILS.checkBoolean(false, fmtest.check_for_alpha_conflict(o_tense_nas), 
				"Error: [õ] should have no alpha conflict but one is detected") ? 1 : 0; 
		
		// should have alpha conflicts -- b ; ɥ ; u ; n ; m
		numCorrect += UTILS.checkBoolean(true, fmtest.check_for_alpha_conflict(testFactory.parseSeqPh("b")), 
				"Error: [b] should have an alpha conflict between βlab vs. (βround & βback), but none is detected") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, fmtest.check_for_alpha_conflict(testFactory.parseSeqPh("ɥ")), 
				"Error: [ɥ] should have an alpha conflict between (βlab & βround) vs. (βback), but none is detected") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, fmtest.check_for_alpha_conflict(testFactory.parseSeqPh("u")), 
				"Error: [u] should have an alpha conflict between ɣnas and ɣsyl, but none is detected") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, fmtest.check_for_alpha_conflict(testFactory.parseSeqPh("n")), 
				"Error: [n] should have an alpha conflict between ɣnas and ɣsyl, but none is detected") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, fmtest.check_for_alpha_conflict(testFactory.parseSeqPh("m")), 
				"Error: [m] should have an alpha conflicts for both β and ɣ, but none are detected") ? 1 : 0; 
		
		prevFeatVect = fmtest.getFeatVect(); initSpecs = fmtest.toString();
		alph_feats_extrd = fmtest.extractAndApplyAlphaValues(o_tense_nas); 
		numCorrect += UTILS.checkBoolean(true, alph_feats_extrd.isEmpty(), 
				"Error: nothing should be extracted for [õ] since it violates [+hi], but something was...") ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(true, fmtest.getFeatVect().equals(prevFeatVect), 
				"Error: nothing should be extracted for [õ] since it violates [+hi], but the feature vector has somehow changed!") ? 1 : 0 ; 
		numCorrect += UTILS.checkBoolean(true, initSpecs.equals(fmtest.toString()), 
				"Error: nothing should be extracted for [õ] as it violates [+hi], but the feature specifications have somehow changed!") ? 1 : 0; 
		numCorrect += UTILS.checkBoolean(true, fmtest.first_unset_alpha() == 'β', 
				"Error: first_unset_alpha() should still return 'β' as no extraction should happen from [õ],"
				+ " but instead we get "+fmtest.first_unset_alpha()) ? 1 : 0 ;		
		numCorrect += UTILS.checkBoolean(true, fmtest.has_multifeat_alpha(), 
				"Error: after (non-)extraction, system fails to detects a multispecified alpha variable but there are twoǃ") ? 1 : 0; 
		fmtest.resetAlphaValues(); 
		
		alph_feats_extrd = fmtest.extractAndApplyAlphaValues(testFactory.parseSeqPh("ɪ̃")); 
		
		numCorrect += UTILS.checkBoolean(true, alph_feats_extrd.size() == 2, "Error: two alpha symbol values should be extracted for [ɪ̃], "
				+ "but "+alph_feats_extrd.size()+" were.") ? 1 : 0 ;
		numCorrect += UTILS.checkBoolean(true, fmtest.first_unset_alpha() == '0', 
				"Error: now that all alpha symbol features should have been extracted, there should be no unset alpha, "
				+ "but the first unset alpha symbol is detected to be "+fmtest.first_unset_alpha()) ? 1 : 0 ; 
		System.out.println("Done testing in this mode; got "+numCorrect+" correct out of 20"); 
		numCorrect = 0 ; 

		System.out.println("Now testing SequentialFilter..."); 
		// TODO make sure covered these SequentialFilter testing...  method: 
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
		System.out.println("------\nBeginning testing of NEG ALPHA  and other alpha coverage in UTILS..."); 
		System.out.println("Beginning testing of neg alpha handling in SequentialFilter.") ; 
		initTestBatch(); 
		String filtTesterStr ="# [astres,+syl,dhi] ([!acons,bround] [!bround,chi] ([chi,flab,fround])) @ ([dcor,enas,econt])* m #";
		filtTester =  testFactory.parseNewSeqFilter(filtTesterStr, true); 
		pointTest(true, filtTester.hasNegAlphProxies(), "Error @"+getLineNumber()+": somehow failed to detect neg proxies in "+filtTesterStr); 
		String negatedAlphsHere = "ab"; 
		for (int nahi = 0; nahi < negatedAlphsHere.length(); nahi++)
			pointTest(false, filtTester.getProxyPair(negatedAlphsHere.substring(nahi,nahi+1)).equals(UTILS.NULL_PROXY_PAIR),
					"Error @"+getLineNumber()+": failed to detect alpha for "+negatedAlphsHere.charAt(nahi)); 	
			
			
		concludeTestBatch();
		
		//TODO working here. 
		
		
		System.out.println("Testing spec alpha detection...");
		initTestBatch(); 
		
		String spuriousAlphMsg = "Spurious detection of alpha marking on feature" , undetectedAlphMsg="Failed to detect alpha marking on feature"; 
		pointTest(false,UTILS.spec_is_alpha_marked("+voi"), "Spurious detection of alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_alpha_marked("-son"), "Spurious detection of alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_alpha_marked("0sg"), "Spurious detection of alpha marking on feature"); 
		pointTest(true,UTILS.spec_is_alpha_marked("βvoi"), "Failed to detect alpha marking on feature"); 
		pointTest(true,UTILS.spec_is_alpha_marked("+ɣhi"), "Failed to detect alpha marking on feature"); 
		pointTest(true,UTILS.spec_is_alpha_marked("!ɣhi"), "Failed to detect alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_preposed_alpha_marked("+voi", '+'), "Spurious detection of alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_preposed_alpha_marked("-son",'-'), "Spurious detection of alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_preposed_alpha_marked("0sg",'0'), "Spurious detection of alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_preposed_alpha_marked("βnas",'+'), "Spurious detection of alpha marking on feature"); 
		pointTest(true,UTILS.spec_is_preposed_alpha_marked("+ɣhi",'+'), "Failed to detect alpha marking on feature"); 
		pointTest(false,UTILS.spec_is_preposed_alpha_marked("+ɣhi",'-'), spuriousAlphMsg); 
		pointTest(true,UTILS.spec_is_preposed_alpha_marked("-ðcons", '-'), undetectedAlphMsg); 
		pointTest(true,UTILS.spec_is_preposed_alpha_marked("!ðcons", '!'), undetectedAlphMsg); 
		pointTest(true,UTILS.spec_is_preposed_alpha_marked("0ðdistr", '0'), undetectedAlphMsg); 
		pointTest(true,UTILS.spec_is_neg_alpha_marked("-ðcons"), undetectedAlphMsg); 
		pointTest(true,UTILS.getNegatedAlpha("-ðcons") == 'ð', "Failed to detect right negated alpha ð via UTILS.getNegatedAlpha");
		
		for (String aphi : "b,+B,-ʋ".split(",")) 
			pointTest(true,UTILS.getAlphaFromFeatSpec(aphi.charAt(aphi.length()-1)+"back" )==aphi.charAt(aphi.length()-1), "Failed to detect alpha in "+aphi+"back"); 

		String[] dummyFeatStrs = new String[]{"+cons,-cont", "+cons,-cons", "acons,+cont", "acons,-acons"}; 
		boolean[] dummyFeatStrConflictedness = new boolean[] {false, true, false, true}; 
		for (int i = 0; i < dummyFeatStrs.length; i++)
			pointTest(!dummyFeatStrConflictedness[i], UTILS.detectFeatConflicts(Arrays.asList(dummyFeatStrs[i].split(","))).equals(""), 
					"Error @"+getLineNumber()+": " + 
							(dummyFeatStrConflictedness[i] ? "feature conflict missed by UTILS.detectFeatConflicts": "spurious feature conflict detected")
							+ " for "+ dummyFeatStrs[i]); 
		
		pointTest("", UTILS.preemptFeatAlphambiguation('b', Arrays.asList("la,lab,lba".split(","))), 
				"Error @"+getLineNumber()+": spurious feature ambiguation detected!"); 
		String pfaOutput = UTILS.preemptFeatAlphambiguation('s', Arrays.asList("top,stop,lng,lngs".split(","))); 
		pointTest("top,stop", pfaOutput , "Error @"+getLineNumber()+": failed to detect feature ambiguation (should be s+top --> stop; got: "+pfaOutput); 
		
		//isValidFeatSpecList? 
		//hasValidFeatSpecList? 
		System.out.println("Done with spec handling"); 
		concludeTestBatch();
		
		initTestBatch(); 
		System.out.println("Looking at alpha feature matrix detection..."); 
		pointTest(false,UTILS.stringHasFMWithAlpha("h > ∅ / __ #"), "spurious detection of alpha in string"); 
		pointTest(false,UTILS.stringHasFMWithNegAlpha("h > ∅ / __ #"), "spurious detection of neg alpha in string @"+getLineNumber()); 
		pointTest(true, UTILS.detectAllFeatSpecs("h > ∅ / __ [-cons] ").size() == 1, "wrong number of feat specs detected in h > ∅ / __ [-cons] ");
		pointTest(true, UTILS.detectAllFeatSpecs("h > ∅ / __ [-cons] ").get(0).equals("-cons"), "wrong feat spec detected in h > ∅ / __ [-cons] : "+UTILS.detectAllFeatSpecs("h > ∅ / __ [-cons] ").get(0));
		pointTest(false,UTILS.stringHasFMWithAlpha("h > ∅ / __ [-cons] "), "spurious detection of alpha in string @"+getLineNumber());  
		pointTest(false,UTILS.stringHasFMWithNegAlpha("h > ∅ / __ [-cons] "), "spurious detection of neg alpha in string @"+getLineNumber());  
		pointTest(true,UTILS.stringHasFMWithAlpha("h > [avoi] / [-cons] __ [acons] "), "missed detection of alpha in string @"+getLineNumber());  
		pointTest(false,UTILS.stringHasFMWithNegAlpha("h > [avoi] / [-cons] __ [acons] "), "spurious detection of neg alpha in string @"+getLineNumber());  
		pointTest(true,UTILS.stringHasFMWithNegAlpha("h > b ɹ ʌː / [-acons] __ [acons] "), "missed detection of neg alpha in string @"+getLineNumber());  
		pointTest(true,UTILS.stringHasFMWithNegAlpha("h > b ɹ ʌː / [-acons] __ [+cons] "), "missed detection of neg alpha in string @"+getLineNumber());  
		pointTest(true,UTILS.catchFirstOrphanedNegAlph(UTILS.detectAllFeatSpecs("h > b ɹ ʌː / [-acons] __ [acons] ")).equals(""),"spurious detection of orphaned alpha @"+getLineNumber()); 
		String currFeatStrTest = "[-acons,+cont] ([ahi,+nas])* #"; 
		pointTest(true,UTILS.stringHasFMWithNegAlpha(currFeatStrTest), "missed detection of neg alpha in string @"+getLineNumber()); 
		List<String> itemsDetected = UTILS.detectAllFeatSpecs(currFeatStrTest); 
		pointTest(true, itemsDetected.size() == 4, "Error @"+getLineNumber()+": wrong number of feat specs detected in "+currFeatStrTest); 
		for (String fti : "-acons,+cont,ahi,+nas".split(","))
			pointTest(true, itemsDetected.contains(fti), "Error @"+getLineNumber()+": failed to detect feat spec "+fti+" in "+currFeatStrTest); 
		pointTest(true, UTILS.catchFirstOrphanedNegAlph(itemsDetected).equals(""), "Spurious detection of orph alph stip @ "+getLineNumber()); 
		currFeatStrTest = "[-acons,æcont,bhi,+βfront,Bback]"; 
		itemsDetected = UTILS.detectAllFeatSpecs(currFeatStrTest); 
		pointTest(true, itemsDetected.size() == 5, "Error @"+getLineNumber()+": wrong number of feat specs detected in "+currFeatStrTest); 
		for (String fti : "-acons,æcont,bhi,+βfront,Bback".split(","))
			pointTest(true, itemsDetected.contains(fti), "Error @"+getLineNumber()+": failed to detect feat spec "+fti+" in "+currFeatStrTest); 
		
		String orphan = UTILS.catchFirstOrphanedNegAlph(itemsDetected); 
		pointTest(true, orphan.equals("a"), "Error @"+getLineNumber()+": failed to detect orphaned alpha 'a'"); 
		
		itemsDetected = UTILS.listAlphasInFeatString(currFeatStrTest, true); // only negative ones first.  

		pointTest(true, itemsDetected.size()==1, "Error @"+getLineNumber()+": detected "+itemsDetected.size()
			+" alphs ("+ "".join("", itemsDetected) +"), but there should be just one neg alpha here"); 
		pointTest(true, itemsDetected.get(0).equals("a"), "Error @"+getLineNumber()+": 'a' not detected as an neg alpha in "+currFeatStrTest);  
		itemsDetected =  UTILS.listAlphasInFeatString(currFeatStrTest, false); 
		pointTest(true, itemsDetected.size() == 5 , "Error @"+getLineNumber()+": detected "+itemsDetected.size()+" alphs ("+ "".join("", itemsDetected) +"), "
				+ "but there should be 5 alphas detected in "+currFeatStrTest); 
		for (char ai : "aæbβB".toCharArray())
			pointTest(true, itemsDetected.contains(""+ai), "Error @"+getLineNumber()+": "+ai+" not detected as alpha in feat str "+currFeatStrTest); 
		
		pointTest(true, UTILS.listAlphasInString("h > ∅ / # ([+cons])* __ [-cons]").size() == 0, "Spurious detection of alphas by UTILS.listAlphasInString()"); 
		
		String currAlphDetectStr = "c a n > h æ t / [æcont] __ [-acons,æcont] ([ahi,+nas])* #"; 
		itemsDetected = UTILS.listAlphasInString(currAlphDetectStr); 
		pointTest(true, itemsDetected.size() == 2 , "Error @"+getLineNumber()+": detected "+itemsDetected.size()+" alphs ("+ "".join("", itemsDetected) +"), but there should be two alphas detected in "+currAlphDetectStr); 
		pointTest(true, itemsDetected.contains("a"), "Error @"+getLineNumber()+": 'a' not detected as an alpha in "+currAlphDetectStr);  
		pointTest(true, itemsDetected.contains("æ"), "Error @"+getLineNumber()+": 'æ' not detected as an alpha in "+currAlphDetectStr); 
		itemsDetected = UTILS.listNegatedAlphasInString(currAlphDetectStr); 
		pointTest(true, itemsDetected.size() == 1 , "Error @"+getLineNumber()+": detected "+itemsDetected.size()+" neg alphs ("+ "".join("", itemsDetected) +"), but there should be one detected in "+currAlphDetectStr); 
		pointTest(true, itemsDetected.contains("a"), "Error @"+getLineNumber()+": 'a' not detected as neg alpha in "+currAlphDetectStr);  

		System.out.println("done with alpha feature matrix detection");
		
		concludeTestBatch(); 
		
		System.out.println("Testing valid feature spec list detection"); 
		currAlphDetectStr = "-sg,+cons,0tense,ason,-avoi,+acont"; 
		for (int i = 0 ; i < 4 ; i++)
			pointTest(i < 3, 
				UTILS.isValidFeatSpecList(
						(i < 2 ? currAlphDetectStr.split(",0")[0] : currAlphDetectStr), 
								i % 2 == 0), 
				"Error @"+getLineNumber()+" (allowing preposed alphas: "+ (i % 2 == 0 )+"; having preposed alphas: "+(i > 1)+"): " 
				+ (i < 3 ? "spurious flagging of " : "missed ") + "invalidity for "
				+ (i < 2 ? currAlphDetectStr.split(",0")[0] : currAlphDetectStr)); 
		pointTest(false, UTILS.isValidFeatSpecList(currAlphDetectStr.split(",+a")[0].substring(1), true),
				"Error @"+getLineNumber()+": missed detection of invalid feat str"); 
		pointTest(false, UTILS.isValidFeatSpecList(currAlphDetectStr.split(",+a")[0].substring(1), false),
				"Error @"+getLineNumber()+": missed detection of invalid feat str"); 
		
		concludeTestBatch(); 
		
		System.out.println("Testing simple feat matrix construction with neg and pos alpha characters in the feat specs..."); 
		initTestBatch(); 
		System.out.println("Testing with feat matrix: [s voiced , -s aspirated]");
		currAlphDetectStr = "svoi,!ssg"; 
		HashMap<String, String> nAlphMapTester = UTILS.createNegProxyAlphabet("["+currAlphDetectStr+"]"); 
		fmtest = UTILS.getFeatMatrix(currAlphDetectStr,true, nAlphMapTester);
		pointTest(true, fmtest.has_alpha_specs(), 
				"Error @"+getLineNumber()+": alpha specs not detected for neg alpha proxied feat matrix!"); 
		pointTest(true, fmtest.has_multifeat_alpha(), 
				"Error @"+getLineNumber()+": multi feature alpha not detected for "+currAlphDetectStr); 
		pointTest(true, fmtest.hasNegProxyAlphs(), "Error @"+getLineNumber()+": presence of neg proxy alphas missed!"); 
		pointTest(true, nAlphMapTester.keySet().size() == 1, 
				"Error @"+getLineNumber()+": size for neg alph proxy map should be 1 but it is "+nAlphMapTester.keySet().size()); 
		String negProxyHere = (new ArrayList<String> (nAlphMapTester.keySet())).get(0); // UTILS.possibleAlphaProxies.substring(UTILS.possibleAlphaProxies.length()-1); 
		String correctOgFVect = ""; 
		for (String fti: featNames)
			correctOgFVect += fti.equals("voi") ? "s" : (fti.equals("sg") ? negProxyHere : ""+UTILS.UNSPEC_INT); 
		pointTest(correctOgFVect, fmtest.getStrInitChArr(), "Error @"+getLineNumber()+": initial feat vect incorrect.\n"+
				"Correct :"+correctOgFVect+"\nObserved:"+fmtest.getStrInitChArr());
		pointTest(correctOgFVect, fmtest.getFeatVect(), "Error @"+getLineNumber()+": initial feat vect incorrect.\n"+
				"Correct :"+correctOgFVect+"\nObserved:"+fmtest.getFeatVect());
		String localAlphabet = fmtest.getLocalAlphabet();
		pointTest(true, localAlphabet.length()==2, "Error @"+getLineNumber()+": local alphabet ("+localAlphabet+") should be length 2 but isn't..."); 
		pointTest("["+currAlphDetectStr+"]", ""+fmtest, "Error @"+getLineNumber()+": the fm for "+currAlphDetectStr
				+" should print as such with neg alph proxies removed, but instead we see "+fmtest); 
		pointTest(UTILS.NULL_PROXY_PAIR, fmtest.getProxyPair("g"), "Error @"+getLineNumber()+" expected niull proxy pair for 'g', got "+fmtest.getProxyPair("g")); 
		pointTest("s", fmtest.getProxyPair(negProxyHere), "Error @"+getLineNumber()+" proxy pair for "+negProxyHere+" should be s but we got "+fmtest.getProxyPair(negProxyHere)); 
		pointTest(negProxyHere, fmtest.getProxyPair("s"), "Error @"+getLineNumber()+" proxy pair for 's' should be '"+negProxyHere+"' but we got "+fmtest.getProxyPair(negProxyHere)); 
		

		SequentialPhonic unaspP = testFactory.parseSeqPh("p"); 
		//this will produce an alpha conflict error internally -- but it should be fine via comparePreUnsetAlpha!
		pointTest(true, fmtest.comparePreUnsetAlpha(unaspP), 
				"Error @"+getLineNumber()+": comparePreUnset alpha false for feat matrix with only alphas...?!"); 
		pointTest(true, fmtest.comparePreUnsetAlpha(o_tense_nas), 
				"Error @"+getLineNumber()+": comparePreUnset alpha false for feat matrix with only alphas...?!"); 
		pointTest(false, fmtest.check_for_alpha_conflict(o_tense_nas), "Error @"+getLineNumber()+": spurious detection of alpha conflict for "+o_tense_nas.print()
			+" per "+currAlphDetectStr+"(no multifeat alpha here)"); 
		pointTest(true, fmtest.check_for_alpha_conflict(unaspP), "Error @"+getLineNumber()+": missed detection of alpha conflict for "+unaspP.print()
			+" (-voi, -sg) per "+currAlphDetectStr+"(no multifeat alpha here)"); 
		
		System.out.println("Testing with feat matrix: [-s long , - back]");
		
		currAlphDetectStr = "!slong,-back";
		FeatMatrix singAlphFmTest = UTILS.getFeatMatrix(currAlphDetectStr,true, nAlphMapTester);
		pointTest(true, singAlphFmTest.has_alpha_specs(), 
				"Error @"+getLineNumber()+": alpha specs not detected for neg alpha proxied feat matrix!"); 
		pointTest(false, singAlphFmTest.has_multifeat_alpha(), 
				"Error @"+getLineNumber()+": multi feature alpha erroneously detected for "+currAlphDetectStr); 
		pointTest(true, nAlphMapTester.keySet().size() == 1, 
				"Error @"+getLineNumber()+": size for neg alph proxy map should be 1 but it is "+nAlphMapTester.keySet().size()); 
		pointTest(true, singAlphFmTest.hasNegProxyAlphs(), "Error @"+getLineNumber()+": presence of neg proxy alphas missed!"); 
		correctOgFVect = ""; 
		for (String fti: featNames)
			correctOgFVect += fti.equals("long") ? negProxyHere : (fti.equals("back") ? ""+UTILS.NEG_INT : ""+UTILS.UNSPEC_INT); 
		pointTest(correctOgFVect, singAlphFmTest.getStrInitChArr(), "Error @"+getLineNumber()+": initial feat vect incorrect.\n"+
				"Correct :"+correctOgFVect+"\nObserved:"+singAlphFmTest.getStrInitChArr());
		pointTest(correctOgFVect, singAlphFmTest.getFeatVect(), "Error @"+getLineNumber()+": initial feat vect incorrect.\n"+
				"Correct :"+correctOgFVect+"\nObserved:"+singAlphFmTest.getFeatVect());
		String singAlphOgFVect = ""+correctOgFVect; 
		localAlphabet = singAlphFmTest.getLocalAlphabet();
		pointTest(true, localAlphabet.length()==1, "Error @"+getLineNumber()+": local alphabet ("+localAlphabet+") should be length 1 but isn't..."); 
		pointTest(UTILS.NULL_PROXY_PAIR, singAlphFmTest.getProxyPair("g"), "Error @"+getLineNumber()+" expected niull proxy pair for 'g', got "+singAlphFmTest.getProxyPair("g")); 
		pointTest("s", singAlphFmTest.getProxyPair(negProxyHere), "Error @"+getLineNumber()+" proxy pair for "+negProxyHere+" should be s but we got "+singAlphFmTest.getProxyPair(negProxyHere)); 
		pointTest(negProxyHere, singAlphFmTest.getProxyPair("s"), "Error @"+getLineNumber()+" proxy pair for 's' should be '"+negProxyHere+"' but we got "+singAlphFmTest.getProxyPair(negProxyHere)); 
		pointTest("["+currAlphDetectStr+"]", ""+singAlphFmTest, "Error @"+getLineNumber()+": the fm for "+currAlphDetectStr
				+" should print as such with neg alph proxies removed, but instead we see "+singAlphFmTest); 
		pointTest(true, singAlphFmTest.comparePreUnsetAlpha(unaspP), "Error @"+getLineNumber()+": compare pre unset alpha for "+unaspP.print()+" should be true for "
				+currAlphDetectStr+" but somehow it's false.");
		pointTest(false, singAlphFmTest.comparePreUnsetAlpha(o_tense_nas), "Error @"+getLineNumber()+": compare pre unset alpha for "+o_tense_nas.print()+" should be false for "
				+currAlphDetectStr+" but somehow it's true.");
		pointTest(false, singAlphFmTest.check_for_alpha_conflict(unaspP), "Error @"+getLineNumber()+": spurious detection of alpha conflict for "+unaspP.print()
			+" per "+currAlphDetectStr+"(no multifeat alpha here)"); 
		pointTest(false, singAlphFmTest.check_for_alpha_conflict(o_tense_nas), "Error @"+getLineNumber()+": spurious detection of alpha conflict for "+o_tense_nas.print()
			+" per "+currAlphDetectStr+"(no multifeat alpha here)"); 
	
		concludeTestBatch(); 
		
		initTestBatch();
	
		//UTILS.tryParseAndDefineMarkedSymbol("pː"); 
		System.out.println("testing alpha extraction from "+singAlphFmTest+" followed by application to "+fmtest); 
		alph_feats_extrd = singAlphFmTest.extractAndApplyAlphaValues(testFactory.parseSeqPh("pː")); // long --> (not - long) --> should extract s = -/0 -> ʎ = +/2, because it's -long, and -son so it passes comparePreUnsetAlpha
		pointTest(singAlphOgFVect, singAlphFmTest.getStrInitChArr(), "Error @"+getLineNumber()+": errant change to initChArr during extraction!"); 
		pointTest(false, singAlphOgFVect.equals(singAlphFmTest.getFeatVect()), 
				"Error @"+getLineNumber()+": feat vect of [-slong,-back] unchanged after extraction!");
		pointTest(singAlphOgFVect.replace(negProxyHere,""+UTILS.POS_INT), singAlphFmTest.getFeatVect(), // neg proxy = +, s = -
				"Error @"+getLineNumber()+": feat vect of [-slong,-back] after extraction errant!\n"
						+ "Should be: "+singAlphOgFVect.replace(negProxyHere,""+UTILS.NEG_INT)+"\n"
						+ "Observed : "+singAlphFmTest.getFeatVect());
		pointTest("[+long,-back]", ""+singAlphFmTest, "Error @"+getLineNumber()+": [-slong,-back] should have become [+long,-back] after extraction from /pː/ but it is "+singAlphFmTest); 
		
		String[] currTestPhStrs = new String[] {"sː","s","uː","u"};
		
		for(int i = 0 ; i < 4 ; i++) {
			List<SequentialPhonic> currPhEmb = testFactory.parseSeqPhSeg(currTestPhStrs[i]); 
			pointTest(i < 1, singAlphFmTest.compare(currPhEmb.get(0)), 
					"Error @"+getLineNumber()+": conditioning on [-slong,-back] with s=- (-> +long) should be "
					+ (i < 1 ? "true" : "false") +" for "+currPhEmb.get(0).print()+" but it is "+(i < 1 ? "false" : "true")); 
			SequentialPhonic correctFTOutput = testFactory.parseSeqPh( i < 2 ? "sː" : "ʉː"), 
					observedFTOutput = singAlphFmTest.forceTruth(currPhEmb, 0).get(0); 
			
			pointTest(""+correctFTOutput, ""+observedFTOutput, 
					"Error @"+getLineNumber()+": result of application should be "+correctFTOutput+" but is "+observedFTOutput); 
		}
		
		// now applying to the other one...
		String multiAlphOgFeatVect = ""+ fmtest.getStrInitChArr(), 
				multiAlphOgPrint = ""+fmtest; 
		String corrMultiAlphFeatVectResult = multiAlphOgFeatVect.replace("s",""+UTILS.NEG_INT).replace(negProxyHere,""+ UTILS.POS_INT) ;
		fmtest.applyAlphaValues(alph_feats_extrd); 
		pointTest(multiAlphOgFeatVect, fmtest.getStrInitChArr(), "Error @"+getLineNumber()+": errant change to initChArr during extraction!"); 
		pointTest(false, multiAlphOgFeatVect.equals(""+fmtest.getFeatVect()), "Error @"+getLineNumber()+": lack of change to feat vect after applying alph values!"); 
		pointTest(false, multiAlphOgPrint.equals(""+fmtest), "Error @"+getLineNumber()+": lack of change to feat specs after applying alph values!"); 
		pointTest(corrMultiAlphFeatVectResult, ""+fmtest.getFeatVect(), "Error @"+getLineNumber()+": outcome of application (s = -) to "+multiAlphOgPrint+" is wrong.\n"
				+ "\ta priori :"+multiAlphOgFeatVect+"\n"
				+ "\tShould be:"+corrMultiAlphFeatVectResult+"\n"
				+ "\tObserved :"+fmtest.getFeatVect()); 
		pointTest("[-voi,+sg]", fmtest+"","Error @"+getLineNumber()+": "+multiAlphOgPrint+" should have become [+voi,-sg] after s = - but it is "+fmtest); 
		currTestPhStrs = new String[] {"tʰ","t","dʰ","d"};
		for(int i = 0 ; i < 4 ; i++) {
			List<SequentialPhonic> currPhEmb = testFactory.parseSeqPhSeg(currTestPhStrs[i]); 
			pointTest(i < 1, fmtest.compare(currPhEmb.get(0)), 
					"Error @"+getLineNumber()+": conditioning on "+multiAlphOgPrint+" with s=- should be "
					+ (i < 1 ? "true" : "false") +" for "+currPhEmb.get(0).print()+" but it is "+(i < 1 ? "false" : "true")); 
			SequentialPhonic correctFTOutput = testFactory.parseSeqPh("tʰ"), 
					observedFTOutput = fmtest.forceTruth(currPhEmb, 0).get(0); 
			
			pointTest(""+correctFTOutput, ""+observedFTOutput, 
					"Error @"+getLineNumber()+": result of application should be "+correctFTOutput+" but is "+observedFTOutput); 
		}
		fmtest.resetAlphaValues(); 
		pointTest(multiAlphOgPrint, ""+fmtest, "Error @"+getLineNumber()+": "+multiAlphOgPrint+" should have been reset but it is "+fmtest); 
		pointTest(multiAlphOgFeatVect, fmtest.getFeatVect(), "Error @"+getLineNumber()+": "+multiAlphOgPrint+"'s feat vector should have been reset but it is not."
				+ "\n\tShould be: "+multiAlphOgFeatVect+"\n\tObserved : "+fmtest.getFeatVect()); 
		singAlphFmTest.resetAlphaValues(); 
		pointTest("["+currAlphDetectStr+"]", ""+singAlphFmTest, "Error @"+getLineNumber()+": ["+currAlphDetectStr+"] should have been reset but it is "+singAlphFmTest); 
		pointTest(singAlphOgFVect, singAlphFmTest.getFeatVect(), "Error @"+getLineNumber()+": ["+currAlphDetectStr+"] feat vect should be reset but it is not. "
				+ "\n\tShould be: "+singAlphOgFVect+"\n\tObserved : "+singAlphFmTest.getFeatVect()); 
		

		concludeTestBatch(); 
		
		initTestBatch();
		System.out.println("Testing neg proxy alphabet creation..."); 
		currAlphDetectStr = "c a n > h [-Along,-Bnas] t / [-æant,Bnas] __ [-acons,æcont,0Ason,-ant] ([+ahi,+nas])* #"; 
		nAlphMapTester = UTILS.createNegProxyAlphabet(currAlphDetectStr); 
		pointTest(true, nAlphMapTester.keySet().size() == 4, "Error @"+getLineNumber()+": wrong number ("+ nAlphMapTester.keySet().size()
				+ ") of neg alpha proxies made for "+currAlphDetectStr); 
		
		for (String sai : "A,a,æ,B".split(","))
			pointTest(true, nAlphMapTester.containsValue(sai), "Error @"+getLineNumber()+": "+sai+" missed as proxied value for "+currAlphDetectStr); 
		for (char pri : UTILS.possibleAlphaProxies.substring(UTILS.possibleAlphaProxies.length() - 4).toCharArray())
			pointTest(true, nAlphMapTester.containsKey(pri+""), "Error @"+getLineNumber()+": "+pri+" missed as proxy"); 
		
		if (numCorrect < totalChecks) {
			System.out.println("neg alph mapping ~ keys, vals : "); 
			for (String ki : nAlphMapTester.keySet())
				System.out.println(ki +", "+nAlphMapTester.get(ki)); 
		}
		
		String correctProxiedString = currAlphDetectStr + ""; 
		for (String ki : nAlphMapTester.keySet())
			correctProxiedString = correctProxiedString.replace("-"+nAlphMapTester.get(ki), ki); 
		String outputProxiedString = UTILS.applyNegalphaProxies(currAlphDetectStr, nAlphMapTester); 

		//the following is to block cases like replacing "-h" in "-hi" for an alpha h tho. 
		for (String ki : nAlphMapTester.keySet())
			if (nAlphMapTester.get(ki).equals("a"))
				correctProxiedString = correctProxiedString.replace(ki+"nt]", "-ant]"); 
		
		pointTest(correctProxiedString, outputProxiedString, 
				"Error @"+getLineNumber()+": mismatch between correct proxied string and what we actually got from the method.\n"
						+ "Correct : " + correctProxiedString
						+ "\nObserved : "+outputProxiedString); 
		pointTest(currAlphDetectStr, UTILS.decodeNegalphaProxies(correctProxiedString, nAlphMapTester).replace(UTILS.MARK_ALPHNEG+"", UTILS.MARK_NEG+""),
				"Error @"+getLineNumber()+": mismatch between correct negalph-proxy-decoded string and what we actually got from the method.\n"
						+ "Correct  : " + currAlphDetectStr
						+ "\nObserved : "+UTILS.decodeNegalphaProxies(correctProxiedString, nAlphMapTester)); 
		
		String ogFeatSpecTest = "-Bcons,-æcont,-Ason,-ant"; 
		
		String proxiedFSRes = ""+ogFeatSpecTest; 
		for(String prxi : nAlphMapTester.keySet())
			if (!nAlphMapTester.get(prxi).equals("a"))
				proxiedFSRes.replace(""+UTILS.MARK_NEG+nAlphMapTester.get(prxi), prxi); 
		
		proxiedFSRes = UTILS.decodeNegAlphProxiesInFeatString(proxiedFSRes, nAlphMapTester); 
		pointTest(ogFeatSpecTest, proxiedFSRes, 
				"Error @"+getLineNumber()+": mismatch between og and proxied-then-deproxied feat string!\n"
					+"Correct :"+ogFeatSpecTest+"\n"
					+"Observed:"+proxiedFSRes); 
		concludeTestBatch();	
	}
	
	private static void initTestBatch()
	{	numCorrect = 0; totalChecks = 0; 	}
	private static void concludeTestBatch()
	{
		System.out.print("Concluding test batch with "+totalChecks+"..."); 
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
