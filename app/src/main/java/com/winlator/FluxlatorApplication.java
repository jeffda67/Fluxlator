package com.winlator;

import android.app.Application;

import com.winlator.core.DiagnosticLogger;

public class FluxlatorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DiagnosticLogger.initialize(this);
        DiagnosticLogger.installCrashHandler();
    }
}
