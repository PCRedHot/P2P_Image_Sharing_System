import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

/**
 * @author CHOI Chong Hing (UID: 3035564940)
 * The HashAlgoSHA1 class
 * It implements Hash interface and uses SHA-1 hashing algorithm to hash String
 */
public class HashAlgoSHA1 implements Hash {
	private MessageDigest md;
	
	/**
	 * The HashAlgoSHA1 constructor
	 * @throws NoSuchAlgorithmException
	 */
	public HashAlgoSHA1() throws NoSuchAlgorithmException {
		md = MessageDigest.getInstance("SHA-1");
	}

	/* (non-Javadoc)
	 * @see Hash#hash(java.lang.String)
	 */
	@Override
	public String hash(String input) {
		md.update(input.getBytes());
		byte[] hash;
		try {
			hash = md.digest(input.getBytes("UTF-8"));
			return bytesToHex(hash);
		} catch (UnsupportedEncodingException e) {}
        return null;
	}
	
	private String bytesToHex(byte[] hash) {
        return DatatypeConverter.printHexBinary(hash);
    }

}
