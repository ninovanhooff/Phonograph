package com.dimowner.audiorecorder;


public class Phonograph {

    private static boolean isRecording = false;

    /** Screen width in dp */
    private static float screenWidthDp = 0;

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

    public static void setRecording(boolean recording){
        isRecording = recording;
    }

    public static boolean isRecording() {
        return isRecording;
    }
}
