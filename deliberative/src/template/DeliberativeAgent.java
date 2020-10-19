package template;

/* import table */

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeAgent implements DeliberativeBehavior {

	enum Algorithm {BFS, ASTAR}

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;
	int numActions = 0;

	/* the planning class */
	Algorithm algorithm;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;

		// initialize the planner
		capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

		// ...
	}


	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		long startTime = System.currentTimeMillis();
		float elapsedTime = 0;
		
		Plan plan;
		Node lastNode = null;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			lastNode = Algorithms.ASTAR(vehicle, tasks);
	        elapsedTime = (System.currentTimeMillis() - startTime) / 1000f;
	        System.out.println("Plan time for ASTAR: " + elapsedTime + "s");
			break;
		case BFS:
			lastNode = Algorithms.BFS(vehicle, tasks);
	        elapsedTime = (System.currentTimeMillis() - startTime) / 1000f;
	        System.out.println("Plan time for BFS: " + elapsedTime + "s");
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		
		return deliberativePlan(lastNode, vehicle);
	}


	/*
	 * deliberativePlan function to compute the plan based on the last node and by going backward
	 * see descendingIterator and backwardAction
	 */
	private Plan deliberativePlan(Node lastNode, Vehicle vehicle) {
		Plan plan = new Plan(vehicle.getCurrentCity());
		Node node = lastNode;

		LinkedList<Action> backwardAction = new LinkedList<Action>();

		while (node.getPreviousAction() != null) {
			backwardAction.add(node.getPreviousAction());
			node = node.getParentNode();
		}

		Iterator<Action> it = backwardAction.descendingIterator();

		while (it.hasNext()) {
			plan.append(it.next());
		}

		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {

		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}




























