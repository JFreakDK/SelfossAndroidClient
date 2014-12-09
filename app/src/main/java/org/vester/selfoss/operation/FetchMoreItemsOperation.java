package org.vester.selfoss.operation;

import java.net.MalformedURLException;
import java.net.URL;

import org.vester.selfoss.FeedEntryMainActivity;
import org.vester.selfoss.R;

public class FetchMoreItemsOperation extends FetchItemsOperation {
	private final int totalItemCount;

	protected FetchMoreItemsOperation(FeedEntryMainActivity itemListActivity, int totalItemCount) {
		super(itemListActivity);
		this.totalItemCount = totalItemCount;
	}

	@Override
	public void setURL(String url) {
		super.setURL(url);
	}

	@Override
	public URL createURL() throws MalformedURLException {
		return new URL(super.createURL().toExternalForm() + "&offset=" + totalItemCount);
	}

	@Override
	boolean isAppendEntries() {
		return true;
	}

	@Override
	public int getOperationTitle() {
		return R.string.load_more_items;
	}
}
