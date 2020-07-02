package org.gmu.sync;

/**
 * User: ttg
 * Date: 3/03/13
 * Time: 13:39
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractSynchronizer {

    public static enum SYNCSTATE {LAST_VERSION_UPDATED, CANCELED_BY_USER, OLD_VERSION, NO_VERSION_FOUND, UPDATE_WARNING, UPDATE_ERROR}

    ;
    protected SYNCSTATE status = SYNCSTATE.NO_VERSION_FOUND;

    public SYNCSTATE getStatus() {
        return status;
    }

    public void setStatus(SYNCSTATE status) {
        this.status = status;

    }

    public abstract String getResultGuideId();
    public abstract void doWork() throws Exception;

    public static final class CancelSyncException extends Exception {
    }

    public interface OnUpdateEvent
    {
        /**
         * Called some event happens
         */
        void onEvent(String msg, int percent);


    }

    protected void checkCancel() throws CancelSyncException {
        if (status == SYNCSTATE.CANCELED_BY_USER) {
            throw new CancelSyncException();
        }
    }

}
