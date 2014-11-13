package org.vester.selfoss.operation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.vester.selfoss.icons.IconLoader;
import org.vester.selfoss.model.FeedEntry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.widget.ImageView;

public class LoadImageOperation implements Operation {
	private final ImageView imgIcon;
	private final FeedEntry entry;
	private String url;
	private Context context;
	private Handler guiThread;

	protected LoadImageOperation(ImageView imgIcon, FeedEntry entry, Context context, Handler guiThread) {
		this.imgIcon = imgIcon;
		this.entry = entry;
		this.context = context;
		this.guiThread = guiThread;
	}

	@Override
	public void writeOutput(HttpURLConnection con) throws IOException {
	}

	@Override
	public void setURL(String url) {
		this.url = url;
	}

	@Override
	public void processResponse(InputStream in) throws JSONException {
		final Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(in), 32, 32, true);
		
		File f = new File(context.getCacheDir(), entry.icon);

		writeFile(bitmap, f);
		guiThread.post(new Runnable() {

			@Override
			public void run() {
				imgIcon.setImageBitmap(bitmap);
			}
		});
	}

	private void writeFile(Bitmap bitmap, File f) {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(f);
			bitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}

	@Override
	public String getRequestMethod() {
		return "GET";
	}

	@Override
	public URL createURL() throws MalformedURLException {
		return new URL(url + "/favicons/" + entry.icon);
	}

	@Override
	public void setLogin(String login) {
	}

	@Override
	public int getOperationTitle() {
		return -1;
	}
}
