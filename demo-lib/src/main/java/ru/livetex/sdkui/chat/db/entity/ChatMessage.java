package ru.livetex.sdkui.chat.db.entity;

import java.util.Date;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.livetex.sdk.entity.Creator;
import ru.livetex.sdk.entity.KeyboardEntity;
import ru.livetex.sdk.entity.Visitor;

public final class ChatMessage implements Comparable<ChatMessage> {
	// Special case for displaying "employee typing" message
	public static final String ID_TYPING = "typing";

	@NonNull
	public String id;
	@NonNull
	public String content;
	@NonNull
	public Date createdAt; // timestamp in millis
	@NonNull
	public final Creator creator;
	@Nullable
	public final KeyboardEntity keyboard;

	@Nullable
	public final String fileUrl; // in case of "file" message
	public final boolean isIncoming;
	public MessageSentState sentState;

	// new local text\file message
	public ChatMessage(@NonNull String id, @NonNull String content, @NonNull Date createdAt) {
		this.id = id;
		this.content = content;
		this.createdAt = createdAt;
		this.isIncoming = false;
		this.sentState = MessageSentState.NOT_SENT;
		this.fileUrl = null;
		this.creator = new Visitor();
		this.keyboard = null;
	}

	// mapped from server text entity
	public ChatMessage(@NonNull String id,
					   @NonNull String content,
					   @NonNull Date createdAt,
					   boolean isIncoming,
					   @NonNull Creator creator,
					   @Nullable KeyboardEntity keyboard) {
		this(id, content, createdAt, isIncoming, null, creator, keyboard);
	}

	// mapped from server file entity
	public ChatMessage(@NonNull String id,
					   @NonNull String content,
					   @NonNull Date createdAt,
					   boolean isIncoming,
					   @Nullable String fileUrl,
					   @NonNull Creator creator,
					   @Nullable KeyboardEntity keyboard) {
		this.id = id;
		this.content = content;
		this.createdAt = createdAt;
		this.isIncoming = isIncoming;
		this.sentState = MessageSentState.SENT;
		this.creator = creator;
		this.keyboard = keyboard;
		// special handling for operator stickers
		if (fileUrl != null && fileUrl.startsWith("//")) {
			this.fileUrl = "https://" + fileUrl;
		} else {
			this.fileUrl = fileUrl;
		}
	}

	public void setSentState(MessageSentState sentState) {
		this.sentState = sentState;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ChatMessage)) {
			return false;
		}
		ChatMessage that = (ChatMessage) o;
		return isIncoming == that.isIncoming &&
				id.equals(that.id) &&
				content.equals(that.content) &&
				createdAt.equals(that.createdAt) &&
				sentState == that.sentState &&
				creator == that.creator &&
				keyboard == that.keyboard;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, content, createdAt, isIncoming, sentState, creator, keyboard);
	}

	@Override
	public int compareTo(ChatMessage o) {
		// "employee typing" message is always the last message
		if (this.id.equals(ID_TYPING)) {
			return 1;
		}
		if (o.id.equals(ID_TYPING)) {
			return -1;
		}
		return this.createdAt.compareTo(o.createdAt);
	}
}
