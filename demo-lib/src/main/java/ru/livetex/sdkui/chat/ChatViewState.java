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
	DEPARTMENTS
}
