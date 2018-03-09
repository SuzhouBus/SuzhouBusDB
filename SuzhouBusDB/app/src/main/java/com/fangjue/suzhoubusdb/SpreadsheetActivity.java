package com.fangjue.suzhoubusdb;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SpreadsheetActivity extends AppCompatActivity {
    private ViewPager busPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spreadsheet);

        this.busPager = (ViewPager)findViewById(R.id.busPager);
        this.busPager.setAdapter(new CategoryPagerAdapter(getSupportFragmentManager(),
                BusDB.getInstance(this.getApplicationContext())));
    }
}

class CategoryPagerAdapter extends FragmentStatePagerAdapter {
    private BusDB db;

    public CategoryPagerAdapter(FragmentManager fm, BusDB db) {
        super(fm);
        this.db = db;
    }

    @Override
    public Fragment getItem(int position) {
        return SheetFragment.newInstance(this.db, null);
    }

    @Override
    public int getCount() {
        return 1;
    }
}
