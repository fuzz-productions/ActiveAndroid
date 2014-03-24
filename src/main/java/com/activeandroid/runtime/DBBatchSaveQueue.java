package com.activeandroid.runtime;

import android.os.Looper;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    private DBRequestQueue mQueue;

    private final ArrayList<Model> mModels;

    public DBBatchSaveQueue(){
        super("DBBatchSaveQueue");

        mModels = new ArrayList<Model>();
        mQueue = new DBRequestQueue("DBBatchSaveRequestQueue");
        mQueue.start();
    }

    @Override
    public void run() {
        super.run();
        Looper.prepare();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        while (true){
            ArrayList<Model> tmpModels;
            synchronized (mModels){
                tmpModels = new ArrayList<Model>(mModels);
                mModels.clear();
            }
            if(tmpModels.size()>0) {
                ActiveAndroid.beginTransaction();
                try {
                    Log.d("DBBatchSaveQueue", "Executing batch save of: " + tmpModels.size());
                    for (Model model: tmpModels) {
                        model.save();
                    }
                    ActiveAndroid.setTransactionSuccessful();
                } catch (Throwable e) {
                    throw new RuntimeException(e.getCause());
                } finally {
                    ActiveAndroid.endTransaction();
                }
            }

            try {
                //sleep for one second to gather as much data as possible
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    public void add(final Model model){
        mQueue.add(new DBRequest() {
            @Override
            public void run() {
                synchronized (mModels){
                    mModels.add(model);
                }
            }
        });
    }

    public <COLLECTION_CLASS extends Collection<OBJECT_CLASS>, OBJECT_CLASS extends Model> void addAll(final COLLECTION_CLASS list){
        mQueue.add(new DBRequest() {
            @Override
            public void run() {
                synchronized (mModels){
                    mModels.addAll(list);
                }
            }
        });

    }

    public void remove(final Model model){
        mQueue.add(new DBRequest(DBRequestInfo.create("Removing : "+  model.toString(), DBRequest.PRIORITY_HIGH)) {
            @Override
            public void run() {
                synchronized (mModels){
                    mModels.remove(model);
                }
            }
        });
    }

    public void removeAll(final Collection collection){
        mQueue.add(new DBRequest(DBRequestInfo.create("Removing : " + collection.toString(),DBRequest.PRIORITY_HIGH)) {
            @Override
            public void run() {
                synchronized (mModels){
                    mModels.removeAll(collection);
                }
            }
        });

    }
}
