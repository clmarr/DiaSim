import java.util.List;
import java.util.ArrayList; 

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
	private String lexClass; //lexical i.e. syntactic class
	private List<String> domains; // semantic domains 
	
	public LexPhon(List<SequentialPhonic> pR)
	{
		if (pR.size() != 0) //not an AbsentLexPhon
		{	
			phonRep = new ArrayList<SequentialPhonic>(pR); 
			if (!phonRep.get(0).equals(new Boundary("word bound")))
				phonRep.add(0, new Boundary("word bound")); 
			if (!phonRep.get(phonRep.size()-1).equals(new Boundary("word bound")))
				phonRep.add(new Boundary("word bound")); 
			this.domains = new ArrayList<String>(); 
		}
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

	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public String getLexClass() {
		return lexClass;
	}

	public void setLexClass(String lex_class) {
		this.lexClass = lex_class;
	}

	public List<String> getDomains() {
		return domains;
	}

	public void addDomain(String domain) {
		this.domains.add(domain);
	}
	
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
		int i_this = 0, i_that = 0;  
		while (i_this < phonRep.size() - sequence.length + 1)
		{
			if(sequence[i_that].compare(phonRep.get(i_this)))
			{
				i_that += 1;
				if (i_that == sequence.length)	return i_this-sequence.length;
			}
			else	i_that = 0;
			i_this += 1; 
		}
		if (i_that > 0)
		{
			while (i_this < phonRep.size() )
			{	if (sequence[i_that].compare(phonRep.get(i_this)))
				{
					i_that += 1; i_this += 1;
					if (i_that == sequence.length)	return i_this-sequence.length;
				}
				else	return -1;
			}
		}
		return -1; 
	}
	
	
	
}
