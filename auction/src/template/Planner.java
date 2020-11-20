package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

public class Planner {

	// ActionV2 Class from previous project
	public static class ActionV2 {
		public boolean isPickup; // pickup or delivery
		public Task task;

		public ActionV2(boolean isPickup, Task task) {
			this.isPickup = isPickup;
			this.task = task;
		}

		public ActionV2 opposite() {
			return new ActionV2(!isPickup, task);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			ActionV2 actionV2 = (ActionV2) o;

			if (isPickup != actionV2.isPickup) return false;
			return task.equals(actionV2.task);
		}

		@Override
		public int hashCode() {
			int result = (isPickup ? 1 : 0);
			result = 31 * result + task.hashCode();
			return result;
		}
	}


	// Variable Class from previous project
	public static class Variables {
		public Map<Vehicle, List<ActionV2>> actions;
		public Map<ActionV2, Vehicle> vehicles;
		public Map<ActionV2, Integer> timing;
		public Set<Task> allTasks;

		public Variables(Map<Vehicle, List<ActionV2>> actions, Map<ActionV2, Vehicle> vehicles, Map<ActionV2, Integer> timing, Set<Task> tasks) {
			this.actions = actions;
			this.vehicles = vehicles;
			this.timing = timing;
			this.allTasks = tasks;
		}

		public Variables(Variables A) {
			this.actions = new HashMap<Vehicle, List<ActionV2>>(); // deep copy since the list will be modified

			for (Vehicle v : A.actions.keySet()) {
				List<ActionV2> actions = new ArrayList<ActionV2>(A.actions.get(v));
				this.actions.put(v, actions);
			}

			this.timing = new HashMap<ActionV2, Integer>(A.timing);
			this.vehicles = new HashMap<ActionV2, Vehicle>(A.vehicles);
			this.allTasks = new HashSet<>(A.allTasks);
		}

		/**
		 * Checks that each task is picked up and delivered at least once
		 */
		public boolean checkDelivery() {
			for (Task t : allTasks) {
				ActionV2 pickup = new ActionV2(true, t);
				ActionV2 delivery = pickup.opposite();

				// each task needs to have a pickup & delivery time, vehicle
				if (!timing.containsKey(pickup) || !timing.containsKey(delivery))
					return false;
				if (!vehicles.containsKey(pickup) || !vehicles.containsKey(delivery))
					return false;

				// the stored actions need to correspond
				ActionV2 storedPickup = actions.get(vehicles.get(pickup)).get(timing.get(pickup) - 1);
				ActionV2 storedDelivery = actions.get(vehicles.get(delivery)).get(timing.get(delivery) - 1);
				if (!pickup.equals(storedPickup) || !delivery.equals(storedDelivery))
					return false;
			}
			return true;
		}

		/**
		 * Check that the vehicle's actions have corresponding entries in the timing map
		 */
		public boolean checkTiming(Vehicle v) {
			List<ActionV2> actions = this.actions.get(v);

			for (int i = 0; i < actions.size(); i++) {
				if (i + 1 != timing.get(actions.get(i))) {
					return false;
				}
			}
			return true;
		}

		/**
		 * Check that the vehicle's actions have corresponding entries in the vehicles map
		 */
		public boolean checkVehicles(Vehicle v) {
			for (ActionV2 a : actions.get(v)) {
				if (vehicles.get(a) != v || vehicles.get(a.opposite()) != v) {
					// double verification
					return false;
				}
			}
			return true;
		}

		public boolean checkOrder(Vehicle v) {
			Set<ActionV2> pickedupTasks = new HashSet<ActionV2>();
			List<ActionV2> actions = this.actions.get(v);

			for (ActionV2 a : actions) {
				if (a.isPickup) {
					pickedupTasks.add(a);
				} else if (!pickedupTasks.contains(a.opposite())) {
					return false;
				}
			}
			return true;
		}

