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
		
		while (p < maxPlace)
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
					if(cand.getType().equals("phone")) {
						
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
				// unnecessary as this bypasses the next block to trigger the reset there. 
			}
			
			if (!targMatchFail) //target matched
			{
				boolean isPriorMatch = !priorSpecd; 
				if(!isPriorMatch) {
					boolean priorPossible = true; 
					HashMap<String,String> alphHere = new HashMap<String, String>() ;

					if (priorContext.has_unset_alphas())
					{
						List<RestrictPhone> pripr = priorContext.getPlaceRestrs();
						String[] pripm = priorContext.getParenMap(); 
						int cpic = p - 1, cpim = pripm.length - 1; // crp = pripr.size() - 1,
						while (cpic >= 0 && cpim >= 0)
						{
							if (pripm[cpim].contains(")"))
							{
								cpim = priorContext.pairedParenLoc(cpim) - 1; 
								continue;
							}
							
							if (pripm[cpim].contains("("))	throw new Error("Unexpected '('!");
							
									
							RestrictPhone pri = pripr.get(Integer.parseInt( pripm[cpim].substring(1)));
							if (pri.first_unset_alpha() != '0')
							{
								SequentialPhonic cpi = input.get(cpic);
								if(cpi.getType().equals("phone")) {
									if(pri.check_for_alpha_conflict(cpi))
									{
										priorPossible = false; 
										break;
									}
									else if (!pri.comparePreUnsetAlpha(cpi))	
									{	//check also for conflict OUTSIDE the alpha values and return false if so
											// as that will cause a downstream UnsetAlphaException otherwise
										priorPossible = false; 
										break;
									}
									else
									{
										alphHere = pri.extractAndApplyAlphaValues(input.get(cpic));
										need_to_reset = true;
										priorContext.applyAlphaValues(alphHere);
										postContext.applyAlphaValues(alphHere);
										pripr = priorContext.getPlaceRestrs();
									}}
							}
							cpic--;  cpim--;// crp--;
						}
					}					
					isPriorMatch = priorPossible ? priorMatch(input,p) : false;
					
					if (!isPriorMatch && alphHere.keySet().size() > 0)
					{
						priorContext.resetTheseAlphaValues(new ArrayList<String>(alphHere.keySet()));
						postContext.resetTheseAlphaValues(new ArrayList<String>(alphHere.keySet()));
					}
				}
					
				if(isPriorMatch)
				{
					boolean isPostrMatch = !postSpecd; 
					
					if(!isPostrMatch) {
						int indAfter = p + minInputSize;
						boolean postrPossible = true; 
						boolean reachedEnd = false;

						if(postContext.has_unset_alphas())
						{
							List<RestrictPhone> popr = postContext.getPlaceRestrs();
							String[] popm = postContext.getParenMap();
							int cpic = indAfter, cpim = 0; //crp = 0, 
							//boolean halt = popm[cpim].contains("(") || cpic >= input.size(); 
							while (cpic < input.size() && cpim < popm.length)
							{
								if(popm[cpim].contains("("))
								{
									cpim = postContext.pairedParenLoc(cpim) + 1 ; 
									continue;
								}
								if(popm[cpim].contains(")"))	throw new Error("Unexpected ')'!");
								
								int crp = Integer.parseInt(popm[cpim].substring(1)); 
								RestrictPhone poi = popr.get(crp); 
								if(poi.first_unset_alpha() != '0')
								{
									SequentialPhonic cpi = input.get(cpic); 
										if (cpi.getType().equals("phone")) {
										if(poi.check_for_alpha_conflict(cpi))
										{
											postrPossible = false; 
											break;
										}
										else if (!poi.comparePreUnsetAlpha(cpi))	
										{	//check also for conflict OUTSIDE the alpha values and return false if so
												// as that will cause a downstream UnsetAlphaException otherwise
											postrPossible = false; 
											break;
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
												break;
											}
										}}
								}
								cpic++; crp++; cpim++;
								if (crp == popr.size())
									reachedEnd = true;
								
							}
							reachedEnd = cpim == popm.length; 
						}


						isPostrMatch = !postrPossible ? false : 
							reachedEnd ? true : postContext.isPosteriorMatch(input, indAfter); 
					}
					if (isPostrMatch)
					{
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
