package com.activeandroid.manager;

import com.activeandroid.IModel;
import com.activeandroid.interfaces.CollectionReceiver;
import com.activeandroid.interfaces.ObjectReceiver;

import java.util.List;

/**
 * Created by andrewgrosner
 * Date: 11/12/13
 * Contributors:
 * Description: Provides a handy base implementation for adding and getting objects from the database.
 * Each extension of this manager corresponds to one table only.
 *
 * @param <OBJECT_CLASS> - the class of objects that represent a Model from the DB
 */
public abstract class DBManager<OBJECT_CLASS extends IModel> extends SingleDBManager {

    protected Class<OBJECT_CLASS> mObjectClass;


    /**
     * Constructs a new instance while keeping an instance of the class for its objects.
     * <br>
     * Shares the same queue with the {@link com.activeandroid.manager.SingleDBManager}
     *
     * @param mObjectClass
     */
    public DBManager(Class<OBJECT_CLASS> mObjectClass) {
        super(mObjectClass.getSimpleName(), false);
        this.mObjectClass = mObjectClass;
    }

    /**
     * Constructs a new instance while keeping an instance of the class for its objects
     *
     * @param classClass
     * @param hasOwnQueue - set this flag to true to create its own request queue
     *                    (useful for many DB operations, however each may use up a chunk  of memory)
     */
    public DBManager(Class<OBJECT_CLASS> classClass, boolean hasOwnQueue) {
        super(classClass.getSimpleName(), hasOwnQueue);
        mObjectClass = classClass;
    }

    /**
     * Override this method to have one instance of the manager accross the app
     *
     * @return
     */
    public static DBManager getSharedInstance() {
        throw new IllegalStateException("Cannot call the base implementation of this method");
    }

    /**
     * Adds a object to this class, however its advised you ensure that the object being passed is what you want, since there's no type checking
     *
     * @param object
     */
    public OBJECT_CLASS add(Object object) {
        return add(mObjectClass, object);
    }

    /**
     * Adds an object to the DB in the BG
     *
     * @param jsonObject
     */
    public void addInBackground(final Object jsonObject) {
        addInBackground(mObjectClass, jsonObject);
    }

    public void addInBackground(final Object object, final ObjectReceiver<OBJECT_CLASS> objectReceiver) {
        addInBackground(mObjectClass, object, objectReceiver);
    }

    /**
     * Adds all objects from the passed array, may NOT be type-safe so be careful with this
     *
     * @param array
     */
    public void addAll(Object array) {
        addAll(mObjectClass, array);
    }

    public void addAllInBackground(final Object array) {
        addAllInBackground(mObjectClass, array);
    }

    public void addAllInBackground(final Object array, final CollectionReceiver<OBJECT_CLASS> collectionReceiver) {
        addAllInBackground(mObjectClass, array, collectionReceiver);
    }

    /**
     * Retrieves a list of objects from the database without any threading
     * Its recommended not to call this method in the foreground thread
     *
     * @return
     */
    public List<OBJECT_CLASS> getAll() {
        return getAll(mObjectClass);
    }

    public OBJECT_CLASS getObject(Object object) {
        return DBManagerCache.constructNewInstance(object, mObjectClass);
    }

    /**
     * Retrieves a list of objects from the database without any threading with the sort passed
     * Its recommended not to call this method in the foreground thread
     *
     * @param sort - valid SQLLite syntax for sort e.g. name ASC
     * @return
     */
    public List<OBJECT_CLASS> getAllWithSort(String sort) {
        return getAllWithSort(mObjectClass, sort);
    }

    /**
     * Fetches objects from this DB on the BG
     *
     * @param receiver - function to call when finished that passes the list of objects that was found
     */
    public void fetchAll(final CollectionReceiver<OBJECT_CLASS> receiver) {
        fetchAll(mObjectClass, receiver);
    }

    /**
     * Fetches objects from this DB on the BG calling orderBy with the sort passed.
     *
     * @param sort     - valid SQLLite syntax for sort e.g. name ASC
     * @param receiver - function to call when finished that passes the list of objects that was found
     */
    public void fetchAllWithSort(final String sort, final CollectionReceiver<OBJECT_CLASS> receiver) {
        fetchAllWithSort(mObjectClass, sort, receiver);
    }

    ;

    public void fetchAllWithColumnValue(final Object value, final String column, final CollectionReceiver<OBJECT_CLASS> receiver) {
        fetchAllWithColumnValue(mObjectClass, value, column, receiver);
    }

    /**
     * This will get the where statement for this object, the amount of ids passed must match the primary key column size
     *
     * @return
     */
    public OBJECT_CLASS getObjectById(Object... ids) {
        return getObjectById(mObjectClass, ids);
    }

    /**
     * If you have multiple primary keys, provide the column names of the primary keys and the values
     * @param columnNames
     * @param ids
     * @return
     */
    public OBJECT_CLASS getObjectById(String[] columnNames, Object[] values) {
        return getObjectById(mObjectClass, columnNames, values);
    }

    /**
     * Returns a single object with the specified column name.
     * Useful for getting objects with a specific primary key
     *
     * @param column
     * @param uid
     * @return
     */
    public OBJECT_CLASS getObjectByColumnValue(String column, Object uid) {
        return getObjectByColumnValue(mObjectClass, column, uid);
    }

    /**
     * Gets all in a table by a group by
     *
     * @param groupBy
     * @return
     */
    public List<OBJECT_CLASS> getAllWithGroupby(String groupBy) {
        return getAllWithGroupby(mObjectClass, groupBy);
    }

    /**
     * Returns all objects with the specified column name
     *
     * @param column
     * @param value
     * @return
     */
    public List<OBJECT_CLASS> getAllWithColumnValue(String column, Object value) {
        return getAllWithColumnValue(mObjectClass, column, value);
    }

    /**
     * Returns the count of rows from this DB manager's DB
     *
     * @return
     */
    public long getCount() {
        return getCount(mObjectClass);
    }

    /**
     * Fetches the count on the DB thread and returns it on the handler
     *
     * @param objectReceiver
     */
    public void fetchCount(final ObjectReceiver<Long> objectReceiver) {
        fetchCount(mObjectClass, objectReceiver);
    }

    /**
     * Will return the object if its within the DB, if not, it will call upon an object requester to get the data from the API
     *
     * @param objectReceiver
     * @param uid
     * @return true if the object exists in the DB, otherwise its on a BG thread
     */
    public boolean fetchObject(final ObjectReceiver<OBJECT_CLASS> objectReceiver, final Object... uid) {
        OBJECT_CLASS object = getObjectById(uid);
        if (object == null) {
            processOnForeground(new Runnable() {
                @Override
                public void run() {
                    requestObject(objectReceiver, uid);
                }
            });
            return false;
        } else {
            objectReceiver.onObjectReceived(object);
            return true;
        }
    }

    /**
     * Implement this method to perform a request if the object does not exist in the DB
     *
     * @param objectReceiver
     * @param uid
     */
    public abstract void requestObject(final ObjectReceiver<OBJECT_CLASS> objectReceiver, final Object... uid);

    public Class<OBJECT_CLASS> getObjectClass() {
        return mObjectClass;
    }

    /**
     * Deletes all from the current object's class
     */
    public void deleteAll() {
        deleteAll(mObjectClass);
    }

}
