package com.activeandroid.runtime;


import com.activeandroid.manager.SingleDBManager;
import com.activeandroid.util.AALog;

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

    public interface GetTask {
        DBRequest getTask();
    }

    /**
     * Queue of requests
     */
//    private final PriorityBlockingQueue<DBRequest> mQueue;
    private ThreadPoolExecutor priorityExecutor;
    private static final Semaphore latch = new Semaphore(1, false);

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
            return t;
        }
    }

    static final String TAG = "DBREQUESTQUEUE";

    private class WriteThread implements Runnable, Comparable<GetTask>, GetTask {

        DBRequest _task;

        private WriteThread(DBRequest task) {
            _task = task;
        }

        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            long start = System.currentTimeMillis();
            AALog.v(TAG, "Write Start Time " + start);

            try {
                latch.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            AALog.v(TAG, "Write Wait Time " + (System.currentTimeMillis() - start));
            AALog.v(TAG, "WRITING");
            AALog.v(TAG, "QUEUE SIZE " + DBRequestQueue.this.priorityExecutor.getQueue().size());
            _task.run();
            AALog.v(TAG, "Write Finish Time " + (System.currentTimeMillis() - start));
            latch.release();
        }

        @Override
        public DBRequest getTask() {
            return _task;
        }


        @Override
        public int compareTo(GetTask o1) {
            return _task.compareTo(o1.getTask());
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

    private class ReadThread implements Runnable, Comparable<GetTask>, GetTask {

        DBRequest _task;

        private ReadThread(DBRequest task) {
            _task = task;
        }

        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            try {
                latch.tryAcquire();
            } catch (Throwable t) {

            }


            long start = System.currentTimeMillis();
            AALog.v(TAG, "Read Start Time " + start);
            AALog.v(TAG, "READING");
            AALog.v(TAG, "QUEUE SIZE " + DBRequestQueue.this.priorityExecutor.getQueue().size());
            AALog.v(TAG, "QUEUE COUNT " + DBRequestQueue.this.priorityExecutor.getTaskCount());
            _task.run();
            AALog.v(TAG, "Read Finish Time " + (System.currentTimeMillis() - start));
            latch.release();
        }


        @Override
        public DBRequest getTask() {
            return _task;
        }


        @Override
        public int compareTo(GetTask o1) {
            return _task.compareTo(o1.getTask());
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
    }

    public DBRequestQueue(String name, int threadCount) {
        super();
        priorityExecutor = getExecutor(threadCount);
    }

    public void add(DBRequest runnable) {
        if (this == SingleDBManager.getWriteQueue()) {
            priorityExecutor.execute(new WriteThread(runnable));
        } else {
            priorityExecutor.execute(new ReadThread(runnable));
            latch.tryAcquire();
        }
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
    }

    /**
     * Cancels all requests by a specific tag
     *
     * @param tag
     */
    public void cancel(String tag) {

    }

    /**
     * Quits this process
     */
    @Deprecated
    public void quit() {
        mQuit = true;
    }

    @Deprecated
    public boolean isAlive() {
        return true;
    }

    @Deprecated
    public void start() {

    }

    public synchronized boolean hasRequest() {
        AALog.v(TAG, "ReadQueue Size " + priorityExecutor.getQueue().size());
        return priorityExecutor.getQueue().size() > 0;
    }
}
