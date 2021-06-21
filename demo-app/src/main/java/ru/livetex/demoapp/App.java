package ru.livetex.demoapp;

import android.app.Application;
import android.util.Log;

import java.util.Objects;

import com.google.firebase.messaging.FirebaseMessaging;

import androidx.annotation.Nullable;
import io.reactivex.Completable;
import ru.livetex.sdk.LiveTex;

public class App extends Application {

	private static final String TAG = "App";

	private static App instance;

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		// For immediately working pushes LiveTex instance should be initialized after Firebase getInstanceId() finishes, it can take some time.
		// Chat can't be used until LiveTex instance create.
		// If you don't care about pushes init, just uncomment initLiveTex() here and don't use init().
		// initLiveTex();
	}

	public static App getInstance() {
		return instance;
	}

	public Completable init() {
		return Completable.create(emitter -> {
			FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
				if (!task.isSuccessful()) {
					Log.w(TAG, "Fetching FCM registration token failed", task.getException());
					initLiveTex(null);
					emitter.onComplete();
					return;
				}

				// Get new FCM registration token
				String token = task.getResult();
				Log.i(TAG, "firebase token = " + token);

				initLiveTex(token);
				emitter.onComplete();
			});
		});
	}

	private void initLiveTex(@Nullable String pushToken) {
		if (Objects.equals(Const.TOUCHPOINT, "YOUR_TOUCH_POINT")) {
			throw new IllegalArgumentException("Please set Const.TOUCHPOINT to your Livetex touchpoint");
		}
		new LiveTex.Builder(Const.TOUCHPOINT)
				.setDeviceToken(pushToken)
//				.setWebsocketLoggingEnabled()
//				.setNetworkLoggingEnabled()
				.build();
	}
}
