package ru.livetex.sdk.ui.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

public final class DateUtils {

	private final static SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
	private final static SimpleDateFormat sdfDay = new SimpleDateFormat("dd EEEE", Locale.getDefault());

	private DateUtils() {}

	public static synchronized String dateToTime(Date date) {
		return sdfTime.format(date);
	}

	public static synchronized String dateToDay(Date date) {
		if (isToday(date.getTime())) {
			return "Сегодня";
		}
		return sdfDay.format(date);
	}

	private static boolean isToday(long time) {
		long todayStart = getDayStartForCalendar(System.currentTimeMillis(), Calendar.getInstance());
		long todayEnd = todayStart + android.text.format.DateUtils.DAY_IN_MILLIS;
		return time >= todayStart && time < todayEnd;
	}

	private static long getDayStartForCalendar(long originTime, @NonNull Calendar calendar) {
		calendar.setTimeInMillis(originTime);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTimeInMillis();
	}
}