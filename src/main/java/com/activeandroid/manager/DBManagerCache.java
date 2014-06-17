package com.activeandroid.manager;

import com.activeandroid.IModel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Created by andrewgrosner
 * Date: 4/7/14
 * Contributors:
 * Description:
 */
public class DBManagerCache {

    private static HashMap<Class, Method> mGetSizeMethodMap = new HashMap<Class, Method>();
    private static HashMap<Class, Method> mGetMethodMap = new HashMap<Class, Method>();
    private static HashMap<Class, Constructor> mConstructorMap = new HashMap<Class, Constructor>();

    public static int invokeGetSizeMethod(Object inObject){
        Class objectClazz = inObject.getClass();
        Method method = mGetSizeMethodMap.get(objectClazz);
        if(method==null){
            try {
                method = objectClazz.getDeclaredMethod("length", null);
            } catch (NoSuchMethodException e) {
                try {
                    method = objectClazz.getDeclaredMethod("size", null);
                } catch (NoSuchMethodException e1) {
                    try {
                        method = objectClazz.getDeclaredMethod("count", null);
                    } catch (NoSuchMethodException e2) {
                        //custom method will go here
                    }
                }
            } finally {
                mGetSizeMethodMap.put(objectClazz, method);
            }
        }

        if(method!=null){
            method.setAccessible(true);
            Integer count = 0;
            try {
                count = (Integer) method.invoke(inObject, null);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            } finally {
                return count;
            }
        } else{
            return 0;
        }
    }

    public static Object invokeGetMethod(Object inObject, int index){

        Class objectClazz = inObject.getClass();
        Method method = mGetMethodMap.get(objectClazz);
        if(method==null) {

            try {
                method = objectClazz.getDeclaredMethod("get", int.class);
            } catch (NoSuchMethodException e) {
                try {
                    method = objectClazz.getDeclaredMethod("getItem", int.class);
                } catch (NoSuchMethodException e1) {

                }
            } finally {
                mGetMethodMap.put(objectClazz, method);
            }
        }
        if(method!=null){
            method.setAccessible(true);
            Object outObject = 0;
            try {
                outObject = method.invoke(inObject, index);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            } finally {
                return outObject;
            }
        } else{
            return null;
        }
    }

    public static <OBJECT_CLASS extends IModel> OBJECT_CLASS constructNewInstance(Object inObject, Class<OBJECT_CLASS> objectClass){
        Constructor<OBJECT_CLASS> constructor = mConstructorMap.get(objectClass);
        if(constructor==null){
            try {
                constructor = objectClass.getConstructor(inObject.getClass());
                mConstructorMap.put(objectClass, constructor);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return constructor.newInstance(inObject);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
