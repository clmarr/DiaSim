import java.util.List;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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

//TODO fix this so it can handle FeatureImplications 
	
public class SChangeFactory {
	private HashMap<String, String> symbToFeatVects; 
	private HashMap<String, String> featVectsToSymb; 
	private HashMap<String, Integer> featIndices;
	private HashMap<String, String[]> featImplications; 
	private Set<String> featsWithImplications; 
	private Set<String> featNames; 
	
	private final char ARROW = '>'; //separates source target from destination 
	private final char contextFlag = '/'; //signals the beginning of context specification
	// may shift to pipe if the fact that this is used to mark phonemic status in synchronic phonology
	// ... becomes problematic
	private final String LOCUS = "__"; //marks place of the source target relative to the contexts
	// of the shift 
	
	private final char cmtFlag = '$'; //marks taht the text after is a comment in the sound rules file, thus doesn't read the rest of the line
	
	private final char phDelim = ' '; // delimits phones that are in the same sequence
	private final char segDelim = ';'; // delimits segments that are in disjunctio
	private final char restrDelim = ','; // delimits restrictiosn between features inside the specification
		// ... for a FeatMatrix : i.e. if "," then the FeatMatrix will be in phonological representation
		// ... as [+A,-B,+C]
	//private final char featVectDelim = ','; //delimiter between features in a FeatMatrix's internal feature vector
		// or a Phone class' feature Vector
		//TODO currently abrogated
	
	private boolean boundsMatter; 
	
	//Constructor
	
	public SChangeFactory(HashMap<String, String> stf, HashMap<String,Integer> featInds, HashMap<String, String[]> featImpls)
	{
		boundsMatter = false; //TODO ffigure out how the user can specify if boundsMatter should be true
		
		symbToFeatVects = new HashMap<String, String>(stf);
		featIndices = new HashMap<String, Integer>(featInds); 
		featNames = featInds.keySet();
		featImplications = new HashMap<String, String[]>(featImpls); 
		featsWithImplications = featImplications.keySet(); 
		
		featVectsToSymb = new HashMap<String, String>(); 
		Set<String> stfKeys = stf.keySet(); 
		for (String key : stfKeys)
		{
			String featdef = stf.get(key); 
			assert featVectsToSymb.containsKey(featdef) == false : 
				"ERROR: duplicate phone definition in symbMap!";
			featVectsToSymb.put(featdef, key); 
		}
	}
	
