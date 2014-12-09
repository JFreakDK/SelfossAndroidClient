package org.vester.selfoss;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONException;
import org.vester.selfoss.operation.FetchItemsOperation;
import org.vester.selfoss.operation.Operation;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

public class SelfossTask implements Runnable {

	private String stringURL;
	private String backupURL;

	private final Operation operation;
	private SharedPreferences prefs;
	private int connectionTimeout;
	private String login;
	private ErrorCallback errorCallBack;
	private FeedEntryAdapter adapter;

	public SelfossTask(final Operation operation, Context context, ErrorCallback errorCallBack) {
		this(operation, context, errorCallBack, (FeedEntryAdapter) null);
	}

	public SelfossTask(final Operation operation, Context context, ErrorCallback errorCallBack, String stringURL) {
		this(operation, context, errorCallBack, (FeedEntryAdapter) null);
		this.stringURL = stringURL;
	}

	public SelfossTask(final Operation operation, Context context, ErrorCallback errorCallBack, FeedEntryAdapter adapter) {
		this.errorCallBack = errorCallBack;
		this.adapter = adapter;
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String connectionTimeout = prefs.getString(SettingsActivity.CONNECTION_TIMEOUT, "5");
		if (connectionTimeout.isEmpty()) {
			connectionTimeout = "0";
		}
		this.connectionTimeout = Integer.parseInt(connectionTimeout);
		if (stringURL == null)
			this.stringURL = prefs.getString(SettingsActivity.URL, SettingsActivity.URL_DEFAULT);
		this.backupURL = prefs.getString(SettingsActivity.BACKUP_URL, SettingsActivity.URL_DEFAULT);
		this.operation = operation;
		String username = prefs.getString(SettingsActivity.USERNAME, null);
		String password = prefs.getString(SettingsActivity.PASSWORD, null);
		if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
			try {
				this.login = "username=" + username + "&password=" + URLEncoder.encode(password, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				this.login = "";
				Log.e(FetchItemsOperation.class.getName(), "Error occured while trying to encode password", e);
			}
		}
	}

	@Override
	public void run() {
		if (adapter != null)
			adapter.setIsLoading(true);
		HttpURLConnection con = null;
		try {
			operation.setLogin(login);
			operation.setURL(stringURL);

			con = setupConnection(operation.createURL(), operation.getRequestMethod(), connectionTimeout);
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			executeURL(operation, con);
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			if (con.getResponseCode() == HttpsURLConnection.HTTP_OK) {
				operation.processResponse(con.getInputStream());
			} else {
				Log.e(SelfossTask.class.getName(), "Response code was not 200 OK but: " + con.getResponseCode());
			}
			String url = prefs.getString(SettingsActivity.URL, SettingsActivity.URL_DEFAULT);
			Editor editor = prefs.edit();
			editor.putString(SettingsActivity.URL, stringURL);
			editor.putString(SettingsActivity.BACKUP_URL, backupURL);
			editor.commit();
		} catch (ConnectException e) {
			if (backupURL != null) {
				Log.d(SelfossTask.class.getName(), " connect exception occured trying to connect to " + stringURL, e);
                String tempURL = stringURL;
                stringURL = backupURL;
                backupURL = tempURL;
				run();
			} else {
				errorCallBack.errorOccured(stringURL, operation, e);
				Log.e(SelfossTask.class.getName(), "ConnectException occured trying to connect to " + stringURL + ", Operation: " + operation, e);
			}
		} catch (SocketTimeoutException e) {
			String orgUrl = stringURL;
			if (backupURL != null) {
				Log.d(SelfossTask.class.getName(), "Timeout occurred trying to connect to " + stringURL + ", Operation: " + operation, e);
                String tempURL = stringURL;
				stringURL = backupURL;
                backupURL = tempURL;
				run();
			} else {
				errorCallBack.errorOccured(stringURL, operation, e);
				Log.e(SelfossTask.class.getName(), "Timeout occurred trying to connect to " + stringURL + ", Operation: " + operation, e);
			}
		} catch (MalformedURLException e) {
			errorCallBack.errorOccured(stringURL, operation, e);
			e.printStackTrace();
		} catch (ProtocolException e) {
			errorCallBack.errorOccured(stringURL, operation, e);
			e.printStackTrace();
		} catch (IOException e) {
			errorCallBack.errorOccured(stringURL, operation, e);
			e.printStackTrace();
		} catch (InterruptedException e) {
			errorCallBack.errorOccured(stringURL, operation, e);
			Log.e(SelfossTask.class.getName(), "InterruptedException occured for: " + stringURL + ", Operation: " + operation, e);
		} catch (JSONException e) {
			errorCallBack.errorOccured(stringURL, operation, e);
			Log.e(SelfossTask.class.getName(), "JSONException occured for: " + stringURL + ", Operation: " + operation, e);
		} catch (KeyManagementException e) {
			errorCallBack.errorOccured(stringURL, operation, e);
			Log.e(SelfossTask.class.getName(), "KeyManagementException occured for: " + stringURL + ", Operation: " + operation, e);
		} catch (NoSuchAlgorithmException e) {
			errorCallBack.errorOccured(stringURL, operation, e);
			Log.e(SelfossTask.class.getName(), "NoSuchAlgorithmException occured for: " + stringURL + ", Operation: " + operation, e);
		} finally {
			if (adapter != null)
				adapter.setIsLoading(false);
			con.disconnect();
		}
	}

	private void executeURL(Operation executor, HttpURLConnection con) throws MalformedURLException, IOException, ProtocolException, InterruptedException, UnsupportedEncodingException {
		con.setDoInput(true);
		if (executor.getRequestMethod().equals("POST")) {
			executor.writeOutput(con);
		} else {
			con.connect();
		}

		if (Thread.interrupted()) {
			throw new InterruptedException();
		}
		if (con.getResponseCode() == HttpsURLConnection.HTTP_OK) {
			Log.d(SelfossTask.class.getName(), "Execution of " + executor + " executed successfully");
		} else {
			Log.e(SelfossTask.class.getName(), "Response code from server was: " + con.getResponseCode());
			InputStream in = con.getErrorStream();
			BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

			String inputStr;
			StringBuilder errorStrBuilder = new StringBuilder();
			while ((inputStr = streamReader.readLine()) != null) {
				errorStrBuilder.append(inputStr);
			}
			Log.e(SelfossTask.class.getName(), "Message from server: " + errorStrBuilder.toString());

		}
	}

	public static HttpURLConnection setupConnection(URL url, String requestMethod, int timeout) throws MalformedURLException, IOException, ProtocolException, KeyManagementException,
			NoSuchAlgorithmException {
		SSLContext sslContext = initSSLContext();
		URLConnection connection = url.openConnection();
		if (connection instanceof HttpsURLConnection) {
			HttpsURLConnection con = (HttpsURLConnection) connection;
			con.setSSLSocketFactory(sslContext.getSocketFactory());
			con.setConnectTimeout(1000 * timeout);
			con.setHostnameVerifier(new HostnameVerifier() {

				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});
		}
		if (connection instanceof HttpURLConnection) {
			HttpURLConnection con = (HttpURLConnection) connection;
			con.setRequestMethod(requestMethod);
			return con;
		}
		return null;
	}

	static SSLContext initSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
		TrustManager tm = new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new TrustManager[] { tm }, null);
		return sslContext;
	}

}
