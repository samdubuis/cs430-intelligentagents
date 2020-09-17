import uchicago.src.sim.space.Object2DTorus;
/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {
	private Object2DTorus grassGrid;
	private Object2DTorus agentGrid;

	public RabbitsGrassSimulationSpace(int xSize, int ySize) {
		grassGrid = new Object2DTorus(xSize, ySize);
		agentGrid = new Object2DTorus(xSize, ySize);

		for (int i = 0; i < xSize; i++) {
			for (int j = 0; j < ySize; j++) {
				grassGrid.putObjectAt(i, j, new Integer(0));
			}
		}
	}

	public void spreadGrass(int numInitGrass) {
		// looping through the number of grass to grow
		for (int i = 0; i < numInitGrass; i++) {

			//randomly choose coordinate
			int x = (int)(Math.random()*(grassGrid.getSizeX()));
			int y = (int)(Math.random()*(grassGrid.getSizeY()));		

			// Get the value of the object at those coordinates
			int currentValue = getGrassAt(x, y);

			// Replace the Integer object with another one with the new value
			grassGrid.putObjectAt(x,y,currentValue+1);

		}
	}

	public int getGrassAt(int x, int y) {
		int i;
		if (grassGrid.getObjectAt(x, y)!=null) {
			i = ((Integer)grassGrid.getObjectAt(x, y)).intValue();
		}
		else { 
			i=(Integer)0;
		}
		return i;
	}

	public Object2DTorus getCurrentRabbitGrassSpace() {
		return grassGrid;
	}

	public Object2DTorus getCurrentAgentSpace(){
	    return agentGrid;
	  }
	
	public boolean isCellOccupied(int x, int y){
		boolean retVal = false;
		if(agentGrid.getObjectAt(x, y)!=null) retVal = true;
		return retVal;
	}

	public boolean addAgent(RabbitsGrassSimulationAgent agent){
		boolean retVal = false;
		int count = 0;
		int countLimit = 10 * agentGrid.getSizeX() * agentGrid.getSizeY();

		while((retVal==false) && (count < countLimit)){
			int x = (int)(Math.random()*(agentGrid.getSizeX()));
			int y = (int)(Math.random()*(agentGrid.getSizeY()));
			if(isCellOccupied(x,y) == false){
				agentGrid.putObjectAt(x,y,agent);
				agent.setXY(x,y);
				agent.setRabbitsGrassSimulationSpace(this);
				retVal = true;
			}
			count++;
		}

		return retVal;
	}
	
	public void removeAgentAt(int x, int y){
	    agentGrid.putObjectAt(x, y, null);
	  }
	
	public int eatGrassAt(int x, int y) {
		int grass = getGrassAt(x, y);
		grassGrid.putObjectAt(x, y, new Integer(0));
		return grass;
	}
	
	public boolean moveAgentAt(int x, int y, int newX, int newY){
	    boolean retVal = false;
	    if(!isCellOccupied(newX, newY)){
	      RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentGrid.getObjectAt(x, y);
	      removeAgentAt(x,y);
	      rga.setXY(newX, newY);
	      agentGrid.putObjectAt(newX, newY, rga);
	      retVal = true;
	    }
	    return retVal;
	  }

}
