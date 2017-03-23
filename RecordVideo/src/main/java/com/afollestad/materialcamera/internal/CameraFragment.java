package com.afollestad.materialcamera.internal;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialcamera.CaptureActivity;
import com.afollestad.materialcamera.ICallback;
import com.afollestad.materialcamera.R;
import com.afollestad.materialcamera.util.CameraUtil;
import com.afollestad.materialcamera.util.Degrees;
import com.afollestad.materialcamera.util.ImageUtil;
import com.afollestad.materialcamera.util.ManufacturerUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.afollestad.materialcamera.internal.BaseCaptureActivity.CAMERA_POSITION_BACK;
import static com.afollestad.materialcamera.internal.BaseCaptureActivity.CAMERA_POSITION_FRONT;
import static com.afollestad.materialcamera.internal.BaseCaptureActivity.CAMERA_POSITION_UNKNOWN;
import static com.afollestad.materialcamera.internal.BaseCaptureActivity.FLASH_MODE_ALWAYS_ON;
import static com.afollestad.materialcamera.internal.BaseCaptureActivity.FLASH_MODE_AUTO;
import static com.afollestad.materialcamera.internal.BaseCaptureActivity.FLASH_MODE_OFF;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class CameraFragment extends BaseCameraFragment implements View.OnClickListener {

    CameraPreview mPreviewView;
    RelativeLayout mPreviewFrame;
    private AutoFitTextureView autoFitTextureView;

    private Camera.Size mVideoSize;
    private Camera mCamera;
    private Point mWindowSize;
    private int mDisplayOrientation;
    private boolean mIsAutoFocusing;
    List<Integer> mFlashModes;
    private LinearLayout body;
    private boolean mReachedZero;
    private boolean canShowPreView=false;
    private int videoWidth=640;
    private int videoHeight=480;

    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    private static Camera.Size chooseVideoSize(BaseCaptureInterface ci, List<Camera.Size> choices) {
        Camera.Size backupSize = null;
        for (Camera.Size size : choices) {
            if (size.height <= ci.videoPreferredHeight()) {
                if (size.width == size.height * ci.videoPreferredAspect())
                    return size;
                if (ci.videoPreferredHeight() >= size.height)
                    backupSize = size;
            }
        }
        if (backupSize != null) return backupSize;
        LOG(CameraFragment.class, "Couldn't find any suitable video size");
        return choices.get(choices.size() - 1);
    }

    private static Camera.Size chooseOptimalSize(List<Camera.Size> choices, int width, int height, Camera.Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Camera.Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.width;
        int h = aspectRatio.height;
        for (Camera.Size option : choices) {
            if (option.height == width * h / w &&
                    option.width >= width && option.height >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            LOG(CameraFragment.class, "Couldn't find any suitable preview size");
            return aspectRatio;
        }
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPreviewFrame = (RelativeLayout) view.findViewById(R.id.rootFrame);
        body= (LinearLayout) view.findViewById(R.id.body);
        autoFitTextureView= (AutoFitTextureView) view.findViewById(R.id.texture);
        autoFitTextureView.setVisibility(View.GONE);
        mPreviewFrame.setOnClickListener(this);
    }

    private void showPopupWindow() {
        CaptureActivity activity= (CaptureActivity) getActivity();
        activity.setCanShowGuide(false);
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.guide_record,null);
        final PopupWindow popupWindow = new PopupWindow(view,
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setTouchable(true);
        popupWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
                // 这里如果返回true的话，touch事件将被拦截
                // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
            }
        });

        // 如果不设置PopupWindow的背景，无论是点击外部区域还是Back键都无法dismiss弹框
        // 我觉得这里是API的一个bug
        popupWindow.setBackgroundDrawable(getResources().getDrawable(
                R.drawable.back_guide));

        // 设置好参数之后再show
        int[] location = new int[2];
        mButtonVideo.getLocationOnScreen(location);
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = view.getMeasuredWidth();
        int popupHeight =  view.getMeasuredHeight();
        popupWindow.showAtLocation(mButtonVideo, Gravity.NO_GRAVITY, (location[0]+mButtonVideo.getWidth()/2)-popupWidth/2,
                location[1]-popupHeight-20);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popupWindow.isShowing()){
                    popupWindow.dismiss();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            mPreviewView.getHolder().getSurface().release();
        } catch (Throwable ignored) {
        }
        mPreviewFrame = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        openCamera();
    }

    @Override
    public void onPause() {
        if (mCamera != null) mCamera.lock();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.rootFrame) {
            if (mCamera == null || mIsAutoFocusing) return;
            try {
                mIsAutoFocusing = true;
                mCamera.cancelAutoFocus();
                Camera.Parameters parameters=mCamera.getParameters();
                List<String> list=parameters.getSupportedFocusModes();
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        mIsAutoFocusing = false;
                        if (!success)
                            Toast.makeText(getActivity(), "Unable to auto-focus!", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } else {
            super.onClick(view);
        }
    }

    @Override
    public void openCamera() {
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) return;
        try {
            final int mBackCameraId = mInterface.getBackCamera() != null ? (Integer) mInterface.getBackCamera() : -1;
            final int mFrontCameraId = mInterface.getFrontCamera() != null ? (Integer) mInterface.getFrontCamera() : -1;
            if (mBackCameraId == -1 || mFrontCameraId == -1) {
                int numberOfCameras = Camera.getNumberOfCameras();
                if (numberOfCameras == 0) {
                    throwError(new Exception("No cameras are available on this device."));
                    return;
                }

                for (int i = 0; i < numberOfCameras; i++) {
                    //noinspection ConstantConditions
                    if (mFrontCameraId != -1 && mBackCameraId != -1) break;
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(i, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && mFrontCameraId == -1) {
                        mInterface.setFrontCamera(i);
                    } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK && mBackCameraId == -1) {
                        mInterface.setBackCamera(i);
                    }
                }
            }

            switch (getCurrentCameraPosition()) {
                case CAMERA_POSITION_FRONT:
                    setImageRes(mButtonFacing, mInterface.iconRearCamera());
                    break;
                case CAMERA_POSITION_BACK:
                    setImageRes(mButtonFacing, mInterface.iconFrontCamera());
                    break;
                case CAMERA_POSITION_UNKNOWN:
                default:
                    if (!getArguments().getBoolean(CameraIntentKey.DEFAULT_TO_FRONT_FACING, false)) {
                        // Check front facing first
                        if (mInterface.getFrontCamera() != null && (Integer) mInterface.getFrontCamera() != -1) {
                            setImageRes(mButtonFacing, mInterface.iconRearCamera());
                            mInterface.setCameraPosition(CAMERA_POSITION_FRONT);
                        } else {
                            setImageRes(mButtonFacing, mInterface.iconFrontCamera());
                            if (mInterface.getBackCamera() != null && (Integer) mInterface.getBackCamera() != -1)
                                mInterface.setCameraPosition(CAMERA_POSITION_BACK);
                            else mInterface.setCameraPosition(CAMERA_POSITION_UNKNOWN);
                        }
                    } else {
                        // Check back facing first
                        if (mInterface.getBackCamera() != null && (Integer) mInterface.getBackCamera() != -1) {
                            setImageRes(mButtonFacing, mInterface.iconFrontCamera());
                            mInterface.setCameraPosition(CAMERA_POSITION_BACK);
                        } else {
                            setImageRes(mButtonFacing, mInterface.iconRearCamera());
                            if (mInterface.getFrontCamera() != null && (Integer) mInterface.getFrontCamera() != -1)
                                mInterface.setCameraPosition(CAMERA_POSITION_FRONT);
                            else mInterface.setCameraPosition(CAMERA_POSITION_UNKNOWN);
                        }
                    }
                    break;
            }

            if (mWindowSize == null)
                mWindowSize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(mWindowSize);
            final int toOpen = getCurrentCameraId();
            mCamera = Camera.open(toOpen == -1 ? 0 : toOpen);
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> videoSizes = parameters.getSupportedVideoSizes();
            if (videoSizes == null || videoSizes.size() == 0)
                videoSizes = parameters.getSupportedPreviewSizes();
            mVideoSize = chooseVideoSize((BaseCaptureActivity) activity, videoSizes);
            Camera.Size previewSize = chooseOptimalSize(parameters.getSupportedPreviewSizes(),
                    mWindowSize.x, mWindowSize.y, mVideoSize);

            if (ManufacturerUtil.isSamsungGalaxyS3()) {
                parameters.setPreviewSize(ManufacturerUtil.SAMSUNG_S3_PREVIEW_WIDTH,
                        ManufacturerUtil.SAMSUNG_S3_PREVIEW_HEIGHT);
            } else {
                parameters.setPreviewSize(videoWidth, 480);
                Log.e("lzf_parameters", "line234  " + previewSize.width + "  " + previewSize.height);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    parameters.setRecordingHint(true);
            }
            List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
            int length = pictureSizes.size();
            for (int i = 0; i < length; i++) {
                Log.e("TAG","SupportedPictureSizes : " + pictureSizes.get(i).width + "x" + pictureSizes.get(i).height);
            }
            Camera.Size mStillShotSize = getHighestSupportedStillShotSize(parameters.getSupportedPictureSizes());
            Log.e("lzf_parameters", "line240  " + mStillShotSize.width + "    " + mStillShotSize.height);
            Log.e("lzf_pictureSize",previewSize.width+" "+ previewSize.height);
            parameters.setPictureSize(videoWidth, videoHeight);
            setCameraDisplayOrientation(parameters);
            mCamera.setParameters(parameters);
            mFlashModes = CameraUtil.getSupportedFlashModes(this.getActivity(), parameters);
            mInterface.setFlashModes(mFlashModes);
            onFlashModesLoaded();

            createPreview();
            mMediaRecorder = new MediaRecorder();

            onCameraOpened();
        } catch (IllegalStateException e) {
            throwError(new Exception("Cannot access the camera.", e));
        } catch (RuntimeException e2) {
            throwError(new Exception("Cannot access the camera, you may need to restart your device.", e2));
        }
    }

    private Camera.Size getHighestSupportedStillShotSize(List<Camera.Size> supportedPictureSizes) {
        Collections.sort(supportedPictureSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                if (lhs.height * lhs.width > rhs.height * rhs.width)
                    return -1;
                return 1;

            }
        });
        Camera.Size maxSize = supportedPictureSizes.get(0);
        Log.d("CameraFragment", "Using resolution: " + maxSize.width + "x" + maxSize.height);
        return maxSize;
    }

    @SuppressWarnings("WrongConstant")
    private void setCameraDisplayOrientation(Camera.Parameters parameters) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(getCurrentCameraId(), info);
        final int deviceOrientation = Degrees.getDisplayRotation(getActivity());
        mDisplayOrientation = Degrees.getDisplayOrientation(
                info.orientation, deviceOrientation, info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
        mCamera.setDisplayOrientation(90);
    }

    private void createPreview() {
        Activity activity = getActivity();
        if (activity == null) return;
        if (mWindowSize == null)
            mWindowSize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(mWindowSize);
        mPreviewView = new CameraPreview(getActivity(), mCamera);
//        if (mPreviewFrame.getChildCount() > 0 && mPreviewFrame.getChildAt(0) instanceof CameraPreview)
//            mPreviewFrame.removeViewAt(0);
//        mPreviewFrame.addView(mPreviewView, 0);
        if (body.getChildCount() > 0 && body.getChildAt(0) instanceof CameraPreview)
            body.removeViewAt(0);
//        RelativeLayout.LayoutParams layoutParams= new RelativeLayout.LayoutParams(
//                RelativeLayout.LayoutParams.WRAP_CONTENT,
//                RelativeLayout.LayoutParams.WRAP_CONTENT);
//        layoutParams.addRule(RelativeLayout.BELOW,R.id.top);
//        layoutParams.addRule(RelativeLayout.ABOVE,R.id.controlsFrame);
//        mPreviewView.setLayoutParams(layoutParams);
        body.addView(mPreviewView, 0);
        mPreviewView.setAspectRatio(videoHeight, videoWidth);
//        mPreviewView.setAspectRatio(240, 320);
//        mPreviewView.setAspectRatio(mWindowSize.x, mWindowSize.y);
    }

    @Override
    public void closeCamera() {
        try {
            if (mCamera != null) {
                try {
                    mCamera.lock();
                } catch (Throwable ignored) {
                }
                mCamera.release();
                mCamera = null;
            }
        } catch (IllegalStateException e) {
            throwError(new Exception("Illegal state while trying to close camera.", e));
        }
    }

    private boolean prepareMediaRecorder() {
        try {
            if (mCamera==null){
                openCamera();
            }
            final Activity activity = getActivity();
            if (null == activity) return false;
            final BaseCaptureInterface captureInterface = (BaseCaptureInterface) activity;

            setCameraDisplayOrientation(mCamera.getParameters());
            mMediaRecorder = new MediaRecorder();
            mCamera.stopPreview();
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);

            boolean canUseAudio = true;
            boolean audioEnabled = !mInterface.audioDisabled();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                canUseAudio = ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

            if (canUseAudio && audioEnabled) {
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            } else if (audioEnabled) {
                Toast.makeText(getActivity(), R.string.mcam_no_audio_access, Toast.LENGTH_LONG).show();
            }
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

            final CamcorderProfile profile = CamcorderProfile.get(getCurrentCameraId(), mInterface.qualityProfile());
            mMediaRecorder.setOutputFormat(profile.fileFormat);

            Log.e("lzf_type", "CameraFragment");
            mMediaRecorder.setVideoEncodingBitRate(1000000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(videoWidth, videoHeight);
//            mMediaRecorder.setVideoSize(320, 240);
//            mMediaRecorder.setVideoSize(mVideoSize.width, mVideoSize.height);
//            mMediaRecorder.setVideoFrameRate(mInterface.videoFrameRate(profile.videoFrameRate));
//            mMediaRecorder.setVideoEncodingBitRate(mInterface.videoEncodingBitRate(profile.videoBitRate));
            mMediaRecorder.setVideoEncoder(profile.videoCodec);

            if (canUseAudio && audioEnabled) {
                mMediaRecorder.setAudioEncodingBitRate(mInterface.audioEncodingBitRate(profile.audioBitRate));
                mMediaRecorder.setAudioChannels(profile.audioChannels);
                mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
                mMediaRecorder.setAudioEncoder(profile.audioCodec);
            }

            Uri uri = Uri.fromFile(getOutputMediaFile());
            mOutputUri = uri.toString();
            mMediaRecorder.setOutputFile(uri.getPath());

            if (captureInterface.maxAllowedFileSize() > 0) {
                mMediaRecorder.setMaxFileSize(captureInterface.maxAllowedFileSize());
                mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                    @Override
                    public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                            Toast.makeText(getActivity(), R.string.mcam_file_size_limit_reached, Toast.LENGTH_SHORT).show();
                            stopRecordingVideo(false);
                        }
                    }
                });
            }
            //横屏
            //前摄像头拍摄  视频对称
            //后摄像头拍摄 正常
            int orient = getActivity().getRequestedOrientation();
            int orientation=getActivity().getResources().getConfiguration().orientation;
            Log.e("lzf_orient", orient + " 当前屏幕方向 "+orientation);
            //固定屏幕方向
            if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mMediaRecorder.setOrientationHint(0);
            } else if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                //只走竖屏
                if (mInterface.getCurrentCameraPosition() == BaseCaptureActivity.CAMERA_POSITION_BACK) {
                    mMediaRecorder.setOrientationHint(90);
                } else {
                    mMediaRecorder.setOrientationHint(270);//竖屏 //前摄像头拍摄  视频对称
                }
            }
            Log.e("lzf_position", mInterface.getCurrentCameraPosition() + " 当前相机 ");
