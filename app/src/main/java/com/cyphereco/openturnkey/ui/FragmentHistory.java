package com.cyphereco.openturnkey.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBTransItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FragmentHistory extends Fragment {
    private final static String TAG = FragmentHistory.class.getSimpleName();

    private OpenturnkeyDB mOtkDB = null;
    private HistoryViewAdapter mViewAdapter;

    private void setAdapterListener() {
        if (null == mViewAdapter) {
            return;
        }
        mViewAdapter.setAdapterListener(new HistoryViewAdapter.AdapterListener() {
            @Override
            public void onClickTransItem(int position) {
                /* Show transaction detail in another activity */
                DBTransItem item = mViewAdapter.getTransItemByPosition(position);

                Intent intent = new Intent(getContext(), TransDetailActivity.class);
                intent.putExtra("TRANS_ID", item.getId());
                getActivity().startActivity(intent);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        if (null == mOtkDB) {
            mOtkDB = new OpenturnkeyDB(getContext());
        }

        List<DBTransItem> historyDataset = mOtkDB.getAllTransaction();
        TextView tvNoHistory = view.findViewById(R.id.text_no_history);
        RecyclerView rvHistory = view.findViewById(R.id.recyclerView_history);
        Log.d(TAG, "size of dataset: " + historyDataset.size());

        if (0 < historyDataset.size()) {
            tvNoHistory.setVisibility(View.INVISIBLE);
            if (null != rvHistory) {
                rvHistory.setVisibility(View.VISIBLE);
                mViewAdapter = new HistoryViewAdapter(historyDataset, inflater);
                this.setAdapterListener();

                final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                rvHistory.setLayoutManager(layoutManager);
                rvHistory.setAdapter(mViewAdapter);
            }
        }
        else {
            tvNoHistory.setVisibility(View.VISIBLE);
            if (null != rvHistory) {
                rvHistory.setVisibility(View.INVISIBLE);
            }
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach");
    }

}
