import java.util.ArrayList;
import java.util.HashMap;
import java.util.List; 

public class SChangeFeat extends SChange {
	protected RestrictPhone targSource, destination; 
	
	//auxiliary for constructors
	public void initialize(List<String> orderedFeats, String targSpecs, String destSpecs, HashMap<String,String[]> featImplications)
	{
		if(!targSpecs.equals("") && !targSpecs.equals("∅"))
		{
			targSource = new FeatMatrix(targSpecs, orderedFeats, featImplications); 
			minTargSize = 1; 
		}
		else //i.e. we know source-targ is null if this is reachedpint. 
			throw new Error("Insertion is not allowed for SChangeFeats -- please use an SChangePhone instead."); 
		if(!destSpecs.equals("") && !destSpecs.equals("∅"))
		{	
			destination = new FeatMatrix(destSpecs, orderedFeats, featImplications);
		}
		else	destination = new NullPhone(); 
	}
	
	public void initialize(RestrictPhone source, RestrictPhone dest) 
	{
		if(source.getClass().toString().contains("NullPhone"))	minTargSize = 0; 
		else	minTargSize = 1; 
		targSource = source; 
		
		if(dest.getClass().toString().contains("NullPhone") && minTargSize <= 0) 
			throw new RuntimeException("Error: both target and destination are null!");  
		destination = dest; 
	}
	
	//constructors follow
	public SChangeFeat(List<String> ordFts, String targSpecs, String destSpecs, String origForm,
			HashMap<String, String[]> featImpls)
	{	super(origForm); initialize(ordFts, targSpecs, destSpecs, featImpls); 	}
	
	public SChangeFeat(List<String> ordFts, String targSpecs, String destSpecs, boolean bm, String origForm, 
			HashMap<String, String[]> featImpls)
	{
		super(bm, origForm); initialize(ordFts, targSpecs, destSpecs, featImpls); 
	}
	
	public SChangeFeat(List<String> ordFts, String targSpecs, String destSpecs, 
			SequentialFilter priors, SequentialFilter postrs, String origForm, HashMap<String,String[]> featImpls)
	{	super(priors,postrs, origForm); initialize(ordFts, targSpecs, destSpecs, featImpls); 	}
	
	public SChangeFeat(List<String>ordFts , String targSpecs, String destSpecs, 
			boolean bm, SequentialFilter priorContxt, SequentialFilter postContxt, String origForm, HashMap<String,String[]> featImpls)
	{	super(priorContxt,postContxt,bm, origForm); initialize(ordFts, targSpecs, destSpecs, featImpls); 	}
	
	public SChangeFeat(RestrictPhone source, RestrictPhone dest, String origForm)
	{	super(origForm); initialize(source,dest); 	}
	
	public SChangeFeat(RestrictPhone source, RestrictPhone dest, boolean bm, String origForm)
	{	super(bm, origForm); initialize(source,dest); 	}
	
	public SChangeFeat(RestrictPhone source, RestrictPhone dest, SequentialFilter priorContxt, SequentialFilter postContxt, String origForm)
	{	super(priorContxt, postContxt, origForm); initialize(source, dest);	}
	
	public SChangeFeat(RestrictPhone source, RestrictPhone dest, SequentialFilter priorContxt, SequentialFilter postContxt, boolean bm, String origForm)
	{	super(priorContxt, postContxt, bm, origForm); initialize(source, dest);	}
	
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
