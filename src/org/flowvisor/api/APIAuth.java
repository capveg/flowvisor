/**
 * 
 */
package org.flowvisor.api;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.apache.xmlrpc.XmlRpcException;

import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfig;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping.AuthenticationHandler;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

/**
 * Figure out if this request should be allowed or not
 * 
 * @author capveg
 *
 */
public class APIAuth implements AuthenticationHandler {

	class AuthFailException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public AuthFailException(String string) {
			super(string);
		}
		
	}
	/* (non-Javadoc)
	 * @see org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping.AuthenticationHandler#isAuthorized(org.apache.xmlrpc.XmlRpcRequest)
	 */
	@Override
	public boolean isAuthorized(XmlRpcRequest req) throws XmlRpcException {
		
		String method = req.getMethodName();
		XmlRpcHttpRequestConfig config =
            (XmlRpcHttpRequestConfig) req.getConfig();
		String user = config.getBasicUserName();
		String passwd = config.getBasicPassword();
		APIUserCred.setUserName(config.getBasicUserName());
		try {
			if(user == null) 
				throw new AuthFailException("client did not try to auth");
			String salt = getPasswdElm(user, FVConfig.SLICE_SALT);
			String crypt = getPasswdElm(user,FVConfig.SLICE_CRYPT);
			String testCrypt = makeCrypt(salt,passwd);
			if( !crypt.equals(testCrypt))
				throw new AuthFailException("incorrect passwd for " + user);
		
		} catch (AuthFailException e) {
			String err = "API auth failed for: " + 
							method + "::" +e;
			FVLog.log(LogLevel.WARN, null,err);
			// throw new XmlRpcException(err);
			return false;
		}
		FVLog.log(LogLevel.DEBUG, null, "API auth " + 
				method + " for user '" + user + "'" );
		// HACK to tie this thread to the user
		
		return true;
	}
	public static String makeCrypt(String salt, String passwd){
	        MessageDigest md;
	        byte[] md5hash;
	        try {
				md = MessageDigest.getInstance("MD5");
				md5hash = new byte[32];
		        String text = salt + passwd;  // hash on both the salt and the passwd
		        md.update(text.getBytes("iso-8859-1"), 0, text.length());
		        md5hash = md.digest();
	        } catch (NoSuchAlgorithmException e) {
	        	e.printStackTrace();
	        	throw new RuntimeException(e);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
	        return convertToHex(md5hash);
	}
    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        return buf.toString();
    }

	
	private String getPasswdElm(String user,String elm) throws AuthFailException{
		String base = FVConfig.SLICES + "." + user;
			if(!FVConfig.confirm(base))
				throw new AuthFailException("unknown user " + user);
			try {
				return FVConfig.getString(base + "." + elm);
			} catch (ConfigError e) {
				String err = "server error: no " + elm + " found(!!) for user " + user;
				FVLog.log(LogLevel.ALERT, null,err );
				throw new AuthFailException(err);
			}
	}
	public static String getSalt() {
		Random rand = new Random();
		return Integer.valueOf(rand.nextInt()).toString();
	}
	
	/**
	 * Did changerSlice transitively create sliceName?
	 * @param changerSlice the slice trying to perform a change
	 * @param sliceName the slice being changes
	 * @return 
	 */
	public static boolean transitivelyCreated(String changerSlice,
			String sliceName) {
		String user = sliceName;
		if(FVConfig.isSupervisor(changerSlice)) // root created everyone
			return true;
		while(!FVConfig.isSupervisor(user)) {
			if(user.equals(changerSlice))
				return true;
			try {
				user = FVConfig.getString(FVConfig.SLICES + "." + user + "." + 
						FVConfig.SLICE_CREATOR);
			} catch (ConfigError e) {
				// FIXME: this config format is stupid
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		// changerSlice is not on the transitive path of who created sliceName
		return false;
	}
}
