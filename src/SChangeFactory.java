import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap; 
import java.util.Set; 


/**takes in a String of the shift in phonological rule notation
	* and outputs an SChange instance of the right subtype : 
	* i.e. SChangeFeat, SChangeFeatToPhone or SChangePhone
	* depending on appropriate types for the parameters
	* 
	* all phonological rules are of form : 
	* 	Target Source -> Destination / Prior Context __ Posterior COntext
	*	this class will appropriately react to optionality (..), disjunction {..;..;...},
	*	and recursive optional windows such as (..)* and (..)+
*/
	
public class SChangeFactory {
	private static HashMap<String, String> symbToFeatVects; 
	private HashMap<String, String> featVectsToSymb; 
	private static HashMap<String, Integer> featIndices;
	private static HashMap<String, String[]> featImplications; 
	private static Set<String> featsWithImplications; 
	private static Set<String> featNames; 
	
	private final char ARROW = '>'; //separates source target from destination 
	private final char contextFlag = '/'; //signals the beginning of context specification
	// may shift to pipe if the fact that this is used to mark phonemic status in synchronic phonology
	// ... becomes problematic
	private final String LOCUS = "__"; //marks place of the source target relative to the contexts
	// of the shift 
	
	private final char cmtFlag = '$'; //marks taht the text after is a comment in the sound rules file, thus doesn't read the rest of the line
	
	private static final char phDelim = ' '; // delimits phones that are in the same sequence
	private final char segDelim = ';'; // delimits segments that are in disjunctio
	private static final char restrDelim = ','; // delimits restrictiosn between features inside the specification
		// ... for a FeatMatrix : i.e. if "," then the FeatMatrix will be in phonological representation
		// ... as [+A,-B,+C]
	//private final char featVectDelim = ','; //delimiter between features in a FeatMatrix's internal feature vector
		// or a Phone class' feature Vector
		//TODO currently abrogated
	
	private final String[] illegalForPhSymbs = new String[]{"[","]","{","}","__",":",",",";"," ","+","#","@","∅","$",">","/","~"};
	
	
	private boolean boundsMatter; 
	
	//Constructor
	
	public SChangeFactory(HashMap<String, String> stf, HashMap<String,Integer> featInds, HashMap<String, String[]> featImpls)
	{
		boundsMatter = false; //TODO figure out how the user can specify if boundsMatter should be true
		
		symbToFeatVects = new HashMap<String, String>(stf);
		checkForIllegalPhoneSymbols(); 
		
		featNames = featInds.keySet();
		featIndices = new HashMap<String, Integer>();
		for(String feat : featNames)
			featIndices.put(feat, featInds.get(feat)); 
		
		featImplications = new HashMap<String, String[]>(featImpls); 
		featsWithImplications = featImplications.keySet(); 
		
		featVectsToSymb = new HashMap<String, String>(); 
		Set<String> stfKeys = stf.keySet(); 
		for (String key : stfKeys)
		{
			String featdef = stf.get(key); 
			assert featVectsToSymb.containsKey(featdef) == false :  
				"ERROR: duplicate phone definition in symbMap! Duplicate key : " +featdef+", redundant hit for "+key+" with original as "+featVectsToSymb.get(featdef);
			featVectsToSymb.put(featdef, key); 
		}
	}
	
	public void checkForIllegalPhoneSymbols()
	{
		Set<String> phSymbols = symbToFeatVects.keySet(); 
		for(String phSymb : phSymbols)
		{
			for(String illegal : illegalForPhSymbs)
				assert !phSymb.contains(illegal) : "Error, the phone symbol "+phSymb+" contains an illegal part, "+illegal; 
		}
	}
	
