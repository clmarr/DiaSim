import java.util.*; 

public class DHSAnalysis {
	/**
	 * Class for performing analysis operations on two Simulations in order to analyze the effect of a proposed change to the cascade
	 */
	private Simulation baseCascSim, hypCascSim; 
		// "baseline cascade" and "hypothesized cascase"
	private int[][] ruleCorrespondences; 
		// -1 in [0][x] -- deletion
		// -1 in [1][x] -- insertion
		// [0][same] and [1][same] -- cells contain numbers of
			//correspondant rules (if neither has -1 as value)
	 
	private HashMap<Integer,String> changedDerivations;
		// Integer type key is the OUTER NESTING INDEX in ruleCorrespondences
			// only etyma with changed derivations are included here.
			// String -- the DIFFERENTIAL DERIVATION -- will contain
				// First line --- <INPUT> \n
				// CONCORDANT DEVELOPMENT THROUUGH TO : <LAST CONCORDANT BASE RULE> | <LAST CONCORDANT HYP RULE> \n
				// for 1-to-1 divergent development : <BASERULE#>: <OLDFORM> > <NEWFORM> | <HYPRULE#>: NEW > OLD \n
				// deletion (i.e. occurs only in baseline: <BASERULE#>: <OLDFORM> > <NEWFORM> | -- \n
				// insertion: the reverse.
	
	private HashMap<Integer,List<String>[]>	changedRuleEffects; 
		// Integer type key is the OUTER NESTING INDEX in ruleCorrespondences
			// only rules with changed effects are included here.
			// List[]<String> contains (index = 0) strings for changes that are only in the baseline
				//and likewise with index 1 for hypCasc
			// these strings are simple X > Y (whole word forms, with changed entities surrounded by {})
	
	private int[] baseRuleIndsToGlobal, hypRuleIndsToGlobal; 
    // indices -- base/hyp rule indices
    // cells contain the index mapped to in ruleCorrespondences
            // i.e. the "global" index. 

		
	public DHSAnalysis(Simulation b, Simulation h, int[] baseToHypIndMap, List<String[]> propdChanges)
	{
		baseCascSim = b; hypCascSim = h ;
		computeRuleCorrespondences(baseToHypIndMap, propdChanges); //init ruleCorrespondences
		makeIndexGlobalizers(); // init baseRuleIndsToGlobal, hypRuleIndsToGlobal
		
		
	}
	
	//generate ruleCorrespondences
	
	private void computeRuleCorrespondences(int[] baseToHypIndMap, List<String[]> propdChanges)
	{
		//length of ruleCorrespondences should be: 
			// number of shared rules 
			// + number of deletions
			// + number of insertions
			// length of baseCasc = shared + deletions
			// length of hypCasc = shared + insertions
			// # of deletions also equals number of -1 values in baseToHypIndMap
		int total_length = hypCascSim.getTotalSteps(); 
		for (int bihimi : baseToHypIndMap)
			if (bihimi == -1)
				total_length += 1; 
		
		//TODO will probably have to debug around here...
		
		ruleCorrespondences = new int[2][total_length]; 
		int ri = 0 , bci = 0, hci = 0, pci = 0;
		
		while (ri < total_length)
		{
			if (hci == Integer.parseInt(propdChanges.get(pci)[0]))
			{
				if( propdChanges.get(pci)[1].equals("deletion"))
				{
					//TODO debugging -- commment out when no longer necessary
					assert baseToHypIndMap[bci] == -1 : "ERROR: inconsistency in stored info about a rule deletion operation"; 
					
					ruleCorrespondences[ri][0] = bci; 
					ruleCorrespondences[ri][1] = -1; 
					bci++;
				}
				else //insertion
				{
					ruleCorrespondences[ri][0] = -1;
					ruleCorrespondences[ri][1] = hci;
					hci++; 
				}
				pci++; 
			}
			else // rules should be corresponding
			{
				//TODO debugging
				assert baseToHypIndMap[bci] == hci : "ERROR: inconsistency about what should be a 1-to-1 rule correspondence"; 
				
				ruleCorrespondences[ri][0] = bci;
				ruleCorrespondences[ri][1] = hci;	
			}
			ri++; 
		}
		
		assert ri == total_length: "ERROR: some inconsistency in rule mapping operation coverage"; 
	}
	
