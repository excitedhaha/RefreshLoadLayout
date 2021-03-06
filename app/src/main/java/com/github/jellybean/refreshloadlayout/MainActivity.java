package com.github.jellybean.refreshloadlayout;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.github.refreshloadlayout.MaterialIndicator;
import com.github.refreshloadlayout.RefreshLoadLayout;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    ListView listView;
    RefreshLoadLayout refreshLoadLayout;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);
        refreshLoadLayout = (RefreshLoadLayout) findViewById(R.id.refreshLayout);
        refreshLoadLayout.setLoadingEnabled(true);
        refreshLoadLayout.setOnRefreshListener(new RefreshLoadLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLoadLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLoadLayout.endRefreshing();
                    }
                }, 2000);
            }
        });
        MaterialIndicator materialIndicator=new MaterialIndicator(this);
        materialIndicator.getProgressView().setColorSchemeColors(Color.RED,Color.YELLOW);
        refreshLoadLayout.setRefreshIndicator(materialIndicator);
        refreshLoadLayout.setLoadingIndicator(new MaterialIndicator(this));
        fillListView();
        refreshLoadLayout.setLoadingHandler(new RefreshLoadLayout.LoadingHandler() {
            @Override
            public boolean canLoadMore() {
                return listView.getCount()<26;
            }

            @Override
            public void onLoading() {
                refreshLoadLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ArrayAdapter<String> arrayAdapter= (ArrayAdapter<String>) listView.getAdapter();
                        arrayAdapter.add("a new data");
                        arrayAdapter.add("a new data");
                        refreshLoadLayout.endLoading();
                    }
                }, 2000);

            }
        });

    }


    private void fillListView() {
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            strings.add("https://github.com/excitedhaha/RefreshLoadLayout");
        }
        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, strings);
        listView.setAdapter(stringArrayAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        switch (id){
            case R.id.toRecycler:
                startActivity(new Intent(this,RecyclerViewActivity.class));
                return true;
            case R.id.ban_refresh:
                boolean enable=item.isChecked();
                item.setChecked(!enable);
                refreshLoadLayout.setRefreshingEnabled(enable);
                return true;
            case R.id.ban_load:
                boolean checked=item.isChecked();
                item.setChecked(!checked);
                refreshLoadLayout.setLoadingEnabled(checked);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}
