package com.cyphereco.openturnkey.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBTransItem;
import com.cyphereco.openturnkey.utils.AddressUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;


public class HistoryViewAdapter extends RecyclerView.Adapter<HistoryViewAdapter.ViewHolder> {
    private final static String TAG = HistoryViewAdapter.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private Context mContext;
    private List<DBTransItem> mTransDataset;
    private AdapterListener mAdapterListener = null;

    public interface AdapterListener {
        void onClickTransItem(int position);
    }

    public void setData(List<DBTransItem> data) {
        if (null != mTransDataset) {
            mTransDataset.clear();
        }
        mTransDataset = data;
        notifyDataSetChanged();
    }

    HistoryViewAdapter(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public HistoryViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.listitem_transaction,
                parent ,false);
        return (new HistoryViewAdapter.ViewHolder(v));
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewAdapter.ViewHolder viewHolder, int position) {
        logger.debug("onBindViewHolder position: " + position);
        if (null == mTransDataset) {
            return;
        }
        if (0 == mTransDataset.get(position).getStatus()) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_success_24dp);
        }
        else {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_fail_24dp);
        }
        viewHolder.mTVTransDate.setText(
                new SimpleDateFormat("yyyy/MM/dd", Locale.US).format(
                mTransDataset.get(position).getDatetime()));
        viewHolder.mTVTransTime.setText(
                new SimpleDateFormat("HH:mm:ss", Locale.US).format(
                mTransDataset.get(position).getDatetime()));
        viewHolder.mTVTransFrom.setText(
                AddressUtils.getShortAddress(mTransDataset.get(position).getPayerAddr()));
        viewHolder.mTVTransTo.setText(
                AddressUtils.getShortAddress(mTransDataset.get(position).getPayeeAddr()));
        viewHolder.mTVTransAmount.setText(getAmountString(mTransDataset.get(position).getAmount()));
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

    DBTransItem getTransItemByPosition(final int position) {
        if (null == mTransDataset) {
            return null;
        }
        return mTransDataset.get(position);
    }

    private String getAmountString(double amount) {
        /*
        * The amount of BTC of the transaction,
        * decimal points up to 4 digits when integer number is less/equal to 4 digits,
        * and decimal points up to 2 digits when integer number is greater than 4 digits
        */
        if (amount >= 1000) {
            return String.format(Locale.getDefault(), "%.2f", amount);
        }
        else {
            return String.format(Locale.getDefault(), "%.4f", amount);
        }
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
