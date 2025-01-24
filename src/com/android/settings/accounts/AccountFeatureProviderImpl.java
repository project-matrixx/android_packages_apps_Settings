package com.android.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

public class AccountFeatureProviderImpl implements AccountFeatureProvider {
    @Override
    public String getAccountType() {
        return "com.google";
    }

    @Override
    public Account[] getAccounts(Context context) {
        return AccountManager.get(context).getAccountsByType("com.google");
    }
}
