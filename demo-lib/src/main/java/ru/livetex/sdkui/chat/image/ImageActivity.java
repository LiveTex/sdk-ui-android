package ru.livetex.sdkui.chat.image;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
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
				.dontAnimate()
				.into(imageView);
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

	public static void start(Context context, String url) {
		Intent intent = new Intent(context, ImageActivity.class);
		intent.putExtra(EXTRA_URL, url);
		context.startActivity(intent);
	}
}