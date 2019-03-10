import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class ErrorAnalysis {
	private int[] levDists; 
	private double[] laks;
	private boolean[] isHit; 
	private double pctAcc, pct1off, pct2off, overallLak; 
	
	private static Phone[] resPhInventory, goldPhInventory; 
	
	private static HashMap<String, Integer> resPhInds, goldPhInds; 
		// indexes for phones in the following int arrays are above.
	private static int[] errorsByResPhone, errorsByGoldPhone; 
	private static double[] errorRateByResPhone, errorRateByGoldPhone; 
	private static int[][] confusionMatrix; 
		// rows -- indexed by resPhInds; columns -- indexed by goldPhInds
	
	private static String[] featsByIndex; 
	
	private static List<LexPhon[]> mismatches; 
	
	private final static int NUM_TOP_ERR_PHS_TO_DISP = 4; 
	
	public ErrorAnalysis(Lexicon theRes, Lexicon theGold, String[] indexedFeats)
	{
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
		laks = new double[NUM_ETYMA]; 
		isHit = new boolean[NUM_ETYMA];
		double totLexQuotients = 0.0, numHits = 0.0, num1off=0.0, num2off=0.0;
		
		for (int i = 0 ; i < NUM_ETYMA ; i++)
		{
			int numPhsGoldWd = getNumPhones(theGold.getByID(i).getPhonologicalRepresentation());
			levDists[i] = levenshteinDistance(theRes.getByID(i), theGold.getByID(i));
			isHit[i] = (levDists[i] == 0); 
			numHits += (levDists[i] == 0) ? 1 : 0; 
			num1off += (levDists[i] <= 1) ? 1 : 0; 
			num2off += (levDists[i] <= 2) ? 1 : 0; 
			double lakation = (double)levDists[i] / (double) numPhsGoldWd; 
			laks[i] = lakation;
			totLexQuotients += lakation; 
			
			if(!isHit[i])
				updateConfusionMatrix(theRes.getByID(i), theGold.getByID(i));
		}
		pctAcc = numHits / (double) NUM_ETYMA; 
		pct1off = num1off / (double) NUM_ETYMA;
		pct2off = num2off / (double) NUM_ETYMA; 
		overallLak = totLexQuotients / (double) NUM_ETYMA; 	
		
		//calculate error rates by phone for each of result and gold sets
		HashMap<SequentialPhonic, Integer> resPhCts = theRes.getPhonemeCounts(), 
				goldPhCts = theGold.getPhonemeCounts(); 
		
		for (int i = 0 ; i < resPhInventory.length; i++)
			errorRateByResPhone[i] = (double)errorsByResPhone[i] 
					/ (double)resPhCts.get(resPhInventory[i]);
		for (int i = 0 ; i < goldPhInventory.length; i++)
			errorRateByGoldPhone[i] = (double)errorsByGoldPhone[i]
					/ (double)goldPhCts.get(goldPhInventory[i]); 
		
		//TODO wehn/where coding wise to call confusionPrognosis()? 
		
		
	}
	
	public static void confusionPrognosis()
	{
		// top n error rates for res and gold
		int[] topErrResPhLocs = arrLocNMax(errorRateByResPhone, NUM_TOP_ERR_PHS_TO_DISP); 
		int[] topErrGoldPhLocs = arrLocNMax(errorRateByGoldPhone, NUM_TOP_ERR_PHS_TO_DISP); 
		
		System.out.println("Result phones most associated with error: ");
		for(int i = 0 ; i < topErrResPhLocs.length; i++)
			System.out.println(""+i+": "+resPhInventory[topErrResPhLocs[i]]+" with rate "+errorRateByResPhone[topErrResPhLocs[i]]);
		System.out.println("Gold phones most associated with error: ");
		for(int i = 0 ; i < topErrGoldPhLocs.length; i++)
			System.out.println(""+i+": "+goldPhInventory[topErrGoldPhLocs[i]]+" with rate "+errorRateByGoldPhone[topErrGoldPhLocs[i]]); 
		
		System.out.println("---\nMost common distortions: "); 
		int[][] topDistortions = arr2dLocNMax(confusionMatrix, 5); 
		for(int i = 0 ; i < topDistortions.length; i++)
		{
			System.out.println(""+i+": "+resPhInventory[topDistortions[i][0]]+" for "
					+goldPhInventory[topDistortions[i][1]]); 
			//parse contexts
			List<String> contextProbs = identifyProblemContextsForDistortion(topDistortions[i][0], topDistortions[i][1]);
			
			//TODO some flag to determine whether we should print this info? 
		}
	}
	
	
	
	//TODO method to use a "pivoting" predictor stage? 
	
	
	//auxiliary: count number of actual Phones in list of SequentialPhonic objects 
	private static int getNumPhones(List<SequentialPhonic> splist)
	{
		int count = 0 ;
		for (SequentialPhonic sp :  splist)
			if(sp.getType().equals("phone"))
				count++; 
		return count; 
	}
	
	//also updates errorsByResPhone and errorsByGoldPhone
	//..and also updates the list mismatches 
	private static void updateConfusionMatrix(LexPhon res, LexPhon gold)
	{
		mismatches.add( new LexPhon[] {res, gold}) ; 
		
		List<SequentialPhonic>[] alignedForms = getAlignedForms(res,gold); 
		// should have the exact same length
		
		for (int i = 0 ; i < alignedForms[0].size(); i++)
		{
			String r = alignedForms[0].get(i).print(), g = alignedForms[1].get(i).print();  
			if (!r.equals(g))
			{
				confusionMatrix[resPhInds.get(r)][goldPhInds.get(g)] += 1;
				errorsByResPhone[resPhInds.get(r)] += 1; 
				errorsByGoldPhone[resPhInds.get(g)] += 1;	
			}
		} 
	}
	
	// report which contexts tend to surround distortion frequently enough that it becomes 
		// suspicious and is worth displaying to the user
	// TODO refine context detection here. 
	private static List<String> identifyProblemContextsForDistortion(int resPhInd, int goldPhInd)
	{
		List<String> out = new ArrayList<String>(); 

		List<LexPhon[]> pairsWithDistortion = mismatchesWithDistortion(resPhInd, goldPhInd); 
		
		// TODO for now we are only using the immediate contexts 
		
		// TODO currently doing this with the wrods in the RESULT forms -- may need to revise this. 
		HashMap<SequentialPhonic, Integer> priorPhoneCounts = new HashMap<SequentialPhonic, Integer>(); 
		HashMap<SequentialPhonic, Integer> posteriorPhoneCounts = new HashMap<SequentialPhonic, Integer>(); 
		
		int total_distortion_instances = 0; 
		
		for (int i = 0 ; i < pairsWithDistortion.size(); i++)
		{
			List<SequentialPhonic>[] alignedReps = 
					getAlignedForms(pairsWithDistortion.get(i)[0], pairsWithDistortion.get(i)[1]); 
			List<Integer> distortLocs = getDistortionLocsInWordPair(resPhInd, goldPhInd, alignedReps); 
			
			total_distortion_instances += distortLocs.size(); 
			
			for (Integer dloc : distortLocs)
			{
				SequentialPhonic priorPh = new Boundary("word bound"); 
				if (dloc > 0)
					priorPh = alignedReps[0].get(dloc - 1); 
				if (priorPhoneCounts.containsKey(priorPh.print()))
					priorPhoneCounts.put(priorPh, priorPhoneCounts.get(priorPh)+1); 
				else
					priorPhoneCounts.put(priorPh, 1);
				
				SequentialPhonic postPh = new Boundary("word bound"); 
				if (dloc < alignedReps[0].size() - 1)
					postPh = alignedReps[0].get(dloc + 1);
				if (posteriorPhoneCounts.containsKey(postPh.print()))
					posteriorPhoneCounts.put(postPh,
							posteriorPhoneCounts.get(postPh) + 1); 
				else
					posteriorPhoneCounts.put(postPh, 1);
				
			}

		}
		
		//now we have the counts for the immediate neighbors -- TODO analyze
		
		//TODO -- if >30% on either side is one particular phone -- report
		for (SequentialPhonic priorPh : priorPhoneCounts.keySet())
		{
			double share = (double)priorPhoneCounts.get(priorPh) / (double)total_distortion_instances;
			if (share > 0.3)
				out.add("Prior context = "+priorPh.print()+", "+share*100.0+"%");
		}
		for (SequentialPhonic postPh : posteriorPhoneCounts.keySet())
		{
			double share = (double)posteriorPhoneCounts.get(postPh) / (double) total_distortion_instances;
			if (share > 0.3)
				out.add("Post context = " +postPh.print()+", "+share*100.0+"%"); 
		}
		
		//TODO -- report any common feature designation shared by 80%+ of either side 
		
		// if word bound is context for more than 50% of the time, report such
		
		//for prior counts...		
		if (priorPhoneCounts.get(new Boundary("word bound")) > (double) total_distortion_instances * 0.5)
			out.add("Majority of instances just after word onset. Common features of remainder: "
					+getContextCommonFeats(priorPhoneCounts, 
							total_distortion_instances - priorPhoneCounts.get(new Boundary("word bound")))); 
		else	out.add("Commmon prior feats: "
					+getContextCommonFeats(priorPhoneCounts, total_distortion_instances)); 
		
		//for posterior counts... 
		if (posteriorPhoneCounts.get(new Boundary("word bound")) > (double) total_distortion_instances * 0.5)
			out.add("Majority of instances just before word coda. Common features of remainder: "
					+getContextCommonFeats(posteriorPhoneCounts, 
							total_distortion_instances - posteriorPhoneCounts.get(new Boundary("word bound")))); 
		else	out.add("Commmon prior feats: "
					+getContextCommonFeats(posteriorPhoneCounts, total_distortion_instances)); 
					
		return out; 
	}
	
	
	//return: HashMap where each feature is key 
	// and value is an int array of [ #+, #-, #. ]
	// where word bounds are concerned, they don't add to ANY of the counts
		// however if the word bound itself is the boundary over 50% of the time, this will be reported in 
		// the method identifyProblemContextsForDistortion
	// @param ctxtName -- i.e. "prior" or "posterior" -- sequential relation to target phone
	private static String getContextCommonFeats(HashMap<SequentialPhonic,Integer> ctxtCts, int total_dist_instances)
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
	
	
	private static List<Integer> getDistortionLocsInWordPair(int resPhInd, int goldPhInd, List<SequentialPhonic>[] alignedReps)
	{
		List<Integer> output = new ArrayList<Integer>(); 
		List<SequentialPhonic> resRep = new ArrayList<SequentialPhonic>(alignedReps[0]),
				goldRep = new ArrayList<SequentialPhonic>(alignedReps[1]); 

		for (int i = 0 ; i < resRep.size(); i++) //size of resRep and goldRep should be equal
			if (resRep.get(i).print().equals(resPhInventory[resPhInd].print()) &&
					goldRep.get(i).print().equals(goldPhInventory[goldPhInd].print()))
				output.add(i); 
		return output;
	}
	
	// return list of word pairs with a particular distorition,
	// as indicated by the pairing of the uniquely indexed result phone
	// and the different uniquely indexed gold phone.
	private static List<LexPhon[]> mismatchesWithDistortion (int resPhInd, int goldPhInd)
	{
		List<LexPhon[]> out = new ArrayList<LexPhon[]>(); 
		for (int i = 0 ; i < mismatches.size() ; i++)
		{
			List<SequentialPhonic>[] alignedReps = 
					getAlignedForms(mismatches.get(i)[0], mismatches.get(i)[1]); 
			List<SequentialPhonic> resRep = new ArrayList<SequentialPhonic>(alignedReps[0]),
					goldRep = new ArrayList<SequentialPhonic>(alignedReps[1]); 
			
			int loc = resRep.indexOf(resPhInventory[resPhInd]); 
			
			while (loc != -1)
			{
				if (goldRep.get(loc).print().equals(goldPhInventory[goldPhInd].print()))
				{
					out.add(mismatches.get(i)); 
					loc = -1; 
				}
				else
				{
					resRep = resRep.subList(i+1, resRep.size()); 
					goldRep = goldRep.subList(i+1, goldRep.size());
					loc = resRep.indexOf(resPhInventory[resPhInd]);
				}
			}
		}
		return out; 
	}
	
	//TODO replace with actual alignment algorithm
	private static List<SequentialPhonic>[] getAlignedForms(LexPhon r, LexPhon g)
	{
		List<SequentialPhonic>[] out = new ArrayList[2]; 
		out[0] = extractPhones(r); out[1] = extractPhones(g); 
		if(out[0].size() > out[1].size())
			while (out[1].size() < out[0].size())
				out[1].add(new NullPhone()); 
		else if (out[0].size() < out[1].size())
			while (out[0].size() < out[1].size())
				out[0].add(new NullPhone()); 
		return out;
	}
	
	//auxiliary -- get only phone objects from a LexPhon
	private static List<SequentialPhonic> extractPhones(LexPhon lp)
	{
		List<SequentialPhonic> sps = lp.getPhonologicalRepresentation(); 
		List<SequentialPhonic> out = new ArrayList<SequentialPhonic>(); 
		for (SequentialPhonic sp : sps)
			if (sp.getType().equals("phone"))	out.add(sp); 
		return out; 
	}
	
	//auxiliary
	//as formulated here : https://people.cs.pitt.edu/~kirk/cs1501/Pruhs/Spring2006/assignments/editdistance/Levenshtein%20Distance.htm
	//under this definition of Levenshtein Edit Distance,
	// substitution has a cost of 1, the same as a single insertion or as a single deletion 
	private static int levenshteinDistance(LexPhon s, LexPhon t)
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

	private static int[] arrLocNMax(double[] arr, int n)
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
	private static int[][] arr2dLocNMax(int[][] arrArr, int n)
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
}
