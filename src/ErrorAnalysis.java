import java.util.HashMap;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;

public class ErrorAnalysis {
	private int[] levDists; 
	private int[] FILTER; //indices of all etyma in subset
	private int[] PRESENT_ETS; 
	private double[] peds, feds;
	private boolean[] isHit; 
	private boolean[] IN_SUBSAMP;
	private double pctAcc, pctWithin1, pctWithin2, avgPED, avgFED; 
	private List<List<int[]>> SS_HIT_BOUNDS, SS_MISS_BOUNDS;
	private int[] SS_HIT_IDS, SS_MISS_IDS; 
	
	private SequentialFilter filterSeq; 
	
	private Phone[] resPhInventory, goldPhInventory, pivotPhInventory;
	
	protected final String ABS_PR ="[ABSENT]"; 
	protected final int MAX_RADIUS = 3;

	
	private HashMap<String, Integer> resPhInds, goldPhInds, pivPhInds;
		// indexes for phones in the following int arrays are above.
	protected int[] errorsByResPhone, errorsByGoldPhone; 
	protected int[] errorsByPivotPhone;
	private double[] errorRateByResPhone, errorRateByGoldPhone; 
	private double[] errorRateByPivotPhone;
	private int[][] confusionMatrix; 
		// rows -- indexed by resPhInds; columns -- indexed by goldPhInds
	
	private String[] featsByIndex; 
		
	private boolean[][] isPhInResEt, isPhInGoldEt, isPhInPivEt; 
		
	private List<LexPhon[]> mismatches; 
	
	private final int NUM_TOP_ERR_PHS_TO_DISP = 4; 
	
	private int NUM_ETYMA, SUBSAMP_SIZE;
	private double TOT_ERRS;
	
	private FED featDist;
	
	private Lexicon RES, GOLD, FOCUS;
	
	private boolean focSet, filtSet; 
	
	public final double AUTOPSY_DISPLAY_THRESHOLD = 0.3;
	
	public ErrorAnalysis(Lexicon theRes, Lexicon theGold, String[] indexedFeats, FED fedCalc)
	{
		RES = theRes;
		GOLD = theGold; 
		FOCUS = null; //must be manually set later.
		
		filtSet = false;
		focSet = false;
		
		featDist = fedCalc; 
		featsByIndex = indexedFeats;
		NUM_ETYMA = theRes.getWordList().length;
		
		resPhInventory = theRes.getPhonemicInventory();
		goldPhInventory = theGold.getPhonemicInventory();
		
		resPhInds = new HashMap<String, Integer>(); 
		goldPhInds = new HashMap<String, Integer>();
		
		for(int i = 0 ; i < resPhInventory.length; i++)
			resPhInds.put(resPhInventory[i].print(), i);
		for (int i = 0 ; i < goldPhInventory.length; i++)
			goldPhInds.put(goldPhInventory[i].print(), i);
				
		NUM_ETYMA = theRes.getWordList().length;
		SUBSAMP_SIZE = NUM_ETYMA - theRes.numAbsentEtyma();
		
		FILTER = new int[SUBSAMP_SIZE];
		PRESENT_ETS = new int[SUBSAMP_SIZE];
		int fi = 0;
		for (int i = 0 ; i < NUM_ETYMA; i++)
		{	if (!theRes.getByID(i).print().equals(ABS_PR))
			{	FILTER[fi] = i;
				PRESENT_ETS[fi] = i;
				fi++;
			}
		}
		
		isPhInResEt = new boolean[resPhInventory.length][NUM_ETYMA]; 
		isPhInGoldEt = new boolean[goldPhInventory.length][NUM_ETYMA]; 
		
		errorsByResPhone = new int[resPhInventory.length];
		errorsByGoldPhone = new int[goldPhInventory.length];

		errorRateByResPhone = new double[resPhInventory.length]; 
		errorRateByGoldPhone = new double[goldPhInventory.length];

		confusionMatrix = new int[resPhInventory.length + 1][goldPhInventory.length + 1];
		// final indices in both dimensions are for the null phone
		
		mismatches = new ArrayList<LexPhon[]>();
		
		levDists = new int[NUM_ETYMA]; 
		peds = new double[NUM_ETYMA];
		feds = new double[NUM_ETYMA];
		isHit = new boolean[NUM_ETYMA];
		double totLexQuotients = 0.0, numHits = 0.0, num1off=0.0, num2off=0.0, totFED = 0.0; 
				
		IN_SUBSAMP = new boolean[NUM_ETYMA]; 
		
		for (int i = 0 ; i < NUM_ETYMA ; i++)
		{	
			IN_SUBSAMP[i] = true; 		// until filter is set, all words are "in the subsample"

			for(int rphi = 0 ; rphi < resPhInventory.length; rphi++)
			{
				LexPhon currEt = theRes.getByID(i);
				isPhInResEt[rphi][i] = (currEt.toString().equals("[ABSENT]")) ? 
						false : (currEt.findPhone(resPhInventory[rphi]) != -1);
			}
			for (int gphi = 0 ; gphi < goldPhInventory.length; gphi++)
			{
				LexPhon currEt = theGold.getByID(i);
				isPhInGoldEt[gphi][i] = (currEt.toString().equals("[ABSENT]")) ?
						false : (currEt.findPhone(goldPhInventory[gphi]) != -1);
			}
			
			if (!theRes.getByID(i).print().equals(ABS_PR) && !theGold.getByID(i).print().equals(ABS_PR))
			{	
				levDists[i] = levenshteinDistance(theRes.getByID(i), theGold.getByID(i));
				isHit[i] = (levDists[i] == 0); 
				numHits += (levDists[i] == 0) ? 1 : 0; 
				num1off += (levDists[i] <= 1) ? 1 : 0; 
				num2off += (levDists[i] <= 2) ? 1 : 0; 
				peds[i] = (double)levDists[i] / (double) theGold.getByID(i).getNumPhones();
				totLexQuotients += peds[i]; 
				
				featDist.compute(theRes.getByID(i), theGold.getByID(i)); 
				
				feds[i] = featDist.getFED();
				totFED += feds[i];
				
				if(!isHit[i])
					updateConfusionMatrix(i);
						//also increments errorsBy(Res/Gold)Phone^ 
			}
			else	isHit[i] = true;
		}
		pctAcc = numHits / (double) SUBSAMP_SIZE; 
		pctWithin1 = num1off / (double) SUBSAMP_SIZE;
		pctWithin2 = num2off / (double) SUBSAMP_SIZE; 
		avgPED = totLexQuotients / (double) SUBSAMP_SIZE; 	
		avgFED = totFED / (double) SUBSAMP_SIZE; 
		TOT_ERRS = (double)NUM_ETYMA - numHits;
		
		//calculate error rates by phone for each of result and gold sets
		HashMap<String, Integer> resPhCts = theRes.getPhonemeCounts(), 
				goldPhCts = theGold.getPhonemeCounts(); 
		
		for (int i = 0 ; i < resPhInventory.length; i++)
			errorRateByResPhone[i] = (double)errorsByResPhone[i] 
					/ (double)resPhCts.get(resPhInventory[i].print());
		for (int i = 0 ; i < goldPhInventory.length; i++)
			errorRateByGoldPhone[i] = (double)errorsByGoldPhone[i]
					/ (double)goldPhCts.get(goldPhInventory[i].print()); 
	}

