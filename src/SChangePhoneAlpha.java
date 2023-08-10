import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SChangePhoneAlpha extends SChangePhone {

	private boolean need_to_reset; 
	
	public SChangePhoneAlpha(List<List<SequentialPhonic>> targs, List<List<SequentialPhonic>> dests, String origForm)
	{
		super(targs, dests, origForm);
		need_to_reset = false;
	}
	
	public SChangePhoneAlpha(List<List<SequentialPhonic>> targs, List<List<SequentialPhonic>> dests, SequentialFilter priors, SequentialFilter postrs, String origForm)
	{
		super(targs, dests, priors, postrs, origForm);
		need_to_reset = false; 
	}
	
	public SChangePhoneAlpha(List<List<SequentialPhonic>> targs, ArrayList<RestrictPhone> mutations, String origForm)
	{
		super(targs, mutations, origForm);
		need_to_reset = false; 
	}
	
	public SChangePhoneAlpha(List<List<SequentialPhonic>> targs, ArrayList<RestrictPhone> mutations, SequentialFilter prior, SequentialFilter postr, String origForm)
	{
		super(targs, mutations, prior, postr, origForm); 
		need_to_reset = false; 
	}
	
	@Override
	public List<SequentialPhonic> realize (List<SequentialPhonic> input)
	{
		int inpSize = input.size(), maxPlace = inpSize - Math.max(minPostSize + minTargSize, 1); 
		
		//abort if too small
		if (inpSize < minPriorSize + minTargSize + minPostSize)	return input; 
		int p = minPriorSize; 
		
		List<SequentialPhonic> res = (p==0) ? new ArrayList<SequentialPhonic>() :
			new ArrayList<SequentialPhonic>(input.subList(0, p));
		
		while(p <= maxPlace) 
		{
			SequentialPhonic currInpPh = input.get(p);

			if(currInpPh.toString().contains("bound"))	
			{	res.add(currInpPh);	p++;	}
			else
			{
				boolean priorPossible = true; 
				if (priorSpecd) {
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
								if (cpi.getType().equals("phone")) {
									
									if(pri.check_for_alpha_conflict(cpi))
									{
										if(need_to_reset)	reset_alphvals_everywhere();
										halt = true; 
										priorPossible = false; 
									}
									else if (!pri.compareExceptAlpha(cpi))	
									{	//check also for conflict OUTSIDE the alpha values and return false if so
											// as that will cause a downstream UnsetAlphaException otherwise
										if(need_to_reset)	reset_alphvals_everywhere();
										halt = true; 
										priorPossible = false; 
									}
									else
									{
										ALPH_VARS.putAll(pri.extractAndApplyAlphaValues(input.get(cpic)));
										need_to_reset = true;
										priorContext.applyAlphaValues(ALPH_VARS);
										pripr = priorContext.getPlaceRestrs();
										pripm = priorContext.getParenMap(); 
									}}
							}
							cpic--; crp--; cpim--;
							if(crp < 0)	halt = true;
							else	halt = pripm[cpim].contains(")"); 
								
						}	
					}}
				
				boolean isPriorMatch = priorPossible ? priorMatch(input,p) : false;
				if (isPriorMatch)
				{
					if (need_to_reset && postSpecd)	postContext.applyAlphaValues(ALPH_VARS);
					int matchInd = whichMatch(input, p);
					if (matchInd != -1)
					{
						int indAfter = foundTargetIndAfter(input, targSources.get(matchInd), p); 
						
						if (indAfter > 0)
						{	//begin mutation
							res.addAll(destinations.get(matchInd)); 
							if(p == indAfter)
							{
								res.add(input.get(p));
								p = p + 1;
							}
							else	p =  indAfter; 
						}
						else
						{	res.add(currInpPh); p++; 	}
					}
					else	{	res.add(currInpPh); p++;	}
				
				}
				else
				{	res.add(currInpPh);	p++;	}		
			}
		}
		

		if (p < inpSize)
			res.addAll(input.subList(p, inpSize));
		
		return res; 
		
	}
	
	@Override
	protected boolean posteriorMatch(List<SequentialPhonic> input, int indAfter)
	{
		if(minPostSize == 0)	return true;
		if (postContext.has_unset_alphas())
		{
			HashMap<String,String> temp_alph_vals = new HashMap<String,String> (ALPH_VARS);
			
			List<RestrictPhone> popr = postContext.getPlaceRestrs();
			String[] popm = postContext.getParenMap();
			int cpic = indAfter, crp = 0, cpim = 0; 
			boolean halt = popm[cpim].contains("(");
			while(!halt)
			{
				RestrictPhone poi = popr.get(crp);
				if(poi.first_unset_alpha() != '0')
				{
					SequentialPhonic cpi = input.get(cpic); 
						if(cpi.getType().equals("phone")) {
							if(poi.check_for_alpha_conflict(cpi))
							{
								postContext.resetAllAlphaValues();
								return false;
							}
							else if (!poi.compareExceptAlpha(cpi))	
							{	//check also for conflict OUTSIDE the alpha values and return false if so
									// as that will cause a downstream UnsetAlphaException otherwise
								postContext.resetAllAlphaValues();
								return false;
							}
							else
							{
								temp_alph_vals.putAll(poi.extractAndApplyAlphaValues(cpi));
								postContext.applyAlphaValues(temp_alph_vals);
								popr = postContext.getPlaceRestrs();
								popm = postContext.getParenMap(); 
							}}
				}
				cpic++; crp++; cpim++;
				if (crp >= popr.size())	halt = true;
				else	halt = popm[cpim].contains("(");
			}
		}
		return postContext.isPosteriorMatch(input, indAfter); 
	}
	
	public void reset_alphvals_everywhere()
	{
		ALPH_VARS = new HashMap<String, String>();
		priorContext.resetAllAlphaValues();
		postContext.resetAllAlphaValues();
		need_to_reset = false;
	}
}

