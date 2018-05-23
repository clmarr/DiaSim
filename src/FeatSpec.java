
/** 
 * class represents a restriction ("specification") on a single feature 
 * two class variable -- featName, string of the name of the feature 
 * 		and truth -- whether it is restricted to be true or restricted to be false
 * 			(we consider it impossible to restrict a feature to be unspecified)
 * @author Clayton Marr
 *
 */
public class FeatSpec {
	private String featName;
	private boolean truth; 
	
	public FeatSpec(String feat, boolean isTrue)
	{	featName = feat; truth = isTrue; }
	
	/** 
	 * @param feat -- simple form of feature specification, ex. "+cor" 
	 * @precondition: feat.charAt(0) == '-' || feat.charAt(0) == '+'
	 */
	public FeatSpec(String feat)
	{
		String indic = feat.substring(0,1);
		assert ("-+".contains(indic))  : 
			"ERROR: first character of feat specification String must be '-' or '+'";
		truth = (indic.equals("+")); 
		featName = feat.substring(1);
	}
	
	public String getFeat()	{	return featName;	}
	public boolean getTruth()	{	return truth; 	}
	
	// true if truth is true and the feature is true for the phone, or if it is false and the feature is false for the phone
	public boolean check(Phone ph)
	{
		return ph.get(featName) == (truth ? 2 : 0); 
	}
	
	// changes phone so that the feature specification represented by this class instance becomes true
	public void forceTruth (Phone ph)
	{
		if(ph.get(featName) != (truth ? 2 : 0)) 
			ph.set(featName, (truth ? 2 : 0));
	}
	
	public String toString()
	{
		return (truth ? '+' : '-') + featName;
	}
	
	public boolean equals(Object other)
	{
		if (other instanceof FeatSpec)	return this.toString().equals(other.toString());
		else return false;
	}
	
}
