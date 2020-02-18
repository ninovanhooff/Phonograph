/*
 * Copyright 2018 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dimowner.phonograph.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.dimowner.phonograph.PhonographConstants;
import com.dimowner.phonograph.R;
import com.dimowner.phonograph.util.FileUtil;

/**
 * A Basic implementation which always returns the same configuration
 */
public class PhonographPrefsImpl implements PhonographPrefs {

	private static final String PREF_NAME = "com.dimowner.phonograph.data.PhonographPrefsImpl";

	private static final String PREF_KEY_RECORD_COUNTER = "record_counter";

	private SharedPreferences sharedPreferences;

	private volatile static PhonographPrefsImpl instance;

	private String publicRecordingDirName;

	private PhonographPrefsImpl(Context context){
		sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		String appName = context.getString(R.string.app_name);
		publicRecordingDirName = FileUtil.removeUnallowedSignsFromName(appName);
	}

	public static PhonographPrefsImpl getInstance(Context context) {
		if (instance == null) {
			synchronized (PhonographPrefsImpl.class) {
				if (instance == null) {
					instance = new PhonographPrefsImpl(context);
				}
			}
		}
		return instance;
	}

	@Override
	public int getRecordChannelCount() {
		return PhonographConstants.RECORD_AUDIO_STEREO;
	}

	@Override
	public boolean isStoreDirPublic() {
		return false;
	}

	@Override
	public int getFormat() {
		return PhonographConstants.RECORDING_FORMAT_M4A;
	}

	@Override
	public int getBitrate() {
		return PhonographConstants.RECORD_ENCODING_BITRATE_128000;
	}

	@Override
	public int getSampleRate() {
		return PhonographConstants.RECORD_SAMPLE_RATE_44100;
	}

	@Override
	public String getPublicRecordingDirName() {
		return publicRecordingDirName;
	}

	@Override
	public void incrementRecordCounter() {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putLong(PREF_KEY_RECORD_COUNTER, getRecordCounter()+1);
		editor.apply();
	}

	@Override
	public int getNamingFormat() {
		return PhonographConstants.NAMING_COUNTED;
	}

	@Override
	public long getRecordCounter() {
		return 0;
	}

}
