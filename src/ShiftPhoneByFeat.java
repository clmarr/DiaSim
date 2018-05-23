import java.util.List;
import java.util.ArrayList; 

public class ShiftPhoneByFeat extends ShiftByFeat {
	private List<List<SequentialPhonic>> targets, destinations;

	//auxiliary for contructors
	private void initialize(List<List<SequentialPhonic>> targs, List<List<SequentialPhonic>> dests)
	{
		assert (targs.size() == dests.size() || targs.size() == 0 || dests.size() == 0) : 
			"Precondition violated: number of targs does not match number of dests and neither is empty"; 
		targets = new ArrayList<List<SequentialPhonic>>(targs);
		destinations = new ArrayList<List<SequentialPhonic>>(dests); 
		minTargSize = targets.get(0).size(); int i = 1; 
		while(i < targets.size())
		{	minTargSize = Math.min(minTargSize, targets.get(i).size()); i++; 	}
	}
	
	public void initializeWithFeats(List<List<SequentialPhonic>> targs, List<RestrictPhone> mutations)
	{
		destinations = new ArrayList<List<SequentialPhonic>>(); 
		for(List<SequentialPhonic> targ: targs)
		{
			int targSize = targ.size(); 
			assert targSize == mutations.size(): "Error: size mismatch when making dests from mutations on targets";
			List<SequentialPhonic> newDest = new ArrayList<SequentialPhonic>(); 
			
			for(int mi = 0; mi < targSize; mi++)
				newDest.add(mutations.get(mi).forceTruth(targ, mi).get(mi));
			destinations.add(newDest); 
		}
	}
	
	//Constructors
	//default contextless constructor
	public ShiftPhoneByFeat(List<List<SequentialPhonic>> targs, List<List<SequentialPhonic>> dests)
	{	super();	initialize(targs, dests);	}
	
	//contextless constructor with pseudosMatter specified
	public ShiftPhoneByFeat(List<List<SequentialPhonic>> targs, List<List<SequentialPhonic>> dests, boolean attendToPseudos)
	{	super(attendToPseudos); initialize(targs, dests); 	}
	
	//default constructor with context
	public ShiftPhoneByFeat(List<List<SequentialPhonic>> targs, List<List<SequentialPhonic>> dests, 
			List<RestrictPhone> prior, List<RestrictPhone> postr)
	{	super(prior,postr); initialize(targs,dests);	}
	
	public ShiftPhoneByFeat(List<List<SequentialPhonic>> targs, List<List<SequentialPhonic>> dests, 
			List<RestrictPhone> prior, List<RestrictPhone> postr, boolean attendToPseudos)
	{	super(prior,postr,attendToPseudos); initialize(targs,dests);	}
	
	//constructor for Phone > Feat by Phone, translated to standard format with only Phones (SequentialPhonic) for storage
	//we assume that since target input is SequentialPhonic, only reasonable uses for this class are 1-to-1, 
	//... with the latter 1 possibly including a null phone
	// i.e. that's really the only reasonable way to use this constructor
	public ShiftPhoneByFeat(List<List<SequentialPhonic>> targs, ArrayList<RestrictPhone> mutations)
	{	super(); initializeWithFeats(targs,mutations); 	}
	
	public ShiftPhoneByFeat(List<List<SequentialPhonic>> targs, ArrayList<RestrictPhone> mutations, boolean attendToPseudos)
	{	super(attendToPseudos); initializeWithFeats(targs, mutations); }
	
	public ShiftPhoneByFeat(List<List<SequentialPhonic>> targs, ArrayList<RestrictPhone> mutations, 
			List<RestrictPhone> prior, List<RestrictPhone> postr)
	{	super(prior,postr); initializeWithFeats(targs, mutations); }
		
