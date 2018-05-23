import java.util.ArrayList;
import java.util.List;

public abstract class ShiftByPhone extends Shift {

	protected List<List<SequentialPhonic>> priorContexts, postContexts; 
	//TODO we assume, necessarily, that no priorContext or postContext is contained in another
	// -- if this is not true, there will be problems... 
	
	//Constructors
	// constructor for no context and pseudos not mattering:
	public ShiftByPhone()
	{
		priorContexts = new ArrayList<List<SequentialPhonic>>(); postContexts = new ArrayList<List<SequentialPhonic>>(); 
		pseudosMatter=false;	minPriorSize = 0;	minPostSize = 0; 
	}
	
	public ShiftByPhone(boolean attendToPseudos)
	{
		priorContexts = new ArrayList<List<SequentialPhonic>>(); postContexts = new ArrayList<List<SequentialPhonic>>(); 
		pseudosMatter=attendToPseudos; minPriorSize=0; minPostSize=0;
	}
	
	public ShiftByPhone(List<List<SequentialPhonic>> priors, List<List<SequentialPhonic>> postrs)
	{
		pseudosMatter = false; 
		priorContexts = new ArrayList<List<SequentialPhonic>>(priors);
		postContexts = new ArrayList<List<SequentialPhonic>>(postrs); 
		minPriorSize = generateMinPriorSize(); minPostSize = generateMinPostSize(); 
	}
	
	public ShiftByPhone(List<List<SequentialPhonic>> priors, List<List<SequentialPhonic>> postrs, boolean attendToPseudos)
	{
		pseudosMatter = attendToPseudos; 
		priorContexts = new ArrayList<List<SequentialPhonic>>(priors);
		postContexts = new ArrayList<List<SequentialPhonic>>(postrs); 
		minPriorSize = generateMinPriorSize(); minPostSize = generateMinPostSize(); 
	}
	
	public abstract List<SequentialPhonic> realize(List<SequentialPhonic> phonologicalSeq);
	
	@Override
	protected boolean priorMatch(List<SequentialPhonic> input, int frstTargInd)
	{	//auto-true if there is no prior context specification or if there is an option of no prior context
		if (minPriorSize == 0)	return true; 
		
		//abort if impossible
		if (frstTargInd < minPriorSize)	return false;
		
		//check each possible prior going backward
		int numpcs = priorContexts.size();
		for (int pci = 0; pci < numpcs ; pci++)
			if(isPriorMatch(input, frstTargInd, pci))	return true;
		
		return false;
	}
	
	private boolean isPriorMatch(List<SequentialPhonic> input, int frstTargInd, int priorCtxtInd)
	{
		List<SequentialPhonic> candPrior = priorContexts.get(priorCtxtInd);
		
		//abort if impossible
		if (frstTargInd < candPrior.size())	return false;
		
		//check going backward one at a time, pass pseudos if they don't matter
		int wordInd = frstTargInd - 1, priInd = candPrior.size() - 1; 
		while(priInd >= 0 && wordInd >= 0)
		{
			SequentialPhonic currPh = input.get(wordInd); 
			if(!pseudosMatter && !currPh.getType().equals("phone") )
				wordInd--; 
			else
			{
				if(!currPh.equals(candPrior.get(priInd)))	return false;
				wordInd--; priInd--;
			}
		}
		return priInd == -1;  
	}
	
	@Override
	protected boolean posteriorMatch(List<SequentialPhonic> input, int lastTargInd)
	{	//auto-true if there is no post context specification or if there is an option of no post context
		if (minPostSize == 0)	return true; 
		
		//abort if impossible 
		if (input.size() - lastTargInd - 1 < minPostSize )	return false; 
		
		//check each possible posterior going forward
		int numpscs = postContexts.size(); 
		for (int pci = 0; pci < numpscs ; pci++)
			if(isPostMatch(input, lastTargInd, pci))	return true;
		
		return false;
	}
	
	private boolean isPostMatch(List<SequentialPhonic> input, int lastTargInd, int postCtxtInd)
	{
		List<SequentialPhonic> candPostr = postContexts.get(postCtxtInd);
		int postrSize = candPostr.size(), inpSize = input.size();
		
		//abort if impossible
		if(inpSize - lastTargInd - 1 < postrSize)	return false;
		
		//check going forward at a time, pass pseudos if they don't matter
		int wordInd = lastTargInd + 1, postInd = 0;
		while(postInd < postrSize && wordInd < inpSize)
		{
			SequentialPhonic currPh = input.get(wordInd); 
			if(!pseudosMatter && !currPh.getType().equals("phone"))	wordInd++; 
			else
			{
				if(!currPh.equals(candPostr.get(postInd)))	return false;
				wordInd++; postInd++; 
			}
		}
		return postInd == postrSize; 
	}
	
	protected int generateMinPriorSize()
	{	
		int numprctxts = priorContexts.size(); 
		if (numprctxts == 0)	return 0;
		int min = priorContexts.get(0).size();
		int prci = 1; 
		while (prci < numprctxts)
			min = Math.min(min, priorContexts.get(prci).size());
		return min;
	}

	protected int generateMinPostSize()
	{	
		int numpstctxts = postContexts.size();
		if (numpstctxts == 0)	return 0; 
		int min = postContexts.get(0).size();
		int ptci = 1; 
		while (ptci < numpstctxts)
			min = Math.min(min,  postContexts.get(ptci).size());
		return min;
	}
	
	@Override
	// earlier parts to be implemented in subclasses
	public String toString()
	{
		int numPriConts = priorContexts.size(), numPostConts = postContexts.size();
		if (numPriConts == 0 && numPostConts == 0)	return "";
		
		String output = "|"; 
		if (numPriConts != 0)
		{
			output += " "; 
			if(numPriConts > 1)	output+="{";
			
			List<SequentialPhonic> currCont = priorContexts.get(0); 
			for(SequentialPhonic curPh : currCont)	output += curPh.print(); 
			
			if(numPriConts > 1)
			{
				for(int prici = 1; prici < numPriConts; prici++)
				{
					output+=",";
					currCont = priorContexts.get(prici); 
					for (SequentialPhonic curPh : currCont)
						output += curPh.print(); 
				}
				output += "} ";
			}
			else	output += " ";
		}
		
		output += "__";
		
		if(numPostConts != 0)
		{	
			output += " ";
			
			if(numPostConts > 1) output += "{"; 
			
			List<SequentialPhonic> currCont = postContexts.get(0); 
			for (SequentialPhonic curPh : currCont)
				output += curPh.print(); 
			
			if(numPostConts > 1)
			{
				for (int pstci = 1 ; pstci < numPostConts ; pstci++)
				{
					output+=",";
					for(SequentialPhonic curPh: postContexts.get(pstci))
						output += curPh.print();
				}
				output+="} ";
			}
		}
		
		return output;
	}
}
