package rla;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.HashMap;

public class Reactive implements ReactiveBehavior {

	private int numActions;
	private Agent myAgent;
	private HashMap<State, Response> policy;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		this.numActions = 0;
		this.myAgent = agent;

		// Get cost per km, knowing a reactive agent only uses the first vehicle of a company
		int costPerKm = agent.vehicles().get(0).costPerKm();
		Learning rla = new Learning(td, costPerKm, discount);

		// Create the set of all possible states
		for (City loc : topology.cities()) {
			rla.addState(new State(loc, null)); // the state with no available task
			for (City dst : topology.cities()) {
				rla.addState(new State(loc, dst));
			}
		}

		// Create the set of all possible actions
		rla.addAction(new Response()); // pickup action
		for (City to : topology.cities()) {
			rla.addAction(new Response(to)); // move action
		}

		// Run RLA
		policy = rla.optimize();
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		// Get best policy for current state
		State current = getCurrentState(vehicle, availableTask);
		Response response = policy.get(current);

		if (response.isPickup()) {
			action = new Pickup(availableTask);
		} else {
			action = new Move(response.moveDestination());
		}

		if (numActions >= 1) {
			System.out.println("The total profit after " + numActions + " actions is " + myAgent.getTotalProfit() + " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
		}
		numActions++;

		return action;
	}

	private State getCurrentState(Vehicle v, Task t) {
		City loc = v.getCurrentCity();
		City dst = t != null ? t.deliveryCity : null;

		return new State(loc, dst);
	}
}
