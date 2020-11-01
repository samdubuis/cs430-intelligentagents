package template;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.io.File;
import java.util.*;


@SuppressWarnings("unused")
public class CentralizedAgent implements CentralizedBehavior {

	private final Random random = new Random(123);
	private static final int RESTART_NUMBER = 5;
	private static final boolean RANDOM_INSERTIONS = true;
	private static final int ROLLBACK_DEPTH = 5;
	private static final int NO_IMPROVEMENT_THRESHOLD = 1000;
	private static final int CHANGE_VEHICLE_COUNT = 10;
	private static final int CHANGE_ORDER_COUNT = 10;

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeout_setup = 1000; // 1s default value, in case we fail to load settings file
	private long timeout_plan = 1000;

	public List<Vehicle> vehicleInOrder;


	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		if(ls != null) {
			// the setup method cannot last more than timeout_setup milliseconds
			timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
			// the plan method cannot execute more than timeout_plan milliseconds
			timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
		}

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long time_start = System.currentTimeMillis();
		this.vehicleInOrder = vehicles;

		Variables action = firstSolution(tasks);
		if(action == null) {
			System.err.println("Impossible to plan for this task distribution");
			return null;
		}
		Variables bestVar = action;


		long aimedTime = (long) ((timeout_plan - 300) * 0.9);
		System.out.println("Estimated time (ms): " + aimedTime);


		for (int i = 0; i < RESTART_NUMBER; i++) {
			Variables result = restartIterations(aimedTime / RESTART_NUMBER, action);
			System.out.println("New iteration result: " + cost(result));
			if (cost(result) < cost(bestVar)) {
				bestVar = result;
			}
			action = firstSolution(tasks);
		}

		System.out.println("final cost = " + cost(bestVar));
		List<Plan> plans = computePlans(bestVar);


		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");

