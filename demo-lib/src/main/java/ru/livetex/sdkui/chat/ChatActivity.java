package ru.livetex.sdkui.chat;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.button.MaterialButton;
import com.yalantis.ucrop.UCrop;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import ru.livetex.sdk.entity.Department;
import ru.livetex.sdk.entity.DialogState;
import ru.livetex.sdk.network.NetworkManager;
import ru.livetex.sdkui.Const;
import ru.livetex.sdkui.R;
import ru.livetex.sdkui.chat.adapter.AdapterItem;
import ru.livetex.sdkui.chat.adapter.ChatItem;
import ru.livetex.sdkui.chat.adapter.ChatMessageDiffUtil;
import ru.livetex.sdkui.chat.adapter.DateItem;
import ru.livetex.sdkui.chat.adapter.EmployeeTypingItem;
import ru.livetex.sdkui.chat.adapter.ItemType;
import ru.livetex.sdkui.chat.adapter.MessagesAdapter;
import ru.livetex.sdkui.chat.db.ChatState;
import ru.livetex.sdkui.chat.db.entity.ChatMessage;
import ru.livetex.sdkui.chat.db.entity.MessageSentState;
import ru.livetex.sdkui.chat.image.ImageActivity;
import ru.livetex.sdkui.utils.DateUtils;
import ru.livetex.sdkui.utils.FileUtils;
import ru.livetex.sdkui.utils.InputUtils;
import ru.livetex.sdkui.utils.IntentUtils;
import ru.livetex.sdkui.utils.RecyclerViewScrollListener;
import ru.livetex.sdkui.utils.TextWatcherAdapter;
import ru.livetex.sdkui.utils.picker.LivetexPicker;
import ru.livetex.sdkui.utils.picker.LivetexPickerHandler;

public class ChatActivity extends AppCompatActivity implements LivetexPickerHandler {
	private static final String TAG = "MainActivity";
	private static final int REQUEST_CODE_STORAGE = 2000;

	private final CompositeDisposable disposables = new CompositeDisposable();
	private ChatViewModel viewModel;
	private final MessagesAdapter adapter = new MessagesAdapter(button -> viewModel.onMessageActionButtonClicked(this, button));
	private AddFileDialog addFileDialog = null;

	private final static long TEXT_TYPING_DELAY = 500L; // milliseconds
	private final PublishSubject<String> textSubject = PublishSubject.create();

	private EditText inputView;
	private ImageView addView;
	private ImageView sendView;
	private RecyclerView messagesView;
	private ViewGroup inputContainerView;
	private ViewGroup inputFieldContainerView;
	private ViewGroup attributesContainerView;
	private ViewGroup departmentsContainerView;
	private ViewGroup departmentsButtonContainerView;
	private ViewGroup feedbackContainerView;
	private ImageView feedbackPositiveView;
	private ImageView feedbackNegativeView;
	private View attributesSendView;
	private EditText attributesNameView;
	private EditText attributesPhoneView;
	private EditText attributesEmailView;
	private ImageView filePreviewView;
	private ImageView filePreviewDeleteView;
	private TextView fileNameView;
	private ViewGroup quoteContainerView;
	private TextView quoteView;
	private ImageView quoteCloseView;

	// For disconnecting from websocket on app background and connecting on app foreground. If you need active websocket while app in background, just use viewModel.start()
	private final LifecycleObserver appLifecycleObserver = new DefaultLifecycleObserver() {
		@Override
		public void onStart(@NonNull LifecycleOwner owner) {
			viewModel.start();
		}

		@Override
		public void onStop(@NonNull LifecycleOwner owner) {
			viewModel.stop();
		}
	};

//	private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
//			registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
//				// Callback is invoked after the user selects a media item or closes the
//				// photo picker.
//				if (uri != null) {
//					Disposable d = Single
//							.fromCallable(() -> FileUtils.getPath(this, uri))
//							.subscribeOn(Schedulers.io())
//							.observeOn(AndroidSchedulers.mainThread())
//							.subscribe(path -> {
//								Uri newUri = Uri.fromFile(new File(path));
//								if (!FileUtils.getMimeType(this, newUri).contains("video")) {
//									addFileDialog.crop(this, newUri);
//								} else {
//									onFileSelected(newUri);
//								}
//							}, thr -> Log.e(TAG, "SELECT_IMAGE_OR_VIDEO", thr));
//				} else {
//					closeFileDialog();
//				}
//			});