	/** generateSChanges
	 * returns a list of Shift instances of the appropriate subclass based on input String,
	 * 		which should be a single change written in phonological rule notation
	 * in most cases this will only have one SChange
	 * however, in some cases of disjunction in the source or the contexts, it is better 
	 * 		to make multiple SChange instances. 
	 */
	public List<SChange> generateSoundChangesFromRule(String inp)
	{
		int cmtStart = inp.indexOf(""+cmtFlag); 
		String input = (cmtStart == -1) ? inp.trim() : inp.substring(0, cmtStart).trim();
		
		List<SChange> output = new ArrayList<SChange>(); 
		
		String[] inputSplit = input.split(""+ARROW); 
		String inputSource = inputSplit[0].trim(), inputParse = inputSplit[1].trim(); 
		String inputDest = inputParse.trim(), inputPrior = "", inputPostr = ""; 
		
		boolean contextSpecified = inputParse.contains(""+contextFlag); 
		boolean priorSpecified = false, postrSpecified = false; 
		if(contextSpecified)
		{
			assert inputParse.contains(LOCUS): "Error: Context flag seen but locus not seen!"; 
			inputSplit = inputParse.split(""+contextFlag); 
			inputDest = inputSplit[0].trim(); 
			inputParse = inputSplit[1].trim();
			inputSplit = inputParse.split(LOCUS); 
			
			postrSpecified = (inputSplit.length == 2 );
			inputPostr = "";
			if(postrSpecified)	inputPostr = inputSplit[1].trim(); 
			if(inputPostr.equals(""))	postrSpecified = false; 
			
			inputPrior = inputSplit[0].trim(); 
			priorSpecified = inputPrior.equals("") == false; 
			
			assert priorSpecified || postrSpecified : 
				"Error : Context flag and locus marker seen, but no specification of either prior or posterior"
				+ "on either side of the locus!"; 

			// in case of disjunction {..,..,..} in the context, use recursion to get all the possibilities
			if(inputPrior.contains("{"))
			{
				assert inputPrior.contains("}") :
					"Error: disjunction opener found but disjunction closer not found";
				assert inputPrior.contains(""+segDelim) :
					"Error: disjunction opener found but disjunction delimiter not found"; 
				int openerInd = inputPrior.indexOf("{"); 
				int braceDepth = 1; 
				int closerInd = openerInd + 8; //7 is the minimum number of characters a disjunction of 
					// FeatMatrices could have in it : +hi;+lo

				assert closerInd < inputPrior.length() : "Error: reached end of inputPrior without finding"
						+ "the corresponding closer of the disjunction which was opened." ; 
				while(! (inputPrior.charAt(closerInd) == '}' && braceDepth == 1))
				{
					if(inputPrior.charAt(closerInd) == '{')	braceDepth++; 
					else if(inputPrior.charAt(closerInd) == '}')	braceDepth--;
					closerInd++;
					assert closerInd < inputPrior.length() : "Error: reached end of inputPrior without finding"
							+ "the corresponding closer of the disjunction which was opened." ; 
				}
				
				String[] disjuncts = inputPrior.split(""+segDelim); 
				for (int di = 0; di < disjuncts.length ; di ++) //recurse.
				{
					output.addAll(generateSoundChangesFromRule(inputSource+ARROW+contextFlag+
							inputPrior.substring(0, openerInd) + phDelim + disjuncts[di] + phDelim + 
							inputPrior.substring(closerInd+1) + LOCUS + inputPostr));
				}
				
				return output; 
			}
			if (inputPostr.contains("{"))
			{
				assert inputPostr.contains("}") :
					"Error: disjunction opener found but disjunction closer not found";
				assert inputPostr.contains(""+segDelim) :
					"Error: disjunction opener found but disjunction delimiter not found"; 
				int openerInd = inputPostr.indexOf("{"); 
				int braceDepth = 1; 
				int closerInd = openerInd + 8; //7 is the minimum number of characters a disjunction of 
					// FeatMatrices could have in it : +hi;+lo

				assert closerInd < inputPostr.length() : "Error: reached end of inputPrior without finding"
						+ "the corresponding closer of the disjunction which was opened." ; 
				
				while(! (inputPostr.charAt(closerInd) == '}' && braceDepth == 1))
				{
					if(inputPostr.charAt(closerInd) == '{')	braceDepth++; 
					else if(inputPostr.charAt(closerInd) == '}')	braceDepth--;
					closerInd++;
					
					assert closerInd < inputPostr.length() : "Error: reached end of inputPrior without finding"
							+ "the corresponding closer of the disjunction which was opened." ;
				}
				
				String[] disjuncts = inputPostr.substring(openerInd+1,closerInd).split(""+segDelim); 
				for (int di = 0; di < disjuncts.length ; di ++) //recurse.
				{
					output.addAll(generateSoundChangesFromRule(inputSource+ARROW+inputDest+contextFlag+
							inputPrior + LOCUS + inputPostr.substring(0, openerInd) +
							disjuncts[di] + inputPostr.substring(closerInd+1))); 
				}
				return output; 				
			}
		}
		
		assert !inputSource.contains("(") && !inputSource.contains(")"): "Error: tried to use optionality"
				+ " features for defining source -- this is forbidden."; 
		assert !inputDest.contains("(") && !inputDest.contains(")") : "Error: tried to use optionality "
				+ "features for defining destination -- this is forbidden.";
		
		//TODO note [ and ] can ONLY be used to surround feature specifications for FeatMatrix
				// otherwise there will be very problematic errors
		boolean srcHasFeatMatrices = inputSource.contains("["); 
		assert srcHasFeatMatrices == inputSource.contains("]"): 
			"Error: mismatch in presence of [ and ], which are correctly used to mark a FeatMatrix specification"; 
		boolean srcHasValidSpecList = hasValidFeatSpecList(inputSource); 
		if(srcHasFeatMatrices) 
			assert srcHasValidSpecList : "Error: usage of brackets without valid feature spec list : "+inputSource; 
		
		if(inputSource.indexOf("]") == inputSource.length() - 1)  // if first index of ] is the last, we know we only have a single feat matrix to deal with. 
			inputSource = inputSource.substring(inputSource.indexOf("[") + 1 , inputSource.indexOf("]")).trim(); 
		if(isValidFeatSpecList(inputSource)) //we are likely dealing with a SChangeFeat then but it could be an SChangeFeatToPhone
		{
			
			RestrictPhone theDest = parseSinglePhonicDest(inputDest); 
			if(theDest.print().equals("#") == false)
			{
				SChangeFeat thisShift = new SChangeFeat(getFeatMatrix(inputSource), theDest, boundsMatter); 
				if(priorSpecified) thisShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
				if(postrSpecified) thisShift.setPostContext(parseNewContext(inputPostr, boundsMatter));
				output.add(thisShift); 
				return output;  
			}
			//if we reach here, we know it is a SChangeFeatToPhone
			List<RestrictPhone> targSource = new ArrayList<RestrictPhone>(); 
			targSource.add(getFeatMatrix(inputSource)); 
			SChangeFeatToPhone thisShift = new SChangeFeatToPhone(featIndices, targSource, 
					parsePhoneSequenceForDest(inputDest), boundsMatter); 
				//errors will be caught by assertions in parsePhoneSequenceForDest
			if(priorSpecified) thisShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
			if(postrSpecified) thisShift.setPostContext(parseNewContext(inputPostr, boundsMatter));
			output.add(thisShift); 
			return output;  
		}
		// if we reach this point, we know the source is not a single FeatMatrix 
		// and the SChange must be an SChangeFeatToPhone or SChangePhone
		
		if(srcHasValidSpecList) // it's an SChangeFeatToPhone
		{
			SChangeFeatToPhone thisShift = new SChangeFeatToPhone(featIndices, 
					parseRestrictPhoneSequence(inputSource), parsePhoneSequenceForDest(inputDest), boundsMatter); 
			if(priorSpecified) thisShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
			if(postrSpecified) thisShift.setPostContext(parseNewContext(inputPostr, boundsMatter));
			output.add(thisShift); 
			return output;  
		}
		
		//if we reach this point, we know we are making an SChangePhone
		
		List<List<SequentialPhonic>> sourceSegs = parseSeqPhDisjunctSegs(inputSource);
		
		//check if making an SChangePhone using FeatMatrices for the dest
		if(hasValidFeatSpecList(inputDest))
		{
			if(inputDest.charAt(0) == '[' && inputDest.indexOf(']') == inputDest.length() - 1)
				inputDest = inputDest.substring(1, inputDest.indexOf(']')); 
			if(isValidFeatSpecList(inputDest))
			{
				ArrayList<RestrictPhone> destMutations = new ArrayList<RestrictPhone>();
				destMutations.add(getFeatMatrix(inputDest, true)) ; 
				SChangePhone newShift = new SChangePhone(sourceSegs, destMutations, boundsMatter);
				if(priorSpecified) newShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
				if(postrSpecified) newShift.setPostContext(parseNewContext(inputPostr, boundsMatter));
				output.add(newShift); 
				return output;
			}
			
			assert !inputDest.contains("{") && !inputDest.contains("}") : 
				"Error: cannot have disjunction braces in the destination for a SChangePhone with feature specified destination -- "
				+ "same mutations must be applied to all disjunctions in the source target, which all must be the same length"; 
			ArrayList<RestrictPhone> destMutations = new ArrayList<RestrictPhone>(parseRestrictPhoneSequence(inputDest, true)); 
			SChangePhone newShift = new SChangePhone(sourceSegs, destMutations, boundsMatter);
			if(priorSpecified) newShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
			if(postrSpecified) newShift.setPostContext(parseNewContext(inputPostr, boundsMatter));
			output.add(newShift); 
			return output;
		}
		
		List<List<SequentialPhonic>> destSegs = parseSeqPhDisjunctSegs(inputDest); 
		assert sourceSegs.size() == destSegs.size() : 
			"Error: mismatch in the number of disjunctions of source segs and disjunctions of dest segs!";
		SChangePhone newShift = new SChangePhone(sourceSegs, destSegs, boundsMatter); 
		if(priorSpecified) newShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
		if(postrSpecified) newShift.setPostContext(parseNewContext(inputPostr, boundsMatter));
		output.add(newShift); 
		return output;
	}
	
