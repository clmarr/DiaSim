import java.util.List; 
import java.util.ArrayList;

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
	
	public List<SequentialPhonic> forceTruth(List<SequentialPhonic> patSeq, int ind)
	{
		List<SequentialPhonic> output = new ArrayList<SequentialPhonic>(patSeq); 
		output.remove(ind); 
		return output; 
	}
	
}