	public void toDefaultFilter()
	{
		SUBSAMP_SIZE = PRESENT_ETS.length;
		FILTER = new int[SUBSAMP_SIZE];
		for (int i = 0 ; i < SUBSAMP_SIZE; i++)	FILTER[i] = PRESENT_ETS[i];
		filterSeq = null;
	}
	
	public void setFilter(SequentialFilter newFilt, String stage_name)
	{
		filterSeq = newFilt; 
		filtSet = true;
		if(focSet)	articulateSubsample(stage_name); 
	}
	
	public void setFocus(Lexicon newFoc, String stage_name)
	{
		FOCUS = newFoc; 
		pivotPhInventory = newFoc.getPhonemicInventory();
		
		pivPhInds = new HashMap<String, Integer>(); 
		
		for(int i = 0 ; i < pivotPhInventory.length; i++)
			pivPhInds.put(pivotPhInventory[i].print(), i);
		
		focSet = true; 
		
		isPhInPivEt = new boolean[pivotPhInventory.length][NUM_ETYMA]; 
		int[] pivPhCts = new int[pivotPhInventory.length]; 
		for (int ei = 0 ; ei < NUM_ETYMA ; ei++)
		{
			LexPhon currEt = FOCUS.getByID(ei);
			for(int pvi = 0 ; pvi < pivotPhInventory.length; pvi++)
			{
				if(!currEt.toString().equals("[ABSENT]"))
					isPhInPivEt[pvi][ei] = (currEt.findPhone(pivotPhInventory[pvi]) != -1);
				else	isPhInPivEt[pvi][ei] = false;
				if(isPhInPivEt[pvi][ei])	pivPhCts[pvi] += 1; 
			}
		}
		
		if(filtSet)	articulateSubsample(stage_name); 
		else
		{
			errorsByPivotPhone =  new int[pivotPhInventory.length];
			errorRateByPivotPhone = new double[pivotPhInventory.length]; //to avoid errors. 
			for (int ei = 0 ; ei < NUM_ETYMA ; ei++)	
			{
				if(!isHit[ei])
					for (SequentialPhonic pivPh : FOCUS.getByID(ei).getPhOnlySeq())
						errorsByPivotPhone[pivPhInds.get(pivPh.print())] += 1; 
			}
			for (int i = 0 ; i < pivotPhInventory.length; i++)
				errorRateByPivotPhone[i] = (double)errorsByPivotPhone[i] / (double)pivPhCts[i]; 
		}
	}
	
	//@param get_contexts -- determine if we want to list the most problematic context info
	public void confusionPrognosis(boolean get_contexts)
	{
		// top n error rates for res and gold
		int[] topErrResPhLocs = arrLocNMax(errorRateByResPhone, NUM_TOP_ERR_PHS_TO_DISP); 
		int[] topErrGoldPhLocs = arrLocNMax(errorRateByGoldPhone, NUM_TOP_ERR_PHS_TO_DISP); 
		int[] topErrPivotPhLocs = new int[NUM_TOP_ERR_PHS_TO_DISP];
		if (focSet)	topErrPivotPhLocs = arrLocNMax(errorRateByPivotPhone, NUM_TOP_ERR_PHS_TO_DISP); 
		
		double max_res_err_rate = errorRateByResPhone[topErrResPhLocs[0]]; 
		double max_gold_err_rate = errorRateByGoldPhone[topErrGoldPhLocs[0]];  
		
		if (max_res_err_rate > 1.25 * (1.0 - pctAcc) || max_gold_err_rate > 1.25 * (1.0 - pctAcc)) 
		{ 
			System.out.println("Result phones most associated with error: ");
			
			for(int i = 0 ; i < topErrResPhLocs.length; i++)
			{
				double rate = errorRateByResPhone[topErrResPhLocs[i]] ;
				
				// we will suppress once the rate is not more than 117% (100+ 0.5sd) of global error rate
				if (rate < (1.0 - pctAcc) * 1.17)	i = topErrResPhLocs.length; 
				else	System.out.println(""+i+": /"+resPhInventory[topErrResPhLocs[i]].print()+
						"/ with rate "+rate+",\tRate present in mismatches : "
						+(""+(double)errorsByResPhone[topErrResPhLocs[i]]*100.0/(double)mismatches.size()));
				
			}
			System.out.println("Gold phones most associated with error: ");
			for(int i = 0 ; i < topErrGoldPhLocs.length; i++)
			{
				double rate = errorRateByGoldPhone[topErrGoldPhLocs[i]];
				if (rate < pctAcc * 1.17)	i = topErrGoldPhLocs.length; 
				else	System.out.println(""+i+": /"+goldPhInventory[topErrGoldPhLocs[i]].print()+
						"/ with rate "+rate+",\tRate present in mismatches : "
						+(""+(double)errorsByGoldPhone[topErrGoldPhLocs[i]]*100.0/(double)mismatches.size()));
			}
			if(focSet)
			{
				System.out.println("Focus point phones most associated with error: ");
				for (int i = 0; i < topErrPivotPhLocs.length; i++)
				{
					double rate = errorRateByPivotPhone[topErrPivotPhLocs[i]];
					if (rate < pctAcc * 1.17)	i = topErrPivotPhLocs.length;
					else	System.out.println(""+i+": /"+pivotPhInventory[topErrPivotPhLocs[i]].print()+
							"/ with rate "+rate+",\tRate present in mismatches : "
							+(""+(double)errorsByPivotPhone[topErrPivotPhLocs[i]]*100.0/(double)mismatches.size()));
				}
			}
		}
		else	System.out.println("No particular phones especially associated with error.");
			
		System.out.println("---\nMost common confusions: "); 
		int[][] topConfusions = arr2dLocNMax(confusionMatrix, 5); 
		
		for(int i = 0 ; i < topConfusions.length; i++)
		{
			SequentialPhonic rTarget = topConfusions[i][0] == resPhInventory.length ? new NullPhone() : resPhInventory[topConfusions[i][0]],
					gTarget = topConfusions[i][1] == goldPhInventory.length ? new NullPhone() : goldPhInventory[topConfusions[i][1]];
			
			System.out.println("----\nConfusion "+(i+1)+": "+ rTarget.print()+" for "+gTarget.print()); 
			
			double wordsWithConfusion = (double)confusionMatrix[topConfusions[i][0]][topConfusions[i][1]];
					
			double errorShare = wordsWithConfusion / (double)mismatches.size() * 100.0; 
			String strErrShare = ""+errorShare; 
			if (strErrShare.length() > 6)
				strErrShare = strErrShare.substring(0,6); 
			
			System.out.println("% of errant words with this confusion : "+
					strErrShare		+"%"); 
			
			//parse contexts
			if (get_contexts)
			{
				List<String> probCtxts = identifyProblemContextsForConfusion(topConfusions[i][0], topConfusions[i][1]);
				System.out.println("Most common predictors of this confusion : "); 
				for (String obs : probCtxts)	System.out.println(""+obs); 
			}	 
		}
	}
	
	
	
