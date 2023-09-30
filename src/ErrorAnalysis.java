import java.util.HashMap;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays; // used for occasional debugging print statements with arrays, not used otherwise.
import java.util.Collections;

public class ErrorAnalysis {
	
	//TODO summer 2023 run through this class and update for insertion/removal of etyma, but also make sure everything is clear
	// TODO investigate: is the "Infinite" rate report noted in early summer still occurring (I think not, but best to check.)  
	
	//TODO decide on what morphosyntactic analyses to perform
	// TODO implement them... -- probably fall 2023 or winter	
	
	private double PHI_SMOOTHING = 0.5; 
	private double F_SMOOTHING = 0.25;
		// in cases where zero hits exist for a certain location relative to a confusion

	private Lexicon RES, GOLD, PIV_PT_LEX;
	//TODO investigate uses of PIV_PT_LEX -- lexicon at the pivot point. 

	private int[] PRESENT_ETS; 
	// TODO investigate uses.

	private boolean pivotSet, filtSet;
	private SequentialFilter filterSeq; 
	private int[] FILTER; //indices of all etyma in subset
	// TODO investigate uses. 	
	
	private Phone[] resPhInventory, goldPhInventory, pivotPhInventory;
		// the first two are largely used for indexing purposes for search and comparison between different phonne(me)s.
	private HashMap<String, Integer> resPhInds, goldPhInds, pivPhInds;
		// indexes for phones in the following int arrays are above.
	private boolean[][] isPhInResEt, isPhInGoldEt, isPhInPivEt; 
		// TODO investigate uses of these. 
	private List<Etymon[]> mismatches; 
		// TODO investigate uses. 

	private int TOTAL_ETYMA, EVAL_SAMP_SIZE;
	private double TOT_ERRS;	
	private boolean[] IN_SUBSAMP; //TODO investigate uses of this one.
	private boolean[] isHit; 	//TODO investigate uses 

	private FED featDist;
	private int[] levDists; 
		// levenshtein edit distance between each reconstructed/observed word pair. 
	private double[] peds, feds; 
		// phonological edit distance and feature edit distance between each reconstructed/observed word pair
	private String[] featsByIndex; 
		// this is distinctive/phonological features -- not the morphosyntactic ones. 
	private List<String> inactiveFeats;
		// features that never vary. To be detected upon construction.
	private List<String> pivotInactiveFeats; 
		// inactive features for subsample at pivot point. Might be a slightly smaller set, for one reason or another. Defined when pivot is made. 
	
	private double pctAcc, pctWithin1, pctWithin2, avgPED, avgFED; 
	private List<List<int[]>> SS_HIT_BOUNDS, SS_MISS_BOUNDS;
	// lists of boundaries (onset, offset) of a filter sequence in words belonging to the two filtered sets described above. 
		// necessary because some filters include parenthesized segments, giving them variable length within a word
		// these are used for various auxiliary functions as well in some places of this class. 
	private int[] SS_HIT_IDS, SS_MISS_IDS; 
		// etymon IDs of words that validly fit are filter and are, respectively, matches and mismatches between reconstructed and observed outcomes.
	
	
	//protected final String ABS_PR ="[ABSENT]"; 
		//TODO note this variable is the locus of protodelta changes
		// now handled via UTILS.ABSENT_INDIC ; consider restoring if necessary. 
	protected final int MAX_RADIUS = 3;
	private final int NUM_TOP_ERR_PHS_TO_DISP = 4; 
	public final double AUTOPSY_DISPLAY_THRESHOLD = 0.1;

	//TODO investigate calculation of each of these for if errors will arise from usage of
		// absent and unattested etyma ...
	protected int[] errorsByResPhone, errorsByGoldPhone; 
	protected int[] errorsByPivotPhone;
	private double[] errorRateByResPhone, errorRateByGoldPhone; 
	private double[] errorRateByPivotPhone;
		// note that this rate is actually calculated in terms of 
			//# of times the phone occurs in mismatch (observed != reconstructed) words 
				// with MULTIPLE OCCURRENCES counted multiply
				// divided by number of WORDS the phone occurs in total
			// so mismatches with MULTIPLE OCCURRENCES of the same phone will inflate the rate slightly
				// though the effect of this is overall not likely to be significant.
			// and it is likely better than the basic alternative, which would fail to take into account
				// multiple errors in the same phone in the same word. 
			// an ideal case would involve an alignment algorithm to insure that cases where only one 
					// occurrence of the phone in said word is errant are not being counted multiply
			// but this is not a priority right now as there are more important things to improve upon given the work necessary to involve that.
	private int[][] confusionMatrix; 
		// rows -- indexed by resPhInds; columns -- indexed by goldPhInds
	
