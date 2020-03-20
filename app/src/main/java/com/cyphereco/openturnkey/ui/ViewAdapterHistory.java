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
import com.cyphereco.openturnkey.db.RecordTransaction;
import com.cyphereco.openturnkey.utils.AddressUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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

        long timepassed = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime().getTime() -
                mTransDataset.get(position).getTimestamp();

        // for test
        if (timepassed < 0) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_unconfirmed);
        } else if (timepassed < 1 * 600000) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm0);
        } else if (timepassed < 2 * 600000) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm1);
        } else if (timepassed < 3 * 600000) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm2);
        } else if (timepassed < 4 * 600000) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm3);
        } else if (timepassed < 5 * 600000) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm4);
        } else if (timepassed < 6 * 600000) {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm5);
        } else {
            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm6);
        }

        // if a transaction is not included in the transaction memory pool,
        // which cannot be queried from the blockchain network (not unconfirmed)
        // the transaction might not be broadcast successfully or simply not accepted
        // by any miners. Such transaction should indicate a question mark symbol
        // for its uncertainty. If still not confirmed after 144 blocks (1 day)
        // the transaction should be considered failed.

//        if (mTransDataset.get(position).getConfirmations() < 0) {
//            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_unconfirmed);
//        } else if (mTransDataset.get(position).getConfirmations() < 1) {
//            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm0);
//        } else if (mTransDataset.get(position).getConfirmations() < 2) {
//            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm1);
//        } else if (mTransDataset.get(position).getConfirmations() < 3) {
//            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm2);
//        } else if (mTransDataset.get(position).getConfirmations() < 4) {
//            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm3);
//        } else if (mTransDataset.get(position).getConfirmations() < 5) {
//            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm4);
//        } else if (mTransDataset.get(position).getConfirmations() < 6) {
//            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm5);
//        } else {
//            viewHolder.mIVTransResult.setImageResource(R.drawable.ic_confirm6);
//        }

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
//        viewHolder.mTVTransAmount.setText(getAmountString(mTransDataset.get(position).getAmount()));
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
        /*
         * The amount of BTC of the transaction,
         * decimal points up to 4 digits when integer number is less/equal to 4 digits,
         * and decimal points up to 2 digits when integer number is greater than 4 digits
         */
        if (amount >= 1000) {
            return String.format(Locale.getDefault(), "%.2f", amount);
        } else {
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
