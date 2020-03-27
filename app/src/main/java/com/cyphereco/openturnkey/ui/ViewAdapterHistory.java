package com.cyphereco.openturnkey.ui;

import android.content.Context;
import android.print.PageRange;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.blockcypher.model.transaction.Transaction;
import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.db.RecordTransaction;
import com.cyphereco.openturnkey.utils.AddressUtils;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.webservices.BlockChainInfo;
import com.cyphereco.openturnkey.webservices.BlockCypher;

import org.slf4j.Logger;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ViewAdapterHistory extends RecyclerView.Adapter<ViewAdapterHistory.ViewHolder> {
    private final static String TAG = ViewAdapterHistory.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private Context mContext;
    private List<RecordTransaction> mTransDataset;
    private AdapterListener mAdapterListener = null;

    public interface AdapterListener {
        void onClickTransItem(int position);
    }

    public void setData(List<RecordTransaction> data) {
        if (null != mTransDataset) {
            mTransDataset.clear();
        }
        mTransDataset = data;
        notifyDataSetChanged();
    }

    ViewAdapterHistory(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public ViewAdapterHistory.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.listitem_transaction,
                parent, false);
        return (new ViewAdapterHistory.ViewHolder(v));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewAdapterHistory.ViewHolder viewHolder, int position) {
        if (null == mTransDataset) {
            return;
        }

        // if a transaction is not included in the transaction memory pool,
        // which cannot be queried from the blockchain network (not unconfirmed)
        // the transaction might not be broadcast successfully or simply not accepted
        // by any miners. Such transaction should indicate a question mark symbol
        // for its uncertainty. If still not confirmed after 144 blocks (1 day)
        // the transaction should be considered failed.

        final RecordTransaction recordTransaction = mTransDataset.get(position);

        // unmark below two lines to force records to be updated with online result
//        recordTransaction.setBlockHeight(-1);
//        OpenturnkeyDB.updateTransaction(recordTransaction);

        if (recordTransaction.getBlockHeight() <= 0) {
            // transaction has not been confirmed, need to update (block height, confirmed time, rawTx) from network
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Transaction tx = null;
                    if (recordTransaction.getRawData() == null || recordTransaction.getRawData().length() == 0) {
                        tx = BlockCypher.getTransaction(recordTransaction.getHash(), true);
                        if (tx != null && tx.getHex().length() > 0) {
                            recordTransaction.setRawData(tx.getHex());
                            OpenturnkeyDB.updateTransaction(recordTransaction);
                        }
                    }

                    if (recordTransaction.getBlockHeight() <= 0){
                        long height = -1;
                        if (!Preferences.isTestnet()) {
                            height = BlockChainInfo.getTxBlockHeight(recordTransaction.getHash());
                        }
                        else {
                            tx = BlockCypher.getTransaction(recordTransaction.getHash(), true);
                            height = tx == null ? -1 : tx.getBlockHeight();
                        }
                        if (height < 0) return;

                        recordTransaction.setBlockHeight(height);
                        if (!Preferences.isTestnet()) {
                            recordTransaction.setTimestamp(BlockChainInfo.getTxTime(recordTransaction.getHash()));
                        }
                        else {
                            recordTransaction.setTimestamp(
                                    BtcUtils.convertDateTimeStringToLong(tx.getReceived()));
                        }
                        OpenturnkeyDB.updateTransaction(recordTransaction);
                        logger.debug("Current Time: {} / {}", System.currentTimeMillis(),
                                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(
                                        System.currentTimeMillis()));
                        logger.debug("Transaction accepted: {}", recordTransaction.toString());
                    }
                }
            }).start();
        }
        long confirmations = MainActivity.getBlockHeight() - mTransDataset.get(position).getBlockHeight() + 1;

        if (mTransDataset.get(position).getBlockHeight() <= 0 || confirmations < 0) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_unconfirmed);
        } else if (confirmations < 1) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm0);
        } else if (confirmations < 2) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm1);
        } else if (confirmations < 3) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm2);
        } else if (confirmations < 4) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm3);
        } else if (confirmations < 5) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm4);
        } else if (confirmations < 6) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm5);
        } else {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm6);
        }

        viewHolder.mTVTransDate.setText(
                new SimpleDateFormat("yyyy/MM/dd", Locale.US).format(
                        mTransDataset.get(position).getTimestamp()));
        viewHolder.mTVTransTime.setText(
                new SimpleDateFormat("HH:mm:ss", Locale.US).format(
                        mTransDataset.get(position).getTimestamp()));
        viewHolder.mTVTransFrom.setText(
                AddressUtils.getShortAddress(mTransDataset.get(position).getPayer()));
        viewHolder.mTVTransTo.setText(
                AddressUtils.getShortAddress(mTransDataset.get(position).getPayee()));
        viewHolder.mTVTransAmount.setText(getAmountString(mTransDataset.get(position).getAmountSent()));
    }

    @Override
    public int getItemCount() {
        if (null == mTransDataset) {
            return 0;
        }
        return mTransDataset.size();
    }

    void setAdapterListener(AdapterListener listener) {
        mAdapterListener = listener;
    }

    RecordTransaction getTransItemByPosition(final int position) {
        if (null == mTransDataset) {
            return null;
        }
        return mTransDataset.get(position);
    }

    private String getAmountString(double amount) {
        NumberFormat formatter = new DecimalFormat("#,###.####");
        return formatter.format(amount);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView mIVTransResult;
        private TextView mTVTransDate;
        private TextView mTVTransTime;
        private TextView mTVTransFrom;
        private TextView mTVTransTo;
        private TextView mTVTransAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mIVTransResult = itemView.findViewById(R.id.iv_trans_item_ret_icon);
            mTVTransDate = itemView.findViewById(R.id.tv_trans_item_date);
            mTVTransTime = itemView.findViewById(R.id.tv_trans_item_time);
            mTVTransFrom = itemView.findViewById(R.id.tv_trans_item_from_addr);
            mTVTransTo = itemView.findViewById(R.id.tv_trans_item_to_addr);
            mTVTransAmount = itemView.findViewById(R.id.tv_trans_item_amount);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logger.debug("item on click");
                    if (null != mAdapterListener) {
                        mAdapterListener.onClickTransItem(getAdapterPosition());
                    }
                }
            });
        }
    }
}
