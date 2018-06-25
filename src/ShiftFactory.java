import java.util.List; 
import java.util.ArrayList; 
import java.util.HashMap; 
import java.util.Set; 
import java.util.Arrays; 

// takes in parameters and outputs Shift of the right type; 
// input is the rule in classical historical phonological form: X > Y | W __ Z 
// depending on how that is written, output a shift of the appropriate type 
// when context could be either feature-defined or phone-defined (most commonly in case of nulls), 
	//... prefer classes with featural context representation
// this class also appropriately will take care of how to react to various phonology notations: (), ()*, {}, ^, C, V,  alpha, beta etc...
// TODO test the other classes FIRST before writing this one ! 
// TODO determine how to indicate to the system that boundaries should matter 
public class ShiftFactory {

	private HashMap<String, String> symbToFeats;
	private HashMap<String, String> featsToSymb; 
	private HashMap<String, Integer> featIndices;
	private HashMap<String, String[]> featImplications; 
	private Set<String> featsWithImplications; 
	
	private final char transitionMark = '>'; 
	private final char phDelim = ' '; // delimits phones that are in a sequence 
	private final char segDelim = ';'; // delimits segments that are in disjunction 
	private final char contextFlag = '/'; //signals the beginning of context specification. May use pipe("|") instead. 
	private final char restrDelim = ','; //delimits restrictions between features inside a CandRestrict Phone
		// i.e. if " ", then [+A -B +C] where A, B and C are feature restrictions. 
	private final char featDelim = ','; //delimiter between features in a feature string or in a feature matrix 
	private final String selfInContextMarker = "__"; //marks place of source target relative to context 
	
	//Constructor 
	public ShiftFactory(HashMap<String, String> stf, HashMap<String,Integer> featInds, HashMap<String,String[]> featImpls)
	{
		symbToFeats = new HashMap<String, String>(stf); 
		featIndices = new HashMap<String, Integer>(featInds);
		featImplications = new HashMap<String, String[]>(featImpls); 
		featsToSymb = new HashMap<String, String>(); 
		Set<String> stfKeys = stf.keySet(); 
		for ( String key : stfKeys)
		{
			String featdef = stf.get(key); 
			assert featsToSymb.containsKey(featdef) == false : 
				"ERROR: duplicate phone definition in symbMap!";
			featsToSymb.put(featdef, key); 
		}
	}
	
