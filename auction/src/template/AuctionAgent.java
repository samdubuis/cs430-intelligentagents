package template;

import logist.LogistPlatform;
import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import template.Planner.Variables;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 */
@SuppressWarnings("unused")
public class AuctionAgent implements AuctionBehavior {

	private static final double AGGRESSIVITY = 0.5;
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	static Random random = new Random();
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

		adversBids = new ArrayList<Long>();
		adversVehicle = createRandomVehicles();
		ourVar = Planner.firstSolution(new HashSet<Task>(), agent.vehicles());
		adversVar = Planner.firstSolution(new HashSet<Task>(), adversVehicle);

		TDH = new HashMap<City, Double>();
		calculateTDH();

		connectivity = calculateAverageConnectivity(topology);
	}


	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (bids.length > 1) {
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

		long ourCost = ourBiasedMarginalCost(task, aimedTime / 2);
		long adversCost = adversBiasedMarginalCost(task, aimedTime / 2);

		long bid = costComparisonBid(ourCost, adversCost);

		// idea TODO do not bid more than 80% of opponent's median bid in order to not let him do very high bid against us
//		if(round > 4) {
//			bid = (long) Math.max(bid, 0.8 * getAdversMedianBid());
//		}

		return bid;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long aimedTime = (long) Math.max((timeoutPlan - 300) * 0.9, timeoutPlan * 0.5);

		Planner.Variables bestVarFromScratch = Planner.plan(vehicles, tasks, aimedTime);
		Planner.Variables bestPlans;

		if (ourVar != null && Planner.cost(bestVarFromScratch, vehicles) > Planner.cost(ourVar, vehicles)) {
			bestPlans = ourVar;
			//System.err.println("Using scratch variables");
		} else {
			//System.err.println("Using old variables");
			bestPlans = bestVarFromScratch;
		}

		//System.out.println("Final cost: " + bestPlans.cost(vehicles));

		return Planner.computePlans(bestPlans, vehicles, tasks);
	}


	// ----------------

	private long adversBiasedMarginalCost(Task task, long l) {
		long cost = adversMarginalCost(task, l);
		int acceptedTasks = getAdversaryTaskCount();
		double tch = TCH(acceptedTasks);
		double tdh = TDH(task, acceptedTasks);
		double ch = CH(task, acceptedTasks);
		Long result = biasedMarginalCost(cost, 1, tdh, ch);
		return result;
	}


	private long ourBiasedMarginalCost(Task task, long l) {
		long cost = ourMarginalCost(task, l);
		int acceptedTasks = getSelfTaskCount();
		double tch = TCH(acceptedTasks);
		double tdh = TDH(task, acceptedTasks);
		double ch = CH(task, acceptedTasks);
		Long result = biasedMarginalCost(cost, 1, tdh, ch);
		return result;
	}


	// Task count heuristic
	private double TCH(int acceptedTasks) {
		return (acceptedTasks + 1) / (acceptedTasks + 2);
	}

	// Task distribution heuristic
	private double TDH(Task task, int acceptedTasks) {
		return Math.pow(TDH.get(task.pickupCity), 1 / (acceptedTasks + 1));
	}

	// connectivity heuristic
	private double CH(Task task, int acceptedTasks) {
		double tmp = connectivity / connectivity(task.pickupCity, task.deliveryCity);
		return Math.pow(tmp, (1 / (acceptedTasks + 1)));
	}

	// function to compute the biased marginal cost
	private Long biasedMarginalCost(long cost, double tch, double tdh, double ch) {
		double weightedTdh = Math.pow(tdh, 1);
		double weightedCh = Math.pow(ch, 0.7);
		double result = Math.max(0, cost) * tch * weightedTdh * weightedCh;

		return (long) result;
	}

	// compute the task distribution heuristic map
	private void calculateTDH() {
		for (City to : topology.cities()) {
			double pArrival = 0D;
			for (City from : topology.cities()) {
				pArrival += distribution.probability(from, to);
			}

			// The threshold ensures that the heuristic does not get arbitrarily large (or even infinite)
			double threshold = 0.2;
			pArrival = Math.max(pArrival, threshold);

			double heuristic = (1 / pArrival);

			TDH.put(to, heuristic);
		}

	}

	// part for the connectivity
	private double calculateAverageConnectivity(Topology topology2) {
		int[] connectivities = new int[(int) Math.pow(topology.cities().size(), 2)];

		int i = 0;
		for (City city1 : topology.cities()) {
			for (City city2 : topology.cities()) {
				connectivities[i] = connectivity(city1, city2);
				i++;
			}
		}

		return (Arrays.stream(connectivities).sum()) / (double) connectivities.length;
	}

	private int connectivity(City city1, City city2) {
		return city1.neighbors().size() * city2.neighbors().size();
	}


	// Marginal cost for our and for adversary
	//
	private long ourMarginalCost(Task task, long l) {
		long startTime = System.currentTimeMillis();
		// Copy the current variables so that they are not modified at the end
		Planner.Variables nextVariables = new Planner.Variables(ourVar);
		// Insert the new task randomly into the next variables
		Planner.randomInsertTask(nextVariables, task, agent.vehicles());
		long elapsedTime = System.currentTimeMillis() - startTime;

		// Refine the next variables by calling the planner
		nextVariables = Planner.planWithFirstSolution(l - elapsedTime, nextVariables, agent.vehicles());
		ourPotentialNextVar = nextVariables;

		return (long) (Planner.cost(nextVariables, agent.vehicles()) - Planner.cost(ourVar, agent.vehicles()));
	}

	private long adversMarginalCost(Task task, long l) {
		long startTime = System.currentTimeMillis();
		Planner.Variables nextVariables = new Planner.Variables(adversVar); // Copy the current variables so that they are not modified at the end
		Planner.randomInsertTask(nextVariables, task, adversVehicle); // Insert the new task randomly into the next variables
		long elapsedTime = System.currentTimeMillis() - startTime;

		nextVariables = Planner.planWithFirstSolution(l - elapsedTime, nextVariables, adversVehicle); // Refine the next variables by calling the planner
		adversPotentialNextVar = nextVariables;

		return (long) (Planner.cost(nextVariables, adversVehicle) - Planner.cost(adversVar, adversVehicle));

	}


	// function used to create some random vehicle to use for adversary
	private List<Vehicle> createRandomVehicles() {
		List<Vehicle> vehicles = new ArrayList<Vehicle>();

		for (int i = 0; i < agent.vehicles().size(); i++) {
			final Vehicle ourVehicle = agent.vehicles().get(i);
			final City city = topology.randomCity(random);
			Vehicle newV = new Vehicle() {
				@Override
				public int id() {
					return 0;
				}

				@Override
				public String name() {
					return null;
				}

				@Override
				public int capacity() {
					return ourVehicle.capacity();
				}

				@Override
				public City homeCity() {
					return city;
				}

				@Override
				public double speed() {
					return ourVehicle.speed();
				}

				@Override
				public int costPerKm() {
					return ourVehicle.costPerKm();
				}

				@Override
				public City getCurrentCity() {
					return null;
				}

				@Override
				public TaskSet getCurrentTasks() {
					return ourVehicle.getCurrentTasks();
				}

				@Override
				public long getReward() {
					return 0;
				}

				@Override
				public long getDistanceUnits() {
					return 0;
				}

				@Override
				public double getDistance() {
					return 0;
				}

				@Override
				public Color color() {
					return null;
				}
			}; // Create a new vehicle which will only use a small subset of its methods for the Planner computations
			// This vehicle has the same parameters as our vehicle, but a different (random) home city.
			vehicles.add(newV);
		}

		return vehicles;
	}

	// -------------
	// divers functions

	private long costComparisonBid(long ourCost, long adversCost) {
		long bid;

		long deltaMC = Math.abs(adversCost - ourCost);

		if (ourCost <= adversCost) {
			bid = (long) (ourCost + deltaMC * AGGRESSIVITY);
		} else {
			bid = (long) (ourCost + deltaMC * (1 - AGGRESSIVITY));
		}

		return bid;
	}


	//	private double getAdversMedianBid() {
//		List<Long> bidCopy = new ArrayList<Long>(adversBids);
//		if(bidCopy.size() > 0) {
//			Collections.sort(bidCopy);
//			return bidCopy.get(bidCopy.size()/2);
//		}
//		return 1000L;
//	}
	private int getSelfTaskCount() {
		return ourVar.getTaskCount();
	}

	private int getAdversaryTaskCount() {
		return adversVar.getTaskCount();
	}
}
