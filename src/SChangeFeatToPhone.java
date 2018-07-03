import java.util.List; 
import java.util.ArrayList;
import java.util.HashMap; 

public class SChangeFeatToPhone extends SChange{
	
	private List<RestrictPhone> targSource; 
	private List<Phone> destination; 
	
	private void initialize(HashMap<String, Integer> ftInds, List<RestrictPhone> sourcePlaces, List<Phone> dest)
	{
		targSource = new ArrayList<RestrictPhone>(sourcePlaces); 
		minTargSize = targSource.size(); 
		destination = dest; 
	}
	
	public SChangeFeatToPhone(HashMap<String, Integer> ftInds, List<RestrictPhone> targSpecs, List<Phone> dest)
	{	super(); initialize(ftInds, targSpecs, dest); }
	
	public SChangeFeatToPhone(HashMap<String, Integer> ftInds, List<RestrictPhone> targSpecs, List<Phone> dest, 
			boolean bm)
	{
		super(bm); initialize(ftInds, targSpecs, dest); 
	}

	public SChangeFeatToPhone(HashMap<String, Integer> ftInds, List<RestrictPhone> targSpecs, List<Phone> dest,
			ShiftContext prior, ShiftContext postr)
	{	super(prior,postr); initialize(ftInds, targSpecs, dest); }
	
	public SChangeFeatToPhone(HashMap<String, Integer> ftInds, List<RestrictPhone> targSpecs, List<Phone> dest,
			boolean bm, ShiftContext prior, ShiftContext postr)
	{	super(prior, postr, bm); initialize(ftInds, targSpecs, dest);	}
	
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
			//if it's a boundary and bounds don't matter, simply ignore it 
			if(!currObs.getType().equals("phone") && !boundsMatter)
				checkInd++; 
			else if(!targSource.get(targInd).compare(currObs)) 
				return -1; 
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
