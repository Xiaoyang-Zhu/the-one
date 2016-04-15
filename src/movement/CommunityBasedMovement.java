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

import java.util.Map;

import core.Coord;
import core.DTNHost;
import core.Settings;

public class CommunityBasedMovement extends MovementModel {
	/* Number of the Community: 
	   e.g. if you have 12 communities, you could set the value to (3,4) or (4,3) */
	public static final String COMMUNITY_NUMBER = "communityNumber";
	/* Size of the map */
	public static final String	MAP_SIZE = "mapSize";
	/* Community identifier that should be identical to the Group ID in configuration file */
	public static final String	COMMUNITY_ID = "communityID";

	/* Probabilities of local or roaming epoch */
	public static final String	PROBABILITIES_LOCAL_ROAMING = "probabilities_local_roaming";
	
	/* Size of the map */
	private int		map_x_max = 100, map_y_max = 100;
	/* Number of the Community */
	private int 	community_x_number = 1, community_y_number = 1;
	
	/* Community identifier that should be identical to the Group ID in configuration file */
	private String[]	community_id;
	
	/* Communities' attributes include the community_id and the coordinates */
	private Map<String, double[]> community_attribute;

	/* Minimum unit of community */
	private double x_unit, y_unit;

	/* Selected destination */
	private Coord lastWaypoint;
	
	/* Selected community */
	private double [] selected_community = null;
	
	/* Probabilities of local or roaming epoch */
	private double p_l = 0.8, p_r = 0.2;
	
	/* Selecting not the current community */
	private int rnd_i;
	private double [] not_selected_community = null;
	
	/** how many waypoints should there be per path */
	private static final int PATH_LENGTH = 1;

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
		if (s.contains(PROBABILITIES_LOCAL_ROAMING)){
			double[] plr = s.getCsvDoubles(PROBABILITIES_LOCAL_ROAMING,2);
			this.p_l = plr[0];
			this.p_r = plr[1];
		}
		
		
		/* According to the setting parameters to initiate the coordinates information of each community */
		assert (this.community_id.length == (this.community_x_number) * 
				(this.community_y_number)) : "the number of communityID is not correct!";
		
		x_unit = map_x_max/community_x_number;
		y_unit = map_y_max/community_y_number;
		
		for (int i = 0; i < this.community_y_number; i++) {
			double community_x_min = 0, community_y_min = 0;
			double community_x_max = x_unit, community_y_max = y_unit;
			
			for (int j = 0; j < this.community_x_number; j++) {
				double[] community_attribute_coordinates = {community_x_min, community_y_min, 
						community_x_max * (j + 1), community_y_max * (i + 1)};
				
				community_attribute.put(this.community_id[(i + 1) * (j + 1)], community_attribute_coordinates);
				
				community_x_min = community_x_max;
			}		
			community_y_min = community_y_max;				
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
		for (Map.Entry<String, double[]> ca : community_attribute.entrySet()) {
			if (getHost().toString().startsWith(ca.getKey())) {
				selected_community = ca.getValue();
				Coord c = randomCoord(selected_community);
				this.lastWaypoint = c;
				return c;
			}
			
		}
		assert selected_community != null : "The node's id is not idential with the community's";
		return null;
	}

	@Override
	public Path getPath() {
		Path p;
		p = new Path(generateSpeed());
		p.addWaypoint(lastWaypoint.clone());
		Coord c = lastWaypoint;

		for (int i=0; i<PATH_LENGTH; i++) {
			c = local_roaming_selection();
			p.addWaypoint(c);
		}

		this.lastWaypoint = c;
		return p;
	}
	
	@Override
	public CommunityBasedMovement replicate() {
		return new CommunityBasedMovement(this);
	}
	
	protected Coord randomCoord(double[] community_area) {
		assert community_area.length == 4 : "Community's coordinates have errors!";
		
		double x = (rng.nextDouble()) * (community_area[2] - community_area[0])
				+ community_area[0];
		double y = (rng.nextDouble()) * (community_area[3] - community_area[1])
				+ community_area[1];

		return new Coord(x,y);
	}
	
	protected Coord local_roaming_selection() {
		 if (((rng.nextDouble()) * 100) < (p_l * 100)) {
			 return randomCoord(selected_community);
		 } else {
			 
			 do {
				  rnd_i = rng.nextInt(community_attribute.size());
				  not_selected_community = community_attribute.get(community_id[rnd_i]);
				 
			 }while((selected_community.equals(not_selected_community)));
			 
			 return randomCoord(not_selected_community);
		 }
		
		
		
	}

}
