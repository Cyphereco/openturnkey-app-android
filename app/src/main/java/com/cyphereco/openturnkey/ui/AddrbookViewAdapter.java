package com.cyphereco.openturnkey.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBAddrItem;
import com.cyphereco.openturnkey.utils.AddressUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class AddrbookViewAdapter extends RecyclerView.Adapter<AddrbookViewAdapter.ViewHolder> implements Filterable {
//    private final static String TAG = AddrbookViewAdapter.class.getSimpleName();

    private Context mContext;
    private AdapterListener mAdapterListener = null;
    private List<DBAddrItem> mAddressbookDataset;
    private AddressFilter mAddressFilter;

    public interface AdapterListener {
        void onDeleteAddress(int position);
        void onEditAddress(int position);
        void onShowQRCode(int position);
        void onPay(int position);
    }

    public void setData(List<DBAddrItem> data) {
        if (null != mAddressbookDataset) {
            this.mAddressbookDataset.clear();
        }
        this.mAddressbookDataset = data;
        notifyDataSetChanged();
    }

    AddrbookViewAdapter(Context context) {
        this.mContext = context;
        mAddressbookDataset = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(
                R.layout.listitem_address , parent ,false);
        return (new ViewHolder(v));
    }

   @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        if (null == mAddressbookDataset) {
            return;
        }
        viewHolder.mTVAlias.setText(
                AddressUtils.getShortAlias(mAddressbookDataset.get(position).getName()));
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

    DBAddrItem getAddressItemByPosition(final int position) {
        if (null == mAddressbookDataset) {
            return null;
        }
        return mAddressbookDataset.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTVAlias;
        private TextView mTVAddress;

        ViewHolder(View itemView) {
            super(itemView);
            mTVAlias = itemView.findViewById(R.id.textView_addrbook_item_alias);
            mTVAddress = itemView.findViewById(R.id.textView_addrbook_item_address);
            ImageView ivDeleteBtn = itemView.findViewById(R.id.imageView_addrbook_item_delete);
            ImageView ivEditBtn = itemView.findViewById(R.id.imageView_addrbook_item_edit);
            ImageView ivQRCodeBtn = itemView.findViewById(R.id.imageView_addrbook_item_qrcode);
            ImageView ivPayBtn = itemView.findViewById(R.id.imageView_addrbook_item_pay);

            ivDeleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mAdapterListener) {
                        mAdapterListener.onDeleteAddress(getAdapterPosition());
                    }
                }
            });

            ivEditBtn.setOnClickListener(new View.OnClickListener() {
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

        private final AddrbookViewAdapter mAdapter;
        private final List<DBAddrItem> mOriginalList;
        private final List<DBAddrItem> mFilteredList;

        private AddressFilter(AddrbookViewAdapter adapter, List<DBAddrItem> originalList) {
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
            }
            else {
                final String filterPattern = constraint.toString().toLowerCase().trim();
                for (DBAddrItem item : mOriginalList) {
                    if (item.getName().toLowerCase().contains(filterPattern)) {
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
            mAdapter.mAddressbookDataset.addAll((List<DBAddrItem>) results.values);
            mAdapter.notifyDataSetChanged();
        }
    }
}
