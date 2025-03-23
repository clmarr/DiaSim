import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File; 
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashMap; 
import java.util.Scanner; 
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * main class for diachronic derivation system
 * @author Clayton Marr
 *
 */
public class DiachronicSimulator {
	
	private static List<String> rulesByTimeInstant;
	
	private static Etymon[] inputForms;
	private static String inputName; 
	private static Lexicon goldOutputLexicon;
	private static int NUM_ETYMA; 
	private static int NUM_GOLD_STAGES, NUM_BLACK_STAGES, NUM_COLUMNED_BLACK_STAGES; 
	private static int NUM_STAGES()	{	return NUM_GOLD_STAGES + NUM_BLACK_STAGES;	}/*having columned is unnecessary as they are either columnedBlack or gold*/
	private static int NUM_COLUMNED_STAGES()	{	return NUM_GOLD_STAGES + NUM_COLUMNED_BLACK_STAGES; 	}
	private static String[] goldStageNames, blackStageNames, allStageNames, columnedStageNames, columnedBlackStageNames; 
	private static Lexicon[] goldStageGoldLexica; //indexes match with those of goldStageNames 
		//so that each stage has a unique index where its lexicon and its name are stored at 
			// in their respective lists.
	
	private static Lexicon[] blackInsertionRemovalLexica; 
		// aka columned black stage lexica
		// TODO lexica for purposes of insertion and removal of etyma only.
		// TODO currently still unimplemented
		// as of March 2025, index will be mapped t that of columnedStageNames, columnedStageInstants etc -- which is now differentiated from the (total/all)Stage name instants. 
	private static int[] goldStageInstants, blackStageInstants, allStageInstants, columnedStageInstants, columnedBlackStageInstants; // i.e. the index of custom stages in the ordered rule set
	private static int[] blackToColumnedIndex; 
		// for each black stage index, among the black stage arrays, 
			// gives the columned stage index if it is columned.
		// if uncolumned (default), contains -1. 
	private static boolean goldStagesSet, blackStagesSet, columnedBlackStagesSet; 
	private static boolean columnedStagesSet()	{	return goldStagesSet || columnedBlackStagesSet;	}
	private static boolean anyStagesSet()	{	return goldStagesSet || blackStagesSet; /*having columned is unnecessary as they are either columnedBlack or gold*/	}
	private static boolean lexiconHasHeader;
	
	private static boolean hasGoldOutput; 
	
	//to be set in command line...
	private static String runPrefix;
	private static String symbDefsLoc; 
	private static String featImplsLoc; 
	private static String symbDiacriticsLoc; 
	private static String cascFileLoc; 	
	private static String lexFileLoc;
	
	private static boolean DEBUG_RULE_PROCESSING, DEBUG_STAGES, print_changes_each_rule, stage_pause, ignore_stages, 
		no_feat_impls, no_symb_diacritics, skip_file_creation, VERBOSE; 
	
	private static int goldStageInd, blackStageInd; 
	
	private static List<SChange> CASCADE;
	private static Simulation theSimulation; 
	
	private static String[] stageOrdering; 
		// letter then number. 'G' = gold stage, 'b' = (uncolumned) black stage, 'B' = columned black stage. 
		// Number is the number AMONG the respective stage subtype.
		// so there will be "skips" among hte black stages for the columned ones. 
	private static String[] initStrForms; 

	private static List<String> formIDs;
		// may need debugging at some point, but for now is being used in an as-necessary (for work with Borja) manner. 
		//TODO note that as it stands currently, if you use formIDs, they MUST be on every word or else there will be concurrence errors (!) 
	
	/** extractCascade
	 * given @param theFactory, extracts ordered cascade from cascade file. 
	 * @note fills @global variables pertaining to stages: NUM_GOLD_STAGES, NUM_BLACK_STAGES, goldStagesSet, blackStagesSet
	 *		goldStageGoldLexica, goldStageNames, goldStageInstants, blackStageNames, blackStageInstants
	 *	but not column stage variables because this is not specified in the cascade but rather in the lexicon file...
	 */
	public static void extractCascade(SChangeFactory theFactory)
	{
		if (VERBOSE)
			System.out.println("Now extracting diachronic sound change rules from rules file...");
		
		rulesByTimeInstant = new ArrayList<String>(); 
		inputName = "Input";

		String nextRuleLine;
		
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader ( 
				new FileInputStream(cascFileLoc), "UTF-8")); 
			
			while((nextRuleLine = in.readLine()) != null)
			{
				String lineWithoutComments = ""+nextRuleLine; 
				if (lineWithoutComments.contains(""+UTILS.CMT_FLAG))
						lineWithoutComments = lineWithoutComments.substring(0,
								lineWithoutComments.indexOf(""+UTILS.CMT_FLAG));
				if(!lineWithoutComments.trim().equals(""))	rulesByTimeInstant.add(lineWithoutComments); 
			}
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
		
		//now filter out the stage name declaration lines.
		
		List<String> goldStageNameAndLocList = new ArrayList<String>(); //to be collected 
		//until the end of collection, at which point the appropriate arrays for the custom
		// stages will be created using this List. These ones will be compared to gold.
		List<String> blackStageNameAndLocList = new ArrayList<String>();
			// same as above, but will not be compared to gold. 
		
		goldStagesSet = false; blackStagesSet=false;  
				
		int rli = 0; 
		
		while (rli < rulesByTimeInstant.size())
		{
			String currRule = rulesByTimeInstant.get(rli); 
			
			if ( (""+UTILS.GOLD_STAGENAME_FLAG+UTILS.BLACK_STAGENAME_FLAG).contains(""+currRule.charAt(0)))
			{
				if (ignore_stages)	rulesByTimeInstant.remove(rli); 
				else if ( currRule.charAt(0) == UTILS.GOLD_STAGENAME_FLAG)
				{
					goldStagesSet = true; 
					
					if (rli == 0)
						throw new RuntimeException("Error: attempted to set gold stage '"+currRule.substring(1)+"' before any rules have modified the input. "
								+ "There is no point in this and it was probably an error. Please reserve stages for time instants after the input has been modified.");
						
					currRule = currRule.substring(1); 
					
					if (currRule.contains(""+UTILS.GOLD_STAGENAME_FLAG))
						throw new RuntimeException("Error: stage name flag <<"+UTILS.GOLD_STAGENAME_FLAG+">> "
								+ "occuring in a place besides the first character in the rule line -- this is illegal: \n"+currRule);
					if (currRule.contains(UTILS.STAGENAME_LOC_DELIM+""))
						throw new RuntimeException("Error: illegal character found in name for custom stage -- <<"
								+UTILS.STAGENAME_LOC_DELIM+">>");  
					goldStageNameAndLocList.add(""+currRule.trim()+UTILS.STAGENAME_LOC_DELIM+rli);
				}
				else if (currRule.charAt(0) == UTILS.BLACK_STAGENAME_FLAG && rli ==0)
				{
					inputName = currRule.substring(1).trim(); 
					if (VERBOSE || DEBUG_STAGES)
						System.out.println("Assuming the attempted black stage at the input, '"+inputName+""
							+ "', is just a preferred name"
							+ " for the input. No black stage constructed here.");
				}
				else if (currRule.charAt(0) == UTILS.BLACK_STAGENAME_FLAG)
				{
					blackStagesSet =true;
					
					currRule = currRule.substring(1); 
					if (currRule.contains(UTILS.STAGENAME_LOC_DELIM+""))
						throw new RuntimeException("Error: illegal character found in name for custom stage -- <<"+UTILS.STAGENAME_LOC_DELIM+">>");  
					blackStageNameAndLocList.add(""+currRule.trim()+UTILS.STAGENAME_LOC_DELIM+rli);
				}
				else	throw new RuntimeException("There must be some bizarre encoding error here in this line where a stage was flagged but the flag character caused a parsing error: "+currRule); 
				rulesByTimeInstant.remove(rli);
			}
			else	rli++;
		}
		
		NUM_GOLD_STAGES = goldStageNameAndLocList.size(); 
		NUM_BLACK_STAGES = blackStageNameAndLocList.size();
		
		System.out.println("Using "+(NUM_GOLD_STAGES+NUM_BLACK_STAGES)+" custom stages."); 
		
		if (VERBOSE || DEBUG_STAGES) {
			if (NUM_GOLD_STAGES > 0)
			{
				System.out.print("Gold stages: ");
				for (String gs : goldStageNameAndLocList)
					System.out.print(gs.substring(0,gs.indexOf(UTILS.STAGENAME_LOC_DELIM))+",");
				System.out.println(""); 
			}
			  
			if (NUM_BLACK_STAGES > 0)
			{
				System.out.print("Black stages:");
				for (String bs : blackStageNameAndLocList)
					System.out.print(bs.substring(0,bs.indexOf(UTILS.STAGENAME_LOC_DELIM))+",");
				System.out.println(""); 
			}
		}
		
		goldStageNames = new String[NUM_GOLD_STAGES];
		blackStageNames = new String[NUM_BLACK_STAGES];
		goldStageInstants = new int[NUM_GOLD_STAGES]; 
		blackStageInstants = new int[NUM_BLACK_STAGES]; 
		blackToColumnedIndex = new int[NUM_BLACK_STAGES]; 
		for (int bsi = 0; bsi < NUM_BLACK_STAGES; bsi++)	blackToColumnedIndex[bsi] = -1; 
		
		// for the purposes of when this is called -- before the lexicon file is engaged, critically --
			// columned stages is effectively the same as gold stages; there are no uncolumned black stages
			// this will be handled at bottom.
		columnedStageInstants = new int[NUM_GOLD_STAGES]; 
		columnedStageInstants = new int[NUM_GOLD_STAGES]; 
		
		
		// parse the rules
		CASCADE = new ArrayList<SChange>();
		
