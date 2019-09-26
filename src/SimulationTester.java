import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * for ensuring functionality of DHSAnalysis, ErrorAnalysis, FED, Simulation, and related classes, ultimately DiachronicSimulator as well. 
 * @author Clayton Marr
 *
 */

public class SimulationTester {

	private static final String SYMBS_LOC = "symbolDefs.csv";
	private static final String LEX_LOC = "DebugDummyLexicon.txt"; 
	private static final String CASC_LOC = "DumCasc"; 
	private static final String FI_LOC = "FeatImplications"; 
	private static double ID_WT = 0.5; 
	
	private static String[] featsByIndex; 
	private static HashMap<String, Integer> featIndices;

	private static boolean feats_weighted;
	private static double[] FT_WTS; 
	
	private static HashMap<String, String> phoneSymbToFeatsMap;

	public static void main(String args[])
	{
		extractSymbDefs(); 
		String nextLine; 
		List<String> featImplLines = new ArrayList<String>(); 
		
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream(FI_LOC), "UTF-8")); 
			while((nextLine = in.readLine()) != null)	featImplLines.add(nextLine); 		
			in.close(); 
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("Encoding unsupported!");
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO Exception!");
			e.printStackTrace();
		}
		
		for(String filine : featImplLines)
		{
			String[] fisides = filine.split(""+DiachronicSimulator.IMPLICATION_DELIM); 
			DiachronicSimulator.featImplications.put(fisides[0], fisides[1].split(""+DiachronicSimulator.FEAT_DELIM));
		}
	}
	
	
	
	private static void extractSymbDefs()
	{
		System.out.println("Collecting symbol definitions...");
		
		List<String> symbDefsLines = new ArrayList<String>();
		String nextLine; 
		
		try 
		{	File inFile = new File(SYMBS_LOC); 
			BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream(inFile), "UTF8")); 
			while((nextLine = in.readLine()) != null)	
				symbDefsLines.add(nextLine); 		
			
			in.close(); 
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("Encoding unsupported!");
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO Exception!");
			e.printStackTrace();
		}
		
		System.out.println("Symbol definitions extracted!");
		System.out.println("Length of symbDefsLines : "+symbDefsLines.size()); 
		

		//from the first line, extract the feature list and then the features for each symbol.
		featsByIndex = symbDefsLines.get(0).replace("SYMB,", "").split(""+DiachronicSimulator.FEAT_DELIM); 
		
		for(int fi = 0; fi < featsByIndex.length; fi++) featIndices.put(featsByIndex[fi], fi);
		
		//Now we check if the features have weights
		if (symbDefsLines.get(1).split(",")[0].equalsIgnoreCase("FEATURE_WEIGHTS"))
		{
			feats_weighted = true; 
			FT_WTS = new double[featsByIndex.length];
			String[] weightsByIndex = symbDefsLines.get(1).substring(symbDefsLines.get(1).indexOf(",")+1).split(",");
			for(int i = 0 ; i < featsByIndex.length; i++)
				FT_WTS[i] = Double.parseDouble(weightsByIndex[i]); 
			symbDefsLines.remove(1); 
		}	
		else	feats_weighted = false;
		
		//from the rest-- extract the symbol def each represents
		int li = 1; 
		while (li < symbDefsLines.size()) 
		{
			nextLine = symbDefsLines.get(li).replaceAll("\\s+", ""); //strip white space and invisible characters 
			int ind1stComma = nextLine.indexOf(DiachronicSimulator.FEAT_DELIM); 
			String symb = nextLine.substring(0, ind1stComma); 
			String[] featVals = nextLine.substring(ind1stComma+1).split(""+DiachronicSimulator.FEAT_DELIM); 		
			
			String intFeatVals = ""; 
			for(int fvi = 0; fvi < featVals.length; fvi++)
			{
				if(featVals[fvi].equals(""+DiachronicSimulator.MARK_POS))	intFeatVals+= DiachronicSimulator.POS_INT; 
				else if (featVals[fvi].equals(""+DiachronicSimulator.MARK_UNSPEC))	intFeatVals += DiachronicSimulator.UNSPEC_INT; 
				else if (featVals[fvi].equals(""+DiachronicSimulator.MARK_NEG))	intFeatVals += DiachronicSimulator.NEG_INT; 
				else	throw new Error("Error: unrecognized feature value, "+featVals[fvi]+" in line "+li);
			}
			
			phoneSymbToFeatsMap.put(symb, intFeatVals);
			li++; 
		}
	}
	
}
