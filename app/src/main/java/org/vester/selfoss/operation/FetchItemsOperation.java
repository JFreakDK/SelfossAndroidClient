package org.vester.selfoss.operation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.vester.selfoss.FeedEntryMainActivity;
import org.vester.selfoss.R;
import org.vester.selfoss.SettingsActivity;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class FetchItemsOperation implements Operation {
	/**
	 * 
	 */
	private final FeedEntryMainActivity itemListActivity;
	protected String stringURL;
	private final String condition;
	private String login;

	public FetchItemsOperation(FeedEntryMainActivity itemListActivity) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(itemListActivity);
		this.condition = prefs.getString(SettingsActivity.TYPE, SettingsActivity.TYPE_DEFAULT);
		this.itemListActivity = itemListActivity;
	}

	@Override
	public String getRequestMethod() {
		return "GET";
	}

	@Override
	public URL createURL() throws MalformedURLException {
		String string = stringURL + "/items?" + condition;
		if (login != null) {
			if (!condition.isEmpty()) {
				string = string + "&";
			}
			string = string + login;
		}
		return new URL(string);
	}

	@Override
	public void setURL(String url) {
		this.stringURL = url;
	}

	@Override
	public void processResponse(InputStream in) throws JSONException, IOException {
		BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		StringBuilder responseStrBuilder = new StringBuilder();
		String inputStr;
		while ((inputStr = streamReader.readLine()) != null) {
			responseStrBuilder.append(inputStr);
		}

		JSONArray json = new JSONArray(responseStrBuilder.toString());
		itemListActivity.onEntriesFetched(json, isAppendEntries());
	}

	boolean isAppendEntries() {
		return false;
	}

	@Override
	public void writeOutput(HttpURLConnection con) throws IOException {
		// only used by POST request with a body

	}

	@Override
	public void setLogin(String login) {
		this.login = login;
	}

	@Override
	public int getOperationTitle() {
		return R.string.refresh_items;
	}

}