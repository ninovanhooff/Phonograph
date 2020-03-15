package com.ninovanhooff.phonograph;

import com.ninovanhooff.phonograph.exception.AppException;

import java.io.File;

public interface AppRecorderCallback {
	void onRecordingStarted();
	void onRecordingPaused();
	void onRecordProcessing();
	void onRecordFinishProcessing();
	void onRecordingStopped(long id, File file);
	void onProgress(long mills, int amp, boolean isRecording);
	void onError(AppException throwable);
}