	public static List<RestrictPhone> parseRestrictPhoneSequence(String input)
	{
		return parseRestrictPhoneSequence(input, false); 
	}
	
	public static List<RestrictPhone> parseRestrictPhoneSequence(String input, boolean forDestination)
	{
		List<RestrictPhone> output = new ArrayList<RestrictPhone>(); 
		String inputLeft = ""+input; 
		
		while(!inputLeft.equals(""))
		{
			if(inputLeft.charAt(0) == '[')
			{
				int brackEnd = inputLeft.indexOf(']'); 
				output.add(getFeatMatrix(inputLeft.substring(1, brackEnd), forDestination));
				inputLeft = inputLeft.substring(brackEnd + 1).trim(); 
			}
			else if(inputLeft.charAt(0) == '∅')
			{
				output.add(new NullPhone()); 
				inputLeft = inputLeft.substring(1).trim(); 
			}
			else if(symbToFeatVects.containsKey(inputLeft))
			{
				output.add(new Phone(symbToFeatVects.get(inputLeft), featIndices, symbToFeatVects));
				return output; 
			}
			else if(inputLeft.indexOf(phDelim) > 0)
			{
				String toDelim = inputLeft.substring(0, inputLeft.indexOf(phDelim)); 
				assert symbToFeatVects.containsKey(toDelim) :
					"Tried to declare phone with illegitimate symbol : "+toDelim; 
				output.add(new Phone(symbToFeatVects.get(toDelim), featIndices, symbToFeatVects));
				inputLeft = inputLeft.substring(inputLeft.indexOf(phDelim)+1);
			}
			else if(inputLeft.indexOf('[') > 0)
			{
				String toPhone = inputLeft.substring(0, inputLeft.indexOf('['));
				assert symbToFeatVects.containsKey(toPhone) : 
					"Tried to declare phone with illegitimate symbol : "+toPhone;
				output.add(new Phone(symbToFeatVects.get(toPhone), featIndices, symbToFeatVects));
				inputLeft = inputLeft.substring(inputLeft.indexOf('['));
			}
			else
				assert 1==2 : "Tried to parse illegitimate unit : "+inputLeft; 
		}
		
		return output; 
	}
	
