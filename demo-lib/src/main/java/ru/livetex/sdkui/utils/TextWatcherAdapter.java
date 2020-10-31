package ru.livetex.sdkui.utils;

import android.text.Editable;
import android.text.TextWatcher;

public class TextWatcherAdapter implements TextWatcher {
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// pass
	}

	@Override
	public void afterTextChanged(Editable s) {
		// pass
	}
}