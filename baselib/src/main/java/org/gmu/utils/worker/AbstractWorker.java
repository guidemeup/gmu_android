package org.gmu.utils.worker;

import android.util.Log;
import org.gmu.utils.OnWorkEventListener;

import java.util.EmptyStackException;
import java.util.Stack;

/**
 * User: ttg
 * Date: 25/04/13
 * Time: 14:54
 * class used to do task in parallel
 */
public abstract class AbstractWorker {


    protected OnWorkEventListener eventListener = null;
    private boolean canceled = false;
    private boolean someError = false;
    protected Stack workerQueue;


    protected  AbstractWorker(Stack workerQueue) {
        this.workerQueue = workerQueue;


    }

    public void setOnWorkEventListener(OnWorkEventListener listener) {
        eventListener = listener;
    }


    public void startWork(int numWorkers) throws Exception {
        Worker[] workers = new Worker[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
            workers[i] = new Worker();
            workers[i].start();

        }
        //join
        for (int i = 0; i < workers.length; i++) {
            try {
                workers[i].join();
            } catch (InterruptedException e) {
            }
        }
        if (someError) {
            throw new Exception("Queue work exception");
        }
    }

    public boolean hasSomeError()  {
        return someError;
    }



    public void cancel() {
        canceled = true;
    }


    protected abstract long doTask(Object work) throws Exception;


    private class Worker extends Thread {
        public Worker() {

        }

        public void run() {
            Object task = null;
            try {
                for (; ; ) {
                    if (canceled || someError) {
                        break;
                    }
                    task = workerQueue.pop();

                    eventListener.onWorkOk(task,doTask(task));

                }
            } catch (EmptyStackException ign) {
                //end of download queue--> OK
            } catch (Throwable error) {
                if (task != null)
                {
                    Log.e("Downloader", "Download: " + task);
                }
                Log.e("Downloader", "Error=", error);
                someError = true;
            }
        }

    }

}
