package com.palashbansal.roadrage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.OnSheetDismissedListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.palashbansal.roadrage.helpers.AccidentItem;
import com.palashbansal.roadrage.helpers.ServerConnector;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	private GoogleMap mMap;
	private BottomSheetLayout bottomSheet;
	private boolean sheetShown;
	private GoogleApiClient mGoogleApiClient;
	private TileOverlay heatMapTile = null;
	private Location mLastLocation;
	private NotificationManager notificationManager;
	private Notification notification;
	private MediaPlayer mPlayer;

	private static final boolean simulating = false;

	private LatLng simulateLocation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);

		bottomSheet = (BottomSheetLayout) findViewById(R.id.bottomsheet);

		if(simulating){
			ActivityRecognitionIntentService.simulateDriving();
		}

		bottomSheet.addOnSheetDismissedListener(new OnSheetDismissedListener() {
			@Override
			public void onDismissed(BottomSheetLayout bottomSheetLayout) {
				sheetShown = false;
			}
		});

		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();



		final LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
		boolean gps_enabled = false;
		boolean network_enabled = false;

		try {
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch(Exception ignored) {}

		try {
			network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch(Exception ignored) {}

		if(!gps_enabled && !network_enabled) {
			// notify user
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setMessage("Location Services is not enabled");
			dialog.setPositiveButton("Enable GPS", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {
					Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivity(myIntent);
					try {
						if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
							finish();
						}
					} catch(Exception ignored) {}

				}
			});
			dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {
					finish();
				}
			});
			dialog.show();
		}

		new backgroundPoll().execute();
	}

	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;

		// Add a marker in Sydney and move the camera
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				if(mLastLocation==null||(mLastLocation.getLatitude()==0&&mLastLocation.getLongitude()==0)){
					new Handler().postDelayed(this, 2000);
					Toast.makeText(MapsActivity.this, "Waiting for location", Toast.LENGTH_SHORT).show();
				}else {
					LatLng loc = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
					mMap.addMarker(new MarkerOptions().position(loc).title("Current Location"));
					mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
					mMap.animateCamera(CameraUpdateFactory.zoomTo(14));

					addHeatMap(mLastLocation.getLatitude(), mLastLocation.getLongitude());
					Log.d("MAP", mLastLocation.toString());
				}
			}
		}, 2000);

		mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
			@Override
			public void onCameraChange(CameraPosition cameraPosition) {
				addHeatMap(cameraPosition.target.latitude, cameraPosition.target.longitude);
				simulateLocation  = new LatLng(cameraPosition.target.latitude, cameraPosition.target.longitude);
			}
		});

		mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
			@Override
			public void onMapClick(LatLng latLng) {
				showSheet(latLng);
			}
		});
	}

	private void addHeatMap(double latitude, double longitude) {
		ServerConnector.getHeatmapData(latitude, longitude, this, new ServerConnector.Listener() {
			@Override
			public void onFinished(@Nullable Object answer, @Nullable Exception error) {
				if (answer != null) {
					Log.d("MAP", "Asd");
					List<AccidentItem> accidents = (List<AccidentItem>) answer;
					if(accidents.size()==0) return;
					List<WeightedLatLng> data = new ArrayList<WeightedLatLng>(accidents.size());
					for (AccidentItem accident : accidents) {
						data.add(accident.getWeightedLatLng());
					}
					HeatmapTileProvider provider = new HeatmapTileProvider.Builder().weightedData(data).build();
					if(heatMapTile!=null)removeTile(heatMapTile);
					heatMapTile = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
				} else {
					assert error != null;
					error.printStackTrace();
				}
			}
		});
	}

	private void removeTile(final TileOverlay tile) {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				tile.remove();
			}
		}, 500);
	}

	void showSheet(LatLng latLng){
		bottomSheet.showWithSheetView(LayoutInflater.from(this).inflate(R.layout.place_details, bottomSheet, false));
		sheetShown = true;
		ServerConnector.getNearbyAccident(latLng.latitude, latLng.longitude, this, new ServerConnector.Listener() {
			@Override
			public void onFinished(@Nullable Object answer, @Nullable Exception error) {
				AccidentItem accident = (AccidentItem) answer;
				((TextView) findViewById(R.id.area_name)).setText(String.valueOf(accident.getLocation().latitude)
						+ ", " + String.valueOf(accident.getLocation().longitude));
				((TextView) findViewById(R.id.stat_text)).setText(String.valueOf(accident.getKilled()) + " died, "
						+ String.valueOf(accident.getInjured()) + " injured");
				((TextView) findViewById(R.id.rating_text)).setText(String.valueOf(accident.getDamageRating()*5));
				((RatingBar) findViewById(R.id.rating_bar)).setRating((float) (accident.getDamageRating() * 5));
				((TextView) findViewById(R.id.time_text)).setText(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(accident.getTimestamp()));
				((TextView) findViewById(R.id.story_text)).setText(accident.getStory());
			}
		});
	}

	@Override
	public void onConnected(@Nullable Bundle bundle) {
		mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
	}

	@Override
	public void onConnectionSuspended(int i) {

	}

	@Override
	protected void onStart() {
		mGoogleApiClient.connect();
		super.onStart();
	}

	@Override
	protected void onStop() {
		mGoogleApiClient.disconnect();
		super.onStop();
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

	}

	private class backgroundPoll extends AsyncTask{
		@Override
		protected Object doInBackground(Object[] params) {
			while(true){
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(mLastLocation==null)continue;
				LatLng location;
				if(simulating){
					location = simulateLocation;
				}else {
					location = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
				}
				ServerConnector.isProneArea(location.latitude, location.longitude, getApplicationContext(), new ServerConnector.Listener() {
					@Override
					public void onFinished(@Nullable Object answer, @Nullable Exception error) {
						if ((boolean) answer) {
							String message = "You have entered an accident prone area please be careful";
							triggerNotification(message);
							if (!currentlyInArea&&ActivityRecognitionIntentService.isDriving()){
								mPlayer = new MediaPlayer().create(MapsActivity.this, R.raw.audio);
								mPlayer.start();
								currentlyInArea = true;
							}
						} else if (notificationManager != null) {
							notificationManager.cancel(1010);
							mPlayer.stop();
							currentlyInArea = false;
						}
					}
				});
			}
		}
	}

	private Boolean currentlyInArea = false;

	private void triggerNotification(String s) {
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MapsActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setContentText(s).setContentTitle("Accident prone area").setContentIntent(pendingIntent)
				.setSmallIcon(R.mipmap.ic_launcher);
		;

		notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		if(Build.VERSION.SDK_INT>Build.VERSION_CODES.JELLY_BEAN){
			notification= builder.build();
		}else{
			notification = builder.getNotification();
		}
		notification.flags |= Notification.FLAG_NO_CLEAR|Notification.FLAG_ONGOING_EVENT;
		notificationManager.notify(1010, notification);
		Log.d("AsD", "were");
	}

	@Override
	protected void onDestroy() {
		notificationManager.cancel(1010);
		super.onDestroy();
	}
}
