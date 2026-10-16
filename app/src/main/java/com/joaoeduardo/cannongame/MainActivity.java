package com.joaoeduardo.cannongame;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private CannonView cannonView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cannonView = new CannonView(this, null);
        setContentView(cannonView);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersiveMode();
        }
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE         |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN      |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION        |
            View.SYSTEM_UI_FLAG_FULLSCREEN             |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        cannonView.stopGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cannonView.resumeGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cannonView.releaseResources();
    }
}
