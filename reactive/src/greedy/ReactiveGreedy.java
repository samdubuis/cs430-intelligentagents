package greedy;

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
import utils.CsvWriter;

import java.util.HashMap;

public class ReactiveGreedy implements ReactiveBehavior {

	private HashMap<City, Double> profits;
	private int numActions;
	private Agent myAgent;
	private CsvWriter csvWriter;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Get cost per km, knowing a reactive agent only uses the first vehicle of a company
		int costPerKm = agent.vehicles().get(0).costPerKm();
		profits = new HashMap<City, Double>();

		// Calculate expected profit in each city
		for (City c : topology) {
			double profit = 0; // unavailable tasks are counted as 0 profit
			for (City dst : topology)
				profit += td.probability(c, dst) * (td.reward(c, dst) - costPerKm * c.distanceTo(dst));
			profits.put(c, profit);
		}

		this.numActions = 0;
		this.myAgent = agent;
		this.csvWriter = new CsvWriter("reactive-greedy.csv");
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		City currentCity = vehicle.getCurrentCity();

		// We search for local max (greedy policy)
		City bestCity = null;
		double bestProfit = Double.NEGATIVE_INFINITY;

		if (availableTask != null) {
			City dst = availableTask.deliveryCity;
			bestProfit = availableTask.reward - vehicle.costPerKm() * currentCity.distanceTo(dst);
		}

		for (City c : currentCity.neighbors()) {
			double p = profits.get(c) - vehicle.costPerKm() * currentCity.distanceTo(c);
			if (p > bestProfit) {
				bestCity = c;
				bestProfit = p;
			}
		}

		// move to best city, or pickup if bestCity == null
		if (bestCity != null) {
			action = new Move(bestCity);
		} else {
			action = new Pickup(availableTask);
		}

		if (numActions >= 1) {
			System.out.println("The total greedy profit after " + numActions + " actions is " + myAgent.getTotalProfit() + " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
			csvWriter.add(numActions, myAgent.getTotalProfit(), myAgent.getTotalProfit() / myAgent.getTotalDistance());
		}
		numActions++;

		return action;
	}
}
