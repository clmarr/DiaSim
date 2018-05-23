import java.util.HashMap;

//this class does not actually represent any real linguistic feature
//however it is used for various programming purposes
//most importantly, marking the ends and beginnings of words and/or syllables with "pseudo Phones"
//designed so that they can fit into a list of phones for analysis. 
public abstract class PseudoPhone extends SequentialPhonic implements RestrictPhone {
	
	protected String type; 
	
	public String toString()
	{	return print(); 	}
	
	public String getType()	{	return type;	}

	public boolean equals(Object other)
	{
		if(other instanceof PseudoPhone) 
			return (this.toString() == other.toString()); 
		return false; // i.e. not a pseudophone. 
	}
	
	//TODO in future version, possibly properly implement these guys
	public String getFeatString()	{	return null;	}
	public HashMap<String,Integer> getFeatIndices()	{	return null;	}
	public HashMap<String,String> getFeatSymbMap()	{	return null;	}
}
