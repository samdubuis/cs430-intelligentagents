package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.*;

public class Algorithms {

	public static Node BFS(Vehicle vehicle, TaskSet tasks) {
		Topology.City currentLoc = vehicle.getCurrentCity();
		Plan plan = new Plan(currentLoc);

		State firstState = new State(currentLoc, tasks, vehicle.getCurrentTasks());

		List<Node> Q = new ArrayList<Node>();
		Q.add(new Node(null, null, firstState));

		Set<State> C = new HashSet<State>();

		while (!Q.isEmpty()) {
			Node n = Q.get(0);  // first element of Q
			Q.remove(0);        // rest(Q)

			if (n.getState().isFinalState()) {
				// return n if final node 
				return n;
			} else if (!C.contains(n.getState())) {
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

	public static Node ASTAR(Vehicle vehicle, TaskSet tasks) {
		// TODO a checker
		Topology.City currentLoc = vehicle.getCurrentCity();
		Plan plan = new Plan(currentLoc);

		State firstState = new State(currentLoc, tasks, vehicle.getCurrentTasks());

		List<Node> Q = new ArrayList<Node>();
		Q.add(new Node(null, null, firstState));

		Map<State, Double> C = new HashMap<State, Double>();

		while (!Q.isEmpty()) {
			Node n = Q.get(0);
			Q.remove(0);

			if (n.getState().isFinalState()) {
				return n;
			}

			if (!C.containsKey(n.getState()) || C.get(n.getState()) > n.f()) {
				// if n not member of C or has lower cost than its own copy in C
				// add n to C
				// append successors of n to Q

				C.put(n.getState(), n.f());
				List<Node> S = n.getSuccessors(vehicle);

				S.sort((node1, node2) -> Double.compare(node1.f(), node2.f())); // successors of n are sorted
				Q.addAll(S);                                                    // merged with Q list
				Q.sort((node1, node2) -> Double.compare(node1.f(), node2.f())); // Q list is also sorted for better use
			}
		}

		// if Q is empty return failure
		System.out.println("Failed");
		return null;
	}
}