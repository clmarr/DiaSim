import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class UTILS {

	public static String fillSpaceToN(String inp, int n)
	{
		String out = inp+"";
		while (out.length() < n)	out += " ";
		return out; 
	}
	
	//to use for checking if an entered etymon or rule id is valid. 
	// max should be the number of words in the lexicon minus 1 (for an etymon)
		// or the length of the cascade (for a rule)
	public static int getValidInd(String s, int max)
	{
		int output; 
		try 		{	output = Integer.parseInt(s);	} 
		catch (NumberFormatException | NullPointerException nfe) {
	        return -1;	}
		return output <= max ? output : -1; 
	}
	

	public static String etymInds(LexPhon[] etList, LexPhon etTarg)
	{
		String output = ""; 
		for (int wli = 0; wli < etList.length; wli++)
			if(etList[wli].toString().equals(etTarg.toString()))
				output += output.equals("") ? ""+wli : ", "+wli;
		return output;
	}
	
	public static Simulation toyDerivation(LexPhon[] inps, List<SChange> ruleCascade )
	{
		Simulation toy = new Simulation(inps, ruleCascade); 
		toy.simulateToEnd();
		return toy; 
	}
	

	//auxiliary
	public static void writeToFile(String filename, String output)
	{	try 
		{	
			int dirBreak = filename.indexOf("/");

			while (dirBreak != -1)
			{
				String curDir = filename.substring(0, dirBreak),
						rem = filename.substring(dirBreak+1); 
				if (!new File(curDir).exists()) 
					new File(curDir).mkdirs(); 
				
				dirBreak = !rem.contains("/") ? -1 : 
					dirBreak + 1 + rem.indexOf("/"); 
			
			}
			
			System.out.println("out loc : "+filename); 
			
			BufferedWriter out = new BufferedWriter(new FileWriter(filename)); 
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
	
}
