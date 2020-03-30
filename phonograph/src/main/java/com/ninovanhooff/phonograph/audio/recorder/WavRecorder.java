/*
 * Copyright 2019 Dmitriy Ponomarenko
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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import com.ninovanhooff.phonograph.Phonograph;
import com.ninovanhooff.phonograph.PhonographConstants;
import com.ninovanhooff.phonograph.exception.InvalidOutputFile;
import com.ninovanhooff.phonograph.exception.RecorderInitException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

import static java.lang.Thread.sleep;

public class WavRecorder implements RecorderContract.Recorder, RecorderContract.Monitor {

	private AudioRecord recorder = null;
	private AudioTrack audioTrack = null;

	private static final int RECORDER_BPP = 16; //bits per sample

	private File recordFile = null;
	private int bufferSize = 0;

	private Thread recordingThread;

	/** Microphone active */
	private volatile boolean isCapturing = false;
	/** Saving to file */
	private volatile boolean isRecording = false;
	/** Whether recording is paused */
	private volatile boolean isRecordingPaused = false;
	/** Piping capture to audio out */
	private volatile boolean isMonitoring = false;

	private int channelCount = 1;

	/** Value for recording used visualization. */
	private int lastVal = 0;

	private Timer timerProgress;
	private long progress = 0;

	private int sampleRate = PhonographConstants.RECORD_SAMPLE_RATE_44100;

	private RecorderContract.RecorderCallback recorderCallback;

	private static class WavRecorderSingletonHolder {
		private static WavRecorder singleton = new WavRecorder();

		static WavRecorder getSingleton() {
			return WavRecorderSingletonHolder.singleton;
		}
	}

	public static WavRecorder getInstance() {
		return WavRecorderSingletonHolder.getSingleton();
	}

	private WavRecorder() { }

	@Override
	public void setRecorderCallback(RecorderContract.RecorderCallback callback) {
		recorderCallback = callback;
	}

	@Override
	public void prepare(int channelCount, int sampleRate, int bitrate){
		this.sampleRate = sampleRate;
		this.channelCount = channelCount;
		//wav has no compression, so ignoring bitrate parameter
		Timber.d("WavRecorder prepared");
		startCapturing();
	}

	@Override
	public void startRecording(String outputFile) {
		if (isRecording){
			stopRecording();
		}

		recordFile = new File(outputFile);
		if (!recordFile.exists() || !recordFile.isFile()) {
			if (recorderCallback != null) {
				recorderCallback.onError(new InvalidOutputFile());
			}
			return;
		}

		if (!isCapturing){
			startCapturing();
		}

		if (isCapturing) {
			isRecording = true;
			progress = 0L;
			if (recorderCallback != null) {
				recorderCallback.onStartRecord();
			}
			Phonograph.setRecording(true);
		}
	}

	@Override
	public void pauseRecording() {
		if (isRecording) {
			isRecordingPaused = true;
			if (recorderCallback != null) {
				recorderCallback.onPauseRecord();
			}
		}
	}

	@Override
	public void resumeRecording(){
		if (isRecordingPaused) {
			recorder.startRecording();
			if (recorderCallback != null) {
				recorderCallback.onStartRecord();
			}
			isRecordingPaused = false;
		}
	}

	@Override
	public void stopRecording() {
		if (isRecording && recorder != null) {
			isRecording = false;
			isRecordingPaused = false;
			Phonograph.setRecording(false);
			if (recorderCallback != null) {
				recorderCallback.onStopRecord(recordFile);
			}
		}
	}

	@Override
	public void startMonitoring() {
		if (isMonitoring){
			return;
		}

		if (!isCapturing){
			startCapturing();
		}

		try {
			audioTrack = new AudioTrack(
					AudioManager.STREAM_MUSIC,
					this.sampleRate,
					channelCount == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
					AudioFormat.ENCODING_PCM_16BIT,
					bufferSize,
					AudioTrack.MODE_STREAM);
			if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED){
				audioTrack.play();
				isMonitoring = true;
				Timber.d("monitoring started");
			} else {
				stopMonitoring(); // cleanup & reset
			}
		} catch (IllegalArgumentException e){
			Timber.e(e, "Could not start monitoring");
			stopMonitoring();
		}
	}

	@Override
	public void stopMonitoring() {
		if (audioTrack != null) {
			audioTrack.stop();
			audioTrack.release();
			audioTrack = null;
			isMonitoring = false;
			Timber.d("monitoring stopped");
		}
	}

	@Override
	public boolean isRecording() {
		return isRecording;
	}

	@Override
	public boolean isMonitoring() {
		return isMonitoring;
	}

	@Override
	public boolean isRecordingPaused() {
		return isRecordingPaused;
	}

	/** Stop monitoring, recording and release the hardware resources */
	@Override
	public void release(){
		stopRecording();
		stopMonitoring();
		stopCapturing();
		Timber.d("WaveRecorder: released");
	}

	private void startCapturing() {
		if (recorder != null && recorder.getState() == AudioRecord.RECORDSTATE_RECORDING) {
			return;
		}

		if (recorder != null){
			Timber.e("Cannot start capturing. Recorder already exists in state %s", recorder.getState());
		}

		int channel = channelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
		try {
			bufferSize = AudioRecord.getMinBufferSize(sampleRate,
					channel,
					AudioFormat.ENCODING_PCM_16BIT);
			Timber.v("buffer size = %s", bufferSize);
			if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
				bufferSize = AudioRecord.getMinBufferSize(sampleRate,
						channel,
						AudioFormat.ENCODING_PCM_16BIT);
			}
			recorder = new AudioRecord(
					MediaRecorder.AudioSource.MIC,
					sampleRate,
					channel,
					AudioFormat.ENCODING_PCM_16BIT,
					bufferSize
			);
		} catch (IllegalArgumentException e) {
			Timber.e(e, "sampleRate = " + sampleRate + " channel = " + channel + " bufferSize = " + bufferSize);
			if (recorder != null) {
				recorder.release();
				recorder = null;
			}
		}

		if (recorder == null || recorder.getState() != AudioRecord.STATE_INITIALIZED) {
			Timber.e("startCapturing() failed");
			if (recorderCallback != null) {
				recorderCallback.onError(new RecorderInitException());
			}
		} else {
			recorder.startRecording();
			isCapturing = true;

			recordingThread = new Thread(new Runnable() {
				@Override
				public void run() {
					captureLoop();
				}
			}, "AudioRecorder Capture Thread");

			recordingThread.start();
			startVisualizationTimer();

			Timber.d("WaveRecorder: capturing");
		}
	}

	private void stopCapturing(){
		if (!isCapturing){
			return;
		}

		stopVisualizationTimer();
		isCapturing = false;
		if (recordingThread != null){
			recordingThread.interrupt();
			recordingThread = null;
		}
		recorder.stop();
		recorder.release();
		recorder = null;
	}

	private void captureLoop() {
		byte[] data = new byte[bufferSize];
		FileOutputStream fos = null;
		int bytesRead;

		// the full buffer time in millis.
		long bufferMillis = 1000 * (bufferSize * 8) / (channelCount * 16 * sampleRate);
		// when sleeping we want to miss at most half a buffer of data
		long pauseSleepMillis = bufferMillis / 2;

		while (isCapturing) {
			if (isRecordingPaused){
				try {
					sleep(pauseSleepMillis);
					continue;
				} catch (InterruptedException ignored) {}
			}

			if (isRecording && fos == null){
				try {
					Timber.d("Opening file for recording: %s", recordFile.getAbsolutePath());
					fos = new FileOutputStream(recordFile);
				} catch (FileNotFoundException e) {
					Timber.e(e);
					fos = null;
				}
			}

			bytesRead = recorder.read(data, 0, bufferSize);
			if (isMonitoring){
				audioTrack.write(data, 0, bufferSize);
			}
			if (AudioRecord.ERROR_INVALID_OPERATION != bytesRead) {
				lastVal = 0;
				int i;
				for (i = 0; i + 1 < data.length; i += 2){
					lastVal = Math.max(lastVal, Math.abs(data[i] | (data[ i + 1] << 8)));
				}
				if (fos != null){
					try {
						fos.write(data);
					} catch (IOException e) {
						Timber.e(e);
					}
				}

			}

			if(!isRecording && fos != null){
				Timber.d("Closing file: %s", recordFile.getAbsolutePath());
				try {
					fos.close();
				} catch (IOException e) {
					Timber.e(e);
				} finally {
					fos = null;
				}
				setWaveFileHeader(recordFile, channelCount);
			}

		}
	}

	private void setWaveFileHeader(File file, int channels) {
		long fileSize = file.length() - 8;
		long totalSize = fileSize + 36;
		long byteRate = sampleRate * channels * (RECORDER_BPP/8); //2 byte per 1 sample for 1 channel.

		try {
			final RandomAccessFile wavFile = randomAccessFile(file);
			wavFile.seek(0); // to the beginning
			wavFile.write(generateHeader(fileSize, totalSize, sampleRate, channels, byteRate));
			wavFile.close();
		} catch (IOException e) {
			Timber.e(e);
		}
	}

	private RandomAccessFile randomAccessFile(File file) {
		RandomAccessFile randomAccessFile;
		try {
			randomAccessFile = new RandomAccessFile(file, "rw");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		return randomAccessFile;
	}

	private byte[] generateHeader(
			long totalAudioLen, long totalDataLen, long longSampleRate, int channels,
			long byteRate) {

		byte[] header = new byte[44];

		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; //16 for PCM. 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (channels * (RECORDER_BPP/8)); // block align
		header[33] = 0;
		header[34] = RECORDER_BPP; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		return header;
	}

	private void startVisualizationTimer() {
		timerProgress = new Timer();
		timerProgress.schedule(new TimerTask() {
			@Override
			public void run() {
				if (recorderCallback != null && recorder != null) {
					boolean isRecordingActive = isRecording && !isRecordingPaused;
					recorderCallback.onProgress(
							progress,
							lastVal,
							isRecordingActive
					);
					if(isRecordingActive){
						progress += PhonographConstants.VISUALIZATION_INTERVAL;
					}
				}
			}
		}, 0, PhonographConstants.VISUALIZATION_INTERVAL);
	}

	private void stopVisualizationTimer() {
		if (timerProgress != null){
			timerProgress.cancel();
			timerProgress.purge();
		}
		progress = 0L;
	}

}
