package com.cyphereco.openturnkey.ui;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.RecordAddress;
import com.cyphereco.openturnkey.utils.AddressUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ViewAdapterAddrbook extends RecyclerView.Adapter<ViewAdapterAddrbook.ViewHolder> implements Filterable {

    private Context mContext;
    private AdapterListener mAdapterListener = null;
    private List<RecordAddress> mAddressbookDataset = new ArrayList<>();
    private AddressFilter mAddressFilter;

    public interface AdapterListener {
        void onDeleteAddress(int position);

        void onEditAddress(int position);

        void onShowQRCode(int position);

        void onPay(int position);
    }

    public void setData(List<RecordAddress> data) {
        if (null != mAddressbookDataset) {
            this.mAddressbookDataset.clear();
        }
        this.mAddressbookDataset = data;
        notifyDataSetChanged();
    }

    ViewAdapterAddrbook(Context context) {
        mContext = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(
                R.layout.listitem_address, parent, false);
        return (new ViewAdapterAddrbook.ViewHolder(v));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        if (null == mAddressbookDataset) {
            return;
        }
        viewHolder.mTVAlias.setText(
                AddressUtils.getShortAlias(mAddressbookDataset.get(position).getAlias()));
        viewHolder.mTVAddress.setText(
                AddressUtils.getShortAddress(mAddressbookDataset.get(position).getAddress()));
    }

    @Override
    public int getItemCount() {
        if (null == mAddressbookDataset) {
            return 0;
        }
        return mAddressbookDataset.size();
    }

    void setAdapterListener(AdapterListener listener) {
        mAdapterListener = listener;
    }

    RecordAddress getAddressItemByPosition(final int position) {
        if (null == mAddressbookDataset) {
            return null;
        }
        return mAddressbookDataset.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTVAlias;
        private TextView mTVAddress;

        @RequiresApi(api = Build.VERSION_CODES.O)
        ViewHolder(View itemView) {
            super(itemView);
            mTVAlias = itemView.findViewById(R.id.textView_addrbook_item_alias);
            mTVAddress = itemView.findViewById(R.id.textView_addrbook_item_address);
            ImageView ivDeleteBtn = itemView.findViewById(R.id.imageView_addrbook_item_delete);
            ivDeleteBtn.setTooltipText(itemView.getContext().getString(R.string.delete));
            ImageView ivQRCodeBtn = itemView.findViewById(R.id.imageView_addrbook_item_qrcode);
            ivQRCodeBtn.setTooltipText(itemView.getContext().getString(R.string.show_qr_code));
            ImageView ivPayBtn = itemView.findViewById(R.id.imageView_addrbook_item_pay);
            ivPayBtn.setTooltipText(itemView.getContext().getString(R.string.pay));

            ivDeleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mAdapterListener) {
                        mAdapterListener.onDeleteAddress(getAdapterPosition());
                    }
                }
            });

            mTVAlias.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mAdapterListener) {
                        mAdapterListener.onEditAddress(getAdapterPosition());
                    }
                }
            });

            mTVAddress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mAdapterListener) {
                        mAdapterListener.onEditAddress(getAdapterPosition());
                    }
                }
            });

            ivQRCodeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mAdapterListener) {
                        mAdapterListener.onShowQRCode(getAdapterPosition());
                    }
                }
            });

            ivPayBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mAdapterListener) {
                        mAdapterListener.onPay(getAdapterPosition());
                    }
                }
            });
        }
    }

    @Override
    public Filter getFilter() {
        if (null == mAddressFilter) {
            mAddressFilter = new AddressFilter(this, mAddressbookDataset);
        }
        return mAddressFilter;
    }

    private static class AddressFilter extends Filter {

        private final ViewAdapterAddrbook mAdapter;
        private final List<RecordAddress> mOriginalList;
        private final List<RecordAddress> mFilteredList;

        private AddressFilter(ViewAdapterAddrbook adapter, List<RecordAddress> originalList) {
            super();
            this.mAdapter = adapter;
            this.mOriginalList = new LinkedList<>(originalList);
            this.mFilteredList = new ArrayList<>();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            mFilteredList.clear();
            final FilterResults results = new FilterResults();

            if (0 == constraint.length()) {
                mFilteredList.addAll(mOriginalList);
            } else {
                final String filterPattern = constraint.toString().toLowerCase().trim();
                for (RecordAddress item : mOriginalList) {
                    if (item.getAlias().toLowerCase().contains(filterPattern)) {
                        mFilteredList.add(item);
                    }
                }
            }
            results.values = mFilteredList;
            results.count = mFilteredList.size();
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mAdapter.mAddressbookDataset.clear();
            mAdapter.mAddressbookDataset.addAll((List<RecordAddress>) results.values);
            mAdapter.notifyDataSetChanged();
        }
    }
}