	/**parseSinglePhonicDest
	 * return RestrictPhone containing correct parse of the input destination 
	 * if it is not a valid string referring to a single PHonic, then return a word bound. s
	 */
	public RestrictPhone parseSinglePhonicDest(String inp)
	{
		if(inp.equals("∅"))	return new NullPhone(); 
		if(symbToFeatVects.containsKey(inp))	
			return new Phone(symbToFeatVects.get(inp),featIndices,symbToFeatVects);
		String input = inp; 
		if(input.charAt(0) == '[' && input.indexOf("]") == input.length() - 1)
			input = input.substring(input.indexOf("[")+1, input.indexOf("]")); 
		if(isValidFeatSpecList(input))
			return getFeatMatrix(input, true); 
		return new Boundary("word bound");
	}
	
	//used specifically for constructing the destination of a ShiftFeatToPhone instane 
	public List<Phone> parsePhoneSequenceForDest (String inp)
	{
		String[] toPhones = inp.split(""+phDelim); 
		List<Phone> output = new ArrayList<Phone>(); 
		for(String toPhone : toPhones)
		{
			assert symbToFeatVects.containsKey(toPhone): "tried to parse illegitimate phone symbol : "+toPhone;
			output.add(new Phone(symbToFeatVects.get(toPhone), featIndices, symbToFeatVects));
		}
		return output; 
	}
	