	private LivetexPicker picker = new LivetexPicker(this, this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_chat);

		inputView = findViewById(R.id.inputView);
		sendView = findViewById(R.id.sendView);
		addView = findViewById(R.id.addView);
		messagesView = findViewById(R.id.messagesView);
		inputContainerView = findViewById(R.id.inputContainerView);
		inputFieldContainerView = findViewById(R.id.inputFieldContainerView);
		attributesContainerView = findViewById(R.id.attributesContainerView);
		departmentsContainerView = findViewById(R.id.departmentsContainerView);
		departmentsButtonContainerView = findViewById(R.id.departmentsButtonContainerView);
		feedbackContainerView = findViewById(R.id.feedbackContainerView);
		feedbackPositiveView = findViewById(R.id.feedbackPositiveView);
		feedbackNegativeView = findViewById(R.id.feedbackNegativeView);
		attributesSendView = findViewById(R.id.attributesSendView);
		attributesNameView = findViewById(R.id.attributesNameView);
		attributesPhoneView = findViewById(R.id.attributesPhoneView);
		attributesEmailView = findViewById(R.id.attributesEmailView);
		filePreviewView = findViewById(R.id.filePreviewView);
		filePreviewDeleteView = findViewById(R.id.filePreviewDeleteView);
		fileNameView = findViewById(R.id.fileNameView);
		quoteContainerView = findViewById(R.id.quoteContainerView);
		quoteView = findViewById(R.id.quoteView);
		quoteCloseView = findViewById(R.id.quoteCloseView);

		viewModel = new ChatViewModelFactory(getSharedPreferences(Const.SHARED_PREFS_NAME, Context.MODE_PRIVATE)).create(ChatViewModel.class);

