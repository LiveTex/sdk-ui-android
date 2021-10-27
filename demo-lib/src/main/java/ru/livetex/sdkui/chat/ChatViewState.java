package ru.livetex.sdkui.chat;

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
}