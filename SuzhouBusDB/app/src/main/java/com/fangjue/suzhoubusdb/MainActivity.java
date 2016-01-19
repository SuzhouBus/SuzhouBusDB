package com.fangjue.suzhoubusdb;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.DateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    private FileWriter logFile;
    private ListView busList;
    private EditText busId;
    private EditText licenseId;
    private EditText lineId;
    private File logFileObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Disable soft keyboard for all fields in favor of fine tuned keyboard in our UI.
        for (int id:new int[]{R.id.busId, R.id.licenseId, R.id.lineId})
            ((EditText)findViewById(id)).setInputType(InputType.TYPE_NULL);

        for (int id:new int[]{R.id.key0, R.id.key1, R.id.key2, R.id.key3, R.id.key4,
                R.id.key5, R.id.key6, R.id.key7, R.id.key8, R.id.key9,
                R.id.keyDel}) {
            findViewById(id).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onKeyClicked(v, true);
                    return true;
                }
            });
        }

        busList = (ListView)findViewById(R.id.busList);
        busId = (EditText)findViewById(R.id.busId);
        licenseId = (EditText)findViewById(R.id.licenseId);
        lineId = (EditText)findViewById(R.id.lineId);

        this.db = new BusDBOpenHelper(this.getApplicationContext(), "Suzhou").getWritableDatabase();
        this.logFileObject = new File(getFilesDir(), "Suzhou.log");
        try {
            this.logFile = new FileWriter(this.logFileObject, true);
            this.logFile.write(DateFormat.getDateTimeInstance().format(new Date()) + "\n");
            this.logFile.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    private void onKeyClicked(View key, boolean longClick) {
        View focus = getCurrentFocus();
        if (focus instanceof EditText) {
            EditText editText = (EditText)focus;
            Editable text = editText.getText();

            if (key.getId() == R.id.keyDel) {
                this.onDeleteClicked(editText, text, longClick);
                return;
            }

            boolean preventDefault;
            CharSequence keyText = ((Button) key).getText();
            KeyType type = this.getKeyType(key);

            if (editText == busId)
                preventDefault = this.onBusIdInput(text, keyText, type, longClick);
            else if (editText == licenseId)
                preventDefault = this.onLicenseIdInput(text, keyText, type, longClick);
            else if (editText == lineId)
                preventDefault = this.onLineIdInput(text, keyText, type, longClick);
            else
                return;

            if (!preventDefault)
                editText.append(keyText);
            boolean hasCompanyId = text.toString().contains("-");
            if (editText == busId && (text.length() >= 3 || text.length() == 2 && !hasCompanyId)) {
                this.query(BusDBOpenHelper.KEY_BUS_ID, (hasCompanyId ? "" : "-") + text.toString());
            } else if (editText.getId() == R.id.licenseId && text.length() >= 3)
                this.query(BusDBOpenHelper.KEY_LICENSE_ID, text.toString());
            else if (editText.getId() == R.id.lineId)
                this.query(BusDBOpenHelper.KEY_LINE_ID, text.toString());
        }
    }

    private void onDeleteClicked(EditText editText, Editable text, boolean longClick) {
        if (longClick) {
            // Clear all fields if the current field is already empty
            if (text.length() == 0) {
                this.busId.getText().clear();
                this.licenseId.getText().clear();
                this.lineId.getText().clear();
            } else
                text.clear();
        } else {
            if (text.length() > 0)
                text.delete(text.length() - 1, text.length());
            if (text.length() == 0) {
                if (editText == this.licenseId)
                    this.busId.requestFocus();
                else if (editText == this.lineId)
                    this.licenseId.requestFocus();
            }
        }
    }

    private KeyType getKeyType(View key) {
        KeyType type = KeyType.kOther;

        if (Arrays.asList(R.id.key0, R.id.key1, R.id.key2, R.id.key3, R.id.key4,
                R.id.key5, R.id.key6, R.id.key7, R.id.key8, R.id.key9).contains(key.getId()))
            type = KeyType.kDigit;
        else if (Arrays.asList(R.id.keyA, R.id.keyB, R.id.keyC, R.id.keyD, R.id.keyE,
                R.id.keyF, R.id.keyG, R.id.keyH, R.id.keyJ, R.id.keyK, R.id.keyL, R.id.keyM,
                R.id.keyN, R.id.keyP, R.id.keyQ, R.id.keyR, R.id.keyS, R.id.keyT, R.id.keyU,
                R.id.keyV, R.id.keyW, R.id.keyX, R.id.keyY, R.id.keyZ).contains(key.getId()))
            type = KeyType.kAlphabet;
        else if (Arrays.asList(R.id.keyMRU1, R.id.keyMRU2, R.id.keyMRU3, R.id.keyMRU4).
                contains(key.getId()))
            type = KeyType.kShortcut;

        return type;
    }

    private void query(String field, String value) {
        String where = null;
        if (field.equals(BusDBOpenHelper.KEY_BUS_ID))
            where = field + " LIKE '%' || ? || '%'";
        else if (field.equals(BusDBOpenHelper.KEY_LICENSE_ID))
            where = field + " LIKE ? || '%'";
        else if (field.equals(BusDBOpenHelper.KEY_LINE_ID))
            where = field + " = ?";
        Cursor cursor = this.db.query(BusDBOpenHelper.BUSES_TABLE_NAME,
                new String[]{BusDBOpenHelper.KEY_BUS_ID, BusDBOpenHelper.KEY_LICENSE_ID,
                        BusDBOpenHelper.KEY_LINE_ID, BusDBOpenHelper.KEY_COMMENTS},
                where, new String[]{value}, null, null, field);
        android.util.Log.i("info", field + (field.equals(BusDBOpenHelper.KEY_LINE_ID) ? " = ?" : " LIKE '%' + ? + '%'"));
        ArrayList<String> buses = new ArrayList<>();
        while (buses.size() < 100 && cursor.moveToNext())
            buses.add(formatListItem(cursor.getString(0), cursor.getString(1),
                    cursor.getString(2), cursor.getString(3)));
        cursor.close();
        busList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, buses));
    }

    private String formatListItem(String busId, String licenseId, String lineId, String comments) {
        final int MAX_LENGTH = 16;
        comments = comments.trim();
        if (comments.length() > MAX_LENGTH)
            comments = comments.substring(0, MAX_LENGTH - 3) + "...";
        return busId + " " + licenseId + " " + lineId +  "   " + comments;
    }

    public void keyClicked(View key) {
        this.onKeyClicked(key, false);
    }

    private boolean onBusIdInput(Editable text, CharSequence keyText,
                                 KeyType type, boolean longClick) {
        // Suzhou buses have only numerical ids.
        if (type != KeyType.kDigit)
            return true;

        // Suzhou Buses have ids like 1-2345 where "1" identifies a company and "2345" identifies
        // buses within the company. We accept ids without the leading "1-" because company ids can
        // be determined by lineId.
        String id = text.toString();

        // Reject if busId is complete.
        if (id.length() >= 6 || (!id.contains("-") && id.length() >= 4 && !longClick))
            return true;
        // The last digit has already been typed. Move focus to the next field.
        // TODO: Restore the expression after company id autocomplete is implemented.
        if (id.length() == 5 || (id.length() == 4 && longClick)
                /*|| (!id.contains("-") && id.length() == 3)*/)
            this.licenseId.requestFocus();
        // Long pressing a number types the company prefix.
        if (longClick && !id.contains("-")) {
            text.insert(0, keyText + "-");
            return true;
        }
        return false;
    }

    private boolean onLicenseIdInput(Editable text, CharSequence keyText,
                                     KeyType type, boolean longClick) {
        // Vehicle license numbers in China look like 苏E1A345. Buses in the same city will always
        // have the same region identifier ("苏E" in Suzhou, for example) and only the remaining
        // five alphanumeric characters are required to type.
        if (text.length() >= 5)
            return true;
        // The last character has been typed. Move to the next field.
        if (text.length() == 4)
            lineId.requestFocus();
        return false;
    }

    private boolean onLineIdInput(Editable text, CharSequence keyText,
                                  KeyType type, boolean longClick) {
        return false;
    }

    public void onUpdateClicked(View v) {
        String busId = this.busId.getText().toString();
        String licenseId = this.licenseId.getText().toString();
        String lineId = this.lineId.getText().toString();
        if (busId.length() == 0 && licenseId.length() == 0)
            return;
        // Reject incomplete input.
        // TODO: The company part of busId can be autocompleted if lineId is available.
        if (busId.length() != 0 && busId.length() != 6)
            return;
        if (licenseId.length() != 0 && licenseId.length() != 5)
            return;

        ContentValues values = new ContentValues();
        values.put(BusDBOpenHelper.KEY_BUS_ID, busId);
        values.put(BusDBOpenHelper.KEY_LICENSE_ID, licenseId);
        values.put(BusDBOpenHelper.KEY_LINE_ID, lineId);
        String where = "busId = ? OR licenseId = ?";
        String[] args = {busId, licenseId};
        if (busId.length() == 0) {
            where = "licenseId = ?";
            args = new String[]{licenseId};
        } else if (licenseId.length() == 0) {
            where = "busId = ?";
            args = new String[]{busId};
        }
        if (this.db.update(BusDBOpenHelper.BUSES_TABLE_NAME, values, where, args) == 0)
            this.db.insert(BusDBOpenHelper.BUSES_TABLE_NAME, null, values);
        try {
            logFile.write(busId + "\t" + licenseId + "\t" + lineId + "\n");
            this.logFile.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.busId.getText().clear();
        this.licenseId.getText().clear();
        this.lineId.getText().clear();
        this.busId.requestFocus();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.showLogs:
                this.onShowLogs();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onShowLogs() {
        String contents = "<无法打开日志文件>";
        Scanner scanner = null;
        try {
            scanner = new Scanner(this.logFileObject);
            contents = scanner.useDelimiter("\\A").next();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchElementException e) {
            contents = "";
        } finally {
            if (scanner != null)
                scanner.close();
        }
        new AlertDialog.Builder(this)
                .setTitle("Log")
                .setMessage(contents)
                .show();
    }

    private enum KeyType {
        kOther,
        kDigit,
        kAlphabet,
        kShortcut,
    }
}
