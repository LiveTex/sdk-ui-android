package ru.livetex.sdkui.utils;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.MotionEvent;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;

public final class TextUtils {
	private static final String HTML_PATTERN = "<(\"[^\"]*\"|'[^']*'|[^'\">])*>";
	private static final Pattern pattern = Pattern.compile(HTML_PATTERN);

	public static boolean containsHtml(String text) {
		Matcher matcher = pattern.matcher(text);
		return matcher.find();
	}

	@SuppressLint("ClickableViewAccessibility")
	public static Spannable setTextWithLinks(String source, TextView textView) {
		boolean hasHtml = containsHtml(source);
		Spanned text;

		if (hasHtml) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				text = Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
			} else {
				text = Html.fromHtml(source);
			}
		} else {
			text = new SpannableString(source);
		}

		URLSpan[] currentSpans = hasHtml ? text.getSpans(0, text.length(), URLSpan.class) : new URLSpan[0];

		SpannableString buffer = new SpannableString(text);
		Linkify.addLinks(buffer, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

		for (Object span : currentSpans) {
			int start = text.getSpanStart(span);
			int end = text.getSpanEnd(span);
			buffer.setSpan(span, start, end, 0);
		}

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

				URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);

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
