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

import com.ninovanhooff.phonograph.exception.AppException;

import java.io.File;

public interface RecorderContract {

	interface RecorderCallback {
		void onStartRecord();
		void onPauseRecord();
		void onStopRecord(File output);
		/** Live recording and visualization data
		 * @param mills The recording progress / duration
		 * @param amp maximum signal amplitude for the last sample
		 * @param isRecording Whether an recording is active. false when paused or stopped
		 */
		void onProgress(long mills, int amp, boolean isRecording);
		void onError(AppException throwable);
	}

	interface Recorder {
		void setRecorderCallback(RecorderCallback callback);
		void prepare(int channelCount, int sampleRate, int bitrate);
		void release();
		void startRecording(String outputFile);
		void pauseRecording();
		void resumeRecording();
		void stopRecording();
		/** Starts emitting live amplitude data via {@link RecorderCallback#onProgress(long, int, boolean)} onProgress()} */
		void startVisualizing();
		void stopVisualizing();
		boolean isRecording();
		boolean isRecordingPaused();
	}

	/** Supports live playback of the input **/
	interface Monitor {
		// Unused because it duplicates Recorder method.
		// Keep to support monitor-only implementations.
		@SuppressWarnings("unused")
		void prepare(int channelCount, int sampleRate, int bitrate);
		void release();
		void startMonitoring();
		void stopMonitoring();
		boolean isMonitoring();
	}
}
