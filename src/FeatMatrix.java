import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class FeatMatrix extends Phonic implements RestrictPhone {
	
	private char[] init_chArr; //retains mark of alpha values given to constructor class
		// whereas they assume their functional numerical values in featVect as they become specified
	private String featVect; // by default a string of 1s, one for each feature
		// as they become specified they become either 0(neg) or 2(pos)
		// despecification -- i.e. arising only because of feature implications,
			// the change of a feature from +/- to . in unspecified in a phone operated upon. 
		// DESPECIFICATION of phones as part of the FeatMatrix is represented as a 9 in FeatSpecs	
	private final char FEAT_DELIM = ','; 
	private String featSpecs, initSpecs; //"+cor,-dist" etc... separated by FEAT_DELIM 
		// will always return to initSpecs after alphas are reset. 
		// initSpecs, once set, must not under any circumstance be changed.
		// featSpecs, meanwhile, changes when an alpha value is set... 
			//TODO need to ascertain this actually works... 
	
	private List<String> ordFeats; // for retrieving feature indices 
	
	private HashMap<String, String[]> featImpls; 
	
	private String localAlphabet; // for handling all features functioning as alpha values within the feature specifications... 
	public static final String FEAT_MATRIX_PRINT_STMT = " @%@ "; 
	private boolean hasAlphSpecs; 
	private boolean hasMultifeatAlpha; 	
	
	private boolean DESPEC_VIA_ALPHA = false; // set to true to allow spreading of despecification via alpha features
	
	// DESPECIFICATION -- 
		// where due to FEATURE IMPLICATIONS, a feature must be despecified -- i.e. set back to unspecified 
		// example: if a vowel goes from -cont to +cont, the feature delrel should be despecified
		// this case is the only time we will ever make use of the List<String> despecifications
		// which is what is stored in the featVect for this case. 
		// 
	
	// TODO may need to add variables or methods to handle situation where has different alpha symbols for different features. 
	
	/**
	 * version of constructor with featSpecs passed directly
	 * should be passed with , delimiters and +/- indicators 
	 */
	// 
	public FeatMatrix(String specs, List<String> orderedFeats, HashMap<String, String[]> ftImpls)
	{
		if (specs.length() <= 1)	throw new RuntimeException("Invalid string entered for specs"); 
		localAlphabet = "";
		hasMultifeatAlpha = false;
		type = "feat matrix";
		featSpecs=specs+""; 
		initSpecs=specs+""; 

		ordFeats = orderedFeats; 
		featImpls = ftImpls;
		
		init_chArr = new char[ordFeats.size()];
		Arrays.fill(init_chArr, '1');
		
		String[] spArr = specs.split(""+FEAT_DELIM); // one cell each for +delrel (1), -cont (2) etc... 
		
		for (int i = 0; i < spArr.length; i++)
		{	
			String sp = spArr[i]; 
			
			String indic = sp.substring(0, 1); 
			boolean is_alph = !"-+0".contains(indic); 
		
			if (is_alph)
			{	if (!localAlphabet.contains(indic))	localAlphabet += indic; 
				else	hasMultifeatAlpha = true;
			}
			String feat = sp.substring(1); 
			if (!ordFeats.contains(feat))	throw new RuntimeException("ERROR: tried to add invalid feature : '"+feat+"'");
			
			int spInd = ordFeats.indexOf(feat); 
			//originally: int spInd= Integer.parseInt(""+ordFeats.indexOf(feat)); 
				// unclear why that double transformation was necessary but if this causes new errors, best to restore it. 
			init_chArr[spInd] = is_alph ? indic.charAt(0) : 
				("+".equals(indic) ? '2' : ("0".equals(indic) ? '9' : '0' ));  
			// thus, after this init_chArr will have 0 for negatively specified features,
				// 2 for positively specified features, 9 for despecified features
			// for alpha specified features, the alpha (or whatever other dummy symbol is used) is left in the vector
				// until we despecify it later. 
				//... and meanwhile, we have 1 for those that were untouched. 
			
		}
		featVect = new String(init_chArr); 
		hasAlphSpecs = localAlphabet.length() > 0; 
	}
		
	/**
	 * checks if candidate phone adheres to the restrictiosn
	 * @precondition: they have the same length feature vectors
	 * @throws UnsetAlphaError */
	public boolean compare(SequentialPhonic cand)
	{
		if (!cand.getType().equals("phone"))
			return false; 
		
		char nonSet = first_unset_alpha();
		if (nonSet != '0')	throw new UnsetAlphaError(""+nonSet); 
			//formerly -- throw new	RuntimeException("ERROR: tried to compare when alpha style symbol '"+nonSet"' remains uninitialized");
		
		String candFeats = cand.toString().split(":")[1]; 
		
		if (candFeats.length() != featVect.length())
			throw new RuntimeException("ERROR: comparing with feature vects of unequal length");
		
		for (int i = 0 ; i < candFeats.length(); i++)
		{
			String restr = featVect.substring(i,i+1); 
			if ("02".contains(restr) && !restr.equals(candFeats.substring(i, i+1)))
					return false;
			if ("9".contains(restr) && !"1".equals(restr))	return false; 
		}
		return true;
	}
	
	/**
	 * @param candPhonSeq -- whole sequence of phones we are testing. This is only really necessary because
	 * 		we need to implement this method so this class implements interface RestrictPhone
	 * @param index -- index of the phone of interest. See above.
	 * @return whether the phone at the index @index of @candPhonSeq adheres to the restrictions embedded in this FeatMatrix instance. 
	 * @precondition: they have the same length feature vectors
	 * @throws UnsetAlphaError */
	public boolean compare(List<SequentialPhonic> candPhonSeq, int index)
	{	return compare(candPhonSeq.get(index));		}
	
	/**
	 *  makes all the restrictions specified in this FeatMatrix true for @param patient
	 *  patient -- patient as in object of modification necessary to impose the truth of the values encoded in this FeatMatrix
	 * by changing any necessary feature values in patient 
	 * @precondition: they have the same length feature vectors
	 * @throws UnsetAlphaError */
	public Phone forceTruth(Phone patient)
	{
		char nonSet = first_unset_alpha();
	
		if (nonSet != '0')	throw new UnsetAlphaError(""+nonSet);
		
		Phone output = new Phone(patient); 
		String patFeats = patient.getFeatString(); //i.e. feature values of the patient, the phone undergoing modification
		if (patFeats.length() != featVect.length())
			throw new Error("ERROR: cannot forceTruths on phone with different length feat vector");
			// technically it could still function if they aren't the same length, 
			// but for security best to call it out, as obscure errors could easily ensue
			// prior to Dec 20 2022, this was throwing an UnsetAlphaError-- unclear why. 
		
		for (int fvi = 0; fvi < featVect.length() ; fvi++)
		{
			char ch = featVect.charAt(fvi); 
			if (ch != '1')
				patFeats = patFeats.substring(0, fvi) + 
					(ch == '9' ? '1' : ch) + patFeats.substring(fvi+1); 
		}
		output.setFeats(patFeats);
		
		return output; 
	}
	
	/**
	 * @param patientSeq
	 * @param ind
	 * @return
	 * @throws UnsetAlphaError 
	 */
	public List<SequentialPhonic> forceTruth (List<SequentialPhonic> patientSeq, int ind)
	{
		char nonSet = first_unset_alpha();
		if( nonSet != '0')
			throw new UnsetAlphaError(""+nonSet); 
		
		SequentialPhonic patient = patientSeq.get(ind);
		if (!patient.getType().equals("phone"))
			throw new RuntimeException("ERROR: trying to force cand restrictions on non-phone!");
		List<SequentialPhonic> outSeq = new ArrayList<SequentialPhonic>(patientSeq); 
		outSeq.set(ind, forceTruth(new Phone(patient)));
		return outSeq;
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof FeatMatrix)	
		{	String othersString = other.toString(); 
			if(othersString.length() < 4)	return false; //no chance
			String[] othersSpecs = othersString.split(""+FEAT_DELIM); 
			String othersVect = ""; 
			for(int ti = 0; ti < this.featVect.length() ; ti++)	othersVect+="1"; //fill with default 1s  
			for(int ki = 0; ki < othersSpecs.length; ki++)
			{
				int othFtInd = ordFeats.indexOf(othersSpecs[ki].substring(1));
				char othFtSpecVal = othersSpecs[ki].charAt(0); 
				othersVect = othersVect.substring(0, othFtInd) + 
						(othFtSpecVal == '0' ? 9 : (othFtSpecVal == '+' ? 2 : 0)) 
						+ othersVect.substring(othFtInd + 1); 
			}
			return this.getFeatVect().equals(othersVect);
		}
		else	return false; 
	}
	
	@Override
	public void resetAlphaValues()
	{	featVect = new String(init_chArr);
		featSpecs = ""+initSpecs;
	}
	
	private char toSurfVal(char i)
	{
		if (!"0129".contains(""+i))	throw new Error("Error: invalid specification value.");
		return "-0+0".charAt("0129".indexOf(i)); 
	}
	
	private char fromSurfVal(char i)
	{
		if (!"-+.0".contains(""+i))	throw new Error("Error: invalid specification value.");
		return "0299".charAt("-+.0".indexOf(i)); 
	}
	
	/**
	 * apply a value to the feature vector
	 * in practice, used as auxiliary to applyAlphaValues
	 * @param value to apply, should be surface value i.e. ( + positive , - negative , 0 despecify... 
	 * 		// ... in practice 0/despecify should never really happen except via a feature implication 
	 * @param feature to apply it to, should be standard feature name as seen in symbolDefs (or replacement file) and featImplications (likewise)
	 * 		as this class does not use feature translations; i.e. "stres", "cor", etc. 
	 * @param via_impl -- whether or not this feature is being applied via a feature implication
	 * 		i.e. if so, featSpecs won't be modified, though the feat vect will be
	 * 		and downstream implications will still be triggered either way
	 * 		in practice, as of December 2022, via_impl is always true.
	 */
	private void apply_value(String value, String feature, boolean via_impl)
	{
		int aff_ind = ordFeats.indexOf(feature);
			
		if (featVect.charAt(aff_ind) != '1')	return; 	// really this shouldn't ever happen unless it was going to be the same value that was already stored (due to being constructed that way, or due to a prior modification due to filling of alpha values earlier)... may need to put more guard rails here if issues with the feature vector arise		
		featVect = featVect.substring(0, aff_ind) + fromSurfVal(value.charAt(0)) + featVect.substring(aff_ind+1); 
		
		if (!via_impl) // if it's not via implication 
		{	
			if (featSpecs.contains(feature))
				System.out.println("Likely error: tried to modify featSpecs for specification of feature "+feature+", but it was already there. Continuing, but you may wish to examine this..."); 
			else	featSpecs += FEAT_DELIM + value + feature;  
		}
		
		//for handling any downstream specifications, 
		List<String> impls = new ArrayList<String>(); 
		
		// first any implications contingent to both + and - specification 
		if("+-".contains(""+value) && featImpls.keySet().contains(feature))
			impls.addAll(Arrays.asList(featImpls.get(feature))); 
		// then any implications contingent to the specific case observed, with + or with - 
		if(featImpls.keySet().contains(value+feature))
			impls.addAll(Arrays.asList(featImpls.get(value+feature))); 
		
		for (String ii: impls)	apply_value(ii.substring(0,1), ii.substring(1), true); 
	}
	
	@Override
	//TODO need to replace values also in featSpecs here. 
	// alphVals should be HashMap with keys of the original alpha symbol set, and values of the surface form of the specification being imposed (+,-)
		// this class should probably not be used for despecification of alpha values 
	// the values in alphVals should be in their 'deep' values with 0 meaning negative, 2 positive, 9 despecified
		// the deep value 1 should never occur as a value in alphVals
		// and while 9 will occur, it should NOT be applied as an alpha value, as that could lead to errors
			// and furthermore, since sound change does not operate by despecifying a feature value according to the recieved literature,
				// neither should the application of alpha values treat despecification as some ternary third feature.
				// of the same functional load as negative or positive feature values.
				// to change this behavior, set the class parameter DESPEC_VIA_ALPHA to true. 
	// @precondition both the keys and the values in alphVals should be one character strings
	// this class should be called using the outputs of extractAndApplyAlphaValues
	public void applyAlphaValues(HashMap<String,String> alphVals)
	{
		if (alphVals.keySet().size() == 0)	return; 
		
		List<String> alphFeatsWImpls = new ArrayList<String>(); 
			// to store which features were modified 
			// so that feature implications can be triggered AFTER they each are modified
				// preempting a possible error in the case where an alpha symbol specified for multiple features 
					// is specified for both a feature and one it has an implication for
				// (in practice that would never cause a serious error unless there was something weird in a custom feature implications file, 
				//  ... but in that case it would create a very subtle error!) 
			// entries in this list will be of form "+son,-cont,0delrel" etc -- i.e. value symbol followed by the feature's abbreviated name	
		
		for (String s : alphVals.keySet())
		{
			//s is the current alpha symbol, every instance of it in the featVect is being changed to the extracted value, val.  
			char val = alphVals.get(s).charAt(0); 
				//alphVals.get(s) must be just one character -- may need to throw an Error if this is not the case.  
				// s must also be one character, but that is for reasons external to this class. 
			
			// disallow despecification via alpha
			if (val == '9' && !DESPEC_VIA_ALPHA)	continue;
			else if (!"02".contains(""+val))	{ 
				if (val != '1')	System.out.println("Alert -- tried to apply a value other than 0,1,2, or 9 to an alpha-specified feature... likely error around here. Ignoring for now...");
					//as for other values outside the accepted four, they really shouldn't be allowed, but we're doing this above for now.
				continue;			
			}
			
			while(featVect.contains(s))
			{
				int nxind = featVect.indexOf(s); 
				
				featVect = featVect.substring(0, nxind) + val + featVect.substring(nxind+1); 

				// feat implications stuff flagged here for handling downstream, at the same time as modifications to featSpecs. 
				// flag  first the case of an any-specification implication, relevant if the value is + or -
				// and then the case of a specific-specification implication, where it has to either be + or be - for the feature implication to be present.
					// these have to be handled separately but NOT disjunctively, because there can be features 
				// that have both any-specification implications, and specific specification implications
				// for example, "stres" in the standard FeatImplications file. 	
				
				String currSpec = ordFeats.get(nxind); 
				
				//handling first the any-specification case to store for implications downstream
				if("02".contains(""+val) && featImpls.keySet().contains(currSpec))
				{	alphFeatsWImpls.add(currSpec); } //will actually be handled downstream in this method.
				
				// feat specs modification
				int fsloc = featSpecs.indexOf(s+currSpec);	//index of where in featSpecs to modify. 
				featSpecs = featSpecs.substring(0,fsloc) + toSurfVal(val) + featSpecs.substring(fsloc+1); 
				
				//now the specific specification for implications downstream. 
				currSpec = toSurfVal(val)+currSpec; 
				if (featImpls.keySet().contains(currSpec))
					alphFeatsWImpls.add(currSpec); 
			}
		}
		
		//now handling any feature implications. 
		//this has to be done here, because it is not done in the forceTruth methods. 
		/**don't need to explicitly handle the possibility that alpha-valued features are going to be affected by feature implications
		 * -- as long as this class is accessed by alpha values previously extracted via .extractAndApplyAlphaValues(SequentialPhonic)
		 * as that class interacts with the entire feature vector of the SequentialPhonic (in practice, a Phone.) 
		 */
		for (String afii : alphFeatsWImpls)	
		{
			String[] impls = featImpls.get(afii); 
			for (String impc : impls)	
				apply_value(impc.substring(0,1), // although this looks potentially bugged, it won't be, because the second column of implications must always have a specific value associated with the feature (otherwise, the feature implication would be vacuous)
						impc.substring(1), true); 
		}
	}
	
	
	// should always be called before extractAndApplyAlphaValues
	// bounds do not matter for our purposes here -- checking for alpha impossibility in multiphone itmes should skip over juncture phones (i.e. word bounds etc) 
	@Override
	public boolean check_for_alpha_conflict(SequentialPhonic inp) 
	{
		if (!inp.getType().equals("phone"))
			throw new RuntimeException("Error: tried to check for alpha value impossibility of a juncture phone!");
		if (!hasMultifeatAlpha)	return false;	
		
		HashMap<String, String> currReqs = new HashMap<String,String> ();
		char[] cand_feat_vect = inp.toString().split(":")[1].toCharArray(); 
		
		if (cand_feat_vect.length != init_chArr.length)	throw new RuntimeException("tried to check for alpha value impossibility "
				+ "for feat vects of differing length"); 
		for (int c = 0 ; c < cand_feat_vect.length; c++)
		{
			String deepSpec = init_chArr[c] + "";
			if (!"0192".contains(deepSpec))
			{
				if (currReqs.containsKey(deepSpec))
				{
					if ("02".contains(currReqs.get(deepSpec)))
					{	if (!currReqs.get(deepSpec).equals(""+cand_feat_vect[c]))	return true;	}
					else if ("9".equals(currReqs.get(deepSpec)))
					{
						if (cand_feat_vect[c] != '1')	return true;
					}
					else if ("1".equals(cand_feat_vect[c]+""))
					{	currReqs.put(deepSpec, "9"); }
					else
					{	currReqs.put(deepSpec, cand_feat_vect[c]+""); }
				}
				else if ("1".equals(cand_feat_vect[c]+""))
				{	currReqs.put(deepSpec, "9"); }
				else
				{	currReqs.put(deepSpec, cand_feat_vect[c]+""); }
			}
		}
		return false; 
		
	}
	
	@Override
	/**
	 *  for a FeatMatrix with either no alpha specs or no UNFILLED alpha specs, returns empty HashMap. 
	 * also returns an empty HashMap if specifications that are not unspecified alpha specs are inconsistent with @param inp 
	 * - because if these requirements are not met, the extraction alpha values for a context phone or input phone cannot occur in the first place
	 * 	* as it won't be a valid situation for the operation of the sound change in question 
	* otherwise APPLIES the value specifications that alpha-valued features have in the SequentialPhonic @param inp
	* 	and then returns those exact value specifications that were applied
	*/
	public HashMap<String,String> extractAndApplyAlphaValues(SequentialPhonic inp)
	{
		if (first_unset_alpha() == '0')	return new HashMap<String,String>(); 
		HashMap<String, String> currReqs = new HashMap<String,String> ();
		char[] cand_feat_vect = inp.toString().split(":")[1].toCharArray(); 
		
		if (cand_feat_vect.length != featVect.length()) 	throw new RuntimeException("cannot extract alpha values for feat vectors of inconsistent length"); 
		
		for (int c = 0 ; c < cand_feat_vect.length; c++)
		{
			char fvspec = featVect.charAt(c); 
			
			if (!"0192".contains(""+fvspec)) // if true, this is a feature with a not-yet-extracted alpha value. 
			{
				if (currReqs.containsKey(""+fvspec))
				{ // value conflict between already-set alpha value, and the (different or redundant) one encountered. 
					String currspec = currReqs.get(""+fvspec); 
					if (currspec.equals("9")  && '1'!=cand_feat_vect[c])
							throw new RuntimeException("Error : Alpha value conflict encountered -- should have called check_for_alpha_conflict() first!"); 
					else	if (!currspec.equals(cand_feat_vect[c]+""))
						throw new RuntimeException("Error : Alpha value conflict encountered -- should have called check_for_alpha_conflict() first!"); 
				}
				else if (cand_feat_vect[c] == '1')	// i.e. alpha-symbol, 9 (despecification)
					currReqs.put(""+fvspec, "9"); // TODO NOTE this is extracted but at present it will NOT be applied. 
				else	
					currReqs.put(""+fvspec, cand_feat_vect[c]+""); // i.e. alpha symbol, and 0 or 2 (negative, positive)
			
			}
			else if ("02".contains(""+fvspec) && fvspec != cand_feat_vect[c] ) //i.e. clash in specified values for the same feature between FeatMatrix and candidate input for a sound change
				return new HashMap<String,String>(); //i.e. this is not a valid input in the first place, nothing to extract -- return empty HashMap
		}
		
		applyAlphaValues(currReqs); //this appears to often be redundantly called in practice  
	
		return currReqs; 
	}
	
	@Override
	public boolean has_alpha_specs()	{	return hasAlphSpecs;	} 
	public boolean has_multifeat_alpha() {	return hasMultifeatAlpha;	}
	
	// returns '0' if not set
	// otherwise the first alpha value detected that has not become a number, in featVect
	@Override
	public char first_unset_alpha()
	{
		if (localAlphabet.length() > 0)
			for (char c : localAlphabet.toCharArray())
				if(featVect.contains(""+c))	return c; 
		
		return '0';
	}
	
	public String getStrInitChArr()
	{	return String.copyValueOf(init_chArr);	}
	
	public String getLocalAlphabet()
	{	return ""+localAlphabet;	}
	
	@Override
	public String toString() 
	{	return "["+featSpecs+"]";		}
	
	//TODO currently used for testing only
	public String getFeatVect() 
	{	return ""+featVect; 	}
	
	@Override
	public String print() {
		return FEAT_MATRIX_PRINT_STMT; //this can be changed for stylistic purposes as long as it is unique with respect to the print outputs of parallel classes
		//TODO however it is changed, it will be necessary to modify various classes that rely on the stability of this symbol, 
			// such as SChangeFeat
	}

}
