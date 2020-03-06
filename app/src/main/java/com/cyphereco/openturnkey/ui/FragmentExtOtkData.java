package com.cyphereco.openturnkey.ui;

import android.support.v4.app.Fragment;

import com.cyphereco.openturnkey.core.OtkData;

public class FragmentExtOtkData extends Fragment {
    public static OtkData OTK_DATA = null;

    public void postOtkData(OtkData data) {
        OTK_DATA = data;
    }
}
