package lzf.video.edit.inter;

import java.util.List;

import lzf.video.edit.entry.VideoEntry;

/**
 * Created by Administrator on 2017/3/23 0023.
 */
public interface OnScanLocalVideoListener {
    void onStarted();
    void onFinish(List<VideoEntry> videoInfo);
}
