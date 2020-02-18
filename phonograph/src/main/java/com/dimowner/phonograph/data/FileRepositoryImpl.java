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

import com.dimowner.phonograph.PhonographConstants;
import com.dimowner.phonograph.exception.CantCreateFileException;
import com.dimowner.phonograph.util.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;

import timber.log.Timber;

public class FileRepositoryImpl implements FileRepository {

	private File recordDirectory;
	private PhonographPrefs prefs;

	private volatile static FileRepositoryImpl instance;

	private FileRepositoryImpl(Context context, PhonographPrefs prefs) {
		updateRecordingDir(context, prefs);
		this.prefs = prefs;
	}

	public static FileRepositoryImpl getInstance(Context context, PhonographPrefs prefs) {
		if (instance == null) {
			synchronized (FileRepositoryImpl.class) {
				if (instance == null) {
					instance = new FileRepositoryImpl(context, prefs);
				}
			}
		}
		return instance;
	}

	@Override
	public File provideRecordFile() throws CantCreateFileException {
		prefs.incrementRecordCounter();
		File recordFile;
		String recordName;
		if (prefs.getNamingFormat() == PhonographConstants.NAMING_COUNTED) {
			recordName = FileUtil.generateRecordNameCounted(prefs.getRecordCounter());
		} else {
			recordName = FileUtil.generateRecordNameDate();
		}
		if (prefs.getFormat() == PhonographConstants.RECORDING_FORMAT_WAV) {
			recordFile = FileUtil.createFile(recordDirectory, FileUtil.addExtension(recordName, PhonographConstants.WAV_EXTENSION));
		} else {
			recordFile = FileUtil.createFile(recordDirectory, FileUtil.addExtension(recordName, PhonographConstants.M4A_EXTENSION));
		}
		if (recordFile != null) {
			return recordFile;
		}
		throw new CantCreateFileException();
	}

	@Override
	public File provideRecordFile(String name) throws CantCreateFileException {
		File recordFile = FileUtil.createFile(recordDirectory, name);
		if (recordFile != null) {
			return recordFile;
		}
		throw new CantCreateFileException();
	}

	@Override
	public File getRecordingDir() {
		return recordDirectory;
	}

	@Override
	public boolean deleteRecordFile(String path) {
		if (path != null) {
			return FileUtil.deleteFile(new File(path));
		}
		return false;
	}

	@Override
	public boolean renameFile(String path, String newName, String extension) {
		return FileUtil.renameFile(new File(path), newName, extension);
	}

	public void updateRecordingDir(Context context, PhonographPrefs prefs) {
		if (prefs.isStoreDirPublic()) {
			recordDirectory = getAppDir();
			if (recordDirectory == null) {
				//Try to init private dir
				try {
					recordDirectory = FileUtil.getPrivateRecordsDir(context);
				} catch (FileNotFoundException e) {
					Timber.e(e);
					//If nothing helped then hardcode recording dir
					recordDirectory = context.getFilesDir();
				}
			}
		} else {
			try {
				recordDirectory = FileUtil.getPrivateRecordsDir(context);
			} catch (FileNotFoundException e) {
				Timber.e(e);
				//Try to init public dir
				recordDirectory = getAppDir();
				if (recordDirectory == null) {
					//If nothing helped then hardcode recording dir
					recordDirectory = context.getFilesDir(); // todo test
				}
			}
		}
	}

	private File getAppDir() {
		return FileUtil.getStorageDir(prefs.getPublicRecordingDirName());
	}
}
