import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Function;

public class SChangeSeqToSeqAlpha extends SChangeSeqToSeq{
	
	private boolean need_to_reset;
	
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
		need_to_reset = false; 
	}
	
	public SChangeSeqToSeqAlpha( List<RestrictPhone> trgsrc, List<RestrictPhone> dstSpcs,
			SequentialFilter prior, SequentialFilter postr, String origForm)
	{	super(trgsrc, dstSpcs, prior, postr, origForm);
		ALPH_VARS = new HashMap<String,String>(); 
		need_to_reset = false;
	}
	
	@Override
	// note that this should always operate on an input headed by # and closed also by # 
	public List<SequentialPhonic> realize (List<SequentialPhonic> input)
	{
		int inpSize = input.size(); 
		//abort if too small
		if(inpSize < minPriorSize + minInputSize + minPostSize)	return input; 
		
		// p -- place in input being operated on.
		int p = minPriorSize , 
				maxPlace = inpSize - Math.max(minPostSize + minInputSize, 1); 
		List<SequentialPhonic> res = (p == 0) ? 
				new ArrayList<SequentialPhonic>() : new ArrayList<SequentialPhonic>(input.subList(0, p));
		
		while (p < maxPlace)
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
					else if (!test.comparePreAlpha(cand))	targMatchFail = true; 
					else
					{
						HashMap<String,String> alphHere = test.extractAndApplyAlphaValues(cand); 
						// if there is no alpha conflict, and there is an unset alpha,
						// the only case where the return of extractAndApplyAlphaValues() is empty
							// is when there is a failure to meet a NON-alpha specified value. 
							// so this is a targ match fail. 
						
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
				boolean isPriorMatch = !priorSpecd;
				if(!isPriorMatch)	{
					boolean priorPossible = true; 
	
					if (priorContext.has_unset_alphas())
					{
						List<RestrictPhone> pripr = priorContext.getPlaceRestrs();
						String[] pripm = priorContext.getParenMap(); 
						int cpic = p - 1, crp = pripr.size() - 1, cpim = pripm.length - 1; 
						boolean halt = pripm[cpim].contains(")"); 
						while (!halt)
						{
							RestrictPhone pri = pripr.get(crp);
							if (pri.first_unset_alpha() != '0')
							{
								SequentialPhonic cpi = input.get(cpic);
								if (cpi.getType().equals("phone"))
								{	
									if(pri.check_for_alpha_conflict(cpi))
									{
										halt = true; 
										priorPossible = false; 
									}
									else if (!pri.comparePreAlpha(cpi))	
									{	//check also for conflict OUTSIDE the alpha values and return false if so
											// as that will cause a downstream UnsetAlphaException otherwise
										halt = true; 
										priorPossible = false;
									}
									else
									{
										HashMap<String,String> alphHere = pri.extractAndApplyAlphaValues(input.get(cpic));
										need_to_reset = true;
										priorContext.applyAlphaValues(alphHere);
										if (postSpecd)	postContext.applyAlphaValues(alphHere);
										for (int k = 0; k < destSpecs.size(); k++)	
											destSpecs.get(k).applyAlphaValues(alphHere); //TODO need to check that this works here, I have suspicions it won't.
										pripr = priorContext.getPlaceRestrs();
									}
								}
							}
							cpic--; crp--; cpim--;
							if(crp < 0)	halt = true;
							else	halt = pripm[cpim].contains(")"); 		
						}
					}
					isPriorMatch = priorPossible ? priorMatch(input,p) : false;
				}
				if(isPriorMatch)
				{
					boolean isPostrMatch = !postSpecd; 
					
					if(!isPostrMatch) {
						int indAfter = p + minInputSize;
						boolean postrPossible = true, reachedEnd = false; 
						if(postContext.has_unset_alphas())
						{
							List<RestrictPhone> popr = postContext.getPlaceRestrs();
							String[] popm = postContext.getParenMap();
							int cpic = indAfter, crp = 0, cpim = 0; 
							boolean halt = popm[cpim].contains("(") || cpic >= input.size(); 
								// note that code here seems to assume that no alpha values will be specified after a parenthesis in a posterior context.
									// ... which may not be safe?  
									// TODO revise this? 
							while (!halt)
							{
								RestrictPhone poi = popr.get(crp); 
								if(poi.first_unset_alpha() != '0')
								{
									SequentialPhonic cpi = input.get(cpic); 
									if(cpi.getType().equals("phone"))	{
										if(poi.check_for_alpha_conflict(cpi))
										{
											halt = true; 
											postrPossible = false; 
										}
										else if (!poi.comparePreAlpha(cpi))	
										{	//check also for conflict OUTSIDE the alpha values and return false if so
												// as that will cause a downstream UnsetAlphaException otherwise
											halt = true; 
											postrPossible = false;
										}
										else
										{
											HashMap<String,String> alphHere = poi.extractAndApplyAlphaValues(cpi);
											poi.applyAlphaValues(alphHere);
											if(poi.compare(cpi))
											{
												postContext.applyAlphaValues(alphHere);
												popr = postContext.getPlaceRestrs();
												popm = postContext.getParenMap();
												need_to_reset = true; 
											}
											else
											{
												postrPossible = false;
												halt = true; 
											}
										}
									}
								}
								if(!halt)
								{
									cpic++; crp++; cpim++;
									if (crp >= popr.size())
									{
										halt = true;
										reachedEnd = true;
									}
									else	halt = popm[cpim].contains("(");
								}
							}
						}
						isPostrMatch = !postrPossible ? false : 
							reachedEnd ? true : postContext.isPosteriorMatch(input, indAfter); 
					}
					if (isPostrMatch)
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
	

	private RestrictPhone applyAlph(RestrictPhone inp)
	{
		inp.applyAlphaValues(ALPH_VARS);
		return inp; 
	}
	
	private final Function<RestrictPhone,RestrictPhone> APPLY_ALPHAS = a -> applyAlph(a); 
	
	//uses global ALPH_VARS
	public void mapAlphVals()
	{
		if(priorSpecd)	
			if(priorContext.hasAlphaSpecs())	priorContext.applyAlphaValues(ALPH_VARS);
		if(postSpecd)
			if(postContext.hasAlphaSpecs())	postContext.applyAlphaValues(ALPH_VARS);
		
		targSource = targSource.stream().map(APPLY_ALPHAS).collect(Collectors.toList());
		destSpecs = destSpecs.stream().map(APPLY_ALPHAS).collect(Collectors.toList()); 
		
	}
	
	
	public void reset_alphvals_everywhere()
	{
		for(int i = 0; i < targSource.size(); i++)
			targSource.get(i).resetAlphaValues();
		for(int j = 0; j < destSpecs.size(); j++)
			destSpecs.get(j).resetAlphaValues();
		
		if (priorSpecd)	priorContext.resetAllAlphaValues();
		if (postSpecd) postContext.resetAllAlphaValues();
		ALPH_VARS = new HashMap<String,String>(); 
		need_to_reset = false;
	}
	
	

}
