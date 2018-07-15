import java.util.List;

public class Boundary extends PseudoPhone {
	
	public Boundary (String t)
	{	type = t;	}
	
	public String print() 
	{
		if (type.equals("non word bound"))	return "@";
		if (type.equals("word bound"))	return "#"; 
		if (type.equals("morph bound"))	return "+";
		return "%DEFAULTBOUND%"; // default -- should never be seen. 
	}
	
	public boolean compare(List<SequentialPhonic> phonSeq, int ind)
	{	
		if(type.equals("non word bound"))
			return phonSeq.get(ind).getType().equals("word bound") == false; 
		return phonSeq.get(ind).getType().equals(type); 	
	}

	public boolean compare(SequentialPhonic cand) 
	{	if(type.equals("non word bound"))
			return cand.getType().equals("word bound") == false; 
		return cand.getType().equals(type);	
	}
	
	//if the unit at the specified index ind is the same as this, do nothing
	// otherwise, throw an error -- the method forceTruth should not be used to insert
	// pseudoPhones into a segment, as this is a phonological program not a morphological one.
	// TODO may need to change this if the model is extended to morphology, or if 
	// we decide to ultimately include syllable boundaries in the model. 
	public List<SequentialPhonic> forceTruth (List<SequentialPhonic> patSeq, int ind)
	{
		if (!patSeq.get(ind).equals(this))		throw new Error("Error: tried to use Boundary to forceTruth on a non-equivalent unit.");
		else return patSeq; 
	}
}