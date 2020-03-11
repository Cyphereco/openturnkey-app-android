package com.cyphereco.openturnkey.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {

    private static FragmentPay fragmentPay;
    private static FragmentOtk fragmentOtk;
    private static FragmentHistory fragmentHistory;
    private static FragmentAddrbook fragmentAddrbook;

    public static final int PAGE_PAY = 0;
    public static final int PAGE_OTK = 1;
    public static final int PAGE_HISTORY = 2;
    public static final int PAGE_ADDRBOOK = 3;

    public PagerAdapter(FragmentManager fm) {
        super(fm);
        fragmentPay = new FragmentPay();
        fragmentOtk = new FragmentOtk();
        fragmentHistory = new FragmentHistory();
        fragmentAddrbook = new FragmentAddrbook();
    }

    @Override
    public Fragment getItem(int page) {
        switch (page) {
            case 0:
                return fragmentPay;
            case 1:
                return fragmentOtk;
            case 2:
                return fragmentHistory;
            case 3:
                return fragmentAddrbook;
        }
        return fragmentPay;
    }

    @Override
    public int getCount() {
        return 4;
    }
}
