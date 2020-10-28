package template;

//the list of imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logist.simulation.Vehicle;
import logist.task.Task;


public class Variables {
	private Map<Vehicle, List<ActionV2>> actions;
	private Map<ActionV2, Vehicle> vehicles;
	private Map<ActionV2, Integer> timing;

	public Variables(Map<Vehicle, List<ActionV2>> actions, Map<ActionV2, Vehicle> vehicles, Map<ActionV2, Integer> timing) {
		this.actions = actions;
		this.vehicles = vehicles;
		this.timing = timing;
	}

	public Variables(Variables A) {
		this.actions = new HashMap<Vehicle, List<ActionV2>>(); // deep copy since the list will be modified

		for (Vehicle v : A.actions.keySet()) {
			List<ActionV2> actions = new ArrayList<ActionV2>();

			for (ActionV2 a : A.actions.get(v)) {
				actions.add(a);
			}
			this.actions.put(v, actions);
		}

		this.timing = new HashMap<ActionV2, Integer>(A.timing);
		this.vehicles = new HashMap<ActionV2, Vehicle>(A.vehicles);
	}


	public boolean checkPossibleWeight(Vehicle v) {
		List<ActionV2> actions = this.actions.get(v);
		int actualWeight = 0;
		int capacity = v.capacity();

		for (ActionV2 a : actions) {
			actualWeight += a.isPickup ? a.task.weight : -a.task.weight;
			if(actualWeight > capacity) {
				return false;
			}
		}
		return true;
	}

	public boolean checkOrder(Vehicle v) {
		Set<ActionV2> pickedupTasks = new HashSet<ActionV2>();
		List<ActionV2> actions = this.actions.get(v);

		for (ActionV2 a : actions) {
			if(a.isPickup) {
				pickedupTasks.add(a);
			} else if (!pickedupTasks.contains(a.opposite())) {
				return false;
			}
		}
		return true;
	}


	public boolean checkTiming(Vehicle v) {
		List<ActionV2> actions = this.actions.get(v);

		for (int i = 0; i < actions.size(); i++) {
			if(i+1 != timing.get(actions.get(i))) {
				return false;
			}
		}
		return true;
	}


	public boolean checkVehicles(Vehicle v) {
		for(ActionV2 a: actions.get(v)) {
			if(vehicles.get(a) != v || vehicles.get(a.opposite()) != v) {
				// double verification
				return false;
			}
		}
		return true;
	}

	public void updateTime(Vehicle v) {
		List<ActionV2> actions = this.actions.get(v);
		for (int i = 0; i < actions.size(); i++) {
			timing.put(actions.get(i), i+1);
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
		for (Vehicle v : orderedVehicles) {
			if(!checkOrder(v)) {
				System.out.println("Order problem for vehicle : "+v.id());
				pass = false;
			}
			if(!checkPossibleWeight(v)) {
				System.out.println("Weight problem for vehicle : "+v.id());
				pass = false;
			}
			if(!checkTiming(v)) {
				System.out.println("Times problem for vehicle : "+v.id());
				pass = false;
			}
			if(!checkVehicles(v)) {
				System.out.println("Vehicles problem for vehicle : "+v.id());
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
