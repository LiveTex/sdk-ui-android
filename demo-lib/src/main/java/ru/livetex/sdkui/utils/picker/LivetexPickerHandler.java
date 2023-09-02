package ru.livetex.sdkui.utils.picker;

import android.net.Uri;

import androidx.annotation.Nullable;

public interface LivetexPickerHandler {
	void onFileSelected(@Nullable Uri uri);
}
