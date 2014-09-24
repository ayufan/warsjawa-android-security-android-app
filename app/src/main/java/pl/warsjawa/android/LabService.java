package pl.warsjawa.android;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import org.apache.http.auth.InvalidCredentialsException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class LabService extends Service {
    private static final String TAG = "LabService";
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int DATARETRIEVAL_TIMEOUT = 10000;

    private LabBinder mBinder = new LabBinder();

    public LabService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "Creating LabService...");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.v(TAG, "Destroying LabService...");
    }

    private static String createServiceUrl(String path, Map<String, String> query) {
        StringBuilder sb = new StringBuilder("https://salty-lake-7009.herokuapp.com" + path);

        sb.append('?');

        for (Map.Entry<String, String> kv : query.entrySet()) {
            sb.append(Uri.encode(kv.getKey()));
            sb.append('=');
            sb.append(Uri.encode(kv.getValue()));
            sb.append('&');
        }

        return sb.toString();
    }

    private static JSONObject requestGET(String serviceUrl) throws InvalidCredentialsException {
        disableConnectionReuseIfNecessary();

        HttpURLConnection urlConnection = null;
        try {
            URL urlToRequest = new URL(serviceUrl);
            urlConnection = (HttpURLConnection)
                    urlToRequest.openConnection();
            urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
            urlConnection.setReadTimeout(DATARETRIEVAL_TIMEOUT);

            // handle issues
            int statusCode = urlConnection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Log.v(TAG, "URL: " + serviceUrl + " => NotAuthorized");
                throw new InvalidCredentialsException();
            } else if (statusCode != HttpURLConnection.HTTP_OK) {
                Log.v(TAG, "URL: " + serviceUrl + " => StatusCode: " + String.valueOf(statusCode));
                throw new ProtocolException("HTTP status " + String.valueOf(statusCode));
            }

            // create JSON object from content
            InputStream in = new BufferedInputStream(
                    urlConnection.getInputStream());
            String text = getResponseText(in);
            Log.v(TAG, "URL: " + serviceUrl + "\nResponse: " + text);
            return new JSONObject(text);

        } catch (MalformedURLException e) {
            // URL is invalid
        } catch (SocketTimeoutException e) {
            // data retrieval or connection timed out
        } catch (IOException e) {
            // could not read response body
            // (could not create input stream)
        } catch (JSONException e) {
            // response body is no valid JSON string
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return null;
    }

    /**
     * required in order to prevent issues in earlier Android version.
     */
    private static void disableConnectionReuseIfNecessary() {
        // see HttpURLConnection API doc
        if (Integer.parseInt(Build.VERSION.SDK)
                < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    private static String getResponseText(InputStream inStream) {
        // very nice trick from
        // http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
        return new Scanner(inStream).useDelimiter("\\A").next();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public String login(final String email, final String password) {
        String url = createServiceUrl("/auth/login", new HashMap<String, String>() {{
            put("email", email);
            put("password", password);
        }});

        try {
            JSONObject jsonObject = requestGET(url);

            if(jsonObject == null)
                return null;

            if ("ok".equals(jsonObject.getString("status"))) {
                return jsonObject.getString("session");
            }
        } catch (InvalidCredentialsException e) {
            return null;
        } catch (JSONException e) {
            return null;
        }

        return null;
    }

    public String[] getItems(final String session) {
        if (TextUtils.isEmpty(session)) {
            return null;
        }

        String url = createServiceUrl("/user/items", new HashMap<String, String>() {{
            put("session", session);
        }});

        try {
            JSONObject jsonObject = requestGET(url);

            if(jsonObject == null)
                return null;

            if ("ok".equals(jsonObject.getString("status"))) {
                ArrayList<String> items = new ArrayList<String>();
                JSONArray jsonItems = jsonObject.getJSONArray("items");

                for (int i = 0; i < jsonItems.length(); ++i) {
                    String value = jsonItems.optString(i);
                    if (value != null)
                        items.add(value);
                }
                return items.toArray(new String[items.size()]);
            }
        } catch (InvalidCredentialsException e) {
            return null;
        } catch (JSONException e) {
            return null;
        }

        return null;
    }

    public class LabBinder extends Binder {
        LabService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LabService.this;
        }
    }
}
