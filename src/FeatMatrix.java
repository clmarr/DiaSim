import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class FeatMatrix extends Phonic implements RestrictPhone {
	
	private char[] init_chArr; //retains mark of alpha values-- whereas they assume there functional values in featVect.
	private String featVect; // by default a string of 1s, one for each feature
		// as they become specified they become either 0(neg) or 2(pos)
		// despecification -- i.e. arising only because of feature implications,
			// the change of a feature from +/- to . in unspecified in a phone operated upon. 
		// DESPECIFICATION of phones as part of the FeatMatrix is represented as a 9 in FeatSpecs	
	private final char FEAT_DELIM = ','; 
	private String featSpecs, initSpecs; //"+cor,-dist" etc... separated by FEAT_DELIM 
		// will always return to initSpecs after alphas are reset. 
		// initSpecs, once set, must not under any circumstance be changed.
	
	private List<String> ordFeats; 
	
	private HashMap<String, String[]> featImpls; 
	
	private String LOCAL_ALPHABET; // for handling alpha notation 
	public static final String FEAT_MATRIX_PRINT_STMT = " @%@ "; 
	private boolean hasAlphSpecs; 
	private boolean hasMultispecAlpha; 	
	// DESPECIFICATION -- 
		// where due to FEATURE IMPLICATIONS, a feature must be despecified -- i.e. set back to unspecified 
		// example: if a vowel goes from -cont to +cont, the feature delrel should be despecified
		// this case is the only time we will ever make use of the List<String> despecifications
		// which is what is stored in the featVect for this case. 
		// 
	/**
	 * version of constructor with featSpecs passed directly
	 * should be passed with , delimiters and +/- indicators 
	 */
	// 
	public FeatMatrix(String specs, List<String> orderedFeats, HashMap<String, String[]> ftImpls)
	{
		assert specs.length() > 1 : "Invalid string entered for specs"; 
		LOCAL_ALPHABET = "";
		hasMultispecAlpha = false;
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
			{	if (!LOCAL_ALPHABET.contains(indic))	LOCAL_ALPHABET += indic; 
				else	hasMultispecAlpha = true;
			}
			String feat = sp.substring(1); 
			assert ordFeats.contains(feat): "ERROR: tried to add invalid feature : '"+feat+"'";
			
			int spInd= Integer.parseInt(""+ordFeats.indexOf(feat)); 
			init_chArr[spInd] = is_alph ? indic.charAt(0) : 
				("+".equals(indic) ? '2' : ("0".equals(indic) ? '9' : '0' ));  
			// thus, after this init_chArr will have 0 for neg specd features, 2 for pos specd features, 9 for despecd features
			// for alpha specified features, the alpha (or whatever other dummy symbol is used) is left in the vector
				// until we despecify it later. 
				//... and meanwhile, we have 1 for those that were untouched. 
			
		}
		featVect = new String(init_chArr); 
		hasAlphSpecs = LOCAL_ALPHABET.length() > 0; 
	}
		
	/**
	 * checks if candidate phone adheres to the restrictiosn
	 */
	public boolean compare(SequentialPhonic cand)
	{
		if (!cand.getType().equals("phone"))
			return false; 
		
		char nonSet = first_unset_alpha();
		assert nonSet == '0': "ERROR: tried to compare when alpha style symbol '"+nonSet+"' remains uninitialized";
		
		String candFeats = cand.toString().split(":")[1]; 
		assert candFeats.length() == featVect.length(): 
			"ERROR: comparing with feature vects of unequal length"; 
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
	 * @param candPhonSeq -- whole segment we are testing. This is only really necessary because
	 * 		we need to implement this method so this class implements interface RestrictPhone
	 * @param index -- index of interest. See above.
	 * @return
	 */
	public boolean compare(List<SequentialPhonic> candPhonSeq, int index)
	{	return compare(candPhonSeq.get(index));		}
	
	/**
	 *  makes all the restrictions specified in this FeatMatrix true for @param patient
	 * by changing any necessary feature values in patient 
	 * @precondition: they have the same length feature vectors*/
	public Phone forceTruth(Phone patient)
	{
		char nonSet = first_unset_alpha();
		
		assert nonSet == '0' :"ERROR: tried to force truth when alpha style symbol '"+nonSet+"' remains uninitialized"; 			
		
		Phone output = new Phone(patient); 
		String patFeats = patient.getFeatString();
		assert patFeats.length() == featVect.length():
			"ERROR: trying to forceTruths on phone with different length feat vector";
			// technically it could still function if this wasn't the case, but for security best to call it out anyways
		
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
	 * 
	 * @param patientSeq
	 * @param ind
	 * @return
	 */
	public List<SequentialPhonic> forceTruth (List<SequentialPhonic> patientSeq, int ind)
	{
		char nonSet = first_unset_alpha();
		if( nonSet != '0')
			throw new UnsetAlphaError(""+nonSet); 
		
		SequentialPhonic patient = patientSeq.get(ind); 
		assert patient.getType().equals("phone"): "ERROR: trying to force cand restrictions on non-phone!";
		List<SequentialPhonic> outSeq = new ArrayList<SequentialPhonic>(patientSeq); 
		outSeq.set(ind, forceTruth(new Phone(patient)));
		return outSeq;
	}
	
	@Override
	public String toString() 
	{	return "["+featSpecs+"]";		}
	
	//TODO currently used for testing only
	public String getFeatVect() 
	{	return featVect; 	}
	
	@Override
	public String print() {
		return FEAT_MATRIX_PRINT_STMT; //this can be changed for stylistic purposes as long as it is unique with respect to the print outputs of parallel classes
		//TODO however it is changed it will be necessary to modify various classes that rely on teh stability of this symbol, 
			// such as SChangeFeat
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
	
	@Override
	public void applyAlphaValues(HashMap<String,String> alphVals)
	{
		for (String s : alphVals.keySet())
		{
			char val = alphVals.get(s).charAt(0); //alphVals.get(s) be just one character -- may need to throw an Error if this is not the case.  
			
			//TODO debugging
			System.out.println("alph "+s+" val "+val);
			
			while(featVect.contains(s))
			{
				int nxind = featVect.indexOf(s); 
				
				//TODO debugging
				System.out.println("nxind "+nxind);
				
				featVect = featVect.substring(0, nxind) + val + featVect.substring(nxind+1); 
				String currSpec = toSurfVal(val)+ordFeats.get(nxind);
				
				if (featImpls.containsKey(currSpec))
				{
					String[] impls = featImpls.get(currSpec);
					for (String impc : impls)
					{
						int aff_ind = ordFeats.indexOf(impc.substring(1));
						
						if (featVect.charAt(aff_ind) == '1')
							featVect = featVect.substring(0, aff_ind) + fromSurfVal(impc.charAt(0)) + featVect.substring(aff_ind+1); 
					}
				}
			}
		}
	}
	
	// should always be called before extract_alpha_values
	// bounds do not matter for our purposes here -- checking for alpha impossibility in multiphone itmes should skip over juncture phones. 
	@Override
	public boolean check_for_alpha_conflict(SequentialPhonic inp) 
	{
		assert inp.getType().equals("phone") : "Error: tried to check for alpha value impossibility of a juncture phone!";
		if (!hasMultispecAlpha)	return false;	
		
		HashMap<String, String> currReqs = new HashMap<String,String> ();
		char[] cand_feat_vect = inp.toString().split(":")[1].toCharArray(); 
		
		assert cand_feat_vect.length == init_chArr.length : "Error: apparently inconsistent length";
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
	// for a FeatMatrix with either no alpha specs or no UNFILLED alpha specs, returns empty HashMap. 
	// also returns an empty HashMap if specifications that are not unspecified alpha specs are inconsistent with @param inp
	// otherwise returns the values that alpha specs have in SequentialPhonic @param inp. 
	public HashMap<String,String> extract_alpha_values(SequentialPhonic inp)
	{
		if (first_unset_alpha() == '0')	return new HashMap<String,String>(); 
		HashMap<String, String> currReqs = new HashMap<String,String> ();
		char[] cand_feat_vect = inp.toString().split(":")[1].toCharArray(); 
		
		assert cand_feat_vect.length == featVect.length() : "Error: apparently inconsistent length";
		
		for (int c = 0 ; c < cand_feat_vect.length; c++)
		{
			char fvspec = featVect.charAt(c); 
			if (!"0192".contains(""+fvspec)) // if true, this is a feature with a not-yet-extracted alpha value. 
			{
				//String currInpSpec = cand_feat_vect[c] + 
				if (currReqs.containsKey(""+fvspec))
				{ // value conflict between already-set alpha value, and the (different or redundant) one encountered. 
					String currspec = currReqs.get(""+fvspec); 
					if (currspec.equals("9"))	assert '1'==cand_feat_vect[c]: 
							"Error : Alpha value conflict encountered -- should have called check_for_alpha_conflict() first!"; 
					else	assert currspec.equals(cand_feat_vect[c]+""): 
						"Error : Alpha value conflict encountered -- should have called check_for_alpha_conflict() first!"; 
				}
				else if (cand_feat_vect[c] == '1')	currReqs.put(""+fvspec, "9"); 
				else	currReqs.put(""+fvspec, cand_feat_vect[c]+""); 
			}
			else if ("02".contains(""+fvspec) && fvspec != cand_feat_vect[c] )
				return new HashMap<String,String>(); 
		}
		applyAlphaValues(currReqs);
		
		return currReqs; 
	}
	
	@Override
	public boolean has_alpha_specs()	{	return hasAlphSpecs;	} 
	public boolean has_multispec_alph() {	return hasMultispecAlpha;	}
	
	// returns '1' if not set
	// otherwise the first alpha value detected that has not become a number, in featVect
	@Override
	public char first_unset_alpha()
	{
		if (LOCAL_ALPHABET.length() > 0)
			for (char c : LOCAL_ALPHABET.toCharArray())
				if(featVect.contains(""+c))	return c; 
		
		return '0';
	}
	
	public String getStrInitChArr()
	{	return String.copyValueOf(init_chArr);	}
}
