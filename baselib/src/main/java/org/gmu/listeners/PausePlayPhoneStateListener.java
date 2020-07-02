package org.gmu.listeners;

import android.telephony.TelephonyManager;
import org.gmu.control.Controller;

/**
 * User: ttg
 * Date: 23/11/12
 * Time: 15:06
 * To change this template use File | Settings | File Templates.
 */
public class PausePlayPhoneStateListener extends android.telephony.PhoneStateListener {

    private boolean pausedByCall = false;

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            if (Controller.getInstance().getMediaPlayer() != null) {
                if (Controller.getInstance().getMediaPlayer().isPlaying()) {
                    Controller.getInstance().getMediaPlayer().pause();
                    pausedByCall = true;
                }
            }

        } else if (state == TelephonyManager.CALL_STATE_IDLE) {
            if (Controller.getInstance().getMediaPlayer() != null) {
                if (pausedByCall) {
                    Controller.getInstance().getMediaPlayer().pause();
                    pausedByCall = false;
                }
            }
        } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            //A call is dialing, active or on hold
        }
        super.onCallStateChanged(state, incomingNumber);
    }

}