		/**
		 * Checks that the vehicle's weight limit is respected at each time instant
		 */
		public boolean checkPossibleWeight(Vehicle v) {
			List<ActionV2> actions = this.actions.get(v);
			int currentWeight = 0;
			int capacity = v.capacity();

			for (ActionV2 a : actions) {
				currentWeight += a.isPickup ? a.task.weight : -a.task.weight;
				if (currentWeight > capacity) {
					return false;
				}
			}
			return true;
		}

		public void updateTime(Vehicle v) {
			List<ActionV2> actions = this.actions.get(v);
			for (int i = 0; i < actions.size(); i++) {
				timing.put(actions.get(i), i + 1);
			}
		}

		public void updateVehicle(Task task, Vehicle newV) {
			ActionV2 a1 = new ActionV2(true, task);
			ActionV2 a2 = a1.opposite();

			vehicles.put(a1, newV);
			vehicles.put(a2, newV);
		}

		public boolean checkConstraints(List<Vehicle> orderedVehicles) {
			boolean pass = true;
			if (!checkDelivery()) {
				System.out.println("Some tasks aren't delivered");
				pass = false;
			}
			for (Vehicle v : orderedVehicles) {
				if (!checkOrder(v)) {
					System.out.println("Order problem for vehicle : " + v.id());
					pass = false;
				}
				if (!checkPossibleWeight(v)) {
					System.out.println("Weight problem for vehicle : " + v.id());
					pass = false;
				}
				if (!checkTiming(v)) {
					System.out.println("Times problem for vehicle : " + v.id());
					pass = false;
				}
				if (!checkVehicles(v)) {
					System.out.println("Vehicles problem for vehicle : " + v.id());
					pass = false;
				}
			}
			return pass;
		}

		public void print(List<Vehicle> orderedVehicles) {
			for (Vehicle v : orderedVehicles) {
				System.out.println("Vehicle " + v.id() + ": " + actions.get(v).size() + " actions");
			}
		}

		public int getTaskCount() {
			return vehicles.keySet().size() / 2;
		}
	}


	private final static Random random = new Random(123);
	private static final int RESTART_NUMBER = 5;
	private static final boolean RANDOM_INSERTIONS = true;
	private static final int ROLLBACK_DEPTH = 5;
	private static final int NO_IMPROVEMENT_THRESHOLD = 1000;
	private static final int CHANGE_VEHICLE_COUNT = 10;
	private static final int CHANGE_ORDER_COUNT = 10;
	private static final double CHANGE_PROBABILITY = 0.8;


	public static Variables plan(List<Vehicle> orderedVehicles, TaskSet tasks, long allowedTime) {
		Variables bestVariables = null;
		for (int i = 0; i < RESTART_NUMBER; i++) {
			Variables firstSolution = firstSolution(tasks, orderedVehicles);
			if (bestVariables == null) {
				bestVariables = firstSolution;
			}
			Planner.Variables result = restartIterations(allowedTime / RESTART_NUMBER, firstSolution, orderedVehicles);

			//System.out.println("New iteration result: " + result.cost(orderedVehicles));
			if (cost(result, orderedVehicles) < cost(bestVariables, orderedVehicles)) {
				bestVariables = result;
			}
		}
		return bestVariables;
	}

	public static double cost(Variables action, List<Vehicle> vehicleInOrder) {
		double cost = 0;
		List<Plan> plans = computePlans(action, vehicleInOrder);
		for (int i = 0; i < plans.size(); i++) {
			cost += plans.get(i).totalDistance() * vehicleInOrder.get(i).costPerKm();
		}
		return cost;
	}

	public static double totalDistance(Variables action, List<Vehicle> vehicleInOrder) {
		double distance = 0;
		List<Plan> plans = computePlans(action, vehicleInOrder);
		for (Plan plan : plans) {
			distance += plan.totalDistance();
		}
		return distance;
	}

	private static List<Plan> computePlans(Variables action, List<Vehicle> vehicleInOrder) {
		return computePlans(action, vehicleInOrder, null);
	}

