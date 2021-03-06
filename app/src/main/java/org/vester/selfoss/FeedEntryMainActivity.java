package org.vester.selfoss;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vester.selfoss.model.FeedEntry;
import org.vester.selfoss.operation.Operation;
import org.vester.selfoss.operation.OperationFactory;
import org.vester.selfoss.operation.SelfossOperationFactory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

/**
 * An activity representing a list of Items. This activity has different presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a {@link FeedEntryContentActivity} representing item details. On tablets, the activity
 * presents the list of items and item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a {@link FeedEntryRowFragment} and the item details (if present) is a
 * {@link FeedEntryContentFragment}.
 * <p>
 * This activity also implements the required {@link FeedEntryRowFragment.Callbacks} interface to listen for item selections.
 */
public class FeedEntryMainActivity extends FragmentActivity implements FeedEntryRowFragment.Callbacks, OnScrollListener, ErrorCallback {

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
	 */
	private boolean mTwoPane;
	private ExecutorService fetchThread;
	private ExecutorService markAsReadThread;
	private Runnable updateTask;
	private Handler guiThread;

	private OperationFactory operationFactory;

	private Future<?> updatePending;
	private Future<?> markAllAsReadPending;
	private Future<?> fetchMoreItemsPending;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private String[] options;

	private String[] apiOption = new String[] { "", SettingsActivity.TYPE_UNREAD, SettingsActivity.TYPE_STARRED };

