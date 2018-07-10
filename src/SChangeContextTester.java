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
import java.util.Set;

public class SChangeContextTester {

	//excludes all material dealing with feature implications 

	private final static char MARK_POS = '+', MARK_NEG = '-', MARK_UNSPEC = '.', FEAT_DELIM = ',', PH_DELIM=' '; 
	private final static char restrDelim = ',';
	private final static int POS_INT = 2, NEG_INT = 0, UNSPEC_INT = 1;
	private static String[] featsByIndex; 
	private static HashMap<String, Integer> featIndices;
	private static HashMap<String, String> phoneSymbToFeatsMap;
	private static HashMap<String, String> phoneFeatsToSymbMap; //TODO abrogate either this or the previous class variable
	private static Set<String> featNames; 

	
	
	public static void main(String args[])
	{
		featIndices = new HashMap<String, Integer>(); 
		phoneSymbToFeatsMap = new HashMap<String, String>(); 
		phoneFeatsToSymbMap = new HashMap<String, String>(); 
		
		System.out.println("Collecting symbol definitions...");
		
		List<String> symbDefsLines = new ArrayList<String>();
		String nextLine; 
		
		try 
		{	File inFile = new File("symbolDefs.csv"); 
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
		
		//TODO debugging
		System.out.println("Symbol definitions extracted!");
		System.out.println("Length of symbDefsLines : "+symbDefsLines.size()); 
		
		//from the first line, extract the feature list and then the features for each symbol.
		featsByIndex = symbDefsLines.get(0).replace("SYMB,", "").split(""+FEAT_DELIM); 
		
		for(int fi = 0; fi < featsByIndex.length; fi++) 
			featIndices.put(featsByIndex[fi], fi);
		
		featNames = featIndices.keySet();
		
		//from the rest-- extract the symbol def each represents
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
				else	throw new Error("Error: unrecognized feature value, "+featVals[fvi]+" in line "+li);
			}
			
			phoneSymbToFeatsMap.put(symb, intFeatVals);
			phoneFeatsToSymbMap.put(intFeatVals, symb);
			li++; 
		}
		System.out.println("Beginning test of class SChangeContext");
		
		System.out.println("Testing method generateMinSize()");

		String X = "[+"+featsByIndex[0]+"]";
		
		SChangeContext testX = parseNewContext(X, false); 
		System.out.println("The following should be 1\n"+testX.generateMinSize());
		
		SChangeContext testXX = parseNewContext(X+" "+X, false); 
		System.out.println("The following should be 2\n"+testXX.generateMinSize());
		
		SChangeContext testCX7 = parseNewContext("( "+X+" )", false);
		System.out.println("The following should be 0\n"+testCX7.generateMinSize());

		SChangeContext testCXX7 = parseNewContext("("+X+" "+X+")", false); 
		System.out.println("The following should be 0\n"+testCXX7.generateMinSize()); 
		
		SChangeContext testXCX7t = parseNewContext(X+" ("+X+")+", false);
		System.out.println("The following should be 2\n"+testXCX7t.generateMinSize());
		
		SChangeContext testXCX7i = parseNewContext(X+" ("+X+")*", false); 
		System.out.println("The following should be 1\n"+testXCX7i.generateMinSize());
		
