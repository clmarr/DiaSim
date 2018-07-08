import java.util.List; 
import java.util.ArrayList; 

/**
 * @date 1 June 2018
 * @author Clayton Marr
 * Class to represent the context of a shift
 * in order to handle phenomena like ()* and ()+, etc.
 */

public class SChangeContext {
	
	private int minSize;
	private boolean boundsMatter; //determines if we will pass over boundary markers (morpheme, word) in the input. 
	
	private List<RestrictPhone> placeRestrs; // the restriction on each place as indicated by index 
	private String[] parenMap;  /**parenMap is a String[] that is a "map" of where parenthetical statements apply
	 * ..., structured as illustrated by this example (the top row is the indices IN PARENMAP)
	 * 		0	|	1	|	2	|	3	|	4 	|	5	|	6	|	7
	 * 		i0 	|  +(:4	| 	i1 	| 	i2 	|  )+:1 | 	(:7	| 	i3	|	):5 
	 * cells with contents starting i indicate that the cell corresponds to the index of the number following 
	 * 		in placeRestrs
	 * cells with paren markers { +(, )+, (, ), *(, )*, } indicate where parens open and close
	 * 		relative to those indices in parenMap
	 * 		the number on the inside of hte paren indicates which index IN PARENMAP 
	 * 			is where the corresponding opening or closing paren lies. 
	 * 
	 * when this is entered, in order to save time, the min number of places within each paren window
	 * 		are calculated using the auxiliary method 
	 * 
	*/
	
	public SChangeContext (List<RestrictPhone> prs, String[] pm)
	{
		placeRestrs = new ArrayList<RestrictPhone>(prs); 
		parenMap = pm; boundsMatter = false;
		minSize = generateMinSize(); 
		markParenMapForMinPlacesInEachWindow();
	}
	
	public SChangeContext (List<RestrictPhone> prs, String[] pm, boolean bm)
	{
		placeRestrs = new ArrayList<RestrictPhone>(prs); 
		parenMap = pm; boundsMatter = bm; 
		minSize = generateMinSize(); 
		markParenMapForMinPlacesInEachWindow(); 
	}
	
	//auxiliary for initialization : mark all parenthetical cells in parenMap
		// with the minimum number of places inside
	// we assume they come in in the form +(:4 and )+:7 etc ... 
	public void markParenMapForMinPlacesInEachWindow()
	{
		int currIndex = parenMap.length - 1;
		while(currIndex > 1) // no parenthesis could ever close before index 2 else it would be containing nothing. 
		{
			if(parenMap[currIndex].contains(")"))
			{
				int openerIndex = Integer.parseInt(parenMap[currIndex].split(":")[1]); 
				int minPlaces = minPlacesInParenWindow(openerIndex, currIndex); 
				parenMap[currIndex] = parenMap[currIndex] + "," + minPlaces;
				parenMap[openerIndex] = parenMap[currIndex] + "," + minPlaces; 
			}
			else
			{	
				while(!parenMap[currIndex].contains(")") && currIndex > 1)	
					currIndex--;
			}
			
		}
			
	}
	
