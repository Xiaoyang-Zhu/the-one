/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

/**
 * CommunityBasedMovement where the coordinates are restricted to rectangle
 * area defined by two coordinates in the entire map.
 * @author Xiaoyang Zhu
 */
package movement;

import core.Coord;
import core.DTNHost;
import core.Settings;

public class CommunityBasedMovement extends RandomWaypoint {
	/* Number of the Community: 
	   e.g. if you have 12 communities, you could set the value to (3,4) or (4,3) */
	public static final String COMMUNITY_NUMBER = "communityNumber";
	/** Size of the map */
	public static final String	MAP_SIZE = "mapSize";
	/* Community identifier that should be identical to the Group ID in configuration file */
	public static final String	COMMUNITY_ID = "communityID";

	private int		map_x_max = 100, map_y_max = 100;
	private int 	community_x_number = 1, community_y_number = 1;
	private String[]	community_id;
	
	private double x_unit, y_unit;

	private Coord lastWaypoint;

	public CommunityBasedMovement(Settings s) {
		super(s);

		if (s.contains(COMMUNITY_NUMBER)){
			int[] cn = s.getCsvInts(COMMUNITY_NUMBER,2);
			this.community_x_number = cn[0];
			this.community_y_number = cn[1];
		}
		if (s.contains(MAP_SIZE)){
			int[] ms = s.getCsvInts(MAP_SIZE,2);
			this.map_x_max = ms[0];
			this.map_y_max = ms[1];
		}
		if (s.contains(COMMUNITY_ID)){
			this.community_id = s.getSetting(COMMUNITY_ID).split(",");
		}
		
		assert (this.community_id.length != (this.community_x_number) * 
				(this.community_y_number)) : "the number of communityID is not correct!";
		
		x_unit = map_x_max/community_x_number;
		y_unit = map_y_max/community_y_number;
		
		for (int i = 0; i < (this.community_x_number) * (this.community_y_number); i++) {
			
			
		}
		
		
		
	}

	private CommunityBasedMovement(CommunityBasedMovement cmv) {
		super(cmv);
		this.community_x_number = cmv.community_x_number;
		this.community_y_number = cmv.community_y_number;
		this.map_x_max = cmv.map_x_max;
		this.map_y_max = cmv.map_y_max;
	}

	/**
	 * Returns a possible (random) placement for a host
	 * @return Random position on the map
	 */
	@Override
	public Coord getInitialLocation() {
		assert rng != null : "MovementModel not initialized!";
		getHost().toString();
		Coord c = randomCoord();

		this.lastWaypoint = c;
		return c;
	}
	
	@Override
	protected Coord randomCoord() {
		double x = (rng.nextDouble()*2 - 1)*this.p_range;
		double y = (rng.nextDouble()*2 - 1)*this.p_range;
		while (x*x + y*y>this.p_range*this.p_range) {
			x = (rng.nextDouble()*2 - 1)*this.p_range;
			y = (rng.nextDouble()*2 - 1)*this.p_range;
		}
		x += this.p_x_center;
		y += this.p_y_center;
		return new Coord(x,y);
	}

	@Override
	public int getMaxX() {
		return (int)Math.ceil(this.p_x_center + this.p_range);
	}

	@Override
	public int getMaxY() {
		return (int)Math.ceil(this.p_y_center + this.p_range);
	}

	@Override
	public ClusterMovement replicate() {
		return new ClusterMovement(this);
	}
}
