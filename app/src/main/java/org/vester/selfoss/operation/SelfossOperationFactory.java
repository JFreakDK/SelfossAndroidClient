package org.vester.selfoss.operation;

import java.util.Collection;

import org.vester.selfoss.FeedEntryMainActivity;
import org.vester.selfoss.SetupActivity.LoginCallback;
import org.vester.selfoss.listener.MarkAsUnreadOperationListener;
import org.vester.selfoss.listener.StarOperationListener;
import org.vester.selfoss.model.FeedEntry;

import android.content.Context;
import android.os.Handler;
import android.widget.ImageView;

public class SelfossOperationFactory implements OperationFactory {

	private static SelfossOperationFactory operationFactory;

	private SelfossOperationFactory() {
	}

	@Override
	public MarkAsReadOperation createMarkAsReadOperation(String id, FeedEntryMainActivity itemListActivity) {
		return new MarkAsReadOperation(id, itemListActivity);
	}

	@Override
	public MarkAllAsReadOperation createMarkAllAsReadOperation(Collection<String> ids, FeedEntryMainActivity itemListActivity) {
		return new MarkAllAsReadOperation(ids, itemListActivity);
	}

	@Override
	public LoadImageOperation createLoadImageOperation(ImageView imgIcon, FeedEntry entry, Context context, Handler guiThread) {
		return new LoadImageOperation(imgIcon, entry, context, guiThread);
	}

	@Override
	public FetchMoreItemsOperation createFetchMoreItemsOperation(FeedEntryMainActivity itemListActivity, int totalItemCount) {
		return new FetchMoreItemsOperation(itemListActivity, totalItemCount);
	}

	@Override
	public FetchItemsOperation createFetchItemsOperation(FeedEntryMainActivity itemListActivity) {
		return new FetchItemsOperation(itemListActivity);
	}

	@Override
	public LoginOperation createLoginOperation(String username, String password, LoginCallback loginCallback) {
		return new LoginOperation(username, password, loginCallback);
	}

	public static SelfossOperationFactory getInstance() {
		if (operationFactory == null)
			operationFactory = new SelfossOperationFactory();
		return operationFactory;
	}

	@Override
	public Operation createMarkAsUnreadOperation(String id, MarkAsUnreadOperationListener markAsUnreadOperationListener) {
		return new MarkAsUnreadOperation(id, markAsUnreadOperationListener);
	}

	@Override
	public Operation createStarOperation(String id, StarOperationListener listener) {
		return new StarOperation(id, listener);
	}

	@Override
	public Operation createUnstarOperation(String id, StarOperationListener listener) {
		return new UnstarOperation(id, listener);
	}
}
