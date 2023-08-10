import java.util.ArrayList;
import java.util.Arrays;
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
	
	private int goldStageInd, blackStageInd; // default 0 -- current next stage's index.
	
	private String[] stagesOrdered; 
	private int currStageInd;
		// use stagesOrdered to get current stage in a way that prevents flipping of the order between the two if
			// ever two stages at the same moment (point between rule steps)

	private String[][] ruleEffects; 
	private String[] etDerivations; 
	//stores derivation (form at every time step), with stages delimited by line breaks, of each word 
	
	private boolean opaque; 
	private boolean goldOutput; 
	
	
	public void initialize(Etymon[] inputForms, List<SChange> casc)
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
		currStageInd = 0; 
	}
	
	public Simulation(Etymon[] inputForms, List<SChange> casc, String[] initializedDerivations, String[] orderedStages)
	{
		initialize(inputForms, casc); 
		etDerivations = initializedDerivations; 
		stagesOrdered = orderedStages;
	}
	
	public Simulation(Etymon[] inputForms, List<SChange> casc, String[] orderedStages)
	{
		initialize(inputForms,casc);
		etDerivations = new String[NUM_ETYMA];
		for (int eti = 0; eti < NUM_ETYMA ; eti++)
			etDerivations[eti] = inputForms[eti].print(); 
		stagesOrdered = orderedStages; 
	}
	
	//constructor for differential hypothesis empiricization Simulation object
	public Simulation(Simulation baseline, List<SChange> propCasc)
	{
		Etymon[] inputForms = baseline.getInput().getWordList();
		this.stagesOrdered = baseline.stagesOrdered; 
		initialize(inputForms, propCasc); 
		etDerivations = new String[NUM_ETYMA];
		for (int eti = 0; eti < NUM_ETYMA ; eti++)
			etDerivations[eti] = inputForms[eti].print(); 
		if (baseline.hasGoldOutput())	{
			goldOutputLexicon = baseline.goldOutputLexicon;
			goldOutput = true; 
		}
		if (baseline.hasBlackStages())	
		{
			blackStageNames = baseline.blackStageNames;
			blackStageResultLexica = new Lexicon[blackStageNames.length];
			NUM_BLACK_STAGES = blackStageNames.length; 		}
		
		if (baseline.hasGoldStages()) {
			goldStageGoldLexica = baseline.goldStageGoldLexica;
			goldStageNames = baseline.goldStageNames; 
			goldStageResultLexica = new Lexicon[goldStageNames.length] ;
			NUM_GOLD_STAGES = goldStageNames.length;
		}		
	}
	
	public void setOpacity(boolean opa)	{	opaque = opa;		}
	
	public void setGold(Etymon[] golds)
	{
		goldOutputLexicon = new Lexicon(golds); 
		goldOutput = true; 
	}
	
	public void setGoldStages(Etymon[][] stageForms, String[] names, int[] times)
	{
		goldStageInstants = times;
		goldStageNames = names; 
		goldStageGoldLexica = new Lexicon[stageForms.length] ;
		for (int gsfi = 0; gsfi < stageForms.length; gsfi++)
			goldStageGoldLexica[gsfi] = new Lexicon(stageForms[gsfi]); 
		goldStageResultLexica = new Lexicon[stageForms.length] ;
		NUM_GOLD_STAGES = names.length;
	}
	
	public void setBlackStages(String[] names, int[] times)
	{
		blackStageInstants = times;
		blackStageNames = names;
		blackStageResultLexica = new Lexicon[names.length];
		NUM_BLACK_STAGES = names.length; 
	}	
	
	//for use in constructing hypothesis simulations
	public void setGoldInstants(int[] times)	{	goldStageInstants = times;	}
	public void setBlackInstants(int[] times)	{	blackStageInstants = times;	}	
	
	public void setStepPrinterval(int newsp)	{	stepPrinterval = newsp;	}
	
	public void iterate()
	{
		if (stepPrinterval == 0 ? false : instant % stepPrinterval == 0 && instant != 0)	System.out.println("Simulated to rule number "+instant); 
		SChange thisShift = CASCADE.get(instant); 
		Etymon[] prevForms = currLexicon.getWordList(); 
		
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
		
		assert NUM_GOLD_STAGES + NUM_BLACK_STAGES == stagesOrdered.length : 
			"Error: illegal construction of class variable Simulation.stagesOrdered";
		
		//while not if for scenario that two stages are at same moment-- but ordered within that.
		while(currStageInd >= stagesOrdered.length ? false : instant == getNextStageInd())     
		{
			char type = stagesOrdered[currStageInd].charAt(0); 
			if (!"gb".contains(""+type)) throw new RuntimeException( "Error: illegal typing of stage number "+currStageInd+
	        		" in stagesOrdered : '"+type+"'");
			if ( type == 'g') //it's a gold stage.
        	{
				//TODO need to fix here 
        		goldStageResultLexica[goldStageInd] = new Lexicon(currLexicon.getWordList());
        		for (int ei = 0 ; ei < NUM_ETYMA ; ei++)
        			etDerivations[ei] += "\n"+goldStageNames[goldStageInd]+" stage form : "+currLexicon.getByID(ei);
        		currLexicon.updateAbsence(goldStageGoldLexica[goldStageInd].getWordList());
        		//TODO this is likely still highly insufficient! Because the haltMenu and errorAnalysis do not happen within this class.
        			//TODO will need to handle this somewhere else -- but where, and how to ensure correct behavior here? 
        		//TODO also need to handle "grey stages" which just consist of an updateAbsence call effectively,
        				// not comparison of reconstructed vs. observed forms...?
        		goldStageInd++;
        	}
        	else //black stage
        	{
        		blackStageResultLexica[blackStageInd] = new Lexicon(currLexicon.getWordList());
        		for (int ei = 0; ei < NUM_ETYMA; ei++)
        			etDerivations[ei] += "\n"+blackStageNames[blackStageInd]+" stage form : "+currLexicon.getByID(ei);
        		blackStageInd++;
        	}
        	currStageInd++; 
        }
		
		if (instant == TOTAL_STEPS)
		{
			for (int ei = 0 ; ei < NUM_ETYMA; ei++)
				etDerivations[ei] += "\nFinal form : "+currLexicon.getByID(ei); 
		}
	}
	
	//TODO : method to simulate until manual halting point?
		// TODO did I test this through? I think so, but best to make sure it works again 
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
	public Etymon getCurrentForm(int id)	{	return currLexicon.getByID(id);	}
	public Etymon getInputForm(int id)	{	return inputLexicon.getByID(id);	}
	public Etymon getGoldOutputForm(int id)	{	return goldOutputLexicon.getByID(id);	}
	public Lexicon getGoldOutput()		{	
		if (!goldOutput) throw new RuntimeException( "called for gold outputs but none are set"); 
		return goldOutputLexicon;	}
	public Lexicon getStageResult(boolean goldnotblack, int stagenum)
	{
		return (goldnotblack ? goldStageResultLexica : blackStageResultLexica)[stagenum];
	}
	
	public int getStageInstant(boolean goldnotblack, int stagenum)
	{
		return (goldnotblack ? goldStageInstants : blackStageInstants)[stagenum]; 
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
	{	return instant >= TOTAL_STEPS;	}
	
	public boolean justHitGoldStage()
	{
		//goldStageInd gets incremented upon hitting a stage in simulate() -- if it is 0 there are no gold stages or we haven't hit one yet
		return goldStageInd == 0 ? false : instant == goldStageInstants[goldStageInd-1]; 
	}
	
	// sometimes used for outgraph production. 
	private void calcStagesOrdered()
	{
		stagesOrdered = new String[ 1 + goldStageInd + blackStageInd + (goldOutput && isComplete() ? 1 : 0)]; 
		stagesOrdered[0] = "in";
		if (goldOutput && isComplete())	stagesOrdered[NUM_GOLD_STAGES+NUM_BLACK_STAGES+1] = "out"; 
		int gsi = 0 , bsi = 0 ;
		while ( gsi < goldStageInd && bsi < blackStageInd)
		{
			if ((gsi == goldStageInd) ? 
					false :
						(bsi == blackStageInd ? true :
							goldStageInstants[gsi] <= blackStageInstants[bsi]))
			{	stagesOrdered[gsi+bsi+1] = "g"+gsi; 
				gsi++; 
			}
			else	{
				stagesOrdered[gsi+bsi+1] = "b"+bsi;
				bsi++; 
			}
		}
	}
	
	// get the forms a certain etymon, accessed by its ID, has at each stage
	// in practice, this is currently an auxiliary for making the run's output_graph. 
	
	public String stageOutsForEt(int ID, String[] ordered_stages)
	{
		String to_return = ""+ID; 
		while (to_return.length() < 6)	to_return+=" "; //add spaces to make sure etyma in output graph have same indentation
			//  6 spaces to be safe in case we (hopefully?) end up with lexica > 10k in size... :)  
		to_return += " | "; 
		for (String st : ordered_stages)
		{
			if (st.equals("in"))	to_return += inputLexicon.getByID(ID); 
			else if (st.equals("out"))	to_return += currLexicon.getByID(ID) 
					+ (goldOutput ? " {GOLD: "+goldOutputLexicon.getByID(ID)+"}":""); 
			else
			{
				boolean isg = st.charAt(0) == 'g'; 
				int stn = Integer.parseInt(st.substring(1)); 
				to_return += (isg ? goldStageResultLexica : blackStageResultLexica)[stn].getByID(ID);
				if (isg)	to_return += " {GOLD: "+goldStageGoldLexica[stn]+"}"; 
			}
			to_return += " | "; 
		}
		return to_return.substring(0, to_return.length()-3); 
	}
	public String stageOutsForEt(int ID)	
	{	return stageOutsForEt(ID, stagesOrdered);	} 
	
	
	public String stageOutHeader(String[] ordered_stages)
	{
		String toRet = ""; 
		for(String st : ordered_stages)
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
	public String stageOutHeader()
	{	return stageOutHeader(stagesOrdered);	}
	
	public String outgraph()
	{
		// calcStagesOrdered(); 
		/** currently using slightly modified stage string array 
		 * to include output without possibly causing errors by modifying a frequently used class variable. */
		String[] graph_stages = Arrays.copyOf(stagesOrdered, stagesOrdered.length+1);
		graph_stages[graph_stages.length-1] = "out"; 
		
		String out = "etID  | "+stageOutHeader(graph_stages); 
		for (int i = 0 ; i < NUM_ETYMA ; i++)
			out += "\n"+stageOutsForEt(i, graph_stages); 
		return out; 
	}
	
	public int getNextStageInd()
	{
		int si = Integer.parseInt(stagesOrdered[currStageInd].substring(1));
		return (stagesOrdered[currStageInd].charAt(0) == 'g' ?
	                goldStageInstants : blackStageInstants)[si]; 
	}

	
	public int getTotalSteps()	{	return TOTAL_STEPS;	}

	public int NUM_ETYMA()	{	return NUM_ETYMA;	}
	public int NUM_GOLD_STAGES()	{	return NUM_GOLD_STAGES;	}
	public int NUM_BLACK_STAGES()	{	return NUM_BLACK_STAGES;	}
	
	public String getRuleAt(int id)	{	return ""+CASCADE.get(id); 	}
	
	public int getInstant()	{	return instant;	}
	
	public List<SChange> CASCADE()	{	return CASCADE;	}
	public String[] getGoldStageNames()	{	return goldStageNames;	}
	public int[] getGoldStageInstants()	{	return goldStageInstants;	}
	public String[] getBlackStageNames()	{	return blackStageNames;	}
	public int[] getBlackStageInstants()	{	return blackStageInstants;	}
	public int getGoldStageInd()	{	return goldStageInd;	}
	public int getBlackStageInd()	{	return blackStageInd;	}
	public String[] getStagesOrdered()	{	return stagesOrdered;	}
	public Etymon[][] getGoldStageGoldForms()	{
		Etymon[][] out = new Etymon[NUM_GOLD_STAGES][NUM_ETYMA]; 
		for (int gsi = 0 ; gsi < NUM_GOLD_STAGES; gsi++)
			out[gsi] = goldStageGoldLexica[gsi].getWordList();
		return out;
	}
	
}
