import java.util.List;
import java.util.ArrayList;

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
	
	public int findPhone(Phone ph)	{	return -1;	}
	
	public boolean applyRule(SChange sch)	{	return false;	}
	
	public String toString()	{	return representation;	}
	
	public String print() {		return representation;	}
	
	//TODO: override or throw errors for LexPhon methods from findSequence onward? 
	
}
