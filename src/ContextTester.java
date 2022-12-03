import java.util.List;

/** 
 * 
 * @author Clayton Marr
 * used originally for debugging purposes, to ensure that the reading of all three original cascades
 * 	included comprehension of at least one rule with both prior and posterior context stipulations. 
 * currently it should be true for BaseCLEF and BaseCLEFstar because their last line is 
 * 		ə > ∅ / {#;[+syl]} [-syl] __ [-syl] {#;[+syl]}
 * but false for DiaCLEF because, in DiaCLEF, that line has been commented out. 
 * TODO: possibly remove from public ready version. (@Dec 3: why remove it though?) 
 */

public class ContextTester {
	
	private static final String[] filesToCheck = new String[] {"DiaCLEF", "BaseCLEF", "BaseCLEFstar"}; 
	
	public static void main(String args[])
	{
		for (String fileName : filesToCheck)
		{
			List<String> lines = UTILS.readFileLines(fileName); 
			System.out.println("File "+fileName+" has a line with both prior and posterior disjunctions?"
					+ "\n\t... "+hasALineWithBothPriorAndPosteriorDisjunctions(lines)); 
		}
	}
	
	private static boolean hasALineWithBothPriorAndPosteriorDisjunctions(List<String> lines)
	{
		String cf = SChangeFactory.contextFlag+"", locus = SChangeFactory.LOCUS,
				s = UTILS.CMT_FLAG+"";

		for (String line : lines)
		{
			if (line.contains(s))	line = line.substring(0, line.indexOf(s)); 
			if (!line.contains(cf)) continue; 
			line = line.substring(line.indexOf(cf) + cf.length()); 
			if (!line.contains(locus)) continue; 
			String prior = line.substring(0, line.indexOf(locus)), 
					postr = line.substring(line.indexOf(locus)+locus.length()); 
			if (prior.contains("{") && postr.contains("{"))	return true; 
		}
		
		return false; 
	}
}
