package com.dimowner.phonograph.data;

public interface PhonographPrefs {

    int getRecordChannelCount();

    boolean isStoreDirPublic();

    int getFormat();

    int getBitrate();

    int getSampleRate();

    String getPublicRecordingDirName();

    void incrementRecordCounter();

    int getNamingFormat();

    long getRecordCounter();
}
