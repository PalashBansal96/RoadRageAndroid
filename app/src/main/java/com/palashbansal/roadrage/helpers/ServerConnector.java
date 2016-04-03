package com.palashbansal.roadrage.helpers;

import android.content.Context;
import android.support.annotation.Nullable;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Palash on 4/3/2016.
 */
public class ServerConnector {

	private static final List<AccidentItem> accidents = new ArrayList<>();
	private static final String BASE_URL = "https://api.roadrage.darkryder.me/";
	private static final String HM_PARAM = "heatmap_coords";
	private static final String NA_PARAM = "nearest_accident";
	private static final String AW_PARAM = "analyse_and_warn";

	public static void getHeatmapData(double lat, double lon, Context context, @Nullable final ServerConnector.Listener listener){
		String url = BASE_URL + HM_PARAM + "?lat=" + String.valueOf(lat) + "&lon=" + String.valueOf(lon);
		accidents.clear();
		getJSONFromGet(url, context, new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) {
				try {
					JSONArray array = response.getJSONArray("accidents");
					for (int i = 0; i < array.length(); i++) {
						JSONObject j = array.getJSONObject(i);
						accidents.add(new AccidentItem(j.getDouble("l"), j.getDouble("L"), j.getDouble("D")));
					}
					if (listener != null) {
						listener.onFinished(accidents, null);
					}
				} catch (JSONException e) {
					if (listener != null) {
						listener.onFinished(null, e);
					}
				}
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				if (listener != null) {
					listener.onFinished(null, error);
				}
			}
		});
	}

	private static void getJSONFromGet(String url, Context context, Response.Listener<JSONObject> listener, @Nullable Response.ErrorListener errorListener) {
		VolleyRequestQueue.addToRequestQueue(
				new JsonObjectRequest(Request.Method.GET, url, new JSONObject(),
						listener, errorListener
				),
				context);
	}

	public static void getNearbyAccident(double lat, double lon, Context context, @Nullable final ServerConnector.Listener listener){
		String url = BASE_URL + NA_PARAM + "?lat=" + String.valueOf(lat) + "&lon=" + String.valueOf(lon);
		accidents.clear();
		getJSONFromGet(url, context, new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) {
				try {
					AccidentItem accident = new AccidentItem(0, new LatLng(response.getDouble("latitude"), response.getDouble("longitude")),
							response.getString("crashDetail"), response.getInt("killed"), response.getInt("injured"),
							response.getDouble("damageRating"), response.getLong("datetime"));
					if (listener != null) {
						listener.onFinished(accident, null);
					}
				} catch (JSONException e) {
					if (listener != null) {
						listener.onFinished(null, e);
					}
				}
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				if (listener != null) {
					listener.onFinished(null, error);
				}
			}
		});
	}

	public static void isProneArea(double lat, double lon, Context context, @Nullable final ServerConnector.Listener listener){
		String url = BASE_URL + AW_PARAM + "?lat=" + String.valueOf(lat) + "&lon=" + String.valueOf(lon);
		accidents.clear();
		getJSONFromGet(url, context, new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) {
				try {
					if (listener != null) {
						listener.onFinished(response.getBoolean("in_accident_prone_area"), null);
					}
				} catch (JSONException e) {
					if (listener != null) {
						listener.onFinished(null, e);
					}
				}
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				if (listener != null) {
					listener.onFinished(null, error);
				}
			}
		});
	}

	public interface Listener {
		void onFinished(@Nullable Object answer, @Nullable Exception error);
	}

	public static List<AccidentItem> getAccidents() {
		return accidents;
	}

	// /api/balance
	// /api/history
	// /api/loan

}
