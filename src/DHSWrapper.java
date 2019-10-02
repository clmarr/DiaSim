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
	public final char STAGE_PRINT_DELIM = DiachronicSimulator.STAGE_PRINT_DELIM,
			CMT_FLAG = DiachronicSimulator.CMT_FLAG;
	public final int MAX_CMT_WIDTH = 150; 
	public final String HANGING_INDENT = "      "; 
	
	private Simulation baseSimulation; 
	private List<SChange> hypCASC, baseCASC; 
	// hypCASC -- new cascade that we are progressively constructing while continually comparing against the "baseline", @varbl baseCASC
	
	private List<String[]> proposedChanges; 
	//TODO important variable here, explanation follows
	// each indexed String[] is form [curr time step, operation details, comments to add to file at writing time]
	// this object is *kept sorted* by current (i.e. baseline) time step
		// (IMPORTANT: equivalent to iterator hci, for hypCASC later)
		// and time steps are kept updated as changes are made elsewhere
	// sorting is from earliest time step, to latest timestep 
	// operation may be either deletion or insertion 
	// both relocdation and modification are handled as deletion then insertion pairs. 
	// for deletion, the second slot simply holds the string "deletion"
	// whereas for insertion, the second index holds the string form of the SChange 
		// that is inserted there in hypCASC. 

	private int[] RULE_IND_MAP; //easy access maps indices of CASCADE to those in hypCASCADE.
		// the mapping is kept updated as new changes are added
		// -1 means deleted. 
	private int[] hypGoldLocs, hypBlackLocs; 
		// same as above for the locations of period waypoints. 
	
	private int originalLastMoment,
		NUM_GOLD_STAGES, NUM_BLACK_STAGES, NUM_ETYMA; 
	private String[] goldStageNames, blackStageNames;
	private boolean feats_weighted;
	private String[] featsByIndex;
	private double[] FT_WTS;
	private double id_wt; 
	public boolean stillQuerying; 
		
	private String origCascLoc; 
	
	public DHSWrapper(Simulation baseSim, boolean feats_weighted, String[] featsByIndex, double[] FT_WTS, double id_wt, String ogCascLoc)
	{
		baseSimulation = baseSim;
		proposedChanges = new ArrayList<String[]>(); 
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
		this.origCascLoc = ogCascLoc; 
	}
	
	public void queryProposedChanges(Scanner inpu, SChangeFactory fac)
	{
		stillQuerying = true; 
		String resp; 

		while (stillQuerying)
		{
			int forkAt = queryForkPoint(inpu, fac); 
			boolean toSetBehavior = true; 
			while(toSetBehavior) {
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
					int deleteAt = -1, addAt = -1; 
					String deletionNotes = "", propLaw = "", insertionNotes = ""; 
					List<SChange> propShifts = null;

					if ("123".contains(resp)) //operation involves deletion.
					{
						deleteAt = forkAt;
						String toRemove = hypCASC.get(deleteAt).toString();
						if(resp.equals("1"))	deletionNotes = "Former rule "+deleteAt+" [ "+toRemove+" ] simply removed."; 
						else if (resp.equals("2")) deletionNotes = "Former rule "+deleteAt+" [ "+toRemove+" ]"; // will have specific modification appended later.
						else //resp.equals("3"), relocdation
						{
							while(addAt == -1)
							{
								System.out.println("Enter the moment (rule index) would you like to move this rule to:");
								//TODO may have to standardize jargon here... 
								
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
									if (conf == 'y')	addAt = candiDate; 
								}
							}
							
							// unnecessary -- handled implicitly. 
							//if ( deleteAt > relocdate ) deleteAt--;
							//else	relocdate--; 
							
							deletionNotes = "Former rule "+deleteAt+" [ "+toRemove+" ] relocdated\n\tmoved to "+addAt; 
							propShifts = new ArrayList<SChange>(); 
							propShifts.add(hypCASC.get(deleteAt)); 
							insertionNotes = "Moved, originally at "+deleteAt; 	
						}
					}
					if ("02".contains(resp)) // all the operations that involve insertion of a NEW rule.
					{
						propShifts = new ArrayList<SChange>(); 
						while (propShifts.size() == 0)
						{
							System.out.println("Please enter the new law");
							propLaw = inpu.nextLine().replace("\n",""); 
							try 
							{	propShifts = fac.generateSoundChangesFromRule(propLaw);	}
							catch (Error e)
							{
								System.out.println("Preempted error : "+e); 
								System.out.println("You entered an invalid rule, clearly. All rules must be of form A -> B / X __ Y.");
								System.out.println("Valid notations aside from alpha functions include [] () ()* ()+ {;} # @ ,");
							}
						}
						if (resp.equals("2"))	{
							insertionNotes = "Usurping former rule: "+deletionNotes.substring(12);
							deletionNotes += " modified\nto: "+propLaw; 
						}
					}
				
					processSingleCh(deleteAt, deletionNotes, addAt, propLaw, propShifts, insertionNotes); 
					
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
			
			boolean gsstops = false; 
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
			
			DifferentialHypothesisSimulator DHScomp = gsstops ? generateDHSWithStops(inpu) : generateDHS(); 

			ErrorAnalysis ea = new ErrorAnalysis(baseSimulation.getCurrentResult(), baseSimulation.getGoldOutput(), featsByIndex,
					feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));
			ErrorAnalysis hea = new ErrorAnalysis(DHScomp.hypCascSim.getCurrentResult(), baseSimulation.getGoldOutput(), featsByIndex,
					feats_weighted ? new FED(featsByIndex.length, FT_WTS,id_wt) : new FED(featsByIndex.length, id_wt));
			System.out.println("Final output comparison for hypothesis simulation"); 
			DHScomp.printBasicResults();
			System.out.println("Overall accuracy : "+ea.getAccuracy()+" >>> "+hea.getAccuracy());
			System.out.println("Accuracy within 1 phone: "+ea.getPctWithin1()+" >>> "+hea.getPctWithin1());
			System.out.println("Accuracy within 2 phone: "+ea.getPctWithin2()+" >>> "+hea.getPctWithin2());
			System.out.println("Average edit distance per from gold phone: "+ea.getAvgPED()+" >>> "+hea.getAvgPED());
			System.out.println("Average feature edit distance from gold: "+ea.getAvgFED()+" >>> "+hea.getAvgFED());

			char choice = 'a';
			while (choice != '9')
			{
				System.out.println("What would you like to do? Please enter the appropriate number:"); 

				System.out.println(
						  "| 0 : Print parallel derivations for one word (by index) ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|\n"
						+ "| 1 : Print all etyma by index                                                        |\n"
						+ "| 2 : Automatically finalize these changes in the cascade [exits interface]           |\n"
						+ "| 3 : Generate cmts at places to change, to help manual editing [exits interface]     |\n"
						+ "| 4 : Display results again                                                           |\n"
						+ "| 9 : Return to main menu.____________________________________________________________|\n"); 
				choice = inpu.nextLine().charAt(0);

				if (choice == '4')
				{	System.out.println("Final output comparison for hypothesis simulation"); 
					DHScomp.printBasicResults();
					System.out.println("Overall accuracy : "+ea.getAccuracy()+" >>> "+hea.getAccuracy());
					System.out.println("Accuracy within 1 phone: "+ea.getPctWithin1()+" >>> "+hea.getPctWithin1());
					System.out.println("Accuracy within 2 phone: "+ea.getPctWithin2()+" >>> "+hea.getPctWithin2());
					System.out.println("Average edit distance per from gold phone: "+ea.getAvgPED()+" >>> "+hea.getAvgPED());
					System.out.println("Average feature edit distance from gold: "+ea.getAvgFED()+" >>> "+hea.getAvgFED());
					System.out.println("Enter anything to continue.");
					char dum = inpu.nextLine().charAt(0); 
					System.out.println("\nWould you like to do anything else?"); 
				}
				else if (choice == '2' || choice == '3')
				{
					String toFileOut = ""; 
					
					while (toFileOut.length() == 0) {
						try 
						{
							if(choice == '2')
								System.out.println("Preparing to automatically implement proposed changes to cascade..."); 
							else // choice == 3
								System.out.println("Placing comments to facilitate manual editing of cascade..."); 
							System.out.println("First, explanatory comments must be entered for the changes..."); 
							
							List<String> editComments = new ArrayList<String>();
							
							for (int pci = 0; pci < proposedChanges.size(); pci++) {
								String[] ipc = proposedChanges.get(pci); 
								
								//TODO check this _v_
								//note: no comments are entered for the insertion part of rule modification, 
									// which, unlike simple deletion, implies a non-empty corresponding entry in index [2]
									// in this way, the system will be able to recognize such cases due to the explanatory comment 
											//being empty
										// in all other cases, empty explanations are strictly forbidden
								String justification = ""; 

								if(ipc[1].equals("deletion") || proposedChanges.get(0)[2].length() == 0)
								{	
									while (justification.equals(""))
									{
										System.out.println("Please enter an explanatory comment for this change : ");
										
										if(ipc[1].equals("deletion"))
											System.out.println(proposedChanges.get(0)[2]); 
										else if (proposedChanges.get(0)[2].length() == 0)
											System.out.println("Simple insertion of "+ipc[1]); 
										justification = inpu.nextLine().replace("\n",""); 
										if (justification.equals(""))
											System.out.println("You must enter a comment to describe your change."); 
									}
									
									//now perform line breaks as appropriate for long comments...
									justification = commentJustify(justification); 
								}
								editComments.add(justification); 
							}
							
							toFileOut = DHScomp.newCascText( editComments, choice == '3', origCascLoc, fac);
						}
						catch (MidDisjunctionEditException e ) 
						{
							System.out.print(e.getMessage());
							System.out.println("Instead, we are making file with comments of cues for where and how to make these changes."); 
								//TODO in future, come up with better behavior to handle this situation... 
							choice = '3'; 
						}
					
					}
					
					String fileDest = ""; 
					while (fileDest.equals("")) {
						System.out.println("Please enter what you want to save your new cascade as:");
						fileDest = inpu.nextLine().replace("\n",""); 
						if (fileDest.equals("") || fileDest.equals(origCascLoc))
							System.out.println("You must enter a file destination, and it must be distinct from the original cascade's location."); 
								//TODO once we have finished debugging, allow initial file's location to be used.
					}
					DiachronicSimulator.writeToFile(fileDest, toFileOut); 
				}
				else if (choice == '1')
				{
					System.out.println("Printing all etyma as ID#, INPUT, BASELINE RESULT, HYP SIM RESULT, GOLD RESULT");
					for (int eti = 0 ; eti < NUM_ETYMA ; eti++)
						System.out.println(""+eti+", "+baseSimulation.getInputForm(eti)+", "+baseSimulation.getCurrentForm(eti)+", "
								+ DHScomp.hypCascSim.getCurrentForm(eti)+", "+baseSimulation.getGoldOutputForm(eti)); 
					System.out.println("Enter anything to continue.");
					char dum = inpu.nextLine().charAt(0); 
					System.out.println("\nWould you like to do anything else?"); 
				}
				else if (choice == '0')
				{
					int theID = -1; 
					while(theID == -1)
					{
						System.out.println("Please enter the index of the etymon that you would like to query:"); 
						String idstr = inpu.nextLine(); 
						theID = getValidInd(idstr, NUM_ETYMA - 1) ; 
						if (theID == -1)	
							System.out.println("Error -- there are only "+NUM_ETYMA+" etyma. Returning to query menu."); 
					}
					System.out.println(DHScomp.getDifferentialDerivation(theID)); 
					System.out.println("Enter anything to continue.");
					char dum = inpu.nextLine().charAt(0); 
					System.out.println("\nWould you like to do anything else?"); 
				}
				else if (choice != '9') //must have been not one of the listed numbers. 
					System.out.println("Invalid entry, please enter one of the listed numbers:"); 
			}

			
		}
		
		//TODO ??? here? ??? 
	}
	
	public DifferentialHypothesisSimulator generateDHS()
	{
		Simulation hypEmpiricized = new Simulation (baseSimulation, hypCASC); 
		hypEmpiricized.simulateToEnd();
		return new DifferentialHypothesisSimulator(baseSimulation, hypEmpiricized, RULE_IND_MAP , proposedChanges ); 
	}

	
	public DifferentialHypothesisSimulator generateDHSWithStops(Scanner inpu)
	{
		Simulation hypEmpiricized = new Simulation (baseSimulation, hypCASC); 
		
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

		return new DifferentialHypothesisSimulator(baseSimulation, hypEmpiricized, RULE_IND_MAP , proposedChanges );  
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

	
	//TODO need to fix this so that we are operating on hypCASC.
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
	
	
	/** updateProposedChanges
	 * @param ch -- single proposed deletion or insertion (NOT both simultaneously)
	 * @param quantity -- number of rules being added, if we are adding; if this is a deletion this should be -1.
	 */
	private void updateProposedChanges(String[] ch, int quantity)
	{
		assert ch[1].equals("deletion") == (quantity == -1) : "Error: @param quantity should be null if and only if we are doing a deletion"; 

		boolean deleteMode = ch[1].equals("deletion"); 
		boolean foundTargSpot = false; 
		int target = Integer.parseInt(ch[0]); 
		int pci = proposedChanges.size(); 
		while (pci == 0 ? false : foundTargSpot)
		{
			String[] prevCh = proposedChanges.get(pci - 1); 
			int prevLoc = Integer.parseInt(prevCh[0]); 
			foundTargSpot = target < (prevLoc + (deleteMode ? 1 : 0)); 
			if (!foundTargSpot)
			{
				pci--; 
				proposedChanges.set(pci, new String[] { "" + (prevLoc + quantity ) , prevCh[1], prevCh[2] } ); 
			} 
		}
		if (pci == proposedChanges.size() )	proposedChanges.add(ch); 
		else	proposedChanges.add(pci, ch); 
		
	}

	
	/** TODO WRITE THIS COMMENT BLOCK 
	 * 
	 */
	public void processSingleCh(int deleteLoc, String deletionNotes, int addLoc, String newLaw, List<SChange> newRules, String insertionNotes)
	{
		List<SChange> insertions = (newRules == null) ? new ArrayList<SChange>() : new ArrayList<SChange>(newRules); 
		if (deleteLoc != -1)
		{
			SChange removed = hypCASC.remove(deleteLoc);
			updateProposedChanges(new String[] {""+deleteLoc, "deletion", deletionNotes }, -1 ); 
			
			if(addLoc != -1) //relocdation or modification
			{
				if (addLoc == deleteLoc) // modification
					assert insertions.size() > 0 : "Error: @param newRules cannot be null or empty if we are doing a modification operation";
				
				
				else //relocdation 
				{
					assert insertions.size() == 0 : "Error: @param newRules must be null or empty if we are doing a relocdation operation"; 
					insertions.add(removed);
				}
				hypCASC.addAll(addLoc, insertions); 
				
				updateProposedChanges(
					new String[] {""+addLoc, (addLoc == deleteLoc) ? newLaw : removed.toString(), insertionNotes}, 
					insertions.size() ) ;	
			}
			
			//now handle the mapping structures.
			if (insertions.size() == 1) //must be relocdation. 
			{
				boolean back = deleteLoc > addLoc;
				RULE_IND_MAP[deleteLoc] = addLoc; 
				for (int rimi = 0 ; rimi < RULE_IND_MAP.length; rimi++)
				{
					int curm = RULE_IND_MAP[rimi]; 
					if (curm == deleteLoc)	RULE_IND_MAP[rimi] = -1; 
					else if (back) 
						if (curm >= addLoc && curm < deleteLoc)
							RULE_IND_MAP[rimi] = curm + 1;
					else /*!back*/ if (curm > deleteLoc && curm <= addLoc)
						RULE_IND_MAP [rimi] = curm - 1; 
				}
				
				if (NUM_GOLD_STAGES > 0)
					for (int gsi = 0 ; gsi < NUM_GOLD_STAGES; gsi++)
						if (hypGoldLocs[gsi] > Math.min(addLoc, deleteLoc))
							if (hypGoldLocs[gsi] < Math.max(addLoc, deleteLoc))
								hypGoldLocs[gsi] = hypGoldLocs[gsi] + (back?-1:1); 

				if (NUM_BLACK_STAGES > 0)
					for (int bsi = 0 ; bsi < NUM_BLACK_STAGES; bsi++)
						if (hypBlackLocs[bsi] > Math.min(addLoc, deleteLoc))
							if (hypBlackLocs[bsi] < Math.max(addLoc, deleteLoc))
								hypBlackLocs[bsi] = hypBlackLocs[bsi] + (back?-1:1); 
			}
			else //simple deletion or modification 
			{
				int increment = insertions.size() -1 ; 
					
				if (increment != 1)
				{
					for (int rimi = 0 ; rimi < RULE_IND_MAP.length; rimi++) 
					{
						int curm = RULE_IND_MAP[rimi];
						if (curm == deleteLoc)	RULE_IND_MAP[rimi] = -1; 
						else if (curm > deleteLoc)	RULE_IND_MAP[rimi] = curm + increment;
						// else it is too early to be effected, so do nothing.
					}
					if (NUM_GOLD_STAGES > 0)
						for (int gsi = 0 ; gsi < NUM_GOLD_STAGES; gsi++)
							if (hypGoldLocs[gsi] >= deleteLoc)
								hypGoldLocs[gsi] += increment;
					if (NUM_BLACK_STAGES > 0)
						for (int bsi = 0 ; bsi < NUM_BLACK_STAGES; bsi++)
							if (hypBlackLocs[bsi] >= deleteLoc)
								hypBlackLocs[bsi] += increment;			
				}
			}	
		}
		
		else // must be simple insertion of one or more rules.
		{
			hypCASC.addAll(addLoc, insertions); 
			updateProposedChanges(new String[] { ""+addLoc, newLaw, insertionNotes}, insertions.size()); 
			for (int rimi = 0 ; rimi < RULE_IND_MAP.length; rimi++)
				if (RULE_IND_MAP[rimi] >= addLoc)
					RULE_IND_MAP[rimi] += insertions.size(); 
			if (NUM_GOLD_STAGES > 0)
				for (int gsi = 0 ; gsi < NUM_GOLD_STAGES; gsi++)
					if (hypGoldLocs[gsi] >= addLoc)
						hypGoldLocs[gsi] += insertions.size();
			if (NUM_BLACK_STAGES > 0)
				for (int bsi = 0 ; bsi < NUM_BLACK_STAGES; bsi++)
					if (hypBlackLocs[bsi] >= addLoc)
						hypBlackLocs[bsi] += insertions.size();			
			
		}
	}
	
	private String commentJustify(String cmt)
	{
		String[] tokenized = cmt.substring(cmt.indexOf(" ")+1).split(" "); 
		
		String out = ""+CMT_FLAG+cmt.substring(0, cmt.indexOf(" ")); 
		for (String token : tokenized)
		{
			if (out.length() + token.length() + 1 > MAX_CMT_WIDTH)
				out += "\n"+HANGING_INDENT+CMT_FLAG+token; 
			else	out += " "+token; 
		}
		
		return out; 
		
	}

}
