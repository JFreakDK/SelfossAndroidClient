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

public interface OperationFactory {

	FetchItemsOperation createFetchItemsOperation(FeedEntryMainActivity itemListActivity);

	FetchMoreItemsOperation createFetchMoreItemsOperation(FeedEntryMainActivity itemListActivity, int totalItemCount);

	LoadImageOperation createLoadImageOperation(ImageView imgIcon, FeedEntry entry, Context context, Handler guiThread);

	MarkAllAsReadOperation createMarkAllAsReadOperation(Collection<String> ids, FeedEntryMainActivity itemListActivity);

	MarkAsReadOperation createMarkAsReadOperation(String id, FeedEntryMainActivity itemListActivity);

	LoginOperation createLoginOperation(String username, String password, LoginCallback loginCallback);

	Operation createMarkAsUnreadOperation(String id, MarkAsUnreadOperationListener feedEntryListener);

	Operation createStarOperation(String id, StarOperationListener listener);

	Operation createUnstarOperation(String id, StarOperationListener listener);

}
