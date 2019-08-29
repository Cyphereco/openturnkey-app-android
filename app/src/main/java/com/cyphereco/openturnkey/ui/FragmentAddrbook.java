package com.cyphereco.openturnkey.ui;

import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBAddrItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;

import java.util.List;


public class FragmentAddrbook extends Fragment {
    private final static String TAG = AddrbookViewAdapter.class.getSimpleName();
    private OpenturnkeyDB mOtkDB = null;
    private AddrbookViewAdapter mAdapter;
    private FragmentAddrbookListener mListener;

    private void setAdapterListener() {
        if (null == mAdapter) {
            return;
        }
        mAdapter.setAdapterListener(new AddrbookViewAdapter.AdapterListener() {
            @Override
            public void onPaying(int position) {
                Log.d(TAG, "On Paying the position is: " + position);

                DBAddrItem item = mAdapter.getAddressItemByPosition(position);

                mListener.onAddressbookPayingButtonClick(item.getAddress());
            }

            @Override
            public void onEditingContact(int position) {
                Log.d(TAG, "onEditingContact the position is: " + position);

                DBAddrItem item = mAdapter.getAddressItemByPosition(position);
                Intent intent = new Intent(getContext(), EditContactActivity.class);

                intent.putExtra("CONTACT_NAME", item.getName());
                intent.putExtra("CONTACT_ADDRESS", item.getAddress());
                getActivity().startActivityForResult(intent, MainActivity.REQUEST_CODE_CONTACT_EDIT);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_addrbook, container, false);

        if (null == mOtkDB) {
            mOtkDB = new OpenturnkeyDB(getContext());
        }

        List<DBAddrItem> addrDataset = mOtkDB.getAllAddressbook();
        TextView noAddressView = view.findViewById(R.id.text_no_address);
        RecyclerView addrRecyclerView = view.findViewById(R.id.recyclerView_address);

        if (0 < addrDataset.size()) {
            noAddressView.setVisibility(View.INVISIBLE);
            if (null != addrRecyclerView) {
                addrRecyclerView.setVisibility(View.VISIBLE);
                mAdapter = new AddrbookViewAdapter(addrDataset, inflater);
                this.setAdapterListener();

                final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                addrRecyclerView.setLayoutManager(layoutManager);
                addrRecyclerView.setAdapter(mAdapter);
            }
        }
        else {
            noAddressView.setVisibility(View.VISIBLE);
            if (null != addrRecyclerView) {
                addrRecyclerView.setVisibility(View.INVISIBLE);
            }
        }
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
