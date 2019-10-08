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

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class FragmentHistory extends Fragment {
    private final static String TAG = FragmentHistory.class.getSimpleName();

    private TextView mTVNoHistoryMessage;
    private RecyclerView mRVHistory;

    private OpenturnkeyDB mOtkDB = null;
    private HistoryViewAdapter mItemViewAdapter;

    private void setAdapterListener() {
        if (null == mItemViewAdapter) {
            return;
        }
        mItemViewAdapter.setAdapterListener(new HistoryViewAdapter.AdapterListener() {
            @Override
            public void onClickTransItem(int position) {
                /* Show transaction detail in another activity */
                DBTransItem item = mItemViewAdapter.getTransItemByPosition(position);

                if (null != item) {
                    Intent intent = new Intent(getContext(), ActivityTransactionInfo.class);
                    intent.putExtra(ActivityTransactionInfo.KEY_CURRENT_TRANS_ID, item.getId());
                    if (getActivity() != null) {
                        getActivity().startActivityForResult(intent,
                                MainActivity.REQUEST_CODE_TRANSACTION_INFO);
                    }
                }
                else {
                    Log.e(TAG, "Cannot find transaction item. Position: " + position);
                }
            }
        });
    }

    private void updateTransactionDataset() {
        List<DBTransItem> dataset = mOtkDB.getAllTransaction();

        Collections.sort(dataset, new Comparator<DBTransItem>() {
            @Override
            public int compare(DBTransItem o1, DBTransItem o2) {
                Date dt1 = new Date(o1.getDatetime());
                Date dt2 = new Date(o2.getDatetime());
                if (dt1.before(dt2)) {
                    return 1;
                }
                else if (dt1.equals(dt2)) {
                    return 0;
                }
                return -1;
            }
        });

        if (0 < dataset.size()) {
            mTVNoHistoryMessage.setVisibility(View.INVISIBLE);
            mRVHistory.setVisibility(View.VISIBLE);

            mItemViewAdapter.setData(dataset);
        }
        else {
            mTVNoHistoryMessage.setVisibility(View.VISIBLE);
            mRVHistory.setVisibility(View.INVISIBLE);
        }
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

        mTVNoHistoryMessage = view.findViewById(R.id.text_no_history);
        mRVHistory = view.findViewById(R.id.recyclerView_history);

        mItemViewAdapter = new HistoryViewAdapter(getContext());
        this.setAdapterListener();

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRVHistory.setLayoutManager(layoutManager);
        mRVHistory.setAdapter(mItemViewAdapter);

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

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        updateTransactionDataset();
    }
}
