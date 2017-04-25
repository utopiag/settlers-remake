/*
 * Copyright (c) 2017
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package jsettlers.main.android.core.controls;

import java.util.Timer;
import java.util.TimerTask;

import go.graphics.android.AndroidSoundPlayer;

import jsettlers.common.menu.action.EActionType;
import jsettlers.graphics.action.Action;
import jsettlers.main.android.R;
import jsettlers.main.android.gameplay.ui.activities.GameActivity;
import jsettlers.main.android.mainmenu.navigation.Actions;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

/**
 * GameMenu is a singleton within the scope of a started game
 */
public class GameMenu {
	public static final String ACTION_PAUSE = "com.jsettlers.pause";
	public static final String ACTION_UNPAUSE = "com.jsettlers.unpause";
	public static final String ACTION_SAVE = "com.jsettlers.save";
	public static final String ACTION_QUIT = "com.jsettlers.quit";
	public static final String ACTION_QUIT_CONFIRM = "com.jsettlers.quitconfirm";
	public static final String ACTION_QUIT_CANCELLED = "com.jsettlers.quitcancelled";

	public static final int NOTIFICATION_ID = 100;
	private final Context context;
	private final ActionControls actionControls;
	private final AndroidSoundPlayer soundPlayer;

	private final LocalBroadcastManager localBroadcastManager;
	private final NotificationManager notificationManager;

	private Timer quitConfirmTimer;

	private boolean paused = false;

	public GameMenu(Context context, AndroidSoundPlayer soundPlayer, ActionControls actionFireable) {
		this.context = context;
		this.soundPlayer = soundPlayer;
		this.actionControls = actionFireable;

		localBroadcastManager = LocalBroadcastManager.getInstance(context);
		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void save() {
		actionControls.fireAction(new Action(EActionType.SAVE));
		Toast.makeText(context, R.string.game_menu_saved, Toast.LENGTH_SHORT).show();
	}

	// mute the game when pausing whether or not its currently visibile
	public void pause() {
		actionControls.fireAction(new Action(EActionType.SPEED_SET_PAUSE));
		mute();
		paused = true;

		// Send a local broadcast so that any UI can update if necessary
		localBroadcastManager.sendBroadcast(new Intent(ACTION_PAUSE));
		notificationManager.notify(NOTIFICATION_ID, createNotification());
	}

	// don't unmute here, MapFragment will unmute when receiving unpause broadcast if its visibile.
	public void unPause() {
		actionControls.fireAction(new Action(EActionType.SPEED_UNSET_PAUSE));
		paused = false;

		// Send a local broadcast so that any UI can update if necessary
		localBroadcastManager.sendBroadcast(new Intent(ACTION_UNPAUSE));
		notificationManager.notify(NOTIFICATION_ID, createNotification());
	}

	public boolean isPaused() {
		return paused;
	}

	public void quit() {
		quitConfirmTimer = new Timer();

		quitConfirmTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (quitConfirmTimer != null) {
					quitConfirmTimer = null;
					notificationManager.notify(NOTIFICATION_ID, createNotification());
					localBroadcastManager.sendBroadcast(new Intent(ACTION_QUIT_CANCELLED));
				}
			}
		}, 3000);

		// Send a local broadcast so that any UI can update if necessary
		localBroadcastManager.sendBroadcast(new Intent(ACTION_QUIT));
		notificationManager.notify(NOTIFICATION_ID, createNotification());
	}

	public void quitConfirm() {
		// Trigger quit from here and callback in MainApplication broadcasts after quit is complete
		quitConfirmTimer = null;
		actionControls.fireAction(new Action(EActionType.EXIT));
	}

	public boolean canQuitConfirm() {
		return quitConfirmTimer != null;
	}

	public void mute() {
		soundPlayer.setPaused(true);
	}

	public void unMute() {
		soundPlayer.setPaused(false);
	}

	public Notification createNotification() {
		NotificationBuilder notificationBuilder = new NotificationBuilder(context);

		if (quitConfirmTimer == null) {
			notificationBuilder.addQuitButton();
		} else {
			notificationBuilder.addQuitConfirmButton();
		}

		notificationBuilder.addSaveButton();

		if (isPaused()) {
			notificationBuilder.addUnPauseButton();
		} else {
			notificationBuilder.addPauseButton();
		}

		return notificationBuilder.build();
	}

	private class NotificationBuilder {
		private final Context context;
		private final NotificationCompat.Builder builder;

		public NotificationBuilder(Context context) {
			this.context = context;

			Intent gameActivityIntent = new Intent(context, GameActivity.class);
			gameActivityIntent.setAction(Actions.RESUME_GAME);
			PendingIntent gameActivityPendingIntent = PendingIntent.getActivity(context, 0, gameActivityIntent, 0);

			builder = new NotificationCompat.Builder(context)
					.setSmallIcon(R.drawable.icon)
					.setContentTitle("Settlers game in progress")
					.setContentIntent(gameActivityPendingIntent);
		}

		public NotificationBuilder addQuitButton() {
			PendingIntent quitPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_QUIT), 0);
			builder.addAction(R.drawable.ic_stop, "Quit", quitPendingIntent);
			return this;
		}

		public NotificationBuilder addQuitConfirmButton() {
			PendingIntent quitPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_QUIT_CONFIRM), 0);
			builder.addAction(R.drawable.ic_stop, "Sure?", quitPendingIntent);
			return this;
		}

		public NotificationBuilder addSaveButton() {
			PendingIntent savePendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_SAVE), 0);
			builder.addAction(R.drawable.ic_save, "Save", savePendingIntent);
			return this;
		}

		public NotificationBuilder addPauseButton() {
			PendingIntent pausePendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_PAUSE), 0);
			builder.addAction(R.drawable.ic_pause, "Pause", pausePendingIntent);
			return this;
		}

		public NotificationBuilder addUnPauseButton() {
			PendingIntent unPausePendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_UNPAUSE), 0);
			builder.addAction(R.drawable.ic_play, "Unpause", unPausePendingIntent);
			return this;
		}

		public Notification build() {
			return builder.build();
		}
	}
}