		int cri = 0, gsgi =0 , bsgi = 0, next_gold = -1, next_black = -1;
		if (goldStagesSet)	next_gold = Integer.parseInt(goldStageNameAndLocList.get(gsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[1]);
		if (blackStagesSet)	next_black = Integer.parseInt(blackStageNameAndLocList.get(bsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[1]);
		
		for(String currRule : rulesByTimeInstant)
		{
			if(DEBUG_RULE_PROCESSING)
			{	
				List<SChange> newShifts = theFactory.generateSoundChangesFromRule(currRule); 

				System.out.println("Generating rules for rule number "+cri+" : "+currRule);
				for(SChange newShift : newShifts)
					System.out.println("SChange generated : "+newShift+", with type"+newShift.getClass());
			}
			CASCADE.addAll(theFactory.generateSoundChangesFromRule(currRule));
			 
			cri++; 
			
			if(goldStagesSet)
			{
				if (cri == next_gold)
				{
					if (VERBOSE || DEBUG_STAGES)
						System.out.println("reached gold stage "+goldStageNameAndLocList.get(gsgi));
					
					goldStageNames[gsgi] = goldStageNameAndLocList.get(gsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[0].trim();
					goldStageInstants[gsgi] = CASCADE.size();		
					gsgi += 1;
					if ( gsgi < NUM_GOLD_STAGES)
						next_gold = Integer.parseInt(goldStageNameAndLocList.get(gsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[1]);
				}
			}
			
			if(blackStagesSet)
			{
				if (cri == next_black)
				{
					if (VERBOSE || DEBUG_STAGES)
						System.out.println("reached black stage "+blackStageNameAndLocList.get(bsgi).replace(":",": ")); 
					
					blackStageNames[bsgi] = blackStageNameAndLocList.get(bsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[0].trim();
					blackStageInstants[bsgi] = CASCADE.size();
					bsgi += 1;
					if (bsgi < NUM_BLACK_STAGES)
						next_black = Integer.parseInt(blackStageNameAndLocList.get(bsgi).split(""+UTILS.STAGENAME_LOC_DELIM)[1]);
				}
			}
		}
		
		if (VERBOSE)	System.out.println("Diachronic rules extracted. "); 
		
		stageOrdering = UTILS.extractStageOrder(cascFileLoc, !inputName.equalsIgnoreCase("input")); 
		preemptAnyBadStageName(); 
		
		if ( NUM_STAGES() != stageOrdering.length)
			throw new Error("Error: mismatch in stage count ("+NUM_STAGES()+") and size of stageOrdering ("+stageOrdering.length+")");
		
		if (anyStagesSet())
		{
			allStageNames = new String[NUM_STAGES()];
			allStageInstants = new int[NUM_STAGES()]; 
			for (int csi = 0 ; csi < NUM_STAGES(); csi++)
			{
				String curr_stage_pointer = stageOrdering[csi]; 
				boolean isGold = curr_stage_pointer.charAt(0) == 'G'; 
				int stageNumber = Integer.parseInt(curr_stage_pointer.substring(1)); 
				allStageNames[csi] = (isGold ? goldStageNames : blackStageNames)[stageNumber];
				allStageInstants[csi] = (isGold ? goldStageInstants : blackStageInstants)[stageNumber]; 
			}
		}
		
		// flag a warning for dangerous stage names
		if (goldStagesSet)
			for (String gsni : goldStageNames)
				if (gsni.equalsIgnoreCase("none"))
					System.out.println("WARNING: it is advised not to use a stage named 'none', as this can cause errors!");
		if (blackStagesSet)
			for (String bsni : blackStageNames)
				if (bsni.equalsIgnoreCase("none"))
					System.out.println("WARNING: it is advised not to use a stage named 'none', as this can cause errors!");
		
		// columned stages the same as gold stages, for now. 
		if (columnedStagesSet()) {
			columnedStageInstants = Arrays.copyOf(goldStageInstants, NUM_GOLD_STAGES); 
			columnedStageNames = Arrays.copyOf(goldStageNames, NUM_GOLD_STAGES); 
			columnedBlackStagesSet = false; 
		}
	}
	
	/** makeBlackStageColumned
	 * makes the black stage with the index 
	 * @param black_stage_number (index being order in which black stages occur -- stages of black stage organization structured)
	 * into a columned black stage -- with a column in the lexicon, for purposes of insertion and removal. 
	 * modifies columned (black) stage @global variables: 
	 * 	NUM_COLUMNED_BLACK_STAGES, columnedBlackStagesSet, columnedStageNames, columnedBlackStageNames
	 * 	 columnedBlackStageInstants, columnedStageInstants
	 * @note does NOT fill blackInsertionRemovalLexica! -- this must be done externally! 
	 * @note will also modify stageOrdering! 'B' as first character in a cell means columned black stage! 
	 * 
	 * @prerequisite: the following are (hopefully finally) filled:
	 * 		@global blackStageNames, blackStageInstants, allStageNames, allStageInstants
	 * @prerequisite: the following already exist and will be modified ( @destructive ): 
	 * 		@global blackToColumnedIndex, stageOrdering
	 * 
	 * TODO may need debugging! (March 15, 2025)
	 */
	public static void makeBlackStageColumned(int black_stage_ind) {
		String thisStageName = blackStageNames[black_stage_ind]; 
		int thisStageInstant = blackStageInstants[black_stage_ind];
		
		int numberAmongAllStages = -1;
		for ( int si = 0 ; si < allStageInstants.length ? allStageInstants[si] <= thisStageInstant : false; si++) 
			if (allStageInstants[si] == thisStageInstant)	numberAmongAllStages = si; 
		if (numberAmongAllStages == -1 )	// not found
			throw new RuntimeException("Error: while attempting to make black stage number "+black_stage_ind+" a columned black stage, "
					+ "\nfailed to find its instant in the cascade in allStageInstants... called too early?"); 
		
		if (!columnedBlackStagesSet)	//i.e. this has never been called yet, things may need to be initialized -- do it here, possibly redundantly, for additional security. 
			NUM_COLUMNED_BLACK_STAGES = 1;  
		else	NUM_COLUMNED_BLACK_STAGES += 1; 
		
		stageOrdering[numberAmongAllStages] = "B"+black_stage_ind;
		
		if(!columnedStagesSet())	//this is going to end the method after it is done.
		{
			columnedBlackStageInstants = new int[1]; columnedBlackStageInstants[0] = thisStageInstant;
			columnedStageInstants = new int[1]; columnedStageInstants[0] = thisStageInstant;
			columnedBlackStageNames = new String[1]; columnedBlackStageNames[0] = thisStageName;
			columnedStageNames = new String[1]; columnedStageNames[0] = thisStageName;
			blackToColumnedIndex[black_stage_ind] = 0; 
			columnedBlackStagesSet = true; 
			return; 
		}
		
		columnedBlackStagesSet = true; 
		// (re)fill columned (black) stage organization/coordination arrays. 
		int[] newColumnedBlackStageInstants = new int[NUM_COLUMNED_BLACK_STAGES], newColumnedStageInstants = new int[NUM_COLUMNED_STAGES()];
		String[] newColumnedBlackStageNames = new String[NUM_COLUMNED_BLACK_STAGES], newColumnedStageNames = new String[NUM_COLUMNED_STAGES()]; 
		
		int si = 0 /* all  stage index*/, csi = 0 /* (previous vsn) columned stage index*/,
				cbsi = 0 /* (previous vsn) columned BLACK stage index*/; 
		boolean focPassed = false ; // true once si has iterated past the stage that is being columned.
		while (si < NUM_STAGES())
		{
			if(si == numberAmongAllStages)	// i.e. we have reached the black stage being columned in this iteration!
			{
				newColumnedStageInstants[csi] = thisStageInstant; newColumnedBlackStageInstants[cbsi] = thisStageInstant;
				newColumnedStageNames[csi] = thisStageName; newColumnedBlackStageNames[cbsi] = thisStageName; 
				
				// si = numberAmongAllStages, c(b)si = current place (-> index) among columned (black) stages.
				blackToColumnedIndex[black_stage_ind] = csi++; 
				cbsi++; si++;
				// TODO NOTE IMPORTANT! from this point forward c(b)si  will now = NEW columned (black) stage index - 1 ! 

				continue; 
			}
			//else, effectively. 
			char stageTypeInd = stageOrdering[si].charAt(0);
			int stage_index = Integer.parseInt(stageOrdering[si].substring(1)); 
				// index for coordination among stages of that type -- gold or black, not columned black (columned black treated as general black here)
			if ("GB".contains(stageTypeInd+"")) // columned. 
			{
				newColumnedStageInstants[csi + (focPassed ? 1 : 0)] = (stageTypeInd == 'G' ? goldStageInstants : blackStageInstants)[stage_index];
				newColumnedStageNames[csi + (focPassed ? 1 : 0)] = (stageTypeInd == 'G' ? goldStageNames : blackStageNames)[stage_index];
				csi++; 
				if (stageTypeInd == 'B')
				{
					newColumnedBlackStageInstants[cbsi + (focPassed ? 1 : 0)] = blackStageInstants[stage_index]; 
					newColumnedBlackStageNames[cbsi+ (focPassed ? 1 : 0)] = blackStageNames[stage_index];
					cbsi++; 
				}
			} // if not columned: do nothing. 
			si++; 
		}
					
		columnedBlackStageInstants = newColumnedBlackStageInstants; columnedBlackStageNames = newColumnedBlackStageNames;
		columnedStageInstants = newColumnedStageInstants ; columnedStageNames = newColumnedStageNames; 
		
		System.out.println("Made black stage "+thisStageName+" a columned black stage!"); 
	}
	
	/** 
	 * changes one gold stage to a UNCOLUMNED black stage (then recolumend if @param to_columned = true)
		*modifying @global variables and data structures as appropriate. 
	 * @param int gsi -- the index in GOLD data structures of the stage we are blackening (and decolumning, perhaps recolumning)
	 * @param boolean to_columned -- if it is to be (re)coluned. As it is done currently, for ease of coding if not computation. 
	 * @destructive modification to all @global organizing arrays for black, gold, and columned (But not specifically columned black) stages
	 * reworking from mid March 2025... TODO may need to be checked for possible errors introduced. 
	 */
	private static void blackenGoldStage(int gsi, boolean to_columned)
	{
		if (VERBOSE || DEBUG_STAGES)
			System.out.println("Changing gold stage "+goldStageNames[gsi]+" at "+goldStageInstants[gsi]+ " to uncolumned black stage!"); 
	
		int[] oldGoldStageInstants, oldBlackStageInstants, oldColumnedStageInstants, 
			oldBlackToColumnedIndex; 
		String[] oldGoldStageNames, oldBlackStageNames, oldColumnedStageNames; 
		
		oldGoldStageInstants =  new int[NUM_GOLD_STAGES]; oldGoldStageNames = new String[NUM_GOLD_STAGES];
		oldBlackStageInstants = new int[NUM_BLACK_STAGES]; oldBlackStageNames = new String[NUM_BLACK_STAGES];
		oldBlackToColumnedIndex = new int[NUM_BLACK_STAGES]; 
		oldColumnedStageInstants = new int[NUM_COLUMNED_STAGES()]; oldColumnedStageNames = new String[NUM_COLUMNED_STAGES()];
		
		if (columnedStagesSet()) {
			for (int ci = 0 ; ci < NUM_COLUMNED_STAGES(); ci++)
			{
				oldColumnedStageInstants[ci] = columnedStageInstants[ci];
				oldColumnedStageNames[ci] = columnedStageNames[ci]; 
			}
			if (goldStagesSet)
			{	for (int gi = 0; gi < NUM_GOLD_STAGES; gi++)
				{	oldGoldStageInstants[gi] = goldStageInstants[gi]; oldGoldStageNames[gi] = goldStageNames[gi]; 	
			}}
		}
		if (blackStagesSet)
		{ 	
			for (int bi = 0; bi < NUM_BLACK_STAGES; bi++)
			{
				oldBlackStageInstants[bi] = blackStageInstants[bi];
				oldBlackStageNames[bi] = blackStageNames[bi];
				oldBlackToColumnedIndex[bi] = blackToColumnedIndex[bi]; 
			}
		}
		
		NUM_GOLD_STAGES--;	NUM_BLACK_STAGES++;
		if (NUM_BLACK_STAGES == 1)	blackStagesSet = true;
		if (NUM_GOLD_STAGES == 0)	goldStagesSet = false; 
		
		goldStageNames = new String[NUM_GOLD_STAGES]; // recall NUM_GOLD_STAGES is one less
		goldStageInstants = new int[NUM_GOLD_STAGES]; 
		blackStageNames = new String[NUM_BLACK_STAGES];// recall NUM_GOLD_STAGES is one more
		blackStageInstants = new int[NUM_BLACK_STAGES]; 
		blackToColumnedIndex = new int[NUM_BLACK_STAGES]; 
		columnedStageNames = new String[NUM_COLUMNED_STAGES()];
		columnedStageInstants = new int[NUM_COLUMNED_STAGES()];
		
		int soi = 0, bsloc = 0, csi = 0; //stage ordering, black stage, and columned stage indices. 
		while ( !stageOrdering[soi].equals("G"+gsi) ) //while looping, stage being blackened+decolumned not yet reached. 
		{
			if (!"GbB".contains(stageOrdering[soi].substring(0,1)))
				throw new RuntimeException("Global variable stageOrdering misconstructed!"); 
			
			if(stageOrdering[soi].charAt(0) == 'G')
			{
				int curgi = Integer.parseInt(stageOrdering[soi].substring(1)); 
				goldStageNames[curgi] = oldGoldStageNames[curgi];
				goldStageInstants[curgi] = oldGoldStageInstants[curgi];  
			}
			if("GB".contains(stageOrdering[soi].charAt(0)+"")) {
				columnedStageNames[csi] = oldColumnedStageNames[csi]; 
				columnedStageInstants[csi] = oldColumnedStageInstants[csi]; 				
				csi++; 
			}
			if("b".equalsIgnoreCase(stageOrdering[soi].charAt(0)+"")) // b or B
			{
				int curbi = Integer.parseInt(stageOrdering[soi].substring(1)); 
				if (curbi != bsloc) throw new RuntimeException("Error: a black stage was skipped in stageOrdering!"); 
				blackStageNames[curbi] = oldBlackStageNames[curbi]; 
				blackStageInstants[curbi] = oldBlackStageInstants[curbi]; 
				blackToColumnedIndex[curbi] = oldBlackToColumnedIndex[curbi]; 
				bsloc++; 
			}
			soi++; 
			if (soi >= stageOrdering.length)	
				throw new RuntimeException("ERROR: the stage we are decolumning and blackening was never found in stageOrdering!") ;
		}
		//the stage being decolumned+blackened was reached. 
		blackStageNames[bsloc] = oldGoldStageNames[gsi] ; 
		blackStageInstants[bsloc] = oldGoldStageInstants[gsi] ;
		blackToColumnedIndex[bsloc] = -1; // the former gold stages is now a noncolumned black stage -- it became "Decolumned" 
			// -- so it is -1 in this array as per its construction. 
		stageOrdering[soi] = "b"+bsloc;
		int newBlackLoc = bsloc; 
	
		int isg = gsi;  //csi also remains same value. 
		soi++; bsloc++; // bsloc now corresponds to the place after the next stage in the old black stage organizing arrays.
		while (soi < stageOrdering.length) {
			String prefix = stageOrdering[soi].substring(0,1); 
			if ("GB".contains(prefix))
			{
				columnedStageNames[csi] = oldColumnedStageNames[csi]; 
				columnedStageInstants[csi] = oldColumnedStageInstants[csi]; 				
				csi++; 
			}
			if(prefix.equals("G")) {  //gold stage number is now one less: -1 + it
				goldStageNames[isg] = oldGoldStageNames[isg+1]; 
				goldStageInstants[isg] = oldGoldStageInstants[isg+1]; 
				isg++; 
				stageOrdering[soi] = "G"+(-1 + Integer.parseInt(stageOrdering[soi].substring(1)));
			}
			else if(prefix.equalsIgnoreCase("b")) {
				blackStageNames[bsloc] = oldBlackStageNames[bsloc-1];
				blackStageInstants[bsloc] = oldBlackStageInstants[bsloc-1];
				blackToColumnedIndex[bsloc] = oldBlackToColumnedIndex[bsloc-1]; 
				stageOrdering[soi] = stageOrdering[soi].charAt(0)+""+(1 + Integer.parseInt(stageOrdering[soi].substring(1))); 
				bsloc++; 
			}
			soi++; 
		}
		
		if(to_columned)	makeBlackStageColumned(newBlackLoc);
	}		

	/**
	 * determine if the column with the index (@param col_ind) in the array of columned cells that are potentially gold Lexica
	 * 	should be blackened to a columned black stage
	 * 	this is to occur if it consists of only insertions and deletions
	 * 		which is determined by the presence of PseudoEtymon objects. 
	 * @param stage_cells -- column/row combinations from the lexicon after being parsed by parseLexPhon.
	 * 		dimensions should be [column][row] 
	 * @param col_ind index of column to check. Note that this does not include the input column. 
	 * 		in practice this should not be called for the final column. 
	 * @prerequisite -- inputForms and NUM_ETYMA must be set. 
	 * @return
	 */
	private static boolean columnToBeBlackened(Etymon[][] stage_cells, int col_ind)
	{	
		//return false if there's ever (at the least) an Etymon object that isn't a PseudoEtymon (e.g. has a phonological representation) 
			// that is in the same row as an earlier non-PseudoEtymon 
			// if make it to the end... true.

		for (int row_ind = 0 ; row_ind < NUM_ETYMA; row_ind ++)
		{
			if (!UTILS.etymonIsPresent(stage_cells[col_ind][row_ind]))	continue; 

			//otherwise this will trigger false if and only if there is phonological material here that is not an *insertion*. 
			//if previous column in the row is an absent etymon, it's obviously an insertion...
			// if previous column is unattested, see what it's continuing by looking further back as long as unattested etyma indications go back. 
			//	 	breaking the loop and calling false if phonological material is found (because that would mean this is a stagewise gold form to compare against)
			// 		and continuing on if an a specification that the etymon was absent is found -- i.e. the same behavior as if it was actually absent. 
			int col_before = col_ind - 1; 
			while (col_before < 0 ? false : UTILS.UNATTD_GOLD_REPR.equals(stage_cells[col_before][row_ind].print()))	
				col_before--; 

			Etymon prevCell = col_before == -1 ? inputForms[row_ind] : stage_cells[col_before][row_ind]; 

			if (UTILS.etymonIsPresent(prevCell))	return false; 

			//if (UTILS.ABSENT_REPR.equals(prevCell.print()))	continue; 
			// effectively, the loop continues otherwise.
		}
		return true; 
	}
	
	/**
	 * @param currIndic -- indicator a la stage ordering -- G/B/b then number among gold or black stages. 
	 * @return its name. 
	 */
	private static String stageOrderingIndicToName(String currIndic)
	{
		boolean isBlack = currIndic.substring(0,1).equalsIgnoreCase("b"); 
		return (isBlack ? blackStageNames : goldStageNames)[Integer.parseInt(currIndic.substring(1))];
	}
	
	/**
	 * @param name of stage to retrieve 
	 * @param ignoreStageOrderingIndex -- index of stageOrdering to bypass.
	 * 		 Use -1 if not ignoring any cell  
	 * @return UTILS.NULL_STAGE_INDIC (currently "NULLSTAGE") @if it never occurs, @else the contents of stageOrdering's cell for it 
	 * 		(e.g. "G" if gold stage then the number of gold stage,
	 * 			 b for uncolumned black, B for columned black...) 
	 */
	private static String retrieveStageByName(String name, int ignoreStageOrderingIndex)
	{
		String out = UTILS.NULL_STAGE_INDIC; 
		if(NUM_STAGES() == 0)	return out; 
		name = name.trim();

		for (int soi = 0 ; soi < stageOrdering.length; soi++)
		{
			if (soi == ignoreStageOrderingIndex)	continue; 
			String currStageIndic = stageOrdering[soi];
			if(  name.equalsIgnoreCase(stageOrderingIndicToName(currStageIndic).trim()))	
				return currStageIndic; 
		}
		return out; 
	}
	
	/**  given @param name, stage name
	 * @return  index in stage ordering
	 * @if it's absent, return -1. 
	 */
	private static int retrieveStageOrderingIndexByName (String name)
	{
		int out = -1; 
		if (NUM_STAGES() == 0)	return out;
		name = name.trim();
		for (int soi = 0 ; soi < stageOrdering.length; soi++)
		{
			String currStageIndic = stageOrdering[soi];
			if(  name.equalsIgnoreCase(stageOrderingIndicToName(currStageIndic).trim()))	
				return soi; 
		}
		return out; 
	}
	
	 /**preemptBadStageName
	  * throws errors for duplicate or insecure stage names. 
	  * @param newName to be tested
	  * @param ignoreStageOrderingIndex -- default -1; otherwise this index will be ignored; use it to test if all names are nonduplicate
	  */
	private static void preemptBadStageName(String newName, int ignoreStageOrderingIndex)
	{
		// illegal names
		if (Arrays.asList(new String[] {UTILS.NULL_STAGE_INDIC,"Gold","Input","Out","In","Output"}).contains(newName)
				|| ( "rbg".contains(newName.substring(0,1).toLowerCase()) && UTILS.isNumeric(newName.substring(1).trim())))
			throw new RuntimeException("Error: you have attempted to name a stage '"+newName
					+ "'. This is illegal for security reasons. Please try another name."); 
		
		// duplicate names
		
		if (!retrieveStageByName(newName, ignoreStageOrderingIndex).equals(UTILS.NULL_STAGE_INDIC))
			throw new RuntimeException("Error: you have attempted to name a stage a duplicate name: "+newName); 
	}
	private void preemptBadStageName(String newName)	{	preemptBadStageName(newName,-1); 	}
	
	/** preemptAnyBadStageName
	 * preempts any existing bad stage name, with stages already extracted from cascade and stageOrdering already built afterward ( @prerequisite) 
	 */
	private static void preemptAnyBadStageName()
	{
		if (NUM_STAGES() == 0) return; 
		for (int soi = 0 ; soi < stageOrdering.length; soi++)
			preemptBadStageName(stageOrderingIndicToName(stageOrdering[soi]),soi); 
	}
	
	
	/** coordinateColumns
	 * matching (or not) stages declared in cascade file with structure in lexicon file 
	 * 		to coordinate stages as they will function in simulation and determine appropriate behavior. 
	 * Behavior based on stipulations on gold stages (or lack of stipulations) in lexicon file and cascade file: 
	 * as of March 2025, @prerequisite -- cascade file with stages flagged has ALREADY been called. 
	 * furthermore, @prerequisite -- @global stageOrdering already and initialized and filled (currently done in extractCascade), 
	 * 		though it may be modified ( @destructive) 
	// if there is no lexicon header : count number of columns
		// if there is only one column, obviously it is just the input
		// otherwise -- first is input, last is output, any in between are gold stages
	// if there is a lexicon header 
		// lexicon header is identified by being FLAGGED by = 
			// i.e. UTILS.BLACK_STAGENAME_FLAG
				// (delimiter is still , i.e. LEX_DELIM )
		// first stage regardless of name is still "in(put)" 
		// for the others the names are saved,
			// and need to be matched...
				// if any are not matched -- throw error
				// if last is "Out(put)" or "Res(ult)" it does not need to be matched
					// and is interpreted to be the output gold stage.
	// to be called AFTER extractCascade is. 
		// goldOutput -- determined here. 
	// including TODO black columned stages -- where there is insertion and removal but no comparison/evaluation
	// TODO for protodelta -- need to make sure variables for columned stages include those that are given the black stage flag
			// in the cascade file, but which have columns here...
	// and TODO reformulate column stage and gold stage blackening aspects present here into a sorting of stages based on appropriate factors
		// TODO March 2025: implementation underway... 
	// currently assuming first column is input and last is output
		//TODO for later expansions -- need to change this behavior to handle the situation where first column is a stage that is not equivalent to the inpu
	 * @param lexicHeader -- first line of lexicon with content
	 */
		public static void coordinateColumns(String lexicHeader)
	{
		if (VERBOSE||DEBUG_STAGES)
			System.out.println("Coordinating stages as per cascade file with stages as per lexicon file..."); 

		//stripping any comments and space -- this should already have been done, but just in case...
		int cmt_loc = lexicHeader.indexOf(UTILS.CMT_FLAG); 
		if (cmt_loc != -1)
			lexicHeader = lexicHeader.substring(0, cmt_loc); 
		lexicHeader = lexicHeader.trim();		
		
		int numCols = lexicHeader.contains(""+UTILS.LEX_DELIM) ? 
			lexicHeader.split(""+UTILS.LEX_DELIM).length : 1	;
		
		if (VERBOSE || DEBUG_STAGES) { 
			System.out.println("Lexicon file has "+numCols+" columns!"); 
			System.out.println("First column assumed to be input."); 
		}	
	
		lexiconHasHeader = lexicHeader.charAt(0) == UTILS.BLACK_STAGENAME_FLAG; 
		
		//TODO still need to integrate protodelta behavior for the rest of this class. 

		if(lexiconHasHeader)
		{
			if (VERBOSE || DEBUG_STAGES)
				System.out.println("Header detected: "+lexicHeader); 
			
			String[] colTitles = lexicHeader.split(""+UTILS.LEX_DELIM); 
			int cti = 0; 
			while (cti < colTitles.length)	
				colTitles[cti] = colTitles[cti++].trim();
			
			if (VERBOSE)
				System.out.println(colTitles.length+" columns detected in header."); 
					
			// assuming the first line is the input .
			inputName =colTitles[0].substring(1); 
			if (VERBOSE)
				System.out.println(inputName+" assumed to be input!"); 
			if ( !retrieveStageByName(inputName,-1).equals(UTILS.NULL_STAGE_INDIC))
				System.out.println("Warning: An intermediate stage's name is listed as header for the input column.\n"
						+ "DiaSim does not yet support starting at an intermediate stage; hopefully this will become available soon (stay tuned)."); 
			cti = 1; 
			
			int prevStageOrderIndex = 0; 
			
			while (cti < colTitles.length)
			{
				int nextStageOrderIndex = retrieveStageOrderingIndexByName(colTitles[cti]); 
				
				if (VERBOSE)
					System.out.println("coordinating header title "+colTitles[cti]+"... per the structure <stageOrdering>, mapped to... "
							+(nextStageOrderIndex == -1 ? "nothing!" : 
								(stageOrdering[nextStageOrderIndex].charAt(0) == 'G' ? "gold" : "black")) +"; number among all stages: "+ nextStageOrderIndex); 
				
				if (nextStageOrderIndex == -1)
				{
					if (cti == colTitles.length - 1 ) // assume it's output
					{
						if (VERBOSE)	System.out.println("Column "+colTitles[cti]+" assumed to be output!"); 
						hasGoldOutput = true; 
						break;
					}
					else throw new RuntimeException("Stage name declared in lexicon header that is not flagged "
						+ "anywhere in the cascade (check spelling): "+colTitles[cti]);
				}
					
				if (nextStageOrderIndex < prevStageOrderIndex)
					throw new RuntimeException("Mismatch in stage ordering in cascade file and in lexicon header.\n"
							+ "The stage '"+colTitles[cti]+"' is placed in the lexicon header after stage '"+colTitles[cti-1]+"', but before it in the cascade!"); 
				
				// if it's an uncolumned black stage, column it. 
				if (stageOrdering[nextStageOrderIndex].substring(0,1).equals("b"))
					makeBlackStageColumned(Integer.parseInt(stageOrdering[nextStageOrderIndex].substring(1)));
				
				//blacken any gold stages in between. 
				while (prevStageOrderIndex < nextStageOrderIndex)
				{	
					if(stageOrdering[prevStageOrderIndex].substring(0,1).equals("G")) 
					{	//decolumn and blacken the skipped gold stage
						int goldStageInd = Integer.parseInt(stageOrdering[prevStageOrderIndex].substring(1)); 
						System.out.println("Blackening (and decolumning) gold stage skipped over in header: "+
								goldStageNames[goldStageInd]); 
						blackenGoldStage(goldStageInd,false); 
					}	
					prevStageOrderIndex++; 
				}
				prevStageOrderIndex = nextStageOrderIndex + 1;
				cti++; 
			}
			
			//if there's stuff in stageOrdering left -- blacken any gold stages. 
			while (prevStageOrderIndex < stageOrdering.length)
			{
				if(stageOrdering[prevStageOrderIndex].substring(0,1).equals("G")) 
				{	//decolumn and blacken the skipped gold stage
					int goldStageInd = Integer.parseInt(stageOrdering[prevStageOrderIndex].substring(1)); 
					System.out.println("Blackening (and decolumning) gold stage skipped over in header: "+
							goldStageNames[goldStageInd]); 
					blackenGoldStage(goldStageInd,false); 
				}	
				prevStageOrderIndex++; 
			}			
		}// either we have passed last column (coli == numCols) or confirmed the last gold stage or both 
		else
		{
			if (VERBOSE || DEBUG_STAGES)
				System.out.println("No explicit header declared in lexicon file."); 
			
			if (numCols <= 2)	// just-input run. 
			{
				hasGoldOutput = numCols == 2; //0 is impossible by how the file is processed. 
				System.out.println(hasGoldOutput ? 
						"Two columns detected: first assumed to be input and last assumed to be final observed output forms!"
						: "Only one column detected in lexicon file -- input only run!");
				
				if(NUM_GOLD_STAGES > 0)	System.out.println("Therefore, blackening and decolumning all gold stages!"); 
				while(NUM_GOLD_STAGES > 0)	blackenGoldStage(0, false); 
			}
			else if(numCols == NUM_GOLD_STAGES + 1)
			{
				System.out.println("Each gold stage from cascade properly identified if we assume no gold output!"); 
				System.out.println("Beware: no systematic comparison possible at endpoint!");
				hasGoldOutput = false; 
			}
			else if(numCols == NUM_GOLD_STAGES + 2)
			{
				if (VERBOSE || DEBUG_STAGES) {
					System.out.println("Each gold stage from cascade properly identified if we assume last is the gold forms for the output time!"); 
					System.out.println("numCols : "+numCols);
				}
				
				hasGoldOutput = true; 
			}
			else if (numCols == NUM_STAGES() + 1 || numCols == NUM_STAGES() + 2 ) // we're going to have columned stages. 
			{
				if (VERBOSE||DEBUG_STAGES) 
					System.out.println("Each gold and black stage from cascade identified to a column"
							+ (numCols == NUM_STAGES() + 1 ? ", if we assume no gold output!\nBeware: no systematic comparison possible at endpoint!" 
									: "!"));  
				hasGoldOutput = numCols == NUM_STAGES() + 2 ; 
				
				// making black stages columned, on the basis of stageOrdering in lieu of a header...
				for (String ordStage : stageOrdering )
					if (ordStage.substring(0,1).equals("b")) //columnize it!
						makeBlackStageColumned(Integer.parseInt(ordStage.substring(1))); 
			}
			else
				throw new RuntimeException("ERROR: invalid number of columns ("+numCols+"), "
							+ "given that we have "+NUM_GOLD_STAGES+" gold stages and "+NUM_BLACK_STAGES+
							" black stages as specified in cascade file!"); 
		}	
	}
	
		
	public static void main(String args[])
	{
		parseArgs(args); 
		
		//extract symbol definitions, and with them, the features in use. 
		List<String> symbDefLines = UTILS.readFileLines(symbDefsLoc);

		if (VERBOSE)		
		{
			System.out.println("Collecting symbol definitions...");
			System.out.println("Symbol definitions extracted!\nLength of symbDefsLines : "+symbDefLines.size()); 
		}
		UTILS.extractSymbDefs(symbDefLines); 

		if (!no_feat_impls)
		{
			if (VERBOSE)
				System.out.println("Now extracting info from feature implications file...");
			UTILS.extractFeatImpls(featImplsLoc); 
		}
		
		if (!no_symb_diacritics)	
			UTILS.extractDiacriticMap(symbDiacriticsLoc);		
		
		if (VERBOSE) 	System.out.println("Creating SChangeFactory...");
		SChangeFactory theFactory = new SChangeFactory(UTILS.phoneSymbToFeatsMap, UTILS.featIndices); 
		
		extractCascade(theFactory);
		// this inits gold and black stage variables because of how they are flagged in the cascade
	
		//now input lexicon 
		//collect init lexicon ( and gold for stages or final output if so specified) 
		//copy init lexicon to "evolving lexicon" 
		//each time a custom stage time step loc (int in the array goldStageTimeInstantLocs or blackStageTimeInstantLocs) is hit, save the 
		// evolving lexicon at that point by copying it into the appropriate slot in the goldStageResultLexica or blackStageLexica array
		// finally when we reach the end of the rule list, save it as testResultLexicon
		
		if (VERBOSE)	System.out.println("Now extracting lexicon...");
		String nextLine; 
		
		List<String> lexFileLines = new ArrayList<String>(); 
		formIDs = new ArrayList<String>(); 
		
		try 
		{	File inFile = new File(lexFileLoc); 
			BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream(inFile), "UTF8"));
			while((nextLine = in.readLine()) != null)	
			{	
				if (nextLine.contains(UTILS.FORM_ID_FLAG+"") && UTILS.USE_FORM_ID)
					formIDs.add(nextLine.substring(nextLine.lastIndexOf(UTILS.FORM_ID_FLAG)+1));
				
				
				//comments now being added to Etymon class.
				if (nextLine.contains(UTILS.CMT_FLAG+""))
				{
					// old version, just this: nextLine = nextLine.substring(0,nextLine.indexOf(UTILS.CMT_FLAG)).trim(); 
					String lineSansComments = nextLine.substring(0,nextLine.indexOf(UTILS.CMT_FLAG)).trim(); 
					if (lineSansComments.equals(""))	continue;
				}
				if (!nextLine.equals("")) 	lexFileLines.add(nextLine); 		
			}
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

		// now extract from lexicon 
		
		String firstlineproxy = ""+lexFileLines.get(0); 
		int numCols = firstlineproxy.contains(""+UTILS.LEX_DELIM) ? firstlineproxy.split(""+UTILS.LEX_DELIM).length : 1 ; 
		
		NUM_ETYMA = lexFileLines.size() - (firstlineproxy.charAt(0) == UTILS.BLACK_STAGENAME_FLAG ? 1 : 0); 
		initStrForms = new String[NUM_ETYMA]; 
		
		//TODO handling of column stages should begin here, possibly within coordinateColumns.
		coordinateColumns(firstlineproxy); 
		
		if (VERBOSE)
			System.out.println("Number of etyma: "+NUM_ETYMA);
		
		boolean justInput = !hasGoldOutput && !goldStagesSet; 
		
		inputForms = new Etymon[NUM_ETYMA];
		Etymon[] goldResults = new Etymon[NUM_ETYMA];  // being built to pass to  goldOutputLexicon
		Etymon[][] columnForms = new Etymon[NUM_COLUMNED_STAGES()][NUM_ETYMA];
				//being built to pass to goldStageGoldLexica and blackInsertionRemovalLexica down the line. 
			// before their shapes are finalized, as they are reinitialized in stage blackening and (de)columning processes
		
		int lfli =  0 ; //"lex file line index"
		if (lexiconHasHeader)	lexFileLines.remove(0); 
		
		//file columnForms
		while(lfli < NUM_ETYMA)
		{
			String theLine = lexFileLines.get(lfli);
			
			String commentContents = "";
			boolean commented = theLine.contains(UTILS.CMT_FLAG+""); 
			if (commented) {
				commentContents = theLine.substring(theLine.indexOf(UTILS.CMT_FLAG+"")); 
				theLine = theLine.substring(0,theLine.indexOf(UTILS.CMT_FLAG)).trim(); 
			}
			
			if(/**lfli <NUM_ETYMA && */numCols != UTILS.countColumns(theLine))
				throw new RuntimeException("ERROR: incorrect number of columns in line "+lfli+".\nThe line: "+theLine);
			
			initStrForms[lfli] = justInput ? theLine : theLine.split(""+UTILS.LEX_DELIM)[0]; 
			inputForms[lfli] = UTILS.parseLexPhon(initStrForms[lfli],no_symb_diacritics);
			if (commented)	inputForms[lfli].setComments(commentContents);
			
			if (!justInput)
			{
				String[] forms = theLine.split(""+UTILS.LEX_DELIM); 
				if(NUM_COLUMNED_STAGES() > 0)
				{	for (int csi = 0 ; csi < NUM_COLUMNED_STAGES() ; csi++) {
						columnForms[csi][lfli] = UTILS.parseLexPhon(forms[csi+1],no_symb_diacritics);
						if(commented)	columnForms[csi][lfli].setComments(commentContents);
					}
				}
				
				if (hasGoldOutput)
					goldResults[lfli] = UTILS.parseLexPhon(forms[forms.length-1], no_symb_diacritics); 
			}
			lfli++;
		}		
	
		if(NUM_COLUMNED_STAGES() > 0)
		{
			// first, determine if any of the gold stages need to become columned black stages
			int coli = 0; 
			for (int soi = 0 ; soi < stageOrdering.length; soi++) {
				if (stageOrdering[soi].charAt(0) == 'G')
					if (columnToBeBlackened(columnForms,coli))
						blackenGoldStage(Integer.parseInt(stageOrdering[soi].substring(1)),true);
							//note the first variable is the number among gold stuff 
				if ("GB".contains(stageOrdering[soi].substring(0,1))) //if its still gold, or columned black. 
					coli++; 
			}
						//TODO note there may be a bug in doing it htis way though -- decolumning then columning, effectively (as of March 21) 
				// make gold stage to columned black.
			
			int gsi = 0 , cbsi = 0 /*, si = 0*/ ; // (gold, columned black, stage ordering column) indices respectively  
			
			// fill goldStageGoldLexica and blackInsertionRemovalLexica
			goldStageGoldLexica = new Lexicon[NUM_GOLD_STAGES]; 
			blackInsertionRemovalLexica = new Lexicon[NUM_COLUMNED_BLACK_STAGES]; 
			coli = 0; 
			
			//TODO debugging
			System.out.print("stageOrdering: "+stageOrdering[0]); 
			for(int stoi = 1 ; stoi < stageOrdering.length; stoi++)	System.out.print(", "+stageOrdering[stoi]); 
			System.out.println("");
			
			for ( int si = 0 ; coli < NUM_COLUMNED_STAGES(); si++)
			{
				//TODO debugging
				System.out.println("column "+coli+" @sOrd: "+stageOrdering[si]); 
				
				char stageTypeIndic = stageOrdering[si].charAt(0); 
				if (stageTypeIndic == 'b') //uncolumned black
					if (si >= stageOrdering.length)	break; 
				else
				{
					if (stageTypeIndic == 'G') // gold
						goldStageGoldLexica[gsi++] = new Lexicon(columnForms[coli++]);
					else // columned black
						blackInsertionRemovalLexica[cbsi++] = new Lexicon(columnForms[coli++]); 
				}
			}
		}
		
		if(hasGoldOutput)	
			goldOutputLexicon = new Lexicon(goldResults); 
		
		theSimulation = new Simulation(inputForms, CASCADE, initStrForms, stageOrdering); 
		if (blackStagesSet)  theSimulation.setBlackStages(blackStageNames, blackStageInstants);
		if (hasGoldOutput)	theSimulation.setGoldOutput(goldResults);
		if (columnedStagesSet())	theSimulation.setColumnedStages(columnForms, columnedStageNames, columnedStageInstants, blackToColumnedIndex);
		
		//TODO imposing standard input name -- turn this off when using intermediate starting points is made possible.
		if (!inputName.equalsIgnoreCase("input"))
				theSimulation.setInputStageName(inputName);
		theSimulation.setStepPrinterval(UTILS.PRINTERVAL); 
		theSimulation.setOpacity(!print_changes_each_rule);

		goldStageInd = 0; blackStageInd=0;
			//index IN THE ARRAYS that the next stage to look for will be at .
		
		File dir = new File(""+runPrefix); 
		if (!skip_file_creation) {
			dir.mkdir(); 
			
			makeRulesLog(CASCADE);
		}
		
		String resp; 		
		Scanner inp = new Scanner(System.in);
		
		System.out.println("Now running simulation...");

		while (!theSimulation.isComplete())
		{	
			if(stage_pause){
				theSimulation.simulateToNextStage();
				if(theSimulation.justHitGoldStage())
				{
					System.out.println("Pausing at gold stage "+goldStageInd+": "+goldStageNames[goldStageInd]); 
					System.out.println("Run accuracy analysis here? Enter 'y' or 'n'"); 
					resp = inp.nextLine().substring(0,1); 
					while(!resp.equalsIgnoreCase("y") && !resp.equalsIgnoreCase("n"))
					{
						System.out.println("Invalid response. Do you want to run accuracy analysis here? Please enter 'y' or 'n'.");
						resp = inp.nextLine().substring(0,1); 
					}
					if(resp.equalsIgnoreCase("y"))	
						haltMenu(goldStageInd, inp, theFactory);
					goldStageInd++; 
				}
				else if (!theSimulation.isComplete())//hit black
				{
					System.out.println("Hit black stage "+blackStageInd+": "+blackStageNames[blackStageInd]); 
					System.out.println("Error analysis at black stages is not currently supported."); //TODO make it supported...
					System.out.println("Print latest developments from latest stage? Please enter 'y' or 'n'.");
					resp = inp.nextLine().substring(0,1); 

					while(!resp.equalsIgnoreCase("y") && !resp.equalsIgnoreCase("n"))
					{
						System.out.println("Invalid response. Do you want to run accuracy analysis here? Please enter 'y' or 'n'.");
						resp = inp.nextLine().substring(0,1); 
					}
					if(resp.equalsIgnoreCase("y"))	
					{
						Lexicon prevLex = theSimulation.getInput(); 
						String prstname = inputName;
						
						if (goldStageInd + blackStageInd > 0)
						{
							boolean lastWasBlack = (goldStageInd > 0 && blackStageInd > 0) ? 
									(goldStageInstants[goldStageInd-1] < blackStageInstants[blackStageInd-1])
									: blackStageInd > 0; 
							prevLex = theSimulation.getStageResult(!lastWasBlack, (lastWasBlack ? blackStageInd : goldStageInd) - 1);
							prstname = lastWasBlack ? blackStageNames[blackStageInd-1] : goldStageNames[goldStageInd -1]; 
						}
						
						String bd = "\t,\t"; 
						System.out.println("etymID"+bd+inputName+bd+"Last: "+prstname+""+bd+"Curr: "+blackStageNames[blackStageInd]);
						for (int i = 0 ; i < NUM_ETYMA ; i++)
							System.out.println(i+bd+inputForms[i]+bd+prevLex.getByID(i)+bd+theSimulation.getCurrentForm(i));
					}
					blackStageInd++; 
				}
			}
			else	theSimulation.simulateToEnd(); 
		}
		
		System.out.println("Simulation complete.");
		
		if (!skip_file_creation) {  
			System.out.println("making derivation files in "+dir);
			
			//make derivation files.
			makeDerivationFiles(); 	
			
			//make output graphs file
			System.out.println("making stagewise output graph file in "+dir);
			makeStagewiseOutGraphFile(); 
			System.out.print("... and rulewise output graph file ...");
			makeRulewiseOutGraphFile(); 
			System.out.print(" ... done.\n");
		}
				
		if(hasGoldOutput)
		{
			haltMenu(-1, inp,theFactory);
			
			if (!skip_file_creation) {
				System.out.println("Writing analysis files...");
				//TODO -- enable analysis on "influence" of black stages and init stage... 
				
				//TODO figure out what we want to do here...
						// TODO what did this mean?^ Figure out or delete it. 
				ErrorAnalysis ea = UTILS.setupErrorAnalysis(theSimulation.getCurrentResult(), goldOutputLexicon); 
						
				ea.makeAnalysisFile((new File(runPrefix,"testResultAnalysis.txt")).toString(), 
						false/*, theSimulation.getCurrentResult()*/);
				ea.makeAnalysisFile((new File(runPrefix,"goldAnalysis.txt").toString()),true/*,goldOutputLexicon*/);
				
				if (UTILS.USE_FORM_ID)
					ea.makeEtymwiseEDfile((new File(runPrefix,"resultEditDistances").toString()), formIDs); 
				
				else	ea.makeEtymwiseEDfile((new File(runPrefix,"resultEditDistances").toString())); 
				
				
				if(goldStagesSet)
				{	
					for(int gsi = 0; gsi < NUM_GOLD_STAGES - 1 ; gsi++)
					{	
						ErrorAnalysis eap = UTILS.setupErrorAnalysis(theSimulation.getStageResult(true, gsi), goldStageGoldLexica[gsi]); 
						String currfile = (new File (runPrefix, goldStageNames[gsi].replaceAll(" ", "")+"ResultAnalysis.txt")
								).toString();
						eap.makeAnalysisFile(currfile,false/*, theSimulation.getStageResult(true, gsi)*/);
						ea.makeEtymwiseEDfile((new File(runPrefix,goldStageNames[gsi].replaceAll(" ","")+"EditDistances").toString())); 
					}
				}
			}
		}
		System.out.println("Thank you for using DiaSim"); 
		inp.close();
	}
	
	private static void makeStagewiseOutGraphFile()
	{	
		String filename = new File(runPrefix, 
				runPrefix.substring(runPrefix.lastIndexOf("/") + 1) 
				+ "_stagewise_output_graph"+ UTILS.OUT_GRAPH_FILE_TYPE).toString(); 
		UTILS.writeToFile(filename, theSimulation.outgraph(),true); 
	}
	
	private static void makeRulewiseOutGraphFile()
	{
		String[][] ruleByEtymGraph = theSimulation.derivationGraph();
		
		//TODO debugging
		System.out.println("made local graph..."); 
		
		String filename = new File(runPrefix, 
				runPrefix.substring(runPrefix.lastIndexOf("/") + 1) 
				+ "_rulewise_output_graph.csv").toString(); 
		try 
		{	
			int dirBreak = filename.indexOf("/");

			while (dirBreak != -1)
			{
				String curDir = filename.substring(0, dirBreak),
						rem = filename.substring(dirBreak+1); 
				if (!new File(curDir).exists()) 
					new File(curDir).mkdirs(); 
				
				dirBreak = !rem.contains("/") ? -1 : 
					dirBreak + 1 + rem.indexOf("/"); 
			
			}
			
			BufferedWriter out = new BufferedWriter(new FileWriter(filename,StandardCharsets.UTF_8)); 
			
			for (int ri = 0 ; ri < ruleByEtymGraph.length; ri++)
				out.write(String.join(",", ruleByEtymGraph[ri]) + "\n"); 
			
			out.close();
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
	}
	
	
	private static void makeRulesLog(List<SChange> theShiftsInOrder) {
		String filename = new File(runPrefix, 
				runPrefix.substring(runPrefix.lastIndexOf("/") + 1)+ "_rules_log.txt").toString(); 
		String output = "";
		for (SChange thisShift : theShiftsInOrder)
			output += ""+thisShift + (DEBUG_RULE_PROCESSING ? "| ORIG : "+thisShift.getOrig(): "") + "\n"; 
		UTILS.writeToFile(filename, output,false); 
	}

	private static void makeDerivationFiles()
	{
		File derdir = new File(runPrefix,"derivation"); 
		derdir.mkdir(); 
	
		for( int wi =0; wi < NUM_ETYMA; wi ++) 
		{
			String filename = new File(runPrefix, new File("derivation","etym"+wi+".txt").toString()).toString(); 
			String output = "Derivation file for run '"+runPrefix+"'; etymon number :"+wi+":\n"
				+	inputForms[wi]+" >>> "+theSimulation.getCurrentForm(wi)
				+ (hasGoldOutput ? " ( GOLD : "+goldOutputLexicon.getByID(wi)+") :\n"  : ":\n")
					+theSimulation.getDerivation(wi)+"\n";
			UTILS.writeToFile(filename, output, false); 
		}
	}
	
	// missLocations are the indices of words that ultimately resulted in a miss between the testResult and the gold
	// outputs the scores for each phone in the word in the lexicon
	/** TODO abrogated -- method currently unused.
	public static HashMap<Phone,Double> missLikelihoodPerPhone (Lexicon lexic)
	{
		LexPhon[] lexList = lexic.getWordList(); //indices should correspond to those in missLocations
		int lexSize = lexList.length; 
		assert NUM_ETYMA == finMissInds.length: "Error : mismatch between size of locMissed array and word list in lexicon"; 
		
		Phone[] phonemicInventory = lexic.getPhonemicInventory(); 
		int inventorySize = phonemicInventory.length; 
		
		HashMap<String,Integer> phonemeIndices = new HashMap<String,Integer>();
			//maps print symbols of phoneme onto its index in phonemicInventory array
		for(int phii = 0 ; phii < phonemicInventory.length; phii++)
			phonemeIndices.put( phonemicInventory[phii].print(), phii); 
			
		int[] phoneFreqForMisses = new int[inventorySize]; 
			//indices correspond to those in phonemicInventory 
		
		for(int li = 0 ; li < lexSize; li++)
		{
			if(finMissInds[li])
			{
				String phonesSeenInWord = ""; 
				List<SequentialPhonic> phs = lexList[li].getPhonologicalRepresentation(); 
				
				for(SequentialPhonic ph : phs)
				{
					if(ph.getType().equals("phone"))
					{
						if(!phonesSeenInWord.contains(ph.print()))
						{
							phonesSeenInWord += ph.print() + ","; 
							phoneFreqForMisses[phonemeIndices.get(ph.print())] += 1; 
						}
					}
				}
			}
		}
		
		HashMap<String,Integer> phoneFreqsByWordInLex = lexic.getPhoneFrequenciesByWord(); 
		//note that the keys for this are the feature vects, not the toString() or print() statements
		
		HashMap<Phone,Double> output = new HashMap<Phone,Double>(); 
		for (int pi = 0; pi < inventorySize ; pi++)
			output.put(phonemicInventory[pi], 
				(double)phoneFreqForMisses[pi] /
				(double)phoneFreqsByWordInLex.get(phonemicInventory[pi].getFeatString()));
		
		return output; 
	}**/
	
	// @param (cutoff) -- rule number that the black stage must be BEFORE.
	private static void printIncludedBlackStages(int first, int last)
	{
		if(blackStagesSet)
			for(int bsi = first; bsi < last + 1; bsi++)
				System.out.println("b"+bsi+": "+
					blackStageNames[bsi]+" (@rule #: "+blackStageInstants[bsi]+")");
	}

	private static void printIncludedGoldStages(int firstToPrint, int lastToPrint)
	{
		if(goldStagesSet)
			for(int gsi = firstToPrint; gsi < lastToPrint + 1; gsi++)
				System.out.println("G"+gsi+": "+
					goldStageNames[gsi]+" gold forms (@rule #: "+goldStageInstants[gsi]+")");
	}
	
	private static List<String> validBlackStageOptions(int first, int last, boolean prepend)
	{
		List<String> out = new ArrayList<String>();
		if (blackStagesSet)
			for (int oi = first; oi < last+1; oi++)	out.add((prepend ? "b":"")+oi);
		return out;
	}
	
	private static List<String> validGoldStageOptions(int first, int last, boolean prepend)
	{
		List<String> out = new ArrayList<String>();
		if (goldStagesSet)
			for (int oi = first; oi < last+1; oi++)	out.add((prepend ? "G":"")+oi);
		return out;
	}
	
	// @param curSt : -1 if at final result point, otherwise valid index of stage in goldStage (Gold/Result)Lexica
	// this should only be called when a gold stage is called, or at the end if there is a gold output supplied.
	private static void haltMenu(int curSt, Scanner inpu, SChangeFactory fac)
	{	
		//TODO from protodelta need to fix here with regard to inserted etyma.
			//TODO figure out what this was referring to...
		boolean atOutput = curSt == -1; 
		
		if (curSt == -1 && !hasGoldOutput)
			throw new RuntimeException("Error: attempted to do analysis and diagnostics on final output (as curSt=-1), "
					+"but no gold output forms were provided!");
		else if (curSt > goldStageGoldLexica.length)
			throw new RuntimeException("Error: attempted to do analysis and diagnostics for a gold stage that does not exist!"); 
		
		Lexicon r = theSimulation.getCurrentResult();
		Lexicon g = (curSt == -1) ? goldOutputLexicon : goldStageGoldLexica[curSt]; 
				
		ErrorAnalysis ea = UTILS.setupErrorAnalysis(r,g);

		System.out.println(UTILS.getAccuracyReport(ea));
		
		int lastGoldOpt = (curSt == -1 ? NUM_GOLD_STAGES : curSt) - 1;
		int lastBlkOpt = NUM_BLACK_STAGES - 1;
		while((lastBlkOpt < 0 || curSt < 0) ? false : blackStageInstants[lastBlkOpt] > goldStageInstants[curSt])
			lastBlkOpt--;
		
		boolean cont = true, firstLoop = true; 
		int evalStage = curSt;  // this should only ever be a gold stage. 
		SequentialFilter filterSeq = new SequentialFilter(new ArrayList<RestrictPhone>(), new String[] {}); 
		Lexicon pivPtLex = null;
		String pivPtName = ""; 
		int pivPtLoc = -1; 
		boolean pivPtSet = false, filterIsSet = false;
		//boolean pivPtIsGoldOrInput = false; 
		
		while(cont)
		{
			if (!firstLoop) 
				System.out.print("(Eval pt: "+(evalStage == -1 ? "output" : goldStageNames[evalStage])
					+ "; pivot pt: "+(pivPtSet ? pivPtName.replace("pivot@","") : "none")
					+ ")\n(filter sequence: "+(filterIsSet ? filterSeq.toString() : "none")+")\n");
				
			firstLoop = false; 
			String resp = ""; 
			
			while (resp.length() == 0)
			{
				System.out.println("\n" +
					"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~# SUITE MENU #~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
					"|        What would you like to do? Please enter the appropriate number below:        |\n" +
					"|                                 ----- Scoping -----                                 |\n" +
					"| 0 : Set evaluation point                                                            |\n" +
					"| 1 : Set pivot point (upon which actions are conditioned, incl. filtering [2])       |\n" +
					"| 2 : Set filter sequence                                                             |\n" +
					"|                                ----- View data -----                                |\n" +
					"| 3 : Query                                                                           |\n" +
					"| 4 : Review filtered results or analyze them (stats, errors) at eval point (submenu) |\n" +
					"|                               ----- Diagnostics -----                               |\n" +
					"| 5 : Run autopsy for (at evaluation point) (for subset lexicon if specified)         |\n" +
					"| 6 : Confusion diagnosis at evaluation point                                         |\n" +
					"| 7 : Test full effects of a proposed change to the cascade                           |\n" +
					"|-------------------------------------------------------------------------------------|\n" +
					"|_9 : End this analysis.______________________________________________________________|");
				resp = inpu.nextLine();
				if (resp.length() > 0)	resp = resp.substring(0,1);
			}
			
			if (resp.equals("0")) //set evaluation point
			{
				if (!goldStagesSet)	System.out.println("Cannot change evaluation stage: no intermediate gold stages are set."); 
				else
				{
					System.out.println("Changing point of evaluation (comparing result against gold)");
					if(!atOutput)
						System.out.println("Note that you cannot change evaluation point to a stage not reached yet! "
								+ "Current stage: gold stage "+curSt+", "+goldStageNames[curSt]); 
					System.out.print("Current evaluation point: ");
					if (evalStage == curSt)
					{
						if (evalStage == -1)	System.out.print("final result\n");
						else	System.out.print("current forms at stage "+evalStage+": "+goldStageNames[evalStage]+"\n");
					}
					else	System.out.println("intermediate stage "+evalStage+": "+goldStageNames[evalStage]); 
				
					List<String> validOptions = validGoldStageOptions(0,lastGoldOpt,true); 
					validOptions.add("f"); 
					boolean chosen = false;
					
					while (!chosen)
					{
						System.out.println("Available options for evaluation stage: ");
						printIncludedGoldStages(0, lastGoldOpt); 
						System.out.println("F : "+ (atOutput ? "final forms" : "current forms at gold stage "+curSt+", "+goldStageNames[curSt]));
						System.out.println("Please enter the indicator for the stage you desire"); 
						resp = inpu.nextLine().toLowerCase();
						chosen = validOptions.contains(resp);
						if(!chosen)	System.out.println("Invalid response. Please choose a valid indicator for the new evaluation stage."); 
					}
					
					evalStage = resp.equals("f") ? curSt : Integer.parseInt(resp.replaceAll("[^0-9]", "")); 
					r = resp.equals("f") ? theSimulation.getCurrentResult() : theSimulation.getStageResult(true, evalStage);
					g = (curSt == -1 && resp.equals("f") ) ? goldOutputLexicon : goldStageGoldLexica[evalStage];
					boolean filtered = ea.isFiltSet();
					boolean pivoted = ea.isPivotSet(); 
					
					ea = UTILS.setupErrorAnalysis(r,g); 
					if (pivoted) 	ea.setPivot(pivPtLex, pivPtName);
					if (filtered) 	ea.setFilter(filterSeq, pivPtName);
				}
			}
			else if (resp.equals("1"))
			{
				System.out.println("Setting pivot point -- extra stage printed for word list, and point at which we filter to make subsets."); 
				System.out.println("Current pivot point lexicon: "+(pivPtSet ? pivPtName : "not (yet) defined"));
				System.out.println("Current filter : "+(filterIsSet ? filterSeq.toString() : "not (yet) defined")); 
				
				if(!atOutput)
					System.out.println("Beware that you cannot set a pivot point after the current gold stage we are at, which is at rule number "
							+goldStageInstants[curSt]); 
				
				boolean chosen = false; 
				while(!chosen)
				{
					System.out.println("\nAvailable options for pivot point:");
					printIncludedGoldStages(0, lastGoldOpt); printIncludedBlackStages(0, lastBlkOpt); 
					System.out.println("In: "+inputName+" as pivot (i.e. filter by input forms)"
							+ "\nOut: filter in terms of generated output forms"
							+ "\nGold: filter by correct observed (gold) forms for output (or last gold stage if halted before end)"
							+ "\nU: delete it, and also delete filter (return to scoping over whole lexicon)"
							+ "\nR#: right before rule with index number <#>"
							+ "(you can find rule indices with option 3 to query on the main menu)"
							+ "\nKeep: keep the current pivot pt (or lack of a pivot pt) and return"
							+ "\nPlease enter the appropriate indicator."); 
					
					List<String> validOptions = validGoldStageOptions(0,lastGoldOpt,true);
					validOptions.addAll(validBlackStageOptions(0,lastBlkOpt,true));
					validOptions.add("In"); validOptions.add("Out"); validOptions.add("U"); validOptions.add("Gold"); validOptions.add("Keep");
					
					for(int ri = 1; ri < CASCADE.size(); ri++)	
						validOptions.add("R"+ri);
					
					resp = ""; 
					while (resp.equals(""))
						resp = UTILS.stripEnds(inpu.nextLine().replace("\n",""));
						//resp = inpu.nextLine().replace("\n","").strip(); 
					resp.replace("\n", "");
					
					if (resp.toLowerCase().equals("keep"))	resp = "Keep"; 
					if (!validOptions.contains(resp) && resp.length() > 1) { 
						if (resp.charAt(0) == 'r' && "0123456789".contains(resp.charAt(1)+""))
							resp = "R"+resp.substring(1); 
						if (validOptions.contains(resp.substring(0,1).toUpperCase()+resp.substring(1).toLowerCase()))
							resp = resp.substring(0,1).toUpperCase() + resp.substring(1).toLowerCase();
					}
										
					chosen = validOptions.contains(resp); 
					if(!chosen)
					{
						if(resp.equals("R0"))	System.out.println("Invalid input: 'R0' is not a valid option -- instead choose 'In' "
								+ "to delete pivot point and use the input"
								+ (inputName.equalsIgnoreCase("input") ? "" : " ("+inputName+")")
								+ " for filtering");
						else if (resp.substring(0,1).equalsIgnoreCase("R") && Integer.parseInt(resp.substring(1)) > goldStageInstants[curSt])
							System.out.println("Invalid input: cannot pivot on a stage after the current point in relative chronology!"); 
						else if ("g".equalsIgnoreCase(""+resp.charAt(0)) && !goldStagesSet)
							System.out.println("Invalid input: cannot use 'g' when no gold stages are set!"); 
						else if ("b".equalsIgnoreCase(""+resp.charAt(0)) && !blackStagesSet)
							System.out.println("Invalid input: cannot use 'b' when no black stages are set!"); 
						else if ("bgR".contains(""+resp.charAt(0)) && !UTILS.isInt(resp.substring(1)))
							System.out.println("Invalid input: 'R', 'g', and 'b' require a valid integer after them, but '"+resp.substring(1)+"' is not!"); 
						else if (resp.charAt(0) == 'R')
							System.out.println("'"+resp+"' is not a valid option: the last rule is number "+(CASCADE.size()-1));
						else if ("g".equalsIgnoreCase(""+resp.charAt(0)))
							System.out.println("'"+resp+"' is not a valid option, the last computed gold stage is number "+lastGoldOpt); 
						else if ("b".equalsIgnoreCase(""+resp.charAt(0)))
							System.out.println("'"+resp+"' is not a valid option, the last computed black stage is number "+lastBlkOpt); 
						else	System.out.println("Invalid input : '"+resp+"'\nPlease select a valid option listed below:");
					}
					else
					{	
						pivPtSet = true; 
						if(resp.length() < 4 ? false : resp.substring(0,4).toLowerCase().equals("gold")) 
							resp = "Gold";// preempt dumb capitalization stuff that could cause errors because g# is used to grab gold stage inds.
						if("g".equalsIgnoreCase(""+resp.charAt(0)))
						{
							int si = Integer.parseInt(resp.substring(1));
							pivPtLex = goldStageGoldLexica[si]; 
							pivPtLoc = goldStageInstants[si];
							pivPtName = goldStageNames[si]+" [r"+pivPtLoc+"]";
							//pivPtIsGoldOrInput = true; 
							ea.setPivot(pivPtLex, pivPtName); 
						}
						else if ("b".equalsIgnoreCase(""+resp.charAt(0)))
						{
							int si = Integer.parseInt(resp.substring(1));
							pivPtLex = theSimulation.getStageResult(false, si);
							pivPtLoc = blackStageInstants[si];
							pivPtName = blackStageNames[si]+" [r"+pivPtLoc+"]";
							//pivPtIsGoldOrInput = false; 
							ea.setPivot(pivPtLex, pivPtName); 
						}
						else if (resp.charAt(0) == 'R')
						{
							pivPtLoc = Integer.parseInt(resp.substring(1)); 
							pivPtLex = UTILS.toyDerivation(theSimulation,CASCADE.subList(0, pivPtLoc)).getCurrentResult();
							pivPtName = "pivot@R"+pivPtLoc; 
							//pivPtIsGoldOrInput = false; 
							ea.setPivot(pivPtLex, pivPtName); 
						}
						else if (!resp.equals("Keep"))
						{
							pivPtLoc = -1; pivPtLex = null; pivPtName = ""+resp;
							ea = UTILS.setupErrorAnalysis(r,g); 
							
							if(resp.equals("U"))
							{	filterSeq = new SequentialFilter(new ArrayList<RestrictPhone>(), new String[] {});
								filterIsSet = false; 
								pivPtName = "";
								pivPtSet = false; 
								//pivPtIsGoldOrInput = false; 
							}
							else
							{	
								pivPtLex = resp.equals("In") ? theSimulation.getInput() : 
									resp.equals("Out") ? theSimulation.getCurrentResult() : 
									(curSt == -1) ? goldOutputLexicon : goldStageGoldLexica[curSt];
								//pivPtIsGoldOrInput = !resp.equals("Out"); // based on the above -- if there's reason to adjust this, then this too
								ea.setPivot(pivPtLex,pivPtName);
							}
						}
						else // resp is "Keep", all need to do is make sure didn't errantly set pivot point. 
							if (pivPtName.equals(""))	pivPtSet = false; 
					}	
				}
			}
			else if (resp.equals("2") && !pivPtSet)
				System.out.println("Error: cannot set a filter sequence without first setting a pivot point.\nUse option '1' on the menu.");
			else if (resp.equals("2"))
			{
				boolean fail = true; 
				
				System.out.println("Setting filter sequence to define lexicon subsample.");
				System.out.println("[Filtering from "+pivPtName.replace("pivot@","")+"]"); 
				
				while(fail)
				{	
					System.out.println("To delete the filter and not restore with a new one, type 'DELETE'.\n"
							+ "To retain it, type 'KEEP'.\n"
							+ "Otherwise, enter the phoneme sequence filter, delimiting phones with '"+UTILS.PH_DELIM+"':");
					
					resp = UTILS.stripEnds(inpu.nextLine().replace("\n",""));
							// resp = inpu.nextLine().replace("\n","").strip(); 
					
					if (UTILS.stripEnds(resp).equalsIgnoreCase("delete"))	//resp.strip().equalsIgnoreCase("delete"))
					{
						filterSeq = new SequentialFilter(new ArrayList<RestrictPhone>(), new String[] {}); 
						filterIsSet = false; 
						fail =false; 
						ea.removeFilter();
					}
					else if (UTILS.stripEnds(resp).equalsIgnoreCase("keep"))
						fail = false; 
					else {
						try {
							filterSeq = fac.parseNewSeqFilter(resp, true);
							fail = false;
						}
						catch (Exception e)
						{
							System.out.println("That is not a valid filter.\nTry again and double check spelling of any feature names, and that the proper delimitation is used...");
						}
						
						if(!fail)
						{
							System.out.println("Success: now making subsample\nNew filter: "+filterSeq.toString());
							System.out.println("(Pivot moment name: "+pivPtName+")");
							
							ea.setFilter(filterSeq,pivPtName);
							filterIsSet = true; 
						}
					}
				}
			}
			else if(resp.equals("3"))
			{
				boolean promptQueryMenu = true; 
				while(promptQueryMenu)
				{	System.out.print("What is your query? Enter the corresponding indicator:\n"
							+ "0 : get ID of an etymon by form at input"
								+ (inputName.equalsIgnoreCase("input") ? "" : " ("+inputName+")")
								+"\n"
							+ "1 : get etymon's input form by ID number\n"
							+ "2 : print all etyma by ID\n"
							+ "3 : get derivation up to this point for etymon by its ID\n"
							+ "4 : get rule by time step\n"
							+ "5 : get time step(s) of any rule whose string form contains the submitted string\n"
							+ "6 : print all rules by time step.\n"
							+ "7 : get phonemic inventory at pivot point (you need to have set pivot point).\n"
							+ "8 : get any comments left in lexicon file for an etymon, by its ID.\n"
							+ "9 : return to main menu.\n"); 
					resp = ""; 
					while (resp.equals(""))
						resp = UTILS.stripEnds(inpu.nextLine().replace("\n","")); 
							// resp = inpu.nextLine().replace("\n","").strip(); 
					resp.replace("\n", "");
					
					promptQueryMenu = false;
					if( !"0123456789".contains(resp) || resp.length() > 1 ) {
						System.out.println("Error : '"+resp+"' is not in the list of valid indicators. Please try again.");
						promptQueryMenu = true;
					}
					else if (resp.equals("9"))	promptQueryMenu = false;
					else if (resp.equals("0")) {
						System.out.println("Enter the input form, separating phones by the character '"+UTILS.PH_DELIM+"' (space)"); 
							// TODO remember to change it saying "space" if that is ever changed.
						resp = inpu.nextLine().replace("\n",""); 
						Etymon query = null;
						boolean reconstructed = false; 
						if (resp.charAt(0) == '*') {
							reconstructed = true; 
							resp = resp.substring(1); 
						}
						try {
							query = new Etymon(fac.parseSeqPhSeg(resp),reconstructed);
						}
						catch (Exception e){
							System.out.println("Error: could not parse entered phone string. Returning to query menu.");
							promptQueryMenu = true;
						}
						if(!promptQueryMenu)
						{
							Etymon[] wl = inputForms;
							String inds = UTILS.etymInds(wl, query);
							if (inds.trim().equals(""))
								System.out.println("No input forms found for '"+query+", check the form and try again."); 
							else	System.out.println("Ind(s) with the form /"+query+"/ as input : "+inds);  
						}
					}
					else if(resp.equals("1")||resp.equals("3") || resp.equals("4") || resp.equals("8"))
					{
						System.out.println("Enter the " + (resp.equals("4") ? "rule number" : "ID" ) +" to query:");
						String idstr = inpu.nextLine();  
						boolean queryingRule = resp.equals("4"); //otherwise we're querying an etymon.
						int theID = UTILS.getValidInd(idstr, queryingRule ? CASCADE.size() : NUM_ETYMA - 1) ; 
						if (theID == -1){
							System.out.println("Oops, '"+idstr+"' is not a valid ID for a" 
									+ (queryingRule ? " rule" : "n etymon")
									+ ". Please try again!"); 
							promptQueryMenu =true;
						}
						else if(queryingRule)
						{
							promptQueryMenu = theID < 0 || theID >= CASCADE.size();
							if(promptQueryMenu)	System.out.println("Error -- there are only "+CASCADE.size()+"rules. Returning to query menu."); 
							else	printRuleAt(theID); 
						}
						else
						{
							if(promptQueryMenu)	/*shouldn't occur anyways but oh well*/	System.out.println("Error -- there are only "+NUM_ETYMA+" etyma. Returning to query menu."); 
							else if(resp.equals("1"))	System.out.println(inputForms[theID]); 
							else if (resp.equals("3"))	System.out.println(""+theSimulation.getDerivation(theID));
							else /*must be 8, comments*/ 
							{
								String cmt = inputForms[theID].getComments().trim(); 
								if (cmt.length() == 0)	System.out.println("No comments for this item!"); 
								else	System.out.println(cmt);
								promptQueryMenu = true; 
							}
						}
					}
					else if(resp.equals("2"))
					{
						System.out.println("etymID"+UTILS.STAGE_PRINT_DELIM+inputName+UTILS.STAGE_PRINT_DELIM+"Gold");
						for (int i = 0 ; i < r.getWordList().length ; i++)
							System.out.println(""+i+UTILS.STAGE_PRINT_DELIM+inputForms[i]+UTILS.STAGE_PRINT_DELIM+goldOutputLexicon.getByID(i));
					}
					else if(resp.equals("5"))
					{
						System.out.println("Note that '(...)+' notation has been converted to '(...) (...)*' internally, "
								+ "so inputs of ')+' will not return any results.\nFurthermore, note that the character '"+UTILS.PH_DELIM+"' "
										+ "is necessary to delimit phones and functional parts of the sound change ('>', etc.)\n"+
								"Enter the string you want to query with: \n");
						
						resp = ""; 
						while (resp.equals(""))
							resp = UTILS.stripEnds(inpu.nextLine().replace("\n",""));
							//resp = inpu.nextLine().replace("\n","").strip(); 
						resp.replace("\n", "").replace("([","( [").replace("])", "] )"); 
						
						boolean noMatches = true; 
						
						for(int ci = 0; ci < CASCADE.size(); ci++)
						{
							if (CASCADE.get(ci).toString().contains(resp))
							{	System.out.println(""+ci+" : "+CASCADE.get(ci).toString());
								noMatches = false;
							}
						}
						if(noMatches)	System.out.println("No matches found."); 
					}
					else if(resp.equals("7")) 
					{
						if (pivPtSet)
						{
							System.out.println("Phonemes present in lexicon at pivot point, "+pivPtName); 
							Phone[] inventory = pivPtLex.getPhonemicInventory(false); 
							for (Phone phonemi : inventory)
								System.out.println(phonemi.print()+": "+UTILS.spellOutFeatVect(phonemi.getFeatVect()));
						}
						else 
							System.out.println("Error : requested phonemic inventory of pivot lexicon (option 7), but no pivot point has been chosen.\n"
									+ "Please return to the main menu and choose the option to designate a pivot point if you wish to use this.");
							
					}
					else //"6"
					{
						for(int ci = 0 ; ci < CASCADE.size(); ci++) {
							String stageDetection = getStageNameHere(ci);
							if (!stageDetection.equals("none"))
							{
								if (stageDetection.contains(",")) 
								{ 
									String[] stagesHere = stageDetection.split(","); 
									for (String shi : stagesHere) 
										System.out.println("\t-- "+shi+" --"); 
								}
								else	System.out.println("\t-- "+stageDetection+" --"); 
							}
							System.out.println(""+ci+": "+CASCADE.get(ci)); 

						}
					}
				}
			}
			else if(resp.equals("6"))	ea.confusionDiagnosis(true);
			else if(resp.equals("5"))
			{
				if(!ea.isFiltSet())
					System.out.println("Error: tried to do context autopsy without beforehand setting filter stipulations: You can do this with 2.");
				else if (!ea.isPivotSet()) System.out.println("Error: can't do context autopsy without first setting pivot point. Use option 1.");
				else	{
					resp = ""; 
					while (resp.length() == 0)
					{
						System.out.println("Please indicate which metric for correlation to error you would like to use: ");
						System.out.println("Options include: ");
						System.out.println("\t'phi': the phi coefficient (Matthews Correlation Coefficient), equivalent to the Pearson coefficient in this case"); 
						System.out.println("\t'f': an F-score (harmonic mean between precision and recall, \n\t\t(i.e. precision of a contextual predictor in predicting error, that is, likewise for recall.)");
						System.out.println("\t'f<RATIO>': an f-score where <RATIO> is replaced by a value >= 0 that indicates the ratio of importance of recall vis a vis precision.\n\t\t(recommendation: favor precision over recall, but not too much!)"); 
						System.out.println("\t'comp': comparison of prepared values for the four different metrics."); 
					
						resp = ""; 
						while (resp.equals(""))
							resp = UTILS.stripEnds(inpu.nextLine().replace("\n",""));
							//resp = inpu.nextLine().replace("\n","").strip(); 
				
						if (resp.length() == 0) continue; 
						else if (resp.length() >= 4 ? resp.substring(0,4).equalsIgnoreCase("comp") : false)					
							ea.contextAutopsyComparison();		
						else if (resp.equalsIgnoreCase("phi"))
							ea.contextAutopsy("phi");
						else if (resp.equalsIgnoreCase("f"))
							ea.contextAutopsy("f"); 
						else if (UTILS.valid_fB(resp))
							ea.contextAutopsy(resp); 
						else
							System.out.println("Invalid response: '"+resp+"'. Please follow the instructions below. ");
					}	
				}
			}
			else if(resp.equals("4"))
			{
				boolean subcont = true; 
				
				while(subcont) {
				
					System.out.print("What results would you like to check? Please enter the appropriate number:\n"
						+ "| 0 : Print stats (at evaluation point) (for subset lexicon if specified)~~~~~~~~~~~~~|\n"
						+ "| 1 : Print all corresponding forms (init(,pivot),res,gold) (for subset if specified) |\n"
						+ "| 2 : Print all corresponding forms as above for all errant etyma                     |\n"
						+ "| 3 : Print all mismatched forms only at eval point (for subset if specified)         |\n"
					    + "| 4 : Print all corresponding forms at each stage up to now (for subset if specified) |\n"
						+ "| 5 : Print all corresponding forms for errant etyma as above (for subset if spec'd)  |\n"
						+ "| 9 : Exit this menu._________________________________________________________________|\n");  
					
					resp = ""; 
					while (resp.equals(""))
						resp = inpu.nextLine().replace("\n","");
					
					resp = resp.substring(0,1);
					
					if("45".contains(resp) && !resp.contains("45") && NUM_GOLD_STAGES == 0 && NUM_BLACK_STAGES == 0) {
						resp = resp.equals("4") ? "1" : "2"; 
						//TODO debugging
						System.out.println("... no stages set..."); 
					}
					
					if(resp.equals("0"))
					{
						System.out.println("Printing stats:"+ (ea.isFiltSet() ? " for filter "+filterSeq.toString()+ " at "+pivPtName : "" ));
						System.out.println(UTILS.getAccuracyReport(ea));
					}
					else if("12".contains(resp) && !resp.contains("12"))
					{
						boolean is2 = "2".equals(resp); 
						System.out.println("Printing all "+(is2 ? "mismatched ":"")+
								"etyma: \n#,\t| "+inputName+" | " + (ea.isPivotSet() ? "PIV: "+pivPtName+" | " : "")
								+" Result | Gold"); 
						ea.printFourColGraph(theSimulation.getInput(), is2);	
					}
					else if("45".contains(resp))
					{	
						boolean errsOnly = "5".equals(resp); 
						String headerRow = "     |"+UTILS.append_space_to_x(inputName, 19)+"|";
						List<Lexicon> lexCols = new ArrayList<Lexicon>();
						lexCols.add(theSimulation.getInput()); 
						
						boolean pivot_inserted = false; 
						if ("InGoldOut".contains(pivPtName)) pivot_inserted = true; 
						
						for (int cosi = 0 ; cosi < (atOutput ? NUM_STAGES() : Arrays.asList(stageOrdering).indexOf("G"+curSt))
							; cosi++) { 
							if (ea.isPivotSet() && !pivot_inserted)
							{
								if (pivPtLoc < allStageInstants[cosi]) 
								{
									headerRow += UTILS.append_space_to_x("PIV@"+pivPtName, 19)+"|";
									lexCols.add(pivPtLex);
									pivot_inserted = true; 
								}
							}
							
							String currSt = stageOrdering[cosi]; 
							boolean goldHere = currSt.charAt(0) == 'G'; 
							int stageNum = Integer.parseInt(currSt.substring(1)); 
							headerRow += UTILS.append_space_to_x(
									(goldHere ? goldStageNames : blackStageNames)[stageNum], 19) + "|"; 
							lexCols.add(theSimulation.getStageResult(goldHere, stageNum)); 
						}
						
						if (ea.isPivotSet() && !pivot_inserted) {
							headerRow += UTILS.append_space_to_x("PIV@"+pivPtName, 19)+"|";
							lexCols.add(pivPtLex);
							pivot_inserted = true; 
						}
						
						headerRow += UTILS.append_space_to_x(" Prediction",19) +" | Gold"; 
						lexCols.add(theSimulation.getCurrentResult()); 
						lexCols.add(atOutput ? theSimulation.getGoldOutput() : theSimulation.getGoldStageGold(curSt)); 
						System.out.println("Printing all "+(errsOnly ? "mismatched ":"")+
								"etyma: \n#"+headerRow);
						ea.printStagedGraph(lexCols, errsOnly);
					}
					else if(resp.equals("3"))
					{
						System.out.println("Printing all mismatched etyma" + (ea.isFiltSet() ? " for filter "+filterSeq.toString()+" at "+pivPtName : "" ));
						System.out.println("Res : Gold");
						List<Etymon[]> mms = ea.getCurrMismatches(new ArrayList<SequentialPhonic>(), true);
						for (Etymon[] mm : mms)
							System.out.println(mm[0].print()+" : "+mm[1].print());
					}
					else if (resp.equals("9"))
					{
						System.out.println("Going back to main menu"); 
						subcont = false;
					}
					else	System.out.println("Invalid response. Please enter one of the listed numbers"); 
				}
			}
			else if(resp.equals("7")) //forking test for proposed changes to cascade. 
			{
				if (!atOutput)
				{
					System.out.println("DHS not yet enabled before final output point is reached!"); 
					continue;
				}
				
				DHSWrapper DHSinterface = new DHSWrapper(theSimulation, cascFileLoc, fac); 
				DHSinterface.queryProposedChanges(inpu); 
			}
			else if(resp.equals("9")) {
				System.out.println("Ending..."); cont = false; 
			}
			else	System.out.println("Invalid response. Please enter one of the listed numbers"); 
			
		}
	}
	
	
	//TODO below is abrogated as it is not in use. 
	//makes  EA object on subset of gold/res pairs that have a specified sequence in either the gold or res as flagged by boolean second param
	/**public static ErrorAnalysis analyze_subset_with_seq (Lexicon ogRes, Lexicon ogGold, List<SequentialPhonic> targSeq, boolean look_in_gold)
	{
		if (targSeq.size() == 0)	throw new Error("Can't make subset based on empty sequence"); 
		List<Integer> indsInSubset = new ArrayList<Integer>(); 
		Lexicon toCheck = look_in_gold ? ogGold : ogRes;
		
		for (int i = 0 ; i < NUM_ETYMA; i++)
			if (Collections.indexOfSubList( toCheck.getByID(i).getPhonologicalRepresentation(), targSeq) != -1)
				indsInSubset.add(i);
		
		int subset_size = indsInSubset.size();
		
		LexPhon[] subRes = new LexPhon[subset_size], subGold = new LexPhon[subset_size]; 
		
		for (int j = 0 ; j < subset_size; j++)
		{
			int k = indsInSubset.remove(0); 
			subRes[j] = ogRes.getByID(k); 
			subGold[j] = ogGold.getByID(k); 	
		}
		 
		return setupErrorAnalysis(new Lexicon(subRes), new Lexicon(subGold)); 
		
	}
	**/
	
	// for the lexicon of any given stage, passed as parameter, 
	// outputs hashmap where the value for each key Phone instance
	// is the average Levenshtein distance for words containing that phone 
	// normalized for length of the word
	// counted for the number of times the phone actually occurs in that word out of total phones in the word, each time. 
	/** TODO currently abrogated, as not in use. 
	private static HashMap<Phone,Double> avgLDForWordsWithPhone (Lexicon lexic)
	{
		Etymon[] lexList = lexic.getWordList(); //indices should correspond to those in missLocations
		int lexSize = lexList.length; 

		Phone[] phonemicInventory = lexic.getPhonemicInventory(); 
		int inventorySize = phonemicInventory.length; 
		HashMap<String,Integer> phonemeIndices = new HashMap<String,Integer>();
			//maps print symbols of phoneme onto its index in phonemicInventory array
		for(int phii = 0 ; phii < phonemicInventory.length; phii++)
			phonemeIndices.put( phonemicInventory[phii].print(), phii); 
		
		int[] totalLevenshtein = new int[inventorySize]; //total levenshtein edit distance 
			// of words with each phone
		
		for(int li = 0; li < lexSize; li++)
		{
			List<SequentialPhonic> phs = lexList[li].getPhonologicalRepresentation();
			for (SequentialPhonic ph : phs)
			{
				if(ph.getType().equals("phone"))
				{					
					totalLevenshtein[phonemeIndices.get(ph.print())] += 
							ErrorAnalysis.levenshteinDistance(theSimulation.getCurrentResult().getByID(li),
									goldOutputLexicon.getByID(li)) / (double)goldOutputLexicon.getByID(li).getNumPhones() ;
				}
			}
		}
		
		HashMap<String,Integer> phoneFreqsByWordInLex = lexic.getPhoneFrequenciesByWord(); 

		HashMap<Phone,Double> output = new HashMap<Phone,Double>(); 
		for(int phi = 0; phi < inventorySize; phi++)
		{
			output.put(phonemicInventory[phi], 
					(double)totalLevenshtein[phi] / 
					(double)phoneFreqsByWordInLex.get(phonemicInventory[phi].getFeatString()));
		}
		return output;
	}**/ 
	
	
	/** TODO currently abrogated, as not in use. 
	private static HashMap<Phone,Double> avgFEDForWordsWithPhone (Lexicon lexic)
	{
		Etymon[] lexList = lexic.getWordList(); //indices should correspond to those in missLocations
		int lexSize = lexList.length; 

		Phone[] phonemicInventory = lexic.getPhonemicInventory(); 
		int inventorySize = phonemicInventory.length; 
		HashMap<String,Integer> phonemeIndices = new HashMap<String,Integer>();
			//maps print symbols of phoneme onto its index in phonemicInventory array
		for(int phii = 0 ; phii < phonemicInventory.length; phii++)
			phonemeIndices.put( phonemicInventory[phii].print(), phii); 
		
		int[] totalFED = new int[inventorySize]; //total feature edit distance 
			// of words with this phone
		
		FED distMeasure = feats_weighted ? new FED(featsByIndex.length, FT_WTS,UTILS.ID_WT) : new FED(featsByIndex.length, UTILS.ID_WT); 
		
		for(int li = 0 ; li < lexSize ; li++)
		{
			List<SequentialPhonic> phs = lexList[li].getPhonologicalRepresentation();
			for (SequentialPhonic ph : phs)
			{
				if(ph.getType().equals("phone"))
				{
					distMeasure.compute(theSimulation.getCurrentResult().getByID(li),
							goldOutputLexicon.getByID(li));
					totalFED[phonemeIndices.get(ph.print())] += distMeasure.getFED();
				}
			}
		}
		
		HashMap<String,Integer> phoneFreqsByWordInLex = lexic.getPhoneFrequenciesByWord(); 

		HashMap<Phone,Double> output = new HashMap<Phone,Double>(); 
		for(int phi = 0; phi < inventorySize; phi++)
		{
			output.put(phonemicInventory[phi], 
					(double)totalFED[phi] / 
					(double)phoneFreqsByWordInLex.get(phonemicInventory[phi].getFeatString()));
		}
		return output;
		
	}**/ 
	
	// required : runPrefix must be specified 
	// flags: -r : debug rule processing
	//		  -p : print words every time they are changed by a rule
	//		  -e : (explicit) do not use feature implications
	//		  -h : halt at stage checkpoints
	//		  -i : ignore stages
	//		  -s : skip file creation
	// variable setting otherwise: -verbose, -out (for run prefix), 
	//        -symbols, -impl (feature implications file location), 
	//        -rules (cascade location), -diacrit (diacritics file location), 
	//        -idcost (insertion/deletion cost)
	//		  -simple_FED (use constant value rather than contextual similarity calculation for insertion/deletion cost in FED) 
	//		  -debug_stages: debug stage processing 
	//
	private static void parseArgs(String[] args)
	{
		int i = 0, j; 
		String arg;
		char flag; 
		VERBOSE = false;
		
		boolean no_prefix = true; 
		
		//defaults
		symbDefsLoc = "symbolDefs.csv";
		lexFileLoc = "FLLex.txt";
		cascFileLoc = "DiaCLEF"; 
		featImplsLoc = "FeatImplications"; 
		symbDiacriticsLoc = "currentSymbolDiacriticDefs.txt";
		UTILS.ID_WT = 0.5; 
		
		
		DEBUG_RULE_PROCESSING = false; DEBUG_STAGES = false; 
		print_changes_each_rule = false;
		no_feat_impls = false;
		no_symb_diacritics = true; 
		skip_file_creation = false;
		UTILS.contextualize_FED = true; 
		
		while (i < args.length && args[i].startsWith("-"))	
		{
			arg = args[i++];
			
			if (arg.equals("-verbose"))	
			{
				VERBOSE = true; 
				UTILS.VERBOSE = true; 
			}
			
			//variable setters
			
			// output prefix -- without this, the folder will be based on the date
			else if (arg.contains("-out"))
			{
				if (i < args.length)
					runPrefix = args[i++]; 
				else	System.err.println("Output prefix specification requires a string");
				if (VERBOSE)	System.out.println("output prefix: "+runPrefix);
				no_prefix = false; 
			}
			
			// symbol definitions file location
			else if (arg.equals("-symbols"))
			{
				if (i < args.length)	symbDefsLoc = args[i++];
				else	System.err.println("-symbols requires a location");
				if (VERBOSE)	System.out.println("symbol definitions location: "+symbDefsLoc);
			}
			
			//feature implications file location
			else if (arg.contains("-impl"))
			{
				if (i < args.length)	featImplsLoc = args[i++]; 
				else	System.err.println("-impl requires a location for feature implications location.");
				if (VERBOSE)	System.out.println("feature implications location: "+featImplsLoc);
			}
			
			//ruleset file location
			else if (arg.contains("-rules"))
			{
				if (i < args.length)	cascFileLoc = args[i++];
				else	System.err.println("-rules requires a location for ruleset file.");
				if (VERBOSE)	System.out.println("ruleset file location: "+cascFileLoc);
			}
			
			//flag to use diacritics, and the location of the diacritics file. 
			//user can just flag this with no file to use the default location. 
			//otherwise they should place the location afterward. 
			else if (arg.contains("-diacrit"))
			{
				
				no_symb_diacritics = false; 
				
				if (i < args.length) 
				{
					if (args[i].charAt(0) != '-')	symbDiacriticsLoc=args[i++]; 
					else	symbDiacriticsLoc = "currentSymbolDiacriticDefs.txt"; // default. 
				}
				
				if (VERBOSE)	System.out.println("diacritics file location: "+symbDiacriticsLoc);

			}
			
			//lexicon location
			else if (arg.contains("-lex"))
			{
				if (i < args.length)	lexFileLoc = args[i++];
				else	System.err.println("-lex requires a location for lexicon file location.");
				if (VERBOSE)	System.out.println("lexicon file location: "+lexFileLoc);
			}
			
			//insertion/deletion cost
			else if (arg.equals("-idcost"))
			{
				if (i < args.length)	UTILS.ID_WT = Double.parseDouble(args[i++]);
				else	System.err.println("-idcost requires a double for ratio of insertion/deletion cost to substitution");
				if (VERBOSE)	System.out.println("insertion/deletion cost ratio to substitution: "+UTILS.ID_WT); 
			}
			
			else if (arg.equals("-simple_FED"))
				UTILS.contextualize_FED = false; 
			
			else if (arg.equalsIgnoreCase("-use_form_ID"))
				UTILS.USE_FORM_ID = true; 
			
			// nothing placed afterward -- triggers stage debugging printouts. 
			else if (arg.equalsIgnoreCase("-debug_stages"))
			{
				DEBUG_STAGES = true; 
				if (VERBOSE)	System.out.println("debugging stage processing"); 
			}
		
			//flag args
			else
			{
				if (VERBOSE)	System.out.println("single character flag set detected: "+arg); 
				
				for (j = 1; j < arg.length(); j++)
				{	
					flag = arg.charAt(j);
					switch(flag)	{
						case 'r':
							DEBUG_RULE_PROCESSING = true;
							if (VERBOSE)	System.out.println("Debugging rule processing.");
							break; 
						case 'e':  // explicit 
							no_feat_impls = true; 
							UTILS.no_feat_impls = true; 
							if (VERBOSE)	System.out.println("Ignoring any feature implications.");
							break; 
						case 'p':
							print_changes_each_rule = true;
							if (VERBOSE)	System.out.println("Printing words changed for each rule.");
							break;
						case 'h':
							stage_pause = true; 
							if (VERBOSE)	System.out.println("Halting for analysis at stage checkpoints.");
							break; 
						case 'i':
							ignore_stages = true; 
							if (VERBOSE)	System.out.println("Ignoring all stages.");
							break;
						case 's':
							skip_file_creation = true;
							if (VERBOSE)	System.out.println("Skipping creation of output files and directories.");
							break;
						default:
							System.err.println("Illegal flag : "+flag);
							break;
					}	
				}
			}
		}
		
		// If user hasn't specified a run output location, make a unique run prefix
		if (no_prefix)
		{
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
			runPrefix = "../unnamed_run_"+timeStamp;
			if (!skip_file_creation) {
				System.out.println("No output location specified. Will create a folder with name '"+runPrefix+"'.");
			}
		}
		
		if (i != args.length) //|| no_prefix)
            throw new Error("Usage: DerivationSimulation [-verbose] [-resphi] [-idcost cost] [-rules afile] [-lex afile] [-symbols afile] [-impl afile] [-diacrit afile] [-out prefix]"); 	
	}
	
	private static void printRuleAt(int theInd)
	{
		if (theInd == CASCADE.size())
			System.out.println("Ind "+theInd+" is right after the realization of the last rule.");
		else System.out.println(CASCADE.get(theInd)); 
	}
	
	/**
	 * for @param index, an instant in the cascade... 
	 *  @return a String the name of any stages that is hit right before this rule operates (the "time instant" 
	 *  	-- TODO change terminology per Brian if necessary)
	 * 	in most cases there will be no stage -- in which case it will say "none" 
	 * 	in cases where there are multiple, they are separated by the column delimiter, which is currently ','
	 */
	private static String getStageNameHere (int index)
	{
		if (stageOrdering.length == 0)	return "none" ; 
		
		List<String> stagesHere = new ArrayList<String>(); 
				
		for (String soi : stageOrdering)
		{
			char prefix = soi.charAt(0); 			
			int stageInstant = -1, stageNumber = Integer.parseInt(soi.substring(1)); 
			if (prefix == 'G') stageInstant = goldStageInstants[stageNumber]; 
			else if (prefix == 'b' || prefix == 'B')	stageInstant = blackStageInstants[stageNumber]; 
			else throw new RuntimeException("Error: illegal prefix for stage at "+index+". Entry in stagesOrdered: "+soi); 
					
			if (stageInstant > index)	break; 
			if (stageInstant == index)
			{
				if (prefix == 'G') stagesHere.add("Gold Stage: "+goldStageNames[stageNumber]); 
				else if (prefix == 'b' || prefix == 'B')	
					stagesHere.add( (prefix == 'B' ? "Columned " : "")+"Black Stage: "+blackStageNames[stageNumber]); 
			}
		}
		
		if (stagesHere.size() == 0)		return "none"; 
		if (stagesHere.size() == 1)	return stagesHere.get(0); 
		return	String.join(",", stagesHere); 
	}
}