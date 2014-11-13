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

//	public static File getCacheDir(Context context) {
//		File cacheDir;
//		// Find the dir to save cached images
//		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
//			cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "vester.org.selfoss");
//		else
//			cacheDir = context.getCacheDir();
//		if (!cacheDir.exists())
//			cacheDir.mkdirs();
//		return cacheDir;
//	}
}
