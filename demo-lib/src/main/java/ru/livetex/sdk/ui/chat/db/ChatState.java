package ru.livetex.sdk.ui.chat.db;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import ru.livetex.sdk.ui.chat.db.entity.ChatMessage;
import ru.livetex.sdk.entity.Employee;
import ru.livetex.sdk.entity.Visitor;

/**
 * In real projects for persistent storage it's recommended to use DB (like Room)
 */
public final class ChatState {

	public final static ChatState instance = new ChatState();

	private Map<String, ChatMessage> messages = new ConcurrentHashMap<>();
	private BehaviorSubject<List<ChatMessage>> messagesSubject = BehaviorSubject.createDefault(Collections.emptyList());
	// Indicates that chat possible has previous messages (GetHistoryRequest can be done on scroll chat to top).
	// Initially it can be always true and set to false when HistoryEntity (related to GetHistoryRequest) messages count < offset.
	public boolean canPreloadChatMessages = true;
	// For link preview
	public Map<String, Boolean> previewsMap = new HashMap<>();

	public Observable<List<ChatMessage>> messages() {
		return messagesSubject
				.map(Collections::unmodifiableList);
	}

	public synchronized void addMessages(List<ChatMessage> newMessages) {
		for (ChatMessage message : newMessages) {
			messages.put(message.id, message);
		}
		notifyObservers();
	}

	private void notifyObservers() {
		ArrayList<ChatMessage> result = new ArrayList<>(messages.values());
		// Must do sorting because
		// 1) "newMessages" can be previous messages, received from history loading
		// 2) messages isn't sorted map and messages.values() isn't sorted result too
		Collections.sort(result);
		messagesSubject.onNext(result);
	}

	public synchronized void addOrUpdateMessage(ChatMessage message) {
		messages.put(message.id, message);
		notifyObservers();
	}

	public synchronized void removeMessage(String id, boolean notify) {
		messages.remove(id);
		if (notify) {
			notifyObservers();
		}
	}

	@Nullable
	public ChatMessage getMessage(String id) {
		return messages.get(id);
	}

	/**
	 * Create local message with fake id (will be overriden by server)
	 * // todo: id scheme need improvement. now no way to distinguish between sent and local messages (fake and not fake id)
	 * @param quoteText - quote other message. It's a temporary solution, here should be id of quoted message.
	 */
	public synchronized ChatMessage createNewTextMessage(String text, String quoteText) {
		if (!TextUtils.isEmpty(quoteText)) {
			text = "> " + quoteText + "\n" + text;
		}
		ChatMessage chatMessage = new ChatMessage(
				UUID.randomUUID().toString(),
				text,
				new Date()
		);
		addOrUpdateMessage(chatMessage);
		return chatMessage;
	}

	/**
	 * Create local message with fake id (will be overriden by server)
	 * // todo: id scheme need improvement. now no way to distinguish between sent and local messages (fake and not fake id)
	 */
	public synchronized ChatMessage createNewFileMessage(String filePath) {
		ChatMessage chatMessage = new ChatMessage(
				UUID.randomUUID().toString(),
				"",
				new Date(),
				false,
				filePath,
				new Visitor(),
				null
		);
		addOrUpdateMessage(chatMessage);
		return chatMessage;
	}

	/**
	 * Create local typing message
	 * // todo: id scheme need improvement. now no way to distinguish between sent and local messages (fake and not fake id)
	 */
	public synchronized ChatMessage createTypingMessage(Employee employee) {
		ChatMessage chatMessage = new ChatMessage(
				"typing",
				"",
				new Date(),
				true,
				null,
				employee,
				null
		);
		addOrUpdateMessage(chatMessage);
		return chatMessage;
	}
}