	//auxiliary method to determine the minimum possible input size that can satisfy these context restrictions
	//must be called BEFORE markParenMapForMinPlacesInEachWindow is called. 
	private int generateMinSize()
	{
		int prSize = placeRestrs.size(), count = 0;
			//optParenDepth is the number of optional { ()*, ()} paren structures we are currently in
		
		for(int i = 0; i < prSize; i++)
		{
			String currMapCell = parenMap[i];
			if(currMapCell.contains("(")) // then hop. 
			{	if(currMapCell.contains("+"))	i = Integer.parseInt(currMapCell.split(":")[1]);	} 
			else
			{
				assert !currMapCell.contains(")"): "Error: unopened ')' found"; 
				count++; 
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
	 *			3a2) Else return whehter we can reach a true value if we hop to before beginning of this paren structure
	 *		3b) else 
	 *			3b1) If the paren is a normal paren or a starred paren,
	 *					 first check if we can get a true value by excluding it (i.e. hop to be before the opening
	 *					, and if not test the branch where it is included. 
	 *			3b2) If it is a plussed paren, iterate as normal -- i.e. simply move the marker from the closing paren to the 
	 *					last element and follow the while loop described in the next case
	 * 4) We have a disjunctive parenthetical statement starting here
	 * 		4a) If it is starred or plussed, branch: first check if we have a match if we just iterate as normal with moving cpim back one
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
		if(cpic < 0)	return false; 
		
		int currPlaceInCand = cpic, currRestrPlace = crp, currPlaceInMap = cpim; 

		while(currRestrPlace >= 0 && currPlaceInCand >= 0 && currPlaceInMap >= 0)
		{
			if(parenMap[currPlaceInMap].contains(")"))
			{
				//if we could not possibly include the contents of this paren structure because there are too many 
					// for the space we have left in the input... 
				if(Integer.parseInt(parenMap[currPlaceInMap].split(":")[1].split(",")[1]) < currPlaceInCand ) 
				{
					if(parenMap[currPlaceInMap].contains("+"))	return false; 
					else	return isPriorMatchHelperExcludeParen(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap);
				}
				// if it is a paren structure taht must occur at least once...
				if(parenMap[currPlaceInMap].charAt(1) != '+') 
				{
					if(isPriorMatchHelperExcludeParen(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap))	return true; 
					return isPriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap - 1); 
				}
				assert parenMap[currPlaceInMap].contains("+"): "Error: '+' likely at wrong spot in parenMap[cpim] String";
				return isPriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap - 1); 
			}
			if (parenMap[currPlaceInMap].contains("("))
			{
				if("+*".contains(parenMap[currPlaceInMap].substring(0, 1)))
				{
					if(isPriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap -1 ))	return true; 
					return isPriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, 
							Integer.parseInt(parenMap[currPlaceInMap].split(":")[1].split(",")[1])); 
				}
				return isPriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap - 1); 
			}
			
			if(!boundsMatter && phonSeq.get(currPlaceInCand).getType().contains("bound")
					&& !placeRestrs.get(currRestrPlace).toString().equals(phonSeq.get(currPlaceInCand)+""))
				currPlaceInCand--;
			else if(!placeRestrs.get(currRestrPlace).compare(phonSeq.get(currPlaceInCand)))	return false; 
			else	{	currPlaceInCand--; currRestrPlace--; currPlaceInMap--; 	}
		} 
		if(currRestrPlace < 0)		return true;
		else	return false; 
	}

	//auxiliary method for recursive calls that exclude the parenthesis ending at the current spot in parenMap
	private boolean isPriorMatchHelperExcludeParen (List<SequentialPhonic> phonSeq, int cpic,
			int crp, int cpim)
	{
		int mapSpotPreOpener = Integer.parseInt(parenMap[cpim].split(":")[1].split(",")[0]) - 1 ;
		if (mapSpotPreOpener < 0)	return false; 
		
		int placeBeforeOpener = 0, proxyMapSpot = mapSpotPreOpener; 
		while (placeBeforeOpener == 0 && proxyMapSpot > 0)
		{
			if(parenMap[proxyMapSpot].charAt(0) == 'i')
				placeBeforeOpener = Integer.parseInt(parenMap[proxyMapSpot].substring(1));
			else	proxyMapSpot--; 
		}
		return isPriorMatchHelper(phonSeq, cpic, mapSpotPreOpener, placeBeforeOpener); 
	}

	public boolean isPosteriorMatch(List<SequentialPhonic> phonSeq, int indAfter)
	{
		System.out.println("ind after is "+indAfter);
		
		if (indAfter < phonSeq.size())
			System.out.println("isPosteriorMatch called with lastInd "+indAfter+", where"
				+ " we have "+phonSeq.get(indAfter)); 
		
		if (minSize == 0)	
		{
			System.out.println("minSize = 0");
			return true; 
		}
		if (minSize > phonSeq.size() - indAfter)
		{
			System.out.println("minSize > phonSeq.size() - lastInd");
			return false; 
		}
		int currPlaceInCand = indAfter, currRestrPlace = 0, currPlaceInMap = 0; 
		return isPosteriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap); 
	}
	
	private boolean isPosteriorMatchHelper(List<SequentialPhonic> phonSeq, int cpic, int crp, int cpim)
	{		
		assert cpic <= phonSeq.size() && crp <= placeRestrs.size() && cpim <= parenMap.length: 
			"Error in call to isPosteriorMatchHelper -- at least one of the counter params was way too high";
		if(crp == placeRestrs.size())	return true;
		if(cpic == phonSeq.size())	return false; 
		
		int currPlaceInCand = cpic, currRestrPlace = crp, currPlaceInMap = cpim,
				lenPhonSeq = phonSeq.size(), numRestrPlaces = placeRestrs.size(), mapSize = parenMap.length; 
		while( currPlaceInCand < lenPhonSeq && 
				!(currRestrPlace >= numRestrPlaces && currPlaceInMap >= mapSize))
		{
			//TODO debugging
			System.out.println("cpic "+currPlaceInCand+", crp "+currRestrPlace+","
					+ " currPlaceInMap "+cpim);
			System.out.println("lenPhonSeq "+lenPhonSeq+" numRestrPlaces "
					+ numRestrPlaces + " mapSize "+mapSize); 
			
			if(parenMap[currPlaceInMap].contains("("))
			{
				//if we could not possibly include the contents of this paren structure because there are too many 
				// for the space we have left in the input... 
				if(Integer.parseInt(parenMap[currPlaceInMap].split(";")[1].split(",")[1]) > lenPhonSeq - currPlaceInCand)
				{	if(parenMap[currPlaceInMap].contains("+"))	return false; 
					else	return isPosteriorMatchHelperExcludeParen(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap); 
				}
				// if it is a paren structure taht must occur at least once...
				if(parenMap[currPlaceInMap].charAt(1) == '+') 
				{
					if(isPosteriorMatchHelperExcludeParen(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap))	return true; 
					return isPosteriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap + 1); 
				}
				assert parenMap[currPlaceInMap].contains("+"): "Error: '+' likely at wrong spot in parenMap[cpim] String";
				return isPosteriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap + 1); 
			}
			if(parenMap[currPlaceInMap].contains(")"))
			{
				if("+*".contains(parenMap[currPlaceInMap].substring(0, 1)))
				{
					if(isPosteriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap + 1 ))	return true; 
					return isPosteriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, 
							Integer.parseInt(parenMap[currPlaceInMap].split(":")[1].split(",")[1])); 
				}
				return isPosteriorMatchHelper(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap + 1); 
			}
			if(!boundsMatter && phonSeq.get(currPlaceInCand).getType().contains("bound") 
					&& !placeRestrs.get(currRestrPlace).toString().equals(phonSeq.get(currPlaceInCand)+""))	{	currPlaceInCand++;	}
			else if(!placeRestrs.get(currRestrPlace).compare(phonSeq.get(currPlaceInCand)))	
			{
				System.out.println("Failure to meet place restriction!" ); //TODO debugging 
				return false; 
			}
			else	{	
				//TODO debugging
				System.out.println("placeRestrs.get(currRestrPlace) : "+placeRestrs.get(currRestrPlace));
				System.out.println("phonSeq.get(currPlaceInCand) : "+phonSeq.get(currPlaceInCand));
				System.out.println("compare : "
					+placeRestrs.get(currRestrPlace).compare(phonSeq.get(currPlaceInCand)));
				System.out.println("proceed"); //TODO debugging
				currPlaceInCand++; currRestrPlace++; currPlaceInMap++; 	}
			
		}
		System.out.println("currPlaceInCand "+currPlaceInCand+" currRestrPlace "+currRestrPlace+" currPlaceInMap "+currPlaceInMap);
		System.out.println("numRestrPlaces "+placeRestrs.size());
		if(currRestrPlace == numRestrPlaces)	return true;
		System.out.println("false");
		return false; 
	}
	
	private boolean isPosteriorMatchHelperExcludeParen(List<SequentialPhonic> phonSeq, int cpic,
			int crp, int cpim)
	{
		int mapSpotPostCloser = Integer.parseInt(parenMap[cpim].split(":")[1].split(",")[0]) + 1 ;
		if (mapSpotPostCloser >= parenMap.length)	return false; 
		
		int placeAfterCloser = placeRestrs.size(), proxyMapSpot = mapSpotPostCloser; 
		while (placeAfterCloser == placeRestrs.size() && proxyMapSpot < placeRestrs.size() - 1)
		{
			if(parenMap[proxyMapSpot].charAt(0) == 'i')
				placeAfterCloser = Integer.parseInt(parenMap[proxyMapSpot].substring(1));
			else	proxyMapSpot++; 
		}
		return isPriorMatchHelper(phonSeq, cpic, mapSpotPostCloser, placeAfterCloser); 
	}
	
	
	//auxiliary method -- gets the minimum number of places in placeRestrs that could be covered 
		//in the contents of one window in parenMap, only to be used for calculating minimum number within
		// a paren structure 
	// first and last are the bounds of the window in indices in PARENMAP
		// --they should both be parentheses
	private int minPlacesInParenWindow (int first, int last)
	{
		assert first + 1 < last && first > 0 && last < parenMap.length : 
			"Error: Invalid bounds of window entered for minPlacesInParenWindow()"; 
		
		assert parenMap[first].contains("(") && parenMap[last].contains(")") : 
			"Error: in minPlacesInParenWindow, window specified should be a parenthetical"; 
		
		int count = 0, mapSpot = first + 1, optParenDepth = 0;
			//optParenDepth counts number of parens that are contained WITHIN the window
		while (mapSpot < last)
		{
			String curr = parenMap[mapSpot]; 
			if (curr.contains("("))
			{	if (!curr.contains("+"))	optParenDepth++; 	}
			else if (curr.contains(")"))
			{	
				if (!curr.contains("+"))	optParenDepth--;
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
}
