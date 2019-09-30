import java.util.ArrayList;
import java.util.List; 
import java.util.Scanner; 

/**
 * "wrapper" class for process of modifying cascade file. 
 * @author Clayton Marr
 *
 */

public class DHSWrapper {
	
	public final String INV_RESP_MSG =  "Invalid response. Please enter a valid response. Returning to hypothesis testing menu.";
	public final char STAGE_PRINT_DELIM = DiachronicSimulator.STAGE_PRINT_DELIM; 
	
	private Simulation baseSimulation;
	private List<SChange> hypCASC, baseCASC; 
	// hypCASC -- new cascade that we are progressively constructing while continually comparing against the "baseline", @varbl baseCASC
	
	private List<String[]> proposedChanges; 
	//TODO important variable here, explanation follows
	// each indexed String[] is form [curr time step, operation details]
	// this object is *kept sorted* by current (i.e. baseline)  form index
		// (IMPORTANT: equivalent to iterator hci, for hypCASC later)
	// operation may be either deletion or insertion 
	// both relocdation and modification are handled as deletion then insertion pairs. 
	// for deletion, the second slot simply holds the string "deletion"
	// whereas for insertion, the second index holds the string form of the SChange 
		// that is inserted there in hypCASC. 
	
	private List<String> propChNotes;
	// will be used to keep notes on changes that will be used in case they are "finalized" 
		// i.e. using automatic modification of the cascade file. 

	private int[] RULE_IND_MAP; //easy access maps indices of CASCADE to those in hypCASCADE.
		// -1 means deleted. 
	private int[] hypGoldLocs, hypBlackLocs; 
	private int originalLastMoment,
		NUM_GOLD_STAGES, NUM_BLACK_STAGES, NUM_ETYMA; 
	private String[] goldStageNames, blackStageNames;
	private boolean feats_weighted;
	private String[] featsByIndex;
	private double[] FT_WTS;
	private int id_wt; 
	public boolean stillQuerying; 
		
	
	
	public DHSWrapper(Simulation baseSim,boolean feats_weighted, String[] featsByIndex, double[] FT_WTS, int id_wt)
	{
		baseSimulation = baseSim;
		proposedChanges = new ArrayList<String[]>(); 
		propChNotes = new ArrayList<String>(); 
		hypCASC = new ArrayList<SChange>(baseSim.CASCADE()); 
		baseCASC = new ArrayList<SChange>(baseSim.CASCADE());
		originalLastMoment = baseCASC.size(); 
		NUM_ETYMA = baseSim.NUM_ETYMA(); 
		NUM_GOLD_STAGES = baseSim.NUM_GOLD_STAGES();
		NUM_BLACK_STAGES = baseSim.NUM_BLACK_STAGES(); 
		RULE_IND_MAP = new int[originalLastMoment + 1]; 
		hypGoldLocs = new int[NUM_GOLD_STAGES]; hypBlackLocs = new int[NUM_BLACK_STAGES]; 
		for (int i = 0; i < originalLastMoment+1; i++)	RULE_IND_MAP[i] = i; //initialize each.
		for (int i = 0; i < NUM_GOLD_STAGES; i++)	hypGoldLocs[i] = baseSim.getStageInstant(true, i);
		for (int i = 0; i < NUM_BLACK_STAGES; i++)	hypBlackLocs[i] = baseSim.getStageInstant(false, i); 
		goldStageNames = baseSim.getGoldStageNames(); 
		blackStageNames = baseSim.getBlackStageNames();	
		this.feats_weighted = feats_weighted;
		this.featsByIndex = featsByIndex;
		this.FT_WTS = FT_WTS; 
		this.id_wt = id_wt; 
	}
	
