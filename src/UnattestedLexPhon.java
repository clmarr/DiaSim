import java.util.List;
import java.util.ArrayList;

/**
 * Class for representing a word that has either not entered the vocabulary yet, or has fallen out of usage.
 * @author Clayton Marr
 */
public class UnattestedLexPhon extends LexPhon {
	
	public UnattestedLexPhon()
	{	super(new ArrayList<SequentialPhonic>());	}
	
	public List<SequentialPhonic> getPhonologicalRepresentation()	{	return null;	}
	
	public int findPhone(Phone ph)	{	return -1;	}
	
	public boolean applyRule(SChange sch)	{	return false;	}
	
	public String toString()	{	return UTILS.UNATTESTED_REPR;	}
	
	public String print() {	return UTILS.UNATTESTED_REPR;	}
	
}
