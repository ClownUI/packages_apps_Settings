/*
 * Copyright (C) 2018 AospExtended ROM Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.custom.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.clown.preference.SystemSettingListPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class StatusBarBattery extends SettingsPreferenceFragment
            implements Preference.OnPreferenceChangeListener  {

    private static final String KEY_STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String KEY_STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
    private static final String KEY_STATUS_BAR_BATTERY_TEXT_CHARGING = "status_bar_battery_text_charging";

    private static final int BATTERY_STYLE_PORTRAIT = 0;
    private static final int BATTERY_STYLE_TEXT = 4;
    private static final int BATTERY_STYLE_HIDDEN = 5;
    private static final int BATTERY_STYLE_IOS16 = 18;

    private static final int BATTERY_PERCENT_HIDDEN = 0;
    private static final int BATTERY_PERCENT_INSIDE = 1;
    private static final int BATTERY_PERCENT_RIGHT = 2;
    private static final int BATTERY_PERCENT_LEFT = 3;

    private SwitchPreference mBatteryTextCharging;
    private SystemSettingListPreference mBatteryPercent;
    private SystemSettingListPreference mBatteryStyle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.statusbar_battery);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        int batterystyle = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);
        int batterypercent = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0, UserHandle.USER_CURRENT);

        mBatteryStyle = (SystemSettingListPreference) findPreference(KEY_STATUS_BAR_BATTERY_STYLE);
        mBatteryStyle.setOnPreferenceChangeListener(this);

        mBatteryPercent = (SystemSettingListPreference) findPreference(KEY_STATUS_BAR_SHOW_BATTERY_PERCENT);
        mBatteryPercent.setOnPreferenceChangeListener(this);

        handleBatteryPercent(batterystyle, batterypercent);

        mBatteryTextCharging = (SwitchPreference) findPreference(KEY_STATUS_BAR_BATTERY_TEXT_CHARGING);
        mBatteryTextCharging.setEnabled(batterystyle != BATTERY_STYLE_TEXT &&
                (batterypercent == BATTERY_PERCENT_INSIDE || batterypercent == BATTERY_PERCENT_HIDDEN));
    }

    private void handleBatteryPercent(int batterystyle, int batterypercent) {
        if (batterystyle < BATTERY_STYLE_TEXT) {
            mBatteryPercent.setEntries(R.array.status_bar_battery_percent_entries);
            mBatteryPercent.setEntryValues(R.array.status_bar_battery_percent_values);;
        }
        else {
            mBatteryPercent.setEntries(R.array.status_bar_battery_percent_no_text_inside_entries);
            mBatteryPercent.setEntryValues(R.array.status_bar_battery_percent_no_text_inside_values);
            if (batterystyle == BATTERY_STYLE_IOS16) {
                if (batterypercent != BATTERY_PERCENT_INSIDE) {
                    batterypercent = BATTERY_PERCENT_INSIDE;
                    mBatteryPercent.setValueIndex(BATTERY_PERCENT_INSIDE);
                }
            } else if (batterypercent == BATTERY_PERCENT_INSIDE) {
                batterypercent = BATTERY_PERCENT_HIDDEN;
                mBatteryPercent.setValueIndex(BATTERY_PERCENT_HIDDEN);
            }
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT,
                    batterypercent, UserHandle.USER_CURRENT);
        }

        mBatteryPercent.setEnabled(
                batterystyle != BATTERY_STYLE_TEXT &&
                batterystyle != BATTERY_STYLE_HIDDEN &&
                batterystyle != BATTERY_STYLE_IOS16);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mBatteryStyle) {
            int batterystyle = Integer.parseInt((String) newValue);
            int batterypercent = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0, UserHandle.USER_CURRENT);
            handleBatteryPercent(batterystyle, batterypercent);
            mBatteryTextCharging.setEnabled(batterystyle != BATTERY_STYLE_TEXT &&
                    (batterypercent == BATTERY_PERCENT_INSIDE || batterypercent == BATTERY_PERCENT_HIDDEN));
            return true;
        } else if (preference == mBatteryPercent) {
            int batterypercent = Integer.parseInt((String) newValue);
            int batterystyle = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);
            mBatteryTextCharging.setEnabled(batterystyle != BATTERY_STYLE_TEXT &&
                    (batterypercent == BATTERY_PERCENT_INSIDE || batterypercent == BATTERY_PERCENT_HIDDEN));
            return true;
        }
        return false;
    }  
    
    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CLOWN;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                    boolean enabled) {
                final ArrayList<SearchIndexableResource> result = new ArrayList<>();
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.statusbar_battery;
                result.add(sir);
                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = super.getNonIndexableKeys(context);
                return keys;
            }
    };
}