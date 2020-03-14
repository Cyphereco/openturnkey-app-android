package com.cyphereco.openturnkey.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {

    private FragmentPay fragmentPay;
    private FragmentOtk fragmentOtk;
    private FragmentHistory fragmentHistory;
    private FragmentAddrbook fragmentAddrbook;

    static final int PAGE_PAY = 0;
    static final int PAGE_OTK = 1;
    static final int PAGE_HISTORY = 2;
    static final int PAGE_ADDRBOOK = 3;

    PagerAdapter(FragmentManager fm) {
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
