import java.util.List; 
import java.util.ArrayList;
import java.util.HashMap;

/**nullPhone class
 * note that currently this class is abrogated for most if not all usages. 
 */

public class NullPhone extends PseudoPhone {
	
	public NullPhone()
	{
		type = "null phone";
	}
	
	public String print()	{	return "∅"; 	}

	public boolean compare(List<SequentialPhonic> phonSeq, int ind)
	{	return true;	} // because there are "always" infinite null phones everywhere
	//TODO might need to make this one throw an error if it is causing errors. 
	
	//see above. 
	public boolean compare(SequentialPhonic phon)	{	return true;	}
	
	public boolean comparePreAlpha (SequentialPhonic phon)
	{	return compare(phon);	}
	
	public List<SequentialPhonic> forceTruth(List<SequentialPhonic> patSeq, int ind)
	{
		List<SequentialPhonic> output = new ArrayList<SequentialPhonic>(patSeq); 
		output.remove(ind); 
		return output; 
	}
	
	public SequentialPhonic copy()
	{	return new NullPhone();	}
	
	@Override
	public HashMap<String,String> extractAndApplyAlphaValues(SequentialPhonic inp)
	{
		return null;
	}
	
	@Override
	public boolean check_for_alpha_conflict(SequentialPhonic inp)
	{	return false;	}
	
	@Override
	public void applyAlphaValues(HashMap<String, String> alphVals)
	{	/* do nothing*/	}
	
	@Override
	public void resetAlphaValues()
	{	/* do nothing*/	}

	@Override
	public boolean has_alpha_specs()	{	return false;	}
	

	@Override
	public char first_unset_alpha()	{	return '0';	}
}