	/**
	 * @param theRes --  result lexicon, lexicon that is the result of forward reconstruction. 
	 * 		May have etyma that are absent at this stage
	 * 			but none represented as unattested (because that would not apply) 
	 * @param theGold -- lexicon of observed etyma to compare against. 
	 * 		May have etyma that are absent at this stage,
	 * 			or those that are unattested
	 * @param indexedFeats -- phonological/distinctive features with their indices as held constant throughout the simulation. 
	 * @param fedCalc -- Feature Edit Distance calculator object.
	 * TODO need to make sure this is called BEFORE Lexicon.updateAbsence occurs, so that just-inserted etyma do not inflate accuracy. 
	 * TODO need to insure that INSERTED etyma are not contributing to calculations!! 
	 */
	public ErrorAnalysis(Lexicon theRes, Lexicon theGold, String[] indexedFeats, FED fedCalc)
	{
		RES = theRes;
		GOLD = theGold; 
		PIV_PT_LEX = null; //must be manually set later, e.g. setPivot() 
		filtSet = false;
		pivotSet = false; // set with setFilter() later. 
		
		featDist = fedCalc; 
		featsByIndex = indexedFeats;
		TOTAL_ETYMA = theRes.totalEtyma(); 
		// total etyma, present or not at this moment 

		if (TOTAL_ETYMA != theGold.totalEtyma()) // guard rail. 
			throw new RuntimeException("Alert: tried to do error analysis between lexica of different sizes "
					+ "(result: "+TOTAL_ETYMA+", vs. gold: "+theGold.totalEtyma()+"). "
							+ "-Absent and unattested etyma should be stored as PseudoEtymon objects, "
							+ "given the paramount of importance of keeping etymon indices constant. "
							+ "Investigate this."); 
		
		resPhInventory = theRes.getPhonemicInventory();
		goldPhInventory = theGold.getPhonemicInventory();
		
		// unlike the *etymon* indices these indices here are not (and cannot) be held equivalent to each other 
			// -- that would be too brittle. 
		resPhInds = new HashMap<String, Integer>(); 
		goldPhInds = new HashMap<String, Integer>();
		
		for(int i = 0 ; i < resPhInventory.length; i++)
			resPhInds.put(resPhInventory[i].print(), i);
		for (int i = 0 ; i < goldPhInventory.length; i++)
			goldPhInds.put(goldPhInventory[i].print(), i);
				
		TOTAL_ETYMA = theRes.getWordList().length;
		EVAL_SAMP_SIZE = TOTAL_ETYMA - theRes.numAbsentEtyma();
		
		FILTER = new int[EVAL_SAMP_SIZE];
		PRESENT_ETS = new int[EVAL_SAMP_SIZE];
		int fi = 0;
		for (int i = 0 ; i < TOTAL_ETYMA; i++)
		{	if (!theRes.getByID(i).print().equals(UTILS.ABSENT_REPR))
			{	FILTER[fi] = i;
				PRESENT_ETS[fi] = i;
				fi++;
			}
		}
		
		isPhInResEt = new boolean[resPhInventory.length][TOTAL_ETYMA]; 
		isPhInGoldEt = new boolean[goldPhInventory.length][TOTAL_ETYMA]; 
		
		errorsByResPhone = new int[resPhInventory.length];
		errorsByGoldPhone = new int[goldPhInventory.length];

		errorRateByResPhone = new double[resPhInventory.length]; 
		errorRateByGoldPhone = new double[goldPhInventory.length];

		confusionMatrix = new int[resPhInventory.length + 1][goldPhInventory.length + 1];
		// final indices in both dimensions are for the null phone
		
		mismatches = new ArrayList<Etymon[]>();
		
		levDists = new int[TOTAL_ETYMA]; 
		peds = new double[TOTAL_ETYMA];
		feds = new double[TOTAL_ETYMA];
		isHit = new boolean[TOTAL_ETYMA];
		double totLexQuotients = 0.0, numHits = 0.0, num1off=0.0, num2off=0.0, totFED = 0.0; 
				
		IN_SUBSAMP = new boolean[TOTAL_ETYMA]; 		
		
		for (int i = 0 ; i < TOTAL_ETYMA ; i++)
		{	
			IN_SUBSAMP[i] = true; 		// until filter is set, all words are "in the subsample"

			for(int rphi = 0 ; rphi < resPhInventory.length; rphi++)
			{
				Etymon currEt = theRes.getByID(i);
				isPhInResEt[rphi][i] = (currEt.toString().equals(UTILS.ABSENT_REPR)) ? 
						false : (currEt.findPhone(resPhInventory[rphi]) != -1);
			}
			for (int gphi = 0 ; gphi < goldPhInventory.length; gphi++)
			{
				Etymon currEt = theGold.getByID(i);
				isPhInGoldEt[gphi][i] = (currEt.toString().equals(UTILS.ABSENT_REPR)) ?
						false : (currEt.findPhone(goldPhInventory[gphi]) != -1);
			}
			
			if (!theRes.getByID(i).print().equals(UTILS.ABSENT_REPR) && !theGold.getByID(i).print().equals(UTILS.ABSENT_REPR))
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
		pctAcc = numHits / (double) EVAL_SAMP_SIZE; 
		pctWithin1 = num1off / (double) EVAL_SAMP_SIZE;
		pctWithin2 = num2off / (double) EVAL_SAMP_SIZE; 
		avgPED = totLexQuotients / (double) EVAL_SAMP_SIZE; 	
		avgFED = totFED / (double) EVAL_SAMP_SIZE; 
		TOT_ERRS = (double)TOTAL_ETYMA - numHits;
		
		//calculate error rates by phone for each of result and gold sets
		HashMap<String, Integer> resPhCts = theRes.getPhonemeCounts(), 
				goldPhCts = theGold.getPhonemeCounts(); 
		
		// TODO source of infinity error may be here. 
		for (int i = 0 ; i < resPhInventory.length; i++)
			errorRateByResPhone[i] = (double)errorsByResPhone[i] 
					/ (double)resPhCts.get(resPhInventory[i].print());
		for (int i = 0 ; i < goldPhInventory.length; i++)
			errorRateByGoldPhone[i] = (double)errorsByGoldPhone[i]
					/ (double)goldPhCts.get(goldPhInventory[i].print()); 
		
		inactiveFeats = new ArrayList<String>(); 
		for (String fbi : featsByIndex)
		{	inactiveFeats.add(UTILS.MARK_POS+fbi); inactiveFeats.add(UTILS.MARK_NEG+fbi);	}
		inactiveFeats = rmvFeatsActiveInSample(inactiveFeats,RES); 
		inactiveFeats = rmvFeatsActiveInSample(inactiveFeats,GOLD);
		
		//TODO Debugging
		// System.out.println("inactive feats now at : "+inactiveFeats.size()); 
		// for (String ifi : inactiveFeats)	System.out.println(ifi); 
	}

	public void toDefaultFilter()
	{
		EVAL_SAMP_SIZE = PRESENT_ETS.length;
		FILTER = new int[EVAL_SAMP_SIZE];
		for (int i = 0 ; i < EVAL_SAMP_SIZE; i++)	FILTER[i] = PRESENT_ETS[i];
		filterSeq = null;
	}
	
	public void setFilter(SequentialFilter newFilt, String filt_name)
	{
		filterSeq = newFilt; 
		filtSet = true;
		if(pivotSet)	articulateSubsample(filt_name); 
	}
	
	public void setPivot(Lexicon newPiv, String piv_name)
	{
		PIV_PT_LEX = newPiv; 
		pivotPhInventory = newPiv.getPhonemicInventory();
		
		pivPhInds = new HashMap<String, Integer>(); 
		
		for(int i = 0 ; i < pivotPhInventory.length; i++)
			pivPhInds.put(pivotPhInventory[i].print(), i);
		
		pivotSet = true; 
		
		isPhInPivEt = new boolean[pivotPhInventory.length][TOTAL_ETYMA]; 
		int[] pivPhCts = new int[pivotPhInventory.length]; 
		for (int ei = 0 ; ei < TOTAL_ETYMA ; ei++)
		{
			Etymon currEt = PIV_PT_LEX.getByID(ei);
			for(int pvi = 0 ; pvi < pivotPhInventory.length; pvi++)
			{
				if(!currEt.toString().equals(UTILS.ABSENT_REPR))
					isPhInPivEt[pvi][ei] = (currEt.findPhone(pivotPhInventory[pvi]) != -1);
				else	isPhInPivEt[pvi][ei] = false;
				if(isPhInPivEt[pvi][ei])	pivPhCts[pvi] += 1; 
			}
		}
		
		if(filtSet)	articulateSubsample(piv_name); 
		else
		{
			errorsByPivotPhone =  new int[pivotPhInventory.length];
			errorRateByPivotPhone = new double[pivotPhInventory.length]; //to avoid errors. 
			for (int ei = 0 ; ei < TOTAL_ETYMA ; ei++)	
			{
				if(!isHit[ei])
					for (SequentialPhonic pivPh : PIV_PT_LEX.getByID(ei).getPhOnlySeq())
						errorsByPivotPhone[pivPhInds.get(pivPh.print())] += 1; 
			}
			for (int i = 0 ; i < pivotPhInventory.length; i++)
				errorRateByPivotPhone[i] = (double)errorsByPivotPhone[i] / (double)pivPhCts[i]; 
			pivotInactiveFeats = rmvFeatsActiveInSample(inactiveFeats, PIV_PT_LEX); 
		}
	}
	
	//@param get_contexts -- determine if we want to list the most problematic context info
	public void confusionDiagnosis(boolean get_contexts)
	{
		// top n error rates for res and gold
		int[] topErrResPhLocs = arrLocNMax(errorRateByResPhone, NUM_TOP_ERR_PHS_TO_DISP); 
		int[] topErrGoldPhLocs = arrLocNMax(errorRateByGoldPhone, NUM_TOP_ERR_PHS_TO_DISP); 
		int[] topErrPivotPhLocs = new int[NUM_TOP_ERR_PHS_TO_DISP];
		if (pivotSet)	topErrPivotPhLocs = arrLocNMax(errorRateByPivotPhone, NUM_TOP_ERR_PHS_TO_DISP); 
		
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
			if(pivotSet)
			{
				System.out.println("Pivot point phones most associated with error: ");
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
		Etymon res = RES.getByID(err_id), gold = GOLD.getByID(err_id); 
		
		mismatches.add( new Etymon[] {res, gold}) ; 
				
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
		if (pivotSet)
			for (SequentialPhonic pivPh : PIV_PT_LEX.getByID(err_id).getPhOnlySeq())
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

		List<Etymon[]> pairsWithConfusion = mismatchesWithConfusion(resPhInd, goldPhInd); 
		
		//NOTE: By default this is done for gold. may need to change that.
		
		int[] prePriorCounts = new int[goldPhInventory.length + 1]; //+1 for word bound #" 
		int[] postPostrCounts = new int[goldPhInventory.length + 1]; 
		int[] priorPhoneCounts = new int[goldPhInventory.length + 1]; 
		int[] posteriorPhoneCounts = new int[goldPhInventory.length + 1]; 
				
		int total_confusion_instances = 0; 

		for (int i = 0 ; i < pairsWithConfusion.size(); i++)
		{
			
			//TODO need to fix error here. 
				//TODO what was the error? 
			
			Etymon[] curPair = pairsWithConfusion.get(i); 
			
			featDist.compute(curPair[0],curPair[1]); 
			int[][] alignment = featDist.get_min_alignment(); 
				// for each outer index (i.e. the first [])...
					// find at ind 0 (in second []) : the position aligned to res phone at outer ind, 
					// and at ind 1 : position aligned to gold phone at outer ind
				// if = -1 -- aligned to null phone
				// if = -2 -- aligned to null phone at word boundary
				// "inner" index -- the number in the sequence of aligned pairs 
					// (including pairs with a null phone as an element on for either the result or the gold) 
			
			List<Integer> confuseLocs = new ArrayList<Integer>();
			//will be based on location in the gold form, unless it is a res phone aligned to gold null,
				// in which case it will be in the res form. 

			SequentialPhonic[] goldPhs = curPair[1].getPhOnlySeq(), resPhs= curPair[0].getPhOnlySeq();
			boolean nullPhForGold = (goldPhInd == goldPhInventory.length),
					nullPhForRes = (resPhInd == resPhInventory.length); 
			
			if (nullPhForGold && nullPhForRes)
				throw new RuntimeException("Error: somehow we ended up calculating the confusion between \n"
						+ "a gold null phone (∅) and a result null phone... this should never happen.\n"
						+ " Investigate this.");
		
			if (nullPhForGold) 			{
				
				for(int rpi = 0 ; rpi == resPhs.length ? false : alignment[rpi][0] != -2 ; rpi++)
					if(resPhs[rpi].print().equals(resPhInventory[resPhInd].print()))
						if(alignment[rpi][0] < 0)
							confuseLocs.add(rpi); 
			}
			else if (nullPhForRes)
			{
				for (int gpi = 0 ; gpi == goldPhs.length ? false : alignment[gpi][1] != -2 ; gpi++)
					if (goldPhs[gpi].print().equals(goldPhInventory[goldPhInd].print()))
						if(alignment[gpi][1] < 0)
							confuseLocs.add(gpi); 
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
				int opLocBefore = getPrevAlignedGoldPos(alignment, dloc, nullPhForGold); 
				
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

				int opLocAfter = getNextAlignedGoldPos(alignment, dloc, nullPhForGold, goldPhs.length) ;

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
	private List<Etymon[]> mismatchesWithConfusion (int resPhInd, int goldPhInd)
	{
		List<Etymon[]> out = new ArrayList<Etymon[]>(); 
		boolean is_insert_or_delete = (resPhInd == resPhInventory.length) ||  (goldPhInd == goldPhInventory.length); 
		for (Etymon[] mismatch : mismatches)
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
	private boolean hasMismatch(int rphi, int gphi, Etymon rlex, Etymon glex)
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
		//TODO (Dec 3 2022) in what capacity was this done? 
	private SequentialPhonic[][] getAlignedForms(Etymon r, Etymon g)
	{
		featDist.compute(r,g); //TODO may need to change insertion/deletion weight here!
		int[][] trace = featDist.get_last_backtrace();
		        // goes "forward" from onset to coda -- unlike direction of backtrace!!

		SequentialPhonic[] rphs = r.getPhOnlySeq(), gphs = g.getPhOnlySeq();

		int rlen = rphs.length , glen = gphs.length;

		SequentialPhonic[][] out = new SequentialPhonic[trace.length][2];

		int ari = trace[0][0], agi = trace[0][1];

		if (ari != 0 || agi != 0 )
			throw new RuntimeException("ERROR: ari and agi should both start as 0 but ari is "+ari+" and agi is "+agi);

		for (int tri = 1; tri <= trace.length ; tri++)
		{	
			//tri == trace.length for the special case of the last step.
			int rtri = (tri == trace.length) ? rlen : trace[tri][0], 
					gtri = (tri == trace.length) ? glen : trace[tri][1];
			
			assert rtri != ari || gtri != agi : "Two null phones matched to each other at trace index "+tri;
			if (rtri == ari)	out[tri-1][0] = new NullPhone();
			else {
			    assert rtri == ari + 1 : "error in trace: rtri = "+rtri+" coming after ari = "+ari;
			    out[tri-1][0] = rphs[ari]; ari++;
			}
			if (gtri == agi)	out[tri-1][1] = new NullPhone(); 
			else {
				assert gtri == agi + 1 : "error in trace : gtri = "+gtri+" coming after agi = "+agi;
				out[tri-1][1] = gphs[agi] ; agi++; 
			}
		}

		//handle the last step. 
		
		if (ari != rlen)
			throw new RuntimeException("Error: failed to reach res end at "+rlen+" of rphs (at ari = "+ari+")");
		if (agi != glen) 
			throw new RuntimeException("Error: failed to reach gold end at "+glen+" of gphs (at agi = "+agi+")");

		return out;
	}
	/** obselete version of above class -- unnecessary and excessive. 
	private SequentialPhonic[][] getAlignedForms(LexPhon r, LexPhon g)
	{
		//TODO debugging
		System.out.println("r: "+r+"; g "+g);
		
		featDist.compute(r, g); //TODO may need to change insertion/deletion weight here!
		int[][] align_stipul = featDist.get_min_alignment(); //TODO check this..
			// nested index [0] -- location (or non-location for -1, -2)
		
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
		
		//comments conceptualize the alignment relationship as a "transformation of the result to the gold" 
		
		for(int oi = 0 ; oi < al_len; oi++)
		{
			//TODO debugging
			System.out.println("ari "+ari+"; agi "+agi+"; oi "+oi);
			
			if (align_stipul[ari][0] == -1) // deletion of phone at place <ari> in result
			{
				out[oi][0] = rphs[ari]; ari++;
				out[oi][1] = new NullPhone(); 
			}
			else if (align_stipul[ari][0] == -2) //deletion of result phone next to word boundary
			{
				out[oi][0] = new NullPhone(); ari++; 
				out[oi][1] = gphs[agi]; agi++; 
			}
			else if (align_stipul[agi][1] == -1) //insertion of phone at place <agi> in gold
			{
				out[oi][0] = new NullPhone(); 
				out[oi][1] = gphs[agi]; agi++;
			}
			else if (align_stipul[agi][1] == -2) // insertion at boundary for gold 
			{
				out[oi][0] = rphs[ari]; ari++; 
				out[oi][1] = new NullPhone(); agi++; 
			}
			else //this means backtrace must be diagonal -- meaning a substitution occurred, or they are identical
			{
				out[oi][0] = rphs[ari]; ari++; //this should be true before ari is incremented : ari == align_stipul[agi]
				out[oi][1] = gphs[agi]; agi++; // same for agi == align_stipul[ari]
			}
		}
		
		return out;
	}*/ 

	//auxiliary
	//as formulated here : https://people.cs.pitt.edu/~kirk/cs1501/Pruhs/Spring2006/assignments/editdistance/Levenshtein%20Distance.htm
	//under this definition of Levenshtein Edit Distance,
	// substitution has a cost of 1, the same as a single insertion or as a single deletion 
		// TODO ^ is the above still true as of September 2023? Check. 
	public static int levenshteinDistance(Etymon s, Etymon t)
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
	public List<Etymon[]> getCurrMismatches( List<SequentialPhonic> targSeq, boolean look_in_gold)
	{
		if (targSeq.size() == 0)	return mismatches;
		List<Etymon[]> out = new ArrayList<Etymon[]>(); 
		int ind = look_in_gold ? 1 : 0; 
		for (Etymon[] msmtch : mismatches)
			if (Collections.indexOfSubList( msmtch[ind].getPhonologicalRepresentation(),
					targSeq) != -1)
				out.add(new Etymon[] {msmtch[0], msmtch[1]});
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
			for (int eti = 0 ; eti < TOTAL_ETYMA; eti++)
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
					// recall that bound[1] is going to be a negative number -- counting back from offset of word, 
						//per how filtMatchBounds are constructed in SequentialFilter.
			}				
		}
		for (List<int[]> locBounds : SS_MISS_BOUNDS)
		{	for (int[] bound : locBounds)
			{
				if(bound[0] < MAX_RADIUS)	startInRadiusCts[bound[0]] += 1; 
				if(bound[1] >= -1*MAX_RADIUS)	endInRadiusCts[bound[1]*-1-1] += 1;
			}}
		
		int denominator = SS_HIT_IDS.length + SS_MISS_IDS.length; 
		int[] out = new int[] {-1,1};
		int[] cumul = new int[] { startInRadiusCts[0] , endInRadiusCts[0] };
		boolean[] freeze = new boolean[] {false, false};
		while (out[0] >= -1*MAX_RADIUS && out[1] <= MAX_RADIUS && (!freeze[0] || !freeze[1])) 
		{
			if (!freeze[0])
			{	
				cumul[0] += startInRadiusCts[-1 * out[0]];
				freeze[0] = (double)cumul[0] / (double)denominator > 0.32;
				if (!freeze[0])	out[0]--; 
			}
			if (!freeze[1])
			{
				cumul[1] += endInRadiusCts[out[1]];
				freeze[1] = (double) cumul[1] / (double)denominator > 0.32; 	
				if (!freeze[1])	out[1]++;
			}
		}
		
		return out;
	}
	
	//assume indices are constant for the word lists across lexica 
	// TODO there is an error around here that is causing filters with word bounds in them to return false
	// but this only seems to happen in the context of filter sequences from the halt menu 
	// though not for the (same??) method called in SequentialFilter for sound changes -- why might this be? 
	// 		relevant method if the error isn't here -- SequentialFilter.filtCheck 
	//	but -- they ARE detected when the filter consists of only a # 
	// likewise it is detected for "# n"
	// it seems to be "n #" that is disfavored. 
	public void articulateSubsample(String subsamp_name)
	{	
		IN_SUBSAMP = new boolean[TOTAL_ETYMA];
		EVAL_SAMP_SIZE = 0; String etStr = ""; 
		int nSSHits = 0, nSSMisses = 0, nSS1off = 0, nSS2off = 0; 
		double totPED = 0.0 , totFED = 0.0; 
		FILTER = new int[EVAL_SAMP_SIZE]; 
		mismatches = new ArrayList<Etymon[]> (); 
		confusionMatrix = new int[resPhInventory.length+1][goldPhInventory.length+1];
		
		errorsByResPhone = new int[resPhInventory.length];
		errorsByGoldPhone = new int[goldPhInventory.length];
		errorsByPivotPhone =  new int[pivotPhInventory.length];
		errorRateByResPhone = new double[resPhInventory.length]; 
		errorRateByGoldPhone = new double[goldPhInventory.length];
		errorRateByPivotPhone = new double[pivotPhInventory.length];
				
		//determining what etyma are in the subsample
		for (int isi = 0; isi < TOTAL_ETYMA ; isi++)
		{
			if(PIV_PT_LEX.getByID(isi).toString().equals(UTILS.ABSENT_REPR))
				IN_SUBSAMP[isi] = false;	//ignore etyma absent at this time;.
			else
				IN_SUBSAMP[isi] = filterSeq.filtCheck(PIV_PT_LEX.getByID(isi).getPhonologicalRepresentation()); 
			if(IN_SUBSAMP[isi])
			{	
				int etld = levDists[isi];
				nSS1off += (etld <= 1) ? 1.0 : 0.0;
				nSS2off += (etld <= 2) ? 1.0 : 0.0;
				EVAL_SAMP_SIZE += 1; 
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
		
		
		FILTER = new int[EVAL_SAMP_SIZE];
		SS_HIT_IDS = new int[nSSHits];
		SS_MISS_IDS = new int[nSSMisses];
		SS_HIT_BOUNDS = new ArrayList<List<int[]>>(); 
		SS_MISS_BOUNDS = new ArrayList<List<int[]>>(); 
			//the -_BOUNDS variables are serving an additional indexing role for building FILTER here
		
		while (etStr.contains(",") && etStr.length()>1)
		{
			int commaloc = etStr.indexOf(",");
			int id = Integer.parseInt(etStr.substring(0, commaloc));
			etStr = etStr.substring(commaloc+1); 
			FILTER[SS_HIT_BOUNDS.size()+SS_MISS_BOUNDS.size()] = id; 
			if (isHit[id])
			{
				SS_HIT_IDS[SS_HIT_BOUNDS.size()] = id;
				SS_HIT_BOUNDS.add(filterSeq.filtMatchBounds(PIV_PT_LEX.getByID(id).getPhonologicalRepresentation()));
			}
			else
			{
				SS_MISS_IDS[SS_MISS_BOUNDS.size()] = id;
				SS_MISS_BOUNDS.add(filterSeq.filtMatchBounds(PIV_PT_LEX.getByID(id).getPhonologicalRepresentation()));
			}
		}
		
		SS_HIT_BOUNDS = processSeqFiltBounds(SS_HIT_BOUNDS); SS_MISS_BOUNDS = processSeqFiltBounds(SS_MISS_BOUNDS);
		
		String subsamp_blurb = (subsamp_name.equals("")) ? "" : " in "+subsamp_name;
				 
		if (EVAL_SAMP_SIZE == 0)
			System.out.println("Uh oh -- size of subset is 0.");
		else {
			pctAcc = (double)nSSHits / (double)EVAL_SAMP_SIZE; 
			
			System.out.println("Size of subset : "+EVAL_SAMP_SIZE+"; ");
			System.out.println((""+(double)EVAL_SAMP_SIZE/(double)TOTAL_ETYMA*100.0).substring(0,5)+"% of etyma in whole dataset.");
			System.out.println("Accuracy on subset with sequence "+filterSeq.toString()+subsamp_blurb+" : "+(""+pctAcc*100.0).substring(0,3)+"%");
			System.out.println("Percent of errors included in subset: "+((double)nSSMisses/TOT_ERRS*100.0)+"%");
	
			int[] resPhCts = new int[resPhInventory.length], goldPhCts = new int[goldPhInventory.length],
					pivPhCts = new int[pivotPhInventory.length]; 
			
			for (int fi : FILTER)
			{
				//TODO need to check this area in protodelta to ensure handling of both absent and unattested etyma correctly 
					// -- so that they are excluded from calculations
				for (int ri = 0; ri < resPhInventory.length ; ri++)	resPhCts[ri] += isPhInResEt[ri][fi] ? 1 : 0;
				for (int gi = 0; gi < goldPhInventory.length; gi++) goldPhCts[gi] += isPhInGoldEt[gi][fi] ? 1 : 0;
				for (int pvi = 0; pvi < pivotPhInventory.length; pvi++) pivPhCts[pvi] += isPhInPivEt[pvi][fi] ? 1 : 0;
			}
			
			pctWithin1 = nSS1off / (double) EVAL_SAMP_SIZE;
			pctWithin2 = nSS2off / (double) EVAL_SAMP_SIZE; 
			avgPED = totPED / (double) EVAL_SAMP_SIZE; 	
			avgFED = totFED / (double) EVAL_SAMP_SIZE; 
			
			for (int i = 0 ; i < resPhInventory.length; i++)
				errorRateByResPhone[i] = (double)errorsByResPhone[i] / (double)resPhCts[i];
			for (int i = 0 ; i < goldPhInventory.length; i++)
				errorRateByGoldPhone[i] = (double)errorsByGoldPhone[i] / (double)goldPhCts[i];
			for (int i = 0 ; i < pivotPhInventory.length; i++)
				errorRateByPivotPhone[i] = (double)errorsByPivotPhone[i] / (double)pivPhCts[i]; 
		}
		
		pivotInactiveFeats = rmvFeatsActiveInSample(inactiveFeats,PIV_PT_LEX);		
	}
	
	
	public void printSubSample()
	{
		System.out.println(UTILS.fillSpaceToN("Pivot pt form",12) +"|" 
				+UTILS.fillSpaceToN("CFR result",12)+"|"
				+UTILS.fillSpaceToN("gold result", 12));
		for (int ei = 0 ; ei < TOTAL_ETYMA; ei++)
			if (IN_SUBSAMP[ei])
				System.out.println(
						UTILS.fillSpaceToN(PIV_PT_LEX.getByID(ei).print(),12) + "|"
						+ UTILS.fillSpaceToN(RES.getByID(ei).print(),12) + "|"
						+ UTILS.fillSpaceToN(GOLD.getByID(ei).print(), 12)); 
	}
	
	/** currently used for development/debugging of context autopsy
	 * 	running four context autopsies next to each other
	 * 		, each with a different correlation metric: 
	 * 		1) phi
	 * 		2) f1
	 * 		3) f3
	 * 		4) f0.2 
	 * 
	 * TODO future behavior: should ask user which sort of test to do, perhaps? 
	 */
	public void contextAutopsyComparison()
	{
		//useful for debugging, below. 
		//System.out.println("Items in subsample: "); 
		//printSubSample(); 
		
		contextAutopsy("phi");
		contextAutopsy("f"); 
		contextAutopsy("f3");
		contextAutopsy("f0.2"); 
	}
	
	/**
	 * get printout elucidating which contexts (in terms of phones, features, bounds)
	 * 		are most correlated to error
	 * @param metric_name -- should be either phi, f, or f followed by a number >= 0
	 * 		f = F1 score
	 * 		f# = fBeta score, with the number being beta
	 */
	public void contextAutopsy(String metric_name)
	{
		System.out.println("Autopsy -- contexts most correlated with error (metric: "+metric_name+"): ");
		
		List<String[]> prior = new ArrayList<String[]>(); 
		
		int[] scope = get_autopsy_scope(); 
		int rel_loc = scope[0];
		while (rel_loc < 0)
		{
			prior.add(topNScoredPredictorsAtRelInd(4, rel_loc, metric_name));
			rel_loc++;
		}
		
		rel_loc = 1; 
		List<String[]> postr = new ArrayList<String[]>();
		while(rel_loc <= scope[1])
		{
			postr.add(topNScoredPredictorsAtRelInd(4, rel_loc, metric_name));
			rel_loc++;
		}
		
		System.out.print(autopsy_print(4, prior,postr)); 
		
	}
	
	// @precondition pri and po should have same and consistent (across high level nestings) length in both dimensions
	// generates the printout for the context autopsy functionality 
	//	each String[] in pri and po should be |height+2| in length. 
	// on bottom two lines: 
	// 		if the top [height] items contain neither a phone nor the word boundary
	//			the last two lines are the top phone and the word boundary	
	//			in order of their frequency. 
	//		if it includes one or both, the [height + 2]^th most common predictor is listed last
	//			if it includes both, the [height + 1]^th predictor is listed second to last. 
	//  
	public String autopsy_print(int height, List<String[]> pri, List<String[]> po)
	{
		String out = "\n ";
		
		//header.
		for (int prli = 0 ; prli < pri.size(); prli++)
			out += "   "+(pri.size() - prli)+" before     |";
		out += " PIVOT ";
		for(int poli =0 ; poli<po.size(); poli++)
			out += "|    "+(1+poli)+" after    ";
		out+="\n|"; 
		for(int di = 0; di < pri.size() * 16 + po.size() * 16 + 8; di++)	out+="-";
		out+="|\n"; 
		
		//now main scope. 
		//iteration over rows
		for (int i = 0 ; i < height + 2 ; i++)
		{
			// delimiter between top N and the "footer" 
			if (i == height)	
				out += append_space_to_x("...",pri.size()*17)+"| XXXXX |"
					  +append_space_to_x("   ...",po.size()*17-4)+"...|\n";

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
	// TODO currently this does not incorporate info on the *frequencies* of hit and miss phones
	//  this is instead accessed after this is called, via get_ph_freqs_at_rel_loc 
	// if we want to make this return frequencies, would need to make it return a HashMap instead... 
	private List<List<SequentialPhonic>> hit_and_miss_phones_at_rel_loc (int rel_ind)
	{
		boolean posterior = rel_ind > 0; 
		List<List<SequentialPhonic>> out = new ArrayList<List<SequentialPhonic>>(); 
		
		List<SequentialPhonic> ph_hits = new ArrayList<SequentialPhonic>(),
				ph_misses = new ArrayList<SequentialPhonic>(); 
		
		for (int hi = 0; hi < SS_HIT_IDS.length; hi++)
		{
			List<SequentialPhonic> curPR = PIV_PT_LEX.getByID(SS_HIT_IDS[hi]).getPhonologicalRepresentation();

			for(int ihi = 0; ihi < SS_HIT_BOUNDS.get(hi).size(); ihi++)
			{
				int curr_ind = posterior ? 
						SS_HIT_BOUNDS.get(hi).get(ihi)[1]+ curPR.size() + rel_ind 
						: SS_HIT_BOUNDS.get(hi).get(ihi)[0] + rel_ind;
				
				if (curr_ind >= 0 && curr_ind < curPR.size())
				{
					SequentialPhonic curr = curPR.get(curr_ind);
					if(!containsSPh(curr,ph_hits))	ph_hits.add(curr); 
				}
			}
		}
		for (int mi = 0 ; mi < SS_MISS_IDS.length; mi++)
		{
			List<SequentialPhonic> curPR = PIV_PT_LEX.getByID(SS_MISS_IDS[mi]).getPhonologicalRepresentation();

			for(int imi = 0; imi < SS_MISS_BOUNDS.get(mi).size(); imi++)
			{
				int curr_ind = posterior ? SS_MISS_BOUNDS.get(mi).get(imi)[1] + curPR.size() + rel_ind 
						: SS_MISS_BOUNDS.get(mi).get(imi)[0] + rel_ind;
				
				if(curr_ind >= 0 && curr_ind < curPR.size()) {
					SequentialPhonic curr = curPR.get(curr_ind); 
					if(!containsSPh(curr,ph_misses))	ph_misses.add(curr);
				}
		}}
		
		out.add(ph_hits);
		out.add(ph_misses);
		
		return out;
	}
	
	
	/**
	 * 
	 * @param rel_ind -- index of analysis relative to first phone of confusion/pivot point
	 * @param ids -- etymon ids 
	 * @param phs -- phs to analyzie (typically obtained by miss_and_hit_phones_at_rel_loc(rel_ind)) 
	 * @param theBounds -- array of two values, with the bounds of the confusion/pivot sequence in the etyma (with the specified ids). 
	 * 		[0] -- beginning of the confusion sequence, and [1] is its end. 
	 * @return
	 */
	private int[] get_ph_freqs_at_rel_loc(int rel_ind, int[] ids, List<SequentialPhonic> phs, List<List<int[]>> theBounds)
	{
		boolean posterior = rel_ind > 0; 
		int[] out = new int[phs.size()];
		for (int pi = 0 ; pi < phs.size(); pi++) {
			for (int eti = 0; eti < ids.length ; eti++)
			{
				List<SequentialPhonic> curPR = PIV_PT_LEX.getByID(ids[eti]).getPhonologicalRepresentation();
				for(int[] bound : theBounds.get(eti))
				{
					int mchi = (posterior ? bound[1] + curPR.size() : bound[0]) + rel_ind ;
					if ( ( posterior ? curPR.size() - 1 - mchi : mchi) >= 0)
						if(curPR.get(mchi).print().equals(phs.get(pi).print()))	out[pi] += 1; 
		}}}
		
		return out; 
	}
	
	//TODO redo score calculation so that it is the Pearson coefficient! 
	//TODO write up function description here ! 
	//TODO incorporate overall frequency of the miss phones and the hit phones in some way? 
	/**
	 * 
	 * @param n_rows
	 * @param rel_ind
	 * @param mode -- determines scoring algorithm 
	 * 		-- must be f1 (for f-score), or phi, for MCC, which is equivalent to Pearson's coefficient in this case,
	 * 			since it is operationalized as a 'binary classification' scenario 
	 * 			(x = predictor present (1) or absent (0); y = miss (1) or hit (0) 
	 * 	 @note that in cases where there are no hits at all for a certain relative location,
	 * 		a smoothing constant (PHI_SMOOTHING,F_SMOOTHING) will be applied 
	 * 			to prevent scores from getting all driven towards 1 (for F-scores) 
	 * 				or 0 (for phi-coefficients) by this 
	 * 				( would happen this way because hit counts that are zero
	 * 					 would drive precision and recall to 1, 
	 * 					but would also zero out the product for the numerator of the phi formula
	 * 
	 * 	@note the counts for boundary markers will not count against other phones
	 * 		as it is currently being done, a word bound and a morpheme bound also won't count against each other
	 * 			but to date, the morpheme bound has never been used -- will need to fix this if it is ever to be used here
	 * 			(there are definitely other things the morpheme bound being used would require changing for,
	 * 				since both it and the phoneme on the other side of it should be considered the context at that relative location...) 
	 * @return top N predictors of error, by position relative to a confusion or filter sequence, along with their correlation score
	 * 	// for bottom two lines: 
	// 		if the top [n_rows] items don't contain (a) a feature, (b) a phone, (c) the word bound
	//			then the highest scoring members of each of the two that is missing are the last two lines
	//			in order of their frequency. 
	//		if it includes two of the three, then the last line is the highest scoring member of the third. 
	//  
	 */
	public String[] topNScoredPredictorsAtRelInd(int n_rows, int rel_ind, String mode)
	{
		// below was useful useful in past debugging
		//System.out.println("Inactive features: "+pivotInactiveFeats.size());
		//for (String pifi : pivotInactiveFeats)	System.out.println(pifi); 
		
		mode = mode.toLowerCase(); 
		if (!mode.equals("phi") && !mode.equals("f") && !UTILS.valid_fB(mode))	
			throw new RuntimeException("Error: tried to run a context autopsy with an illegal scoring algorithm stipulation\n"
					+ "The options are either 'f' or 'f1' for F(1)-score, f and then an integer or double >= 0 for f-Beta score,\n"
					+ " or 'phi' for MCC (equivalent to Pearson's coefficient, in this case)."); 
		
		if (SS_MISS_IDS.length == 0)
			throw new RuntimeException("Error: tried to predict feats for a sequence subset that has no misses!");
		
		// below was useful for past debugging. 
		// System.out.println("calculating phones at rel loc "+rel_ind+"...");
		
		List<List<SequentialPhonic>> phs_here = hit_and_miss_phones_at_rel_loc(rel_ind); 
		List<SequentialPhonic> hit_phs_here = phs_here.get(0), miss_phs_here = phs_here.get(1); 
		
		// get frequency of phones among etyma that are misses, and those that are hits
		int[] hit_ph_frqs = get_ph_freqs_at_rel_loc(rel_ind, SS_HIT_IDS, hit_phs_here, SS_HIT_BOUNDS); 
		int[] miss_ph_frqs = get_ph_freqs_at_rel_loc(rel_ind, SS_MISS_IDS, miss_phs_here, SS_MISS_BOUNDS); 
		
		if (hit_ph_frqs.length != hit_phs_here.size() )
			throw new RuntimeException("Error : mismatch in size for hit_ph_frqs");
		if (miss_ph_frqs.length != miss_phs_here.size() )
			throw new RuntimeException("Error : mismatch in size for miss_ph_frqs");
	
		//debugging -- checking accuracy of frequency counts. -- was useful in past, commented out currently. 
		/** System.out.println("Frequency counts -- misses:"); 
		for (int mpi = 0 ; mpi < miss_phs_here.size(); mpi++)
			System.out.println(miss_phs_here.get(mpi)+": "+miss_ph_frqs[mpi]);
		System.out.println("and for hits:"); 
		for (int hpi = 0 ; hpi < hit_phs_here.size(); hpi++)
			System.out.println(hit_phs_here.get(hpi)+": "+hit_ph_frqs[hpi]); 
		System.out.println("----\n"); */ 
		
		
		HashMap<String,Integer> predPhIndexer = new HashMap<String,Integer>(); 
		
		String[] candPredictors = new String[featsByIndex.length*2+miss_ph_frqs.length]; 
			// variables that are being investigated as possible predictors of error. 
		
		boolean WDBND_INCR_ABS_FEAT = true,
				WDBND_INCR_ABS_PH = true; 
			// these determine, for the purposes of counts in the N matrix, 
				// if the word bound "counts" as the absence of other predictors
				// be they features, or phonemes, respectively
			// behavior will necessarily have to be different for a morpheme bound
				// -- those should always not count.
		
		//int[][] cand_freqs = new int[2][featsByIndex.length*2+miss_ph_frqs.length]; 
				// above -- deprecated, used before September 25, 2023. 
		
		int[][][] predictor_n_matr = new int[featsByIndex.length*2 + miss_ph_frqs.length]
				[2][2]; 
			// matrix for calculation of F1 or phi.
			// first dimension -- one for each candidate predictor variable of a miss (these include features, phones, and in some cases maybe bounds. 
			//second dimension -- 0 for hit, 1 for miss
			//third dimension -- 1 if predictor (feat, phone, bound) is present at the relative location, 0 if absent
		
		// fill list of all possible feature value predictors. 
		for (int fti = 0 ; fti < featsByIndex.length; fti++)
		{
			candPredictors[2*fti] = "-"+featsByIndex[fti];
			candPredictors[2*fti+1] = "+"+featsByIndex[fti];
		}
		
		// (may) need to get indices of word and morphemic boundary cells in advance
		// this is the list index (mpi) values they *will* have, if present -- if absent, -1. 
		int wdbnd_miss_loc = UTILS.findSeqPhBySymb(miss_phs_here, (new Boundary("word bound")).print() ), 
			mphbnd_miss_loc = UTILS.findSeqPhBySymb(miss_phs_here, (new Boundary("morph bound")).print() ),
			mphbnd_hit_loc = UTILS.findSeqPhBySymb(hit_phs_here, (new Boundary("morph bound")).print() ); 
		

		// MORPH BOUND AS PREDICTOR HANDLING
		// current handling if one of the predictors is a morpheme boundary -- don't handle it.
			// need to make some decisions on how handling it would work
			// in the following loop, it will only count against word boundaries if present, and nothing else
			// and is not permitted to be used as a predictor itself, 
				// if/when it needs to be, decisions will need to be made about how to count negatives against it 
				// and positives too. 
				// it should be able to be "seen through" for the phones on the other side, to handle cases where 
				// both the phonetic material on the otherside and the morpheme bound together are the contextual trigger.
				// and each of the cases were only *one* of the two is, 
					// the conditioning being insensitive to the other . 
		if (mphbnd_miss_loc != -1)
		{
			// do NOT add to predPhIndexer (current policy)
			
			// n00 and n01 counting wrt. word bound, if present as a predictor of misses
			if (wdbnd_miss_loc != -1)
			{
				// increment n01 for # as predictor with morph bound misses.
				predictor_n_matr[featsByIndex.length*2 + wdbnd_miss_loc][0][1] 
						= miss_ph_frqs[mphbnd_miss_loc]; 
				
				// increment n00 for # as predictor with morph bound *hits*. 
				if (mphbnd_hit_loc != -1)
					predictor_n_matr[featsByIndex.length*2 + wdbnd_miss_loc][0][0] 
							= hit_ph_frqs[mphbnd_hit_loc]; 
			}
		}
		
		// increment counts of misses for downstream use in calculation (hits will be next). 
		for(int mpi = 0 ; mpi < miss_ph_frqs.length; mpi++)
		{
			if (mpi == mphbnd_miss_loc)	continue; // behavior handled above, before loop. 
			
			SequentialPhonic missPred_ph = miss_phs_here.get(mpi); 
			int matr_phIndex = featsByIndex.length*2 + mpi;
			
			boolean curPredIsWdBnd = missPred_ph.print().equals((new Boundary("word bound")).print()); 
			candPredictors[matr_phIndex] = curPredIsWdBnd ? 
					missPred_ph.print() : "/"+missPred_ph.print()+"/" ; 
			predPhIndexer.put(missPred_ph.print(), mpi + featsByIndex.length*2); 

				// always increment n11 
			predictor_n_matr[matr_phIndex][1][1] = miss_ph_frqs[mpi]; 
			
			// all the other phones are ones that are NOT at this rel_loc for this miss...
				// thus the predictor is absent, but they are misses -- include in counts of n01 (unpredicted "positives", the positive being a 'miss') 
				// increment accordingly 
				// however -- do not penalize proper phones if the segment found is a boundary. 
				// currently this will also avoid punishing other boundary phones
				// NOTE need to fix this if we ever actually use the morpheme bound. 
					// but for now this seems fine. 
			if (!curPredIsWdBnd || WDBND_INCR_ABS_PH)
			{
				int wi = featsByIndex.length*2; 
				while(wi < matr_phIndex) 
					predictor_n_matr[wi++][0][1] += miss_ph_frqs[mpi]; 
				wi = matr_phIndex + 1; 
				while (wi < featsByIndex.length*2 + miss_ph_frqs.length)
					predictor_n_matr[wi++][0][1] += miss_ph_frqs[mpi]; 
			}
			
			
			//incorporate also the counts for features that the phone has (unless they are unspecified) 
				// for the "miss/error" side of the calculation 
				// ... if it is not a boundary flag
			if (!curPredIsWdBnd)
			{
				char[] fstr = missPred_ph.getFeatString().toCharArray(); 
				for (int spi = 0 ; spi < featsByIndex.length; spi++)
				{
					int fspi = Integer.parseInt(""+fstr[spi]); 
					if (fspi != UTILS.UNSPEC_INT)
					{
						predictor_n_matr[2*spi + fspi/2] // fspi / 2 = 0 if '-', 1 if '+', which is in accord with the ordering of these in candPredictors
								[1][1] += miss_ph_frqs[mpi]; 
						
						//and the converse, for the feature that it is *not* 
						predictor_n_matr[2*spi + (1 - fspi/2)][0][1] += miss_ph_frqs[mpi]; 
					}
				}	
			}
			else if (WDBND_INCR_ABS_FEAT) /** if the current predictor is a word bound, 
				 * and we are counting it as negative for feature predictors... */ 
			{
				for (int spi = 0 ; spi < featsByIndex.length; spi++)
				{
					predictor_n_matr[spi*2][0][1] += miss_ph_frqs[mpi]; 
					predictor_n_matr[spi*2+1][0][1] += miss_ph_frqs[mpi]; 
				}
			}
		}
		
		// do the same for counts for phones at rel_locs for hits
			// , but only if those phones have a non-zero count for misses.
			// however, the increment for features happens either way 
			// to make sure the calculations for those goes correctly. 
		for (int hpi = 0 ; hpi < hit_ph_frqs.length; hpi++)
		{
			if (hpi == mphbnd_hit_loc)	continue; 
			
			SequentialPhonic hitPred_ph = hit_phs_here.get(hpi); 
			boolean curPredIsWdBnd = hitPred_ph.print().equals((new Boundary("word bound")).print()); 

			//feature increments first, here. 
			if (!curPredIsWdBnd)
			{
				char[] fstr = hitPred_ph.getFeatString().toCharArray(); 
				for (int spi = 0 ; spi < featsByIndex.length; spi++)
				{
					int fspi = Integer.parseInt(""+fstr[spi]); 
					if (fspi != UTILS.UNSPEC_INT) {
						predictor_n_matr[2*spi + fspi/2][1][0] += hit_ph_frqs[hpi]; 
						predictor_n_matr[2*spi + 1 - fspi/2][0][0] += hit_ph_frqs[hpi]; 
					}
				}
			}
			else if (WDBND_INCR_ABS_FEAT) // if incrementing wordbound as absence of feature spec'd predictor ... 
			{
				for (int spi = 0 ; spi < featsByIndex.length; spi++)
				{
					predictor_n_matr[spi*2][0][0] += hit_ph_frqs[hpi]; 
					predictor_n_matr[spi*2+1][0][0] += hit_ph_frqs[hpi]; 
				}
			}
			
			// now the segment-wise counts
				// count positively for this segment is found at this rel_loc for misses...
				// negatively for all others (unless this segment is a boundary flag,
						// in which case it only counts for its own calculation 
						// i.e. phones count against it , it counts for itself 
						// (that is, counting 'for' itself as a predictor if present at a miss, 
						// against itself if absent at a miss or present at a hit; same reverse for proper segmental phones)
			int ppii_for_hpi = 
					predPhIndexer.containsKey(hitPred_ph.print()) 
					? predPhIndexer.get(hitPred_ph.print()) : -1; 
			if (ppii_for_hpi != -1)	
				predictor_n_matr[ppii_for_hpi][1][0] += hit_ph_frqs[hpi];
			
			if (!curPredIsWdBnd || WDBND_INCR_ABS_PH)
			{
				int wi = featsByIndex.length * 2; 
				while ( wi < ppii_for_hpi) 
					predictor_n_matr[wi++][0][0] += hit_ph_frqs[hpi]; 
				if (wi == ppii_for_hpi) wi++;	// already incremented above. If wi starts at -1 both this and the loop above are bypassed of course.  
				while ( wi < featsByIndex.length * 2 + miss_ph_frqs.length)
					predictor_n_matr[wi++][0][0] += hit_ph_frqs[hpi]; 
			}
		}
		
		//zero out n0* cells for morpheme boundary -- per current policy of "not handling" it.
		if (mphbnd_miss_loc != -1)
		{
			predictor_n_matr[mphbnd_miss_loc][0][0] = 0; 
			predictor_n_matr[mphbnd_miss_loc][0][1] = 0; 
		}
		
		/** below is old version before September 25, 2023. 
		
		// increment counts of predictiveness toward misses here. 
		for (int mpi = 0; mpi < miss_ph_frqs.length; mpi++)
		{
			String phprint =  phs_here.get(1).get(mpi).print(); 
			candPredictors[featsByIndex.length*2 + mpi] = "/"+phprint+"/"; 
			
			cand_freqs[1][featsByIndex.length*2 + mpi] = miss_ph_frqs[mpi];
			
			predPhIndexer.put(phprint, mpi + featsByIndex.length*2); 
		}

		//TODO currently debugigng here -- September 25, 2023. 
			// Looking for error that triggers irrelevant features like splng to appear in context autopsy
			// may be related to the surprisingly high numerical outputs too.. 
		// only to cand_freqs for hits, since we are predicting misses, not hits per se. 
		for (int hpi = 0; hpi < hit_ph_frqs.length; hpi++)
			if (predPhIndexer.containsKey(phs_here.get(0).get(hpi).print()))
				cand_freqs[0][predPhIndexer.get(phs_here.get(0).get(hpi).print())] = hit_ph_frqs[hpi]; 
		
		int unspec = UTILS.UNSPEC_INT;
		
		// get counts for feature value predictors using phone counts. 
		for (int phi = 0; phi < phs_here.get(1).size(); phi++) // iterating over miss phones
		{
			SequentialPhonic missed_ph_here = phs_here.get(1).get(phi); 
			if (!missed_ph_here.getType().contains("bound"))
			{	
				char[] fstr = missed_ph_here.toString().split(":")[1].toCharArray();
				
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
			SequentialPhonic hit_ph_here = phs_here.get(0).get(phi); 
			
			if(!hit_ph_here.getType().contains("bound"))
			{	char[] fstr = hit_ph_here.toString().split(":")[1].toCharArray();
			
				for (int spi = 0; spi < featsByIndex.length; spi++)
				{
					int fspi = Integer.parseInt(""+fstr[spi]); 
					if (fspi != unspec)
						cand_freqs[0][2*spi + fspi/2] += hit_ph_frqs[phi];
				}
			}
		}
		*/
		
		double[] scores = new double[candPredictors.length];
		for(int fi = 0 ; fi < candPredictors.length; fi++)
		{
			if(pivotInactiveFeats.contains(UTILS.MARK_POS+candPredictors[fi].substring(1)) 
					|| pivotInactiveFeats.contains(UTILS.MARK_NEG+candPredictors[fi].substring(1)))
			{
				// material below useful in past debugging
				//System.out.println("Suppressing scoring for inactive feature: "+candPredictors[fi]); 
				
				fi += (UTILS.MARK_NEG == candPredictors[fi].charAt(0) ? 1 : 0) ; 
				continue; 
			}
			
			// material below useful in past debugging
			/** System.out.print("counts for "+candPredictors[fi]+": "); 
			for (int xi = 0; xi<2; xi++)
				for (int yi = 0 ; yi<2 ; yi++)
					System.out.print("n"+xi+yi+" = "+
						predictor_n_matr[fi][xi][yi] + " | ");*/
			
			if (mode.equals("phi"))
				scores[fi] = UTILS.phi_coeff( //with smoothing for zero hit scenario if necessary
						Math.max(PHI_SMOOTHING,predictor_n_matr[fi][0][0]),
						Math.max(PHI_SMOOTHING,predictor_n_matr[fi][1][0]), 
						Math.max(PHI_SMOOTHING, predictor_n_matr[fi][0][1]),
						predictor_n_matr[fi][1][1]); 
			else 
			{
				double miss_pred_precision = 
						(double) predictor_n_matr[fi][1][1] 
						/ ((double) Math.max( F_SMOOTHING, 
								predictor_n_matr[fi][1][0] + predictor_n_matr[fi][1][1])); 
				double miss_pred_recall = 
						(double) predictor_n_matr[fi][1][1]
						/ ((double) Math.max(F_SMOOTHING, 
								predictor_n_matr[fi][0][1] + predictor_n_matr[fi][1][1])); 
				
				if (mode.equals("f"))
					scores[fi] = UTILS.f1(miss_pred_precision, miss_pred_recall);
				else	/*mode must be f_Beta!*/ 
					scores[fi] = UTILS.fB_score(miss_pred_precision, miss_pred_recall, 
							Double.parseDouble(mode.substring(1))); 	
			}
			
			//below was useful for past debugging
			// System.out.println("score : "+scores[fi]);
		}
		
		//choose final output
		int ffi = 0 ; 
		while(ffi < scores.length ? scores[ffi] <= AUTOPSY_DISPLAY_THRESHOLD : false )	
			ffi++; 
		
		String adt_print = ""+AUTOPSY_DISPLAY_THRESHOLD; 
		if (adt_print.length() > 4) adt_print = adt_print.substring(0,4); 
 		
		double[] lb_scores = new double[n_rows+2]; //lb = "leader board"
		String[] lb_out = new String[n_rows+2];
		for (int oi = 0; oi < n_rows + 2 ; oi++)	lb_out[oi] = "<"+adt_print+" low limit";
				
		int topFtRank = -1, topPhRank = -1, wdBndRank = -1; 
		int topFtFFI = -1, topPhFFI = -1,
				wdBndFFI = (wdbnd_miss_loc == -1) ? -1 : wdbnd_miss_loc + featsByIndex.length*2; 
		
		// we know whether the predictor is a feature (index < featsByIndex.length*2)
		// the word bound (index == wdBndFFI) 
		// or a phone (the other cases) -- assuming now morphemic phones are in use here. 
		// can also use the cell in lb_out :
			// for feature: "+-".contains(lb_out[IND].substring(0,1)) && !" ".equals(lb_out[IND].substring(1,2))
			// for phone: "/".equals(lb_out[IND].substring(0,1))
			// for word bound: "#".equals(lb_out[IND].substring(0,1)) 
			// same would apply to scout. 
		while (ffi < candPredictors.length)
		{
			double sc= scores[ffi]; 
			if (sc > AUTOPSY_DISPLAY_THRESHOLD)
			{	
				int placer = 0; 
				boolean try_place = true; 
				String scout = aut_score_out(candPredictors[ffi],sc);
				
				// check for usurpation of type-wise top score. 
				String predTypeHere = 
						scout.substring(0,1).equals("#") ? "wdbnd"
							: scout.substring(0,1).equals("/") ? "phone" : "feat"; 

				
				
				while(try_place)
				{	if (sc < lb_scores[placer])
					{	placer++; 
						try_place = placer < n_rows + 2; 
					}
					else	
						try_place = false;
				}
				
				boolean usurpTopForPredType = // because we should always be at a lower place number if the score is higher (massive errors will make it clear if this assumption is wrong) 
						predTypeHere.equals("wdbnd") ? false : //word bound -- tautologically, true, but irrelevant as we always know the word bound FFI. 
							ffi < featsByIndex.length * 2 ? //if true -- a feat spec. If false -- a phone. 
								(topFtFFI == -1 ? true : sc > scores[topFtFFI]) 
								: (topPhFFI == -1 ? true : sc > scores[topPhFFI]); 
				if (usurpTopForPredType && ffi < featsByIndex.length * 2) 
				{	topFtFFI = ffi ; topFtRank = placer < n_rows ? placer : -1; }
				else if (usurpTopForPredType)	
				{	topPhFFI = ffi ; topPhRank = placer < n_rows ? placer : -1; }
				
					
				String type_last_placed = ""; 
				while (placer < n_rows + 2)
				{
					// this whole section (ironically) determines the type-wise ranking. The main action is much more concise, below this. 
					String type_to_place = 
						scout.substring(0,1).equals("#") ? "wdbnd"
							: scout.substring(0,1).equals("/") ? "phone" : "feat"; 
					
						// note that a -1 for feat/phone could be the first feat or phone,
							// or a situation where no feat/phone is currently in the top N on the leader board
								// but there will still be a top scoring member to compete with. 
						
					int typeTopRank = type_to_place.equals("feat") ? topFtRank : topPhRank; 
					int newRankIfTop = placer < n_rows ? placer : -1; 
					
					if (type_to_place.equals("wdbnd"))	wdBndRank = newRankIfTop; // forking below doesn't apply as only one word bound predictor possible. 
					
					// already handled the usurpation case with an initial addition before this while-loop. 
					
					// otherwise, for initial insertions that DO NOT usurp the ft/ph top runner -- do not change it. 
					// do nothing if previous top is more than one above 
					// but if it is only one above, this is a demotion, and the top rank for the feat/ph must be bumped down (or off the board)
						// except in the case where top ft/ph was usurped and is itself being bumped down [type_last_placed.equals(type_to_place)] 
						// -- in that case, do nothing, as necessary changes were already made
						// note that the top for the type cannot be >= placer, because then newTopForPredType would have been true
							// and the else if clause above would have preempted this one.. 
					else if (!type_last_placed.equals("") && typeTopRank == placer -1 && !type_last_placed.equals(type_to_place))
					{
						// note newRankIfTop will be -1 if placer >= n_rows
						if (type_to_place.equals("feat"))	topFtRank = newRankIfTop;
						else /*phone*/  topPhRank = newRankIfTop; 	
						// no change to top--FFI since it's just being bumped down in rank. 
					}
					
					// finally, the actual bumping down. 
					double to_move = lb_scores[placer];	 String moving_outp = lb_out[placer];
					lb_scores[placer] = sc; 	lb_out[placer] = scout;
		
					if (to_move <= AUTOPSY_DISPLAY_THRESHOLD)	break;
					else
					{
						sc = to_move; 	scout = moving_outp;	placer++;
						type_last_placed = ""+type_to_place; 
					}
				}
			}
			ffi++; 
		}
		
		// now deal with the last two rows, so that if any of the three of (a) feats, (b) phones, (c) the word bound is not shown,
			// the top scoring member of that category will be shown at the bottom of the chart. 
			// bottom of chart just stays at is, otherwise .
		if (topFtRank == -1 || topPhRank == -1 || wdBndRank == -1)
		{
			//note that if no phones were correlated to error at a level above the threshold,
				// topPhFFI will still be -1 at this point, 
				// likewise for topFtFFI 
			//handle this first to preempt errors. 
			if (topFtFFI == -1 || topPhFFI == -1) 
			{
				if (topFtFFI == -1 && topPhFFI == -1) // both feats and phones are insignificant 
					// (so WdBound must be alone at top or insignificant too...) just return.
					return lb_out; 
				
				// this point means either no phones above the printing threshold, or no feats, but not both. 
				if (wdBndRank == -1)	{
					lb_out[n_rows] = aut_score_out("#",scores[wdBndFFI]); 	
					lb_out[n_rows+1] = 
						(topFtFFI == -1 ? "fts" : "phs" ) + " all < "+adt_print;
				  	return lb_out; 
				}
			}
			 
			boolean featsFirst = scores[topFtFFI] > scores[topPhFFI]; 
			int lowerSegTopFFI = featsFirst ? topPhFFI : topFtFFI; 
			
			// the word bound cannot squeeze out both of the other two, so if it made the top N, only one of the other two must've been excluded
				// -- so, only one row to fill in. 
			if (wdBndRank != -1)  
				lb_out[n_rows + 1] =  
					aut_score_out (candPredictors[lowerSegTopFFI], scores[lowerSegTopFFI]); 
			
			// if the word bound is higher than the loser between phones and feats, it is penultimate, and phones/feats are the last
			else if (scores[wdBndFFI] > scores[lowerSegTopFFI])
			{
				lb_out[n_rows] = aut_score_out("#",scores[wdBndFFI]); 
				lb_out[n_rows+1] = aut_score_out(candPredictors[lowerSegTopFFI], scores[lowerSegTopFFI]); 
			}
			else
			{
				//first of all, word bound is in last. 
				lb_out[n_rows+1] = aut_score_out("#", scores[wdBndFFI]) ; 
				// modify the penultimate row only if it the remaining type (ft/ph) is not already in top N
				if ( (featsFirst ? topPhRank : topFtRank) == -1)
					lb_out[n_rows] = aut_score_out( candPredictors[lowerSegTopFFI] , scores[lowerSegTopFFI]); 
				
			}
		}
		
		return lb_out; 
	}
	
	public String aut_score_out (String predictor, double score)
	{
		boolean below_threshhold = score < AUTOPSY_DISPLAY_THRESHOLD; 
		String numeric_element = 
				below_threshhold ? " < "+ AUTOPSY_DISPLAY_THRESHOLD : " : "+ score; 
		numeric_element.replace("0.","."); 
		int nePdLoc = numeric_element.lastIndexOf("."); 
		if (nePdLoc != -1 && nePdLoc < numeric_element.length()-4)
			numeric_element = numeric_element.substring(0,nePdLoc + 4); 
		
		return predictor + numeric_element; 
	}
	
	public boolean isFiltSet()
	{
		return filtSet; 
	}
	
	public boolean isPivotSet()
	{
		return pivotSet;
	}
	
	public void printFourColGraph(Lexicon inpLex, boolean errorsOnly)
	{
		Etymon[] inpWds = inpLex.getWordList(); 
		for(int i = 0; i < inpWds.length; i++) {
			if (errorsOnly ? IN_SUBSAMP[i] && !isHit[i] : IN_SUBSAMP[i])
			{
				System.out.print(append_space_to_x(i+",",6)+"| ");
				System.out.print(append_space_to_x(inpWds[i].toString(), 19) + "| ");
				if (pivotSet)
					System.out.print(append_space_to_x(PIV_PT_LEX.getByID(i).toString(),19)+"| ");
				System.out.print(append_space_to_x(RES.getByID(i).toString(),19)+"| ");
				System.out.print(GOLD.getByID(i)+"\n");
			}	}
	}
	
	/** rmvFeatsActiveInSample
	 *
	/* Used on subsamples to form the inactiveFeats lists for the subsample, which may be smaller,
		/* as the features may actually (somehow) be active (for some reason) at the intermediate pivot stage) 
	/* @param earlier_inactive_list the preexisting list of inactive features (which at the start is just the entire list of +/- feature stipulations) 
	/* @param sample -- lexicon we are looking through. 
	 * @return list of inactive feats that has been modified in this way.
	 * 			(will include the values that the feature CONSTANTLY has: e.g. -splng if all segments are -splng. )  
	 */
	public List<String> rmvFeatsActiveInSample(List<String> earlier_inactive_list, Lexicon sample)
	{
		if (earlier_inactive_list.size() == 0)	return earlier_inactive_list; 
		
		List<String> out = new ArrayList<String>(earlier_inactive_list), 
				indexedFeatList = Arrays.asList(featsByIndex); 
		for (int idi = 0 ; idi < TOTAL_ETYMA; idi++)
		{
			if (IN_SUBSAMP[idi])
			{
				SequentialPhonic[] repi = sample.getByID(idi).getPhOnlySeq();
				for (SequentialPhonic phmi : repi)
				{
					char[] featstring = phmi.getFeatString().toCharArray(); 
					int ifi = 0; 
					while (ifi < out.size())
					{	
						String fstipi = out.get(ifi); 
						int fsloc = indexedFeatList.indexOf(fstipi.substring(1)); 
							// need to use Integer.parseInt to avoid misinterpretation as hashcode.
						if ( UTILS.getFeatspecIntFromMark(fstipi.charAt(0)) != Integer.parseInt(""+featstring[fsloc]) )  
							out.remove(ifi); 
						else	ifi++; 
					}
					
					if (out.size() == 0)	return out; 
				}
			}
		}		
		return out; 
	}
	
	// class used to fix the fact that, (probably) for class-internal reasons,
		// filtMatchBounds the method within the class SequentialFilter that is used to get the list of bounds, 
		// for filter-onsets, uses the n+1 index for every case except when the word starts after the onset
			// i.e. the first element of each filter boundary tuple is never 1, but it can be 0 (or 2) 
		// within this class, however, it needs to be 1, so that the word boundary itself can be properly considered as a 
			// possible predictor of error (previously, it was being ignored to avoid out-of-bounds exceptions).
		// further investigation into the interaction of this class with SequentialFilter may be necessary 
			// but with a fairly broad if not deep investigation on September 27 2023 it seems fine... 
	public List<List<int[]>> processSeqFiltBounds (List<List<int[]>> sfbounds)
	{
		List<List<int[]>> out = new ArrayList<List<int[]>> ();
		for (List<int[]> sublist : sfbounds)
		{
			List<int[]> outsublist = new ArrayList<int[]>(); 
			for (int[] pair : sublist) 
				outsublist.add(new int[] 
						{ Math.max(pair[0], 1), pair[1]} ); 
			out.add(outsublist);
		}
		return out; 
	}
}