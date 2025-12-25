import java.util.ArrayList;
import java.util.HashMap;
import java.util.List; 

/**
 * @author Clayton Marr
 * Abstract parent class for all Shift classes, 
 * 		whose basic role is to intake phonological sequences and output what they will be 
 * 		after the diachronic rule ("Shift") has applied.
 * child classes are differentiated by how their targets are specified
 * it is unnecessary to differentiate them by how their destinations are specified because
 * 	(a) for shifts with phone-specified targets, it is most computationally efficient to make 
 * 			those that are constructed with feature-specified targets internally stored with phone
 * 			specified targets, as this is much more computationally efficient. 
 * 	(b) it is very unadvisable to construct instances with feature-specified targets
 * 			but phone-specified destinations, because this would require very frequent searches
 * 			of all phones in use-- very inefficient. 
 * 
 */

public abstract class SChange {

	protected SequentialFilter priorContext, postContext; 
	protected boolean boundsMatter, priorSpecd, postSpecd; 
	protected int minPriorSize, minPostSize, minInputSize; 
	protected String orig;
	protected boolean need_to_reset; // if alphas will need to be reset upon match failure (or after resolved, success. 
	protected boolean isAlphaSubclass;//if it's an alpha subclass. Effectively overridden in alpha subclasses.
	public boolean alphaSubclass()	{	return isAlphaSubclass;	}
	
	protected HashMap<String,String> ALPH_VARS; 
		// stores current alpha variable settings. 
		// if there are none, it's empty (unlike in some other structures, does not keep them constant with some unset value.
		// not used in subclasses that aren't **Alpha subclasses
		// in practice not really used in SChangeFeatToPhoneAlpha either  (TODO NOTE may be obsolete after 10/21/25!) 
			// -- instead any alpha computation within realize is handled within method, 
				// passing from input matching to contexts, and then prior context to posterior, as necessary.
		// it is however used for the other 3 SChange*Alpha subclasses, sometimes via auxiliaries in SChangeSeqToSeqAlpha
				// e.g. SChangeSeqToSeqAlpha has mapAlphaValues() to map an input alph value to all structures
				// SChangeSeqToSeqAlpha.applyAlph applies whatever ALPH_VALS has to an input RestrictPhone
	
	protected HashMap<String,String> NEG_ALPH_PROXIES; //key proxy, value alph it is the neg val for. 
		// unimplemented for all non-alpha subclasses.... just like ALPH_VARS
		// originally not used, just in comments bc all neg alph proxy handling is in FeatMatrix and SequentialFilter
			// and these should be constructed with the relevant mappings BEFORE this is constructed (i.e. passed to it with them already.)
		// as of 10/21/25, implementing, as it appears necessary for SChangeFeatAlpha at lesat. 
	
	public SChange(String origForm)
	{
		orig = ""+origForm; 
		boundsMatter = false; minPriorSize = 0; minPostSize = 0;
		priorSpecd = false; postSpecd = false; isAlphaSubclass = false;
		NEG_ALPH_PROXIES = new HashMap<String, String>(); 
	}
	public SChange(boolean bm, String origForm)
	{
		orig = ""+origForm; 
		boundsMatter = bm; minPriorSize = 0; minPostSize = 0; 
		priorSpecd = false; postSpecd = false; isAlphaSubclass = false;
		NEG_ALPH_PROXIES = new HashMap<String, String>(); 
	}
	public SChange(SequentialFilter prior, SequentialFilter post, String origForm)
	{
		orig = ""+origForm;
		priorContext = prior; postContext = post; boundsMatter = false; 
		minPriorSize = priorContext.getMinSize(); minPostSize = postContext.getMinSize(); 
		priorSpecd = true; postSpecd = true; isAlphaSubclass = false;
		NEG_ALPH_PROXIES = new HashMap<String, String>(); 
	}
	public SChange(SequentialFilter prior, SequentialFilter post, boolean bm, String origForm)
	{
		orig = ""+origForm;
		priorContext = prior; postContext = post; boundsMatter = bm;
		minPriorSize = priorContext.getMinSize(); minPostSize = postContext.getMinSize(); 
		priorSpecd = true; postSpecd = true; isAlphaSubclass = false;
		NEG_ALPH_PROXIES = new HashMap<String, String>(); 
	}
	
	public boolean hasNegAlphProxies()	{	return NEG_ALPH_PROXIES.size() > 0;	}
	
	public void setNegAlphProxies(HashMap<String,String> newProxyMap)
	{
		if (!isAlphaSubclass)
			System.out.println("Warning: setting neg alpha proxies in non-alpha subclass of SChange"); 
		if (hasNegAlphProxies())
			throw new Error("Error: Tried to reset NEG_ALPH_PROXIES once already set!"); 
		NEG_ALPH_PROXIES = new HashMap<String, String> (newProxyMap); 
	}
	
	public void setPriorContext(SequentialFilter p)
	{	priorContext = p; minPriorSize = priorContext.getMinSize(); priorSpecd = true; }
	
	public void setPostContext(SequentialFilter p)
	{	postContext = p; minPostSize = postContext.getMinSize(); postSpecd = true; }
	
	public abstract List<SequentialPhonic> realize(List<SequentialPhonic> phonologicalSeq);
	
	public String toString()
	{
		if (!priorSpecd && !postSpecd)	return "";
		
		String output = "/ "; 
		if(priorSpecd)	output += priorContext.toString() + " "; 
		output = output.trim() + " __ "; 
		if(postSpecd)	output += postContext.toString(); 
		return output.trim(); 
	}

