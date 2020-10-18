package template;

import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.LinkedList;
import java.util.List;


public class Node {
	private final Node parentNode;
	private final Action previousAction;
	private final State state;
	private final double cost;

	public Node(Node parent, Action previousAction, State state, double cost) {
		this.parentNode = parent;
		this.previousAction = previousAction;
		this.state = state;
		this.cost = cost;

	}

	public Node(Node parent, Action previousAction, State state) {
		this(parent, previousAction, state, 0);
	}

	public double h() {
		// heuristic function 
		// ? available task reward * available weight   +   pickedup tasks reward * pickedup weight ?
		// a jouer sur le reward je pense
		return 0;
	}

	public double f() {
		// as in the slide
		return h() + cost;
	}

	public Node getParentNode() {
		return parentNode;
	}

	public Action getPreviousAction() {
		return previousAction;
	}

	public State getState() {
		return state;
	}

	public double getCost() {
		return cost;
	}

	public List<Node> getSuccessors(Vehicle vehicle) {
		// TODO A checker --> OK pour moi, tres propre, juste lignes 67 et 82 j'ai rajouté le cout des nodes (qui reste le meme)

		LinkedList<Node> successors = new LinkedList<Node>();

		// Deliver
		for (Task task : state.getPickedupTasks()) {
			if (task.deliveryCity.equals(state.getLoc())) {
				TaskSet nextToPickupTasks = state.getPickedupTasks().clone();
				nextToPickupTasks.remove(task);

				State nextState = new State(state.getLoc(), state.getAvailableTasks(), nextToPickupTasks);
				successors.add(new Node(this, new Action.Delivery(task), nextState, cost));
			}
		}


		// Pickup
		for (Task task : state.getAvailableTasks()) {
			if (task.pickupCity.equals(state.getLoc()) && (task.weight + state.getPickedupTasks().weightSum() <= vehicle.capacity())) {
				TaskSet nextAvailableTask = state.getAvailableTasks().clone();
				nextAvailableTask.remove(task);

				TaskSet nextToPickupTasks = state.getPickedupTasks().clone();
				nextToPickupTasks.add(task);

				State nextState = new State(state.getLoc(), nextAvailableTask, nextToPickupTasks);
				successors.add(new Node(this, new Action.Pickup(task), nextState, cost));
			}
		}


		// Move
		for (City neighbour : state.getLoc().neighbors()) {
			State nextState = new State(neighbour, state.getAvailableTasks(), state.getPickedupTasks());
			double movementCost = vehicle.costPerKm() * state.getLoc().distanceTo(neighbour);
			successors.add(new Node(this, new Action.Move(neighbour), nextState, cost + movementCost));
		}

		return successors;
	}

}