		setupUI();
		subscribeViewModel();
		NetworkManager.getInstance().startObserveNetworkState(this);
		ProcessLifecycleOwner.get().getLifecycle().addObserver(appLifecycleObserver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		disposables.clear();

		NetworkManager.getInstance().stopObserveNetworkState(this);
		ProcessLifecycleOwner.get().getLifecycle().removeObserver(appLifecycleObserver);
		// Force call because observer won't receive it
		viewModel.stop();

		closeFileDialog();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case AddFileDialog.RequestCodes.CAMERA: {
				if (resultCode == Activity.RESULT_OK) {
					addFileDialog.crop(this, addFileDialog.getSourceFileUri());
				} else {
					closeFileDialog();
				}
				break;
			}
			case UCrop.REQUEST_CROP: {
				if (resultCode == Activity.RESULT_OK && data != null) {
					final Uri resultUri = UCrop.getOutput(data);
					if (resultUri == null) {
						closeFileDialog();
						Log.e(TAG, "crop: resultUri == null");
						return;
					}
					addFileDialog.close(false);
					handleFile(resultUri);
				} else if (resultCode != Activity.RESULT_CANCELED) {
					Toast.makeText(this, "Ошибка при попытке вызвать редактор фото", Toast.LENGTH_SHORT).show();
					closeFileDialog();
				}
				break;
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case AddFileDialog.RequestCodes.CAMERA_PERMISSION:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					if (addFileDialog != null) {
						addFileDialog.onCameraPermissionGranted(this);
					}
				}
				break;
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	void resendMessage(ChatMessage message) {
		if (!canSendMessage()) {
			Toast.makeText(this, "Отправка сейчас недоступна", Toast.LENGTH_SHORT).show();
			return;
		}
		viewModel.resendMessage(message);
	}

	private void subscribeViewModel() {
		viewModel.viewStateLiveData.observe(this, this::setViewState);
		viewModel.errorLiveData.observe(this, this::onError);
		viewModel.connectionStateLiveData.observe(this, this::onConnectionStateUpdate);
		viewModel.departmentsLiveData.observe(this, this::showDepartments);
		viewModel.dialogStateUpdateLiveData.observe(this, this::updateDialogState);
	}

	private void setupUI() {
		setupInput();

		adapter.setOnMessageClickListener(item -> {
			MessageActionsDialog dialog = new MessageActionsDialog(this, item.sentState == MessageSentState.FAILED);
			dialog.show();
			dialog.attach(new MessageActionsListener() {
				@Override
				public void onResend() {
					if (item.sentState == MessageSentState.FAILED) {
						ChatMessage message = ChatState.instance.getMessage(item.id);
						if (message != null) {
							resendMessage(message);
						}
					}
				}

				@Override
				public void onCopy() {
					ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("Текст сообщения", item.content);
					clipboard.setPrimaryClip(clip);
					Toast.makeText(ChatActivity.this, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show();
				}

				@Override
				public void onQuote() {
					viewModel.setQuoteText(item.content);
				}
			});
		});

		adapter.setOnFileClickListener(fileUrl -> {
			// Download file or open full screen image
			// todo: will be something better in future
			boolean isImgFile = fileUrl.contains("jpg") ||
					fileUrl.contains("jpeg") ||
					fileUrl.contains("png") ||
					fileUrl.contains("bmp");

			if (isImgFile) {
				ImageActivity.start(this, fileUrl);
			} else {
				IntentUtils.goUrl(this, fileUrl);
			}
		});

		messagesView.setAdapter(adapter);
//		DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(messagesView.getContext(),
//				DividerItemDecoration.VERTICAL);
//		dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divider));
//		messagesView.addItemDecoration(dividerItemDecoration);
		((SimpleItemAnimator) messagesView.getItemAnimator()).setSupportsChangeAnimations(false);

		disposables.add(ChatState.instance.messages()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::setMessages, thr -> Log.e(TAG, "messages observe", thr)));

		messagesView.addOnScrollListener(new RecyclerViewScrollListener((LinearLayoutManager) messagesView.getLayoutManager()) {
			@Override
			public void onLoadRequest() {
				String firstMessageId = null;
				for (AdapterItem adapterItem : adapter.getData()) {
					if (adapterItem.getAdapterItemType() == ItemType.CHAT_MESSAGE) {
						firstMessageId = ((ChatItem) adapterItem).getId();
						break;
					}
				}

				viewModel.loadPreviousMessages(firstMessageId, Const.PRELOAD_MESSAGES_COUNT);
			}

			@Override
			public boolean canLoadMore() {
				return viewModel.canPreloadMessages();
			}

			@Override
			protected void onScrollDown() {
			}
		});

		View.OnClickListener feedbackClickListener = v -> {
			feedbackContainerView.postDelayed(() -> feedbackContainerView.setVisibility(View.GONE), 250);
			viewModel.sendFeedback(v.getId() == R.id.feedbackPositiveView);
		};
		feedbackPositiveView.setOnClickListener(feedbackClickListener);
		feedbackNegativeView.setOnClickListener(feedbackClickListener);

		quoteCloseView.setOnClickListener(v -> {
			viewModel.setQuoteText(null);
		});
	}

	private void setMessages(List<ChatMessage> chatMessages) {
		List<AdapterItem> items = new ArrayList<>();
		List<String> days = new ArrayList<>();
		LinearLayoutManager layoutManager = (LinearLayoutManager) messagesView.getLayoutManager();
		boolean isLastMessageVisible = adapter.getItemCount() > 0 && layoutManager.findLastVisibleItemPosition() == adapter.getItemCount() - 1;

		for (ChatMessage chatMessage : chatMessages) {
			String dayDate = DateUtils.dateToDay(chatMessage.createdAt);

			if (!days.contains(dayDate)) {
				days.add(dayDate);
				items.add(new DateItem(dayDate));
			}

			if (!chatMessage.id.equals(ChatMessage.ID_TYPING)) {
				items.add(new ChatItem(chatMessage));
			} else {
				items.add(new EmployeeTypingItem(chatMessage));
			}
		}

		ChatMessageDiffUtil diffUtil =
				new ChatMessageDiffUtil(adapter.getData(), items);
		DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffUtil);

		adapter.setData(items);
		diffResult.dispatchUpdatesTo(adapter);

		if (isLastMessageVisible && adapter.getItemCount() > 0) {
			// post() allows to scroll when child layout phase is done and sizes are proper.
			messagesView.post(() -> {
				messagesView.smoothScrollToPosition(adapter.getItemCount());
			});
		}
	}

