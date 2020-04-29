package com.cntwo.wifitransfer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MenuActivity extends AppCompatActivity {

	private static final int REQUEST_PERMISSION = 0;
	TextView instructionsTextView;
	TextView ipAddressTextView;
	Button button;
	ImageView wifiLed;
	ImageView hddLed;

	static HttpFileServer httpFileServer;
	static String formattedIpAddress;
	static ConnectivityManager cm;
	static boolean hasConnection;
	static short port = 8080;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);

		instructionsTextView = findViewById(R.id.instructions_text_view);
		ipAddressTextView = findViewById(R.id.ip_address_text_view);
		button = findViewById(R.id.button);
		wifiLed = findViewById(R.id.wifi_led);
		hddLed = findViewById(R.id.hdd_led);


		cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		httpFileServer = new HttpFileServer(port, getApplicationContext(), this);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(new BroadcastReceiver() {
			                 @Override
			                 public void onReceive(Context context, Intent intent) {
				                 hasConnection = cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;
				                 if(hasConnection) {
					                 WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
					                 final int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
					                 formattedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
							                 (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
					                 button.setText(R.string.run);
					                 instructionsTextView.setText(R.string.serverOffline);
					                 ipAddressTextView.setText("");
				                 } else {
					                 if(httpFileServer != null) httpFileServer.terminate();
					                 instructionsTextView.setText(R.string.noActiveConnection);
					                 ipAddressTextView.setText(R.string.pleaseConnectToWiFiFirst);
				                 }
			                 }
		                 }

				, intentFilter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		hasConnection = cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;
		if(hasConnection) {
			WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
			final int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
			formattedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
					(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
			if(httpFileServer.isAlive()) {
				instructionsTextView.setText(R.string.typeInBrowser);
				ipAddressTextView.setText(formattedIpAddress + ":" + port);
			} else {
				instructionsTextView.setText(R.string.serverOffline);
				ipAddressTextView.setText("");
			}
		} else {
			instructionsTextView.setText(R.string.noActiveConnection);
			ipAddressTextView.setText(R.string.pleaseConnectToWiFiFirst);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(httpFileServer != null) httpFileServer.terminate();
	}

	public void switchButtonPressed(View view) {
		if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
					REQUEST_PERMISSION);
		}
		else{
			if (hasConnection) {
				if (httpFileServer.isAlive()) {
					button.setText(R.string.run);
					instructionsTextView.setText(R.string.serverOffline);
					ipAddressTextView.setText("");
					httpFileServer.terminate();
				} else {
					button.setText(R.string.stop);
					instructionsTextView.setText(R.string.typeInBrowser);
					ipAddressTextView.setText(formattedIpAddress + ":" + port);
					httpFileServer.create();
				}
			}
		}
	}

	public void settingsButtonPressed(View view) {
		Intent intent = new Intent(this, SettingsActivity.class);
		intent.putExtra("Port", port);
		startActivityForResult(intent, 0);
	}

	public void wifi(View view){
		WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		if (wifiInfo != null) {
			int linkSpeed = wifiInfo.get(); //measured using WifiInfo.LINK_SPEED_UNITS
			System.out.println("WIFI SPEED IS"+linkSpeed);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(data != null) {
			final boolean serverWasRunning = httpFileServer.isAlive();
			final int newPort = data.getIntExtra("Port", 8080);
			if(newPort != port) {
				port = (short)newPort;
				httpFileServer.terminate();
				httpFileServer = new HttpFileServer(port, getApplicationContext(), this);
				if(serverWasRunning) {
					httpFileServer.create();
				}
			}
		}
	}

	protected void activateWiFiLED() {
		new AsyncTask<Void, Boolean, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				for(byte i = 0 ; i < 2 ; i++) {
					publishProgress(true);
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
					publishProgress(false);
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(Boolean... state) {
				super.onProgressUpdate(state);
				if(state[0]) {
					wifiLed.setImageResource(R.drawable.led_on);
				} else {
					wifiLed.setImageResource(R.drawable.led_off);
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	protected void activateHDDLED() {
		new AsyncTask<Void, Boolean, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				for(byte i = 0 ; i < 2 ; i++) {
					publishProgress(true);
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
					publishProgress(false);
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(Boolean... state) {
				super.onProgressUpdate(state);
				if(state[0]) {
					hddLed.setImageResource(R.drawable.led_on);
				} else {
					hddLed.setImageResource(R.drawable.led_off);
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}
