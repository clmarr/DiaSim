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
	
	private Simulation baseSimulation;
	private List<SChange> hypCASC, baseCASC; 
	// hypCASC -- new cascade that we are progressively constructing while continually comparing against the "baseline", @varbl baseCASC
	
	private List<String[]> proposedChanges; 
	//TODO important variable here, explanation follows
	// each indexed String[] is form [curr time step, operation details]
	// this object is *kept sorted* by current form index
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
		NUM_GOLD_STAGES, NUM_BLACK_STAGES;
	private String[] goldStageNames, blackStageNames; 
		
	
	
	public DHSWrapper(Simulation baseSim)
	{
		baseSimulation = baseSim;
		proposedChanges = new ArrayList<String[]>(); 
		propChNotes = new ArrayList<String>(); 
		hypCASC = new ArrayList<SChange>(baseSim.CASCADE()); 
		baseCASC = new ArrayList<SChange>(baseSim.CASCADE());
		originalLastMoment = baseCASC.size(); 
		NUM_GOLD_STAGES = baseSim.NUM_GOLD_STAGES();
		NUM_BLACK_STAGES = baseSim.NUM_BLACK_STAGES(); 
		RULE_IND_MAP = new int[originalLastMoment + 1]; 
		hypGoldLocs = new int[NUM_GOLD_STAGES]; hypBlackLocs = new int[NUM_BLACK_STAGES]; 
		for (int i = 0; i < originalLastMoment+1; i++)	RULE_IND_MAP[i] = i; //initialize each.
		for (int i = 0; i < NUM_GOLD_STAGES; i++)	hypGoldLocs[i] = baseSim.getStageInstant(true, i);
		for (int i = 0; i < NUM_BLACK_STAGES; i++)	hypBlackLocs[i] = baseSim.getStageInstant(false, i); 
		goldStageNames = baseSim.getGoldStageNames(); 
		blackStageNames = baseSim.getBlackStageNames();	
	}
	
	public void queryProposedChanges(Scanner inpu)
	{
		boolean queryMore = true; 
		String resp; 

		while (queryMore)
		{
			int forkAt = -1; 
			String currRuleOptions = "\t\t\t'get curr rule X', to get the index of any rules containing an entered string replacing <X>.\n"
					+ "\t\t\t'get curr rule at X', to get the rule at the original index number <X>.\n" 
					+ "\t\t\t'get curr rule effect X', to get all changes from a rule by the index <X>.\n";
			while(forkAt == -1 && queryMore) {
				
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
				
				if (resp.equals("quit"))	queryMore = false; 
				else if(forkAt > -1)	queryMore = true; //NOTE dummy stmt --  do nothing but continue on to next stage. 
				else if(getValidInd(resp, 99999) > -1)
					System.out.println(INV_RESP_MSG+". There are only "+(originalLastMoment+1)+" timesteps."); 
				else if(!resp.contains("get ") || resp.length() < 10)	System.out.println(INV_RESP_MSG);
				else if(resp.equals("get cascade"))
				{
				
				}
				
			}
			
			
		}
	}
		
	private int getValidInd(String s, int max)
	{
		return DiachronicSimulator.getValidInd(s, max); 
	}
	

}