		SChangeContext testCXCX7i7t = parseNewContext("("+X+" ("+X+")* )+",false);
		System.out.println("The following should be 2\n"+testCXCX7i7t.generateMinSize()); 
		
	}
	
	private static String forceParenSpaceConsistency(String input)
	{
		String output = input; 
		int i = 0; 
		while ( i < output.length() - 1)
		{
			if(output.charAt(i) == '(')
			{
				if(i > 0)
					if (output.charAt(i-1) != ' ')
					{	output = output.substring(0, i) + " " + output.substring(i); i++;	}
				if(output.charAt(i+1) != ' ')
				{	output = output.substring(0, i+1) + " "+ output.substring(i+1); i++;	}
			}
			i++; 
			if(output.charAt(i) == ')')
			{
				if(output.charAt(i-1) != ' ')
				{	output = output.substring(0,i) + " " + output.substring(i); i++;	}
				if( i < output.length() - 1)
				{	
					if("*+".contains(output.charAt(i+1)+""))
					{	if( i < output.length() - 2)
						{	if(output.charAt(i+2) != ' ')
							{
								output = output.substring(0, i+2) + " " + output.substring(i+2); 
								i += 3;
							}}}
					else 
					{
						if(output.charAt(i+1) != ' ')
						{	output = output.substring(0, i+1) + " " + output.substring(i+1); 
							i += 2;	}}}}}
		return output;
	}
	
	/** parseNewContext 
	 * constructs a new ShiftContext instance
	 * with variables currRestrList and currParenMap to match the parse of this new contxt
	 * currRestrList -- all the RestrictPhones i.e. specifications on each context phone (either FeatMatrix or Boundary) 
	 * while parenMap is a String[] that is a "map" of where parenthetical structures apply
	 * ... structured as illustrated by this example (the top row is the indices in PARENMAP)
	 * 
	 * 		0	|	1	|	2	|	3	|	4 	|	5	|	6	|	7
	 * 		i0 	| +(:4	| 	i1 	| 	i2 	| )+:1	| 	(:7	| 	i3	|	):5 
	 * cells with contents starting i indicate that the cell corresponds to the index of the number following 
	 * 		in placeRestrs
	 * cells with paren markers { +(, )+, (, ), *(, )*, } indicate where parens open and close
	 * 		relative to those indices in parenMap
	 * 		the number on the inside of hte paren indicates which index IN PARENMAP 
	 * 			is where the corresponding opening or closing paren lies.
	 * @param inp -- raw input for the context specifications  
	 * @param boundsMatter -- determines whether the context restrictions we create will pass over boundaries 
	 * 		in input for context matching checker functions 
	 * @precondition : all elements separated phDelim 
	 * @return
	 */
	private static SChangeContext parseNewContext(String input, boolean boundsMatter)
	{

		String inp = forceParenSpaceConsistency(input); //force parenthesis statements to be
			// surrounded by spaces 
		String[] toPhones = inp.trim().split(""+PH_DELIM); 
		List<String> parenMapInProgress = new ArrayList<String>();
		List<RestrictPhone> thePlaceRestrs = new ArrayList<RestrictPhone>(); 
		
		for(int i = 0; i < toPhones.length; i++)
		{
			String curtp = toPhones[i].trim(); 
			if(curtp.charAt(0) == '(') 
			{	//if it starts with '(' remove this and add to parenMap 
				curtp = curtp.substring(1).trim(); 
				parenMapInProgress.add("(");
			}
			String parenClose = "";
			if(curtp.contains(")"))
			{	//same as above for ending with ) (or ")*", ")+" ) 
				int lencurtp = curtp.length(); 
				if(curtp.indexOf(")") == lencurtp - 1 )	parenClose = ")"; 
				else
				{	//if ) is not the last character, ensure that either + or * is 
						// -- meaning 1 or more or 0 or more respectively 
					assert curtp.indexOf(")") == lencurtp - 2 && 
							"+*".contains(curtp.substring(lencurtp - 1)) : 
						"Error: invalid usage of closing parenthesis in new context to parse"; 
					parenClose = curtp.substring(lencurtp-2); 		
				}
				// remove from curtp
				curtp = curtp.substring(0, curtp.indexOf(")")).trim();
			}
			
			if (curtp.length() > 0) // if there is anyhting left after trimming and paren deletion
			{
				parenMapInProgress.add("i"+thePlaceRestrs.size()); 
				if(phoneSymbToFeatsMap.containsKey(curtp)) //it's a Phone instance
					thePlaceRestrs.add(new Phone(phoneSymbToFeatsMap.get(curtp), featIndices, phoneSymbToFeatsMap)); 
				else if("+#".contains(curtp))
					thePlaceRestrs.add(new Boundary(("#".equals(curtp) ? "word " : "morph ") + "bound"));
				else if("@".equals(curtp))
					thePlaceRestrs.add(new Boundary("non word bound"));
				else
				{
					assert (curtp.charAt(0) == '[') == (curtp.charAt(curtp.length() - 1) == ']'): 
						"Error : mismatch between presenced of opening bracket and presence "
						+ "of closing bracket"; 
					if(curtp.charAt(0) == '[')
						curtp = curtp.substring(1, curtp.length() - 1).trim(); 
					assert isValidFeatSpecList(curtp): 
						"Error: had to preempt attempted construction of a FeatMatrix instance"
						+ "with an invalid entrace for the list of feature specifications.";
					thePlaceRestrs.add(getFeatMatrix(curtp));  
				}
			}
			if(!parenClose.isEmpty()) //i.e. we are adding closing parentheses 
			{
				int corrOpenIndex = parenMapInProgress.lastIndexOf("("); 
					// corresponding index for the opening paren 
				if(parenClose.length() == 2) // the closing paren is either starred or plussed
					parenMapInProgress.set(corrOpenIndex,  parenClose.charAt(1)+"("); 
				parenMapInProgress.set(corrOpenIndex, parenMapInProgress.get(corrOpenIndex) +
						":" + parenMapInProgress.size()); 
				parenMapInProgress.add(parenClose + ":" + corrOpenIndex); 
			}
		}
		
		String[] theParenMap = new String[parenMapInProgress.size()];
		theParenMap = parenMapInProgress.toArray(theParenMap); 
		
		return new SChangeContext(thePlaceRestrs, theParenMap, boundsMatter) ;
	}

	public static FeatMatrix getFeatMatrix(String featSpecs)
	{	return getFeatMatrix(featSpecs, false);	}
	
	//derives FeatMatrix object instance from String of featSpec instances
	public static FeatMatrix getFeatMatrix(String featSpecs, boolean isInputDest)
	{
		assert isValidFeatSpecList(featSpecs) : "Error : preempted attempt to get FeatMatrix from an invalid list of feature specifications" ; 
		
		String theFeatSpecs = featSpecs;
		
		if(theFeatSpecs.contains(".") == false)
			return new FeatMatrix(theFeatSpecs, featIndices); 
		
		List<String> despecs = new ArrayList<String>(); 
		
		//TODO we should make sure someone doesn't insert use "unspecification" -- i.e. period '.' as a SPECIFICATION
		while(theFeatSpecs.contains(".") && !isInputDest)
		{
			int pdIndex = theFeatSpecs.indexOf("."); 
			assert (theFeatSpecs.charAt(pdIndex - 1)==restrDelim): 
				"Error: Unspecification marker, '.', found at an illegitimate place, likely was used in the middle of a feature"
				+ "	as a part of a feature name"; //TODO set up earlier assertion error to troubleshoot this
			String fWIAfterPd = theFeatSpecs.substring(pdIndex+1); 
			theFeatSpecs = theFeatSpecs.substring(0, pdIndex - 1); 
			if(fWIAfterPd.contains(""+restrDelim))
			{	theFeatSpecs += fWIAfterPd.substring(fWIAfterPd.indexOf(""+restrDelim));
				despecs.add(fWIAfterPd.substring(0, fWIAfterPd.indexOf(""+restrDelim))); 	}
			else	despecs.add(fWIAfterPd); //i.e. this is when it was the last element in the string .
		}
		return new FeatMatrix(theFeatSpecs, featIndices, despecs); 
	}

	/** isValidFeatSpecList
	 * @return @true iff @param input consists of a list of valid feature specifications 
	 * 	each delimited by restrDelim
	 */
	private static boolean isValidFeatSpecList(String input)
	{
		String[] specs = input.split(""+restrDelim); 
		
		for(int si = 0; si < specs.length; si++)
		{	
			if("+-.".contains(specs[si].substring(0,1)))
			{	if(!featNames.contains(specs[si].substring(1)))	return false;	}
			else if(!featNames.contains(specs[si]))	return false; 
		}
		return true; 
	}
	
	
	
}
