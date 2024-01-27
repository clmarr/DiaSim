public abstract class SequentialPhonic extends Phonic {
	abstract public String getFeatString(); 
	abstract public SequentialPhonic copy(); 
	public boolean equals(Object other)
	{
		if (!other.getClass().equals(this.getClass()))	return false;
		return other.toString().equals(this.toString()); 
	}

}
