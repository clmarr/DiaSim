import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File; 
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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
	private final static char IMPLICATION_DELIM=':', PH_DELIM = ' '; 
	private final static char CMT_FLAG = '$'; //marks taht the text after is a comment in the sound rules file, thus doesn't read the rest of the line
	private final static char STAGENAME_FLAG = '~'; 
	private final static char STAGENAME_LOC_DELIM = ':'; 
	
	private static String[] featsByIndex; 
	private static HashMap<String, Integer> featIndices;
	private static HashMap<String, String> phoneSymbToFeatsMap;
	private static HashMap<String, String> phoneFeatsToSymbMap; //TODO abrogate either this or the previous class variable
	private static HashMap<String, String[]> featImplications; 
	private HashMap featTranslations; //TODO currently abrogated 
	private static List<String> rulesByTimeInstant; 
	private static Lexicon initLexicon, testResultLexicon, goldResultLexicon;
	private static int NUM_ETYMA; 
	private static String[] customStageNames; 
	private static Lexicon[] customStageLexica; //indexes match with those of customStageNames 
		//so that each stage has a unique index where its lexicon and its name are stored at 
			// in their respective lists.
	private static int[] customStageTimeInstants; // i.e. the index of custom stages in the ordered rule set
	private static boolean customStagesSet; 
	private static String[] wordTrajectories; //stores trajectory, with stages delimited by line breaks, of each word 
	
	private static int[] LDByWord; //if gold is input: Levenshtein distance between gold and testResutlt for each word.
	private static boolean[] wordMissLocs; //each index true if the word for this index
		// resulted in a missmatch between the gold and the test result
	
	private static double PERFORMANCE; // for the final score of Levenshtein Distance / #phones, avgd over words 
	
	public static void main(String args[])
	{
		Scanner input = new Scanner(System.in); 
		
		featIndices = new HashMap<String, Integer>() ; 
		phoneSymbToFeatsMap = new HashMap<String, String>(); 
		phoneFeatsToSymbMap = new HashMap<String, String>(); 
		featImplications = new HashMap<String, String[]>(); 
		
		System.out.println("Would you like to use the standard symbol definitions file? Please enter 'yes' or 'no'.");
		String resp = input.nextLine(); 
		
		while(!resp.equalsIgnoreCase("yes") && !resp.equalsIgnoreCase("no"))
		{
			System.out.println("Invalid response.");
			System.out.println("Would you like to use the standard symbol definitions file location? Please enter 'yes' or 'no'. ");
			resp = input.nextLine(); 
		}
		
		String symbDefsLoc = (resp.equalsIgnoreCase("yes")) ? "symbolDefs.csv" : ""; 
		if(resp.equalsIgnoreCase("no"))
		{
			System.out.println("Please enter the correct location of the symbol definitions file you would like to use:");
			symbDefsLoc = input.nextLine(); 
		}
		
		//collect task information from symbol definitions file. 
		
		//TODO debugging
		System.out.println("Symbol defs location: "+symbDefsLoc);
		System.out.println("Collecting symbol definitions...");
		
		List<String> symbDefsLines = new ArrayList<String>();
		String nextLine; 
		
		try 
		{	File inFile = new File(symbDefsLoc); 
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
		
		for(int fi = 0; fi < featsByIndex.length; fi++) featIndices.put(featsByIndex[fi], fi);
		
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
		

		System.out.println("Would you like to use the standard feature implications file location? Please enter 'yes' or 'no'.");
		resp = input.nextLine(); 
		
		while(!resp.equalsIgnoreCase("yes") && !resp.equalsIgnoreCase("no"))
		{
			System.out.println("Invalid response.");
			System.out.println("Would you like to use the standard symbol definitions file? Please enter 'yes' or 'no'. ");
			resp = input.nextLine(); 
		}
		
		String featImplsLoc = (resp.equals("yes")) ? "FeatImplications" : ""; 
		if(resp == "no")
		{
			System.out.println("Please enter the correct location of the symbol definitions file you would like to use:");
			featImplsLoc = input.nextLine(); 
		}
		
		//TODO debugging
		System.out.println("Now extracting info from feature implications file...");
		
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
		
		//TODO debugging
		System.out.println("Done extracting feature implications!");
		System.out.println("Creating SChangeFactory...");
		SChangeFactory theFactory = new SChangeFactory(phoneSymbToFeatsMap, featIndices, featImplications); 
		
		System.out.println("Use current default rules file location? Enter 'yes' or 'no'."); 
		resp = input.nextLine(); 
		
		while(!resp.equalsIgnoreCase("yes") && !resp.equalsIgnoreCase("no"))
		{
			System.out.println("Invalid response.");
			System.out.println("Use current default rules file location? Please enter 'yes' or 'no'. ");
			resp = input.nextLine(); 
		}
		
		String ruleFileLoc = (resp.equalsIgnoreCase("yes")) ? "LatinToFrenchRules.txt" : ""; 
		if (resp.equalsIgnoreCase("no"))	
		{
			System.out.println("Please enter the location of your alternative rules file: ");
			ruleFileLoc = input.nextLine(); 
		}
		
		System.out.println("Now extracting diachronic sound change rules from rules file...");
		
		rulesByTimeInstant = new ArrayList<String>(); 

		String nextRuleLine;
		
		try 
		{	BufferedReader in = new BufferedReader ( new InputStreamReader ( 
				new FileInputStream(ruleFileLoc), "UTF-8")); 
			
			while((nextRuleLine = in.readLine()) != null)
			{
				String lineWithoutComments = ""+nextRuleLine; 
				if (lineWithoutComments.contains(""+CMT_FLAG))
						lineWithoutComments = lineWithoutComments.substring(0,
								lineWithoutComments.indexOf(""+CMT_FLAG));
				if(!lineWithoutComments.trim().equals(""))	rulesByTimeInstant.add(lineWithoutComments); 
			}
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
		
		//now filter out the stage name declaration lines.
		
		List<String> provisionalStageNameAndLocList = new ArrayList<String>(); //to be collected 
		//until the end of collection, at which point the appropriate arrays for the custom
		// stages will be created using this List. 

		customStagesSet = false; 
				
		int rli = 0; 
		
		while (rli < rulesByTimeInstant.size())
		{
			String currRule = rulesByTimeInstant.get(rli); 
			if( currRule.charAt(0) == STAGENAME_FLAG )
			{
				customStagesSet = true; 
				assert rli != 0: "Error: Stage set at the first line -- this is useless, redundant with the initial stage ";
				
				currRule = currRule.substring(1); 
				assert !currRule.contains(""+STAGENAME_FLAG): 
					"Error: stage name flag "+STAGENAME_FLAG+" occuring in a place besides the first character in the rule line -- this is illegal: \n"+currRule; 
				assert !currRule.contains(STAGENAME_LOC_DELIM+""):
					"Error: illegal character found in name for custom stage -- "+STAGENAME_LOC_DELIM;  
				provisionalStageNameAndLocList.add(""+currRule+STAGENAME_LOC_DELIM+rli);
				rulesByTimeInstant.remove(rli);  
			}
			else	rli++;
		}
		
		int numStages = provisionalStageNameAndLocList.size(); 
		
		System.out.println("Using "+numStages+" custom stages."); 
		
		customStageLexica = new Lexicon[numStages];
		customStageNames = new String[numStages];
		customStageTimeInstants = new int[numStages]; 
		
		for(int csi = 0; csi < numStages; csi++)
		{
			//TODO debugging
			System.out.println("Stage name and loc : "+provisionalStageNameAndLocList.get(csi));
			
			String[] stageNameAndLoc = provisionalStageNameAndLocList.get(csi).split(""+STAGENAME_LOC_DELIM);
			customStageNames[csi] = stageNameAndLoc[0]; 
			
			//TODO debuggging
			System.out.println("stage : "+customStageNames[csi]);
			
			customStageTimeInstants[csi] = Integer.parseInt(stageNameAndLoc[1]); 
		}
		
		// parse the rules
		List<SChange> theShiftsInOrder = new ArrayList<SChange>();
		
		int cri = 0; 
		
		for(String currRule : rulesByTimeInstant)
		{
			//TODO debugging
			System.out.println("Generating rules for rule number "+cri+" : "+currRule);
			
			List<SChange> newShifts = theFactory.generateSoundChangesFromRule(currRule); 
			for(SChange newShift : newShifts)
				System.out.println("SChange generated : "+newShift+", with type"+newShift.getClass());
			
			theShiftsInOrder.addAll(theFactory.generateSoundChangesFromRule(currRule));
			cri++; 
		}
		
		System.out.println("Diachronic rules extracted. "); 
		
		//now input lexicon 
		//detect whether we have the gold or just the initial stage lexicon by whether the input file is a .csv
		//collect init lexicon ( and gold if so specified) 
		//copy init lexicon to "evolving lexicon" 
		//each time a custom stage time step loc (int in the array customStageTimeInstantLocs) is hit, save the 
		// evolving lexicon at that point by copying it into the appropriate slot in the customStageLexica array
		// finally when we reach the end of the rule list, save it as testResultLexicon
	
		System.out.println("Do you wish to use the default location for the lexicon input file? Enter 'yes' or 'no'"); 
		resp = input.nextLine();
		while(!resp.equalsIgnoreCase("yes") && !resp.equalsIgnoreCase("no"))
		{
			System.out.println("Invalid response.");
			System.out.println("Do you wish to use the default location for the lexicon input file? Please enter 'yes' or 'no'. ");
			resp = input.nextLine(); 
		}
		String lexFileLoc = (resp.equalsIgnoreCase("yes")) ? "LatinLexFileForMKPopeTester.txt" : "";
		if(resp.equalsIgnoreCase("no")) 
		{
			System.out.println("Please enter the correct location of the symbol definitions file you would like to use:");
			lexFileLoc = input.nextLine(); 
		}
		
		System.out.println("Now extracting lexicon...");
		
		List<String> lexFileLines = new ArrayList<String>(); 
		
		try 
		{	File inFile = new File(lexFileLoc); 
			BufferedReader in = new BufferedReader ( new InputStreamReader (
				new FileInputStream(inFile), "UTF8")); 
			while((nextLine = in.readLine()) != null)	
			{	if (nextLine.contains(CMT_FLAG+""))
					nextLine = nextLine.substring(0,nextLine.indexOf(CMT_FLAG)).trim(); 
				if (!nextLine.equals("")) 	lexFileLines.add(nextLine); 		
			}
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
		
		//TODO fix -- at this point implement a branching of the simulation -- one branch with a gold, the other without? 
		
		NUM_ETYMA = lexFileLines.size(); 
		wordTrajectories = new String[NUM_ETYMA]; 
		
		LexPhon[] initWords = new LexPhon[NUM_ETYMA];
		LexPhon[] goldWords = new LexPhon[NUM_ETYMA];
		boolean goldIsInput = (lexFileLoc.substring(lexFileLoc.length()-5, lexFileLoc.length()).equals(".csv"));
		
		
		int lfli = 0; 
		while (lfli < NUM_ETYMA)
		{
			String theLine = lexFileLines.get(lfli);
			wordTrajectories[lfli] = goldIsInput ? theLine : theLine.split(",")[0]; 
			initWords[lfli] = parseLexPhon(wordTrajectories[lfli]); 
			if (goldIsInput) 
				goldWords[lfli] = parseLexPhon(theLine.split(",")[1]);
			
			lfli++;
		}
		
		initLexicon = new Lexicon(initWords); 
		testResultLexicon = new Lexicon(initWords); // this one will "evolve" with "time" 
		goldResultLexicon = goldIsInput ? new Lexicon(goldWords) : null;
		
		//TODO debugging
		LexPhon[] testResultWords = testResultLexicon.getWordList();
		for (LexPhon tRWord : testResultWords)
		{	System.out.println(tRWord); }
		
		System.out.println("Lexicon extracted :");

		//TODO debugging
		for (LexPhon tRWord : testResultWords)
		{	System.out.print(tRWord+"|"); }
		System.out.println("\nNow evolving the words.");
		
		//TODO evolve the words 
		int nextStageIndex = 0; //index IN THE ARRAYS that the next stage to look for will be at .
		int ri = 0, numRules = theShiftsInOrder.size(); //for iteration.
		
		while (ri < numRules)
		{
			//TODO debugging
			System.out.println("Applying rule "+ri+" : "+theShiftsInOrder.get(ri));
			
			SChange thisShift = theShiftsInOrder.get(ri);
			if (customStagesSet && nextStageIndex < numStages)
			{
				if (ri == customStageTimeInstants[nextStageIndex])
				{
					customStageLexica[nextStageIndex] = new Lexicon(testResultLexicon.getWordList());
					System.out.println("---------------------------------------------------------------------------");
					System.out.println("STAGE: "+customStageNames[nextStageIndex]+"\n---------------------------------------------------");
					nextStageIndex++;
				}
			}
			
			boolean[] wordsChanged = testResultLexicon.applyRuleAndGetChangedWords(thisShift);
			
			for (int wci = 0; wci < wordsChanged.length; wci++)
			{
				if(wordsChanged[wci])
				{
					System.out.println("Word "+wci+" changed! "+testResultLexicon.getByID(wci)); //TODO debugging
					wordTrajectories[wci] += "\n"+testResultLexicon.getByID(wci)+" | Shift "+ri+" : "+thisShift;
				}
			}
			
			ri++; 
		}
	
		if(goldIsInput)
		{	
			PERFORMANCE = getLDErrorAvgdOverWordLengthInPhones(); 
			wordMissLocs = new boolean[NUM_ETYMA]; 
			for(int i = 0; i < NUM_ETYMA; i++)
			{
				LDByWord[i] = levenshteinDistance(testResultLexicon.getByID(i), 
						goldResultLexicon.getByID(i)); 
				wordMissLocs[i] = (LDByWord[i] != 0);
				wordTrajectories[i] = wordTrajectories[i].substring(0, wordTrajectories[i].indexOf("\n")) +
					(wordMissLocs[i] ? " MISS, edit distance: "+LDByWord[i]:" HIT") +
					wordTrajectories[i].substring(wordTrajectories[i].indexOf("\n"));	
			}
			
			System.out.println("Writing analysis files...");//TODO maybe need more print statements here. 
			
			makeAnalysisFile("initialFormInfluenceAnalysis.txt", "Initial", initLexicon); 
			makeAnalysisFile("testResultAnalysis.txt", "Test Result", testResultLexicon); 
			makeAnalysisFile("goldAnalysis.txt","Gold",goldResultLexicon); 
			
			if(customStagesSet)
				for(int csi = 0; csi < customStageNames.length ; csi++)
					makeAnalysisFile(customStageNames[csi].replaceAll(" ", ""),
							customStageNames[csi], customStageLexica[csi]);
				
			System.out.println("Analysis files written!");
			
		}
		
		//TODO debugging
		System.out.println(wordTrajectories[0]);
		
		//TODO make the calculations and output the files! 
	}

	private static void makeAnalysisFile(String fileName, String lexicName, Lexicon lexic)
	{
		String output = "Analysis for "+lexicName+"/n";
		output += "Overall performance in average derivational distance : "+PERFORMANCE+"\n"; //TODO come up with better name for this 
		output += "Performance associated with each phone in "+lexicName+"\n"; 
		output += "Phone in "+lexicName+"\tAssociated lakation\tMean Associated Normalized Lev.Dist.\n";
		
		HashMap<Phone, Double> lakationByPhone = lakationPerPhone(lexic);
		HashMap<Phone, Double> avgAssocdLDs = avgLDForWordsWithPhone(lexic); 
		Phone[] phonInv = lexic.getPhonemicInventory(); 
		for(Phone ph : phonInv)
			output += ph.print()+"\t|\t"+lakationByPhone.get(ph)+"\t|\t"+avgAssocdLDs.get(ph)+"\n"; 
		
		try 
		{	FileWriter outFile = new FileWriter(fileName); 
			BufferedWriter out = new BufferedWriter(outFile); 
			out.write(output);
			out.close();
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
		
	}
	
	//on the word level, it is divided by the number of phones in the INITIAL lexicon's version of the word! 
	private static double getLDErrorAvgdOverWordLengthInPhones()
	{
		double totLexQuotients = 0.0; 
		for(int i = 0; i < NUM_ETYMA; i++)
		{
			int numPhonesInInitWord = 0; 
			List<SequentialPhonic> initWordPhSeq  = initLexicon.getByID(i).getPhonologicalRepresentation(); 
			for(SequentialPhonic ph : initWordPhSeq)
				if(ph.getType().equals("phone"))	numPhonesInInitWord++; 
			
			totLexQuotients = (double)levenshteinDistance(testResultLexicon.getByID(i), goldResultLexicon.getByID(i))
					/	(double)numPhonesInInitWord; 
		}
		return totLexQuotients / (double)NUM_ETYMA; 
				
	}
	
	// missLocations are the indices of words that ultimately resulted in a miss between the testResult and the gold
	// outputs the scores for each phone in the wordin the lexicon 
	private static HashMap<Phone,Double> lakationPerPhone (Lexicon lexic)
	{
		LexPhon[] lexList = lexic.getWordList(); //indices should correspond to those in missLocations
		int lexSize = lexList.length; 
		assert NUM_ETYMA == wordMissLocs.length: "Error : mismatch between size of locMissed array and word list in lexicon"; 
		
		Phone[] phonemicInventory = lexic.getPhonemicInventory(); 
		int inventorySize = phonemicInventory.length; 
		
		HashMap<String,Integer> phonemeIndices = new HashMap<String,Integer>();
			//maps print symbols of phoneme onto its index in phonemicInventory array
		for(int phii = 0 ; phii < phonemicInventory.length; phii++)
			phonemeIndices.put( phonemicInventory[phii].print(), phii); 
			
		int[] phoneFreqForMisses = new int[inventorySize]; 
			//indices correspond to those in phonemicInventory 
		
		for(int li = 0 ; li < lexSize; li++)
		{
			if(wordMissLocs[li])
			{
				String phonesSeenInWord = ""; 
				List<SequentialPhonic> phs = lexList[li].getPhonologicalRepresentation(); 
				
				for(SequentialPhonic ph : phs)
				{
					if(ph.getType().equals("phone"))
					{
						if(!phonesSeenInWord.contains(ph.print()))
						{
							phonesSeenInWord += ph.print() + ","; 
							phoneFreqForMisses[phonemeIndices.get(ph.print())] += 1; 
						}
					}
				}
			}
		}
		
		HashMap<String,Integer> phoneFreqsByWordInLex = lexic.getPhoneFrequenciesByWord(); 
		//note that the keys for this are the feature vects, not the toString() or print() statements
		
		HashMap<Phone,Double> output = new HashMap<Phone,Double>(); 
		for (int pi = 0; pi < inventorySize ; pi++)
			output.put(phonemicInventory[pi], 
				(double)phoneFreqForMisses[pi] /
				(double)phoneFreqsByWordInLex.get(phonemicInventory[pi].getFeatString()));
		
		return output; 
	}
	
	// outputs hashmap where the value for each key Phone instance
	// is the average Levenshtein distance for words containing that phone 
	private static HashMap<Phone,Double> avgLDForWordsWithPhone (Lexicon lexic)
	{
		LexPhon[] lexList = lexic.getWordList(); //indices should correspond to those in missLocations
		int lexSize = lexList.length; 

		Phone[] phonemicInventory = lexic.getPhonemicInventory(); 
		int inventorySize = phonemicInventory.length; 
		HashMap<String,Integer> phonemeIndices = new HashMap<String,Integer>();
			//maps print symbols of phoneme onto its index in phonemicInventory array
		for(int phii = 0 ; phii < phonemicInventory.length; phii++)
			phonemeIndices.put( phonemicInventory[phii].print(), phii); 
		
		int[] totalLevenshtein = new int[inventorySize]; //total levenshtein edit distance 
			// of words with this phone
		
		for(int li = 0; li < lexSize; li++)
		{
			List<SequentialPhonic> phs = lexList[li].getPhonologicalRepresentation(); 
			String phonesSeenInWord = ""; 
			for (SequentialPhonic ph : phs)
			{
				if(ph.getType().equals("phone"))
				{
					if(!phonesSeenInWord.contains(ph.print()))
					{
						phonesSeenInWord += ph.print() + ","; 
						totalLevenshtein[phonemeIndices.get(ph.print())] += 
								levenshteinDistance(testResultLexicon.getByID(li),
										goldResultLexicon.getByID(li));
					}
				}
			}
		}
		
		HashMap<String,Integer> phoneFreqsByWordInLex = lexic.getPhoneFrequenciesByWord(); 

		HashMap<Phone,Double> output = new HashMap<Phone,Double>(); 
		for(int phi = 0; phi < inventorySize; phi++)
		{
			output.put(phonemicInventory[phi], 
					(double)totalLevenshtein[phi] / 
					(double)phoneFreqsByWordInLex.get(phonemicInventory[phi].getFeatString()));
		}
		return output;
	}
	/**
	 * given String @param toLex
	 * @return its representation as a LexPhon containing a sequence of Phone instances
	 * TODO note we assume the phones are separated by PH_DELIM (presumably ' ') 
	 */
	private static LexPhon parseLexPhon(String toLex)
	{
		String[] toPhones = toLex.split(""+PH_DELIM);
		List<SequentialPhonic> phones = new ArrayList<SequentialPhonic>(); //LexPhon class stores internal List of phones not an array,
			// for better ease of mutation

		for (String toPhone : toPhones)
		{
			if (toPhone.equals("#") || toPhone.equals("+"))
				phones.add(new Boundary(toPhone.equals("#") ? "word bound" : "morph bound"));
			else
			{
				assert phoneSymbToFeatsMap.containsKey(toPhone): 
					"Error: tried to declare a phone in a word in the lexicon using an invalid symbol.\nSymbol is : "+toPhone; 
				phones.add(new Phone(phoneSymbToFeatsMap.get(toPhone), featIndices, phoneSymbToFeatsMap));
			}
		}
		return new LexPhon(phones);
	}
	
	//as formulated here : https://people.cs.pitt.edu/~kirk/cs1501/Pruhs/Spring2006/assignments/editdistance/Levenshtein%20Distance.htm
	//under this definition of Levenshtein Edit Distance,
	// substitution has a cost of 1, the same as a single insertion or as a single deletion 
	private static int levenshteinDistance(LexPhon s, LexPhon t)
	{
		List<SequentialPhonic> sPhons = s.getPhonologicalRepresentation(), 
				tPhons = t.getPhonologicalRepresentation(); 
		int n = sPhons.size(), m = tPhons.size(); 
		
		String[] sPhonStrs = new String[n], tPhonStrs = new String[m];
	
		for(int i = 0; i < n; i++)	sPhonStrs[i] = sPhons.get(i).print(); 
		for(int i = 0; i < m; i++)	tPhonStrs[i] = tPhons.get(i).print(); 
		
		int[][] distMatrix = new int[n][m], costMatrix = new int[n][m]; 
	
		//first we fill it with the base costs
		for(int i = 0; i < n; i++)
		{
			for(int j = 0; j < m; j++)
			{
				if( sPhonStrs[i].equals(tPhonStrs[j]) )	costMatrix[i][j] = 0; 
				else	costMatrix[i][j] = 1; 
			}
		}
		
		//then accumulate the Levenshtein Distance across the graph toward the bottom right
		//arbitrarily, do this top-right then down row by row (could also be done up-dwon then right)
		for(int i=0; i < n; i++)
		{
			for(int j = 0; j < m; j++)
			{
				distMatrix[i][j] = Math.min(distMatrix[i-1][j-1]+costMatrix[i-1][j-1],
						Math.min(distMatrix[i-1][j], distMatrix[i][j-1]) + 1); 
			}
		}
		
		return distMatrix[n-1][m-1]; 
	}
	
}