public class FED {

	private static boolean weighted;
	private static double[] weights;
	
	private static int len1, len2;
	
	public static double last_min_dist, isdl_wt;
	
	private static int[][] last_min_alignment;
	private static int[][] last_backtrace; 
	private static int n_feats;
	
	private static final boolean CONTEXTUALIZE_ISDL = true; 
	
	public FED (int numFeats, double id_wt)
	{	n_feats = numFeats;
		weighted = false; 
		isdl_wt = id_wt;
	}
	
	public FED(int numFeats, double[] wts, double id_wt)
	{	n_feats = numFeats; 
		weighted = true;
		weights = wts;
		isdl_wt = id_wt;  
	}
	
	public void compute(Etymon l1, Etymon l2)
	{
		SequentialPhonic[] s1 = l1.getPhOnlySeq(), s2 = l2.getPhOnlySeq ();
		len1 = s1.length; len2= s2.length; 
		
		// dynamic programming 
		double[][] matr = new double[len1+1][len2+1];
			/** matr -- each row corresponds to a spot between phones in l1, 
			* 	advancing past it means aligning (or inserting/deleting) that phone
			* likewise each column has the same relationship to its respective phone in l2
			*/ 
		String[][] backtraces = new String[len1+1][len2+1]; 
			//with the same correspondences as in matr
			// each cell contains the coordinates of the previous cell in the path of minimum cost to that cell
					//from the origin
		
		//IMPORTANT: 
		//i-1 in s1/s2 (the SequentialPhonic arrays as declared at the top)
			// corresponds to matrix index i and etc
		
		// initialize
		for(int i = 1; i < len1 + 1; i++)
		{	matr[i][0] = matr[i-1][0] + isdl_cost(s1,i-1);
			backtraces[i][0] = (i-1)+",0";
		}
		for(int j = 1; j < len2 + 1; j++)
		{	matr[0][j] = matr[0][j-1] + isdl_cost(s2,j-1); 
			backtraces[0][j] = "0,"+(j-1); 
		}
		
		// dynamic solution
		for(int i = 1; i < len1 + 1 ; i++)
		{	for (int j = 1; j < len2+1; j++)
			{	
				double[] cands = new double[]{matr[i-1][j-1] + subst_cost(s1[i-1],s2[j-1]),  
						matr[i-1][j] + isdl_cost(s1,i-1),
								matr[i][j-1] + isdl_cost(s2,j-1)};
				if (cands[0] < cands[1] && cands[0] < cands[2]) //alignment i.e. "substitution"
				{
					matr[i][j] = cands[0];
					backtraces[i][j] = ""+(i-1)+","+(j-1); 
				}
				else if (cands[1] < cands[2])	//insertion/deletion
				{
					matr[i][j] = cands[1];
					backtraces[i][j] = ""+(i-1)+","+j;
				}
				else //deletion/insertion
				{
					matr[i][j] = cands[2];
					backtraces[i][j] = ""+i+","+(j-1);
				}
			}
		}
		last_min_dist = matr[len1][len2] / (double)n_feats; 
		
		last_backtrace = last_backtrace_array(backtraces); 
		
		last_min_alignment = new int[Math.max(len1,len2)][2]; 
		
		
		int ib = len1, jb= len2 ; //effectively -1+1 because an extra entry row and col were added
		
		if (ib != jb)
		{
			// reformulate this...
			if (ib < jb)
				for(int ip = jb; ip > ib; ip--)	last_min_alignment[ip-1][0] = -2;
			else
				for(int jp= ib; jp > jb; jp--)	last_min_alignment[jp-1][1] = -2; 
		}
		
		while (ib > 0 && jb > 0)
		{
			int i = Integer.parseInt(backtraces[ib][jb].split(",")[0]), 
					j = Integer.parseInt(backtraces[ib][jb].split(",")[1]);
			// i, j -- coordinates of previous cell on path of least cost. 
			
			if (i == ib - 1 && j == jb - 1)
			{
				last_min_alignment[ib-1][0] = j; 
				last_min_alignment[jb-1][1] = i; 
				ib--; jb--;
			}
			else if( i == ib - 1  && j == jb)
			{
				last_min_alignment[ib-1][0] = -1; 
				ib--;
			}
			else if( i == ib && j == jb - 1 )
			{
				last_min_alignment[jb-1][1] = -1; 
				jb--;
			}
			else
				throw new Error("Error: invalid backtrace");
		}
		
		
		
	}
	
	// minimum feature edit distance
	public double getFED()
	{	return last_min_dist;	}
	
