package org.gmu.utils;

import android.util.Pair;
import android.webkit.CookieManager;


import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: minime2 Date: 28/09/11
 * Time: 9:13
 */
public class NetUtils {
    public static int READTIMEOUT = 8000;
    public static int CONNTIMEOUT = 4000;

    public static byte[] getURL(String myUrl) throws Exception {
        HttpURLConnection http = null;
        try {

            HttpURLConnection urlConnection = getConnection(myUrl);

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            return StreamUtils.inputStreamToBytes(in);
        } finally {
            if (http != null)
                http.disconnect();
        }
    }

    public static InputStream getInputStream(String myUrl, int bufferSize) throws Exception {
        HttpURLConnection http = null;

        try {

            HttpURLConnection urlConnection = getConnection(myUrl);


            InputStream in = new BufferedInputStream(urlConnection.getInputStream(), bufferSize);
            return in;
        } finally {
            if (http != null)
                http.disconnect();
        }
    }

    /**
     * @param myUrl
     * @param user
     * @param pass
     * @return
     * @throws Exception
     * @deprecated authentication are based on cookies
     */
    public static InputStream getInputStreamBasicAtuh(String myUrl, String user, String pass) throws Exception {
        HttpURLConnection http = null;

        user = "acasquero@gmu.com";
        pass = "acasquero";

        final String u = user;
        final String p = pass;
        try {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(u, p.toCharArray());
                }
            });


            HttpURLConnection urlConnection = getConnection(myUrl);


            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            return in;
        } finally {
            if (http != null)
                http.disconnect();
        }
    }

    private static void syncCookies(HttpURLConnection conn) {

        try {
            conn.setRequestProperty("Cookie", CookieManager.getInstance().getCookie(conn.getURL().toString()));
        } catch (Throwable ign) {//ignore
        }

    }

    public static Boolean doLogin(String myUrl, String user, String pass) {
        HttpURLConnection http = null;
        final String u = user;
        final String p = pass;
        try {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(u, p.toCharArray());
                }
            });

            HttpURLConnection urlConnection = NetUtils.getConnection(myUrl);

            int statusCode = urlConnection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (http != null)
                http.disconnect();
        }
        return false;
    }

    public static byte[] postUrl(String myUrl, List<Pair<String,String>> nameValuePairs) throws Exception {


        HttpURLConnection connection = getConnection(myUrl);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        String urlParameters = toParams(nameValuePairs);
        connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
        connection.setRequestProperty("Content-Language", "en-US");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);


        //Send request
        DataOutputStream wr = new DataOutputStream(
                connection.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        return StreamUtils.inputStreamToBytes(new BufferedInputStream(connection.getInputStream()));

    }

    private static String toParams(List<Pair<String,String>> nameValuePairs) {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < nameValuePairs.size(); i++) {
            if (i > 0) ret.append("&");
            Pair<String,String> nameValuePair = nameValuePairs.get(i);
            ret.append(nameValuePair.first + "=" + URLEncoder.encode(nameValuePair.second));
        }
        return ret.toString();
    }

    public static HttpURLConnection getConnection(String myUrl) throws Exception {

        HttpURLConnection http = null;

        //disable cache

        if(!myUrl.contains("?"))
        {
            myUrl=myUrl+"?gmuts="+System.currentTimeMillis();
        }

        URL url = new URL(myUrl);
        if (url.getProtocol().toLowerCase().equals("https")) {
            trustAllHosts();
            HttpsURLConnection https = (HttpsURLConnection) url.openConnection();

            https.setHostnameVerifier(DO_NOT_VERIFY);
            http = https;
        } else {

            http = (HttpURLConnection) url.openConnection();
        }
        http.setConnectTimeout(CONNTIMEOUT);
        http.setReadTimeout(READTIMEOUT);
        http.setUseCaches(false);
        //syncCookies(http);
        return http;
    }

    // always verify the host - dont check for certificate
    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * Trust every server - dont check for any certificate
     */
    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509AllTrust()};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
