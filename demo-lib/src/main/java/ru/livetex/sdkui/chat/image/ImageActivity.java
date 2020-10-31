package ru.livetex.sdkui.chat.image;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import androidx.appcompat.app.AppCompatActivity;
import ru.livetex.sdkui.R;

public class ImageActivity extends AppCompatActivity {

	public static final String EXTRA_URL = "EXTRA_URL";

	private ImageView imageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_image);

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

	public static void start(Context context, String url) {
		Intent intent = new Intent(context, ImageActivity.class);
		intent.putExtra(EXTRA_URL, url);
		context.startActivity(intent);
	}
}