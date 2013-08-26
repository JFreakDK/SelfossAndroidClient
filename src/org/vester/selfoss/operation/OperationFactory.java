package org.vester.selfoss.operation;

import java.util.Collection;

import org.vester.selfoss.ItemListActivity;
import org.vester.selfoss.SetupActivity.LoginCallback;
import org.vester.selfoss.listener.MarkAsUnreadOperationListener;
import org.vester.selfoss.listener.StarOperationListener;
import org.vester.selfoss.listener.UnstarOperationListener;
import org.vester.selfoss.model.FeedEntry;

import android.content.Context;
import android.os.Handler;
import android.widget.ImageView;

public interface OperationFactory {

	FetchItemsOperation createFetchItemsOperation(ItemListActivity itemListActivity);

	FetchMoreItemsOperation createFetchMoreItemsOperation(ItemListActivity itemListActivity, int totalItemCount);

	LoadImageOperation createLoadImageOperation(ImageView imgIcon, FeedEntry entry, Context context, Handler guiThread);

	MarkAllAsReadOperation createMarkAllAsReadOperation(Collection<String> ids, ItemListActivity itemListActivity);

	MarkAsReadOperation createMarkAsReadOperation(String id, ItemListActivity itemListActivity);

	LoginOperation createLoginOperation(String username, String password, LoginCallback loginCallback);

	Operation createMarkAsUnreadOperation(String id, MarkAsUnreadOperationListener feedEntryListener);

	Operation createStarOperation(String id, StarOperationListener listener);

	Operation createUnstarOperation(String id, UnstarOperationListener listener);

}
