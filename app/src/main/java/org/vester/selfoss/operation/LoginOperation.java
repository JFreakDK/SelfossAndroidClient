package org.vester.selfoss.operation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;
import org.vester.selfoss.R;
import org.vester.selfoss.SetupActivity.LoginCallback;

import android.util.Log;

public class LoginOperation implements Operation {

	private String url;
	private final String password;
	private final String username;
	private final LoginCallback loginCallback;

	public LoginOperation(String username, String password, LoginCallback loginCallback) {
		this.username = username;
		this.password = password;
		this.loginCallback = loginCallback;

	}

	@Override
	public void setURL(String url) {
		this.url = url;
	}

	@Override
	public URL createURL() throws MalformedURLException {
		String password = "";
		try {
			password = URLEncoder.encode(this.password, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.e(LoginOperation.class.getName(), "Error occured while encoding password", e);
		}
		return new URL(url + "/login?username=" + username + "&password=" + password);
	}

	@Override
	public String getRequestMethod() {
		return "GET";
	}

	@Override
	public void processResponse(InputStream in) throws JSONException, IOException {
		BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		StringBuilder responseStrBuilder = new StringBuilder();
		String inputStr;
		while ((inputStr = streamReader.readLine()) != null) {
			responseStrBuilder.append(inputStr);
		}

		JSONObject json = new JSONObject(responseStrBuilder.toString());
		loginCallback.loginResult(json.getBoolean("success"));
	}

	@Override
	public void writeOutput(HttpURLConnection con) throws IOException {
		// Only used for POST requests
	}

	@Override
	public void setLogin(String login) {
	}

	@Override
	public int getOperationTitle() {
		return R.string.login;
	}

}
