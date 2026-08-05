import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Function;

public class SChangeSeqToSeqAlpha extends SChangeSeqToSeq{
	
	/**
	 * In master class: 
	 * protected List<RestrictPhone> targSource, destSpecs;
	protected int targSeqSize; 
	protected HashMap<String,Integer> featInds; 
	protected HashMap<String,String> symbMap; 
		 */
	
	public SChangeSeqToSeqAlpha(List<RestrictPhone> trgsrc, List<RestrictPhone> dstSpcs, String origForm)
	{
		super(trgsrc, dstSpcs, origForm); 
		ALPH_VARS = new HashMap<String,String>(); 
		need_to_reset = false; isAlphaSubclass = true;
	}
	
	public SChangeSeqToSeqAlpha( List<RestrictPhone> trgsrc, List<RestrictPhone> dstSpcs,
			SequentialFilter prior, SequentialFilter postr, String origForm)
	{	super(trgsrc, dstSpcs, prior, postr, origForm);
		ALPH_VARS = new HashMap<String,String>(); 
		need_to_reset = false;isAlphaSubclass = true;
	}
	
	@Override
	// note that this should always operate on an input headed by # and closed also by # 
	// neg proxy alpha handling all done within FeatMatrix and SequentialFilter, 
			// but requires that they are constructed correctly with the same neg proxy alpha mapping.
	public List<SequentialPhonic> realize (List<SequentialPhonic> input)
	{
		int inpSize = input.size(); 
		//abort if too small
		if(inpSize < minPriorSize + minInputSize + minPostSize)	return input; 
		
		// p -- place in input being operated on.
		int p = minPriorSize , 
				maxPlace = inpSize - Math.max(minPostSize + minInputSize , 1); 
		List<SequentialPhonic> res = (p == 0) ? 
				new ArrayList<SequentialPhonic>() : new ArrayList<SequentialPhonic>(input.subList(0, p));
		
		while (p <= maxPlace)
		{
			int p_if_match_fail = p; 
			boolean targMatchFail = false; // for halting the for-loop.
			// i -- place in targ source abstraction. 
			for (int i = 0 ; i < minInputSize && !targMatchFail ; i++)
			{
				SequentialPhonic cand = input.get(p+i);
				RestrictPhone test = targSource.get(i);
				
				if(!cand.getType().equals("phone"))
					targMatchFail = !cand.print().equals(test.print()) ;
				else if (test.first_unset_alpha() != '0')
				{
					if(test.check_for_alpha_conflict(cand)) targMatchFail = true;
					else if (!test.comparePreUnsetAlpha(cand))	targMatchFail = true; 
					else
					{
						HashMap<String,String> alphHere = test.extractAndApplyAlphaValues(cand); 
						// if there is no alpha conflict, and there is an unset alpha,
						// the only case where the return of extractAndApplyAlphaValues() is empty
							// is when there is a failure to meet a NON-alpha specified value. 
							// so this is a targ match fail. 
						// handling of neg alpha proxies here depends on handling within FeatMatrix.extract-- if it's a FeatMatrix (otherwise irrelevant) 
						
						if (alphHere.size() == 0 )	targMatchFail = true; 
						else
						{
							for (String alph: alphHere.keySet())  //there will be no replacements since check_for_alpha_conflict was false.
								ALPH_VARS.put(alph,alphHere.get(alph)); 
							need_to_reset = true;	
							test.applyAlphaValues(ALPH_VARS);
							mapAlphVals(); 
						}
					}
				}
				targMatchFail = targMatchFail ? true : !test.compare(cand); 
			}
			if (!targMatchFail) //target matched
			{	
				if(priorSpecd ? priorMatch(input,p) : true)
				{	// SChange.priorMatch, calling SChange.priorMatch if there were unset alphas in prior, will apply them elsewhere
					// and will then reset them in the prior context SequentialFilter itself, tho. 
					
					// ditto for SChange.posteriorMatch below
					if (postSpecd ? posteriorMatch(input, p + minInputSize) : true)
					{
						res.addAll(generateResult(input,p)); 
						p += minInputSize; 
					}
				}
			}
			if ( p == p_if_match_fail)
			{	res.add(input.get(p));
				p++; 
			}
			if (need_to_reset)	reset_alphvals_everywhere(); 
		}
		if (p < inpSize)	res.addAll(input.subList(p, inpSize)); 
		
		return res;
	}

	@Override
	protected List<SequentialPhonic> generateResult(List<SequentialPhonic> input, int firstInd)
	{
		List<SequentialPhonic> output = new ArrayList<SequentialPhonic>();
		int checkInd = firstInd, targInd = 0 ;
		while ( targInd < targSeqSize )
		{
			if(targSource.get(targInd).print().equals("∅")) // a null phone -- must correspond to a proper Phone
			{
				String theSpecs = UTILS.phoneSymbToFeatsMap.get(destSpecs.get(targInd).print());
				output.add(new Phone(theSpecs, UTILS.featIndices, UTILS.phoneSymbToFeatsMap));
			}
			else
			{
				RestrictPhone thisDest = destSpecs.get(targInd); 
				if(!thisDest.compare(new NullPhone())) 
					output.add( destSpecs.get(targInd).forceTruth(input, checkInd).get(checkInd));
				checkInd++; 
			}
			targInd++; 
		}
		return output;
	}
	

	private RestrictPhone applyAlphToInput(RestrictPhone inp)
	{
		inp.applyAlphaValues(ALPH_VARS);
		return inp; 
	}
	
	private final Function<RestrictPhone,RestrictPhone> APPLY_ALPHAS = a -> applyAlphToInput(a); 
	
	//uses global ALPH_VARS
	public void mapAlphVals()
	{
		if(priorSpecd)	
			if(priorContext.hasAlphaSpecs())	priorContext.applyAlphaValues(ALPH_VARS);
		if(postSpecd)
			if(postContext.hasAlphaSpecs())	postContext.applyAlphaValues(ALPH_VARS);
		
		/** for(int i = 0; i < targSource.size(); i++)
			targSource.get(i).applyAlphaValues(ALPH_VARS);
		for(int j = 0; j < destSpecs.size(); j++)
			destSpecs.get(j).applyAlphaValues(ALPH_VARS);*/ 
		
		targSource = targSource.stream().map(APPLY_ALPHAS).collect(Collectors.toList());
		destSpecs = destSpecs.stream().map(APPLY_ALPHAS).collect(Collectors.toList()); 
	}
	
	
	public void reset_alphvals_everywhere()
	{
		super.reset_alphvals_everywhere(); 
		
		for(int i = 0; i < targSource.size(); i++)
			targSource.get(i).resetAlphaValues();
		for(int j = 0; j < destSpecs.size(); j++)
			destSpecs.get(j).resetAlphaValues();
	}
	
	

}
