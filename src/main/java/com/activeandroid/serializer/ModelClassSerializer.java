package com.activeandroid.serializer;

import com.activeandroid.Model;

import java.lang.reflect.Type;

/**
 * Created by andrewgrosner
 * Date: 4/3/14
 * Contributors:
 * Description:
 */
public class ModelClassSerializer extends ClassSerializer<Model> {

    public ModelClassSerializer(){

    }

    @Override
    public Class<?> getTableType() {
        return Model.class;
    }
}
