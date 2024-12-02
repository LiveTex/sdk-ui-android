package ru.livetex.sdkui.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.Functions;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import ru.livetex.sdk.LiveTex;
import ru.livetex.sdk.entity.Department;
import ru.livetex.sdk.entity.DepartmentRequestEntity;
import ru.livetex.sdk.entity.DialogState;
import ru.livetex.sdk.entity.FileMessage;
import ru.livetex.sdk.entity.GenericMessage;
import ru.livetex.sdk.entity.HistoryEntity;
import ru.livetex.sdk.entity.KeyboardEntity;
import ru.livetex.sdk.entity.LiveTexError;
import ru.livetex.sdk.entity.TextMessage;
import ru.livetex.sdk.logic.LiveTexMessagesHandler;
import ru.livetex.sdk.network.AuthData;
import ru.livetex.sdk.network.NetworkManager;
import ru.livetex.sdkui.Const;
import ru.livetex.sdkui.chat.adapter.AdapterItem;
import ru.livetex.sdkui.chat.adapter.EmployeeTypingItem;
import ru.livetex.sdkui.chat.adapter.RatingItem;
import ru.livetex.sdkui.chat.db.ChatState;
import ru.livetex.sdkui.chat.db.Mapper;
import ru.livetex.sdkui.chat.db.entity.ChatMessage;
import ru.livetex.sdkui.chat.db.entity.MessageSentState;
import ru.livetex.sdkui.utils.IntentUtils;

public final class ChatViewModel extends ViewModel {
	private static final String TAG = "ChatViewModel";

	private final CompositeDisposable disposables = new CompositeDisposable();
	private Disposable employeeTypingDisposable = null;
	private final SharedPreferences sp;
	private final LiveTexMessagesHandler messagesHandler = LiveTex.getInstance().getMessagesHandler();
	private final NetworkManager networkManager = LiveTex.getInstance().getNetworkManager();

	final MutableLiveData<NetworkManager.ConnectionState> connectionStateLiveData = new MutableLiveData<>();
	final MutableLiveData<List<Department>> departmentsLiveData = new MutableLiveData<>();
	final MutableLiveData<DialogState> dialogStateUpdateLiveData = new MutableLiveData<>();
	final MutableLiveData<ChatViewStateData> viewStateLiveData = new MutableLiveData<>(new ChatViewStateData(ChatViewState.NORMAL, ChatInputState.DISABLED));
	final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
	final BehaviorSubject<Boolean> updateMessagesSignal = BehaviorSubject.createDefault(false);

	// File for upload
	Uri selectedFile = null;
	// Flag indicating that input can be hidden by business logic
	boolean isInputShown = true;
	boolean isConnected = false;
	private String quoteText = null;
	private RatingItem ratingChatItem = null;
	private EmployeeTypingItem typingChatItem = null;

	public ChatViewModel(SharedPreferences sp) {
		this.sp = sp;
		subscribe();
	}

	@Override
	protected void onCleared() {
		super.onCleared();
		disposables.clear();
		networkManager.forceDisconnect();
	}

	/**
	 * Subscribe to connection state and chat events. Should be done before connect.
	 */
	private void subscribe() {
		disposables.add(networkManager.connectionState()
				.observeOn(Schedulers.io())
				.subscribe(this::onConnectionStateUpdate, thr -> {
					Log.e(TAG, "connectionState error", thr);
				}));

		disposables.add(messagesHandler.historyUpdate()
				.observeOn(Schedulers.io())
				.subscribe(this::updateHistory, thr -> {
					Log.e(TAG, "history update error", thr);
				}));

		disposables.add(messagesHandler.departmentRequest()
				.observeOn(Schedulers.io())
				.subscribe(this::onDepartmentsRequest, thr -> {
					Log.e(TAG, "departmentRequest error", thr);
				}));

		disposables.add(messagesHandler.attributesRequest()
				.observeOn(Schedulers.io())
				.subscribe(attributesRequest -> {
					// Это лишь пример реализации того, как собрать и отправить аттрибуты.
					// Важно только ответить на attributesRequest посылкой обязательных (если есть) аттрибутов.
					// То есть если не требуется собирать аттрибуты от пользователя, можно просто ответить на запрос с помощью messagesHandler.sendAttributes
					updateViewState(ChatViewState.ATTRIBUTES);

//					Disposable d = Completable.fromAction(() -> messagesHandler.sendAttributes("Demo user", null, null, null))
//							.subscribeOn(Schedulers.io())
//							.observeOn(Schedulers.io())
//							.subscribe(Functions.EMPTY_ACTION, thr -> Log.e(TAG, "", thr));
//					disposables.add(d);
				}, thr -> {
					Log.e(TAG, "attrs request error", thr);
				}));

		disposables.add(messagesHandler.dialogStateUpdate()
				.observeOn(Schedulers.io())
				.subscribe(this::onDialogStateUpdate, thr -> {
					Log.e(TAG, "dialogStateUpdate error", thr);
				}));

		disposables.add(messagesHandler.employeeTyping()
				.observeOn(Schedulers.io())
				.subscribe(event -> {
					if (dialogStateUpdateLiveData.getValue() == null) {
						// We need Employee info
						return;
					}
					if (typingChatItem == null) {
						typingChatItem = new EmployeeTypingItem(dialogStateUpdateLiveData.getValue().employee);
						updateMessagesSignal.onNext(true);
					}
					if (employeeTypingDisposable != null && !employeeTypingDisposable.isDisposed()) {
						employeeTypingDisposable.dispose();
					}

					employeeTypingDisposable = Completable.timer(3, TimeUnit.SECONDS)
							.observeOn(Schedulers.io())
							.subscribe(() -> {
								typingChatItem = null;
								updateMessagesSignal.onNext(true);
							}, thr -> {
								Log.e(TAG, "employeeTyping disposable error", thr);
							});
				}, thr -> {
					Log.e(TAG, "employeeTyping error", thr);
				}));
	}