	public static List<Plan> computePlans(Variables variables, List<Vehicle> vehicleInOrder, TaskSet taskSet) {
		Map<Vehicle, List<ActionV2>> vehiclesActions = variables.actions;
		List<Plan> result = new ArrayList<Plan>();
		for (Vehicle v : vehicleInOrder) {
			Topology.City current = v.homeCity();
			List<ActionV2> actions = vehiclesActions.get(v);
			Plan plan = new Plan(v.homeCity());
			for (ActionV2 a : actions) {
				if (a.isPickup) {
					for (Topology.City city : current.pathTo(a.task.pickupCity)) {
						plan.appendMove(city);
					}
					current = a.task.pickupCity;
					plan.appendPickup(getTask(taskSet, a.task));
				} else {
					for (Topology.City city : current.pathTo(a.task.deliveryCity)) {
						plan.appendMove(city);
					}
					current = a.task.deliveryCity;
					plan.appendDelivery(getTask(taskSet, a.task));
				}
			}
			result.add(plan);
		}
		return result;
	}


	private static Task getTask(TaskSet set, Task task) {
		if (set == null) {
			return task;
		}

		for (Task t : set) {
			if (t.id == task.id) {
				return t;
			}
		}
		return null;
	}

	public static Variables firstSolution(Set<Task> tasks, List<Vehicle> vehicleInOrder) {
		Map<Vehicle, List<ActionV2>> actions = new HashMap<Vehicle, List<ActionV2>>();
		Map<ActionV2, Integer> time = new HashMap<ActionV2, Integer>();
		Map<ActionV2, Vehicle> vehicles = new HashMap<ActionV2, Vehicle>();

		for (Vehicle vehicle : vehicleInOrder) {
			actions.put(vehicle, new ArrayList<ActionV2>());
		}

		for (Task task : tasks) {
			List<Vehicle> potentialVehicles = vehiclesWithSufficientCapacity(vehicleInOrder, task.weight);
			if (potentialVehicles.size() <= 0)
				break;
			Vehicle vehicle = potentialVehicles.get(random.nextInt(potentialVehicles.size()));

			ActionV2 action = new ActionV2(true, task);
			actions.get(vehicle).add(action);
			int actionTime = actions.get(vehicle).size();
			time.put(action, actionTime);
			vehicles.put(action, vehicle);

			action = new ActionV2(false, task);
			actions.get(vehicle).add(action);
			time.put(action, actionTime + 1);
			vehicles.put(action, vehicle);
		}

		Variables vars = new Variables(actions, vehicles, time, tasks);
		if (!vars.checkConstraints(vehicleInOrder))
			return null;
		return vars;
	}

	private static List<Vehicle> vehiclesWithSufficientCapacity(List<Vehicle> vehicles, int weight) {
		List<Vehicle> result = new ArrayList<Vehicle>();
		for (Vehicle v : vehicles) {
			if (v.capacity() >= weight + v.getCurrentTasks().weightSum()) { // TODO : check this
				result.add(v);
			}
		}
		return result;
	}

	public static Variables restartIterations(long allowedTime, Variables action, List<Vehicle> vehicleInOrder) {
		// Track processing time
		long time_start = System.currentTimeMillis();
		long elapsedTime = 0;

		// Create additional history tracking, so we can "backtrack" if a branch is not promising
		List<Variables> history = new ArrayList<Variables>();
		history.add(action);
		int noImprovementCount = 0;

		// Track best solution seen so far
		double bestCost = cost(action, vehicleInOrder);
		Variables bestVariables = action;

		while (elapsedTime < allowedTime) {
			double previousCost = cost(action, vehicleInOrder);

			// Explore & choose best neighbor
			List<Variables> neighbors = chooseNeighbors(action, CHANGE_VEHICLE_COUNT, CHANGE_ORDER_COUNT, vehicleInOrder);
			neighbors.add(action);
			Variables choice = localChoice(neighbors, vehicleInOrder);

			// If no new neighbor is found, no change can be operated => return
			if (choice == null) {
				System.out.println("Impossible to make changes");
				return bestVariables;
			}

			// Evaluate new action
			double currentCost = cost(choice, vehicleInOrder);
			if (currentCost < bestCost) {
				bestCost = currentCost;
				bestVariables = choice;
			}

			// Choose to keep or discard (NOTE: this logic was extracted from the localChoice function and put here)
			if (random.nextDouble() <= CHANGE_PROBABILITY) {
				action = choice;

				if (currentCost < previousCost) {
					noImprovementCount = 0;
					history.add(action);
				} else {
					noImprovementCount++;
					if (noImprovementCount >= NO_IMPROVEMENT_THRESHOLD) {
						noImprovementCount = 0;
						//System.out.println("ROLLBACK");
						int index = Math.max(history.size() - ROLLBACK_DEPTH, 1);
						history = history.subList(0, index);
						action = history.get(history.size() - 1);
					}
				}
			}

			elapsedTime = System.currentTimeMillis() - time_start;
		}

		return bestVariables;
	}

