
package com.android.settings.deviceinfo;

import android.os.SystemProperties;

public class VersionUtils {
    public static String getClownVersion(){
        return SystemProperties.get("ro.build.description","");
    }
}
