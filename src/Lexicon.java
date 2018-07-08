import java.util.List;
import java.util.HashMap;
import java.util.ArrayList; 

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
		//occurs at least once in a word in the  lexicon
	public HashMap<String, Integer> getPhoneFrequencies()
	{
		HashMap<String, Integer> output = new HashMap<String, Integer>(); 
		for (LexPhon lex : theWordList) //TODO make sure this way of iterating is legit through an array
		{
			List<SequentialPhonic> lexPhonRep = lex.getPhonologicalRepresentation();
			List<SequentialPhonic> phonesAlreadySeen = new ArrayList<SequentialPhonic>(); 
			for (SequentialPhonic ph : lexPhonRep)
			{
				if(ph.getType().equals("phone"))
				{
					if(!phonesAlreadySeen.contains(ph))
					{	String theFeatVect = ph.getFeatString(); 
						if (output.containsKey(theFeatVect))
							output.put(theFeatVect, output.get(theFeatVect)+1); 	
						else	output.put(theFeatVect, 1); 
						phonesAlreadySeen.add(ph);
					}
				}
			}
		}
		return output; 
	}
	
	public LexPhon[] getWordList()
	{	return theWordList;	}
	
	public boolean[] applyRuleAndGetChangedWords(SChange rule)
	{
		int wlLen = theWordList.length ;
		boolean[] wordsChanged = new boolean[wlLen]; 
		
		for( int wli = 0; wli < wlLen; wli++)
		{
			if(theWordList[wli].applyRule(rule))	wordsChanged[wli] = true; 
			else	wordsChanged[wli] = false; 
		}
		return wordsChanged; 
	}
	
}
