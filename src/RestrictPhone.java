import java.util.List;

public interface RestrictPhone{
	public String print(); 
	public boolean compare(List<SequentialPhonic> phonSeg, int ind);
	public boolean compare(SequentialPhonic candPh);
	public List<SequentialPhonic> forceTruth(List<SequentialPhonic> patientSeq, int ind); 
	//public SequentialPhonic forceTruth(SequentialPhonic patient); 
	
}
