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
		// Integer type key is the GLOBAL RULE INDEX, as used in ruleCorrespondences
			// only etyma with changed derivations are included here.
			// String -- the DIFFERENTIAL DERIVATION -- will contain
				// First line --- <INPUT> \n
				// CONCORDANT DEVELOPMENT THROUUGH TO : <LAST CONCORDANT BASE RULE> | <LAST CONCORDANT HYP RULE> \n
				// for 1-to-1 divergent development : <BASERULE#>: <OLDFORM> > <NEWFORM> | <HYPRULE#>: NEW > OLD \n
				// deletion (i.e. occurs only in baseline: <BASERULE#>: <OLDFORM> > <NEWFORM> | -- \n
				// insertion: the reverse.
	
	private int divergencePoint; 
		// i.e. first global time step at which at least one word's resultant form is different between the two cascades. 
		// this is distinct from the first "domino" as returned by locateFirstDomino() because that (a)
			// pertains to only one word and (b) is specifically the first time the effect of a rule 
				// that is not one of the ones stipulated by the proposed changes 
				// is effected -- i.e. a bleeding or feeding effect. 
	
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
	
	private List<String[]> proposedChs; 

	public DHSAnalysis(Simulation b, Simulation h, int[] baseToHypIndMap, List<String[]> propdChanges)
	{
		baseCascSim = b; hypCascSim = h ;
		proposedChs = propdChanges; 
		computeRuleCorrespondences(baseToHypIndMap); //init ruleCorrespondences
		makeIndexGlobalizers(); // init baseRuleIndsToGlobal, hypRuleIndsToGlobal
		computeTrajectoryChange(); // changedRuleEffects, changedDerivations. 
	}
	
	//generate ruleCorrespondences
	
	private void computeRuleCorrespondences(int[] baseToHypIndMap)
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
			if (hci == Integer.parseInt(proposedChs.get(pci)[0]))
			{
				if( proposedChs.get(pci)[1].equals("deletion"))
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
	
	/**fills changedDerivations and changedRuleEffects, and sets divergence point. 
	 * auxiliary for computeTrajectoryChange()
	 * @param et_id -- etymon index, which should be consistent between the two Simulations. 
	 * @return @default an empty String ""- means there is no difference between the derivations
	 * 	otherwise: the DIFFERENTIAL DERIVATION, formed as follows: 
	 * // String will contain
				// First line --- <INPUT> \n
				// CONCORDANT UNTIL : <LAST CONCORDANT BASE RULE> | <LAST CONCORDANT HYP RULE> \n
				// for 1-to-1 divergent development : <GLOBALRULE#> : [BASERULE#] <OLDFORM> > <NEWFORM> | [HYPRULE#] NEW > OLD \n
				// deletion (i.e. occurs only in baseline: <BASERULE#>: <OLDFORM> > <NEWFORM> | -- \n
				// insertion: the reverse.
	 */ 
	private void computeTrajectoryChange() 
	{
		divergencePoint = -1; // will remain -1 if two cascades never diverge for a single word.
		
		//TODO debugging
		assert baseCascSim.NUM_ETYMA() == hypCascSim.NUM_ETYMA() :
			"ERROR: Inconsistent number of etyma between base and hypothesis cascade simulation objects"; 
		int n_ets = baseCascSim.NUM_ETYMA();  
		
		changedDerivations = new HashMap<Integer,String>(); 
		changedRuleEffects = new HashMap<Integer,List<String>[]>(); 
		
		for (int ei = 0 ; ei < n_ets ; ei++)
		{
			//TODO will need to debug here...
			
			String ddHere = getDifferentialDerivation(ei); 
			if (!"".contains(ddHere))
			{
				changedDerivations.put(ei, ddHere);
				ddHere = ddHere.substring(ddHere.indexOf("CONCORD")); 
				
				//lexical divergence point extract here -- see if it causes the overall initial divergence point to be earlier.
				int lexDivPt = Math.max(Integer.parseInt(ddHere.substring(ddHere.indexOf(":")+1, ddHere.indexOf("|"))),
						Integer.parseInt(ddHere.substring(ddHere.indexOf("|")+1,ddHere.indexOf("\n"))));
						// we're taking the max because one of the two between the global base and hyp inds here
								// could be -1 (actually, one usually is.)
				if (divergencePoint == -1)	divergencePoint = lexDivPt; 
				else	divergencePoint = Math.min(divergencePoint, lexDivPt); 
				
				ddHere = ddHere.substring(ddHere.indexOf("\n")+"\n".length());
				for (String ddl : ddHere.split("\n"))
				{
					if(ddl.contains(">"))
					{
						int globInd = Integer.parseInt(ddl.substring(0, ddl.indexOf("["))); 
						String[] effs = ddl.substring(ddl.indexOf(": "+2)).split(" | "); 
						boolean[] hit = new boolean[] { effs[0].contains(">"), effs[1].contains(">")}; 
						if (hit[0] != hit[1])
						{
							if(changedRuleEffects.containsKey(globInd))
							{	//TODO check this.
								List<String>[] valHere = changedRuleEffects.get(globInd); 
								if(hit[0])	valHere[0].add(""+ei+": "+effs[0]);
								else	valHere[1].add(""+ei+": "+effs[1]); 
								changedRuleEffects.put(globInd, valHere); 
							}
							else //TODO check this
								changedRuleEffects.put(globInd, new List[] {
										hit[0] ? Arrays.asList(new String[] {""+ei+": "+effs[0]}) : new ArrayList<String>(), 
												hit[1] ? Arrays.asList(new String[] {""+ei+": "+effs[1]}) : new ArrayList<String>() } );
						}

					}
				}
			}
		}
	}

	private String getDifferentialDerivation(int et_id)
	{
		String baseDer= baseCascSim.getDerivation(et_id), hypDer = hypCascSim.getDerivation(et_id); 
		if (baseDer.equals(hypDer))	return "";
		// passing here does not exclude the possibility of an identical derivation
			// -- we will have to use ruleCorrespondences to ascertain that.
			// we do this by changing the rule index numbers in both derivations to their "global" indices in ruleCorrespondences
				// conveniently handled with mapping arrays
		
		baseDer= derivationToGlobalInds(baseDer, false); 
		hypDer = derivationToGlobalInds(hypDer, true); 
		
		if(baseDer.equals(hypDer))	return "";
		//now we know they are indeed different -- so fill in info on how... 
		
		String[] bdlines = baseDer.split("\n"), hdlines = hypDer.split("\n"); 
		
		//TODO debugging
		assert bdlines[0].equals(hdlines[0]) : "Error: inconsistent initial line between two corresponding lexical derivations" ;
		String out = bdlines[0]; 
		int bdli = 1, hdli = 1 ; //skip first line as we know it must be identical
		
		
		//determine how long the developments remain concordant 
		while ( bdli < bdlines.length && hdli < hdlines.length && bdlines[bdli].equals(hdlines[hdli]) )
		{
			bdli++; hdli++; 
			
			//skip any stage declaration lines -- but only if we don't have a case of deletion/insertion at the end, to avoid an invalid index error.
			if (bdli < bdlines.length && hdli < hdlines.length)
			{	if (bdlines[bdli].contains(" stage form : ") && hdlines[hdli].contains(" stage form : ")) {
					bdli++; hdli++; }	}
		}
		
		String lastBform = "" , lastHform = "";
		
		if (bdli == 1 ) { //hdli will equal 1 too othen		
			lastBform = bdlines[0]; lastHform = hdlines[0];	}
		else	{
			lastBform = bdlines[bdli-1].substring(0, bdlines[bdli-1].indexOf(" | "));
			lastHform = hdlines[hdli-1].substring(0, hdlines[hdli-1].indexOf(" | ")); 
		}

		// we know next line cannot be gold/black stage announcement as that could not be the first line with divergence. 
		
		int nextGlobalBaseInd = Integer.parseInt(bdlines[bdli].substring(bdlines[bdli].indexOf(" | ")+3, bdlines[bdli].indexOf(" : "))),
				nextGlobalHypInd = Integer.parseInt(hdlines[hdli].substring(hdlines[hdli].indexOf(" | ")+3, hdlines[hdli].indexOf(" : ")));
		
		out += "\nCONCORDANT UNTIL RULE : "+nextGlobalBaseInd+" | "+nextGlobalHypInd; 
			//TODO wait.. aren't these the same? 
		
		// recall -- we have already aligned the numbers in the two derivations using derivationToGlobalInds()
		// so it is obvious if we are dealing with a deletion or insertion as it is simply absent on the other side.
		
		// we now will no longer necessarily be iterating simultaneously on bdlines and hdlines
		// we only do so when they have hte same rule (same = same global index)
		// when the index is not shared, we are handling a case of deletion/insertion, 
			// or bleeding/feeding as a result of the rule change.
		
		while ( bdli < bdlines.length && hdli < hdlines.length)
		{
			int[] stageHere = new int[] { bdlines[bdli].indexOf(" stage form : "), 
							hdlines[hdli].indexOf(" stage form : ")}; 
			
			if (stageHere[0]== -1)
				nextGlobalBaseInd = Integer.parseInt(bdlines[bdli].substring(bdlines[bdli].indexOf(" | ")+3, bdlines[bdli].indexOf(" : ")));
			if (stageHere[1]== -1)
				nextGlobalHypInd = Integer.parseInt(hdlines[hdli].substring(hdlines[hdli].indexOf(" | ")+3, hdlines[hdli].indexOf(" : ")));
		
			if (stageHere[0] > -1 && stageHere[1] > -1)
			{
				out += "\n"+bdlines[bdli].substring(0, stageHere[0])+bdlines[bdli].substring(stageHere[0]+12)+" | "
						+ hdlines[hdli].substring(0, stageHere[1])+hdlines[hdli].substring(stageHere[1]+12); 
				bdli++; hdli++; 
			}
			else if (nextGlobalBaseInd == nextGlobalHypInd) { // effects of same rule 
				String nextBform = bdlines[bdli].substring(0, bdlines[bdli].indexOf(" | ")),
						nextHform = hdlines[hdli].substring(0, hdlines[hdli].indexOf(" | "));
				
				out += "\n"+nextGlobalBaseInd+"["+ruleCorrespondences[0][nextGlobalBaseInd]
						+"|"+ruleCorrespondences[1][nextGlobalHypInd]+" : "
						+lastBform+" > "+nextBform+" | "+lastHform+" > "+nextHform;
				lastBform = nextBform; 
				lastHform = nextHform; 
				bdli++; hdli++; 	
			}
			else if (stageHere[1] == -1? true : nextGlobalBaseInd < nextGlobalHypInd) //deletion or bleeding
			{
				String nextBform = bdlines[bdli].substring(0, bdlines[bdli].indexOf(" | "));
				
				out += "\n"+nextGlobalBaseInd+"["+ruleCorrespondences[0][nextGlobalBaseInd]
						+ "|-1] : "+lastBform+" > "+nextBform+" | bled or deleted"; 
				bdli++; 
				lastBform = nextBform; 
			}
			else //insertion or feeding
			{
				String nextHform = hdlines[hdli].substring(0, hdlines[hdli].indexOf(" | ")); 
				out += "\n"+nextGlobalHypInd+"[-1|"+ruleCorrespondences[1][nextGlobalHypInd]+"] : "
						+ "fed or inserted | "+lastHform+" > "+nextHform; 
				hdli++; 
				lastHform = nextHform; 
			}
		}
		return out; 
		
	}
	
	//TODO plans to report any change in phonemic inventory.
	
	//TODO need to check that this works properly
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
	
	//TODO this
	// prints basic info on changes in words effected 
		// and rules effected
		// does not print evaluation statistic change -- that is for DiachronicSimulation and ErrorAnalysis to handle. 
	public void printBasicResults()
	{
		System.out.println("ANALYSIS OF EFFECT OF PROPOSED CHANGES:\n");
		System.out.println("Other rules effected:\n"); 
		for (int globInd : changedRuleEffects.keySet())
		{
			if (ruleCorrespondences[1][globInd] != -1)
			{
				int baseInd= ruleCorrespondences[0][globInd]; 
				if (baseInd > -1)
					System.out.println("Baseline rule "+baseInd+": "+baseCascSim.getRuleAt(baseInd)+"\n\t"
							+ "bled for "+changedRuleEffects.get(globInd)[0].size()+", fed for "
									+ changedRuleEffects.get(globInd)[1].size()+"."); 
			}
		}
		
		boolean prgold = baseCascSim.hasGoldOutput(); 
		
		System.out.println("\nEtyma effected: (#: BASE>HYP"+(prgold ? "[GOLD]" : "")+")"); 
		
		HashMap<Integer, List<Integer>> classedChdEts = changedEtsClassifiedByFirstDomino(); 
		
		for (List<Integer> ets : classedChdEts.values())
		{
			for (int et : ets)
			{
				System.out.print(""+et+": "); 
				String thisdd = changedDerivations.get(et); 
				String lastline = thisdd.substring(thisdd.lastIndexOf("\n")+"\n".length()); 
				System.out.println(lastline.substring(thisdd.indexOf(">")+2, lastline.indexOf("|")-1)+" >> "
						+ lastline.substring(lastline.lastIndexOf(">")+2, lastline.length())+"; ");
					//TODO beautify this?
			}
			
		}
		
	}
	
	// @ precondition; have called computeTrajectoryChanges()
	private HashMap<Integer, List<Integer>> changedEtsClassifiedByFirstDomino()
	{
		HashMap<Integer, List<Integer>> out = new HashMap<Integer, List<Integer>>(); 
		for (int ei : changedDerivations.keySet())
		{
			int fd = locateFirstDomino(ei); 
			if (out.containsKey(fd)) 
			{
				List<Integer> val = out.get(fd); 
				val.add(ei); 
				out.put(fd, val); 
			}
			else	out.put(fd, new ArrayList<Integer> (Arrays.asList(Integer.valueOf(ei))));
		}
		return out; 
	}
	
	// if there is at least one bleeding or feeding effect on this etID, return global ind for it
	// if the et has no changes, return -1
	// otherwise return the global ind of the first effectual proposed change for this etymon.
	private int locateFirstDomino(int etID)
	{
		if (!changedDerivations.containsKey(etID))	return -1; 
		String dd = changedDerivations.get(etID); 
		dd = dd.substring(dd.indexOf("CONC"));
		dd = dd.substring(dd.indexOf("\n")+"\n".length()); 

		//we know the next line must be the proposed change
		int ogi = Integer.parseInt(dd.substring(0,dd.indexOf("[")));
		int gi = ogi; 
		dd = dd.substring(dd.indexOf("\n")+"\n".length()); 
		
		
		while(dd.contains("\n"))
		{
			while (dd.contains("\n") ? 
					!dd.substring(0,dd.indexOf("\n")).contains(">") : false)
				dd = dd.substring(0, dd.indexOf("\n")+"\n".length()); 
			if (dd.contains("\n"))
			{
				gi =  Integer.parseInt(dd.substring(0,dd.indexOf("[")));
				String[] effs = dd.substring(dd.indexOf(": ")+2, dd.indexOf("\n")).split(" | "); 
				if (effs[0].contains(">") != effs[1].contains(">")) //either bleeding/feeding or insertion/deletion
					if ( ruleCorrespondences[0][gi] != -1 && ruleCorrespondences[1][gi] == -1) 
						// i.e. if false this is an insertion or deletion, i.e. one of the proposed changes. 
						return gi; 
				dd = dd.substring(0, dd.indexOf("\n")+"\n".length()); 
			}
		}
		
		gi =  Integer.parseInt(dd.substring(0,dd.indexOf("[")));
		String[] effs = dd.substring(dd.indexOf(": ")+2, dd.indexOf("\n")).split(" | "); 
		if (effs[0].contains(">") != effs[1].contains(">")) //either bleeding/feeding or insertion/deletion
			if ( ruleCorrespondences[0][gi] != -1 && ruleCorrespondences[1][gi] == -1) 
				// i.e. if false this is an insertion or deletion, i.e. one of the proposed changes. 
				return gi; 
		
		return ogi; 
	}
	
	//-1 if they never diverge
	public int getDivergencePoint()
	{
		return divergencePoint; 
	}
	

}
