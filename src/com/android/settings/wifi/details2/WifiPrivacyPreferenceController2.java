/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.wifi.details2;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.wifi.flags.Flags;
import com.android.wifitrackerlib.WifiEntry;

import static com.android.settings.wifi.WifiConfigController.PRIVACY_PREF_INDEX_DEVICE_MAC;
import static com.android.settings.wifi.WifiConfigController.PRIVACY_PREF_INDEX_PER_CONNECTION_RANDOMIZED_MAC;
import static com.android.settings.wifi.WifiConfigController.PRIVACY_PREF_INDEX_PER_NETWORK_RANDOMIZED_MAC;

/**
 * A controller that controls whether the Wi-Fi network is mac randomized or not.
 */
public class WifiPrivacyPreferenceController2 extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_WIFI_PRIVACY = "privacy";
    private final WifiManager mWifiManager;
    private WifiEntry mWifiEntry;

    public WifiPrivacyPreferenceController2(Context context) {
        super(context, KEY_WIFI_PRIVACY);

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public void setWifiEntry(WifiEntry wifiEntry) {
        mWifiEntry = wifiEntry;
    }

    @Override
    public int getAvailabilityStatus() {
        return (!Flags.androidVWifiApi() && mWifiManager.isConnectedMacRandomizationSupported())
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        final int randomizationLevel = getRandomizationValue();
        final boolean isSelectable = mWifiEntry.canSetPrivacy();
        preference.setSelectable(isSelectable);
        listPreference.setValue(Integer.toString(randomizationLevel));
        updateSummary(listPreference, randomizationLevel);

        // If the preference cannot be selectable, display a temporary network in the summary.
        if (!isSelectable) {
            listPreference.setSummary(R.string.wifi_privacy_settings_ephemeral_summary);
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        final int privacy = Integer.parseInt((String) newValue);
        if (mWifiEntry.getPrivacy() == privacy) {
            // Prevent disconnection + reconnection if settings not changed.
            return true;
        }
        mWifiEntry.setPrivacy(privacy);

        // To activate changing, we need to reconnect network. WiFi will auto connect to
        // current network after disconnect(). Only needed when this is connected network.
        if (mWifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTED) {
            mWifiEntry.disconnect(null /* callback */);
            mWifiEntry.connect(null /* callback */);
        }
        updateSummary((ListPreference) preference, privacy);
        return true;
    }

    @VisibleForTesting
    int getRandomizationValue() {
        return mWifiEntry.getPrivacy();
    }

    /**
     * Translates a WifiEntry.Privacy value to the matching preference index value.
     *
     * @param privacy WifiEntry.Privacy value
     * @return index value of preference
     */
    public static int translateWifiEntryPrivacyToPrefValue(@WifiEntry.Privacy int privacy) {
        Log.d("WifiMacRnd", "translateMacRandomizedValueToPrefValue called from", new Throwable());
        return switch (privacy) {
            case WifiEntry.PRIVACY_RANDOMIZATION_ALWAYS -> PRIVACY_PREF_INDEX_PER_CONNECTION_RANDOMIZED_MAC;
            case WifiEntry.PRIVACY_RANDOMIZED_MAC -> PRIVACY_PREF_INDEX_PER_NETWORK_RANDOMIZED_MAC;
            case WifiEntry.PRIVACY_DEVICE_MAC -> PRIVACY_PREF_INDEX_DEVICE_MAC;
            default -> PRIVACY_PREF_INDEX_DEVICE_MAC;
        };
    }

    /**
     * Translates the pref value to WifiConfiguration.MacRandomizationSetting value
     *
     * @param prefMacRandomized is preference index value
     * @return WifiConfiguration.MacRandomizationSetting value
     */
    public static int translatePrefValueToWifiConfigSetting(int prefMacRandomized) {
        Log.d("WifiMacRnd", "translatePrefValueToWifiConfigSetting called from", new Throwable());
        return switch (prefMacRandomized) {
            case PRIVACY_PREF_INDEX_DEVICE_MAC -> WifiConfiguration.RANDOMIZATION_NONE;
            case PRIVACY_PREF_INDEX_PER_NETWORK_RANDOMIZED_MAC -> WifiConfiguration.RANDOMIZATION_PERSISTENT;
            case PRIVACY_PREF_INDEX_PER_CONNECTION_RANDOMIZED_MAC -> WifiConfiguration.RANDOMIZATION_ALWAYS;
            default -> WifiConfiguration.RANDOMIZATION_ALWAYS;
        };
    }

    private void updateSummary(ListPreference preference, int macRandomized) {
        // Translates value here to set RANDOMIZATION_PERSISTENT as first item in UI for better UX.
        final int prefMacRandomized = translateWifiEntryPrivacyToPrefValue(macRandomized);
        preference.setSummary(preference.getEntries()[prefMacRandomized]);
    }
}
