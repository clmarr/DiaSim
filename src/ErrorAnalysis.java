import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;

public class ErrorAnalysis {
	private int[] levDists; 
	private double[] peds, feds;
	private boolean[] isHit; 
	private double pctAcc, pct1off, pct2off, avgPED, avgFED; 
	
	private Phone[] resPhInventory, goldPhInventory; 
	
	private HashMap<String, Integer> resPhInds, goldPhInds; 
		// indexes for phones in the following int arrays are above.
	private int[] errorsByResPhone, errorsByGoldPhone; 
	private double[] errorRateByResPhone, errorRateByGoldPhone; 
	private int[][] confusionMatrix; 
		// rows -- indexed by resPhInds; columns -- indexed by goldPhInds
	
	private String[] featsByIndex; 
		
	private List<LexPhon[]> mismatches; 
	
	private final int NUM_TOP_ERR_PHS_TO_DISP = 4; 
	
	private FED featDist;
	
	public ErrorAnalysis(Lexicon theRes, Lexicon theGold, String[] indexedFeats, FED fedCalc)
	{
		featDist = fedCalc; 
		featsByIndex = indexedFeats;
		
		resPhInventory = theRes.getPhonemicInventory();
		goldPhInventory = theGold.getPhonemicInventory();
		
		resPhInds = new HashMap<String, Integer>(); 
		goldPhInds = new HashMap<String, Integer>();
		
		for(int i = 0 ; i < resPhInventory.length; i++)
			resPhInds.put(resPhInventory[i].print(), i);
		for (int i = 0 ; i < goldPhInventory.length; i++)
			goldPhInds.put(goldPhInventory[i].print(), i);
		
		errorsByResPhone = new int[resPhInventory.length];
		errorsByGoldPhone = new int[goldPhInventory.length];

		errorRateByResPhone = new double[resPhInventory.length]; 
		errorRateByGoldPhone = new double[goldPhInventory.length];

		confusionMatrix = new int[resPhInventory.length][goldPhInventory.length];
		
		mismatches = new ArrayList<LexPhon[]>();
		
		int NUM_ETYMA = theGold.getWordList().length; 
		levDists = new int[NUM_ETYMA]; 
		peds = new double[NUM_ETYMA];
		feds = new double[NUM_ETYMA];
		isHit = new boolean[NUM_ETYMA];
		double totLexQuotients = 0.0, numHits = 0.0, num1off=0.0, num2off=0.0, totFED = 0.0; 
				
		for (int i = 0 ; i < NUM_ETYMA ; i++)
		{	
			levDists[i] = levenshteinDistance(theRes.getByID(i), theGold.getByID(i));
			isHit[i] = (levDists[i] == 0); 
			numHits += (levDists[i] == 0) ? 1 : 0; 
			num1off += (levDists[i] <= 1) ? 1 : 0; 
			num2off += (levDists[i] <= 2) ? 1 : 0; 
			peds[i] = (double)levDists[i] / (double) theGold.getByID(i).getNumPhones();
			totLexQuotients += peds[i]; 
			
			featDist.compute(theRes.getByID(i), theGold.getByID(i), 1.0); 
			
			feds[i] = featDist.getFED();
			totFED += feds[i];
			
			if(!isHit[i])
				updateConfusionMatrix(theRes.getByID(i), theGold.getByID(i));
		}
		pctAcc = numHits / (double) NUM_ETYMA; 
		pct1off = num1off / (double) NUM_ETYMA;
		pct2off = num2off / (double) NUM_ETYMA; 
		avgPED = totLexQuotients / (double) NUM_ETYMA; 	
		avgFED = totFED / (double) NUM_ETYMA; 
		
		//TODO debugging
		System.out.println("Calculate by-phone error rates...");
		
		//calculate error rates by phone for each of result and gold sets
		HashMap<SequentialPhonic, Integer> resPhCts = theRes.getPhonemeCounts(), 
				goldPhCts = theGold.getPhonemeCounts(); 
		
		for (int i = 0 ; i < resPhInventory.length; i++)
			errorRateByResPhone[i] = (double)errorsByResPhone[i] 
					/ (double)resPhCts.get(resPhInventory[i]);
		for (int i = 0 ; i < goldPhInventory.length; i++)
			errorRateByGoldPhone[i] = (double)errorsByGoldPhone[i]
					/ (double)goldPhCts.get(goldPhInventory[i]); 
		
		//TODO debuggging
		System.out.println("EA constr.");
	}
	
