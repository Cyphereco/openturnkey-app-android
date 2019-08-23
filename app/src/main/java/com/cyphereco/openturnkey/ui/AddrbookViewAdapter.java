package com.cyphereco.openturnkey.ui;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBAddrItem;

import java.util.List;

public class AddrbookViewAdapter extends BaseAdapter {
    private List<DBAddrItem> mAddressbookData ;
    private LayoutInflater mInflater;

    public AddrbookViewAdapter(List<DBAddrItem> data, LayoutInflater inflater){
        this.mAddressbookData = data;
        this.mInflater = inflater;
    }

    @Override
    public int getCount() {
        return mAddressbookData.size();
    }

    @Override
    public Object getItem(int position) {
        return mAddressbookData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = mInflater.inflate(R.layout.addrbook_item_view, parent, false);
        TextView tvName = (TextView) v.findViewById(R.id.tvAddrBookName);

        tvName.setText(mAddressbookData.get(position).getName().toString());

        return v;
    }
}
