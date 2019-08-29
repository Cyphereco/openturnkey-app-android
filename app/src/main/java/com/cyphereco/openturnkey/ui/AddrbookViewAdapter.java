package com.cyphereco.openturnkey.ui;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBAddrItem;

import java.util.List;


public class AddrbookViewAdapter extends RecyclerView.Adapter<AddrbookViewAdapter.ViewHolder> {
    private final static String TAG = AddrbookViewAdapter.class.getSimpleName();

    private List<DBAddrItem> mAddressbookDataset ;
    private LayoutInflater mInflater;

    private AdapterListener mAdapterListener = null;

    public interface AdapterListener {
        void onPaying(int position);
        void onEditingContact(int position);
    }

    AddrbookViewAdapter(List<DBAddrItem> data, LayoutInflater inflater) {
        this.mAddressbookDataset = data;
        this.mInflater = inflater;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = mInflater.inflate(R.layout.addrbook_item_view, parent, false);
        return (new ViewHolder(v));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        viewHolder.mTextViewAddressName.setText(
                mAddressbookDataset.get(position).getName());
    }

    @Override
    public int getItemCount() {
        return mAddressbookDataset.size();
    }

    void setAdapterListener(AdapterListener listener) {
        mAdapterListener = listener;
    }

    DBAddrItem getAddressItemByPosition(final int position) {
        return mAddressbookDataset.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextViewAddressName;

        ViewHolder(View itemView) {
            super(itemView);
            mTextViewAddressName = itemView.findViewById(R.id.textView_addrbook_name);
            Button mButtonPay = itemView.findViewById(R.id.button_addrbook_pay);

            mButtonPay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mAdapterListener) {
                        mAdapterListener.onPaying(getAdapterPosition());
                    }
                }
            });
            mTextViewAddressName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mAdapterListener) {
                        mAdapterListener.onEditingContact(getAdapterPosition());
                    }
                }
            });
        }
    }
}
