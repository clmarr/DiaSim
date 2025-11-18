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
		return output; 
	}

	protected boolean priorMatch(List<SequentialPhonic> input, int frstTargInd)
	{
		if(minPriorSize == 0)	return true; 
		else 	return priorContext.isPriorMatch(input, frstTargInd); 
	}
	protected boolean posteriorMatch(List<SequentialPhonic> input, int indAfter)
	{
		if(minPostSize == 0)	return true;
		
		//if posterior has unset alphas still, these need to be handled within here
		// so they don't remain set in case there's a possibility the same rule could apply twice in the same word.
		// list alph specs to be set and reset within this method 
		List<String> tempAlphSpecs  = postContext.has_unset_alphas() ? 
				postContext.getUnsetAlphSpecs() : new ArrayList<String>(); 
		
		boolean result = postContext.isPosteriorMatch(input, indAfter);
		if (tempAlphSpecs.size() > 0)	postContext.resetTheseAlphaValues(tempAlphSpecs);
		
		return result;
	}
	
	public String getOrig()
	{	return orig;	}
	
	public void reset_alphvals_everywhere() 
	{
		if (!isAlphaSubclass)	return; 
		ALPH_VARS = new HashMap<String, String>();
		if (priorSpecd)	priorContext.resetAllAlphaValues();
		if (postSpecd)	postContext.resetAllAlphaValues();
		need_to_reset = false;
	}
	
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
