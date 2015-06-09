package org.vester.selfoss.operation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;
import org.vester.selfoss.R;
import org.vester.selfoss.listener.StarOperationListener;

public class StarOperation implements Operation {

	private String url;
	private final String id;
	private final StarOperationListener itemListActivity;
	private String login;

	protected StarOperation(String id, StarOperationListener listener) {
		this.id = id;
		this.itemListActivity = listener;
	}

	@Override
	public void setURL(String url) {
		this.url = url;

	}

	@Override
	public URL createURL() throws MalformedURLException {
		String localUrl = url + "/starr/" + id;
		if (login != null)
			localUrl = localUrl + "?" + login;
		return new URL(localUrl);
	}

	@Override
	public String getRequestMethod() {
		return "POST";
	}

	@Override
	public void processResponse(InputStream in) throws JSONException, IOException {
		BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		StringBuilder responseStrBuilder = new StringBuilder();
		String inputStr;
		while ((inputStr = streamReader.readLine()) != null) {
			responseStrBuilder.append(inputStr);
		}

		JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());
		if (jsonObject.has("success") && jsonObject.getBoolean("success") == true)
			itemListActivity.starred(id);
	}

	@Override
	public void writeOutput(HttpURLConnection con) throws IOException {
		con.setFixedLengthStreamingMode(0);
	}

	@Override
	public void setLogin(String login) {
		this.login = login;

	}

	@Override
	public int getOperationTitle() {
		return R.string.star_item;
	}

}
