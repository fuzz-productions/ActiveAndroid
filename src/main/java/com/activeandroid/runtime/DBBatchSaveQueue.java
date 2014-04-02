package com.activeandroid.runtime;

import android.os.Looper;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;
import com.activeandroid.manager.SingleDBManager;
import com.activeandroid.util.AALog;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by andrewgrosner
 * Date: 3/19/14
 * Contributors:
 * Description: This queue will bulk save items added to it when it gets access to the DB. It should only exist as one entity.
 */
public class DBBatchSaveQueue extends Thread{

    private static DBBatchSaveQueue mBatchSaveQueue;

    public static DBBatchSaveQueue getSharedSaveQueue(){
        if(mBatchSaveQueue==null){
            mBatchSaveQueue = new DBBatchSaveQueue();
        }
        return mBatchSaveQueue;
    }

    private final ArrayList<Model> mModels;

    public DBBatchSaveQueue(){
        super("DBBatchSaveQueue");

        mModels = new ArrayList<Model>();
    }

    @Override
    public void run() {
        super.run();
        Looper.prepare();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        while (true){
            final ArrayList<Model> tmpModels;
            synchronized (mModels){
                tmpModels = new ArrayList<Model>(mModels);
                mModels.clear();
            }
            if(tmpModels.size()>0) {
                //run this on the DBManager thread
                SingleDBManager.getSharedInstance().getQueue().add(new DBRequest(DBRequestInfo.create("Batch Saving")) {
                    @Override
                    public void run() {
                        long time = System.currentTimeMillis();
                        ActiveAndroid.beginTransaction();
                        try {
                            AALog.d("DBBatchSaveQueue", "Executing batch save of: " + tmpModels.size() + " on :" + Thread.currentThread().getName());
                            for (Model model: tmpModels) {
                                model.save();
                            }
                            ActiveAndroid.setTransactionSuccessful();
                        } catch (Throwable e) {
                            throw new RuntimeException(e.getCause());
                        } finally {
                            ActiveAndroid.endTransaction();
                        }
                        AALog.d("DBBatchSaveQueue", "Time took: " + (System.currentTimeMillis() -time));
                    }
                });
            }

            try {
                //sleep for one second to gather as much data as possible
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    public void add(final Model model){
        synchronized (mModels){
            mModels.add(model);
        }
    }

    public <COLLECTION_CLASS extends Collection<OBJECT_CLASS>, OBJECT_CLASS extends Model> void addAll(final COLLECTION_CLASS list){
        synchronized (mModels){
            mModels.addAll(list);
        }
    }

    public void remove(final Model model){
        synchronized (mModels){
            mModels.remove(model);
        }
    }

    public void removeAll(final Collection collection){
        synchronized (mModels){
            mModels.removeAll(collection);
        }
    }
}