	//returns list of Shift instances of appropriate subclass based on input String in phonological rule notation
	//in most cases this will only have one Shift in it
	// however in some cases of disjunction in the targets or contexts, it is better to make multiple Shift instances
	// as of May 25, one CAN mix phones into a feature-specificied list, but not the reverse 
	// -- in practice, pragmatically, the only place it seems sensible to do this for is the context. 
	public List<SChange> generateShifts(String input)
	{
		String[] inputSplit = input.split(""+transitionMark);
		String inputSourceTarget = inputSplit[0].trim(), inputParse = inputSplit[1].trim(); 
		
		String inputDest = inputParse, inputPrior = "", inputPostr = "";
		
		boolean contextSpecified = inputParse.contains(""+contextFlag);
		boolean priorSpecified = false, postrSpecified = false; 
		
		if(contextSpecified)
		{
			assert inputParse.contains(selfInContextMarker) : 
				"Error: Context flag seen but no mark of target source in relation to context!";
			inputSplit = inputParse.split(""+contextFlag);
			inputDest = inputSplit[0].trim(); 
			
			inputParse = inputSplit[1].trim(); 
			inputSplit = inputParse.split(selfInContextMarker);
			
			assert inputSplit[0].trim().equals("") == false || inputSplit[1].trim().equals("") == false : 
				"Error : Context flag and source target location marker seen, but no specification of either prior or posterior"
				+ "on either side of the source target location marker!"; 
			
			if(!inputSplit[0].trim().equals(""))
			{
				inputPrior = inputSplit[0].trim(); 
				priorSpecified = true; 
			}
			if(!inputSplit[1].trim().equals(""))
			{
				inputPostr = inputSplit[1].trim(); 
				postrSpecified = true; 
			}	
		} 
		
		//TODO first parse contexts
		ShiftContext priorContext, postrContext; 
		if(priorSpecified)	priorContext = parseNewContext(inputPrior, false); 
		if(postrSpecified)	postrContext = parseNewContext(inputPostr, false); 
		
		
		//TODO note [ and ] can ONLY be used to surround feature specifications for FeatMatrix
		// otherwise there will be very problematic errors
		
		//TODO first handle parse of target source
		boolean targSrcHasFeatMatrices = inputSourceTarget.contains("[");
		assert targSrcHasFeatMatrices == inputSourceTarget.contains("]"): 
			"Error: mismatch in presence of [ and ], which are supposed to mark a FeatMatrix specification"; 
		if(targSrcHasFeatMatrices)
		{
			assert !inputSourceTarget.contains(")*") && !inputSourceTarget.contains(")+") : 
				"Error : no recursive disjunctions allowed for inputs with FeatMatrices";
			
			//TODO determine proper return and return it. 
		}
		else // it is phone shift. 
		{
			//parse input. 
			
			boolean destHasFeatMatrices = inputDest.contains("["); 
			assert destHasFeatMatrices == inputDest.contains("]"): 
				"Error: mismatch in presence of [ and ], which are supposed to mark a FeatMatrix specification"; 
			if(destHasFeatMatrices)
			{
				
			}
		}
		
		
		//TODO make decision on whether it is necessary to split this shift into multiple Shift instances due to
			// unhandlable disjunction
		
		//TODO determine specification type of target and support notation
		
		//TODO determine specification type of dest and support notation
		
		//TODO determine specification type of context and support notation 
		
		//TODO return appropriate shift. 
		
		
	}
	
	// TODO need to fix this -- forgot what was wrong though. 
	public List<List<SequentialPhonic>> parseSeqPhs (String inp)
	{
		if (inp.trim().equals("∅"))		return new ArrayList<List<SequentialPhonic>>(); 
		
		List<String> disjunctions = getParseSeqPhDisjuncts(inp); 
		List<List<SequentialPhonic>> output = new ArrayList<List<SequentialPhonic>>(); 
		for (String disj: disjunctions)
		{
			String[] phones = disj.split(""+phDelim); 
			List<SequentialPhonic> currSeg = new ArrayList<SequentialPhonic>(); 
			for (int phi = 0; phi < phones.length; phi++)
				currSeg.add(
						new Phone(symbToFeats.get(phones[phi]), featIndices, symbToFeats));
			output.add(currSeg);
		}
		return output;
	}
	
