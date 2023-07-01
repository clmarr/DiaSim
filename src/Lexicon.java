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
	protected final String ABS_PR ="[ABSENT]"; 
	
	public Lexicon(List<LexPhon> theWords)
	{
		theWordList = new LexPhon[theWords.size()]; 
		theWords.toArray(theWordList);
	}
	
	public Lexicon(LexPhon[] theWords)
	{
		theWordList = new LexPhon[theWords.length];
		for (int wi = 0; wi < theWords.length; wi++)
		{
			if (theWords[wi].print().equals(ABS_PR))
				theWordList[wi] = new AbsentLexPhon();
			else
				theWordList[wi] = new LexPhon(theWords[wi].getPhonologicalRepresentation());
		}
	}
	
	//retrieve a particular lexical phonology by its "ID" -- i.e. its index in theWordList
	// DerivationSimulation should construct instances of this class for the word set being simulated 
	// such that words with the same index represent different stages of the same word
	public LexPhon getByID(int ind)
	{	return theWordList[ind]; 	}
	
	// maps each unique phone feat vect onto the number of times a phone with that feat vect 
		//occurs at least once in a word in the  lexicon
	public HashMap<String, Integer> getPhoneFrequenciesByWord()
	{
		HashMap<String, Integer> output = new HashMap<String, Integer>(); 
		for (LexPhon lex : theWordList)
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
	
	public boolean[] getPhonePresenceByEt(Phone ph)
	{
		boolean[] out = new boolean[theWordList.length];
		for (int wi = 0 ; wi < theWordList.length; wi++)
		{
			out[wi] = theWordList[wi].toString().equals("[ABSENT]") ? false : theWordList[wi].findPhone(ph) != -1; 
		}
		return out;
	}
	
	// "Get changed" -- i.e. we get them by hvaing their indexes be true.
	// used for writing the trajectory files as the lexicon moves forward through time.
	public boolean[] applyRuleAndGetChangedWords(SChange rule)
	{
		int wlLen = theWordList.length ;
		boolean[] wordsChanged = new boolean[wlLen]; 
		
		for( int wli = 0; wli < wlLen; wli++)
		{
			if(theWordList[wli].print().equals(ABS_PR))	wordsChanged[wli] = false;
			if(theWordList[wli].applyRule(rule))	wordsChanged[wli] = true; 
			else	wordsChanged[wli] = false; 
		}
		return wordsChanged; 
	}
	
	//return list of all phones present in words of the lexicon
	public Phone[] getPhonemicInventory()
	{
		List<String> hitPhonesListStr = new ArrayList<String>(); 
		List<SequentialPhonic> phList = new ArrayList<SequentialPhonic>(); 
		for (LexPhon theWord : theWordList)
		{	
			if (!theWord.print().equals(ABS_PR))
			{
				List<SequentialPhonic> thePhones = theWord.getPhonologicalRepresentation(); 
				for (SequentialPhonic curPh : thePhones)
				{
					if(curPh.getType().equals("phone"))
					{
						if(!hitPhonesListStr.contains(curPh.print()))
						{
							hitPhonesListStr.add(curPh.print()); 
							phList.add(curPh);
						}
					}
				}
			}
		}
		int numPhones = phList.size(); 
		Phone[] output = new Phone[numPhones]; 
		for (int phi = 0; phi < numPhones; phi++)	output[phi] = new Phone(phList.get(phi)); 
		return output; 
	}
	
	//counts for each phoneme
	public HashMap<String,Integer> getPhonemeCounts()
	{
		HashMap<String,Integer> theMap = new HashMap<String,Integer>(); 
		for (LexPhon lex: theWordList)
		{
			if (!lex.print().equals(ABS_PR))
				{SequentialPhonic[] thePhones = lex.getPhOnlySeq();
				for (SequentialPhonic curPh : thePhones)
				{
					if(!theMap.containsKey(curPh.print()))
						theMap.put(curPh.print(), 1);
					else	theMap.put(curPh.print(), theMap.get(curPh.print()) + 1);
				}
			}
		}
		return theMap;
	}
	
	//get number of times a particular sequence of phones occurs
	// for use in error analysis when predicting with a third, predictor, stage. 
	public int getPhoneSeqFrequency(List<Phone> targSeq)
	{
		int currSeqInd = 0, count = 0;
		for (LexPhon lex : theWordList)
		{	
			List<SequentialPhonic> thePhones = lex.getPhonologicalRepresentation(); 
			for (SequentialPhonic curPh : thePhones)
			{
				if (curPh.getType().equals("phone"))
				{
					if(curPh.print().equals(targSeq.get(currSeqInd).print()))
					{
						currSeqInd++;
						if (currSeqInd == targSeq.size())
						{
							currSeqInd = 0;
							count++; 
						}
					}
					else
						currSeqInd = 0;
				}
			}	
		}	
		return count;
	}
	
	//update which phones are absent (not yet in language or fell out of use) 
		//based on whether they are absent or not in the latest gold stage 
	//TODO can build off this but will need to work here .
	
	public void updateAbsenceTEST(LexPhon[] stageGold)
	{
		int theLen = stageGold.length;
		if (theLen != theWordList.length)
			throw new RuntimeException("ERROR: mismatch on vocab size of new gold stage and lexicon!");
		for (int wi = 0 ; wi < theLen ; wi++)
		{	
			if(theWordList[wi].print().equals(ABS_PR))
				if(!stageGold[wi].print().equals(ABS_PR))
					theWordList[wi] = new LexPhon(stageGold[wi].getPhonologicalRepresentation());
			if(stageGold[wi].print().equals(ABS_PR))
				if(!theWordList[wi].print().equals(ABS_PR))
					theWordList[wi] = new AbsentLexPhon(); 
		}
	}

	
	public int numAbsentEtyma()
	{
		int cnt = 0;
		for (LexPhon lex: theWordList)
			if (lex.print().equals(ABS_PR))	cnt += 1;
		return cnt;
	}
	
	
}
