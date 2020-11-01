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
