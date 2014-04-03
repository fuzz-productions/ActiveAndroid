package com.activeandroid.serializer;

import com.activeandroid.Cache;
import com.activeandroid.IModelInfo;
import com.activeandroid.Model;
import com.activeandroid.TableInfo;
import com.activeandroid.annotation.ForeignKey;
import com.activeandroid.annotation.PrimaryKey;
import com.activeandroid.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

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
    public abstract Object serializeField(OBJECT_CLASS object, int position);

    public abstract Object deserializeField(OBJECT_CLASS object, int position, String entityId, boolean columnIsNull);

    public int getFieldCount(OBJECT_CLASS iModelInfo){
        return Cache.getTableInfo(iModelInfo.getClass()).getFields().length;
    }

    public String getFieldName(OBJECT_CLASS iModelInfo, int position){
        TableInfo tableInfo = Cache.getTableInfo(iModelInfo.getClass());
        Field field = tableInfo.getFields()[position];
        String fieldName = tableInfo.getColumnName(field);
        if (field.isAnnotationPresent(ForeignKey.class) && ReflectionUtils.isModel(getFieldType(position))) {
            ForeignKey key = field.getAnnotation(ForeignKey.class);
            if (!key.name().equals("")) {
                fieldName = field.getAnnotation(ForeignKey.class).name();
            }
        }

        return fieldName;
    }

    public String getPrimaryFieldName(IModelInfo iModelInfo, int position){
        return Cache.getTableInfo(iModelInfo.getClass()).getPrimaryKeys().get(position).getName();
    }

    public int getPrimaryFieldCount(IModelInfo iModelInfo){
        return Cache.getTableInfo(iModelInfo.getClass()).getPrimaryKeys().size();
    }

    public Class<?> getFieldType(IModelInfo iModelInfo, int position){
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

    public Class<OBJECT_CLASS> getTableType(OBJECT_CLASS object){
        return (Class<OBJECT_CLASS>) Cache.getTableInfo(object.getClass()).getType();
    }

    public void setField(int position, OBJECT_CLASS iModelInfo, Object value){
        Field field = Cache.getTableInfo(iModelInfo.getClass()).getFields()[position];
        field.setAccessible(true);
        try {
            field.set(iModelInfo, value);
        } catch (IllegalAccessException e) {

        }
    }
}
