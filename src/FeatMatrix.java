import java.util.Arrays;
import java.util.HashMap;
import java.util.List; 
import java.util.ArrayList;

public class FeatMatrix extends Phonic implements RestrictPhone {
	
	private String featVect; // by default a string of 1s, one for each feature
		// as they become specified they become either 0(neg) or 2(pos)
	private String featSpecs; //"+cor,-dist" etc... 
	private HashMap<String,Integer> featInds; 
	private final char FEAT_DELIM = ','; 
	private List<String> despecifications; 
	private boolean hasDespecs; // proxy for if the List<String> despecifications is initialized 
	
	// DESPECIFICATION -- 
		// where due to FEATURE IMPLICATIONS, a feature must be despecified -- i.e. set back to unspecified 
		// example: if a vowel goes from -cont to +cont, the feature delrel should be despecified
		// this case is the only time we will ever make use of the List<String> despecifications
		// which is what is stored in the featVect for this case. 
	
	public FeatMatrix(List<FeatSpec> specs, HashMap<String,Integer> ftInds)
	{
		featInds=new HashMap<String,Integer>(ftInds); 
		featSpecs=""; 
		char[] chArr = new char[ftInds.size()];
		Arrays.fill(chArr, '1' );
		featVect=new String(chArr); 
		for (FeatSpec spec : specs)
		{
			String ft = spec.getFeat();
			assert featInds.containsKey(ft): 	"ERROR: tried to add invalid feature";  
			featSpecs+= specs.toString() + FEAT_DELIM; 
			int spInd = featInds.get(ft); //ind for this feature 
			featVect = featVect.substring(0,spInd) + (spec.getTruth() ? 2 : 0) + featVect.substring(spInd+1); 
		}
		featSpecs = featSpecs.substring(0, featSpecs.length()-1); //chop of last comma
		hasDespecs = false; 
	}
	
	
	public FeatMatrix(List<FeatSpec> specs, HashMap<String,Integer> ftInds, List<String> despecs)
	{
		featInds=new HashMap<String,Integer>(ftInds); 
		featSpecs=""; 
		char[] chArr = new char[ftInds.size()];
		Arrays.fill(chArr, '1' );
		featVect=new String(chArr); 
		for (FeatSpec spec : specs)
		{
			String ft = spec.getFeat();
			assert featInds.containsKey(ft): 	"ERROR: tried to add invalid feature";  
			featSpecs+= specs.toString() + FEAT_DELIM; 
			int spInd = featInds.get(ft); //ind for this feature 
			featVect = featVect.substring(0,spInd) + (spec.getTruth() ? 2 : 0) + featVect.substring(spInd+1); 
		}
		featSpecs = featSpecs.substring(0, featSpecs.length()-1); //chop of last comma
		hasDespecs = true;
		despecifications = new ArrayList<String>(despecs); 
	}
	
	/**
	 * version of constructor with featSpecs passed directly
	 * should be passed with , delimiters and +/- indicators 
	 */
	public FeatMatrix(String specs, HashMap<String,Integer> ftInds)
	{
		assert specs.length() > 1 : "Invalid string entered for specs"; 

		featInds = new HashMap<String,Integer>(ftInds); 
		
		char[] chArr = new char[ftInds.size()];
		Arrays.fill(chArr, '1');
		featVect = new String(chArr); 
		
		String[] spArr = specs.split(""+FEAT_DELIM); 
		
		for (int i = 0; i < spArr.length; i++)
		{
			String sp = spArr[i]; 
			String indic = sp.substring(0, 1); 
			assert "-+.".contains(indic) : "ERROR at spec number "+i+": Invalid indicator."; 
			boolean tr= (indic.equals("+")); 
			String feat = sp.substring(1); 
			assert featInds.containsKey(feat): "ERROR: tried to add invalid feature";
			
			//TODO debugging
			System.out.println("Feat : "+feat); 
			
			int spInd=featInds.get(feat); 
			featVect = featVect.substring(0,spInd)+ (tr ? 2 : 0) +featVect.substring(spInd+1); 
		}
		
		featSpecs=specs; 
		hasDespecs = false;
	}
	
	public FeatMatrix(String specs, HashMap<String,Integer> ftInds, List<String> despecs)
	{
		assert specs.length() > 1 : "Invalid string entered for specs"; 

		featInds = new HashMap<String,Integer>(ftInds); 
		
		char[] chArr = new char[ftInds.size()];
		Arrays.fill(chArr, '1');
		featVect = new String(chArr); 
		
		String[] spArr = specs.split(""+FEAT_DELIM); 
		
		for (int i = 0; i < spArr.length; i++)
		{
			String sp = spArr[i]; 
			String indic = sp.substring(0, 1); 
			assert "-+.".contains(indic) : "ERROR at spec number "+i+": Invalid indicator."; 
			boolean tr= (indic.equals("+")); 
			String feat = sp.substring(1); 
			assert featInds.containsKey(feat): "ERROR: tried to add invalid feature";
			
			//TODO debugging
			System.out.println("Feat : "+feat); 
			
			int spInd=featInds.get(feat); 
			featVect = featVect.substring(0,spInd)+ (tr ? 2 : 0) +featVect.substring(spInd+1); 
		}
		
		featSpecs=specs; 
		hasDespecs = true;
		despecifications = new ArrayList<String>(despecs); 
	}
	
