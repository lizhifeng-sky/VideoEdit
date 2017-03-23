package com.afollestad.materialcamera.internal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.afollestad.materialcamera.MaterialCamera;
import com.afollestad.materialcamera.R;
import com.afollestad.materialcamera.util.CameraUtil;
import com.afollestad.materialcamera.util.Degrees;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;

import static android.app.Activity.RESULT_CANCELED;
import static com.afollestad.materialcamera.internal.BaseCaptureActivity.CAMERA_POSITION_BACK;
import static com.afollestad.materialcamera.internal.BaseCaptureActivity.FLASH_MODE_ALWAYS_ON;
import static com.afollestad.materialcamera.internal.BaseCaptureActivity.FLASH_MODE_AUTO;
import static com.afollestad.materialcamera.internal.BaseCaptureActivity.FLASH_MODE_OFF;

/**
 * @author Aidan Follestad (afollestad)
 */
abstract class BaseCameraFragment extends Fragment implements CameraUriInterface, View.OnClickListener {

    protected TextView mButtonVideo;
    protected ImageButton mButtonStillshot;
    protected ImageView mButtonFacing;
//    protected ImageButton mButtonFlash;
//    protected TextView mRecordDuration;
    protected TextView mDelayStartCountdown;

    private boolean mIsRecording;
    protected String mOutputUri;
    protected BaseCaptureInterface mInterface;
    protected Handler mPositionHandler;
    protected MediaRecorder mMediaRecorder;
    protected ImageView cancel_video;
    protected ImageView save_video;
    private boolean canRecord=true;

    protected static void LOG(Object context, String message) {
        Log.d(context instanceof Class<?> ? ((Class<?>) context).getSimpleName() :
                context.getClass().getSimpleName(), message);
    }

    private final Runnable mPositionUpdater = new Runnable() {
        @Override
        public void run() {
            if (mInterface == null
//                    || mRecordDuration == null
                    ) return;
            final long mRecordStart = mInterface.getRecordingStart();
            final long mRecordEnd = mInterface.getRecordingEnd();
            if (mRecordStart == -1 && mRecordEnd == -1) return;
            final long now = System.currentTimeMillis();
            if (mRecordEnd != -1) {
                if (now >= mRecordEnd) {
                    stopRecordingVideo(true);
                } else {
                    final long diff = mRecordEnd - now;
                    if (diff<60000) {
//                    mRecordDuration.setText(String.format("-%s", CameraUtil.getDurationString(diff)));
                        mButtonVideo.setText(String.format("-%s", CameraUtil.getDurationString(diff)));
                    }else {
                        stopRecordingVideo(false);
                        mIsRecording = false;
                    }
                }
            } else {
//                mRecordDuration.setText(CameraUtil.getDurationString(now - mRecordStart));
                if (now-mRecordStart<60000) {
                    mButtonVideo.setText(CameraUtil.getDurationString(now - mRecordStart));
                }else {
                    stopRecordingVideo(false);
                    mIsRecording = false;
                }
            }
            if (mPositionHandler != null)
                mPositionHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.mcam_fragment_videocapture, container, false);
    }

