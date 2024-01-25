import java.util.ArrayList;
import java.util.List;

//import javax.swing.plaf.basic.BasicInternalFrameTitlePane.SystemMenuBar;

public class Simulation {
	
	private List<SChange> CASCADE; 
	
	private Lexicon inputLexicon, currLexicon, goldOutputLexicon; 
	private Lexicon[] goldStageResultLexica, blackStageResultLexica;
	private Lexicon[] goldStageGoldLexica; 
	
	private int[] goldStageInstants, blackStageInstants; 
	private String[] goldStageNames, blackStageNames; 
	private String inputStageName;
	
	private int NUM_ETYMA; 
	private int NUM_GOLD_STAGES, NUM_BLACK_STAGES; 
	
	private int instant, stepPrinterval, TOTAL_STEPS; 
	
	private int goldStageInd, blackStageInd; // default 0 -- current next stage's index.
	
	private String[] stagesOrdered; 
	private int currStageInd;
		// use stagesOrdered to get current stage in a way that prevents flipping of the order between the two if
			// ever two stages at the same moment (point between rule steps)

	private String[][] ruleEffects; 
		// first index is rule number
		// second index is etymon id number
			// it will be null if the etymon is unaffected by the rule
			// otherwise it will be of the form : /X/ > /Y/
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
		inputStageName = "Input"; 
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
		this.inputStageName = baseline.inputStageName; 
	}
	
	public void setOpacity(boolean opa)	{	opaque = opa;		}
	
	public void setGoldOutput(Etymon[] golds)
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
	
	public void setInputStageName(String isn)
	{	inputStageName = ""+isn;	}
	
	//for use in constructing hypothesis simulations
	public void setGoldInstants(int[] times)	{	goldStageInstants = times;	}
	public void setBlackInstants(int[] times)	{	blackStageInstants = times;	}	
	
	public void setStepPrinterval(int newsp)	{	stepPrinterval = newsp;	}
	
	public void iterate()
	{
		if (stepPrinterval == 0 ? false : instant % stepPrinterval == 0 && instant != 0)	System.out.println("Simulated to rule number "+instant); 
		SChange thisShift = CASCADE.get(instant); 
		
		// need to make clones here as doing sometihng like 
			// Etymon[] prevForms = currLexicon.getWordList(); 
		// ... will just have the prevForms modified due to pointers when applyRuleAndGetChangedWords() operates. 
		Etymon[] prevForms = new Etymon[NUM_ETYMA]; 
		for (int pfi = 0 ; pfi < NUM_ETYMA; pfi++)
			prevForms[pfi] = currLexicon.cloneLexemeAt(pfi);
		
		boolean[] etChanged = currLexicon.applyRuleAndGetChangedWords(thisShift); 
		for (int ei = 0; ei< NUM_ETYMA; ei++)
		{	
			if(etChanged[ei])
			{
				etDerivations[ei] += "\n"+currLexicon.getByID(ei)+" | "+instant+" : "+thisShift; 
				ruleEffects[instant][ei] = prevForms[ei].print()+ " > "+currLexicon.getByID(ei).print()
						+ ";             (et."+ei+"; "+inputLexicon.getByID(ei)
						+ (goldOutput ? " > ... > " + goldOutputLexicon.getByID(ei) : "") 
						+ ")"; 
			}
			else ruleEffects[instant][ei] = ""; 
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
        		//TODO also need to handle "black stages" which just consist of an updateAbsence call effectively,
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
				if (isg)	to_return += " {GOLD: "+goldStageGoldLexica[stn].getByID(ID)+"}"; 
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
			if (st.equals("in"))	toRet += inputStageName; 
			else if (st.equals("out"))	toRet += "Output {GOLD}";
			else
			{
				boolean isg = st.charAt(0) == 'g'; 
				int stn = Integer.parseInt(st.substring(1)); 
				toRet += isg ? goldStageNames[stn] + " {GOLD}" : blackStageNames[stn];
			}
			toRet += " | "; 
		}
		return toRet.substring(0, toRet.length()-3); 
	}
	public String stageOutHeader()
	{	return stageOutHeader(stagesOrdered);	}
	
	// returns 2d graph -- in [rows = rule time step + 1 for each stage] [ columns = etyma] 
	// to use for printing a .csv file where each column is an etymon
	// and each row is its form after the operation of rule number [row number - 1]
			// this relationship will change by 1 whenever a stage is passed once intermediate stage insertion/deletion is covered -- but it is not covered yet. 
	// when intermediate stage insertion/deletion is covered, it *will be* "ABSENT" (or UTILS.ABSENT_REPR) for times at which something is absent i.e. fell out of use, not loaned yet ... etc.
	// but this is not covered yet. 
	// cells have... 
	// "{new form}" whenever an etymon is changed by a rule
	// "+ {new form}" whenever an etymon is inserted in an intermediate stage -- once this is covered... 
	// if form is unchanged by the latest rule, so as not to explode file size, the cell will be empty.
	//TODO note that at present, this may not handle insertion/deletion of etyma at stages exactly properly. The goal is to fix this. 
	public String[][] derivationGraph()
	{
		String[][] output = new String[1 + ruleEffects.length + NUM_GOLD_STAGES + NUM_BLACK_STAGES][1 + NUM_ETYMA];
		output[0][0] = "INPUT";

		for (int eti = 0 ; eti < NUM_ETYMA; eti++)
			output[0][1 + eti] = ""+ inputLexicon.getByID(eti).print(); 
		
		int soi = 0; // index in stagesOrdered
		int bsi = 0, gsi = 0; // black stage instance, gold stage instance. ... for when we cover intermediate stage insertion/deletion. 
		int rule_i = 0; // current rule timestep 		
		int outrow_i = 1; // current output column index; which is number of rules plus number of stages past, plus 1 
		
		// current policy: do the following with the idea being that it is as to deal with the possible situation of inserted or deleted etyma
			// can ( and should ) skip if there is no stages at all.  
		if (stagesOrdered.length > 0) 
		{
			boolean nextStageBlack = stagesOrdered[soi].charAt(0) == 'b'; 
			int next_stage_ri = (nextStageBlack ? blackStageInstants : goldStageInstants)
					[Integer.parseInt(stagesOrdered[soi].substring(1))]; 
	
			while (soi >= stagesOrdered.length ? false : !stagesOrdered[soi].equals("out"))
			{
				if (rule_i == next_stage_ri) 
				{
					boolean stageIsBlack = stagesOrdered[soi].charAt(0) == 'b'; 
					
					//TODO debugging
					System.out.println("stage loc detected; " +
							(stageIsBlack ? UTILS.BLACK_STAGENAME_FLAG + blackStageNames[bsi]
									: UTILS.GOLD_STAGENAME_FLAG + goldStageNames[gsi]) 
							+"... (n stages : "+(NUM_GOLD_STAGES + NUM_BLACK_STAGES)); 
				
					output[outrow_i][0] = stageIsBlack ? UTILS.BLACK_STAGENAME_FLAG + blackStageNames[bsi]
							: UTILS.GOLD_STAGENAME_FLAG + goldStageNames[gsi]; 
					Lexicon this_stage_lexicon = stageIsBlack ? blackStageResultLexica[bsi] : goldStageResultLexica[gsi]; 
					for (int eti = 0 ; eti < NUM_ETYMA; eti++)	
					{
						String stagewise_reflex = this_stage_lexicon.getByID(eti).print(); 
						if (output[outrow_i-1][1 + eti].equals(UTILS.ABSENT_REPR) 
								&& !stagewise_reflex.equals(UTILS.ABSENT_REPR)) 
							output[outrow_i][1 + eti] = "+ " ;
						output[outrow_i][1 + eti] += stagewise_reflex; 
					}
							
					soi ++; outrow_i++; 
					if (stageIsBlack)	bsi++; 
					else	gsi++; 
					
					if (soi >= stagesOrdered.length ? false : !stagesOrdered[soi].equals("out"))
					{
						nextStageBlack = stagesOrdered[soi].charAt(0) == 'b'; 
						next_stage_ri = (nextStageBlack ? blackStageInstants : goldStageInstants)
								[Integer.parseInt(stagesOrdered[soi].substring(1))]; 	
					}
					else	break; 
				}
				
				output[outrow_i][0] = "R"+rule_i; 
				
				for (int eti=0; eti < NUM_ETYMA; eti++)
				{
					String rule_eff_here = ruleEffects[rule_i][eti]; 
					int arrow_loc = rule_eff_here.indexOf("> ");
					if (arrow_loc == -1) // no change -- then take previous form. 
						output[outrow_i][1 + eti] = ""; // "^"; // output[outrow_i-1][1 + eti].replace("> ",""); 
					else	// there was a change: cell will have "> [new form]" 
						output[outrow_i][1 + eti] = rule_eff_here.substring(arrow_loc+2, rule_eff_here.indexOf(";")); 
				}
				outrow_i++; rule_i++ ; 
			}				
		}
		
		// cover remaining rules, no risk of insertion or deletion of etyma anymore, or stages being hit... 
		while (rule_i < CASCADE.size())
		{
			output[outrow_i][0] = "R"+rule_i; 
			
			for (int eti=0; eti < NUM_ETYMA; eti++)
			{
				String rule_eff_here = ruleEffects[rule_i][eti]; 
				
				int arrow_loc = rule_eff_here.indexOf("> ");
				if (arrow_loc == -1) // no change -- then take previous form. 
					output[outrow_i][1 + eti] = ""; // "^"; // output[outrow_i-1][1 + eti].replace("> ",""); 
				else	// there was a change: cell will have "> [new form]" 
					output[outrow_i][1 + eti] = ""+rule_eff_here.substring(arrow_loc+2, rule_eff_here.indexOf(";")); 
			}
			outrow_i++; rule_i++ ; 
		}
		
		//TODO debugging
		System.out.println ("\nrulewise outgraph built successfully.");
		
		return output; 
	}
	
	public String outgraph()
	{
		// calcStagesOrdered(); 
		/** currently using slightly modified stage string array 
		 * to include output without possibly causing errors by modifying a frequently used class variable. */
		
		// code below was missing the input stage, probably due to a fix that caused a side effect ?
		/**  String[] graph_stages = Arrays.copyOf(stagesOrdered, stagesOrdered.length+1);
		* graph_stages[graph_stages.length-1] = "out"; */
		String[] graph_stages = new String[stagesOrdered.length+2];
		graph_stages[0] = "in"; 
		for (int gsi = 1; gsi < graph_stages.length-1; gsi++)
			graph_stages[gsi] = stagesOrdered[gsi-1]; 
		graph_stages[graph_stages.length-1] = "out"; 
		
		String out = "etID  | "+stageOutHeader(graph_stages); 
		for (int i = 0 ; i < NUM_ETYMA ; i++)
			out += "\n"+stageOutsForEt(i, graph_stages); 
		return out; 
	}
	
	public int getNextStageInd()
	{
		int si = Integer.parseInt(stagesOrdered[currStageInd].substring(1));

		//TODO debugging
		// System.out.println("stagesOrdered exists? "+stagesOrdered);
		// System.out.println("length of it : "+stagesOrdered.length);
		// System.out.println("currStageInd ...? : "+currStageInd);
		// System.out.println("si : "+si);
		// System.out.println("blackStageInstants exists? "+blackStageInstants);
		// System.out.println("goldStages.... ? "+goldStageInstants);

		return (stagesOrdered[currStageInd].charAt(0) == 'g' ?
	                goldStageInstants : blackStageInstants)[si]; 
	}

	
	public int getTotalSteps()	{	return TOTAL_STEPS;	}

	public int NUM_ETYMA()	{	return NUM_ETYMA;	}
	public int NUM_GOLD_STAGES()	{	return NUM_GOLD_STAGES;	}
	public int NUM_BLACK_STAGES()	{	return NUM_BLACK_STAGES;	}
	
	public String getRuleAt(int id)	{	return ""+CASCADE.get(id); 	}
	public String getOrigRuleAt(int id)	{	return ""+CASCADE.get(id).getOrig();	}
	
	public int getInstant()	{	return instant;	}
	
	public List<SChange> CASCADE()	{	return CASCADE;	}
	public String getInputStageName()	{	return inputStageName;	}
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
