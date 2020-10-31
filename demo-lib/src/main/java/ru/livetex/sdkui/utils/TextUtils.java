package ru.livetex.sdkui.utils;

import android.annotation.SuppressLint;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.annotation.Nullable;

public final class TextUtils {

	@SuppressLint("ClickableViewAccessibility")
	public static Spannable setTextWithLinks(String source, TextView textView) {
		Spanned text = new SpannableString(source);
		SpannableString buffer = new SpannableString(text);
		Linkify.addLinks(buffer, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
		// Fix click area (https://stackoverflow.com/a/17246463/2190250)
		textView.setOnTouchListener((v, event) -> {
			boolean ret = false;
			TextView widget = (TextView) v;
			int action = event.getAction();

			if (action == MotionEvent.ACTION_UP ||
					action == MotionEvent.ACTION_DOWN) {
				int x = (int) event.getX();
				int y = (int) event.getY();

				x -= widget.getTotalPaddingLeft();
				y -= widget.getTotalPaddingTop();

				x += widget.getScrollX();
				y += widget.getScrollY();

				Layout layout = widget.getLayout();
				int line = layout.getLineForVertical(y);
				int off = layout.getOffsetForHorizontal(line, x);

				ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

				if (link.length != 0) {
					if (action == MotionEvent.ACTION_UP) {
						link[0].onClick(widget);
					}
					ret = true;
				}
			}
			return ret;
		});
		textView.setText(buffer);
		return buffer;
	}

	@Nullable
	public static String getFirstLink(Spannable source) {
		URLSpan[] spans = source.getSpans(0, source.length(), URLSpan.class);
		return spans != null && spans.length > 0 ? spans[0].getURL() : null;
	}
}
