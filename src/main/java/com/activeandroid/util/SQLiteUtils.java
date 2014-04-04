package com.activeandroid.util;

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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.text.TextUtils;

import com.activeandroid.Cache;
import com.activeandroid.IModelInfo;
import com.activeandroid.Model;
import com.activeandroid.TableInfo;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.ForeignKey;
import com.activeandroid.annotation.PrimaryKey;
import com.activeandroid.content.ContentProvider;
import com.activeandroid.exception.ClassSerializerNotFoundException;
import com.activeandroid.exception.PrimaryKeyCannotBeNullException;
import com.activeandroid.query.Select;
import com.activeandroid.serializer.ClassSerializer;
import com.activeandroid.serializer.TypeSerializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class SQLiteUtils {
	//////////////////////////////////////////////////////////////////////////////////////
	// ENUMERATIONS
	//////////////////////////////////////////////////////////////////////////////////////

	public enum SQLiteType {
		INTEGER, REAL, TEXT, BLOB
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC CONSTANTS
	//////////////////////////////////////////////////////////////////////////////////////

	public static final boolean FOREIGN_KEYS_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;

	//////////////////////////////////////////////////////////////////////////////////////
	// PRIVATE CONTSANTS
	//////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("serial")
	private static final HashMap<Class<?>, SQLiteType> TYPE_MAP = new HashMap<Class<?>, SQLiteType>() {
		{
			put(byte.class, SQLiteType.INTEGER);
			put(short.class, SQLiteType.INTEGER);
			put(int.class, SQLiteType.INTEGER);
			put(long.class, SQLiteType.INTEGER);
			put(float.class, SQLiteType.REAL);
			put(double.class, SQLiteType.REAL);
			put(boolean.class, SQLiteType.INTEGER);
			put(char.class, SQLiteType.TEXT);
			put(byte[].class, SQLiteType.BLOB);
			put(Byte.class, SQLiteType.INTEGER);
			put(Short.class, SQLiteType.INTEGER);
			put(Integer.class, SQLiteType.INTEGER);
			put(Long.class, SQLiteType.INTEGER);
			put(Float.class, SQLiteType.REAL);
			put(Double.class, SQLiteType.REAL);
			put(Boolean.class, SQLiteType.INTEGER);
			put(Character.class, SQLiteType.TEXT);
			put(String.class, SQLiteType.TEXT);
			put(Byte[].class, SQLiteType.BLOB);
		}
	};

	//////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	public static void execSql(String sql) {
		Cache.openDatabase().execSQL(sql);
	}

	public static void execSql(String sql, Object[] bindArgs) {
		Cache.openDatabase().execSQL(sql, bindArgs);
	}

	public static <T extends IModelInfo> List<T> rawQuery(Class<? extends IModelInfo> type, String sql, String[] selectionArgs) {
		Cursor cursor = Cache.openDatabase().rawQuery(sql, selectionArgs);
		List<T> entities = processCursor(type, cursor);
		cursor.close();

		return entities;
	}

	public static <T extends IModelInfo> T rawQuerySingle(Class<? extends IModelInfo> type, String sql, String[] selectionArgs) {
		List<T> entities = rawQuery(type, sql, selectionArgs);

		if (entities.size() > 0) {
			return entities.get(0);
		}

		return null;
	}

	// Database creation

	public static String createTableDefinition(TableInfo tableInfo) {
		final ArrayList<String> definitions = new ArrayList<String>();

		for (Field field : tableInfo.getFields()) {
			String definition = createColumnDefinition(tableInfo, field);
			if (!TextUtils.isEmpty(definition)) {
				definitions.add(definition);
			}
		}

        List<Field> primaryColumns = tableInfo.getPrimaryKeys();
        List<Field> foreignColumns = tableInfo.getForeignKeys();
        if(!primaryColumns.isEmpty()){
            StringBuilder builder = new StringBuilder("PRIMARY KEY(");


            for(int i  =0 ; i< primaryColumns.size(); i++){
                builder.append(tableInfo.getColumnName(primaryColumns.get(i)));
                if(i< primaryColumns.size()-1){
                    builder.append(", ");
                }
            }

            builder.append(")");

            definitions.add(builder.toString());
        }

        for(int i = 0; i < foreignColumns.size(); i++){
            final Field column = foreignColumns.get(i);
            ForeignKey foreignKey = column.getAnnotation(ForeignKey.class);

            StringBuilder forDef = new StringBuilder("FOREIGN KEY(");
            forDef.append(tableInfo.getColumnName(column)).append(") REFERENCES ")
                    .append(Cache.getTableName((Class<? extends Model>) column.getType()))
                    .append("(").append(foreignKey.foreignColumn()).append(")");

            definitions.add(forDef.toString());
        }


		return String.format("CREATE TABLE IF NOT EXISTS %s (%s);", tableInfo.getTableName(),
				TextUtils.join(", ", definitions));
	}

	@SuppressWarnings("unchecked")
	public static String createColumnDefinition(TableInfo tableInfo, Field field) {
		StringBuilder definition = new StringBuilder();

		Class<?> type = field.getType();
		final String name = tableInfo.getColumnName(field);
		final TypeSerializer typeSerializer = Cache.getParserForType(field.getType());
		final Column column = field.getAnnotation(Column.class);

		if (typeSerializer != null) {
			type = typeSerializer.getSerializedType();
		}

		if (TYPE_MAP.containsKey(type)) {
			definition.append(name);
			definition.append(" ");
			definition.append(TYPE_MAP.get(type).toString());
		}
		else if (ReflectionUtils.isModel(type)) {
			definition.append(name);
			definition.append(" ");
			definition.append(SQLiteType.INTEGER.toString());
		}
		else if (ReflectionUtils.isSubclassOf(type, Enum.class)) {
			definition.append(name);
			definition.append(" ");
			definition.append(SQLiteType.TEXT.toString());
		}

		if (!TextUtils.isEmpty(definition)) {
			if (column.length() > -1) {
				definition.append("(");
				definition.append(column.length());
				definition.append(")");
			}

			if (field.isAnnotationPresent(PrimaryKey.class)) {
                PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
                if(primaryKey.type().equals(PrimaryKey.Type.AUTO_INCREMENT)){
				    definition.append(" PRIMARY KEY AUTOINCREMENT");
                }
			}

			if (column.notNull()) {
				definition.append(" NOT NULL ON CONFLICT ");
				definition.append(column.onNullConflict().toString());
			}

			if (column.unique()) {
				definition.append(" UNIQUE ON CONFLICT ");
				definition.append(column.onUniqueConflict().toString());
			}
		}
		else {
			AALog.e("No type mapping for: " + type.toString());
		}

		return definition.toString();
	}

	@SuppressWarnings("unchecked")
	public static <T extends IModelInfo> List<T> processCursor(Class<? extends IModelInfo> type, Cursor cursor) {
		final List<T> entities = new ArrayList<T>();

		try {
			Constructor<?> entityConstructor = type.getConstructor();

            //enable private constructors
            entityConstructor.setAccessible(true);

			if (cursor.moveToFirst()) {
				do {
					IModelInfo entity = (T) entityConstructor.newInstance();
                    loadFromCursor(cursor, entity, Cache.getClassSerializerForType(entity.getClass()));
					entities.add((T) entity);
				}
				while (cursor.moveToNext());
			}

		}
        catch (IllegalArgumentException i){
            throw new RuntimeException("Default constructor for: " + type.getName() + " was not found.");
        } catch (Exception e) {
			AALog.e("Failed to process cursor.", e);
		}

		return entities;
	}

    /**
     * Returns the where statement with primary keys with no values
     * @param tableInfo
     * @return
     */
    public static String getWhereStatementWithoutValues(ClassSerializer classSerializer, Class<? extends IModelInfo> iModelInfo){
        if(classSerializer==null){
            throw new ClassSerializerNotFoundException("You Must Define a Class Serializer for a class that does not directly inherit from Model for: " + Cache.getTableName(iModelInfo));
        }

        int count = classSerializer.getPrimaryFieldCount(iModelInfo);
        final StringBuilder where = new StringBuilder();
        for(int i = 0 ; i < count; i++){
            where.append(classSerializer.getPrimaryFieldName(iModelInfo,i));
            where.append("=?");

            if(i < count-1){
                where.append(" AND ");
            }
        }

        String sql = where.toString();

        return sql;
    }

    /**
     * Returns the where statement with primary keys and values filled in
     * @param model
     * @param tableInfo
     * @return
     */
    public static String getWhereStatement(ClassSerializer classSerializer, IModelInfo model){
        if(classSerializer==null){
            throw new ClassSerializerNotFoundException("You Must Define a Class Serializer for a class that does not directly inherit from Model for: " + Cache.getTableName(model.getClass()));
        }

        int count = classSerializer.getPrimaryFieldCount(model.getModelClass());

        final StringBuilder where = new StringBuilder();
        for(int i = 0 ; i < count; i++){
            try {
                String actualName = classSerializer.getActualPrimaryFieldName(model.getModelClass(), i);
                String primaryName = classSerializer.getPrimaryFieldName(model.getModelClass(), i);
                Object object = classSerializer.getPrimaryFieldValue(model, i, actualName);
                where.append(primaryName);
                where.append("=");

                if (object == null) {
                    throw new PrimaryKeyCannotBeNullException("The primary key: " + actualName + "from " + model.getTableName() + " cannot be null.");
                } else if (object instanceof Number) {
                    where.append(object.toString());
                } else {
                    String escaped = DatabaseUtils.sqlEscapeString(object.toString());

                    where.append(escaped);
                }

                if (i < count - 1) {
                    where.append(" AND ");
                }
            } catch (Throwable e){
                throw new RuntimeException(e);
            }
        }

        return where.toString();
    }

    public static String getWhereFromEntityId(IModelInfo model, String entityId){
        String[] primaries = entityId.split(",");
        String whereString = getWhereStatementWithoutValues(Cache.getClassSerializerForType(model.getClass()), model.getClass());

        List<Field> fields = new ArrayList<Field>();
        fields = ReflectionUtils.getAllFields(fields, model.getClass());

        ArrayList<Field> primaryColumn = new ArrayList<Field>();
        for(Field field : fields){
            if(field.isAnnotationPresent(PrimaryKey.class)){
                primaryColumn.add(field);
            }
        }

        for(int i = 0; i < primaries.length; i++){
            final Field field = primaryColumn.get(i);
            field.setAccessible(true);
            try {
                if(field.getType().isAssignableFrom(String.class)){
                    String escaped = DatabaseUtils.sqlEscapeString(primaries[i]);
                    whereString = whereString.replaceFirst("\\?", escaped);
                } else {
                    whereString = whereString.replaceFirst("\\?", primaries[i]);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        return whereString;
    }

    /**
     * Saves a model to the DB
     * @param model
     */
    public static void save(ClassSerializer serializer, IModelInfo model){
        final SQLiteDatabase db = Cache.openDatabase();
        final ContentValues values = new ContentValues();
        int count = serializer.getFieldCount(model);

        for (int i =  0; i  < count; i++) {
            serializer.serializeField(values, model, i);
        }

        if(values.size()>0) {
            if (!model.exists()) {
                model.setRowId(db.insert(model.getTableName(), null, values));
                serializer.applyPrimaryKeys(model);
            } else {
                model.setRowId(db.update(model.getTableName(), values, SQLiteUtils.getWhereStatementWithoutValues(serializer, model.getModelClass()), null));
            }

            Cache.getContext().getContentResolver()
                    .notifyChange(ContentProvider.createUri(serializer.getTableType(model), model.getId()), null);
        } else{
            AALog.e("SQLiteUtils", "IModelInfo: " + model.getClass() + " for class serializer: " + serializer.getClass() + " had empty content values");
        }
    }

    public static final void loadFromCursor(Cursor cursor, IModelInfo model, ClassSerializer classSerializer){

        int count = classSerializer.getFieldCount(model);

        for (int i = 0; i < count; i++) {
            try {
                Object value = classSerializer.deserializeField(cursor, model, i);
                // Set the field name
                if (value != null) {
                    classSerializer.setField(i, model, value);
                }
            }
            catch (IllegalArgumentException e) {
                AALog.e(e.getClass().getName(), e);
            }
            catch (SecurityException e) {
                AALog.e(e.getClass().getName(), e);
            }
        }

        if (model.getId() != null) {
            Cache.addEntity(model);
        }
    }

    /**
     * Check whether the given model exists
     * @param model
     * @return
     */
    public static <OBJECT_CLASS extends IModelInfo> boolean exists(Class<OBJECT_CLASS> modelClass, OBJECT_CLASS model){
        return (new Select().from(modelClass).where(SQLiteUtils.getWhereStatement(Cache.getClassSerializerForType(modelClass), model)).executeSingle())!=null;
    }

    /**
     * Deletes a model
     * @param model
     */
    public static void delete(IModelInfo model){
        Cache.openDatabase().delete(Cache.getTableName(model.getClass()), SQLiteUtils.getWhereStatement(Cache.getClassSerializerForType(model.getClass()), model), null);
        Cache.removeEntity(model);

        Cache.getContext().getContentResolver()
                .notifyChange(ContentProvider.createUri(Cache.getTableInfo(model.getClass()).getType(), model.getId()), null);
    }

    /**
     * Attempts to use a {@link com.activeandroid.serializer.TypeSerializer} to convert the value from the DB into the object's value
     * @param outValue - the object that gets converted
     * @return
     */
    public static Object getTypeSerializedValue(Object outValue){
        if (outValue != null) {
            Class fieldType = outValue.getClass();

            final TypeSerializer typeSerializer = Cache.getParserForType(fieldType);
            if (typeSerializer != null) {
                // serialize data
                outValue = typeSerializer.serialize(outValue);
                // set new object type
                if (outValue != null) {
                    fieldType = outValue.getClass();
                    // check that the serializer returned what it promised
                    if (!fieldType.equals(typeSerializer.getSerializedType())) {
                        AALog.w(String.format("TypeSerializer returned wrong type: expected a %s but got a %s",
                                typeSerializer.getSerializedType(), fieldType));
                    }
                }
            }
        }
        return outValue;
    }

    public static void applyToContentValues(ContentValues values, Object value, String fieldName, Class<?> fieldType){
        try {
            // TODO: Find a smarter way to do this? This if block is necessary because we
            // can't know the type until runtime.
            if (value == null) {
                values.putNull(fieldName);
            }
            else if (fieldType.equals(Byte.class) || fieldType.equals(byte.class)) {
                values.put(fieldName, ((Number) value).byteValue());
            }
            else if (fieldType.equals(Short.class) || fieldType.equals(short.class)) {
                values.put(fieldName, ((Number) value).shortValue());
            }
            else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
                values.put(fieldName, ((Number) value).intValue());
            }
            else if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
                values.put(fieldName, ((Number) value).longValue());
            }
            else if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
                values.put(fieldName, ((Number) value).floatValue());
            }
            else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
                values.put(fieldName, ((Number) value).doubleValue());
            }
            else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
                values.put(fieldName, (Boolean) value);
            }
            else if (fieldType.equals(Character.class) || fieldType.equals(char.class)) {
                values.put(fieldName, value.toString());
            }
            else if (fieldType.equals(String.class)) {
                values.put(fieldName, value.toString());
            }
            else if (fieldType.equals(Byte[].class) || fieldType.equals(byte[].class)) {
                values.put(fieldName, (byte[]) value);
            }
            else if (ReflectionUtils.isSubclassOf(fieldType, Enum.class)) {
                values.put(fieldName, ((Enum<?>) value).name());
            }
        }
        catch (IllegalArgumentException e) {
            AALog.e(e.getClass().getName(), e);
        }
    }

    public static Object loadValues(Cursor cursor, Object value, Class<?> fieldType, int columnIndex){
        if (fieldType.equals(Byte.class) || fieldType.equals(byte.class)) {
            value = cursor.getInt(columnIndex);
        }
        else if (fieldType.equals(Short.class) || fieldType.equals(short.class)) {
            value = cursor.getInt(columnIndex);
        }
        else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
            value = cursor.getInt(columnIndex);
        }
        else if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
            value = cursor.getLong(columnIndex);
        }
        else if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
            value = cursor.getFloat(columnIndex);
        }
        else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
            value = cursor.getDouble(columnIndex);
        }
        else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
            value = cursor.getInt(columnIndex) != 0;
        }
        else if (fieldType.equals(Character.class) || fieldType.equals(char.class)) {
            value = cursor.getString(columnIndex).charAt(0);
        }
        else if (fieldType.equals(String.class)) {
            value = cursor.getString(columnIndex);
        }
        else if (fieldType.equals(Byte[].class) || fieldType.equals(byte[].class)) {
            value = cursor.getBlob(columnIndex);
        }
        else if (ReflectionUtils.isSubclassOf(fieldType, Enum.class)) {
            @SuppressWarnings("rawtypes")
            final Class<? extends Enum> enumType = (Class<? extends Enum>) fieldType;
            value = Enum.valueOf(enumType, cursor.getString(columnIndex));
        }
        return value;
    }
}
