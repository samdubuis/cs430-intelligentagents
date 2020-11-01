package template;

import logist.task.Task;

public class ActionV2 {
	public boolean isPickup; // pickup or delivery
	public Task task;

	public ActionV2(boolean isPickup, Task task) {
		this.isPickup = isPickup;
		this.task = task;
	}

	public ActionV2 opposite() {
		return new ActionV2(!isPickup, task);
	}

}
