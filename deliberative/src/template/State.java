package template;

import logist.task.TaskSet;
import logist.topology.Topology.City;

public class State {
	private final City loc;
	private final TaskSet availableTasks;
	private final TaskSet pickedupTasks;

	public State(City loc, TaskSet availableTasks, TaskSet pickedupTasks) {
		this.loc = loc;
		this.availableTasks = availableTasks;
		this.pickedupTasks = pickedupTasks;
	}

	public boolean isFinalState() {
		return availableTasks.isEmpty() && pickedupTasks.isEmpty();
	}

	public City getLoc() {
		return loc;
	}

	public TaskSet getAvailableTasks() {
		return availableTasks;
	}

	public TaskSet getPickedupTasks() {
		return pickedupTasks;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		State state = (State) o;

		if (!loc.equals(state.loc)) return false;
		if (!availableTasks.equals(state.availableTasks)) return false;
		return pickedupTasks.equals(state.pickedupTasks);
	}

	@Override
	public int hashCode() {
		int result = loc.hashCode();
		result = 31 * result + availableTasks.hashCode();
		result = 31 * result + pickedupTasks.hashCode();
		return result;
	}
}
