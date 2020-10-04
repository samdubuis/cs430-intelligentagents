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
		
	public HashMap<City, Double> rewards;
	public HashMap<City, Double> values;
	
	public City bestDst;
	public float bestReward;
	public float bestValue;
	
	public State(City loc, City dst, int costPerKm, TaskDistribution td) {
		locToDst.put(loc, dst);
		
		values = new HashMap<Topology.City, Double>();
		rewards = new HashMap<City, Double>();
		
		for (City neighbor : loc.neighbors()) {
			values.put(neighbor, 0.0);
			rewards.put(neighbor, neighbor.distanceTo(loc)*costPerKm*(-1));
		}
		if (dst!=null) {
			values.put(dst, 0.0);
			rewards.put(dst, td.reward(loc, dst)-(dst.distanceTo(loc)*costPerKm));
		}
	}
	
	
	
	
	
}
