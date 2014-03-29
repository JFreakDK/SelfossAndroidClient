package org.vester.selfoss;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringEscapeUtils;
import org.vester.selfoss.listener.MarkAsUnreadOperationListener;
import org.vester.selfoss.listener.StarOperationListener;
import org.vester.selfoss.model.FeedEntry;
import org.vester.selfoss.operation.Operation;
import org.vester.selfoss.operation.SelfossOperationFactory;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * A fragment representing a single Item detail screen. This fragment is either contained in a {@link FeedEntryMainActivity} in two-pane mode (on tablets)
 * or a {@link FeedEntryContentActivity} on handsets.
 */
public class FeedEntryContentFragment extends Fragment implements MarkAsUnreadOperationListener, StarOperationListener {
	/**
	 * The fragment argument representing the item ID that this fragment represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";

	/**
	 * The FeedEntry content this fragment is presenting.
	 */
	private FeedEntry mItem;

	private ExecutorService markAsUnreadThreads;

	private Handler guiThread;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation changes).
	 */
	public FeedEntryContentFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments().containsKey(ARG_ITEM_ID)) {
			// Load the dummy content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
			String id = getArguments().getString(ARG_ITEM_ID);
			if (org.vester.selfoss.FeedEntryRowFragment.items != null) {
				for (FeedEntry feedEntry : org.vester.selfoss.FeedEntryRowFragment.items) {
					if (feedEntry.id.equals(id)) {
						mItem = feedEntry;
						break;
					}
				}
			}
			markAsUnreadThreads = Executors.newSingleThreadExecutor();
			guiThread = new Handler();
		}

		setHasOptionsMenu(true);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.feed_entry_content, container, false);

		// Show the content in a WebView.
		if (mItem != null) {
			String title = StringEscapeUtils.escapeHtml4(StringEscapeUtils.unescapeHtml4(mItem.title));
			String sourceTitle = StringEscapeUtils.escapeHtml4(StringEscapeUtils.unescapeHtml4(mItem.sourcetitle));
			String content;
			if (needsEscaping(mItem.content)) {
				content = StringEscapeUtils.escapeHtml4(mItem.content);
			} else {
				content = mItem.content;
			}
			((WebView) rootView.findViewById(R.id.item_detail)).loadData("<html><head><title>" + title
					+ "</title></head><body><a style=\"font-size: 120%;text-decoration: none;\" href=\"" + parseLink(mItem.link) + "\" >" + title
					+ "</a><br/><hr><span style=\"font-weight:bold\">" + sourceTitle + "</span><br/>" + content + "</body></html>", "text/html",
					"UTF-8");
		} else {
			// Switch to list activity
			getActivity().finish();
		}

		return rootView;
	}

	private boolean needsEscaping(String content) {
		return Html.fromHtml(content).equals(content);
	}

	private String parseLink(String link) {
		return link.replace("http://xkcd.com/", "http://m.xkcd.com/");
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.detail_options, menu);
		updateStarIcon(menu);

	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		updateStarIcon(menu);
	}

	private void updateStarIcon(Menu menu) {
		if (mItem != null) {
			menu.findItem(R.id.star).setVisible(mItem.isStared());
			menu.findItem(R.id.unstar).setVisible(!mItem.isStared());
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.keep_unread:
			SelfossTask task = new SelfossTask(SelfossOperationFactory.getInstance().createMarkAsUnreadOperation(mItem.id, this), getActivity(),
					new ErrorCallback() {

						@Override
						public void errorOccured(String url, Operation operation, Exception e) {

						}
					});
			markAsUnreadThreads.submit(task);
			break;
		case R.id.star:
			task = new SelfossTask(SelfossOperationFactory.getInstance().createUnstarOperation(mItem.id, this), getActivity(), new ErrorCallback() {

				@Override
				public void errorOccured(String url, Operation operation, Exception e) {

				}
			});
			markAsUnreadThreads.submit(task);
			break;
		case R.id.unstar:
			task = new SelfossTask(SelfossOperationFactory.getInstance().createStarOperation(mItem.id, this), getActivity(), new ErrorCallback() {

				@Override
				public void errorOccured(String url, Operation operation, Exception e) {

				}
			});
			markAsUnreadThreads.submit(task);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void markedAsUnread(final String id) {
		for (FeedEntry feedEntry : org.vester.selfoss.FeedEntryRowFragment.items) {
			if (feedEntry.id.equals(id)) {
				feedEntry.unread = true;
			}
		}
		FeedEntryRowFragment itemListFragment = (FeedEntryRowFragment) getFragmentManager().findFragmentById(R.id.item_list);
		itemListFragment.getAdapter().notifyDataSetChanged();

	}

	@Override
	public void starred(final String id) {
		Log.i(FeedEntryContentFragment.class.getName(), "Starred the feed entry: " + id);
		if (mItem.id.equals(id)) {
			mItem.setStarred(true);
			guiThread.post(new Runnable() {

				@Override
				public void run() {
					getActivity().invalidateOptionsMenu();
				}
			});
		}
	}

	@Override
	public void unstarred(String id) {

		Log.i(FeedEntryContentFragment.class.getName(), "Unstarred the feed entry: " + id);
		if (mItem.id.equals(id)) {
			mItem.setStarred(false);
			guiThread.post(new Runnable() {

				@Override
				public void run() {
					getActivity().invalidateOptionsMenu();
				}
			});
		}
	}
}
