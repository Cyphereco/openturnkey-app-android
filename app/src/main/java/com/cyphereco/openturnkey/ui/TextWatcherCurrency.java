package com.cyphereco.openturnkey.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.lang.ref.WeakReference;

public class TextWatcherCurrency implements TextWatcher {
    private final WeakReference<EditText> editTextWeakReference;
    private String current;
    public TextWatcherCurrency(EditText editText) {
        editTextWeakReference = new WeakReference<EditText>(editText);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        EditText editText = editTextWeakReference.get();
        if (editText == null) return;
        String now = s.toString();
        if (now.isEmpty()) return;
        editText.removeTextChangedListener(this);
        try {
            double parsed = Double.parseDouble(now);
            current = now;

        }
        catch (Exception e) {
            editText.setText(current);
            editText.setSelection(start);
        }
        editText.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }

}
