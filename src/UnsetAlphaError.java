
public class UnsetAlphaError extends Error {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6060952546862041401L;

	public UnsetAlphaError (String ch) {	super("UnsetAlphaException: cannot force truth when alpha style symbol "+ch+" remains uninitialized"); 
	}
}
