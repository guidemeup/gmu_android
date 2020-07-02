package org.gmu.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;


import org.gmu.base.R;
import org.gmu.config.Constants;
import org.gmu.control.Controller;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.dao.impl.sqlite.DBPlaceElementDAO;
import org.gmu.sync.AbstractSynchronizer;
import org.gmu.sync.GuideSynchronizer;
import org.gmu.sync.InitialPackageExtractor;
import org.gmu.sync.UpdateStateChecker;
import org.gmu.track.Tracker;
import org.gmu.utils.Utils;

import java.io.File;
import java.util.LinkedHashMap;

/**
 * User: ttg
 * Date: 22/01/13
 * Time: 9:41
 * To change this template use File | Settings | File Templates.
 */
public class GuideDownloadProgressDialog extends ProgressDialog {
    private static final String TAG = GuideSynchronizer.class.getName();
    private String currentMsg = "";
    private SynchroThread currentTask = null;
    private Activity theActivity;
    private String guideUID;

    private boolean interactive = false;
    private boolean isPreproductionGuide;
    private GuideSynchronizer.SYNCSTATE endStatus;

    private Runnable changeMessage = new Runnable() {

        public void run() {
            //Log.v(TAG, strCharacters);
            GuideDownloadProgressDialog.this.setMessage(currentMsg);
        }
    };


    public GuideDownloadProgressDialog(Activity context, String guideId, boolean interactive) {

        super(context);
        //enable mutual sync
        Controller.getInstance().getGmuContext().setUpdateInProgress(true);

        theActivity = context;

        this.setCancelable(false);
        this.setIndeterminate(false);
        this.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        //this.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        //this.setTitle(R.string.syncguide);
        this.setTitle(null);
        this.setMax(100);
        this.guideUID = guideId;


        this.show();
        setMessage(context.getString(R.string.updating));
        this.showMsg(context.getString(R.string.checkupdates), 0);
        this.interactive = interactive;
        isPreproductionGuide = (guideId.endsWith(Constants.PREPRODUCTION_GUIDE_SUFFIX) || isPreProduction(guideId));
        if (!isPreproductionGuide) {
            new CheckerThread().start();
        } else {
            //update always preproduction guides
            doUpdate();
        }


    }

    public GuideSynchronizer.SYNCSTATE getEndStatus() {
        return endStatus;
    }

    public String getGuideUID() {
        return guideUID;
    }

