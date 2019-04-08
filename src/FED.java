import java.util.List;
import java.util.ArrayList;



public class FED {

	// minimum feature edit distance if features are unweighted
	public static double feat_edit_dist(List<SequentialPhonic> seq1, List<SequentialPhonic> seq2, double id_wt)
	{
		SequentialPhonic[] s1 = phonesOnly(seq1), s2 = phonesOnly(seq2);
		int len1 = seq1.size(), len2= seq2.size(); 
		
		double[][] matr = new double[len1+1][len2+1]; 
		
		// initialize
		for(int i = 1; i < len1 + 1; i++)
			matr[i][0] = matr[i-1][0] + unweighted_isdl_cost(s1[i], id_wt);
		for(int j = 1; j < len2 + 1; j++)
			matr[0][j] = matr[0][j-1] + unweighted_isdl_cost(s2[j], id_wt); 
		
		// dynamic solution
		for(int i = 1; i < len1 + 1 ; i++)
			for (int j = 1; j < len2+1; j++)
				matr[i][j] = Math.min(
						matr[i-1][j-1] + unweighted_subst_cost(s1[i],s2[i]),  
						Math.min( matr[i-1][j] + unweighted_isdl_cost(s1[i], id_wt),
								matr[i][j-1] + unweighted_isdl_cost(s2[j], id_wt) ));
				
		return matr[len1][len2]; 
	}
	
	
	// @param(isdl_wt) : insertion/deletion weight
	// TODO no current need to normalized by length o feature vector (i.e. number of features) because this is constant
	private static double unweighted_isdl_cost(SequentialPhonic sp, double isdl_wt)  
	{
		double sum = 0.0;
		char[] ftvals = sp.getFeatString().toCharArray();
		for(char ft : ftvals)
			sum += (Integer.parseInt(""+ft) == DerivationSimulation.UNSPEC_INT) ? isdl_wt / 2 : isdl_wt;
		return sum;
	}
	
	private static double unweighted_subst_cost(SequentialPhonic p1, SequentialPhonic p2)
	{
		double sum = 0.0; 
		char[] p1fts = p1.getFeatString().toCharArray(), p2fts = p2.getFeatString().toCharArray();
		for(int i = 0; i < p1fts.length; i++)
			sum += Math.abs(Integer.parseInt(p1fts[i]+"") - Integer.parseInt(""+p2fts[i]));
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
