package com.vanillaforums;

import java.util.*;

/**
 * @author Todd Burry <todd@vanillaforums.com>
 * @version 1.0b
 * This object contains the client code for Vanilla jsConnect signle-sign-on.
 */
public class jsConnect {

   /**
    * Convenience method that returns a map representing an error.
    * @param code The code of the error.
    * @param message A user-readable message for the error.
    * @return 
    */
   protected static Map Error(String code, String message) {
      Map result = new HashMap();
      result.put("error", code);
      result.put("message", message);

      return result;
   }

   /**
    * Returns a JSONP formatted string suitable to be consumed by jsConnect.
    * This is usually the only method you need to call in order to implement jsConnect.
    * @param user A map containing the user information. The map should have the following keys:
    *  - uniqueid: An ID that uniquely identifies the user in your system. This value should never change for a given user.
    * @param request: A map containing the query string for the current request. You usually just pass in request.getParameterMap().
    * @param clientID: The client ID for your site. This is usually configured on Vanilla's jsConnect configuration page.
    * @param secret: The secret for your site. This is usually configured on Vanilla's jsConnect configuration page.
    * @param secure: Whether or not to check security on the request. You can leave this false for testing, but you should make it true in production.
    * @return The JSONP formatted string representing the current user.
    */
   public static String GetJsConnectString(Map user, Map request, String clientID, String secret, Boolean secure) {
      Map error = null;

      long timestamp = 0;
      try {
         timestamp = Long.parseLong(Val(request, "timestamp"));
      } catch (Exception ex) {
         timestamp = 0;
      }
      long currentTimestamp = jsConnect.Timestamp();

      if (secure) {
         if (Val(request, "client_id") == null) {
            error = jsConnect.Error("invalid_request", "The client_id parameter is missing.");
         } else if (!Val(request, "client_id").equals(clientID)) {
            error = jsConnect.Error("invalid_client", "Unknown client " + Val(request, "client_id") + ".");
         } else if (Val(request, "timestamp") == null && Val(request, "signature") == null) {
            if (user != null && !user.isEmpty()) {
               error = new HashMap();
               error.put("name", user.get("name"));
               error.put("photourl", user.containsKey("photourl") ? user.get("photourl") : "");
            } else {
               error = new HashMap();
               error.put("name", "");
               error.put("photourl", "");
            }
         } else if (timestamp == 0) {
            error = jsConnect.Error("invalid_request", "The timestamp is missing or invalid.");
         } else if (Val(request, "signature") == null) {
            error = jsConnect.Error("invalid_request", "The signature is missing.");
         } else if (Math.abs(currentTimestamp - timestamp) > 30 * 60) {
            error = jsConnect.Error("invalid_request", "The timestamp is invalid.");
         } else {
            // Make sure the timestamp's signature checks out.
            String timestampSig = jsConnect.MD5(Long.toString(timestamp) + secret);
            if (!timestampSig.equals(Val(request, "signature"))) {
               error = jsConnect.Error("access_denied", "Signature invalid.");
            }
         }
      }

      Map result;

      if (error != null) {
         result = error;
      } else if (user != null && !user.isEmpty()) {
         result = new LinkedHashMap(user);
         SignJsConnect(result, clientID, secret, true);
      } else {
         result = new LinkedHashMap();
         result.put("name", "");
         result.put("photourl", "");
      }

      String json = jsConnect.JsonEncode(result);
      if (Val(request, "callback") == null) {
         return json;
      } else {
         return Val(request, "callback") + "(" + json + ");";
      }
   }

   /**
    * JSON encode some data.
    * @param data The data to encode.
    * @return The JSON encoded data.
    */
   public static String JsonEncode(Map data) {
      StringBuilder result = new StringBuilder();
      Iterator iterator = data.entrySet().iterator();

      while (iterator.hasNext()) {
         if (result.length() > 0) {
            result.append(", ");
         }

         Map.Entry v = (Map.Entry) iterator.next();

         String key = v.getKey().toString();
         key = key.replace("\"", "\\\"");

         String value = v.getValue().toString();
         value = value.replace("\"", "\\\"");
         String q = "\"";

         result.append(q + key + q + ": " + q + value + q);
      }

      return "{ " + result.toString() + " }";
   }

   /**
    * Compute the MD5 hash of a string.
    * @param password The data to compute the hash on.
    * @return A hex encoded string representing the MD5 hash of the string.
    */
   public static String MD5(String password) {
      try {
         java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
         digest.update(password.getBytes("UTF-8"));
         byte[] hash = digest.digest();

         StringBuilder ret = new StringBuilder();
         for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xFF & hash[i]);
            if (hex.length() == 1) {
               // could use a for loop, but we're only dealing with a single byte
               ret.append('0');
            }
            ret.append(hex);
         }
         return ret.toString();
      } catch (Exception ex) {
         return "ERROR";
      }
   }

   /**
    * Get a value from a map.
    * @param request The map to get the value from.
    * @param key The key of the value.
    * @param defaultValue The default value if the map doesn't contain the value.
    * @return The value from the map or the default if it isn't found.
    */
   protected static String Val(Map request, String key, String defaultValue) {
      try {
         Object result = null;
         if (request.containsKey(key)) {
            result = request.get(key);
            if (result instanceof String[]) {
               return ((String[]) request.get(key))[0];
            } else {
               return result.toString();
            }
         }
      } catch (Exception ex) {
         return defaultValue;
      }
      return defaultValue;
   }

   /**
    * Get a value from a map.
    * @param request The map to get the value from.
    * @param key The key of the value.
    * @return The value from the map or the null if it isn't found.
    */
   protected static String Val(Map request, String key) {
      return Val(request, key, null);
   }

   /**
    * Sign a jsConnect response. Responses are signed so that the site requesting the response knows that this is a valid site signing in.
    * @param data The data to sign.
    * @param clientID The client ID of the site. This is usually configured on Vanilla's jsConnect configuration page.
    * @param secret The secret of the site. This is usually configured on Vanilla's jsConnect configuration page.
    * @param setData Whether or not to add the signature information to the data.
    * @return The computed signature of the data.
    */
   public static String SignJsConnect(Map data, String clientID, String secret, Boolean setData) {
      // Generate a sorted list of the keys.
      String[] keys = new String[data.keySet().size()];
      data.keySet().toArray(keys);
      Arrays.sort(keys, String.CASE_INSENSITIVE_ORDER);

      // Generate the String to sign.
      StringBuilder sigStr = new StringBuilder();
      for (int i = 0; i < keys.length; i++) {
         if (sigStr.length() > 0) {
            sigStr.append("&");
         }

         String key = keys[i];
         String value = data.get(key).toString();

         try {
            sigStr.append(java.net.URLEncoder.encode(key.toLowerCase(), "UTF-8"));
            sigStr.append("=");
            sigStr.append(java.net.URLEncoder.encode(value, "UTF-8"));
         } catch (Exception ex) {
            if (setData) {
               data.put("clientid", clientID);
               data.put("signature", "ERROR");
            }
            return "ERROR";
         }
      }

      // MD5 sign the String with the secret.
      String signature = jsConnect.MD5(sigStr.toString() + secret);

      if (setData) {
         data.put("clientid", clientID);
         data.put("signature", signature);
      }
      return signature;
   }

   /**
    * Returns the current timestamp of the server, suitable for synching with the site.
    * @return The current timestamp.
    */
   public static long Timestamp() {
      long result = System.currentTimeMillis() / 1000;
      return result;
   }
}
