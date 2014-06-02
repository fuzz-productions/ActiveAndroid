package com.activeandroid.app;

/*
 * Copyright (C) 2010 Michael Pardo
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

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Cache;
import com.activeandroid.manager.DBManagerRuntime;

public class AAApplication extends android.app.Application {

    private static boolean mDebug = false;

    @Override
    public void onCreate() {
        super.onCreate();
        ActiveAndroid.initialize(this);
        DBManagerRuntime.restartManagers();
        if(Cache.hasMigrationExecuted()) {
            onMigrationSuccessful();
        }
    }

    public static void setDebugLogEnabled(boolean enabled) {
        mDebug = enabled;
    }

    public static boolean isDebugEnabled() {
        return mDebug;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        ActiveAndroid.dispose();
    }

    /**
     * override this method to perform any special operations when a migration takes place
     */
    protected void onMigrationSuccessful() {

    }
}