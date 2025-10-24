import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class FeatMatrix extends Phonic implements RestrictPhone {
	
	private char[] init_chArr; /**@retains mark of @alpha values given to constructor class
		// whereas they assume their functional numerical values in @featVect as they become specified*/ 
		// NOTE that this will have negative proxy alphas in it because '-' + the alpha val would be two characters
	private String featVect; // by default a string of 1s, one for each feature
		// as they become specified they become either 0(neg) or 2(pos)
		// note that this will have negative proxy alphas in it because '-' + the alpha val would be two characters
		// despecification -- i.e. arising only because of feature implications,
			// the change of a feature from +/- to . in unspecified in a phone operated upon. 
		// DESPECIFICATION of phones as part of the FeatMatrix is represented as a 9 in FeatSpecs	
	private final char FEAT_DELIM = UTILS.FEAT_DELIM; 
	private String featSpecs, initSpecs; //"+cor,-dist" etc... separated by FEAT_DELIM 
		// will always return to initSpecs after alphas are reset. 
		// initSpecs, once set, must not under any circumstance be changed.
		// featSpecs, meanwhile, changes when an alpha value is set... 
			//TODO need to ascertain this actually works... (9/25/25 : unsure when that was written. Before implementation of neg alphas [as is the state at time of writing], seemed to be fine. 
		// note -- these will use the negative proxy alpha symbols too, for convenience/ coding continuity. 
			// however, external access will see "-α" rather than β (if β is the proxy for {-}α) 
	private List<String> ordFeats; // for retrieving feature indices 
	
	//private HashMap<String, String[]> featImpls; 
	//	abrogated -- as of Jan 24, 2024, now using a global feature implications hashmap stored in UTILS.
	
	private String localAlphabet; // for handling all symbols functioning as alpha values within the feature specifications... 
		// will include neg proxy alphas 
	public static final String FEAT_MATRIX_PRINT_STMT = " @%@ "; 
	private boolean hasAlphSpecs; 
	private boolean hasMultifeatAlpha; 	
		//note: a FeatMatrix with both an alpha value and its negated value,
			// represented as a proxy in internal structures, 
			// is counted as a FM bearing a multifeatured alpha 
	
	
	private HashMap<String, String> negProxyAlphs; 
		// symbols for negated alphas, since '-' + alpha character would be two characters
			// proxies are internally stored as one charater, function in opposition to whatever the lapha value is, bidirectionally
			// so if A and B are an alph value and its neg proxy (either way) making one - makes the other +, and vice versa
	
	
	// DESPECIFICATION -- 
			// where due to FEATURE IMPLICATIONS, a feature must be despecified -- i.e. set back to unspecified 
			// example: if a vowel goes from -cont to +cont, the feature delrel should be despecified
			// this case is the only time we will ever make use of the List<String> despecifications
			// which is what is stored in the featVect for this case. 
			// 
	private boolean DESPEC_VIA_ALPHA = false; // set to true to allow spreading of despecification via alpha features
	// without this, one cannot despecify alpha features directly, explicitly,
		// though the specification of an alpha feature could downstream lead to the despecification of other features
			// via feature implications. 
	// currently, making this true may cause errors.  (9/30/25 -- check this ? TODO) 
		// despecification via alpha doesn't apply between an alpha and its negative proxy (what would be the opposite of despecification anyways?) 
	
	// TODO note on despecification
		// alphVals should be HashMap with keys of the original alpha symbol set, and values of the surface form of the specification being imposed (+,-)
			// this class should probably not be used for despecification of alpha values 
		// the values in alphVals should be in their 'deep' values with 0 meaning negative, 2 positive, 9 despecified
			// the deep value 1 should never occur as a value in alphVals
			// and while 9 will occur, it should NOT be applied as an alpha value, as that could lead to errors
				// and furthermore, since sound change does not operate by despecifying a feature value according to the received literature,
					// neither should the application of alpha values treat despecification as some ternary third feature.
					// of the same functional load as negative or positive feature values.
					// to change this behavior, set the class parameter DESPEC_VIA_ALPHA to true. 
		// however, it is permissible in one specific case to use LOCALLY ONLY despecification: 
			// that is when the alpha feature in the output, and specified in the prior and posterior contexts
				// i.e. the output is assimilating to the context -- for example a coronal becoming non-coronal
						// thus losing specification for [ant], and [distr] 
			// it is not permissible for the input
					// (i.e. you wouldn't want to catch the shared non-specification of dorsals and glottals for [ant] when you meant to indicate either "both alveolars" or "both postalveolars" etc.
		// to handle this, the method setAsOutput is added, which changes DESPEC_VIA_ALPHA for that instance to true. 
	public void setAsOutput()	{ DESPEC_VIA_ALPHA = true; 	}
		
	/**
	 * version of constructor with featSpecs passed directly
	 * should be passed with ',' as  delimiters, and '+/-' as indicators (or '0', for despecification if the result of upstream application of feature implications)
	 */
	// 
	public FeatMatrix(String specs, List<String> orderedFeats)
	{	this (specs, orderedFeats, new HashMap<String, String> ());	}
	public FeatMatrix(String specs, List<String> orderedFeats, HashMap<String,String> negAlphProxMap)
	{
		if (specs.length() <= 1)	throw new RuntimeException("Invalid string entered for specs"); 
		
		localAlphabet = "";
		hasMultifeatAlpha = false;
		type = "feat matrix";
		negProxyAlphs = new HashMap<String, String> (negAlphProxMap); 
		ordFeats = orderedFeats; 
		
		// if specs does not already have proxies applied and there are proxies, then apply them now.
		// do it to featSpecs as we fill it. InitSpecs will be identical at this time. 
		// Current (9/25/25) policy is that they internally have the neg proxy alphas but print with the negated actual alphas that are proxied. 
		featSpecs=specs+""; 
		if (UTILS.listNegAlphasInFeatString(specs).size() > 0) 
		{
			featSpecs = UTILS.applyNegalphaProxies(featSpecs, negProxyAlphs); 
			if (UTILS.listNegAlphasInFeatString(featSpecs).size() > 0)  // if there are still neg alphs -- must be error! 
				throw new Error("Error: failed to proxy all negated alphas. Inspect.\n\tOriginal specs: "+specs+";\n\tProxied specs: "+featSpecs); 
		}
				
		initSpecs=featSpecs+""; 
		
		init_chArr = new char[ordFeats.size()];
		Arrays.fill(init_chArr, '1');
		
		String[] spArr = specs.split(""+FEAT_DELIM); // one cell each for +delrel (1), -cont (2) etc... 
		
		for (int i = 0; i < spArr.length; i++)
		{	
			String sp = spArr[i]; 
			
			// as of July 2024, spaces are automatically removed when parsing FMs.
			sp = sp.replace(" ",""); 
			
			String indic = sp.substring(0, 1); 
			boolean is_alph = !UTILS.ALL_FTSPEC_MARKS.contains(indic); 
		
			if (is_alph)
			{	UTILS.abortIllegalAlpha(indic);
				if (!localAlphabet.contains(indic))	localAlphabet += indic; 
				else	hasMultifeatAlpha = true;

				// implement having both an alpha and its neg proxy counting as having a multifeat alpha 
				if (hasNegProxyAlphs() )
				{
					if (negProxyAlphs.containsKey(indic))
						if (localAlphabet.contains(negProxyAlphs.get(indic)))
							hasMultifeatAlpha = true; 
					if (negProxyAlphs.containsValue(indic))
						for (String pxi : negProxyAlphs.keySet()) 
							if (negProxyAlphs.get(pxi).equals(indic) ? localAlphabet.contains(pxi) : false)
							{
								hasMultifeatAlpha = true; 
								break; 
							}
				}
					
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
	 * compare truth against an ordered feat vect ( @param candFeats) , whether from a phone or not 
	 * @return true if this would be true if the cand feat vector meets the specifications of this FeatMatrix. 
	 */
	public boolean compareToFeatVect (String candFeats)
	{
		if (candFeats.length() != featVect.length())
			throw new RuntimeException("ERROR: comparing with feature vects of unequal length");
		
		for (int i = 0 ; i < candFeats.length(); i++)
		{
			String restr = featVect.substring(i,i+1),
					cand_spec = candFeats.substring(i,i+1); 
			if ("02".contains(restr) && !restr.equals(candFeats.substring(i, i+1)))
					return false;
			if ("9".contains(restr) && !"1".equals(cand_spec))	return false; 
		}
		return true;
	}
	
	/**
	 * checks if candidate phone adheres to the restrictions
	 * (9/30/25) should behave the same regardless of presence of neg alpha proxies, but currently (9/30/25) untested. 
	 * @precondition: they have the same length feature vectors
	 * @precondition: alphas are set. 
	 * @throws UnsetAlphaError */
	public boolean compare(SequentialPhonic cand)
	{
		if (!cand.getType().equals("phone"))
			return false; 
		
		char nonSet = first_unset_alpha();
		if (nonSet != '0')	throw new UnsetAlphaError(""+nonSet); 		//formerly -- throw new	RuntimeException("ERROR: tried to compare when alpha style symbol '"+nonSet"' remains uninitialized");
		
		return compareToFeatVect (cand.toString().split(":")[1]); 
	}
	
	/**
	 * checks if @param cand adheres to restrictions @except those that are @alpha values
	 * presently (early Aug 2023) used to skip preemptively to "false" conclusion in SChange objects when extracting alphas,
	 *  currently in terms of alpha values embedded in contexts (not source phones). 
	 *  as of @2025 -- no longer operating through init_chArr but now via featspecs,
	 *  	because sometimes some alphas are filled and others are not. 
	 *  (9/30/2025) -- should behave the same if neg alpha proxies are present because of their handling in localAlphabet, featVect
	 */
	public boolean comparePreUnsetAlpha(SequentialPhonic cand)
	{
		if (!cand.getType().equals("phone"))
			return false; 
		
		String candFeats = cand.toString().split(":")[1]; 
		if (candFeats.length() != featVect.length())
			throw new RuntimeException("ERROR: comparing with feature vects of unequal length");
		
		for (int i = 0 ; i < candFeats.length(); i++)
		{
			// abrogated 2025 -- String restr = ""+init_chArr[i]; // working with init_chArr -- which retains alpha values. 
			String restr = featVect.substring(i,i+1); 
			String cand_feat = candFeats.substring(i, i+1); 
			if ( UTILS.POLAR_FTVECT_INTS.contains(restr) && !restr.equals(cand_feat))
					return false;
			if ((""+UTILS.DESPEC_INT).contains(restr) && !(""+UTILS.UNSPEC_INT).equals(cand_feat))	
				return false; 
			if (!DESPEC_VIA_ALPHA && localAlphabet.contains(restr) 
					&& (""+UTILS.DESPEC_INT+UTILS.UNSPEC_INT).contains(cand_feat))
				return false;  //(2025 interpretation) this would require alpha to be set at this time. Though blocking that via this class is a bit categorically off, it's not really comparing *pre* alpha...
			// if DESPEC_VIA_ALPHA is true, no need to handled alpha valued features at all; 
					// this will already doing nothing for alpha valued items -- which is exactly as should happen, they are being ignored. 
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
	
	// modify feat vect so this feat matrices values have been imposed. 
	public String forceTruthOnFeatVect (String patientFeatVect) 
	{
		String out = ""+patientFeatVect; 
		
		for (int fvi = 0; fvi < featVect.length() ; fvi++)
		{
			char ch = featVect.charAt(fvi); 
			if (ch != '1')
				out = out.substring(0, fvi) + 
					(ch == '9' ? '1' : ch) + out.substring(fvi+1); 
		}
		
		return out; 
	}
	
	/**
	 *  makes all the restrictions specified in this FeatMatrix true for @param patient
	 *  patient -- patient as in object of modification necessary to impose the truth of the values encoded in this FeatMatrix
	 * by changing any necessary feature values in patient 
	 * @precondition: they have the same length feature vectors
	 * @precondition: alphas are set.
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
		
		output.setFeats(forceTruthOnFeatVect(patFeats));
		
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
	// if alpha features are not yet specified MUST call extractAndApplyAlphaValues first. 
			// or else this will not return true 
				// in such a scenario where it is being matched pre-setting agianst a candidate segment
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
	//(10/20/25 , overriding 9/30/25) resetting all alphas including neg proxies, for xecurity.
	public void resetAlphaValues()
	{	featVect = new String(init_chArr);
		featSpecs = ""+initSpecs;
		for (char afi : localAlphabet.toCharArray())	resetAlphVal(afi); 
			// will cascade onto neg proxies. 
			// probably unnecessary. 
	}
	
	@Override
	/** 
	 * reset only one alpha value, @param alph,
	 * using @init_chArr to locate it within @featVect
	 * as of 9/30/25 -- also resets the proxy or proxied pair. 
	 */
	public void resetAlphVal (char alph) {
		char proxPair = (hasNegProxyAlphs() ? getProxyPair(""+alph) : UTILS.NULL_PROXY_PAIR).charAt(0); 
		for (int ispi = 0 ; ispi < initSpecs.length() ; ispi++)
		{
			if (initSpecs.charAt(ispi) == alph)
				featSpecs = featSpecs.substring(0,ispi) + alph + featSpecs.substring(ispi+1);
			else if ( hasNegProxyAlphs() ? initSpecs.charAt(ispi) == proxPair: false)
				featSpecs = featSpecs.substring(0,ispi) + proxPair + featSpecs.substring(ispi+1);
		}
		
		// doing it this way in order to not cascade onto implications that are alpha marked... 
		featVect = new String(init_chArr);
	
		for (String featspec : featSpecs.split(","))
			if (!UTILS.spec_is_alpha_marked(featspec))
				apply_value(featspec.substring(0,1), featspec.substring(1),false); 
	}
	
	/**
	 * given @param i, a feature int from a feat vector 
	 * @return the surface marking in the feat spec list that corresponds 
	 * @beware, @surjective for 1 and 9 which both go to '0' (UTILS.MARK_UNSPEC)
	 * moved to UTILS for broader access, now the local class here just references that.  
	 */
	private char ftIntToMark(char i)
	{	return UTILS.ftIntToMark(i); 	}
	
	/**
	 * given @param i, a feature marking
	 * @return the ft int form to be stored in feature vectors of the FeatMatrix. (9 not 1 -- despecifying) 
	 * surjective for 0 and '.' which both go to 9, not 1 ('.' being used is no longer a thing) 
	 */
	private char ftMarkToInt(char i)
	{
		if (!"-+.0".contains(""+i))	throw new Error("Error: invalid specification value.");
		return "0299".charAt("-+.0".indexOf(i)); 
	}
	
	/**
	 * apply a value to the integer marked feature vector @featVect
	 * in practice, used as @auxiliary to @applyAlphaValues and @resetAlphVals
	 * 		because -- note -- these are the only values that would be changed anyways. 
	 * 		i.e. a feat vector that is declared as [-voi] will never become '+voi' or unset. 
	 * @param @newVal new value to apply (mark it as), should be surface value i.e. ( + positive , - negative , 0 despecify... 
	 * 		// ... in practice 0/despecify should never really happen except via a feature implication 
	 * @param feature to apply it to, should be standard feature name as seen in symbolDefs (or replacement file) and featImplications (likewise)
	 * 		as this class does not use feature translations; i.e. "stres", "cor", etc. 
	 * @param via_impl -- whether or not this feature is being applied via a feature implication
	 * 		i.e. if so, featSpecs won't be modified, though the feat vect will be
	 * 		and downstream implications will still be triggered either way
	 * 		in practice, as of December 2022, via_impl is always true.
	 * (9/30/25) -- neg alpha value coverage not handled within here ,but within applyAlphaValues 
	 */
	private void apply_value(String newVal, String feature, boolean via_impl)
	{
		int aff_ind = ordFeats.indexOf(feature);
		
		String prevState = ""+featVect.charAt(aff_ind); 
		boolean applyingToAlpha = !UTILS.ALL_FTVECT_INTS.contains(prevState); // UTILS.ALL_FTVECT_INTS.contains(prevState) ? false : UTILS.spec_is_alpha_marked(prevState+feature); <-- this was probably an error. 
		
		boolean alphaResetOverride = applyingToAlpha && featSpecs.contains(newVal+feature); 
			// to overrule the below in cases of partial alpha reset. 
		
		if (featVect.charAt(aff_ind) != '1' && !alphaResetOverride)	return; 	// really this shouldn't ever happen unless it was going to be the same value that was already stored (due to being constructed that way, or due to a prior modification due to filling of alpha values earlier)... may need to put more guard rails here if issues with the feature vector arise		
		featVect = featVect.substring(0, aff_ind) + ftMarkToInt(newVal.charAt(0)) + featVect.substring(aff_ind+1); 
		
		if (!via_impl && !alphaResetOverride) // if it's not via implication 
		{	
			if (featSpecs.contains(feature) )
				System.out.println("Likely error: tried to modify featSpecs for specification of feature "+feature+", but it was already there. Continuing, but you may wish to examine this..."); 
			else	featSpecs += FEAT_DELIM + newVal + feature;  
		}
		
		//for handling any downstream specifications, 
		List<String> impls = new ArrayList<String>(); 
		
		// first any implications contingent to both + and - specification 
		if(UTILS.POLAR_FTSPEC_MARKS.contains(""+newVal) && UTILS.FT_IMPLICATIONS.keySet().contains(feature))
			impls.addAll(Arrays.asList(UTILS.FT_IMPLICATIONS.get(feature))); 
		// then any implications contingent to the specific case observed, with + or with - 
		if(UTILS.FT_IMPLICATIONS.keySet().contains(newVal+feature))
			impls.addAll(Arrays.asList(UTILS.FT_IMPLICATIONS.get(newVal+feature))); 
		
		for (String ii: impls)	apply_value(ii.substring(0,1), ii.substring(1), true); 
	}
	
	@Override
	// alph -- an alpha variable, val {0,1,2,9} the value it'll be set too. 
	public void setAlphaValue(String alph, String val)
	{
		HashMap<String,String> hm = new HashMap<String,String>();
		hm.put(alph, val);
		applyAlphaValues(hm); 
	}
	
	/** 
	 * 
	 * @param alph -- an alpha variable
	 * @return( @global NULL_PROXY_PAIR) if it is neither a negative proxy, nor proxied
	 * 			@else @return the proxy/proxied alpha variable 
	 */
	public String getProxyPair (String alph)
	{	
		if (!hasNegProxyAlphs())	return  UTILS.NULL_PROXY_PAIR; 
		return UTILS.getProxyPair(alph, negProxyAlphs); 
		
		/**
		 * NULL_PROXY_PAIR = UTILS.NULL_PROXY_PAIR;
		if (!hasNegProxyAlphs())	return NULL_PROXY_PAIR; 
		if (negProxyAlphs.containsKey(alph))	return negProxyAlphs.get(alph); 
		if (negProxyAlphs.containsValue(alph))	
			for (String pxi : negProxyAlphs.keySet()) 
				if (negProxyAlphs.get(pxi).equals(alph)) return pxi; 
		return NULL_PROXY_PAIR;*/
	}
	public boolean hasProxyPair (String alph)	{ return !getProxyPair(alph).equals(UTILS.NULL_PROXY_PAIR);	}
	
	@Override
	/** 
	 * @param alphVals -- [key] alpha, [value] the value (2 for + , 0 for -  ...) it is being set to. 
	 * @precondition both the keys [alpha features] and the values [String numerical featvect values] 
	 * 		in alphVals should be one character strings
	 * this class should be called using the outputs of extractAndApplyAlphaValues
	 * on despecification, see notes near the variable DESPEC_VIA_ALPHA.
	  */ 
	public void applyAlphaValues(HashMap<String,String> theAlphVals)
	{
		HashMap<String, String> alphVals = new HashMap<String, String>(theAlphVals); 
		if (alphVals.keySet().size() == 0)	return; 
		if (! hasAlphSpecs )	return; 	// don't apply alpha value filling if there's no values to fill! 
		
		
		// extend to coverage to negative alpha proxies from proxied alphas, or vice versa
		if (hasNegProxyAlphs()) {
			for (String avi : new ArrayList<String>(alphVals.keySet()))
			{
				String proxPair = getProxyPair(avi); // '∅' if there is none. 
				if (proxPair.equals(UTILS.NULL_PROXY_PAIR))	continue; 
				
				String vali = alphVals.get(avi); 
				String oppVal = "" +  UTILS.getOppFtInt(vali);  // opposite value if polar (0 ~ -/ 2 ~ +), otherwise same
					// will throw error if vali is not a valid feature int (0 1 2 9) 
				
				if (!UTILS.POLAR_FTVECT_INTS.contains(vali)
						&& ! (vali.equals(UTILS.DESPEC_INT_CHAR+"") && DESPEC_VIA_ALPHA))	
						continue; 

				//if it's already in here and NOT specified as the opposite value, htere must be an error! Throw it. 
				if (alphVals.containsKey(proxPair)) 
				{	
					if (!alphVals.get(proxPair).equals(oppVal))
						throw new Error("Error: discovered unexpected polarized value for proxy pair:"
								+ "\n\t'"+avi+"'("+alphVals.get(avi)+"); '"+proxPair+"'("+oppVal+")"); 
				
					//must be there already to be marked for opposite value as proxy/proxied
						// in this case, do nothing, don't put it in as a duplicate. 
					else continue; 
				}
				alphVals.put(proxPair,oppVal);
			}
		}
		
		
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
			//s [key] is the current alpha symbol, 
				// every instance of it in the featVect is being changed to the extracted value, val.  
			
			if(alphVals.get(s).length() > 1)
				throw new RuntimeException("Error: value for alpha-specified feature (alpha symbol: "+s+") "
						+ "is more than one character : "+alphVals.get(s)); 
				//alphVals.get(s) must be just one character
				// s must also be one character,
					//but that is for reasons external to this class,
					// as the length of the featVect must be static. 
			
			char val = alphVals.get(s).charAt(0); 
				
			// disallow despecification via alpha unless DESPEC_VIA_ALPHA is true.
			if (val == UTILS.DESPEC_INT_CHAR && !DESPEC_VIA_ALPHA)	continue;
			else if (!UTILS.POLAR_FTVECT_INTS.contains(""+val))	{ // shouldn't have anything other than 0,2, or 9 if this is being fed what was produced by extractAndApplyAlpha ... if there is, something is amiss.
				if (/*val != '1' &&*/ !(val == UTILS.DESPEC_INT_CHAR && DESPEC_VIA_ALPHA))
				{	System.out.println("Alert -- tried to apply a value other than 0,2, or 9 to an alpha-specified feature "
							+ "\n   ... likely error around here. Ignoring for now...");
					//as for other values outside the accepted four, they really shouldn't be allowed, but we're doing this above for now.
					continue;		
				}
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
				if(UTILS.POLAR_FTVECT_INTS.contains(""+val) 
						&& UTILS.FT_IMPLICATIONS.keySet().contains(currSpec))
				{	alphFeatsWImpls.add(currSpec); } 
					//will actually be handled downstream in this method.
				
				// feat specs modification
				int fsloc = featSpecs.indexOf(s+currSpec);	//index of where in featSpecs to modify. 
				featSpecs = featSpecs.substring(0,fsloc) + ftIntToMark(val) + featSpecs.substring(fsloc+1); 
				
				//now the specific specification for implications downstream. 
				currSpec = ftIntToMark(val)+currSpec; 
				if (UTILS.FT_IMPLICATIONS.keySet().contains(currSpec))
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
			String[] impls = UTILS.FT_IMPLICATIONS.get(afii); 
			for (String impc : impls)	
				apply_value(impc.substring(0,1), // although this looks potentially bugged, it won't be, because the second column of implications must always have a specific value associated with the feature (otherwise, the feature implication would be vacuous)
						impc.substring(1), true); 
		}
	}
	
	
	/** 
	 * should always be called before extractAndApplyAlphaValues
	 * bounds do not matter for our purposes here 
	 *		checking for alpha impossibility @specifically in @multiphone items 
	 * 		should skip over juncture phones (i.e. word bounds etc) 
	 * @return @true if @alphaconflict -- conflicting values (that would be) assigned to an @alpha character (if extracted). 
	 * as of 9/30/25 -- will treat non-opposite values between an alpha value and its assigned neg proxy as a feature conflict.
	 *  (if one is a polar value) */ 
	@Override
	public boolean check_for_alpha_conflict(SequentialPhonic inp) 
	{
		if (!inp.getType().equals("phone"))
		{	System.out.println("Warning -- potential error: tried to check for alpha value impossibility of a juncture phone!");
			return false; 
		}
		if (!hasMultifeatAlpha)	return false;	
		
		HashMap<String, String> currReqs = new HashMap<String,String> ();
		char[] cand_feat_vect = inp.toString().split(":")[1].toCharArray(); 
		
		if (cand_feat_vect.length != init_chArr.length)	throw new RuntimeException("tried to check for alpha value impossibility "
				+ "for feat vects of differing length"); 
		for (int c = 0 ; c < cand_feat_vect.length; c++)
		{
			String deepSpec = init_chArr[c] + "";
			if (!UTILS.ALL_FTVECT_INTS.contains(deepSpec)) // deepSpec is an alpha symbol
			{
				String valHere = cand_feat_vect[c] + ""; 
				
				if (currReqs.containsKey(deepSpec)) // and it's already been assigned a value...! 
				{
					if (UTILS.POLAR_FTVECT_INTS.contains(currReqs.get(deepSpec)))
					{	if (!currReqs.get(deepSpec).equals(""+cand_feat_vect[c]))	return true;	}
					else if ((""+UTILS.DESPEC_INT).equals(currReqs.get(deepSpec)))
					{
						if (valHere != ""+UTILS.UNSPEC_INT)	return true; 
					}
					else if ((""+UTILS.UNSPEC_INT).equals(valHere))
					{	currReqs.put(deepSpec, ""+UTILS.DESPEC_INT); }
					else
					{	currReqs.put(deepSpec, valHere); }
				}
				else if ((""+UTILS.UNSPEC_INT).equals(valHere))
				{	currReqs.put(deepSpec, ""+ UTILS.DESPEC_INT); }
				else 
				{	currReqs.put(deepSpec, valHere);
				
					// if it's a polar value and this is a proxy/proxied alpha, put the opposite for the prox pair ... 
					if (hasNegProxyAlphs()? UTILS.POLAR_FTVECT_INTS.contains(valHere) : false) 
						if (hasProxyPair(deepSpec)) 
							currReqs.put(getProxyPair(deepSpec), ""+UTILS.getOppFtInt(valHere)); 
				}
			}
		}
		return false; 	
	}
	
	@Override
	/**
	 *  for a FeatMatrix with either no alpha specs or no UNFILLED alpha specs, @return empty HashMap. 
	 * also @return an empty HashMap if specifications that are not unspecified alpha specs 
	 * 			are inconsistent with @param inp 
	 * - because if these requirements are not met, 
	 * 		the extraction alpha values for a context phone or input phone cannot occur in the first place
	 * 	* as it won't be a valid situation for the operation of the sound change in question 
	* otherwise @apply the value specifications that alpha-valued features have in the SequentialPhonic @param inp
	* 	and then @return those exact value specifications that were applied
	* 		in HashMap with key = alpha symbol, 
	* 			value = feat vect spec (a [String] number ~ 0,1,2, or 9) 
	*/
	public HashMap<String,String> extractAndApplyAlphaValues(SequentialPhonic inp)
	{
		if (first_unset_alpha() == '0')	return new HashMap<String,String>(); 
		
		//output, to be filled.
		HashMap<String, String> currReqs = new HashMap<String,String> ();
		
		char[] cand_feat_vect = inp.toString().split(":")[1].toCharArray(); 
			// "candidate feature vector"
		if (cand_feat_vect.length != featVect.length()) 	throw new RuntimeException("cannot extract alpha values for feat vectors of inconsistent length"); 

		for (int c = 0 ; c < cand_feat_vect.length; c++)
		{
			char fvspec = featVect.charAt(c); 

			if (!UTILS.ALL_FTVECT_INTS.contains(""+fvspec)) // if true, this is a feature with a not-yet-extracted alpha value. 
			{
				if (currReqs.containsKey(""+fvspec))
				{ // value conflict between already-set alpha value, and the (different or redundant) one encountered. 
					String currspec = currReqs.get(""+fvspec); 
					if (currspec.equals(UTILS.DESPEC_INT+"")  && UTILS.UNSPEC_INT_CHAR!=cand_feat_vect[c])
							throw new RuntimeException("Error : Alpha value conflict encountered -- should have called check_for_alpha_conflict() first!"); 
					else	if (!currspec.equals(cand_feat_vect[c]+""))
						throw new RuntimeException("Error : Alpha value conflict encountered -- should have called check_for_alpha_conflict() first!"); 
				}
				else if (cand_feat_vect[c] == UTILS.UNSPEC_INT_CHAR)	// i.e. alpha-symbol, 9 (despecification)
					currReqs.put(""+fvspec, ""+UTILS.DESPEC_INT); // TODO NOTE this is extracted but at present it will NOT be applied unless DESPEC_VIA_ALPHA is true. 
				else	
					currReqs.put(""+fvspec, cand_feat_vect[c]+""); // i.e. alpha symbol, and 0 or 2 (negative, positive)
			
			}
			else if (UTILS.POLAR_FTVECT_INTS.contains(""+fvspec) && fvspec != cand_feat_vect[c] ) //i.e. clash in specified values for the same feature between FeatMatrix and candidate input for a sound change
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
	// doesn't directly engage negative alpha proxies, bc when either a proxy or the proxied alpha is set, the other is set to the opposite. 
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
	public List<String> getAlphaVars()	{	return localAlphabet.equals("") ? new ArrayList<String> ()  
			: Arrays.asList(localAlphabet.split(""));  	}
	
	@Override
	public String toString() 
	{	return "["+
			(hasNegProxyAlphs() ? UTILS.decodeNegAlphProxiesInFeatString(featSpecs,negProxyAlphs) : featSpecs )+"]";		}
	
	/**
	 *  currently used for testing only
	 *  does NOT sub out proxies. 
	 */
	public String getFeatVect() 
	{	return ""+featVect; 	}
	
	@Override
	public String print() {
		return FEAT_MATRIX_PRINT_STMT; //this can be changed for stylistic purposes as long as it is unique with respect to the print outputs of parallel classes
		//TODO however it is changed, it will be necessary to modify various classes that rely on the stability of this symbol, 
			// such as SChangeFeat
	}
	
	public boolean hasNegProxyAlphs()	{	return negProxyAlphs.size() > 0 ;	}
	public HashMap<String,String>	getNegProxyAlphs()	{	return negProxyAlphs;	}
}
