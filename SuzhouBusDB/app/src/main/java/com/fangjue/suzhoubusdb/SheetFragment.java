package com.fangjue.suzhoubusdb;

import java.util.ArrayList;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SheetFragment extends Fragment {
    private String category;
    private ArrayList<Bus> buses;

    /**
     * Factory method to create new SheetFragment.
     *
     * @param db BusDB instance from MainActivity.
     * @param category Category of buses to show in this fragment.
     * @return A new instance of fragment SheetFragment.
     */
    public static SheetFragment newInstance(BusDB db, String category) {
        SheetFragment fragment = new SheetFragment();
        fragment.category = category;
        fragment.buses = db.getBusesByCategory(category);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sheet, container, false);
    }
}
