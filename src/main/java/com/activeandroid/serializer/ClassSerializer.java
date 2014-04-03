package com.activeandroid.serializer;

import com.activeandroid.IModelInfo;
import com.activeandroid.TableInfo;

/**
 * Created by andrewgrosner
 * Date: 4/3/14
 * Contributors:
 * Description: Allows objects other than just {@link com.activeandroid.Model} objects to store data in the DB
 */
public interface ClassSerializer<OBJECT_CLASS extends IModelInfo> {

    /**
     * Instructs the class to retrieve the field at a specific position
     * @param position
     * @return
     */
    public Object serializeField(OBJECT_CLASS object, int position);

    public Object deserializeField(OBJECT_CLASS object, int position);

    public int getFieldCount();

    public String getFieldName(int position);
}
