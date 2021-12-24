package ru.livetex.demoapp.push;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import ru.livetex.demoapp.BuildConfig;
import ru.livetex.demoapp.R;
import ru.livetex.sdkui.chat.ChatActivity;

public final class FbMessagingService extends FirebaseMessagingService {

	private static final String TAG = "FbMessagingService";
	private static final String CHANNEL_ID = BuildConfig.APPLICATION_ID + ".notifications";

	@Override
	public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
		Log.d(TAG, "Received push message");

		if (remoteMessage.getData().size() > 0) {
			Log.d(TAG, "Message data payload: " + remoteMessage.getData());
		}

		if (remoteMessage.getNotification() != null) {
			Log.d(TAG, "Message notification body: " + remoteMessage.getNotification().getBody());
		}

		// todo: future api changes will allow to distinguish between LiveTex and non-LiveTex pushes

		sendNotification(remoteMessage);
	}


	private void sendNotification(RemoteMessage notificationMessage) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		android.app.Notification notification = buildNotification(notificationMessage, notificationManager);

		if (notificationManager != null) {
			notificationManager.notify(12345, notification);
		}
	}

	private static NotificationCompat.Builder createNotificationBuilder(Context context, NotificationManager notificationManager) {
		NotificationCompat.Builder notificationBuilder;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			String channelName = context.getString(R.string.notifications_channel_name);
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
			channel.setDescription("Chat messages");
			channel.setShowBadge(true);
			notificationManager.createNotificationChannel(channel);
		}

		notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);

		return notificationBuilder;
	}

	private android.app.Notification buildNotification(RemoteMessage remoteMessage, NotificationManager notificationManager) {
		NotificationCompat.Builder notificationBuilder = createNotificationBuilder(this, notificationManager);

		String messageTitle = remoteMessage.getNotification().getTitle();
		String messageText = remoteMessage.getNotification().getBody();

		// Open ChatActivity on click
		Intent resultIntent = new Intent(this, ChatActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addNextIntentWithParentStack(resultIntent);
		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

		notificationBuilder = notificationBuilder
				.setDefaults(android.app.Notification.DEFAULT_ALL)
				.setContentTitle(messageTitle)
				.setContentText(messageText)
				.setTicker(messageTitle)
				.setOnlyAlertOnce(true)
				.setAutoCancel(true)
				.setSmallIcon(R.drawable.logo)
				.setContentIntent(resultPendingIntent);

		return notificationBuilder.build();
	}

	@Override
	public void onNewToken(@NonNull String s) {
		Log.i(TAG, "New token = " + s);
		// todo: pass to Livetex when API will allow realtime updates. Or if you really need realtime token change, force reconnect
	}
}
