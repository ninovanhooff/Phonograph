/*
 * Copyright 2018 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ninovanhooff.phonograph.audio.recorder;

import android.media.MediaRecorder;
import android.os.Build;

import com.ninovanhooff.phonograph.Phonograph;
import com.ninovanhooff.phonograph.exception.AppException;
import com.ninovanhooff.phonograph.exception.InvalidOutputFile;
import com.ninovanhooff.phonograph.exception.RecorderInitException;
import com.ninovanhooff.phonograph.PhonographConstants;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

public class AudioRecorder implements RecorderContract.Recorder {

	private MediaRecorder recorder = null;
	private File recordFile = null;

	private boolean isPrepared = false;
	private boolean isRecording = false;
	private boolean isPaused = false;
	private Timer visualizationTimer;
	private long progress = 0;

	private RecorderContract.RecorderCallback recorderCallback;

	private static class RecorderSingletonHolder {
		private static AudioRecorder singleton = new AudioRecorder();

		static AudioRecorder getSingleton() {
			return RecorderSingletonHolder.singleton;
		}
	}

	public static AudioRecorder getInstance() {
		return RecorderSingletonHolder.getSingleton();
	}

	private AudioRecorder() { }

	@Override
	public void setRecorderCallback(RecorderContract.RecorderCallback callback) {
		this.recorderCallback = callback;
	}

	@Override
	public void prepare(int channelCount, int sampleRate, int bitrate){
		recorder = new MediaRecorder();
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		recorder.setAudioChannels(channelCount);
		recorder.setAudioSamplingRate(sampleRate);
		recorder.setAudioEncodingBitRate(bitrate);
		recorder.setMaxDuration(-1); //Duration unlimited
		isPrepared = true;
	}

	@Override
	public void startRecording(String outputFile) {
		recordFile = new File(outputFile);
		if (recordFile.exists() && recordFile.isFile()) {

			recorder.setOutputFile(recordFile.getAbsolutePath());
			try {
				if (!isPrepared){
					Timber.e("Recorder is not prepared!!!");
					throw new IllegalStateException("startRecording() called before prepare()");
				}
				recorder.prepare();
			} catch (IOException | IllegalStateException e) {
				Timber.e(e, "prepare() failed");
				emitAppException(new RecorderInitException());
			}
		} else {
			emitAppException(new InvalidOutputFile());
		}

		try {
			recorder.start();
			isRecording = true;
			Phonograph.setRecording(true);
			startVisualizationTimer();
			if (recorderCallback != null) {
				recorderCallback.onStartRecord();
			}
		} catch (RuntimeException e) {
			Timber.e(e, "startRecording() failed");
			emitAppException(new RecorderInitException());
		}
	}

	@Override
	public void pauseRecording() {
		if (isRecording) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				try {
					recorder.pause();
					pauseRecordingTimer();
					if (recorderCallback != null) {
						recorderCallback.onPauseRecord();
					}
					isPaused = true;
				} catch (IllegalStateException e) {
					Timber.e(e, "pauseRecording() failed");
					//TODO: Fix exception
					emitAppException(new RecorderInitException());
				}
				isPaused = false;
			} else {
				stopRecording();
			}
		}
	}

	@Override
	public void resumeRecording(){
		if (!isPaused){
			throw new IllegalStateException("Can only resume paused recordings");
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			Timber.e("Resume not supported by SDK");
			emitAppException(new RecorderInitException());
		} else {
			recorder.resume();
		}
	}

	@Override
	public void stopRecording() {
		if (isRecording) {
			stopVisualizationTimer();
			try {
				recorder.stop();
				Phonograph.setRecording(false);
			} catch (RuntimeException e) {
				Timber.e(e, "stopRecording() problems");
			}
			recorder.release();
			if (recorderCallback != null) {
				recorderCallback.onStopRecord(recordFile);
			}
			recordFile = null;
			isRecording = false;
			isPaused = false;
			recorder = null;
		} else {
			Timber.e("Recording has already stopped or hasn't started");
		}
	}

	@Override
	public void startVisualizing() {
		// Not Implemented
	}

	@Override
	public void stopVisualizing() {
		// Not Implemented
	}

	@Override
	public void release() {
		if (isRecording){
			stopVisualizationTimer();
			stopRecording();
		}
	}

	@Override
	public boolean isRecording() {
		return isRecording;
	}

	@Override
	public boolean isRecordingPaused() {
		return isPaused;
	}

	private void startVisualizationTimer() {
		visualizationTimer = new Timer();
		visualizationTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (recorderCallback != null && recorder != null) {
					boolean isRecordingActive = isRecording && !isPaused;
					try{
						recorderCallback.onProgress(
								progress,
								recorder.getMaxAmplitude(),
								isRecordingActive
						);
					} catch (IllegalStateException e) {
						Timber.e(e);
					}

					if(isRecordingActive){
						progress += PhonographConstants.VISUALIZATION_INTERVAL;
					}
				}
			}
		}, 0, PhonographConstants.VISUALIZATION_INTERVAL);
	}

	private void stopVisualizationTimer() {
		visualizationTimer.cancel();
		visualizationTimer.purge();
		progress = 0;
	}

	private void pauseRecordingTimer() {
		visualizationTimer.cancel();
		visualizationTimer.purge();
	}

	private void emitAppException(AppException e){
		if (recorderCallback != null) {
			recorderCallback.onError(e);
		}
	}
}
