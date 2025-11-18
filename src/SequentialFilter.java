import java.util.List; 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @date 1 June 2018
 * @author Clayton Marr
 * Class to represent the context of a shift
 * in order to handle phenomena like ()* and ()+, etc.
 * TODO note as of July 12 2018, (...)+ structures are now illegal 
 		TODO (2025) is this still the case
 */

public class SequentialFilter {
	
	private int minSize;
	private boolean boundsMatter; //determines if we will pass over boundary markers (morpheme, word) in the input.
	
	private List<RestrictPhone> placeRestrs; // the restriction on each place as indicated by index 
	private String[] parenMap;  /**parenMap is a String[] that is a "map" of where parenthetical statements apply
	 * ..., structured as illustrated by this example (the top row is the indices IN PARENMAP)
	 *		0  | 1     | 2  | 3  | 4    | 5   | 6  |	 7			parenMap index
	 *		i0 | *(:4  | i1 | i2 | )*:1 | (:7 | i3 |	 ):5 		contents
	 * cells with contents starting i indicate that the cell corresponds to the index of the number following 
	 * 		in placeRestrs
	 * cells with paren markers { (, ), *(, )*, } indicate where parens open and close
	 * 		relative to those indices in parenMap
	 * 		the number on the inside of the paren(thesis) indicates which index IN PARENMAP 
	 * 			is where the corresponding opening or closing paren lies. 
	 * note as of July 12 2018 -- when this is entered, in order to save time, the min number of places within each paren window
	 * 		are calculated using the auxiliary method markParenMapForMinPlacesInEachWindow
	 * 		so it could look like this: 
	 *		0  | 1      | 2  | 3  | 4      | 5     | 6  |	 7			parenMap index
	 *		i0 | *(:4,2 | i1 | i2 | )*:1,2 | (:7,1 | i3 |	 ):5,1		contents
	*/
	
	private boolean DEBUGGING_ON = true; 
	
	public static String UNSET_ALPHVAL =""; 
	public static char ALPH_DELIM = '|';
	private HashMap<String,String> localAlphSpecs; // key -- alpha symbol, value -- current setting, "" if unset.
	private HashMap<String,List<Integer>> localAlphLocs; // key-- alpha symbol, value -- locations in paren(Alpha)Map where it occurs
	private List<String> parenthesizedAlphas; // list of alphas that occur in parens
	
	//because alphas must all be one character, proxies are used for negated alphas -- either externally determined in a rule def (SChangeFactory), or locally here
		// key -- proxy character, value -- alpha value it's negating
		// note that in the EXTERNAL case (coming from SChangeFactory, most likely), the negated alpha value WILL NOT BE PRESENT in this SequentialFilter. 
		// TODO NOTE currently these are set in the declaration!  
	private HashMap<String,String> negProxyAlphas; 
	
	private String[] parenAlphaMap; 
	/** parenAlphaMap -- for calculating where alphas are in hte parenMap
	 * indices correspond to those of parenMap, NOT placeRestrs
	 * default (no alphas present) = "" -- also applies for paren cells
	 * if alphas present, not parenthesized: list of alpha symbols present at this location, delimited by '|'. 
	 * if alphas present, parenthesized: '(' followed by list of alpha symbols present at this location, delimited by '|'. 
	 */
	
	/**
	 * @param prs place restrictions
	 * @param pm  paren map. 
	
	 * @param bm whether bounds matter
	 * @param negProxMap -- neg proxies for - alpha values -- key proxy, value proxied alpha val
	 * 		if empty -- no neg proxies. 
	 * 		should already be passed in at this point
	 * 			will not be extracted in this class.
	 */
	// abbreviations in use in comments: 
	// bm = whether bounds matter. 
	// pm = paren map. 
	// npalphs = neg proxy alph map 
	private void initialize(List<RestrictPhone> prs, String[] pm, boolean bm, HashMap<String, String> negProxMap)
	{
		parenMap = pm ; 
		placeRestrs = new ArrayList<RestrictPhone>(prs); 
		
		boundsMatter = bm;
		minSize = generateMinSize(); 
		
		markParenMapForMinPlacesInEachWindow();		
		
		minSize = generateMinSize(); 
		
		initAlpha(negProxMap); 	
	}
	
	public SequentialFilter (List<RestrictPhone> prs, String[] pm)
	{	initialize(prs, pm, false, new HashMap<String, String>() ); }
	
	public SequentialFilter (List<RestrictPhone> prs, String[] pm, boolean bm)
	{	initialize(prs, pm, bm, new HashMap<String, String>() ); 	}
	
	public SequentialFilter (List<RestrictPhone> prs, String[] pm, HashMap<String, String> proxies)
	{	initialize(prs, pm, false, proxies);  }
	
	public SequentialFilter (List<RestrictPhone> prs, String[] pm, boolean bm,  HashMap<String, String> proxies)
	{	initialize(prs, pm, bm, proxies); 	}

	
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
					proxypim = pairedParenLoc(proxypim) - 1; 
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
				int minContents = getMinParenSegments(currPlaceInMap);  
				//if we could not possibly include the contents of this paren structure because there are too many 
					// for the space we have left in the input... 
				if(minContents > currPlaceInCand || minContents > currRestrPlace)
					return isPriorMatchHelperExcludeParen(phonSeq, currPlaceInCand, currRestrPlace, currPlaceInMap);
				
								
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
					currPlaceInMap = pairedParenLoc(currPlaceInMap); 
					
