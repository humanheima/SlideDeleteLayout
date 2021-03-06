package com.hm.slidedeletelayoutdemo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private RecyclerView recyclerView;
    private List<String> stringList;
    private RvAdapter adapter;
    private LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recycler_view);
        stringList = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            stringList.add("string" + i);
        }
        adapter = new RvAdapter(this, stringList);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(MainActivity.this, "position=" + position, Toast.LENGTH_SHORT).show();
            }
        });
        adapter.setOnItemDeleteListener(new OnItemDeleteListener() {
            @Override
            public void onItemDelete(int position) {
                stringList.remove(position);
                adapter.notifyItemRemoved(position);
            }
        });
        adapter.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public void onLongClick(View view, int position) {
                Toast.makeText(MainActivity.this, "onLongClick position=" + position, Toast.LENGTH_SHORT).show();
            }
        });
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    class RvAdapter extends RecyclerView.Adapter<RvAdapter.VH> {

        private Context context;
        private List<String> stringList;
        private OnItemClickListener onItemClickListener;
        private OnItemDeleteListener onItemDeleteListener;
        private OnItemLongClickListener onItemLongClickListener;

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
        }

        public void setOnItemDeleteListener(OnItemDeleteListener onItemDeleteListener) {
            this.onItemDeleteListener = onItemDeleteListener;
        }

        public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
            this.onItemLongClickListener = onItemLongClickListener;
        }

        public RvAdapter(Context context, List<String> stringList) {
            this.context = context;
            this.stringList = stringList;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_slide_delete, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(final VH holder, final int position) {
            if (onItemClickListener != null) {
                holder.itemRlContent.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.e(TAG, "onClick: position=" + position);
                        onItemClickListener.onItemClick(v, holder.getAdapterPosition());
                    }
                });
            }
            if (onItemLongClickListener != null) {
                holder.itemRlContent.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Log.e(TAG, "onLongClick: position=" + position);
                        onItemLongClickListener.onLongClick(v, position);
                        return true;
                    }
                });
            }
            if (onItemDeleteListener != null) {
                holder.itemTextRemove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onItemDeleteListener.onItemDelete(holder.getAdapterPosition());
                    }
                });
            }
            holder.itemImageBook.setImageResource(R.mipmap.ic_launcher);
            holder.itemTextBookIsbn.setText(stringList.get(position));
        }

        @Override
        public int getItemCount() {
            return stringList.size();
        }

        class VH extends RecyclerView.ViewHolder {

            private ImageView itemImageBook;
            private TextView itemTextBookName;
            private RelativeLayout itemRlContent;
            private TextView itemTextBookIsbn;
            private TextView itemTextRemove;

            public VH(View itemView) {
                super(itemView);
                itemImageBook = itemView.findViewById(R.id.item_image_book);
                itemTextBookName = itemView.findViewById(R.id.item_text_book_name);
                itemRlContent = itemView.findViewById(R.id.item_rl_content);
                itemTextBookIsbn = itemView.findViewById(R.id.item_text_book_isbn);
                itemTextRemove = itemView.findViewById(R.id.item_text_remove);
            }
        }
    }

    interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    interface OnItemLongClickListener {
        void onLongClick(View view, int position);
    }

    interface OnItemDeleteListener {
        void onItemDelete(int position);
    }
}
