
// Master class for all things that can hold the place of a phone(me) in a word -- to include pseudoPhones
public abstract class Phonic {
	protected String type; 
	
	public String getType()	{	return type;	}
	
	abstract public String print();
	abstract public boolean equals (Object other);
}
