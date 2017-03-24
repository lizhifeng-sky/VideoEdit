package lzf.video.edit.adapter;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import lzf.video.edit.R;
import lzf.video.edit.entry.AudioEntry;


/**
 * Created by Administrator on 2017/3/22 0022.
 */
public class AudioInfoAdapter extends RecyclerView.Adapter<AudioInfoAdapter.AudioViewHolder> {
    private Activity context;
    private List<AudioEntry> list;

    public AudioInfoAdapter(Activity context, List<AudioEntry> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public AudioViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AudioViewHolder(LayoutInflater.from(context).inflate(R.layout.item_mp3,parent,false));
    }

    @Override
    public void onBindViewHolder(AudioViewHolder holder, int position) {
        AudioEntry audioEntry = list.get(position);
        holder.tvArtist.setText(audioEntry.artist);
        holder.tvFileName.setText(audioEntry.fileName);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 :list.size();
    }

    class AudioViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView tvFileName;
        TextView tvArtist;
        ImageView ivPlay;
        RelativeLayout parent;
        public AudioViewHolder(View itemView) {
            super(itemView);
            tvFileName = (TextView) itemView.findViewById(R.id.tv_file_name);
            tvArtist = (TextView) itemView.findViewById(R.id.tv_artist);
            ivPlay = (ImageView) itemView.findViewById(R.id.iv_play);
            parent= (RelativeLayout) itemView.findViewById(R.id.parent);
            parent.setOnClickListener(this);
            ivPlay.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.parent:
                    Intent okData = new Intent();
                    okData.putExtra("audio", list.get(getAdapterPosition()));
                    context.setResult(Activity.RESULT_OK, okData);
                    context.finish();
                    break;
                case R.id.iv_play:
                    break;
            }
        }
    }
}
