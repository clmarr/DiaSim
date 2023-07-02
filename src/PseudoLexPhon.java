import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Class for representing one of two cases...
 *  	ABSENT -- a word that has either not entered the vocabulary yet, or has fallen out of usage.
 *				indicator in lexicon file: "--" (as of July 2023) 
 *				if a previously present/inherited word is indicated as absent, it will be REMOVED
 *				if it is absent from the beginning, 
 *					it remains such until phonological material is provided in a later column, 
 *						and this will be replaced with a "real" LexPhon instance.
 *		UNATTESTED -- for use ONLY in gold stages or stages for insertion/removal of vocab
 *			this means the form continues to be inherited, but is not attested at the stage
 *				indicator in lexicon file: ">*" (as of July 2023)
 * 
 * @author Clayton Marr
 * TODO implement this to model words that enter the vocabulary late or leave it early in next project.
 */
public class PseudoLexPhon extends LexPhon {
	
	private String representation; 
	
	public	PseudoLexPhon(String repr)
	{
		super(new ArrayList<SequentialPhonic>());
		this.representation = ""+repr; 
		
		// guard rail: 
		if (!UTILS.PSEUDO_LEXPHON_REPRS.contains(representation))
			throw new RuntimeException("Alert: illegal typing of PseudoLexPhon instance: '"+repr+"'. Investigate this."); 
	}
	
	public List<SequentialPhonic> getPhonologicalRepresentation()	{	return null;	}
	
	//TODO consider overriding phRepLen and/or getNumPhones, perhaps with an error/exception
	
	public int findPhone(Phone ph)	{	return -1;	}
	
	public boolean applyRule(SChange sch)	{	return false;	}
	
	public String toString()	{	return representation;	}
	
	public String print() {		return representation;	}

	public int findSequence(RestrictPhone[] sequence)	{	
		System.out.println("Warning: searching for sequence in a PseudoLexPhon...");
		//TODO consider throwing error.
		return -1;  
	}
	public int rFindSequence(RestrictPhone[] sequence)	{	
		System.out.println("Warning: searching backward for sequence in a PseudoLexPhon...");
		//TODO consider throwing error.
		return -1;  
	}
	
	
	/**  the following mutators for morpohlogical info are legal for unattested etyma, 
		* but not for absent etyma! 
		* this for the edge case that an etymon is unattested but we know or want to test a scenario where it changed 
		* something morphologically (likely as part of some larger phenomena)*/ 
	public void setLemma (String lemma) 
	{
		checkForMutateAbsentError("the lemma id", lemma);
		this.lemma = ""+lemma; 
	}
	
	public void setLexClass (String lex_class) 
	{
		checkForMutateAbsentError("the morpholexical class", lex_class); 
		this.lexClass = ""+lex_class; 
	}
	public void setMorphSynSpec (String feat, String val) 
	{
		checkForMutateAbsentError("the morphosyntactic feature '"+feat+"'", val); 
		this.morphSynSpecs.put(feat, val); 
	}
	public void removeMorphSynSpec (String feat)
	{
		checkForMutateAbsentError("the morphosyntactic feature '"+feat+"'", "null"); 
		this.morphSynSpecs.remove(feat); 
	}
	public void resetMorphSynSpecs ( HashMap<String, String> newSpecs)
	{
		checkForMutateAbsentError("the morphosyntactic feature mapping", 
				Arrays.asList(newSpecs).toString()); 
		this.morphSynSpecs = new HashMap<String,String>(newSpecs); 
	}
	public void setFrequency(double freq)
	{
		checkForMutateAbsentError("the token frequency", ""+freq); 
		this.frequency = freq; 
	}
	public void addDomain(String domain)
	{
		checkForMutateAbsentError("the domain set", ""+Arrays.asList(this.domains)+"+"+domain); 
		this.domains.add(domain); 
	}
	public void removeDomain(String domain)
	{
		checkForMutateAbsentError("the semantic domain '"+domain+"'", "removed"); 
		this.domains.remove(domain); 
	}
	public void resetDomains (List<String> newDomains)
	{
		checkForMutateAbsentError("the semantic domain set", ""+Arrays.asList(newDomains)); 
		this.domains = new ArrayList<String>(newDomains);
	}
	
	private void checkForMutateAbsentError (String param, String target)
	{
		if (representation.equals(UTILS.ABSENT_REPR))
			throw new RuntimeException( "Alert: tried to set "+param+" (to '"+target+"') "
					+ "an etymon that is currently absent! Check this.");	 
	}
}
