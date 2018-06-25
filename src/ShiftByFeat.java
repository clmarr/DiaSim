import java.util.List;
import java.util.ArrayList;

public abstract class ShiftByFeat extends Shift{

	private List<RestrictPhone> priorContext, postContext; 
	
	//Constructors
	public ShiftByFeat()
	{	pseudosMatter = false; minPriorSize = 0; minPostSize = 0;  	}
	
	public ShiftByFeat(boolean attendToPseudos)
	{	pseudosMatter = attendToPseudos; minPriorSize = 0; minPostSize = 0;	}
	
	public ShiftByFeat(List<RestrictPhone> prior, List<RestrictPhone> post)
	{
		pseudosMatter = false; 
		priorContext = new ArrayList<RestrictPhone>(prior); 
		postContext = new ArrayList<RestrictPhone>(post); 
		minPriorSize = priorContext.size(); minPostSize = postContext.size(); 
	}
	
	public ShiftByFeat(List<RestrictPhone> prior, List<RestrictPhone> post, boolean attendToPseudos)
	{
		pseudosMatter = attendToPseudos; 
		priorContext = new ArrayList<RestrictPhone>(prior); 
		postContext = new ArrayList<RestrictPhone>(post); 
		minPriorSize = priorContext.size(); minPostSize = postContext.size(); 
	}
	
	public abstract List<SequentialPhonic> realize(List<SequentialPhonic> phonologicalSeq); 
	
	@Override
	protected boolean priorMatch (List<SequentialPhonic> input, int frstTargInd)
	{	//auto-true if there is no prior context specification or if there is an option of no prior context
		if (minPriorSize == 0)	return true; 
		
		//abort if impossible
		if (frstTargInd < minPriorSize)	return false;
		
		// check going backward
		int wordInd = frstTargInd - 1, priInd = minPriorSize;  
		
		while(priInd >= 0 && wordInd >= 0)
		{
			SequentialPhonic currObs = input.get(wordInd);
			if(!pseudosMatter && !currObs.getType().equals("phone"))	wordInd--; 
			else
			{
				RestrictPhone currRestr = priorContext.get(priInd); 
				if (!currRestr.compare(currObs))	return false; 
				else	{	wordInd--; priInd--; 	}
			}
		}
		return priInd == -1; 
	}
	
	@Override 
	protected boolean posteriorMatch(List<SequentialPhonic> input, int lastTargInd)
	{
		//auto-true if there is no post context specification or if there is an option of no post context
		if (minPostSize == 0)	return true; 
		
		//abort if impossible 
		if (input.size() - lastTargInd - 1 < minPostSize )	return false; 
		
		//check each possible posterior going forward
		int wordInd = lastTargInd + 1, postInd = 0, inpSize = input.size(); 
		while(postInd < minPostSize && wordInd < inpSize)
		{
			SequentialPhonic currObs = input.get(wordInd); 
			if(!pseudosMatter && !currObs.getType().equals("phone"))	wordInd++; 
			else
			{
				RestrictPhone currRestr = priorContext.get(postInd); 
				if (!currRestr.compare(currObs))	return false; 
				else	{	wordInd++; postInd++;	}
			}
		}
		return postInd == minPostSize; //i.e. the actual post size since for this class the min is the exact size. 
	}
	
	@Override
	// earlier parts, i.e. the target and destination, implemented in subclasses
	public String toString()
	{
		// if there are no context requirements, no need for this method to return anything. 
		if(minPriorSize == 0 && minPostSize == 0)	return ""; 
		
		String output = "| "; 
		if(minPriorSize != 0)
		{
			for(int specPlace = 0; specPlace < minPriorSize; specPlace++)
			{	
				RestrictPhone currSpecs = priorContext.get(specPlace); 
				if(currSpecs.print().equals(" @%@ ")) //i.e. it's a FeatMatrix
					output+=currSpecs.toString(); 
				else //pseudoPhone or proper phone
					output+=currSpecs.print();
			}
			output += " "; 
		}
		
		output += "__";
		
		if(minPostSize != 0)
		{
			for(int specPlace = 0; specPlace < minPostSize; specPlace++ )
			{
				RestrictPhone currSpecs = priorContext.get(specPlace); 
				if(currSpecs.print().equals(" @%@ ")) //i.e. it's a FeatMatrix
					output+=currSpecs.toString(); 
				else //pseudoPhone or proper phone 
					output+=currSpecs.print();
			}
		}
		return output; 
	}
}
