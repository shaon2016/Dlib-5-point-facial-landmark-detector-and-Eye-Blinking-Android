package com.shaon2016.dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark;

import android.app.UiAutomation;
import android.os.Build;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

public class MyPermissionRequester {

    public static final String TAG = MyPermissionRequester.class.getSimpleName();

    public static void request(String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            UiAutomation auto = InstrumentationRegistry.getInstrumentation().getUiAutomation();
            String cmd = "pm grant " + InstrumentationRegistry.getInstrumentation().getContext().getPackageName() + " %1$s";
            String cmdTest = "pm grant " + InstrumentationRegistry.getInstrumentation().getContext().getPackageName() + " %1$s";
            String currCmd;
            for (String perm : permissions) {
                execute(String.format(cmd, perm), auto);
                execute(String.format(cmdTest, perm), auto);
            }
        }
        GrantPermissionRule.grant(permissions);
    }

    private static void execute(String currCmd, UiAutomation auto){
        Log.d(TAG, "exec cmd: " + currCmd);
        auto.executeShellCommand(currCmd);
    }
}