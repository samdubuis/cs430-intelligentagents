package template;

import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.*;

public class Algorithms {

	public static Node BFS(Vehicle vehicle, TaskSet tasks) {
		City currentLoc = vehicle.getCurrentCity();
		State firstState = new State(currentLoc, tasks, vehicle.getCurrentTasks());

		Queue<Node> Q = new LinkedList<Node>();
		Q.add(new Node(null, null, firstState, vehicle));

		Map<State, Double> C = new HashMap<State, Double>();

		Node bestNode = null;

		while (!Q.isEmpty()) {
			Node n = Q.poll();  // first element of Q
			State s = n.getState();

			if (s.isFinalState()) {
				// if final node, potential candidate for bestNode
				if (bestNode == null || n.getCost() < bestNode.getCost())
					bestNode = n;
			} else if (!C.containsKey(s) || n.getCost() < C.get(s)) {
				// if n not member of C or has lower cost than its own copy in C
				// add n to C
				// append successors of n to Q

				C.put(s, n.getCost());
				Q.addAll(n.getSuccessors()); // TODO : better adding in case this state was already explored, and only cost changed ???
			}
		}

		// return best final state, with the lowest cost plan (can be null if none is found)
		return bestNode;
	}

	public static Node ASTAR(Vehicle vehicle, TaskSet tasks) {
		City currentLoc = vehicle.getCurrentCity();
		State firstState = new State(currentLoc, tasks, vehicle.getCurrentTasks());

		Queue<Node> Q = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
		Q.add(new Node(null, null, firstState, vehicle));

		Map<State, Double> C = new HashMap<State, Double>();

		while (!Q.isEmpty()) {
			Node n = Q.poll();
			State s = n.getState();

			if (s.isFinalState()) {
				// the first final state reached is the optimal one
				return n;
			} else if (!C.containsKey(s) || n.f() < C.get(s)) {
				// if n not member of C or has lower cost than its own copy in C
				// add n to C
				// append successors of n to Q

				C.put(s, n.f());
				Q.addAll(n.getSuccessors()); // successor states are merged into priority queue
			}
		}

		// if Q is empty return failure
		System.out.println("Failed");
		return null;
	}
}