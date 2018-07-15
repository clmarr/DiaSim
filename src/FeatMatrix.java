import java.util.Arrays;
import java.util.HashMap;
import java.util.List; 
import java.util.ArrayList;

public class FeatMatrix extends Phonic implements RestrictPhone {
	
	private String featVect; // by default a string of 1s, one for each feature
		// as they become specified they become either 0(neg) or 2(pos)
		// despecification -- i.e. arising only because of feature implications,
			// the change of a feature from +/- to . in unspecified in a phone operated upon. 
		// DESPECIFICATION of phones as part of the FeatMatrix is represented as a 9 in FeatSpecs	
	private final char FEAT_DELIM = ','; 
	private String featSpecs; //"+cor,-dist" etc... separated by FEAT_DELIM 
	private HashMap<String,Integer> featInds; 

	// DESPECIFICATION -- 
		// where due to FEATURE IMPLICATIONS, a feature must be despecified -- i.e. set back to unspecified 
		// example: if a vowel goes from -cont to +cont, the feature delrel should be despecified
		// this case is the only time we will ever make use of the List<String> despecifications
		// which is what is stored in the featVect for this case. 
		// 
	/**
	 * version of constructor with featSpecs passed directly
	 * should be passed with , delimiters and +/- indicators 
	 */
	public FeatMatrix(String specs, HashMap<String,Integer> ftInds)
	{
		assert specs.length() > 1 : "Invalid string entered for specs"; 
		
		type = "feat matrix";
		featSpecs=specs+""; 

		featInds = new HashMap<String,Integer>(ftInds); 
		
		char[] chArr = new char[ftInds.size()];
		Arrays.fill(chArr, '1');
		featVect = new String(chArr); 
		
		String[] spArr = specs.split(""+FEAT_DELIM); 
		
		for (int i = 0; i < spArr.length; i++)
		{	
			//TODO debugging
			System.out.println("spArr["+i+"] = "+spArr[i]);
			
			String sp = spArr[i]; 
			
			String indic = sp.substring(0, 1); 
			assert "-+.".contains(indic) : "ERROR at spec number "+i+": Invalid indicator."; 
			String feat = sp.substring(1); 
			assert featInds.containsKey(feat): "ERROR: tried to add invalid feature";
			
			//TODO debugging
			System.out.println("feat : "+feat);
			
			int spInd= Integer.parseInt(""+featInds.get(feat)); 
			featVect = featVect.substring(0,spInd)+ 
					("+".equals(indic) ? 2 : (".".equals(indic) ? 9 : 0) ) +
					featVect.substring(spInd+1); 
		}
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
			if ("9".contains(restr) && !"1".equals(restr))	return false; 
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
	{	return compare(candPhonSeq.get(index));		}
	
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
			int targVal = (specArr[spi].substring(0, 1).equals("+")) ? 2 : 
				( (specArr[spi].substring(0, 1).equals(".")) ? 1 : 0);
			String feat = specArr[spi].substring(1);
			
			if(output.get(feat) != targVal)// if it is not already the acceptable value: 
				output.set(feat, targVal);
		}	
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
		List<SequentialPhonic> outSeq = new ArrayList<SequentialPhonic>(patientSeq); 
		outSeq.set(ind, forceTruth(new Phone(patient)));
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
		//TODO however it is changed it will be necessary to modify various classes that rely on teh stability of this symbol, 
			// such as SChangeFeat
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof FeatMatrix)	
		{	String othersString = other.toString(); 
			if(othersString.length() < 4)	return false; //no chance
			String[] othersSpecs = othersString.split(""+FEAT_DELIM); 
			String othersVect = ""; 
			for(int ti = 0; ti < this.featVect.length() ; ti++)	othersVect+="1"; //fill with default 1s  
			for(int ki = 0; ki < othersSpecs.length; ki++)
			{
				int othFtInd = featInds.get(othersSpecs[ki].substring(1));
				char othFtSpecVal = othersSpecs[ki].charAt(0); 
				othersVect = othersVect.substring(0, othFtInd) + 
						(othFtSpecVal == '.' ? 9 : (othFtSpecVal == '+' ? 2 : 0)) 
						+ othersVect.substring(othFtInd + 1); 
			}
			return this.getFeatVect().equals(othersVect);
		}
		else	return false; 
	}

}
