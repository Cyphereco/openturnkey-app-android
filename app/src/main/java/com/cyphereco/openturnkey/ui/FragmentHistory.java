package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.RecordTransaction;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.BtcExchangeRates;
import com.cyphereco.openturnkey.utils.TxFee;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

        final Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (isSelected()) {
                    // switch page quickly to refresh adapter items confirmation icon
                    MainActivity.switchToPage(MainActivity.PAGE.PAY.ordinal());
                    MainActivity.switchToPage(MainActivity.PAGE.HISTORY.ordinal());
                }
                return true;
            }
        });

        MainActivity.addToListOnlineDataUpdateListener(new MainActivity.OnlineDataUpdateListener() {
            @Override
            public void onExchangeRateUpdated(BtcExchangeRates xrate) {

            }

            @Override
            public void onTxFeeUpdated(TxFee txFee) {

            }

            @Override
            public void onBlockHeightUpdated(long height) {
                Message msg = new Message();
                msg.obj = height;
                handler.sendMessage(msg);
            }
        });

        return view;
    }

    @Override
    public void onPageSelected() {
        super.onPageSelected();
        // fragment has not been attached, do not update
        if (this.getContext() == null) return;

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
            processClearHistory();
//            DialogClearHistory dialog = new DialogClearHistory();
//            dialog.setDialogClearHistoryListener(new DialogClearHistory.DialogClearHistoryListener() {
//                @Override
//                public void onHistoryCleared() {
//                    updateTransactionDataset();
//                }
//            });
//            assert getFragmentManager() != null;
//            dialog.show(getFragmentManager(), "dialog");
        }
        return super.onOptionsItemSelected(item);
    }

    private void processClearHistory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogNarrowWidth);

        builder.setMessage(R.string.clear_all_payment_history)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Clear payment history
                        if (OpenturnkeyDB.clearTransactionTable()) {
                            Toast.makeText(getContext(), getString(R.string.all_history_cleared), Toast.LENGTH_LONG).show();
                            updateTransactionDataset();
                        }
                        else {
                            Toast.makeText(getContext(), getString(R.string.failed_to_clear_history), Toast.LENGTH_LONG).show();
                        }
                    }
                });

        Dialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_read_otk_round);
        dialog.getWindow().getAttributes().windowAnimations = R.style.ShowReadOtkAnimation;
        dialog.show();
    }
}