	//for separating disjunctions for shifts involving FeatMatrix instances
	private List<String> getParseWithFeatMatrixDisjuncts(String inp)
	{
		List<String> output = new ArrayList<String>(); 
		output.add(inp); 
		int i = 0; 
		while(i < output.size())
		{
			while(output.get(i).contains("{") || output.get(i).contains("("))
			{
				String toDisjoin = output.get(i); 
				int firstBraceInd = toDisjoin.indexOf('{'), firstParenInd = toDisjoin.indexOf('('); 
				if(firstBraceInd < firstParenInd || firstBraceInd != -1)
				{
					//iterate to find closer; 
					int endingInd = firstBraceInd + 1, braceDepth = 1; 
					while(! (toDisjoin.charAt(endingInd) == '}' && braceDepth == 1))
					{
						if(toDisjoin.charAt(endingInd) == '{')	braceDepth++; 
						else if (toDisjoin.charAt(endingInd) == '}')	braceDepth--; 
						endingInd++; 
						assert endingInd < toDisjoin.length() : "Error : reached end without finding the closer brace"; 
					}
					String[] disjuncts = toDisjoin.substring(firstBraceInd, endingInd).split(""+segDelim); 
					output.set(i, toDisjoin.substring(0, firstBraceInd)+disjuncts[0].trim()+toDisjoin.substring(endingInd+1)); 
					int addi = 1; 
					while (addi < disjuncts.length)
						output.add(i, toDisjoin.substring(0, firstBraceInd)+disjuncts[addi].trim()+toDisjoin.substring(endingInd+1));
				}
				else //.i.e. firstParenInd < firstBraceInd || firstParenInd != -
				{
					//iterate to find closing paren
					int endingInd = firstParenInd + 1, parenDepth = 1; 
					while(! (toDisjoin.charAt(endingInd) == ')' && parenDepth == 1))
					{
						if(toDisjoin.charAt(endingInd) == '(')	parenDepth++; 
						else if (toDisjoin.charAt(endingInd) == ')')	parenDepth--; 
						endingInd++; 
						assert endingInd < toDisjoin.length() : "Error : reached end without finding the closer brace"; 
					}
					String[] disjuncts = toDisjoin.substring(firstParenInd, endingInd).split(""+segDelim); 
					output.set(i, toDisjoin.substring(0, firstParenInd)+disjuncts[0].trim()+toDisjoin.substring(endingInd+1)); 
					int addi = 1; 
					while (addi < disjuncts.length)
						output.add(i, toDisjoin.substring(0, firstParenInd)+disjuncts[addi].trim()+toDisjoin.substring(endingInd+1)); 
				}
			}
			i++; 
		}	
		return output; 
	}
	
	
	//TODO I think this method has become obselete in its setup -- must be fixed or discarded 
	//auxiliary method for parseSeqPhs
	//analyzes the input and returns the list of all hte possible segments that could be indicated by the input
	//as per the phonological rule notation
	// this is done by recursion whereby we convert "messy" disjunctions into proper total disjunctions 
	// users should (please ) not use *-notation, +-notation or zero subscript notation, 
	// rather just indicate (within recursive reason) what all these mean with parentheses.
	private List<String> getParseSeqPhDisjuncts (String inp)
	{
		List<String> output = new ArrayList<String>();
		
		// Case 0 -- recursion base case 
		if(!seqPhDisjctIndicated(inp))	
		{	output.add(inp); return output;	}
		
		//now we know we have to make splits. 
		String remainingInp = inp.trim();
		int ril = remainingInp.length();
		
		//Case 1 -- we have a proper total disjunction. Recurse for each disjunct segment.
		if(remainingInp.charAt(0) == '{' && remainingInp.charAt(ril - 1) == '}')
		{
			remainingInp = remainingInp.substring(1, ril - 1); 
			assert remainingInp.indexOf(segDelim) != -1 : "Error: disjunction with no delimiter of disjunction segments!";
			String[] inpDisjcts = remainingInp.split(""+segDelim); 
			int numDisjuncts = inpDisjcts.length; 
			for (int idi = 0 ; idi < numDisjuncts ; idi++)
				output.addAll(getParseSeqPhDisjuncts(inpDisjcts[idi]));
			return output;  
		}
			
		//Cases 2 to 4-- we have some type of non-proper total disjunction. We try to convert this into a proper disjunction
		String constEarlyInp = remainingInp.substring(0,1); 
		remainingInp = remainingInp.substring(1); 
		while(! "{<(".contains(remainingInp.substring(0,1)))
		{
			constEarlyInp += remainingInp.substring(0,1); 
			remainingInp = remainingInp.substring(1); 
		}
		char opener = remainingInp.charAt(0);
		ril = remainingInp.length();			

		if (opener == '(' ) 
		{
			int firstCloseParenIndex = remainingInp.indexOf(')'); //gets the FIRST index of ')'
			int closeIndex; 
			
			//how many were opened? Find the correct closing index dependent on this. 
			int firstOpenParenIndexAfter = remainingInp.substring(1).indexOf('(') + 1 ;
			if(firstOpenParenIndexAfter < 1 || firstOpenParenIndexAfter > firstCloseParenIndex)
				closeIndex = firstCloseParenIndex; 
			else
			{	int parenDepth = 2 , ci = firstOpenParenIndexAfter; 
				while (parenDepth > 0 && ci < ril ) 
				{	
					ci++; 
					if(remainingInp.charAt(ci) == '(')	parenDepth++; 
					else if (remainingInp.charAt(ci) == ')')	parenDepth--;
				}
				assert ci < ril : "Error: parentheses left hanging open by end of input"; 
				closeIndex = ci; 
			}
			
			String outputWithout = constEarlyInp, outputWith = constEarlyInp+remainingInp.substring(1, closeIndex); 
			if(closeIndex != ril - 1)
			{	outputWithout += remainingInp.substring(closeIndex+1); 
				outputWith += remainingInp.substring(closeIndex+1); 
			}
			
			output.addAll(getParseSeqPhDisjuncts(outputWithout)); 
			output.addAll(getParseSeqPhDisjuncts(outputWith)); 
			return output; 
		}
		else if (opener == '{')
		{
			int firstCloserIndex = remainingInp.indexOf('}'); 
			int closeIndex; 
			
			//find how many were opened
			int nextOpenerIndex = remainingInp.substring(1).indexOf('{') + 1; 
			if(nextOpenerIndex < 1 || nextOpenerIndex > firstCloserIndex)
				closeIndex = firstCloserIndex; 
			else
			{
				int parenDepth = 2, ci = nextOpenerIndex; 
				while (parenDepth > 0 && ci < ril)
				{
					ci++; 
					if(remainingInp.charAt(ci) == '{')	parenDepth++; 
					else if (remainingInp.charAt(ci) == '}') parenDepth--;
				}
				assert ci < ril : "Error: braces left hanging open by end of input"; 
				closeIndex = ci; 
			}
			
			String disjunction = remainingInp.substring(1, closeIndex); 
			assert disjunction.contains(""+segDelim): "Error: disjunction with no segment delimiter"; 
			String[] disjuncts = disjunction.split(""+segDelim); 
			int numDisjuncts = disjuncts.length; 
			if(closeIndex != ril - 1)
				for (int idi = 0; idi < numDisjuncts; idi++)
					output.addAll(getParseSeqPhDisjuncts(
							constEarlyInp + disjuncts[idi] + remainingInp.substring(closeIndex + 1))); 
			else
				for (int idi = 0; idi < numDisjuncts; idi++)
					output.addAll(getParseSeqPhDisjuncts(
							constEarlyInp + disjuncts[idi]));
			return output; 
		}
		assert opener == '<': "Error: something went wrong with typing the opener character."; 
		String outputWith="", outputWithout=""; 
		int nextCloserIndex = remainingInp.indexOf('>'), nextOpenerIndex = remainingInp.substring(1).indexOf('<')+1; 
		ril = remainingInp.length(); 
		if(nextOpenerIndex == 0) // not found
		{
			outputWith = constEarlyInp + remainingInp.substring(1, nextCloserIndex);
			outputWithout = constEarlyInp; 
			if (nextCloserIndex != ril - 1 )
			{	outputWith += remainingInp.substring(nextCloserIndex+1);
				outputWithout += remainingInp.substring(nextCloserIndex+1); 
			}
			output.addAll(getParseSeqPhDisjuncts(outputWith));
			output.addAll(getParseSeqPhDisjuncts(outputWithout)); 
			return output; 
		}
		else if (nextOpenerIndex > nextCloserIndex)
		{
			outputWith = constEarlyInp + remainingInp.substring(1, nextCloserIndex); 
			outputWithout = constEarlyInp; 
			remainingInp = remainingInp.substring(nextCloserIndex + 1); 
			ril = remainingInp.length(); 
			while(nextOpenerIndex > 0) //TODO change criterion if necessary-- review after filling inside
			{
				int parenDepth = 0,  rii = 0;
				char currChar = remainingInp.charAt(rii); 
				while(currChar != '<' && rii < ril)
				{	
					if("{(".contains(""+currChar))	parenDepth++; 
					else if (")}".contains(""+currChar))	parenDepth--; 
					rii++; currChar = remainingInp.charAt(rii); 
				}
				rii++; 
				assert rii < ril: "Error: reached end of remaining input without reaching previously confirmed next opener '<'"; 
				boolean open = (parenDepth == 0); //if this is contained in a parenthetical statement, it isn't something of interest 
					//... at this level of operation. Note also that the closing of parens should either contain or contain the <>: 
					// i.e. we cannot have things like this: (A<B)C> 
				int targDepth = 1; //we also aren't interested in '<>' statements within others-- although pragmatically it's hard to see
					//why anyone would want to use those! 
				currChar = remainingInp.charAt(rii); 
				while(targDepth != 0 && rii < ril)
				{
					if("{(".contains(""+currChar))	parenDepth++; 
					else if (")}".contains(""+currChar))	parenDepth--; 
					else if (currChar == '<')	targDepth++; 
					else if (currChar == '>')	targDepth--; 
					rii++; currChar = remainingInp.charAt(rii);
				}
				if(rii == ril)
				{
					assert targDepth == 0 && parenDepth == 0 && !open: "Error: unclosed '<', parenthesis or brace when input string ended"; 
					outputWith += remainingInp.substring(0, nextOpenerIndex) + remainingInp.substring(nextOpenerIndex+1, rii-1);
					outputWithout += remainingInp.substring(0, nextOpenerIndex); 
					output.addAll(getParseSeqPhDisjuncts(outputWith)); 
					output.addAll(getParseSeqPhDisjuncts(outputWithout)); 
					return output; 
				}
				//so we know targDepth == 0
				int closeIndex = rii - 1; 
				outputWith += remainingInp.substring(0, nextOpenerIndex);  
				outputWithout += remainingInp.substring(0, nextOpenerIndex); 
				if(open) // i.e. this <> is matched with the original one that was activated
					outputWith += remainingInp.substring(nextOpenerIndex+1, closeIndex); 
				else
				{
					outputWith += remainingInp.substring(nextOpenerIndex, closeIndex+1); 
					outputWithout += remainingInp.substring(nextOpenerIndex, closeIndex + 1); 
				}
				remainingInp = remainingInp.substring(rii); 
				ril = remainingInp.length(); 
				nextOpenerIndex = remainingInp.indexOf('<'); 
			}
			// i.e. nextOpenerIndex = -1 
			outputWith += remainingInp;
			outputWithout += remainingInp;
		}
		output.addAll(getParseSeqPhDisjuncts(outputWith));
		output.addAll(getParseSeqPhDisjuncts(outputWithout)); 
		return output; 
	}
	