	/** parseSeqPhDisjunctSegs
	 * for the posterior creation of an SChangePhone
	 * parse the (disjunctive if necessary) sequential phonic segments for either
	 * the source or the destination
	 * @param input -- either the source or the dest
	 * @return list of disjunctions (if not disjunctive, contains only one) of segments of SequentialPhonic instances
	 */
	private List<List<SequentialPhonic>> parseSeqPhDisjunctSegs (String input)
	{
		List<List<SequentialPhonic>> output = new ArrayList<List<SequentialPhonic>>(); 
		String inp = input.trim(); 
		assert (inp.charAt(0) == '{') == (inp.charAt(inp.length() - 1) == '}') : 
			"Mismatch between presence of disjunction opener and closer for parsing "
			+ "the sequentional phonic segs to make a SChangehPhone"; 
		if(inp.charAt(0) == '{')
		{
			String[] inpSegStrs = inp.substring(1, inp.length() - 1).split(""+segDelim); 
			for (int issi = 0 ; issi < inpSegStrs.length; issi++)
				output.add(parseSeqPhSeg(inpSegStrs[issi])); 
			return output; 
		}
		//if reached this point, it's not disjunctive
		output.add(parseSeqPhSeg(inp)); 
		return output; 
	}
	
	public List<SequentialPhonic> parseSeqPhSeg (String inp)
	{
		List<SequentialPhonic> output = new ArrayList<SequentialPhonic>(); 
		String[] phsInSeg = inp.trim().split(""+phDelim); 
		for (int pisi = 0; pisi < phsInSeg.length; pisi++)
		{	if(!phsInSeg[pisi].equals("∅"))
				output.add(parseSeqPh(phsInSeg[pisi].trim()));
		}
		return output;
	}
	
	private SequentialPhonic parseSeqPh (String curtp)
	{
		if("+#".contains(curtp))
			return new Boundary("#".equals(curtp) ? "word " : "morph " + "bound"); 
		assert symbToFeatVects.containsKey(curtp) : "Error: tried to parse invalid symbol! Symbol : "+curtp; 
		return new Phone(symbToFeatVects.get(curtp), featIndices, symbToFeatVects); 
	}
	