	public void queryProposedChanges(Scanner inpu, SChangeFactory fac)
	{
		stillQuerying = true; 
		String resp; 

		while (stillQuerying)
		{
			int forkAt = queryForkPoint(inpu, fac); 
			boolean toSetBehavior = true;
			while (toSetBehavior) {
				System.out.println("Operating on rule "+forkAt+": "+hypCASC.get(forkAt).toString()) ;
				System.out.print("What would you like to do?\n"
						+ "0: Insertion -- insert a rule at "+forkAt+".\n"
						+ "1: Deletion -- delete the rule at "+forkAt+".\n"
						+ "2: Modification -- change the rule at "+forkAt+".\n"
						+ "3: 'Relocdation' -- move the rule at "+forkAt+" to a different moment in time.\n"
						+ "8: Go back to previous option (setting fork point, querying options available).\n"
						+ "9: Cancel entire forking test and return to main menu.\n"); 
				resp = inpu.nextLine().substring(0,1);
			
				if(!"012389".contains(resp))
					System.out.println("Invalid entry. Please enter the valid number for what you want to do."); 
				else if(resp.equals("9"))
					stillQuerying = false; 
				else if(resp.equals("8"))
					System.out.println("Returning to prior menu.");
				else
				{
					int deleteAt = -1; 
					List<String[]> insertions = new ArrayList<String[]>(); 
					List<String> insertionNotes = new ArrayList<String>();
					String deletionNotes = ""; 
					
					if("123".contains(resp)) // all the operations that involve deletion.
					{
						deleteAt = forkAt; 
						SChange removed = hypCASC.remove(deleteAt); 

						if(resp.equals("1"))	deletionNotes = "Former rule "+deleteAt+" [ "+removed.toString()+" ] simply removed."; 
						else if (resp.equals("2")) deletionNotes = "Former rule "+deleteAt+" [ "+removed.toString()+" ]"; // will have specific modification appended later.  
						else if(resp.equals("3"))
						{
							int relocdate = -1; 
							while(relocdate == -1)
							{
								System.out.println("Enter the moment (rule index) would you like to move this rule to:");

								int candiDate = getValidInd(inpu.nextLine().replace("\n",""), hypCASC.size());
								if (candiDate == -1)
									System.out.println("Invalid rule index entered. There are currently "+hypCASC.size()+" rules."); 
								else
								{
									System.out.println("Rule before moment "+candiDate+": "
											+ (candiDate == 0 ? "BEGINNING" : hypCASC.get(candiDate - 1)) );
									System.out.println("Rule after moment "+candiDate+": "
											+ (candiDate == hypCASC.size() ? "END" : hypCASC.get(candiDate)) ); 
									System.out.println("Are you sure you want to move rule "+forkAt+" to here?"); 
									char conf = '0';
									while (!"yn".contains(conf+""))
									{
										System.out.println("Please enter 'y' or 'n' to confirm or not."); 
										conf = inpu.nextLine().toLowerCase().charAt(0); 
									}
									if (conf == 'y')	relocdate = candiDate; 
								}
							}
							
							// unnecessary -- handled implicitly. 
							//if ( deleteAt > relocdate ) deleteAt--;
							//else	relocdate--; 
							deletionNotes = "Former rule "+deleteAt+" [ "+removed.toString()+" ] relocdated\n\tmoved to "+relocdate; 
					
							insertions.add(new String[] {""+relocdate, removed.toString()} );
							insertionNotes.add("Moved, originally at "+deleteAt); 
							hypCASC.add( relocdate , removed);
						}
					}
					if ("02".contains(resp)) // all the operations that involve insertion of a NEW rule.
					{
						
						List<SChange> propShifts = null;
						String propRule = ""; 
						while(toSetBehavior) {
							System.out.println("Please enter the new rule:");
							propRule = inpu.nextLine().replace("\n", ""); 
							toSetBehavior = false;

							try
							{	propShifts = fac.generateSoundChangesFromRule(propRule); 	}
							catch (Error e)
							{
								toSetBehavior = true;
								System.out.println("Preempted error : "+e); 
								System.out.println("You entered an invalid rule, clearly. All rules must be of form A -> B / X __ Y.");
								System.out.println("Valid notations include [] () ()* ()+ {;} # @ ,");
							}
						}
														
						//actual insertions here.
						// insert "backwards" so that intended order is preserved
						while(propShifts.size() > 0)
						{
							SChange curr = propShifts.remove(propShifts.size()-1); 
							insertions.add(new String[] {  "" +(forkAt + propShifts.size() ) ,
									curr.toString() } );
							if (resp.equals("0"))
								insertionNotes.add(""); //no notes for simple insertion. 
							else	//modification
								insertionNotes.add("Part of replacement of "+deletionNotes.substring(12)); 
							hypCASC.add(forkAt,curr);
						}

						if(resp.equals("2"))
							deletionNotes += " modified\nto "+propRule; 
						
						
					}
					
					// data structure manipulation
					updateMappings(forkAt, resp.charAt(0), insertions); 
						
					// update proposedChanges while keeping it sorted by index of operation
						// if there is a "tie" in index, we always list deletions first, then insertions.
					 
					// only way that an insertion could come before a deletion is in relocdation
						// if we relocdate to an earlier date. 
					
					if("123".contains(resp)) //handling deletion for @varbl proposedChanges
					{
						int pcplace = proposedChanges.size(); 
						boolean foundTargSpot = false; 
						while (pcplace == 0 ? false : foundTargSpot)
						{
							
							String[] prev = proposedChanges.get(pcplace - 1); 
							int prevLoc = Integer.parseInt(prev[0]); 
							if (deleteAt < prevLoc)	foundTargSpot = true; 
							else if (deleteAt == prevLoc)
								foundTargSpot = "deletion".equals(prev[1]); 
							
							if (!foundTargSpot)
							{	
								pcplace--; 
								proposedChanges.set(pcplace, 
										new String[] { ""+(prevLoc-1) , prev[1] } );
							}
						}
						
						if (pcplace == proposedChanges.size())	
						{	proposedChanges.add(new String[] {""+deleteAt, "deletion"});
							propChNotes.add(deletionNotes);
						}
						else
						{
							proposedChanges.add(pcplace, new String[] {""+deleteAt, "deletion"});	
							propChNotes.add(pcplace, deletionNotes); 
						}
					}
					
					if("023".contains(resp))
					{
						int insertLoc = Integer.parseInt(insertions.get(0)[0]);
						int pcplace = proposedChanges.size(); //because we iterate backwards. 
						boolean foundTargSpot = false; 
						int increment = insertions.size(); 
						while (pcplace == 0 ? false : foundTargSpot)
						{
							String[] prev = proposedChanges.get(pcplace - 1); 
							int prevLoc = Integer.parseInt(prev[0]); 
							if (insertLoc <prevLoc ) foundTargSpot = true; 
							else if (insertLoc == prevLoc)	foundTargSpot = "deletion".equals(prev[1]); 
							
							if (!foundTargSpot)
							{	
								pcplace--; 
								proposedChanges.set(pcplace, 
										new String[] { ""+(prevLoc+increment) , prev[1] } );
							}
						}
						if (pcplace == proposedChanges.size())
						{
							proposedChanges.addAll(insertions); 
							propChNotes.addAll(insertionNotes); 
						}
						else	
						{
							proposedChanges.addAll(pcplace, insertions);
							propChNotes.addAll(pcplace, insertionNotes); 
						}
					}
					
					System.out.println("Would you like to make another change at this time? Enter 'y' or 'n'."); 
					char conf = inpu.nextLine().toLowerCase().charAt(0); 
					while (!"yn".contains(conf+""))
					{
						System.out.println("Please enter 'y' or 'n' to confirm or not."); 
						conf = inpu.nextLine().toLowerCase().charAt(0); 
					}
					toSetBehavior = (conf == 'y'); 
				}
			}

			//actual hypothesis test simulation of CASCADE and hypCASCADE... results, etc. 
			Simulation hypEmpiricized = new Simulation (baseSimulation, hypCASC); 
			//TODO iteration of @varbl hypEmpiricized 
			boolean gsstops; 
			if (NUM_GOLD_STAGES > 0)
			{
				System.out.println("Halt at gold stages? Enter 'y' or 'n'."); 
				char conf = inpu.nextLine().toLowerCase().charAt(0); 
				while (!"yn".contains(conf+""))
				{
					System.out.println("Please enter 'y' or 'n' to confirm stage stopping or not."); 
					conf = inpu.nextLine().toLowerCase().charAt(0); 
				}
				gsstops = (conf == 'y'); 
			}
			else	gsstops = false; 
			
			if(gsstops)
			{
				int gssi = 0; 
				while(!hypEmpiricized.isComplete())
				{
					hypEmpiricized.simulateToNextStage();
					while (!hypEmpiricized.justHitGoldStage() && !hypEmpiricized.isComplete()) //TODO check this. 
						hypEmpiricized.simulateToNextStage();
					
					
					ErrorAnalysis hsea = new ErrorAnalysis(hypEmpiricized.getCurrentResult(), baseSimulation.getGoldStageGold(gssi), featsByIndex, 
							feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));
					
					ErrorAnalysis bsea = new ErrorAnalysis(baseSimulation.getStageResult(true, gssi), baseSimulation.getGoldStageGold(gssi), featsByIndex, 
									feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));
					
					System.out.println("Hit gold stage "+gssi+": "+goldStageNames[gssi]); 
					gssi++; 
					
					double[] pctAccs = new double[] { bsea.getAccuracy(), hsea.getAccuracy() },
							pct1offs = new double[] { bsea.getPctWithin1(), hsea.getPctWithin1() },
							avgFEDs = new double[] { bsea.getAvgFED(), hsea.getAvgFED() };
					if (pctAccs[0] != pctAccs[1] || pct1offs[0] != pct1offs[1] || avgFEDs[0] != avgFEDs[1])
					{
						System.out.println("Overall accuracy : "+pctAccs[0]+" >>> "+pctAccs[1]);
						System.out.println("Accuracy within 1 phone: "+pct1offs[0]+" >>> "+pct1offs[1]);
						System.out.println("Accuracy within 2 phone: "+bsea.getPctWithin2()+" >>> "+hsea.getPctWithin2());
						System.out.println("Average edit distance per from gold phone: "+bsea.getAvgPED()+" >>> "+hsea.getAvgPED());
						System.out.println("Average feature edit distance from gold: "+avgFEDs[0]+" >>> "+avgFEDs[1]); 
						System.out.println("Press anything to continue."); 
						char dum = inpu.nextLine().charAt(0);
						//TODO possibly enable further user interaction here? 

					}
					else	System.out.println("No divergence yet."); 
					
				}
			}
			else	hypEmpiricized.simulateToEnd();


			
		}
	}
		
	private int getValidInd(String s, int max)
	{
		return DiachronicSimulator.getValidInd(s, max); 
	}
	
	private void printBaselineRuleAt(int theInd)
	{
		if (theInd == baseCASC.size())
			System.out.println("Ind "+theInd+" is right after the realization of the last rule.");
		else System.out.println(baseCASC.get(theInd)); 
	}

	public int queryForkPoint(Scanner inpu, SChangeFactory fac)
	{
		String resp;
		int forkAt = -1; 
		String currRuleOptions = "\t\t\t'get curr rule X', to get the index of any rules containing an entered string replacing <X>.\n"
				+ "\t\t\t'get curr rule at X', to get the rule at the original index number <X>.\n" 
				+ "\t\t\t'get curr rule effect X', to get all changes from a rule by the index <X>.\n";
		while(forkAt == -1 && stillQuerying) {
			
			//TODO is this confusing? 
			System.out.print("At what current rule number would you like to modify cascade? Please type the number.\n"
					+ "You may also enter:\t'quit', to return to the main menu\n"
					+ "\t\t\t'get rule X', to get the index of any rules containing an entered string replacing <X>.\n"
					+ "\t\t\t'get rule at X', to get the rule at the original index number <X>.\n" 
					+ "\t\t\t'get rule effect X', to get all changes from a rule by the index <X>.\n"
					+ "\t\t\t'get cascade', to print all rules with their original/new indices.\n"
					+ (proposedChanges.size() >= 1 ? currRuleOptions : "")
					+ "\t\t\t'get etym X', to print the index of the INPUT form etyma entered <X>.\n"
					+ "\t\t\t:'get etym at X', to get the etymon at index <X>.\n"
					+ "\t\t\t:'get etym derivation X', to get the full derivation of etymon with index <X>.\n"
					+ "\t\t\t:'get lexicon', print entire lexicon with etyma mapped to inds.\n"); 
			resp = inpu.nextLine().replace("\n",""); 
			forkAt = getValidInd(resp, originalLastMoment) ;
				//TODO make sure it is correct to use base's last moment as max here...
			
			if (resp.equals("quit"))		stillQuerying = false; 
			else if(forkAt > -1)	return forkAt;	
			else if(getValidInd(resp, 99999) > -1)
				System.out.println(INV_RESP_MSG+". There are only "+(originalLastMoment+1)+" timesteps."); 
			else if(!resp.contains("get ") || resp.length() < 10)	System.out.println(INV_RESP_MSG);
			else if(resp.equals("get cascade"))
			{
				int ci = 0 , gsi = 0, bsi = 0,
						firstFork = proposedChanges.size() > 0 ? 
								originalLastMoment : Integer.parseInt(proposedChanges.get(0)[0]); 
				while ( ci < firstFork) 
				{
					if (gsi < NUM_GOLD_STAGES)
					{	if (hypGoldLocs[gsi] == ci)
						{	System.out.println("Gold stage "+gsi+": "+goldStageNames[gsi]); gsi++; }}
					else if (bsi < NUM_BLACK_STAGES)
					{	if (hypBlackLocs[bsi] == ci)
						{	System.out.println("Black stage "+bsi+": "+blackStageNames[bsi]); bsi++; }}
					
					System.out.println(ci+" : "+baseCASC.get(ci)); 
					ci += 1; 	
				}
				
				int pci = 0, hci = ci; 
				int nextFork = pci < proposedChanges.size() ? 
						Integer.parseInt(proposedChanges.get(pci)[0]) : originalLastMoment; 
						
				while (pci < proposedChanges.size())
				{
					assert hci == nextFork : "Error : should be at next fork moment but we are not"; 
					
					String currMod = proposedChanges.get(pci)[1]; 
					if (currMod.equals("deletion"))	
					{
						System.out.println("[DELETED RULE : "+baseCASC.get(ci).toString()); 
						ci++; 
					}
					else //insertion
					{
						System.out.println(hci+" [INSERTED] : "+hypCASC.get(hci));
						hci++; 
					}
					
					//then print all the rest until the next stopping point. 
					pci++; 
					nextFork = pci < proposedChanges.size() ? 
							Integer.parseInt(proposedChanges.get(pci)[0]) : originalLastMoment; 
					
					while (Math.max(ci, hci) < nextFork)
					{
						if (gsi < NUM_GOLD_STAGES)
						{	if (hypGoldLocs[gsi] == hci)
							{	System.out.println("Gold stage "+gsi+": "+goldStageNames[gsi]); gsi++; }}
						
						else if (bsi < NUM_BLACK_STAGES)
						{	if (hypBlackLocs[bsi] == hci)
							{	System.out.println("Black stage "+bsi+": "+blackStageNames[bsi]); bsi++; }}
						
						System.out.println(ci
								+(ci==hci ? "" : "->"+hci)
								+" : "+baseCASC.get(ci)); 
						ci++; hci++;
					}
					
					//then must be at last moment. 
					System.out.println(ci + (ci == hci ? "->"+hci : "")+ ": Last moment, after final rule and before output.") ;	
				}
			}
			else if(resp.equals("get lexicon"))
			{
				System.out.println("etymID"+STAGE_PRINT_DELIM+"Input"+STAGE_PRINT_DELIM+"Gold");
				for (int i = 0 ; i < NUM_ETYMA; i++)
					System.out.println(""+i+STAGE_PRINT_DELIM+baseSimulation.getInputForm(i)+STAGE_PRINT_DELIM+baseSimulation.getGoldOutputForm(i));
			}
			else
			{
				int cutPoint = 9; 
				if(resp.substring(0,9).equals("get rule ")) {
					if (resp.length() >= 13)
					{	if (resp.substring(9,12).equals("at "))	cutPoint = 12;
						else if (resp.length() < 17 ? false : resp.substring(9,16).equals("effect "))
							cutPoint = 16;
					}
					String entry = resp.substring(cutPoint); 
					if (cutPoint > 9)
					{
						int theInd = getValidInd(entry, baseCASC.size());
						if(cutPoint == 12)	printBaselineRuleAt(theInd);
						else /*curPoint == 16*/	if (theInd > -1)	baseSimulation.getRuleEffect(theInd); 
					}
					else
					{	boolean noMatches = true; 
						for(int ci = 0; ci < baseCASC.size(); ci++)
							if (baseCASC.get(ci).toString().contains(entry))
								System.out.println(""+ci+" : "+baseCASC.get(ci).toString());
						if(noMatches)	System.out.println("No matches found.");
					}
				}
				if(resp.substring(0,9).equals("get etym ")) {
					if (resp.length() >= 13)
					{	if (resp.substring(9,12).equals("at "))	cutPoint = 12;
						else if (resp.length() < 21 ? false : resp.substring(9,20).equals("derivation "))
							cutPoint = 20;
					}
					String entry = resp.substring(cutPoint); 
					if (cutPoint > 9)
					{
						int theInd = getValidInd(entry, NUM_ETYMA - 1); 
						if (cutPoint == 12 && theInd > -1)	System.out.println(baseSimulation.getInputForm(theInd));
						else /* cutPoint == 20 */ if (theInd > -1)	System.out.println(baseSimulation.getDerivation(theInd)); 
						else System.out.println("Error: invalid etymon index; there are only "+NUM_ETYMA+" etyma.\nReturning to forking test menu."); 
					}
					else
					{	LexPhon query = null; boolean validLexPhon = true;
						try {	query = new LexPhon(fac.parseSeqPhSeg(resp));	}
						catch (Exception e){
							System.out.println("Error: could not parse entered phone string. Returning to forking menu.");
							validLexPhon = false;
						}
						if(validLexPhon)
						{
							String inds = DiachronicSimulator.etymInds(baseSimulation.getInput().getWordList(), query);
							System.out.println("Ind(s) with this word as input : "+inds);  
						}
					}
				}
				else	System.out.println(INV_RESP_MSG);
			}	
		}
		
		return forkAt;
	}
	
	private void updateMappings(int forkAt, char mode, List<String[]> insertions)
	{
		if(mode == '3') //relocdation -- handled separately from others. 
		{
			int relocdate = Integer.parseInt(insertions.get(0)[0]); 
			boolean back = forkAt > relocdate;
			RULE_IND_MAP[forkAt] = relocdate; 
			for (int rimi = 0 ; rimi < RULE_IND_MAP.length; rimi++)
			{
				int curm = RULE_IND_MAP[rimi];
				if (back && rimi != forkAt)
				{	if ( curm >= relocdate && curm < forkAt )
						RULE_IND_MAP[rimi] = curm + 1 ; }
				else if (curm > forkAt && curm <= relocdate )
					RULE_IND_MAP[rimi] = curm - 1; 
			}
			if (NUM_GOLD_STAGES > 0)
				for (int gsi = 0 ; gsi < NUM_GOLD_STAGES; gsi++)
					if (hypGoldLocs[gsi] > Math.min(relocdate, forkAt))
						if (hypGoldLocs[gsi] < Math.max(relocdate, forkAt))
							hypGoldLocs[gsi] = hypGoldLocs[gsi] + (back?-1:1); 

			if (NUM_BLACK_STAGES > 0)
				for (int bsi = 0 ; bsi < NUM_BLACK_STAGES; bsi++)
					if (hypBlackLocs[bsi] > Math.min(relocdate, forkAt))
						if (hypBlackLocs[bsi] < Math.max(relocdate, forkAt))
							hypBlackLocs[bsi] = hypBlackLocs[bsi] + (back?-1:1); 
							
		}
		else if (mode == '2' && insertions.size() == 1) //single replacement modification
			return;
		else if (mode == '1') // deletion
		{
			RULE_IND_MAP[forkAt] = -1;
			for (int rimi = 0 ; rimi < RULE_IND_MAP.length; rimi++)
				if (RULE_IND_MAP[rimi] > forkAt)
					RULE_IND_MAP[rimi] = RULE_IND_MAP[rimi] - 1; 
			
			if (NUM_GOLD_STAGES > 0)
				for (int gsi = 0 ; gsi < NUM_GOLD_STAGES; gsi++)
					if (hypGoldLocs[gsi] >= forkAt)
						hypGoldLocs[gsi] -= 1; 

			if (NUM_BLACK_STAGES > 0)
				for (int bsi = 0 ; bsi < NUM_BLACK_STAGES; bsi++)
					if (hypBlackLocs[bsi] >= forkAt)
						hypBlackLocs[bsi] -= 1; 

		}
		else //insertion
		{
			int insertLoc = Integer.parseInt(insertions.get(0)[0]),
					increment = insertions.size(); 
			for (int rimi = 0 ; rimi < RULE_IND_MAP.length; rimi++)
				if (RULE_IND_MAP[rimi] > insertLoc)
					RULE_IND_MAP[rimi] = RULE_IND_MAP[rimi] + increment; 
			
			if (NUM_GOLD_STAGES > 0)
				for (int gsi = 0 ; gsi < NUM_GOLD_STAGES; gsi++)
					if (hypGoldLocs[gsi] >= insertLoc)
						hypGoldLocs[gsi] += increment; 

			if (NUM_BLACK_STAGES > 0)
				for (int bsi = 0 ; bsi < NUM_BLACK_STAGES; bsi++)
					if (hypBlackLocs[bsi] >= insertLoc)
						hypBlackLocs[bsi] += increment;
		}
	}

}