	public ShiftPhoneByFeat(List<List<SequentialPhonic>> targs, ArrayList<RestrictPhone> mutations,  
			List<RestrictPhone> priors, List<RestrictPhone> postrs, boolean attendToPseudos)
	{	super(priors,postrs,attendToPseudos); initializeWithFeats(targs, mutations); 	}
	
	//Realization
	public List<SequentialPhonic> realize(List<SequentialPhonic> input)
	{
		int inpSize = input.size();
		//abort if too small
		if (inpSize < minPriorSize + minTargSize + minPostSize)	return input; 
		
		List<SequentialPhonic> res = new ArrayList<SequentialPhonic>(input); 
		int p = minPriorSize; 
		
		/** check if target with correct context occurs at each index. 
		 * We iterate from the beginning to the end of the word. 
		 * We acknowledge the decision to iterate this way (progressive) is arbitrary, and that it may not be technically correct as some changes happen regressively. 
		 * However it is most convenient for the time being.
		 */ 
		while ( p < inpSize - minPostSize + 1 - minTargSize)
		{
			if (priorMatch(res, p ))
			{
				int matchInd = whichMatch(res, p); 
				if (matchInd != -1) // i.e. it exists
				{
					int indAfter = foundTargetLastIndex(res, targets.get(matchInd), p);
					
					//begin mutation 
					List<SequentialPhonic> fjale = new ArrayList<SequentialPhonic>(res.subList(0, p)); 
					if(pseudosMatter)	fjale.addAll(destinations.get(matchInd)); 
					else
					{	//TODO admittedly may need to fix this somewhat clumsy way of preserving the Pseudos sometime
						List<SequentialPhonic> toAdd = new ArrayList<SequentialPhonic>(destinations.get(matchInd)); 
						int lenPrePseudos = toAdd.size(); 
						//(clumsily) add the pseudo-phones to this 
						for (int cwInd = p; cwInd < indAfter; cwInd++)
						{
							SequentialPhonic currUnit = res.get(cwInd); 
							if(!currUnit.getType().equals("phone"))
							{
								if (cwInd - p < lenPrePseudos)	toAdd.add(cwInd - p, currUnit); 
								else	toAdd.add(currUnit); 
							}
						}
						fjale.addAll(toAdd); 
					}
					p = fjale.size(); //to increment p 
					fjale.addAll(res.subList(indAfter, res.size()));
					res = new ArrayList<SequentialPhonic>(fjale); 
				}
				else	p++; 
			}
			else 	p++;
		}
		return res; 
	}
	
	/** method WHICHMATCH
	 * @param input -- phonological representation of hte input 
	 * @param ind -- index for which we are checking if one of our target segments starts at 
	 * @return index of hte targ segment, in our list of targets, that starts at this index; 
	 * 	-1 if none of target segments match (most common scenario) 
	 * @precondition : priorMatch(input, ind) == true
	 * @precondition: only one targ segment starts at the index ind.
	 * 		This should be true if no targ segment is contained by another as a sublist. 
	 */
	public int whichMatch (List<SequentialPhonic> input, int ind)
	{
		// check each of the possible targets. We arbitrarily go by the order in which they are placed in the targets list.
		for(int it=0; it < targets.size(); it++)
		{	if(isMatch(input, targets.get(it), ind)) 	return it;	}
		
		//if we have reached this point, none of the potential targets have been found to match. 
		return -1; 
			
	}
	
