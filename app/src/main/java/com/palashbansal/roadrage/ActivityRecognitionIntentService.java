package com.palashbansal.roadrage;

/**
 * Created by Palash on 4/3/2016.
 */

import android.app.IntentService;
import android.content.Intent;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * Service that receives ActivityRecognition updates. It receives updates
 * in the background, even if the main Activity is not visible.
 */
public class ActivityRecognitionIntentService extends IntentService {
	/**
	 * Creates an IntentService.  Invoked by your subclass's constructor.
	 *
	 * @param name Used to name the worker thread, important only for debugging.
	 */

	private static boolean driving = false;
	private static final String TAG = "ActivityRecognitionIntentService";
	private static boolean simulating = false;

	public ActivityRecognitionIntentService() {
		super(TAG);
	}

	/**
	 * Called when a new activity detection update is available.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		//...
		// If the intent contains an update
		if (ActivityRecognitionResult.hasResult(intent)) {
			// Get the update
			ActivityRecognitionResult result =
					ActivityRecognitionResult.extractResult(intent);

			DetectedActivity mostProbableActivity
					= result.getMostProbableActivity();

			// Get the confidence % (probability)
			int confidence = mostProbableActivity.getConfidence();

			// Get the type
			int activityType = mostProbableActivity.getType();
           /* types:
            * DetectedActivity.IN_VEHICLE
            * DetectedActivity.ON_BICYCLE
            * DetectedActivity.ON_FOOT
            * DetectedActivity.STILL
            * DetectedActivity.UNKNOWN
            * DetectedActivity.TILTING
            */
			driving = activityType == DetectedActivity.IN_VEHICLE;
		}
	}

	public static boolean isDriving() {
		return simulating||driving;
	}

	public static void simulateDriving() {
		simulating = true;
	}
}
