package com.zeapo.pwdstore.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import com.zeapo.pwdstore.git.GitActivity;

public abstract class FocusAwareTextWatcher implements TextWatcher {
    protected EditText editText;
    protected GitActivity gitActivity;

    public FocusAwareTextWatcher(EditText editText, GitActivity gitActivity) {
        this.editText = editText;
        this.gitActivity = gitActivity;
    }

    protected abstract void handleURI();

    @Override
    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        if (editText.isFocused())
            handleURI();
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }

}

