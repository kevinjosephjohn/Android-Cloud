package example.com.m4dr4t;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.nfc.Tag;
import android.os.CountDownTimer;
import android.os.Environment;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class CallRecorder extends BroadcastReceiver{
    MediaRecorder recorder;
    TelephonyManager telManager;
    static boolean recordStarted;
    Context context;
    File audiofile = null;
    File directory;
    static boolean listener = false;

    @Override
    public void onReceive(Context context, Intent intent) {


        this.context=context;
        recorder = new MediaRecorder();
        String action = intent.getAction();
        if(listener == false) {
            try {

                telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                telManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
                listener = true;
                Log.i("Listener", "Listener Started");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    private final PhoneStateListener phoneListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, final String incomingNumber) {
            try {
                Log.i("Number",incomingNumber);
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING: {
                        Log.i("Call", "Call Ringing");
//                        telManager.listen(null,PhoneStateListener.LISTEN_NONE);
                        break;


                    }
                    case TelephonyManager.CALL_STATE_OFFHOOK: {

                        if(recordStarted == false) {
                            File sampleDir = Environment.getExternalStorageDirectory();
                            directory = new File (sampleDir.getAbsolutePath() + "/CallRecordings");
                            directory.mkdirs();
                            try {
                                audiofile = File.createTempFile("sound" + System.currentTimeMillis(), ".mp4", directory);

                            } catch (IOException e) {
                                e.printStackTrace();
                                return;
                            }

                            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
                            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                            recorder.setOutputFile(audiofile.getAbsolutePath());
                            recorder.prepare();
                            recorder.start();
                            recordStarted = true;
                            Log.i("Call", "Call Started");
                        }

                        break;

                    }
                    case TelephonyManager.CALL_STATE_IDLE: {
                        if (recordStarted) {
                            recorder.stop();
                            recordStarted = false;

                            telManager.listen(phoneListener,PhoneStateListener.LISTEN_NONE);

                            Log.i("Call", "Call Ended");
                            new CountDownTimer(2000, 1000) {
                                public void onFinish() {
                                    // When timer is finished
                                    // Execute your code here
                                    listener = false;
                                    DateFormat df = new SimpleDateFormat("d MMM yyyy hh:mm a z");
                                    String date = df.format(Calendar.getInstance().getTime());
                                    File to        = new File(directory, incomingNumber + "-"+date+ ".mp4");
                                    audiofile.renameTo(to);

                                }

                                public void onTick(long millisUntilFinished) {
                                    // millisUntilFinished    The amount of time until finished.
                                }
                            }.start();


                        }


                        break;
                    }

                    default: { }
                }
            } catch (Exception ex) {

            }
        }
    };
}
