package ru.livetex.sdkui.utils;

import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public abstract class RecyclerViewScrollListener extends RecyclerView.OnScrollListener {
	private boolean requireLoading = false;

	private final LinearLayoutManager layoutManager;

	public RecyclerViewScrollListener(LinearLayoutManager layoutManager) {
		this.layoutManager = layoutManager;
	}

	@Override
	public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
		super.onScrolled(view, dx, dy);

		int firstVisibleItemPosition = -1;

		if (layoutManager != null) {
			firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
		}

		if (!requireLoading && dy < 0 && firstVisibleItemPosition == 0 && canLoadMore()) {
			requireLoading = true;
		}

		if (dy > 0) {
			onScrollDown();
		} else if (dy < 0) {
			onScrollUp();
		}
	}

	protected void onScrollDown() {
	}

	protected void onScrollUp() {
	}

	@Override
	public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
		super.onScrollStateChanged(recyclerView, newState);
		if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && requireLoading) {
			requireLoading = false;
			onLoadRequest();
		}
	}

	public void onLoadRequest() {
	}

	public boolean canLoadMore() {
		return true;
	}

	protected LinearLayoutManager getLayoutManager() {
		return layoutManager;
	}
}