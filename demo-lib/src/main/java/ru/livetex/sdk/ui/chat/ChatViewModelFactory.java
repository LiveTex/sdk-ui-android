package ru.livetex.sdk.ui.chat;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public final class ChatViewModelFactory implements ViewModelProvider.Factory {
	private final SharedPreferences sp;

	public ChatViewModelFactory(SharedPreferences sp) {
		this.sp = sp;
	}

	@Override
	public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
		return (T) new ChatViewModel(sp);
	}
}
