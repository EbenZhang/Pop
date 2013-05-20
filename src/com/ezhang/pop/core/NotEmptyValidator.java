package com.ezhang.pop.core;

import android.widget.Toast;
import android.content.Context;

/**
 * Created by eben on 13-5-20.
 */
public class NotEmptyValidator {
    public static boolean NotEmpty(Context ctx, String toBeChecked, String errorMsg) {
        if (toBeChecked == null || toBeChecked.trim().length() == 0) {
            Toast.makeText(ctx, errorMsg, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