	/** parseNewContext 
	 * constructs a new ShiftContext instance
	 * with variables currRestrList and currParenMap to match the parse of this new contxt
	 * currRestrList -- all the RestrictPhones i.e. specifications on each context phone -- i.e. FeatMatrix or Boundary instances
	 * while parenMap is a String[] that is a "map" of where parenthetical structures apply
	 * ... structured as illustrated by this example (the top row is the indices in PARENMAP)
	 * 
	 * 		0	|	1	|	2	|	3	|	4 	|	5	|	6	|	7
	 * 		i0 	| +(:4	| 	i1 	| 	i2 	| )+:1	| 	(:7	| 	i3	|	):5 
	 * cells with contents starting i indicate that the cell corresponds to the index of the number following 
	 * 		in placeRestrs
	 * cells with paren markers { +(, )+, (, ), *(, )*, } indicate where parens open and close
	 * 		relative to those indices in parenMap
	 * 		the number on the inside of hte paren indicates which index IN PARENMAP 
	 * 			is where the corresponding opening or closing paren lies.
	 * @param inp -- raw input for the context specifications  
	 * @param boundsMatter -- determines whether the context restrictions we create will pass over boundaries 
	 * 		in input for context matching checker functions 
	 * @precondition : all elements separated phDelim 
	 * @return
	 */
	//TODO finish fixing this  
	public SChangeContext parseNewContext(String input, boolean boundsMatter)
	{
		String inp = forceParenSpaceConsistency(input); //force single spaces on spaces surrounding
			//parenthetical symbols, in order to standardize and make errors more controllable as code expands
		inp = expandOutAllPlusses(inp);
		
		String[] toPhones = inp.trim().split(""+phDelim); // given the method above
			// this should force parenthesis statements to be separate "phones" from the actual phones
		
		// iteratively built throughout process 
		List<String> parenMapInProgress = new ArrayList<String>();
		List<RestrictPhone> thePlaceRestrs = new ArrayList<RestrictPhone>(); 
		
		//as we build, associate opening and closing parens -- ):# ; (:# 
		for (int i = 0 ; i < toPhones.length; i++)
		{
			String curtp = toPhones[i].trim(); 
			
			if(curtp.equals("("))
				parenMapInProgress.add(curtp); 
			else if(curtp.contains(")"))
			{
				boolean rec = false; 
				if(!curtp.equals(")"))
				{	rec =true ;	assert curtp.equals(")*") || curtp.equals(")+"): 
						"Error: illegitimate use of closing bracket : "+curtp; }
				int corrOpenIndex = parenMapInProgress.lastIndexOf("(");
					//index of corresponding opening paren. 
				
				parenMapInProgress.set(corrOpenIndex, 
						(rec ? curtp.charAt(1)+"" : "")+"(:"+parenMapInProgress.size());
				parenMapInProgress.add(curtp+":"+corrOpenIndex); 
			}
			else
			{
				if(curtp.charAt(0) == '[')
				{
					assert curtp.charAt(curtp.length() - 1) == ']' || curtp.length() > 3 : 
						"Error : illegitimate use of FeatMatrix brackets : "+curtp;
					curtp = curtp.substring(1, curtp.length() - 1);
				}
				assert !curtp.contains("[") || !curtp.contains("]") : 
					"Error : illegitimate usage of brackets " + curtp; 
				
				parenMapInProgress.add("i"+thePlaceRestrs.size()) ;
				if(symbToFeatVects.containsKey(curtp))
					thePlaceRestrs.add(new Phone(symbToFeatVects.get(curtp), featIndices, symbToFeatVects));
				else if ("#+".contains(curtp))
					thePlaceRestrs.add(new Boundary(("#".equals(curtp) ? "word " : "morph ") + "bound"));
				else if ("@".equals(curtp))
					thePlaceRestrs.add(new Boundary("non word bound"));
				else
				{
					assert (curtp.charAt(0) == '[') == (curtp.charAt(curtp.length() - 1) == ']'): 
						"Error : mismatch between presenced of opening bracket and presence "
						+ "of closing bracket"; 
					if(curtp.charAt(0) == '[')
						curtp = curtp.substring(1, curtp.length() - 1).trim(); 
					assert isValidFeatSpecList(curtp): 
						"Error: had to preempt attempted construction of a FeatMatrix instance"
						+ "with an invalid entrace for the list of feature specifications.";
					
					thePlaceRestrs.add(getFeatMatrix(curtp));  
				}
			}
		}
		

		String[] theParenMap = new String[parenMapInProgress.size()];
		theParenMap = parenMapInProgress.toArray(theParenMap); 
		
		return new SChangeContext(thePlaceRestrs, theParenMap, boundsMatter) ;
	}
	
	/** isValidFeatSpecList
	 * @return @true iff @param input consists of a list of valid feature specifications 
	 * 	each delimited by restrDelim
	 */
	public static boolean isValidFeatSpecList(String input)
	{
		String[] specs = input.split(""+restrDelim); 
		
		for(int si = 0; si < specs.length; si++)
		{	
			if("+-.".contains(specs[si].substring(0,1)))
			{	if(!featNames.contains(specs[si].substring(1)))	return false;	}
			else if(!featNames.contains(specs[si]))	return false; 
		}
		return true; 
	}
	
	//hasValidFeatSpecList
	// breaks string up according to delimiter phDelim 
	// and @return true if any of hte components describe a feat vector
	
	private boolean hasValidFeatSpecList(String inp)
	{
		if(isValidFeatSpecList(inp.trim()))		return true; 
		String[] protophones = inp.split(""+phDelim);
		for(int ppi = 0; ppi < protophones.length; ppi++)
		{
			String curpp = ""+protophones[ppi].trim();
			while(curpp.contains("["))	curpp = curpp.substring(curpp.indexOf('[')+1);
			while(curpp.contains("]"))	curpp = curpp.substring(0, curpp.indexOf(']'));
			if(isValidFeatSpecList(curpp))	return true; 
		}
		return false; 
	}

	public static FeatMatrix getFeatMatrix(String featSpecs)
	{	return getFeatMatrix(featSpecs, false);	}
	
