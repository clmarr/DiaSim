import java.util.HashMap;
import java.util.List;

public interface RestrictPhone{
	public String print(); 
	public boolean compare(List<SequentialPhonic> phonSeg, int ind);
	public boolean compare(SequentialPhonic candPh);
	public boolean comparePreAlpha(SequentialPhonic candPh); 
		// to compare before alpha values are extracted
		// relevant for FeatMatrix instances
		// returns false if non-alpha specified feature stipulations are not true for the candidate Seqph
		// and, if FeatMatrix.DESPEC_VIA_ALPHA is false,
			// also returns false if an alpha specified feature is *unspecified*
		// this method has no handling for alpha-valued features otherwise. 
			// and no handling at all if it is DESPEC_VIA_ALPHA is true. 
	
	public List<SequentialPhonic> forceTruth(List<SequentialPhonic> patientSeq, int ind); 
	
	public void applyAlphaValues(HashMap<String,String> alphVals);
	public HashMap<String,String> extractAndApplyAlphaValues(SequentialPhonic inp);
	public boolean check_for_alpha_conflict(SequentialPhonic inp); 
	//public SequentialPhonic forceTruth(SequentialPhonic patient); 
	public void resetAlphaValues();
	public boolean has_alpha_specs(); 
	public char first_unset_alpha();  // returns '0' if "false" i.e. no unset alphas, otherwise the value of the (first) unset alpha

	public String getFeatVect(); 
}
