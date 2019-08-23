package com.cyphereco.openturnkey.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBAddrItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;

import java.util.List;


public class FragmentAddrbook extends Fragment {
    private OpenturnkeyDB mOtkDB = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_addrbook, container, false);

        if (null == mOtkDB) {
            mOtkDB = new OpenturnkeyDB(getContext());
        }

        List<DBAddrItem> addrData = mOtkDB.getAllAddressbook();
        TextView noAddressView = (TextView) view.findViewById(R.id.text_no_history);
        ListView addrListView = (ListView) view.findViewById(R.id.addrbookListView);

        if (0 < addrData.size()) {
            noAddressView.setVisibility(View.INVISIBLE);
            addrListView.setVisibility(View.VISIBLE);

            AddrbookViewAdapter adapter = new AddrbookViewAdapter(addrData, inflater);
            addrListView.setAdapter(adapter);
            addrListView.setOnItemClickListener(onClickListView);
        }
        else {
            noAddressView.setVisibility(View.VISIBLE);
            addrListView.setVisibility(View.INVISIBLE);
        }
        return view;
    }

    private AdapterView.OnItemClickListener onClickListView = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Toast.makeText(getContext(), "Selected Position: "+ (position +1), Toast.LENGTH_SHORT).show();
        }
    };
}
