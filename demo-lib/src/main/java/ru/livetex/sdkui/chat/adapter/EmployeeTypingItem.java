package ru.livetex.sdkui.chat.adapter;

import ru.livetex.sdk.entity.Creator;

public final class EmployeeTypingItem implements Comparable<AdapterItem>, AdapterItem {
	public Creator creator;

	public EmployeeTypingItem(Creator creator) {
		this.creator = creator;
	}

	@Override
	public ItemType getAdapterItemType() {
		return ItemType.EMPLOYEE_TYPING;
	}

	@Override
	public int compareTo(AdapterItem o) {
		// always at the end of chat list
		return 1;
	}
}