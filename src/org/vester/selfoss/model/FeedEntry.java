package org.vester.selfoss.model;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class FeedEntry {

	public String title;
	public String id;
	public String content;
	private Object datetime;
	public boolean unread;
	public String link;
	public String icon;
	public String sourcetitle;
	private boolean starred;
	private String uid;

	public FeedEntry(JSONObject jsonObject) {
		try {
			id = jsonObject.getString("id");
			content = jsonObject.getString("content").trim();
			title = jsonObject.getString("title");
			datetime = jsonObject.getString("datetime");
			unread = jsonObject.getString("unread").equals("1");
			starred = jsonObject.getString("starred").equals("1");
			link = jsonObject.getString("link");
			icon = jsonObject.getString("icon");
			uid = jsonObject.getString("uid");
			sourcetitle = jsonObject.getString("sourcetitle");
		} catch (JSONException e) {
			Log.e(FeedEntry.class.getName(), "Could not set feed entry value: " + e.getMessage());
			e.printStackTrace();
		}

	}

	protected FeedEntry() {

	}

	@Override
	public String toString() {
		return "FeedEntry [title=" + title + ", id=" + id + ", datetime=" + datetime + "]";
	}

	public boolean isStared() {
		return starred;
	}

	public void setStarred(boolean starred) {
		this.starred = starred;
	}

}
