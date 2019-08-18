import java.util.HashMap; 

public abstract class SequentialPhonic extends Phonic {
	abstract public String getFeatString(); 
	abstract public HashMap<String, Integer> getFeatIndices(); 
	abstract public HashMap<String, String> getFeatSymbMap(); 
	abstract public SequentialPhonic copy(); 
	public boolean equals(Object other)
	{
		if (!other.getClass().equals(this.getClass()))	return false;
		return other.toString().equals(this.toString()); 
	}

}