	private static List<Variables> chooseNeighbors(Variables action, int changeVehicleCount, int changeOrderCount, List<Vehicle> vehicleInOrder) {
		int MAX_TRIES = 1000;
		List<Variables> neighbors = new ArrayList<Variables>();

		int i = 0;
		for (int tries = 0; tries < MAX_TRIES && i < changeVehicleCount; tries++) {
			Variables n = randomChangeVehicle(action, vehicleInOrder);
			if (n != null) {
				assert n.checkConstraints(vehicleInOrder);
				neighbors.add(n);
				i++;
			}
		}

		int j = 0;
		for (int tries = 0; tries < MAX_TRIES && j < changeOrderCount; tries++) {
			Variables n = randomSwapActions(action, vehicleInOrder);
			if (n != null) {
				assert n.checkConstraints(vehicleInOrder);
				neighbors.add(n);
				j++;
			}
		}

		return neighbors;
	}

	private static Variables localChoice(List<Variables> neighbors, List<Vehicle> vehicleInOrder) {
		if (neighbors.size() <= 0)
			return null;

		List<Variables> bestNeighbors = new ArrayList<Variables>();
		double bestCost = Double.POSITIVE_INFINITY;

		for (Variables neighbor : neighbors) {
			double cost = cost(neighbor, vehicleInOrder);

			if (cost == bestCost) {
				bestNeighbors.add(neighbor);
			}

			if (cost < bestCost) {
				bestNeighbors = new ArrayList<Variables>();
				bestNeighbors.add(neighbor);
				bestCost = cost;
			}
		}

		return bestNeighbors.get(random.nextInt(bestNeighbors.size()));
	}

	private static Variables randomChangeVehicle(Variables A, List<Vehicle> vehicleInOrder) {
		// Choose a random task, among all available
		Set<Task> tasks = A.allTasks;

		int taskIndex = random.nextInt(tasks.size());
		int i = 0;
		Task task = null;

		for (Task t : tasks) {
			if (i == taskIndex) {
				task = t;
				break;
			}
			i++;
		}
		assert task != null;

		// Choose a vehicle to exchange with
		Vehicle v1 = A.vehicles.get(new ActionV2(true, task));

		List<Vehicle> possibleVehicles = new ArrayList<Vehicle>();
		for (Vehicle v : vehicleInOrder) {
			if (v != v1 && v.capacity() >= task.weight)
				possibleVehicles.add(v);
		}

		if (possibleVehicles.size() <= 0)
			return null;
		int v2Index = random.nextInt(possibleVehicles.size());
		Vehicle v2 = possibleVehicles.get(v2Index);

		// Do the exchange between v1 and v2
		return changeVehicle(A, v1, v2, task, RANDOM_INSERTIONS);
	}

