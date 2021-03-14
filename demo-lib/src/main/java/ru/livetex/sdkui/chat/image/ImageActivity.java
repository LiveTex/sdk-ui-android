package ru.livetex.sdkui.chat.image;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;

import androidx.appcompat.app.AppCompatActivity;
import ru.livetex.sdkui.R;

public class ImageActivity extends AppCompatActivity {

	public static final String EXTRA_URL = "EXTRA_URL";

	private ImageView imageView;
	private GestureDetector gestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_image);

		gestureDetector = new GestureDetector(this, new MyGestureDetector(this));

		MaterialToolbar toolbar = findViewById(R.id.toolbar);

		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		toolbar.setNavigationOnClickListener(view -> finish());

		imageView = findViewById(R.id.imageView);

		String url = getIntent().getStringExtra(EXTRA_URL);

		Glide.with(this)
				.load(url)
				.placeholder(R.drawable.placeholder)
				.error(R.drawable.placeholder)
				.centerCrop()
				.dontAnimate()
				.into(imageView);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// TouchEvent dispatcher.
		if (gestureDetector != null) {
			if (gestureDetector.onTouchEvent(ev))
			// If the gestureDetector handles the event, a swipe has been
			// executed and no more needs to be done.
			{
				return true;
			}
		}

		return super.dispatchTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		gestureDetector = null;
	}

	private static class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
		private static final int SWIPE_MIN_DISTANCE = 150;
		private static final int SWIPE_MAX_OFF_PATH = 300;
		private static final int SWIPE_THRESHOLD_VELOCITY = 200;

		private final Activity activity;

		private MyGestureDetector(Activity activity) {this.activity = activity;}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

			// Check movement along the Y-axis. If it exceeds SWIPE_MAX_OFF_PATH,
			// then dismiss the swipe.
			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
				return false;
			}

			// from left to right
			if (e2.getX() > e1.getX()) {
				if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					activity.finish();
					return true;
				}
			}

			// from right to left
			if (e1.getX() > e2.getX()) {
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					activity.finish();
					return true;
				}
			}

			return false;
		}
	}

	public static void start(Context context, String url) {
		Intent intent = new Intent(context, ImageActivity.class);
		intent.putExtra(EXTRA_URL, url);
		context.startActivity(intent);
	}
}