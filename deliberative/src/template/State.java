package template;

import logist.task.TaskSet;
import logist.topology.*;
import logist.topology.Topology.City;

public class State {
	private City loc;
	private TaskSet availableTasks;
	private TaskSet pickedupTasks;
	
	public State(City loc, TaskSet availableTasks, TaskSet pickedupTasks) {
		this.loc = loc;
		this.availableTasks = availableTasks;
		this.pickedupTasks = pickedupTasks;
	}
	
	
	
}
