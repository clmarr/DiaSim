import java.util.HashMap; 
import java.util.List;
import java.util.Set;
import java.util.ArrayList;


// as of May 25, this class is now implementing the RestrictPhone interface, so that Phones can be used along with 
// FeatMatrixes in otherwise feature-specified structures-- in practice the only place it seems to make sense
// to use this capability seems for context specification. 

public class Phone extends SequentialPhonic implements RestrictPhone {

	private String featString; // string of 0s, 1s and 2s -- 0 is negative, 2 positive, 1 unspecified
			// TODO note however in practice we are currently using - negative, + positive, 0 unspecified 
	private HashMap<String, Integer> featIndices; 
	private HashMap<String, String> mapToSymb; // key is feature string, value is ipa symbol. 
	private String symb; 
		
	/**
	 * Constructor
	 * @precondition : featvals.size() = featInds.size()
	 * @precondition : each value, the string of feat values, for symbMap is unique
	 * @note that symbMap is INVERTED before it is stored -- i.e. keys are ipa symbols while values are feature strings
	 */
	public Phone(String featVals, HashMap<String, Integer> featInds, HashMap<String, String> symbMap)
	{
		assert featVals.length() == featInds.size() :
			"ERROR: violated precondition, featVals' size is not the same as featInds hashmap";
		type = "phone";
		featString = featVals; 
		featIndices = new HashMap<String, Integer>(featInds);
		mapToSymb = new HashMap<String, String>(); 
		Set<String> sMKeys = symbMap.keySet(); 
		for ( String key : sMKeys)
		{
			String featdef = symbMap.get(key); 
			assert mapToSymb.containsKey(featdef) == false : 
				"ERROR: duplicate phone definition in symbMap!";
			mapToSymb.put(featdef, key); 
		}
		regenerateSymb(); 
	}
	
	/**
	 * clone constructor
	 */
	public Phone(Phone dolly)
	{
		type = "phone"; 
		featString = dolly.getFeatString();
		featIndices = dolly.getFeatIndices();
		mapToSymb = dolly.getFeatSymbMap(); 
		regenerateSymb(); 
	}
	
	/**
	 * @precondition: input @param dolly is actually a Phone. 
	 */
	public Phone(SequentialPhonic dolly)
	{
		assert dolly.getType().equals("phone"): "Type error in constructing phone clone!"; 
		type="phone";
		featString = dolly.getFeatString();
		featIndices = dolly.getFeatIndices();
		mapToSymb = dolly.getFeatSymbMap(); 
		regenerateSymb(); 
	}
	
	private void regenerateSymb()
	{
		if(mapToSymb.containsKey(featString))	symb = mapToSymb.get(featString);
		else	symb = "?";
	}
	
	// accessors : all return clones of the objects, not the originals. 
	public String getFeatString()	{	return ""+featString;	}
	public HashMap<String,Integer> getFeatIndices()	
	{	return featIndices; 	}
	public HashMap<String,String> getFeatSymbMap()
	{	return mapToSymb;	}
	
	/**
	 * @precondition featExists(featName)
	 * @return 1 if +, -1 if -, else 0 if unspecified 
	 * */
	public int get(String featName)
	{	
		assert featExists(featName): "Violated precondition: featExists(featName) is false! ";
		int index = featIndices.get(featName); 
		return Integer.parseInt(featString.substring(index, index+1)); 	}
	
	/**
	 * @precondition: newVal is between 0 and 2
	 * @precondition: featExists(featName)
	 * */
	public void set(String featName, int newVal)
	{
		assert featExists(featName): "ERROR: tried to set to inexistant feature";
		assert (newVal >= 0 && newVal <= 2) : "ERROR: invalid number for feature";
		int ind = featIndices.get(featName); 
		featString = featString.substring(0, ind) + newVal + featString.substring(ind+1); 
		regenerateSymb();
	}
	
	@Override
	public String print() {
		return symb; 	}
	
	@Override
	public String toString() {
		return symb+": "+featString; 
	}
	
	@Override
	public boolean equals(Object other) {
		if(other instanceof Phone)
			return this.toString().equals(other.toString()); 
		else	return false; 
	} 
	
	@Override
	public boolean compare(SequentialPhonic other) 
	{	return this.equals(other); }
	
	@Override
	public boolean compare(List<SequentialPhonic> phonSeg, int ind)
	{	return this.equals(phonSeg.get(ind)); }
	
	@Override
	public List<SequentialPhonic> forceTruth(List<SequentialPhonic> patientSeq, int ind)
	{	
		List<SequentialPhonic> output = new ArrayList<SequentialPhonic>();
		if(ind > 0)	output.addAll(patientSeq.subList(0, ind)); 
		output.add(new Phone(this)); 
		if (ind < patientSeq.size() - 1)	output.addAll(patientSeq.subList(ind+1, patientSeq.size()));
		
		/**List<SequentialPhonic> output = new ArrayList<SequentialPhonic>(patientSeq); 
		output.set(ind, new Phone(this));*/
		
		return output;
	}
	
	public boolean featExists(String ftName)
	{
		return featIndices.containsKey(ftName); 
	}
}