	// returns minimum FED alignment array 
		// whereby each place indicates what the aligned index of that place in the seq1 (usually res)
		// is for the seq2 (usually gold)
		// and vice versa
		// -1 means aligned to null phone
		// -2 means aligned to null phone at word boundary		
	public int[][] get_min_alignment()
	{
		return last_min_alignment;
	}
	
	public int[][] get_last_backtrace()
	{
		return last_backtrace;
	}
	
	// @param(isdl_wt) : insertion/deletion weight
	// TODO no current need to normalized by length of feature vector (i.e. number of features) because this is constant
	private static double isdl_cost(SequentialPhonic[] fullSeq, int loc)  
	{
		return CONTEXTUALIZE_ISDL ? contextualized_isdl_cost(fullSeq,loc) : isdl_cost_default(); 
		
		/** questionable abrogated method below. 
		double sum = 0.0;
		char[] ftvals = sp.getFeatString().toCharArray();
		for(int i = 0; i < ftvals.length; i++)
			sum += (Integer.parseInt(""+ftvals[i]) == DerivationSimulation.UNSPEC_INT) ? isdl_wt / 2 : isdl_wt 
					* (weighted ? weights[i] : 1.0 );
		return sum;**/
	}
	
	private static double isdl_cost_default()
	{	return isdl_wt * n_feats;	}
	
	private static double contextualized_isdl_cost(SequentialPhonic[] fullSeq, int loc)
	{
		double sum = 0.0;
		SequentialPhonic sp = fullSeq[loc];
		int[] spFtVals = new int[n_feats];
		for (int i = 0; i < n_feats; i++)	spFtVals[i] = Integer.parseInt(""+sp.getFeatString().substring(i,i+1));
		if (loc == 0)
		{	for(int i = 0; i < n_feats; i++)	sum += isdl_wt * spFtVals[i]; 	}
		else
		{
			String prFtStr = fullSeq[loc-1].getFeatString();
			for (int i = 0; i < n_feats; i++)	
				sum += isdl_wt * 
					Math.abs(spFtVals[i] - Integer.parseInt(prFtStr.substring(i,i+1)));
		}
		if (loc == fullSeq.length-1)
		{	for(int i = 0; i < n_feats; i++)	sum += isdl_wt * spFtVals[i]; 	}
		else
		{
			String psFtStr = fullSeq[loc+1].getFeatString();
			for (int i = 0; i < n_feats; i++)	
				sum += isdl_wt *
					Math.abs(spFtVals[i] - Integer.parseInt(psFtStr.substring(i,i+1)));
		}
		return sum;
	}
	
	private static double subst_cost(SequentialPhonic p1, SequentialPhonic p2)
	{
		double sum = 0.0; 
		char[] p1fts = p1.getFeatString().toCharArray(), p2fts = p2.getFeatString().toCharArray();
		for(int i = 0; i < p1fts.length; i++)
			sum += (double)Math.abs(Integer.parseInt(p1fts[i]+"") - Integer.parseInt(""+p2fts[i])) / 2.0
				* (weighted ? weights[i] : 1.0 );
		return sum;
	}
	
	
	//auxiliary.
	// backtrace steps are returned "forwards" (i.e. form word onset to word coda)
		// this is actually "backwards" with respect to the actual backtrace though!)
	private int[][] last_backtrace_array(String[][] btm) // btm -- back trace matrix
	{
		int bsteps_left = number_backtrace_steps(btm); 
		int[][] out = new int[bsteps_left][2] ; 
		
		String[] source = btm[btm.length-1][btm[0].length-1].split(","); 
		out[bsteps_left-1][0] = Integer.parseInt(source[0]); 
		out[bsteps_left-1][1] = Integer.parseInt(source[1]); 
		bsteps_left--;
		
		while (bsteps_left > 0)
		{
			source = btm[out[bsteps_left][0]][out[bsteps_left][1]].split(",");
			out[bsteps_left-1][0] = Integer.parseInt(source[0]); 
			out[bsteps_left-1][1] = Integer.parseInt(source[1]); 
			bsteps_left--; 
		}
		return out; 
	}
	
	//auxiliary. 
	private int number_backtrace_steps(String[][] backtraceMatrix)
	{
		int iback = backtraceMatrix.length - 1, jback = backtraceMatrix[0].length - 1; 
		int output = 0;
		while (iback > 0 || jback > 0) 
		{
			String[] source = backtraceMatrix[iback][jback].split(","); 
			iback = Integer.parseInt(source[0]); 
			jback = Integer.parseInt(source[1]); 
			output++;
		}
		return output; 
	}
	
	
	
}
