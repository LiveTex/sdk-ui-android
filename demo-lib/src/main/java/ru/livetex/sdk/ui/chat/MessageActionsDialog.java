package ru.livetex.sdk.ui.chat;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import ru.livetex.sdk.R;
import ru.livetex.sdk.ui.chat.db.ChatState;
import ru.livetex.sdk.ui.chat.db.entity.ChatMessage;
import ru.livetex.sdk.ui.chat.db.entity.MessageSentState;
import ru.livetex.sdk.ui.chat.adapter.ChatItem;

public final class MessageActionsDialog extends Dialog {
	private static final String TAG = "MessageActionsDialog";

	private final boolean withResend;

	private View resendView;
	private View copyView;
	private View quoteView;

	public MessageActionsDialog(@NonNull Context context, boolean withResend) {
		super(context);
		this.withResend = withResend;

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.d_message_actions);
		getWindow().setBackgroundDrawable(null);
		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
	}

	@Override
	public void show() {
		super.show();

		resendView = findViewById(R.id.resendView);
		copyView = findViewById(R.id.copyView);
		quoteView = findViewById(R.id.quoteView);

		resendView.setVisibility(withResend ? View.VISIBLE : View.GONE);
	}

	public void attach(Activity activity, ChatViewModel viewModel, ChatItem item) {
		resendView.setOnClickListener(v -> {
			if (item.sentState == MessageSentState.FAILED) {
				ChatMessage message = ChatState.instance.getMessage(item.id);
				if (message != null) {
					viewModel.resendMessage(message);
				}
			}
			dismiss();
		});

		copyView.setOnClickListener(v -> {
			ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("Текст сообщения", item.content);
			clipboard.setPrimaryClip(clip);
			Toast.makeText(activity, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show();
			dismiss();
		});

		quoteView.setOnClickListener(v -> {
			viewModel.setQuoteText(item.content);
			dismiss();
		});
	}
}
