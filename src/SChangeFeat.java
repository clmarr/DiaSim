import java.util.ArrayList;
import java.util.HashMap;
import java.util.List; 

public class SChangeFeat extends SChange {
	private RestrictPhone targSource, destination; 
	
	//auxiliary for constructors
	public void initialize(HashMap<String,Integer> ftInds, String targSpecs, String destSpecs)
	{
		if(!targSpecs.equals("") && !targSpecs.equals("∅"))
		{
			targSource = new FeatMatrix(targSpecs, ftInds); 
			minTargSize = 1; 
		}
		else //i.e. we know source-targ is null if this is reachedpint. 
			throw new Error("Insertion is not allowed for SChangeFeats -- please use an SChangePhone instead."); 
		if(!destSpecs.equals("") && !destSpecs.equals("∅"))
		{	
			destination = new FeatMatrix(destSpecs, ftInds);
		}
		else	destination = new NullPhone(); 
	}
	
	public void initialize(RestrictPhone source, RestrictPhone dest) 
	{
		if(source.getClass().toString().contains("NullPhone"))	minTargSize = 0; 
		else	minTargSize = 1; 
		targSource = source; 
		
		if(dest.getClass().toString().contains("NullPhone"))	
			assert minTargSize > 0 : "Error: both target and destination are null!";  
		destination = dest; 
	}
	
	//constructors follow
	public SChangeFeat(HashMap<String,Integer> ftInds, String targSpecs, String destSpecs)
	{	super(); initialize(ftInds, targSpecs, destSpecs); 	}
	
	public SChangeFeat(HashMap<String,Integer> ftInds, String targSpecs, String destSpecs, boolean bm)
	{
		super(bm); initialize(ftInds, targSpecs, destSpecs); 
	}
	
	public SChangeFeat(HashMap<String,Integer> ftInds, String targSpecs, String destSpecs, 
			SChangeContext priors, SChangeContext postrs)
	{	super(priors,postrs); initialize(ftInds, targSpecs, destSpecs); 	}
	
	public SChangeFeat(HashMap<String,Integer> ftInds, String targSpecs, String destSpecs, 
			boolean bm, SChangeContext priorContxt, SChangeContext postContxt)
	{	super(priorContxt,postContxt,bm); initialize(ftInds, targSpecs, destSpecs); 	}
	
	public SChangeFeat(RestrictPhone source, RestrictPhone dest)
	{	super(); initialize(source,dest); 	}
	
	public SChangeFeat(RestrictPhone source, RestrictPhone dest, boolean bm)
	{	super(bm); initialize(source,dest); 	}
	
	public SChangeFeat(RestrictPhone source, RestrictPhone dest, SChangeContext priorContxt, SChangeContext postContxt)
	{	super(priorContxt, postContxt); initialize(source, dest);	}
	
	public SChangeFeat(RestrictPhone source, RestrictPhone dest, SChangeContext priorContxt, SChangeContext postContxt, boolean bm)
	{	super(priorContxt, postContxt, bm); initialize(source, dest);	}
	
	//Realization
	@Override
	public List<SequentialPhonic> realize(List<SequentialPhonic> input)
	{
		//abort if too small
		if (input.size() < minPriorSize + minTargSize + minPostSize)	return input; 
		
		List<SequentialPhonic> res = new ArrayList<SequentialPhonic>(input.subList(0, minPriorSize)); 
		int p = minPriorSize; 
		int maxPlace = input.size() - minPostSize - minTargSize; 
		/** check if target with correct context occurs at each index
		 * We iterate from the beginning to the end of the word. 
		 * We acknowledge the decision to iterate this way (progressive) is arbitrary, and that it may not be technically correct as some changes happen regressively. 
		 * However it is most convenient for the time being.
		 */
		while(p <= maxPlace)
		{
			if(!boundsMatter)
			{
				boolean stopIncrement = (p >= input.size()); 
				while(!stopIncrement)
				{
					if(p >= input.size())	stopIncrement = true; 
					else if(input.get(p).getType().equals("bound") && p <= maxPlace)
						p++; 
					else	stopIncrement = true; 
				}
			}
			if(priorMatch(input, p))
			{
				if(isMatch(input, p))	//test if both target and posterior specs are met
				{
					// when destination is null, we add nothing,
					// and increment p TWICE
					// this is done to block a segment that is deleted itself causing the deletion of the following unit
					// note that this will itself cause rare erros if the that averted situation was actually the intention
					// however it is assumed that this would be incredibly rare. if ever occuring at all. 
					if (destination.print().equals("∅"))	
					{	
						if(p < input.size() - 1)
							res.add(input.get(p+1)); 
						p+=2;
					}
					else
					{
						res.add(destination.forceTruth(input, p).get(p));
						p++; 
					}
				}
				else	{	res.add(input.get(p)) ; p++;	}
			}
			else	{	res.add(input.get(p)); p++;	}
		}
		
		if(p < input.size())
			res.addAll(input.subList(p, input.size()));
		
		return res; 
	}


	/** isMatch
	 *  @param input -- phonological representation of the input
	 *  @param ind -- index we are checking for a match at 
	 *  @precondition : priorMatch(input, ind) == true
	 *  @return true iff target specs fulfilled at this index and posterior specs are too. 
	 */
	public boolean isMatch(List<SequentialPhonic> input, int ind)
	{
		//there is only one target, so within this method, minTargSize just equals 
		// ... the constant target size. 
		int inpSize = input.size(); 
		//abort if index is obviously invalid 
		if(ind + minTargSize + minPostSize - 1 > inpSize || ind < 0)	return false; 
		
		//return false if phone at specified place does not match restrictions on target
		if(!targSource.compare(input, ind))	return false;
	
		//now we know the prior context and target requirements are both met
		// we now check for the posterior context
		if(minPostSize == 0)	return true; 
		return posteriorMatch(input, ind+minTargSize); 
	}
	
	public String toString()
	{
		String output=""; 
		if (minTargSize == 0)	output += "∅";
		else //in this case we know target is a FeatMatrix
			output+=targSource.toString(); 
		
		output += " > ";
		
		if(destination.toString().equals("null phone"))
			output += "∅";
		else	output += destination.print().equals(" @%@ ") ?  destination.toString() : destination.print(); 
			//TODO above line will need to be changed if the standard print of the FeatMatrix class is ever changed
		
		return output + " " + super.toString(); 
	}
}