//            mMediaRecorder.setOrientationHint(90);//竖屏 //后摄像头拍摄  正常
//            mMediaRecorder.setOrientationHint(270);//竖屏 //前摄像头拍摄  视频对称
            mMediaRecorder.setPreviewDisplay(mPreviewView.getHolder().getSurface());

            try {
                mMediaRecorder.prepare();
                return true;
            } catch (Throwable e) {
                throwError(new Exception("Failed to prepare the media recorder: " + e.getMessage(), e));
                return false;
            }
        } catch (Throwable t) {
            try {
                if (mCamera!=null) {
                    mCamera.lock();
                }
            } catch (IllegalStateException e) {
                throwError(new Exception("Failed to re-lock camera: " + e.getMessage(), e));
                return false;
            }
            t.printStackTrace();
            throwError(new Exception("Failed to begin recording: " + t.getMessage(), t));
            return false;
        }
    }

    @Override
    public boolean startRecordingVideo() {
        super.startRecordingVideo();
        CaptureActivity activity= (CaptureActivity) getActivity();
        if (activity.isCanShowGuide()){
            showPopupWindow();
        }
        if (prepareMediaRecorder()) {
            try {
                // UI
                setImageRes(mButtonVideo, mInterface.iconStop());
                if (!CameraUtil.isChromium())
                    mButtonFacing.setVisibility(View.GONE);

                // Only start counter if count down wasn't already started
                if (!mInterface.hasLengthLimit()) {
                    mInterface.setRecordingStart(System.currentTimeMillis());
                    startCounter();
                }

                // Start recording
                mMediaRecorder.start();

                mButtonVideo.setEnabled(false);
                mButtonVideo.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mButtonVideo.setEnabled(true);
                    }
                }, 200);

                return true;
            } catch (Throwable t) {
                t.printStackTrace();
                mInterface.setRecordingStart(-1);
                stopRecordingVideo(false);
                throwError(new Exception("Failed to start recording: " + t.getMessage(), t));
            }
        }
        return false;
    }

    @Override
    public void stopRecordingVideo(final boolean reachedZero) {
        super.stopRecordingVideo(reachedZero);
        if (mInterface.hasLengthLimit() && mInterface.shouldAutoSubmit() &&
                (mInterface.getRecordingStart() < 0 || mMediaRecorder == null)) {
            stopCounter();
            if (mCamera != null) {
                try {
                    mCamera.lock();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            releaseRecorder();
            closeCamera();
            mReachedZero=reachedZero;
            return;
        }

        if (mCamera != null)
            mCamera.lock();
        releaseRecorder();
        closeCamera();

        if (!mInterface.didRecord())
            mOutputUri = null;

        setImageRes(mButtonVideo, mInterface.iconRecord());
        if (!CameraUtil.isChromium())
            mButtonFacing.setVisibility(View.VISIBLE);
        if (mInterface.getRecordingStart() > -1 && getActivity() != null)
        mReachedZero=reachedZero;
        stopCounter();
    }

    @Override
    public void showPreView() {
        super.showPreView();
        mInterface.onShowPreview(this,mOutputUri, mReachedZero);
    }

    private void setupFlashMode() {
        String flashMode = null;
        switch (mInterface.getFlashMode()) {
            case FLASH_MODE_AUTO:
                flashMode = Camera.Parameters.FLASH_MODE_AUTO;
                break;
            case FLASH_MODE_ALWAYS_ON:
                flashMode = Camera.Parameters.FLASH_MODE_ON;
                break;
            case FLASH_MODE_OFF:
                flashMode = Camera.Parameters.FLASH_MODE_OFF;
            default:
                break;
        }
        if (flashMode != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(flashMode);
            mCamera.setParameters(parameters);
        }
    }

    @Override
    public void onPreferencesUpdated() {
        setupFlashMode();
    }

    @Override
    public void takeStillshot() {
        Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
            public void onShutter() {
                //Log.d(TAG, "onShutter'd");
            }
        };
        Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                //Log.d(TAG, "onPictureTaken - raw. Raw is null: " + (data == null));
            }
        };
        Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(final byte[] data, Camera camera) {
                //Log.d(TAG, "onPictureTaken - jpeg, size: " + data.length);
                final File outputPic = getOutputPictureFile();
                // lets save the image to disk
                ImageUtil.saveToDiskAsync(data, outputPic, new ICallback() {
                    @Override
                    public void done(Exception e) {
                        if (e == null) {
                            Log.d("CameraFragment", "Picture saved to disk - jpeg, size: " + data.length);
                            mOutputUri = Uri.fromFile(outputPic).toString();
                            mInterface.onShowStillshot(mOutputUri);
                            //mCamera.startPreview();
                            mButtonStillshot.setEnabled(true);
                        } else {
                            throwError(e);
                        }
                    }
                });
            }
        };

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            // We could have configurable shutter sound here
//            mCamera.enableShutterSound(false);
//        }

        mButtonStillshot.setEnabled(false);
        mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    static class CompareSizesByArea implements Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.width * lhs.height -
                    (long) rhs.width * rhs.height);
        }
    }
}