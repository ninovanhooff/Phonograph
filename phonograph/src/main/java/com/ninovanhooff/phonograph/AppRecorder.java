package com.ninovanhooff.phonograph;

import com.ninovanhooff.phonograph.audio.recorder.RecorderContract;

import java.util.List;

public interface AppRecorder {

	void addRecordingCallback(AppRecorderCallback recorderCallback);
	void removeRecordingCallback(AppRecorderCallback recorderCallback);
	void setRecorder(RecorderContract.Recorder recorder);
	void startRecording(String filePath);
	void pauseRecording();
	void resumeRecording();
	void stopRecording();
	List<Integer> getRecordingData();
	boolean isRecording();
	boolean isPaused();
	boolean isProcessing();
	/** Starts emitting live amplitude data via {@link RecorderContract.RecorderCallback#onProgress(long, int, boolean)} onProgress()} */
	void startVisualizing();
	// keep: called through release, but keep so it could be called stand-alone
	@SuppressWarnings("unused")
	void stopVisualizing();
	boolean supportsMonitoring();
	/** Start monitoring if the Recorder supports it*/
	void startMonitoring();
	void stopMonitoring();
	boolean isMonitoring();
	/** Stop all proccesses (visualizing, monitoring, recording),
	 *  release hardware and remove callbacks */
	void release();
}
