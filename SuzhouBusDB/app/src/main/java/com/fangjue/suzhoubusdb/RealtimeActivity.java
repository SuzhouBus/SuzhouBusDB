package com.fangjue.suzhoubusdb;

import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RealtimeActivity extends AppCompatActivity {
    private EditText searchBox;
    private GridView detailsView;
    private BusDB db;
    private AsyncTask<?,?,?> activeTask = null;
    private int currentSearchLength = 0;
    private List<RealtimeLine> currentLines = null;
    private List<RealtimeEntry> currentEntries = null;
    private HashMap<String, String> busIdMap = null;

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
                return false;
            }
        });
        this.detailsView.setOnItemClickListener(new GridView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
                onDetailsItemClick(position);
            }
        });

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                List<Bus> buses = BusDB.getInstance(getApplicationContext()).queryCompleteBuses();
                busIdMap = new HashMap<>(buses.size());
                for(Bus bus: buses) {
                    busIdMap.put(bus.getLicenseId(), bus.getBusId());
                }
                return null;
            }
        }.execute();

        this.db = BusDB.getInstance(this.getApplicationContext());
    }

    private void onSearchBoxInput() {
        String query = this.searchBox.getText().toString();
        if (this.currentEntries == null && query.length() > this.currentSearchLength) {
            this.updateDetailsViewByQuery(query, 10, 0);
        }
        this.currentSearchLength = query.length();
    }

    public void searchClicked(View view) {
        String query = this.searchBox.getText().toString();
        this.updateDetailsViewByQuery(query, 0, Color.GRAY);
        if (this.activeTask == null) {
            AsyncTask<String, Void, ArrayList<RealtimeLine>> task = new AsyncTask<String, Void, ArrayList<RealtimeLine>>() {
                @Override
                protected ArrayList<RealtimeLine> doInBackground(String... strings) {
                    return RequestSender.getInstance(getApplicationContext()).searchLine(strings[0]);
                }

                @Override
                protected void onPostExecute(ArrayList<RealtimeLine> results) {
                    activeTask = null;
                    if (results != null) {
                        updateDetailsWithLines(results, 0);
                    }
                }
            };
            this.activeTask = task;
            task.execute(query);
        }
    }

    private void onDetailsItemClick(int position) {
        if (currentLines != null && detailsView.getNumColumns() == 2) {
            int row = position / detailsView.getNumColumns();
            if (row < currentLines.size()) {
                RealtimeLine line = currentLines.get(row);
                final String guid = line.getGuid();
                final String lineDescription = line.getName() + "(" + line.getDirection() + ")";
                new AsyncTask<Void, Void, ArrayList<RealtimeEntry>>() {
                    @Override
                    protected ArrayList<RealtimeEntry> doInBackground(Void... voids) {
                        return RequestSender.getInstance(getApplicationContext()).queryLine(guid);
                    }

                    @Override
                    protected void onPostExecute(ArrayList<RealtimeEntry> results) {
                        updateDetailsWithRealtimeEntries(results);
                        searchBox.setText(lineDescription);
                    }
                }.execute();
            }
        }
    }

    private void updateDetailsViewByQuery(String query, int limit, int color) {
        List<RealtimeLine> lines = db.queryRealtimeLines(query, limit);
        this.updateDetailsWithLines(lines, color);
    }

    private void updateDetailsWithLines(List<RealtimeLine> lines, final int color) {
        this.currentLines = lines;
        this.currentEntries = null;
        ArrayList<String> values = new ArrayList<>();
        for (RealtimeLine line : lines) {
            values.add(line.getName());
            values.add(line.getDirection());
        }
        this.detailsView.setNumColumns(2);
        this.detailsView.setAdapter(new ArrayAdapter<String>(this.getBaseContext(), android.R.layout.simple_list_item_1, values) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                if (color != 0) {
                    view.setTextColor(color);
                }
                return view;
            }
        });
    }

    private void updateDetailsWithRealtimeEntries(ArrayList<RealtimeEntry> entries) {
        this.currentEntries = entries;
        ArrayList<String> values = new ArrayList<>();
        for (RealtimeEntry entry: entries) {
            values.add(entry.stopName);
            String licenseId = entry.licenseId;
            if (licenseId.startsWith("苏E-")) {
                licenseId = licenseId.substring(3);
            }
            String busId = this.busIdMap.get(licenseId);
            if (busId == null)
                busId = "";
            values.add(busId);
            values.add(licenseId);
            values.add(entry.time);
        }
        this.detailsView.setNumColumns(4);
        this.detailsView.setAdapter(new ArrayAdapter<String>(this.getBaseContext(), android.R.layout.simple_list_item_1, values));
    }
}