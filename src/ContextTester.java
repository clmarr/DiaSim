import java.util.List;

/** 
 * 
 * @author Clayton Marr
 * used originally for debugging purposes, to ensure that the reading of all three original cascades
 * 	included comprehension of at least one rule with both prior and posterior context stipulations. 
 * TODO: possibly remove from public ready version. 
 */

public class ContextTester {
	
	private static final String[] filesToCheck = new String[] {"DiaCLEF", "BaseCLEF", "BaseCLEFstar"}; 
	
	public static void main(String args[])
	{
		for (String fileName : filesToCheck)
		{
			List<String> lines = UTILS.readFileLines(fileName); 
			System.out.println("For file "+fileName+"... "+hasALineWithBothPriorAndPosteriorDisjunctions(lines)); 
		}
	}
	
	private static boolean hasALineWithBothPriorAndPosteriorDisjunctions(List<String> lines)
	{
		String cf = SChangeFactory.contextFlag+"", locus = SChangeFactory.LOCUS,
				s = UTILS.CMT_FLAG+"";
		for (String line : lines)
		{
			if (line.contains(s))	line = line.substring(0, line.indexOf(s)); 
			if (!line.contains(cf)) return false;
			line = line.substring(line.indexOf(cf)+cf.length()); 
			if (!line.contains(locus)) return false;
			String prior = line.substring(0, line.indexOf(locus)), 
					postr = line.substring(line.indexOf(locus)+locus.length()); 
			if (prior.contains("{") && postr.contains("{"))	return true; 
		}
		return false; 
	}
}
