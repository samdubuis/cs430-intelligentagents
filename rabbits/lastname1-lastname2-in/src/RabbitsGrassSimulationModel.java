import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

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
	private ArrayList agentList;

	private int numInitRabbits;
	private int gridSize = GRIDSIZE;
	private int numInitGrass;
	private int grassGrowthRate;
	private int birthThreshold;
	private int rabbitLifespan = RABBIT_LIFESPAN;

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
		agentList = new ArrayList();
		schedule = new Schedule();

		//display surface part
		if (displaySurface != null){
			displaySurface.dispose();
		}
		displaySurface = null;

		displaySurface = new DisplaySurface(this, "Rabbit Grass Simulation Model Window 1");

		registerDisplaySurface("Rabbit Grass Simulation Model Window 1", displaySurface);
	}

	public void begin() {
		buildModel();
		buildSchedule();
		buildDisplay();

		displaySurface.display();

	}

	public void buildModel(){
		System.out.println("running buildmodel");
		rgSpace = new RabbitsGrassSimulationSpace(gridSize, gridSize);
		rgSpace.spreadGrass(numInitGrass);

		for (int i = 0; i < numInitRabbits; i++) {
			addNewAgent();
		}
		for (int i = 0; i < agentList.size(); i++) {
			RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
			rga.report();
		}
	}

	public void buildSchedule(){
		System.out.println("running buildschedule");

		class RabbitGrassSimulationStep extends BasicAction {
			public void execute() {
				SimUtilities.shuffle(agentList);
				for(int i =0; i < agentList.size(); i++){
					RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
					rga.step();
				}
				
				displaySurface.updateDisplay();
			}
		}

		schedule.scheduleActionBeginning(0, new RabbitGrassSimulationStep());

		class RabbitGrassSimulationCountLiving extends BasicAction {
			public void execute(){
				countLivingAgents();
			}
		}

		schedule.scheduleActionAtInterval(10, new RabbitGrassSimulationCountLiving());
	}

	public void buildDisplay(){
		System.out.println("running builddisplay");
		ColorMap map = new ColorMap();

		for(int i = 1; i<16; i++){
			map.mapColor(i, new Color(0, 255, 0));
		}
		map.mapColor(0, Color.black);

		Value2DDisplay displayGrass =
				new Value2DDisplay(rgSpace.getCurrentRabbitGrassSpace(), map);

		Object2DDisplay displayAgents = new Object2DDisplay(rgSpace.getCurrentAgentSpace());
		displayAgents.setObjectList(agentList);


		displaySurface.addDisplayable(displayGrass, "Money");
		displaySurface.addDisplayable(displayAgents, "Agents");
	}

	public String[] getInitParam() {
		// TODO Auto-generated method stub
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
		String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "RabbitLifespan"};
		return params;
	}

	private void addNewAgent(){
		RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(rabbitLifespan);
		agentList.add(a);
		rgSpace.addAgent(a);
	}

	private int countLivingAgents(){
	    int livingAgents = 0;
	    for(int i = 0; i < agentList.size(); i++){
	      RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
	      if(rga.getStepsToLive() > 0) livingAgents++;
	    }
	    System.out.println("Number of living agents is: " + livingAgents);

	    return livingAgents;
	  }
	
	public String getName() {
		// TODO Auto-generated method stub
		return null;
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
