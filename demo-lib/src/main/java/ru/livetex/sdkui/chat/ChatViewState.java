package ru.livetex.sdkui.chat;

import java.util.Objects;

enum ChatViewState {
	// Normal UI with visible input
	NORMAL,
	// UI with visible disabled input and file preview
	SEND_FILE_PREVIEW,
	// UI with visible quote text
	QUOTE,
	// Only Attributes form is visible
	ATTRIBUTES,
	// Only Departments selection is visible
	DEPARTMENTS;
}

enum ChatInputState {
	// All input controls are visible and enabled
	NORMAL,
	DISABLED,
	HIDDEN
}

final class ChatViewStateData {
	ChatViewState state;
	ChatInputState inputState;

	ChatViewStateData(ChatViewState state, ChatInputState inputState) {
		this.state = state;
		this.inputState = inputState;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ChatViewStateData that = (ChatViewStateData) o;
		return state == that.state && inputState == that.inputState;
	}

	@Override
	public int hashCode() {
		return Objects.hash(state, inputState);
	}
}