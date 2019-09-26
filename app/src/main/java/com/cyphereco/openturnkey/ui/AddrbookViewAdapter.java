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
import com.cyphereco.openturnkey.db.DBAddrItem;
import com.cyphereco.openturnkey.utils.AddressUtils;

import java.util.List;


public class AddrbookViewAdapter extends RecyclerView.Adapter<AddrbookViewAdapter.ViewHolder> {
//    private final static String TAG = AddrbookViewAdapter.class.getSimpleName();

    private Context mContext;
    private AdapterListener mAdapterListener = null;
    private List<DBAddrItem> mAddressbookDataset;

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
}
