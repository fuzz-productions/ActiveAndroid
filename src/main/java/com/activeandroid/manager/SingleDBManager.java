package com.activeandroid.manager;

import android.database.DatabaseUtils;
import android.os.Handler;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.exception.DBManagerNotOnMainException;
import com.activeandroid.interfaces.ObjectRequester;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.interfaces.CollectionReceiver;
import com.activeandroid.interfaces.ObjectReceiver;
import com.activeandroid.runtime.DBBatchSaveQueue;
import com.activeandroid.runtime.DBRequest;
import com.activeandroid.runtime.DBRequestInfo;
import com.activeandroid.runtime.DBRequestQueue;
import com.activeandroid.util.Log;
import com.activeandroid.util.ReflectionUtils;
import com.activeandroid.util.SQLiteUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by andrewgrosner
 * Date: 12/26/13
 * Contributors:
 * Description: This class will provide one instance for all tables,
 * however the downside requires the class of an object when retrieving from the DB. Any {@link com.activeandroid.manager.DBManager} will extend off this class
 * and provide its own {@link com.activeandroid.runtime.DBRequestQueue}
 */
public class SingleDBManager {

    private static SingleDBManager manager;

    private DBRequestQueue mQueue;

    private String mName;

    /**
     * Creates the SingleDBManager while starting its own request queue
     * @param name
     */
    public SingleDBManager(String name){
        checkThread();
        mName = name;

        if(!getQueue().isAlive()){
            getQueue().start();
        }

        if(!getSaveQueue().isAlive()){
            getSaveQueue().start();
        }
    }

    /**
     * Returns the application's only needed DBManager.
     * Note: this manager must be created on the main thread, otherwise a {@link com.activeandroid.exception.DBManagerNotOnMainException} will be thrown
     * @return
     */
    public static SingleDBManager getSharedInstance(){
        if(manager==null){
           manager = new SingleDBManager("SingleDBManager");
        }
        return manager;
    }

    public DBRequestQueue getQueue(){
        if(mQueue==null){
            mQueue = new DBRequestQueue(mName);
        }
        return mQueue;
    }

    public DBBatchSaveQueue getSaveQueue(){
        return DBBatchSaveQueue.getSharedSaveQueue();
    }

    /**
     * Ensure manager was created in the main thread, otherwise handler will not work
     */
    protected void checkThread(){
        if(!Thread.currentThread().getName().equals("main")){
            throw new DBManagerNotOnMainException("DBManager needs to be instantiated on the main thread so Handler is on UI thread. Was on : " + Thread.currentThread().getName());
        }
    }

    /**
     * Runs all of the UI threaded requests
     */
    protected Handler mRequestHandler = new Handler();

    /**
     * Runs a request from the DB in the request queue
     * @param runnable
     */
    protected void processOnBackground(DBRequest runnable){
        getQueue().add(runnable);
    }

    /**
     * Runs UI operations in the handler
     * @param runnable
     */
    protected synchronized void processOnForeground(Runnable runnable){
        mRequestHandler.post(runnable);
    }

