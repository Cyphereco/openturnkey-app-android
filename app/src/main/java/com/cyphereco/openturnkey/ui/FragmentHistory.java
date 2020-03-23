package com.cyphereco.openturnkey.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.RecordTransaction;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.BtcExchangeRates;
import com.cyphereco.openturnkey.utils.TxFee;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class FragmentHistory extends FragmentExtendOtkViewPage {

    private TextView mTVNoHistoryMessage;
    private RecyclerView mRVHistory;
    private ViewAdapterHistory mItemViewAdapter;

    private void setAdapterListener() {
        if (null == mItemViewAdapter) {
            return;
        }
        mItemViewAdapter.setAdapterListener(new ViewAdapterHistory.AdapterListener() {
            @Override
            public void onClickTransItem(int position) {
                /* Show transaction detail in another activity */
                RecordTransaction item = mItemViewAdapter.getTransItemByPosition(position);

                if (null != item) {
                    Intent intent = new Intent(getContext(), ActivityTransactionInfo.class);
                    intent.putExtra(ActivityTransactionInfo.KEY_CURRENT_TRANS_ID, item.getId());
                    if (getActivity() != null) {
                        getActivity().startActivityForResult(intent,
                                MainActivity.REQUEST_CODE_TRANSACTION_INFO);
                    }
                } else {
                    logger.error("Cannot find transaction item. Position: " + position);
                }
            }
        });
    }

    private void updateTransactionDataset() {
        List<RecordTransaction> dataset = OpenturnkeyDB.getAllTransactions();

        Collections.sort(dataset, new Comparator<RecordTransaction>() {
            @Override
            public int compare(RecordTransaction o1, RecordTransaction o2) {
                Date dt1 = new Date(o1.getTimestamp());
                Date dt2 = new Date(o2.getTimestamp());
                if (dt1.before(dt2)) {
                    return 1;
                } else if (dt1.equals(dt2)) {
                    return 0;
                }
                return -1;
            }
        });

        if (0 < dataset.size()) {
            mTVNoHistoryMessage.setVisibility(View.INVISIBLE);
            mRVHistory.setVisibility(View.VISIBLE);

            mItemViewAdapter.setData(dataset);
        } else {
            mTVNoHistoryMessage.setVisibility(View.VISIBLE);
            mRVHistory.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        mTVNoHistoryMessage = view.findViewById(R.id.text_no_history);
        mRVHistory = view.findViewById(R.id.recyclerView_history);

        mItemViewAdapter = new ViewAdapterHistory(getContext());
        this.setAdapterListener();

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRVHistory.setLayoutManager(layoutManager);
        mRVHistory.setAdapter(mItemViewAdapter);

        MainActivity.addToListOnlineDataUpdateListener(new MainActivity.OnlineDataUpdateListener() {
            @Override
            public void onExchangeRateUpdated(BtcExchangeRates xrate) {

            }

            @Override
            public void onTxFeeUpdated(TxFee txFee) {

            }

            @Override
            public void onBlockHeightUpdated(int height) {
                if (isSelected()) {
                    // switch page quickly to refresh adapter items confirmation icon
                    MainActivity.switchToPage(MainActivity.PAGE.PAY.ordinal());
                    MainActivity.switchToPage(MainActivity.PAGE.HISTORY.ordinal());
                }
            }
        });

        return view;
    }

    @Override
    public void onPageSelected() {
        super.onPageSelected();
        // fragment has not been attached, do not update
        if (this.getContext() == null) return;;

        logger.debug("refresh history");
        updateTransactionDataset();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_history, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_history_clear_history) {
            DialogClearHistory dialog = new DialogClearHistory();
            dialog.setDialogClearHistoryListener(new DialogClearHistory.DialogClearHistoryListener() {
                @Override
                public void onHistoryCleared() {
                    updateTransactionDataset();
                }
            });
            assert getFragmentManager() != null;
            dialog.show(getFragmentManager(), "dialog");
        }
        return super.onOptionsItemSelected(item);
    }
}
