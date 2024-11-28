package ru.livetex.sdkui.chat.adapter;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.DiffUtil;

public final class ChatMessageDiffUtil extends DiffUtil.Callback {

	private final List<AdapterItem> oldList = new ArrayList<>();
	private final List<AdapterItem> newList = new ArrayList<>();

	public ChatMessageDiffUtil(List<AdapterItem> oldList, List<AdapterItem> newList) {
		this.oldList.addAll(oldList);
		this.newList.addAll(newList);
	}

	@Override
	public int getOldListSize() {
		return oldList.size();
	}

	@Override
	public int getNewListSize() {
		return newList.size();
	}

	@Override
	public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
		AdapterItem oldProduct = oldList.get(oldItemPosition);
		AdapterItem newProduct = newList.get(newItemPosition);

		if (oldProduct.getAdapterItemType() == newProduct.getAdapterItemType()) {
			switch (oldProduct.getAdapterItemType()) {
				case CHAT_MESSAGE:
					return ((ChatItem) oldProduct).id.equals(((ChatItem) newProduct).id);
				case DATE:
					return ((DateItem) oldProduct).text.equals(((DateItem) newProduct).text);
				case EMPLOYEE_TYPING:
					// shouldn't happen
					return true;
				case RATING:
					// shouldn't happen
					return true;
			}
		} else {
			return false;
		}

		return oldProduct == newProduct;
	}

	@Override
	public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
		AdapterItem oldProduct = oldList.get(oldItemPosition);
		AdapterItem newProduct = newList.get(newItemPosition);
		// here should be checked only variables which affect UI
		return oldProduct.equals(newProduct);
	}
}
