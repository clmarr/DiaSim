import java.util.List;


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
	
	public DHSAnalysis(Simulation b, Simulation h, int[] baseToHypIndMap, List<String[]> propdChanges)
	{
		baseCascSim = b; hypCascSim = h ;
		
		//generate ruleCorrespondences
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
				assert baseToHypIndMap[bci] == hci : "ERROR: inconsistency about what should be a 1-to-1 rule correspondence"; 
				
				ruleCorrespondences[ri][0] = bci;
				ruleCorrespondences[ri][1] = hci;	
			}
			ri++; 
		}
		
		assert ri == total_length: "ERROR: some inconsistency in rule mapping operation coverage"; 
		
	}
	
	 
	//TODO analysis methods follow
	// print nothing by default
	// if there are discrepancies in phonemic inventory, print them
		// TODO decide if we want to indicate this at stages and if so under what conditions should we do so
	private String changeInPhInventory() 
	{
		
	}
	
	//TODO this.
	//return pair of lists
	// first is list of all phones in baseCasc's resulting lexicon but not in hypCasc's
	// second is the reverse. 
	private List[]<Phone> getInventoryDiscrepancies()
	{
	}
	

}
