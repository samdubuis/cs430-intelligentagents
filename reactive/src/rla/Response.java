package rla;

import logist.topology.Topology.City;

public class Response {
	// city: destination for a move action, null for a pickup action
	private final City city;

	/**
	 * Creates an Accept / Pickup response
	 */
	public Response() {
		this.city = null;
	}

	/**
	 * Creates a Move response, with destination being city
	 *
	 * @param city the destination of the move action
	 */
	public Response(City city) {
		this.city = city;
	}

	public boolean isPickup() {
		return city == null;
	}

	public City moveDestination() {
		if (isPickup())
			throw new Error("Destination doesn't make sense for a pickup action!");
		return city;
	}

}