	/**
	 * to initialize baseRuleIndsToGlobal, hypRuleIndsToGlobal
	 * @prerequisite: must have already called computeRuleCorrespondences
	 */
	private void makeIndexGlobalizers()
	{
		baseRuleIndsToGlobal = new int[baseCascSim.getTotalSteps()];
		hypRuleIndsToGlobal = new int[hypCascSim.getTotalSteps()];
		
		for (int rci = 0; rci < ruleCorrespondences[0].length; rci++)
		{
			if (ruleCorrespondences[0][rci] != -1)	
				baseRuleIndsToGlobal[ruleCorrespondences[0][rci]] = rci; 
			if (ruleCorrespondences[1][rci] != -1)
				hypRuleIndsToGlobal[ruleCorrespondences[1][rci]] = rci; 
		}
	}
	
	// fills changedDerivations and changedRuleEffects
	private void computeTrajectoryChange() 
	{
		//TODO debugging
		assert baseCascSim.NUM_ETYMA() == hypCascSim.NUM_ETYMA() :
			"ERROR: Inconsistent number of etyma between base and hypothesis cascade simulation objects"; 
		int n_ets = baseCascSim.NUM_ETYMA();  
		
		changedDerivations = new HashMap<Integer,String>(); 
		changedRuleEffects = new HashMap<Integer,List<String>[]>(); 
		
		for (int ei = 0 ; ei < n_ets ; ei++)
		{

		}
		
		
		
		
		//TODO here
		
	}
	
	
	/**
	 * auxiliary for computeTrajectoryChange()
	 * @param et_id -- etymon index, which should be consistent between the two Simulations. 
	 * @return @default an empty String ""- means there is no difference between the derivations
	 * 	otherwise: the DIFFERENTIAL DERIVATION, formed as follows: 
	 * // String will contain
				// First line --- <INPUT> \n
				// CONCORDANT DEVELOPMENT THROUUGH TO : <LAST CONCORDANT BASE RULE> | <LAST CONCORDANT HYP RULE> \n
				// for 1-to-1 divergent development : <BASERULE#>: <OLDFORM> > <NEWFORM> | <HYPRULE#>: NEW > OLD \n
				// deletion (i.e. occurs only in baseline: <BASERULE#>: <OLDFORM> > <NEWFORM> | -- \n
				// insertion: the reverse.
	 */ 
	private String getDifferentialDerivation(int et_id)
	{
		String baseDer= baseCascSim.getDerivation(et_id), hypDer = hypCascSim.getDerivation(et_id); 
		if (baseDer.equals(hypDer))	return ""; 
		// passing here does not exclude the possibility of an identical derivation
			// -- we will have to use ruleCorrespondences to ascertain that.
			// we do this by changing the rule index numbers in both derivations to their "global" indices in ruleCorrespondences
				// conveniently handled with mapping arrays
		
		
		
		
		
	}
	
	
	//TODO analysis methods follow
	// print nothing by default
	// if there are discrepancies in phonemic inventory, print them
		// TODO decide if we want to indicate this at stages and if so under what conditions should we do so
	private String reportChangeInPhInventory() 
	{
		
	}
	
	//TODO this.
	//return pair of lists
	// first is list of all phones in baseCasc's resulting lexicon but not in hypCasc's
	// second is the reverse. 
	private List[]<Phone> getInventoryDiscrepancies()
	{
	}
	
	private String derivationToGlobalInds(String der, boolean isHyp)
	{
		String[] lines = der.split("\n"); 
		String out = lines[0]; 
		for (String li : lines)
		{
			int br = li.indexOf(" | "),
					br2 = li.lastIndexOf(" : "); 
			if (br != -1 && br2 != -1)
			{
				out += "\n" + li.substring(0,br+3); 
				int raw_ind = Integer.parseInt(li.substring(br+3,br2));
				out += ""+(isHyp ? hypRuleIndsToGlobal : baseRuleIndsToGlobal)[ raw_ind ] + li.substring(br2); 
			}
		}
		return out; 
	}
	

}