    private void showConfirm() {
        if (!interactive) {   //always update
            doUpdate();
            return;
        }
        //show user confirmation
        OnClickListener dialogClickListener =
                new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                doUpdate();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                GuideDownloadProgressDialog.this.dismiss();
                                break;
                        }
                    }
                };

        Builder builder = new Builder(theActivity);
        builder.setMessage(theActivity.getString(R.string.updateprompt))

                .setPositiveButton(theActivity.getString(R.string.yes), dialogClickListener)
                .setNegativeButton(theActivity.getString(R.string.no), dialogClickListener).show();

    }


    private void doUpdate() {
        currentTask = new SynchroThread(this.guideUID, theActivity);
        currentTask.start();
        this.setOnCancelListener(new OnCancelListener() {

            public void onCancel(DialogInterface dialog) {
                currentTask.cancel();


            }
        });


    }

    public void dismiss() {
        try {


            //disable download indicator
            Controller.getInstance().getGmuContext().setUpdateInProgress(false);

            super.dismiss();

        } catch (Throwable ign) {
            //ignore view not attached errors
            Log.e(TAG, "error on dismiss:", ign);
        }
    }


    /**
     * @param showAlert true if I want to show error alert prompt
     * @return true if result OK
     */
    public boolean showSyncMessages(boolean showAlert) {
        if (currentTask == null) {   //no update necessary
            return true;

        }
        endStatus = currentTask.getStatus();

        if (endStatus != GuideSynchronizer.SYNCSTATE.CANCELED_BY_USER) {
            if (endStatus == GuideSynchronizer.SYNCSTATE.UPDATE_ERROR || endStatus == GuideSynchronizer.SYNCSTATE.NO_VERSION_FOUND) {
                //no version available

                if (showAlert) {
                    showAlert();
                }

                return false;
            } else {
                if (endStatus == GuideSynchronizer.SYNCSTATE.UPDATE_WARNING) {
                    //don't show msg (connection error)
                    //Toast.makeText(context, "La guia esta desactualizada", Toast.LENGTH_LONG).show();

                }
                return true;
            }

        }
        return false;
    }

    private void showAlert() {
        theActivity.runOnUiThread(new Runnable() {
            public void run() {
                //show error message
                OnClickListener dialogClickListener =
                        new OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:

                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:

                                        break;
                                }
                            }
                        };

                Builder builder = new Builder(theActivity);
                builder.setMessage(theActivity.getString(R.string.errorupdateprompt))

                        .setPositiveButton(theActivity.getString(R.string.alert_dialog_ok), dialogClickListener).show();
            }
        });
    }

    private void showMsg(String msg, int percent) {

        GuideDownloadProgressDialog.this.setProgress(percent);
        GuideDownloadProgressDialog.this.currentMsg = percent + "% " + msg;

        //TODO: ignorar, el mensaje es fijo (demasiada info para el usuario)
        Log.i(TAG, GuideDownloadProgressDialog.this.currentMsg);
        //  ((Activity) GuideDownloadProgressDialog.this.theActivity).runOnUiThread(changeMessage);
    }


    private class CheckerThread extends Thread {
        public void run() {
            boolean updateOnError = true;
            try {
                UpdateStateChecker.UPDATESTATE state = UpdateStateChecker.checkGuide(GuideDownloadProgressDialog.this.guideUID);
                updateOnError = false;


                if (Utils.needPackageUpdate(getContext())||state == UpdateStateChecker.UPDATESTATE.NOEXISTENT || state == UpdateStateChecker.UPDATESTATE.NOTCHECKED) {
                    try {
                        //throw analytics initial download event
                        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
                        params.put("uid", GuideDownloadProgressDialog.this.guideUID);
                        Tracker.getInstance().sendEvent(GuideDownloadProgressDialog.this.guideUID, "user_action", "down_guide", params);
                    } catch (Exception ign) {
                    }
                    theActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            doUpdate();
                        }
                    });

                } else if (state == UpdateStateChecker.UPDATESTATE.NOTUPDATED) {
                    theActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            showConfirm();
                        }
                    });

                } else if (state == UpdateStateChecker.UPDATESTATE.UPDATED) {
                    GuideDownloadProgressDialog.this.dismiss();
                }
            } catch (Exception ign) {
                //
                if (updateOnError) {
                    Log.e(TAG, "Error checking status guide", ign);
                    theActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            doUpdate();
                        }
                    });
                }

            }

            return;
        }

    }


    private class SynchroThread extends Thread {
        private AbstractSynchronizer gs;
        private int lastPercent = -1;
        private String resultGuideId = null;

        public SynchroThread(String guideId, Context context) {


            resultGuideId = guideId;


            //setup msg listener
            GuideSynchronizer.OnUpdateEvent listener = new GuideSynchronizer.OnUpdateEvent() {
                public void onEvent(String msg, int percent) {
                    if (lastPercent < percent) {
                        GuideDownloadProgressDialog.this.showMsg(msg, percent);
                    }
                    lastPercent = percent;
                }


            };
            //check guide
            gs = new GuideSynchronizer(guideId, listener, context);


        }

        public void cancel() {
            gs.setStatus(GuideSynchronizer.SYNCSTATE.CANCELED_BY_USER);

        }

        public void run() {

            try {

                gs.doWork();
                //reload dao to update loaded guide
                Controller.getInstance().reloadDAO();
                //update preproduction indicator
                Controller.getInstance().getDao().setPreProductionGuide(gs.getResultGuideId(), isPreproductionGuide);


            } catch (AbstractSynchronizer.CancelSyncException ign) {
                //canceled, do nothing
                return;
            } catch (Exception ign) {
                Log.e(TAG, "Error downloading guide", ign);

            }
            GuideDownloadProgressDialog.this.guideUID = gs.getResultGuideId();
            GuideDownloadProgressDialog.this.dismiss();
            return;
        }

        public GuideSynchronizer.SYNCSTATE getStatus() {
            return gs.getStatus();

        }
    }

    private boolean isPreProduction(String guideId) {
        boolean ret = false;
        File oldFile = new File(Utils.getFilePath("/" + guideId + "/" + Constants.UPDATEFILENAME));
        if (oldFile.exists()) {
            DBPlaceElementDAO db = new DBPlaceElementDAO(theActivity, guideId, new IPlaceElementDAO.LoadListener() {
                public void onDAOLoadEvent(String msg, int percent) {
                }
            }
            );
            ret = db.isPreProductionGuide(guideId);

            db.destroy();


        }
        return ret;
    }


}