	//@param get_contexts -- determine if we want to list the most problematic context info
	public void confusionPrognosis(boolean get_contexts)
	{
		// top n error rates for res and gold
		int[] topErrResPhLocs = arrLocNMax(errorRateByResPhone, NUM_TOP_ERR_PHS_TO_DISP); 
		int[] topErrGoldPhLocs = arrLocNMax(errorRateByGoldPhone, NUM_TOP_ERR_PHS_TO_DISP); 
		
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
				else	System.out.println(""+i+": "+resPhInventory[topErrResPhLocs[i]]+" with rate "+rate);
				
			}
			System.out.println("Gold phones most associated with error: ");
			for(int i = 0 ; i < topErrGoldPhLocs.length; i++)
			{
				double rate = errorRateByGoldPhone[topErrGoldPhLocs[i]];
				if (rate < pctAcc * 1.17)	i = topErrGoldPhLocs.length; 
				else	System.out.println(""+i+": "+goldPhInventory[topErrGoldPhLocs[i]]+" with rate "+rate); 
			}	
		}
		else	System.out.println("No particular phones especially associated with error.");
			
		System.out.println("---\nMost common distortions: "); 
		int[][] topDistortions = arr2dLocNMax(confusionMatrix, 5); 
		for(int i = 0 ; i < topDistortions.length; i++)
		{
			System.out.println(""+i+": "+resPhInventory[topDistortions[i][0]]+" for "
					+goldPhInventory[topDistortions[i][1]]); 
			
			//parse contexts
			if (get_contexts)
			{
				List<String> probCtxts = identifyProblemContextsForDistortion(topDistortions[i][0], topDistortions[i][1]);
				System.out.println("Most common contexts in result phone sequence for this distortion: "); 
				for (String obs : probCtxts)	System.out.println(""+obs); 
			}
			 
		}
	}
	
	//TODO method to use a "pivoting" predictor stage? -- implement this later -- first paper submission first. 
	
	
	
	//also updates errorsByResPhone and errorsByGoldPhone
	//..and also updates the list mismatches 
	private void updateConfusionMatrix(LexPhon res, LexPhon gold)
	{
		mismatches.add( new LexPhon[] {res, gold}) ; 
		
		SequentialPhonic[][] alignedForms = getAlignedForms(res,gold); 

		
		for (int i = 0 ; i < alignedForms.length ; i++)
		{
			String r = alignedForms[i][0].print(), g = alignedForms[i][1].print(); 
			if (!r.equals(g))
			{
				if (r.equals("∅"))	confusionMatrix[resPhInventory.length][goldPhInds.get(g)] += 1;
				else if(g.equals("∅"))	confusionMatrix[resPhInds.get(r)][goldPhInventory.length] += 1; 
				else
				{
					confusionMatrix[resPhInds.get(r)][goldPhInds.get(g)] += 1;
					errorsByResPhone[resPhInds.get(r)] += 1; 
					errorsByGoldPhone[resPhInds.get(g)] += 1;	
				}
			}
		}
	}
	
	// report which contexts tend to surround distortion frequently enough that it becomes 
		// suspicious and is worth displaying to the user
	// will report only features that are CONSTANT for pre-prior and post-posterior positions
	// and features taht are more common than 
	// TODO reorganize this! 
	private List<String> identifyProblemContextsForDistortion(int resPhInd, int goldPhInd)
	{
		List<String> out = new ArrayList<String>(); 

		List<LexPhon[]> pairsWithDistortion = mismatchesWithDistortion(resPhInd, goldPhInd); 
	
		// TODO currently doing this with the words in the GOLD forms -- may need to revise this. 
		HashMap<SequentialPhonic, Integer> prePriorCounts = new HashMap<SequentialPhonic, Integer>(); 
		HashMap<SequentialPhonic, Integer> postPostrCounts = new HashMap<SequentialPhonic, Integer>();
		HashMap<SequentialPhonic, Integer> priorPhoneCounts = new HashMap<SequentialPhonic, Integer>(); 
		HashMap<SequentialPhonic, Integer> posteriorPhoneCounts = new HashMap<SequentialPhonic, Integer>(); 
		
		int total_distortion_instances = 0; 
		
		for (int i = 0 ; i < pairsWithDistortion.size(); i++)
		{
			SequentialPhonic[][] alignedReps = 
					getAlignedForms(pairsWithDistortion.get(i)[0], pairsWithDistortion.get(i)[1]); 
			List<Integer> distortLocs = getDistortionLocsInWordPair(resPhInd, goldPhInd, alignedReps); 
			
			total_distortion_instances += distortLocs.size(); 
			
			for (Integer dloc : distortLocs)
			{
				SequentialPhonic prePrior = new Boundary("word bound"); 
				SequentialPhonic priorPh = new Boundary("word bound"); 
				if (dloc > 0)
				{
					priorPh = alignedReps[dloc-1][1];
					if (dloc > 1)	prePrior = alignedReps[dloc-2][1]; 
				}
				if (priorPhoneCounts.containsKey(priorPh))
					priorPhoneCounts.put(priorPh, priorPhoneCounts.get(priorPh)+1); 
				else	priorPhoneCounts.put(priorPh, 1);
				if (prePriorCounts.containsKey(prePrior))
					prePriorCounts.put(prePrior, prePriorCounts.get(prePrior) + 1);
				else	prePriorCounts.put(prePrior, 1);
				
				SequentialPhonic ppph =new Boundary("word bound");
				SequentialPhonic postPh = new Boundary("word bound"); 
				if (dloc < alignedReps[1].length - 1)
				{
					postPh = alignedReps[dloc+1][1];
					if (dloc < alignedReps[1].length - 2)	ppph = alignedReps[dloc+2][1];
				}
				if (posteriorPhoneCounts.containsKey(postPh))
					posteriorPhoneCounts.put(postPh,
							posteriorPhoneCounts.get(postPh) + 1); 
				else
					posteriorPhoneCounts.put(postPh, 1);
				if (postPostrCounts.containsKey(ppph))
					postPostrCounts.put(ppph, postPostrCounts.get(ppph) + 1);
				else	postPostrCounts.put(ppph, 1);
				
			}

		}
		
		//now we have the counts for the immediate neighbors -- TODO analyze
	
		//prior context info
		if (prePriorCounts.keySet().size() == 1)
			out.add("Constant pre prior phone : "+ (new ArrayList<SequentialPhonic>(prePriorCounts.keySet())).get(0).print());
		else
		{
			String ppcfs = getConstantFeats(prePriorCounts); 
			if (ppcfs.length() > 1 )
				out.add("Pre prior phone constant features: "+ppcfs);
		}
		
		String commPhsStringed = getCommonCtxtPhs(priorPhoneCounts, total_distortion_instances, 0.3); 
		if(commPhsStringed.length() > 1)
			out.add("Most common immediate prior phones : "+commPhsStringed); 
		if (priorPhoneCounts.get(new Boundary("word bound")) > (double) total_distortion_instances * 0.5)
			out.add("Majority of instances just after word onset. Common features of remainder: "
					+getContextCommonFeats(priorPhoneCounts, 
							total_distortion_instances - priorPhoneCounts.get(new Boundary("word bound")))); 
		else	out.add("Common prior feats: "
					+getContextCommonFeats(priorPhoneCounts, total_distortion_instances)); 
		
		//posterior context info
		commPhsStringed = getCommonCtxtPhs(posteriorPhoneCounts, total_distortion_instances, 0.3); 
		if (commPhsStringed.length() > 1)
			out.add("Most common immediate posterior phones : "+commPhsStringed); 
		else
		{
			String ppcfs = getConstantFeats(postPostrCounts); 
			if (ppcfs.length() > 1)
				out.add("Post posterior phone constant features : "+ppcfs); 
			
		}
		
		return out; 
	}
	
	
	// Stringed list of all feats that are present in EVERY phone that is a key in the HashMap. 
	private String getConstantFeats(HashMap<SequentialPhonic,Integer> ctxts)
	{
		List<SequentialPhonic> phs = new ArrayList<SequentialPhonic>(ctxts.keySet()); 
		if (phs.size() == 1) //all consistently one phone!
			return phs.get(0).print(); 
		
		char[] constVals = phs.get(0).getFeatString().toCharArray(); 
		for (int i = 1 ; i < phs.size(); i++)
		{	char[] theFeats = phs.get(i).getFeatString().toCharArray();
			for (int c = 0 ; c < theFeats.length; c++)
				if (theFeats[c] != constVals[c])	constVals[c] = ' '; 
		}
		
		String output = ""; 
		for (int c = 0 ; c < constVals.length; c++)
			if ("+-.".contains(""+constVals[c]))
				output += constVals[c]+featsByIndex[c]+","; 
		
		if (output.equals("")) 
			return output;
		else
			return output.substring(0, output.length()-1);		
	}
	
	/**
	 * getCommonCtxtPhs
	 * @param phCts -- map of phones linked to their frequency of occurrence in the context of a paritcular distortion
	 * @param total_dist_occs -- toal occurrences of the distortion we are getting contexts for
	 * @param thresh -- minimum percentage of total frequency necessary to report .
	 * @return in String form, any phones with a frequency above the threshold, followed by their share of all phones in that context
	 */
	private String getCommonCtxtPhs (HashMap<SequentialPhonic, Integer> phCts, int total_dist_occs, double thresh)
	{
		String output = ""; 
		
		for (SequentialPhonic ph : phCts.keySet())
		{
			double share = (double) phCts.get(ph) / (double) total_dist_occs ;
			if (share > thresh) 	output += ph.print()+" "+share+"; ";
		}
		
		if (output.equals(""))	return output; 
		else	return output.substring(0, output.length() - 1 ); 	
	}
	
	//return: HashMap where each feature is key 
	// and value is an int array of [ #+, #-, #. ]
	// where word bounds are concerned, they don't add to ANY of the counts
		// however if the word bound itself is the boundary over 50% of the time, this will be reported in 
		// the method identifyProblemContextsForDistortion
	// @param ctxtName -- i.e. "prior" or "posterior" -- sequential relation to target phone
	private String getContextCommonFeats(HashMap<SequentialPhonic,Integer> ctxtCts, int total_dist_instances)
	{
		
		Set<SequentialPhonic> phonesFound = ctxtCts.keySet(); 
		HashMap<String, int[]> ftMatr = new HashMap<String, int[]>(); 
		for(int i = 0 ; i < featsByIndex.length; i++)
			ftMatr.put(featsByIndex[i], new int[] {0,0,0} );
		for (SequentialPhonic ph : phonesFound)
		{
			char[] featVals = ph.getFeatString().toCharArray(); 
			for (int i = 0 ; i < featsByIndex.length; i++)
			{
				int[] theArr = ftMatr.get(featsByIndex[i]); 
				int thisVal = Integer.parseInt(""+featVals[i]); 
				theArr[thisVal] = theArr[thisVal] + ctxtCts.get(ph); 
				ftMatr.put(featsByIndex[i], theArr); 
			}
		}
		
		String output = ""; 
		
		for (String ft : ftMatr.keySet())
		{
			int[] arr = ftMatr.get(ft); 
			if(arr[0] > (double)total_dist_instances * 0.8 )
				output += "-"+ft+","; 
			else if (arr[1] > (double)total_dist_instances * 0.8)
				output += "."+ft+",";
			else if(arr[2] >  (double) total_dist_instances * 0.8)
				output += "+"+ft+","; 
		}
		
		if (output.length() == 0)	return output; 
		else	return output.substring(0, output.length()-1);
	}
	
	
	private List<Integer> getDistortionLocsInWordPair(int resPhInd, int goldPhInd, SequentialPhonic[][] alignedReps)
	{
		List<Integer> output = new ArrayList<Integer>(); 
		String rTarg = (resPhInd == -1) ? "∅" : resPhInventory[resPhInd].print(),
				gTarg = (goldPhInd == -1) ? "∅" : goldPhInventory[goldPhInd].print(); 
	
		for (int i = 0 ; i < alignedReps.length; i++)
			if (alignedReps[i][0].print().equals(rTarg) && alignedReps[i][1].print().equals(gTarg))
				output.add(i);
		
		return output;
	}
	
	// return list of word pairs with a particular distorition,
	// as indicated by the pairing of the uniquely indexed result phone
	// and the different uniquely indexed gold phone.
	// if either resPhInd or goldPhInd are -1, they are the null phone. 
	private List<LexPhon[]> mismatchesWithDistortion (int resPhInd, int goldPhInd)
	{
		List<LexPhon[]> out = new ArrayList<LexPhon[]>(); 
		boolean is_insert_or_delete = (resPhInd == -1) ||  (goldPhInd == -1); 
		for (int i = 0 ; i < mismatches.size() ; i++)
		{
			if ( is_insert_or_delete)
			{	if(hasMismatch(resPhInd, goldPhInd, mismatches.get(i)[0], mismatches.get(i)[1]))	
				out.add(mismatches.get(i)); 	}
			else if ( mismatches.get(i)[0].findPhone(resPhInventory[resPhInd]) != -1 &&
					mismatches.get(i)[1].findPhone(goldPhInventory[goldPhInd]) != -1)
			{	if(hasMismatch(resPhInd, goldPhInd, mismatches.get(i)[0], mismatches.get(i)[1]))	
				out.add(mismatches.get(i)); 	}
		}
		return out; 
	}
	
	//check if specific mismatch has a specific distortion
	// we assume either the distortion involves a null phone (i.e. it's insertion or deletion)
	// or we have already checked that both phones involve are in fact present in both words
	private boolean hasMismatch(int rphi, int gphi, LexPhon rlex, LexPhon glex)
	{
		SequentialPhonic[][] alignment = getAlignedForms(rlex, glex); 
	
		SequentialPhonic rph = new NullPhone(), gph = new NullPhone(); 
		if (rphi != -1)	rph = resPhInventory[rphi]; 
		if (gphi != -1)	gph = goldPhInventory[gphi]; 
		
		for(int ai = 0 ; ai < alignment.length; ai++)
			if (rph.print().equals(alignment[ai][0].print()))
				if (gph.print().equals(alignment[ai][1].print()))	return true; 
		return false; 
	}
	
	//TODO replace with actual alignment algorithm
	private SequentialPhonic[][] getAlignedForms(LexPhon r, LexPhon g)
	{
		featDist.compute(r, g, 1.0); //TODO may need to change insertion/deletion weight here!
		int[][] align_stipul = featDist.get_min_alignment(); 
		
		int al_len = r.getNumPhones(); 
		for (int a = 0; a < align_stipul.length; a++)
			if (align_stipul[a][0] < 0)	al_len++; 
		
		SequentialPhonic[][] out = new SequentialPhonic[al_len][2]; 
		int ari = 0, agi = 0;
		SequentialPhonic[] rphs = r.getPhOnlySeq(), gphs = g.getPhOnlySeq(); 
		
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
				out[oi][1] = rphs[agi]; agi++; // same for agi == align_stipul[ari]
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
		int num_filled = 1; //since maxLocs[0] = 0 already by default
		while ( num_filled < n && num_filled < arr.length)
		{
			int curr = num_filled; 
			for (int i = 0; i < num_filled ; i++)
			{
				if (arr[maxLocs[i]] < arr[curr])
				{
					int temp = curr; 
					curr = maxLocs[i]; 
					maxLocs[i] = temp; 
				}	
			}
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
					int temp = curr;
					curr = maxLocs[i];
					maxLocs[i] = temp; 
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
		int nrow = 0, num_filled = 1; //since maxLocs[0][0] = 0 already by default

		while (num_filled < n)
		{
			int currCol = num_filled - nrow * arrArr[0].length, currRow = nrow; 
			for (int i = 0; i < num_filled; i++)
			{
				if (arrArr[maxLocs[i][0]][maxLocs[i][1]] < arrArr[currRow][currCol] )
				{
					int tempRow = currRow, tempCol = currCol; 
					currRow = maxLocs[i][0]; currCol = maxLocs[i][1]; 
					maxLocs[i][0] = tempRow; maxLocs[i][1] = tempCol; 
				}
			}
			num_filled++; 
			if (num_filled % arrArr[0].length == 0)	nrow++;
		}
		int currLocCol = num_filled - nrow * arrArr[0].length; 
		int currLocRow = nrow; 
		while (nrow < arrArr.length)
		{
			int currRow = currLocRow, currCol = currLocCol; 
			for (int i = 0; i < n; i++)
			{
				if(arrArr[maxLocs[i][0]][maxLocs[i][1]] < arrArr[currRow][currCol])
				{
					int tempRow = currRow, tempCol = currCol; 
					currRow = maxLocs[i][0]; currCol = maxLocs[i][1]; 
					maxLocs[i][0] = tempRow; maxLocs[i][1] = tempCol; 
				}
			}
			currLocCol++; 
			if (currLocCol % arrArr[0].length == 0)	nrow++; 
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
	
	public double getPercentAccuracy()
	{	return pctAcc;	}
	
	public double getPct1off()
	{	return pct1off;	}
	
	public double getPct2off()
	{	return pct2off;	}
	
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
	
	public void makeAnalysisFile(String fileName, String lexicName, Lexicon lexic)
	{
		String output = "Analysis for "+lexicName+"/n";
		
		System.out.println("Average feature edit distance from gold: "+getAvgFED());
		
		output += "Overall accuracy : "+getPercentAccuracy()+"\n";
		output += "Accuracy within 1 phone: "+getPct1off()+"%\n"; 
		output += "Accuracy within 2 phone: "+getPct2off()+"%\n";
		output += "Average edit distance per from gold phone: "+getAvgPED()+"\n"; 
		output += "Average feature edit distance from gold: "+getAvgFED()+"\n";
		
		output += "Performance associated with each phone in "+lexicName+"\n"; 
		output += "Phone in "+lexicName+"\tAssociated final miss likelihood\t"
				+ "Mean Associated Normalized Lev.Dist.\tMean Associated Feat Edit Dist\n";
		
		HashMap<Phone, Double> missLikByPhone = DerivationSimulation.missLikelihoodPerPhone(lexic);
		HashMap<Phone, Double> avgAssocdLDs = DerivationSimulation.avgLDForWordsWithPhone(lexic); 
		HashMap<Phone, Double> avgAssocdFEDs = DerivationSimulation.avgFEDForWordsWithPhone(lexic);
		Phone[] phonInv = lexic.getPhonemicInventory(); 
		for(Phone ph : phonInv)
			output += ph.print()+"\t|\t"+missLikByPhone.get(ph)+"\t|\t"+avgAssocdLDs.get(ph)
				+ "\t|\t"+avgAssocdFEDs.get(ph)+"\n"; 
		
		writeToFile(fileName, output); 
	}
}
