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

	private static final double AGGRESSIVITY_POSITIVE = 0.8;
	private static final double AGGRESSIVITY_NEGATIVE = -0.2;
	private static final double ADVERSARY_MARGIN_RATIO = 0.4;

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	static Random random = new Random();

	private long timeoutPlan;
	private long timeoutBid;

	private Variables ourVar;
	private Variables ourPotentialNextVar;

	private Variables adversVar;
	private Variables adversPotentialNextVar;
	private List<Long> adversBids;
	private List<Vehicle> adversVehicle;
	private double adversTotalCost;
	private double adversMargin;

	private Map<City, Double> TDH;
	private double connectivity;

	private int round = 0;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
					  Agent agent) {

		long startTime = System.currentTimeMillis();
		timeoutPlan = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.PLAN);
		timeoutBid = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.BID);

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;

		adversBids = new ArrayList<Long>();
		adversVehicle = createRandomVehicles();
		adversMargin = 0;
		ourVar = Planner.firstSolution(new HashSet<Task>(), agent.vehicles());
		adversVar = Planner.firstSolution(new HashSet<Task>(), adversVehicle);

		TDH = new HashMap<City, Double>();
		calculateTDH();

		connectivity = calculateAverageConnectivity(topology);
	}


	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		long ourBid = bids[agent.id()];
		int theirID = 1 - agent.id();
		if (theirID >= 0 && theirID < bids.length) {
			adversBids.add(bids[theirID]);
		}

		if (winner == agent.id()) {
			ourVar = ourPotentialNextVar;
			System.out.println("We won: " + ourBid + " " + Arrays.toString(bids));
		} else {
			adversVar = adversPotentialNextVar;
			System.out.println("They won: " + ourBid + " " + Arrays.toString(bids));
		}

		if (round >= 5 && adversTotalCost > 0) {
			// Estimate adversary's costs more precisely
			long adversTotalBids = 0;
			for (Long b : adversBids) {
				if (b != null)
					adversTotalBids += b;
			}
			adversMargin = Math.max(0, (adversTotalBids - adversTotalCost) / round); //1 + AGGRESSIVITY * Math.max(0, adversTotalBids / adversTotalCost - 1);
			System.out.println("Estimated margin: " + adversMargin);
		}
		round++;
	}

	@Override
	public Long askPrice(Task task) {
		long aimedTime = (long) Math.max((timeoutBid - 300) * 0.9, timeoutBid * 0.5);

		Long ourCost = ourBiasedMarginalCost(task, aimedTime / 2);
		if (ourCost == null) // this task is impossible for us
			return null;

		Long adversCost = adversBiasedMarginalCost(task, aimedTime / 2);
		if (adversCost == null) // this should not happen, since we suppose adversary has same vehicles as we do
			return (long) (1.1 * ourCost);

		long bid = costComparisonBid(ourCost, adversCost);
		System.out.println("Our cost: " + ourCost + ", their cost: " + adversCost + " (" + (adversCost + adversMargin) + "), our bid: " + bid);

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

		if (ourVar == null || Planner.cost(bestVarFromScratch, vehicles) <= Planner.cost(ourVar, vehicles)) {
			//System.err.println("Using scratch variables");
			ourVar = bestVarFromScratch;
		} else {
			//System.err.println("Using old variables");
		}

		//System.out.println("Final cost: " + bestPlans.cost(vehicles));

		return Planner.computePlans(ourVar, vehicles, tasks);
	}


	// ----------------

	private Long adversBiasedMarginalCost(Task task, long l) {
		Long cost = adversMarginalCost(task, l);
		int acceptedTasks = getAdversaryTaskCount();
		return biasedMarginalCost(task, cost, acceptedTasks);
	}


	private Long ourBiasedMarginalCost(Task task, long l) {
		Long cost = ourMarginalCost(task, l);
		int acceptedTasks = getSelfTaskCount();
		return biasedMarginalCost(task, cost, acceptedTasks);
	}


	// Task count heuristic
	private double TCH(int acceptedTasks) {
		return (acceptedTasks + 1.) / (acceptedTasks + 2.);
	}

	// Task distribution heuristic
	private double TDH(Task task, int acceptedTasks) {
		return Math.pow(TDH.get(task.pickupCity), 1. / (acceptedTasks + 1));
	}

	// connectivity heuristic
	private double CH(Task task, int acceptedTasks) {
		double tmp = connectivity / connectivity(task.pickupCity, task.deliveryCity);
		return Math.pow(tmp, (1. / (acceptedTasks + 1)));
	}

	// function to compute the biased marginal cost
	private Long biasedMarginalCost(Task task, Long cost, int acceptedTasks) {
		if (cost == null)
			return null;

		double tch = TCH(acceptedTasks);
		double tdh = TDH(task, acceptedTasks);
		double ch = CH(task, acceptedTasks);

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
		double sumConnectivity = 0;

		for (City city1 : topology.cities()) {
			for (City city2 : topology.cities()) {
				sumConnectivity += connectivity(city1, city2);
			}
		}

		int numPairs = topology.size() * topology.size();
		return sumConnectivity / numPairs;
	}

	private int connectivity(City city1, City city2) {
		return city1.neighbors().size() * city2.neighbors().size();
	}


	// Marginal cost for ourselves and for adversary
	private Variables getNextVariables(Task task, Variables currentVars, List<Vehicle> orderedVehicles, long l) {
		long startTime = System.currentTimeMillis();
		// Copy the current variables so that they are not modified at the end
		Variables nextVariables = new Variables(currentVars);
		// Insert the new task randomly into the next variables
		boolean success = Planner.randomInsertTask(nextVariables, task, orderedVehicles);
		long elapsedTime = System.currentTimeMillis() - startTime;

		if (!success)
			return null;

		// Refine the next variables by calling the planner
		nextVariables = Planner.planWithFirstSolution(l - elapsedTime, nextVariables, orderedVehicles);
		return nextVariables;
	}

	private Long ourMarginalCost(Task task, long l) {
		Variables nextVariables = getNextVariables(task, ourVar, agent.vehicles(), l);
		if (nextVariables == null)
			return null;

		ourPotentialNextVar = nextVariables;
		return (long) (Planner.cost(nextVariables, agent.vehicles()) - Planner.cost(ourVar, agent.vehicles()));
	}

	private Long adversMarginalCost(Task task, long l) {
		Variables nextVariables = getNextVariables(task, adversVar, adversVehicle, l);
		if (nextVariables == null)
			return null;

		adversPotentialNextVar = nextVariables;
		double marginalCost = Planner.cost(nextVariables, adversVehicle) - Planner.cost(adversVar, adversVehicle);
		adversTotalCost += marginalCost;
		return (long) marginalCost;
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

	private long costComparisonBid(double ourCost, double adversCost) {
		long bid;

		double adversBid = adversCost + ADVERSARY_MARGIN_RATIO * adversMargin;
		double deltaMC = Math.abs(adversBid - ourCost);

		if (ourCost <= adversBid) {
			bid = (long) (ourCost + deltaMC * AGGRESSIVITY_POSITIVE);
		} else {
			bid = (long) (ourCost + deltaMC * AGGRESSIVITY_NEGATIVE);
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
