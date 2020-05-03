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

import android.app.Application;
import android.os.Handler;

import com.dimowner.audiorecorder.app.main.MainActivity;
import com.dimowner.phonograph.BuildConfig;
import com.ninovanhooff.phonograph.Phonograph;
import com.ninovanhooff.phonograph.util.AndroidUtils;

import timber.log.Timber;

//import com.crashlytics.android.Crashlytics;
//import io.fabric.sdk.android.Fabric;

public class ARApplication extends Application {

	private static String PACKAGE_NAME ;
	public static volatile Handler applicationHandler;

	public static Injector injector;

	public static Injector getInjector() {
		return injector;
	}

	public static String appPackage() {
		return PACKAGE_NAME;
	}

	@Override
	public void onCreate() {
		if (BuildConfig.DEBUG) {
			//Timber initialization
			Timber.plant(new Timber.DebugTree() {
				@Override
				protected String createStackElementTag(StackTraceElement element) {
					return "AR-AR " + super.createStackElementTag(element) + ":" + element.getLineNumber();
				}
			});
		}

		super.onCreate();
//		Fabric.with(this, new Crashlytics());

		PACKAGE_NAME = getApplicationContext().getPackageName();
		applicationHandler = new Handler(getApplicationContext().getMainLooper());
		injector = new Injector(getApplicationContext());
		Phonograph.initialize(getApplicationContext(), MainActivity.class, injector.provideAppRecorder(), null);
		Phonograph.setScreenWidthDp(AndroidUtils.pxToDp(AndroidUtils.getScreenWidth(getApplicationContext())));

	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		Timber.v("onTerminate");
		injector.releaseMainPresenter();
		injector.closeTasks();
	}

}
