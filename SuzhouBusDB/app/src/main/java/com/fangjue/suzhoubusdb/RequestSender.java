package com.fangjue.suzhoubusdb;

import android.content.Context;
import android.util.Log;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
