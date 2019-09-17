package com.cyphereco.openturnkey.ui;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBTransItem;

import java.text.SimpleDateFormat;
import java.util.List;


public class HistoryViewAdapter extends RecyclerView.Adapter<HistoryViewAdapter.ViewHolder> {
    private final static String TAG = HistoryViewAdapter.class.getSimpleName();

    private List<DBTransItem> mTransDataset;
    private LayoutInflater mInflater;
    private AdapterListener mAdapterListener = null;

    public interface AdapterListener {
        void onClickTransItem(int position);
    }

    HistoryViewAdapter(List<DBTransItem> data, LayoutInflater inflater) {
        Log.d(TAG, "HistoryViewAdapter()");
        this.mTransDataset = data;
        this.mInflater = inflater;
    }

    @NonNull
    @Override
    public HistoryViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = mInflater.inflate(R.layout.history_item_view, viewGroup, false);
        return (new HistoryViewAdapter.ViewHolder(v));
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewAdapter.ViewHolder viewHolder, int position) {
        Log.d(TAG, "onBindViewHolder position: " + position);
        viewHolder.mTVTransDate.setText(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(
                mTransDataset.get(position).getDatetime()));
        viewHolder.mTVTransFrom.setText(mTransDataset.get(position).getPayerAddr());
        viewHolder.mTVTransTo.setText(mTransDataset.get(position).getPayeeAddr());
        viewHolder.mTVTransAmount.setText(String.valueOf(mTransDataset.get(position).getAmount()));
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
        return mTransDataset.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTVTransDate;
        private TextView mTVTransFrom;
        private TextView mTVTransTo;
        private TextView mTVTransAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTVTransDate = itemView.findViewById(R.id.textView_history_item_date);
            mTVTransFrom = itemView.findViewById(R.id.textView_history_item_from);
            mTVTransTo = itemView.findViewById(R.id.textView_history_item_to);
            mTVTransAmount = itemView.findViewById(R.id.textView_history_item_amount);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "item on click");
                    if (null != mAdapterListener) {
                        mAdapterListener.onClickTransItem(getAdapterPosition());
                    }
                }
            });
        }
    }
}
