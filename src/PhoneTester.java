import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List; 
import java.util.ArrayList;
import java.util.Arrays;

/** PhoneTester -- class for testing the functionality of the classes Phone and FeatMatrix
 * @date 8 June 2018
 * @author Clayton Marr
 *
 */

public class PhoneTester {
	
	public static void main(String args[]) throws IOException
	{	
		System.out.println("Initiating test of class Phone");
		
		HashMap<String, String> symbsToFeatures = new HashMap<String, String>(); 
		HashMap<String, Integer> featureIndices = new HashMap<String, Integer>(); 
		HashMap<String, String[]> featImplications = new HashMap<String, String[]>();
		String[] feats;
		String firstFeat = "", nextLine; 
		List<String> lines = new ArrayList<String>(); 
		
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream("symbolDefs.csv"), "UTF-8")); 
			while((nextLine = in.readLine()) != null)	lines.add(nextLine); 		
			in.close(); 
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("Encoding unsupported!");
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
			e.printStackTrace();
		}
		
		// feats -- feature names as defined in input file 
		feats = lines.get(0).replace("SYMB,","").split(","); 
		firstFeat = feats[0];
		
		System.out.println("feats[0] = "+feats[0]);
		
		for(int fi = 0; fi < feats.length; fi++)	featureIndices.put(feats[fi], fi); 

		int li = 1; 
		while (li < lines.size()) 
		{
			nextLine = lines.get(li).replaceAll("\\s+", ""); //strip white space and invisible characters 
			int ind1stComma = nextLine.indexOf(UTILS.FEAT_DELIM); 
			String symb = nextLine.substring(0, ind1stComma); 
			String[] featVals = nextLine.substring(ind1stComma+1).split(""+UTILS.FEAT_DELIM); 		
			
			String intFeatVals = ""; 
			for(int fvi = 0; fvi < featVals.length; fvi++)
			{
				if(featVals[fvi].equals(""+UTILS.MARK_POS))	intFeatVals+= UTILS.POS_INT; 
				else if (featVals[fvi].equals(""+UTILS.MARK_UNSPEC))	intFeatVals += UTILS.UNSPEC_INT; 
				else if (featVals[fvi].equals(""+UTILS.MARK_NEG))	intFeatVals += UTILS.NEG_INT; 
				else	throw new Error("Error: unrecognized feature value ");
			}
			
			symbsToFeatures.put(symb, intFeatVals);
			li++; 
		}
		
		lines = new ArrayList<String>(); 
		try
		{
			BufferedReader in =  new BufferedReader ( new InputStreamReader (
				new FileInputStream("FeatImplications"), "UTF-8")); 
			while((nextLine = in.readLine()) != null)	lines.add(nextLine); 		
			in.close(); 
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("Encoding unsupported!");
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
			e.printStackTrace();
		}
		
		for(String line : lines)
		{
			String[] sides = line.split(""+UTILS.IMPLICATION_DELIM); 
			featImplications.put(sides[0], sides[1].split(""+UTILS.FEAT_DELIM)); 
		}
		
		System.out.println("feats[0] = "+feats[0]);
		System.out.println("firstFeat = "+firstFeat);
		
		//test with /p/ -- this is almost certainly represents the same phone in every paradigm
		Phone testPhone = new Phone(symbsToFeatures.get("p"), featureIndices, symbsToFeatures); 
		System.out.println("(testing Phone default constructor method with IPA symbol provided)");
		System.out.println("Following output should be 'phone'");
		System.out.println(testPhone.getType());
		System.out.println("Following output should be 'p'");
		System.out.println(testPhone.print());
		System.out.println("(Testing phone's feature vector via its toString method)\n"
				+ "Is the following correctly 'p:"+symbsToFeatures.get("p")+"'?");
		if(testPhone.toString().equals("p:"+symbsToFeatures.get("p")))
			System.out.println("Yes."); 
		else	System.out.println("No, it is "+testPhone); 
		
		System.out.println("(Additionally testing Phone.equals(Phone))");
		System.out.println("Following output should be 'true'");
		System.out.println(testPhone.equals(new Phone(symbsToFeatures.get("p"), 
				featureIndices, symbsToFeatures))); 
		
		System.out.println("Following output should be 'false'"); 
		Phone badProxyP = new Phone(symbsToFeatures.get("p"), featureIndices, symbsToFeatures); 
		String badProxyFeats = badProxyP.getFeatString(); 
		String featIndices = testPhone.getFeatString(); 
		// recall that testPhone is in fact /p/ -- so we are simply zeroing a single featureb below. 
		int randFeatIndex = (int)(Math.random() * testPhone.getFeatIndices().size()); 
		badProxyFeats = badProxyFeats.substring(0, randFeatIndex) 
				+ ((featIndices.charAt(randFeatIndex) == UTILS.POS_INT) ? UTILS.NEG_INT : UTILS.POS_INT) 
				+ badProxyFeats.substring(randFeatIndex+1); 
		System.out.println(testPhone.equals(new Phone(badProxyFeats, featureIndices, symbsToFeatures))); 
		
		System.out.println("Following output should be 'false'"); 
		System.out.println(testPhone.equals("p: "+featIndices));
		
		SequentialPhonic proxyP = new Phone(featIndices, featureIndices, symbsToFeatures);
		System.out.println("Following output should be 'true'");
		System.out.println(testPhone.equals(proxyP));
		
		SequentialPhonic proxy2 = new Boundary("word bound"); 
		List<SequentialPhonic> listylist = new ArrayList<SequentialPhonic>(); 
		listylist.add(proxy2);
		listylist = testPhone.forceTruth(listylist, 0);
		System.out.println("(testing Phone.forceTruth(SequentialPhonic))\nThe following should be 'true'"); 
		System.out.println(testPhone.equals(listylist.get(0)));
		
		System.out.println("The following should be '"+symbsToFeatures.get("p").charAt(0)+"'  ");
		System.out.println(""+testPhone.get(firstFeat)); 
		
		System.out.println("The following should be true ");
		System.out.println(testPhone.featExists(firstFeat));
		
		Phone proxy3 = new Phone(testPhone); 
		System.out.println("The following should be true (testing Phone.compare(Phone) and new Phone(Phone))");
		System.out.println(testPhone.compare(proxy3));
		
		SequentialPhonic proxy4 = new Phone(testPhone); 
		Phone proxy5 = new Phone(proxy4); 
		System.out.println("The following should be true (testing Phone.compare(Phone) and new Phone(SequentialPhonic)) ");
		System.out.println(testPhone.compare(proxy5));
		
		System.out.println("----------------------");
		
		//TODO will need to add in many more testing statements 
				// if feature implications and feature translations are ever implemented
				// ... in order to ensure that these are working properly 
				// TODO in practice these worked with the current featImplications.txt file at least for BaseCLEF, DiaCLEF etc... but need to check for general case!
				// unless these are already tested in effect in one of the other debugger classes.
		
		System.out.println("Testing class FeatMatrix"); 
		System.out.println("The following should be 'true'"); 
		System.out.println((new FeatMatrix("+lab,-cont,-delrel", Arrays.asList(feats), featImplications)).compare(testPhone));
		
		FeatMatrix voicedStop = new FeatMatrix("-cont,-delrel,+voi", Arrays.asList(feats), featImplications); 
		System.out.println("The following should be 'false' (testing FeatMatrix.compare(Phone))");
		System.out.println(voicedStop.compare(testPhone)); 
		
		//testing whether featVect is stored properly in the FeatMatrix object instance 
		String agreements = ""; 
		for(int i = 0; i < feats.length; i++)	agreements += "1";
		
		String[] feature_stipulations = new String[]{"-cont","-delrel","+voi"}; 
			
		// now we give feature_stipulations the feature vector a voiced stop should have... 
		for(int fsi = 0; fsi < feature_stipulations.length; fsi++)
		{
			String curr_stip = feature_stipulations[fsi]; 
			String curr_feat = curr_stip.substring(1); 
			boolean positivity = curr_stip.charAt(0) == '+'; 
			int featLoc = featureIndices.get(curr_feat);
			agreements = agreements.substring(0, featLoc) + 
					(positivity ? 2 : 0) + agreements.substring(featLoc+1); 
		}
		
		String currfeatvect = voicedStop.getFeatVect(); 
		System.out.println("Is the feature vector correctly '"+agreements+"'?");
		if(currfeatvect.equals(agreements))		System.out.println("Yes."); 
		else	System.out.println("no, it is instead... "+currfeatvect); 
		
		System.out.println("The following should be 'b' (testing FeatMatrix.forceTruth(phone))"); // recall the testPhone is currently [b].
		System.out.println(voicedStop.forceTruth(testPhone).print()); 
		
		testPhone=voicedStop.forceTruth(testPhone); 
		System.out.println("The following should be 'true' (testing FeatMatrix.forceTruth(phone))"); 
		System.out.println(voicedStop.compare(testPhone));
		
		List<SequentialPhonic> testPhones = new ArrayList<SequentialPhonic>(); 
		testPhones.add(testPhone); 
		testPhones.add(testPhone); 
		
		FeatMatrix nasalStop = new FeatMatrix("+nas,+cont,+son,0delrel", Arrays.asList(feats), featImplications); 

		// now, we test the feature vector of FeatMatrix in a case where DESPECIFICATION has occurred. 
		feature_stipulations = new String[]{"+nas","+cont","+son","0delrel"}; 
		for(int i = 0; i < feats.length; i++)	if (agreements.charAt(i) != '1')	agreements = agreements.substring(0,i)+"1"+agreements.substring(i+1); 
		
		for(int fsi = 0; fsi < feature_stipulations.length; fsi++)
		{
			String curr_stip = feature_stipulations[fsi]; 
			String curr_feat = curr_stip.substring(1); 
			char stip_val = curr_stip.charAt(0); 
			int featLoc = featureIndices.get(curr_feat);
			agreements = agreements.substring(0, featLoc) + 
					(stip_val == '+' ? 2 : (stip_val == '-' ? 0 : 9)) 
					+ agreements.substring(featLoc+1); 
		}
		
		currfeatvect = nasalStop.getFeatVect(); 
		System.out.println("Is the feature vector correctly '"+agreements+"'?");
		if(currfeatvect.equals(agreements))		System.out.println("Yes."); 
		else	System.out.println("no, it is instead... "+currfeatvect); 
		
		System.out.println("The following should be 'false'"); 
		System.out.println(nasalStop.compare(testPhones, 0));
		
		testPhones= nasalStop.forceTruth(testPhones, 0); 
		System.out.println("The following should be 'true'"); 
		nasalStop = new FeatMatrix("+nas,+cont,+son", Arrays.asList(feats), featImplications); 

		System.out.println(nasalStop.compare(testPhones, 0));
		
		System.out.println("The following should be 'm'");
		System.out.println(testPhones.get(0).print()); 
		
		System.out.println("----------------------");
		
		System.out.println("Now testing functionality of alpha feature handling within FeatMatrix...");
		
		System.out.println("(single alpha feature, no feature implications in play.") ;
		System.out.println("(testing FeatMatrix constructor in this condition)"); 
		FeatMatrix laxv_alphahigh = new FeatMatrix("-tense,βhi", Arrays.asList(feats), featImplications); 
		
		System.out.println("Is the local alphabet correctly 'β'?"); 
		if (laxv_alphahigh.getLocalAlphabet().equals("β"))	System.out.println("Yes.");
		else	System.out.println("No, instead it isː "+laxv_alphahigh.getLocalAlphabet()); 
		
		System.out.println("Was the alpha feature's presence correctly detected?"); 
		System.out.println( laxv_alphahigh.has_alpha_specs() ? "Yes." : "No.") ; 
		
		System.out.println("Were multiple alpha features (incorrectly) detected?"); 
		System.out.println( laxv_alphahigh.has_multispec_alph()? "Yes." : "No.") ; 
		
		// use alpha-hi  with u and o 
		
		
		
		
		System.out.println("(single alpha feature, with a feature implication in play...)") ; 
		
		// TODO expansion to handle FeatMatrix's alpha features. 
	}
}