	//auxiliary for getParseSeqPhDisjuncts
	//return true if there are any characters present that would indicate there is a disjunction indicated
	// as per phonological rule notation for phonic specification, in the input string
	// else false.
	private boolean seqPhDisjctIndicated (String inp)
	{
		if(inp.contains("{"))	return true;
		if(inp.contains("}"))	return true;
		assert !inp.contains(""+segDelim): "Error: segment delimiter present without {}";
		if(inp.contains("("))	return true;
		if(inp.contains(")"))	return true;
		return false; 
	}
	
	//@precondition: all restrictions are in the correct form, which is [+A,-B] et cetera for all FeatMatrixs, 
	// and the bare phone or pseudophone symbol for PseudoPhones or proper Phones, 
	// all separated by phDelim 
	// this method does not allow for disjunctive inputs. 
	// TODO this. 
	public List<RestrictPhone> parseRestrs (String inp)
	{
		List<RestrictPhone> output = new ArrayList<RestrictPhone>(); 
		String[] unitPlaces = inp.split(phDelim); 
		int len = unitPlaces.length; 
		for (int upi = 0; upi < len ; upi++)
		{
			String curr = unitPlaces[upi]; 
			
		}
					
	}
	
	//TODO blablablablabla
	

	/**
	 * constructs new ShiftContext instance = 
	 * with variables currRestrList and currParenMap to match the parse of this new context 
	 * currRestrList will be all the RestrictPhones embodying the specifications on the context by place
	 * while parenMap is a String[] that is a "map" of where parenthetical statements apply
	 * ..., structured as illustrated by this example (the top row is the indices IN PARENMAP)
	 * 
	 * 		0	|	1	|	2	|	3	|	4 	|	5	|	6	|	7
	 * 		i0 	| +(:4	| 	i1 	| 	i2 	| )+:1	| 	(:7	| 	i3	|	):5 
	 * cells with contents starting i indicate that the cell corresponds to the index of the number following 
	 * 		in placeRestrs
	 * cells with paren markers { +(, )+, (, ), *(, )*, } indicate where parens open and close
	 * 		relative to those indices in parenMap
	 * 		the number on the inside of hte paren indicates which index IN PARENMAP 
	 * 			is where the corresponding opening or closing paren lies.
	 * 
	 * @param boundsMatter -- determines whether the context restrictions we create will pass over boundaries 
	 * 		in input for context matching checker functions 
	 * @precondition : all elements separated phDelim 
	 */
	public ShiftContext parseNewContext(String inp, boolean boundsMatter)
	{
		String[] toPhones = inp.trim().split(""+phDelim); 
		List<String> parenMapInProgress = new ArrayList<String>(); 
		List<RestrictPhone> thePlaceRestrs = new ArrayList<RestrictPhone>(); 
		int currParenDepth = 0; //at any time, this should equal how many unclosed parentheses we have 
				// at the current place
		
		for(int i = 0; i < toPhones.length; i++)
		{
			String curtp = toPhones[i].trim();
			if(curtp.charAt(0) == '(')
			{	curtp = curtp.substring(1).trim(); 
				currParenDepth++; 
				parenMapInProgress.add("("); 
			}
			String parenClose = "" ;
			if(curtp.contains(")"))
			{
				int lencurtp = curtp.length(); 
				if(curtp.indexOf(")") == lencurtp - 1)	parenClose = ")";
				else 
				{	//if ) is not the last character, ensure that either + or * is -- meaning 1 or more or 0 or more respectively 
					assert curtp.indexOf(")") == lencurtp - 2 && 
							"+*".contains(curtp.substring(lencurtp - 1)) : 
						"Error: invalid usage of closing parenthesis in new context to parse"; 
					parenClose = curtp.substring(lencurtp-2);
				}
				curtp = curtp.substring(0, curtp.indexOf(")")).trim();
			}
			
			if(curtp.length() > 0)
			{
				parenMapInProgress.add("i"+thePlaceRestrs.size());
				if(symbToFeats.containsKey(curtp)) //it's a Phone instance
					thePlaceRestrs.add(new Phone(symbToFeats.get(curtp), featIndices, symbToFeats)); 
				else if ("+#".contains(curtp))
					thePlaceRestrs.add(new Boundary( ("+".equals(curtp) ? "morph " : "word ")  + "bound"));
				else 
				{
					 if(curtp.charAt(0) == '[' && curtp.charAt(curtp.length() - 1) == ']')
						 curtp = curtp.substring(1, curtp.length() - 1).trim(); 
					 
					 //TODO might need security method here to assert that curtp is now a valid list of specs
					 		//and thhrow error otherwise 
					 thePlaceRestrs.add(new FeatMatrix(curtp, featIndices)); 
				}
			}
			if(!parenClose.isEmpty()) //i.e. we are adding closing parenthesis
			{
				int corrOpenIndex = parenMapInProgress.lastIndexOf("("); 
					//index of corresponding opening index
				if(parenClose.length() == 2) //it is either starred or plussed
					parenMapInProgress.set(corrOpenIndex, parenClose.charAt(1) + "("); 
				parenMapInProgress.set(corrOpenIndex, 
						parenMapInProgress.get(corrOpenIndex) + ":" + parenMapInProgress.size());
				parenMapInProgress.add(parenClose + ":"+ corrOpenIndex); 
			}	
		}
		
		String[] theParenMap = new String[parenMapInProgress.size()];
		theParenMap = parenMapInProgress.toArray(theParenMap);
				
		return new ShiftContext(thePlaceRestrs, theParenMap, boundsMatter); 
	}
	
