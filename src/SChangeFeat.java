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
		else //i.e. we know source-targ is null if this is reached. 
		{	assert (!destSpecs.equals("") && !destSpecs.equals("∅")):"Error: both target and destination are null!";
			targSource = new NullPhone(); 
			minTargSize = 0;
		}
		if(!destSpecs.equals("") && !destSpecs.equals("∅"))
		{	
			destination = new FeatMatrix(destSpecs, ftInds);
		}
		else	destination = new NullPhone(); 
	}
	
	public void initializeWRestrPhones(RestrictPhone source, RestrictPhone dest) 
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
			ShiftContext priors, ShiftContext postrs)
	{	super(priors,postrs); initialize(ftInds, targSpecs, destSpecs); 	}
	
	public SChangeFeat(HashMap<String,Integer> ftInds, String targSpecs, String destSpecs, 
			boolean bm, ShiftContext priorContxt, ShiftContext postContxt)
	{	super(priorContxt,postContxt,bm); initialize(ftInds, targSpecs, destSpecs); 	}
	
	public SChangeFeat(RestrictPhone source, RestrictPhone dest)
	{	super(); initializeWRestrPhones(source,dest); 	}
	
	public SChangeFeat(RestrictPhone source, RestrictPhone dest, boolean bm)
	{	super(bm); initializeWRestrPhones(source,dest); 	}
	
	public SChangeFeat(RestrictPhone source, RestrictPhone dest, ShiftContext priorContxt, ShiftContext postContxt)
	{	super(priorContxt, postContxt); initializeWRestrPhones(source, dest);	}
	
	public SChangeFeat(RestrictPhone source, RestrictPhone dest, ShiftContext priorContxt, ShiftContext postContxt, boolean bm)
	{	super(priorContxt, postContxt, bm); initializeWRestrPhones(source, dest);	}
	
	//Realization
	@Override
	public List<SequentialPhonic> realize(List<SequentialPhonic> input)
	{
		int seqSize = input.size(); 
		//abort if too small
		if (seqSize < minPriorSize + minTargSize + minPostSize)	return input; 
		
		List<SequentialPhonic> res = new ArrayList<SequentialPhonic>(input); 
		int p = minPriorSize; 
		
		/** check if target with correct context occurs at each index
		 * We iterate from the beginning to the end of the word. 
		 * We acknowledge the decision to iterate this way (progressive) is arbitrary, and that it may not be technically correct as some changes happen regressively. 
		 * However it is most convenient for the time being.
		 */
		while(p < seqSize - minPostSize + 1 - minTargSize)
		{
			if(priorMatch(input, p))
			{
				if(isMatch(input, p))	//test if both target and posterior specs are met
				{
					//mutate 
					res = destination.forceTruth(res, p);
					if (destination.print().equals("∅"))	{	seqSize--;	}
					p++; //even when destination is null, we increase p by one so that we 
						// don't get an error whereby hte prior context affects a phone
						// acting as a prior context to a shift again
						// when it was not originally in contact with that phone.
						// note that this method will itself cause much rarer errors
						// namely that if hte deleted segment ITSELF was a valid prior context for the shift
						// then this info will be lost-- however we assume that this situation is extremely rare if it 
						// ever occurs at all
					if(!boundsMatter)
					{
						boolean stopIncrement = (p >= seqSize); 
						System.out.println(seqSize+" inpSize"); //TODO debugging
						while(!stopIncrement)
						{
							if(p >= seqSize)	stopIncrement = true; 
							else if(!res.get(p).getType().equals("phone") && 
									p < seqSize - minPostSize + 1 - minTargSize)
								p++; 
							else	stopIncrement = true; 
						}
					}
				}
				else p++;
			}
			else p++;
		}
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
		
		return output + super.toString(); 
	}
}
