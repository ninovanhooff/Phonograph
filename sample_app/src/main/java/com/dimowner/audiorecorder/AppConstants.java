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

package com.dimowner.audiorecorder;

/**
 * AppConstants that may be used in multiple classes.
 */
public class AppConstants {

	private AppConstants() {}

	public static final String REQUESTS_RECEIVER = "dimmony@gmail.com";

	public static final String M4A_EXTENSION = "m4a";
	public static final String WAV_EXTENSION = "wav";
	public static final String EXTENSION_SEPARATOR = ".";
	public static final int MAX_RECORD_NAME_LENGTH = 50;

	public static final int NAMING_COUNTED = 0;
	public static final int NAMING_DATE = 1;

	public static final int RECORDING_FORMAT_M4A = 0;
	public static final int RECORDING_FORMAT_WAV = 1;

	public static final int DEFAULT_PER_PAGE = 50;

	public final static long RECORD_IN_TRASH_MAX_DURATION = 5184000000L; // 1000 X 60 X 60 X 24 X 60 = 60 Days

	//BEGINNING-------------- Waveform visualization constants ----------------------------------



	/** Density pixel count per one second of time.
	 *  Used for short records (shorter than {@link AppConstants#LONG_RECORD_THRESHOLD_SECONDS}) */
	public static final int SHORT_RECORD_DP_PER_SECOND = 25;

	/** Threshold in second which defines when record is considered as long or short.
	 *  For short and long records used a bit different visualization algorithm. */
	public static final int LONG_RECORD_THRESHOLD_SECONDS = 20;

	//END-------------- Waveform visualization constants ----------------------------------------

	public static final int SORT_DATE = 1;
	public static final int SORT_NAME = 2;
	public static final int SORT_DURATION = 3;

	public final static int RECORD_AUDIO_MONO = 1;
	public final static int RECORD_AUDIO_STEREO = 2;

}
