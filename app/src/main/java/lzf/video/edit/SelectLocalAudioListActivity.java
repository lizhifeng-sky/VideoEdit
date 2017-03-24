package lzf.video.edit;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import lzf.video.edit.adapter.AudioInfoAdapter;
import lzf.video.edit.entry.AudioEntry;
import lzf.video.edit.utils.MediaUtils;

public class SelectLocalAudioListActivity extends AppCompatActivity {
    private static final String[] SUPPORT_DECODE_AUDIO_FORMAT = {"audio/mpeg", "audio/x-ms-wma", "audio/mp4a-latm", "audio/x-wav"};
    private RecyclerView recycler;
    private AudioInfoAdapter adapter;
    private List<AudioEntry> list;
    private static final int PAGE_SIZE = 20;
    private int mPage = 1;
    private boolean mHasMore = true;
    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp3_local_list);
        recycler = (RecyclerView) findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        list = new ArrayList<>();
        adapter = new AudioInfoAdapter(this, list);
        recycler.setAdapter(adapter);
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if(newState == RecyclerView.SCROLL_STATE_SETTLING){
                    //加载下页
                    if(mHasMore){
                        mPage++;
                        new GetAudiosTask(SelectLocalAudioListActivity.this).execute();
                    }else {
                        Toast.makeText(SelectLocalAudioListActivity.this,"没有更多",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        new GetAudiosTask(this).execute();
    }

    class GetAudiosTask extends AsyncTask<Void, Void, List<AudioEntry>> {

        Context context;

        GetAudiosTask(Context context) {
            this.context = context;
        }

        @Override
        protected List<AudioEntry> doInBackground(Void... params) {
            return getLocalAudio(context, mPage, PAGE_SIZE);
        }

        @Override
        protected void onPostExecute(List<AudioEntry> result) {
            super.onPostExecute(result);
            Log.e("lzf_audio",result.toString());
            if (result == null || result.isEmpty()) {
                mHasMore = false;
            } else {
                list.addAll(result);
                adapter.notifyDataSetChanged();
            }
        }

        /**
         * 获取sd卡所有的音频文件
         *
         * @param context
         * @param page    从1开始
         * @param context
         * @return
         * @throws Exception
         */
        private ArrayList<AudioEntry> getLocalAudio(Context context, int page, int pageSize) {

            ArrayList<AudioEntry> audios = null;

            StringBuilder selectionBuilder = new StringBuilder();
            int size = SUPPORT_DECODE_AUDIO_FORMAT.length;
            for (int i = 0; i != size; ++i) {
                selectionBuilder.append("mime_type=? or ");
            }
            int sbLen = selectionBuilder.length();
            selectionBuilder.delete(sbLen - 3, sbLen);

            final String selection = selectionBuilder.toString();
            final String orderBy = String.format("%s LIMIT %s , %s ", MediaStore.Audio.Media.DEFAULT_SORT_ORDER, (page - 1) * pageSize, pageSize);

            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.DISPLAY_NAME,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.DURATION,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.YEAR,
                            MediaStore.Audio.Media.MIME_TYPE,
                            MediaStore.Audio.Media.SIZE,
                            MediaStore.Audio.Media.DATA},
                    selection,
                    SUPPORT_DECODE_AUDIO_FORMAT, orderBy);
            audios = new ArrayList<>();
            if (cursor != null && cursor.moveToFirst()) {
                AudioEntry audioEntry = null;
                String fileUrl = null;
                boolean isMatchAudioFormat = false;
                do {
                    fileUrl = cursor.getString(9);
                    isMatchAudioFormat = MediaUtils.isMatchAudioFormat(fileUrl, 44100, 2);
                    if (!isMatchAudioFormat) {
                        continue;
                    }

                    audioEntry = new AudioEntry();
                    audioEntry.id = cursor.getLong(0);
                    // 文件名
                    audioEntry.fileName = cursor.getString(1);
                    // 歌曲名
                    audioEntry.title = cursor.getString(2);
                    // 时长
                    audioEntry.duration = cursor.getInt(3);
                    // 歌手名
                    audioEntry.artist = cursor.getString(4);
                    // 专辑名
                    audioEntry.album = cursor.getString(5);
                    // 年代
                    audioEntry.year = cursor.getString(6);
                    // 歌曲格式
                    audioEntry.mime = cursor.getString(7).trim();
                    // 文件大小
                    audioEntry.size = cursor.getString(8);
                    // 文件路径
                    audioEntry.fileUrl = fileUrl;
                    audios.add(audioEntry);
                } while (cursor.moveToNext());
                cursor.close();
            }
            return audios;
        }
    }
}