    protected void setImageRes(ImageView iv, @DrawableRes int res) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && iv.getBackground() instanceof RippleDrawable) {
//            RippleDrawable rd = (RippleDrawable) iv.getBackground();
//            rd.setColor(ColorStateList.valueOf(CameraUtil.adjustAlpha(mIconTextColor, 0.3f)));
//        }
//        Drawable d = AppCompatResources.getDrawable(iv.getContext(), res);
//        d = DrawableCompat.wrap(d.mutate());
//        DrawableCompat.setTint(d, mIconTextColor);
//        iv.setImageDrawable(d);
    }
    protected void setImageRes(TextView iv, @DrawableRes int res) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && iv.getBackground() instanceof RippleDrawable) {
//            RippleDrawable rd = (RippleDrawable) iv.getBackground();
//            rd.setColor(ColorStateList.valueOf(CameraUtil.adjustAlpha(mIconTextColor, 0.3f)));
//        }
//        Drawable d = AppCompatResources.getDrawable(iv.getContext(), res);
//        d = DrawableCompat.wrap(d.mutate());
//        DrawableCompat.setTint(d, mIconTextColor);
//        iv.setBackground(d);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDelayStartCountdown = (TextView) view.findViewById(R.id.delayStartCountdown);
        mButtonVideo = (TextView) view.findViewById(R.id.video);
        mButtonStillshot = (ImageButton) view.findViewById(R.id.stillshot);
//        mRecordDuration = (TextView) view.findViewById(R.id.recordDuration);
        mButtonFacing = (ImageView) view.findViewById(R.id.facing);
        ImageView back = (ImageView) view.findViewById(R.id.back);
        save_video= (ImageView) view.findViewById(R.id.save);
        cancel_video= (ImageView) view.findViewById(R.id.delete);

        if (mInterface.shouldHideCameraFacing() || CameraUtil.isChromium()) {
            mButtonFacing.setVisibility(View.GONE);
        } else {
            setImageRes(mButtonFacing, mInterface.getCurrentCameraPosition() == CAMERA_POSITION_BACK ?
                    mInterface.iconFrontCamera() : mInterface.iconRearCamera());
        }

//        mButtonFlash = (ImageButton) view.findViewById(R.id.flash);
        setupFlashMode();

        mButtonVideo.setOnClickListener(this);
        mButtonStillshot.setOnClickListener(this);
        mButtonFacing.setOnClickListener(this);
        back.setOnClickListener(this);
        cancel_video.setOnClickListener(this);
        save_video.setOnClickListener(this);
//        mButtonFlash.setOnClickListener(this);
//        int primaryColor = getArguments().getInt(CameraIntentKey.PRIMARY_COLOR);
//        view.findViewById(R.id.controlsFrame).setBackgroundColor(primaryColor);
//        mRecordDuration.setTextColor(mIconTextColor);

        if (mMediaRecorder != null && mIsRecording) {
            setImageRes(mButtonVideo, mInterface.iconStop());
        } else {
            setImageRes(mButtonVideo, mInterface.iconRecord());
            mInterface.setDidRecord(false);
        }

        if (savedInstanceState != null)
            mOutputUri = savedInstanceState.getString("output_uri");

        if (mInterface.useStillshot()) {
            mButtonVideo.setVisibility(View.GONE);
//            mRecordDuration.setVisibility(View.GONE);
            mButtonStillshot.setVisibility(View.VISIBLE);
            setImageRes(mButtonStillshot, mInterface.iconStillshot());
//            mButtonFlash.setVisibility(View.VISIBLE);
        }

        if (mInterface.autoRecordDelay() < 1000) {
            mDelayStartCountdown.setVisibility(View.GONE);
        } else {
            mDelayStartCountdown.setText(Long.toString(mInterface.autoRecordDelay() / 1000));
        }
    }

    protected void onFlashModesLoaded() {
        if (getCurrentCameraPosition() != BaseCaptureActivity.CAMERA_POSITION_FRONT) {
            invalidateFlash(false);
        }
    }

    private boolean mDidAutoRecord = false;
    private Handler mDelayHandler;
    private int mDelayCurrentSecond = -1;

    protected void onCameraOpened() {
        if (mDidAutoRecord || mInterface == null || mInterface.useStillshot() || mInterface.autoRecordDelay() < 0 || getActivity() == null) {
            mDelayStartCountdown.setVisibility(View.GONE);
            mDelayHandler = null;
            return;
        }
        mDidAutoRecord = true;
        mButtonFacing.setVisibility(View.GONE);

        if (mInterface.autoRecordDelay() == 0) {
            mDelayStartCountdown.setVisibility(View.GONE);
            mIsRecording = startRecordingVideo();
            mDelayHandler = null;
            return;
        }

        mDelayHandler = new Handler();
        mButtonVideo.setEnabled(false);

        if (mInterface.autoRecordDelay() < 1000) {
            // Less than a second delay
            mDelayStartCountdown.setVisibility(View.GONE);
            mDelayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isAdded() || getActivity() == null || mIsRecording) return;
                    mButtonVideo.setEnabled(true);
                    mIsRecording = startRecordingVideo();
                    mDelayHandler = null;
                }
            }, mInterface.autoRecordDelay());
            return;
        }

        mDelayStartCountdown.setVisibility(View.VISIBLE);
        mDelayCurrentSecond = (int) mInterface.autoRecordDelay() / 1000;
        mDelayHandler.postDelayed(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (!isAdded() || getActivity() == null || mIsRecording) return;
                mDelayCurrentSecond -= 1;
                mDelayStartCountdown.setText(Integer.toString(mDelayCurrentSecond));

                if (mDelayCurrentSecond == 0) {
                    mDelayStartCountdown.setVisibility(View.GONE);
                    mButtonVideo.setEnabled(true);
                    mIsRecording = startRecordingVideo();
                    mDelayHandler = null;
                    return;
                }

                mDelayHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mButtonVideo = null;
        mButtonStillshot = null;
        mButtonFacing = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mInterface != null && mInterface.hasLengthLimit()) {
            if (mInterface.countdownImmediately() || mInterface.getRecordingStart() > -1) {
                if (mInterface.getRecordingStart() == -1)
                    mInterface.setRecordingStart(System.currentTimeMillis());
                startCounter();
            } else {
                if (mInterface.getLengthLimit()<60000) {
                    mButtonVideo.setText(String.format("-%s", CameraUtil.getDurationString(mInterface.getLengthLimit())));
                }else {
                    stopRecordingVideo(false);
                    mIsRecording = false;
                }
//                mRecordDuration.setText(String.format("-%s", CameraUtil.getDurationString(mInterface.getLengthLimit())));
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void onAttach(Activity activity) {
        super.onAttach(activity);
        mInterface = (BaseCaptureInterface) activity;
    }

    @NonNull
    protected final File getOutputMediaFile() {
        return CameraUtil.makeTempFile(getActivity(), null, "VID_", ".mp4");
    }

    @NonNull
    protected final File getOutputPictureFile() {
        return CameraUtil.makeTempFile(getActivity(), null, "IMG_", ".jpg");
    }

    public abstract void openCamera();

    public abstract void closeCamera();

    public void cleanup() {
        closeCamera();
        releaseRecorder();
        stopCounter();
    }

    public abstract void takeStillshot();

    public abstract void onPreferencesUpdated();

    @Override
    public void onPause() {
        super.onPause();
        cleanup();
    }

    @Override
    public final void onDetach() {
        super.onDetach();
        mInterface = null;
    }

    public final void startCounter() {
        if (mPositionHandler == null)
            mPositionHandler = new Handler();
        else mPositionHandler.removeCallbacks(mPositionUpdater);
        mPositionHandler.post(mPositionUpdater);
    }

    @BaseCaptureActivity.CameraPosition
    public final int getCurrentCameraPosition() {
        if (mInterface == null) return BaseCaptureActivity.CAMERA_POSITION_UNKNOWN;
        return mInterface.getCurrentCameraPosition();
    }

    public final int getCurrentCameraId() {
        if (mInterface.getCurrentCameraPosition() == BaseCaptureActivity.CAMERA_POSITION_BACK)
            return (Integer) mInterface.getBackCamera();
        else return (Integer) mInterface.getFrontCamera();
    }

    public final void stopCounter() {
        if (mPositionHandler != null) {
            mPositionHandler.removeCallbacks(mPositionUpdater);
            mPositionHandler = null;
        }
    }

    public final void releaseRecorder() {
        if (mMediaRecorder != null) {
            if (mIsRecording) {
                try {
                    mMediaRecorder.stop();
                } catch (Throwable t) {
                    //noinspection ResultOfMethodCallIgnored
                    new File(mOutputUri).delete();
                    t.printStackTrace();
                }
                mIsRecording = false;
            }
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    public boolean startRecordingVideo() {
        mButtonVideo.setTextColor(Color.WHITE);
        mButtonVideo.setBackgroundResource(R.drawable.record_background);
        cancel_video.setVisibility(View.GONE);
        save_video.setVisibility(View.GONE);
        if (mInterface != null && mInterface.hasLengthLimit() && !mInterface.countdownImmediately()) {
            // Countdown wasn't started in onResume, start it now
            if (mInterface.getRecordingStart() == -1)
                mInterface.setRecordingStart(System.currentTimeMillis());
            startCounter();
        }

        final int orientation = Degrees.getActivityOrientation(getActivity());
        //noinspection ResourceType
        getActivity().setRequestedOrientation(orientation);
        mInterface.setDidRecord(true);
        return true;
    }

    public void stopRecordingVideo(boolean reachedZero) {
        mButtonVideo.setTextColor(Color.rgb(119,119,119));
        mButtonVideo.setBackgroundResource(R.drawable.video_length);
        mButtonVideo.setPadding(36,18,36,18);
        cancel_video.setVisibility(View.VISIBLE);
        save_video.setVisibility(View.VISIBLE);
        canRecord=false;
//        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public final void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("output_uri", mOutputUri);
    }

    @Override
    public final String getOutputUri() {
        return mOutputUri;
    }

    protected final void throwError(Exception e) {
        Activity act = getActivity();
        if (act != null) {
            act.setResult(RESULT_CANCELED, new Intent().putExtra(MaterialCamera.ERROR_EXTRA, e));
            act.finish();
        }
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (id == R.id.facing) {
            canRecord=true;
            if (cancel_video.isShown()&&save_video.isShown()){
                new MaterialDialog.Builder(getActivity())
                                .title("提示")
                                .content("您已经录制了视频确定放弃吗？")
                                .positiveText("确定")
                                .negativeText("取消")
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                        save_video.setVisibility(View.GONE);
                                        cancel_video.setVisibility(View.GONE);
                                        mButtonVideo.setBackgroundResource(R.drawable.background);
                                        mButtonVideo.setText("");
                                        mInterface.toggleCameraPosition();
                                        setImageRes(mButtonFacing, mInterface.getCurrentCameraPosition() == BaseCaptureActivity.CAMERA_POSITION_BACK ?
                                                mInterface.iconFrontCamera() : mInterface.iconRearCamera());
                                        closeCamera();
                                        openCamera();
                                        setupFlashMode();
                                    }
                                })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                mButtonVideo.setEnabled(false);
                            }
                        })
                                .show();
            }else {
                mButtonVideo.setBackgroundResource(R.drawable.start_record);
                mButtonVideo.setText("");
                mInterface.toggleCameraPosition();
                setImageRes(mButtonFacing, mInterface.getCurrentCameraPosition() == BaseCaptureActivity.CAMERA_POSITION_BACK ?
                        mInterface.iconFrontCamera() : mInterface.iconRearCamera());
                closeCamera();
                openCamera();
                setupFlashMode();
            }

        } else if (id == R.id.video) {
            if (mIsRecording) {
                stopRecordingVideo(false);
                mIsRecording = false;
            } else {
                if (canRecord) {
                        mIsRecording = startRecordingVideo();
                }
            }
        } else if (id == R.id.stillshot) {
            takeStillshot();
        }else if (id==R.id.back){
            getActivity().finish();
        }else if (id==R.id.save){
            showPreView();
        }else if (id==R.id.delete){
            new MaterialDialog.Builder(getActivity())
                    .title("提示")
                    .content("要放弃当前拍摄的视频吗？")
                    .positiveText("确认")
                    .negativeText("取消")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            //重新拍摄
                            mInterface.onRetry(getOutputUri());
                        }
                    })
                    .show();
        }
    }

    public void showPreView() {

    }

    private void invalidateFlash(boolean toggle) {
        if (toggle) mInterface.toggleFlashMode();
        setupFlashMode();
        onPreferencesUpdated();
    }

    private void setupFlashMode() {
//        if (mInterface.shouldHideFlash()) {
//            mButtonFlash.setVisibility(View.GONE);
//            return;
//        } else {
//            mButtonFlash.setVisibility(View.VISIBLE);
//        }

        final int res;
        switch (mInterface.getFlashMode()) {
            case FLASH_MODE_AUTO:
                res = mInterface.iconFlashAuto();
                break;
            case FLASH_MODE_ALWAYS_ON:
                res = mInterface.iconFlashOn();
                break;
            case FLASH_MODE_OFF:
            default:
                res = mInterface.iconFlashOff();
        }

//        setImageRes(mButtonFlash, res);
    }
}