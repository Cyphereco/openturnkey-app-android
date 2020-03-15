package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBAddrItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.QRCodeUtils;
import com.sandro.bitcoinpaymenturi.BitcoinPaymentURI;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FragmentAddrbook extends FragmentExtendOtkViewPage {

    private TextView mTVNoAddressMessage;
    private RecyclerView mRVAddressList;

    private ViewAdapterAddrbook mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // inflate layout
        View view = inflater.inflate(R.layout.fragment_addrbook, container, false);

        // get view components
        mTVNoAddressMessage = view.findViewById(R.id.text_no_address);
        mRVAddressList = view.findViewById(R.id.recyclerView_address);

        // setup address record item adapter
        mAdapter = new ViewAdapterAddrbook(getContext());
        mAdapter.setAdapterListener(new ViewAdapterAddrbook.AdapterListener() {
            @Override
            public void onDeleteAddress(int position) {
//                logger.debug("onDelete the position is: " + position);
                processDeleteAddress(position);
            }

            @Override
            public void onEditAddress(int position) {
//                logger.debug("onEditAddress the position is: " + position);
                processEditAddress(position);
            }

            @Override
            public void onShowQRCode(int position) {
//                logger.debug("onShowQRCode the position is: " + position);
                processShowQRCode(position);
            }

            @Override
            public void onPay(int position) {
//                logger.debug("onPay the position is: " + position);
                DBAddrItem item = mAdapter.getAddressItemByPosition(position);
                MainActivity.setPayToAddress(item.getAddress());
                MainActivity.navToFragment(MainActivity.FRAGMENT_PAY);
            }
        });

        // assign record item adapter to recycler view
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRVAddressList.setLayoutManager(layoutManager);
        mRVAddressList.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        logger.debug("started");
        if (MainActivity.getSelectedFragment() == null) {
            logger.debug("activity restarted");
            MainActivity.setSelectedFragment(this);
            onPageSelected();
        }
    }

    @Override
    public void onPageSelected() {
        super.onPageSelected();

        logger.debug("refresh addresses");
        updateAddressDataset();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_addresses, menu);
        setAddressSearchView(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_addresses_add) {
            Intent intent = new Intent(getContext(), ActivityAddressEditor.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            // we don't need result here, we called startActivityForResult
            // only to prevent a bug, that we user press back button
            // the app closed instead of return to MainActivity,
            // when it return from pause state
            startActivityForResult(intent, 0);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateAddressDataset() {
        List<DBAddrItem> addrDataset = OpenturnkeyDB.getAllAddressbook();

        // Sort by alias
        Collections.sort(addrDataset, new Comparator<DBAddrItem>() {
            @Override
            public int compare(DBAddrItem o1, DBAddrItem o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        if (0 < addrDataset.size()) {
            // has address records, show address in recycler view
            mTVNoAddressMessage.setVisibility(View.INVISIBLE);
            if (null != mRVAddressList) {
                mRVAddressList.setVisibility(View.VISIBLE);
                mAdapter.setData(addrDataset);
            }
        } else {
            // no address records, show no record message and hide recycler view
            mTVNoAddressMessage.setVisibility(View.VISIBLE);
            if (null != mRVAddressList) {
                mRVAddressList.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void setAddressSearchView(Menu menu) {
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_addresses_search).getActionView();
        if (null != searchView) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    logger.info("SearchView onQueryTextSubmit: " + query);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    logger.info("Filter Addresses contains ({})", newText);
                    showAddressFilter(newText);
                    return false;
                }
            });
        }
    }

    public void showAddressFilter(String searchString) {
        mAdapter.getFilter().filter(searchString);
    }

    private void processDeleteAddress(int position) {
        final DBAddrItem item = mAdapter.getAddressItemByPosition(position);
        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setMessage(R.string.delete_address_dialog_message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (OpenturnkeyDB.deleteAddressbookByAlias(item.getName())) {
                            updateAddressDataset();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void processEditAddress(int position) {
        DBAddrItem item = mAdapter.getAddressItemByPosition(position);
        Intent intent = new Intent(getContext(), ActivityAddressEditor.class);

        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_CONTACT_DB_ID, item.getDbId());
        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_CONTACT_ALIAS, item.getName());
        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_CONTACT_ADDR, item.getAddress());
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        if (null != getActivity()) {
            getActivity().startActivity(intent);
        }
    }

    private void processShowQRCode(int position) {
        final DBAddrItem item = mAdapter.getAddressItemByPosition(position);
        final View v = View.inflate(getContext(), R.layout.dialog_address_item_qrcode, null);
        TextView tvAlias = v.findViewById(R.id.textView_addrbook_item_dialog_alias);
        TextView tvAddress = v.findViewById(R.id.textView_addrbook_item_dialog_address);
        ImageView ivQRCode = v.findViewById(R.id.imageView_addrbook_item_address_qrcode);
        final String address = item.getAddress();

        tvAlias.setText(item.getName());
        tvAddress.setText(item.getAddress());
        BitcoinPaymentURI uri = new BitcoinPaymentURI.Builder().address(item.getAddress()).build();
        if (uri != null) {
            Bitmap bitmap = QRCodeUtils.encodeAsBitmap(uri.getURI(),
                    ivQRCode.getDrawable().getIntrinsicWidth(),
                    ivQRCode.getDrawable().getIntrinsicHeight());
            ivQRCode.setImageBitmap(bitmap);
        }

        // copy button
        ImageView copy = v.findViewById(R.id.btc_addr_copy);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = view.getContext();
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("address", address);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }
                Toast.makeText(context, R.string.address_copied, Toast.LENGTH_SHORT).show();
            }
        });

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.btc_qr_code))
                .setView(v)
                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
