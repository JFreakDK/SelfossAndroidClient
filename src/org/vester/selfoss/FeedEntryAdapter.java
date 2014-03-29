package org.vester.selfoss;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.vester.selfoss.FeedEntryRowFragment.FeedEntryHolder;
import org.vester.selfoss.icons.IconLoader;
import org.vester.selfoss.model.FeedEntry;
import org.vester.selfoss.operation.Operation;
import org.vester.selfoss.operation.SelfossOperationFactory;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

final class FeedEntryAdapter extends ArrayAdapter<FeedEntry> {

	/**
	 * 
	 */
	// private ItemListFragment itemListFragment;
	private final String url;
	private Set<String> loadingIconSet = Collections.synchronizedSet(new HashSet<String>());
	private boolean mIsLoading = false;
	private final Handler guiThread;
	private final ExecutorService iconLoadThreads;

	FeedEntryAdapter(FragmentActivity context, int resource, int textViewResourceId, List<FeedEntry> objects, String url, Handler guiThread, ExecutorService iconLoadThreads) {
		super(context, resource, textViewResourceId, objects);
		this.url = url;
		this.guiThread = guiThread;
		this.iconLoadThreads = iconLoadThreads;
	}

	public void setIsLoading(final boolean isLoading) {
		guiThread.post(new Runnable() {

			@Override
			public void run() {
				if (mIsLoading != isLoading) {
					mIsLoading = isLoading;
					notifyDataSetChanged();
				}

			}
		});
	}

	@Override
	public int getCount() {
		return super.getCount() + (mIsLoading ? 1 : 0);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (mIsLoading && position == (getCount() - 1)) {
			// return your progress view goes here. Ensure that it has
			// the ID R.id.progress;
			if (row == null || row.getTag() instanceof FeedEntryHolder) {
				LayoutInflater inflater = ((FragmentActivity) getContext()).getLayoutInflater();
				row = inflater.inflate(R.layout.list_view_progress_bar_row, parent, false);
				row.setTag(new LoadingFeedEntryHolder());
			}
		} else {
			// http://www.ezzylearning.com/tutorial.aspx?tid=1763429
			row = convertView;
			FeedEntryHolder holder = null;

			if (row == null || row.getTag() instanceof LoadingFeedEntryHolder) {
				LayoutInflater inflater = ((FragmentActivity) getContext()).getLayoutInflater();
				row = inflater.inflate(R.layout.feed_entry_row, parent, false);

				holder = new FeedEntryRowFragment.FeedEntryHolder();
				holder.imgIcon = (ImageView) row.findViewById(R.id.imgIcon);
				holder.txtTitle = (TextView) row.findViewById(R.id.txtTitle);
				holder.txtSource = (TextView) row.findViewById(R.id.txtSource);
				row.setTag(holder);
			} else if (row.getTag() instanceof FeedEntryHolder) {
				holder = (FeedEntryHolder) row.getTag();
			}

			final FeedEntry entry = FeedEntryRowFragment.items.get(position);
			if (!entry.unread) {
				holder.txtTitle.setTypeface(null);
				row.setBackgroundColor(Color.LTGRAY);
			} else {
				holder.txtTitle.setTypeface(Typeface.DEFAULT_BOLD);
				row.setBackgroundColor(Color.WHITE);
			}
			if (entry.icon.length() > 0) {
				if (entry.sourcetitle.trim().equals("Planet Android")) {
					Log.d(FeedEntryRowFragment.class.getName(), "Now loading for Planet Android: " + entry.icon);
				}
				Bitmap bitmap = IconLoader.getBitmap(url, entry.icon, getContext());
				if (bitmap != null) {
					holder.imgIcon.setImageBitmap(bitmap);
				} else {
					Log.d(FeedEntryRowFragment.class.getName(), entry.icon + " is not in the cache. (" + entry.sourcetitle + ")");
					holder.imgIcon.setImageBitmap(null);
					if (!loadingIconSet.contains(entry.icon)) {
						loadingIconSet.add(entry.icon);
						Operation operation = SelfossOperationFactory.getInstance().createLoadImageOperation(holder.imgIcon, entry, getContext(), guiThread);
						iconLoadThreads.submit(new SelfossTask(operation, getContext(), new ErrorCallback() {

							@Override
							public void errorOccured(String url, Operation operation, Exception e) {
								Log.e(FeedEntryAdapter.class.getName(), "Error occured trying to load icon for: " + entry + " on url: " + url, e);
							}

						}));
					} else {
						// Add listener that can react when operation has stored
						// icon.
					}
				}
			} else {
				holder.imgIcon.setImageBitmap(null);
			}
			holder.txtTitle.setText(Html.fromHtml(entry.title));
			holder.txtSource.setText(entry.sourcetitle);
		}
		return row;
	}

	private final class LoadingFeedEntryHolder {
	}

}