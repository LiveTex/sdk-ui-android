package ru.livetex.sdkui.utils.picker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class LivetexPicker {
	private final String TAG = "LivetexPicker";
	private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
	private final ActivityResultLauncher<String[]> pickFile;
	private final ActivityResultLauncher<String> reqPermissions;

	private final AppCompatActivity activity;

	public LivetexPicker(AppCompatActivity activity, LivetexPickerHandler handler) {
		this.activity = activity;
		pickMedia = activity.registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
			if (uri != null) {
				Log.d(TAG, "Selected media URI: $uri");
				handler.onFileSelected(uri);
			} else {
				Log.d(TAG, "No media selected");
				handler.onFileSelected(null);
			}
		});
		pickFile = activity.registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
			if (uri != null) {
				Log.d(TAG, "Selected file URI: $uri");
				handler.onFileSelected(uri);
			} else {
				Log.d(TAG, "No file selected");
				handler.onFileSelected(null);
			}
		});
		reqPermissions = activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
			if (isGranted) {
				Log.d(TAG, "Permission granted");
				selectPhoto();
			} else {
				Log.i(TAG, "Permission denied");
			}
		});
	}

	public void selectPhoto() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
			if (!hasPermission()) {
				requestPermission();
				return;
			}
		}
		pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
	}

	public void selectFile() {
		pickFile.launch(new String[] { "*/*" });
	}

	void requestPermission() {
		reqPermissions.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
	}

	@RequiresApi(Build.VERSION_CODES.M)
	private Boolean hasPermission() {
		return activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}
}
