package rla;

import logist.topology.Topology.City;

public class State {
	public City loc;
	public City dst;

	public State(City loc, City dst) {
		this.loc = loc;
		this.dst = dst;
	}

	public boolean isTaskAvailable() {
		return dst != null;
	}

	public double taskDistance() {
		if(dst == null)
			return Double.POSITIVE_INFINITY;

		return loc.distanceTo(dst);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		State state = (State) o;

		if (!loc.equals(state.loc))
			return false;

		if (dst == null)
			return state.dst == null;
		else
			return dst.equals(state.dst);
	}

	@Override
	public int hashCode() {
		int result = loc.hashCode();
		result = 31 * result + (dst != null ? dst.hashCode() : 0);
		return result;
	}
}
