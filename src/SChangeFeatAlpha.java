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
		int maxPlace = input.size() - minPostSize - minInputSize; 
		
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

			if(isMatch(input,p))
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
				set_alphvals_everywhere(); 
				need_to_reset = true;
			}
		}
		
		if (!targSource.compare(phHere))
		{
			if (need_to_reset)	reset_alphvals_everywhere(); 
			return false;
		}
		
		if(priorSpecd) {
			// process alpha specs for prior if necessary... this isn't prior match checking --will skip parens, for now. 
			if (priorContext.hasAlphaSpecs())
			{
				if (need_to_reset)	priorContext.applyAlphaValues(ALPH_VARS); // probably redundant with earlier application from input but alas. 
				List<RestrictPhone> pripr = priorContext.getPlaceRestrs();
				String[] pripm = priorContext.getParenMap(); 
				int cpic = ind - 1, // candidate at prior index counter
						//crp = pripr.size() - 1, 
						cpim = pripm.length - 1; 
				//boolean halt = false; // pripm[cpim].contains(")"); 
				while(cpim >= 0 )
				{
					if (pripm[cpim].contains(")")) // parenthesis hopping behavior triggers
					{
						cpim = priorContext.pairedParenLoc(cpim) - 1;
						continue; 
						
						//TODO abrogated below 
						/**
						boolean haltParenthesisHopping = false; 
						
						while (!haltParenthesisHopping)
						{
							cpim = Integer.parseInt (pripm[cpim].split(":")[1].split(",")[0]) - 1; 
							if (cpim < 2) // no more parenthesis possible then; 
							{	
								if (cpim < 0)	// then need to break this entire process.
								{	halt = true; break; }
								if (pripm[cpim].contains(")") || pripm[cpim].contains("("))
									throw new Error("Unexpected parenthesis in parenMap cell "+cpim+": "+pripm[cpim]); 
								haltParenthesisHopping = true; 
							}
							else haltParenthesisHopping = !pripm[cpim].contains(")"); 
						}
						if (halt)	break; 
						crp = Integer.parseInt(pripm[cpim].substring(1)); 
						*/
					}
					
					RestrictPhone pri = pripr.get(
							Integer.parseInt(pripm[cpim].substring(1))); 
					
					if(pri.first_unset_alpha() != '0')
					{
						SequentialPhonic cpi = input.get(cpic); // candidate at prior index
						if (cpi.getType().equals("phone")) {
							if (pri.check_for_alpha_conflict(cpi)) //but note: currently Phone.check_for_alpha_conflict always returns false. 
							{	
								if (need_to_reset)	reset_alphvals_everywhere(); 
								return false;
							}
							//check also for conflict between the restriction on this prior place and the phone there,
								// in matters OUTSIDE the alpha values and return false if so
								// as that will cause a downstream UnsetAlphaException otherwise
							if (!pri.comparePreUnsetAlpha(cpi))	{
								if (need_to_reset)	reset_alphvals_everywhere(); 
								return false; 
							}
							
							ALPH_VARS.putAll(pri.extractAndApplyAlphaValues(cpi));
							need_to_reset = true;
							set_alphvals_everywhere(); 
							/**priorContext.applyAlphaValues(ALPH_VARS);
							if (postSpecd ? postContext.hasAlphaSpecs() : false) postContext.applyAlphaValues(ALPH_VARS);
							destination.applyAlphaValues(ALPH_VARS);*/
							pripr = priorContext.getPlaceRestrs();
						}
					}					
	
					cpic--; cpim--;
				}
			}
		}
		
		if (!priorMatch(input, ind))
		{
			if (need_to_reset)	reset_alphvals_everywhere(); 
			return false;
		}
		
		if (postSpecd) {
			
			//process alpha specs for posterior if necessary...
			if (postContext.hasAlphaSpecs())
			{
				if (need_to_reset)	// alpha values already set from input/source, or from prior context... 
					postContext.applyAlphaValues(ALPH_VARS); 	 // probalby redundant, keeping it for now... 
				List<RestrictPhone> popr = postContext.getPlaceRestrs();
				String[] popm = postContext.getParenMap();
				int cpic = ind + minInputSize, // before July 25 2024 was ind + inpSize but that was probably an error. 
						cpim = 0; 
				//boolean halt = popm[cpim].contains("(") || cpic >= input.size(); 
				
				while(cpic < input.size() && cpim < popm.length)
				{
					if (popm[cpim].contains("("))
					{
						cpim = postContext.pairedParenLoc(cpim) + 1; 
						continue; 
					}
					if (popm[cpim].contains(")"))	throw new Error("Unexpected ')'");
					
					RestrictPhone poi = popr.get(Integer.parseInt(popm[cpim].substring(1)) ); 
					
					if(poi.first_unset_alpha() != '0')
					{	
						SequentialPhonic cpi = input.get(cpic); 
						if (cpi.getType().equals("phone")) {
							if(poi.check_for_alpha_conflict(cpi)) // but note that for Phone instances this is currently always false. 
							{
								if (need_to_reset)	reset_alphvals_everywhere(); 
								return false;
							}
							//check also for conflict between restriction and observed context phone 
							// wrt features OUTSIDE the alpha values and return false if so
								// as that will cause a downstream UnsetAlphaException otherwise
							if (!poi.comparePreUnsetAlpha(cpi))	{
								if (need_to_reset)	reset_alphvals_everywhere(); 
								return false; 
							}

							ALPH_VARS.putAll(poi.extractAndApplyAlphaValues(cpi));
							set_alphvals_everywhere(); 
							/** need_to_reset = true;
							* postContext.applyAlphaValues(ALPH_VARS); 
							* destination.applyAlphaValues(ALPH_VARS);*/
							
							popr = postContext.getPlaceRestrs();
						}
					}
					cpic++; cpim++; 
				}
			}
		}
		
		if (!posteriorMatch(input, ind+minInputSize)) // prior to Aug 22, was ind+inpSize, but that was likely a bug. 
		{
			if (need_to_reset)	reset_alphvals_everywhere(); 
			return false;
		}
		
		if (destination.has_alpha_specs() && need_to_reset)	destination.applyAlphaValues(ALPH_VARS); 

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
