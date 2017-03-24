package lzf.video.edit;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.afollestad.materialcamera.MaterialCamera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lzf.video.edit.adapter.CurrentVideoBitmapAdapter;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        MediaPlayer.OnCompletionListener,
        Runnable,
        MediaPlayer.OnPreparedListener {
    private VideoView videoView;
    private ImageView play;
    private ImageView select;
    private String videoPath = null;
    private static final String TAG = "MainActivity";
    private static final int CAMERA_RQ = 8001;
    private static final int PERMISSION_CAMERA_RQ = 8002;
    private static final int LOCAL_RQ = 8003;
    private static final int PERMISSION_LOCAL_RQ = 8004;
    private Thread getVideoBitmap;
    private List<String> videoBitmapPath;
    private RecyclerView recycler_bitmap;
    private CurrentVideoBitmapAdapter adapter;
    private static final String VIDEO_FROM_LOCAL = "local";
    private static final String VIDEO_FROM_RECORD = "record";
    private String videoFrom = null;//拍摄 本地
    private EditText start_time;
    private EditText end_time;
    private LinearLayout bottom;
    private LinearLayout finish_decode;
    private TextView cancel;
    private TextView done;
    private TextView start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
        videoBitmapPath = new ArrayList<>();
        adapter = new CurrentVideoBitmapAdapter(this, videoBitmapPath);
        recycler_bitmap.addItemDecoration(new MyItemDecoration());
        recycler_bitmap.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recycler_bitmap.setAdapter(adapter);
    }

    private void initListener() {
        play.setOnClickListener(this);
        select.setOnClickListener(this);
        videoView.setOnCompletionListener(this);
        videoView.setOnPreparedListener(this);

        start.setOnClickListener(this);
        cancel.setOnClickListener(this);
        done.setOnClickListener(this);
    }

    private void initView() {
        videoView = (VideoView) findViewById(R.id.videoView);
        play = (ImageView) findViewById(R.id.play);
        select = (ImageView) findViewById(R.id.selectVideo);
        recycler_bitmap = (RecyclerView) findViewById(R.id.recycler_bitmap);
        start_time = (EditText) findViewById(R.id.start_time);
        end_time = (EditText) findViewById(R.id.end_time);
        bottom = (LinearLayout) findViewById(R.id.bottom);
        cancel = (TextView) findViewById(R.id.cancel);
        done = (TextView) findViewById(R.id.done);
        finish_decode = (LinearLayout) findViewById(R.id.finish);
        start = (TextView) findViewById(R.id.start);

        bottom.setVisibility(View.GONE);
        finish_decode.setVisibility(View.GONE);

        play.setVisibility(View.GONE);
        videoView.setMediaController(null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //播放视频
            case R.id.play:
                if (videoView != null && videoPath != null) {
                    videoView.setVideoPath(videoPath);
                    videoView.start();
                } else {
                    Toast.makeText(MainActivity.this, "打开视频失败", Toast.LENGTH_SHORT).show();
                }
                break;
            //选择视频
            case R.id.selectVideo:
                selectVideo();
                break;
            //开始剪切视频
            case R.id.start:
                startDecodeVideo();
                break;
            //取消
            case R.id.cancel:
                finish_decode.setVisibility(View.GONE);
                videoView.setVideoPath(videoPath);
                videoView.start();
                break;
            //完成
            case R.id.done:
                Intent intent=new Intent(MainActivity.this,AddAudioActivity.class);
                //Environment.getExternalStorageDirectory().getPath() + "/lzf_decoder_video.mp4"
                intent.putExtra("video_path",Environment.getExternalStorageDirectory().getPath() + "/lzf_decoder_video.mp4");
                startActivity(intent);
                break;
        }
    }

    private void startDecodeVideo() {
        //起始时间  截取时长
        if (start_time.getText() != null) {
            if (Integer.parseInt(start_time.getText().toString()) * 1000 > videoView.getDuration()) {
                Toast.makeText(MainActivity.this, "开始时间错误", Toast.LENGTH_SHORT).show();
            } else {
                if (end_time.getText() != null) {
                    if (Integer.parseInt(start_time.getText().toString()) * 1000 + Integer.parseInt(end_time.getText().toString()) * 1000 > videoView.getDuration()) {
                        Toast.makeText(MainActivity.this, "错误的截取时长", Toast.LENGTH_SHORT).show();
                    } else {
                        boolean finish = new VideoDecoder().decodeVideo(videoPath, Integer.parseInt(start_time.getText().toString()) * 1000 * 1000, Integer.parseInt(end_time.getText().toString()) * 1000 * 1000);
                        if (finish) {
                            videoView.setVideoPath(Environment.getExternalStorageDirectory().getPath() + "/lzf_decoder_video.mp4");
                            videoView.start();
                            finish_decode.setVisibility(View.VISIBLE);
                        } else {
                            Log.e("lzf_decoder_video", "视频剪切失败");
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "请输入截取时长", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(MainActivity.this, "请输入开始时间", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectVideo() {
        //本地视频或拍摄
        final AlertDialog selectVideoDialog = new AlertDialog.Builder(this).create();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_select_video_type, null);
        TextView local = (TextView) view.findViewById(R.id.local);
        TextView record = (TextView) view.findViewById(R.id.record);
        local.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectVideoDialog != null) {
                    selectVideoDialog.dismiss();
                }
                Intent intent = new Intent(MainActivity.this, SelectLocalVideoActivity.class);
                MainActivity.this.startActivityForResult(intent, LOCAL_RQ);
            }
        });
        record.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View v) {
                //关闭dialog
                if (selectVideoDialog != null) {
                    selectVideoDialog.dismiss();
                }
                //检验权限
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    File saveDir = new File(Environment.getExternalStorageDirectory(), "lzf");
                    saveDir.mkdirs();
                    startRecord(saveDir);
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO
                    }, PERMISSION_CAMERA_RQ);
                }
            }
        });
        selectVideoDialog.setView(view);
        selectVideoDialog.show();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        getVideoBitmap = new Thread(MainActivity.this);
        getVideoBitmap.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        play.setVisibility(View.VISIBLE);
        getVideoBitmap.interrupt();
    }

    public void startRecord(File file) {
        MaterialCamera materialCamera = new MaterialCamera(MainActivity.this)
                .saveDir(file)
                .showPortraitWarning(true)
                .allowRetry(true)
                .defaultToFrontFacing(true)
                .allowRetry(true)
                .autoSubmit(false)
                .labelConfirm(R.string.mcam_use_video);
        materialCamera.start(CAMERA_RQ, true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CAMERA_RQ:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED
                        && grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                    //用户同意授权
                    File saveDir = new File(Environment.getExternalStorageDirectory(), "lzf");
                    saveDir.mkdirs();
                    startRecord(saveDir);
                } else {
                    //用户拒绝授权
                    Toast.makeText(MainActivity.this, "获取权限失败", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA_RQ:
                    //拍摄
                    String filePath = data.getStringExtra("videoUrl");
                    if (filePath != null) {
                        videoPath = filePath;
                        Log.e("lzf_activity_result", "视频路径" + videoPath);
//                        requestVideoWH(480,640);
                        videoView.setVideoURI(Uri.parse(filePath));
                        videoView.start();
                        videoFrom = VIDEO_FROM_RECORD;
                    } else {
                        Log.e("lzf_activity_result", "返回失败");
                    }
                    break;
                case LOCAL_RQ:
                    //本地
                    String filePath_local = data.getStringExtra("videoUrl");
                    if (filePath_local != null) {
                        videoPath = filePath_local;
                        Log.e("lzf_activity_result", "视频路径" + filePath_local);
//                        requestVideoWH(480,640);
                        videoView.setVideoURI(Uri.parse(filePath_local));
                        videoView.start();
                        videoFrom = VIDEO_FROM_LOCAL;
                    } else {
                        Log.e("lzf_activity_result", "选择本地视频返回失败");
                    }
                    break;
            }
        }
    }

    @Override
    public void run() {
        //获取视频帧图
        ArrayList<String> list = new ArrayList<>();
//        String [] array=new String[]{};

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        Log.i(TAG, Environment.getExternalStorageDirectory().getPath() + "/video.mp4" + "     " + videoPath);
        Log.i(TAG, Environment.getExternalStorageDirectory() + "/video.mp4" + "     " + videoPath.substring(7, videoPath.length()));
//        metadataRetriever.setDataSource(videoPath);
        if (videoFrom.equals(VIDEO_FROM_RECORD)) {
            metadataRetriever.setDataSource(videoPath.substring(7, videoPath.length()));
        } else if (videoFrom.equals(VIDEO_FROM_LOCAL)) {
            metadataRetriever.setDataSource(videoPath);
        }
        Log.e("lzf_duration", videoView.getDuration() + "   ");
        for (int i = 500 * 1000; i <= videoView.getDuration() * 1000; i += 1000 * 1000) {
            Bitmap bitmap = metadataRetriever.
                    getFrameAtTime(videoView.getCurrentPosition() * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
            Log.i(TAG, "bitmap---i: " + i / 1000);
            Log.e(TAG, videoView.getCurrentPosition() + "  ");
            String path = Environment.getExternalStorageDirectory() + "/bitmap_" + i + ".png";
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(path);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
                Log.i(TAG, "i: " + i / 1000);
                list.add(path);
//                array[i]=path;
            } catch (Exception e) {
                Log.i(TAG, "Error: " + i / 1000 + "   " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            bitmap.recycle();
        }
        Message msg = new Message();
        msg.what = 0;
        handler.obtainMessage();
        //将msg发送到目标对象，所谓的目标对象，就是生成该msg对象的handler对象
        Bundle b = new Bundle();
        b.putStringArrayList("array", list);
        msg.setData(b);
//        msg.sendToTarget();
        handler.sendMessage(msg);

        metadataRetriever.release();
    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO 接收消息并且去更新UI线程上的控件内容
            if (msg.what == 0) {
                Bundle b = msg.getData();
                Log.e("lzf_video_bitmap_path", videoBitmapPath.size() + "   ");
                if (b.getStringArrayList("array") != null) {
                    ArrayList<String> list = b.getStringArrayList("array");
                    if (list != null) {
                        videoBitmapPath.addAll(list);
                    }
                }
                Log.e("lzf_video_bitmap_path", videoBitmapPath.size() + "   ");
                adapter.notifyDataSetChanged();
                bottom.setVisibility(View.VISIBLE);
            }
            super.handleMessage(msg);
        }
    };


}
