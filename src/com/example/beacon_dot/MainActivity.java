package com.example.beacon_dot;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.ActionMode;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.BeaconManager.MonitoringListener;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;

public class MainActivity extends Activity {
	private String userID = "Your Name";
	
	//List<Item> itemList;
	//DBActivity dbApp;
	//ArrayAdapter<Item> lvArrayAdapter;
	ActionMode mActionMode = null;
	public static final String CLASS_NAME = "MainActivity";

	/*Insert here the UUID of your beacon
	 * You can find it using applications such as iBeacon Locate (on google play)
	 */
	private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";

	private static final Region ALL_ESTIMOTE_BEACONS = new Region("CreativeRoom",
			ESTIMOTE_PROXIMITY_UUID, null, null);

	protected static final String TAG = "EstimoteiBeacon";

	private static final int NOTIFICATION_ID = 123;
	
	public int flagRegion = 0;

	BeaconManager beaconManager;
	NotificationManager notificationManager;

	@Override
	/**
	 * onCreate called when main activity is created.
	 * 
	 * Sets up the itemList, application, and sets listeners.
	 *
	 * @param savedInstanceState
	 */
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.access_control_activity);
		String temp = getIntent().getStringExtra("username");
		if (!temp.equals("")) {
			userID=temp;		
		}
		/* use application class to maintain global state */
		//dbApp = (DBActivity) getApplication();

		beaconManager = new BeaconManager(this);
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(1), 0);
		
		if (!beaconManager.isBluetoothEnabled()) {
			alertbox("Alert", "Please, activate your bluetooth");
		}
		
		beaconManager.setMonitoringListener(new MonitoringListener() {
			@Override
			public void onEnteredRegion(Region region, List<Beacon> arg1) {
				if (isAppInForeground(getApplicationContext())) {
					Toast.makeText(getApplicationContext(), "Entered region",
							Toast.LENGTH_LONG).show();
				
					TextView txtAccessControl = (TextView)findViewById(R.id.txtAccessControl);
					txtAccessControl.setText("Welcome in Asiance!");
					
					// ---range for beacons---
					try {
						beaconManager.startRanging(ALL_ESTIMOTE_BEACONS);
					} catch (RemoteException e) {
						Log.e(TAG, "Cannot start ranging", e);
					}
				} else {
					//postNotification("Entered region");
				}
			}

			@Override
			public void onExitedRegion(Region region) {
				/*Reset	this flag, when enter region again and it is "IMMEDIATE"
				 * it sends again to the cloud a message*/
				
				flagRegion = 0; 
				
				if (isAppInForeground(getApplicationContext())) {
					Toast.makeText(getApplicationContext(), "Exited region",
							Toast.LENGTH_LONG).show();
					
					TextView txtAccessControl = (TextView)findViewById(R.id.txtAccessControl);
					txtAccessControl.setText("See you next time!");
				} 

				// ---stop ranging for beacons---
				try {
					beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
				} catch (RemoteException e) {
					Log.e(TAG, e.getLocalizedMessage());
				}
			}
		});
		// ---called when beacons are found---
		beaconManager.setRangingListener(new BeaconManager.RangingListener() {
			@Override
			public void onBeaconsDiscovered(Region region,
					final List<Beacon> beacons) {
				Log.d(TAG, "Ranged beacons: " + beacons);

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (beacons.size() > 0) {
							Beacon iBeacon1 = null;
							// ---get the first
							// iBeacon---
							iBeacon1 = beacons.get(0);

					/*If it is on region, it's "IMMEDIATE" and flag is 0 
					 * (first time it is IMMEDIATE since it entered the 
					 * region) it sends a message to the cloud with the ID.
					 * */		
							if(String.valueOf(Utils.
									proximityFromAccuracy(Utils.
											computeAccuracy(iBeacon1))) == "IMMEDIATE" 
									&& flagRegion == 0){
								
								flagRegion++;
								postNotification("Welcome to Asiance, " + userID);
							}
							
						} 
					}
				});
			}
		});

	}

	DecimalFormat df = new DecimalFormat("#.##");

	@Override
	protected void onStart() {
		super.onStart();

		notificationManager.cancel(NOTIFICATION_ID);
		beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
			@Override
			public void onServiceReady() {
				try {
					beaconManager.startMonitoring(ALL_ESTIMOTE_BEACONS);
				} catch (RemoteException e) {
					Log.d(TAG, "Error while starting monitoring");
				}
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		notificationManager.cancel(NOTIFICATION_ID);
		beaconManager.disconnect();
	}

	private void postNotification(String msg) {
		Intent notifyIntent = new Intent(MainActivity.this, MainActivity.class);

		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivities(
				MainActivity.this, 0, new Intent[] { notifyIntent },
				PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notification = new Notification.Builder(MainActivity.this)
				.setSmallIcon(R.drawable.logo)
				.setContentTitle("Asiance monitoring region").setContentText(msg)
				.setAutoCancel(true).setContentIntent(pendingIntent).build();
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	// ---helper method to determine if the app is in
	// the foreground---
	public static boolean isAppInForeground(Context context) {
		List<RunningTaskInfo> task = ((ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE)).getRunningTasks(1);
		if (task.isEmpty()) {
			return false;
		}
		return task.get(0).topActivity.getPackageName().equalsIgnoreCase(
				context.getPackageName());
	}

	public void alertbox(String title, String mymessage) {
		new AlertDialog.Builder(this)
				.setMessage(mymessage)
				.setTitle(title)
				.setCancelable(true)
				.setNeutralButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// ((AlertDialog)dialog).getButton(whichButton).setVisibility(View.INVISIBLE);
								finish();
							}
						}).show();
	}

	@Override
	protected void onStop() {
		super.onStop();
		try {
			beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
		} catch (RemoteException e) {
			Log.e(TAG, "Cannot stop", e);
		}
	}

}
