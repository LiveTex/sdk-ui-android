package ru.livetex.sdkui;

public interface Const {
	String SHARED_PREFS_NAME = "livetex-demo";
	String KEY_VISITOR_TOKEN = "KEY_VISITOR_TOKEN";
	String KEY_CUSTOM_TOKEN = "KEY_CUSTOM_TOKEN";
	String KEY_CONNECT_TYPE = "KEY_CONNECT_TYPE";
	String KEY_CUSTOM_TOUCHPOINT = "KEY_CUSTOM_TOUCHPOINT";

	// Type of token which used in authorization (see AuthData sdk class)
	enum TokenType {
		VISITOR,
		CUSTOM,
		VISITOR_AND_CUSTOM
	}

	int PRELOAD_MESSAGES_COUNT = 20;
}