package ru.livetex.sdk.ui.chat.adapter;

import java.util.Date;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.livetex.sdk.ui.chat.db.entity.ChatMessage;
import ru.livetex.sdk.ui.chat.db.entity.MessageSentState;
import ru.livetex.sdk.entity.Bot;
import ru.livetex.sdk.entity.Creator;
import ru.livetex.sdk.entity.KeyboardEntity;
import ru.livetex.sdk.entity.SystemUser;

/**
 * This is wrapper for ChatMessage entity. It allows to use only UI data and also made adapter item mutable (for DiffUtil).
 */
public class ChatItem implements Comparable, AdapterItem {
	@NonNull
	public String id;
	@NonNull
	public String content;
	@Nullable
	public String quoteText = null;
	@NonNull
	public final Date createdAt; // timestamp in millis
	public final boolean isIncoming;
	public final boolean isSystem;
	public final boolean isBot;
	public MessageSentState sentState;
	@Nullable
	public final String fileUrl; // in case of "file" message
	@NonNull
	public final Creator creator;
	@Nullable
	public final KeyboardEntity keyboard;

	public ChatItem(ChatMessage message) {
		this.id = message.id;
		this.content = message.content;
		this.createdAt = message.createdAt;
		this.isIncoming = message.isIncoming;
		this.sentState = message.sentState;
		this.fileUrl = message.fileUrl;
		this.creator = message.creator;
		this.isSystem = message.creator instanceof SystemUser;
		this.isBot = message.creator instanceof Bot;
		this.keyboard = message.keyboard;
		findQuotedText();
	}

	@Override
	public int compareTo(Object o) {
		if (o instanceof ChatItem) {
			return createdAt.compareTo(((ChatItem) o).createdAt);
		}
		return 0;
	}

	@NonNull
	public String getId() {
		return id;
	}

	public void setId(@NonNull String id) {
		this.id = id;
	}

	@NonNull
	public String getContent() {
		return content;
	}

	public void setContent(@NonNull String content) {
		this.content = content;
	}

	@NonNull
	public Date getCreatedAt() {
		return createdAt;
	}

	public boolean isIncoming() {
		return isIncoming;
	}

	@NonNull
	public Creator getCreator() {
		return creator;
	}

	@Override
	public ItemType getAdapterItemType() {
		return ItemType.CHAT_MESSAGE;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ChatItem)) {
			return false;
		}
		ChatItem chatItem = (ChatItem) o;
		return isIncoming == chatItem.isIncoming &&
				isSystem == chatItem.isSystem &&
				isBot == chatItem.isBot &&
				id.equals(chatItem.id) &&
				content.equals(chatItem.content) &&
				Objects.equals(quoteText, chatItem.quoteText) &&
				createdAt.equals(chatItem.createdAt) &&
				sentState == chatItem.sentState &&
				creator == chatItem.creator &&
				keyboard == chatItem.keyboard;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, content, quoteText, createdAt, isIncoming, isSystem, isBot, sentState, creator, keyboard);
	}

	// It's a temporary solution
	private void findQuotedText() {
		boolean hasQuote = content.startsWith("> ") && content.contains("\n");
		if (hasQuote) {
			int prefixLength = "> ".length();
			quoteText = content.substring(prefixLength, content.indexOf("\n"));
			content = content.substring(quoteText.length() + prefixLength + 1);
		}
	}
}
