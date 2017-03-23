package com.afollestad.materialcamera;

import android.app.Fragment;
import android.support.annotation.NonNull;

import com.afollestad.materialcamera.internal.BaseCaptureActivity;
import com.afollestad.materialcamera.internal.Camera2Fragment;

public class CaptureActivity2 extends BaseCaptureActivity {

    @Override
    @NonNull
    public Fragment getFragment() {
        this.setCanShowGuide(this.showGuide());
        return Camera2Fragment.newInstance();
    }
}