	//derives FeatMatrix object instance from String of featSpec instances
	public static FeatMatrix getFeatMatrix(String featSpecs, boolean isInputDest)
	{
		assert isValidFeatSpecList(featSpecs) : "Error : preempted attempt to get FeatMatrix from an invalid list of feature specifications" ; 
		
		String theFeatSpecs = isInputDest ? applyImplications(featSpecs) : featSpecs+"";
		
		if(theFeatSpecs.contains(".") == false)
			return new FeatMatrix(theFeatSpecs, featIndices); 
				
		//TODO we should make sure someone doesn't insert use "unspecification" -- i.e. period '.' as a SPECIFICATION
		assert(!theFeatSpecs.contains(".") || isInputDest): 
			"Error : despecification used for a FeatMatrix that is not in the destination -- this is inappropriate."; 
		return new FeatMatrix(theFeatSpecs, featIndices); 
	}
	
	/**	applyImplications
	 * modifies a list of features which will presumably be used to define a FeatMatrix 
	 * so that the implications regarding the specification or non-specifications of certain features are adhered to 
	 * @param featSpecs, feature specifications before application of the stored implications
	 */
	public static String applyImplications (String featSpecs) 
	{
		assert isValidFeatSpecList(featSpecs) : "Error : preempted attempt to apply implications to an invalid list of feature specifications" ; 
		String[] theFeatSpecs = featSpecs.trim().split(""+restrDelim); 
		String output = ""+featSpecs; 
		
		for(int fsi = 0 ; fsi < theFeatSpecs.length; fsi++)
		{
			String currSpec = theFeatSpecs[fsi]; 
			
			if(featsWithImplications.contains(currSpec)) 
			{
				String[] implications = featImplications.get(currSpec); 
				for (int ii = 0; ii < implications.length; ii++)
				{	if (output.contains(implications[ii].substring(1)) == false)
					{	
						output += restrDelim + implications[ii];
						theFeatSpecs = output.trim().split(""+restrDelim); 
					}
				}
			}
			if("+-".contains(currSpec.substring(0,1)))
			{
				if(featsWithImplications.contains(currSpec.substring(1)))
				{
					String[] implications = featImplications.get(currSpec.substring(1)); 
					for (int ii=0; ii < implications.length; ii++)
					{	if(output.contains(implications[ii]) == false)
						{
							output += restrDelim + implications[ii]; 
							theFeatSpecs = output.trim().split(""+restrDelim); 
						}
					}
				}
			}
		}
		
		return output; 
	}
	
	//TODO abrogate this -- it doesn't seem necessary 
	/** parseBoundaryType -- given @param inp, boundary symbol in phonological rule notation 
	 * @return	output the operation type name within this package's paradigm
	 */
	public String parseBoundaryType(String inp)
	{
		assert Arrays.asList(new String[]{"+","#","@"}).contains(inp) : 
			"Error: String inp is not a valid boundary symbol"; 
		if(inp.equals("#"))	return "word bound"; 
		if(inp.equals("+"))	return "morph bound";
		else /*inp.equals("@"))*/  return "non word bound"; 
	}
	
	public String forceParenSpaceConsistency(String input)
	{
		String output = input; 
		int i = 0; 
		while ( i < output.length() - 1)
		{
			if(i < output.length() - 2)
			{
				if(output.charAt(i) == ' ' && output.charAt(i+1) == ' ')
				{
					output = output.substring(0,i+1) + output.substring(i+2); i+=2; 
				}
			}
			if(i < output.length() - 2)
			{	if(output.charAt(i) == '(')
				{
					if(i > 0)
						if (output.charAt(i-1) != ' ')
						{	output = output.substring(0, i) + " " + output.substring(i); i++;	}
					if(output.charAt(i+1) != ' ')
					{	output = output.substring(0, i+1) + " "+ output.substring(i+1); i++;	}
				}
			}
			i++; 
			if(output.charAt(i) == ')')
			{
				if(output.charAt(i-1) != ' ')
				{	output = output.substring(0,i) + " " + output.substring(i); i++;	}
				if( i < output.length() - 1)
				{	
					if("*+".contains(output.charAt(i+1)+""))
					{	if( i < output.length() - 2)
						{	if(output.charAt(i+2) != ' ')
							{
								output = output.substring(0, i+2) + " " + output.substring(i+2); 
								i += 3;
							}}}
					else 
					{
						if(output.charAt(i+1) != ' ')
						{	output = output.substring(0, i+1) + " " + output.substring(i+1); 
							i += 2;	}
					}
				}
			}
		}
		return output.trim();
	}
	
