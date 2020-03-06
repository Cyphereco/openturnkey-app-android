package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
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
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.utils.QRCodeUtils;
import com.sandro.bitcoinpaymenturi.BitcoinPaymentURI;

import org.slf4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FragmentAddrbook extends FragmentExtOtkData {
    private final static String TAG = FragmentAddrbook.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private TextView mTVNoAddressMessage;
    private RecyclerView mRVAddressList;

    private OpenturnkeyDB mOtkDB = null;
    private ViewAdapterAddrbook mAdapter;
//    private FragmentAddrbookListener mListener;

    private void setAdapterListener() {
        if (null == mAdapter) {
            return;
        }
        mAdapter.setAdapterListener(new ViewAdapterAddrbook.AdapterListener() {
            @Override
            public void onDeleteAddress(int position) {
                logger.debug("onDelete the position is: " + position);
                processDeleteAddress(position);
            }

            @Override
            public void onEditAddress(int position) {
                logger.debug("onEditAddress the position is: " + position);
                processEditAddress(position);
            }

            @Override
            public void onShowQRCode(int position) {
                logger.debug("onShowQRCode the position is: " + position);
                processShowQRCode(position);
            }

            @Override
            public void onPay(int position) {
                logger.debug("onPay the position is: " + position);
                DBAddrItem item = mAdapter.getAddressItemByPosition(position);
                MainActivity.setPayToAddress(item.getAddress());
                MainActivity.navToFragment(MainActivity.FRAGMENT_PAY);
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
                        if (mOtkDB.deleteAddressbookByAlias(item.getName())) {
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

        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_CONTACT_DB_ID, item.getDbId());
        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_CONTACT_ALIAS, item.getName());
        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_CONTACT_ADDR, item.getAddress());
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
        if (mOtkDB == null) return;

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
        logger.debug("onCreateView");
        View view = inflater.inflate(R.layout.fragment_addrbook, container, false);

        mTVNoAddressMessage = view.findViewById(R.id.text_no_address);
        mRVAddressList = view.findViewById(R.id.recyclerView_address);

        if (null == mOtkDB) {
            mOtkDB = new OpenturnkeyDB(getContext());
        }

        mAdapter = new ViewAdapterAddrbook(getContext());
        this.setAdapterListener();

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRVAddressList.setLayoutManager(layoutManager);
        mRVAddressList.setAdapter(mAdapter);

        updateAddressDataset();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        logger.debug("Refresh Addresses List");
        updateAddressDataset();
    }


    public void showAddressFilter(String searchString) {
        mAdapter.getFilter().filter(searchString);
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


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_addresses, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_addresses_add) {
            Intent intent = new Intent(getContext(), ActivityAddressEditor.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
