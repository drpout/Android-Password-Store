package com.zeapo.pwdstore.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import com.zeapo.pwdstore.git.GitActivity;

public class SplitFocusAwareTextWatcher extends FocusAwareTextWatcher {

    public SplitFocusAwareTextWatcher(EditText editText, GitActivity gitActivity) {
        super(editText, gitActivity);
    }

    protected void handleURI() {
        gitActivity.splitURI();
    }
}

