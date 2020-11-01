package template;

//the list of imports

import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

import java.util.*;


public class Variables {
	public Map<Vehicle, List<ActionV2>> actions;
	public Map<ActionV2, Vehicle> vehicles;
	public Map<ActionV2, Integer> timing;
	private final TaskSet allTasks;

	public Variables(Map<Vehicle, List<ActionV2>> actions, Map<ActionV2, Vehicle> vehicles, Map<ActionV2, Integer> timing, TaskSet tasks) {
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
		this.allTasks = A.allTasks;
	}

	/**
	 * Checks that each task is picked up and delivered at least once
	 */
	public boolean checkDelivery() {
		for (Task t: allTasks) {
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
			if(!pickup.equals(storedPickup) || !delivery.equals(storedDelivery))
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

	/**
	 * Check for each task that pickup happens before delivery
	 * TODO use timing map instead
	 */
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
		if(!checkDelivery()) {
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

	public void print(Vehicle v) {
		List<ActionV2> actions = this.actions.get(v);
		System.out.println(actions.size() + " actions:");
		for (ActionV2 action : actions) {
			System.out.println(action);
		}
	}
}
