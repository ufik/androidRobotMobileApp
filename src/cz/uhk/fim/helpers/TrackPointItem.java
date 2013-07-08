package cz.uhk.fim.helpers;

/**
 *   Copyright (C) <2013>  <Tomáš Voslař (t.voslar@gmail.com)>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Class for Track point item, which holds information about one location point.
 * @author Tomáš Voslař
 */
public class TrackPointItem {
	
	/**
	 * Coordinates of gps location.
	 */
	private double longtitude, latitude;
	
	/**
	 * Time stamp of location.
	 */
	private long time;
	
	/**
	 * Constructor.
	 * @param longtitude
	 * @param latitude
	 * @param time
	 */
	public TrackPointItem(double longtitude, double latitude, long time) {
		super();
		this.longtitude = longtitude;
		this.latitude = latitude;
		this.time = time;
	}

	/**
	 * Getter of longtitude.
	 * @return the longtitude
	 */
	public double getLongtitude() {
		return longtitude;
	}

	/**
	 * Setter of longtitude.
	 * @param longtitude the longtitude to set
	 */
	public void setLongtitude(double longtitude) {
		this.longtitude = longtitude;
	}

	/**
	 * Getter of latitude.
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Setter of latitude.
	 * @param latitude the latitude to set
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * Getter of time stamp.
	 * @return the time
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Setter of time stamp. 
	 * @param time the time to set
	 */
	public void setTime(long time) {
		this.time = time;
	}
}