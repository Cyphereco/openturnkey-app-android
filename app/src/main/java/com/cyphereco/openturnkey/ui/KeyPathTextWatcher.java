package com.cyphereco.openturnkey.ui;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.lang.ref.WeakReference;

public class KeyPathTextWatcher implements TextWatcher {
    private final WeakReference<EditText> editTextWeakReference;
    private String current;
    private boolean isValid = false;
    public KeyPathTextWatcher(EditText editText) {
        editTextWeakReference = new WeakReference<EditText>(editText);
    }

    public boolean isKeyPathValid() {
        return isValid;
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
            long parsed = Long.parseLong(now);
            if (parsed >= 0 && parsed <= 2147483647L) {
                editText.setTextColor(Color.BLACK);
                isValid = true;
            }
            else {
                editText.setTextColor(Color.RED);
                isValid = false;
            }
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
