package com.fangjue.suzhoubusdb;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RealtimeActivity extends AppCompatActivity {
    private EditText searchBox;
    private GridView detailsView;
    private BusDB db;
    private SearchLineTask activeTask = null;
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
                if (currentLines != null && detailsView.getNumColumns() == 2) {
                    int row = position / detailsView.getNumColumns();
                    if (row < currentLines.size()) {
                        final String guid = currentLines.get(row).getGuid();
                        new AsyncTask<Void, Void, ArrayList<RealtimeEntry>>() {
                            @Override
                            protected ArrayList<RealtimeEntry> doInBackground(Void... voids) {
                                return RequestSender.getInstance(getApplicationContext()).queryLine(guid);
                            }

                            @Override
                            protected void onPostExecute(ArrayList<RealtimeEntry> results) {
                                updateDetailsWithRealtimeEntries(results);
                            }
                        }.execute();
                    }
                }
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
            this.activeTask = new SearchLineTask(this);
            this.activeTask.execute(query);
        }
    }

    public void onSearchCompleted(ArrayList<RealtimeLine> results) {
        if (results != null) {
            this.activeTask = null;
            this.updateDetailsWithLines(results, 0);
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

class RequestSender {
    private static final String CACHE_KEY_MAIN = "Main";
    private static RequestSender instance = null;
    private static final String BASE_URL = "http://www.szjt.gov.cn/BusQuery/APTSLine.aspx";
    private final File cacheDir;
    private final Pattern VIEWSTATE_PATTERN;
    private final Pattern LINE_PATTERN;
    private final Pattern LINE_REQUEST_SUCCESS_PATTERN;
    private final Pattern REALTIME_ENTRY_PATTERN;

    public static synchronized RequestSender getInstance(Context context) {
        if (instance == null) {
            instance = new RequestSender(context);
        }
        return instance;
    }

    RequestSender(Context context) {
        this.cacheDir = context.getCacheDir();
        this.VIEWSTATE_PATTERN = Pattern.compile("<input\\s+type=\"hidden\"\\s+name=\"([^\"]+)\"\\s+id=\"[^\"]+\"\\s+value=\"([^\"]+)\"\\s*/?>");
        this.LINE_PATTERN = Pattern.compile("<tr><td><a\\s+href=\"APTSLine\\.aspx\\?.*?LineGuid=([^&]+)[^>]*>([^<]*)</a></td><td>([^<]*)</td></tr>");
        this.LINE_REQUEST_SUCCESS_PATTERN = Pattern.compile("<span\\s+ id=\"MainContent_DATA\">");
        this.REALTIME_ENTRY_PATTERN = Pattern.compile("<tr><td>(?:<a\\s+[^>]*>)?([^<]*)(?:</a>)?</td><td>([^<]*)</td><td>([^<]*)</td><td>([^<]*)</td></tr>");
    }

    private String sendRequest(String fullUrl) {
        return sendRequest(fullUrl, "GET");
    }

    private String sendRequest(String fullUrl, String method) {
        return sendRequest(fullUrl, method, null);
    }

    private String sendRequest(String fullUrl, String method, LinkedHashMap<String, String> arguments) {
        HttpURLConnection connection = null;
        InputStream stream = null;
        Scanner scanner = null;
        try {
            URL url = new URL(fullUrl);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod(method);
            if (arguments != null && method.equals("POST") && arguments.size() > 0) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                OutputStream postBodyStream = connection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(postBodyStream, "UTF-8"));
                writer.write(this.buildRequestBody(arguments));
                writer.flush();
                writer.close();
                postBodyStream.close();
            }
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("");
            }
            stream = connection.getInputStream();
            if (stream == null) {
                throw new IOException("");
            }
            scanner = new Scanner(stream, "utf-8").useDelimiter("\\A");
            if (!scanner.hasNext()) {
                throw new IOException("");
            }
            return scanner.next();
        } catch(Exception e) {
            Log.e(e.getClass().getName(), e.getMessage(), e);
            return null;
        } finally {
            if (scanner != null) {
                scanner.close();
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch(IOException e) { }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildRequestBody(LinkedHashMap<String, String> arguments) {
        StringBuilder postBody = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            try {
                String encodedName = URLEncoder.encode(entry.getKey(), "UTF-8");
                String encodedValue = URLEncoder.encode(entry.getValue(), "UTF-8");

                if (first) {
                    first = false;
                } else {
                    postBody.append('&');
                }
                postBody.append(encodedName);
                postBody.append("=");
                postBody.append(encodedValue);
            } catch(UnsupportedEncodingException e) { }
        }
        return postBody.toString();
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, String> readCachedViewState(String key) {
        FileInputStream stream = null;
        ObjectInputStream stream2 = null;
        try {
            stream = new FileInputStream(new File(this.cacheDir, "ViewState_" + key));
            stream2 = new ObjectInputStream(stream);
            return (LinkedHashMap<String, String>)stream2.readObject();
        } catch(IOException | ClassNotFoundException | ClassCastException e) {
            return null;
        } finally {
            try {
                if (stream2 != null) {
                    stream2.close();
                }
                if (stream != null) {
                    stream.close();
                }
            } catch(IOException e) { }
        }
    }

    private void writeCachedViewState(String key, LinkedHashMap<String, String> value) {
        FileOutputStream stream = null;
        ObjectOutputStream stream2 = null;
        try {
            stream = new FileOutputStream(new File(this.cacheDir, "ViewState_" + key));
            stream2 = new ObjectOutputStream(stream);
            stream2.writeObject(value);
        } catch(IOException e) {
        } finally {
            try {
                if (stream2 != null) {
                    stream2.close();
                }
                if (stream != null) {
                    stream.close();
                }
            } catch(IOException e) { }
        }
    }

    private LinkedHashMap<String, String> parseViewState(String body) {
        LinkedHashMap<String, String> results = new LinkedHashMap<>();
        Matcher matcher = this.VIEWSTATE_PATTERN.matcher(body);
        while (matcher.find()) {
            results.put(matcher.group(1), matcher.group(2));
        }
        return results;
    }

    public ArrayList<RealtimeLine> searchLine(String query) {
        return searchLine(query, true, null);
    }

    @SuppressWarnings("unchecked")
    private ArrayList<RealtimeLine> searchLine(String query, boolean renewViewStateOnFailure, LinkedHashMap<String, String> viewState) {
        if (viewState == null) {
            viewState = this.readCachedViewState(CACHE_KEY_MAIN);
        }
        if (viewState == null) {
            viewState = this.fetchNewViewState();
            if (viewState == null) {
                return null;
            }
            this.writeCachedViewState(CACHE_KEY_MAIN, viewState);
        }

        viewState.put("ctl00$MainContent$LineName", query);
        viewState.put("ctl00$MainContent$SearchLine", "搜索");
        String body = this.sendRequest(BASE_URL, "POST", viewState);
        ArrayList<RealtimeLine> results;
        if (body != null) {
            Matcher matcher = LINE_PATTERN.matcher(body);
            results = new ArrayList<>();
            while (matcher.find()) {
                results.add(new RealtimeLine(matcher.group(1), matcher.group(2), matcher.group(3), RealtimeLine.STATUS_ACTIVE));
            }
            if (results.size() > 0) {
                return results;
            }
            if (LINE_REQUEST_SUCCESS_PATTERN.matcher(body).matches()) {
                return results;
            }
        }

        if (renewViewStateOnFailure) {
            viewState = this.fetchNewViewState();
            results = this.searchLine(query, false, viewState);
            if (results != null && results.size() > 0) {
                this.writeCachedViewState(CACHE_KEY_MAIN, (LinkedHashMap<String, String>)viewState.clone());
            }
            return results;
        }

        return null;
    }

    public ArrayList<RealtimeEntry> queryLine(String guid) {
        StringBuilder url = new StringBuilder(BASE_URL);
        url.append("?LineGuid=");
        url.append(guid);
        String body = this.sendRequest(url.toString());
        if (body == null) {
            return null;
        }

        Matcher matcher = REALTIME_ENTRY_PATTERN.matcher(body);
        ArrayList<RealtimeEntry> results = new ArrayList<>();
        while (matcher.find()) {
            results.add(new RealtimeEntry(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)));
        }
        return results;
    }

    private LinkedHashMap<String, String> fetchNewViewState() {
        String body = this.sendRequest(BASE_URL);
        if (body == null)
            return null;
        return this.parseViewState(body);
    }
}

class SearchLineTask extends AsyncTask<String, Void, ArrayList<RealtimeLine>> {
    private RealtimeActivity context;
    private RequestSender requestSender;

    SearchLineTask(RealtimeActivity context) {
        this.context = context;
        this.requestSender = RequestSender.getInstance(context.getApplicationContext());
    }

    @Override
    protected ArrayList<RealtimeLine> doInBackground(String... strings) {
        return this.requestSender.searchLine(strings[0]);
    }

    @Override
    protected void onPostExecute(ArrayList<RealtimeLine> results) {
        this.context.onSearchCompleted(results);
    }
}