	/**
	 * collects a list, in order, of all the sound changes instances implied by the rules indicated
	 * 		in the text of the rules file at @param ruleFileLoc
	 * @precondition : @param ruleFileLoc is a valid and correct location of the rules file. 
	 * @return list of SChange instances for all rules, in order
	 */
	public List<SChange> collectAllChangesFromRulesFile(String ruleFileLoc)
	{
		List<SChange> output = new ArrayList<SChange>(); 
		List<String> ruleLines = new ArrayList<String>(); 
		String nextLine; 
		
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream(ruleFileLoc), "UTF-8")); 
			while((nextLine = in.readLine()) != null)	
			{
				String lineWoComments = ""+nextLine;
				if(lineWoComments.contains(""+cmtFlag))
					lineWoComments = lineWoComments.substring(0, lineWoComments.indexOf(""+cmtFlag));
				if(!lineWoComments.trim().equals(""))	ruleLines.add(lineWoComments); 
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
		
		for(String ruleLine : ruleLines)
		{	//TODO debugging
			System.out.println("Generating sound change for: "+ruleLine);
			List<SChange> schangesForThisRule = generateSoundChanges(ruleLine); 
			output.addAll(schangesForThisRule);
			//TODO debugging
			System.out.println("Generated these sound changes: ");
			for (SChange shift : schangesForThisRule)
				System.out.println(""+shift);
			
		}
		return output; 
	}
	
	
	/** generateSChanges
	 * returns a list of Shift instances of the appropriate subclass based on input String,
	 * 		which should be a single change written in phonological rule notation
	 * in most cases this will only have one SChange
	 * however, in some cases of disjunction in the source or the contexts, it is better 
	 * 		to make multiple SChange instances. 
	 */
	public List<SChange> generateSoundChanges(String inp)
	{
		String input = (inp.indexOf(""+cmtFlag) == -1) ? inp.trim() : inp.substring(0,inp.indexOf(""+cmtFlag)).trim(); 
		
		List<SChange> output = new ArrayList<SChange>(); 
		
		String[] inputSplit = input.split(""+ARROW);
		String inputSource = inputSplit[0].trim(), inputParse = inputSplit[1].trim(); 
		
		String inputDest = inputParse, inputPrior = "", inputPostr = ""; 
		
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
					output.addAll(generateSoundChanges(inputSource+ARROW+contextFlag+
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
					output.addAll(generateSoundChanges(inputSource+ARROW+inputDest+contextFlag+
							inputPrior + LOCUS + inputPostr.substring(0, openerInd) +
							disjuncts[di] + inputPostr.substring(closerInd+1))); 
				}
				return output; 				
			}
		}
		
		//TODO note [ and ] can ONLY be used to surround feature specifications for FeatMatrix
				// otherwise there will be very problematic errors
		
		assert !inputSource.contains("(") && !inputSource.contains(")"): "Error: tried to use optionality"
				+ " features for defining source -- this is forbidden."; 
		assert !inputDest.contains("(") && !inputDest.contains(")") : "Error: tried to use optionality "
				+ "features for defining destination -- this is forbidden.";
		
		//parse of target source
		boolean srcHasFeatMatrices = inputSource.contains("["); 
		assert srcHasFeatMatrices == inputSource.contains("]"): 
			"Error: mismatch in presence of [ and ], which are correctly used to mark a FeatMatrix specification"; 
		if(hasValidFeatSpecList(inputSource)) //i.e. source should be characterized by RestrictPhone instances
		{
			assert !inputSource.contains("{") && !inputSource.contains("}") : 
				"Error : preempted disjunction applied to source of shift with feature-defined source -- these "
				+ "are not permitted in the current version of this application" ; 
			
			assert !inputDest.contains("{") && !inputDest.contains("}") : 
				"Error : preempted disjnction applied to destination of shift with feature-defined source --"
				+ " these are not permitted. " ;
			
			//TODO parse the correct either SChangeFeat or SChangeFeatToPhone, and return 
			if(inputDest.charAt(0) == '[') // then we are dealing with a SChangeFeat
			{
				assert inputDest.charAt(inputDest.length() - 1) == ']' : 
					"Error: Expected single FeatMatrix as destination for a new SChangeFeat to be constructed,"
					+ "and found first char '[' but ']' was not the last character"; //TODO reword this
				assert isValidFeatSpecList(inputDest.substring(1, inputDest.length() - 1)):
					"Error : preempted construction of FeatMatrix destination that was not defined "
					+ "by a valid list of feature specifications." ; 
				assert inputSource.charAt(0) == '[' && inputSource.charAt(inputSource.length() - 1) == ']': 
					"Error : we are constructing a SChangeFeat which requires a single FeatMatrix for the "
					+ "source and one for the destination, we know the source has brackets, but they are in "
					+ "the wrong places in the source -- they should be at the beginning and end if they are present.";
				SChangeFeat newShift = new SChangeFeat(getFeatMatrix(inputSource.substring(1, inputSource.length() - 1)),
						getFeatMatrix(inputDest.substring(1, inputDest.length() - 1)), boundsMatter); 
				output.add(newShift); 
				return output; 
			}
			assert !inputDest.contains("[") && !inputDest.contains("]"): "Invalid bracket found in "
					+ "destination; brackets should only be used to surround feat specs for a FeatMatrix"; 
			if(isValidFeatSpecList(inputDest)) // i.e. single FeatMatrix specified without brackets
			{
				output.add(new SChangeFeat(getFeatMatrix(inputSource), getFeatMatrix(inputDest), boundsMatter)); 
				return output;
			}
			else	//it's a SChangeFeatToPhone instance. 
			{		
				String[] sourcePlaceSpecs = inputSource.split(""+phDelim), destPlaceSpecs = inputDest.split(""+phDelim); 
								
				List<RestrictPhone> sourcePlaces = new ArrayList<RestrictPhone>(); 
				for(int spsi = 0; spsi < sourcePlaceSpecs.length; spsi++)
				{
					String currPlaceSpec = sourcePlaceSpecs[spsi].trim();
					assert !"+#".contains(currPlaceSpec) : "Boundaries not allowed for source specification"; 
					if(symbToFeatVects.containsKey(currPlaceSpec))
						sourcePlaces.add(new Phone(symbToFeatVects.get(currPlaceSpec), featIndices, symbToFeatVects));
					else if(currPlaceSpec.charAt(0) == '[')
					{
						assert currPlaceSpec.charAt(currPlaceSpec.length() - 1 ) == ']': "Error: illegitimate brace usage "
								+ "in source specifications."; 
						assert isValidFeatSpecList(currPlaceSpec.substring(1, currPlaceSpec.length() - 1)) : "Error : preempted "
								+ "attempted construction of FeatMatrix with illegitimate feature specification list"; 
						sourcePlaces.add(getFeatMatrix(currPlaceSpec.substring(1, currPlaceSpec.length() - 1))); 
					}
					else
					{
						assert isValidFeatSpecList(currPlaceSpec) : "Error : preempted "
								+ "attempted construction of FeatMatrix with illegitimate feature specification list";
						sourcePlaces.add(getFeatMatrix(currPlaceSpec));
					}
				}

				//the same for dest place specs -- except these are sequential phonic, cannot include FeatMatrix instances
				List<Phone> destSeg = new ArrayList<Phone>(); 
				for (int dpsi = 0 ; dpsi < destPlaceSpecs.length ; dpsi ++ )
				{
					String currSymb = destPlaceSpecs[dpsi].trim(); 
					assert symbToFeatVects.containsKey(currSymb) : "ERROR: destination contains a non-valid phone symbol"; 
					destSeg.add(new Phone(symbToFeatVects.get(currSymb), featIndices, symbToFeatVects));
				}
				
				SChangeFeatToPhone newShift = new SChangeFeatToPhone(featIndices, sourcePlaces, destSeg, boundsMatter);
				if(priorSpecified) newShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
				if(postrSpecified) newShift.setPostContext(parseNewContext(inputPostr, boundsMatter));
				output.add(newShift); 
				return output;  
			}
		}
		if(isValidFeatSpecList(inputDest.trim())) // if we have a SChangeFeat with a phone input and feat matrix output
		{
			assert !inputDest.contains("{") && !inputDest.contains("}") : 
				"Error : preempted disjnction applied to destination of shift which is a single FeatMatrix--"
				+ " these are not permitted. " ;
			FeatMatrix theDestSpec = getFeatMatrix(inputDest); 
			
			if(inputSource.contains("{"))	//disjunctive. Make various SChangeFeat instances in the list
			{
				assert inputSource.charAt(0) == '{' && inputSource.charAt(inputSource.length() - 1) == '}' 
						&& inputSource.contains(""+segDelim): 
					"Error: illegitimate usage of disjunction braces"; 
				String[] srcPhones = inputSource.substring(1,inputSource.length() - 1).trim().split(""+segDelim); 
				for(int spi = 0; spi<srcPhones.length; spi++)
				{	
					String curPh = srcPhones[spi].trim(); 
					assert symbToFeatVects.containsKey(curPh) : "Error: tried to add illegitimate phone for source disjunct";
					SChangeFeat curShift = new SChangeFeat( new Phone(symbToFeatVects.get(curPh), featIndices, symbToFeatVects),
							theDestSpec, boundsMatter); 
					if(priorSpecified)	curShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
					if(postrSpecified)	curShift.setPostContext(parseNewContext(inputPostr, boundsMatter)); 
					output.add(curShift); 
				}
				return output; 
			}
			assert symbToFeatVects.containsKey(inputSource) : "Error: tried to use invalid phone as source target"; 
			SChangeFeat theShift = new SChangeFeat ( new Phone(symbToFeatVects.get(inputSource), featIndices, symbToFeatVects),
					theDestSpec, boundsMatter); 
			if(priorSpecified)	theShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
			if(postrSpecified)	theShift.setPostContext(parseNewContext(inputPostr, boundsMatter)); 
			output.add(theShift); 
			return output; 
		}
		//if we reach this point, we know we are making an SChangePHone
		List<List<SequentialPhonic>> sourceSegs = parseSeqPhDisjunctSegs(inputSource),
				destSegs = parseSeqPhDisjunctSegs(inputDest); 
		assert sourceSegs.size() == destSegs.size() : 
			"Error: mismatch in the number of disjunctions of source segs and disjunctions of dest segs!";
		SChangePhone newShift = new SChangePhone(sourceSegs, destSegs, boundsMatter); 
		if(priorSpecified) newShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
		if(postrSpecified) newShift.setPostContext(parseNewContext(inputPostr, boundsMatter));
		output.add(newShift); 
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
	
	private List<SequentialPhonic> parseSeqPhSeg (String inp)
	{
		List<SequentialPhonic> output = new ArrayList<SequentialPhonic>(); 
		String[] phsInSeg = inp.trim().split(""+phDelim); 
		for (int pisi = 0; pisi < phsInSeg.length; pisi++)
			output.add(parseSeqPh(phsInSeg[pisi].trim()));
		return output;
	}
	
	private SequentialPhonic parseSeqPh (String curtp)
	{
		if("+#".contains(curtp))
			return new Boundary("#".equals(curtp) ? "word " : "morph " + "bound"); 
		assert symbToFeatVects.containsKey(curtp) : "Error: tried to parse invalid symbol!";
		return new Phone(symbToFeatVects.get(curtp), featIndices, symbToFeatVects); 
	}
	/** parseNewContext 
	 * constructs a new ShiftContext instance
	 * with variables currRestrList and currParenMap to match the parse of this new contxt
	 * currRestrList -- all the RestrictPhones i.e. specifications on each context phone
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
	private ShiftContext parseNewContext(String inp, boolean boundsMatter)
	{
		String[] toPhones = inp.trim().split(""+phDelim); 
		List<String> parenMapInProgress = new ArrayList<String>();
		List<RestrictPhone> thePlaceRestrs = new ArrayList<RestrictPhone>(); 
		
		for(int i = 0; i < toPhones.length; i++)
		{
			String curtp = toPhones[i].trim(); 
			if(curtp.charAt(0) == '(') 
			{	//if it starts with '(' remove this and add to parenMap 
				curtp = curtp.substring(1).trim(); 
				parenMapInProgress.add("(");
			}
			String parenClose = "";
			if(curtp.contains(")"))
			{	//same as above for ending with ) (or ")*", ")+" ) 
				int lencurtp = curtp.length(); 
				if(curtp.indexOf(")") == lencurtp - 1 )	parenClose = ")"; 
				else
				{	//if ) is not the last character, ensure that either + or * is 
						// -- meaning 1 or more or 0 or more respectively 
					assert curtp.indexOf(")") == lencurtp - 2 && 
							"+*".contains(curtp.substring(lencurtp - 1)) : 
						"Error: invalid usage of closing parenthesis in new context to parse"; 
					parenClose = curtp.substring(lencurtp-2); 		
				}
				// remove from curtp
				curtp = curtp.substring(0, curtp.indexOf(")")).trim();
			}
			
			if (curtp.length() > 0) // if there is anyhting left after trimming and paren deletion
			{
				parenMapInProgress.add("i"+thePlaceRestrs.size()); 
				if(symbToFeatVects.containsKey(curtp)) //it's a Phone instance
					thePlaceRestrs.add(new Phone(symbToFeatVects.get(curtp), featIndices, symbToFeatVects)); 
				else if("+#".contains(curtp))
					thePlaceRestrs.add(new Boundary(("#".equals(curtp) ? "word " : "morph ") + "bound"));
				else if("@".equals(curtp))
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
			if(!parenClose.isEmpty()) //i.e. we are adding closing parentheses 
			{
				int corrOpenIndex = parenMapInProgress.lastIndexOf("("); 
					// corresponding index for the opening paren 
				if(parenClose.length() == 2) // the closing paren is either starred or plussed
					parenMapInProgress.set(corrOpenIndex,  parenClose.charAt(1)+"("); 
				parenMapInProgress.set(corrOpenIndex, parenMapInProgress.get(corrOpenIndex) +
						":" + parenMapInProgress.size()); 
				parenMapInProgress.add(parenClose + ":" + corrOpenIndex); 
			}
		}
		
		String[] theParenMap = new String[parenMapInProgress.size()];
		theParenMap = parenMapInProgress.toArray(theParenMap); 
		
		return new ShiftContext(thePlaceRestrs, theParenMap, boundsMatter) ;
	}

	/** isValidFeatSpecList
	 * @return @true iff @param input consists of a list of valid feature specifications 
	 * 	each delimited by restrDelim
	 */
	private boolean isValidFeatSpecList(String input)
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
		String[] protophones = inp.split(""+phDelim);
		for(int ppi = 0; ppi < protophones.length; ppi++)
		{
			String curpp = ""+protophones[ppi].trim();
			while(curpp.contains("("))	curpp = curpp.substring(curpp.indexOf('(')+1);
			while(curpp.contains(")"))	curpp = curpp.substring(0, curpp.indexOf(')'));
			if(isValidFeatSpecList(curpp))	return true; 
		}
		return false; 
	}

	//derives FeatMatrix object instance from String of featSpec instances
	public FeatMatrix getFeatMatrix(String featSpecs)
	{
		assert isValidFeatSpecList(featSpecs) : "Error : preempted attempt to get FeatMatrix from an invalid list of feature specifications" ; 
		String featsIncludingImplications = applyImplications(featSpecs); 
		
		if(featsIncludingImplications.contains(".") == false)
			return new FeatMatrix(featsIncludingImplications, featIndices); 
		
		List<String> despecs = new ArrayList<String>(); 
		
		//TODO we should make sure someone doesn't insert periods into their feature names 
		while(featsIncludingImplications.contains("."))
		{
			//TODO note: as per who implications are added AFTER the features that implied them in the method 
			// applyImplications(), all periods should come after commae -- otherwise we suspect 
			// there is some feature name with a period in it, which would be illegitimate
			int pdIndex = featsIncludingImplications.indexOf("."); 
			assert (featsIncludingImplications.charAt(pdIndex - 1)==restrDelim): 
				"Error: Unspecification marker, '.', found at an illegitimate place, likely was used in the middle of a feature"
				+ "	as a part of a feature name"; //TODO set up earlier assertion error to troubleshoot this
			String fWIAfterPd = featsIncludingImplications.substring(pdIndex+1); 
			featsIncludingImplications = featsIncludingImplications.substring(0, pdIndex - 1); 
			if(fWIAfterPd.contains(""+restrDelim))
			{	featsIncludingImplications += fWIAfterPd.substring(fWIAfterPd.indexOf(""+restrDelim));
				despecs.add(fWIAfterPd.substring(0, fWIAfterPd.indexOf(""+restrDelim))); 	}
			else	despecs.add(fWIAfterPd); //i.e. this is when it was the last element in the string .
		}
		return new FeatMatrix(featsIncludingImplications, featIndices, despecs); 
	}
	
	/**	applyImplications
	 * modifies a list of features which will presumably be used to define a FeatMatrix 
	 * so that the implications regarding the specification or non-specifications of certain features are adhered to 
	 * @param featSpecs, feature specifications before application of the stored implications
	 */
	public String applyImplications (String featSpecs) 
	{
		assert isValidFeatSpecList(featSpecs) : "Error : preempted attempt to apply implications to an invalid list of feature specifications" ; 
		String[] theFeatSpecs = featSpecs.trim().split(""+restrDelim); 
		String output = ""; 
		
		for(int fsi = 0 ; fsi < theFeatSpecs.length; fsi++)
		{
			String currSpec = theFeatSpecs[fsi]; 
			output += currSpec + restrDelim ; 
			
			if(featsWithImplications.contains(currSpec)) 
			{
				String[] implications = featImplications.get(currSpec); 
				for (int ii = 0; ii < implications.length; ii++)
					if ((featSpecs.contains(implications[ii]) == false)
						&& output.contains(implications[ii]) == false)
						output += implications[ii] + restrDelim; 		
			}
			if("+-".contains(currSpec.substring(0,1)))
			{
				if(featsWithImplications.contains(currSpec.substring(1)))
				{
					String[] implications = featImplications.get(currSpec.substring(1)); 
					for (int ii=0; ii < implications.length; ii++)
						if((featSpecs.contains(implications[ii]) == false ) && 
								output.contains(implications[ii]) == false)
							output += implications[ii] + restrDelim; 
				}
			}
		}
		return output.substring(0, output.length() - 1 ); 
	}
	
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
	
	
}
