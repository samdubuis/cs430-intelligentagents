package template;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.HashMap;
import java.util.Random;

public class State {

	
	public HashMap<City, City> locToDst;
	
	public City bestDst;
	
	public State(City loc, City dst) {
		locToDst.put(loc, dst);
	}
	
	
	
	
	
}
