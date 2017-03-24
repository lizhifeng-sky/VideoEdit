package lzf.video.edit;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import lzf.video.edit.decoder.AudioDecoder;
import lzf.video.edit.decoder.AudioEncoder;
import lzf.video.edit.decoder.CustomAudioDecoder;
import lzf.video.edit.decoder.MultiAudioMixer;
import lzf.video.edit.entry.AudioEntry;
import lzf.video.edit.utils.MD5Util;

public class ChangeBackgroundAudioActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ChangeBgAudioActivity";
    private final static int REQUEST_CODE_ADD_MUSIC = 0x1;
    private final static int PERMISSION_DECODER_MUSIC = 8005;
    private String videoPath = null;
    private VideoView videoView;
    private ImageView play_video;
    private ImageView add_music;
    private ImageView play_music;
    private TextView complex;
    private LinearLayout music;
    private TextView music_name;
    private TextView music_time;
    private EditText start_time;
    private EditText end_time;
    private TextView change_music;
    private AudioEntry audioEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_audio);
        initView();
        initListener();
        getVideoPath();
        play_video.setVisibility(View.GONE);
        music.setVisibility(View.GONE);
        if (videoPath != null) {
            videoView.setVideoPath(videoPath);
            videoView.start();
        }
    }

    private void initListener() {
        play_video.setOnClickListener(this);
        play_music.setOnClickListener(this);
        add_music.setOnClickListener(this);
        complex.setOnClickListener(this);
        change_music.setOnClickListener(this);
    }

    private void initView() {
        videoView = (VideoView) findViewById(R.id.videoView);
        play_video = (ImageView) findViewById(R.id.play);
        add_music = (ImageView) findViewById(R.id.add_music);
        play_music = (ImageView) findViewById(R.id.play_music);
        complex = (TextView) findViewById(R.id.complex);
        music = (LinearLayout) findViewById(R.id.music);
        music_name = (TextView) findViewById(R.id.music_name);
        music_time = (TextView) findViewById(R.id.music_time);
        start_time = (EditText) findViewById(R.id.start_time);
        end_time = (EditText) findViewById(R.id.end_time);
        change_music = (TextView) findViewById(R.id.change_music);
    }

    public void getVideoPath() {
        if (getIntent() != null) {
            videoPath = getIntent().getStringExtra("video_path");
            Log.e(TAG, "视频路径" + videoPath);
        } else {
            Log.e(TAG, "获取视频路径失败");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //添加音乐
            case R.id.add_music:
                Intent intent = new Intent(this, SelectLocalAudioListActivity.class);
                startActivityForResult(intent, REQUEST_CODE_ADD_MUSIC);
                break;
            //播放视频
            case R.id.play:
                if (videoPath != null) {
                    videoView.setVideoPath(videoPath);
                    videoView.start();
                } else {
                    Log.e(TAG, "获取视频路径为空");
                }
                break;
            //播放音乐
            case R.id.play_music:
                break;
            //合成音乐和视频
            case R.id.complex:
                //音视频合并  耗时比较久  MP3先转成acc在与video合并
                videoComplexAudio();
                break;
            //剪切音频 获得的是MP3
            case R.id.change_music:
                checkAudioTime();
                break;
        }
    }

    private void videoComplexAudio() {
        Log.e("lzf_audio_acc",audioEntry.fileUrl);
        mp3ToAcc();
        Log.e("lzf_audio_acc",audioEntry.fileUrl);
        muxerVideo(videoPath,audioEntry.fileUrl);
    }
    private void mp3ToAcc(){
        AudioEncoder accEncoder = AudioEncoder.createAccEncoder(audioEntry.fileUrl);
        String finalMixPath = Environment.getExternalStorageDirectory().getPath() + "/videoEdit_MixAudioTest.acc";
        accEncoder.encodeToFile(finalMixPath);
        audioEntry.fileUrl=finalMixPath;
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void muxerVideo(String videoPath, String accPath) {
        try {
            //获取视频
            MediaExtractor videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(videoPath);
            MediaFormat videoFormat = null;
            int videoTrackIndex = -1;
            int videoTrackCount = videoExtractor.getTrackCount();
            for (int i = 0; i < videoTrackCount; i++) {
                videoFormat = videoExtractor.getTrackFormat(i);
                String type = videoFormat.getString(MediaFormat.KEY_MIME);
                if (type.startsWith("video/")) {
                    videoTrackIndex = i;
                    break;
                }
            }
            //获取音频
            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(accPath);
            MediaFormat audioFormat = null;
            int audioTrackIndex = -1;
            int audioTrackCount = audioExtractor.getTrackCount();
            for (int i = 0; i < audioTrackCount; i++) {
                audioFormat = audioExtractor.getTrackFormat(i);
                String type = audioFormat.getString(MediaFormat.KEY_MIME);
                if (type.startsWith("audio/")) {
                    audioTrackIndex = i;
                    break;
                }
            }
            //设置数据信道
            videoExtractor.selectTrack(videoTrackIndex);
            audioExtractor.selectTrack(audioTrackIndex);

            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
            //输出路径
            String videoPath_muxer=Environment.getExternalStorageDirectory() +"/"+(System.currentTimeMillis()+"output.mp4");
            MediaMuxer mediaMuxer = new MediaMuxer(videoPath_muxer, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int writeVideoTrackIndex = mediaMuxer.addTrack(videoFormat);
            int writeAudioTrackIndex = mediaMuxer.addTrack(audioFormat);
            Log.e("lzf_index", writeVideoTrackIndex + "  " + writeAudioTrackIndex);
            mediaMuxer.start();
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            long sampleTime = 0;
            {
                videoExtractor.readSampleData(byteBuffer, 0);
                if (videoExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                    videoExtractor.advance();
                }
                videoExtractor.readSampleData(byteBuffer, 0);
                long secondTime = videoExtractor.getSampleTime();
                videoExtractor.advance();
                long thirdTime = videoExtractor.getSampleTime();
                sampleTime = Math.abs(thirdTime - secondTime);
            }
            videoExtractor.unselectTrack(videoTrackIndex);
            videoExtractor.selectTrack(videoTrackIndex);

            //写入视频
            while (true) {
                int readVideoSampleSize = videoExtractor.readSampleData(byteBuffer, 0);
                if (readVideoSampleSize < 0) {
                    break;
                }
                videoBufferInfo.size = readVideoSampleSize;
                videoBufferInfo.presentationTimeUs += sampleTime;
                videoBufferInfo.offset = 0;
                videoBufferInfo.flags = videoExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(writeVideoTrackIndex, byteBuffer, videoBufferInfo);
                videoExtractor.advance();
            }
            //写入音频
            while (true) {
                int readAudioSampleSize = audioExtractor.readSampleData(byteBuffer, 0);
                if (readAudioSampleSize < 0) {
                    break;
                }
                audioBufferInfo.size = readAudioSampleSize;
                audioBufferInfo.presentationTimeUs += sampleTime;
                audioBufferInfo.offset = 0;
                audioBufferInfo.flags = videoExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(writeAudioTrackIndex, byteBuffer, audioBufferInfo);
                audioExtractor.advance();
            }


            //释放
            mediaMuxer.stop();
            mediaMuxer.release();
            videoExtractor.release();
            audioExtractor.release();
            play_video.setVisibility(View.VISIBLE);
            videoView.setVideoPath(videoPath_muxer);
            videoView.start();
            this.videoPath=videoPath_muxer;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    * 检验输入的合法性
    * */
    private void checkAudioTime() {
        if (start_time.getText() != null) {
            if (!(Integer.parseInt(start_time.getText().toString()) < 0
                    || Integer.parseInt(start_time.getText().toString()) > audioEntry.duration/1000)) {
                if (end_time.getText() != null) {
                    if (!(Integer.parseInt(end_time.getText().toString()) <= Integer.parseInt(start_time.getText().toString())
                            || Integer.parseInt(end_time.getText().toString()) >= audioEntry.duration/1000)) {
                        //检验权限
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            if (audioEntry != null) {
                                decodeMusic(audioEntry.fileUrl, Integer.parseInt(start_time.getText().toString()), Integer.parseInt(end_time.getText().toString()));
                            }
                        } else {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.READ_EXTERNAL_STORAGE},
                                    PERMISSION_DECODER_MUSIC);
                        }
                    } else {
                        Toast.makeText(this, "错误的结束时间", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "请输入结束时间", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "错误的开始时间", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "请输入开始时间", Toast.LENGTH_SHORT).show();
        }
    }
    /*
    * 分割音频
    * */
    private void decodeMusic(String audioPath, int fromTime, int endTime) {
        String decode_file = null;
        try {
            decode_file = CustomAudioDecoder.fenLiData(audioPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Integer> list = null;
        if (decode_file != null) {
            list = CustomAudioDecoder.initMP3Frame(decode_file);
        }
        if (list != null) {
            try {
                final String path = CustomAudioDecoder.CutingMp3(decode_file, "分离",
                        list,
                        fromTime, endTime);
                Log.e("lzf_music", "分离路径" + path);
                AudioEntry mAudioEntry = new AudioEntry();
                mAudioEntry.fileName="分离";
                mAudioEntry.fileUrl=path;
                mAudioEntry.duration=endTime-fromTime;
                mAudioEntry.album=audioEntry.album;
                mAudioEntry.mime=audioEntry.mime;
                music_name.setText(mAudioEntry.fileName);
                music_time.setText("音频时长"+mAudioEntry.duration+"秒");
                change_music.setText("转码中 请稍后");
                new DecodeAudioTask(mAudioEntry).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            final File file = new File(decode_file);
            file.delete();
        } else {
            Log.e("lzf_music", "分离失败");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_MUSIC && resultCode == RESULT_OK) {
            //设置音频信息  显示音乐
            audioEntry = (AudioEntry) data.getSerializableExtra("audio");
            music.setVisibility(View.VISIBLE);
            music_name.setText(audioEntry.fileName);
            try {
                music_time.setText(audioEntry.duration/1000+"秒");
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.e("lzf——audio mime", audioEntry.mime);
            Log.e("lzf——audio", audioEntry.toString());
        } else {
            Toast.makeText(this, "返回失败", Toast.LENGTH_SHORT).show();
        }
    }
    class DecodeAudioTask extends AsyncTask<Void, Double, Boolean> {

        AudioEntry decAudio;

        DecodeAudioTask(AudioEntry decAudio) {
            this.decAudio = decAudio;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            String decodeFilePath = Environment.getExternalStorageDirectory().getPath() + "/"
                    + MD5Util.getMD5Str(decAudio.fileUrl);
            File decodeFile = new File(decodeFilePath);
            if (decodeFile.exists()) {
                publishProgress(1.0);
                decAudio.fileUrl = decodeFilePath;
                return true;
            }

            if (decAudio.mime.contains("x-ms-wma")) {
                FileInputStream fisWavFile = null;
                FileOutputStream fosRawAudioFile = null;
                try {
                    File srcAudioFile = new File(decAudio.fileUrl);
                    long audioFileSize = srcAudioFile.length();
                    fisWavFile = new FileInputStream(srcAudioFile);
                    fosRawAudioFile = new FileOutputStream(decodeFile);
                    fisWavFile.read(new byte[44]);
                    byte[] rawBuf = new byte[1024];
                    int readCount;
                    double totalReadCount = 44;
                    while ((readCount = fisWavFile.read(rawBuf)) != -1) {
                        fosRawAudioFile.write(rawBuf, 0, readCount);
                        totalReadCount += readCount;
                        publishProgress(totalReadCount / audioFileSize);
                    }
                    publishProgress(1.0);
                    return true;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (fisWavFile != null)
                            fisWavFile.close();

                        if (fosRawAudioFile != null)
                            fosRawAudioFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            } else {
                AudioDecoder audioDec = AudioDecoder.createDefualtDecoder(decAudio.fileUrl);
                try {
                    decAudio.fileUrl = decodeFilePath;
                    audioDec.setOnAudioDecoderListener(new AudioDecoder.OnAudioDecoderListener() {
                        @Override
                        public void onDecode(byte[] decodedBytes,
                                             double progress) throws IOException {
                            publishProgress(progress);
                        }
                    });
                    audioDec.decodeToFile(decodeFilePath);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        @Override
        protected void onProgressUpdate(Double... values) {
            super.onProgressUpdate(values);
            Log.e("lzf_进度", values + " ");
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            change_music.setText("转码完成");
            Log.e("lzf_进度", "转码完成");
            if (result) {
                addMusicTrack(decAudio);
            }
        }

        private void addMusicTrack(final AudioEntry decAudio) {
            audioEntry=decAudio;
        }
    }
}