	/**
	 * given @param s, a String of place specifications with at least one paren statement -- i.e. A B ( C D)  etc...
	 * and @param openInd, the index of a particular opening paren (
	 * @return ihdex of the corresponding closing paren )  
	 */
	public int findClosingInd (String s, int openInd)
	{
		assert openInd < s.length() - 2 : "Error : findClosingInd called with openInd set to value too high";
		assert s.charAt(openInd) == '(' : "Error : findClosingInd called with openInd set to an index where a '(' does not lie!";
		assert s.charAt(openInd + 1) != ')' : "Error: closing paren found immediately after opening paren-- this is useless to write";
		int checkInd = openInd + 2;  
		int parenDepth = 1; 
		
		while (checkInd < s.length())
		{
			if(s.charAt(checkInd) == ')' )
			{
				if(parenDepth == 1)	return checkInd;
				else parenDepth--; 
			}
			else if (s.charAt(checkInd) == '(')	parenDepth ++; 
			
			checkInd++; 
		}
		throw new Error ("Error : Reached end of string in findClosingInd and corresponding closing paren was nowhere to be found");
	}
	
	
	/**
	 * given @param s, a String of place specifications with at least one paren statement -- i.e. A B ( C D)  etc...
	 * and @param closInd, the index of a particular closing paren )
	 * @return ihdex of the corresponding opening paren (
	 */
	public int findOpenInd(String s, int closInd)
	{
		assert closInd > 1 : "Error: closing ind too low"; 
		assert s.charAt(closInd) == ')' : "Error : closing paren ) does not lie at closing ind";
		assert s.charAt(closInd - 1) != '(' : "Error : opening paren immediately before closer () -- this is useless to write"; 
		
		int checkInd = closInd - 2 ; 
		int parenDepth = 1 ; 
		while (checkInd >= 0)
		{
			if(s.charAt(checkInd) == '(')
			{
				if (parenDepth == 1)	return checkInd;
				parenDepth--;
			}
			else if (s.charAt(checkInd) == ')')	parenDepth++;
		}
		throw new Error("Error : no opening ind found");
	}
	
	/** 
	 * @param s, a string of place specifications with at least one ( ... )+ clause -- i.e. indicated contents must occur once or more --
	 * and @param ind, index of a ( closed by a )+, 
	 * @return version of string s modified such taht ( A B C )+ becomes A B C ( A B C )*
	 */
	public String expandOutPlus (String s, int ind)
	{
		assert s.charAt(ind) == '(' : "error: expandOutPlus called with ind that doesn't have a '(' ";
		int corrClosInd = findClosingInd(s, ind); 
		assert corrClosInd < s.length() - 1 : "Error: expandOutPlus called for '(' that isn't a + paren ";
		assert s.charAt(corrClosInd + 1) == '+': "Error: expandOutPlus called for paren that isn't a + paren"; 
	
		int start = s.charAt(ind + 1) == ' ' ? ind + 2 : ind + 1; 
		int end = s.charAt(corrClosInd - 1) == ' ' ? corrClosInd - 1 : corrClosInd; 
		String insidePlusParen = s.substring(start, end); 
		
		String output = s.substring(0, ind) + insidePlusParen + " ( "+insidePlusParen+" )*";
	
		if(corrClosInd + 2 < s.length())
		{
			int startAfter = s.charAt(corrClosInd + 2) == ' ' ? corrClosInd + 3 : corrClosInd + 2;
			output += " "+ s.substring(startAfter);
		}
		
		return output;
	}
	
	/** expandOutAllPlusses
	 * given @param s, a String with at least one ( ... )+ "one or more of " clause
	 * convert it to ... (...)* -- the first as plain text and the second an "any number of repeats" clause
	 * 		for purposes of computational convenience 
	 */
	public String expandOutAllPlusses(String s)
	{	
		String output = s+""; 
		int checkInd = 0; 
		while ( checkInd < output.length() - 2)	{
			if( output.charAt(checkInd) == '(')
			{
				int checkClose = findClosingInd(output, checkInd); 
				
				if(checkClose < output.length() - 1)
				{
					if(output.charAt(checkClose + 1 ) == '+')
						output = expandOutPlus(output, checkInd); 
					else	checkInd++; 
				}
				else	checkInd++; 
			}
			else	checkInd++;
		}
		return output; 
	}
	
}