	//also updates errorsByResPhone and errorsByGoldPhone
	//..and also updates the list mismatches 
	private void updateConfusionMatrix(int err_id)
	{
		LexPhon res = RES.getByID(err_id), gold = GOLD.getByID(err_id); 
		
		mismatches.add( new LexPhon[] {res, gold}) ; 
				
		SequentialPhonic[][] alignedForms = getAlignedForms(res,gold); 
		
		for (int i = 0 ; i < alignedForms.length ; i++)
		{
			String r = alignedForms[i][0].print(), g = alignedForms[i][1].print(); 
			
			if(!"#∅".contains(r))	errorsByResPhone[resPhInds.get(r)] += 1; 
			if(!"#∅".contains(g))	errorsByGoldPhone[goldPhInds.get(g)] += 1;
			if (!r.equals(g))
			{
				if (r.equals("∅"))	confusionMatrix[resPhInventory.length][goldPhInds.get(g)] += 1;
				else if(g.equals("∅"))	confusionMatrix[resPhInds.get(r)][goldPhInventory.length] += 1; 
				else	confusionMatrix[resPhInds.get(r)][goldPhInds.get(g)] += 1;
			}
		}
		if (focSet)
			for (SequentialPhonic pivPh : FOCUS.getByID(err_id).getPhOnlySeq())
				errorsByPivotPhone[pivPhInds.get(pivPh.print())] += 1; 
	}
	

	//@prerequisite: indexedCts should have one more index than inventory, for storing instances of the word bound
	private List<String> ctxtPrognose (String ctxtName, int[] indexedCts, SequentialPhonic[] inventory, int total_confusion_instances, double thresh)
	{ 
		List<String> out = new ArrayList<String>();
		int n_wdbd = indexedCts[inventory.length];
		
		if (n_wdbd > (double)total_confusion_instances * 0.2)
			out.add("Percent word bound for "+ctxtName+": " + 100.0 * (double)n_wdbd / (double)total_confusion_instances); 
		if(n_wdbd == total_confusion_instances)	return out;

		String[] candFeats = new String[featsByIndex.length * 2];
		for (int fti = 0 ; fti < featsByIndex.length; fti++)
		{
			candFeats[2*fti] = "-"+featsByIndex[fti];
			candFeats[2*fti+1] = "+"+featsByIndex[fti];
		}
		boolean constFeatsRemain = true; 
		boolean candFeatsTouched = false;
		
		String commonPhs = ""; 
		for(int cti = 0; cti < indexedCts.length - 1; cti++)
		{
			if (indexedCts[cti] > 0)
			{
				SequentialPhonic curPh = inventory[cti];
				double phShare = (double)indexedCts[cti] / (double)total_confusion_instances;
				
				if (phShare > thresh)
					commonPhs += "/"+curPh.print()+"/ ("+(""+100.0*phShare).substring(0,4)+"%) ";
				
				char[] curPhFeats = curPh.toString().split(":")[1].toCharArray(); 
				
				if(constFeatsRemain)
				{
					int cfii = 0;
					boolean cont = true;
					while (cont)
					{
						cont = candFeats[cfii].equals("") && cfii + 1 < candFeats.length;
						cfii = cont ? cfii + 1 : (cfii + 1 == candFeats.length) ? cfii + 1: cfii; 
					}
					if (cfii == candFeats.length)	constFeatsRemain = false;
					while(cfii < candFeats.length)
					{
						if (candFeats[cfii].equals(""))	cfii++;
						else
						{
							int featVal = Integer.parseInt(""+curPhFeats[(int)Math.floor(cfii/2)]); 
							if(((double)featVal)/2.0 != (double)(cfii % 2) * 2.0) //i.e. not pos/pos or neg/neg
							{
								candFeats[cfii] = ""; 
								candFeatsTouched = true;
							}
							cfii++;
						}
					}
				} 
			}
		}
		
		if (!candFeatsTouched)	constFeatsRemain = false; 
		
		if(constFeatsRemain)
		{
			String constFeatMsg = "";
			for(String cft : candFeats)
				if (!cft.equals(""))	constFeatMsg += cft+" ";
			out.add(ctxtName+" phone constant features: "+constFeatMsg);
		}
		else	out.add("No constant features for "+ctxtName);
		
		if(commonPhs.length() > 0)	out.add("Most common "+ctxtName+" phones: "+commonPhs);
		else	out.add("No particularly common "+ctxtName+" phones."); 
		return out;
		
	}
	
	
	private int getPrevAlignedGoldPos (int[][] alignment, int dloc, boolean alignedToNullGold)
	{
		if (!alignedToNullGold)	return dloc - 1; 
		int currResLoc = dloc - 1, out = -1;
		while (out == -1)
		{
			if (currResLoc == -1)	return -1;
			out = alignment[currResLoc][0]; 
			currResLoc = currResLoc - 1; 
		}
		return out; 
	}
	
	private int getNextAlignedGoldPos (int[][] alignment, int dloc, boolean alignedToNullGold, int end)
	{
		if (!alignedToNullGold)	return dloc + 1;
		int currResLoc = dloc, out = -1; 
		while (out == -1)
		{
			if (currResLoc == alignment.length)	return end; 
			out = alignment[currResLoc][0];
			if (out == -2)	return end; 
			currResLoc += 1;
		}
		return out; 
	}
	
	
	public void printAlignment(int[][] anmt, SequentialPhonic[] rphs, SequentialPhonic[] gphs)
	{
		for (int i = 0; i < anmt.length; i++)
		{
			System.out.print(append_space_to_x(i+"",2)+"| "); 
			int ar = anmt[i][1], ag = anmt[i][0];

			System.out.print(append_space_to_x(
					(ar == -1) ? "∅" : rphs [ ag == -2 ? ar : i].print(), 5)+"| ");
			
			System.out.print(
					((ag == -1) ? "∅" : gphs [ ar == -2 ? ag : i].print()) + "\n"); 		
		}
		
	}
	
