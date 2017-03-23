package lzf.video.edit.task;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import lzf.video.edit.entry.VideoEntry;
import lzf.video.edit.inter.OnScanLocalVideoListener;

/**
 * Created by Administrator on 2017/3/23 0023.
 */
public class ScanLocalVideoAsyncTask extends AsyncTask<Void, Integer, List<VideoEntry>> {
    private List<VideoEntry> videoInfo = new ArrayList<>();
    private OnScanLocalVideoListener onScanLocalVideoListener;

    public void setOnScanLocalVideoListener(OnScanLocalVideoListener onScanLocalVideoListener) {
        this.onScanLocalVideoListener = onScanLocalVideoListener;
    }

    @Override
    protected List<VideoEntry> doInBackground(Void... params) {
        if (onScanLocalVideoListener!=null) {
            onScanLocalVideoListener.onStarted();
        }
        videoInfo = getVideoFile(videoInfo, Environment.getExternalStorageDirectory());
//            videoInfos=filterVideo(videoInfos);//过滤小文件
        Log.i("tga", "最后的大小" + videoInfo.size());
        return videoInfo;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(List<VideoEntry> videoInfo) {
        super.onPostExecute(videoInfo);
        if (onScanLocalVideoListener!=null) {
            onScanLocalVideoListener.onFinish(videoInfo);
        }
//        list_video.addAll(videoInfo);
//        adapter.notifyDataSetChanged();
    }

    /**
     * 获取视频文件
     *
     * @param list
     * @param file
     * @return
     */
    private List<VideoEntry> getVideoFile(final List<VideoEntry> list, File file) {
        file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String name = file.getName();
                int i = name.indexOf('.');
                if (i != -1) {
                    name = name.substring(i);
                    if (name.equalsIgnoreCase(".mp4")) {
                        VideoEntry video = new VideoEntry();
                        file.getUsableSpace();
                        video.setDisplayName(file.getName());
                        video.setPath(file.getAbsolutePath());
                        Log.i("local_map4", "name" + video.getPath());
                        list.add(video);
                        return true;
                    }
                    //判断是不是目录
                } else if (file.isDirectory()) {
                    getVideoFile(list, file);
                }
                return false;
            }
        });
        return list;
    }
}
