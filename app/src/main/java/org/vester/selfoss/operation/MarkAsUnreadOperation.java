package org.vester.selfoss.operation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;
import org.vester.selfoss.R;
import org.vester.selfoss.listener.MarkAsUnreadOperationListener;

public class MarkAsUnreadOperation implements Operation {

	private String url;
	private final String id;
	private final MarkAsUnreadOperationListener markAsUnreadOperationListener;
	private String login;

	protected MarkAsUnreadOperation(String id, MarkAsUnreadOperationListener markAsUnreadOperationListener) {
		this.id = id;
		this.markAsUnreadOperationListener = markAsUnreadOperationListener;
	}

	@Override
	public void setURL(String url) {
		this.url = url;

	}

	@Override
	public URL createURL() throws MalformedURLException {
		return new URL(url + "/unmark/" + id);
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
			markAsUnreadOperationListener.markedAsUnread(id);
	}

	@Override
	public void writeOutput(HttpURLConnection con) throws IOException {
		String parameter = createParameter();
		con.setDoOutput(true);
		con.setFixedLengthStreamingMode(parameter.getBytes("UTF8").length);
		con.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.connect();
		if (parameter != null) {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"));
			writer.write(parameter);
			writer.flush();
		}

	}

	private String createParameter() {
		String parameters = null;
		if (login != null) {
			parameters = parameters + "&" + login;
		}
		return parameters;
	}

	@Override
	public void setLogin(String login) {
		this.login = login;

	}

	@Override
	public int getOperationTitle() {
		return R.string.mark_item_as_read;
	}

}
