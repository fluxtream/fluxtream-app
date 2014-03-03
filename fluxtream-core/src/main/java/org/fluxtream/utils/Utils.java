package org.fluxtream.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class Utils {
	
//	private static Policy policy;
//	
// 	@SuppressWarnings("deprecation")
// 	public static String clean(String userContent) {
// 		try {
// 			if (policy==null)
// 				policy = Policy.getInstance(Utils.class.getResourceAsStream("/antisamy-ebay-1.4.4.xml"));
// 
// 			AntiSamy as = new AntiSamy();
// 			CleanResults cr;
// 			cr = as.scan(userContent, policy);
// 			
// 			return cr.getCleanHTML();
// 		} catch (Exception e) {
// 			e.printStackTrace();
// 		}
// 		
// 		return "YOUR CONTENT COULD NOT BE VERIFIED FOR CODE INJECTION";
// 	}
    private static final char[] symbols = new char[36];

    static {
        for (int idx = 0; idx < 10; ++idx)
            symbols[idx] = (char) ('0' + idx);
        for (int idx = 10; idx < 36; ++idx)
            symbols[idx] = (char) ('a' + idx - 10);
    }

    private static SecureRandom random = new SecureRandom();

    public static String generateSecureRandomString()
    {
        return new BigInteger(200, random).toString(32);
    }

	public static final Map<String,String> parseParameters(String s){
		StringTokenizer st = new StringTokenizer(s, "=&"); 
		Map<String,String> result = new HashMap<String,String>();
		while(st.hasMoreTokens()) { 
			String key = st.nextToken(); 
			String val = st.nextToken();
			result.put(key, val);
		}
		return result;
	}
	
	public static String hash(String toHash)  {
		byte[] uniqueKey = toHash.getBytes();
		byte[] hash = null;
		try {
			hash = MessageDigest.getInstance("MD5").digest(uniqueKey);
		} catch (NoSuchAlgorithmException e) {e.printStackTrace();}
		StringBuilder hashString = new StringBuilder();
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(hash[i]);
			if (hex.length() == 1) {
				hashString.append('0');
				hashString.append(hex.charAt(hex.length() - 1));
			} else
				hashString.append(hex.substring(hex.length() - 2));
		}
		return hashString.toString();
	}
	
	public static String sha1Hash(String toHash) throws NoSuchAlgorithmException {
		byte[] uniqueKey = toHash.getBytes();
		byte[] hash = null;
		hash = MessageDigest.getInstance("SHA1").digest(uniqueKey);
		StringBuilder hashString = new StringBuilder();
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(hash[i]);
			if (hex.length() == 1) {
				hashString.append('0');
				hashString.append(hex.charAt(hex.length() - 1));
			} else
				hashString.append(hex.substring(hex.length() - 2));
		}
		return hashString.toString();
	}
	
	public static String shortStackTrace(Throwable t) {
		StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer));
		String trace = writer.toString();
		return trace.length()<64?trace:trace.substring(0,64);
	}
	
	public static String mediumStackTrace(Throwable t) {
		StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer));
		String trace = writer.toString();
		return trace.length()<64?trace:trace.substring(0,512);
	}
	
	public static String stackTrace(Throwable t) {
		StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}
	
}
