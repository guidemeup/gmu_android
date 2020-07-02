package org.gmu.fragments;


import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.gmu.base.R;
import org.gmu.control.Controller;
import org.gmu.pojo.PlaceElement;
import org.gmu.utils.Utils;


/**
 * Created by IntelliJ IDEA.
 * User: acasquero
 * Date: 9/11/12
 * Time: 8:53
 * To change this template use File | Settings | File Templates.
 */
public class GuidePlayerFragment extends GMUBaseFragment {

    private static final int SEEKTASKPERIOD = 300;
    private static final int SEEKRESOLUTION_MS = 1000;
    private int lastSeekPosition = 0;
    private SeekBar mSeekBarGlobal;
    private SeekTaskUpdater updater;
    private boolean updateSeek = true;
    private TextView currentPos;
    private TextView duration;
    private ImageView mPlayAndPause = null;

    private TextView title = null;
    private ImageView icon = null;
    private ImageView next = null;
    private ImageView prev = null;
    private String playingUid = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LinearLayout main = (LinearLayout) inflater.inflate(R.layout.fragment_guideplayer,
                container, false);


        currentPos = (TextView) main.findViewById(R.id.currentpos);
        duration = (TextView) main.findViewById(R.id.total);
        title = (TextView) main.findViewById(R.id.title);
        icon = (ImageView) main.findViewById(R.id.categoryicon);
        //play/pause button
        mPlayAndPause = (ImageView) main.findViewById(R.id.play);
        mPlayAndPause.setOnClickListener(mPlayPauseOnclickListener);

        //cancel play button
        ((ImageView) main.findViewById(R.id.cancel)).setOnClickListener(mCloseOnclickListener);

        //info button
        ((ImageView) main.findViewById(R.id.about)).setOnClickListener(mInfoOnclickListener);

        //seek bar
        mSeekBarGlobal = (SeekBar) main.findViewById(R.id.seek);
        mSeekBarGlobal.setOnSeekBarChangeListener(mOnSeekBarChanger);

        //next
        next = (ImageView) main.findViewById(R.id.next);
        next.setOnClickListener(nextOnclickListener);
        prev = (ImageView) main.findViewById(R.id.prev);
        prev.setOnClickListener(prevOnclickListener);
        //seek bar updater from mediaplayer

        updater = new SeekTaskUpdater();
        updater.start();


        return main;
    }

    public void show() {


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        updater.interrupt();

    }


    private class SeekTaskUpdater extends Thread {


        @Override
        public void run() {
            try {
                for (; ; ) {

                    updateSeek();
                    Thread.sleep(SEEKTASKPERIOD);
                }
            } catch (Exception ign) {

            }

        }
    }

    private boolean isPlaying() {
        return Controller.getInstance().getPlayingAudio() != null;
    }


    private void updateSeek() {
        try {
            if (isPlaying()) {

                if (updateSeek) {
                    if (!Utils.equals(playingUid, Controller.getInstance().getPlayList()[Controller.getInstance().getPlayingAudio()])) {

                        GuidePlayerFragment.this.getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                playingUid = Controller.getInstance().getPlayList()[Controller.getInstance().getPlayingAudio()];
                                int currentfileDuration = Controller.getInstance().getMediaPlayer().getDuration();
                                //update playing file on ui
                                updateControlsState();
                                mPlayAndPause.setImageResource(R.drawable.pause);
                                duration.setText(Utils.formatMs(currentfileDuration));
                                mSeekBarGlobal.setProgress(0);
                                lastSeekPosition = 0;
                                mSeekBarGlobal.setMax(currentfileDuration);

                            }
                        });


                    }
                    int current = Controller.getInstance().getMediaPlayer().getCurrentPosition();
                    int tim = Math.abs(current - lastSeekPosition);

                    if (tim >= SEEKRESOLUTION_MS) {   //control frequency of seek updates
                        lastSeekPosition = current;

                        mSeekBarGlobal.setProgress(current);
                    }


                }

            } else {
                mSeekBarGlobal.setProgress(0);
                playingUid = null;
                //updateControlsState();

            }
        } catch (Exception ign) {
            ign.printStackTrace();
        }
    }

    private void updateControlsState() {   //updates controls when audio ends or starts: enabled, states, etc

        PlaceElement pl = Controller.getInstance().getDao().load(Controller.getInstance().getPlayList()[Controller.getInstance().getPlayingAudio()]);
        if (pl != null) {
            title.setText((Controller.getInstance().getPlayingAudio() + 1) + " - " + pl.getTitle());


            Controller.getInstance().getDao().loadCategoryIconInView(icon, pl);

            //check next and previous


            boolean nextActive = !(Controller.getInstance().getPlayingAudio() >= Controller.getInstance().getPlayList().length - 1);
            boolean prevActive = (Controller.getInstance().getPlayingAudio() > 0);


            if (nextActive) {
                next.setVisibility(View.VISIBLE);
            } else {
                next.setVisibility(View.INVISIBLE);
            }
            if (prevActive) {
                prev.setVisibility(View.VISIBLE);
            } else {
                prev.setVisibility(View.INVISIBLE);
            }
        }


    }


    /**
     * *******BEGIN : LISTENERS****************
     */
    private SeekBar.OnSeekBarChangeListener mOnSeekBarChanger = new SeekBar.OnSeekBarChangeListener() {


        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if (isPlaying()) {


                currentPos.setText(Utils.formatMs(mSeekBarGlobal.getProgress()));


            }
        }


        public void onStartTrackingTouch(SeekBar seekBar) {
            if (isPlaying()) {
                updateSeek = false;
            }
            //To change body of implemented methods use File | Settings | File Templates.
        }


        public void onStopTrackingTouch(SeekBar seekBar) {
            if (isPlaying()) {
                Controller.getInstance().getMediaPlayer().seekTo(seekBar.getProgress());
                updateSeek = true;
            }
        }
    };


    private View.OnClickListener mCloseOnclickListener = new View.OnClickListener() {
        public void onClick(View v) {
            Controller.getInstance().playRelatedAudio(null, null);
        }
    };

    private View.OnClickListener mInfoOnclickListener = new View.OnClickListener() {
        public void onClick(View v) {  //go to detail on parent
            PlaceElement pl = Controller.getInstance().getDao().load(Controller.getInstance().getPlayList()[Controller.getInstance().getPlayingAudio()]);
            if (pl != null) {
                Controller.getInstance().goToDetail(pl.getAttributes().get("parent"));
            }
        }
    };


    private View.OnClickListener mPlayPauseOnclickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (Controller.getInstance().getMediaPlayer() != null) {
                if (Controller.getInstance().getMediaPlayer().isPlaying()) {
                    Controller.getInstance().getMediaPlayer().pause();
                    mPlayAndPause.setImageResource(R.drawable.play);
                } else {
                    Controller.getInstance().getMediaPlayer().start();
                    mPlayAndPause.setImageResource(R.drawable.pause);
                }


            }

        }
    };


    private View.OnClickListener nextOnclickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (Controller.getInstance().getMediaPlayer() != null) {
                Controller.getInstance().playRelatedAudio(Controller.getInstance().getPlayList(), Controller.getInstance().getPlayingAudio() + 1);


            }

        }
    };
    private View.OnClickListener prevOnclickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (Controller.getInstance().getMediaPlayer() != null) {
                Controller.getInstance().playRelatedAudio(Controller.getInstance().getPlayList(), Controller.getInstance().getPlayingAudio() - 1);


            }

        }
    };


    /**********END : LISTENERS*****************/


}
