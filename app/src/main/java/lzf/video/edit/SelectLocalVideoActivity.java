package lzf.video.edit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import java.util.ArrayList;
import java.util.List;

import lzf.video.edit.adapter.LocalVideoInfoAdapter;
import lzf.video.edit.entry.VideoEntry;
import lzf.video.edit.inter.OnLocalVideoSelectListener;
import lzf.video.edit.inter.OnScanLocalVideoListener;
import lzf.video.edit.task.ScanLocalVideoAsyncTask;

public class SelectLocalVideoActivity extends AppCompatActivity implements
        OnLocalVideoSelectListener,
        View.OnClickListener,
        OnScanLocalVideoListener{
    private LocalVideoInfoAdapter adapter;
    private List<VideoEntry> list_video;
    private VideoView videoView;
    private String localVideoPath =null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp4_local_list);
        videoView= (VideoView) findViewById(R.id.video_local);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_select_local_video);
        TextView confirm = (TextView) findViewById(R.id.confirm);
        confirm.setOnClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        list_video = new ArrayList<>();
        adapter = new LocalVideoInfoAdapter(list_video, this);
        adapter.setOnSelectListener(this);
        recyclerView.setAdapter(adapter);

        //遍历所有文件 太慢了。。。。。。。。。
        ScanLocalVideoAsyncTask asyncTask=new ScanLocalVideoAsyncTask();
        asyncTask.setOnScanLocalVideoListener(this);
        asyncTask.execute();
    }

    @Override
    public void onSelect(String localVideoPath) {
        this.localVideoPath =localVideoPath;
        Log.e("lzf_video_onSelect",localVideoPath);
        RelativeLayout.LayoutParams layoutParams=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        videoView.setLayoutParams(layoutParams);
        videoView.setVideoPath(localVideoPath);
        videoView.start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.confirm:
                if (localVideoPath !=null){
                    Intent intent = new Intent();
                    intent.putExtra("videoUrl", localVideoPath);
                    setResult(Activity.RESULT_OK, intent);
                    this.finish();
                }else {
                    Toast.makeText(this,"请选择视频",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onStarted() {
        Log.e("lzf_video_onSelect","开始扫描本地");
    }

    @Override
    public void onFinish(List<VideoEntry> videoInfo) {
        Log.e("lzf_video_onSelect","扫描本地完成");
        list_video.addAll(videoInfo);
        adapter.notifyDataSetChanged();
    }

}
