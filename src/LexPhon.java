import java.util.List;
import java.util.ArrayList;
import java.util.HashMap; 

/**
 * Class for representing the phonology of one word in the vocabulary
 * This class currently forces the edges of the word to have word bounds
 * 	although this actually might not be appropriate for languages with weak boundaries
 *  	such as French with its liaison phenomenon -- nevertheless this phenomenon is outside the purview of this project. 
 * @author Clayton Marr
 */
public class LexPhon {
	private List<SequentialPhonic> phonRep; //phonological representation
	private String lemma; //name of its paradigm
	private String lexClass; //(morpho-)lexical class. Morphosyntactic class, if handled, is to be a key-value pair within morphSynFeatSpecs; 
	private HashMap<String,String> morphSynSpecs; 
	private double frequency; // token frequency, if present; else -1 (which is default).
	private List<String> domains; // semantic domains 
	
	public LexPhon(List<SequentialPhonic> pR)
	{
		if (pR.size() != 0) //not an AbsentLexPhon or UnattestedLexPhon
		{	
			phonRep = new ArrayList<SequentialPhonic>(pR); 
			if (!phonRep.get(0).equals(new Boundary("word bound")))
				phonRep.add(0, new Boundary("word bound")); 
			if (!phonRep.get(phonRep.size()-1).equals(new Boundary("word bound")))
				phonRep.add(new Boundary("word bound")); 
		}
		this.lemma = ""; 
		this.lexClass = ""; 
		this.morphSynSpecs = new HashMap<String,String>(); 
		this.frequency = -1.0; 
		this.domains = new ArrayList<String>(); 
	}	
	
	public List<SequentialPhonic> getPhonologicalRepresentation()
	{	return phonRep;	}
	
	public SequentialPhonic[] getPhOnlySeq()
	{
		SequentialPhonic[] out = new SequentialPhonic[this.getNumPhones()]; 
		int i = 0;
		for(int j = 0; i < out.length; j++)
		{
			if (phonRep.get(j).getType().equals("phone"))
			{
				out[i] = phonRep.get(j); 
				i++; 
			}
		}
		return out; 
	
	}
	
	public int phRepLen()
	{
		return phonRep.size();
	}
	
	//index of first location of the phone if it is present, else returns -1 
	public int findPhone(Phone ph)
	{
		for (int i = 1; i < phonRep.size()-1; i++)
			if (ph.equals(phonRep.get(i)))		return i; 
		return -1; 
	}
	
	//returns true if at least one phone is changed
	// false otherwise 
	public boolean applyRule(SChange theRule)
	{
		List<SequentialPhonic> newPhonRep = theRule.realize(phonRep); 
		boolean changed = false; 
		if (newPhonRep.size() != phonRep.size() )	changed = true; 
		else 
		{
			int prSize = phonRep.size(), i = 0;
			while (i < prSize && !changed)
			{
				changed = !phonRep.get(i).equals(newPhonRep.get(i));
				i++; 
			}
		}
		
		phonRep = new ArrayList<SequentialPhonic>(newPhonRep); 
		return changed; 
	}
	
	
	public String toString()
	{
		String output = "";
		for (SequentialPhonic ph : phonRep)
			output += ph.print(); 
		return output; 
	}
	
	// difference between toString() and print()
		// for this class is that toString() includes Boundary instances
		// and potentially other PseudoPhones
		// whereas print only prints the phones involved
	public String print() 
	{
		String output = "/"; 
		for (SequentialPhonic ph : phonRep)
			if (ph.getType().equals("phone"))	output += ph.print(); 
		return output + "/"; 
	}
	
	public boolean lemmaIsSpecified()	{	return	lemma.length() > 0; 	}
	public boolean lexClassIsSpecified()	{	return lexClass.length() > 0; 	}
	public boolean hasMorphSynSpecs()	{	return morphSynSpecs.size() > 0;	} 
	public boolean frequencyIsSpecified()	{	return frequency != -1.0;	}
	public boolean hasDomains()	{	return domains.size() > 0 ; 	}

	public String getLemma() {
		return lemma;
	}

	public String getLexClass() {
		return lexClass;
	}
	
	public String checkMorphSynSpec(String feat)	{	return morphSynSpecs.get(feat);	}
	public HashMap<String, String> getMorphSynSpecs()	{	return new HashMap<String, String>(morphSynSpecs);	}
		// returning clone to avoid accidental tampering with it. 
	public double getFrequency()	{	return frequency;	}
	
	public boolean checkDomain(String dom)	{	return domains.contains(dom); 	}
	public List<String> getDomains() {	return domains;	}
	
	//auxiliary: count number of actual Phones in list of SequentialPhonic objects 
	public int getNumPhones()
	{
		int count = 0 ;
		for (SequentialPhonic sp :  phonRep)
			if(sp.getType().equals("phone"))
				count++; 
		return count; 
	}
	
	public int findSequence(RestrictPhone[] sequence)
	{
		int lexpr_i = 0, seq_i = 0;  
		while (lexpr_i < phonRep.size() - sequence.length + 1)
		{
			if(sequence[seq_i].compare(phonRep.get(lexpr_i)))
			{
				seq_i += 1;
				if (seq_i == sequence.length)	return lexpr_i -sequence.length + 1;
			}
			else	seq_i = 0;
			lexpr_i += 1; 
		}
		if (seq_i > 0)
		{
			while (lexpr_i <= phonRep.size() - sequence.length + seq_i)
			{	if (sequence[seq_i].compare(phonRep.get(lexpr_i)))
				{
					seq_i += 1; lexpr_i += 1;
					if (seq_i == sequence.length)	return lexpr_i-sequence.length;
				}
				else	return -1;
			}
		}
		return -1; 
	}
	
	// return: how many phones before the end is it? 
	// return of 0 = not found -- because -1 means its the last. 
	public int rFindSequence(RestrictPhone[] sequence)
	{
		int lexpr_i = phonRep.size() - 1, seq_i = sequence.length -1 ;
		while (lexpr_i >= sequence.length - 1)
		{
			if (sequence[seq_i].compare(phonRep.get(lexpr_i)))
			{
				seq_i--; 
				if (seq_i < 0)	return lexpr_i + sequence.length - phonRep.size(); 
			}
			else	seq_i = sequence.length - 1;
			lexpr_i--; 
		}
		if (seq_i < sequence.length - 1)
		{
			while(lexpr_i >= seq_i)
			{
				if (sequence[seq_i].compare(phonRep.get(lexpr_i)))
				{
					seq_i --; lexpr_i--;
					if (seq_i < 0)	return sequence.length - phonRep.size(); 
				}
				else	return 0; 
			}
		}
		return 0;
	}
	
	public void setLemma(String lemma) {	this.lemma = lemma;	}

	public void setLexClass(String lex_class) {	this.lexClass = lex_class;	}
	
	public void setMorphSynSpec (String feat, String val)	{	morphSynSpecs.put(feat, val);	}
	public void removeMorphSynSpec	(String feat)	{	morphSynSpecs.remove(feat);	}
	public void resetMorphSynSpecs	(HashMap<String,String> newSpecs)	
	{	morphSynSpecs = new HashMap<String, String> (newSpecs);	}
	
	public void setFrequency(double freq)	{	this.frequency = freq; 	}

	public void addDomain(String domain) {
		this.domains.add(domain);
	}
	
	public void removeDomain(String domain)	{	this.domains.remove(domain); 	}
	
	public void resetDomains(List<String> newDomains)	{	this.domains = new ArrayList<String>(newDomains); }	
	
}
