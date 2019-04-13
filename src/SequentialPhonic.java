import java.util.HashMap; 

public abstract class SequentialPhonic extends Phonic {
	abstract public String getFeatString(); 
	abstract public HashMap<String, Integer> getFeatIndices(); 
	abstract public HashMap<String, String> getFeatSymbMap(); 

}
