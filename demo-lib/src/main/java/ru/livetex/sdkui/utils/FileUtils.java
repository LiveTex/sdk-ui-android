package ru.livetex.sdkui.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;

import androidx.annotation.WorkerThread;

public final class FileUtils {

	@WorkerThread
	public static String getPath(Context context, Uri uri) {
		String returnedPath;

		returnedPath = getRealPathFromUri(context, uri);

		// Get the file extension
		final MimeTypeMap mime = MimeTypeMap.getSingleton();

		if (uri.getScheme() != null && uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
			return copyFile(uri, context);
		}
		return returnedPath;
	}

	public static String getMimeType(Context context, Uri uri) {
		String returnedPath;

		returnedPath = getRealPathFromUri(context, uri);

		final MimeTypeMap mime = MimeTypeMap.getSingleton();
		String subStringExtension = String.valueOf(returnedPath).substring(String.valueOf(returnedPath).lastIndexOf(".") + 1);
		String extensionFromMime = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));
		if (extensionFromMime == null) {
			extensionFromMime = mime.getMimeTypeFromExtension(subStringExtension);
		}
		return extensionFromMime;
	}

	public static String getFilename(Context context, Uri uri) {
		String returnedPath = getRealPathFromUri(context, uri);
		return new File(returnedPath).getName();
	}

	public static String getRealPathFromUri(final Context context, final Uri uri) {
		if (DocumentsContract.isDocumentUri(context, uri)) {
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				if ("primary".equalsIgnoreCase(type)) {
					if (split.length > 1) {
						return Environment.getExternalStorageDirectory() + "/" + split[1];
					} else {
						return Environment.getExternalStorageDirectory() + "/";
					}
				} else {
					return "storage" + "/" + docId.replace(":", "/");
				}

			} else if (isRawDownloadsDocument(uri)) {
				String fileName = getFilePath(context, uri);
				String subFolderName = getSubFolders(uri);

				if (fileName != null) {
					return Environment.getExternalStorageDirectory().toString() + "/Download/" + subFolderName + fileName;
				}
				String id = DocumentsContract.getDocumentId(uri);

				final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
				return getDataColumn(context, contentUri, null, null);
			} else if (isDownloadsDocument(uri)) {
				String fileName = getFilePath(context, uri);

				if (fileName != null) {
					return Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
				}
				String id = DocumentsContract.getDocumentId(uri);
				if (id.startsWith("raw:")) {
					id = id.replaceFirst("raw:", "");
					File file = new File(id);
					if (file.exists()) {
						return id;
					}
				}
				if (id.startsWith("raw%3A%2F")) {
					id = id.replaceFirst("raw%3A%2F", "");
					File file = new File(id);
					if (file.exists()) {
						return id;
					}
				}
				final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
				return getDataColumn(context, contentUri, null, null);
			} else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[] {
						split[1]
				};

				return getDataColumn(context, contentUri, selection, selectionArgs);
			}
		} else if ("content".equalsIgnoreCase(uri.getScheme())) {
			if (isGooglePhotosUri(uri)) {
				return uri.getLastPathSegment();
			}
			if (getDataColumn(context, uri, null, null) == null) {
				// some error
			}
			return getDataColumn(context, uri, null, null);
		} else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	private static String copyFile(Uri uri, Context context) {
		return new CopyFileTask(uri).run(context);
	}

	private static String getSubFolders(Uri uri) {
		String replaceChars = String.valueOf(uri).replace("%2F", "/").replace("%20", " ").replace("%3A", ":");
		String[] bits = replaceChars.split("/");
		String sub5 = bits[bits.length - 2];
		String sub4 = bits[bits.length - 3];
		String sub3 = bits[bits.length - 4];
		String sub2 = bits[bits.length - 5];
		String sub1 = bits[bits.length - 6];
		if (sub1.equals("Download")) {
			return sub2 + "/" + sub3 + "/" + sub4 + "/" + sub5 + "/";
		} else if (sub2.equals("Download")) {
			return sub3 + "/" + sub4 + "/" + sub5 + "/";
		} else if (sub3.equals("Download")) {
			return sub4 + "/" + sub5 + "/";
		} else if (sub4.equals("Download")) {
			return sub5 + "/";
		} else {
			return "";
		}
	}

	private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = { column };
		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(index);
			}
		} catch (Exception e) {
			Log.e("FileUtils", "", e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return null;
	}

	private static String getFilePath(Context context, Uri uri) {
		Cursor cursor = null;
		final String[] projection = { MediaStore.Files.FileColumns.DISPLAY_NAME };
		try {
			cursor = context.getContentResolver().query(uri, projection, null, null,
					null);
			if (cursor != null && cursor.moveToFirst()) {
				final int index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
				return cursor.getString(index);
			}
		} catch (Exception e) {
			Log.e("FileUtils", "", e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return null;
	}

	private static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	private static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	private static boolean isRawDownloadsDocument(Uri uri) {
		String uriToString = String.valueOf(uri);
		return uriToString.contains("com.android.providers.downloads.documents/document/raw");
	}

	private static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	private static boolean isGooglePhotosUri(Uri uri) {
		return "com.google.android.apps.photos.content".equals(uri.getAuthority());
	}
}