	/**
	 * checks if candidate phone adheres to the restrictiosn
	 */
	public boolean compare(SequentialPhonic cand)
	{
		assert cand.getType().equals("phone"):	"ERROR: comparing to Phonic that is not a Phone"; 
		
		String candFeats = cand.toString().split(": ")[1]; 
		assert candFeats.length() == featVect.length(): 
			"ERROR: comparing with feature vects of unequal length"; 
		for (int i = 0 ; i < candFeats.length(); i++)
		{
			String restr = featVect.substring(i,i+1); 
			if ("02".contains(restr) && !restr.equals(candFeats.substring(i, i+1)))
					return false;
		}
		return true;
	}
	
	/**
	 * @param candPhonSeq -- whole segment we are testing. This is only really necessary because
	 * 		we need to implement this method so this class implements interface RestrictPhone
	 * @param index -- index of interest. See above.
	 * @return
	 */
	public boolean compare(List<SequentialPhonic> candPhonSeq, int index)
	{	
		SequentialPhonic cand = candPhonSeq.get(index); 
		assert cand.getType().equals("phone"):	"ERROR: comparing to Phonic that is not a Phone"; 
		
		String candFeats = cand.toString().split(": ")[1]; 
		assert candFeats.length() == featVect.length(): 
			"ERROR: comparing with feature vects of unequal length"; 
		for (int i = 0 ; i < candFeats.length(); i++)
		{
			String restr = featVect.substring(i,i+1); 
			if ("02".contains(restr) && !restr.equals(candFeats.substring(i, i+1)))
					return false;
		}
		return true;
	}
	
	/**
	 *  makes all the restrictions specified in this FeatMatrix true for @param patient
	 * by changing any necessary feature values in patient 
	 * @precondition: they have the same length feature vectors*/
	public Phone forceTruth(Phone patient)
	{
		Phone output = new Phone(patient); 
		String patFeats = patient.getFeatString();
		assert patFeats.length() == featVect.length():
			"ERROR: trying to forceTruths on phone with different length feat vector";
			// technically it could still function if this wasn't the case, but for security best to call it out anyways
		//iterate over featSpecs, not the feat vector -- easier to change and check them this way, and also fewer iterations
		String[] specArr = featSpecs.split(""+FEAT_DELIM); 
		for (int spi = 0 ; spi < specArr.length; spi++)
		{
			int targVal = (specArr[spi].substring(0, 1).equals("+")) ? 2 : 0; 
			String feat = specArr[spi].substring(1);
			
			if(output.get(feat) != targVal)// if it is not already the acceptable value: 
				output.set(feat, targVal);
		}
		
		if(hasDespecs)
			for(String despec : despecifications)	output.set(despec, 1);
		
		return output; 
	}
	
	/**
	 * 
	 * @param patientSeq
	 * @param ind
	 * @return
	 */
	public List<SequentialPhonic> forceTruth (List<SequentialPhonic> patientSeq, int ind)
	{
		SequentialPhonic patient = patientSeq.get(ind); 
		
		assert patient.getType().equals("phone"): "ERROR: trying to force cand restrictions on non-phone!";
		
		Phone output = new Phone(patient); 
		
		String[] specArr = featSpecs.split(""+FEAT_DELIM);
		for(int spi = 0; spi<specArr.length; spi++)
		{
			int targVal = (specArr[spi].substring(0, 1).equals("+")) ? 2 : 0; 
			String feat = specArr[spi].substring(1); 
			
			if(output.get(feat) != targVal)	output.set(feat, targVal);
		}
		
		if(hasDespecs)
			for(String despec : despecifications)	output.set(despec, 1);
		
		List<SequentialPhonic> outSeq = new ArrayList<SequentialPhonic>(patientSeq); 
		outSeq.set(ind, output);
		return outSeq;
	}
	
	@Override
	public String toString() 
	{	return "["+featSpecs+"]";		}
	
	//TODO currently used for testing only
	public String getFeatVect() 
	{	return featVect; 	}
	
	@Override
	public String print() {
		return " @%@ "; //this can be changed for stylistic purposes as long as it is unique with respect to the print outputs of parallel classes
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof FeatMatrix)	
		{	String othersString = other.toString(); 
			if(othersString.length() < 4)	return false; //no chance
			String[] kushtiITij = othersString.split(""+FEAT_DELIM); 
			String vektorITij = ""; 
			for(int ti = 0; ti < this.featVect.length() ; ti++)	vektorITij+="1"; 
			for(int ki = 0; ki < kushtiITij.length; ki++)
			{
				int indeksTipari = featInds.get(kushtiITij[ki].substring(1));
				boolean ndershmeri = kushtiITij[ki].charAt(0) == '+'; 
				vektorITij = vektorITij.substring(0, indeksTipari) + 
						(ndershmeri ? 2 : 0 ) + vektorITij.substring(indeksTipari + 1); 
			}
			return this.getFeatVect().equals(vektorITij);
		}
		else	return false; 
	}

}
