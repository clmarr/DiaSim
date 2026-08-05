import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SChangeFeatAlpha extends SChangeFeat {
		
	public SChangeFeatAlpha(List<String> ordFts, String targSpecs, String destSpecs, String origForm)
	{	super(ordFts, targSpecs, destSpecs, origForm);	
		ALPH_VARS = new HashMap<String, String>(); 
		need_to_reset = false; isAlphaSubclass = true;
	}
	
	public SChangeFeatAlpha(List<String> ordFts, String targSpecs, String destSpecs, 
			boolean bm, String origForm)
	{	super(ordFts, targSpecs, destSpecs, bm, origForm);
		ALPH_VARS = new HashMap<String, String>();
		need_to_reset = false; isAlphaSubclass = true;
	}
	
	public SChangeFeatAlpha(List<String> ordFts, String targSpecs, String destSpecs, 
			SequentialFilter priors, SequentialFilter postrs, String origForm)
	{	super(ordFts, targSpecs, destSpecs, priors, postrs,  origForm); 
		ALPH_VARS = new HashMap<String, String>();
		need_to_reset = false; isAlphaSubclass = true;
	}
	
	public SChangeFeatAlpha(List<String> ordFts, String targSpecs, String destSpecs, 
			boolean bm, SequentialFilter priorContxt, SequentialFilter postContxt, String origForm)
	{	super(ordFts, targSpecs, destSpecs, bm, priorContxt, postContxt, origForm);
		ALPH_VARS = new HashMap<String, String>();
		need_to_reset = false;  isAlphaSubclass = true;
	}
	
	public SChangeFeatAlpha(RestrictPhone source, RestrictPhone dest, String origForm)
	{	super(source, dest, origForm); 
		ALPH_VARS = new HashMap<String, String>();
		need_to_reset = false; isAlphaSubclass = true;
	}
	
	public SChangeFeatAlpha(RestrictPhone source, RestrictPhone dest,
			boolean bm, String origForm)
	{
		super(source, dest, bm, origForm);
		ALPH_VARS = new HashMap<String, String>();
		need_to_reset = false; isAlphaSubclass = true;
	}
	
	public SChangeFeatAlpha(RestrictPhone source, RestrictPhone dest, 
			SequentialFilter priorContxt, SequentialFilter postContxt, String origForm)
	{	
		super(source, dest, priorContxt, postContxt, origForm); 
		ALPH_VARS = new HashMap<String, String>();
		need_to_reset = false; isAlphaSubclass = true;
	}
	
	public SChangeFeatAlpha(RestrictPhone source, RestrictPhone dest, 
			boolean bm, SequentialFilter priorContxt, SequentialFilter postContxt, String origForm)
	{	super(source, dest, priorContxt, postContxt, bm, origForm); 
		ALPH_VARS = new HashMap<String, String>();
		need_to_reset = false; isAlphaSubclass = true;
	}
		
	//Realization
	@Override
	public List<SequentialPhonic> realize(List<SequentialPhonic> input)
	{
		//abort if too small
		if (input.size() < minPriorSize + minInputSize + minPostSize)	return input; 
		
		List<SequentialPhonic> res = new ArrayList<SequentialPhonic>(input.subList(0, minPriorSize)); 
		int p = minPriorSize; 
		int maxPlace = input.size() - minPostSize - minInputSize ; 
		
		while(p <= maxPlace)
		{
			if(!boundsMatter)
			{
				boolean stopIncrement = (p >= input.size()); 
				while(!stopIncrement)
				{
					if(p >= input.size())	stopIncrement = true; 
					else if(input.get(p).print().equals("#") && p == 0)
					{
						res.add(input.get(p)); p++; 
					}
					else if(input.get(p).getType().contains("bound") && p < maxPlace)
						p++; 
					else	stopIncrement = true; 
				}
			}

			if(isMatch(input,p) ) // local override method {as of Dec 24 '25 at least }, will call priorMatch and posteriorMatch
			{
				// when destination is null, we add nothing,
				// and increment p TWICE
				// this is done to block a segment that is deleted itself causing the deletion of the following unit
				// note that this will itself cause rare errors if the that averted situation was actually the intention
				// however it is assumed that this would be incredibly rare. if ever occurring at all. 
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
			if(need_to_reset)	reset_alphvals_everywhere();		
		}
		if(p < input.size())
			res.addAll(input.subList(p, input.size()));
		
		return res; 
	}
	
	// abrogated Dec 24 '25
	
	// note: sets alpha values and only resets them in case of non-match.
	@Override
	public boolean isMatch(List<SequentialPhonic> input, int ind)
	{		
		//there is only one target, so within this method, minTargSize just equals 
		// ... the constant target size. 
		int inpSize = input.size(); 
		//abort if index is obviously invalid 
		if(ind + minInputSize + minPostSize - 1 > inpSize || ind < minPriorSize)	
			return false; 
		
		SequentialPhonic phHere = input.get(ind); 
		
		if(targSource.has_alpha_specs())
		{
			if (!phHere.getType().equals("phone"))	
			{	if(!phHere.print().equals(targSource.print()))	return false;	}
			else if (targSource.check_for_alpha_conflict(phHere))	return false;
			else if (!targSource.comparePreUnsetAlpha(phHere))	return false;
			else 
			{
				ALPH_VARS.putAll(targSource.extractAndApplyAlphaValues(phHere));
				applyAlphasTo(ALPH_VARS, true, true, true, true); 
				need_to_reset = true;
			}
		}
		
		if (!targSource.compare(phHere))
		{
			if (need_to_reset)	reset_alphvals_everywhere(); 
			return false;
		}
		
		if (!priorMatch(input, ind)) // alpha extraction from prior should be handled via methods in SChange via this method. 
		{	// this method also resets alphas in the prior locally for security
			if (need_to_reset)	reset_alphvals_everywhere(); 
			return false;
		}

		//posteriorMatch method will also handle relevant alpha extraction -- to dest only -- and afterwards resets local posterior alphs.
		if (!posteriorMatch(input, ind+minInputSize)) // prior to Aug 22, was ind+inpSize, but that was likely a bug. 
		{	
			if (need_to_reset)	reset_alphvals_everywhere(); 
			return false;
		}
		
		if (destination.has_alpha_specs() && need_to_reset)	applyAlphasTo(ALPH_VARS,false,true,false,false); 
			// probably unnecessary 
		
		return true;
	}
	 

	public void reset_alphvals_everywhere()
	{
		super.reset_alphvals_everywhere(); 
		targSource.resetAlphaValues();
		destination.resetAlphaValues();

		// just to be sure. 
		if (priorSpecd)	priorContext.resetAllAlphaValues();
		if (postSpecd)	postContext.resetAllAlphaValues();
	}
	
	//sets everything based one what's currently in ALPH_VARS
	// accounts for neg proxy pairs 
	// as of Dec 24, '25, unnecessary, functions are handled in SChange. 
	public void set_alphvals_everywhere()
	{
		if (ALPH_VARS.size() == 0)	return; 
		
		targSource.applyAlphaValues(ALPH_VARS);
		destination.applyAlphaValues(ALPH_VARS);
		if (priorSpecd ? priorContext.hasAlphaSpecs() : false) priorContext.applyAlphaValues(ALPH_VARS);
		if (postSpecd ? postContext.hasAlphaSpecs() : false) postContext.applyAlphaValues(ALPH_VARS);
		need_to_reset = true; 
		
		// handling any proxy pairs for extra security -- especially applies to FeatMatrix instances. 
		if (!hasNegAlphProxies())	return; 
		
		HashMap<String, String> NEG_PROX_ADDENDA = new HashMap<String, String>(); 
				
		for (String avki : ALPH_VARS.keySet())
		{
			String proxPair = UTILS.getProxyPair(avki, NEG_ALPH_PROXIES); 
			if (proxPair.equals(UTILS.NULL_PROXY_PAIR) ? false 
					: !ALPH_VARS.containsKey(proxPair) && UTILS.POLAR_FTVECT_INTS.contains(ALPH_VARS.get(avki)))
				NEG_PROX_ADDENDA.put(proxPair, ""+UTILS.getOppFtInt(ALPH_VARS.get(avki))); 
		}

		if(NEG_PROX_ADDENDA.size() == 0)	return; 
		
		targSource.applyAlphaValues(NEG_PROX_ADDENDA);
		destination.applyAlphaValues(NEG_PROX_ADDENDA);
		if (priorSpecd ? priorContext.hasAlphaSpecs() : false) priorContext.applyAlphaValues(NEG_PROX_ADDENDA);
		if (postSpecd ? postContext.hasAlphaSpecs() : false) postContext.applyAlphaValues(NEG_PROX_ADDENDA);
	}
}
