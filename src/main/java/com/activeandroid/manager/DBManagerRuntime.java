package com.activeandroid.manager;

import com.activeandroid.runtime.DBBatchSaveQueue;

import java.util.ArrayList;

/**
 * Created by andrewgrosner
 * Date: 4/17/14
 * Contributors:
 * Description:
 */
public class DBManagerRuntime {


    private static ArrayList<SingleDBManager> managers;

    static ArrayList<SingleDBManager> getManagers(){
        if(managers==null){
            managers = new ArrayList<SingleDBManager>();
        }
        return managers;
    }

    /**
     * Quits all active DBManager queues
     */
    public static void quit(){
        for(SingleDBManager manager: getManagers()){
            if(manager.hasOwnQueue()) {
                manager.getQueue().quit();
                manager.disposeQueue();
            }
        }
        DBBatchSaveQueue.getSharedSaveQueue().quit();
        DBBatchSaveQueue.disposeSharedQueue();
    }

    public static void restartManagers(){
        for(SingleDBManager manager: getManagers()){
            manager.checkQueue();
        }
    }
}
