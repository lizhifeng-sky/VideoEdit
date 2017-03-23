package lzf.video.edit.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import lzf.video.edit.R;
import lzf.video.edit.entry.VideoEntry;
import lzf.video.edit.inter.OnLocalVideoSelectListener;

/**
 * Created by Administrator on 2017/3/23 0023.
 */
public class LocalVideoInfoAdapter extends RecyclerView.Adapter<LocalVideoInfoAdapter.MyViewHolder> {
    private List<VideoEntry> list;
    private Context context;
    private OnLocalVideoSelectListener onSelectListener;

    public LocalVideoInfoAdapter(List<VideoEntry> list, Context context) {
        this.list = list;
        this.context = context;
    }

    public void setOnSelectListener(OnLocalVideoSelectListener onSelectListener) {
        this.onSelectListener = onSelectListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_mp4,parent,false));
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.video_name.setText(list.get(position).getTitle());
        holder.video_path.setText(list.get(position).getPath());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView video_photo;
        TextView video_name;
        TextView video_path;
        RelativeLayout parent;
        public MyViewHolder(View itemView) {
            super(itemView);
            video_photo= (ImageView) itemView.findViewById(R.id.video_photo);
            video_name= (TextView) itemView.findViewById(R.id.video_name);
            video_path= (TextView) itemView.findViewById(R.id.video_path);
            parent= (RelativeLayout) itemView.findViewById(R.id.parent);
            parent.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId()==R.id.parent){
                onSelectListener.onSelect(list.get(getAdapterPosition()).getPath());
            }
        }
    }
}
