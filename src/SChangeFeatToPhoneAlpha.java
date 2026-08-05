import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SChangeFeatToPhoneAlpha extends SChangeFeatToPhone {
		
	public SChangeFeatToPhoneAlpha(HashMap<String, Integer> ftInds, List<RestrictPhone> targSpecs, List<Phone> dest, String origForm)
	{
		super(ftInds, targSpecs, dest, origForm);
		ALPH_VARS = new HashMap<String,String>(); need_to_reset = false; isAlphaSubclass = true;
	}
	
	public SChangeFeatToPhoneAlpha(HashMap<String, Integer> ftInds, List<RestrictPhone> targSpecs, List<Phone> dest,
			SequentialFilter prior, SequentialFilter postr, String origForm)
	{	super (ftInds, targSpecs, dest,prior, postr, origForm);	
		ALPH_VARS = new HashMap<String,String>(); need_to_reset = false;isAlphaSubclass = true;
	}
	
	@Override
	public List<SequentialPhonic> realize (List<SequentialPhonic> input)
	{
		int inpSize = input.size(); 
		//abort if too small
		if(inpSize < minPriorSize + minInputSize + minPostSize)	return input; 
		
		int p = minPriorSize , 
				maxPlace = inpSize - Math.max(minPostSize + minInputSize, 1); 
		List<SequentialPhonic> res = (p == 0) ? 
				new ArrayList<SequentialPhonic>() : new ArrayList<SequentialPhonic>(input.subList(0, p));
		
		while (p <= maxPlace)
		{
			int p_if_match_fail = p; 
			boolean targMatchFail = false; 

			for (int i = 0 ; i < minInputSize && !targMatchFail ; i++)
			{
				SequentialPhonic cand = input.get(p+i);
				RestrictPhone test = targSource.get(i);
				HashMap<String,String> srcAlphs = new HashMap<String, String>() ;

				if (test.first_unset_alpha() != '0')
				{
					if(cand.getType().equals("phone")) 
					{
						if(test.check_for_alpha_conflict(cand))
							targMatchFail = true;
						else if (!test.comparePreUnsetAlpha(cand))	targMatchFail = true; 
						else
						{
							srcAlphs = test.extractAndApplyAlphaValues(cand); 
							need_to_reset = true;
							test.applyAlphaValues(srcAlphs);
							if (priorSpecd)
								if (priorContext.hasAlphaSpecs())	priorContext.applyAlphaValues(srcAlphs);
							if (postSpecd)
								if (postContext.hasAlphaSpecs())	postContext.applyAlphaValues(srcAlphs);
							for (int j = i; j < minInputSize; j++)	targSource.get(j).applyAlphaValues(srcAlphs);
						}
					}
					else	targMatchFail = true; 
				}

				targMatchFail = targMatchFail ? true : !test.compare(cand);
				
				//if(targMatchFail && srcAlphs.keySet().size() > 0) reset_alphvals_everywhere();
				// above seems unnecessary as this bypasses the next block to trigger the reset there
			}
			
			if (!targMatchFail) //target matched
			{
				if(priorSpecd ? priorMatch(input,p)  : true)
				{// SChange.priorMatch will handle alph extraction from prior, and resetting within it afterward
					if (postSpecd ? posteriorMatch(input, p + minInputSize) : true) 
					{// SChange.posteriorMatch handles alph extraction from postr, then resetting of it
						res.addAll(destination); 
						p += minInputSize; 
					}	
				}
			}
			if ( p == p_if_match_fail)
			{	res.add(input.get(p)); p++;	}
			if (need_to_reset)	reset_alphvals_everywhere(); 
		}
		if (p < inpSize)	res.addAll(input.subList(p, inpSize)); 
		
		return res;
	}
	
	// doesn't super since that would redundantly reset ALPH_VALS (not a huge diff either way though at time of writing.) 
	public void reset_alphvals_everywhere()
	{
		for(int i = 0; i < targSource.size(); i++)
			targSource.get(i).resetAlphaValues();
		if (priorSpecd)	priorContext.resetAllAlphaValues();
		if (postSpecd)	postContext.resetAllAlphaValues();
		need_to_reset = false; 
	}
	
	
}
