package ru.livetex.sdk.ui.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import ru.livetex.sdk.Const;
import ru.livetex.sdk.ui.chat.db.ChatState;
import ru.livetex.sdk.ui.chat.db.Mapper;
import ru.livetex.sdk.ui.chat.db.entity.ChatMessage;
import ru.livetex.sdk.ui.chat.db.entity.MessageSentState;
import ru.livetex.sdk.ui.utils.IntentUtils;
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
import ru.livetex.sdk.network.AuthTokenType;
import ru.livetex.sdk.network.NetworkManager;

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
	final MutableLiveData<ChatViewState> viewStateLiveData = new MutableLiveData<>(ChatViewState.NORMAL);
	final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

	// File for upload
	Uri selectedFile = null;
	boolean inputEnabled = true;
	private String quoteText = null;

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
				.subscribe(connectionStateLiveData::postValue, thr -> {
					Log.e(TAG, "connectionState", thr);
				}));

		disposables.add(messagesHandler.historyUpdate()
				.observeOn(Schedulers.io())
				.subscribe(this::updateHistory, thr -> {
					Log.e(TAG, "history", thr);
				}));

		disposables.add(messagesHandler.departmentRequest()
				.observeOn(Schedulers.io())
				.subscribe(this::onDepartmentsRequest, thr -> {
					Log.e(TAG, "departmentRequest", thr);
				}));

		disposables.add(messagesHandler.attributesRequest()
				.observeOn(Schedulers.io())
				.subscribe(attributesRequest -> {
					// Это лишь пример реализации того, как собрать и отправить аттрибуты.
					// Важно только ответить на attributesRequest посылкой обязательных (если есть) аттрибутов.
					// То есть если не требуется собирать аттрибуты от пользователя, можно просто ответить на запрос с помощью messagesHandler.sendAttributes
					viewStateLiveData.postValue(ChatViewState.ATTRIBUTES);

//					Disposable d = Completable.fromAction(() -> messagesHandler.sendAttributes("Demo user", null, null, null))
//							.subscribeOn(Schedulers.io())
//							.observeOn(Schedulers.io())
//							.subscribe(Functions.EMPTY_ACTION, thr -> Log.e(TAG, "", thr));
//					disposables.add(d);
				}, thr -> {
					Log.e(TAG, "", thr);
				}));

		disposables.add(messagesHandler.dialogStateUpdate()
				.observeOn(Schedulers.io())
				.subscribe(state -> {
					boolean inputStateChanged = this.inputEnabled != state.isInputEnabled();
					if (inputStateChanged) {
						this.inputEnabled = state.isInputEnabled();
						viewStateLiveData.postValue(viewStateLiveData.getValue());
					}
					dialogStateUpdateLiveData.postValue(state);
				}, thr -> {
					Log.e(TAG, "dialogStateUpdate", thr);
				}));

		disposables.add(messagesHandler.employeeTyping()
				.observeOn(Schedulers.io())
				.subscribe(event -> {
					if (dialogStateUpdateLiveData.getValue() == null) {
						// We need Employee info
						return;
					}
					if (ChatState.instance.getMessage(ChatMessage.ID_TYPING) == null) {
						ChatMessage typingMessage = ChatState.instance.createTypingMessage(dialogStateUpdateLiveData.getValue().employee);
					}
					if (employeeTypingDisposable != null && !employeeTypingDisposable.isDisposed()) {
						employeeTypingDisposable.dispose();
					}

					employeeTypingDisposable = Completable.timer(3, TimeUnit.SECONDS)
							.observeOn(Schedulers.io())
							.subscribe(() -> {
								ChatState.instance.removeMessage(ChatMessage.ID_TYPING, true);
							}, thr -> {
								Log.e(TAG, "employeeTyping disposable", thr);
							});
				}, thr -> {
					Log.e(TAG, "employeeTyping", thr);
				}));
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
		viewStateLiveData.postValue(ChatViewState.DEPARTMENTS);
	}

	void sendAttributes(String name, String phone, String email) {
		Disposable d = Completable.fromAction(() -> messagesHandler.sendAttributes(name, phone, email, null))
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.io())
				.subscribe(() -> {
					viewStateLiveData.postValue(ChatViewState.NORMAL);
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
		messagesHandler.sendTypingEvent(message);
	}

	void selectDepartment(Department department) {
		Disposable d = messagesHandler.sendDepartmentSelectionEvent(department.id)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(response -> {
					if (response.error != null && response.error.contains(LiveTexError.INVALID_DEPARTMENT)) {
						errorLiveData.setValue("Была выбрана невалидная комната");
					} else {
						viewStateLiveData.setValue(ChatViewState.NORMAL);
					}
				}, thr -> {
					errorLiveData.setValue(thr.getMessage());
					Log.e(TAG, "sendDepartmentSelectionEvent", thr);
				});
	}

	@Nullable
	public String getQuoteText() {
		return quoteText;
	}

	public void setQuoteText(@Nullable String quoteText) {
		this.quoteText = quoteText;
		if (TextUtils.isEmpty(this.quoteText)) {
			viewStateLiveData.setValue(ChatViewState.NORMAL);
		} else {
			viewStateLiveData.setValue(ChatViewState.QUOTE);
		}
	}

	public void onMessageActionButtonClicked(Context context, KeyboardEntity.Button button) {
		messagesHandler.sendButtonPressedEvent(button.payload);

		if (!TextUtils.isEmpty(button.url)) {
			// Delay for better visual UX
			Disposable d = Completable.timer(300, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
					.subscribe(() -> IntentUtils.goUrl(context, button.url), thr -> Log.e(TAG, "onMessageActionButtonClicked: go url", thr));
			disposables.add(d);
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
			ChatState.instance.removeMessage(ChatMessage.ID_TYPING, false);
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
					viewStateLiveData.postValue(ChatViewState.NORMAL);
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

		disposables.add(networkManager.connect(visitorToken, AuthTokenType.DEFAULT)
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.io())
				.subscribe(visitorTokenReceived -> {
					sp.edit().putString(Const.KEY_VISITOR_TOKEN, visitorTokenReceived).apply();
				}, e -> {
					Log.e(TAG, "connect", e);
					errorLiveData.postValue("Ошибка соединения " + e.getMessage());
				}));
	}

	public boolean canPreloadMessages() {
		return ChatState.instance.canPreloadChatMessages;
	}

	public void loadPreviousMessages(String messageId, int count) {
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

	public void sendFeedback(boolean isPositive) {
		Disposable d = Completable.fromAction(() -> messagesHandler.sendRatingEvent(isPositive))
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.io())
				.subscribe(Functions.EMPTY_ACTION, e -> {
					Log.e(TAG, "sendFeedback", e);
				});
		disposables.add(d);
	}

	public void onResume() {
		connect();
	}

	public void onPause() {
		networkManager.forceDisconnect();
	}
}