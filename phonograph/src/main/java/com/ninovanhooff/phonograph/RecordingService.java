package com.ninovanhooff.phonograph;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.dimowner.phonograph.R;
import com.ninovanhooff.phonograph.data.FileRepository;
import com.ninovanhooff.phonograph.data.PhonographPrefs;
import com.ninovanhooff.phonograph.exception.AppException;
import com.ninovanhooff.phonograph.util.AndroidUtils;
import com.ninovanhooff.phonograph.util.FileUtil;

import java.io.File;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class RecordingService extends Service {

	private final static String CHANNEL_DEFAULT_NAME = "Default";
	private final static String CHANNEL_DEFAULT_ID = "com.dimowner.audiorecorder.NotificationId";
	private final static int CHANNEL_DEFAULT_PRIORITY = NotificationCompat.PRIORITY_LOW;
	private final static int CHANNEL_DEFAULT_IMPORTANCE = NotificationManagerCompat.IMPORTANCE_LOW;

	private final static String CHANNEL_ERRORS_NAME = "Errors";
	private final static String CHANNEL_ERRORS_ID = "com.dimowner.audiorecorder.Errors";
	private final static int CHANNEL_ERRORS_PRIORITY = NotificationCompat.PRIORITY_MAX;
	private final static int CHANNEL_ERRORS_IMPORTANCE = NotificationManagerCompat.IMPORTANCE_MAX;

	public static final String ACTION_START_RECORDING_SERVICE = "ACTION_START_RECORDING_SERVICE";

	public static final String ACTION_STOP_RECORDING_SERVICE = "ACTION_STOP_RECORDING_SERVICE";

	public static final String ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING";
	public static final String ACTION_PAUSE_RECORDING = "ACTION_PAUSE_RECORDING";

	private static final int NOTIF_ID = 101;
	private NotificationCompat.Builder builder;
	private NotificationManager notificationManager;
	private RemoteViews remoteViewsSmall;
//	private RemoteViews remoteViewsBig;
	private Notification notification;

	private AppRecorder appRecorder;
	private AppRecorderCallback appRecorderCallback;
	private PhonographColorMap colorMap;
	private boolean started = false;
	private PhonographPrefs prefs;
	private FileRepository fileRepository;

	public RecordingService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		appRecorder = Phonograph.getAppRecorder();
		colorMap = Phonograph.getInjector().provideColorMap();
		prefs = Phonograph.getInjector().providePrefs();
		fileRepository = Phonograph.getInjector().provideFileRepository();

		appRecorderCallback = new AppRecorderCallback() {
			@Override public void onRecordingStarted() {
				updateNotificationResume();
			}
			@Override public void onRecordingPaused() {
				updateNotificationPause();
			}
			@Override public void onRecordProcessing() { }
			@Override public void onRecordFinishProcessing() { }
			@Override public void onRecordingStopped(long id, File file) { }

			@Override
			public void onRecordingProgress(long mills, int amp) {
				if (!hasAvailableSpace()) {
					AndroidUtils.runOnUIThread(new Runnable() {
						@Override
						public void run() {
							stopRecording();
							Toast.makeText(getApplicationContext(), R.string.error_no_available_space, Toast.LENGTH_LONG).show();
							showNoSpaceNotification();
						}
					});
				}
			}

			@Override public void onError(AppException throwable) { }
		};
		appRecorder.addRecordingCallback(appRecorderCallback);
	}

	public void showNoSpaceNotification() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createNotificationChannel(CHANNEL_ERRORS_ID, CHANNEL_ERRORS_NAME, CHANNEL_ERRORS_IMPORTANCE);
		}
		NotificationCompat.Builder builder =
				new NotificationCompat.Builder(getApplicationContext(), CHANNEL_DEFAULT_ID)
						.setSmallIcon(R.drawable.ic_phonograph_record_rec)
						.setContentTitle(getApplicationContext().getString(R.string.app_name))
						.setContentText(getApplicationContext().getString(R.string.error_no_available_space))
						.setContentIntent(createContentIntent())
						.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
						.setLights(Color.RED, 500, 500)
						.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
						.setAutoCancel(true)
						.setPriority(CHANNEL_ERRORS_PRIORITY);

		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
		notificationManager.notify(303, builder.build());
	}

	private boolean hasAvailableSpace() {
		final long space = FileUtil.getFree(fileRepository.getRecordingDir());
		final long time = spaceToTimeSecs(space, prefs.getFormat(), prefs.getSampleRate(), prefs.getRecordChannelCount());
		return time > PhonographConstants.MIN_REMAIN_RECORDING_TIME;
	}

	private long spaceToTimeSecs(long spaceBytes, int format, int sampleRate, int channels) {
		if (format == PhonographConstants.RECORDING_FORMAT_M4A) {
			return 1000 * (spaceBytes/(PhonographConstants.RECORD_ENCODING_BITRATE_48000 /8));
		} else if (format == PhonographConstants.RECORDING_FORMAT_WAV) {
			return 1000 * (spaceBytes/(sampleRate * channels * 2));
		} else {
			return 0;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			String action = intent.getAction();
			if (action != null && !action.isEmpty()) {
				switch (action) {
					case ACTION_START_RECORDING_SERVICE:
						startForegroundService();
						break;
					case ACTION_STOP_RECORDING_SERVICE:
						stopForegroundService();
						break;
					case ACTION_STOP_RECORDING:
						stopRecording();
						break;
					case ACTION_PAUSE_RECORDING:
						if (appRecorder.isPaused()) {
							appRecorder.resumeRecording();
							updateNotificationResume();
						} else {
							appRecorder.pauseRecording();
							updateNotificationPause();
						}
						break;
				}
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void stopRecording() {
		appRecorder.stopRecording();
		stopForegroundService();
	}

	private void startForegroundService() {
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createNotificationChannel(CHANNEL_DEFAULT_ID, CHANNEL_DEFAULT_NAME, CHANNEL_DEFAULT_IMPORTANCE);
		}

		remoteViewsSmall = new RemoteViews(getPackageName(), R.layout.layout_record_notification_small);
		remoteViewsSmall.setOnClickPendingIntent(R.id.btn_recording_stop, getPendingSelfIntent(getApplicationContext(), ACTION_STOP_RECORDING));
		remoteViewsSmall.setOnClickPendingIntent(R.id.btn_recording_pause, getPendingSelfIntent(getApplicationContext(), ACTION_PAUSE_RECORDING));
		remoteViewsSmall.setTextViewText(R.id.txt_recording_progress, getResources().getString(R.string.recording_is_on));
		remoteViewsSmall.setInt(R.id.container, "setBackgroundColor", this.getResources().getColor(colorMap.getPrimaryColorRes()));

		// Create notification builder.
		builder = new NotificationCompat.Builder(this, CHANNEL_DEFAULT_ID);

		builder.setWhen(System.currentTimeMillis());
		builder.setSmallIcon(R.drawable.ic_phonograph_record);
		builder.setPriority(CHANNEL_DEFAULT_PRIORITY);
		// Make head-up notification.
		builder.setContentIntent(createContentIntent());
		builder.setCustomContentView(remoteViewsSmall);
		builder.setOnlyAlertOnce(true);
		builder.setDefaults(0);
		builder.setSound(null);
		notification = builder.build();
		startForeground(NOTIF_ID, notification);
		started = true;
	}

	private PendingIntent createContentIntent() {
		// Create notification default intent.
		Intent intent = new Intent(getApplicationContext(), Phonograph.getActivityClass());
		intent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
		return PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
	}

	private void stopForegroundService() {
		appRecorder.removeRecordingCallback(appRecorderCallback);
		stopForeground(true);
		stopSelf();
		started = false;
	}

	protected PendingIntent getPendingSelfIntent(Context context, String action) {
		Intent intent = new Intent(context, StopRecordingReceiver.class);
		intent.setAction(action);
		return PendingIntent.getBroadcast(context, 10, intent, 0);
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private String createNotificationChannel(String channelId, String channelName, int channelImportance) {
		NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
		if (channel == null) {
			NotificationChannel chan = new NotificationChannel(channelId, channelName, channelImportance);
			chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
			chan.setSound(null, null);
			chan.enableLights(false);
			chan.enableVibration(false);

			notificationManager.createNotificationChannel(chan);
		}
		return channelId;
	}

	private void updateNotificationPause() {
		if (started && remoteViewsSmall != null) {
			remoteViewsSmall.setTextViewText(R.id.txt_recording_progress, getResources().getString(R.string.recording_paused));
			remoteViewsSmall.setImageViewResource(R.id.btn_recording_pause, R.drawable.ic_phonograph_pause);

			notificationManager.notify(NOTIF_ID, notification);
		}
	}

	private void updateNotificationResume() {
		if (started && remoteViewsSmall != null) {
			remoteViewsSmall.setTextViewText(R.id.txt_recording_progress, getResources().getString(R.string.recording_is_on));
			remoteViewsSmall.setImageViewResource(R.id.btn_recording_pause, android.R.drawable.ic_media_pause);

			notificationManager.notify(NOTIF_ID, notification);
		}
	}

	public static class StopRecordingReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Intent stopIntent = new Intent(context, RecordingService.class);
			stopIntent.setAction(intent.getAction());
			context.startService(stopIntent);
		}
	}
}
