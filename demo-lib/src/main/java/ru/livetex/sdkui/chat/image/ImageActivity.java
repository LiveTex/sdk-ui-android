package ru.livetex.sdkui.chat.image;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import ru.livetex.sdkui.R;

public class ImageActivity extends AppCompatActivity {

	public static final String EXTRA_URL = "EXTRA_URL";

	private ImageView imageView;
	private GestureDetector gestureDetector;

	private final ActivityResultLauncher<String> requestPermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
				if (isGranted) {
					downloadImage();
				}
			});

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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.image, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.save) {
			boolean isGranted = ContextCompat.checkSelfPermission(
					this,
					Manifest.permission.WRITE_EXTERNAL_STORAGE
			) == PackageManager.PERMISSION_GRANTED;
			if (isGranted) {
				downloadImage();
			} else {
				requestPermissionLauncher.launch(
						Manifest.permission.WRITE_EXTERNAL_STORAGE
				);
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void downloadImage() {
		String url = getIntent().getStringExtra(EXTRA_URL);
		DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
		r.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, (System.currentTimeMillis() + ".jpg"));
		r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
		DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		dm.enqueue(r);
		Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
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