	private void setupInput() {
		Runnable sendAction = () -> {
			if (!validateSend()) {
				return;
			}
			// Send file or message
			if (viewModel.selectedFile != null) {
				String path = FileUtils.getRealPathFromUri(this, viewModel.selectedFile);
				if (path != null) {
					viewModel.sendFile(path);
				}
			} else {
				sendTextMessage();
			}
		};

		// --- Chat input
		sendView.setOnClickListener(v -> {
			sendAction.run();
		});

		inputView.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEND) {
				sendAction.run();
				return true;
			}
			return false;
		});

		addView.setOnClickListener(v -> {
			InputUtils.hideKeyboard(this);
			showAddFileDialog();
		});

		Disposable disposable = textSubject
				.throttleLast(TEXT_TYPING_DELAY, TimeUnit.MILLISECONDS)
				.observeOn(Schedulers.io())
				.subscribe(viewModel::sendTypingEvent, thr -> {
					Log.e(TAG, "typing observe", thr);
				});
		disposables.add(disposable);

		inputView.addTextChangedListener(new TextWatcherAdapter() {
			@Override
			public void afterTextChanged(Editable editable) {
				// notify about typing
				textSubject.onNext(editable.toString());
			}
		});

		filePreviewDeleteView.setOnClickListener(v -> {
			viewModel.onFileSelected(null);
		});

		// --- Attributes

		attributesSendView.setOnClickListener(v -> {
			String name = attributesNameView.getText().toString().trim();
			String phone = attributesPhoneView.getText().toString().trim();
			String email = attributesEmailView.getText().toString().trim();

			// Check for required fields. In demo only name is required, in real app depends on your configs.
			if (TextUtils.isEmpty(name)) {
				attributesNameView.setError("Заполните поле");
				attributesNameView.requestFocus();
				return;
			}

			InputUtils.hideKeyboard(this);
			viewModel.sendAttributes(name, phone, email);
		});
	}

	private boolean validateSend() {
		if (!canSendMessage()) {
			Toast.makeText(this, "Отправка сейчас недоступна", Toast.LENGTH_SHORT).show();
			return false;
		}
		if (!viewModel.isConnected) {
			Toast.makeText(this, "Нет соединения с сервером, попробуйте позже", Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	private void showAddFileDialog() {
		addFileDialog = new AddFileDialog(this);
		addFileDialog.show();
		addFileDialog.attach(new AddFileDialog.AddFileActions() {
			@Override
			public void onCamera() {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
						requestPermissions(new String[] { Manifest.permission.CAMERA },
								AddFileDialog.RequestCodes.CAMERA_PERMISSION);
						return;
					}
				}
				addFileDialog.requestCamera(ChatActivity.this);
			}

			@Override
			public void onGallery() {
				picker.selectMedia();
			}

			@Override
			public void onFile() {
				picker.selectFile();
			}
		});
	}

	private void setViewState(ChatViewStateData data) {
		if (data == null) {
			return;
		}

		// Set default state at first
		if (data.inputState == ChatInputState.HIDDEN) {
			inputContainerView.setVisibility(View.GONE);
		} else {
			inputContainerView.setVisibility(View.VISIBLE);
		}
		sendView.setEnabled(data.inputState != ChatInputState.DISABLED);
		inputView.setEnabled(true);

		inputFieldContainerView.setBackgroundResource(0);

		quoteContainerView.setVisibility(View.GONE);
		filePreviewView.setVisibility(View.GONE);
		filePreviewDeleteView.setVisibility(View.GONE);
		fileNameView.setVisibility(View.GONE);

		// Apply specific state
		switch (data.state) {
			case NORMAL:
				attributesContainerView.setVisibility(View.GONE);
				departmentsContainerView.setVisibility(View.GONE);

				break;
			case SEND_FILE_PREVIEW:
				// force disable of text input. Currently we can't send image and text together.
				inputFieldContainerView.setBackgroundResource(R.drawable.bg_input_field_container_disabled);
				inputView.setEnabled(false);

				// file preview img
				filePreviewView.setVisibility(View.VISIBLE);
				filePreviewDeleteView.setVisibility(View.VISIBLE);

				String mime = FileUtils.getMimeType(this, viewModel.selectedFile);

				if (mime != null && mime.contains("image")) {
					Glide.with(this)
							.load(viewModel.selectedFile)
							.placeholder(R.drawable.placeholder)
							.error(R.drawable.placeholder)
							.dontAnimate()
							.transform(new CenterCrop(), new RoundedCorners(getResources().getDimensionPixelOffset(R.dimen.chat_upload_preview_corner_radius)))
							.into(filePreviewView);
				} else {
					Glide.with(this)
							.load(R.drawable.doc_big)
							.dontAnimate()
							.transform(new CenterCrop(), new RoundedCorners(getResources().getDimensionPixelOffset(R.dimen.chat_upload_preview_corner_radius)))
							.into(filePreviewView);

					String filename = FileUtils.getFilename(this, viewModel.selectedFile);
					fileNameView.setVisibility(View.VISIBLE);
					fileNameView.setText(filename);
				}
				break;
			case QUOTE:
				quoteContainerView.setVisibility(View.VISIBLE);
				quoteView.setText(viewModel.getQuoteText());
				break;
			case ATTRIBUTES:
				InputUtils.hideKeyboard(this);
				inputView.clearFocus();
				inputContainerView.setVisibility(View.GONE);
				attributesContainerView.setVisibility(View.VISIBLE);
				break;
			case DEPARTMENTS:
				InputUtils.hideKeyboard(this);
				inputView.clearFocus();
				inputContainerView.setVisibility(View.GONE);
				attributesContainerView.setVisibility(View.GONE);
				departmentsContainerView.setVisibility(View.VISIBLE);
				break;
		}
	}

	private void showDepartments(List<Department> departments) {
		departmentsButtonContainerView.removeAllViews();

		for (Department department : departments) {
			MaterialButton view = (MaterialButton) View.inflate(this, R.layout.l_department_button, null);
			view.setText(department.name);
			view.setOnClickListener(v -> viewModel.selectDepartment(department));

			departmentsButtonContainerView.addView(view);
		}
	}

	@Override
	public void onFileSelected(@Nullable Uri uri) {
		if (uri == null) {
			return;
		}

		Disposable ignore = Maybe
				.fromCallable(() -> FileUtils.getPath(ChatActivity.this, uri))
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(path -> {
					Uri newUri = Uri.fromFile(new File(path));
					String mime = FileUtils.getMimeType(this, newUri);

					if (mime != null && mime.contains("image")) {
						addFileDialog.crop(this, newUri);
					} else {
						closeFileDialog();
						handleFile(newUri);
					}
				}, thr -> {
					Log.e(TAG, "onFileSelected", thr);
				}, () -> {
				});
	}

	private void handleFile(@Nullable Uri file) {
		if (file != null) {
			viewModel.onFileSelected(file);
		}
	}

	private void sendTextMessage() {
		String text = inputView.getText().toString().trim();

		if (TextUtils.isEmpty(text)) {
			Toast.makeText(this, "Введите сообщение", Toast.LENGTH_SHORT).show();
			return;
		}

		ChatMessage chatMessage = ChatState.instance.createNewTextMessage(text, viewModel.getQuoteText());
		inputView.setText(null);

		// wait a bit and scroll to newly created user message
		inputView.postDelayed(() -> messagesView.smoothScrollToPosition(adapter.getItemCount() - 1), 200);

		viewModel.setQuoteText(null);
		viewModel.sendMessage(chatMessage);
	}

	private void closeFileDialog() {
		if (addFileDialog != null && addFileDialog.isShowing()) {
			addFileDialog.close(true);
			addFileDialog = null;
		}
	}

	/**
	 * Here you can use dialog status and employee data
	 */
	private void updateDialogState(DialogState dialogState) {
		boolean shouldShowFeedback = dialogState.employee != null && dialogState.employee.rating == null;
		feedbackContainerView.setVisibility(shouldShowFeedback ? View.VISIBLE : View.GONE);
		// Don't use showInput from DialogState, see ChatViewState
	}

	/**
	 * Convenient check of UI state (which in turn is driven by logic or state)
	 */
	private boolean canSendMessage() {
		return sendView.isEnabled() && inputContainerView.getVisibility() == View.VISIBLE;
	}

	private void onConnectionStateUpdate(NetworkManager.ConnectionState connectionState) {
		switch (connectionState) {
			case DISCONNECTED: {
				break;
			}
			case CONNECTING: {
				break;
			}
			case CONNECTED: {
				break;
			}
			default:
				break;
		}
	}

	private void onError(String msg) {
		if (TextUtils.isEmpty(msg)) {
			return;
		}
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}
}