	private String mDrawerTitle = "Navigation";
	private CharSequence mTitle = "Selfoss";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		operationFactory = SelfossOperationFactory.getInstance();
		setContentView(R.layout.feed_entry_list);
		initThreading();
		FeedEntryRowFragment itemListFragment = (FeedEntryRowFragment) getSupportFragmentManager().findFragmentById(R.id.item_list);
		itemListFragment.getListView().setOnScrollListener(this);
		if (findViewById(R.id.item_detail_container) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;

			// In two-pane mode, list items should be given the
			// 'activated' state when touched.
			itemListFragment.setActivateOnItemClick(true);
		} else {
			mTwoPane = false;
		}
		if (FeedEntryRowFragment.items.isEmpty())
			guiThread.post(updateTask);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		Resources res = getResources();
		options = res.getStringArray(R.array.options_array);
		// Set the adapter for the list view
		mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.navigation_drawer_row, options));
		updateDrawerList();

		// Set the list's click listener
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
		R.string.drawer_open, /* "open drawer" description */
		R.string.drawer_close /* "close drawer" description */
		);

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	private void updateDrawerList() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String type = prefs.getString(SettingsActivity.TYPE, SettingsActivity.TYPE_DEFAULT);

		for (int i = 0; i < apiOption.length; i++) {
			String option = apiOption[i];
			if (option.equals(type)) {
				mDrawerList.setItemChecked(i, true);
				setTitle(options[i]);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
		case R.id.settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		case R.id.mark_all_as_read:
			if (markAllAsReadPending == null || markAllAsReadPending.isDone()) {
				markAllAsReadPending = markAsReadThread.submit(new SelfossTask(operationFactory.createMarkAllAsReadOperation(
						filter(FeedEntryRowFragment.items), this), this, this));
				guiThread.post(updateTask);
			}
			break;
		case R.id.refresh:
			guiThread.post(updateTask);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private Collection<String> filter(List<FeedEntry> items) {
		Collection<String> ids = new ArrayList<String>();
		for (FeedEntry feedEntry : items) {
			if (feedEntry.unread) {
				ids.add(feedEntry.id);
			}
		}
		return ids;
	}

	/**
	 * Callback method from {@link FeedEntryRowFragment.Callbacks} indicating that the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(final String id) {
		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putString(FeedEntryContentFragment.ARG_ITEM_ID, id);
			FeedEntryContentFragment fragment = new FeedEntryContentFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction().replace(R.id.item_detail_container, fragment).commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID.
			Intent detailIntent = new Intent(this, FeedEntryContentActivity.class);
			detailIntent.putExtra(FeedEntryContentFragment.ARG_ITEM_ID, id);
			startActivity(detailIntent);
		}
		guiThread.post(new Runnable() {

			@Override
			public void run() {
				try {
					SelfossTask task = new SelfossTask(operationFactory.createMarkAsReadOperation(id, FeedEntryMainActivity.this),
							FeedEntryMainActivity.this, FeedEntryMainActivity.this);
					fetchThread.submit(task);
				} catch (RejectedExecutionException e) {
					Log.e(FeedEntryMainActivity.class.getName(), "RejectedExecutionException occured");
				}
			}
		});

	}

	private void initThreading() {
		guiThread = new Handler();
		fetchThread = Executors.newSingleThreadExecutor();
		markAsReadThread = Executors.newCachedThreadPool();
		updateTask = new Runnable() {

			@Override
			public void run() {
				try {
					if (updatePending == null || updatePending.isDone()) {
						FeedEntryRowFragment itemListFragment = (FeedEntryRowFragment) getSupportFragmentManager().findFragmentById(R.id.item_list);
						FeedEntryAdapter adapter = (FeedEntryAdapter) itemListFragment.getAdapter();
						SelfossTask task = new SelfossTask(operationFactory.createFetchItemsOperation(FeedEntryMainActivity.this),
								FeedEntryMainActivity.this, FeedEntryMainActivity.this, adapter);
						updatePending = fetchThread.submit(task);
					}
				} catch (RejectedExecutionException e) {
					Log.e(FeedEntryMainActivity.class.getName(), "RejectedExecutionException occured");
				}

			}
		};
	}

	public void onEntriesFetched(final JSONArray json, final boolean append) {
		guiThread.post(new Runnable() {

			@Override
			public void run() {
				if (!append) {
					FeedEntryRowFragment.items.clear();
				}
				for (int i = 0; i < json.length(); i++) {
					try {
						JSONObject jsonObject = (JSONObject) json.get(i);
						FeedEntryRowFragment.items.add(new FeedEntry(jsonObject));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public void markedAsRead(final Collection<String> ids) {
		guiThread.post(new Runnable() {

			@Override
			public void run() {
				for (String id : ids) {
					for (FeedEntry entry : FeedEntryRowFragment.items) {
						if (entry.id.equals(id)) {
							entry.unread = false;
						}
					}
				}
				ArrayAdapter<FeedEntry> adapter = ((FeedEntryRowFragment) getSupportFragmentManager().findFragmentById(R.id.item_list)).getAdapter();
				adapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, final int totalItemCount) {
		switch (view.getId()) {
		case android.R.id.list:
			if (totalItemCount > 0 && totalItemCount % 50 == 0) {
				// Sample calculation to determine if the last
				// item is fully visible.
				final int lastItem = firstVisibleItem + visibleItemCount;
				if (lastItem == totalItemCount) {
					// Last item is fully visible.
					Log.d(FeedEntryMainActivity.class.getName(), "Scrolled to the end of the list.");
					guiThread.post(new Runnable() {

						@Override
						public void run() {
							if (fetchMoreItemsPending == null || fetchMoreItemsPending.isDone()) {
								FeedEntryAdapter adapter = (FeedEntryAdapter) ((FeedEntryRowFragment) getSupportFragmentManager().findFragmentById(
										R.id.item_list)).getAdapter();
								Operation executor = operationFactory.createFetchMoreItemsOperation(FeedEntryMainActivity.this, totalItemCount);
								fetchMoreItemsPending = fetchThread.submit(new SelfossTask(executor, FeedEntryMainActivity.this,
										FeedEntryMainActivity.this, adapter));
							}
						}
					});
				}
			}
		}

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	@Override
	public void errorOccured(final String url, final Operation operation, Exception e) {
		guiThread.post(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(FeedEntryMainActivity.this,
						"Failed while executing operation: " + getString(operation.getOperationTitle()) + ", on " + url, Toast.LENGTH_LONG).show();
			}
		});
	}

	@Override
	public void onBackPressed() {
		Log.i(FeedEntryMainActivity.class.getName(), "Finishing");
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	public void starred(String id) {
		Log.i(FeedEntryMainActivity.class.getName(), "Item starred: " + id);
	}

	private class DrawerItemClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// Highlight the selected item, update the title, and close the drawer
			mDrawerList.setItemChecked(position, true);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FeedEntryMainActivity.this);
			Editor editor = prefs.edit();
			editor.putString(SettingsActivity.TYPE, apiOption[position]);
			editor.apply();

			setTitle(options[position]);
			mDrawerLayout.closeDrawer(mDrawerList);
			guiThread.post(updateTask);
		}

	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

}
