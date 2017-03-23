package lzf.video.edit.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

import lzf.video.edit.R;

/**
 * Created by Administrator on 2017/3/23 0023.
 */
public class CurrentVideoBitmapAdapter extends RecyclerView.Adapter<CurrentVideoBitmapAdapter.MyViewHolder> {
    private Context context;
    private List<String> list;

    public CurrentVideoBitmapAdapter(Context context, List<String> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_current_video_bitmap,parent,false));
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.imageView.setImageURI(Uri.parse(list.get(position)));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder{
        private ImageView imageView;
        public MyViewHolder(View itemView) {
            super(itemView);
            imageView= (ImageView) itemView.findViewById(R.id.image);
        }
    }
}
