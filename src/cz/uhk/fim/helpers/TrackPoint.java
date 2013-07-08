package cz.uhk.fim.helpers;

import java.util.ArrayList;

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
 * Class for information about location.
 * @author Tomáš Voslař
 */
public class TrackPoint {
	
	/**
	 * Definition of constant - kilometers per hour
	 */
	public static final int KMH = 0;
	
	/**
	 * Definition of constant - miles per hour
	 */
	public static final int MPH = 1;
	
	/**
	 * Definition of constant - meters per second
	 */
	public static final int MS = 2;
	
	/**
	 * Definition of constant - title of kilometers per hour
	 */
	public static final String KMHTITLE = "km/h";
	
	/**
	 * Definition of constant - title of meters per hour
	 */
	public static final String MPHTITLE = "mph";
	
	/**
	 * Definition of constant - title of meters per second
	 */
	public static final String MSTITLE = "m/s";
	
	/**
	 * Holds points of TrackPoinItem.
	 */
	private ArrayList<TrackPointItem> route = null;

	/**
	 * Constructor.
	 */
	public TrackPoint() {
		super();
		this.route = new ArrayList<TrackPointItem>();
	}

	/**
	 * Saves point into route.
	 * @param TrackPointItem tpi point
	 */
	public void addPoint(TrackPointItem tpi){
		if(route.size() > 0) route.add(1, route.get(0));
		route.add(0, tpi);
	}
	
	/**
	 * Returns speed calculated with two points of route.
	 * @return float
	 */
	public float getSpeed(){
		
		if(route.size() > 1){
		
			double ltActual = route.get(0).getLatitude();
			double lngActual = route.get(0).getLongtitude();
			
			double ltLast = route.get(1).getLatitude();
			double lngLast = route.get(1).getLongtitude();
			
			Long delta = route.get(0).getTime() - route.get(1).getTime();
	
			// speed calculation
			return (float) Math.sqrt(Math.exp((ltActual - ltLast)) + Math.exp((lngActual - lngLast))) / delta;
		}else{
			return 0;
		}
	}
	
	/**
	 * Returns title of speed.
	 * @param index
	 * @return String
	 */
	protected String getUnits(int index){
		
		String units;
		
		switch (index) {
		case KMH:
			units = KMHTITLE;
			break;
		
		case MPH:
			units = MPHTITLE;
			break;
			
		default:
			units = MSTITLE;
			break;
		}
		
		return units;
	}
	
	/**
	 * Recalculate speed into another units.
	 * @param speed
	 * @param units
	 * @return String
	 */
	public String calculateInto(float speed, int units) {
		
		float newValue;
		
		switch (units) {
			case KMH:
				newValue = (float) (speed * 3.6);
			break;
			case MPH:
				newValue = (float) (speed / 0.44704);
			break;
			default:
				// defaultni m/s
				newValue = speed;
			break;
		}
		
		return Float.toString(newValue) + getUnits(units);
	}
	
}
