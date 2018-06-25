import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap; 
import java.util.Scanner; 
import java.util.List;
import java.util.ArrayList; 

/**TODO update here when decisions have been made
 * 
 * main class for diachronic derivation system
 * first takes in info from relecvant files: 
 * 		symbolDefs.csv -- gets the list of relevant features from the first row
 * 			and the definition of each phone symbol with respect to those features
 * 				from the lines below 
 *		FeatTranslations.txt and FeatImplications.txt -- for auxiliary operations, use as necessary
 *	then inputs shifts file -- saves these as is appropriate (decide how to do this, then update here
 *	and finally dataset or words entered by user -- probably use separate method for this. 
 * @author Clayton Marr
 *
 */
public class DerivationSimulation {
	
	private final static char MARK_POS = '+', MARK_NEG = '-', MARK_UNSPEC = '.', FEAT_DELIM = ','; 
	private final static int POS_INT = 2, NEG_INT = 0, UNSPEC_INT = 1;
	private final static char IMPLICATION_DELIM=':';
	
	private static String[] featsByIndex; 
	private static HashMap<String, Integer> featIndices;
	private static HashMap<String, String> phoneSymbToFeatsMap;
	private static HashMap<String, String> phoneFeatsToSymbMap; //TODO abrogate either this or the previous class variable
	private static HashMap<String, String[]> featImplications; //TODO currently abrogated
	private HashMap featTranslations; //TODO currently abrogated 
	
	public static void main(String args[])
	{
		Scanner input = new Scanner(System.in); 
		
		System.out.println("Would you like to use the standard symbol definitions file? Please enter 'yes' or 'no'.");
		String resp = input.nextLine(); 
		
		while(!resp.equalsIgnoreCase("yes") && !resp.equalsIgnoreCase("no"))
		{
			System.out.println("Invalid response.");
			System.out.println("Would you like to use the standard symbol definitions file location? Please enter 'yes' or 'no'. ");
			resp = input.nextLine(); 
		}
		
		String symbDefsLoc = (resp == "yes") ? "symbolDefs.csv" : ""; 
		if(resp == "no")
		{
			System.out.println("Please enter the correct location of the symbol definitions file you would like to use:");
			symbDefsLoc = input.nextLine(); 
		}
		
		//collect task information from symbol definitions file. 
		
		List<String> symbDefsLines = new ArrayList<String>();
		String nextLine; 
		
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream(symbDefsLoc), "UTF-8")); 
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
		
		//extract the feature list and then the features for each symbol.
		featsByIndex = symbDefsLines.get(0).replace("SYMB,", "").split(""+FEAT_DELIM); 
		
		for(int fi = 0; fi < featsByIndex.length; fi++) featIndices.put(featsByIndex[fi], fi);
		
		int li = 1; 
		while (li < symbDefsLines.size()) 
		{
			nextLine = symbDefsLines.get(li).replaceAll("\\s+", ""); //strip white space and invisible characters 
			int ind1stComma = nextLine.indexOf(FEAT_DELIM); 
			String symb = nextLine.substring(0, ind1stComma); 
			String[] featVals = nextLine.substring(ind1stComma+1).split(""+FEAT_DELIM); 		
			
			String intFeatVals = ""; 
			for(int fvi = 0; fvi < featVals.length; fvi++)
			{
				if(featVals[fvi].equals(""+MARK_POS))	intFeatVals+= POS_INT; 
				else if (featVals[fvi].equals(""+MARK_UNSPEC))	intFeatVals += UNSPEC_INT; 
				else if (featVals[fvi].equals(""+MARK_NEG))	intFeatVals += NEG_INT; 
				else	throw new Error("Error: unrecognized feature value ");
			}
			
			phoneSymbToFeatsMap.put(symb, intFeatVals);
			phoneFeatsToSymbMap.put(intFeatVals, symb);
			li++; 
		}
		

		System.out.println("Would you like to use the standard feature implications file location? Please enter 'yes' or 'no'.");
		resp = input.nextLine(); 
		
		while(!resp.equalsIgnoreCase("yes") && !resp.equalsIgnoreCase("no"))
		{
			System.out.println("Invalid response.");
			System.out.println("Would you like to use the standard symbol definitions file? Please enter 'yes' or 'no'. ");
			resp = input.nextLine(); 
		}
		
		String featImplsLoc = (resp == "yes") ? "FeatImplications.txt" : ""; 
		if(resp == "no")
		{
			System.out.println("Please enter the correct location of the symbol definitions file you would like to use:");
			featImplsLoc = input.nextLine(); 
		}
		
		
		List<String> featImplLines = new ArrayList<String>(); 
		
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream(featImplsLoc), "UTF-8")); 
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
			String[] fisides = filine.split(""+IMPLICATION_DELIM); 
			featImplications.put(fisides[0], fisides[1].split(""+FEAT_DELIM));
		}
		
		//TODO input info from FeatImplications and FeatTranslations files or user specified variants if deemed necessary
			// to not abrogate...
	}
	
	

	/**
	 * for when we actually deal with parsing the shifts: 
	 * 	
				if(nextLine.trim().charAt(0) != '$') //i.e. if it is not true that the whole line is a cmt
					
	 */
	
}

