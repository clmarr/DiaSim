import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class DifferentialHypothesisSimulator {
	/**
	 * Class for performing analysis operations on two Simulations in order to analyze the effect of a proposed change to the cascade
	 */
	
	//finals
	public final static String HANGING_INDENT = "      "; 
	public static final char CMT_FLAG = UTILS.CMT_FLAG; 
	
	//target variables needing tracked for any thorough debugging procedure
	public Simulation baseCascSim, hypCascSim; 
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
	
	//TODO need to reform this...
	private HashMap<Integer,String[][]>	changedRuleEffects; 
		// Integer type key is ruleCorrespondences's inner nesting index (i.e. the "global index") 
			// only rules with changed effects are included here.
		// String[2][NUM_ETYMA] value has all empty cells by default.
			// outer nesting 0 is for the baseline , 1 is for alt hyp
			//inner nesting is by global etymon index
		// cells are filled when and only when we find 
			// changes that are only in the baseline/hyp are found
		// these strings are simple X > Y 
		// (whole word forms)
			//TODO : enclose changed phonemes with {} 
	
	private int[] baseRuleIndsToGlobal, hypRuleIndsToGlobal; 
    // indices -- base/hyp rule indices
    // cells contain the index mapped to in ruleCorrespondences
            // i.e. the "global" index. 
	
	private List<String[]> proposedChs; 
		//TODO important variable here, explanation follows
		// each indexed String[] is form [curr time step, operation details]
		// this object is *kept sorted* by current form index
			// (IMPORTANT: equivalent to iterator hci, for hypCASCADE later)
		// operation may be either deletion or insertion 
		// both relocdation and modification are handled as deletion then insertion pairs. 
		// for deletion, the second slot simply holds the string "deletion"
		// whereas for insertion, the second index holds the string form of the SChange 
			// that is inserted there in hypCASCADE. 
	
	private boolean[] locHasPrCh;
		// one cell for each *global* rule index -- i.e. the inner nesting in ruleCorrespondences
		// by default, false
		// true only for those indices of those operations which are either deleted or added 
			//as part of the transformation of the baseline cascade to the hypothesis cascade. 
	
	public DifferentialHypothesisSimulator(Simulation b, Simulation h, int[] baseToHypIndMap, int[] hypToBaseIndMap, List<String[]> propdChanges)
	{
		baseCascSim = b; hypCascSim = h ;
		proposedChs = propdChanges; 
		computeRuleCorrespondences(baseToHypIndMap, hypToBaseIndMap); //init ruleCorrespondences
		
		//TODO debugging
		System.out.println("RCs[0] : "+UTILS.print1dIntArr(ruleCorrespondences[0]));
		System.out.println("RCs[1] : "+UTILS.print1dIntArr(ruleCorrespondences[1]));
		
		
		makeIndexGlobalizers(); // init baseRuleIndsToGlobal, hypRuleIndsToGlobal
		
		computeTrajectoryChange(); // changedRuleEffects, changedDerivations. 
	}
	
	//generate ruleCorrespondences and prChLocs
	// ruleCorrespondences -- each int[] (length = 2) instance in RELOCDS is [deleteLoc, addLoc)
	private void computeRuleCorrespondences(int[] baseToHypIndMap, int[] hypToBaseIndMap) 
	{
		// dummy versions that we will modify by placing -2 as a way of "crossing out" cells we have operated upon. 
		int[] dumRIMBH = new int[baseToHypIndMap.length], dumRIMHB = new int[hypToBaseIndMap.length];
		for (int bhi = 0 ; bhi < dumRIMBH.length; bhi++)	dumRIMBH[bhi] = baseToHypIndMap[bhi];
		for (int hbi = 0 ; hbi < dumRIMHB.length; hbi++)	dumRIMHB[hbi] = hypToBaseIndMap[hbi];
		
		if ( proposedChs.size() == 0 )	
		{
			assert UTILS.compare1dIntArrs(baseToHypIndMap, hypToBaseIndMap): 
				"ERROR: no proposed changes detected, but baseToHypIndMap and hypToBaseIndMap differ"; 
			ruleCorrespondences = new int[][] { baseToHypIndMap, hypToBaseIndMap};  
		}
		else
		{
			/**
			 * ruleCorrespondences -- tracked rule pairs in hyp and base cascs share the same INNER index
			 * 		a rule in baseCasc is tracked unless it is deleted or non-bijectively modified
			 * 			likewise one in hypCasc is unless it was inserted, or the result of non-bijective modification.
			 * ordering of the "global indices" of ruleCorrespondences is primarily built off of the base side of things
			 * 		with additions every place there was an insertion
			 * (so, where there is relocdation, it will always agree once effective insertions are accounted for wiht the base index)
			 * length of ruleCorrespondences should be : 
			 * 	#shared rules + #deletions + #insertions (baseCasc : shared + deletions; hypCasc: shared + insertions)
			 */
		
			int baseLen = baseCascSim.getTotalSteps(), 
					hypLen = hypCascSim.getTotalSteps();
			
			int total_length = hypLen;
				// get it from the hypothesis cascade, because this is the only we to capture the case of modifications or insertions that add multiple rules at once, 
				//or single insertions for that matter.
			for (int bihimi : baseToHypIndMap)
				if (bihimi == -1)
					total_length += 1; 
			
			locHasPrCh = new boolean[total_length];
			
			ruleCorrespondences = new int[2][total_length]; 
			//init ruleCorrespondences with -2 so we know for sure which cells have been operated upon ([0] could be the result of an operation)
			for (int rci = 0 ; rci < total_length; rci++)	{	
				ruleCorrespondences[0][rci] = -2; ruleCorrespondences[1][rci] = -2;				}
			
			int sameUntil = Integer.parseInt(proposedChs.get(0)[0]);
			
			for (int gri = 0 ; gri < sameUntil; gri++)
			{
				ruleCorrespondences[0][gri] = gri ; 
				ruleCorrespondences[1][gri] = gri ;
			}
			
			int gi = sameUntil, bi = sameUntil, hi = sameUntil; 
				// global, base, and hyp instant iterators
			
			//TODO debugging
			System.out.println("tot len "+total_length);
			
			while (gi < total_length)
			{
				int ilbi = (bi < baseLen) ? dumRIMBH[bi] : -1, 
						ilhi = (hi < hypLen) ? dumRIMHB[hi] : -1; 
					//indices linked to base instant and to hyp instant

				List<Integer> brSrcHis = new ArrayList<Integer>(); 
					// any his that are the source of a back relocdation and need to be handled in order to resolve it within ruleCorrespondences
					// note that the indices should not get corrupted since it is mapping onto hi spots, i.e. rules hit in hypCASC, not the global 
				
				boolean bihiAligned = (ilbi == hi) && (ilhi == bi); 
				// i.e. the current spots in the base and hyp share the same global index
			
				if (bihiAligned)
				{	ruleCorrespondences[0][gi] = ilhi; bi++;
					ruleCorrespondences[1][gi] = ilbi; hi++;
				}
				else // we will have to do some asymmetrical operation -- and there must have been a propCh here 
				{
					locHasPrCh[gi] = true; 
					assert ilbi != ilhi || ilbi != -1: 
						"ERROR: cannot have a rule that exists neither that has neither a base nor hyp index"; 
					/**
					 * There are six possible cases here 
					 * (1) deletion of some sort including as part of non-bijective modification:
					 * 			ilbi == -1
					 * (2) insertion of some sort including as part of a non-bijective modification
					 * 			ilhi == -1
					 * (3) forward relocdation that has not yet been handled/realized in ruleCorrespondences
					 * 			ilbi > hi  
					 * 				note that forward relocdation from X to Y can be reinterpeted as Y-X backward relocdations each by one place instead 
					 * 					this is handled by auxiliary. 
					 * (4) forward relocdation that has already been handled/realized in ruleCorrespondences
					 * 			ilhi < bi && ilhi != -1 && findInt(ruleCorrespondences[1], hi) != -1
					 * (5) backward relocdation that has not yet been handled/realized in ruleCorrespondences -- detectable on HB side
					 * 			ilhi > bi 
					 * (6) backward relocdation that has already been handled/realized in ruleCorrespondences
					 */
					
					//first -- ensure that stumbling upon indices effected by already handled relocdation operations
						// do not cause redundant or erroneous behaviors
					// when we come upon a "crossed out" cell for ilhi e know this must have been from an already handled forward relocdation.
					if (ilhi == -2)	hi++;	// already handled forward relocdation. 
					
				}
						
				// TODO abrogated below. 
				boolean borrowForHI= false, advance1OnBoth= false;
				
				if (ilhi == -2)
				{
					borrowForHI = true; 
					hi++;
					int mapped = UTILS.findInt(ruleCorrespondences[1], hi);
					
					if (mapped == -1)	{
						ilhi = (hi < hypLen) ? dumRIMHB[hi] : -1; 
						dumRIMHB[hi] = -2; 
					}
					else
					{
						ilhi = hi+1;
						advance1OnBoth = true;
					}
					
				}
				
				bihiAligned = (ilbi == hi) && (ilhi == bi); 
						//TODO is placement here erroneous?? -- determine.
					// i.e. the current spots in the base and hyp share the same global index
				if(borrowForHI)	hi--;
				
				if (bihiAligned)
				{	ruleCorrespondences[0][gi] = ilbi; bi++;
					ruleCorrespondences[1][gi] = ilhi; hi++;
				}
				if(advance1OnBoth)	{	bi++;	hi++;	}
				else // we will have to do some asymmetrical operation -- and there must have been a propCh here 
				{
					locHasPrCh[gi] = true; 
					assert ilbi != ilhi || ilbi != -1: 
						"ERROR: cannot have a rule that exists neither that has neither a base nor hyp index"; 
					// recall: global mapping scheme is always built off the base cascade by default
					
					/**
					 * There are six possible cases here 
					 * (1) deletion of some sort including as part of non-bijective modification:
					 * 			ilbi == -1
					 * (2) insertion of some sort including as part of a non-bijective modification
					 * 			ilhi == -1
					 * (3) forward relocdation that has not yet been handled/realized in ruleCorrespondences
					 * 			ilbi > hi  
					 * 				note that forward relocdation from X to Y can be reinterpeted as Y-X backward relocdations each by one place instead 
					 * 					to avoid this problem we are forced to use the HashMap relocdations 
					 * (4) forward relocdation that has already been handled/realized in ruleCorrespondences
					 * 			ilhi < bi && ilhi != -1 && findInt(ruleCorrespondences[1], hi) != -1
					 * (5) backward relocdation that has not yet been handled/realized in ruleCorrespondences -- detectable on HB side
					 * 			ilhi > bi 
					 * (6) backward relocdation that has already been handled/realized in ruleCorrespondences
					 * 		
					 */
					//TODO curate cmt block
					
					//first -- ensure that stumbling upon indices effected by already handled relocdation operations
						// do not cause redundant or erroneous behaviors
					// when we come upon a "crossed out" cell (i.e. wiht -2 in it) we know this must have been from an already handled relocdation.
					// due to how we iterate through this algorithm, this will only ever happen for ilhi -- i.e. only on the hyp side of things
					
					
					
					if(ilbi==-2) bi++; 
					else
					{
						if(ilhi == -1) // insertion of some sort (including from a non-bijective modification)
						{
							ruleCorrespondences[0][gi] = -1; 
							ruleCorrespondences[1][gi] = hi++; 
						}
						else if (ilbi == -1) //deletion of some sort including from a non-bijective modification
						{
							ruleCorrespondences[0][gi] = bi++; 
							ruleCorrespondences[1][gi] = -1; 
						}
						else 
						{
							//now must be some sort of relocdation 
							
							if(relocdIsForward(gi, hi, ilbi, ilhi))
							{
								int mapped_to_bi = UTILS.findInt(dumRIMHB, bi); 
								assert (mapped_to_bi == -1) == (ilhi == -1) : "ERROR: baseline index "+bi+" is never mapped to..."; 
								ruleCorrespondences[0][gi] = bi++; 
								ruleCorrespondences[1][gi] = mapped_to_bi; 
								dumRIMHB[mapped_to_bi] = -2; //cross-out. 
							}
							else
							{
								System.out.println("hi "+hi); //TODO debuggin
								
								int mapped_to_hi = UTILS.findInt(dumRIMBH, hi); 
								assert (mapped_to_hi == -1) == (ilbi == -1) : "ERROR: hyp index "+hi+" is never mapped to..."; 
								ruleCorrespondences[0][gi] = bi; 
								ruleCorrespondences[1][gi] = ilhi; 
								dumRIMBH[ilhi] = -2; //cross-out. 
								dumRIMHB[hi] = -2; //cross-out
							}
							
							
							//TODO below is abrogated 
							/**
							// note that if bi == hi, it doesn't matter which way we consider this except for debugging purposes
							// for simplicity purposes -- go with whatever
							int mapped_to_bi = UTILS.findInt(dumRIMHB, bi); 
							assert (mapped_to_bi == -1) == (ilhi == -1) : "ERROR: baseline index "+bi+" is never mapped to..."; 
							ruleCorrespondences[0][gi] = bi++; 
							ruleCorrespondences[1][gi] = mapped_to_bi; 
							dumRIMHB[mapped_to_bi] = -2; //cross-out. 
							*/
						}
					}
				}
			}  			
		}
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
	
	/**fills changedDerivations and changedRuleEffects, and sets divergencePoint. 
	 * @param et_id -- etymon index, which should be consistent between the two Simulations. 
	 * @return @default an empty String ""- means there is no difference between the derivations
	 * 	otherwise: the DIFFERENTIAL DERIVATION, formed as follows: 
	 * // String will contain
				// First line --- <INPUT> \n
				// CONCORDANT UNTIL : <INDEX AFTER LAST CONCORDANT RULE> \n
				// for 1-to-1 divergent development : <GLOBALRULE#> : [BASERULE#] <OLDFORM> > <NEWFORM> | [HYPRULE#] NEW > OLD \n
				// deletion (i.e. occurs only in baseline: <BASERULE#>: <OLDFORM> > <NEWFORM> | -- \n
				// insertion: the reverse.
	 */ 
	private void computeTrajectoryChange() 
	{
		divergencePoint = -1; // will remain -1 if two cascades never diverge for a single word.
		
		assert baseCascSim.NUM_ETYMA() == hypCascSim.NUM_ETYMA() :
			"ERROR: Inconsistent number of etyma between base and hypothesis cascade simulation objects"; 
		int n_ets = baseCascSim.NUM_ETYMA();  
		
		changedDerivations = new HashMap<Integer,String>(); 
		changedRuleEffects = new HashMap<Integer,String[][]>(); 
		
		for (int ei = 0 ; ei < n_ets ; ei++)
		{
			int lexDivPt = findEtDivergenceMoment(ei);
			
			//recall -- if findEtDivergenceMoment() returns -1 it means there is no difference. 
			if(lexDivPt != -1)
			{
		        if(divergencePoint == -1)       divergencePoint = lexDivPt;
		        else    divergencePoint = Math.min(divergencePoint, lexDivPt); 
	
		        String ddHere = getDifferentialDerivation(ei);
		        changedDerivations.put(ei, ddHere); 
		        
		        // now adding effects for changedRuleEffects 
		        ddHere = ddHere.substring(ddHere.indexOf("CONCORD")); 
		        	//error of lacking this will have already been caught by findLexicalDerivationPoint(). 
		        ddHere = ddHere.substring(ddHere.indexOf("\n") +"\n".length()); 

				for (String ddl : ddHere.split("\n"))
				{
					if(ddl.contains(">"))
					{
						int globInd = Integer.parseInt(ddl.substring(0, ddl.indexOf("[")));
						String[] effs = ddl.substring(ddl.indexOf(": ")+2).split(" \\| "); 
						boolean[] hit = new boolean[] { effs[0].contains(">"), effs[1].contains(">")};  
						// since modification is stored here as a deletion and an insertion,
							// it will not be represented as a single rule in a differential derivation
						// therefore, there is a bidirectional implication between non-equivalence of 
							// rule effects "hitting" words
								// and changes in rule effects, 
							// so hit[0] != hit[1] is a perfect proxy.
						if (hit[0] != hit[1])
						{
							String[][] diffEffsHere;
							if(changedRuleEffects.containsKey(globInd))
							{
								// differences detected thus far that we are adding the latest to
								diffEffsHere = changedRuleEffects.get(globInd); 
								if (hit[0])	diffEffsHere[0][ei] = effs[0] ; 
								else /*hit[1]*/	diffEffsHere[1][ei] = effs[1] ; 
							}
							else
							{
								diffEffsHere = new String[2][baseCascSim.NUM_ETYMA()]; 
								if (hit[0])	diffEffsHere[0][ei] = effs[0]; 
								else /*hit[1]*/	diffEffsHere[1][ei] = effs[1];
							}
							changedRuleEffects.put(globInd, diffEffsHere);
						}
					}
				}
			}
		}
	}

	/** getDifferentialDerivation 
	 * @return the differential derivation for a particular etymon
	 * 	* the etymon being indexed by @param et_id
	 * this is the a two-sided derivation which shows the engendered difference between
	 * 	* the baseline cascade and the hypothesis cascade
	 * @return the empty string "" if there is no difference. 
	 */
	public String getDifferentialDerivation(int et_id)
	{
		String baseDer= baseCascSim.getDerivation(et_id), 
				hypDer = hypCascSim.getDerivation(et_id); 
		if (baseDer.equals(hypDer))	return "";
		// passing here does not exclude the possibility of an identical derivation
			// -- we will have to use ruleCorrespondences to ascertain that.
			// we do this by changing the rule index numbers in both derivations to their "global" indices in ruleCorrespondences
				// conveniently handled with mapping arrays
		
		baseDer= globalizeDerivInds(baseDer, false); 
		hypDer = globalizeDerivInds(hypDer, true); 
		
		if(baseDer.equals(hypDer))	return "";
		//now we know they are indeed different -- so fill in info on how... 
		
		String[] bdlines = baseDer.split("\n"), hdlines = hypDer.split("\n"); 
		
		assert bdlines[0].equals(hdlines[0]) : "Error: inconsistent initial line between two corresponding lexical derivations" ;
		String out = bdlines[0]; 
		int bdli = globalDivergenceLine(baseDer, hypDer),
				hdli = bdli ;
		
		String lastBform = "", lastHform = ""; 
		if (bdli == 1 ) {
			lastBform = bdlines[0].replace("/", "#"); lastHform = hdlines[0].replace("/", "#");	}
		else	{
			//note that at this point bdli and hdli are essentially interchangeable. 
			lastBform = (bdlines[bdli - 1].contains(" \\|") ? bdlines : hdlines)[bdli-1].split(" \\|")[0];  
			lastHform = (hdlines[hdli - 1].contains(" \\|") ? hdlines : bdlines)[bdli-1].split(" \\|")[0]; 
			
			if (lastBform.contains("stage form : "))	lastBform = lastBform.split("stage form : ")[1];
			if (lastHform.contains("stage form : "))	lastHform = lastHform.split("stage form : ")[1];
		}
		
		int nextGlobalBaseInd = UTILS.extractInd(bdlines[bdli]), 
				nextGlobalHypInd = UTILS.extractInd(hdlines[hdli]); 
		
		// we know next line cannot be gold/black stage announcement as that could not be the first line with divergence. 
		//cannot allow divergence to be at something other than a rule realization -- throw AssertionError if so
		assert nextGlobalBaseInd != -1 || nextGlobalBaseInd != nextGlobalHypInd : 
			"Error : cannot have divergence occur due to something other than a difference in sound rules."; 
		
		int concordantUntil = Math.min(nextGlobalBaseInd, nextGlobalHypInd);
		concordantUntil = concordantUntil != -1 ? concordantUntil : Math.max(nextGlobalBaseInd, nextGlobalHypInd);
		out += "\nCONCORDANT UNTIL RULE : "+ concordantUntil;
		
		// recall -- we have already aligned the numbers in the two derivations using derivationToGlobalInds()
		// so it is obvious if we are dealing with a deletion or insertion as it is simply absent on the other side.
		
		// we now will no longer necessarily be iterating simultaneously on bdlines and hdlines
		// we only do so when they have hte same rule (same = same global index)
		// when the index is not shared, we are handling a case of deletion/insertion, 
			// or bleeding/feeding as a result of the rule change.
		
		while ( bdli < bdlines.length && hdli < hdlines.length)
		{
			int[] stageHere = new int[] { bdlines[bdli].indexOf("stage form "), 
							hdlines[hdli].indexOf("stage form : ")}; 
			
			boolean[] isFin = new boolean[] { bdlines[bdli].substring(0,5).equals("Final"), hdlines[hdli].substring(0,5).equals("Final")} ; 
			
			nextGlobalBaseInd = (isFin[0] || stageHere[0] > -1) ? -1 : UTILS.extractInd(bdlines[bdli]);
			nextGlobalHypInd = (isFin[1] || stageHere[1] > -1) ? -1 : UTILS.extractInd(hdlines[hdli]); 

			if (nextGlobalBaseInd == nextGlobalHypInd && nextGlobalBaseInd != -1)
			{
				String nextBform = bdlines[bdli].substring(0, bdlines[bdli].indexOf(" | ")), 
					nextHform = hdlines[hdli].substring(0, hdlines[hdli].indexOf(" | "));
				
				out += "\n"+nextGlobalBaseInd+"["+ruleCorrespondences[0][nextGlobalBaseInd]
						+"|"+ruleCorrespondences[1][nextGlobalHypInd]+"] : "
						+lastBform+" > "+nextBform+" | "+lastHform+" > "+nextHform;
				lastBform = nextBform; 
				lastHform = nextHform; 
				bdli++; hdli++; 	
			}
			else if(stageHere[0] > -1 && stageHere[1] > -1)
			{
				out += "\n"+bdlines[bdli].substring(0, stageHere[0])+bdlines[bdli].substring(stageHere[0]+11)+" | "
						+ hdlines[hdli].substring(stageHere[1]+13); 
				bdli++; hdli++; 
			}
			else if(isFin[0] && isFin[1])
			{	out += "\nFinal forms : "+bdlines[bdli].substring(bdlines[bdli].indexOf(":")+2) + " | "+
									hdlines[hdli].substring(hdlines[hdli].lastIndexOf(":")+2);
				hdli++; bdli++;	}
			else if (nextGlobalHypInd == -1 ? true : nextGlobalBaseInd < nextGlobalHypInd && nextGlobalBaseInd != -1) //deletion or bleeding
			{
				String nextBform = bdlines[bdli].substring(0, bdlines[bdli].indexOf("|")-1);
				
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
	private String globalizeDerivInds(String der, boolean isHyp)
	{
		int br = der.indexOf("\n");
		String out = der.substring(0, der.indexOf("\n"));
		String[] lines = der.substring(br+"\n".length()).split("\n") ;
		
		for (String li : lines)
		{
			int br1 = li.indexOf(" | "),
					br2 = li.lastIndexOf(" : "); 
			if (br1 != -1 && br2 != -1)
			{
				out += "\n" + li.substring(0,br1+3); 
				int raw_ind = Integer.parseInt(li.substring(br1+3,br2).trim());
				out += (isHyp ? hypRuleIndsToGlobal : baseRuleIndsToGlobal)[ raw_ind ] + li.substring(br2); 
			}
			else	out += "\n"+li;
		}
		return out; 
	}
	
	public String getGlobalizedDerivation(int et_id, boolean isHyp)
	{
		return globalizeDerivInds( (isHyp ? hypCascSim : baseCascSim).getDerivation(et_id), isHyp);
	}
	
	// prints basic info on changes in words effected 
		// and rules effected
		// does not print evaluation statistic change -- that is for DiachronicSimulation and ErrorAnalysis to handle. 
	public void printBasicResults()
	{
		//TODO need to fix this. 
		
		//Analysis of changes transforming base -> hyp upon the effects of rules. 
		System.out.println("ANALYSIS OF EFFECT OF PROPOSED CHANGES:\n");
		System.out.println("Last rule before divergence: "+divergencePoint); //TODO may have to debug this... 
		System.out.println("Effects of specific changes between baseline and proposed cascade."); 
		for (int globInd = 0; globInd < ruleCorrespondences[0].length; globInd++)
		{	
			if (locHasPrCh[globInd]) // true -- implies this was one of the rules that are specific changes. 
			{
				if(ruleCorrespondences[0][globInd] == -1)
				{
					System.out.println("Developments directly caused by a proposed change:"); // i.e. in hyp but not baseline.
					System.out.println(strEffects(changedRuleEffects.get(globInd)[1]));
				}
				else
				{
					assert ruleCorrespondences[1][globInd] == -1 : "Error: comprehension of which rules were added is malformed"; //TOOD reword?
					System.out.println("Developments directly aborted by proposed change:"); 
					System.out.println(strEffects(changedRuleEffects.get(globInd)[0])); 
				}
			}
		}
		
		System.out.println("Effects on rules other than those explicitly changed:\n"); 
		for (int globInd = 0; globInd < ruleCorrespondences[0].length; globInd++)
		{	
			if (!locHasPrCh[globInd]) // false -- implies this was not one of the rules that are specific changes, so changes must be bleeding or feeding effects.
			{
				int baseInd = ruleCorrespondences[0][globInd] ; 
				String[] bleedings = changedRuleEffects.get(globInd)[0],
						feedings = changedRuleEffects.get(globInd)[1] ; 
				
				System.out.println("Baseline rule "+baseInd+" (global: "+globInd+")\n\tbled for "
						+ UTILS.numFilled(bleedings)+" etyma, fed for "+UTILS.numFilled(feedings)+"."); 
				
				System.out.println("Bled: "+strEffects(bleedings)); 
				System.out.println("Fed: "+strEffects(feedings)); 
							
			}
		}
		
		boolean prgold = baseCascSim.hasGoldOutput(); 
		
		System.out.println("\nEtymon effected: (#: BASE>HYP"+(prgold ? "[GOLD]" : "")+")"); 
		
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
	
	
	// @param skipCode -- "g2" -- i.e. "skip 2 gold stages"
	// @param aggRemTxt -- aggregrate remaining text
	// @return break point to skip those stages
	private static int brkPtForStageSkip(String aggRemTxt, String skipCode)
	{
		boolean isGold = skipCode.charAt(0) == 'g'; 
		char SN = isGold ? UTILS.GOLD_STAGENAME_FLAG : UTILS.BLACK_STAGENAME_FLAG; 
		int skips_left = Integer.parseInt(skipCode.substring(1));  
		if (aggRemTxt.charAt(0) == SN)	skips_left--; 
		String dummyTxt = aggRemTxt + "";
		int brkpt = 0; 
		String breaker = "\n"+SN; 
		
		while (skips_left > 0)
		{
			int nextbreak = dummyTxt.indexOf(breaker) + breaker.length(); 
			
			//then go to the end of that line, since it will just be the stage declaration
			nextbreak += dummyTxt.substring(nextbreak).indexOf("\n")+"\n".length(); 
			
			brkpt += nextbreak; 
			dummyTxt = dummyTxt.substring(nextbreak); 
			
			skips_left--; 
		}
		
		return brkpt; 
	}
	
	private String STAGEFLAGS = ""+UTILS.GOLD_STAGENAME_FLAG + UTILS.BLACK_STAGENAME_FLAG; 
	private boolean flagged(String str)	{	return STAGEFLAGS.contains(""+str.charAt(0));	}

	/** newCascText
	 * gets text from @global cascFileLoc
	 * @return casc text as appropriately modified. 
	 * @varbl proposedChs -- proposedChanges, of form [index, description] -- see above in comments about proposedChanges @varbl for more info
	 * @param comments -- comments to be inserted for each proposed change 
	 * 		(note that the insertion part of modification is bidirectionally indicated with the lack of such a comment)
	 * @param justPlaceHolders -- if true, we are only placing comments to indicate where user should edit cascade file
	 * 		otherwise we are actually carrying out the edits. 
	 * @param targCascLoc -- location of file containing the cascade we are making a modified version of. 
	 * @param fac -- SChangeFactory for cascade comprehension, should be declared with consistent structures for 
	 * 		phoneSYmbToFeatsMap, featIndices and featImplications as used in DiachronicSimulator/SimulationTester/any other classes concurrently in use. 
	 * @return
	 */
	public String newCascText(List<String> comments, boolean justPlaceHolders, String targCascLoc, SChangeFactory fac) throws MidDisjunctionEditException
	{
		int linesPassed = 0; 
		String readIn = ""; 
		
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader ( 
				new FileInputStream(targCascLoc), "UTF-8")); 
			String nextLine = ""; 
		
			while((nextLine = in.readLine()) != null)
				readIn += nextLine.replace("\n", "")+"\n"; 
			in.close();
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("Encoding unsupported!");
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO Exception!");
			e.printStackTrace();
		}
		
		int igs = -1, ibs = -1; //these indicate curent place among gold and black stages respectively
		int nxRuleInd = 0 ; // next rule index
		String out = ""; 
		
		int nGSt = baseCascSim.NUM_GOLD_STAGES(), nBSt = baseCascSim.NUM_BLACK_STAGES(); 
		
		/**
		int[] opGoldStages = new int[nGSt], opBlackStages = new int[nBSt] ; // operational lists. 
		int gi = 0, bi = 0; 
		while (gi < nGSt)	
		{
			opGoldStages[gi] = baseCascSim.getStageInstant(true, gi); 
			gi++; 
		}
		while (bi < nBSt)
		{
			opBlackStages[bi] = baseCascSim.getStageInstant(false, bi); 
			bi++; 
		}**/
		
		int effLocModifier = 0; // add +1 after a deletion, -1 after insertion etc... 
			// to normalize for cahnges in place within data structures since they favor the hyp side of the equation.
		
		//iterate over each proposed change
		for (int pci = 0; pci < proposedChs.size(); pci++)
		{
			int nxChRuleInd = Integer.parseInt(proposedChs.get(pci)[0]) + effLocModifier ; 
			boolean isDelet = proposedChs.get(pci)[1].equals("deletion"); 
			// will be used to determine where we place new content with respect to comment blocks
			
			String stagesToSkip = ""; 
			int prev_igs = igs , prev_ibs = ibs; 		
			
			if(nGSt > 0)
				while(igs == nGSt - 1 ? false : baseCascSim.getStageInstant(true,igs + 1) <= nxChRuleInd)
					igs+=1; 
			if(nBSt > 0)
				while(ibs == nBSt -1 ? false : baseCascSim.getStageInstant(false, ibs + 1) <= nxChRuleInd)
					ibs+=1;

			if(igs > prev_igs && ibs > prev_ibs)
			{
				int nxBsInstant = baseCascSim.getStageInstant(false, ibs), 
						nxGsInstant = baseCascSim.getStageInstant(true, igs); 
				
				if(nxBsInstant < nxGsInstant)	stagesToSkip = "b"+(ibs-prev_ibs); 
				else	stagesToSkip = "g"+(igs-prev_igs); 
			}
			else if (igs > prev_igs)	stagesToSkip = "g"+(igs-prev_igs);
			else if (ibs > prev_ibs)	stagesToSkip = "b"+(ibs-prev_ibs); 
			// else: there is no stage to skip, and stagesToSkip remains ""; 
			
			if(!stagesToSkip.equals(""))
			{		
				int break_pt = brkPtForStageSkip(readIn, stagesToSkip); 
					// should end in "\n"
				
				String hop = readIn.substring(0, break_pt); 
				linesPassed += hop.split("\n").length - 1; //minus 1 because of the final \n 
				readIn = readIn.substring(break_pt); 
				boolean sg = stagesToSkip.charAt(0) == 'g'; 
				nxRuleInd = baseCascSim.getStageInstant(sg, sg ? igs : ibs); 
				out += hop; 
			}
			
			while (nxRuleInd <= nxChRuleInd)
			{
				// first - skip any leading blank lines or stage declaration lines
				while (flagged(readIn) || UTILS.isJustSpace(readIn.substring(0, readIn.indexOf("\n"))))
				{
					int brkpt = readIn.indexOf("\n") + "\n".length(); 
					linesPassed ++; 
					out += readIn.substring(0, brkpt);
					readIn = readIn.substring(brkpt); 
				}
				
				String cmtBlock = ""; 
				// case of if the next line is headed by the comment flag. 
				// absorb all consecutive comment lines in @varbl commentBlock
				while (readIn.charAt(0) == CMT_FLAG ) {
					int brkpt = readIn.indexOf("\n") + "\n".length(); 
					cmtBlock += readIn.substring(0, brkpt); 
					readIn = readIn.substring(brkpt); 
					linesPassed++; 
				}
				
				if (!cmtBlock.equals("") && UTILS.isJustSpace(readIn.substring(0,readIn.indexOf("\n"))))
				{
					int brkpt = readIn.indexOf("\n") + "\n".length(); 
					cmtBlock += readIn.substring(0, brkpt); 
					readIn = readIn.substring(brkpt); 
					linesPassed++; 
				}
				//if next line is either another blank line, another comment after blank line, 
				// or a stage, this iteration of loop is over
				if ((STAGEFLAGS + CMT_FLAG).contains(readIn.substring(0,1))
						|| UTILS.isJustSpace(readIn.substring(0, readIn.indexOf("\n"))) ) {
					out += cmtBlock; 
					cmtBlock = "";
				}
				else // i.e. we are handling a line holding a rule.
				{
					//on the other hand, if a rule comes after this block, we consider the comment block to have been
						// the explanation or justification for the rule, and will then operate on the rule.
					// if the comment block is empty, nothing explicitly differs in code, so both are handled here. 
					int brkpt = readIn.indexOf("\n"); 
					String ruleLine = readIn.substring(0, brkpt); 
					
					List<SChange> shiftsHere = fac.generateSoundChangesFromRule(ruleLine); 
					
					assert shiftsHere.get(0).toString().equals(baseCascSim.getRuleAt(nxRuleInd).toString()) : 
						"Error : misalignment in saved CASCADE and its source file"; //TODO debugging likely necessary 
					
					readIn = readIn.substring(brkpt+"\n".length());
					
					if (nxRuleInd - 1 + shiftsHere.size() < nxChRuleInd) // then we can simply absorb it into @varbl[out] as usual.
					{
						out += cmtBlock + ruleLine + "\n"; 
						nxRuleInd += shiftsHere.size(); 
						linesPassed++; 
					}
					else //perform proper file text modification behavior according to proposed change and whether we are automodification or merely commenting mode. 
					{
						String newCmt = comments.get(pci); 
						if (newCmt.length() > 0)
							if (!UTILS.cmtIsStandardized(newCmt))
								newCmt = UTILS.standardizeCmt(newCmt);
						
						//TODO we are assuming all rule indexing is correct as supplied by @param propChs
						// ... may need to check this. 
						
						if (shiftsHere.size() == 1 ) // current rule written is NOT a disjunction that is internally represented as multiple rules.
						{ 
							if(isDelet) 
							{	// then we add comments AFTER prior block
								out += cmtBlock; 
								
								out += newCmt;
								if (!out.substring(out.length() - "\n".length()).equals("\n"))
									out += "\n"; 
								
								if (justPlaceHolders)
									out += CMT_FLAG+"AUTOGENERATED CORRECTION CUE : delete following rule.\n"
											+ ruleLine + "\n"; 
								else//  comment out the rule that we are deleting... 
									out += CMT_FLAG + ruleLine + "\n"; 
								linesPassed ++; 
								nxRuleInd++; 
								
								effLocModifier += 1; 
								
							}
							else // we are dealing with an insertion then.
							{	
								// and thus comments and insertion come before next rule's preceding comment block
								
								out += newCmt; 
								if (!out.substring(out.length() - "\n".length()).equals("\n"))
									out += "\n"; 
								if (justPlaceHolders)
									out += CMT_FLAG+"AUTOGENERATED CORRECTION CUE: insert following rule to replace this comment\n"
											+CMT_FLAG;
								out += proposedChs.get(pci)[1] + "\n\n"; 
								//nextRuleInd or linesPassed do not increment since we are only inserting new content. 
							
								//restore commentBlock and ruleLine to readIn
									// because later rules may operate on them. 
								readIn = cmtBlock + ruleLine + "\n" + readIn; 
								//track back on linesPassed as appropriate. 
								linesPassed -= (cmtBlock+ruleLine).split("\n").length; 
								nxRuleInd++;
								
								effLocModifier += -1 * fac.generateSoundChangesFromRule(proposedChs.get(pci)[1]).size(); 
							}
						}
						else //then there is a disjunction -- whether we can pass without error is determined by value of justPlaceHolders
						{
							if(justPlaceHolders)
							{
								if (isDelet) {
									out += cmtBlock; 
									out += comments.get(pci); 
									if (!out.substring(out.length() - "\n".length()).equals("\n"))
										out += "\n"; 
									out += CMT_FLAG+"AUTOGENERATED CORRECTION CUE: note, deletion targets one of many rules generated from "
											+ "\n"+HANGING_INDENT+CMT_FLAG+"a rule form with a {} disjucntion:\n"
											+ HANGING_INDENT+"\t"+CMT_FLAG+ruleLine+"\n"+HANGING_INDENT+CMT_FLAG
											+ "The one targeted for deletion is "+shiftsHere.get(nxChRuleInd- nxRuleInd)
											+ "\n"+HANGING_INDENT+CMT_FLAG+"Manually edit as is appropriate below.\n"
											+ruleLine+"\n"; 
									nxRuleInd += shiftsHere.size(); 
									linesPassed++; 
								}
								else // insertion with justPlaceHolders = true and with a disjunction in the rule form. 
								{
									// if we are inserting before the first rule generated by the disjunction, this is just like the normal case 
										// without the issues arising from the disjunction.
									if( nxRuleInd == nxChRuleInd) 
									{
										// and thus comments and insertion come before next rule's preceding comment block
										out += comments.get(pci); //will do nothing if this is part of a modification. 
										if (!out.substring(out.length() - "\n".length()).equals("\n"))
											out += "\n"; 
										out += CMT_FLAG+"AUTOGENERATED CORRECTION CUE: insert following rule to replace this comment\n"
													+CMT_FLAG+proposedChs.get(pci)[1] + "\n"; 
										
									}
									else
									{
										out += comments.get(pci); 
										if (!out.substring(out.length() - "\n".length()).equals("\n"))
											out += "\n"; 
										out += CMT_FLAG + "AUTOGENERATED CORRECTION CUE: note, an insertion of a new rule was placed here\n"
												+ HANGING_INDENT+CMT_FLAG+"between two rules that were drawn from a rule form with a {} disjunction:\n"
														+ HANGING_INDENT+"\t"+CMT_FLAG+ruleLine+"\n"
												+ HANGING_INDENT+CMT_FLAG+"Specifically between the two generated rules,\n"
														+ HANGING_INDENT+"\t"+CMT_FLAG+shiftsHere.get(nxChRuleInd - nxRuleInd - 1)+"\n"
																+ HANGING_INDENT+"\t\t\t"+CMT_FLAG+"and\n"
														+ HANGING_INDENT+"\t"+CMT_FLAG+shiftsHere.get(nxChRuleInd - nxRuleInd)+"\n"
														+CMT_FLAG+"AUTOGENERATED CORRECTION CUE: insert following rule to replace this comment\n"
														+CMT_FLAG+proposedChs.get(pci)[1]+"\n"; 
									}

									//nextRuleInd or linesPassed do not increment since we are only inserting new content. 
								
									//restore commentBlock and ruleLine to readIn
										// because later rules may operate on them. 
									readIn = cmtBlock + ruleLine + "\n" + readIn; 
									//track back on linesPassed as appropriate. 
									linesPassed -= (cmtBlock+ruleLine).split("\n").length; 
								}
									
							}
							else
							{
								String errorMessage = "MidDisjunctionException : Currently, you cannot ";
								//TODO check for proper error message generation here .... 
								
								if (isDelet)
								errorMessage += isDelet ? "delete only one" : "insert a rule between two"; 
								errorMessage += " of the sound changes derived from rule written in the original cascade file "
											+ "with a {} disjunction in its context stipulations.\n"
											+ "The disjunct context rule in question was "+ruleLine+"\n"
											+ "It is on line "+linesPassed+" of the original cascade file, "+targCascLoc+"\n";
								
								if (isDelet) errorMessage +=  "You tried to delete this derived rule : "+shiftsHere.get(nxChRuleInd - nxRuleInd);
								else	errorMessage += "You tried to insert this rule : "+proposedChs.get(pci)[1]+"\n"
										+ "between this derived rule : "+shiftsHere.get(nxChRuleInd - nxRuleInd - 1)+"\n"
												+ "and this one : "+shiftsHere.get(nxChRuleInd - nxRuleInd); 
								
								throw new MidDisjunctionEditException(errorMessage+ "\nInstead, you should manually make this change at the specified line number yourself."); 
							}
						}
					}
				}
			}
			
		}

		//TODO make sure this final append behavior is carried out correctly.
		return out + readIn; 
	}
	
	public int[][] getRuleCorrespondences()	{	return ruleCorrespondences;	}
	
	
	/** findEtDivergenceLine
	 * @return the earliest *line* where the baseline and hypothesis derivations for one etyma diverge
	 * @return -1 -- if there is no divergence at all. 
	 * @param et_id -- index of the etymon
	 */
	private int findEtDivergenceLine (int et_id) 
	{
		String bd = baseCascSim.getDerivation(et_id), hd = hypCascSim.getDerivation(et_id); 
		if (bd.equals(hd))	return -1; 
		bd = globalizeDerivInds(bd, false); 
		hd = globalizeDerivInds(hd, true); 
		if (bd.equals(hd))	return -1; 
		
		return globalDivergenceLine(bd, hd); 
	}
	
	/**
	 * @return earliest (global) *moment* that the baseline and hyp derivs diverge for one etyma
	 * @return -1 -- if there is no divergence at all
	 * @param et_id -- index of the etymon.
	 */
	private int findEtDivergenceMoment (int et_id)
	{
		String dd = getDifferentialDerivation(et_id);
		if(dd.equals(""))	return -1;
		dd = dd.substring(dd.indexOf("CONCORDANT UNTIL RULE : ")); 
		dd = dd.substring(dd.indexOf(":")+2, dd.indexOf("\n"));
		return Integer.parseInt(dd); 
	}
	
	/**
	 * find the line of divergence between two line-split derivations
	 *  of the same word
	 * derivations entered should already be "globalized"
	 */
	private int globalDivergenceLine (String bd, String hd)
	{
		assert !bd.equals(hd): "Error: tried to find divergence point for two identical derivations!";

		// recall that the first line indicates the input form -- hence etymon identity
		String[] bdlns = bd.split("\n"), hdlns = hd.split("\n");
		assert bdlns[0].equals(hdlns[0]) : "Error: tried to find divergence point for derivations with different inputs";

		int out = 1, minlen = Math.min(bdlns.length, hdlns.length); 

		//determine how long the derivations remain consistent.
		while (out >= minlen ? false : bdlns[out].equals(hdlns[out]))
			out++;

		return out; 
	}

	// accessors.
	public int[] getBaseIndsToGlobal()	{	return baseRuleIndsToGlobal;	}
	public int[] getHypIndsToGlobal()	{	return hypRuleIndsToGlobal;	}
	public boolean[] getPrChLocs()	{	return locHasPrCh; 	}
	public int getDivergencePoint()	{	return divergencePoint;	}
	public HashMap<Integer,String[][]> getChangedRuleEffects()	{	return changedRuleEffects;	}
	public String[][] getRuleEffectChanges(int global_id)	{	return changedRuleEffects.get(global_id); 	}
	public String[] getEffectsBlocked(int global_id)	
	{	return changedRuleEffects.containsKey(global_id) ? changedRuleEffects.get(global_id)[0] : new String[baseCascSim.NUM_ETYMA()]; 	}
	public String[] getEffectsCaused(int global_id)	
	{	return changedRuleEffects.containsKey(global_id) ? changedRuleEffects.get(global_id)[1] : new String[baseCascSim.NUM_ETYMA()];	}
	
	public HashMap<Integer,String> getChangedDerivations()	{	return changedDerivations;	}
	public int[] getEtsWithChangedDerivations()	{
		List<Integer> keys = new ArrayList<Integer>(changedDerivations.keySet());
		int N = keys.size(); 
		int[] out = new int[N]; 
		while (!keys.isEmpty())
		{
			Integer min = Collections.min(keys); 
			out[N-keys.size()] = min.intValue();
			keys.remove(min); 
		}
		return out;
	}
	
	
	//TODO expl here.
	private String strEffects (String[] effsOverEts)
	{
		String out = "";
		for (String eoei : effsOverEts)
			if (!eoei.equals(""))
				out += "; "+eoei; 
		
		return out.substring(2); 
	}
	
	//auxiliary for compareRuleCorrespondences, 
	// recruits proposedChs to disambiguate if a relocdation is forward or backward 
	//recall : indices in proposedChs[][0] are always the current (at point of operation) index in hypCASC -- so hi is used not bi
	private boolean relocdIsForward(int gi, int hi, int ilbi, int ilhi)
	{
		assert ilbi != -1 || ilbi != ilhi : "ERROR: called relocdIsForward() for something that cannot be a relocdation as both ilhi and ilbi are -1."; 
		if (Math.min(ilbi, ilhi) == -1)
			return ilbi == -1; 
	
		//int candBackSrc = ilbi, candForwDest = ilhi;
	
		int pci = 0, lenpc = proposedChs.size(); 
		boolean reached = false; 
		while (!reached)
		{
			assert pci < lenpc : "ERROR: reached of end of proposedChanges without finding current target of operation."; 
			int curhi = Integer.parseInt(proposedChs.get(pci)[0]); 
			assert curhi <= hi : "ERROR: could not find current target of operation in proposedChanges!";
			reached = (curhi == hi); 
			pci ++; 
		}
		
		//PRIOR DELETION SKIPPING pass any prior deletions we have already processed wiht operations at this hyp index.
		int deletionsToPass = 0; 
		for (int pdgi = gi - 1 ; pdgi < 0 ? false : ruleCorrespondences[1][pdgi] == -1 ; pdgi-- )
			deletionsToPass++; 
			
		pci += deletionsToPass ; 
		assert proposedChs.get(pci-1)[0].equals(""+hi) : "Error in ruleCorrespondences[1] in placement of -1 for deletion rules"; 
		assert pci < lenpc : "ERROR: reached end of proposedChanges without finding later index of relocdation operation.";
		
		boolean passed = false; 
		
		while(!passed)
		{
			// if this a deletion we then know it is forward
			if (proposedChs.get(pci-1)[1].equalsIgnoreCase("deletion"))	return true; 
			
			// now we know this is an insertion 
			// we have to make sure it isn't immediately deleted by dumb users.
			passed = !proposedChs.get(pci)[0].equals(""+hi);
			if (!passed) //it's a stupid user deleting the rule they just added, and we haven't explicitly added support on the rule processing side for this annoyance yet...
			{
				assert proposedChs.get(pci)[1].equalsIgnoreCase("deletion") : "index processing error."; 
				pci += 2;
				assert proposedChs.get(pci-1)[0].equals(""+hi) : "Error in ruleCorrespondences[1] in placement of -1 for deletion rules"; 
				assert pci < lenpc : "ERROR: reached end of proposedChanges without finding later index of relocdation operation.";
			}
		}
		// if reached this point must be an insertion, i.e. backward
		return false; 
					
	
	}
}
	
	