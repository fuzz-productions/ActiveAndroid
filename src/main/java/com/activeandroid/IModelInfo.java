package com.activeandroid;

/**
 * Created by andrewgrosner
 * Date: 4/3/14
 * Contributors:
 * Description:
 */
public interface IModelInfo {

    public String getId();

    public void setRowId(long rowId);

    public long getRowId();

    public boolean exists();

    public void save();

    public void delete();

    public String getTableName();

}
