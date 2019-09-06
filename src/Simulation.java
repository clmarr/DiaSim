import java.util.ArrayList;
import java.util.List;

public class Simulation {
	
	private List<SChange> CASCADE; 
	
	private Lexicon inputLexicon, currLexicon, goldOutputLexicon; 
	private Lexicon[] goldStageResultLexica, blackStageResultLexica;
	private Lexicon[] goldStageGoldLexica; 
	
	private int[] goldStageInstants, blackStageInstants; 
	private String[] goldStageNames, blackStageNames; 
	
	private int NUM_ETYMA; 
	private int NUM_GOLD_STAGES, NUM_BLACK_STAGES; 
	
	private int instant, stepPrinterval; 
	private int goldStageInd, blackStageInd; // default 0
	
	private String[][] ruleEffects; 
	private String[] etDerivations; 
	
	private boolean opaque; 
	private boolean hasGold; 
	
	public Simulation(LexPhon[] inputForms, List<SChange> casc) 
	{
		inputLexicon = new Lexicon(inputForms); 
		currLexicon = new Lexicon(inputForms); 
		CASCADE = new ArrayList<SChange>(casc); 
		hasGold = false; 
		NUM_ETYMA = inputLexicon.getWordList().length; 
		stepPrinterval = 0; 
		opaque = true; 
		ruleEffects = new String[CASCADE.size()][NUM_ETYMA];
		etDerivations = new String[NUM_ETYMA];
		goldStageNames = new String[0]; 
		blackStageNames = new String[0]; 
		goldStageInstants = new int[0];
		blackStageInstants = new int[0];
	}
	
	public void setGold(LexPhon[] golds)
	{
		goldOutputLexicon = new Lexicon(golds); 
		hasGold = true; 
	}
	
	public void setGoldStages(List<LexPhon[]> stageForms, String[] names, int[] times)
	{
		hasGold = true; 
		goldStageInstants = times;
		goldStageNames = names; 
		goldStageGoldLexica = new Lexicon[stageForms.size()] ;
		goldStageResultLexica = new Lexicon[stageForms.size()] ;
		for (int gsfi = 0; gsfi < stageForms.size(); gsfi++)
			goldStageGoldLexica[gsfi] = new Lexicon(stageForms.get(gsfi)); 
	}
	
	public void setBlackStages(String[] names, int[] times)
	{
		blackStageInstants = times;
		blackStageNames = names;
		blackStageResultLexica = new Lexicon[names.length];
	}	
	
	public void iterate()
	{
		if (instant % stepPrinterval == 0)	System.out.println("On rule number "+instant); 
		SChange thisShift = CASCADE.get(instant); 
		LexPhon[] prevForms = currLexicon.getWordList(); 
		
		boolean[] etChanged = currLexicon.applyRuleAndGetChangedWords(thisShift); 
		for (int ei = 0; ei< NUM_ETYMA; ei++)
		{	if(etChanged[ei])
			{
				etDerivations[ei] += "\n"+currLexicon.getByID(ei)+" | "+instant+" : "+thisShift; 
				ruleEffects[instant][ei] = prevForms[ei].print()+ " > "+currLexicon.getByID(ei); 
			}
		}
		
		if (!opaque){
			System.out.println("Words changed for rule "+instant+" "+thisShift+" : "); 
			for (int wi = 0 ; wi < NUM_ETYMA ;  wi++)
				if (etChanged[wi])
					System.out.println("etym "+wi+" is now : "+currLexicon.getByID(wi)
						+"\t\t[ "+inputLexicon.getByID(wi)+" >>> "+goldOutputLexicon.getByID(wi)+" ]");
		}
		
		instant++; 
		
		if (goldStageInd < NUM_GOLD_STAGES ? instant == goldStageInstants[goldStageInd] : false)
		{
			currLexicon.updateAbsence(goldStageGoldLexica[goldStageInd].getWordList());
			goldStageResultLexica[goldStageInd] = new Lexicon(currLexicon.getWordList()); 
			for (int ei = 0; ei < NUM_ETYMA; ei++)
				etDerivations[ei] += "\n"+goldStageNames[goldStageInd]+" form : "+currLexicon.getByID(ei); 
			goldStageInd++; 
		}
		
		if(blackStageInd<NUM_BLACK_STAGES ? instant == blackStageInstants[blackStageInd] : false)
		{
			blackStageResultLexica[blackStageInd] = new Lexicon(currLexicon.getWordList());
			for (int ei = 0; ei < NUM_ETYMA; ei++)
				etDerivations[ei] += "\n"+blackStageNames[blackStageInd]+" form : "+currLexicon.getByID(ei); 
			blackStageInd++; 
		}	
	}
	
	public void simulate()
	{
		while (instant < CASCADE.size())	iterate(); 
	}
	
	//accessors follow
	public Lexicon getInput()	{	return inputLexicon;	}
	public Lexicon getCurrentResult()	{	return currLexicon;	}
	public Lexicon getGoldOutput()		{	
		assert hasGold : "Error: called for gold outputs but none are set"; 
		return goldOutputLexicon;	}
	public Lexicon getStageResult(boolean goldnotblack, int stagenum)
	{
		return (goldnotblack ? goldStageResultLexica : blackStageResultLexica)[stagenum];
	}
	
	public Lexicon getGoldStageGold(int stagenum)	{	return goldStageGoldLexica[stagenum]; 	}
	
	public String[] getAllDerivations()	{	return etDerivations;	}
	public String getDerivation (int etID)	{	return etDerivations[etID];	}
	public String[][] getAllRuleEffects()	{	return ruleEffects;	}
	public String[] getRuleEffect(int instant)	{	return ruleEffects[instant];	}
	

}
