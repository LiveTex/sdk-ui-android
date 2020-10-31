package ru.livetex.demoapp.ui.splash;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ru.livetex.demoapp.App;
import ru.livetex.demoapp.R;
import ru.livetex.sdkui.chat.ChatActivity;

public class SplashActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_splash);
		Disposable d = App.getInstance().init()
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(() -> {
					startActivity(new Intent(this, ChatActivity.class));
					finish();
					overridePendingTransition(0, android.R.anim.fade_out);
				});
	}
}