import java.util.List;
import java.util.HashMap;

/**
 * class for representing the set of words (LexPhon instances) being simulated
 * in the language as it develops over time (diachronically) 
 * @author Clayton Marr
 *
 */
public class Lexicon {
	private LexPhon[] theWordList; 
	
	public Lexicon(List<LexPhon> theWords)
	{
		theWordList = new LexPhon[theWords.size()]; 
		theWords.toArray(theWordList);
	}
	
	public Lexicon(LexPhon[] theWords)
	{
		theWordList = theWords.clone(); //TODO make sure this is a deep clone!
	}
	
	//retrieve a particular lexical phonology by its "ID" -- i.e. its index in theWordList
	// DerivationSimulation should construct instances of this class for the word set being simulated 
	// such that words with the same index represent different stages of the same word
	public LexPhon getByID(int ind)
	{	return theWordList[ind]; 	}
	
	// maps each unique phone feat vect onto the number of times a phone with that feat vect 
		//occurs in the lexicon
	// TODO consult David -- is this better to do by presence at all (regardless of # of times) in words 
		// or just raw occurence? 
	public HashMap<String, Integer> getPhoneFrequencies()
	{
		HashMap<String, Integer> output = new HashMap<String, Integer>(); 
		for (LexPhon lex : theWordList) //TODO make sure this way of iterating is legit through an array
		{
			List<SequentialPhonic> lexPhonRep = lex.getPhonologicalRepresentation();
			for (SequentialPhonic ph : lexPhonRep)
			{
				if(ph.getType().equals("phone"))
				{
					String theFeatVect = ph.getFeatString(); 
					if (output.containsKey(theFeatVect))
						output.put(theFeatVect, output.get(theFeatVect)+1); 	
					else	output.put(theFeatVect, 1); 
				}
			}
		}
		return output; 
	}
	
	public LexPhon[] getWordList()
	{	return theWordList;	}
	
	public void applyRuleToLexicon(SChange rule)
	{
		for (LexPhon verbum : theWordList)	verbum.applyRule(rule); 
	}
}
