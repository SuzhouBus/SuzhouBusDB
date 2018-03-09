package com.fangjue.suzhoubusdb;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class RealtimeActivity extends AppCompatActivity {
    private EditText searchBox;
    private GridView detailsView;
    private BusDB db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime);

        this.searchBox = findViewById(R.id.searchBox);
        this.detailsView = findViewById(R.id.detailsView);
        this.searchBox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                onSearchBoxInput();
                return true;
            }
        });

        this.db = BusDB.getInstance(this.getApplicationContext());
    }

    private void onSearchBoxInput() {
        this.updateDetailsViewByQuery(this.searchBox.getText().toString(), 10, 0);
    }

    public void searchClicked(View view) {
        this.updateDetailsViewByQuery(this.searchBox.getText().toString(), 0, Color.GRAY);
    }

    private void updateDetailsViewByQuery(String query, int limit, final int color) {
        List<RealtimeLine> lines = db.queryRealtimeLines(query, limit);
        ArrayList<String> values = new ArrayList<>();
        for (RealtimeLine line: lines) {
            values.add(line.getName());
            values.add(line.getDirection());
        }
        this.detailsView.setNumColumns(2);
        this.detailsView.setAdapter(new ArrayAdapter<String>(this.getBaseContext(), android.R.layout.simple_list_item_1, values) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView)super.getView(position, convertView, parent);
                if (color != 0) {
                    view.setTextColor(color);
                }
                return view;
            }
        });
    }
}
