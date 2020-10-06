package rla;

import logist.task.TaskDistribution;
import logist.topology.Topology.City;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Learning {
	// Initial setup
	private final Set<State> states;
	private final Set<Response> actions;
	private final TaskDistribution td;
	private final int costPerKm;
	private final double discount;

	public Learning(TaskDistribution td, int costPerKm, double discount) {
		this.td = td;
		this.costPerKm = costPerKm;
		this.discount = discount;

		this.states = new HashSet<State>();
		this.actions = new HashSet<Response>();
	}

	public void addState(State state) {
		states.add(state);
	}

	public void addAction(Response action) {
		actions.add(action);
	}

	public HashMap<State, Response> optimize() {
		final double MAX_DIFF = 1e-6;
		double diff = Double.POSITIVE_INFINITY;

		HashMap<State, Double> values = new HashMap<State, Double>(); // vector V in the lecture
		HashMap<State, Response> policy = new HashMap<State, Response>(); // policy Pi in the lecture

		// while not good enough
		while (diff > MAX_DIFF) {
			HashMap<State, Double> new_values = new HashMap<State, Double>();

			// loop over states
			for (State s : states) {
				double max_val = Double.NEGATIVE_INFINITY;
				Response max_action = null;

				// find best action / best value over all possible actions
				for (Response a : actions) {
					// calculate Q(s, a)
					double Q = reward(s, a);
					for (State next : states)
						Q += discount * transition(s, a, next) * values.getOrDefault(next, 0.0);

					// keep this action if better
					if (Q > max_val) {
						max_val = Q;
						max_action = a;
					}
				}

				new_values.put(s, max_val);
				policy.put(s, max_action);
			}

			// calculate diff between old and new values
			diff = 0;
			for (State s : states) {
				diff += Math.abs(values.getOrDefault(s, 0.0) - new_values.get(s));
			}
			values = new_values;
		}

		return policy;
	}

	/**
	 * Reward function, returning expected profit from taking action a in state s
	 *
	 * @param s initial state
	 * @param a action to be taken
	 * @return expected profit
	 */
	private double reward(State s, Response a) {
		// First type of action: Accept / Pickup the task
		if (a.isPickup()) {
			// pickup action only makes sense if state has a task
			if (!s.isTaskAvailable())
				return Double.NEGATIVE_INFINITY;

			return td.reward(s.loc, s.dst) - costPerKm * s.taskDistance();
		}
		// Second type of action: Move to city c
		else {
			City c = a.moveDestination();
			// move action only makes sense if destination is a neighbor
			if (!s.loc.hasNeighbor(c))
				return Double.NEGATIVE_INFINITY;

			return -costPerKm * s.loc.distanceTo(c);
		}
	}

	/**
	 * Transition function, returning probability of transition (init, a) -> next
	 *
	 * @param init initial state we are in
	 * @param a    action taken
	 * @param next potential next state
	 * @return probability of ending up in `next` state, knowing we take action `a` in state `init`
	 */
	private double transition(State init, Response a, State next) {
		// First type of action: Accept / Pickup the task
		if (a.isPickup()) {
			// pickup action only makes sense if initial state has a task
			if (!init.isTaskAvailable())
				return 0;

			// probability of going anywhere that is NOT the current task destination is 0
			if (!init.dst.equals(next.loc))
				return 0;

			return td.probability(next.loc, next.dst);
		}
		// Second type of action: Move to city c
		else {
			City c = a.moveDestination();
			// move action only makes sense if destination is a neighbor
			if (!init.loc.hasNeighbor(c))
				return 0;

			// probability of going anywhere that is NOT the destination of the move action is 0
			if (!c.equals(next.loc))
				return 0;

			return td.probability(next.loc, next.dst);
		}
	}
}