	private static Variables changeVehicle(Variables action, Vehicle v1, Vehicle v2, Task task, boolean isPosRandom) {
		// Create a new variable assignment
		Variables newA = new Variables(action);

		ActionV2 pickup = new ActionV2(true, task);
		ActionV2 delivery = pickup.opposite();

		// Delete the task from v1
		int pickupTime = newA.timing.get(pickup);
		int deliveryTime = newA.timing.get(delivery);

		newA.actions.get(v1).remove(deliveryTime - 1);
		newA.actions.get(v1).remove(pickupTime - 1);

		newA.updateTime(v1);

		// Add the task to v2 (either at random position, or at the end)
		if (isPosRandom) {
			Variables tentativeA = new Variables(newA);
			List<ActionV2> v2InitActions = tentativeA.actions.get(v2);

			do {
				List<ActionV2> vehicleActions = new ArrayList<ActionV2>(v2InitActions);
				int iRand1 = random.nextInt(vehicleActions.size() + 1);
				vehicleActions.add(iRand1, pickup);
				int iRand2 = iRand1 + 1 + random.nextInt(vehicleActions.size() - iRand1);
				vehicleActions.add(iRand2, delivery);
				tentativeA.actions.put(v2, vehicleActions);
			} while (!tentativeA.checkPossibleWeight(v2));

			List<ActionV2> v2NewActions = tentativeA.actions.get(v2);
			newA.actions.put(v2, v2NewActions);

		} else {
			// Simply append to the end of the list
			newA.actions.get(v2).add(pickup);
			newA.actions.get(v2).add(delivery);
		}

		newA.updateTime(v2);
		newA.updateVehicle(task, v2);

		return newA;
	}

	private static Variables randomSwapActions(Variables action, List<Vehicle> vehicleInOrder) {
		// Choose a random vehicle for which actions will be swapped
		List<Vehicle> possibleVehicles = new ArrayList<Vehicle>();
		for (Vehicle v : vehicleInOrder) {
			if (action.actions.get(v).size() > 0)
				possibleVehicles.add(v);
		}
		if (possibleVehicles.size() <= 0) {
			System.err.println("No vehicle has any tasks!");
			return null;
		}

		int vehicleIndex = random.nextInt(possibleVehicles.size());
		Vehicle randomVehicle = possibleVehicles.get(vehicleIndex);

		// Select the two actions to be swapped
		List<ActionV2> actions = action.actions.get(randomVehicle);
		int t1Index = random.nextInt(actions.size());
		int t2Index = random.nextInt(actions.size() - 1);
		if (t2Index >= t1Index)
			t2Index++;

		return swapActions(action, randomVehicle, t1Index, t2Index);
	}

	private static Variables swapActions(Variables action, Vehicle v, int index1, int index2) {
		// Create a new assignment swapping the two indices
		Variables newA = new Variables(action);
		List<ActionV2> vehiclesActions = newA.actions.get(v);

		ActionV2 a1 = vehiclesActions.get(index1);
		ActionV2 a2 = vehiclesActions.get(index2);

		vehiclesActions.set(index1, a2);
		vehiclesActions.set(index2, a1);

		newA.updateTime(v);

		if (newA.checkOrder(v) && newA.checkPossibleWeight(v)) {
			return newA;
		} else {
			return null;
		}
	}

	public static boolean randomInsertTask(Variables variables, Task task, List<Vehicle> orderedVehicles) {
		List<Vehicle> potentialVehicles = vehiclesWithSufficientCapacity(orderedVehicles, task.weight);
		if(potentialVehicles.size() <= 0)
			return false;

		Vehicle vehicle = potentialVehicles.get(AuctionAgent.random.nextInt(potentialVehicles.size()));

		ActionV2 pickupAction = new ActionV2(true, task);
		variables.actions.get(vehicle).add(pickupAction);
		int actionTime = variables.actions.get(vehicle).size();
		variables.timing.put(pickupAction, actionTime);
		variables.vehicles.put(pickupAction, vehicle);

		ActionV2 deliveryAction = new ActionV2(false, task);
		variables.actions.get(vehicle).add(deliveryAction);
		variables.timing.put(deliveryAction, actionTime + 1);
		variables.vehicles.put(deliveryAction, vehicle);

		variables.allTasks.add(task);
		return true;
	}


}