	// report which contexts tend to surround confusion frequently enough that it becomes 
		// suspicious and deemed worth displaying
	// this method is frequently modified at current state of the project 
	// NOTE: context info for confusions now disabled! 
	private List<String> identifyProblemContextsForConfusion(int resPhInd, int goldPhInd)
	{
		List<String> out = new ArrayList<String>(); 

		List<LexPhon[]> pairsWithConfusion = mismatchesWithConfusion(resPhInd, goldPhInd); 
		
		//NOTE: By default this is done for gold. may need to change that.
		
		int[] prePriorCounts = new int[goldPhInventory.length + 1]; //+1 for word bound #" 
		int[] postPostrCounts = new int[goldPhInventory.length + 1]; 
		int[] priorPhoneCounts = new int[goldPhInventory.length + 1]; 
		int[] posteriorPhoneCounts = new int[goldPhInventory.length + 1]; 
				
		int total_confusion_instances = 0; 

		for (int i = 0 ; i < pairsWithConfusion.size(); i++)
		{
			
			//TODO need to fix error here. 
			
			LexPhon[] curPair = pairsWithConfusion.get(i); 
			
			featDist.compute(curPair[0],curPair[1]); 
			int[][] alignment = featDist.get_min_alignment(); 
				// at each outer index, find at ind 0 : position aligned to res phone at outer ind, 
				// at ind 1 : position aligned to gold phone at outer ind
				// -1 -- aligned to null phone
				// -2 -- aligned to null phone at word boundary
			
			List<Integer> confuseLocs = new ArrayList<Integer>();
			//will be based on location in the gold form, unless it is a res phone aligned to gold null,
				// in which case it will be in the res form. 

			SequentialPhonic[] goldPhs = curPair[1].getPhOnlySeq(), resPhs= curPair[0].getPhOnlySeq();
			boolean nullGold = (goldPhInd == goldPhInventory.length);
		
			if (nullGold) 			{
				
				for(int rpi = 0 ; rpi == resPhs.length ? false : alignment[rpi][0] != -2 ; rpi++)
					if(resPhs[rpi].print().equals(resPhInventory[resPhInd].print()))
						if(alignment[rpi][0] < 0)
							confuseLocs.add(rpi); 
			}
			else {
				for(int gpi = 0 ; gpi < goldPhs.length; gpi++)
				{
					if(goldPhs[gpi].print().equals(goldPhInventory[goldPhInd].print())) 
						if(alignment[gpi][1] >= 0)
							if(resPhs[alignment[gpi][1]].print().equals(resPhInventory[resPhInd].print()))
								confuseLocs.add(gpi); 
				}
			}
			total_confusion_instances += confuseLocs.size(); 

			//now retrieve context IN GOLD of confusion.
			for (Integer dloc : confuseLocs)
			{
				int opLocBefore = getPrevAlignedGoldPos(alignment, dloc, nullGold); 
				
				if (opLocBefore == goldPhs.length) {
					printAlignment(alignment,resPhs,goldPhs); 
				}
				
				
				if(opLocBefore != -1)
				{
					priorPhoneCounts[goldPhInds.get(goldPhs[opLocBefore].print())] += 1; 
					int opLocPrPr = getPrevAlignedGoldPos(alignment, opLocBefore, false); 
					if (opLocPrPr != -1)
						prePriorCounts[goldPhInds.get(goldPhs[opLocPrPr].print())] += 1; 
					else	prePriorCounts[goldPhInventory.length] += 1;
				}
				else	priorPhoneCounts[goldPhInventory.length] += 1;  
				//goldPhInventory.length -- i.e. word bound.

				int opLocAfter = getNextAlignedGoldPos(alignment, dloc, nullGold, goldPhs.length) ;

				if (opLocAfter < goldPhs.length)
				{
					posteriorPhoneCounts[goldPhInds.get(goldPhs[opLocAfter].print())] += 1; 
					int opLocPoPo = getNextAlignedGoldPos(alignment, opLocAfter, false, goldPhs.length) ;
					if(opLocPoPo != goldPhs.length)
						postPostrCounts[goldPhInds.get(goldPhs[opLocPoPo].print())] += 1;
					else postPostrCounts[goldPhInventory.length] += 1; 
				}
				else	posteriorPhoneCounts[goldPhInventory.length] += 1;
			}
		} 
		
		
		
		out.addAll(ctxtPrognose("pre prior",prePriorCounts,goldPhInventory,total_confusion_instances,0.2)); 
		out.addAll(ctxtPrognose("prior",priorPhoneCounts,goldPhInventory,total_confusion_instances,0.2));
		out.addAll(ctxtPrognose("posterior",posteriorPhoneCounts,goldPhInventory,total_confusion_instances,0.2));
		out.addAll(ctxtPrognose("post posterior",postPostrCounts,goldPhInventory,total_confusion_instances,0.2));
		
		return out; 
	}
	
	// return list of word pairs with a particular confusion,
	// as indicated by the pairing of the uniquely indexed result phone
	// and the different uniquely indexed gold phone.
	// if either resPhInd or goldPhInd are -1, they are the null phone. 
	private List<LexPhon[]> mismatchesWithConfusion (int resPhInd, int goldPhInd)
	{
		List<LexPhon[]> out = new ArrayList<LexPhon[]>(); 
		boolean is_insert_or_delete = (resPhInd == resPhInventory.length) ||  (goldPhInd == goldPhInventory.length); 
		for (LexPhon[] mismatch : mismatches)
		{
			if ( is_insert_or_delete)
			{	if(hasMismatch(resPhInd, goldPhInd, mismatch[0], mismatch[1]))	out.add(mismatch); 	}
			else if ( mismatch[0].findPhone(resPhInventory[resPhInd]) != -1 &&
					mismatch[1].findPhone(goldPhInventory[goldPhInd]) != -1)
				if(hasMismatch(resPhInd, goldPhInd, mismatch[0], mismatch[1]))	out.add(mismatch); 	
		}
		return out; 
	}
	
	//check if specific mismatch has a specific confusion
	// we assume either the confusion involves a null phone (i.e. it's insertion or deletion)
	// or we have already checked that both phones involve are in fact present in both words
	private boolean hasMismatch(int rphi, int gphi, LexPhon rlex, LexPhon glex)
	{
		SequentialPhonic[][] alignment = getAlignedForms(rlex, glex); 
	
		SequentialPhonic rph = new NullPhone(), gph = new NullPhone(); 
		if (rphi != resPhInventory.length)	rph = resPhInventory[rphi]; 
		if (gphi != goldPhInventory.length)	gph = goldPhInventory[gphi]; 
		
		for(int ai = 0 ; ai < alignment.length; ai++)
			if (rph.print().equals(alignment[ai][0].print()))
				if (gph.print().equals(alignment[ai][1].print()))	return true; 
		return false; 
	}
	
	//TODO replace with actual alignment algorithm
	private SequentialPhonic[][] getAlignedForms(LexPhon r, LexPhon g)
	{
		//TODO debugging
		System.out.println("r: "+r+"; g "+g);
		
		
		featDist.compute(r, g); //TODO may need to change insertion/deletion weight here!
		int[][] align_stipul = featDist.get_min_alignment(); //TODO check this..
		
		//TODO debugging
		System.out.println("align_stipul: "); 
		for (int asi = 0; asi < align_stipul.length ; asi++)
			System.out.println(UTILS.print1dIntArr(align_stipul[asi])); 
		
		SequentialPhonic[] rphs = r.getPhOnlySeq(), gphs = g.getPhOnlySeq(); 

		int al_len = rphs.length;
		for (int a = 0; a < align_stipul.length; a++)
			if (align_stipul[a][1] == -1)	al_len++; 
		
		SequentialPhonic[][] out = new SequentialPhonic[al_len][2]; 
		int ari = 0, agi = 0;
		
		for(int oi = 0 ; oi < al_len; oi++)
		{
			if (align_stipul[ari][0] == -1)
			{
				out[oi][0] = rphs[ari]; ari++;
				out[oi][1] = new NullPhone(); 
			}
			else if (align_stipul[ari][0] == -2)
			{
				out[oi][0] = new NullPhone(); ari++; 
				out[oi][1] = gphs[agi]; agi++; 
			}
			else if (align_stipul[agi][1] == -1)
			{
				out[oi][0] = new NullPhone(); 
				out[oi][1] = gphs[agi]; agi++;
			}
			else if (align_stipul[agi][1] == -2)
			{
				out[oi][0] = rphs[ari]; ari++; 
				out[oi][1] = new NullPhone(); agi++; 
			}
			else //backtrace must be diagonal -- meaning a substitution occurred, or they are identical
			{
				out[oi][0] = rphs[ari]; ari++; //this should be true before ari is incremented : ari == align_stipul[agi]
				out[oi][1] = gphs[agi]; agi++; // same for agi == align_stipul[ari]
			}
		}
		
		return out;
	}

