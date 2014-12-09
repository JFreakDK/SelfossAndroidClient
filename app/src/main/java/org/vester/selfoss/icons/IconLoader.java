package org.vester.selfoss.icons;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class IconLoader {

	public static Bitmap getBitmap(String url, String filename, Context context) {
		File f = new File(context.getCacheDir(), filename);
		// Is the bitmap in our cache?
		Bitmap bitmap = BitmapFactory.decodeFile(f.getPath());
		return bitmap;
	}
}
