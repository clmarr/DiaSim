import java.util.ArrayList;
import java.util.List;

public class SChangeFactOld {

	/** generateSChanges
	 * returns a list of Shift instances of the appropriate subclass based on input String,
	 * 		which should be a single change written in phonological rule notation
	 * in most cases this will only have one SChange
	 * however, in some cases of disjunction in the source or the contexts, it is better 
	 * 		to make multiple SChange instances. 
	 */
	public List<SChange> generateSoundChangesFromRule(String inp)
	{
		String input = (inp.indexOf(""+cmtFlag) == -1) ? inp.trim() : inp.substring(0,inp.indexOf(""+cmtFlag)).trim(); 
		
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
		
		
		//parse of target source
		boolean srcHasFeatMatrices = inputSource.contains("["); 
		assert srcHasFeatMatrices == inputSource.contains("]"): 
			"Error: mismatch in presence of [ and ], which are correctly used to mark a FeatMatrix specification"; 
		boolean srcHasValidSpecList = hasValidFeatSpecList(inputSource); 
		if(srcHasFeatMatrices) 
			assert srcHasValidSpecList :
			"Error: usage of brackets without valid feature spec list : "+inputSource; 
		
		if(srcHasValidSpecList) //i.e. source should be characterized by RestrictPhone instances
		{	assert !inputSource.contains("{") && !inputSource.contains("}") : 
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
						getFeatMatrix(applyImplications(inputDest.substring(1, inputDest.length() - 1)), true), boundsMatter); 
				if(priorSpecified) newShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
				if(postrSpecified) newShift.setPostContext(parseNewContext(inputPostr, boundsMatter));
				output.add(newShift); 
				return output; 
			}
			assert !inputDest.contains("[") && !inputDest.contains("]"): "Invalid bracket found in "
					+ "destination; brackets should only be used to surround feat specs for a FeatMatrix"; 
			// we eliminated the possiblity that the first char was '[' with the if clause above this
			if(isValidFeatSpecList(inputDest)) // i.e. single FeatMatrix specified without brackets
			{
				SChangeFeat newShift = new SChangeFeat(getFeatMatrix(inputSource), 
						getFeatMatrix(applyImplications(inputDest), true), boundsMatter); 
				if(priorSpecified) newShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
				if(postrSpecified) newShift.setPostContext(parseNewContext(inputPostr, boundsMatter));
				output.add(newShift); 
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
					if (!currSymb.equals("∅"))
					{	
						assert symbToFeatVects.containsKey(currSymb) : "ERROR: destination contains a non-valid phone symbol : "+currSymb; 
						destSeg.add(new Phone(symbToFeatVects.get(currSymb), featIndices, symbToFeatVects));
					}
				}
				
				SChangeFeatToPhone newShift = new SChangeFeatToPhone(featIndices, sourcePlaces, destSeg, boundsMatter);
				if(priorSpecified) newShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
				if(postrSpecified) newShift.setPostContext(parseNewContext(inputPostr, boundsMatter));
				output.add(newShift); 
				return output;  
			}
		}
		assert !inputSource.contains("[") && !inputSource.contains("]") : 
			"Error: found brackets in input source that is without a valid feat spec list.\nInvalid input source is : "+inputSource;
		
		if(inputDest.charAt(0) == '[')
		{
			inputDest = inputDest.trim();
			
			int inpdl = inputDest.length() - 1; 
			
			if (inputDest.charAt(inpdl) == ']' && inputDest.indexOf("]") == inpdl )
			{
				//TODO debugging
				System.out.println("Ends in ]");	
				
				inputDest = inputDest.substring(1, inpdl);
			}
		
		}
		if(isValidFeatSpecList(inputDest.trim())) // if we have a SChangePhone with a phone input and feat matrix output
		{
			//TODO debugging
			System.out.println("dest is feat matr");
			
			assert !inputDest.contains("{") && !inputDest.contains("}") : 
				"Error : preempted disjnction applied to destination of shift which is a single FeatMatrix--"
				+ " these are not permitted. " ;
			ArrayList<RestrictPhone> mutats = new ArrayList<RestrictPhone>(); 
			mutats.add(getFeatMatrix(applyImplications(inputDest), true)); 

			SChangePhone thisShift = new SChangePhone(parseSeqPhDisjunctSegs(inputSource), mutats, boundsMatter); 
			if(priorSpecified)	thisShift.setPriorContext(parseNewContext(inputPrior, boundsMatter)); 
			if(postrSpecified)	thisShift.setPostContext(parseNewContext(inputPostr, boundsMatter)); 
			output.add(thisShift); 
			return output; 
		}
		assert !inputDest.contains("[") && !inputDest.contains("]") : 
			"Error: found brackets in input dest that is without a valid feat spec list: " +inputDest ; 
		
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

}