	//auxiliary
	//as formulated here : https://people.cs.pitt.edu/~kirk/cs1501/Pruhs/Spring2006/assignments/editdistance/Levenshtein%20Distance.htm
	//under this definition of Levenshtein Edit Distance,
	// substitution has a cost of 1, the same as a single insertion or as a single deletion 
	public static int levenshteinDistance(LexPhon s, LexPhon t)
	{
		List<SequentialPhonic> sPhons = s.getPhonologicalRepresentation(), 
				tPhons = t.getPhonologicalRepresentation(); 
		int n = sPhons.size(), m = tPhons.size(); 
		
		String[] sPhonStrs = new String[n], tPhonStrs = new String[m];
	
		for(int i = 0; i < n; i++)	sPhonStrs[i] = sPhons.get(i).print(); 
		for(int i = 0; i < m; i++)	tPhonStrs[i] = tPhons.get(i).print(); 
		
		int[][] distMatrix = new int[n][m], costMatrix = new int[n][m]; 
	
		//first we fill it with the base costs
		for(int i = 0; i < n; i++)
		{
			for(int j = 0; j < m; j++)
			{
				if( sPhonStrs[i].equals(tPhonStrs[j]) )	costMatrix[i][j] = 0; 
				else	costMatrix[i][j] = 1; 
			}
		}
		
		//then accumulate the Levenshtein Distance across the graph toward the bottom right
		//arbitrarily, do this top-right then down row by row (could also be done up-dwon then right)
		
		for(int i = 0 ;  i < n ; i++)	distMatrix[i][0] = i ;
		for(int j = 0 ; j < m ; j++)	distMatrix[0][j] = j;
		
		for(int i=1; i < n; i++)
		{
			for(int j = 1; j < m; j++)
			{
				distMatrix[i][j] = Math.min(distMatrix[i-1][j-1]+costMatrix[i-1][j-1],
						1 + Math.min(distMatrix[i-1][j], distMatrix[i][j-1])); 
			}
		}
		
		return distMatrix[n-1][m-1]; 
	}

	private int[] arrLocNMax(double[] arr, int n)
	{
		int[] maxLocs = new int[n];
		int num_filled = 1; //since maxLocs[0] = 0 [the ind] already by default
		while ( num_filled < n && num_filled < arr.length)
		{
			int curr = num_filled; 
			for (int i = 0; i < num_filled ; i++)
			{
				if (arr[maxLocs[i]] < arr[curr])
				{
					while(i < num_filled)
					{
						int temp = maxLocs[i];
						maxLocs[i] = curr;
						curr = temp; 
						i++; 
					}
				}	
			}
			maxLocs[num_filled] = curr; 
			num_filled++; 
		}
		int j = num_filled + 1;
		while (j < arr.length)
		{
			int curr = j;
			for (int i = 0; i < n ; i++)
			{
				if (arr[maxLocs[i]] < arr[curr])
				{
					while (i < maxLocs.length)
					{
						int temp = maxLocs[i]; 
						maxLocs[i] = curr; 
						curr = temp; 
						i++; 
					}
				}
			}
			
			j++; 
		}
		return maxLocs;
	}
	
	//rows -- results, cols -- gold
	private int[][] arr2dLocNMax(int[][] arrArr, int n)
	{
		int[][] maxLocs = new int[n][2]; 
		int num_filled = 1; //since maxLocs[0] = {0,0} already by default

		int currCol = 1, currRow = 0; 
		
		while (num_filled < n)
		{
			if (arrArr[currRow][currCol] > arrArr[maxLocs[num_filled - 1][0]][maxLocs[num_filled-1][1]])
			{
				boolean keep_replacing = true; 
				int i = num_filled - 1; 
				while(keep_replacing)
				{
					maxLocs[i+1] = new int[] {maxLocs[i][0], maxLocs[i][1]}; 
					maxLocs[i] = new int[]{currRow, currCol}; 
					i--;
					if (i < 0)	keep_replacing = false;
					else	keep_replacing = (arrArr[currRow][currCol] > arrArr[maxLocs[i][0]][maxLocs[i][1]]);
				}
				num_filled++;
			}
			currCol++; 
			if (currCol == arrArr[0].length)
			{	currCol = 0; currRow++; 	}
		}
		// now output is filled -- now replacing those with less than cells we still have not yet seen.
		// currRow and currCol are still correct next spots we assume 
		
		while(currRow < arrArr.length)
		{
			if(arrArr[currRow][currCol] > arrArr[maxLocs[n-1][0]][maxLocs[n-1][1]])
			{
				maxLocs[n-1] = new int[]{currRow, currCol}; 
				int i = n - 2; 
				boolean keep_replacing = i >= 0 ; 
				if (keep_replacing)
					keep_replacing = (arrArr[currRow][currCol] > arrArr[maxLocs[i][0]][maxLocs[i][1]]);
				while(keep_replacing)
				{
					maxLocs[i+1] = new int[] {maxLocs[i][0], maxLocs[i][1]}; 
					maxLocs[i] = new int[] {currRow, currCol}; 
					i--;
					if ( i < 0)	keep_replacing = false;
					else	keep_replacing = (arrArr[currRow][currCol] > arrArr[maxLocs[i][0]][maxLocs[i][1]]);
				}
			}
			currCol++; 
			if (currCol == arrArr[0].length)
			{	currCol = 0; currRow++; 	}
		}
		
		return maxLocs; 
	}
	
	/** 
	 * @return all mismatches with a specified string of seqphs in them, @param targSeq
	 * @param look_in_gold determines whether we look for the said string in gold or not (otherwise the result)
	 * to get all mismatches, enter a an empty list for @param targSeq
	 * 
	 */
	public List<LexPhon[]> getCurrMismatches( List<SequentialPhonic> targSeq, boolean look_in_gold)
	{
		if (targSeq.size() == 0)	return mismatches;
		List<LexPhon[]> out = new ArrayList<LexPhon[]>(); 
		int ind = look_in_gold ? 1 : 0; 
		for (LexPhon[] msmtch : mismatches)
			if (Collections.indexOfSubList( msmtch[ind].getPhonologicalRepresentation(),
					targSeq) != -1)
				out.add(new LexPhon[] {msmtch[0], msmtch[1]});
		return out;
	}
	
	public double getAccuracy()
	{	return pctAcc;	}
	
	public double getPctWithin1()
	{	return pctWithin1;	}
	
	public double getPctWithin2()
	{	return pctWithin2;	}
	
	public double getAvgPED()
	{	return avgPED;	}
	
	public double getAvgFED()
	{	return avgFED;	}
	
