package ru.livetex.sdkui.chat.adapter;

import java.util.Objects;

import androidx.annotation.NonNull;

public class DateItem implements Comparable<AdapterItem>, AdapterItem {
	@NonNull
	public String text;

	public DateItem(@NonNull String text) {
		this.text = text;
	}

	@Override
	public int compareTo(AdapterItem o) {
		if (o instanceof DateItem) {
			return text.compareTo(((DateItem) o).text);
		}
		return 0;
	}

	@Override
	public ItemType getAdapterItemType() {
		return ItemType.DATE;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DateItem)) {
			return false;
		}
		DateItem chatItem = (DateItem) o;
		return text.equals(chatItem.text);
	}

	@Override
	public int hashCode() {
		return Objects.hash(text);
	}
}
