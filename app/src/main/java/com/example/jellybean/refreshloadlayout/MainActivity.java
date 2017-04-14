package com.example.jellybean.refreshloadlayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.refreshloadlayout.RefreshLoadLayout;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    String url = "https://xiaodanchen.github.io/hello-world/img/Engin.png";

    ListView listView;
    RefreshLoadLayout refreshLoadLayout;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);
        refreshLoadLayout = (RefreshLoadLayout) findViewById(R.id.refreshLayout);
        refreshLoadLayout.setOnRefreshListener(new RefreshLoadLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLoadLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLoadLayout.endRefreshing();
                    }
                }, 3000);
            }
        });
        fillListView();


    }


    private void fillListView() {
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            strings.add(Integer.toString(i));
        }
        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, strings);
        listView.setAdapter(stringArrayAdapter);
    }
}
