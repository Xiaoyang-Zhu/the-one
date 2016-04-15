/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */

/** 
 * Random waypoint movement where the coordinates are restricted to circular
 * area defined by a central point and range.
 * @author teemuk
 */
package movement;

import java.util.*;
import core.Coord;
import core.Settings;

public class CommunityMovement extends MovementModel {
	/** Number of the Community */
	public static final String COMMUNITY_NUMBER = "communityNumber";
	/** Range of the Community */
	public static final String COMMUNITY_RANGE = "communityRange";
	/** Center point of the Community */
	public static final String COMMUNITY_CENTER = "communityCenter";
	public static final String MAX_SOCIAL_DEGREE = "MaxSocialDegree";

	private static final int PATH_LENGTH = 1;
	private Coord lastWaypoint;
	private int p_x_center = 100, p_y_center = 100;
	private double p_range = 100.0;

	private int[] CommunityXY;
	private double[] Range;
	private int community_number;
	private double SocialDegree;
	private int selectedC;
	private ArrayList<Integer> trace;
	static int hop = 0;

	public CommunityMovement(Settings s) {
		super(s);
		this.community_number = s.getInt(COMMUNITY_NUMBER);
		// this.selectedC = rng.nextInt(community_number);
		this.selectedC = 0;
		this.SocialDegree = s.getDouble(MAX_SOCIAL_DEGREE) * rng.nextDouble();

		this.trace = new ArrayList<Integer>();

		if (s.contains(COMMUNITY_CENTER)) {
			this.CommunityXY = s.getCsvInts(COMMUNITY_CENTER);
		}

		if (s.contains(COMMUNITY_RANGE)) {
			this.Range = s.getCsvDoubles(COMMUNITY_RANGE);
		}
	}

	private CommunityMovement(CommunityMovement cmv) {
		super(cmv);
		this.p_range = cmv.p_range;
		this.p_x_center = cmv.p_x_center;
		this.p_y_center = cmv.p_y_center;
		this.CommunityXY = cmv.CommunityXY;
		this.Range = cmv.Range;
		this.community_number = cmv.community_number;
		this.selectedC = cmv.selectedC;
		this.SocialDegree = cmv.SocialDegree;
		this.trace = cmv.trace;
	}

	@Override
	public Coord getInitialLocation() {
		assert rng != null : "MovementModel not initialized!";
		Coord c = randomCoord();
		this.lastWaypoint = c;
		return c;
	}

	@Override
	public Path getPath() {
		Path p;
		p = new Path(generateSpeed());
		p.addWaypoint(lastWaypoint.clone());
		Coord c = lastWaypoint;

		for (int i = 0; i < PATH_LENGTH; i++) {
			c = randomCoord();

			p.addWaypoint(c);
		}

		this.lastWaypoint = c;

		int select = this.selectedC;
		int total = 0;
		if (trace.isEmpty()) {
			for (int i = 0; i < community_number; i++)
				trace.add(i, 0);
		}
		trace.set(select, trace.get(select) + 1);

		if (hop++ % community_number == 0) {
			for (int i = 0; i < community_number; i++)
				total += trace.get(i);
			if (total > 0) {
				for (int j = 0; j < community_number; j++)
					if (trace.get(j) > 0)
						getHost().getTrace().put(String.valueOf(j),
								1.0 * trace.get(j) / total);
			}
		}
		return p;
	}

	@Override
	public CommunityMovement replicate() {
		return new CommunityMovement(this);
	}

	protected Coord randomCoord() {
		this.selectedC = selectDestination();
		this.p_x_center = CommunityXY[selectedC * 2];
		this.p_y_center = CommunityXY[selectedC * 2 + 1];
		this.p_range = Range[selectedC];

		double x = (rng.nextDouble() * 2 - 1) * this.p_range;
		double y = (rng.nextDouble() * 2 - 1) * this.p_range;
		while (x * x + y * y > this.p_range * this.p_range) {
			x = (rng.nextDouble() * 2 - 1) * this.p_range;
			y = (rng.nextDouble() * 2 - 1) * this.p_range;
		}
		x += this.p_x_center;
		y += this.p_y_center;
		return new Coord(x, y);
	}

	protected int selectDestination() {
		double random = rng.nextDouble();
		int home = Integer.parseInt(getHost().getgroupID());
		int select = home;

		if (hop == 0)
			return select;
		if (!getHost().isSTATION())
			getHost().setSocialDegree(this.SocialDegree);
		if (random > 1 - getHost().getSocialDegree()) {
			boolean skip = false;
			do {
				select = rng.nextInt(community_number);
				if ((select == home - 1 && home != 1 && home != 5 && home != 9)
						|| (select == home + 1 && home != 4 && home != 8 && home != 12)
						|| (select == home - 4 && home != 1 && home != 2
								&& home != 3 && home != 4)
						|| (select == home + 4 && home != 13 && home != 14
								&& home != 15 && home != 16))
					skip = true;
			} while (!skip);
		}
		return select;
	}
}
