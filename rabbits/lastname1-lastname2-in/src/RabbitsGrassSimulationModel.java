import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

import java.awt.*;
import java.util.ArrayList;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {


	// Default values
	private static final int GRIDSIZE = 20;
	private static final int RABBIT_LIFESPAN = 20;


	private Schedule schedule;
	private RabbitsGrassSimulationSpace rgSpace;
	private DisplaySurface displaySurface;
	private OpenSequenceGraph graphPopulation;
	private ArrayList<RabbitsGrassSimulationAgent> agentList;

	private int gridSize = GRIDSIZE;
	private int numInitRabbits;
	private int numInitGrass;
	private int grassGrowthRate;
	private int birthThreshold;
	private int rabbitLifespan = RABBIT_LIFESPAN;

	//--------------------------------------------------------

	public static void main(String[] args) {

		System.out.println("Rabbit skeleton");

		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		// Do "not" modify the following lines of parsing arguments
		if (args.length == 0) // by default, you don't use parameter file nor batch mode 
			init.loadModel(model, "", false);
		else
			init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));

	}

	public void setup() {
		System.out.println("Running setup");
		rgSpace = null;
		agentList = new ArrayList<>();
		schedule = new Schedule();

		// Tear down displays
		if (displaySurface != null) {
			displaySurface.dispose();
		}
		displaySurface = null;

		if (graphPopulation != null) {
			graphPopulation.dispose();
		}
		graphPopulation = null;

		// Create displays
		displaySurface = new DisplaySurface(this, "Rabbit Grass Simulation Model Window 1");
		graphPopulation = new OpenSequenceGraph("Population", this);

		// Register displays
		registerDisplaySurface("Rabbit Grass Simulation Model Window 1", displaySurface);
		registerMediaProducer("Population plot", graphPopulation);
	}

	public void begin() {
		buildModel();
		buildSchedule();
		buildDisplay();

		displaySurface.display();
		graphPopulation.display();
	}

	public void buildModel() {
		System.out.println("running buildModel");
		rgSpace = new RabbitsGrassSimulationSpace(gridSize, gridSize);
		rgSpace.spreadGrass(numInitGrass);

		for (int i = 0; i < numInitRabbits; i++) {
			addNewAgent();
		}
		for (RabbitsGrassSimulationAgent rga : agentList) {
			rga.report();
		}
	}

	public void buildSchedule() {
		System.out.println("running buildSchedule");

		// schedule for simulation step
		schedule.scheduleActionBeginning(0, new BasicAction() {
			@Override
			public void execute() {
				SimUtilities.shuffle(agentList);
				for (RabbitsGrassSimulationAgent rga : agentList) {
					rga.step();
				}

				// kills rabbit at 0 energy
				reapDeadAgents();

				//grows grass based on rate
				growGrass(grassGrowthRate);

				//birth new rabbit for those that have enough energy
				birthNewAgent(birthThreshold);

				displaySurface.updateDisplay();
			}
		});

		// schedule for counting and displaying number of rabbits alive, & plotting graph
		schedule.scheduleActionAtInterval(10, new BasicAction() {
			@Override
			public void execute() {
				countLivingAgents();
				graphPopulation.step();
			}
		});
	}

	public void buildDisplay() {
		System.out.println("running buildDisplay");

		ColorMap map = new ColorMap();
		for (int i = 0; i < 16; i++) {
			map.mapColor(i, new Color(0, i * 255 / 15, 0));
		}

		Value2DDisplay displayGrass =
				new Value2DDisplay(rgSpace.getCurrentRabbitGrassSpace(), map);

		Object2DDisplay displayAgents = new Object2DDisplay(rgSpace.getCurrentAgentSpace());
		displayAgents.setObjectList(agentList);

		displaySurface.addDisplayableProbeable(displayGrass, "Grass");
		displaySurface.addDisplayableProbeable(displayAgents, "Agents");

		// Build population plot
		class PopulationInSpace implements DataSource, Sequence {
			@Override
			public Object execute() {
				return new Double(getSValue());
			}

			@Override
			public double getSValue() {
				return countLivingAgents();
			}
		}
		graphPopulation.addSequence("Number of rabbits in space", new PopulationInSpace());
	}

	public String[] getInitParam() {
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
		String[] params = {"GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "RabbitLifespan"};
		return params;
	}

	private void addNewAgent() {
		RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(rabbitLifespan);
		agentList.add(a);
		rgSpace.addAgent(a);
	}

	private int countLivingAgents() {
		int livingAgents = 0;
		for (RabbitsGrassSimulationAgent rga : agentList) {
			if (rga.getEnergy() > 0) livingAgents++;
		}
		System.out.println("Number of living agents is: " + livingAgents);
		return livingAgents;
	}

	// changed the return to void as we do not need to count the number of dead rabbits
	private void reapDeadAgents() {
		for (int i = (agentList.size() - 1); i >= 0; i--) {
			RabbitsGrassSimulationAgent rga = agentList.get(i);
			if (rga.getEnergy() <= 0) {
				rgSpace.removeAgentAt(rga.getX(), rga.getY());
				agentList.remove(i);
			}
		}
	}

	// function called every tick that reads all agents, and if they have enough energy, 
	// and have not yet birthed, reproduce
	private void birthNewAgent(int birthThreshold) {
		// max defined before so that the loop size doesnt change while we add new agents
		int max = agentList.size();
		for (int i = 0; i < max; i++) {
			RabbitsGrassSimulationAgent rga = agentList.get(i);
			if (rga.getEnergy() > birthThreshold && !rga.isHasBirthed()) {
				addNewAgent();
				int actual_energy = rga.getEnergy();
				rga.setEnergy(actual_energy - birthThreshold);
				// TODO: I don't think this is needed. Rabbits can only reproduce once in the SAME simulation step, but can reproduce again later on.
				rga.setHasBirthed(true);
			}
		}
	}

	//function that calls the "initialization" of the grass so that it repopulates every tick
	private void growGrass(int grassGrowthRate) {
		rgSpace.spreadGrass(grassGrowthRate);
	}


	public String getName() {
		return "Rabbit Grass Simulation";
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public int getNumInitRabbits() {
		return numInitRabbits;
	}

	public void setNumInitRabbits(int numInitRabbits) {
		this.numInitRabbits = numInitRabbits;
	}

	public int getGridSize() {
		return gridSize;
	}

	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;
	}

	public int getNumInitGrass() {
		return numInitGrass;
	}

	public void setNumInitGrass(int numInitGrass) {
		this.numInitGrass = numInitGrass;
	}

	public int getGrassGrowthRate() {
		return grassGrowthRate;
	}

	public void setGrassGrowthRate(int grassGrowthRate) {
		this.grassGrowthRate = grassGrowthRate;
	}

	public int getBirthThreshold() {
		return birthThreshold;
	}

	public void setBirthThreshold(int birthThreshold) {
		this.birthThreshold = birthThreshold;
	}

	public int getRabbitLifespan() {
		return rabbitLifespan;
	}

	public void setRabbitLifespan(int rabbitLifespan) {
		this.rabbitLifespan = rabbitLifespan;
	}


}
