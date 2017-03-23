package com.afollestad.materialcamera.internal;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.afollestad.materialcamera.R;


/**
 * Created by lzf on 2016/9/2.
 * http://ocgig6vo9.bkt.clouddn.com/Wildlife.wmv
 */
public class PlayActivity extends Activity {
    private VideoView videoView;//视频
    protected String videoUrl="";
    private RelativeLayout controller,relativeLayout;//控制面板  动画载体
    private SeekBar seekBar;//控制面板 进度条
//    private boolean flag=true;
    private TextView current,total;//当前时间 总时间
    private long lastPosition=0;//
//    private AnimationDrawable animationDrawable;//动画
    private Thread thread;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.qiniu_activity_play);
        this.initVideoPlay();
    }

    private void initVideoPlay() {
        if (getIntent() != null && getIntent().getStringExtra("videoUrl") != null && !getIntent().getStringExtra("videoUrl").equals("")) {
            videoUrl = getIntent().getStringExtra("videoUrl");
        }
        videoView = (VideoView) findViewById(R.id.videoView);
//        //动画
//        ImageView imageView = (ImageView) findViewById(R.id.image);
        //返回
        controller= (RelativeLayout) findViewById(R.id.controller);
        seekBar= (SeekBar) findViewById(R.id.seekBar);
        current= (TextView) findViewById(R.id.current);
        total= (TextView) findViewById(R.id.total);
//        animationDrawable= (AnimationDrawable) imageView.getDrawable();
        if (videoUrl!=null&&!videoUrl.equals("")) {
            videoView.setVideoURI(Uri.parse(videoUrl));
            videoView.start();
//            if (animationDrawable!=null&&!animationDrawable.isRunning()) {
//                animationDrawable.start();
//            }
        }else {
            Toast.makeText(PlayActivity.this,"视频地址错误", Toast.LENGTH_SHORT).show();
        }
        thread=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true){
                        if (videoView!=null&&seekBar!=null) {
                            if (videoView.isPlaying()) {
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if (videoView.getCurrentPosition() != 0 && videoView.getCurrentPosition() != lastPosition) {
                                    Log.e("lzf_thread", "CurrentPosition  " + videoView.getCurrentPosition() + "");
                                    seekBar.setProgress(videoView.getCurrentPosition());
                                    lastPosition = videoView.getCurrentPosition();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.e("lzf_onPrepared","Duration  "+videoView.getDuration()+"");
//                //准备完毕隐藏动画
//                relativeLayout.setVisibility(View.GONE);
//                if (animationDrawable != null && animationDrawable.isRunning()) {
//                    animationDrawable.stop();
//                }
                Log.e("lzf_onPrepared",videoView.getWidth()+" "+videoView.getHeight()+"  "+videoView.getRotation());
                //设置seekBar和总时间
                if (videoView.getDuration()!=0){
                    seekBar.setMax(videoView.getDuration());
                    total.setText(setTime(videoView.getDuration()/1000));
                }
                thread.start();
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
//                flag=false;
                Log.e("lzf_Completion","播放完成");
                seekBar.setProgress(videoView.getDuration());
                Log.e("lzf_Completion",videoView.getWidth()+" "+videoView.getHeight()+"  "+videoView.getRotation());
            }
        });
        //根据seekBar的进度 更新当前时间
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (current!=null) {
                    current.setText(setTime(progress/1000));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                videoView.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (videoView!=null) {
                    videoView.seekTo(seekBar.getProgress());
                    videoView.start();
                }
            }
        });
    }
    public void restart(View view) {
        //播放过 才可以重新开始
        Log.e("lzf_video","重新开始");
        if (videoView!=null&&videoView.getCurrentPosition()>0){
            videoView.seekTo(0);
//            flag=true;
            videoView.start();
        }
    }

    public void start(View view) {
        Log.e("lzf_video","开始");
        //只有不是播放状态才可以start
        if (videoView!=null&&!videoView.isPlaying()){
            videoView.start();
        }
    }

    public void pause(View view) throws InterruptedException {
        //正在播放 暂停
        Log.e("lzf_video","暂停");
        if (videoView!=null&&videoView.isPlaying()){
            videoView.pause();
        }
    }
    public String setTime(long time){
        if (time<60){
            if (time<10){
                return "00:00:0"+time;
            }else {
                return "00:00:"+time;
            }
        }else if (time<3600){
            if (time<600){
                if (time%60<10){
                    return "00:0"+time/60+":0"+time%60;
                }else {
                    return "00:0"+time/60+":"+time%60;
                }
            }else {
                if (time%60<10){
                    return "00:"+time/60+":0"+time%60;
                }else {
                    return "00:"+time/60+":"+time%60;
                }
            }
        }else {
            //小于10小时
            if (time<36000){
                //0+time/3600:  时
                if (time%3600<600){
                    if (time%60<10){
                        return "0"+time/3600+":0"+time/60+":0"+time%60;
                    }else {
                        return "0"+time/3600+":0"+time/60+":"+time%60;
                    }
                }else {
                    if (time%60<10){
                        return "0"+time/3600+":"+time/60+":0"+time%60;
                    }else {
                        return "0"+time/3600+":"+time/60+":"+time%60;
                    }
                }
            }else {
                if (time%3600<600){
                    if (time%60<10){
                        return time/3600+":0"+time/60+":0"+time%60;
                    }else {
                        return time/3600+":0"+time/60+":"+time%60;
                    }
                }else {
                    if (time%60<10){
                        return time/3600+":"+time/60+":0"+time%60;
                    }else {
                        return time/3600+":"+time/60+":"+time%60;
                    }
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode== KeyEvent.KEYCODE_BACK){
            if (thread!=null&&thread.isAlive()){
                thread.interrupt();
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
