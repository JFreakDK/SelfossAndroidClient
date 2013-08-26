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
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.Toast;

/**
 * An activity representing a list of Items. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link ItemDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ItemListFragment} and the item details (if present) is a
 * {@link ItemDetailFragment}.
 * <p>
 * This activity also implements the required {@link ItemListFragment.Callbacks}
 * interface to listen for item selections.
 */
public class ItemListActivity extends FragmentActivity implements ItemListFragment.Callbacks, OnScrollListener, ErrorCallback {

	private static final int UNREAD = R.string.unread;
	private static final int ALL_ITEMS = R.string.all_items;
	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		operationFactory = SelfossOperationFactory.getInstance();
		setContentView(R.layout.activity_item_list);
		initThreading();
		ItemListFragment itemListFragment = (ItemListFragment) getSupportFragmentManager().findFragmentById(R.id.item_list);
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
		}
		if (ItemListFragment.items.isEmpty())
			guiThread.post(updateTask);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		MenuItem typeMenuItem = menu.findItem(R.id.type);
		updateTypeMenuItem(typeMenuItem);
		return true;
	}

	private void updateTypeMenuItem(MenuItem typeMenuItem) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String type = prefs.getString(SettingsActivity.TYPE, SettingsActivity.TYPE_DEFAULT);

		if (type.equals(SettingsActivity.TYPE_DEFAULT)) {
			typeMenuItem.setTitle(getString(ALL_ITEMS));
		} else if (type.equals(SettingsActivity.TYPE_UNREAD)) {
			typeMenuItem.setTitle(getString(UNREAD));
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		case R.id.mark_all_as_read:
			if (markAllAsReadPending == null || markAllAsReadPending.isDone()) {
				markAllAsReadPending = markAsReadThread.submit(new SelfossTask(operationFactory.createMarkAllAsReadOperation(filter(ItemListFragment.items), this), this, this));
				guiThread.post(updateTask);
			}
			break;
		case R.id.type:
			updatePreference(item);
			updateTypeMenuItem(item);
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

	private void updatePreference(MenuItem item) {
		String type = SettingsActivity.TYPE_DEFAULT;
		if (item.getTitle().equals(getString(ALL_ITEMS))) {
			type = SettingsActivity.TYPE_UNREAD;
		} else {
			Log.i(ItemListActivity.class.getName(), "Unhandled title detected");
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = prefs.edit();
		editor.putString(SettingsActivity.TYPE, type);
		editor.apply();
	}

	/**
	 * Callback method from {@link ItemListFragment.Callbacks} indicating that
	 * the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(final String id) {
		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putString(ItemDetailFragment.ARG_ITEM_ID, id);
			ItemDetailFragment fragment = new ItemDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction().replace(R.id.item_detail_container, fragment).commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID.
			Intent detailIntent = new Intent(this, ItemDetailActivity.class);
			detailIntent.putExtra(ItemDetailFragment.ARG_ITEM_ID, id);
			startActivity(detailIntent);
		}
		guiThread.post(new Runnable() {

			@Override
			public void run() {
				try {
					SelfossTask task = new SelfossTask(operationFactory.createMarkAsReadOperation(id, ItemListActivity.this), ItemListActivity.this, ItemListActivity.this);
					fetchThread.submit(task);
				} catch (RejectedExecutionException e) {
					Log.e(ItemListActivity.class.getName(), "RejectedExecutionException occured");
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
						ItemListFragment itemListFragment = (ItemListFragment) getSupportFragmentManager().findFragmentById(R.id.item_list);
						FeedEntryAdapter adapter = (FeedEntryAdapter) itemListFragment.getAdapter();
						SelfossTask task = new SelfossTask(operationFactory.createFetchItemsOperation(ItemListActivity.this), ItemListActivity.this, ItemListActivity.this, adapter);
						updatePending = fetchThread.submit(task);
					}
				} catch (RejectedExecutionException e) {
					Log.e(ItemListActivity.class.getName(), "RejectedExecutionException occured");
				}

			}
		};
	}

	public void onEntriesFetched(final JSONArray json, final boolean append) {
		guiThread.post(new Runnable() {

			@Override
			public void run() {
				if (!append) {
					ItemListFragment.items.clear();
				}
				for (int i = 0; i < json.length(); i++) {
					try {
						JSONObject jsonObject = (JSONObject) json.get(i);
						ItemListFragment.items.add(new FeedEntry(jsonObject));
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
					for (FeedEntry entry : ItemListFragment.items) {
						if (entry.id.equals(id)) {
							entry.unread = false;
						}
					}
				}
				ArrayAdapter<FeedEntry> adapter = ((ItemListFragment) getSupportFragmentManager().findFragmentById(R.id.item_list)).getAdapter();
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
					Log.d(ItemListActivity.class.getName(), "Scrolled to the end of the list.");
					guiThread.post(new Runnable() {

						@Override
						public void run() {
							if (fetchMoreItemsPending == null || fetchMoreItemsPending.isDone()) {
								FeedEntryAdapter adapter = (FeedEntryAdapter) ((ItemListFragment) getSupportFragmentManager().findFragmentById(R.id.item_list)).getAdapter();
								Operation executor = operationFactory.createFetchMoreItemsOperation(ItemListActivity.this, totalItemCount);
								fetchMoreItemsPending = fetchThread.submit(new SelfossTask(executor, ItemListActivity.this, ItemListActivity.this, adapter));
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
				Toast.makeText(ItemListActivity.this, "Failed while executing operation: " + getString(operation.getOperationTitle()) + ", on " + url, Toast.LENGTH_LONG).show();
			}
		});
	}

	@Override
	public void onBackPressed() {
		Log.i(ItemListActivity.class.getName(), "Finishing");
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	public void starred(String id) {
		Log.i(ItemListActivity.class.getName(), "Item starred: " + id);
	}
}
