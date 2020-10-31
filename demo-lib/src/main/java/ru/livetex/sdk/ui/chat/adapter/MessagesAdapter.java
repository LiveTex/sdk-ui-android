package ru.livetex.sdk.ui.chat.adapter;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.functions.Consumer;
import ru.livetex.sdk.R;
import ru.livetex.sdk.ui.chat.db.ChatState;
import ru.livetex.sdk.ui.utils.DateUtils;
import ru.livetex.sdk.entity.Employee;
import ru.livetex.sdk.entity.KeyboardEntity;

public final class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	private static final String TAG = "MessagesAdapter";
	private static final int VIEW_TYPE_MESSAGE_INCOMING = 1;
	private static final int VIEW_TYPE_MESSAGE_OUTGOING = 2;
	private static final int VIEW_TYPE_IMAGE_INCOMING = 3;
	private static final int VIEW_TYPE_IMAGE_OUTGOING = 4;
	private static final int VIEW_TYPE_FILE_INCOMING = 5;
	private static final int VIEW_TYPE_FILE_OUTGOING = 6;
	private static final int VIEW_TYPE_SYSTEM_MESSAGE = 7;
	private static final int VIEW_TYPE_DATE = 8;
	private static final int VIEW_TYPE_EMPLOYEE_TYPING = 9;

	private List<AdapterItem> items = new ArrayList<>();
	private int messagesCount = 0;
	@Nullable
	private Consumer<ChatItem> onMessageClickListener = null;
	@Nullable
	private Consumer<String> onFileClickListener = null;
	@NonNull
	private final Consumer<KeyboardEntity.Button> onActionButtonClickListener;

	public MessagesAdapter(Consumer<KeyboardEntity.Button> listener) {
		this.onActionButtonClickListener = listener;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view;

		switch (viewType) {
			case VIEW_TYPE_MESSAGE_INCOMING:
				view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.i_chat_message_in, parent, false);
				return new IncomingMessageHolder(view);
			case VIEW_TYPE_MESSAGE_OUTGOING:
				view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.i_chat_message_out, parent, false);
				return new OutgoingMessageHolder(view);
			case VIEW_TYPE_IMAGE_INCOMING:
				view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.i_chat_message_image_in, parent, false);
				return new IncomingImageHolder(view);
			case VIEW_TYPE_IMAGE_OUTGOING:
				view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.i_chat_message_image_out, parent, false);
				return new OutgoingImageHolder(view);
			case VIEW_TYPE_FILE_INCOMING:
				view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.i_chat_message_in, parent, false);
				return new IncomingFileHolder(view);
			case VIEW_TYPE_FILE_OUTGOING:
				view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.i_chat_message_out, parent, false);
				return new OutgoingFileHolder(view);
			case VIEW_TYPE_SYSTEM_MESSAGE:
				view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.i_chat_message_system, parent, false);
				return new SystemMessageHolder(view);
			case VIEW_TYPE_DATE:
				view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.i_chat_message_date, parent, false);
				return new DateHolder(view);
			case VIEW_TYPE_EMPLOYEE_TYPING:
				view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.i_chat_message_in, parent, false);
				return new IncomingTypingMessageHolder(view);
		}
		return null;
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		final AdapterItem message = items.get(position);

		switch (holder.getItemViewType()) {
			case VIEW_TYPE_MESSAGE_INCOMING:
				((IncomingMessageHolder) holder).bind((ChatItem) message, onActionButtonClickListener);
				break;
			case VIEW_TYPE_MESSAGE_OUTGOING:
				((OutgoingMessageHolder) holder).bind((ChatItem) message);
				break;
			case VIEW_TYPE_IMAGE_INCOMING:
				((IncomingImageHolder) holder).bind((ChatItem) message, onFileClickListener);
				break;
			case VIEW_TYPE_IMAGE_OUTGOING:
				((OutgoingImageHolder) holder).bind((ChatItem) message, onFileClickListener);
				break;
			case VIEW_TYPE_FILE_INCOMING:
				((IncomingFileHolder) holder).bind((ChatItem) message, onFileClickListener);
				break;
			case VIEW_TYPE_FILE_OUTGOING:
				((OutgoingFileHolder) holder).bind((ChatItem) message, onFileClickListener);
				break;
			case VIEW_TYPE_SYSTEM_MESSAGE:
				((SystemMessageHolder) holder).bind((ChatItem) message);
				break;
			case VIEW_TYPE_DATE:
				((DateHolder) holder).bind((DateItem) message);
				break;
			case VIEW_TYPE_EMPLOYEE_TYPING:
				((IncomingTypingMessageHolder) holder).bind((EmployeeTypingItem) message);
				break;
		}

		if (onMessageClickListener != null && message.getAdapterItemType() == ItemType.CHAT_MESSAGE) {
			holder.itemView.setOnClickListener(view -> {
				try {
					onMessageClickListener.accept((ChatItem) message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
	}

	@Override
	public int getItemViewType(int position) {
		AdapterItem item = items.get(position);

		switch (item.getAdapterItemType()) {
			case CHAT_MESSAGE:
				ChatItem message = (ChatItem) item;

				if (!TextUtils.isEmpty(message.fileUrl)) {
					// todo: will be something better in future
					boolean isImgFile = message.fileUrl.contains("jpg") ||
							message.fileUrl.contains("jpeg") ||
							message.fileUrl.contains("png") ||
							message.fileUrl.contains("bmp");

					if (isImgFile) {
						if (message.isIncoming) {
							return VIEW_TYPE_IMAGE_INCOMING;
						} else {
							return VIEW_TYPE_IMAGE_OUTGOING;
						}
					} else {
						if (message.isIncoming) {
							return VIEW_TYPE_FILE_INCOMING;
						} else {
							return VIEW_TYPE_FILE_OUTGOING;
						}
					}
				} else {
					if (message.isSystem) {
						return VIEW_TYPE_SYSTEM_MESSAGE;
					}

					if (message.isIncoming) {
						return VIEW_TYPE_MESSAGE_INCOMING;
					} else {
						return VIEW_TYPE_MESSAGE_OUTGOING;
					}
				}
			case DATE:
				return VIEW_TYPE_DATE;
			case EMPLOYEE_TYPING:
				return VIEW_TYPE_EMPLOYEE_TYPING;
		}

		return -1;
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public int getMessagesCount() {
		return messagesCount;
	}

	public List<AdapterItem> getData() {
		return items;
	}

	public void setData(List<AdapterItem> chatMessages) {
		this.items.clear();
		this.items.addAll(chatMessages);

		messagesCount = 0;
		for (AdapterItem adapterItem : chatMessages) {
			if (adapterItem.getAdapterItemType() == ItemType.CHAT_MESSAGE) {
				messagesCount++;
			}
		}
	}

	public void setOnMessageClickListener(@NonNull Consumer<ChatItem> onMessageClickListener) {
		this.onMessageClickListener = onMessageClickListener;
	}

	public void setOnFileClickListener(@NonNull Consumer<String> onFileClickListener) {
		this.onFileClickListener = onFileClickListener;
	}

	private static class IncomingMessageHolder extends RecyclerView.ViewHolder {
		TextView messageView;
		ImageView avatarView;
		TextView nameView;
		TextView timeView;
		TextView quoteView;
		View quoteSeparatorView;
		ViewGroup buttonsContainerView;

		IncomingMessageHolder(View itemView) {
			super(itemView);

			nameView = itemView.findViewById(R.id.nameView);
			messageView = itemView.findViewById(R.id.messageView);
			avatarView = itemView.findViewById(R.id.avatarView);
			timeView = itemView.findViewById(R.id.timeView);
			quoteView = itemView.findViewById(R.id.quoteView);
			quoteSeparatorView = itemView.findViewById(R.id.quoteSeparatorView);
			buttonsContainerView = itemView.findViewById(R.id.buttonsContainerView);
		}

		void bind(ChatItem message, Consumer<KeyboardEntity.Button> onActionButtonClickListener) {
			Spannable text = ru.livetex.sdk.ui.utils.TextUtils.setTextWithLinks(message.content, messageView);
			handleQuotedText(message, quoteView, quoteSeparatorView);
			handleLinkPreview(messageView, text, message.id);

			loadAvatar(avatarView, message);
			setOperatorName(nameView, message);
			setState(timeView, message);

			handleKeyboardButtons(buttonsContainerView, message, onActionButtonClickListener);
		}

		private void handleKeyboardButtons(ViewGroup containerView, ChatItem message, Consumer<KeyboardEntity.Button> onActionButtonClickListener) {
			containerView.removeAllViews();
			containerView.setVisibility(message.keyboard != null ? View.VISIBLE : View.GONE);

			if (message.keyboard != null) {
				for (KeyboardEntity.Button button : message.keyboard.buttons) {
					MaterialButton view = (MaterialButton) View.inflate(containerView.getContext(), R.layout.l_message_keyboard_button, null);
					view.setText(button.label);
					view.setEnabled(!message.keyboard.pressed);
					view.setOnClickListener(v -> {
						try {
							onActionButtonClickListener.accept(button);
							for (int i = 0; i < containerView.getChildCount(); i++) {
								View v2 = containerView.getChildAt(i);
								v2.setEnabled(false);
							}
						} catch (Exception e) {
							Log.e(TAG, "onActionButtonClickListener", e);
						}
					});

					containerView.addView(view);
				}
			}
		}
	}

	private static class IncomingTypingMessageHolder extends RecyclerView.ViewHolder {
		TextView messageView;
		ImageView avatarView;
		TextView nameView;
		TextView timeView;
		Handler handler;

		IncomingTypingMessageHolder(View itemView) {
			super(itemView);

			nameView = itemView.findViewById(R.id.nameView);
			messageView = itemView.findViewById(R.id.messageView);
			avatarView = itemView.findViewById(R.id.avatarView);
			timeView = itemView.findViewById(R.id.timeView);
		}

		void bind(EmployeeTypingItem message) {
			handler = new Handler(Looper.getMainLooper());

			loadAvatar(avatarView, message);
			setOperatorName(nameView, message);

			timeView.setVisibility(View.GONE);

			typingAnimation(messageView, "Печатает.....", "Печатает".length(), "Печатает".length());
		}

		private void typingAnimation(TextView view, String text, int initialLength, int length) {
			if (handler == null) {
				return;
			}
			long delay = 250L;
			view.setText(text.substring(0, length));
			if (length < text.length()) {
				handler.postDelayed(() -> typingAnimation(view, text, initialLength, length + 1), delay);
			} else {
				if (view.isAttachedToWindow()) {
					handler.postDelayed(() -> typingAnimation(view, text, initialLength, initialLength), delay);
				} else {
					handler = null;
				}
			}
		}
	}

	private static class OutgoingMessageHolder extends RecyclerView.ViewHolder {
		TextView messageView;
		TextView timeView;
		TextView quoteView;
		View quoteSeparatorView;

		OutgoingMessageHolder(View itemView) {
			super(itemView);

			messageView = itemView.findViewById(R.id.messageView);
			timeView = itemView.findViewById(R.id.timeView);
			quoteView = itemView.findViewById(R.id.quoteView);
			quoteSeparatorView = itemView.findViewById(R.id.quoteSeparatorView);
		}

		void bind(ChatItem message) {
			Spannable text = ru.livetex.sdk.ui.utils.TextUtils.setTextWithLinks(message.content, messageView);
			handleQuotedText(message, quoteView, quoteSeparatorView);
			handleLinkPreview(messageView, text, message.id);

			setState(timeView, message);
		}
	}

	private static class IncomingFileHolder extends RecyclerView.ViewHolder {
		TextView messageView;
		ImageView avatarView;
		TextView nameView;
		TextView timeView;

		IncomingFileHolder(View itemView) {
			super(itemView);

			nameView = itemView.findViewById(R.id.nameView);
			messageView = itemView.findViewById(R.id.messageView);
			avatarView = itemView.findViewById(R.id.avatarView);
			timeView = itemView.findViewById(R.id.timeView);
		}

		void bind(ChatItem message, Consumer<String> onFileClickListener) {
			messageView.setText(message.content);

			loadAvatar(avatarView, message);
			setOperatorName(nameView, message);
			setState(timeView, message);

			messageView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.doc, 0, 0, 0);
			messageView.setCompoundDrawablePadding(messageView.getResources().getDimensionPixelOffset(R.dimen.chat_message_file_icon_padding));

			if (onFileClickListener != null) {
				messageView.setOnClickListener(view -> {
					try {
						onFileClickListener.accept(message.fileUrl);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		}
	}

	private static class OutgoingFileHolder extends RecyclerView.ViewHolder {
		TextView messageView;
		TextView timeView;

		OutgoingFileHolder(View itemView) {
			super(itemView);

			messageView = itemView.findViewById(R.id.messageView);
			timeView = itemView.findViewById(R.id.timeView);
		}

		void bind(ChatItem message, Consumer<String> onFileClickListener) {
			messageView.setText(message.content);

			setState(timeView, message);

			messageView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.doc, 0, 0, 0);
			messageView.setCompoundDrawablePadding(messageView.getResources().getDimensionPixelOffset(R.dimen.chat_message_file_icon_padding));

			if (onFileClickListener != null) {
				messageView.setOnClickListener(view -> {
					try {
						onFileClickListener.accept(message.fileUrl);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		}
	}

	private static class SystemMessageHolder extends RecyclerView.ViewHolder {
		TextView messageView;

		SystemMessageHolder(View itemView) {
			super(itemView);

			messageView = itemView.findViewById(R.id.messageView);
		}

		void bind(ChatItem message) {
			messageView.setText(message.content);
		}
	}

	private static class DateHolder extends RecyclerView.ViewHolder {
		TextView messageView;

		DateHolder(View itemView) {
			super(itemView);

			messageView = itemView.findViewById(R.id.messageView);
		}

		void bind(DateItem message) {
			messageView.setText(message.text);
		}
	}


	private static class IncomingImageHolder extends RecyclerView.ViewHolder {
		ImageView imageView;
		ImageView avatarView;
		TextView nameView;
		TextView timeView;

		IncomingImageHolder(View itemView) {
			super(itemView);

			imageView = itemView.findViewById(R.id.image);
			nameView = itemView.findViewById(R.id.nameView);
			avatarView = itemView.findViewById(R.id.avatarView);
			timeView = itemView.findViewById(R.id.timeView);
		}

		void bind(ChatItem message, Consumer<String> onFileClickListener) {
			loadImage(message, imageView);

			loadAvatar(avatarView, message);
			setOperatorName(nameView, message);
			setState(timeView, message);

			if (onFileClickListener != null) {
				imageView.setOnClickListener(view -> {
					try {
						onFileClickListener.accept(message.fileUrl);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		}
	}

	private static class OutgoingImageHolder extends RecyclerView.ViewHolder {
		ImageView imageView;
		TextView timeView;

		OutgoingImageHolder(View itemView) {
			super(itemView);

			imageView = itemView.findViewById(R.id.image);
			timeView = itemView.findViewById(R.id.timeView);
		}

		void bind(ChatItem message, Consumer<String> onFileClickListener) {
			loadImage(message, imageView);

			setState(timeView, message);

			if (onFileClickListener != null) {
				imageView.setOnClickListener(view -> {
					try {
						onFileClickListener.accept(message.fileUrl);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		}
	}

	private static void setState(TextView timeView, ChatItem message) {
		timeView.setText(DateUtils.dateToTime(message.createdAt));

		if (message.isIncoming) {
			return;
		}

		// todo: double check should be on read state
		Drawable drawable = null;
		int dSize = timeView.getResources().getDimensionPixelOffset(R.dimen.chat_message_indicator_size);
		switch (message.sentState) {
			case NOT_SENT:
			case SENDING:
				drawable = ContextCompat.getDrawable(timeView.getContext(), R.drawable.check);
				break;
			case SENT:
				drawable = ContextCompat.getDrawable(timeView.getContext(), R.drawable.double_check);
				break;
			case FAILED:
				drawable = ContextCompat.getDrawable(timeView.getContext(), R.drawable.alert);
				break;
		}
		if (drawable != null) {
			drawable.setBounds(0, 0, dSize, dSize);
			timeView.setCompoundDrawables(drawable, null, null, null);
		}
	}

	// For better implementation see https://bumptech.github.io/glide/int/recyclerview.html
	private static void loadImage(ChatItem message, ImageView imageView) {
		int cornersRadius = imageView.getResources().getDimensionPixelOffset(R.dimen.chat_image_corner_radius);

		Glide.with(imageView.getContext())
				.load(message.fileUrl)
				.placeholder(R.drawable.placeholder)
				.error(R.drawable.placeholder)
				.centerCrop()
				.dontAnimate()
				.transform(new RoundedCorners(cornersRadius))
				.into(imageView);
	}

	// For better implementation see https://bumptech.github.io/glide/int/recyclerview.html
	private static void loadAvatar(ImageView avatarView, ChatItem message) {
		Object avatarUrl;
		if (!message.isBot) {
			avatarUrl = ((Employee) message.creator).avatarUrl;
		} else {
			avatarUrl = R.drawable.logo;
		}

		if (avatarUrl != null) {
			Glide.with(avatarView.getContext())
					.load(avatarUrl)
					.placeholder(R.drawable.avatar)
					.error(R.drawable.avatar)
					.centerCrop()
					.dontAnimate()
					.apply(RequestOptions.circleCropTransform())
					.into(avatarView);
		} else {
			avatarView.setImageResource(R.drawable.avatar);
		}
	}

	private static void setOperatorName(TextView nameView, ChatItem message) {
		String name = null;
		if (!message.isBot) {
			name = ((Employee) message.creator).name;
		}

		nameView.setText(name);
		nameView.setVisibility(!TextUtils.isEmpty(name) ? View.VISIBLE : View.GONE);
	}

	private static void handleQuotedText(ChatItem message, TextView quoteView, View quoteSeparatorView) {
		boolean hasQuote = message.quoteText != null;
		quoteView.setVisibility(hasQuote ? View.VISIBLE : View.GONE);
		quoteSeparatorView.setVisibility(hasQuote ? View.VISIBLE : View.GONE);
		quoteView.setText(message.quoteText);
	}

	private static void handleLinkPreview(TextView messageView, Spannable text, String messageId) {
		String link = ru.livetex.sdk.ui.utils.TextUtils.getFirstLink(text);
		if (link != null && !Objects.equals(ChatState.instance.previewsMap.get(messageId), false)) {
			int width = messageView.getResources().getDimensionPixelOffset(R.dimen.chat_message_link_preview_width);
			int height = messageView.getResources().getDimensionPixelOffset(R.dimen.chat_message_link_preview_height);
			Glide.with(messageView.getContext())
					.load(link)
					.addListener(new RequestListener<Drawable>() {
						@Override
						public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
							ChatState.instance.previewsMap.put(messageId, false);
							return false;
						}

						@Override
						public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
							ChatState.instance.previewsMap.put(messageId, true);
							return false;
						}
					})
					.into(new CustomTarget<Drawable>(width, height) {
						@Override
						public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
							messageView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, resource);
						}

						@Override
						public void onLoadCleared(@Nullable Drawable placeholder) {

						}
					});
		}
	}
}
