package ru.livetex.demoapp.ui.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import ru.livetex.demoapp.R;
import ru.livetex.sdkui.Const;
import ru.livetex.sdkui.utils.TextWatcherAdapter;

public class SettingsActivity extends AppCompatActivity {

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.a_settings);

      SharedPreferences sp = getSharedPreferences(Const.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
      String visitorToken = sp.getString(Const.KEY_VISITOR_TOKEN, null);
      String customToken = sp.getString(Const.KEY_CUSTOM_TOKEN, null);
      String customTouchpoint = sp.getString(Const.KEY_CUSTOM_TOUCHPOINT, "");

      ((TextView)findViewById(R.id.curVisitorToken)).setText(visitorToken);

      EditText customTokenEditText = findViewById(R.id.customToken);
      EditText customTouchpointEditText = findViewById(R.id.customTouchpoint);

      customTokenEditText.setText(customToken);
      customTokenEditText.addTextChangedListener(new TextWatcherAdapter() {
         @Override
         public void afterTextChanged(Editable editable) {
            String customToken = editable.toString();
            sp.edit().putString(Const.KEY_CUSTOM_TOKEN, customToken).apply();
         }
      });

      customTouchpointEditText.setText(customTouchpoint);
      customTouchpointEditText.addTextChangedListener(new TextWatcherAdapter() {
         @Override
         public void afterTextChanged(Editable editable) {
            String customTouchpoint = editable.toString();
            sp.edit().putString(Const.KEY_CUSTOM_TOUCHPOINT, customTouchpoint).apply();
         }
      });

      findViewById(R.id.visitorOnly).setOnClickListener(v -> {
         sp.edit().putInt(Const.KEY_CONNECT_TYPE, Const.TokenType.VISITOR.ordinal()).commit();
         finishOrRestart(customTouchpoint, sp);
      });
      findViewById(R.id.customOnly).setOnClickListener(v -> {
         sp.edit().putInt(Const.KEY_CONNECT_TYPE, Const.TokenType.CUSTOM.ordinal()).commit();
         finishOrRestart(customTouchpoint, sp);
      });
      findViewById(R.id.customAndVisitor).setOnClickListener(v -> {
         sp.edit().putInt(Const.KEY_CONNECT_TYPE, Const.TokenType.VISITOR_AND_CUSTOM.ordinal()).commit();
         finishOrRestart(customTouchpoint, sp);
      });
   }

   private void finishOrRestart(String customTouchpointInitial, SharedPreferences sp) {
      String customTouchpointNow = sp.getString(Const.KEY_CUSTOM_TOUCHPOINT, "");
      if (customTouchpointNow.equals(customTouchpointInitial)) {
         finish();
      } else {
         PackageManager packageManager = getPackageManager();
         Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
         ComponentName componentName = intent.getComponent();
         Intent mainIntent = Intent.makeRestartActivityTask(componentName);
         startActivity(mainIntent);
         Runtime.getRuntime().exit(0);
      }
   }
}
