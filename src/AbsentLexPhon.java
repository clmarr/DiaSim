import java.util.List;
import java.util.ArrayList;

/**
 * Class for representing a word that has either not entered the vocabulary yet, or has fallen out of usage.
 * @author Clayton Marr
 * TODO implement this to model words that enter the vocabulary late or leave it early in next project.
 * 	TODO as of Dec 3, 2022 -- unsure if this was already implemented. Need to check. 
 */
public class AbsentLexPhon extends Etymon {
	
	public AbsentLexPhon()
	{	super(new ArrayList<SequentialPhonic>());	}
	
	public List<SequentialPhonic> getPhonologicalRepresentation()	{	return null;	}
	
	public int findPhone(Phone ph)	{	return -1;	}
	
	public boolean applyRule(SChange sch)	{	return false;	}
	
	public String toString()	{	return "[ABSENT]";	}
	
	public String print() {		return "[ABSENT]";	}
	
}
