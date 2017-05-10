package com.example.jellybean.refreshloadlayout;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.refreshloadlayout.RefreshLoadLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jellybean on 2017/4/26.
 */

public class RecyclerViewActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    RefreshLoadLayout refreshLoadLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);
        recyclerView= (RecyclerView) findViewById(R.id.recyclerView);
        refreshLoadLayout= (RefreshLoadLayout) findViewById(R.id.refreshLayout);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final List<String> strings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            strings.add(Integer.toString(i));
        }
        final RecyclerView.Adapter adapter=new RecyclerAdapter(strings);
        recyclerView.setAdapter(adapter);
        refreshLoadLayout.setRefreshingEnabled(true);
        refreshLoadLayout.setLoadingEnabled(true);
        refreshLoadLayout.setOnRefreshListener(new RefreshLoadLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLoadLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLoadLayout.endRefreshing();
                    }
                },3000);
            }
        });
        refreshLoadLayout.setLoadingHandler(new RefreshLoadLayout.LoadingHandler() {
            @Override
            public boolean canLoadMore() {
                return strings.size()<25;
            }

            @Override
            public void onLoading() {
                refreshLoadLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        int positionStart=strings.size();
                        strings.add("new");
                        strings.add("new");
                        adapter.notifyItemRangeInserted(positionStart,2);
                        refreshLoadLayout.endLoading();
                    }
                },2000);

            }
        });
    }

    private static class Holder extends RecyclerView.ViewHolder{
        TextView textView;
        public Holder(View itemView) {
            super(itemView);
            textView= (TextView) itemView.findViewById(android.R.id.text1);
        }

    }

    private static class RecyclerAdapter extends RecyclerView.Adapter<Holder>{

        private List<String> items;

        RecyclerAdapter(@NonNull List<String> content){
            items=content;
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView= LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1,parent,false);
            return new Holder(itemView);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            holder.textView.setText(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
