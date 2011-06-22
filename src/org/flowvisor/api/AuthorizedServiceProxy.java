package org.flowvisor.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;

import lib.jsonrpc.BasicObjectFactory;
import lib.jsonrpc.ObjectFactory;
import lib.jsonrpc.RPCException;
import lib.jsonrpc.ServiceProxy;
import lib.jsonrpc.Utils;

import net.sf.cglib.proxy.MethodProxy;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.BCodec;
import org.eclipse.jetty.http.HttpHeaders;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AuthorizedServiceProxy  extends ServiceProxy{

	private String user;

	private String passwd;

	private String url;

	private int nextId =0;

    /** The factory. */
    ObjectFactory factory;

	public AuthorizedServiceProxy(Class<?> serviceInterface, String url, String user, String passwd) {
		super(serviceInterface, url);
		this.user = user;
		this.passwd = passwd;
		this.url = url;
		this.factory = new BasicObjectFactory();
	}


    /**
     * Next id.
     *
     * @return the int
     */
    private synchronized int nextId(){
            nextId += 1;
            return nextId;
    }



    /* (non-Javadoc)
     * @see net.sf.cglib.proxy.MethodInterceptor#intercept(java.lang.Object, java.lang.reflect.Method, java.lang.Object[], net.sf.cglib.proxy.MethodProxy)
     */
    @Override
    public Object intercept(Object obj, Method method, Object[] args,
                    MethodProxy proxy) throws RPCException {

            try {
                    JSONArray jargs = encodeArguments(method, args);
                    return post(method, jargs);
            } catch (JSONException e) {
            		System.out.println(e.getMessage());
                    throw new RPCException(RPCException.PARSE_ERROR, e.getMessage(), e);
            } catch (IllegalAccessException e) {
            	System.out.println(e.getMessage());
                    throw new RPCException(RPCException.METHOD_NOT_FOUND, e.getMessage(), e);
            } catch (InvocationTargetException e) {
            	System.out.println(e.getMessage());
                    throw new RPCException(RPCException.INTERNAL_ERROR, e.getMessage(), e);
            } catch (IOException e) {
            	System.out.println(e.getMessage());
                    throw new RPCException(RPCException.INTERNAL_ERROR, e.getMessage(), e);
            } catch (InstantiationException e) {
                    System.out.println(e.getMessage());
                    throw new RPCException(RPCException.INTERNAL_ERROR, e.getMessage(), e);
            }

    }

	 /**
     * Post.
     *
     * @param method the method
     * @param args the args
     * @return the object
     * @throws JSONException the jSON exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InstantiationException the instantiation exception
     * @throws IllegalAccessException the illegal access exception
     * @throws InvocationTargetException the invocation target exception
     */
    protected Object post(Method method, JSONArray args) throws JSONException,
                    IOException, InstantiationException, IllegalAccessException,
                    InvocationTargetException {

            StringBuffer sb = new StringBuffer();
            HttpURLConnection connection = null;
            OutputStreamWriter writer = null;
            InputStreamReader reader = null;

            int responseCode = 200;

            try {
            		JSONObject packet = new JSONObject();
                    packet.put("jsonrpc", "2.0");
                    packet.put("method", method.getName());
                    packet.put("params", args);
                    packet.put("id", nextId());


                    sb = new StringBuffer();
                    char[] buff = new char[1024];

                    URL u = new URL(url);
                    connection = (HttpURLConnection) u.openConnection();

                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type",
                                    "application/x-www-form-urlencoded");
                    connection.setDoOutput(true);

    	            String encodedAuth;
					try {
						encodedAuth = new BCodec().encode (user + ":" + passwd);
					} catch (EncoderException e) {
						encodedAuth = "";
					}
    	            connection.setRequestProperty(HttpHeaders.AUTHORIZATION, encodedAuth);

                    writer = new OutputStreamWriter(connection.getOutputStream(),
                                    "UTF-8");
                    writer.write(packet.toString());
                    writer.flush();

                    responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                            reader = new InputStreamReader(connection.getInputStream(),
                                            "UTF-8");
                            int i = 0;
                            while ((i = reader.read(buff, 0, 1024)) != -1) {
                                    sb.append(buff, 0, i);
                            }

                            String val = sb.toString();
                            JSONObject jobj = new JSONObject(val);
                            return parseReturnValue(method, jobj, factory);
                    } else
                            throw new RPCException(RPCException.INTERNAL_ERROR, "response code is:" + responseCode);
            } finally {
                    if (writer != null)
                            try {
                                    writer.close();
                            } catch (Exception e) {
                            }
                    if (reader != null)
                            try {
                                    reader.close();
                            } catch (Exception e) {
                            }
                    if (connection != null) {
                            try {
                                    connection.disconnect();
                            } catch (Exception e) {
                            }
                    }
            }

    }

    /**
     * Parses the return value.
     *
     * @param method the method
     * @param pkt the pkt
     * @param factory the factory
     * @return the object
     * @throws JSONException the jSON exception
     * @throws InstantiationException the instantiation exception
     * @throws IllegalAccessException the illegal access exception
     * @throws InvocationTargetException the invocation target exception
     */
    public static Object parseReturnValue(Method method, JSONObject pkt,
                    lib.jsonrpc.ObjectFactory factory) throws JSONException,
                    InstantiationException, IllegalAccessException,
                    InvocationTargetException {
            if (pkt.has("error")) {
                    String errorMessage = pkt.optString("message");
                    long errorCode = pkt.optInt("code");
                    String errorData = pkt.optString("data");
                    RPCException e = new RPCException(errorCode, errorMessage);
                    e.setData(errorData);
                    throw e;
            }

            Type type = method.getGenericReturnType();
            Object obj = Utils.getJSONProperty(type, type, pkt,
                            "result", factory);
            return obj;

    }

    /**
     * Encode arguments.
     *
     * @param method the method
     * @param args the args
     * @return the jSON array
     * @throws JSONException the jSON exception
     * @throws IllegalAccessException the illegal access exception
     * @throws InvocationTargetException the invocation target exception
     */
    public static JSONArray encodeArguments(Method method, Object[] args)
                    throws JSONException, IllegalAccessException,
                    InvocationTargetException {
            JSONArray result = new JSONArray();

            Type[] argTypes = method.getGenericParameterTypes();

            for (int i = 0; i < argTypes.length; i++) {
                    Utils.putObjectToJSONArray(argTypes[i], result, i,
                                    args[i]);
            }
            return result;
    }

}
