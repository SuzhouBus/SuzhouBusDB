package com.fangjue.suzhoubusdb;

import java.util.ArrayList;
import java.util.Arrays;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;

public class SheetFragment extends Fragment {
    private String category;
    private int numColumns;
    private ArrayList<String> gridData;
    private GridView sheetGrid;
    private LinearLayout sheetGridContainer;

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
        fragment.gridData = new ArrayList<>();
        ArrayList<Bus> buses = db.getBusesByCategory(category);

        int commentsColumns = 1;
        for (Bus bus:buses)
            commentsColumns = Math.max(bus.getComments().length, commentsColumns);
        fragment.numColumns = commentsColumns + 3;

        for (Bus bus: buses) {
            fragment.gridData.add(bus.getBusId());
            fragment.gridData.add(bus.getLicenseId());
            fragment.gridData.add(bus.getLineId());
            if (bus.getComments().length > 0)
                fragment.gridData.addAll(Arrays.asList(bus.getComments()));
            for (int i = commentsColumns - bus.getComments().length; i > 0; --i)
                fragment.gridData.add("");
        }
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sheet, container, false);
        this.sheetGrid = (GridView)view.findViewById(R.id.sheetGrid);
        this.sheetGridContainer = (LinearLayout)view.findViewById((R.id.sheetGridContainer));

        this.sheetGrid.setNumColumns(this.numColumns);
        ViewGroup.LayoutParams layoutParams = this.sheetGrid.getLayoutParams();
        layoutParams.width = 180 * this.numColumns;
        this.sheetGrid.setLayoutParams(layoutParams);
        this.sheetGrid.setColumnWidth(180);
        this.sheetGrid.setAdapter(new ArrayAdapter<>(this.getContext(),
                android.R.layout.simple_list_item_1, this.gridData));
        return view;
    }
}