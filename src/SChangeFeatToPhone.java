import java.util.List; 
import java.util.ArrayList;
import java.util.HashMap; 

//TODO Note: is assumed when using this class, that bounds matter
// this is necessary for pragmatic purposes. 
public class SChangeFeatToPhone extends SChange{
	
	protected List<RestrictPhone> targSource; 
	protected List<Phone> destination; 
	
	protected void initialize(HashMap<String, Integer> ftInds, List<RestrictPhone> sourcePlaces, List<Phone> dest)
	{
		for(RestrictPhone sourcePlace : sourcePlaces)	
			if ("∅@".contains(sourcePlace.print()))
				throw new Error("Error: cannot enter non-word-boundaries (@) or null phones (∅) into a SChangeFeatToPhone!");
		
		targSource = new ArrayList<RestrictPhone>(sourcePlaces); 
		minTargSize = targSource.size(); 
		destination = dest; 
	}
	
	public SChangeFeatToPhone(HashMap<String, Integer> ftInds, List<RestrictPhone> targSpecs, List<Phone> dest, String origForm)
	{	super(true, origForm); initialize(ftInds, targSpecs, dest); 	}

	public SChangeFeatToPhone(HashMap<String, Integer> ftInds, List<RestrictPhone> targSpecs, List<Phone> dest,
			SequentialFilter prior, SequentialFilter postr, String origForm)
	{	super(prior,postr, true, origForm); initialize(ftInds, targSpecs, dest); }
	
	//Realization
	public List<SequentialPhonic> realize (List<SequentialPhonic> input)
	{
		int inpSize = input.size(); 
		//abort if too small
		if(inpSize < minPriorSize + minTargSize + minPostSize)	return input; 
		
		int p = minPriorSize , 
				maxPlace = inpSize - Math.max(minPostSize + minTargSize, 1); 
		List<SequentialPhonic> res = (p == 0) ? 
				new ArrayList<SequentialPhonic>() : new ArrayList<SequentialPhonic>(input.subList(0, p));
		

		/** check if target with correct context occurs at each index 
		 * We iterate from the beginning to the end of the word
		 * We acknowledge the decision to iterate this way (progressively) is arbitrary,
		 * and that it might not be technically correct for a restricted set of cases (some changes may occur "regressively" within the word)
		 * change if it becomes necessary
		 */
		while (p <= maxPlace)
		{
			if (priorMatch(input, p))
			{
				//note that firstIndAfterMatchedWindow 
				//determines both if we have a window match and a posterior match
				// if it is not a match, retruns -1
				int candIndAfter = firstIndAfterMatchedWindow(input, p);
				if(candIndAfter != -1)
				{	res.addAll(destination); 
					p = candIndAfter; 
				}
				else
				{
					res.add(input.get(p)); p++; 
				}
			}
			else	{	res.add(input.get(p)); p++; }
		}
		if (p < inpSize)
			res.addAll(input.subList(p, inpSize)); 
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
		if(firstInd >= inpSize)
			throw new Error("Error: tried to match with firstInd after word ends!");
		
		int checkInd = firstInd, targInd = 0; 
		while (targInd < minTargSize && checkInd < inpSize ) 
		{
			SequentialPhonic currObs = input.get(checkInd); 
			if(!targSource.get(targInd).compare(currObs)) 	return -1; 
			else
			{	checkInd++; targInd++; 	}
		}
		if(targInd == minTargSize)
			return (posteriorMatch(input, checkInd) ? checkInd : -1);
		else	return -1; 	
	}
	
	public String toString()
	{
		String output = "";
		for (RestrictPhone targRPh : targSource)
			output += (targRPh.getClass().toString().contains("FeatMatrix")) ? targRPh+" " : targRPh.print() + " ";
		output += "> "; 
		
		if(destination.isEmpty())	output += "∅"; 
		else
			for (Phone destPh : destination)
				output += destPh.print() + " "; 
		return output.trim() + super.toString();
	}
	
}
