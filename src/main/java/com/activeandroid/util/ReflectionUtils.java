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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.activeandroid.Model;
import com.activeandroid.serializer.TypeSerializer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public final class ReflectionUtils {
	//////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	public static boolean isModel(Class<?> type) {
		return isSubclassOf(type, Model.class);
	}

	public static boolean isTypeSerializer(Class<?> type) {
		return isSubclassOf(type, TypeSerializer.class);
	}

	// Meta-data

	@SuppressWarnings("unchecked")
	public static <T> T getMetaData(Context context, String name) {
		try {
			final ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(),
					PackageManager.GET_META_DATA);

			if (ai.metaData != null) {
				return (T) ai.metaData.get(name);
			}
		}
		catch (Exception e) {
			AALog.w("Couldn't find meta-data: " + name);
		}

		return null;
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	public static boolean isSubclassOf(Class<?> type, Class<?> superClass) {
		if (type.getSuperclass() != null) {
			if (type.getSuperclass().equals(superClass)) {
				return true;
			}

			return isSubclassOf(type.getSuperclass(), superClass);
		}

		return false;
	}

    public static List<Field> getAllFields(List<Field> outFields, Class<?> inClass) {
        for (Field field : inClass.getDeclaredFields()) {
            outFields.add(field);
        }
        if (inClass.getSuperclass() != null && !inClass.getSuperclass().equals(Model.class)) {
            outFields = getAllFields(outFields, inClass.getSuperclass());
        }
        return outFields;
    }

    /**
     * Finds the method from the object to get the size of its components
     * @param objectClazz
     * @return
     */
    public static int invokeGetSizeOfObject(Object inObject){
        Method method = null;
        Class objectClazz = inObject.getClass();
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
    }

    public static Object invokeGetMethod(Object inObject, int index){
        Method method = null;
        Class objectClazz = inObject.getClass();

        try {
            method = objectClazz.getDeclaredMethod("get", int.class);
        } catch (NoSuchMethodException e) {
            try {
                method = objectClazz.getDeclaredMethod("getItem", int.class);
            } catch (NoSuchMethodException e1) {

            }
        } finally {
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
    }
}