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
	
	private int instant, stepPrinterval, TOTAL_STEPS; 
	private int goldStageInd, blackStageInd; // default 0
	
	private String[][] ruleEffects; 
	private String[] etDerivations; 
	//stores derivation (form at every time step), with stages delimited by line breaks, of each word 
	
	private boolean opaque; 
	private boolean goldOutput; 
	
	private String[] stIndex; 

	public void initialize(LexPhon[] inputForms, List<SChange> casc)
	{
		inputLexicon = new Lexicon(inputForms); 
		currLexicon = new Lexicon(inputForms); 
		CASCADE = new ArrayList<SChange>(casc); 
		TOTAL_STEPS = CASCADE.size(); 
		goldOutput = false; 
		NUM_ETYMA = inputLexicon.getWordList().length; 
		NUM_GOLD_STAGES = 0;
		NUM_BLACK_STAGES = 0; 
		stepPrinterval = 0; 
		opaque = true; 
		ruleEffects = new String[CASCADE.size()][NUM_ETYMA];
		instant = 0; 
		goldStageInd = 0; 
		blackStageInd = 0; 
	}
	
	public Simulation(LexPhon[] inputForms, List<SChange> casc, String[] initializedDerivations)
	{
		initialize(inputForms, casc); 
		etDerivations = initializedDerivations; 
	}
	
	public Simulation(LexPhon[] inputForms, List<SChange> casc)
	{
		initialize(inputForms,casc);
		etDerivations = new String[NUM_ETYMA];
		for (int eti = 0; eti < NUM_ETYMA ; eti++)
			etDerivations[eti] = inputForms[eti].print(); 
	}
	
	//constructor for differential hypothesis empiricization Simulation object
	public Simulation(Simulation baseline, List<SChange> propCasc)
	{
		LexPhon[] inputForms = baseline.getInput().getWordList();
		initialize(inputForms, propCasc); 
		etDerivations = new String[NUM_ETYMA];
		for (int eti = 0; eti < NUM_ETYMA ; eti++)
			etDerivations[eti] = inputForms[eti].print(); 
		if (baseline.hasGoldOutput())	{
			goldOutputLexicon = baseline.goldOutputLexicon;
			goldOutput = true; 
		}
		if (baseline.hasBlackStages())	setBlackStages(baseline.blackStageNames, baseline.blackStageInstants); 
		if (baseline.hasGoldStages()) {
			goldStageGoldLexica = baseline.goldStageGoldLexica;
			goldStageInstants = baseline.goldStageInstants;
			goldStageNames = baseline.goldStageNames; 
			goldStageResultLexica = new Lexicon[goldStageNames.length] ;
		}		
	}
	
	public void setOpacity(boolean opa)	{	opaque = opa;	}
	
	public void setGold(LexPhon[] golds)
	{
		goldOutputLexicon = new Lexicon(golds); 
		goldOutput = true; 
	}
	
	public void setGoldStages(LexPhon[][] stageForms, String[] names, int[] times)
	{
		goldStageInstants = times;
		goldStageNames = names; 
		goldStageGoldLexica = new Lexicon[stageForms.length] ;
		for (int gsfi = 0; gsfi < stageForms.length; gsfi++)
			goldStageGoldLexica[gsfi] = new Lexicon(stageForms[gsfi]); 
		goldStageResultLexica = new Lexicon[stageForms.length] ;
	}
	
	public void setBlackStages(String[] names, int[] times)
	{
		blackStageInstants = times;
		blackStageNames = names;
		blackStageResultLexica = new Lexicon[names.length];
	}	
	
	public void setStepPrinterval(int newsp)	{	stepPrinterval = newsp;	}
	
	public void iterate()
	{
		if (stepPrinterval == 0 ? false : instant % stepPrinterval == 0)	System.out.println("On rule number "+instant); 
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
						+"\t\t[ "+inputLexicon.getByID(wi)
						+ (goldOutput ? " >>> "+goldOutputLexicon.getByID(wi) : "") +" ]");
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
	
	//TODO : method to simulate until manual halting point?
	
	public void simulateToNextStage()
	{
		int prevgsi = goldStageInd + 0 , prevbsi = blackStageInd + 0 ;
		while(prevgsi == goldStageInd && prevbsi == blackStageInd && instant < TOTAL_STEPS)
			iterate(); 		
	}
	
	public void simulateToEnd()
	{
		while (instant < TOTAL_STEPS)	iterate(); 
	}
	
	//accessors follow
	public Lexicon getInput()	{	return inputLexicon;	}
	public Lexicon getCurrentResult()	{	return currLexicon;	}
	public LexPhon getCurrentForm(int id)	{	return currLexicon.getByID(id);	}
	public Lexicon getGoldOutput()		{	
		assert goldOutput : "Error: called for gold outputs but none are set"; 
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
	
	public boolean hasGoldOutput()	{	return goldOutput;	}
	public boolean hasGoldStages()	{	return NUM_GOLD_STAGES > 0;	}
	public boolean hasBlackStages()	{	return NUM_BLACK_STAGES > 0; }
	
	public boolean isComplete()
	{	return instant < CASCADE.size();	}
	public boolean justHitGoldStage()
	{
		//goldStageInd gets incremented upon hitting a stage in simulate() -- if it is 0 there are no gold stages or we haven't hit one yet
		return goldStageInd == 0 ? false : instant == goldStageInstants[goldStageInd-1]; 
	}
	
	private void calcStIndex()
	{
		stIndex = new String[ 1 + goldStageInd + blackStageInd + (goldOutput && isComplete() ? 1 : 0)]; 
		stIndex[0] = "in";
		if (goldOutput && isComplete())	stIndex[NUM_GOLD_STAGES+NUM_BLACK_STAGES+1] = "Out"; 
		int gsi = 0 , bsi = 0 ;
		while ( gsi < goldStageInd && bsi < blackStageInd)
		{
			if ((gsi == goldStageInd) ? 
					false : (bsi == blackStageInd ? true : goldStageInstants[gsi] <= blackStageInstants[bsi]))
			{	stIndex[gsi+bsi+1] = "g"+gsi; 
				gsi++; 
			}
			else	{
				stIndex[gsi+bsi+1] = "b"+bsi;
				bsi++; 
			}
		}
	}
	
	public String stageOutsForEt(int ID)
	{
		String toRet = ""+ID; 
		while (toRet.length() < 4)	toRet+=" ";
		toRet += " | "; 
		for (String st : stIndex)
		{
			if (st.equals("in"))	toRet += inputLexicon.getByID(ID); 
			else if (st.equals("out"))	toRet += currLexicon.getByID(ID) 
					+ (goldOutput ? " [GOLD: "+goldOutputLexicon.getByID(ID)+"]":""); 
			else
			{
				boolean isg = st.charAt(0) == 'g'; 
				int stn = Integer.parseInt(st.substring(1)); 
				toRet += (isg ? goldStageResultLexica : blackStageResultLexica)[stn].getByID(ID);
				if (isg)	toRet += " [GOLD: "+goldStageGoldLexica[stn]+"]"; 
			}
			toRet += " | "; 
		}
		return toRet.substring(0, toRet.length()-3); 
	}
	
	public String stageOutHeader()
	{
		String toRet = ""; 
		for(String st : stIndex)
		{
			if (st.equals("in"))	toRet += "Input";
			else if (st.equals("out"))	toRet += "Output [REFERENCE]";
			else
			{
				boolean isg = st.charAt(0) == 'g'; 
				int stn = Integer.parseInt(st.substring(1)); 
				toRet += isg ? goldStageNames[stn] + " [REFERENCE]" : blackStageNames[stn];
			}
			toRet += " | "; 
		}
		return toRet.substring(0, toRet.length()-3); 
	}
	
	public String outgraph()
	{
		calcStIndex(); 
		String out = "etID | "+stageOutHeader(); 
		for (int i = 0 ; i < NUM_ETYMA && i < 10; i++)
			out += "\n"+stageOutsForEt(i); 
		return out; 
	}
	
	public int getTotalSteps()	{	return TOTAL_STEPS;	}

	public int NUM_ETYMA()	{	return NUM_ETYMA;	}
	
}