	/**
	 * method isMatch
	 * @param input -- phonological representation of the input
	 * @param targSeg -- which of our target segments we are checking for
	 * @param ind -- start index in the input word for the target segment for which we are checking
	 * @precondition : priorMatch(inpWord, ind) == true
	 * @return ture iff a proper target lies at this index and its posterior context regs are flfilled 
	 */
	public boolean isMatch(List<SequentialPhonic> input, List<SequentialPhonic> targSeg, int ind)
	{
		int inpSize = input.size(), targSize = targSeg.size(); 
		//return flase in case of invalid index
		if(ind + targSize + minPostSize >= inpSize || ind < 0 )	return false; 
		
		//also we return false if the target is not found in the specified place
		//and while we're at it, we fix indAfter to incorporate any pseudophones using the auxiliary method foundTargetLastIndex
		int indAfter = foundTargetLastIndex (input, targSeg, ind) ; 
		
		// if indAfter is 0, now it means it was originally -1 -- hence it was not found
		if (indAfter == 0)	return false; 
		
		//now we know the prior context and target requirements are both met 
		//we now check for the posterior 
		if (minPostSize == 0 )	return true; //i.e. there is no posterior context requirement necessary -- so can skip the following 
		if (minPostSize > inpSize - indAfter)	return false; 
		return posteriorMatch(input, indAfter); 
	}
	
	/** method foundTargetLastIndex
	 * checks if @param targSeg is equal, or operationally equal 
	 * to a window starting at @param firstInd within @param input
	 * @return -1 if not 
	 * else @return the index after the last index of the matching window
	 * behavior changes depending on the value of the boolean param pseudosMatter
	 * if true than PseudoPHones will be compared just like other phones to phones at hte same index in the target
	 * if false then they will be passed over as the counter continues to increment for the ultimate return of the last index 
	 */
	public int foundTargetLastIndex(List<SequentialPhonic> input, List<SequentialPhonic> targSeg, int firstInd)
	{
		int targSize = targSeg.size(); 
		
		//if target is empty i.e. null (i.e. we have an insertion shift), then return its same index
		if (targSize == 0)	return firstInd;
		
		//abortively return false (-1) if size of input, targSeg and minPostSize make any match impossible
		if(input.size() - minPostSize - firstInd < targSize)	return -1;
		
		int targInd = 0, wdInd = firstInd; //the index in target we will be comparing to, 
			// and the corresponding index of comparison in the word 
		
		//check equality to each index in target
		while (targInd < targSize)
		{
			//if it's a pseudophone and pseudosMatter is false, pass it
			if(!input.get(wdInd).getType().equals("phone") && !pseudosMatter)	wdInd++; 
			else
			{
				//if they are unequal at the current index, return false (-1)
				if(!targSeg.get(targInd).equals(input.get(wdInd)))	return -1; 
				//otherwise continue counting
				targInd++; wdInd++; 
			}
		}
		//if we reached this point, we have found the last index -- or rather the one after it. 
		return wdInd;
	}
	
	@Override
	//@precondition: targets.size() == destinations.size() or one of them is empty (i.e. representing either insertion or deletion)
	public String toString()
	{
		assert (targets.size() == 0 || destinations.size() == 0 || targets.size() == destinations.size()) :
			"fatal error in toString: diff number destinations than targets";
		int numTargets = targets.size(); 
		String output= ""; 
		if (numTargets == 0)	output += "∅"; 
		else
		{
			if (numTargets > 1)	output += "{"; 

			output += printSegment(targets.get(0)); 
			
			if( numTargets > 1)
				for(int ti = 1; ti < numTargets; ti++)
					output += ", "+ printSegment(targets.get(ti));
			
			output += "}";
		}
		
		output += " > "; 

		int numDests = destinations.size(); 
		if(numDests == 0)	output += "∅";
		else
		{
			if(numDests > 1 )	output += "{"; 
			output += printSegment(destinations.get(0));
			if (numDests > 1)
				for (int ti = 1; ti < numDests; ti++)
					output += ", "+printSegment(destinations.get(ti)); 
			output += "}";
		}
		
		return output + super.toString();
	}
	
	//auxiliary method for toString 
	private String printSegment(List<SequentialPhonic> seg)
	{
		String output = ""; 
		if (seg.size() == 0)	output += "∅"; 
		else 
		{	output += "/"; 
			for (SequentialPhonic curPh : seg)	output += curPh.print(); 
			output += "/";
		}
		return output;
	}	
}
