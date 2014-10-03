package com.activeandroid.runtime;

import java.util.Date;
import java.util.UUID;

/**
 * Created by andrewgrosner
 * Date: 2/2/14
 * Contributors:
 * Description: Holds information related to a {@link com.activeandroid.runtime.DBRequest}
 */
public class DBRequestInfo {

    private String name;

    private long startTime;

    private int priority;

    private DBRequestInfo(){
    }

    /**
     * Creates with a name and priority
     * @param name
     * @param priority
     * @return
     */
    public static DBRequestInfo create(String name, int priority){
        DBRequestInfo requestInfo = new DBRequestInfo();
        requestInfo.name = name;
        requestInfo.priority = priority;
        requestInfo.startTime = new Date().getTime();
        return requestInfo;
    }

    /**
     * Creates with a name and default {@link com.activeandroid.runtime.DBRequest#PRIORITY_NORMAL}
     * @param name
     * @return
     */
    public static DBRequestInfo create(String name){
        DBRequestInfo requestInfo = new DBRequestInfo();
        requestInfo.name = name;
        requestInfo.priority = DBRequest.PRIORITY_LOW;
        requestInfo.startTime = new Date().getTime();
        return requestInfo;
    }

    /**
     * Creates with a priority and name generated from {@link java.util.UUID#randomUUID()}
     * @param priority
     * @return
     */
    public static DBRequestInfo create(int priority){
        DBRequestInfo requestInfo = new DBRequestInfo();
        requestInfo.name = UUID.randomUUID().toString();
        requestInfo.priority = priority;
        requestInfo.startTime = new Date().getTime();
        return requestInfo;
    }

    /**
     * Creates with a priority and name generated from {@link java.util.UUID#randomUUID()} and {@link com.activeandroid.runtime.DBRequest#PRIORITY_NORMAL}
     * @param priority
     * @return
     */
    public static DBRequestInfo create(){
        DBRequestInfo requestInfo = new DBRequestInfo();
        requestInfo.name = UUID.randomUUID().toString();
        requestInfo.priority = DBRequest.PRIORITY_LOW;
        requestInfo.startTime = new Date().getTime();
        return requestInfo;
    }

    /**
     * Returns a prefilled, fetch request
     * @return
     */
    public static DBRequestInfo createFetch(){
        DBRequestInfo requestInfo = new DBRequestInfo();
        requestInfo.priority = DBRequest.PRIORITY_UI;
        requestInfo.name = "fetch " + UUID.randomUUID().toString();
        requestInfo.startTime = new Date().getTime();
        return requestInfo;
    }



    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public long getStartTime() {
        return startTime;
    }
}