    public <OBJECT_CLASS extends Model> OBJECT_CLASS getObject(Class<OBJECT_CLASS> obClazz, Object object){
        try {
            return obClazz.getConstructor(object.getClass()).newInstance(object);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Adds an object to the manager's database
     * @param inObject - object of the class defined by the manager
     */
    public <OBJECT_CLASS extends Model> OBJECT_CLASS add(OBJECT_CLASS inObject){
        inObject.save();
        return inObject;
    }

    /**
     * Adds a json object to this class, however its advised you ensure that the jsonobject being passed is what you want, since there's no type checking
     * @param object
     */
    public <OBJECT_CLASS extends Model> OBJECT_CLASS add(Class<OBJECT_CLASS> obClazz, Object object){
        try {
            return add(getObject(obClazz,object));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Adds an object to the DB in the BG
     * @param jsonObject
     */
    public <OBJECT_CLASS extends Model> void addInBackground(final Class<OBJECT_CLASS> obClazz, final Object jsonObject, final ObjectReceiver<OBJECT_CLASS> objectReceiver){
        OBJECT_CLASS object = getObject(obClazz, jsonObject);
        if(objectReceiver!=null){
            objectReceiver.onObjectReceived(object);
        }
        getSaveQueue().add(object);

    }

    public <OBJECT_CLASS extends Model> void addInBackground(final Class<OBJECT_CLASS> obClazz, final Object jsonObject){
        addInBackground(obClazz, jsonObject,null);
    }


    public <OBJECT_CLASS extends Model> void addInBackground(final OBJECT_CLASS object){
        getSaveQueue().add(object);
    }

    /**
     * Adds all objects to the DB
     * @param objects
     */
    public <OBJECT_CLASS extends Model, COLLECTION_CLASS extends Collection<OBJECT_CLASS>> void addAll(COLLECTION_CLASS objects){
        ActiveAndroid.beginTransaction();
        try{
            for(OBJECT_CLASS object: objects){
                add(object);
            }
            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }
    }

    /**
     * Adds all objects from the passed object (if it has collection-like methods), may NOT be type-safe so be careful with this
     * @param array
     */
    public <OBJECT_CLASS extends Model> void addAll(Class<OBJECT_CLASS> obClazz, Object array){
        ActiveAndroid.beginTransaction();
        try{
            int count = ReflectionUtils.invokeGetSizeOfObject(array);
            for(int i = 0; i < count;i++){
                Object getObject = ReflectionUtils.invokeGetMethod(array, i);
                OBJECT_CLASS object = obClazz.getConstructor(getObject.getClass()).newInstance(getObject);
                add(object);
            }
            ActiveAndroid.setTransactionSuccessful();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            ActiveAndroid.endTransaction();
        }

    }

    public <OBJECT_CLASS extends Model> void addAllInBackground(final Class<OBJECT_CLASS> obClazz, final Object array) {
        addAllInBackground(obClazz, array, null);
    }


    public <OBJECT_CLASS extends Model> void addAllInBackground(final Class<OBJECT_CLASS> obClazz, final Object array, final CollectionReceiver<OBJECT_CLASS> collectionReceiver){
        processOnBackground(new DBRequest() {
            @Override
            public void run() {
                final List<OBJECT_CLASS> objects = new ArrayList<OBJECT_CLASS>();
                int count = ReflectionUtils.invokeGetSizeOfObject(array);
                for(int i = 0; i < count;i++){
                    Object getObject = ReflectionUtils.invokeGetMethod(array, i);
                    objects.add(getObject(obClazz, getObject));
                }

                if(collectionReceiver!=null){
                    processOnForeground(new Runnable() {
                        @Override
                        public void run() {
                            collectionReceiver.onCollectionReceived(objects);
                        }
                    });
                }

                getSaveQueue().addAll(objects);
            }
        });

    }

    public <COLLECTION_CLASS extends Collection<OBJECT_CLASS>, OBJECT_CLASS extends Model> void addAllInBackground(final COLLECTION_CLASS collection){
       getSaveQueue().addAll(collection);
    }

    /**
     * Retrieves a list of objects from the database without any threading
     * Its recommended not to call this method in the foreground thread
     * @return
     */
    public <OBJECT_CLASS extends Model> List<OBJECT_CLASS> getAll(final Class<OBJECT_CLASS> obClazz){
        return new Select().from(obClazz).execute();
    }

    /**
     * Retrieves a list of objects from the database without any threading with the sort passed
     * Its recommended not to call this method in the foreground thread
     * @param sort - valid SQLLite syntax for sort e.g. name ASC
     * @return
     */
    public <OBJECT_CLASS extends Model> List<OBJECT_CLASS> getAllWithSort(Class<OBJECT_CLASS> obClazz, String sort){
        return new Select().from(obClazz).orderBy(sort).execute();
    }

    /**
     * Fetches objects from this DB on the BG
     * @param receiver - function to call when finished that passes the list of objects that was found
     */
    public <OBJECT_CLASS extends Model> void fetchAll(final Class<OBJECT_CLASS> obClazz, final CollectionReceiver<OBJECT_CLASS> receiver){
        processOnBackground(new DBRequest(DBRequestInfo.createFetch()) {
            @Override
            public void run() {
                final List<OBJECT_CLASS> list = getAll(obClazz);
                processOnForeground(new Runnable() {
                    @Override
                    public void run() {
                        receiver.onCollectionReceived(list);
                    }
                });
            }
        });
    }

    /**
     * Fetches objects from this DB on the BG calling orderBy with the sort passed.
     * @param sort - valid SQLLite syntax for sort e.g. name ASC
     * @param receiver - function to call when finished that passes the list of objects that was found
     */
    public <OBJECT_CLASS extends Model> void fetchAllWithSort(final Class<OBJECT_CLASS> obClazz, final String sort, final CollectionReceiver<OBJECT_CLASS> receiver){
        processOnBackground(new DBRequest(DBRequestInfo.createFetch()) {
            @Override
            public void run() {
                final List<OBJECT_CLASS> list = getAllWithSort(obClazz, sort);
                processOnForeground(new Runnable() {
                    @Override
                    public void run() {
                        receiver.onCollectionReceived(list);
                    }
                });
            }
        });
    };

    public <OBJECT_CLASS extends Model> void fetchAllWithColumnValue(final Class<OBJECT_CLASS> obClazz, final Object value, final String column, final CollectionReceiver<OBJECT_CLASS> receiver){
        processOnBackground(new DBRequest(DBRequestInfo.create("fetch" , DBRequest.PRIORITY_UI)) {
            @Override
            public void run() {
                final List<OBJECT_CLASS> list = getAllWithColumnValue(obClazz, column, value);
                processOnForeground(new Runnable() {
                    @Override
                    public void run() {
                        receiver.onCollectionReceived(list);
                    }
                });
            }
        });
    }

    /**
     * This will get the where statement for this object, the amount of ids passed must match the primary key column size
     * @return
     */
    public <OBJECT_CLASS extends Model> OBJECT_CLASS getObjectById(final Class<OBJECT_CLASS> obClazz, Object...ids){
        return new Select().from(obClazz).where(SQLiteUtils.getWhereStatement(obClazz, Cache.getTableInfo(obClazz)), ids).executeSingle();
    }

    /**
     * Returns a single object with the specified column name.
     * Useful for getting objects with a specific primary key
     * @param column
     * @param uid
     * @return
     */
    public <OBJECT_CLASS extends Model> OBJECT_CLASS getObjectByColumnValue(final Class<OBJECT_CLASS> obClazz, String column, Object uid){
        return new Select().from(obClazz).where(column+" =?", uid).executeSingle();
    }

    /**
     * Returns all objects with the specified column name
     * @param column
     * @param value
     * @return
     */
    public <OBJECT_CLASS extends Model> List<OBJECT_CLASS> getAllWithColumnValue(final Class<OBJECT_CLASS> obClazz, String column, Object value){
        return new Select().from(obClazz).where(column + "= ?", value).execute();
    }

    /**
     * Gets all in a table by a group by
     * @param obClazz
     * @param groupBy
     * @param <OBJECT_CLASS>
     * @return
     */
    public <OBJECT_CLASS extends Model> List<OBJECT_CLASS> getAllWithGroupby(final Class<OBJECT_CLASS> obClazz, String groupBy){
        return new Select().from(obClazz).groupBy(groupBy).execute();
    }

    /**
     * Returns the count of rows from this DB manager's DB
     * @return
     */
    public long getCount(final Class<? extends Model> obClazz){
        return DatabaseUtils.queryNumEntries(Cache.openDatabase(), Cache.getTableName(obClazz));
    }

    /**
     * Fetches the count on the DB thread and returns it on the handler
     * @param objectReceiver
     */
    public <OBJECT_CLASS extends Model> void fetchCount(final Class<OBJECT_CLASS> obclazz, final ObjectReceiver<Long> objectReceiver){
        processOnBackground(new DBRequest(DBRequestInfo.createFetch()) {
            @Override
            public void run() {
                processOnForeground(new Runnable() {
                    @Override
                    public void run() {
                        objectReceiver.onObjectReceived(getCount(obclazz));
                    }
                });
            }
        });
    }

    /**
     * Will return the object if its within the DB, if not, it will call upon an {@link com.activeandroid.interfaces.ObjectRequester} to get the data from the API
     *
     * @param objectReceiver
     * @param uid
     * @return true if the object exists in the DB, otherwise its on a BG thread
     */
    public <OBJECT_CLASS extends Model> boolean fetchObject(final Class<OBJECT_CLASS> obClazz, final ObjectRequester<OBJECT_CLASS> requester,  final ObjectReceiver<OBJECT_CLASS> objectReceiver, final Object... uid){
        OBJECT_CLASS object = getObjectById(obClazz, uid);
        if(object==null&&requester!=null){
            processOnForeground(new Runnable() {
                @Override
                public void run() {
                    requester.requestObject(obClazz, objectReceiver, uid);
                }
            });
            return false;
        } else{
            objectReceiver.onObjectReceived(object);
            return true;
        }
    }
    /**
     * Will return the object if its within the DB, if not, it will not call an{@link com.activeandroid.interfaces.ObjectRequester}
     *
     * @param objectReceiver
     * @param uid
     * @return true if the object exists in the DB, otherwise its on a BG thread
     */
    public <OBJECT_CLASS extends Model> boolean fetchObject(final Class<OBJECT_CLASS> obClazz, final ObjectReceiver<OBJECT_CLASS> objectReceiver, final Object... uid){
       return fetchObject(obClazz, null, objectReceiver, uid);
    }


    /**
     * Deletes all objects from the specified table
     * @param obClazz
     * @param <OBJECT_CLASS>
     */
    public <OBJECT_CLASS extends Model> void deleteAll(Class<OBJECT_CLASS> obClazz){
        new Delete().from(obClazz).execute();
    }

    /**
     * Deletes objects from the db
     * @param <OBJECT_CLASS>
     */
    public<OBJECT_CLASS extends Model> void deleteAll(OBJECT_CLASS...objects) {
        ActiveAndroid.beginTransaction();
        try{
            for(OBJECT_CLASS object: objects){
                object.delete();
            }
            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }
    }

    /**
     * Deletes all objects from the collection specified
     * @param objects - the list of model objects you wish to delete
     */
    public <COLLECTION_CLASS extends Collection<OBJECT_CLASS>, OBJECT_CLASS extends Model> void deleteAll(COLLECTION_CLASS objects) {
        ActiveAndroid.beginTransaction();
        try{
            for(OBJECT_CLASS object: objects){
                object.delete();
            }
            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }
    }

    /**
     * Deletes objects from the db
     * @param finishedRunnable
     * @param dbRequestInfo
     * @param objects
     * @param <OBJECT_CLASS>
     */
    public<LIST_CLASS extends List<OBJECT_CLASS>, OBJECT_CLASS extends Model> void deleteAllInBackground(final Runnable finishedRunnable, DBRequestInfo dbRequestInfo, final LIST_CLASS objects) {
        processOnBackground(new DBRequest(dbRequestInfo) {
            @Override
            public void run() {
                deleteAll(objects);
                if(finishedRunnable!=null){
                    finishedRunnable.run();
                }
            }
        });
    }

    /**
     * Deletes objects from the db
     * @param finishedRunnable
     * @param dbRequestInfo
     * @param objects
     * @param <OBJECT_CLASS>
     */
    public<OBJECT_CLASS extends Model> void deleteAllInBackground(final Runnable finishedRunnable, DBRequestInfo dbRequestInfo, final OBJECT_CLASS...objects) {
        processOnBackground(new DBRequest(dbRequestInfo) {
            @Override
            public void run() {
                deleteAll(objects);
                if(finishedRunnable!=null){
                    finishedRunnable.run();
                }
            }
        });
    }
}
