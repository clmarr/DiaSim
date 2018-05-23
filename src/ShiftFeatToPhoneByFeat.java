import java.util.List;
import java.util.ArrayList;
import java.util.HashMap; 

// to invoke null target (i.e. insertion), use empty targSegList

public class ShiftFeatToPhoneByFeat extends ShiftByFeat {
	//TODO note that, if pseudosMatter is not set to true
	//... morpheme boundaries may end up deleted by this class 
	//for an insertion shift using this class, enter an empty list as the target
	
	private List<RestrictPhone> target; 
	private List<SequentialPhonic> destination; 
	
	private void initialize(HashMap<String, Integer> ftInds, List<String> targSpecList, List<SequentialPhonic> dest)
	{
		target = new ArrayList<RestrictPhone>(); 
		for(String targSpecs : targSpecList)
			target.add(new CandRestrictPhone(targSpecs, ftInds)); 
		minTargSize = target.size(); 
		destination = dest;
	}
	
	public ShiftFeatToPhoneByFeat(HashMap<String, Integer> ftInds, List<String> targSpecs, List<SequentialPhonic> dest)
	{	super(); initialize(ftInds, targSpecs, dest); }
	
	public ShiftFeatToPhoneByFeat(HashMap<String, Integer> ftInds, List<String> targSpecs, List<SequentialPhonic> dest,
			List<RestrictPhone> prior, List<RestrictPhone> postr)
	{	super(prior,postr); initialize(ftInds, targSpecs, dest); }
		
	public ShiftFeatToPhoneByFeat(HashMap<String, Integer> ftInds, List<String> targSpecs, List<SequentialPhonic> dest,
			boolean attendToPseudos, List<RestrictPhone> prior, List<RestrictPhone> postr)
	{	super(prior, postr, attendToPseudos); initialize(ftInds, targSpecs, dest);	}
		
	//Realization
	public List<SequentialPhonic> realize (List<SequentialPhonic> input)
	{
		int inpSize = input.size(), destSize = destination.size(); //destination size is constant in this class, unlike the others
		//abort function if too small
		if (inpSize < minPriorSize + minTargSize + minPostSize)	return input; 
		
		List<SequentialPhonic> res = new ArrayList<SequentialPhonic>(input); 
		int p = minPriorSize; 
		
		/** check if target with correct context occurs at each index 
		 * We iterate from the beginning to the end of the word
		 * We acknowledge the decision to iterate this way (progressively) is arbitrary,
		 * and that it might not be technically correct for a restricted set of cases (some changes may occur "regressively" within the word)
		 * change if it becomes necessary
		 */
		
		while (p < inpSize - minPostSize + 1 - minTargSize)
		{
			if(priorMatch(input, p))
			{
				//foundMatchLastIndex also determines whether the posterior context is true
				int possNextInd = firstIndAfterMatchedWindow(input, p); //gets the index after the last matched index in the target window 
				if (possNextInd != -1)
				{
					//begin mutation 
					List<SequentialPhonic> fjale = new ArrayList<SequentialPhonic>(res.subList(0,p)); 
					fjale.addAll(destination);
					fjale.addAll(res.subList(possNextInd, inpSize)); 
					inpSize = fjale.size(); 
					p += destSize; 
				}
				else p++; 
			}
			else p++;
		}
		return res; 
	} 
	
	/** firstIndAfterMatchedWindow
	 * @param input -- word being tested 
	 * @param ind -- starting index being tested 
	 * @precondition : priorMatch(input, ind) == true 
	 * @return -1 if the target and proper posterior context are not found at this place 
	 * @return the first index after the final index of the relevant window otherwise
	 * -- this will very depending on the value of pseudosMatter
	 * interacting with whether there are pseudoPhones within the relevant/corresponding window
	 */
	public int firstIndAfterMatchedWindow(List<SequentialPhonic> input, int firstInd)
	{
		//there is only one target, so within this method, minTargSize just equals 
		// ... the constant targ size. 
		int inpSize = input.size();
		//abort if the starting index is obviously invalid: 
		int checkInd = firstInd, targInd = 0; 
		while (targInd < minTargSize && checkInd < inpSize ) 
		{
			SequentialPhonic currObs = input.get(checkInd); 
			//if it's a pseudo phone and pseudos don't matter, simply ignore it 
			if(!currObs.getType().equals("phone") && !pseudosMatter)
				checkInd++; 
			else if(!target.get(targInd).compare(currObs)) 
				return -1; 
			else
			{	checkInd++; targInd++; 	}
		}
		if(targInd == minTargSize)
			return (posteriorMatch(input, checkInd) ? checkInd : -1);
		else	return -1; 	
	}
	
	
}
