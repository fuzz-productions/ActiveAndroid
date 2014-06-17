package com.activeandroid;

import android.database.Cursor;

/**
 * Author: andrewgrosner
 * Date: 6/16/14
 * Contributors: { }
 * Description: Describes the base interface for all DB classes
 */
public interface IModel {

    public void save();

    public void delete();

    /**
     * if the object exists in the DB
     * @return
     */
    public boolean exists();

    /**
     * Load the model from the cursor
     * @param cursor
     */
    public void loadFromCursor(Cursor cursor);

    /**
     * Set the id returned by the DB
     * @param id
     */
    public void setRowId(long id);

    public long getRowId();

    /**
     * Return the ID of this class specified by "(" and comma separated primary key fields
     * @return
     */
    public String getId();
}
