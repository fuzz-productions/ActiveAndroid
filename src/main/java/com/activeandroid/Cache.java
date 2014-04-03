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

import java.util.Collection;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.LruCache;

import com.activeandroid.serializer.ClassSerializer;
import com.activeandroid.serializer.TypeSerializer;
import com.activeandroid.util.AALog;

public final class Cache {
	//////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC CONSTANTS
	//////////////////////////////////////////////////////////////////////////////////////

	public static final int DEFAULT_CACHE_SIZE = 1024;

	//////////////////////////////////////////////////////////////////////////////////////
	// PRIVATE MEMBERS
	//////////////////////////////////////////////////////////////////////////////////////

	private static Context sContext;

	private static ModelInfo sModelInfo;
	private static DatabaseHelper sDatabaseHelper;

	private static LruCache<String, IModelInfo> sEntities;

	private static boolean sIsInitialized = false;

	//////////////////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	//////////////////////////////////////////////////////////////////////////////////////

	private Cache() {
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	public static synchronized void initialize(Configuration configuration) {
		if (sIsInitialized) {
			AALog.v("ActiveAndroid already initialized.");
			return;
		}

		sContext = configuration.getContext();
		sModelInfo = new ModelInfo(configuration);
		sDatabaseHelper = new DatabaseHelper(configuration);

		// TODO: It would be nice to override sizeOf here and calculate the memory
		// actually used, however at this point it seems like the reflection
		// required would be too costly to be of any benefit. We'll just set a max
		// object size instead.
		sEntities = new LruCache<String, IModelInfo>(configuration.getCacheSize());

		openDatabase();

		sIsInitialized = true;

		AALog.v("ActiveAndroid initialized successfully.");
	}

	public static synchronized void clear() {
		sEntities.evictAll();
		AALog.v("Cache cleared.");
	}

	public static synchronized void dispose() {
		closeDatabase();

		sEntities = null;
		sModelInfo = null;
		sDatabaseHelper = null;

		sIsInitialized = false;

		AALog.v("ActiveAndroid disposed. Call initialize to use library.");
	}

	// Database access

	public static synchronized SQLiteDatabase openDatabase() {
		return sDatabaseHelper.getWritableDatabase();
	}

	public static synchronized void closeDatabase() {
		sDatabaseHelper.close();
	}

	// Context access

	public static Context getContext() {
		return sContext;
	}

	// Entity cache

	public static String getIdentifier(Class<? extends IModelInfo> type, String entityId) {
		return getTableName(type) + "@" + entityId;
	}

	public static String getIdentifier(IModelInfo entity) {
		return getIdentifier(entity.getClass(), entity.getId());
	}

	public static synchronized void addEntity(IModelInfo entity) {
		sEntities.put(getIdentifier(entity), entity);
	}

	public static synchronized IModelInfo getEntity(Class<? extends IModelInfo> type, String entityId) {
		return sEntities.get(getIdentifier(type, entityId));
	}

	public static synchronized void removeEntity(Model entity) {
		sEntities.remove(getIdentifier(entity));
	}

	// Model cache

	public static synchronized Collection<TableInfo> getTableInfos() {
		return sModelInfo.getTableInfos();
	}

	public static synchronized TableInfo getTableInfo(Class<? extends IModelInfo> type) {
		return sModelInfo.getTableInfo(type);
	}

	public static synchronized TypeSerializer getParserForType(Class<?> type) {
		return sModelInfo.getTypeSerializer(type);
	}

	public static synchronized String getTableName(Class<? extends IModelInfo> type) {
		return sModelInfo.getTableInfo(type).getTableName();
	}

    public static synchronized ClassSerializer getClassSerializerForType(Class<?> type){
        return sModelInfo.getClassSerializer(type);
    }
}
