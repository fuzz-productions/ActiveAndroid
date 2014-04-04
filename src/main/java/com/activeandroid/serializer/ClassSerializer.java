package com.activeandroid.serializer;

import android.content.ContentValues;
import android.database.Cursor;

import com.activeandroid.Cache;
import com.activeandroid.IModelInfo;
import com.activeandroid.Model;
import com.activeandroid.TableInfo;
import com.activeandroid.annotation.ForeignKey;
import com.activeandroid.annotation.PrimaryKey;
import com.activeandroid.query.Select;
import com.activeandroid.util.ReflectionUtils;
import com.activeandroid.util.SQLiteUtils;

import java.lang.reflect.Field;

/**
 * Created by andrewgrosner
 * Date: 4/3/14
 * Contributors:
 * Description: Allows objects other than just {@link com.activeandroid.Model} objects to store data in the DB
 */
public abstract class ClassSerializer<OBJECT_CLASS extends IModelInfo> {

    /**
     * Instructs the class to retrieve the field at a specific position
     * @param position
     * @return
     */
    public void serializeField(ContentValues contentValues, OBJECT_CLASS object, int position){
        TableInfo tableInfo = Cache.getTableInfo(object.getClass());
        Field field = tableInfo.getFields()[position];
        field.setAccessible(true);
        String fieldName = tableInfo.getColumnName(field);

        Class<?> fieldType = field.getType();

        if (field.isAnnotationPresent(ForeignKey.class) && ReflectionUtils.isModel(getFieldType(object, position))) {
            ForeignKey key = field.getAnnotation(ForeignKey.class);
            if (!key.name().equals("")) {
                fieldName = key.name();
            }
        }

        SQLiteUtils.applyToContentValues(contentValues, getFieldValue(object, position), fieldName, fieldType);
    }

    public Object getFieldValue(OBJECT_CLASS object, int position){
        TableInfo tableInfo = Cache.getTableInfo(object.getClass());
        Field field = tableInfo.getFields()[position];
        field.setAccessible(true);

        Object value = null;
        try {
            value = field.get(object);
            if (field.isAnnotationPresent(ForeignKey.class) && ReflectionUtils.isModel(getFieldType(object, position))) {
                value = ((Model) value).getId();
            }
        } catch (IllegalAccessException e) {

        }
        value = SQLiteUtils.getTypeSerializedValue(value);
        return value;
    }

    public Object deserializeField(Cursor cursor, OBJECT_CLASS object, int position){

        TableInfo tableInfo = Cache.getTableInfo(object.getClass());
        Field field = tableInfo.getFields()[position];
        field.setAccessible(true);

        Class fieldType = field.getType();
        String fieldName = tableInfo.getColumnName(field);

        int columnIndex = cursor.getColumnIndex(fieldName);

        if(columnIndex<0){
            return null;
        }

        boolean columnIsNull = cursor.isNull(columnIndex);
        String entityId = cursor.getString(columnIndex);

        TypeSerializer typeSerializer = Cache.getParserForType(fieldType);
        if (typeSerializer != null) {
            fieldType = typeSerializer.getSerializedType();
        }
        Object value = null;
        if (field.isAnnotationPresent(ForeignKey.class) && ReflectionUtils.isModel(fieldType)) {
            Model entity = (Model) Cache.getEntity(object.getClass(), entityId);
            if (entity == null) {
                entity = new Select().from(object.getClass()).where(SQLiteUtils.getWhereFromEntityId(object, entityId)).executeSingle();
            }

            value = entity;
        }

        // Use a deserializer if one is available
        if (typeSerializer != null && !columnIsNull) {
            value = typeSerializer.deserialize(value);
        }

        return SQLiteUtils.loadValues(cursor, value, fieldType, columnIndex);
    }

    public int getFieldCount(OBJECT_CLASS iModelInfo){
        return Cache.getTableInfo(iModelInfo.getClass()).getFields().length;
    }

    public String getPrimaryFieldName(Class<? extends IModelInfo> iModelInfo, int position){
        TableInfo tableInfo = Cache.getTableInfo(iModelInfo);
        return tableInfo.getColumnName(tableInfo.getPrimaryKeys().get(position));
    }

    public int getPrimaryFieldCount(Class<? extends IModelInfo> iModelInfo){
        return Cache.getTableInfo(iModelInfo).getPrimaryKeys().size();
    }

    public Class<?> getFieldType(OBJECT_CLASS iModelInfo, int position){
        return Cache.getTableInfo(iModelInfo.getClass()).getFields()[position].getType();
    }

    public void applyPrimaryKeys(OBJECT_CLASS model){
        TableInfo tableInfo = Cache.getTableInfo(model.getClass());
        for(Field field : tableInfo.getPrimaryKeys()){
            if(field.isAnnotationPresent(PrimaryKey.class) &&
                    field.getAnnotation(PrimaryKey.class).type().equals(PrimaryKey.Type.AUTO_INCREMENT)){
                field.setAccessible(true);
                try {
                    field.set(model, model.getRowId());
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public Class<?> getTableType(OBJECT_CLASS object){
        return Cache.getTableInfo(object.getClass()).getType();
    }

    public void setField(int position, OBJECT_CLASS iModelInfo, Object value){
        Field field = Cache.getTableInfo(iModelInfo.getClass()).getFields()[position];
        field.setAccessible(true);
        try {
            field.set(iModelInfo, value);
        } catch (IllegalAccessException e) {

        }
    }

    public abstract Class<?> getTableType();
}
