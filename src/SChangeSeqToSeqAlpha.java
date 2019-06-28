import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SChangeSeqToSeqAlpha extends SChangeSeqToSeq{
	
	private boolean need_to_reset;
	
	/**
	 * In master class: 
	 * protected List<RestrictPhone> targSource, destSpecs;
	protected int targSeqSize; 
	protected HashMap<String,Integer> featInds; 
	protected HashMap<String,String> symbMap; 
		 */
	
	public SChangeSeqToSeqAlpha(HashMap<String, Integer> ftInds, HashMap<String,String> symb_map, List<RestrictPhone> trgsrc, List<RestrictPhone> dstSpcs, String origForm)
	{
		super(ftInds, symb_map, trgsrc, dstSpcs, origForm); 
		need_to_reset = false; 
	}
	
	public SChangeSeqToSeqAlpha(HashMap<String, Integer> ftInds, HashMap<String,String> symb_map,  List<RestrictPhone> trgsrc, List<RestrictPhone> dstSpcs,
			SequentialFilter prior, SequentialFilter postr, String origForm)
	{	super(ftInds, symb_map, trgsrc, dstSpcs, prior, postr, origForm);
		need_to_reset = false;
	}
	
	@Override
	public List<SequentialPhonic> realize (List<SequentialPhonic> input)
	{
		int inpSize = input.size(); 
		//abort if too small
		if(inpSize < minPriorSize + minTargSize + minPostSize)	return input; 
		
		int p = minPriorSize , 
				maxPlace = inpSize - Math.max(minPostSize + minTargSize, 1); 
		List<SequentialPhonic> res = (p == 0) ? 
				new ArrayList<SequentialPhonic>() : new ArrayList<SequentialPhonic>(input.subList(0, p));
		
		while (p < maxPlace)
		{
			int p_if_match_fail = p; 
			boolean targMatchFail = false; 
			for (int i = 0 ; i < minTargSize && !targMatchFail ; i++)
			{
				SequentialPhonic cand = input.get(p+i);
				RestrictPhone test = targSource.get(i);
				if (test.has_unset_alphas() != '0')
				{
					if(test.check_for_alpha_conflict(cand)) targMatchFail = true;
					else
					{
						HashMap<String,String> alphHere = test.extract_alpha_values(cand); 
						need_to_reset = true;
						test.applyAlphaValues(alphHere);
						if (priorContext.hasAlphaSpecs())	priorContext.applyAlphaValues(alphHere);
						if (postContext.hasAlphaSpecs())	postContext.applyAlphaValues(alphHere);
						for (int j = i; j < targSource.size(); j++)	targSource.get(j).applyAlphaValues(alphHere);
						for (int k = 0; k < destSpecs.size(); k++)	destSpecs.get(k).applyAlphaValues(alphHere);
					}
					targMatchFail = targMatchFail ? true : test.compare(cand); 
				}
			}
			if (!targMatchFail) //target matched
			{
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
						if (pri.has_unset_alphas() != '0')
						{
							SequentialPhonic cpi = input.get(cpic);
							if(pri.check_for_alpha_conflict(cpi))
							{
								halt = true; 
								priorPossible = false; 
							}
							else
							{
								HashMap<String,String> alphHere = pri.extract_alpha_values(input.get(cpic));
								need_to_reset = true;
								priorContext.applyAlphaValues(alphHere);
								postContext.applyAlphaValues(alphHere);
								pripr = priorContext.getPlaceRestrs();
							}
						}
						cpic--; crp--; cpim--;
						if(crp < 0)	halt = true;
						else	halt = pripm[cpim].contains(")"); 		
					}
				}
				boolean isPriorMatch = priorPossible ? priorMatch(input,p) : false;
				if(isPriorMatch)
				{
					int indAfter = p + minTargSize;
					boolean postrPossible = true; 
					boolean reachedEnd = false; 
					if(postContext.has_unset_alphas())
					{
						List<RestrictPhone> popr = postContext.getPlaceRestrs();
						String[] popm = postContext.getParenMap();
						int cpic = indAfter, crp = 0, cpim = 0; 
						boolean halt = popm[cpim].contains("("); 
						while (!halt)
						{
							RestrictPhone poi = popr.get(crp); 
							if(poi.has_unset_alphas() != '0')
							{
								SequentialPhonic cpi = input.get(cpic); 
								if(poi.check_for_alpha_conflict(cpi))
								{
									halt = true; 
									postrPossible = false; 
								}
								else
								{
									HashMap<String,String> alphHere = poi.extract_alpha_values(cpi);
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
					boolean isPostrMatch = !postrPossible ? false : 
						reachedEnd ? true : postContext.isPosteriorMatch(input, indAfter); 
					if (isPostrMatch)
					{
						res.addAll(generateResult(input,p)); 
						p += minTargSize; 
					}	
				}
			}
			if ( p == p_if_match_fail)	res.add(input.get(p));
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
				String theSpecs = symbMap.get(destSpecs.get(targInd).print());
				output.add(new Phone(theSpecs, featInds, symbMap));
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
	
	
	public void reset_alphvals_everywhere()
	{
		for(int i = 0; i < targSource.size(); i++)
			targSource.get(i).resetAlphaValues();
		for(int j = 0; j < destSpecs.size(); j++)
			destSpecs.get(j).resetAlphaValues();
		
		priorContext.resetAllAlphaValues();
		postContext.resetAllAlphaValues();
		need_to_reset = false;
	}

}
