package com.activeandroid.serializer;

import com.activeandroid.Model;
import com.activeandroid.TableInfo;
import com.activeandroid.util.SQLiteUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by andrewgrosner
 * Date: 4/3/14
 * Contributors:
 * Description:
 */
public class ModelClassSerializer implements ClassSerializer<Model> {

    private TableInfo mTableInfo;
    private ArrayList<Field> mFields;

    public ModelClassSerializer(TableInfo tableInfo){
        mTableInfo = tableInfo;
        mFields = new ArrayList<Field>(mTableInfo.getFields());
    }

    @Override
    public Object serializeField(Model model, int position) {
        Field field = mFields.get(position);
        field.setAccessible(true);

        Object value = null;
        try {
            value = field.get(model);
        } catch (IllegalAccessException e) {

        }
        return SQLiteUtils.getTypeSerializedValue(value);
    }

    @Override
    public Object deserializeField(Model model, int position) {
        return null;
    }

    @Override
    public int getFieldCount() {
        return mFields.size();
    }

    @Override
    public String getFieldName(int position) {
        return mTableInfo.getColumnName(mFields.get(position));
    }
}
