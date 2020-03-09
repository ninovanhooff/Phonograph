package com.ninovanhooff.phonograph;


import android.content.Context;
import android.os.Handler;

import com.ninovanhooff.phonograph.data.FileRepository;

public class Phonograph {

    // todo make not static
    private static PhonographInjector injector;
    public static volatile Handler applicationHandler;
    private static Class<?> intentActivityClass;
    private static AppRecorder appRecorder;


    private static boolean isRecording = false;

    /** Screen width in dp */
    private static float screenWidthDp = 0;

    public static void initialize(Context applicationContext, Class<?> activityClass, AppRecorder appRecorder) {
        injector = new PhonographInjector(applicationContext);
        applicationHandler = new Handler(applicationContext.getMainLooper());
        intentActivityClass = activityClass;
        Phonograph.appRecorder = appRecorder;
    }

    public static void setScreenWidthDp(float screenWidthDp) {
        Phonograph.screenWidthDp = screenWidthDp;
    }

    /**
     * Calculate density pixels per second for record duration.
     * Used for visualisation waveform in view.
     * @param durationSec record duration in seconds.
     *                    In general, higher resolutions will be returned for shorter clips.
     */
    public static float getWaveformDpPerSecond(float durationSec) {
        if (durationSec > PhonographConstants.LONG_RECORD_THRESHOLD_SECONDS) {
            return PhonographConstants.WAVEFORM_WIDTH * screenWidthDp / durationSec;
        } else {
            return PhonographConstants.SHORT_RECORD_DP_PER_SECOND;
        }
    }

    public static int getLongWaveformSampleCount() {
        return (int)(PhonographConstants.WAVEFORM_WIDTH * screenWidthDp);
    }

    @SuppressWarnings("unused") // used by applications
    // todo should this be exposed?
    public static FileRepository getFileRepository(){
        return getInjector().provideFileRepository();
    }

    public static void setRecording(boolean recording){
        isRecording = recording;
    }

    public static boolean isRecording() {
        return isRecording;
    }

    static PhonographInjector getInjector() {
        return injector;
    }

    static Class<?> getActivityClass() {
        return intentActivityClass;
    }

    static AppRecorder getAppRecorder() {
        return appRecorder;
    }
}