		return plans;
	}

	public int cost(Variables action) {
		int cost = 0;
		List<Plan> plans = computePlans(action);
		for (int i = 0; i < plans.size(); i++) {
			cost += plans.get(i).totalDistance() * vehicleInOrder.get(i).costPerKm();
		}
		return cost;
	}

	private Variables restartIterations(long allowedTime, Variables action) {

		long time_start = System.currentTimeMillis();
		long elapsedTime = 0;

		List<Variables> history = new ArrayList<Variables>();
		history.add(action);
		int noImprovementCount = 0;

		int bestCost = cost(action);
		Variables bestVariables = action;
		int i = 0;

		while (elapsedTime < allowedTime) {
			int previousCost = cost(action);

			List<Variables> neighbors = chooseNeighbors(action, CHANGE_VEHICLE_COUNT, CHANGE_ORDER_COUNT);
			neighbors.add(action);
			action = localChoice(neighbors, action);

			int currentCost = cost(action);
			if (currentCost < bestCost) {
				bestCost = currentCost;
				bestVariables = action;
			}

			if (currentCost < previousCost) {
				noImprovementCount = 0;
				history.add(action);
			}

			if (currentCost == previousCost) {
				noImprovementCount++;
				if (noImprovementCount >= NO_IMPROVEMENT_THRESHOLD) {
					noImprovementCount = 0;
					//System.out.println("ROLLBACK");
					int index = Math.max(history.size() - ROLLBACK_DEPTH, 0);
					history = history.subList(0, index);
					action = history.get(history.size() - 1);
				}
			}
			i++;

			elapsedTime = System.currentTimeMillis() - time_start;
		}

		return bestVariables;
	}

	private Variables firstSolution(TaskSet tasks) {
		Map<Vehicle, List<ActionV2>> actions = new HashMap<Vehicle, List<ActionV2>>();
		Map<ActionV2, Integer> time = new HashMap<ActionV2, Integer>();
		Map<ActionV2, Vehicle> vehicles = new HashMap<ActionV2, Vehicle>();

		for (Vehicle vehicle : vehicleInOrder) {
			actions.put(vehicle, new ArrayList<ActionV2>());
		}

		for (Task task : tasks) {
			List<Vehicle> potentialVehicles = vehiclesWithSufficientCapacity(vehicleInOrder, task.weight);
			if(potentialVehicles.size() <= 0)
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
		if(!vars.checkConstraints(vehicleInOrder))
			return null;
		return vars;
	}

	private List<Vehicle> vehiclesWithSufficientCapacity(List<Vehicle> vehicles, int weight) {
		List<Vehicle> result = new ArrayList<Vehicle>();
		for (Vehicle v : vehicles) {
			if (v.capacity() >= weight + v.getCurrentTasks().weightSum()) {
				result.add(v);
			}
		}
		return result;
	}

	private List<Plan> computePlans(Variables action) {
		Map<Vehicle, List<ActionV2>> vehiclesActions = action.actions;
		List<Plan> result = new ArrayList<Plan>();
		for (Vehicle v : vehicleInOrder) {
			City current = v.homeCity();
			List<ActionV2> actions = vehiclesActions.get(v);
			Plan plan = new Plan(v.homeCity());
			for (ActionV2 a : actions) {
				if (a.isPickup) {
					for (City city : current.pathTo(a.task.pickupCity)) {
						plan.appendMove(city);
					}
					current = a.task.pickupCity;
					plan.appendPickup(a.task);
				} else {
					for (City city : current.pathTo(a.task.deliveryCity)) {
						plan.appendMove(city);
					}
					current = a.task.deliveryCity;
					plan.appendDelivery(a.task);
				}
			}
			result.add(plan);
		}
		return result;
	}

	private Variables randomChangeVehicle(Variables A) {

		Set<ActionV2> actions = A.vehicles.keySet();

		int actionIndex = random.nextInt(actions.size());
		int i = 0;
		ActionV2 action = null;

		for (ActionV2 a : actions) {
			if (i == actionIndex) {
				action = a;
			}
			i++;
		}

		Vehicle v1 = A.vehicles.get(action);
		Vehicle v2 = null;
		Set<Vehicle> vehicles = A.actions.keySet();
		int v2Index = 0;
		do {
			int j = 0;
			v2Index = random.nextInt(A.actions.keySet().size());
			for (Vehicle v : vehicles) {
				if (j == v2Index) {
					v2 = v;
				}
				j++;
			}

		} while (v1 == v2);

		Task randomTask = action.task;
		return changeVehicle(A, v1, v2, randomTask, RANDOM_INSERTIONS);
	}

	private Variables changeVehicle(Variables action, Vehicle v1, Vehicle v2, Task task, boolean isPosRandom) {

		Variables newA = new Variables(action);

		ActionV2 pickup = new ActionV2(true, task);
		ActionV2 delivery = pickup.opposite();

		int pickupTime = newA.timing.get(pickup);
		int deliveryTime = newA.timing.get(delivery);

		newA.checkConstraints(vehicleInOrder);

		newA.actions.get(v1).remove(deliveryTime - 1);
		newA.actions.get(v1).remove(pickupTime - 1);

		newA.updateTime(v1);

		if (isPosRandom && newA.actions.get(v2).size() > 0) {
			Variables tentativeA;
			List<ActionV2> v2NewActions;

			do {
				tentativeA = new Variables(newA);
				List<ActionV2> vehicleActions = tentativeA.actions.get(v2);
				int iRand1 = random.nextInt(vehicleActions.size());
				vehicleActions.add(iRand1, pickup);
				int iRand2 = random.nextInt(vehicleActions.size());
				vehicleActions.add(iRand2, delivery);

			} while (!(tentativeA.checkPossibleWeight(v2) && tentativeA.checkOrder(v2)));

			v2NewActions = tentativeA.actions.get(v2);
			newA.actions.put(v2, v2NewActions);

		} else {
			// Simply append add the end of the list
			newA.actions.get(v2).add(pickup);
			newA.actions.get(v2).add(delivery);
		}

		newA.updateTime(v2);
		newA.updateVehicle(task, v2);

		newA.checkConstraints(vehicleInOrder);
		return newA;
	}

	private List<Variables> chooseNeighbors(Variables action, int changeVehicleCount, int changeOrderCount) {

		List<Variables> neighbors = new ArrayList<Variables>();

		int i = 0;
		while (i < changeVehicleCount) {
			Variables n = randomChangeVehicle(action);
			if (n != null) {
				neighbors.add(randomChangeVehicle(action));
				i++;
			}
		}

		int j = 0;
		while (j < changeOrderCount) {
			Variables n = randomSwapActions(action);
			if (n != null) {
				neighbors.add(n);
				j++;
			}
		}

		return neighbors;
	}

	private Variables localChoice(List<Variables> neighbors, Variables actual) {

		double CHANGE_PROBABILITY = 1;
		List<Variables> bestNeighbors = new ArrayList<Variables>();
		int bestCost = Integer.MAX_VALUE;

		for (Variables neighbor : neighbors) {
			int cost = cost(neighbor);

			if (cost == bestCost) {
				bestNeighbors.add(neighbor);
			}

			if (cost < bestCost) {
				bestNeighbors = new ArrayList<Variables>();
				bestNeighbors.add(neighbor);
				bestCost = cost;
			}
		}

		Variables next = actual;

		if (random.nextDouble() <= CHANGE_PROBABILITY) {
			next = bestNeighbors.get(random.nextInt(bestNeighbors.size()));
		}

		return next;
	}

	private Variables randomSwapActions(Variables action) {

		Set<Vehicle> vehicles = action.actions.keySet();
		Vehicle randomVehicle = null;

		do {
			int i = 0;
			int vehicleIndex = random.nextInt(vehicles.size());
			for (Vehicle v : vehicles) {
				if (i == vehicleIndex) {
					randomVehicle = v;
				}
				i++;
			}
		} while (action.actions.get(randomVehicle).size() == 0);

		List<ActionV2> actions = action.actions.get(randomVehicle);
		int t1Index, t2Index;

		do {
			t1Index = random.nextInt(actions.size());
			t2Index = random.nextInt(actions.size());

		} while (t1Index == t2Index);

		return swapActions(action, randomVehicle, actions.get(t1Index), actions.get(t2Index));
	}

	private Variables swapActions(Variables action, Vehicle v, ActionV2 a1, ActionV2 a2) {

		Variables newA = new Variables(action);
		List<ActionV2> vehiclesActions = newA.actions.get(v);
		int index1 = vehiclesActions.indexOf(a1);
		int index2 = vehiclesActions.indexOf(a2);

		vehiclesActions.set(index1, a2);
		vehiclesActions.set(index2, a1);

		newA.updateTime(v);

		if (newA.checkOrder(v) && newA.checkPossibleWeight(v)) {
			return newA;
		} else {
			return null;
		}
	}


}
	