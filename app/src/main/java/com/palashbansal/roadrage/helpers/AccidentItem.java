package com.palashbansal.roadrage.helpers;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.util.Date;

/**
 * Created by Palash on 4/3/2016.
 */
public class AccidentItem {
	private LatLng location;
	private String story;
	private int killed;
	private int injured;
	private double damageRating;
	private Date timestamp;
	private int id;

	public AccidentItem(int id, LatLng location, String story, int killed, int injured, double damageRating, long timestamp) {
		this.location = location;
		this.story = story;
		this.killed = killed;
		this.injured = injured;
		this.damageRating = damageRating;
		this.timestamp = new Date(timestamp);
		this.id = id;
	}

	public AccidentItem(double lat, double lon, double damageRating) {
		this.location = new LatLng(lat, lon);
		this.damageRating = damageRating;
		this.id = -1;
	}

	public WeightedLatLng getWeightedLatLng(){
		return new WeightedLatLng(location, 10*damageRating);
	}

	public LatLng getLocation() {
		return location;
	}

	public String getStory() {
		return story;
	}

	public int getKilled() {
		return killed;
	}

	public int getInjured() {
		return injured;
	}

	public double getDamageRating() {
		return damageRating;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public int getId() {
		return id;
	}
}
