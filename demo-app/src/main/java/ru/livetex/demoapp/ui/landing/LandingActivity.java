package ru.livetex.demoapp.ui.landing;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import ru.livetex.demoapp.R;
import ru.livetex.demoapp.ui.settings.SettingsActivity;
import ru.livetex.sdkui.chat.ChatActivity;

public class LandingActivity extends AppCompatActivity {

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.a_landing);

      findViewById(R.id.chat).setOnClickListener(v -> {
         startActivity(new Intent(this, ChatActivity.class));
         finish();
         overridePendingTransition(0, android.R.anim.fade_out);
      });
      findViewById(R.id.settings).setOnClickListener(v -> {
         startActivity(new Intent(this, SettingsActivity.class));
      });
   }

}
