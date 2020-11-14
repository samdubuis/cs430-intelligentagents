package template;

import java.util.AbstractCollection;
//the list of imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logist.LogistPlatform;
import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import template.Planner.Variables;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionAgent implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	
	private long timeoutSetup;
	private long timeoutPlan;
	private long timeoutBid;
	
	private List<Long> adversBids;
	private Map<City, Double> TDH;
	private List<Vehicle> adversVehicle;
	private double connectivity;
	
	private Variables ourVar;
	private Variables adversVar;
	private Variables ourPotentialNextVar;
	private Variables adversPotentialNextVar;
	private int round = 0;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		long startTime = System.currentTimeMillis();
        timeoutSetup = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.SETUP);
        timeoutPlan = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.PLAN);
        timeoutBid = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.BID);

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();

		// TODO
		adversVehicle = null;
		ourVar = Planner.firstSolution(agent.getTasks(), agent.vehicles());
		adversVar = Planner.firstSolution(agent.getTasks(), adversVehicle);
		
		TDH = new HashMap<City, Double>();
		calculateTDH();
		
		connectivity = calculateAverageConnectivity(topology);
		
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if(bids.length > 1) {
            adversBids.add(bids[1]);
        }

        if (winner == agent.id()) {
            ourVar = ourPotentialNextVar;
        } else {
            adversVar = adversPotentialNextVar;
        }
        round++;
	}
	
	@Override
	public Long askPrice(Task task) {
		long aimedTime = (long) Math.max((timeoutBid - 300) * 0.9, timeoutBid * 0.5);

        long ourCost = ourMarginalCost(task, aimedTime/2);
        long adversCost = adversMarginalCost(task, aimedTime/2);

        long bid = costComparisonBid(ourCost, adversCost);
		
		
		return bid;
	}

	private long costComparisonBid(long ourCost, long adversCost) {
		long bid;

        long deltaMC = Math.abs(adversCost-ourCost);

        if (ourCost <= adversCost) {
            bid = (long) (ourCost + deltaMC * 0.5);
        }
        else {
            bid = (long) (ourCost + deltaMC * (-0.5));
        }

        //System.err.println("Cost estimates: " + ourCost + " VS " + adversCost + " ===> bid: " + bid);
        return bid;
	}
	
	
	@Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long aimedTime = (long) Math.max((timeoutPlan - 300) * 0.9, timeoutPlan * 0.5);

        Planner.Variables bestVarFromScratch = Planner.plan(vehicles, tasks, aimedTime);
        Planner.Variables bestPlans;

        if(ourVar != null && bestVarFromScratch.cost(vehicles) > ourVar.cost(vehicles)) {
            bestPlans = ourVar;
            //System.err.println("Using scratch variables");
        } else {
            //System.err.println("Using old variables");
            bestPlans = bestVarFromScratch;
        }

        //System.out.println("Final cost: " + bestPlans.cost(vehicles));

        return Planner.computePlans(bestPlans, vehicles, tasks);
    }

}
