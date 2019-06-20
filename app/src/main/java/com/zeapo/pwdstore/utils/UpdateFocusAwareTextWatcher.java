package com.zeapo.pwdstore.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import com.zeapo.pwdstore.git.GitActivity;

public class UpdateFocusAwareTextWatcher extends FocusAwareTextWatcher {

    public UpdateFocusAwareTextWatcher(EditText editText, GitActivity gitActivity) {
        super(editText, gitActivity);
    }

    protected void handleURI() {
        gitActivity.updateURI();
    }
}

