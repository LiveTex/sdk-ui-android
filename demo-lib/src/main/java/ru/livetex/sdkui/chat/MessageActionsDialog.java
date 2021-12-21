package ru.livetex.sdkui.chat;

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
import ru.livetex.sdkui.R;
import ru.livetex.sdkui.chat.adapter.ChatItem;
import ru.livetex.sdkui.chat.db.ChatState;
import ru.livetex.sdkui.chat.db.entity.ChatMessage;
import ru.livetex.sdkui.chat.db.entity.MessageSentState;

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

	public void attach(MessageActionsListener listener) {
		resendView.setOnClickListener(v -> {
			listener.onResend();
			dismiss();
		});

		copyView.setOnClickListener(v -> {
			listener.onCopy();
			dismiss();
		});

		quoteView.setOnClickListener(v -> {
			listener.onQuote();
			dismiss();
		});
	}
}

interface MessageActionsListener {
	void onResend();

	void onCopy();

	void onQuote();
}