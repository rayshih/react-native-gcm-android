package com.oney.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.content.res.Resources;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import java.lang.System;
import java.util.List;
import com.facebook.react.LifecycleState;
import com.facebook.react.ReactInstanceManager;

import java.lang.reflect.Field;

import io.neson.react.notification.NotificationPackage;

public class BackgroundService extends Service {
    private static final String TAG = "BackgroundService";
    private ReactInstanceManager mReactInstanceManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        sendNotification(intent.getBundleExtra("bundle"));
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mReactInstanceManager.onPause();
        mReactInstanceManager.onDestroy();
        mReactInstanceManager = null;
    }

    private Class getBuildConfigClass() {
        try {
            String packageName = getPackageName();

            return Class.forName(packageName + ".BuildConfig");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    private boolean getBuildConfigDEBUG() {
        Class klass = getBuildConfigClass();
        for (Field f : klass.getDeclaredFields()) {
            if (f.getName().equals("DEBUG")) {
                try {
                    return f.getBoolean(this);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

  public Class getMainActivityClass() {
    try {
      String packageName = getApplication().getPackageName();
      return Class.forName(packageName + ".MainActivity");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }

  private String getApplicationName() {
    final PackageManager pm = getApplicationContext().getPackageManager();

    ApplicationInfo ai;
    try {
      ai = pm.getApplicationInfo( this.getPackageName(), 0);
    } catch (final NameNotFoundException e) {
      ai = null;
    }

    return (String) (ai != null ? pm.getApplicationLabel(ai) : "");
  }

  private boolean applicationIsRunning() {
    ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
    List<RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
    for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
      if (processInfo.processName.equals(getApplication().getPackageName())) {
        if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
          for (String d: processInfo.pkgList) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void sendNotification(Bundle bundle) {
    Resources resources = getApplication().getResources();
    String packageName = getApplication().getPackageName();

    Class intentClass = getMainActivityClass();
    if (intentClass == null) {
      return;
    }

    if (applicationIsRunning()) {
      Intent i = new Intent("RNGCMReceiveNotification");
      i.putExtra("bundle", bundle);
      sendBroadcast(i);
      return;
    }

    int resourceId = resources.getIdentifier("small_icon", "mipmap", packageName);

    Intent intent = new Intent(this, intentClass);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.putExtra("bundle", bundle);
    int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, uniqueInt, intent,
      PendingIntent.FLAG_UPDATE_CURRENT);

    Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    Notification.Builder notificationBuilder = new Notification.Builder(this)
      .setSmallIcon(resourceId)
      .setContentTitle(getApplicationName())
      .setContentText(bundle.getString("notificationMessage"))
      .setAutoCancel(true)
      .setSound(defaultSoundUri)
      .setPriority(Notification.PRIORITY_HIGH)
      .setContentIntent(pendingIntent);

    NotificationManager notificationManager =
      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    Notification notif = notificationBuilder.build();
    notif.defaults |= Notification.DEFAULT_VIBRATE;
    notif.defaults |= Notification.DEFAULT_SOUND;
    notif.defaults |= Notification.DEFAULT_LIGHTS;

    notificationManager.notify(uniqueInt, notif);
  }
}
