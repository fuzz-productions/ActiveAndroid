package com.activeandroid;

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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.LruCache;

import com.activeandroid.serializer.TypeSerializer;
import com.activeandroid.util.AALog;

import java.util.Collection;

public final class Cache {
	//////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC CONSTANTS
	//////////////////////////////////////////////////////////////////////////////////////

	public static final int DEFAULT_CACHE_SIZE = 1024;

	//////////////////////////////////////////////////////////////////////////////////////
	// PRIVATE MEMBERS
	//////////////////////////////////////////////////////////////////////////////////////

	private static Context sContext;

	private static ModelInfo sIModelInfo;
	private static DatabaseHelper sDatabaseHelper;

	private static LruCache<String, IModel> sEntities;

	private static boolean sIsInitialized = false;


    private static final Object SYN_OBJECT = new Object();

    /**
     * boolean to tell us whether a migration has successfully been executed
     */
    private static boolean migrationExecuted;

	//////////////////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	//////////////////////////////////////////////////////////////////////////////////////

	private Cache() {
	}


    public static boolean hasMigrationExecuted() {
        return migrationExecuted;
    }

    public static void setMigrationExecuted(boolean executed) {
        migrationExecuted = executed;
    }

	//////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	public static void initialize(Configuration configuration) {
		if (sIsInitialized) {
			AALog.v("ActiveAndroid already initialized.");
			return;
		}

		sContext = configuration.getContext();
		sIModelInfo = new ModelInfo(configuration);
		sDatabaseHelper = new DatabaseHelper(configuration);

        synchronized (SYN_OBJECT) {
            // TODO: It would be nice to override sizeOf here and calculate the memory
            // actually used, however at this point it seems like the reflection
            // required would be too costly to be of any benefit. We'll just set a max
            // object size instead.
            sEntities = new LruCache<String, IModel>(configuration.getCacheSize());
        }

		openDatabase();

		sIsInitialized = true;

		AALog.v("ActiveAndroid initialized successfully.");
	}

	public static void clear() {
        synchronized (SYN_OBJECT) {
            sEntities.evictAll();
        }
		AALog.v("Cache cleared.");
	}

	public static void dispose() {
		closeDatabase();

        synchronized (SYN_OBJECT) {
            sEntities = null;
        }
		sIModelInfo = null;
		sDatabaseHelper = null;

		sIsInitialized = false;

		AALog.v("ActiveAndroid disposed. Call initialize to use library.");
	}

	// Database access

	public static SQLiteDatabase openDatabase() {
		return sDatabaseHelper.getWritableDatabase();
	}

	public static void closeDatabase() {
		sDatabaseHelper.close();
	}

	// Context access

	public static Context getContext() {
		return sContext;
	}

	// Entity cache

	public static String getIdentifier(Class<? extends IModel> type, String entityId) {
		return getTableName(type) + "@" + entityId;
	}

	public static String getIdentifier(IModel entity) {
		return getIdentifier(entity.getClass(), entity.getId());
	}

	public static void addEntity(IModel entity) {
        synchronized (SYN_OBJECT) {
            sEntities.put(getIdentifier(entity), entity);
        }
	}

	public static IModel getEntity(Class<? extends IModel> type, String entityId) {
        synchronized (SYN_OBJECT) {
            return sEntities.get(getIdentifier(type, entityId));
        }
	}

	public static void removeEntity(IModel entity) {
        synchronized (SYN_OBJECT) {
            sEntities.remove(getIdentifier(entity));
        }
	}

	// IModel cache

	public static Collection<TableInfo> getTableInfos() {
		return sIModelInfo.getTableInfos();
	}

	public static TableInfo getTableInfo(Class<? extends IModel> type) {
		return sIModelInfo.getTableInfo(type);
	}

	public static TypeSerializer getParserForType(Class<?> type) {
		return sIModelInfo.getTypeSerializer(type);
	}

	public static String getTableName(Class<? extends IModel> type) {
		return sIModelInfo.getTableInfo(type).getTableName();
	}
}