	private static void writeToFile(String filename, String output)
	{	try 
		{	FileWriter outFile = new FileWriter(filename); 
			BufferedWriter out = new BufferedWriter(outFile); 
			out.write(output);
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
	
	public void makeAnalysisFile(String fileName, boolean use_gold, Lexicon lexic)
	{
		String output = "Analysis for "+(use_gold ? "Gold" : "Result")+"/n";
		
		// System.out.println("Average feature edit distance from gold: "+getAvgFED());
		
		output += "Overall accuracy : "+getAccuracy()+"\n";
		output += "Accuracy within 1 phone: "+getPctWithin1()+"%\n"; 
		output += "Accuracy within 2 phone: "+getPctWithin2()+"%\n";
		output += "Average edit distance per from gold phone: "+getAvgPED()+"\n"; 
		output += "Average feature edit distance from gold: "+getAvgFED()+"\n";
		
		output += "Performance associated with each phone in "+(use_gold ? "Gold" : "Result")+"\n"; 
		output += "Phone in "+(use_gold ? "Gold" : "Result")+"\tAssociated final miss likelihood\t"
				+ "Mean Associated Normalized Lev.Dist.\tMean Associated Feat Edit Dist\n";
		
		double[] errorRateByPhone = use_gold ? errorRateByGoldPhone : errorRateByResPhone; 
		HashMap<String, Integer> phonemeInds = use_gold ? goldPhInds : resPhInds; 
		Phone[] phonInv = use_gold ? goldPhInventory : resPhInventory; 
		boolean[][] phInEt = use_gold ? isPhInGoldEt : isPhInResEt; 
		
		for (Phone ph : phonInv)
		{
			output += ph.print() + "\t|\t";
			int phind = (int)phonemeInds.get(ph.print()); 
			output += errorRateByPhone[phind] + "\t|\t"; 
			
			int ph_ind_str = (int)phonemeInds.get(ph.print());
			
			double n_words_ph_in = 0;
			
			
			double totLevDist = 0.0, totFED = 0.0;
			for (int eti = 0 ; eti < NUM_ETYMA; eti++)
			{
				if(phInEt[ph_ind_str][eti])	
				{
					totLevDist += levDists[eti];
					totFED += feds[eti]; 
					n_words_ph_in += 1.0; 
				}
			}
			
			output += (totLevDist / n_words_ph_in) + "\t|\t"; 
			output += (totFED / n_words_ph_in) + "\n";
		}
		
		writeToFile(fileName, output); 
	}
	
	// determine the scope of the autopsy based on the relation of sequence starts (and ends) to word boundaries
		// we are conditioning this only on the hits because we figure context much more often is a positive determiner
		// of defining context for a shift, rather than a negative determiner.
	private int[] get_autopsy_scope()
	{
		int[] startInRadiusCts = new int[MAX_RADIUS+1];
			// [n] -- count of filter matches that start at index n. for those starting beyond the max radius, we don't care.
		int[] endInRadiusCts= new int[MAX_RADIUS+1];
			// [n] -- count of filter matches ending at endex (-1*n-1) -- if beyond max radius, we don't care.
		
		for (List<int[]> locBounds : SS_HIT_BOUNDS)
		{
			for (int[] bound : locBounds)
			{
				if(bound[0] < MAX_RADIUS)	startInRadiusCts[bound[0]] += 1; 
				if(bound[1] >= -1*MAX_RADIUS)	endInRadiusCts[bound[1]*-1-1] += 1;
			}				
		}
		
		int[] out = new int[] {-1,1};
		int[] cumul = new int[] { startInRadiusCts[0] , endInRadiusCts[0] };
		boolean[] freeze = new boolean[] {false, false};
		while (out[0] >= -1*MAX_RADIUS && out[1] <= MAX_RADIUS && (!freeze[0] || !freeze[1])) 
		{
			if (!freeze[0])
			{	
				cumul[0] += startInRadiusCts[-1 * out[0]];
				freeze[0] = (double)cumul[0] / (double)SS_HIT_IDS.length > 0.32;
				if (!freeze[0])	out[0]--; 
			}
			if (!freeze[1])
			{
				cumul[1] += endInRadiusCts[out[1]];
				freeze[1] = (double) cumul[1] / (double)SS_HIT_IDS.length > 0.32; 	
				if (!freeze[1])	out[1]++;
			}
		}
		
		return out;
	}
	
	//assume indices are constant for the word lists across lexica 
	public void articulateSubsample(String stage_name)
	{	
		IN_SUBSAMP = new boolean[NUM_ETYMA];
		SUBSAMP_SIZE = 0; String etStr = ""; 
		int nSSHits = 0, nSSMisses = 0, nSS1off = 0, nSS2off = 0; 
		double totPED = 0.0 , totFED = 0.0; 
		FILTER = new int[SUBSAMP_SIZE]; 
		mismatches = new ArrayList<LexPhon[]> (); 
		confusionMatrix = new int[resPhInventory.length+1][goldPhInventory.length+1];
		
		errorsByResPhone = new int[resPhInventory.length];
		errorsByGoldPhone = new int[goldPhInventory.length];
		errorsByPivotPhone =  new int[pivotPhInventory.length];
		errorRateByResPhone = new double[resPhInventory.length]; 
		errorRateByGoldPhone = new double[goldPhInventory.length];
		errorRateByPivotPhone = new double[pivotPhInventory.length];
				
		for (int isi = 0; isi < NUM_ETYMA ; isi++)
		{
			if(FOCUS.getByID(isi).toString().equals("[ABSENT]"))
				IN_SUBSAMP[isi] = false;
			else
				IN_SUBSAMP[isi] = filterSeq.filtCheck(FOCUS.getByID(isi).getPhonologicalRepresentation()); 
			if(IN_SUBSAMP[isi])
			{	
				int etld = levDists[isi];
				nSS1off += (etld <= 1) ? 1.0 : 0.0;
				nSS2off += (etld <= 2) ? 1.0 : 0.0;
				SUBSAMP_SIZE += 1; 
				etStr += isi+",";
				if (isHit[isi])	nSSHits+=1; 
				else	
				{
					nSSMisses+=1;
					updateConfusionMatrix(isi);
				}
				totPED += peds[isi];
				totFED += feds[isi]; 
			}
		}
		
		
		FILTER = new int[SUBSAMP_SIZE];
		SS_HIT_IDS = new int[nSSHits];
		SS_MISS_IDS = new int[nSSMisses];
		SS_HIT_BOUNDS = new ArrayList<List<int[]>>(); 
		SS_MISS_BOUNDS = new ArrayList<List<int[]>>(); 
		
		while (etStr.contains(",") && etStr.length()>1)
		{
			int commaloc = etStr.indexOf(",");
			int id = Integer.parseInt(etStr.substring(0, commaloc));
			etStr = etStr.substring(commaloc+1); 
			FILTER[SS_HIT_BOUNDS.size()+SS_MISS_BOUNDS.size()] = id; 
			if (isHit[id])
			{
				SS_HIT_IDS[SS_HIT_BOUNDS.size()] = id;
				SS_HIT_BOUNDS.add(filterSeq.filtMatchBounds(FOCUS.getByID(id).getPhonologicalRepresentation()));
			}
			else
			{
				SS_MISS_IDS[SS_MISS_BOUNDS.size()] = id;
				SS_MISS_BOUNDS.add(filterSeq.filtMatchBounds(FOCUS.getByID(id).getPhonologicalRepresentation()));
			}
		}
		
		String stage_blurb = (stage_name.equals("")) ? "" : " in "+stage_name;
		
		pctAcc = (double)nSSHits / (double)SUBSAMP_SIZE; 
		 
		if (SUBSAMP_SIZE == 0)
			System.out.println("Uh oh -- size of subset is 0.");
		else {
			System.out.println("Size of subset : "+SUBSAMP_SIZE+"; ");
			System.out.println((""+(double)SUBSAMP_SIZE/(double)NUM_ETYMA*100.0).substring(0,5)+"% of whole");
			System.out.println("Accuracy on subset with sequence "+filterSeq.toString()+stage_blurb+" : "+(""+pctAcc*100.0).substring(0,3)+"%");
			System.out.println("Percent of errors included in subset: "+((double)nSSMisses/TOT_ERRS*100.0)+"%");
	
			int[] resPhCts = new int[resPhInventory.length], goldPhCts = new int[goldPhInventory.length],
					pivPhCts = new int[pivotPhInventory.length]; 
			for(int i = 0; i < SUBSAMP_SIZE; i++)
			{
				for (int ri = 0; ri < resPhInventory.length ; ri++)	resPhCts[ri] += isPhInResEt[ri][i] ? 1 : 0;
				for (int gi = 0; gi < goldPhInventory.length; gi++) goldPhCts[gi] += isPhInGoldEt[gi][i] ? 1 : 0;
				for (int pvi = 0; pvi < pivotPhInventory.length; pvi++) pivPhCts[pvi] += isPhInPivEt[pvi][i] ? 1 : 0;
				
			}
			pctWithin1 = nSS1off / (double) SUBSAMP_SIZE;
			pctWithin2 = nSS2off / (double) SUBSAMP_SIZE; 
			avgPED = totPED / (double) SUBSAMP_SIZE; 	
			avgFED = totFED / (double) SUBSAMP_SIZE; 
			for (int i = 0 ; i < resPhInventory.length; i++)
				errorRateByResPhone[i] = (double)errorsByResPhone[i] / (double)resPhCts[i];
			for (int i = 0 ; i < goldPhInventory.length; i++)
				errorRateByGoldPhone[i] = (double)errorsByGoldPhone[i] / (double)goldPhCts[i];
			for (int i = 0 ; i < pivotPhInventory.length; i++)
				errorRateByPivotPhone[i] = (double)errorsByPivotPhone[i] / (double)pivPhCts[i]; 
		}
			
	}
	
	public void contextAutopsy()
	{
		System.out.println("Autopsy -- contexts most associated with error:");
		System.out.println("Features:"); 
		
		List<String[]> prior = new ArrayList<String[]>(); 
		
		int[] scope = get_autopsy_scope(); 
		
		//TODO debugging
		System.out.println("Context autopsy scope is "+scope[0]+","+scope[1]);
		
		int rel_loc = scope[0];
		while (rel_loc < 0)
		{
			prior.add(topNPredictorsForRelInd(4, rel_loc));
			rel_loc++;
		}
		
		rel_loc = 1; 
		List<String[]> postr = new ArrayList<String[]>();
		while(rel_loc <= scope[1])
		{
			postr.add(topNPredictorsForRelInd(4, rel_loc));
			rel_loc++;
		}
		
		System.out.print(feature_autopsy(4, prior,postr)); 
		
	}
	
	// @precondition pri and po should have same and consistent (across high level nestings) length in both dimensions
	public String feature_autopsy(int height, List<String[]> pri, List<String[]> po)
	{
		String out = "\n ";
		
		//header.
		for (int prli = 0 ; prli < pri.size(); prli++)
			out += "   "+(pri.size() - prli)+" before     |";
		out += " FOCUS ";
		for(int poli =0 ; poli<po.size(); poli++)
			out += "|    "+(1+poli)+" after    ";
		out+="\n|"; 
		for(int di = 0; di < pri.size() * 16 + po.size() * 16 + 8; di++)	out+="-";
		out+="|\n"; 
		
		//now main scope. 
		
		for (int i = 0 ; i < height ; i++)
		{
			out += "|"; 
			for (String[] ipri : pri)
			{
				if (ipri[i].equals(""))	out += "               |";
				else	out += " "+append_space_to_x(ipri[i],14) + " |"; 
			}
			out += " XXXXX ";
			for (String[] ipo : po)
			{
				if (ipo[i].equals(""))	out += "|               ";
				else	out += "| "+append_space_to_x(ipo[i],14) + " "; 
			}
			out += "|\n";
		}
		return out+"\n";
	}
	
	private String append_space_to_x (String in, int x)
	{
		if (in.length() >= x)	return in;
		String out = in + " ";
		while (out.length() < x)	out += " ";
		return out;
	}
	
	private boolean containsSPh(SequentialPhonic cand, List<SequentialPhonic> sphlist)
	{
		for (SequentialPhonic sp : sphlist)
			if (cand.equals(sp))	return true;
		return false; 
	}
	
	// @param rel_ind -- index relative to start of the sequence in question we are checking for 
		//-- so if it is 8 and rel_ind is -2, we look at index 6
	// ind 0 -- hit, ind 1 -- miss
	private List<List<SequentialPhonic>> miss_and_hit_phones_at_rel_loc (int rel_ind)
	{
		boolean posterior = rel_ind > 0; 
		List<List<SequentialPhonic>> out = new ArrayList<List<SequentialPhonic>>(); 
		
		List<SequentialPhonic> out0 = new ArrayList<SequentialPhonic>(),
				out1 = new ArrayList<SequentialPhonic>(); 
		
		for (int hi = 0; hi < SS_HIT_IDS.length; hi++)
		{
			List<SequentialPhonic> curPR = FOCUS.getByID(SS_HIT_IDS[hi]).getPhonologicalRepresentation();

			for(int ihi = 0; ihi < SS_HIT_BOUNDS.get(hi).size(); ihi++)
			{
				int curr_ind = posterior ? SS_HIT_BOUNDS.get(hi).get(ihi)[1]+ curPR.size() + rel_ind :
					SS_HIT_BOUNDS.get(hi).get(ihi)[0] + rel_ind;
				if (curr_ind >= 0 && curr_ind < curPR.size())
				{
					SequentialPhonic curr = curPR.get(curr_ind);
					if(!containsSPh(curr,out0))	out0.add(curr); 
				}
			}
		}
		for (int mi = 0 ; mi < SS_MISS_IDS.length; mi++)
		{
			List<SequentialPhonic> curPR = FOCUS.getByID(SS_MISS_IDS[mi]).getPhonologicalRepresentation();

			for(int imi = 0; imi < SS_MISS_BOUNDS.get(mi).size(); imi++)
			{
				int curr_ind = posterior ? SS_MISS_BOUNDS.get(mi).get(imi)[1] + curPR.size() + rel_ind 
						: SS_MISS_BOUNDS.get(mi).get(imi)[0] + rel_ind;
				if(curr_ind >= 0 && curr_ind < curPR.size()) {
					SequentialPhonic curr = curPR.get(curr_ind); 
					if(!containsSPh(curr,out1))	out1.add(curr);
				}
			}
		}
		out.add(out0);
		out.add(out1);
		
		return out;
	}
	
	
	/**
	 * 
	 * @param rel_ind -- index of analysis relative to first phone of confusion/focus
	 * @param ids -- etymon ids 
	 * @param phs -- phs to analyzie (typically optained by miss_and_hit_phones_at_rel_loc(rel_ind)) 
	 * @param theBounds -- bounds of the confusion/focus in those ids. 
	 * @return
	 */
	private int[] get_ph_freqs_at_rel_loc(int rel_ind, int[] ids, List<SequentialPhonic> phs, List<List<int[]>> theBounds)
	{
		boolean posterior = rel_ind > 0; 
		int[] out = new int[phs.size()];
		for (int pi = 0 ; pi < phs.size(); pi++) {
			for (int eti = 0; eti < ids.length ; eti++)
			{
				List<SequentialPhonic> curPR = FOCUS.getByID(ids[eti]).getPhonologicalRepresentation();
				for(int[] bound : theBounds.get(eti))
				{
					int mchi = (posterior ? bound[1] + curPR.size() : bound[0]) + rel_ind ;
					if ( ( posterior ? curPR.size() - 1 - mchi : mchi) >= 0)
						if(curPR.get(mchi).print().equals(phs.get(pi).print()))	out[pi] += 1; 
				}
			}
		}
		return out; 
	}
	
	public String[] topNPredictorsForRelInd(int n, int rel_ind)
	{
		assert SS_MISS_IDS.length > 0 : "Error: tried to predict feats for a sequence subset that has no misses!";
		
		System.out.println("calculating phones at rel loc "+rel_ind+"...");
		
		List<List<SequentialPhonic>> phs_here = miss_and_hit_phones_at_rel_loc(rel_ind); 
		// frequencies of phones among hits and among misses
		int[] miss_ph_frqs = get_ph_freqs_at_rel_loc(rel_ind, SS_MISS_IDS, phs_here.get(1), SS_MISS_BOUNDS); 
		int[] hit_ph_frqs = get_ph_freqs_at_rel_loc(rel_ind, SS_HIT_IDS, phs_here.get(0), SS_HIT_BOUNDS); 
		
		assert hit_ph_frqs.length == phs_here.get(0).size() : "Error : mismatch in size for hit_ph_frqs"; 
		assert miss_ph_frqs.length == phs_here.get(1).size() : "Error : mismatch in size for miss_ph_frqs";
		
		HashMap<String,Integer> predPhIndexer = new HashMap<String,Integer>(); 
		
		String[] candPredictors = new String[featsByIndex.length*2+miss_ph_frqs.length]; 
		
		int[][] cand_freqs = new int[2][featsByIndex.length*2+miss_ph_frqs.length]; 
			//first dimensh -- 0 for hit, 1 for miss
		
		// fill list of all possible feature value predictors. 
		for (int fti = 0 ; fti < featsByIndex.length; fti++)
		{
			candPredictors[2*fti] = "-"+featsByIndex[fti];
			candPredictors[2*fti+1] = "+"+featsByIndex[fti];
		}
		
		// increment counts of predictiveness toward misses here. 
		for (int mpi = 0; mpi < miss_ph_frqs.length; mpi++)
		{
			String phprint =  phs_here.get(1).get(mpi).print(); 
			candPredictors[featsByIndex.length*2 + mpi] = "/"+phprint+"/"; 
			
			cand_freqs[1][featsByIndex.length*2 + mpi] = miss_ph_frqs[mpi];
			
			predPhIndexer.put(phprint, mpi + featsByIndex.length*2); 
		}

		// only to cand_freqs for hits, since we are predicting misses, not hits per se. 
		for (int hpi = 0; hpi < hit_ph_frqs.length; hpi++)
			if (predPhIndexer.containsKey(phs_here.get(0).get(hpi).print()))
				cand_freqs[0][predPhIndexer.get(phs_here.get(0).get(hpi).print())] = hit_ph_frqs[hpi]; 
		
		int unspec = UTILS.UNSPEC_INT;
		
		// get counts for feature value predictors using phone counts. 
		for (int phi = 0; phi < phs_here.get(1).size(); phi++)
		{
			String curprint = phs_here.get(1).get(phi).toString(); 
			if (!curprint.equals("#"))
			{	
				char[] fstr = curprint.split(":")[1].toCharArray();
				
				for (int spi = 0; spi < featsByIndex.length; spi++)
				{	
					int fspi = Integer.parseInt(""+fstr[spi]); 
					if (fspi != unspec)
						cand_freqs[1][2*spi + fspi/2] += miss_ph_frqs[phi];
				}
			}
		}
		
		for(int phi = 0 ; phi < phs_here.get(0).size(); phi++)
		{
			
			String curprint = phs_here.get(0).get(phi).toString();
				
			if(!curprint.equals("#"))
			{	char[] fstr = curprint.split(":")[1].toCharArray();
			
				for (int spi = 0; spi < featsByIndex.length; spi++)
				{
					int fspi = Integer.parseInt(""+fstr[spi]); 
					if (fspi != unspec)
						cand_freqs[0][2*spi + fspi/2] += hit_ph_frqs[phi];
				}
			}
		}
		
		double[] scores = new double[candPredictors.length];
		for(int fi = 0 ; fi < candPredictors.length; fi++)
		{
			if (cand_freqs[1][fi] > 0)
			{
				double c_miss = (double)cand_freqs[1][fi], c_hit = (double)cand_freqs[0][fi]; 
				scores[fi] = ((c_miss + 1.0) / (c_hit + 1.0)) * (c_miss / SS_MISS_IDS.length); 
			}
		}
		
		//choose final output
		int ffi = 0 ; 
		while(ffi < scores.length ? scores[ffi] <= AUTOPSY_DISPLAY_THRESHOLD : false )	
			ffi++; 
		
		String insignif = "<"+AUTOPSY_DISPLAY_THRESHOLD+" thresh";
		
		double[] lb = new double[n]; //"leader board"
		String[] out = new String[n];
		for (int oi = 0; oi < n ; oi++)	out[oi] = insignif; 
		
		
		while (ffi < candPredictors.length)
		{
			double sc= scores[ffi]; 
			if (sc > AUTOPSY_DISPLAY_THRESHOLD)
			{	
				int placer = 0; 
				boolean try_place = true; 
				String scout = candPredictors[ffi] + " : "+sc;
				
				scout = scout.substring(0,Math.min(scout.indexOf('.')+3,scout.length()));
				
				while(try_place)
				{	if (sc < lb[placer])
					{	placer++; 
						try_place = placer < n; 
					}
					else	try_place = false;
				}
				while (placer <  n)
				{
					double to_move = lb[placer];	 String moving_outp = out[placer];
					lb[placer] = sc; 	out[placer] = scout;
		
					if (to_move <= AUTOPSY_DISPLAY_THRESHOLD)	placer = n;
					else
					{
						sc = to_move; 	scout = moving_outp; placer++;
					}
				}
			}
			ffi++; 
		}
		
		return out; 
	}
	
	public boolean isFiltSet()
	{
		return filtSet; 
	}
	
	public boolean isFocSet()
	{
		return focSet;
	}
	
	public void printFourColGraph(Lexicon inpLex, boolean errorsOnly)
	{
		LexPhon[] inpWds = inpLex.getWordList(); 
		for(int i = 0; i < inpWds.length; i++) {
			if (errorsOnly ? IN_SUBSAMP[i] && !isHit[i] : IN_SUBSAMP[i])
			{
				System.out.print(append_space_to_x(i+",",6)+"| ");
				System.out.print(append_space_to_x(inpWds[i].toString(), 19) + "| ");
				if (focSet)
					System.out.print(append_space_to_x(FOCUS.getByID(i).toString(),19)+"| ");
				System.out.print(append_space_to_x(RES.getByID(i).toString(),19)+"| ");
				System.out.print(GOLD.getByID(i)+"\n");
			}	}
	}
	
}