	//derives FeatMatrix object instance from String of featSpec instances
	public FeatMatrix getFeatMatrix(String featSpecs)
	{
		//TODO need to place assertions in here to prevent code from being broken
		String featsWithImplications = applyImplications(featSpecs); 
		
		if(featsWithImplications.contains(".")==false)
			return new FeatMatrix(featsWithImplications, featIndices); 
		
		List<String> despecs = new ArrayList<String>(); 
		
		//TODO we should make sure someone doesn't insert periods into their feature names 
		while (featsWithImplications.contains("."))
		{
			// TODO note: as per how implications are added AFTER the features that implied them in the 
				// calss applyImplications, all periods should come after commae -- otherwise we suspect 
				//there is some feature name with a period in it, which would be illegitimate. 
			int pdIndex = featsWithImplications.indexOf("."); 
			assert (featsWithImplications.charAt(pdIndex - 1)==restrDelim): 
				"Error: Unspecification marker, '.', found at an illegitimate place, likely was used in the middle of a feature"
				+ "	as a part of a feature name"; 
			String fWIAfterPd = featsWithImplications.substring(pdIndex+1); 
			featsWithImplications= featsWithImplications.substring(0, pdIndex - 1);  
			if(fWIAfterPd.contains(""+restrDelim))
			{	featsWithImplications += fWIAfterPd.substring(fWIAfterPd.indexOf(""+restrDelim)); 
				despecs.add(fWIAfterPd.substring(0, fWIAfterPd.indexOf(""+restrDelim)));	}
			else	despecs.add(fWIAfterPd);
		}
		return new FeatMatrix(featsWithImplications, featIndices, despecs); 
	}
			
	
	/** applyImplications 
	 * modifies a list of features, which will presumably be used to define a FeatMatrix
	 * so that implications regarding the specification or non-specifications of certain features are adhered to
	 * @param featSpecs -- feature specifications before application of the stored implications
	 */
	public String applyImplications (String featSpecs)
	{
		String[] theFeatSpecs = featSpecs.split(""+restrDelim); 
		String output = "";  
		
		for(int fsi = 0; fsi < theFeatSpecs.length ; fsi++ )
		{
			String currSpec = theFeatSpecs[fsi]; 
			output += currSpec + restrDelim; 
			if(featsWithImplications.contains(currSpec))
			{
				String[] implications = featImplications.get(currSpec); 
				int ii = 0; 
				while( ii < implications.length)
				{
					if(featSpecs.contains(implications[ii]) == false)	
						output += implications[ii] + restrDelim; 
					ii++; 
				}
			}
			if("+-".contains(currSpec.substring(0,1)))
			{
				if(featsWithImplications.contains(currSpec.substring(1))) // i.e. if it being specified as all has implications
				{
					String[] implications = featImplications.get(currSpec.substring(1));
					int ii = 0; 
					while( ii < implications.length)
					{
						if(featSpecs.contains(implications[ii]) == false)	
							output += implications[ii] + restrDelim; 
						ii++; 
					}
				}
			}
		}
		return output.substring(0, output.length() - 1); 
	}
	
}
