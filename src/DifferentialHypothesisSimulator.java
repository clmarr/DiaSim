import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class DifferentialHypothesisSimulator {
	/**
	 * Class for performing analysis operations on two Simulations in order to
	 * analyze the effect of a proposed change to the cascade
	 */

	// finals
	public final static String HANGING_INDENT = "      ";
	public static final char CMT_FLAG = UTILS.CMT_FLAG;

	// target variables needing tracked for any thorough debugging procedure
	public Simulation baseCascSim, hypCascSim;
	// "baseline cascade" and "hypothesized cascade"

	private List<String[]> proposedChs;
	// TODO important variable here, explanation follows
	// each indexed String[] is form [curr time step, operation details]
	// this object is *kept sorted* by index in the cascade as ordered at time of operation!!
	// thus it never descends: 
	//	Value at each cell indexed i + 1 must be >= than that at cell indexed i.
	// (deletion is considered to be indexed at the spot before the item being deleted)
	// operation may be either deletion or insertion
	// both relocdation and modification are handled as deletion then insertion pairs.
	// for deletion, the second slot simply holds the string "deletion"
	// whereas for insertion, 
		// the second index holds the string form of the SChange that is inserted in hypCASCADE.

	private HashMap<Integer, String> changedDerivations;
	// Integer type key is the GLOBAL RULE INDEX, as used in ruleCorrespondences
	// only etyma with changed derivations are included here.
	// String -- the DIFFERENTIAL DERIVATION -- will contain
	// First line --- <INPUT> \n
	// CONCORDANT DEVELOPMENT THROUGH TO : <LAST CONCORDANT BASE RULE> | <LAST
	// CONCORDANT HYP RULE> \n
	// for 1-to-1 divergent development : <BASERULE#>: <OLDFORM> > <NEWFORM> |
	// <HYPRULE#>: NEW > OLD \n
	// deletion (i.e. occurs only in baseline: <BASERULE#>: <OLDFORM> > <NEWFORM> |
	// -- \n
	// insertion: the reverse.

	private int[] baseRuleIndsToGlobal, hypRuleIndsToGlobal;
	// indices -- base/hyp rule indices
	// cells contain the index mapped to in ruleCorrespondences
	// i.e. the "global" index.

	private int[][] ruleCorrespondences;
	// INNER index -- global index. ;
	// OUTER index: 0 is baseline, 1 is hypothesis cascade. 
	// -1 in [0][x] -- deletion occurred
	// -1 in [1][x] -- insertion occurred
	// [0][same] and [1][same] -- cells contain numbers of correspondent rules (if neither has -1 as value)
	// intentionally indifferent to which changes underlie the correspondences. 
		// as such it cannot be derived from proposedChanges, which treats relocdations (for which the rule correspondence should be preserved) as pairs of insertion and deletion! 
	
	private HashMap<Integer, String[][]> changedRuleEffects;
	// Integer type key is ruleCorrespondences's inner nesting index (i.e. the
	// "global index")
	// only rules with changed effects are included here.
	// String[2][NUM_ETYMA] value has all empty cells by default.
	// outer nesting 0 is for the baseline , 1 is for alt hyp
	// inner nesting is by global etymon index
	// cells are filled when and only when we find
	// changes that are only in the baseline/hyp are found
	// these strings are simple X > Y
	// (whole word forms)
	// TODO : enclose changed phonemes with {} -- but at present this doesn't seem worth the work necessary to realize it. 
	
	private int divergencePoint;
	// the first global time step at which at least one word's resultant form is different between the two cascades.
	// this is *DISTINCT* from the first "domino" as returned by locateFirstDomino() because that 
	//			(a)	pertains to only one word and 
	//			(b) is specifically the first time the effect of a rule that is not one of the ones stipulated by the proposed changes is effected 
						// -- i.e. a bleeding or feeding effect.

	private boolean[] locHasPrCh;
	// one cell for each *global* rule index -- i.e. the inner nesting in  ruleCorrespondences
	// by default, false
	// true only for those indices of those operations which are either deleted or added
	// 			as part of the transformation of the baseline cascade to the hypothesis cascade.

	public DifferentialHypothesisSimulator(Simulation b, Simulation h, int[] baseToHypIndMap, int[] hypToBaseIndMap,
			List<String[]> propdChanges) {
		baseCascSim = b;
		hypCascSim = h;
		proposedChs = propdChanges;
		computeRuleCorrespondences(baseToHypIndMap, hypToBaseIndMap); // init ruleCorrespondences

		makeIndexGlobalizers(); // init baseRuleIndsToGlobal, hypRuleIndsToGlobal

		computeTrajectoryChange(); // changedRuleEffects, changedDerivations.
	}

	
	// generate class variables ruleCorrespondences and locHasPrCh
	// ruleCorrespondences -- each int[] (inner length = 2) instance in RELOCDS is [deleteLoc, addLoc)
	// @prerequisite: class variables baseCascSim, hypCascSim and proposedChs are already set
	// format of @param baseToHypIndMap: index -- cell number in base -- contains: corresponding cell number in hyp
	// @param hypToBaseIndMap -- is the reverse
	private void computeRuleCorrespondences(int[] baseToHypIndMap, int[] hypToBaseIndMap) {
	
		// initialize dummy versions of the mappings
		int[] dumRIMBH = new int[baseToHypIndMap.length], dumRIMHB = new int[hypToBaseIndMap.length];
		for (int bhi = 0; bhi < dumRIMBH.length; bhi++)	dumRIMBH[bhi] = baseToHypIndMap[bhi];
		for (int hbi = 0; hbi < dumRIMHB.length; hbi++)	dumRIMHB[hbi] = hypToBaseIndMap[hbi];
	
		// initializing class variable ruleCorrespondences...
		if (proposedChs.size() == 0) {
			assert UTILS.compare1dIntArrs(baseToHypIndMap,
					hypToBaseIndMap) : "ERROR: no proposed changes detected, "
							+ "but baseToHypIndMap and hypToBaseIndMap differ";
			ruleCorrespondences = new int[][] { baseToHypIndMap, hypToBaseIndMap };
		} 
		else {
			/**
			 * ruleCorrespondences -- tracked rule pairs in hyp and base cascs share the same GLOBAL index 
			 * a rule in baseCasc is tracked unless it is deleted or non-bijectively modified 
			 *			likewise one in hypCasc is unless it was inserted 	or  the result of non-bijective modification. 
			 * ordering of the "global indices" of ruleCorrespondences is primarily built off of the baseline side of things 
			 * 			with	additions every place there was an insertion 
			 * 					(so, where there is relocdation,
			 * 							it will always agree once effective insertions are accounted for with the base index) 
			 * 	length of ruleCorrespondences should be : 
			 * 			#shared rules + #deletions + #insertions 
			 * 				(baseCasc : shared + deletions; hypCasc: shared + insertions)
			 */
			 
			 int baseLen = baseCascSim.getTotalSteps(), hypLen = hypCascSim.getTotalSteps(); 
			 int globLen = hypLen; 
			// get the total length value from the hypothesis cascade,
			// 	because this is the only we to capture the case of modifications or insertions
			// 		that add multiple rules at once,  or single insertions for that matter.
			// increment total_length for each time there is a deleted index from the baseline
			for (int bihimi : baseToHypIndMap)	if (bihimi == -1)		globLen += 1;
			
			locHasPrCh = new boolean[globLen]; //indices are global
			
			ruleCorrespondences = new int[2][globLen];
			// init ruleCorrespondences with -2 
			// -2 thus means the cell is untouched.
			// ([0] could be the result of an operation)
			// indices for this are also global.
			for (int rci = 0; rci < globLen; rci++) {
				ruleCorrespondences[0][rci] = -2; ruleCorrespondences[1][rci] = -2;
			}
			
			List<Integer> fwd_dests_left = new ArrayList<Integer>(); 
				// new entries added when we encounter current sources in baseline for forward relocdation
				// deleted when the corresponding destination in the hyp cascade is reached
				// as confirmed by the presence of the index in this list. 
			HashMap<Integer,Integer> bwd_srcs_left = new HashMap<Integer,Integer>();
				// key -- src, value -- dest
				// new entries added when we encounter current destinations in hyp cascade for backward relocdation.
				// removed when corresponding source in the baseline cascade is reached
				// as confirmed by presence of the baseline index as a key. 
							
			int gi = 0, bi = 0, hi = 0, pci = 0; 
				//as of May 2020, new version of this algorithm will use proposedChs as the prime method of iteration, rather than RIMBH and RIMHB... 
				//pci -- index in proposedChs
				//gi, bi, hi -- current (global, baseline cascade, hypothesis cascade) indices
			
			List<String[]> propChs = new ArrayList<String[]> (proposedChs); 
			while(pci < propChs.size())  // trim out 1-1 modifications, which are basically non-changes from the perspective of rule correspondences. 
			{
				if(propChs.get(pci)[2].contains(":bijective modification:")) propChs.remove(pci);
				else	pci++; 
			}
			pci = 0; 
			
			while (pci < proposedChs.size()) 
			{
				String[] nextPC = proposedChs.get(pci++); 
				int sameUntil = Integer.parseInt(nextPC[0]); 
				
				while (hi < sameUntil) { // simply pass any spots that were not modified.
					ruleCorrespondences[0][gi] = bi++; 
					ruleCorrespondences[1][gi++] = hi++; } 
				
				//now we should be on an index of operation. 
				
				int ilbi = dumRIMBH[bi]; 
				
				if ("deletion".equals(nextPC[1]) ) 
				{
					if(DHSWrapper.validRelocdationNotes(nextPC[2])) // this is the deletion aspect of a relocdation.
					{
						String inds = nextPC[2].substring(16); 
						int src_loc = UTILS.getIntPrefix(inds);
						int dest_loc = UTILS.getIntPrefix(inds.substring(inds.indexOf(" ")+4)) ;
						
						if (dest_loc > src_loc) { //i.e. current source for forward relocdation that will be done
							if (ilbi <= hi)	throw new RuntimeException("Error: detected forward relocdation, but dumRIMBH[bi] (= "+dumRIMBH[bi]+" ) is greater than hi (= "+hi+")!");
							fwd_dests_left.add(ilbi);
							ruleCorrespondences[1][gi] = ilbi;
							ruleCorrespondences[0][gi] = bi++; 
							locHasPrCh[gi++] = true;  
						}
						else // current source for backward relocdation that has been done
						{
							if (!bwd_srcs_left.containsKey(bi))	throw new RuntimeException("Detected a backward relocdation source, but current base index "+bi+" is not a key in bwd_srcs_left!"); 
							
							if (bwd_srcs_left.remove(bi) != dumRIMBH[bi])	throw new RuntimeException("Destination for backward relocdation is wrong!"); 
								// ... is bwd_srcs_left really necessary though? 
							ruleCorrespondences[1][gi] = dumRIMBH[bi];
							ruleCorrespondences[0][gi++] = bi++;
							// mark in locHasPrCh however is done at the destination.  
						}
					}
					else	//simple deletion or deletion aspect of modification.  
					{
						if (ilbi != -1)	throw new RuntimeException("Detected a simple deletion operation, but ilbi != -1 (ilbi = "+ilbi+")\n"
								+ "bi "+bi+", hi "+hi);
						ruleCorrespondences[0][gi] = bi++; 
						ruleCorrespondences[1][gi] = -1; 
						locHasPrCh[gi++] = true; 
					}
				}
				else // insertion type operation 
				{
					if ( DHSWrapper.validRelocdationNotes(nextPC[2]))	//insertion aspect of relocdation
					{
						String inds = nextPC[2].substring(16); 
						int src_loc = UTILS.getIntPrefix(inds); 
						int dest_loc = UTILS.getIntPrefix(inds.substring(inds.indexOf(" ")+4)); 
						
						if (dest_loc > src_loc) // resolution of a forward relocdation. 
						{
							if (!fwd_dests_left.contains(hi))	throw new RuntimeException("Error: detected the resolution (destination) of a forward relocdation, but hi (= "+hi+") is not in fwd_dests_left!");
							// no need to do anything with ruleCorrespondences or locHasPrCh -- these were handled at the source. 
							fwd_dests_left.remove(fwd_dests_left.indexOf(hi++)); //call this way to avoid errors in case where hi < fwd_dests_left.size().  
						}  
						else // current destination. for backward relocdation. 
						{
							locHasPrCh[gi] = true;  // do not count up global index though.
							bwd_srcs_left.put(dumRIMHB[hi], hi); 
							hi++; 
						} 
					}
					else // simple insertion or insertion aspect of modification
					{
						if (dumRIMHB[hi] != -1)	throw new RuntimeException("Detected insertion in proposedChs, but ilhi is not -1! (hi = "+hi+", ilhi = "+dumRIMHB[hi]+")");  

						//modify necessary structures for each...
						for (int quant = UTILS.countDisjunctContexts(nextPC[1]); quant > 0; quant--) {
							ruleCorrespondences[0][gi] = -1; 
							ruleCorrespondences[1][gi] = hi++;
							locHasPrCh[gi++] = true;
						}
					}
				}
			}
			
			if (globLen - gi != baseLen - bi || globLen - gi!= hypLen - hi)
				throw new RuntimeException("Error: misalignment after last proposed change accounted for in computeRuleCorrespondences...\n"
						+ "gi "+gi+"  bi "+bi+"  hi "+hi+"  globLen "+globLen+"  hypLen "+hypLen+"  baseLen "+baseLen); 
			while (gi < globLen) {
				ruleCorrespondences[0][gi] = bi++; 
				ruleCorrespondences[1][gi++] = hi++; 
			}
		}
	}
			
	
	/** old version: 
	// generate class variables ruleCorrespondences and locHasPrCh
	// ruleCorrespondences -- each int[] (inner length = 2) instance in RELOCDS is [deleteLoc, addLoc)
	// @prerequisite: class variables baseCascSim, hypCascSim and proposedChs are already set
	// format of @param baseToHypIndMap: index -- cell number in base -- contains: corresponding cell number in hyp
	// @param hypToBaseIndMap -- is the reverse
	private void computeRuleCorrespondences(int[] baseToHypIndMap, int[] hypToBaseIndMap) {

		// initialize dummy versions of the mappings
		int[] dumRIMBH = new int[baseToHypIndMap.length], dumRIMHB = new int[hypToBaseIndMap.length];
		for (int bhi = 0; bhi < dumRIMBH.length; bhi++)	dumRIMBH[bhi] = baseToHypIndMap[bhi];
		for (int hbi = 0; hbi < dumRIMHB.length; hbi++)	dumRIMHB[hbi] = hypToBaseIndMap[hbi];
		
		// initializing class variable ruleCorrespondences...
		if (proposedChs.size() == 0) {
			assert UTILS.compare1dIntArrs(baseToHypIndMap,
					hypToBaseIndMap) : "ERROR: no proposed changes detected, "
							+ "but baseToHypIndMap and hypToBaseIndMap differ";
			ruleCorrespondences = new int[][] { baseToHypIndMap, hypToBaseIndMap };
		} 
		else {
			/**
			 * ruleCorrespondences -- tracked rule pairs in hyp and base cascs share the
			 *		 same INNER index a rule in baseCasc is tracked 
			 *					unless it is deleted or non-bijectively modified 
			 *			likewise one in hypCasc is unless it was inserted,
			 * 						or the result of non-bijective modification. 
			 * ordering of the "global indices" of ruleCorrespondences is primarily built off of the base side of things 
			 * 			with	additions every place there was an insertion 
			 * 					(so, where there is relocdation,
			 * 							it will always agree once effective insertions are accounted for with the base index) 
			 * 	length of ruleCorrespondences should be : 
			 * 			#shared rules + #deletions + #insertions 
			 * 				(baseCasc : shared + deletions; hypCasc: shared + insertions)
			 */
/**
			int baseLen = baseCascSim.getTotalSteps(), 
					hypLen = hypCascSim.getTotalSteps();

			int globLen = hypLen;
			// get the total length value from the hypothesis cascade,
			// 	because this is the only we to capture the case of modifications or insertions
			// 		that add multiple rules at once,  or single insertions for that matter.
			// increment total_length for each time there is a deleted index from the baseline
			for (int bihimi : baseToHypIndMap)	if (bihimi == -1)		globLen += 1;

			locHasPrCh = new boolean[globLen];
			// the indices of locHasPrCh are the GLOBAL indices.

			ruleCorrespondences = new int[2][globLen];
			
			// init ruleCorrespondences with -2 
			// -2 thus means the cell is untouched.
			// ([0] could be the result of an operation)
			// indices for this are also global.
			for (int rci = 0; rci < globLen; rci++) {
				ruleCorrespondences[0][rci] = -2; ruleCorrespondences[1][rci] = -2;
			}

			int sameUntil = Integer.parseInt(proposedChs.get(0)[0]);
			int gi = 0, bi = 0, hi = 0; 
				// global, base, and hyp instant iterators
			int pci = 0; //iterator for proposedChs. 
			
			while (gi < sameUntil) 	{
				ruleCorrespondences[0][gi] = bi++; 
				ruleCorrespondences[1][gi++] = hi++;
			}
			
			HashMap<Integer,Integer> fut_sources_left = new HashMap<Integer,Integer>(); 
			List<Integer> unresolved_past_sources = new ArrayList<Integer>(); 
				// for purposes of locHasPrCh, so that all the one-step back relocdations used 
					// as proxies for a single forward relocdations are not counted as genuine changes
						// between the baseline and hypothesis cascades.
			
			while (gi < globLen) {
				int ilbi = (bi < baseLen) ? dumRIMBH[bi] : -1, // index linked to current base instant
						ilhi = (hi < hypLen) ? dumRIMHB[hi] : -1; // index linked to current hyp instant
				
				if (ilbi == hi && ilhi == bi) { // i.e. the current spots in the base and hyp share the same global index
					ruleCorrespondences[0][gi] = bi++;
					ruleCorrespondences[1][gi] = hi++;
					gi++; 
				} 
				else // we will have to do some asymmetrical operation -- and there must have been a non-bijective propCh here
				{
					/**
					 * There are six possible cases here 
					 * (1) a deletion of some sort including as  part of non-bijective modification: ilbi == -1 
					 * (2) a insertion of some sort including as part of a non-bijective modification ilhi == -1 
					 * (3) a forward relocdation that has not yet been handled/realized in ruleCorrespondences
					 * 						ilbi > hi 
					 * 			note that forward relocdation from X to Y can be reinterpeted as |Y-X| backward relocdations each by one place
					 * (4) a forward relocdation that has already been handled/realized in ruleCorrespondences 
					 * 								ilhi < bi && ilhi != -1 && findInt(ruleCorrespondences[1], hi) != -1 
					 * 			* we are now indicating this by placing -2 at in dumRIMHB[<destination>] 
					 * (5) backward relocdation that has not yet been handled/realized in ruleCorrespondences -- detectable on HB side
					 * 					ilhi > bi 
					 * (6) backward relocdation that has already been handled/realized in ruleCorrespondences
					 * 
					 * As of April 13, 2020, we are now, for purposes of simplicity, reinterpreting all forward relocdations 
					 * 			as backward relocdation
					 * a forward relocdation from X to Y is |Y-X| backward relocdations, each by one place. 
					 * 
					 * a future backward relocdation is detected as a case of : 		ilhi > bi
					 * 					in this case we 
					 * 							store the current hi in fut_sources_remaining with ilhi as the key
					 * 							increment only hi
					 * 							and set dumRIMBH[ilhi] = -2 
					 * a current backward relocdation is detected when ilbi hits that -2
					 * 					and only gi and bi are incremented in this case. 
					 * 
					 * HOWEVER, the computation of locHasPrChs will still treat this as a single change. 
					 *//**
					
					if ( ilbi == -2) // resolution of relocdation 
					{
						if(!fut_sources_left.containsKey(bi))	throw new RuntimeException("ERROR: found flag for backward relocdation resolution (ilhi == -2)\n "
								+ "but the current base index (bi = "+bi+") is not in fut_sources_left!"); 
						int past_corr = fut_sources_left.remove(bi); 
						ruleCorrespondences[1][gi] = past_corr;
						ruleCorrespondences[0][gi] = bi++; 
						
						if(!unresolved_past_sources.contains(past_corr))	// if it wasn't actually a forward relocdation then. 
							locHasPrCh[gi] = true; 
						else	unresolved_past_sources.remove(unresolved_past_sources.indexOf(past_corr));
						gi++; 
					} 
					else if (ilhi == -1 && ilbi > hi && isForwardRelocd(hi,ilbi)) // specific bandaid for error case of current forward relocdation at same index as assertion -- curr forward relocdation to be handled first. 
					{
						//TODO debugging
						System.out.println("Bandaid at bi "+bi+" hi "+hi+" ilbi "+ilbi); 
						
						// we know this cannot be a backward relocdation, as ilhi = -1. 
						unresolved_past_sources.add(ilbi); 
						locHasPrCh[gi] = true; 
						fut_sources_left.put(bi, ilbi); 
						dumRIMBH[bi] = -2;
					}
					else if (ilbi == -1 || ilhi == -1) // insertion or deletion  
					{
						//assert ilbi != ilhi : "ERROR: cannot have a rule that exists neither that has neither a base nor hyp index";
						
						//insertion comes first, deletion is considered to happen to LATER inds (ind numbers are adjusted to avoid errors)
						boolean isInsertion = (ilhi == -1); 
						ruleCorrespondences[0][gi] = isInsertion ? ilhi : bi++; 
						ruleCorrespondences[1][gi] = isInsertion ? hi++ : ilbi; 
						locHasPrCh[gi++] = true;
					}
					else if (ilhi > bi) // future backwards relocdation (or current forward relocdation?)
					{
						if (!relocdIsBackward(hi)) {
							unresolved_past_sources.add(hi);  
							locHasPrCh[gi] = true; 
						}
						fut_sources_left.put(ilhi, hi++);
						dumRIMBH[ilhi] = -2; 
					}
					else		throw new Error("ERROR: should not never have ilhi < bi but ilhi = "+ilhi+" and bi = "+bi); 
				}
			}
		}
	}**/

	/**
	 * to initialize baseRuleIndsToGlobal, hypRuleIndsToGlobal
	 * 
	 * @prerequisite: must have already called computeRuleCorrespondences
	 */
	private void makeIndexGlobalizers() {
		baseRuleIndsToGlobal = new int[baseCascSim.getTotalSteps()];
		hypRuleIndsToGlobal = new int[hypCascSim.getTotalSteps()];

		for (int rci = 0; rci < ruleCorrespondences[0].length; rci++) {
			if (ruleCorrespondences[0][rci] != -1)
				baseRuleIndsToGlobal[ruleCorrespondences[0][rci]] = rci;
			if (ruleCorrespondences[1][rci] != -1)
				hypRuleIndsToGlobal[ruleCorrespondences[1][rci]] = rci;
		}
	}

	/**
	 * fills changedDerivations and changedRuleEffects, and sets divergencePoint.
	 */
	private void computeTrajectoryChange() {
		divergencePoint = -1; // will remain -1 if two cascades never diverge for a single word.

		if (baseCascSim.NUM_ETYMA() != hypCascSim.NUM_ETYMA() )
			throw new RuntimeException("ERROR: Inconsistent number of etyma between base and hypothesis cascade simulation objects"); 
		int n_ets = baseCascSim.NUM_ETYMA();

		changedDerivations = new HashMap<Integer, String>();
		changedRuleEffects = new HashMap<Integer, String[][]>();

		for (int ei = 0; ei < n_ets; ei++) {
			int lexDivPt = findEtDivergenceMoment(ei);

			// recall -- if findEtDivergenceMoment() returns -1 it means there is no
			// difference.
			if (lexDivPt != -1) {
				if (divergencePoint == -1)
					divergencePoint = lexDivPt;
				else
					divergencePoint = Math.min(divergencePoint, lexDivPt);

				String ddHere = getDifferentialDerivation(ei);
				changedDerivations.put(ei, ddHere);

				HashMap<Integer, String[]> lexChRuleEffs = fedOrBledRuleLinesInDD(ei);
				for (int gi : lexChRuleEffs.keySet()) // gi -- global index. Can access base and hyp inds via <ruleCorrespondences> 
				{
					String[][] diffEffsHere = changedRuleEffects.containsKey(gi) ? 
							changedRuleEffects.get(gi) : new String[2][baseCascSim.NUM_ETYMA()];
					String[] effsHere = lexChRuleEffs.get(gi); 
					int i = effsHere[1].contains(">") ? 1 : 0; 
					diffEffsHere[i][ei] = effsHere[i]; 
					changedRuleEffects.put(gi, diffEffsHere);
				}
			}
		}
	}

	/**	fedOrBledRulesInDD
	 * auxiliary for making changeRuleEffects in computeTrajectoryChange
	 * in such a way that non-changed rules that are now realized on opposite sides 
	 * 		of a relocdated rule are no longer counted as "changes".
	 * @return hashmap instance where keys are the global inds, and the values are the effects
	 * 		in String[] form
	 * 	will be empty (no keys) if there are no changes.
	 * 
	 * // since modification is stored here as a deletion and an insertion,
						// it will not be represented as a single rule in a differential derivation
						// therefore, there is a bidirectional implication between non-equivalence of
						// rule effects "hitting" words
						// and changes in rule effects,
						// so hit[0] != hit[1] is a perfect proxy.
							/* EXCEPT in one case: where, due to a relocdation, the same (non-relocdated)
							 * 	rule is realized before the relocdated rule in one of base/hyp 
							 * 			and after in the other
							 * 	thus we have to check for this case, 
							 *				by removing inds which have already been hit. 
							 */
	private HashMap<Integer,String[]> fedOrBledRuleLinesInDD (int et_id) {
		HashMap<Integer, String[]> output = new HashMap<Integer, String[]> (); 
		int lexDivPt = findEtDivergenceMoment(et_id); 
		if (lexDivPt == -1)	return output; 
		
		String dd = getDifferentialDerivation(et_id); 
		String[] lines = dd.substring(dd.indexOf("CONCORD")).split("\n"); 
		for (int li = 1; li < lines.length; li++) // starting at li = 1 to skip "CONCORDANT UNTIL..." line.
		{
			String cur = lines[li]; 
			if (cur.contains(">"))	{
				int globInd = Integer.parseInt(cur.substring(0, cur.indexOf("[")));
				if (output.containsKey(globInd))	output.remove(globInd); 
				else
				{
					String[] effs = cur.substring(cur.indexOf(": ")+2).split(" \\| ");
					boolean[] hit = new boolean[] { effs[0].contains(">"), effs[1].contains(">")};
					if ( hit[0] != hit[1] )	output.put(globInd, effs); 
				}
			}
		}
		
		return output;
	}
	
	/**
	 * @return earliest (global) *moment* that the baseline and hyp derivs diverge
	 *         for one etyma
	 * @return -1 -- if there is no divergence at all
	 * @param et_id -- index of the etymon.
	 */
	private int findEtDivergenceMoment(int et_id) {
		String dd = getDifferentialDerivation(et_id);
		if (dd.equals(""))
			return -1;
		dd = dd.substring(dd.indexOf("CONCORDANT UNTIL RULE : "));
		dd = dd.substring(dd.indexOf(":") + 2, dd.indexOf("\n"));
		return Integer.parseInt(dd);
	}

	/**
	 * getDifferentialDerivation
	 * 
	 * @return the differential derivation for a particular etymon 
	 *	 the etymon being indexed by @param et_id this is the a two-sided derivation
	 *         which shows the engendered difference between the baseline cascade
	 *         and the hypothesis cascade
	 * @return the empty string "" if there is no difference.
	 */
	public String getDifferentialDerivation(int et_id) {
		String baseDer = baseCascSim.getDerivation(et_id), hypDer = hypCascSim.getDerivation(et_id);
		if (baseDer.equals(hypDer))
			return "";
		// passing here does not exclude the possibility of an identical derivation
		// -- we will have to use ruleCorrespondences to ascertain that.
		// we do this by changing the rule index numbers in both derivations to their
		// "global" indices in ruleCorrespondences
		// conveniently handled with mapping arrays

		baseDer = globalizeDerivInds(baseDer, false);
		hypDer = globalizeDerivInds(hypDer, true);

		if (baseDer.equals(hypDer))
			return "";
		// now we know they are indeed different -- so fill in info on how...

		String[] bdlines = baseDer.split("\n"), hdlines = hypDer.split("\n");

		assert bdlines[0].equals(hdlines[0]) : "Error: inconsistent initial line between two corresponding lexical derivations";
			// this one kept; although in a public method, it makes sense as an assertion. 
		String out = bdlines[0];
		int bdli = globalDivergenceLine(baseDer, hypDer), hdli = bdli;

		String lastBform = "", lastHform = "";
		if (bdli == 1) {
			lastBform = bdlines[0].replace("/", "#");
			lastHform = hdlines[0].replace("/", "#");
		} else {
			// note that at this point bdli and hdli are essentially interchangeable.
			lastBform = (bdlines[bdli - 1].contains(" \\|") ? bdlines : hdlines)[bdli - 1].split(" \\|")[0];
			lastHform = (hdlines[hdli - 1].contains(" \\|") ? hdlines : bdlines)[bdli - 1].split(" \\|")[0];

			if (lastBform.contains("stage form : "))
				lastBform = lastBform.split("stage form : ")[1];
			if (lastHform.contains("stage form : "))
				lastHform = lastHform.split("stage form : ")[1];
		}

		int nextGlobalBaseInd = UTILS.extractInd(bdlines[bdli]), nextGlobalHypInd = UTILS.extractInd(hdlines[hdli]);

		// we know next line cannot be gold/black stage announcement as that could not
		// be the first line with divergence.
		// cannot allow divergence to be at something other than a rule realization --
		// throw RuntimeException if so
		if (nextGlobalBaseInd == -1 && nextGlobalBaseInd == nextGlobalHypInd) 
			throw new RuntimeException("Error : cannot have divergence occur due to something other than a difference in sound rules.");

		int concordantUntil = Math.min(nextGlobalBaseInd, nextGlobalHypInd);
		concordantUntil = concordantUntil != -1 ? concordantUntil : Math.max(nextGlobalBaseInd, nextGlobalHypInd);
		out += "\nCONCORDANT UNTIL RULE : " + concordantUntil;

		// recall -- we have already aligned the numbers in the two derivations using
		// derivationToGlobalInds()
		// so it is obvious if we are dealing with a deletion or insertion as it is
		// simply absent on the other side.

		// we now will no longer necessarily be iterating simultaneously on bdlines and hdlines
		// we only do so when they have the same rule (same = same global index)
		// when the index is not shared, we are handling a case of deletion/insertion,
		// or bleeding/feeding as a result of the rule change.

		while (bdli < bdlines.length && hdli < hdlines.length) {
			int[] stageHere = new int[] { bdlines[bdli].indexOf("stage form "),
					hdlines[hdli].indexOf("stage form : ") };

			boolean[] isFin = new boolean[] { bdlines[bdli].substring(0, 5).equals("Final"),
					hdlines[hdli].substring(0, 5).equals("Final") };

			nextGlobalBaseInd = (isFin[0] || stageHere[0] > -1) ? -1 : UTILS.extractInd(bdlines[bdli]);
			nextGlobalHypInd = (isFin[1] || stageHere[1] > -1) ? -1 : UTILS.extractInd(hdlines[hdli]);

			if (nextGlobalBaseInd == nextGlobalHypInd && nextGlobalBaseInd != -1) {
				String nextBform = bdlines[bdli].substring(0, bdlines[bdli].indexOf(" | ")),
						nextHform = hdlines[hdli].substring(0, hdlines[hdli].indexOf(" | "));

				out += "\n" + nextGlobalBaseInd + "[" + ruleCorrespondences[0][nextGlobalBaseInd] + "|"
						+ ruleCorrespondences[1][nextGlobalHypInd] + "] : " + lastBform + " > " + nextBform + " | "
						+ lastHform + " > " + nextHform;
				lastBform = nextBform;
				lastHform = nextHform;
				bdli++;
				hdli++;
			} else if (stageHere[0] > -1 && stageHere[1] > -1) {
				out += "\n" + bdlines[bdli].substring(0, stageHere[0]) + bdlines[bdli].substring(stageHere[0] + 11)
						+ " | " + hdlines[hdli].substring(stageHere[1] + 13);
				bdli++;
				hdli++;
			} else if (isFin[0] && isFin[1]) {
				out += "\nFinal forms : " + bdlines[bdli].substring(bdlines[bdli].indexOf(":") + 2) + " | "
						+ hdlines[hdli].substring(hdlines[hdli].lastIndexOf(":") + 2);
				hdli++;
				bdli++;
			} else if (nextGlobalHypInd == -1 ? true : nextGlobalBaseInd < nextGlobalHypInd && nextGlobalBaseInd != -1) // deletion
																														// or
																														// bleeding
			{
				String nextBform = bdlines[bdli].substring(0, bdlines[bdli].indexOf("|") - 1);

				out += "\n" + nextGlobalBaseInd + "[" + ruleCorrespondences[0][nextGlobalBaseInd] + "|-1] : "
						+ lastBform + " > " + nextBform + " | bled or deleted";
				bdli++;
				lastBform = nextBform;
			} else // insertion or feeding
			{
				String nextHform = hdlines[hdli].substring(0, hdlines[hdli].indexOf(" | "));
				out += "\n" + nextGlobalHypInd + "[-1|" + ruleCorrespondences[1][nextGlobalHypInd] + "] : "
						+ "fed or inserted | " + lastHform + " > " + nextHform;
				hdli++;
				lastHform = nextHform;
			}
		}

		return out;
	}

	// TODO plans to report any change in phonemic inventory.
	// isHyp -- hypothesis not baseline
	// der -- a derivation. 
	private String globalizeDerivInds(String der, boolean isHyp) {
		int br = der.indexOf("\n");
		String out = der.substring(0, der.indexOf("\n"));
		String[] lines = der.substring(br + "\n".length()).split("\n");

		for (String li : lines) {
			int br1 = li.indexOf(" | "), br2 = li.lastIndexOf(" : ");
			if (br1 != -1 && br2 != -1) {
				out += "\n" + li.substring(0, br1 + 3);
				int raw_ind = Integer.parseInt(li.substring(br1 + 3, br2).trim());
				out += (isHyp ? hypRuleIndsToGlobal : baseRuleIndsToGlobal)[raw_ind] + li.substring(br2);
			} else
				out += "\n" + li;
		}
		return out;
	}

	/* This comment block may be misplaced. But I don't think so?
	 * @param et_id -- etymon index, which should be consistent between the two
	 *              Simulations.
	 * @return @default an empty String ""- means there is no difference between the derivations
	 * 		 otherwise: the DIFFERENTIAL DERIVATION, formed as follows: 
	 * 		// String will contain 
	 * 		// First line --- <INPUT> \n //
	 *      CONCORDANT UNTIL : <INDEX AFTER LAST CONCORDANT RULE> \n 
	 *      		// for 1-to-1 divergent development : 
	 *      				<GLOBALRULE#> : [BASERULE#] <OLDFORM> > <NEWFORM> | [HYPRULE#] NEW > OLD \n 
	 *      // deletion (i.e. occurs only in baseline: <BASERULE#>: <OLDFORM> > <NEWFORM> | -- \n 
	 *      // insertion: the reverse.
	*/ 
	public String getGlobalizedDerivation(int et_id, boolean isHyp) {
		return globalizeDerivInds((isHyp ? hypCascSim : baseCascSim).getDerivation(et_id), isHyp);
	}

	// prints basic info on changes in words effected
	// and rules effected
	// does not print evaluation statistic change -- that is for
	// DiachronicSimulation and ErrorAnalysis to handle.
	public void printBasicResults() {
		// TODO need to fix this
		// TODO but what was the error...?

		// Analysis of changes transforming base -> hyp upon the effects of rules.
		System.out.println("ANALYSIS OF EFFECT OF PROPOSED CHANGES:\n");
		System.out.println("Last rule before divergence: " + divergencePoint); // TODO may have to debug this...
		System.out.println("Effects of specific changes between baseline and proposed cascade.");
		for (int globInd = 0; globInd < ruleCorrespondences[0].length; globInd++) {
			if (locHasPrCh[globInd]) // true -- implies this was one of the rules that are specifically modified between the hyp and baseline 
			{
				if (ruleCorrespondences[0][globInd] == -1) {
					System.out.println("Developments directly caused by a proposed change:"); // i.e. in hyp but not
																								// baseline.
					System.out.println(strEffects(changedRuleEffects.get(globInd)[1]));
				} 
				else {
					assert ruleCorrespondences[1][globInd] == -1 : "Error: comprehension of which rules were added is malformed"; 
										// assertion in public method, but makes sense as such. 
					System.out.println("Developments directly aborted by proposed change:");
					System.out.println(strEffects(changedRuleEffects.get(globInd)[0]));
				}
			}
		}

		System.out.println("Effects on rules other than those explicitly changed:\n");
		for (int globInd = 0; globInd < ruleCorrespondences[0].length; globInd++) {
			if (!locHasPrCh[globInd]) // false -- implies this was not one of the rules that are specific changes, so
										// changes must be bleeding or feeding effects.
			{
				int baseInd = ruleCorrespondences[0][globInd];
				String[] bleedings = changedRuleEffects.get(globInd)[0], feedings = changedRuleEffects.get(globInd)[1];

				System.out.println("Baseline rule " + baseInd + " (global: " + globInd + ")\n\tbled for "
						+ UTILS.numFilled(bleedings) + " etyma, fed for " + UTILS.numFilled(feedings) + ".");

				System.out.println("Bled: " + strEffects(bleedings));
				System.out.println("Fed: " + strEffects(feedings));

			}
		}

		boolean prgold = baseCascSim.hasGoldOutput();

		System.out.println("\nEtymon effected: (#: BASE>HYP" + (prgold ? "[GOLD]" : "") + ")");

		HashMap<Integer, List<Integer>> classedChdEts = changedEtsClassifiedByFirstDomino();

		for (List<Integer> ets : classedChdEts.values()) {
			for (int et : ets) {
				System.out.print("" + et + ": ");
				String thisdd = changedDerivations.get(et);
				String lastline = thisdd.substring(thisdd.lastIndexOf("\n") + "\n".length());
				System.out.println(lastline.substring(thisdd.indexOf(">") + 2, lastline.indexOf("|") - 1) + " >> "
						+ lastline.substring(lastline.lastIndexOf(">") + 2, lastline.length()) + "; ");
				// TODO beautify this?
			}

		}

	}

	// @ precondition; have called computeTrajectoryChanges()
	private HashMap<Integer, List<Integer>> changedEtsClassifiedByFirstDomino() {
		HashMap<Integer, List<Integer>> out = new HashMap<Integer, List<Integer>>();
		for (int ei : changedDerivations.keySet()) {
			int fd = locateFirstDomino(ei);
			if (out.containsKey(fd)) {
				List<Integer> val = out.get(fd);
				val.add(ei);
				out.put(fd, val);
			} else
				out.put(fd, new ArrayList<Integer>(Arrays.asList(Integer.valueOf(ei))));
		}
		return out;
	}

	// if there is at least one bleeding or feeding effect on this etID, return
	// global ind for it
	// if the et has no changes, return -1
	// otherwise return the global ind of the first effectual proposed change for
	// this etymon.
	private int locateFirstDomino(int etID) {
		if (!changedDerivations.containsKey(etID))
			return -1;
		String dd = changedDerivations.get(etID);
		dd = dd.substring(dd.indexOf("CONC"));
		dd = dd.substring(dd.indexOf("\n") + "\n".length());

		// we know the next line must be the proposed change
		int ogi = Integer.parseInt(dd.substring(0, dd.indexOf("[")));
		int gi = ogi;
		dd = dd.substring(dd.indexOf("\n") + "\n".length());

		while (dd.contains("\n")) {
			while (dd.contains("\n") ? !dd.substring(0, dd.indexOf("\n")).contains(">") : false)
				dd = dd.substring(0, dd.indexOf("\n") + "\n".length());
			if (dd.contains("\n")) {
				gi = Integer.parseInt(dd.substring(0, dd.indexOf("[")));
				String[] effs = dd.substring(dd.indexOf(": ") + 2, dd.indexOf("\n")).split(" | ");
				if (effs[0].contains(">") != effs[1].contains(">")) // either bleeding/feeding or insertion/deletion
					if (ruleCorrespondences[0][gi] != -1 && ruleCorrespondences[1][gi] == -1)
						// i.e. if false this is an insertion or deletion, i.e. one of the proposed
						// changes.
						return gi;
				dd = dd.substring(0, dd.indexOf("\n") + "\n".length());
			}
		}

		gi = Integer.parseInt(dd.substring(0, dd.indexOf("[")));
		String[] effs = dd.substring(dd.indexOf(": ") + 2, dd.indexOf("\n")).split(" | ");
		if (effs[0].contains(">") != effs[1].contains(">")) // either bleeding/feeding or insertion/deletion
			if (ruleCorrespondences[0][gi] != -1 && ruleCorrespondences[1][gi] == -1)
				// i.e. if false this is an insertion or deletion, i.e. one of the proposed
				// changes.
				return gi;

		return ogi;
	}

	public int[][] getRuleCorrespondences() {
		return ruleCorrespondences;
	}

	/**
	 * findEtDivergenceLine
	 * 
	 * @return the earliest *line* where the baseline and hypothesis derivations for
	 *         one etyma diverge
	 * @return -1 -- if there is no divergence at all.
	 * @param et_id -- index of the etymon
	 */
	private int findEtDivergenceLine(int et_id) {
		String bd = baseCascSim.getDerivation(et_id), hd = hypCascSim.getDerivation(et_id);
		if (bd.equals(hd))
			return -1;
		bd = globalizeDerivInds(bd, false);
		hd = globalizeDerivInds(hd, true);
		if (bd.equals(hd))
			return -1;

		return globalDivergenceLine(bd, hd);
	}

	/**
	 * find the line of divergence between two line-split derivations of the same
	 * word derivations entered should already be "globalized"
	 */
	private int globalDivergenceLine(String bd, String hd) {
		if (bd.equals(hd))		throw new RuntimeException("ERROR: tried to find divergence point for two identical derivations!");

		// recall that the first line indicates the input form -- hence etymon identity
		String[] bdlns = bd.split("\n"), hdlns = hd.split("\n");
		if (!bdlns[0].equals(hdlns[0]))	
			throw new RuntimeException("ERROR: tried to find divergence point for derivations with different inputs");
		
		int out = 1, minlen = Math.min(bdlns.length, hdlns.length);

		// determine how long the derivations remain consistent.
		while (out >= minlen ? false : bdlns[out].equals(hdlns[out]))
			out++;

		return out;
	}

	// accessors.
	public int[] getBaseIndsToGlobal() {
		return baseRuleIndsToGlobal;
	}

	public int[] getHypIndsToGlobal() {
		return hypRuleIndsToGlobal;
	}

	public boolean[] getPrChLocs() {
		return locHasPrCh;
	}

	public int getDivergencePoint() {
		return divergencePoint;
	}

	public HashMap<Integer, String[][]> getChangedRuleEffects() {
		return changedRuleEffects;
	}

	public String[][] getRuleEffectChanges(int global_id) {
		return changedRuleEffects.get(global_id);
	}

	public String[] getEffectsBlocked(int global_id) {
		return changedRuleEffects.containsKey(global_id) ? changedRuleEffects.get(global_id)[0]
				: new String[baseCascSim.NUM_ETYMA()];
	}

	public String[] getEffectsCaused(int global_id) {
		return changedRuleEffects.containsKey(global_id) ? changedRuleEffects.get(global_id)[1]
				: new String[baseCascSim.NUM_ETYMA()];
	}

	public HashMap<Integer, String> getChangedDerivations() {
		return changedDerivations;
	}

	public int[] getEtsWithChangedDerivations() {
		List<Integer> keys = new ArrayList<Integer>(changedDerivations.keySet());
		int N = keys.size();
		int[] out = new int[N];
		while (!keys.isEmpty()) {
			Integer min = Collections.min(keys);
			out[N - keys.size()] = min.intValue();
			keys.remove(min);
		}
		return out;
	}

	// print all etyma changed by the proposed changes.
	private String strEffects(String[] effsOverEts) {
		String out = "";
		for (String eoei : effsOverEts)
			if (!eoei.equals(""))
				out += "; " + eoei;

		return out.substring(2);
	}
	
	/** from standard description of a relocdation, return @true if it is forward, @false if backward
	 */
	public static boolean relocd_notes_forward(String notes)
	{
		if (!DHSWrapper.validRelocdationNotes(notes))	throw new RuntimeException("Cannot determine forward or backward for invalid notes for relocdation"); 
		int src_step = UTILS.getIntPrefix(notes.substring(16));
		int dest_step = UTILS.getIntPrefix(notes.substring(16+notes.substring(16).indexOf(" ")+4));
		return src_step > dest_step;
	}
	
	/** relocdIsBackward
	 * auxiliary for computeRuleCorrespondences
	 * for purposes of determining if a relocdation detected (on grounds of ilhi > bi) 
	 * is a future backward relocdation 
	 * 	or, alternatively, a current future relocdation that that method
	 * 		treats as a series of one-step backward relocdations 
	 * 		except when it comes to computing locHasPrChs (i.e. where this class comes into play)
	 * we know it is in fact a future backward relocdation
	 * if we can find an insertion row in proposedChs that has: 
	 * 	* @param <hi> (hypothesis cascade index) at index [0]
	 * 	* <string form of hypothesis rule at hi> at index [1]
	 * 			* if it is at that spot, it MUST represent rule located at <hi> in the hypothesis cascade. 
	 * with just these two, there are possible edge cases here: :
	 * 	* we could instead have found the insertion of another instance of a rule identical to the hyp rule at <hi>
	 * 	* we could even have a relocdation of a thus identical rule
	 * 			(the latter case really shouldn't happen, but it could with a bug) 
	 * to avoid these, we locate the row with x[0] = <hi> 
	 * 	* and determine that is in fact an insertion of this rule,
	 * 	* and also a backward relocdation 
	 * 	* using the format enforced by DHSWrapper.validRelocdationNotes.
	* @return whether this is in fact a future backward relocdation ( @true )
	* 		rather than a current forward relocdation ( @false )
	 */
	private boolean relocdIsBackward(int hi)
	{
		String hypRuleStr = hypCascSim.getRuleAt(hi); 
		int pci = proposedChs.size() - 1; 
		while (pci < 0 ? false : hi < Integer.parseInt(proposedChs.get(pci)[0]))	pci--;
		if (pci < 0 ? true : hi > Integer.parseInt(proposedChs.get(pci)[0]))		return false;
		
		if (!hypRuleStr.equals(proposedChs.get(pci)[1]))	return false;
		
		String hnotes = proposedChs.get(pci)[2]; 
		if (!DHSWrapper.validRelocdationNotes(hnotes))	return false;
		return !relocd_notes_forward(hnotes); 
	}
	
	private boolean isForwardRelocd(int hi, int ilbi)
	{
		if (proposedChs.size() < 2)	return false;
		int pci = 0 , last_spot = proposedChs.size() - 1; 
		
		boolean advance = true ;
		
		do {
			if (pci >= last_spot)	return false; 
			String[] pc = proposedChs.get(pci++);
			int pc_hi = Integer.parseInt(pc[0]); 
			if (pc_hi > hi )	return false; 
			if (pc_hi == hi && "deletion".equals(pc[1]))
				if(DHSWrapper.validRelocdationNotes(pc[2]))
					if(relocd_notes_forward(pc[2]))	return true; 
		} while (advance);
		
		// if we reach this point the deletion is correct... but is the corresponding later insertion?
		String hypInsStr = hypCascSim.getRuleAt(ilbi); 
		
		do {
			String[] pc = proposedChs.get(pci++); 
			if (ilbi < Integer.parseInt(pc[0]))	return false; 
			if (pc[1].equals(hypInsStr))
				if (DHSWrapper.validRelocdationNotes(pc[2]))
					if(relocd_notes_forward(pc[2]))	return true; 
		} while (pci <= last_spot);
		// if reached this point, failed to find it. 
		return false; 
	}
}