					int proxyPlace = currPlaceInMap - 1; 
					while(parenMap[proxyPlace].charAt(0) != 'i')	
					{
						proxyPlace--; 
						if(proxyPlace <= formerPlace)
							throw new Error("Something wrong: parenthesis structure seems to have no actual phone restrictions inside: "+UTILS.printParenMap(this));
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
			else {
				SequentialPhonic cpi = phonSeq.get(currPlaceInCand); 
				HashMap<String,String> alphExtract = new HashMap<String,String>(); 
				
				if(UTILS.hasUnsetAlpha(placeRestrs.get(currRestrPlace))) // there's an unset alpha. 
				{

					String typeHere = cpi.getType();
					if (typeHere.equals("morph bound"))	
					{	currPlaceInCand--; currRestrPlace--; currPlaceInMap--; continue; }
					if (!typeHere.equals("phone")) // i.e. we have a word bound, most probably. 
					{	if (!typeHere.equals("word bound"))	System.out.println("unexpected comparison of alpha feature matrix to object of type "+typeHere); 
						return false; }
					RestrictPhone rpi = placeRestrs.get(currRestrPlace); 
					if (rpi.check_for_alpha_conflict(cpi) ? true : !rpi.comparePreUnsetAlpha(cpi))	
						return false; 
					// if reached here, going to have to extract and apply alpha values 
					alphExtract = rpi.extractAndApplyAlphaValues(cpi); 
						//^ keyset of which will be reset in case of failure. 
					
					applyAlphaValues(alphExtract); 	
				}
				
				if(!placeRestrs.get(currRestrPlace).compare(phonSeq.get(currPlaceInCand))) {
					resetTheseAlphaValues( new ArrayList<String>(alphExtract.keySet())); 

					return false; 
				}
				currPlaceInCand--; currRestrPlace--; currPlaceInMap--; 	
			}
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
					proxypim = pairedParenLoc(proxypim) - 1; 
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
		int mapSpotPreOpener = pairedParenLoc(cpim) - 1 ;
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

	
	
	/** 
	 * @return @true if stipulations of placeRestrs match for (somewhere in) pr
	 * @note isPosteriorMatchHelper called as means of checking matching for a sequence; `asymmetry' of not calling isPriorMatch(Helper) no cause for alarm. 
	 *  all necessary issues should be caught on the forward iteration through the phonetic segments
	 * 	*  since this is how iteration works where this is called in ErrorAnalysis.articulateSubsample (its only call it seems)
		*  and it's not like a segment would just be missed, since it starts at the beginning and goes to the end.
	*  concerning @alpha features, this computes compatibility recursive, effectively based @locally based on those NOT set already
	*  		those that are set outside this method could have been set for adherence to already @determined for adherence to input, output, or another context SequentialFilter
	 * @param prCand -- sequence to compare for potential match
	 * @param backward -- true if going backward, like if this is being used for a prior context. 
	 * if this is used for prior or posterior context in a way that alpha feats will need to be extracted for external concordance,
	 * 		may need to turn off the resetting with @param resetAfterMatch 
	 * 			but this will not be relevant for the recursvie calls to filtCheckHelper, which are only resetting in the case of a match failure. 
	 */
	public boolean filtCheck(List<SequentialPhonic> prCand, boolean resetAfterMatch)	{	return filtCheck(prCand,resetAfterMatch,false);	}
	public boolean filtCheck(List<SequentialPhonic> prCand, boolean resetAfterMatch, boolean backwards ) {	
		
		//alphs that will be set and reset within this method's recursion. 
		List<String> internAlphs = new ArrayList<String>(); 
		for (String alphi : localAlphSpecs.keySet())
			if (localAlphSpecs.get(alphi).equals(UNSET_ALPHVAL))	internAlphs.add(alphi);
		
		// if there are no alpha values, task is easy. 
		if (internAlphs.size() == 0)
		{
			if (backwards) 
			{
				for (int cpic = prCand.size()-1; cpic >= minSize; cpic--)
					if (isPriorMatchHelper(prCand,cpic,placeRestrs.size() - 1, parenMap.length-1))
						return true; 
				return false; 
			}
			for (int cpic = 0 ; cpic <= prCand.size()- minSize; cpic++)
			{	if (isPosteriorMatchHelper(prCand,cpic,0,0))	return true; }
			return false; 
		}
	
		//if we're here, we have local alphas to deal with... 
		for (int matchStart = 0; matchStart <= prCand.size() - minSize ; matchStart ++ ) // cpic is starting index
		{
			boolean success =  filtCheckHelper ( 
					new ArrayList<SequentialPhonic>( backwards ?
							prCand.subList(0, prCand.size() - matchStart) 
							: prCand.subList(matchStart, prCand.size())), 
					backwards ? placeRestrs.size() - 1 : 0 , 
					backwards ? parenMap.length - 1 : 0 , 
					backwards, AlphaTester.getLineNumber()) ; 
			if (success) 
			{
				if (resetAfterMatch)	resetTheseAlphaValues(internAlphs);
				return true; 
			} 
			resetTheseAlphaValues(internAlphs);
		}
	
		return false;
	}
	
	/** 
	 * 
	 * @param prCandLeft -- this method will be called to recursively remove elements from the candidate segment as they are checked , 
	 * 		wihle keeping htem in the parent call if it fails
	 * @param alphsToSetWithin -- alphas being LOCALLY determined; not those set (from external class objects) before filter is checked
	 * 			 	and not those that have already been set in parent calls. 
	 * 				it is these that will be UNSET at the end of the call if a match fails. 
	 * @note filter resetting within this method only happens upon match failure! 
	 * 
	 * @param placeRestrLoc -- place in placeRestrs structure
	 * @param parenMapLoc -- place in parenMap (and parenAlphaMap) 
	 * @param backward -- if going backwards [e.g. if this ends up used to check a prior contexgt
	 * @param lineCall -- debugging purposes. 
	 * @return
	 */
	// TODO is alphsToSetWithin even necessary? {currently (last notated 10/6/25 -- not being used.)
	public boolean filtCheckHelper ( List<SequentialPhonic> prCandLeft, /*List<String> alphsToSetWithin, */ int placeRestrLoc, int parenMapLoc, boolean backward)
	{	return filtCheckHelper(prCandLeft,placeRestrLoc,parenMapLoc,backward,-1); 	}
	public boolean filtCheckHelper ( List<SequentialPhonic> prCandLeft, /*List<String> alphsToSetWithin, */ int placeRestrLoc, int parenMapLoc, boolean backward, int lineCall)
	{
		assert backward ? placeRestrLoc >= -1 && parenMapLoc >=  -1
				: (placeRestrLoc <= placeRestrs.size() && parenMapLoc <= parenMap.length): 
			"Error in call to isPosteriorMatchHelper -- at least one of the counter params was way too high";
		
		// if reached end of placeRestrs -- good chance filter is passed!
		if (backward ? placeRestrLoc == -1 : placeRestrLoc == placeRestrs.size())	
			return true; 
		// if somehow exhausted parenmap without exhausting placeRestrs (which would trigger the above) --  must be structure storing error
		if (backward ? parenMapLoc == -1 : parenMapLoc == parenMap.length)	
			throw new Error("Reached end of parenMap but still iterating in placeRestrs (@"+placeRestrLoc+"/"+placeRestrs.size()+") -- must be error!");
		
		int incr = backward ? -1 : 1; //increment 
		String PMcell = parenMap[parenMapLoc]; 

		// if we're at paren opening -- either (1) advance past paren, or (2) enter it 
		if (backward ? PMcell.contains(")") : PMcell.contains("("))
		{		
			//advance past? :
			int nextPMspot = pairedParenLoc(parenMapLoc) + incr;
			// if would advance to end -- true. 
			if (backward ? nextPMspot < 0 : nextPMspot == parenMap.length) 	return true; 
			
			boolean nextSpotIsParen = 
					parenMap[nextPMspot].contains(")") || parenMap[nextPMspot].contains("(");
					
			 
			if (filtCheckHelper(prCandLeft, nextSpotIsParen ? placeRestrLoc : Integer.parseInt(parenMap[nextPMspot].substring(1)), nextPMspot, backward, AlphaTester.getLineNumber()))
				return true; 
			
			nextPMspot = parenMapLoc + incr; 
			nextSpotIsParen =  parenMap[nextPMspot].contains(")") || parenMap[nextPMspot].contains("(");
			//if minimum possible places would exceed material we have left to match to, then don't enter paren -- return match failure instead
			if (getMinParenSegments(parenMapLoc) > prCandLeft.size() )	// don't restrict based on placeRestrs, bc could recurse in that too. 
				return false; 
			
			//enter paren. 
			else return filtCheckHelper(prCandLeft, nextSpotIsParen ? placeRestrLoc : Integer.parseInt(parenMap[parenMapLoc+incr].substring(1)), nextPMspot, backward,AlphaTester.getLineNumber()); 
		}
		
		/** at @closing paren, and reached it from normal advancing (since skipping paren would advance to right after it
		 *  @reset @parenthesis @local @alphas
		 */
		if (backward ? PMcell.contains("(") : PMcell.contains(")"))
		{
			// reset parenthesis local alphas.
			List<String> parenLocalAlphas = parenthesisLocalAlphas(parenMapLoc); 
			resetTheseAlphaValues(parenLocalAlphas); 
			
			// if can advance, do so  -- and its all we do unless it's a ()+ or ()* paren
			int nextPMspot = parenMapLoc + incr; 
			boolean nextSpotIsParen = 
					parenMap[nextPMspot].contains(")") || parenMap[nextPMspot].contains("(");
			
			if (filtCheckHelper(prCandLeft, 
					nextSpotIsParen ? placeRestrLoc : Integer.parseInt(parenMap[nextPMspot].substring(1)), 
					nextPMspot, backward,AlphaTester.getLineNumber()))
				return true; 
			
			
			nextPMspot = pairedParenLoc(parenMapLoc) + incr; 
			nextSpotIsParen = 
					parenMap[nextPMspot].contains(")") || parenMap[nextPMspot].contains("(");
			// if it's a ()+ or ()* paren
			if (PMcell.contains("*") || PMcell.contains("+")) //recurse if you can, if couldn't advance.. 
				return  
					getMinParenSegments(parenMapLoc) > prCandLeft.size() ? false :
						filtCheckHelper(prCandLeft, 
								nextSpotIsParen ? placeRestrLoc : Integer.parseInt(parenMap[nextPMspot].substring(1)), 
								nextPMspot, backward,AlphaTester.getLineNumber()); 
					
			//if reached here, possibilities have been exhausted. 
			return false;
		}

		// now we know were not at a paren. 
		// false if candidate material has been exhausted, sinec only material left can no longer be optional
		if (prCandLeft.size() == 0)	return false; 
		
		//if reached this point, we're dealing with actual content...
		RestrictPhone rpi = placeRestrs.get(placeRestrLoc); 
		List<SequentialPhonic> candRemainder = new ArrayList<SequentialPhonic>(prCandLeft); 
		
		SequentialPhonic cpi = candRemainder.remove(backward ? prCandLeft.size() - 1 : 0); 
		if (rpi.has_alpha_specs() ? rpi.first_unset_alpha() != '0' : false ) // alph feats to extract here ..
		{
			String cpitype = cpi.getType(); 
			// edge case: bypass morphbound
			if (cpitype.equals("morph bound")) 
				return filtCheckHelper(candRemainder, placeRestrLoc, parenMapLoc, backward,AlphaTester.getLineNumber()); 
			if (!cpitype.equals("phone"))	return false; 
			
			//abort alpha conflict, or if it wouldn't match anyways
			if (rpi.check_for_alpha_conflict(cpi) ? true : !rpi.comparePreUnsetAlpha(cpi))	
				return false; 
			
			// if reached here, going to have to extract and apply alpha values 
			HashMap<String,String> alphExtract = rpi.extractAndApplyAlphaValues(cpi); 
				//^ keyset of which will be reset in case of failure. 
			
			applyAlphaValues(alphExtract); 
			
			// revert alpha values if recursive calls fails. 
			int nextPMspot = parenMapLoc + incr; 
			int nextPRspot = nextPMspot == parenMap.length ? placeRestrLoc + incr 
					: parenMap[nextPMspot].contains(")") || parenMap[nextPMspot].contains("(") ? placeRestrLoc : Integer.parseInt(parenMap[nextPMspot].substring(1)); 

			if ( ! filtCheckHelper (candRemainder, nextPRspot, nextPMspot, backward,AlphaTester.getLineNumber())) 
			{
				resetTheseAlphaValues( new ArrayList<String>(alphExtract.keySet())); 
				return false; 
			}
			else return true; 
		}
		else { 
			int nextPMspot = parenMapLoc + incr; 
			int nextPRspot = nextPMspot == parenMap.length ? placeRestrLoc + incr 
					: parenMap[nextPMspot].contains(")") || parenMap[nextPMspot].contains("(") ? placeRestrLoc : Integer.parseInt(parenMap[nextPMspot].substring(1)); 

			return rpi.compare(cpi) == false ? false 
				: filtCheckHelper (candRemainder, nextPRspot, nextPMspot, backward,AlphaTester.getLineNumber()); 
		}
	}
	
	/**
	 * @return list of all boundaries ([onset, end]) of matched filters in @param pr
	 *  	@empty if there are none, i.e. no match. 
		* @note that of the boundary pairs, while the first element is the (positive) index of the onset of the filter match
			* the second is the *negative* index of the offset *counting back from the end of the word* (as in python indexing, etc.) 
	 */  
	public List<int[]> filtMatchBounds(List<SequentialPhonic> pr)
	{
		if(minSize == 0)	throw new Error("You shouldn't be using filterSequence.filtMatchBounds with filter with no necessary length.");
		if(minSize > pr.size())	return new ArrayList<int[]>(); 

		List<int[]> out = new ArrayList<int[]>(); 
		
 		int trueOnset = 0, currMatchStart = -1;
		List<SequentialPhonic> dummy = new ArrayList<SequentialPhonic>(pr); 

		while (dummy.size() >= minSize) {
			for (int cpic = 0 ; cpic < dummy.size() && currMatchStart == -1; cpic++)
				if(filtCheck(cpic == 0 ? dummy : dummy.subList(cpic, dummy.size()), true, false))
				//formerly: if(isPosteriorMatchHelper(dummy,cpic,0,0))	currMatchStart = cpic; //this will effectively halt the for-loop
			
			if (currMatchStart == -1)	return out;	// this is an empty list at this point -- returning empty, as there is no match. 
			else	{
				int matchEnd = currMatchStart + minSize - 1; 
				while(matchEnd < dummy.size() ? 
						!isPriorMatchHelper(dummy,matchEnd,placeRestrs.size()-1,parenMap.length-1) : false)
					matchEnd++;
				
				out.add(new int[] {trueOnset + currMatchStart, 
						trueOnset + matchEnd - pr.size()}
						); 
				trueOnset = trueOnset + matchEnd + 1;
				currMatchStart = -1;
				dummy = dummy.subList(matchEnd+1,dummy.size());
		}}
		
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
	
	/**
	 * @param phonSeq	phone sequence we are checking
	 * @param cpic	location in phonSeq		- current place in (candidate) phonic sequence
	 * @param crp	location in placeRestrs	- current restriction place (restrictions upon candidate phones)
	 * @param cpim	location in parenMap	
	 * @return
	 * */
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
					proxypim = pairedParenLoc(proxypim) + 1; 
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
			// if we're at a parenthesis -- then return true if it's true either with or without it, taking account length of the input for if "with" is possible. 
				// forking based on any number of recurrences scenario (i.e. "( ... )*") handled in next conditional, since '*' is placed upon closing parenthesis
			if(parenMap[currPlaceInMap].contains("("))
			{
				int minPhonesInParen = getMinParenSegments(currPlaceInMap); 
							//Integer.parseInt(parseInt(parenMap[currPlaceInMap].split(":")[1].split(",")[1]); 

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
					currPlaceInMap = pairedParenLoc(currPlaceInMap); //go back to beginning of repeated optional segment
					int proxyPlace = currPlaceInMap + 1; 
					
					while(parenMap[proxyPlace].charAt(0) != 'i') 
					{
						proxyPlace++; 
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
			else {
				SequentialPhonic cpi = phonSeq.get(currPlaceInCand); 
				HashMap<String,String> alphExtract = new HashMap<String,String>(); 
				
				if(UTILS.hasUnsetAlpha(placeRestrs.get(currRestrPlace))) // there's an unset alpha. 
				{
					String typeHere = cpi.getType();
					if (typeHere.equals("morph bound"))	
					{	currPlaceInCand++; currRestrPlace++; currPlaceInMap++; continue; }
					if (!typeHere.equals("phone")) // i.e. we have a word bound, most probably. 
					{	if (!typeHere.equals("word bound"))	System.out.println("unexpected comparison of alpha feature matrix to object of type "+typeHere); 
						return false; }
					
					RestrictPhone rpi = placeRestrs.get(currRestrPlace); 
					if (rpi.check_for_alpha_conflict(cpi) ? true : !rpi.comparePreUnsetAlpha(cpi))	
						return false; 
					
					// if reached here, going to have to extract and apply alpha values 
					alphExtract = rpi.extractAndApplyAlphaValues(cpi); 
						//^ keyset of which will be reset in case of failure. 
					
					applyAlphaValues(alphExtract); 	
				}

				if(!placeRestrs.get(currRestrPlace).compare(cpi))	
				{
					resetTheseAlphaValues( new ArrayList<String>(alphExtract.keySet())); 
					return false; 
				}
					
				currPlaceInCand++; currRestrPlace++; currPlaceInMap++;
			}	
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
					proxypim = pairedParenLoc(proxypim) + 1; 
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
		int mapSpotPostCloser = pairedParenLoc(cpim) + 1 ;
		
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
	
	
	// -- PAREN MAP AUXILIARIES FOLLOW -- 
	
	/**markParenMapForMinPlacesInEachWindow
	 * auxiliary for initialization : mark all parenthetical cells in parenMap
		* with the minimum number of places inside
	 	* @prerequisite assume they come in in the form *(:4 and )*:7 etc ... 
	 	*/ 
	private void markParenMapForMinPlacesInEachWindow()
	{
		int currIndex = parenMap.length - 1;
		while(currIndex > 1) // no parenthesis could ever close before index 2 else it would be containing nothing. 
		{
			if(parenMap[currIndex].contains(")"))
			{
				int openerIndex = pairedParenLoc(currIndex);  
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
	
	/** genMinSize
	 * 
	 * @return the minimum possible input size for parenMap that can satisfy these context restrictions
	 * @precondition must be called AFTER markParenMapForMinPlacesInEachWindow is called. 
	 */
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
	
	/** minPlacesInParenWindow
	 * auxiliary for parenMaps
	 * @param first -- opening bound of window in indexed cell of @paramMap
	 * @param last -- closing bound corresponding to hte above
	 * @return minimum number of places in @param @placeRestrs that could be covered 
		* in the contents of one window in @param @parenMap
		* @usagenote only to be used for calculating minimum number within a parenthesized structure 
	 */
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
	
	/**
	 * pairedParenLoc
	 * given @param thisParenLoc loc of current parenthesis in @parenMap
	 * @return location in parenMap of the corresponding parenthesis
	 * @prerequisite thisParenLoc indexes a cell in parenMap that actually has a parenthesis. 
	 */
	public int pairedParenLoc(int thisParenLoc)
	{	
		String contents = parenMap[thisParenLoc].split(":")[1]; 
		return Integer.parseInt(contents.contains(",") ? contents.split(",")[0] : contents); 
	}
	
	/**
	 * given @param parenLoc, an index in @parenMap of a parenthesis
	 * @return minimum number of segments in the parenthesis (i.e. it oculd be repeated etc.)
	 * @prerequisite these have bee marked by markParenMapForMinPlacesInEachWindow.
	 */
	public int getMinParenSegments(int parenLoc)	{	return Integer.parseInt(parenMap[parenLoc].split(",")[1]);	}
	
	// ---- ACCESSORS ----- 
	
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
	
	// --- ACCESSORS---
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
	
	// -- ALPHA ACCESSORS -- 
	public HashMap<String,String> getLocalAlphSpecs()	{	return localAlphSpecs;	}
				// above seems to only be used in AlphaTester
	public HashMap<String,List<Integer>> getLocalAlphLocs()	{	return localAlphLocs;	}
	public List<String> getParenthesizedAlphas()	{	return parenthesizedAlphas;	}
	public String[] getParenAlphaMap()	{	return parenAlphaMap;	}
	public boolean hasAlphaSpecs()	{	return localAlphSpecs.size() > 0 ;	}
	public boolean hasNegAlphProxies() 	{	return negProxyAlphas.size() > 0;	}
	public boolean hasParenthesizedAlpha() // true if there is at least one alpha value in a parenthesis -- these need to be set outside the paren first. 
	{	return parenthesizedAlphas.size() > 0;	}
	
	public boolean has_unset_alphas()
	{
		if (!hasAlphaSpecs())	return false;
		for (String spec : localAlphSpecs.values())
			if (spec.equals(UNSET_ALPHVAL))	return true; 
		
		for (RestrictPhone pri : placeRestrs) //TODO this should be trivial, but for security do this too. 
			if (pri.first_unset_alpha() != '0')	return true;
		return false; 
	}
	
	public boolean has_unset_paren_alphas()
	{
		if (!hasParenthesizedAlpha())	return false; 
		for (String pa_i : parenthesizedAlphas)
			if (localAlphSpecs.get(pa_i).equals(UNSET_ALPHVAL))	return true; 
		
		return false;
	}
		

	/** 
	 * @param alph -- an alpha variable
	 * @return ( @global UTILS.NULL_PROXY_PAIR) if it is neither a negative proxy, nor proxied
	 * 			@else @return the proxy/proxied alpha variable 
	 */
	public String getProxyPair (String alph)
	{
		return !hasNegAlphProxies() ? UTILS.NULL_PROXY_PAIR :
			UTILS.getProxyPair(alph, negProxyAlphas) ;
		/**if (negProxyAlphas.containsKey(alph))
			return negProxyAlphas.get(alph); 
		if (negProxyAlphas.containsValue(alph))	
			for (String pxi : negProxyAlphas.keySet()) 
				if (negProxyAlphas.get(pxi).equals(alph))
					return pxi; 
		return UTILS.NULL_PROXY_PAIR;*/
	}
	public boolean hasProxyPair (String alph)	{ return !getProxyPair(alph).equals(UTILS.NULL_PROXY_PAIR);	}
	
	
	/**
	 * @return @true iff @param alph is only marked within parentheses
	 * @prerequisite: @parenthesizedAlphas, @localAlphSpecs, @parenMap, and @parenAlphaMap have been initialized. 
	 */
	public boolean alphaOnlyInParentheses(String alpha)
	{
		String candProxy = hasNegAlphProxies() ? getProxyPair (alpha) : UTILS.NULL_PROXY_PAIR; 
		String alph = localAlphSpecs.containsKey(alpha) ? alpha : candProxy; 
		
		if (alph.equals(UTILS.NULL_PROXY_PAIR)) // if it's this, then alpha is neither a valid alpha spec nor a proxy for one. 
				throw new Error("Error: tried to check if an inexistent alpha ("+alpha+")is only parenthetical"); 
		
		if (!parenthesizedAlphas.contains(alph))	return false; 
		
		for (int pami = 0 ; pami < parenAlphaMap.length ; pami ++)
		{
			if (parenMap[pami].contains("("))	{	pami = pairedParenLoc(pami)+1; continue;	}
			if (parenAlphaMap[pami].contains(alph))	return false; 
		}
		return true; 
	}
	
	/**
	 * @param parenBoundLoc -- location in parenMap of parenthesis opener or closer. 
	 * @return list of alphas in the SequentialFilter that exist only in this parenthesis
	 * 		which will be @reset when it is exited!
	 * @else return @empty list
	 */
	public List<String> parenthesisLocalAlphas(int parenBoundLoc)
	{
		if (parenBoundLoc < 0 || parenBoundLoc >= parenMap.length) 
			throw new Error("Error: tried to check for parenthesis-local alpha at an index out of the range of parenMap"); 
		if (!parenMap[parenBoundLoc].contains("(") && !parenMap[parenBoundLoc].contains(")"))
			throw new Error("Error: tried to check for parenthesis-local alpha at an index that isn't a parenthesis bound."); 
		
		ArrayList<String> out = new ArrayList<String>(); 
		int iterationEndpoint = pairedParenLoc(parenBoundLoc); 
		int increment = parenBoundLoc < iterationEndpoint ? 1 : -1; 
		
		// fill possibilities
		int currLoc = parenBoundLoc + increment; 
		while (currLoc != iterationEndpoint)
		{
			if (!parenMap[currLoc].contains("(") && !parenMap[currLoc].contains(")"))
			{
				String[] alphsHere = parenAlphaMap[currLoc].replace("(", "").split(ALPH_DELIM+""); 
				for (String ahi: alphsHere)
					if (!out.contains(ahi))	out.add(ahi); 
				currLoc += increment; 
			}
			else /* paren loc*/	currLoc = pairedParenLoc(currLoc) + increment; 
		}
		
		if (out.size() == 0)	return out; 
		
		// remove if they're elsewhere, except in a subsumed parenthesis
		currLoc = 0; 
		while (currLoc < Math.min(iterationEndpoint, parenBoundLoc))
		{
			// increments currLoc("++") -- don't miss this when debugging!
			String contentHere = parenAlphaMap[currLoc++].replace("(", ""); 
			if (contentHere.length() > 0) 
				for (int oalphj = out.size() - 1; oalphj >= 0 ; oalphj--)
					if (contentHere.contains(out.get(oalphj)))
						out.remove(oalphj); 
		}
		
		if (out.size() == 0)	return out; 

		currLoc = Math.max(iterationEndpoint, parenBoundLoc); 
		
		while(currLoc < parenAlphaMap.length)
		{
				// increments currLoc("++") -- don't miss this when debugging!
			String contentHere = parenAlphaMap[currLoc++].replace("(", ""); 
			if (contentHere.length() > 0) 
				for (int oalphj = out.size() - 1; oalphj >= 0 ; oalphj--)
					if (contentHere.contains(out.get(oalphj)))
						out.remove(oalphj); 
		}
		
		return out;
	}
	
	
	public List<Integer> getPlaceRestrLocsWithAlpha(char alph)	{	return getPlaceRestrLocsWithAlpha(alph+""); 	}
	public List<Integer> getPlaceRestrLocsWithAlpha(String alphsymb)
	{
		List<Integer> out = new ArrayList<Integer>(); 
		if	(!localAlphLocs.containsKey(alphsymb))	return out;
		
		for (int loc_i: localAlphLocs.get(alphsymb))
			out.add(Integer.parseInt(parenMap[loc_i].substring(1)));  //after the "i" 
		return out; 
	}
	
	/**
	 * given @param loc in @parenMap
	 * @return list of all alpha specs present there
	 * otherwise return empty list.
	 * 	empty list also returned if a paren is there IN PAREN MAP, with a warning.
	 */
	public String[] alphasAtParenMapLoc(int loc)	{	
		String pmContent = parenMap[loc]; 
		if ("*()+".contains(pmContent.substring(0,1)))
		{
			System.out.println("tried to check for alphas at a spot ("+loc+") marking a parenthesis in paren map!");
			return new String[0]; 
		}
		
		if (parenAlphaMap[loc].equals(UNSET_ALPHVAL))	return new String[0]; 
		
		if (pmContent.charAt(0) == '(')	pmContent = pmContent.substring(1); 
		
		if (pmContent.contains(""+ALPH_DELIM))
			return pmContent.split(""+ALPH_DELIM);
		else return new String[] {pmContent};	
	}
	
	
	
	// -- ALPHA MUTATORS --
	/**
	 * given @param alphSymbs containing alpha symbols 
	 * @return an initialized HashMap for them before being set
	 */
	public HashMap<String, String> initAlphaStips (List<String> alphSymbs) 
	{
		HashMap<String, String> out = new HashMap<String, String>(); 
		for (String alphi : alphSymbs)	out.put(alphi, UNSET_ALPHVAL); 
		return out; 
	}
	
	/** 
	 * fill parenAlphaMap, given @prerequisite that @parenMap and @placeRestrs are already filled. 
	 * also sets @global @param @hasParenthesizedAlpha and @param @hasAlphSpecs to true if appropriate
	 * @param negProxies -- 
	 */
	public void initAlpha(HashMap<String,String> negProxies)
	{
		parenAlphaMap = new String[parenMap.length]; 
		
		int parenDepth = 0; 
		negProxyAlphas = new HashMap<String, String> (negProxies); 
		localAlphSpecs = new HashMap<String,String>(); 
		localAlphLocs = new HashMap<String,List<Integer>>(); 
		parenthesizedAlphas = new ArrayList<String>(); 
		
		for (int pmi = 0 ; pmi < parenMap.length; pmi++)
		{
			parenAlphaMap[pmi] = ""; 	//bc it inits as null in Java, annoyingly...
			if (parenMap[pmi].contains("("))
			{	parenDepth++; continue; 	}
			if (parenMap[pmi].contains(")"))
			{	parenDepth--;
				assert parenDepth > 0: "parenDepth fell below zero?! How did this happen?"; 
				continue;	}
			
			RestrictPhone pr = placeRestrs.get(Integer.parseInt(parenMap[pmi].substring(1))); 
			if (!pr.has_alpha_specs())	continue; 
			// if go past this point, there must be alph specs. 
			
			List<String> localAlphs = pr.getAlphaVars(); 
			
			if (parenDepth > 0) {
				parenAlphaMap[pmi] += "("; 
				for (String loc_alph_i : localAlphs)
					if (!parenthesizedAlphas.contains(loc_alph_i))	parenthesizedAlphas.add(loc_alph_i); 
			}
			
			parenAlphaMap[pmi] += String.join(ALPH_DELIM+"", localAlphs); 
			if (parenAlphaMap[pmi].charAt(parenAlphaMap[pmi].length()-1) == ALPH_DELIM)	parenAlphaMap[pmi] = parenAlphaMap[pmi].substring(0, parenAlphaMap[pmi].length()-1);  
			
			for (String lai : localAlphs)
			{
				if (!localAlphSpecs.containsKey(lai))
				{
					specifyLocalAlph(lai, UNSET_ALPHVAL, false);  // fills localAlphSpecs
					localAlphLocs.put(lai, Arrays.asList(pmi)); // TODO there might be a data type issue here? 
				}
				else
				{
					List<Integer> updatedLocs = new ArrayList<Integer>(localAlphLocs.get(lai)); 
					updatedLocs.add(pmi); 
					localAlphLocs.put(lai,updatedLocs); 
				}
			}
		}
	}	
	
	public void applyAlphaValues(HashMap<String, String> alphVals)
	{
		if (!hasAlphaSpecs())	return; 
		for (String alph: alphVals.keySet()) {
			specifyLocalAlph(alph, alphVals.get(alph)); 
		}
		
		//the below should be trivial, but uncomment as bandaid if errors of lack of coverage arise if need quick fix
		//for (int pri = 0 ; pri < placeRestrs.size(); pri++)	placeRestrs.get(pri).applyAlphaValues(alphVals);
	}
	
	// not (yet at least?) making this dependent on setAlphaValue() bc it is faster to reset all alpha values at each placeRestr as done in here. 
	public void resetAllAlphaValues()
	{
		for (String alph_i : localAlphSpecs.keySet())
			specifyLocalAlph(alph_i, UNSET_ALPHVAL, false);  
		
		for (int pri = 0 ; pri < placeRestrs.size() ; pri++)	
			placeRestrs.get(pri).resetAlphaValues(); 
	}
	
	public void resetTheseAlphaValues(List<String> toReset) {		
		for (String reseti: toReset)
			specifyLocalAlph(reseti, UNSET_ALPHVAL, true); 
	}
	
	/**
	 * centralize handling (for efficiency of debugging, etc.) of setting an alpha value in localAlphSpecs 
	 * @param alph -- alpha value that will be reset
	 * @param newVal -- new setting  (0/1/2/9) 
	 * @modifies @global @localAlphSpecs
	 * handles local neg alpha proxy policy within 
	 * 		- automatically adds any alpha proxies! 
	 * 			@precondition -- @global @negProxyAlphas has been filled already (!!)
				automatically polarizes values as necessary
					manually within here
					and via FeatMatrix methods within placeRestrs. 
				but does not force them upon other structures in getting or setting, functionally or internally. 
	 	TODO NOTE -- WILL despecify via alpha(!!) 
	 * neg alpha proxy coverage in placeRestrs as applicable is handled in FeatMatrix methods. 
	 	
	 */
	public void specifyLocalAlph(String alph, String newVal)	{	specifyLocalAlph(alph, newVal, true); 	}
	public void specifyLocalAlph(String alph, String newVal, boolean modifyPlaceRestrs)
	{
		UTILS.abortIllegalAlpha(alph);
		
		putLocalAlph(alph, newVal, modifyPlaceRestrs); 
		
		// alpha proxy policy implementation: 
		specifyAlphViaNegProxy(alph, newVal, modifyPlaceRestrs); 
	}
	
	// auxiliary for specifyLocalAlph, specifyAlphViaNegProxy
	private void putLocalAlph(String a, String nv, boolean modifyPRs)
	{
		boolean resetting = nv.equals(UNSET_ALPHVAL); 
		if (!resetting) UTILS.abortInvalidFtIntStr(nv); 
		
		localAlphSpecs.put(a, nv); 
	
		// if not modifying placeRestrs structure, end here. 
		if (!modifyPRs)	return; 
		
		for (int pri:  getPlaceRestrLocsWithAlpha(a))
		{
			if (resetting) placeRestrs.get(pri).resetAlphVal(a.charAt(0));
			else placeRestrs.get(pri).setAlphaValue(a, nv); 
		}
	}
	
	/**
	 * @modify @global @localAlphSpecs appropriately via a neg proxy/proxied alpha symbol 
	 * @param prAlph -- possibly proxy or proxied [pair] alpha 
	 * 			if it has no proxy pair, then @donothing (no error)
	 * @param prVal -- value -- which will be polarized if it is polar
	 * @param modifyPlaceRestrs -- if placeRestrs will be modified
	 * @precondition @global @negProxyAlphas @initialized
	 */
	public void specifyAlphViaNegProxy(String prAlph, String prVal, boolean modifyPlaceRestrs)
	{
		String targAlph = getProxyPair(prAlph); 
		if (targAlph.equals(UTILS.NULL_PROXY_PAIR))	return; 
		/*else*/ 
		String targVal = prVal.equals(UNSET_ALPHVAL) ? UNSET_ALPHVAL : ""+UTILS.getOppFtInt(prVal);
		putLocalAlph(targAlph, targVal, modifyPlaceRestrs); 
		
		//doing the following to avoid null reference errors in localAlphLocs...
		if (!localAlphLocs.containsKey(targAlph))
			localAlphLocs.put(targAlph, new ArrayList<Integer>());
	}

}
