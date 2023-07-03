import java.util.List; 
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @date 1 June 2018
 * @author Clayton Marr
 * Class to represent the context of a shift
 * in order to handle phenomena like ()* and ()+, etc.
 * TODO note as of July 12 2018, (...)+ structures are now illegal 
 */

public class SequentialFilter {
	
	private int minSize;
	private boolean boundsMatter; //determines if we will pass over boundary markers (morpheme, word) in the input. 
	private boolean hasAlphSpecs;
	private List<RestrictPhone> placeRestrs; // the restriction on each place as indicated by index 
	private String[] parenMap;  /**parenMap is a String[] that is a "map" of where parenthetical statements apply
	 * ..., structured as illustrated by this example (the top row is the indices IN PARENMAP)
	 * 		0	|	1	|	2	|	3	|	4 	|	5	|	6	|	7
	 * 		i0 	|  *(:4	| 	i1 	| 	i2 	|  )*:1 | 	(:7	| 	i3	|	):5 
	 * cells with contents starting i indicate that the cell corresponds to the index of the number following 
	 * 		in placeRestrs
	 * cells with paren markers { (, ), *(, )*, } indicate where parens open and close
	 * 		relative to those indices in parenMap
	 * 		the number on the inside of the paren(thesis) indicates which index IN PARENMAP 
	 * 			is where the corresponding opening or closing paren lies. 
	 * TODO Note as of July 12 2018
	 * when this is entered, in order to save time, the min number of places within each paren window
	 * 		are calculated using the auxiliary method 
	 * 
	*/
	// bm = whether bounds matter. 
	// pm = paren map. 
	private void initialize(List<RestrictPhone> prs, String[] pm, boolean bm)
	{
		parenMap = pm ; 
		placeRestrs = new ArrayList<RestrictPhone>(prs); 
		
		boundsMatter = false;
		minSize = generateMinSize(); 
		
		markParenMapForMinPlacesInEachWindow();		
		
		minSize = generateMinSize(); 
		
		hasAlphSpecs = false; 
		for(RestrictPhone pr : placeRestrs)
			if (pr.has_alpha_specs())	hasAlphSpecs = true; 
		
	}
	
	public SequentialFilter (List<RestrictPhone> prs, String[] pm)
	{
		initialize(prs, pm, false); 
	}
	
	public SequentialFilter (List<RestrictPhone> prs, String[] pm, boolean bm)
	{
		initialize(prs, pm, bm); 
	}
	
	//auxiliary for initialization : mark all parenthetical cells in parenMap
		// with the minimum number of places inside
	// we assume they come in in the form *(:4 and )*:7 etc ... 
	private void markParenMapForMinPlacesInEachWindow()
	{
		int currIndex = parenMap.length - 1;
		while(currIndex > 1) // no parenthesis could ever close before index 2 else it would be containing nothing. 
		{
			if(parenMap[currIndex].contains(")"))
			{
				int openerIndex = Integer.parseInt(parenMap[currIndex].split(":")[1].split(",")[0]); 
				int minPlaces = minPlacesInParenWindow(openerIndex, currIndex); 
				parenMap[currIndex] = parenMap[currIndex] + "," + minPlaces;
				parenMap[openerIndex] = parenMap[openerIndex] + "," + minPlaces; 
				currIndex--; 
			}
			else
			{	
				while(!parenMap[currIndex].contains(")") && currIndex > 1)	
					currIndex--;
			}
			
		}
			
	}
	
	//auxiliary method to determine the minimum possible input size that can satisfy these context restrictions
	//must be called AFTER markParenMapForMinPlacesInEachWindow is called. 
	public int generateMinSize()
	{
		int pmSize = parenMap.length, count = 0;
		
			//optParenDepth is the number of optional { ()*, ()} paren structures we are currently in
		
		int i = 0; 
		while (i < pmSize) 
		{
			String currMapCell = parenMap[i];
			
			if(currMapCell.contains("(")) // then hop. 
				i = Integer.parseInt(currMapCell.split(":")[1].split(",")[0]) + 1 ;	
			else	
			{
				if (currMapCell.contains(")")) throw new RuntimeException( "Error: unopened ')' found"); 
				count++;
				i++;
			}
				
		}
		return count; 
	}
	
