package ru.livetex.sdkui.chat.adapter;

import ru.livetex.sdkui.chat.db.entity.ChatMessage;

public class EmployeeTypingItem extends ChatItem {

	public EmployeeTypingItem(ChatMessage message) {
		super(message);
	}

	@Override
	public ItemType getAdapterItemType() {
		return ItemType.EMPLOYEE_TYPING;
	}
}