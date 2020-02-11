package com.nsf.nsfsciencezone;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.nsf.nsfsciencezone.activity.BaseActivity;
import com.nsf.nsfsciencezone.activity.CarouselActivity;

public class SplashActivity extends BaseActivity implements Animation.AnimationListener {

    static public SplashActivity splashActivity;
    static public boolean dataIsLoaded = false;

    ImageView imageView01;
    ImageView imageView02;
    ImageView currentImageView;
    Animation fadeInAnim;
    Animation fadeOutAnim;

    // Images and variables for transitioning through background photos
    int[] img_ids = {R.mipmap.img_launch_image1, R.mipmap.img_launch_image2, R.mipmap.img_launch_image3, R.mipmap.img_launch_image4, R.mipmap.img_launch_image5, R.mipmap.img_launch_image6};
    int img_index = 0;
    int old_img_index = 0;

    // Track user timeouts
    long clickTime;

    int uiOptions;
    View mDecorView;

    // Track loading progress for all items (Brightcove & Webdam)
    NumberProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initProgressBar();
        initImages();
        initAnimators();
        initLabels();

        // Data is already loaded, so no need to wait
        if (dataIsLoaded) {
            showMainActivity();
        }

        showNextLaunchImage();
        cycleLaunchImages(1500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }

    private void initProgressBar() {
        progressBar = findViewById(R.id.progressBar);
        progressBar.setProgressTextSize(32);
        progressBar.setVisibility(View.VISIBLE);
    }

    public void updateProgressBar(int videosLoaded, int totalNumVideos) {
        double percentageComplete = (1.0 * videosLoaded) / totalNumVideos;
        int roundedPercentage = (int) Math.round(percentageComplete * 100);
        progressBar.setProgress(roundedPercentage);
    }

    public void updateProgressBar(int progress) {
        progressBar.setProgress(progress);
    }

    private void initImages() {
        imageView01 = findViewById(R.id.image_view01);
        imageView02 = findViewById(R.id.image_view02);
        currentImageView = imageView01;
    }

    private void initAnimators() {
        fadeInAnim = new AlphaAnimation(0.00f, 1.00f);
        fadeInAnim.setDuration(1000);
        fadeInAnim.setAnimationListener(this);
        fadeOutAnim = new AlphaAnimation(1.00f, 0.00f);
        fadeOutAnim.setDuration(1000);
    }

    private void initLabels() {
        TextView developerLabel = findViewById(R.id.developer_label);
        Typeface face;
        face = Typeface.createFromAsset(getAssets(), "fonts/Avenir.ttc");
        developerLabel.setTypeface(face);

        face = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Regular.ttf");
        TextView label = findViewById(R.id.loading_label);
        label.setTypeface(face);
    }

    public void showMainActivity() {
        Intent intent = new Intent(this, CarouselActivity.class);
        startActivity(intent);
        finish();

    }

    public void showNextLaunchImage() {
        imageView02.setImageResource(img_ids[img_index]);
        imageView02.startAnimation(fadeInAnim);

        old_img_index = img_index;
        img_index++;
        if (img_index >= img_ids.length) {
            img_index = 0;
        }
    }

    private void cycleLaunchImages(int delayMillis) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showNextLaunchImage();
                    }
                });
            }
        }, delayMillis);
    }

    //** UI and Animation overrides and helper functions **//
    @Override
    public void onBackPressed() {}

    @Override
    public void onAnimationStart(Animation animation) {}

    @Override
    public void onAnimationEnd(Animation animation) {

        if (dataIsLoaded == false) {
            imageView01.setImageResource(img_ids[old_img_index]);
            cycleLaunchImages(2000);
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {}

    private void hideSystemUI() {
        mDecorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        mDecorView.setSystemUiVisibility(uiOptions);
        mDecorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                // Note that system bars will only be "visible" if none of the
                // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    tryingHideNavigator();
                }
            }
        });
    }

    private void tryingHideNavigator() {
        clickTime = System.currentTimeMillis();
        new Handler().postDelayed(navigatorHideRunnable, 2500);
    }

    Runnable navigatorHideRunnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - clickTime > 2000) {
                mDecorView.setSystemUiVisibility(uiOptions);
            }
        }
    };

}