	private void onConnectionStateUpdate(NetworkManager.ConnectionState connectionState) {
		connectionStateLiveData.postValue(connectionState);

		switch (connectionState) {
			case CONNECTED:
				if (!isConnected) {
					isConnected = true;
					updateViewState(viewStateLiveData.getValue().state);
				}
				break;
			default:
				if (isConnected) {
					isConnected = false;
					updateViewState(viewStateLiveData.getValue().state);
				}
				break;
		}
	}

	/**
	 * Give user ability to choose chat department (room). Select the one if only one in list.
	 */
	private void onDepartmentsRequest(DepartmentRequestEntity departmentRequestEntity) {
		List<Department> departments = departmentRequestEntity.departments;

		if (departments.isEmpty()) {
			errorLiveData.postValue("Список комнат пуст, свяжитесь с поддержкой");
			return;
		}

		if (departments.size() == 1) {
			selectDepartment(departments.get(0));
			return;
		}

		departmentsLiveData.postValue(departments);
		updateViewState(ChatViewState.DEPARTMENTS);
	}

	void sendAttributes(String name, String phone, String email) {
		Disposable d = Completable.fromAction(() -> messagesHandler.sendAttributes(name, phone, email, null))
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.io())
				.subscribe(() -> {
					updateViewState(ChatViewState.NORMAL);
				}, thr -> Log.e(TAG, "sendAttributes", thr));
		disposables.add(d);
	}

	void sendMessage(ChatMessage chatMessage) {
		Disposable d = messagesHandler.sendTextMessage(chatMessage.content)
				.doOnSubscribe(ignore -> {
					chatMessage.setSentState(MessageSentState.SENDING);
					ChatState.instance.addOrUpdateMessage(chatMessage);
				})
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.io())
				.subscribe(resp -> {
					// remove message with local id
					ChatState.instance.removeMessage(chatMessage.id, false);

					chatMessage.id = resp.sentMessage.id;
					chatMessage.setSentState(MessageSentState.SENT);
					// server time considered as correct one
					// also this is time when message was actually sent, not created
					chatMessage.createdAt = resp.sentMessage.createdAt;

					// in real project here should be saving (upsert) in persistent storage
					ChatState.instance.addOrUpdateMessage(chatMessage);
				}, thr -> {
					Log.e(TAG, "sendMessage", thr);
					errorLiveData.postValue("Ошибка отправки " + thr.getMessage());

					chatMessage.setSentState(MessageSentState.FAILED);
					ChatState.instance.addOrUpdateMessage(chatMessage);
				});
	}

	void sendFile(@NonNull String filePath) {
		ChatMessage chatMessage = ChatState.instance.createNewFileMessage(filePath);
		sendFileMessage(chatMessage);
	}

	void resendMessage(ChatMessage message) {
		if (TextUtils.isEmpty(message.fileUrl)) {
			sendMessage(message);
		} else {
			sendFileMessage(message);
		}
	}

	void sendTypingEvent(String message) {
		message = message.trim();
		if (TextUtils.isEmpty(message)) {
			return;
		}
		if (!isConnected) {
			return;
		}
		messagesHandler.sendTypingEvent(message);
	}

	void selectDepartment(Department department) {
		Disposable d = messagesHandler.sendDepartmentSelectionEvent(department.id)
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.io())
				.subscribe(response -> {
					if (response.error != null && response.error.contains(LiveTexError.INVALID_DEPARTMENT)) {
						errorLiveData.postValue("Была выбрана невалидная комната");
					} else {
						updateViewState(ChatViewState.NORMAL);
					}
				}, thr -> {
					errorLiveData.postValue(thr.getMessage());
					Log.e(TAG, "sendDepartmentSelectionEvent", thr);
				});
	}

	@Nullable
	String getQuoteText() {
		return quoteText;
	}

	void setQuoteText(@Nullable String quoteText) {
		this.quoteText = quoteText;
		if (TextUtils.isEmpty(this.quoteText)) {
			updateViewState(ChatViewState.NORMAL);
		} else {
			updateViewState(ChatViewState.QUOTE);
		}
	}

	@MainThread
	void onMessageActionButtonClicked(Context context, KeyboardEntity.Button button) {
		messagesHandler.sendButtonPressedEvent(button.payload);

		if (!TextUtils.isEmpty(button.url)) {
			// Delay for better visual UX
			Disposable d = Completable.timer(300, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
					.subscribe(() -> IntentUtils.goUrl(context, button.url), thr -> Log.e(TAG, "onMessageActionButtonClicked: go url", thr));
			disposables.add(d);
		}
	}

	boolean canPreloadMessages() {
		return ChatState.instance.canPreloadChatMessages;
	}

	@AnyThread
	void loadPreviousMessages(String messageId, int count) {
		Disposable d = messagesHandler.getHistory(messageId, count)
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.io())
				.subscribe(preloadedCount -> {
					if (preloadedCount < count) {
						ChatState.instance.canPreloadChatMessages = false;
					}
				}, e -> {
					Log.e(TAG, "loadPreviousMessages", e);
				});
		disposables.add(d);
	}

	@AnyThread
	void sendFeedback2points(boolean isPositive, @Nullable String comment) {
		Disposable d = Completable.fromAction(() -> messagesHandler.sendRatingEvent(isPositive, comment))
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.io())
				.subscribe(Functions.EMPTY_ACTION, e -> {
					Log.e(TAG, "sendFeedback2points error", e);
				});
		disposables.add(d);
	}

	@AnyThread
	void sendFeedback5points(float rating, @Nullable String comment) {
		Disposable d = Completable.fromAction(() -> messagesHandler.sendRatingEvent((short) Math.round(rating), comment))
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.io())
				.subscribe(Functions.EMPTY_ACTION, e -> {
					Log.e(TAG, "sendFeedback5points error", e);
				});
		disposables.add(d);
	}

	public List<AdapterItem> getAdditionalChatItems() {
		ArrayList<AdapterItem> list = new ArrayList<>();
		if (ratingChatItem != null) {
			list.add(ratingChatItem);
		}
		if (typingChatItem != null) {
			list.add(typingChatItem);
		}
		return list;
	}

	public void start() {
		connect();
	}

	public void stop() {
		networkManager.forceDisconnect();
	}

	void onFileSelected(@Nullable Uri file) {
		selectedFile = file;

		if (file != null) {
			setQuoteText(null);
			updateViewState(ChatViewState.SEND_FILE_PREVIEW);
		} else {
			updateViewState(ChatViewState.NORMAL);
		}
	}

	private void updateHistory(HistoryEntity historyEntity) {
		List<ChatMessage> messages = new ArrayList<>();
		for (GenericMessage genericMessage : historyEntity.messages) {
			if (genericMessage instanceof TextMessage) {
				ChatMessage chatMessage = Mapper.toChatMessage((TextMessage) genericMessage);
				messages.add(chatMessage);
			} else if (genericMessage instanceof FileMessage) {
				ChatMessage chatMessage = Mapper.toChatMessage((FileMessage) genericMessage);
				messages.add(chatMessage);
			}
		}

		// Remove "Employee Typing" indicator
		if (employeeTypingDisposable != null && !employeeTypingDisposable.isDisposed()) {
			employeeTypingDisposable.dispose();
			typingChatItem = null;
			// will be updated anyway
			//updateMessagesSignal.onNext(true);
		}

		ChatState.instance.addMessages(messages);
	}

	private void sendFileMessage(@NonNull ChatMessage chatMessage) {
		File f = new File(chatMessage.fileUrl);
		Disposable d = networkManager.getApiManager().uploadFile(f)
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.io())
				.doOnSubscribe(ignore -> {
					chatMessage.setSentState(MessageSentState.SENDING);
					ChatState.instance.addOrUpdateMessage(chatMessage);

					// return UI to normal
					selectedFile = null;
					updateViewState(ChatViewState.NORMAL);
				})
				.flatMap(messagesHandler::sendFileMessage)
				.subscribe(resp -> {
							// remove message with local id
							ChatState.instance.removeMessage(chatMessage.id, false);

							chatMessage.id = resp.sentMessage.id;
							chatMessage.setSentState(MessageSentState.SENT);
							// server time considered as correct one
							// also this is time when message was actually sent, not created
							chatMessage.createdAt = resp.sentMessage.createdAt;

							// in real project here should be saving (upsert) in persistent storage
							ChatState.instance.addOrUpdateMessage(chatMessage);

							f.delete();
						},
						thr -> {
							Log.e(TAG, "onFileUpload", thr);
							errorLiveData.postValue("Ошибка отправки " + thr.getMessage());

							chatMessage.setSentState(MessageSentState.FAILED);
							ChatState.instance.addOrUpdateMessage(chatMessage);
						});

		disposables.add(d);
	}

	private void connect() {
		String visitorToken = sp.getString(Const.KEY_VISITOR_TOKEN, null);
		String customToken = sp.getString(Const.KEY_CUSTOM_TOKEN, null);
		Const.TokenType connectType = Const.TokenType.values()[sp.getInt(Const.KEY_CONNECT_TYPE, 0)];

		AuthData authData;

		switch (connectType) {
			case VISITOR:
			default:
				authData = AuthData.withVisitorToken(visitorToken);
				break;
			case CUSTOM:
				authData = AuthData.withCustomVisitorToken(customToken);
				break;
			case VISITOR_AND_CUSTOM:
				authData = AuthData.withVisitorAndCustomTokens(visitorToken, customToken);
				break;
		}

		disposables.add(networkManager.connect(authData, true)
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.io())
				.subscribe(visitorTokenReceived -> {
					sp.edit().putString(Const.KEY_VISITOR_TOKEN, visitorTokenReceived).apply();
				}, e -> {
					Log.e(TAG, "connect error " + e.getMessage(), e);
					errorLiveData.postValue("Ошибка соединения " + e.getMessage());
				}));
	}

	private void updateViewState(ChatViewState viewState) {
		ChatInputState inputState;
		if (!isInputShown) {
			inputState = ChatInputState.HIDDEN;
		} else if (!isConnected) {
			inputState = ChatInputState.DISABLED;
		} else {
			inputState = ChatInputState.NORMAL;
		}
		ChatViewStateData newState = new ChatViewStateData(viewState, inputState);
		if (!Objects.equals(newState, viewStateLiveData.getValue())) {
			viewStateLiveData.postValue(newState);
		}
	}

	private void onDialogStateUpdate(DialogState state) {
		boolean inputStateChanged = this.isInputShown != state.canShowInput();
		if (inputStateChanged) {
			this.isInputShown = state.canShowInput();
			updateViewState(viewStateLiveData.getValue().state);
		}

		// In-chat rating item
		boolean shouldShowFeedback = state.rate != null &&
				state.rate.enabledType != null &&
				state.status == DialogState.DialogStatus.UNASSIGNED;

		boolean notify;
		if (shouldShowFeedback) {
			Date date;

			if (ratingChatItem == null) {
				// try to set date as a bit later than last message
				List<ChatMessage> messages = ChatState.instance.messages().blockingFirst();
				if (!messages.isEmpty()) {
					ChatMessage msg = messages.get(messages.size() - 1);
					date = msg.createdAt;
					date.setTime(date.getTime() + 1000);
				} else  {
					date = new Date();
				}
			} else {
				date = ratingChatItem.createdAt;
			}

			RatingItem item = new RatingItem(state.rate.textBefore,
					state.rate.textAfter,
					state.rate.commentEnabled != null && state.rate.commentEnabled,
					state.rate.isSet != null ? Integer.parseInt(state.rate.isSet.value) : 0,
					state.rate.isSet != null ? state.rate.isSet.value.equals("1") : null,
					state.rate.isSet != null ? state.rate.isSet.comment : null,
					state.rate.isSet != null,
					state.rate.enabledType,
					date);

			notify = !Objects.equals(ratingChatItem, item);
			ratingChatItem = item;
		} else {
			notify = ratingChatItem != null;
			this.ratingChatItem = null;
		}

		if (notify) {
			updateMessagesSignal.onNext(true);
		}

		dialogStateUpdateLiveData.postValue(state);
	}
}