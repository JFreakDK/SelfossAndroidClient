package org.vester.selfoss.operation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;
import org.vester.selfoss.FeedEntryMainActivity;
import org.vester.selfoss.R;

public class MarkAllAsReadOperation implements Operation {

	private String url;
	private final Collection<String> ids;
	private final FeedEntryMainActivity itemListActivity;
	private String login;

	protected MarkAllAsReadOperation(Collection<String> ids, FeedEntryMainActivity itemListActivity) {
		this.ids = ids;
		this.itemListActivity = itemListActivity;
	}

	@Override
	public void setURL(String url) {
		this.url = url;

	}

	@Override
	public URL createURL() throws MalformedURLException {
		return new URL(url + "/mark/");
	}

	private String createParameter() throws UnsupportedEncodingException {
		StringBuilder s = new StringBuilder();
		for (String id : ids) {
			s.append("ids%5B%5D=" + id + "&");
		}
		if (login != null) {
			s.append(login);
		} else {
			s.deleteCharAt(s.length() - 1);
		}
		return s.toString();
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
			itemListActivity.markedAsRead(ids);
	}

	@Override
	public void writeOutput(HttpURLConnection con) throws IOException {
		con.setDoOutput(true);
		String parameter = createParameter();
		con.setChunkedStreamingMode(0);
		con.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		// con.setFixedLengthStreamingMode(parameter.getBytes().length);
		con.connect();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"));
		writer.write(parameter);
		writer.flush();
	}

	@Override
	public void setLogin(String login) {
		this.login = login;
	}

	@Override
	public int getOperationTitle() {
		return R.string.mark_all_as_read;
	}

}
