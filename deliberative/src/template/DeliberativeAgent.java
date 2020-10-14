package template;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Action;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;
/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeAgent implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;

		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

		// ...
	}

	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		Node lastNode = null;
		
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// 
			lastNode = BFS(vehicle, tasks);
//			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			lastNode = ASTAR(vehicle, tasks);
//			plan = naivePlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return deliberativePlan(lastNode, vehicle);
	}

	
/*
 * deliberative Plan function to compute the plan based on the last node and going backward 
 * see descendingIterator and backwardAction
 */
	private Plan deliberativePlan(Node lastNode, Vehicle vehicle) {
		// TODO a checker
		Plan plan = new Plan(vehicle.getCurrentCity());
		Node node = lastNode;
		
		LinkedList<Action> backwardAction = new LinkedList<Action>();
		
		while (node.getPreviousAction()!=null) {
			backwardAction.add(node.getPreviousAction());
			node = node.getParentNode();
		}
		
		Iterator<Action> it = backwardAction.descendingIterator();
		
		while (it.hasNext()) {
			plan.append(it.next());			
		}
		
		return plan;
	}

	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
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


	private Node BFS(Vehicle vehicle, TaskSet tasks) {
		City currentLoc = vehicle.getCurrentCity();
		Plan plan = new Plan(currentLoc);

		State firstState = new State(currentLoc, tasks, vehicle.getCurrentTasks());

		List<Node> Q = new ArrayList<Node>();
		Q.add(new Node(null, null, firstState));

		Set<State> C = new HashSet<State>();

		while(!Q.isEmpty()) {
			Node n = Q.get(0);  // first element of Q
			Q.remove(0); 		// rest(Q)

			if (n.getState().isFinalState()) { 
				// return n if final node 
				return n; 		
			}
			
			else if (!C.contains(n.getState())) {
				// if n not member of C
				// add n to C
				// append successors of n to Q
				
				C.add(n.getState());
				Q.addAll(n.getSuccessors(vehicle));

			}
		}

		// if Q is empty return failure
		System.out.println("Failed");
		return null;
	}
	
	public Node ASTAR(Vehicle vehicle, TaskSet tasks) {
		// TODO
	}
	
}




























