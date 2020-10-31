package ru.livetex.demoapp;

import android.app.Application;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

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
			FirebaseInstanceId.getInstance().getInstanceId()
					.addOnCompleteListener(task -> {
						if (!task.isSuccessful()) {
							Log.w(TAG, "getInstanceId failed", task.getException());
							initLiveTex();
							emitter.onComplete();
							return;
						}

						String token = task.getResult().getToken();
						Log.i(TAG, "firebase token = " + token);

						initLiveTex();
						emitter.onComplete();
					});
		});
	}

	private void initLiveTex() {
		new LiveTex.Builder(Const.TOUCHPOINT)
				.setDeviceToken(FirebaseInstanceId.getInstance().getToken())
				.build();
	}
}
