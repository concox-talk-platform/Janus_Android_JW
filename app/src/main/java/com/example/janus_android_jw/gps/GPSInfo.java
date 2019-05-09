package com.example.janus_android_jw.gps;

public class GPSInfo {
	private long locationTime;
	private double latitude;
	private double longitude;
	private float speed;
	private float bearing;
	private byte mode;
	private byte supply;

	public long getTime() {
		return locationTime;
	}

	public void setTime(long locationTime) {
		this.locationTime = locationTime;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public float getSpeed() {
		return speed;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}

	public float getBearing() {
		return bearing;
	}

	public void setBearing(float bearing) {
		this.bearing = bearing;
	}

	public byte getMode() {
		return mode;
	}

	public void setMode(byte mode) {
		this.mode = mode;
	}

	public byte getSupply() {
		return supply;
	}

	public void setSupply(byte supply) {
		this.supply = supply;
	}

	@Override
	public String toString() {
		return "[locationTime:" + locationTime + ",latitude:" + latitude
				+ ",longitude:" + longitude + ",speed:" + speed + ",bearing:"
				+ bearing + ",mode:" + mode + ",supply:" + supply + "]";
	}
}