	protected boolean priorMatch(List<SequentialPhonic> input, int frstTargInd)
	{
		if(minPriorSize == 0)	return true; 
		
		boolean hadUnsetAlphVals = priorContext.has_unset_alphas(); 
		
		boolean priorMatch = priorContext.isPriorMatch(input, frstTargInd);
		
		if (priorContext.hasAlphaSpecs())
		{
			HashMap<String,String> priorAlphSpecs = priorContext.getLocalAlphSpecs(); 
			if (hadUnsetAlphVals && priorMatch)					//extract prior's resulting alpha values, 
			{	applyAlphasTo(priorAlphSpecs, true, true, true, true); 	}
			// ...  and reset prior's alphas internally after they are made use of, so they don't bleed into next usage. 
			priorContext.resetAllAlphaValues();
		}
		return priorMatch; 
	}
	protected boolean posteriorMatch(List<SequentialPhonic> input, int indAfter)
	{
		if(minPostSize == 0)	return true;
		boolean hadUnsetAlphVals = postContext.has_unset_alphas(); 
		boolean postrMatch = postContext.isPosteriorMatch(input, indAfter);
		
		if (postContext.hasAlphaSpecs())
		{
			HashMap<String,String> postrAlphSpecs = postContext.getLocalAlphSpecs(); 
			
			if (hadUnsetAlphVals && postrMatch)					//extract prior's resulting alpha values, to destination since that's the only thing that comes next
			{	applyAlphasTo(postrAlphSpecs,true,true,false,false); 	}
			// ...  and reset prior's alphas internally after they are made use of, so they don't bleed into next usage. 
			postContext.resetAllAlphaValues();
		}

		return postrMatch; 
	}
	
	public String getOrig()
	{	return orig.trim();	}
	
	// given what's in @paramater alphSpecs currently, 
	// generate hte negative proxy setting addenda to add to any alpha setting, when necessary
	public HashMap<String,String> negProxAddendaForAlphaComplement(HashMap<String, String> alphSpecs)
	{
		HashMap<String, String> NEG_PROX_ADDENDA = new HashMap<String, String>(); 
		if(!hasNegAlphProxies())	return NEG_PROX_ADDENDA; 
				
		for (String avki : alphSpecs.keySet())
		{
			String proxPair = UTILS.getProxyPair(avki, NEG_ALPH_PROXIES); 
			if (proxPair.equals(UTILS.NULL_PROXY_PAIR) ? false 
					: !alphSpecs.containsKey(proxPair) && UTILS.POLAR_FTVECT_INTS.contains(alphSpecs.get(avki)))
				NEG_PROX_ADDENDA.put(proxPair, ""+UTILS.getOppFtInt(alphSpecs.get(avki))); 
		}
		return NEG_PROX_ADDENDA; 
	}
	
	// cascading of alphval setting:  {except in classes where there are never alphs in one palce -- e.g. no alphs in source or dest for SChangePhoneAlpha, none in dest for SChangeFeatToPhoneAlpha
	//		prior --> rest of prior, source, posterior, dest 
	//		source --> rest of source, posterior, dest
	//		posterior --> rest of posterior, dest. 
	//		dest -- only set from elsewhere, naturally -- it is emergent. 
	
	// built ot allow flexibility via booleans of where to apply alphas
	public void applyAlphasTo(HashMap<String,String> alphVals, boolean toSource, boolean toDest, boolean toPrior, boolean toPostr)
	{
		need_to_reset = true; 

		if (!isAlphaSubclass)	return; 
		for (String avi: alphVals.keySet())
			ALPH_VARS.put(avi, alphVals.get(avi)); 
		
		HashMap<String,String> alphsToApply = new HashMap<String,String> ( alphVals); 
		
		if (hasNegAlphProxies())
			alphsToApply.putAll( negProxAddendaForAlphaComplement(alphVals));

		if (toSource)
			applyAlphasToSource(alphVals) ;
		if (toPrior)
			if (priorSpecd)	
				priorContext.applyAlphaValues(alphVals);
		if (toPostr)
			if (postSpecd)	postContext.applyAlphaValues(alphVals);
		if (toDest)
			applyAlphasToDest(alphVals) ; 
	}

	
	public abstract void applyAlphasToSource(HashMap<String,String> alphVals) ;
	public abstract void applyAlphasToDest(HashMap<String,String> alphVals) ;

	public void reset_alphvals_everywhere() 
	{
		if (!isAlphaSubclass)	return; 
		ALPH_VARS = new HashMap<String, String>();
		if (priorSpecd)	priorContext.resetAllAlphaValues();
		if (postSpecd)	postContext.resetAllAlphaValues();
		resetAlphasInSource() ;
		resetAlphasInDest() ; 
		need_to_reset = false;
	}
	
	public abstract void resetAlphasInSource() ;
	public abstract void resetAlphasInDest() ;
	
	public HashMap<String,String> getNegAlphProxies()
	{	return NEG_ALPH_PROXIES; 	}
	
	// currently here mainly for testing purposes for SChangeFeatAlpha.isMatch,
		// possibly later for other alpha subclasses
	public boolean isMatch(List<SequentialPhonic> input, int ind)
	{
		if (!isAlphaSubclass)
			throw new Error("isMatch called for class that is not SChangeFeat ");
		//otherwise should be overriddent anways. 
		
		System.out.println("Warning: in SChange superclass isMatch() method. This exists only for testing compilation purposes -- you should not be here..."); 
		return false; 
	}
	
}
