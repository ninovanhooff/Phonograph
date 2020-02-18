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

package com.dimowner.phonograph;

/**
 * PhonographConstants that may be used in multiple classes.
 */
public class PhonographConstants {

	private PhonographConstants() {}

	//BEGINNING-------------- Waveform visualisation constants ----------------------------------

	/** Density pixel count per one second of time.
	 *  Used for short records (shorter than {@link PhonographConstants#LONG_RECORD_THRESHOLD_SECONDS}) */
	public static final int SHORT_RECORD_DP_PER_SECOND = 25;

	/** Waveform length, measured in screens count of device.
	 *  Used for long records (longer than {@link PhonographConstants#LONG_RECORD_THRESHOLD_SECONDS})   */
	public static final float WAVEFORM_WIDTH = 1.5f; //one and half of screen waveform width.

	/** Threshold in second which defines when record is considered as long or short.
	 *  For short and long records used a bit different visualisation algorithm. */
	public static final int LONG_RECORD_THRESHOLD_SECONDS = 20;

	/** Count of grid lines on visible part of Waveform (actually lines count visible on screen).
	 *  Used for long records visualisation algorithm. (longer than {@link PhonographConstants#LONG_RECORD_THRESHOLD_SECONDS} ) */
	public static final int GRID_LINES_COUNT = 16;

	//END-------------- Waveform visualisation constants ----------------------------------------

	public static final int TIME_FORMAT_24H = 11;
	public static final int TIME_FORMAT_12H = 12;

	// recording and playback
	public final static int PLAYBACK_SAMPLE_RATE = 44100;
	public final static int RECORD_SAMPLE_RATE_44100 = 44100;
	public final static int RECORD_SAMPLE_RATE_8000 = 8000;
	public final static int RECORD_SAMPLE_RATE_16000 = 16000;
	public final static int RECORD_SAMPLE_RATE_32000 = 32000;
	public final static int RECORD_SAMPLE_RATE_48000 = 48000;

	public final static int RECORD_ENCODING_BITRATE_24000 = 24000;
	public final static int RECORD_ENCODING_BITRATE_48000 = 48000;
	public final static int RECORD_ENCODING_BITRATE_96000 = 96000;
	public final static int RECORD_ENCODING_BITRATE_128000 = 128000;
	public final static int RECORD_ENCODING_BITRATE_192000 = 192000;

	public static final int SORT_DATE = 1;
	public static final int SORT_NAME = 2;
	public static final int SORT_DURATION = 3;

//	public final static int RECORD_AUDIO_CHANNELS_COUNT = 2;
	public final static int RECORD_AUDIO_MONO = 1;
	public final static int RECORD_AUDIO_STEREO = 2;
	public final static int RECORD_MAX_DURATION = 14400000; // 240 min 4 hours

	/** Time interval for Recording progress visualisation. */
	public final static int VISUALIZATION_INTERVAL = 1000/SHORT_RECORD_DP_PER_SECOND; //1000 mills/25 dp per sec

	public final static int RECORD_BYTES_PER_SECOND = RECORD_ENCODING_BITRATE_48000 /8; //bits per sec converted to bytes per sec.

}
