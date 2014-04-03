package com.activeandroid.serializer;

import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.annotation.ForeignKey;
import com.activeandroid.query.Select;
import com.activeandroid.util.ReflectionUtils;
import com.activeandroid.util.SQLiteUtils;

import java.lang.reflect.Field;

/**
 * Created by andrewgrosner
 * Date: 4/3/14
 * Contributors:
 * Description:
 */
public class ModelClassSerializer extends ClassSerializer<Model> {

    private static ModelClassSerializer

    public ModelClassSerializer(){

    }
    @Override
    public Object serializeField(Model model, int position) {
        Field field = model.getTableInfo().getFields()[position];
        field.setAccessible(true);

        Object value = null;
        try {
            value = field.get(model);
            if (field.isAnnotationPresent(ForeignKey.class) && ReflectionUtils.isModel(getFieldType(model, position))) {
                value = ((Model) value).getId();
            }
        } catch (IllegalAccessException e) {

        }
        return SQLiteUtils.getTypeSerializedValue(value);
    }

    @Override
    public Object deserializeField(Model model, int position, String entityId, boolean columnIsNull) {
        Field field = mFields.get(position);
        Class fieldType = getFieldType(position);
        TypeSerializer typeSerializer = Cache.getParserForType(fieldType);
        if (typeSerializer != null) {
            fieldType = typeSerializer.getSerializedType();
        }
        Object value = null;
        if (field.isAnnotationPresent(ForeignKey.class) && ReflectionUtils.isModel(fieldType)) {
            Model entity = (Model) Cache.getEntity(model.getClass(), entityId);
            if (entity == null) {
                entity = new Select().from(model.getClass()).where(SQLiteUtils.getWhereFromEntityId(model.getId(), entityId)).executeSingle();
            }

            value = entity;
        }

        if (columnIsNull) {
            field = null;
        }

        // Use a deserializer if one is available
        if (typeSerializer != null && !columnIsNull) {
            value = typeSerializer.deserialize(value);
        }

        return value;
    }

}
