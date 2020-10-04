package template;

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
import java.util.Random;

public class Reactive implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;

	private HashMap<City, State> states;
	
	private HashMap<State, Action> reward;
	
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		
		
		int costPerKm = agent.vehicles().get(0).costPerKm();
		states = new HashMap<Topology.City, State>();
		
		for (City each : topology.cities()) {
			for (City to : topology.cities()) {
				State state = new State(each, to, costPerKm, td);
				states.put(each, state);
			}
		}
		
		
		
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		
		City currentLoc = vehicle.getCurrentCity();
		if (availableTask == null) {
			// no task to take
			action = new Move(states.get(currentLoc).get(null).bestDst);
		} else {
			bestDst = states.get(currentLoc).get(availableTask.deliveryCity).bestDst;
			if (bestDst.name == availableTask.deliveryCity.name) {
				// taking the task
				action = new Pickup(availableTask);
			} else {
				// not taking the task
				action = new Move(bestDst);
			}
		}
		
		
		if (numActions >= 1) {
			System.out.println("The total profit after " + numActions + " actions is " + myAgent.getTotalProfit() + " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
		}
		numActions++;

		return action;
	}
}
