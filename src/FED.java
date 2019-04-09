import java.util.List;
import java.util.ArrayList;



public class FED {

	private static boolean weighted;
	private static double[] weights;
	
	private static int len1, len2;
	
	public static double last_min_dist;
	
	private static int[][] last_min_alignment; 
	
	public FED()
	{	weighted = false;	}
	
	public FED(double[] wts)
	{
		weighted = true;
		weights = wts;
	}
	
	public static void compute(List<SequentialPhonic> seq1, List<SequentialPhonic> seq2, double id_wt)
	{
		SequentialPhonic[] s1 = phonesOnly(seq1), s2 = phonesOnly(seq2);
		len1 = seq1.size(); len2= seq2.size(); 
		
		double[][] matr = new double[len1+1][len2+1];
		String[][] backtraces = new String[len1+1][len2+1]; 
		
		// initialize
		for(int i = 1; i < len1 + 1; i++)
			matr[i][0] = matr[i-1][0] + isdl_cost(s1[i], id_wt);
		for(int j = 1; j < len2 + 1; j++)
			matr[0][j] = matr[0][j-1] + isdl_cost(s2[j], id_wt); 
		
		// dynamic solution
		for(int i = 1; i < len1 + 1 ; i++)
		{	for (int j = 1; j < len2+1; j++)
			{	
				double[] cands = new double[]{matr[i-1][j-1] + subst_cost(s1[i],s2[i]),  
						matr[i-1][j] + isdl_cost(s1[i], id_wt),
								matr[i][j-1] + isdl_cost(s2[j], id_wt)};
				if (cands[0] < cands[1] && cands[0] < cands[2])
				{
					matr[i][j] = cands[0];
					backtraces[i][j] = ""+(i-1)+","+(j-1);
				}
				else if (cands[1] < cands[2])
				{
					matr[i][j] = cands[1];
					backtraces[i][j] = ""+(i-1)+","+j;
				}
				else
				{
					matr[i][j] = cands[2];
					backtraces[i][j] = ""+i+","+(j-1);
				}
			}
		}
		last_min_dist = matr[len1][len2]; 
		
		//backtrace to get the alignment
		last_min_alignment = new int[Math.max(len1,len2)][2]; 
		int ib = len1, jb= len2; 
		
		if (ib != jb)
		{
			if (ib < jb)
				for(int ip = jb; ip > ib; ip--)	last_min_alignment[ip][0] = -2;
			else
				for(int jp= ib; jp > jb; jp--)	last_min_alignment[jp][1] = -2; 
		}
		
		while (ib > 0 && jb > 0)
		{
			int i = Integer.parseInt(backtraces[ib][jb].split(",")[0]),
					j = Integer.parseInt(backtraces[ib][jb].split(",")[1]); 
			if (i == ib - 1 && j == jb - 1)
			{
				last_min_alignment[ib][0] = j; 
				last_min_alignment[jb][1] = i; 
				ib = ib - 1; jb = jb -1;
			}
			else if( i == ib - 1 && j == jb)
				last_min_alignment[jb][1] = -1; 
			else if( i == ib && j == jb - 1)
				last_min_alignment[ib][0] = -1; 
			else
				throw new Error("Error: invalid backtrace");
		}
	}
	
	// minimum feature edit distance
	public double getFED()
	{	return last_min_dist;	}
	
	// returns minimum FED alignment array 
		// whereby each place indicates what hte aligned index of that place in the seq1 (usually res)
		// is for the seq2 (usually gold)
	public int[][] get_min_alignment(List<SequentialPhonic> seq1, List<SequentialPhonic> seq2, double id_wt)
	{
		return last_min_alignment;
	}
	
	// @param(isdl_wt) : insertion/deletion weight
	// TODO no current need to normalized by length o feature vector (i.e. number of features) because this is constant
	private static double isdl_cost(SequentialPhonic sp, double isdl_wt)  
	{
		double sum = 0.0;
		char[] ftvals = sp.getFeatString().toCharArray();
		for(int i = 0; i < ftvals.length; i++)
			sum += (Integer.parseInt(""+ftvals[i]) == DerivationSimulation.UNSPEC_INT) ? isdl_wt / 2 : isdl_wt 
					* (weighted ? weights[i] : 1.0 );
		return sum;
	}
	
	private static double subst_cost(SequentialPhonic p1, SequentialPhonic p2)
	{
		double sum = 0.0; 
		char[] p1fts = p1.getFeatString().toCharArray(), p2fts = p2.getFeatString().toCharArray();
		for(int i = 0; i < p1fts.length; i++)
			sum += Math.abs(Integer.parseInt(p1fts[i]+"") - Integer.parseInt(""+p2fts[i])) / 2 
				* (weighted ? weights[i] : 1.0 );
		return sum;
	}
	
	private static SequentialPhonic[] phonesOnly(List<SequentialPhonic> src)
	{
		List<SequentialPhonic> out = new ArrayList<SequentialPhonic>(); 
		for (SequentialPhonic sp : src)	out.add(sp); 
		SequentialPhonic[] outArr = new SequentialPhonic[out.size()]; 
		for (int i = 0; i < out.size(); i++)	outArr[i] = out.get(i); 
		return outArr;
	}
	
	
}
