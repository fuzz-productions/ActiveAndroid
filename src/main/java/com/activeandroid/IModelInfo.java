package com.activeandroid;

/**
 * Created by andrewgrosner
 * Date: 4/3/14
 * Contributors:
 * Description:
 */
public interface IModelInfo {

    public String getId();

    public void setRowId(String rowId);

    public String getRowId();

    public boolean exists();

    public void save();

    public void delete();
}
