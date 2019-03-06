import java.util.List;

public class ErrorAnalysis {
	//TODO define class variables
	
	private int[] levDists; 
	private double[] laks;
	private boolean[] isHit; 
	
	public ErrorAnalysis(Lexicon theRes, Lexicon theGold)
	{
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
		}
		
		//TODO finish here -- calculate phones most common where misses are, etfc
	}
	
	
	//TODO method to use a "pivoting" predictor stage? 
	
	//auxiliary: count number of actual Phones in list of SequentialPhonic objects 
	private static int getNumPhones(List<SequentialPhonic> splist)
	{
		int count = 0 ;
		for (SequentialPhonic sp :  splist)
		{
			if(sp.getType().equals("phone"))
			{
				count++; 
			}
		}
		return count; 
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

}
