package com.fangjue.suzhoubusdb;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    private ListView busList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

        this.db = new BusDBOpenHelper(this.getApplicationContext(), "Suzhou").getWritableDatabase();
    }

    private void onKeyClicked(View key, boolean longClick) {
        View focus = getCurrentFocus();
        if (focus instanceof EditText) {
            EditText editText = (EditText)focus;

            if (key.getId() == R.id.keyDel) {
                Editable text = editText.getText();
                if (longClick) {
                    text.clear();
                } else {
                    if (text.length() > 0)
                        text.delete(text.length() - 1, text.length());
                }
                return;
            }

            boolean preventDefault;
            CharSequence text = ((Button) key).getText();
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
            if (editText.getId() == R.id.busId)
                preventDefault = this.onBusIdInput(editText, text, type, longClick);
            else if (editText.getId() == R.id.licenseId)
                preventDefault = this.onLicenseIdInput(editText, text, type, longClick);
            else if (editText.getId() == R.id.lineId)
                preventDefault = this.onLineIdInput(editText, text, type, longClick);
            else
                return;

            if (!preventDefault)
                editText.append(text);
            if (editText.getId() == R.id.busId && editText.getText().length() >= 3)
                this.query(BusDBOpenHelper.KEY_BUS_ID, editText.getText().toString());
            else if (editText.getId() == R.id.licenseId && editText.getText().length() >= 3)
                this.query(BusDBOpenHelper.KEY_LICENSE_ID, editText.getText().toString());
            else if (editText.getId() == R.id.lineId)
                this.query(BusDBOpenHelper.KEY_LINE_ID, editText.getText().toString());
        }
    }

    private void query(String field, String value) {
        Cursor cursor = this.db.query(BusDBOpenHelper.BUSES_TABLE_NAME, null,
                field + (field.equals(BusDBOpenHelper.KEY_LINE_ID) ?
                        " = ?" : " LIKE '%' || ? || '%'"),
                new String[]{value}, null, null, field);
        android.util.Log.i("info", field + (field.equals(BusDBOpenHelper.KEY_LINE_ID) ? " = ?" : " LIKE '%' + ? + '%'"));
        ArrayList<String> buses = new ArrayList<>();
        while (buses.size() < 20 && cursor.moveToNext())
            buses.add(cursor.getString(0) + " " + cursor.getString(1) + " " + cursor.getString(2));
        // TODO: Move the expression above to a dedicated function.
        cursor.close();
        busList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, buses));
    }

    public void keyClicked(View key) {
        this.onKeyClicked(key, false);
    }

    private boolean onBusIdInput(EditText editText, CharSequence text,
                                 KeyType type, boolean longClick) {
        // Suzhou buses have only numerical ids.
        if (type != KeyType.kDigit)
            return true;
        String id = editText.getText().toString();
        if (longClick && !id.contains("-")) {
            editText.getText().insert(0, text + "-");
            return true;
        } else if (id.length() >= 6 || (!id.contains("-") && id.length() >= 4))
            return true;
        return false;
    }

    private boolean onLicenseIdInput(EditText editText, CharSequence text,
                                     KeyType type, boolean longClick) {
        if (editText.getText().length() >= 5)
            return true;
        return false;
    }

    private boolean onLineIdInput(EditText editText, CharSequence text,
                                  KeyType type, boolean longClick) {
        return false;
    }

    public void onUpdateClicked(View v) {
        ContentValues values = new ContentValues();
        String busId = ((EditText)findViewById(R.id.busId)).getText().toString();
        String licenseId = ((EditText)findViewById(R.id.licenseId)).getText().toString();
        if (busId.length() == 0 && licenseId.length() == 0)
            return;
        values.put(BusDBOpenHelper.KEY_BUS_ID, busId);
        values.put(BusDBOpenHelper.KEY_LICENSE_ID, licenseId);
        values.put(BusDBOpenHelper.KEY_LINE_ID,
                ((EditText)findViewById(R.id.lineId)).getText().toString());
        if (this.db.update(BusDBOpenHelper.BUSES_TABLE_NAME, values, "busId = ? OR licenseId = ?",
                new String[]{busId, licenseId}) == 0)
            this.db.insert(BusDBOpenHelper.BUSES_TABLE_NAME, null, values);
    }

    private enum KeyType {
        kOther,
        kDigit,
        kAlphabet,
        kShortcut,
    }
}
