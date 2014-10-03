package com.activeandroid.runtime;

import android.util.Log;

import com.activeandroid.manager.SingleDBManager;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by andrewgrosner
 * Date: 12/11/13
 * Contributors:
 * Description: will handle concurrent requests to the DB based on priority
 */
public class DBRequestQueue {

    /**
     * Queue of requests
     */
//    private final PriorityBlockingQueue<DBRequest> mQueue;
    private ThreadPoolExecutor priorityExecutor;
    private static final Semaphore latch = new Semaphore(4);

    private ThreadPoolExecutor getExecutor(final int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<Runnable>(), new BaseThreadFactory(nThreads));
    }

    private class BaseThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        BaseThreadFactory(int nThreads) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            if (nThreads == 1) {
                namePrefix = "WRITEQUEUE";
            } else {
                namePrefix = "READQUEUE";
            }
        }

        public Thread newThread(Runnable r) {
            Thread t;
            if (namePrefix.equals("WRITEQUEUE")) {
                t = new Thread(group, r,
                        namePrefix + threadNumber.getAndIncrement(),
                        0);
            } else {
                t = new Thread(group, r,
                        namePrefix + threadNumber.getAndIncrement(),
                        0);
            }

            if (t.isDaemon())
                t.setDaemon(false);
//            if (t.getPriority() != Thread.NORM_PRIORITY)
//                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    static final String TAG = "DBREQUESTQUEUE";

    private class WriteThread implements Runnable, Comparable<WriteThread> {

        DBRequest _task;

        private WriteThread(DBRequest task) {
            _task = task;
        }

        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            long start = System.currentTimeMillis();
            Log.v(TAG, "Write Start Time " + start);

            try {
                latch.acquire(4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Log.v(TAG, "Write Wait Time " + (System.currentTimeMillis() - start));
            Log.v(TAG, "WRITING");
            Log.v(TAG, "QUEUE SIZE " + DBRequestQueue.this.priorityExecutor.getQueue().size());
            _task.run();
            Log.v(TAG, "Write Finish Time " + (System.currentTimeMillis() - start));
            latch.release(4);
        }

        public DBRequest get_task() {
            return _task;
        }


        @Override
        public int compareTo(WriteThread o1) {
            return _task.compareTo(o1.get_task());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            WriteThread that = (WriteThread) o;

            if (_task != null ? !_task.equals(that._task) : that._task != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return _task != null ? _task.hashCode() : 0;
        }
    }

    private class ReadThread implements Runnable, Comparable<ReadThread> {

        DBRequest _task;

        private ReadThread(DBRequest task) {
            _task = task;
        }

        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            try {
                latch.acquire(1);
            } catch (Throwable t) {

            }

            long start = System.currentTimeMillis();
            Log.v(TAG, "Read Start Time " + start);
            Log.v(TAG, "READING");
            Log.v(TAG, "QUEUE SIZE " + DBRequestQueue.this.priorityExecutor.getQueue().size());
            Log.v(TAG, "QUEUE COUNT " + DBRequestQueue.this.priorityExecutor.getTaskCount());
            _task.run();
            Log.v(TAG, "Read Finish Time " + (System.currentTimeMillis() - start));
            latch.release(1);
        }


        public DBRequest get_task() {
            return _task;
        }


        @Override
        public int compareTo(ReadThread o1) {
            return _task.compareTo(o1.get_task());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ReadThread that = (ReadThread) o;

            if (_task != null ? !_task.equals(that._task) : that._task != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return _task != null ? _task.hashCode() : 0;
        }

    }

    private boolean mQuit = false;

    /**
     * Creates a queue with the specified name to ID it.
     *
     * @param name
     */
    public DBRequestQueue(String name) {
        this(name, Runtime.getRuntime().availableProcessors());
//        mQueue = new PriorityBlockingQueue<DBRequest>();
    }

    public DBRequestQueue(String name, int threadCount) {
        super();
        priorityExecutor = getExecutor(threadCount);
//        mQueue = new PriorityBlockingQueue<DBRequest>();
    }

//    @Override
//    public void run() {
//        Looper.prepare();
//        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
//        DBRequest runnable;
//        while (true){
//            try{
//                runnable = mQueue.take();
//            } catch (InterruptedException e){
//                if(mQuit){
//                    synchronized (mQueue) {
//                        mQueue.clear();
//                    }
//                    return;
//                }
//                continue;
//            }
//
//            try{
//                AALog.d("DBRequestQueue + " + getName(), "Size is: " + mQueue.size() + " executing:" + runnable.getName());
//                runnable.run();
//            } catch (Throwable t){
//                throw new RuntimeException(t);
//            }
//        }
//
//    }

    public void add(DBRequest runnable) {
//        if (!mQueue.contains(runnable)) {
//            mQueue.add(runnable);
//        }
        if (this == SingleDBManager.getWriteQueue()) {
            priorityExecutor.execute(new WriteThread(runnable));
        } else {
            priorityExecutor.execute(new ReadThread(runnable));
        }

//        try {
//            synchronized (waitObject) {
//                waitObject.notify();
//            }
//        }catch (Throwable t) {
//
//        }
    }

    /**
     * Cancels the specified request.
     *
     * @param runnable
     */
    public void cancel(DBRequest runnable) {
        if (this == SingleDBManager.getWriteQueue()) {
            priorityExecutor.remove(new WriteThread(runnable));
        } else {
            priorityExecutor.remove(new ReadThread(runnable));
        }
//        if (mQueue.contains(runnable)) {
//            mQueue.remove(runnable);
//        }
    }

    /**
     * Cancels all requests by a specific tag
     *
     * @param tag
     */
    public void cancel(String tag) {
//        synchronized (mQueue){
//            Iterator<DBRequest> it = mQueue.iterator();
//            while(it.hasNext()){
//                DBRequest next = it.next();
//                if(next.getName().equals(tag)){
//                    it.remove();
//                }
//            }
//        }
    }

    /**
     * Quits this process
     */
    public void quit() {
        mQuit = true;
//        interrupt();
    }

    public boolean isAlive() {
        return true;
    }

    public void start() {

    }

    public synchronized boolean hasRequest() {
        Log.v(TAG, "ReadQueue Size " + priorityExecutor.getQueue().size());
        return priorityExecutor.getQueue().size() > 0;
    }
}
