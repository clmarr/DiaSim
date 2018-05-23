import java.util.List;

/**
 * @author Clayton Marr
 * Abstract parent class for all Shift classes, 
 * 		whose basic role is to intake phonological sequences and output what they will be 
 * 		after the diachronic rule ("Shift") has applied.
 * child classes are differentiated by how their targets are specified
 * ... and how their contexts are specified
 * it is unnecessary to differentiate them by how their destinations are specified because
 * 	(a) for shifts with phone-specified targets, it is most computationally efficient to make 
 * 			those that are constructed with feature-specified targets internally stored with phone
 * 			specified targets, as this is much more computationally efficient. 
 * 	(b) it is very unadvisable to construct instances with feature-specified targets
 * 			but phone-specified destinations, because this would require very frequent searches
 * 			of all phones in use-- very inefficient. 
 * 
 */

public abstract class Shift {
	
	protected boolean pseudosMatter; 
	protected int minPriorSize, minPostSize, minTargSize; 
	
	public abstract List<SequentialPhonic> realize(List<SequentialPhonic> phonologicalSeq);
	
	//override toString with phonological-esque notation for this. 
	public abstract String toString(); 

	protected abstract boolean priorMatch(List<SequentialPhonic> input, int frstTargInd); 
	protected abstract boolean posteriorMatch(List<SequentialPhonic> input, int lastTargInd);  
	
}
