import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class ShiftFeatByPhone extends ShiftByPhone{
	private RestrictPhone target;
	private RestrictPhone destination;
	
	public void initialize(HashMap<String,Integer> ftInds, String targSpecs, String destSpecs)
	{
		if(!targSpecs.equals(""))
		{
			target = new CandRestrictPhone(targSpecs, ftInds);
			minTargSize = 1; 
		}
		else
		{	assert !destSpecs.equals(""):"Error: both target and destination are null!";
			target = new NullPhone(); 
			minTargSize = 0;
		}
		if(!destSpecs.equals(""))	destination = new CandRestrictPhone(destSpecs, ftInds);
		else	destination = new NullPhone(); 
	}

	// Constructors follow: 
	public ShiftFeatByPhone(HashMap<String,Integer> ftInds, String targSpecs, String destSpecs)
	{	super(); initialize(ftInds, targSpecs, destSpecs); 	}
	
	public ShiftFeatByPhone(HashMap<String,Integer> ftInds, String targSpecs, String destSpecs, boolean attendToPseudos)
	{	super(attendToPseudos); initialize(ftInds, targSpecs, destSpecs); 	}
	
	public ShiftFeatByPhone(HashMap<String,Integer> ftInds, String targSpecs, String destSpecs, 
			List<List<SequentialPhonic>> priors, List<List<SequentialPhonic>> postrs)
	{	super(priors,postrs); initialize(ftInds, targSpecs, destSpecs); 	}
	
	public ShiftFeatByPhone(HashMap<String,Integer> ftInds, String targSpecs, String destSpecs, 
			boolean attendToPseudos,List<List<SequentialPhonic>> priors, List<List<SequentialPhonic>> postrs)
	{	super(priors,postrs,attendToPseudos); initialize(ftInds, targSpecs, destSpecs); 	}
	
	// Realization 
	public List<SequentialPhonic> realize(List<SequentialPhonic> input)
	{
		int inpSize = input.size(); 
		//abort if too small
		if (inpSize < minPriorSize + minTargSize + minPostSize)	return input; 
		
		List<SequentialPhonic> res = new ArrayList<SequentialPhonic>(input); 
		int p = minPriorSize; 
				
		/** check if target with correct context occurs at each index
		 * We iterate from the beginning to the end of the word. 
		 * We acknowledge the decision to iterate this way (progressive) is arbitrary, and that it may not be technically correct as some changes happen regressively. 
		 * However it is most convenient for the time being.
		 */ 
		while(p < inpSize - minPostSize + 1 - minTargSize)
		{
			if(priorMatch(input, p))
			{
				if(isMatch(input, p)) //test if both target and posterior specs met
				{
					//mutate
					res = destination.forceTruth(res, p); 
					p++; // even when destination is null, we increase one so that we 
						// don't get an error whereby the prior context affects a phone
						// that it was not originally in contact with. 
						// note that this method will itself cause a much rarer error: 
						// that if the deleted segment was ITSELF a valid prior context for the shift
						//, this info will be lost -- however this situation is extremely rare if it 
						// .... ever occurs at all. 
					while(!res.get(p).getType().equals("phone") 
							&& p < inpSize - minPostSize + 1 - minTargSize)	
						p++; 
				}
				else	p++;
			}
			else	p++;
		}
		return res; 
	}
	 
	/** isMatch
	 * @param input -- phonological representation of the input
	 * @param ind -- index we are looking at 
	 * @precondition : priorMatch(input, ind) == true
	 * @return true iff a legal target lies at this index and its posterior context reqs are also fulfilled
	 */
	public boolean isMatch(List<SequentialPhonic> input, int ind)
	{
		//there is only one target, so within this method, minTargSize just equals 
			// ... the constant target size. 
		int inpSize = input.size(); 
		//abort if index is obviously invalid
		if(ind + minTargSize + minPostSize - 1 > inpSize || ind < 0)	return false; 
		
		//return false if phone at specified place does not match restrictions on target
		if(!target.compare(input, ind))	return false;
		
		//now we know the prior context and target requirements are both met
		// we now check for the posterior context
		if(minPostSize == 0)	return true; 
		return posteriorMatch(input, ind+minTargSize); 
	}
	
	public String toString()
	{
		String output=""; 
		if (minTargSize == 0)	output += "∅";
		else //in this case we know target is a CandRestrictPhone
			output+=target.toString(); 
		
		output += " > ";
		
		if(destination.toString().equals("null phone"))
			output += "∅";
		else	output += destination.toString(); 
		
		return output + super.toString(); 
	}

}
