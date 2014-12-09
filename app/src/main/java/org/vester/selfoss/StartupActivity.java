package org.vester.selfoss;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class StartupActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Class<? extends Activity> activityClass;
		if (clientIsConfigured())
			activityClass = FeedEntryMainActivity.class;
		else
			activityClass = SetupActivity.class;

		Intent newActivity = new Intent(getApplicationContext(), activityClass);
		startActivityForResult(newActivity, 1);
	}

	private boolean clientIsConfigured() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		return !prefs.getString(SettingsActivity.URL, SettingsActivity.URL_DEFAULT).equals(SettingsActivity.URL_DEFAULT);
	}

}