	/**isPriorMatch
	 * checks if a legal prior context can be found in @param phonSeq
	 * if it proceeds @param firstInd, the first index of the possible
	 * ... targeted segment for a shift
	 * @return true if so, otherwise false. 
	 */
	public boolean isPriorMatch (List<SequentialPhonic> phonSeq, int firstInd)
	{
		if(minSize == 0)	return true; 
		if(minSize > firstInd)	return false; 
		int  currPlaceInCand = firstInd - 1 , currRestrPlace = placeRestrs.size()-1, 
				currPlaceInMap = parenMap.length - 1; 
		//TODO note: currPlaceInCand is also the maximum size of the possible prior, for obvious reasons-- 
		/// this is important because if the postulated prior becomes greater than that size, we will return false. 
		//this is relevant for the method deciding whether to investigate further possibilities due to 
			//... parenthetical disjunctions 
		return isPriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap);
	}
	
	/** isPriorMatchHelper --  recursive helper method for isPriorMatch, with hopping counters and branching recursion to handle exclusion of parens 
	 * @param phonSeq -- the input 
	 * @param cpic -- current place being checked in the input 
	 * @param crp -- current place in place restrictions array
	 * @param cpim -- current place in parenMap 
	 * 
	 * CASES: 
	 * 1) We have matched all places in placeRestrs from end to the beginning -- then @return true
	 * 2) We have run out of places in phonSeq to match but have not matched all the requirements -- @return false 
	 * 3) We have a disjunctive parenthetical statement ending here -- see subcases
	 * 		3a) If the min number of places in the paren is more than the remaining possible length
	 * 			3a1) If we are dealing with a plussed paren -- immediately return false
	 *			PLUSSED PARENS NO LONGER SUPPORTED  --- 3a2) Else return whether we can reach a true value if we hop to before beginning of this paren structure
	 *		3b) else 
	 *			3b1) If the paren is a normal paren or a starred paren,
	 *					 first check if we can get a true value by excluding it (i.e. hop to be before the opening
	 *					, and if not test the branch where it is included. 
	 *			PLUSSED PARENS NO LONGER SUPPORTED --- 3b2) If it is a plussed paren, iterate as normal -- i.e. simply move the marker from the closing paren to the last element and follow the while loop described in the next case
	 * 4) We have a disjunctive parenthetical statement starting here
	 * 		4a) If it is starred branch: first check if we have a match if we just iterate as normal with moving cpim back one
	 * 				and otherwise check if we can get a true value by returning to the end of the paren structure
	 * 		4b) Else i.e. if it's a normal paren -- precede as normal, just cpim back one more space
	 * 5) if none of the first three are initially met.. while conditions (1) and (2) are not met do the following
	 * 		5a) if current place is a pseudophone and pseudos don't matter, increment only currPlaceInCand
	 * 			 and return to beginning of loop
	 * 		5b) if non-match return false
	 * 		5c) if match, increment (subtract one from) both currRestrPlace and currPlaceInCand and go to slowly beginning of loop  
	 *		5d) if ever case (3) is hit (presumably after the first loop) then do as described above for that case. 
	 * 6) after the loop terminates, test for cases (1) and (2) in that order and behave accordingly as described above for those two cases
	 */			
	private boolean isPriorMatchHelper (List<SequentialPhonic> phonSeq, int cpic, int crp, int cpim) 
	{
		if(crp < 0)	return true;
		if(cpic < 0)	
		{
			//check if all that's left in parenMap is optional 
			if( cpim >= 0 )
			{	
				throw new Error("Something is wrong, true should have been returned. Likely mismatch between placeRestrictions and parenMap.");
			}
			if(parenMap[cpim].contains(")"))
			{
				int proxypim = cpim; 
				while(parenMap[proxypim].contains(")"))
				{	
					proxypim = Integer.parseInt(parenMap[proxypim].split(":")[1].split(",")[0]) - 1; 
					if(proxypim == -1)	return true; 
				}
				return false; 
			}
			return false; 
		}
		
		int currPlaceInCand = cpic, currRestrPlace = crp, currPlaceInMap = cpim; 

		while(currRestrPlace >= 0 && currPlaceInCand >= 0 && currPlaceInMap >= 0)
		{			
			if(parenMap[currPlaceInMap].contains(")"))
			{
				int minContents = Integer.parseInt(parenMap[currPlaceInMap].split(",")[1]); 
				//if we could not possibly include the contents of this paren structure because there are too many 
					// for the space we have left in the input... 
				if(minContents > currPlaceInCand || minContents > currRestrPlace)
				{
					return isPriorMatchHelperExcludeParen(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap);
				}
								
				if(isPriorMatchHelperExcludeParen(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap))	return true; 
								
				return isPriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap - 1); 
				
			}
			if (parenMap[currPlaceInMap].contains("("))
			{
				if('*' == parenMap[currPlaceInMap].charAt(0))
				{
					if(isPriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap -1 ))	return true; 
					
					//find correct currRestrPlace to return to if we are going back to beginning of paren. 
					int formerPlace = currPlaceInMap; 
					currPlaceInMap = Integer.parseInt(parenMap[currPlaceInMap].split(":")[1].split(",")[0]); 
					
					int proxyPlace = currPlaceInMap - 1; 
					while(parenMap[proxyPlace].charAt(0) != 'i')	
					{
						proxyPlace--; 
						if(proxyPlace <= formerPlace)
							throw new Error("Something wrong: paren structure seems to have no actual phone restrictions inside");
					}
					currRestrPlace = Integer.parseInt(parenMap[proxyPlace].substring(1)); 
					return isPriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap); 
				}
				return isPriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap - 1); 
			}
			
			if(!boundsMatter && phonSeq.get(currPlaceInCand).getType().contains("bound")
					&& !placeRestrs.get(currRestrPlace).toString().equals(phonSeq.get(currPlaceInCand)+"")
					&& !placeRestrs.get(currRestrPlace).print().equals("@"))
			{	currPlaceInCand--;	}
			else if(!placeRestrs.get(currRestrPlace).compare(phonSeq.get(currPlaceInCand)))
				return false; 
			else	{	currPlaceInCand--; currRestrPlace--; currPlaceInMap--; 	}
		} 
		if(currRestrPlace < 0)		return true;
		if(currPlaceInCand < 0)	
		{
			//check if all that's left in parenMap is optional 
			if( currPlaceInMap < 0)
			{	
				throw new Error("Something is wrong, true should have been returned. Likely mismatch between placeRestrictions and parenMap.");
			}
			if(parenMap[currPlaceInMap].contains(")"))
			{
				int proxypim = currPlaceInMap; 
				while(parenMap[proxypim].contains(")"))
				{	
					proxypim = Integer.parseInt(parenMap[proxypim].split(":")[1].split(",")[0]) - 1; 
					if(proxypim == -1)	return true; 
				}
				return false; 
			}
			return false; 
		}
		
		else	return false; 
	}

	//auxiliary method for recursive calls that exclude the parenthesis ending at the current spot in parenMap
	private boolean isPriorMatchHelperExcludeParen (List<SequentialPhonic> phonSeq, int cpic,
			int crp, int cpim)
	{
		int mapSpotPreOpener = Integer.parseInt(parenMap[cpim].split(":")[1].split(",")[0]) - 1 ;
		if (mapSpotPreOpener < 0)	return true; 
		
		int placeBeforeOpener = -1, proxyMapSpot = mapSpotPreOpener; 
		while (placeBeforeOpener == -1 && proxyMapSpot >= 0)
		{
			if(parenMap[proxyMapSpot].charAt(0) == 'i')
				placeBeforeOpener = Integer.parseInt(parenMap[proxyMapSpot].substring(1));
			else	proxyMapSpot--; 
		}
		
		return isPriorMatchHelper(phonSeq, cpic, placeBeforeOpener, mapSpotPreOpener); 
	}

	//TODO possible error here that isPriorMatch(Helper) is... not called? 
		// nor is it called anywhere in ErrorAnalysis.
		// TODO need to make tester to check behavior of this as it is used in ErrorAnalysis.
		// though in practice it seems likely that all necessary issues will be caught on the forward iteration through the phonetic segments
		// since this is how iteration works where this is called in ErrorAnalysis.articulateSubsample (its only call it seems)
		// and it's not like a segment would just be missed, since it starts at the beginning and goes to the end. 
	public boolean filtCheck(List<SequentialPhonic> pr)
	{
		if(minSize == 0)	throw new Error("You shouldn't be using filtCheck with filter with no necessary length.");
		if(minSize > pr.size())	return false;
		for(int cpic = 0; cpic < pr.size() - minSize; cpic++)
			if(isPosteriorMatchHelper(pr,cpic,0,0))	return true;
		return false;
	}
	
	// returns list of all boundaries of matched filters -- empty if there are none i.e. no match. 
	public List<int[]> filtMatchBounds(List<SequentialPhonic> pr)
	{
		
		if(minSize == 0)	throw new Error("You shouldn't be using filtCheck with filter with no necessary length.");
		if(minSize > pr.size())	return new ArrayList<int[]>(); 

		List<int[]> out = new ArrayList<int[]>(); 
		
		int trueOnset = 0, currMatchStart = -1;
		List<SequentialPhonic> dummy = new ArrayList<SequentialPhonic>(pr); 

		while (dummy.size() >= minSize) {
			for (int cpic = 0 ; cpic < dummy.size() && currMatchStart == -1; cpic++)
				if(isPosteriorMatchHelper(dummy,cpic,0,0))	currMatchStart = cpic;
			if (currMatchStart == -1)	return out;
			else
			{
				int matchEnd = currMatchStart + minSize - 1; 
				while(matchEnd < dummy.size() ? 
						!isPriorMatchHelper(dummy,matchEnd,placeRestrs.size()-1,parenMap.length-1) : false)
					matchEnd++;
				
				out.add(new int[] {trueOnset + currMatchStart, trueOnset + matchEnd - pr.size()}); 
				trueOnset = trueOnset + matchEnd + 1;
				currMatchStart = -1;
				dummy = dummy.subList(matchEnd+1,dummy.size());
			}
		}
		
		return out; 
	}
	
	public boolean isPosteriorMatch(List<SequentialPhonic> phonSeq, int indAfter)
	{
		if (minSize == 0)	
			return true; 
		if (minSize > phonSeq.size() - indAfter)
			return false; 
		int currPlaceInCand = indAfter, currRestrPlace = 0, currPlaceInMap = 0; 
		return isPosteriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap); 
	}
	
	private boolean isPosteriorMatchHelper(List<SequentialPhonic> phonSeq, int cpic, int crp, int cpim)
	{	
		assert cpic <= phonSeq.size() && crp <= placeRestrs.size() && cpim <= parenMap.length: 
			"Error in call to isPosteriorMatchHelper -- at least one of the counter params was way too high";
		if(crp == placeRestrs.size())	return true;
		if(cpic == phonSeq.size())	
		{	
			//NOTE: if plussed parens ()+ -- i.e. "one or more" clauses -- are ever added back in
				// this statement will need to be modified so it doesn't apply to them. 
			if(cpim >= parenMap.length)
				throw new Error("Likely mismatch between placeRestrs and parenMap!");
			if(parenMap[cpim].contains("("))
			{
				//check if all that's left is optional
				int proxypim = cpim;
				while(parenMap[proxypim].contains("("))
				{
					proxypim = Integer.parseInt(parenMap[proxypim].split(":")[1].split(",")[0]) + 1; 
					if(proxypim == parenMap.length)	return true; 
				}
				return false; 
			}
			return false; 
		}
		
		int currPlaceInCand = cpic, currRestrPlace = crp, currPlaceInMap = cpim,
				lenPhonSeq = phonSeq.size(), numRestrPlaces = placeRestrs.size(), mapSize = parenMap.length; 
		while( currPlaceInCand < lenPhonSeq && currRestrPlace < numRestrPlaces && currPlaceInMap < mapSize)
		{
			if(parenMap[currPlaceInMap].contains("("))
			{
				int minPhonesInParen = Integer.parseInt(parenMap[currPlaceInMap].split(":")[1].split(",")[1]); 

				//if we could not possibly include the contents of this paren structure because there are too many 
				// for the space we have left in the input... 
				if(minPhonesInParen > lenPhonSeq - currPlaceInCand)
				{	
					return isPosteriorMatchHelperExcludeParen(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap); 
				}
				
				if(isPosteriorMatchHelperExcludeParen(phonSeq,currPlaceInCand, currRestrPlace, currPlaceInMap))
					return true; 
				return isPosteriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap + 1); 
			}
			if(parenMap[currPlaceInMap].contains(")"))
			{
				if('*' == parenMap[currPlaceInMap].charAt(1))
				{
					if(isPosteriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap + 1 ))		return true; 
					int formerPlace = currPlaceInMap;
					currPlaceInMap = Integer.parseInt(parenMap[currPlaceInMap].split(":")[1].split(",")[0]); 
					int proxyPlace = currPlaceInMap + 1; 
					while(parenMap[proxyPlace].charAt(0) != 'i') 
					{
						proxyPlace--; 
						if(proxyPlace >= formerPlace)	throw new Error("Error: no actual place restriction inside paren structure");
					}
					currRestrPlace = Integer.parseInt(parenMap[proxyPlace].substring(1));
					return isPosteriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap); 
				}
				return isPosteriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap + 1); 
			}

			if(!boundsMatter && phonSeq.get(currPlaceInCand).getType().contains("bound") 
					&& !placeRestrs.get(currRestrPlace).print().equals(phonSeq.get(currPlaceInCand)+"")
					&& !placeRestrs.get(currRestrPlace).print().equals("@"))	
			{	currPlaceInCand++;	}
			else if(!placeRestrs.get(currRestrPlace).compare(phonSeq.get(currPlaceInCand)))	
				return false; 
			else	{		
				currPlaceInCand++; currRestrPlace++; currPlaceInMap++; 	}
			
		}
		if(currRestrPlace == numRestrPlaces)	{	return true;	}
		if(currPlaceInCand == lenPhonSeq)
		{	
			//NOTE: if plussed parens ()+ -- i.e. "one or more" clauses -- are ever added back in
				// this statement will need to be modified so it doesn't apply to them. 
			if(currPlaceInMap >= parenMap.length)
				throw new Error("Likely mismatch between placeRestrs and parenMap!");
			if(parenMap[currPlaceInMap].contains("("))
			{
				//check if all that's left is optional
				int proxypim = currPlaceInMap;
				while(parenMap[proxypim].contains("("))
				{
					proxypim = Integer.parseInt(parenMap[proxypim].split(":")[1].split(",")[0]) + 1; 
					if(proxypim == parenMap.length)	return true; 
				}
				return false; 
			}
			return false; 
		}
		
		return false; 
	}
	
	private boolean isPosteriorMatchHelperExcludeParen(List<SequentialPhonic> phonSeq, int cpic,
			int crp, int cpim)
	{
		int mapSpotPostCloser = Integer.parseInt(parenMap[cpim].split(":")[1].split(",")[0]) + 1 ;
		
		if( mapSpotPostCloser > parenMap.length) throw new RuntimeException("Error: illegitimate closing index recorded!"); 
		if (mapSpotPostCloser == parenMap.length)	
			return true; 	
		
		// search for first non paren using proxyMapSpot and use its i# statement in parenMap to find the 
			// place after the closing parenthesis in parenMap. 
		int placeAfterCloser = placeRestrs.size(); //this spot is never checked -- and should not be.
		int proxyMapSpot = mapSpotPostCloser; 
		while (placeAfterCloser == placeRestrs.size() && proxyMapSpot < parenMap.length )
		{
			if(parenMap[proxyMapSpot].charAt(0) == 'i')
				placeAfterCloser = Integer.parseInt(parenMap[proxyMapSpot].substring(1));
			else	proxyMapSpot++; 
		}		
		
		return isPosteriorMatchHelper(phonSeq, cpic, placeAfterCloser, mapSpotPostCloser); 
	}
	
	
	//auxiliary method -- gets the minimum number of places in placeRestrs that could be covered 
		//in the contents of one window in parenMap, only to be used for calculating minimum number within
		// a paren structure 
	// first and last are the bounds of the window in indices in PARENMAP
		// --they should both be parentheses
	private int minPlacesInParenWindow (int first, int last)
	{
		if( first + 1 >= last || first < 0 || last >= parenMap.length )	throw new RuntimeException(
			"Error: Invalid bounds of window entered for minPlacesInParenWindow()"); 
		
		assert parenMap[first].contains("(") && parenMap[last].contains(")") : 
			"Error: in minPlacesInParenWindow, window specified should be a parenthetical"; 
		
		int count = 0, mapSpot = first + 1, optParenDepth = 0;
			//optParenDepth counts number of parens that are contained WITHIN the window
		while (mapSpot < last)
		{
			String curr = parenMap[mapSpot]; 
			if (curr.contains("("))
				optParenDepth++; 	
			else if (curr.contains(")"))
			{	
				optParenDepth--;
				assert optParenDepth >= 0: 
					"An error must of occurred: negative optParenDepth in minPlacesInParenWindow"; 
			}
			else if (optParenDepth == 0)	count++; 
			mapSpot++; 
		}
		
		return count; 
	}
	
	public int getMinSize() 	{	return minSize;	}
	
	@Override
	public String toString()
	{
		String output = "";
		for(int i = 0; i < parenMap.length; i++)
		{
			if(parenMap[i].charAt(0) == 'i')
			{
				RestrictPhone currSpecs = placeRestrs.get(Integer.parseInt(parenMap[i].substring(1)));
				if(currSpecs.print().equals(" @%@ ")) //i.e. it's a FeatMatrix
					output+=currSpecs.toString() + " "; 
				else //pseudoPhone or proper phone 
					output+=currSpecs.print()+ " ";
			}
			else if (parenMap[i].contains("("))	output += "( "; 
			else //must be a paren statement
			{
				assert parenMap[i].contains(")") : "Error: place i in parenMap is not ')' when that is the only option left"; 
				output += parenMap[i].split(":")[0]+" "; 
			}
		}
		return output.substring(0, output.length() - 1); 
	}
	
	//strictly for debugging purposes. 
	public String[] getParenMap()	{	return parenMap;	}
	public List<RestrictPhone> getPlaceRestrs()	{	return placeRestrs;	}
	
	//TODO abrogated, but kept around for possible debugging purposes
	/**
	private static String printParenMap(SChangeContext testCont)
	{
		String output = ""; 
		String[] pm = testCont.getParenMap();
		for(String p : pm)	output += p + " "; 
		return output.trim();
	}*/
	
	public void applyAlphaValues(HashMap<String, String> alphVals)
	{
		for (int pri = 0 ; pri < placeRestrs.size(); pri++)	placeRestrs.get(pri).applyAlphaValues(alphVals);
	}
	
	public void resetAllAlphaValues()
	{
		for (int pri = 0 ; pri < placeRestrs.size() ; pri++)	placeRestrs.get(pri).resetAlphaValues(); 
	}
	
	public boolean hasAlphaSpecs()
	{
		return hasAlphSpecs;
	}
	
	public boolean has_unset_alphas()
	{
		for (RestrictPhone pri : placeRestrs)
			if (pri.first_unset_alpha() != '0')	return true;
		return false; 
	}
	
	
}
