import java.util.List;
import java.util.ArrayList;
import java.util.HashMap; 

// to invoke null target (i.e. insertion), use empty targSegList

public class ShiftFeatToPhoneByPhone extends ShiftByPhone 
{
	//TODO note that, if pseudosMatter is not set to true
	//... morpheme boundaries may end up deleted by this class 
	//for an insertion shift using this class, enter an empty list as the target
	private List<RestrictPhone> target; 
	private List<SequentialPhonic> destination; 
	
	private void initialize(HashMap<String,Integer> ftInds, List<String> targSpecList, List<SequentialPhonic> dest)
	{
		target = new ArrayList<RestrictPhone>(); 
		for (String targSpecs : targSpecList)
			target.add(new CandRestrictPhone(targSpecs, ftInds)); 
		minTargSize = target.size(); //in effect may be larger bc of pseudophones if pseudosMatter==false
		destination = dest; 
	}
	
	public ShiftFeatToPhoneByPhone(HashMap<String,Integer> ftInds, List<String> targSpecs, List<SequentialPhonic> dest)
	{	super(); initialize(ftInds, targSpecs, dest); 	}
	
	public ShiftFeatToPhoneByPhone(HashMap<String,Integer> ftInds, List<String> targSpecs, List<SequentialPhonic> dest, boolean attendToPseudos)
	{	super(attendToPseudos); initialize(ftInds, targSpecs, dest); 	}
	
	public ShiftFeatToPhoneByPhone(HashMap<String,Integer> ftInds, List<String> targSpecs, List<SequentialPhonic> dest, 
			List<List<SequentialPhonic>> priors, List<List<SequentialPhonic>> postrs)
	{	super(priors,postrs); initialize(ftInds, targSpecs, dest); 	}
	
	public ShiftFeatToPhoneByPhone(HashMap<String,Integer> ftInds, List<String> targSpecs, List<SequentialPhonic> dest, 
			boolean attendToPseudos,List<List<SequentialPhonic>> priors, List<List<SequentialPhonic>> postrs)
	{	super(priors,postrs,attendToPseudos); initialize(ftInds, targSpecs, dest); 	}
	
	//Realization
	public List<SequentialPhonic> realize(List<SequentialPhonic> input)
	{
		int inpSize = input.size(), destSize = destination.size(); 	//destination size is constant in this class, not so in others
		//abort if too small 
		if(inpSize < minPriorSize + minTargSize + minPostSize)	return input; 
		
		List<SequentialPhonic> res = new ArrayList<SequentialPhonic>(input); 
		int p = minPriorSize;
		
		/** check if target with correct context occurs at each index
		 * We iterate from the beginning to the end of the word. 
		 * We acknowledge the decision to iterate this way (progressive) is arbitrary, and that it may not be technically correct as some changes happen regressively. 
		 * However it is most convenient for the time being.
		 */ 
		while (p< inpSize - minPostSize + 1 - minTargSize)
		{
			if(priorMatch(input,p))
			{
				//firstIndAfterMatchedWindow also determines whether the posterior context is true
				int possNextInd = firstIndAfterMatchedWindow(input,p); 
				if(possNextInd != -1)
				{
					//begin mutation
					List<SequentialPhonic> fjale = new ArrayList<SequentialPhonic>(res.subList(0,p));
					fjale.addAll(destination);
					fjale.addAll(res.subList(possNextInd, inpSize));
					inpSize = fjale.size(); 
					p += destSize; 
				}
				else	p++; 
			}
			else	p++; 
		}
		
		return res; 
	}
	
	
	/** firstIndAfterMatchedWindow 
	 * @param input -- the word being tested
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
		// ... the constant target size. 
		int inpSize = input.size();
		//abort if starting index is obviously invalid:
		if(firstInd + minTargSize + minPostSize - 1 > inpSize || firstInd < 0)	return -1;
		
		//check if the window is matching the target requirements
		int checkInd = firstInd, targInd = 0; 
		while(targInd < minTargSize && checkInd < inpSize) 
		{
			SequentialPhonic currObs = input.get(checkInd); 
			//if pseudo phone and pseudos dont' matter, simply ignore it.
			if(!currObs.getType().equals("phone") && !pseudosMatter)
				checkInd++; 
			else if(!target.get(targInd).compare(currObs))
				return -1; 
			else	
			{	checkInd++; targInd++;	}
		}
		if(targInd == minTargSize)
			return (posteriorMatch(input,checkInd)) ? checkInd : -1; 
		else	return -1; 
	}
}
