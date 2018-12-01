import java.util.List;
import java.util.ArrayList;
import java.util.HashMap; 

/** SChangeSeqToSeq
 * maps a List<RestrictPhone> to another List<RestrictPhone> strictly of the same length -- the target source and the dest specs
 * indices must strictly align
 * for insertion use a null phone in the target source
 * for any deletion, use a null phone in the  destination
 * @author Clayton Marr
 *
 */

public class SChangeSeqToSeq  extends SChange
{
	private List<RestrictPhone> targSource, destSpecs;
	private int targSeqSize; 
	private HashMap<String,Integer> featInds; 
	private HashMap<String,String> symbMap; 
	
	
	private void initialize(HashMap<String,Integer> ftInds, HashMap<String,String> symb_map, List<RestrictPhone> trgsrc, List<RestrictPhone> dstspcs)
	{
		assert trgsrc.size() == dstspcs.size(): "Error: lengths off lists entered into SChangeSeqToSeq are not the same length.";
		
		targSource = new ArrayList<RestrictPhone>(trgsrc);
		targSeqSize = targSource.size();
		minTargSize = generateTrueSize(targSource); 
		destSpecs = new ArrayList<RestrictPhone>(dstspcs);
		symbMap = new HashMap<String,String>(symb_map);
		featInds = new HashMap<String,Integer>(ftInds); 
		
	}
	
	public SChangeSeqToSeq(HashMap<String, Integer> ftInds, HashMap<String,String> symb_map, List<RestrictPhone> trgsrc, List<RestrictPhone> dstSpcs)
	{
		super(true); initialize(ftInds, symb_map, trgsrc, dstSpcs); 
	}
	
	public SChangeSeqToSeq(HashMap<String, Integer> ftInds, HashMap<String,String> symb_map,  List<RestrictPhone> trgsrc, List<RestrictPhone> dstSpcs,
			SChangeContext prior, SChangeContext postr)
	{	super(prior,postr, true); initialize(ftInds, symb_map, trgsrc, dstSpcs); }
	
	//Realization
	public List<SequentialPhonic> realize (List<SequentialPhonic> input)
	{
		int inpSize = input.size(); 
		//abort if too small
		if(inpSize < minPriorSize + minTargSize + minPostSize)	return input; 
		
		int p = minPriorSize , 
				maxPlace = inpSize - Math.min(minPostSize + minTargSize, 1); 
		List<SequentialPhonic> res = (p == 0) ? 
				new ArrayList<SequentialPhonic>() : new ArrayList<SequentialPhonic>(input.subList(0, p));
		
		/** check if target with correct context occurs at each index 
		 * We iterate from the beginning to the end of the word
		 * We acknowledge the decision to iterate this way (progressively) is arbitrary,
		 * and that it might not be technically correct for a restricted set of cases (some changes may occur "regressively" within the word)
		 * change if it becomes necessary
		 */
		while (p < maxPlace)
		{
			if (priorMatch(input, p))
			{
				int candIndAfter = firstIndAfterMatchedWindow(input, inpSize, p);
				if(candIndAfter != -1)
				{
					res.addAll(generateResult(input,p));
					p = candIndAfter;
				}
				else
				{	res.add(input.get(p)); p++;	}
			}
			else
			{	res.add(input.get(p)); p++; }
		}
		if ( p < inpSize )
			res.addAll(input.subList(p, inpSize));
		return res;
	}
	
	//i.e. counting null phones in the list as 0.
	private int generateTrueSize(List<RestrictPhone> seq)
	{
		int count = 0;
		for (RestrictPhone si : seq)
			if (!si.compare(new NullPhone()))	count++;
		return count;
	}
	
	private List<SequentialPhonic> generateResult(List<SequentialPhonic> input, int firstInd)
	{
		List<SequentialPhonic> output = new ArrayList<SequentialPhonic>();
		int checkInd = firstInd, targInd = 0 ;
		while ( targInd < targSeqSize )
		{
			if(targSource.get(targInd).print().equals("∅")) // a null phone -- must correspond to a proper Phone
			{
				String theSpecs = symbMap.get(destSpecs.get(targInd).print());
				output.add(new Phone(theSpecs, featInds, symbMap));
			}
			else
			{
				RestrictPhone thisDest = destSpecs.get(targInd); 
				if(!thisDest.compare(new NullPhone()))
						output.add( destSpecs.get(targInd).forceTruth(input, checkInd).get(checkInd));
				checkInd++; 
			}
			targInd++; 
		}
		return output;
	}
	
	public int firstIndAfterMatchedWindow(List<SequentialPhonic>input, int inpSize, int firstInd)
	{
		//abort if the starting index is obviously invalid: 
		if(firstInd >= inpSize)
			throw new Error("Error: tried to match with firstInd after word ends!");
		
		int checkInd = firstInd, targInd = 0 ;
		while(targInd < targSeqSize && checkInd < inpSize)
		{
			if(targSource.get(targInd).print().equals("∅")) // a null phone
				targInd++; 
			else
			{
				SequentialPhonic currObs = input.get(checkInd); 
				if( !targSource.get(targInd).compare(currObs))	return -1;
				else
				{	checkInd++; targInd++; 	}
			}
		}
		if(targInd == targSeqSize)
			return posteriorMatch(input, checkInd) ? checkInd : -1; 
		else return -1;
	}

	//@Override
	public String toString()
	{
		String output = "";
		for (RestrictPhone targRPh : targSource)
			output += (targRPh.getClass().toString().contains("FeatMatrix")) ? targRPh+" " : targRPh.print() + " ";
		output += "> "; 
		
		for (RestrictPhone destRPh : destSpecs)
			output += (destRPh.getClass().toString().contains("FeatMatrix")) ? destRPh +" " : destRPh.print() + " ";
		
		return output.trim() + super.toString();
	}
	
}
