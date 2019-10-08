package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBAddrItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.QRCodeUtils;
import com.google.zxing.qrcode.encoder.QRCode;
import com.sandro.bitcoinpaymenturi.BitcoinPaymentURI;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class FragmentAddrbook extends Fragment {
    private final static String TAG = AddrbookViewAdapter.class.getSimpleName();

    private TextView mTVNoAddressMessage;
    private RecyclerView mRVAddressList;

    private OpenturnkeyDB mOtkDB = null;
    private AddrbookViewAdapter mAdapter;
    private FragmentAddrbookListener mListener;

    private void setAdapterListener() {
        if (null == mAdapter) {
            return;
        }
        mAdapter.setAdapterListener(new AddrbookViewAdapter.AdapterListener() {
            @Override
            public void onDeleteAddress(int position) {
                Log.d(TAG, "onDelete the position is: " + position);
                processDeleteAddress(position);
            }

            @Override
            public void onEditAddress(int position) {
                Log.d(TAG, "onEditAddress the position is: " + position);
                processEditAddress(position);
            }

            @Override
            public void onShowQRCode(int position) {
                Log.d(TAG, "onShowQRCode the position is: " + position);
                processShowQRCode(position);
            }

            @Override
            public void onPay(int position) {
                Log.d(TAG, "onPay the position is: " + position);
                DBAddrItem item = mAdapter.getAddressItemByPosition(position);
                mListener.onAddressbookPayingButtonClick(item.getAddress());
            }
        });
    }

    private void processDeleteAddress(int position) {
        final DBAddrItem item = mAdapter.getAddressItemByPosition(position);
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.delete_address_dialog_message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (OpenturnkeyDB.ReturnValue.SUCCESS ==
                                mOtkDB.deleteAddressbookByAddr(item.getAddress())) {
                            updateAddressDataset();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void processEditAddress(int position) {
        DBAddrItem item = mAdapter.getAddressItemByPosition(position);
        Intent intent = new Intent(getContext(), ActivityAddressEditor.class);

        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_TYPE,
                ActivityAddressEditor.EDITOR_TYPE_EDIT);
        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_CONTACT_ALIAS, item.getName());
        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_CONTACT_ADDR, item.getAddress());
        if (null != getActivity()) {
            getActivity().startActivityForResult(intent,
                    MainActivity.REQUEST_CODE_ADDRESS_EDIT);
        }
    }

    private void processShowQRCode(int position) {
        final DBAddrItem item = mAdapter.getAddressItemByPosition(position);
        final View v = View.inflate(getContext(), R.layout.dialog_address_item_qrcode, null);
        TextView tvAlias = v.findViewById(R.id.textView_addrbook_item_dialog_alias);
        TextView tvAddress = v.findViewById(R.id.textView_addrbook_item_dialog_address);
        ImageView ivQRCode = v.findViewById(R.id.imageView_addrbook_item_address_qrcode);

        tvAlias.setText(item.getName());
        tvAddress.setText(item.getAddress());
        BitcoinPaymentURI uri = new BitcoinPaymentURI.Builder().address(item.getAddress()).build();
        if (uri != null) {
            Bitmap bitmap = QRCodeUtils.encodeAsBitmap(uri.getURI(),
                    ivQRCode.getDrawable().getIntrinsicWidth(),
                    ivQRCode.getDrawable().getIntrinsicHeight());
            ivQRCode.setImageBitmap(bitmap);
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.btc_qr_code))
                .setView(v)
                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void updateAddressDataset() {
        List<DBAddrItem> addrDataset = mOtkDB.getAllAddressbook();

        // Sort by alias
        Collections.sort(addrDataset, new Comparator<DBAddrItem>() {
            @Override
            public int compare(DBAddrItem o1, DBAddrItem o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        if (0 < addrDataset.size()) {
            mTVNoAddressMessage.setVisibility(View.INVISIBLE);
            if (null != mRVAddressList) {
                mRVAddressList.setVisibility(View.VISIBLE);
                mAdapter.setData(addrDataset);
            }
        }
        else {
            mTVNoAddressMessage.setVisibility(View.VISIBLE);
            if (null != mRVAddressList) {
                mRVAddressList.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_addrbook, container, false);

        mTVNoAddressMessage = view.findViewById(R.id.text_no_address);
        mRVAddressList = view.findViewById(R.id.recyclerView_address);

        if (null == mOtkDB) {
            mOtkDB = new OpenturnkeyDB(getContext());
        }

        mAdapter = new AddrbookViewAdapter(getContext());
        this.setAdapterListener();

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRVAddressList.setLayoutManager(layoutManager);
        mRVAddressList.setAdapter(mAdapter);

        updateAddressDataset();

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach");
        if (context instanceof FragmentAddrbookListener) {
            mListener = (FragmentAddrbookListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach");
        mListener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
    }

    public interface FragmentAddrbookListener {
        void onAddressbookPayingButtonClick(String address);
    }
}
