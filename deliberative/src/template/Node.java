package template;

import logist.plan.Action;
import logist.simulation.Vehicle;
import java.util.*;


public class Node {
	private Node parentNode;
	private Action previousAction;
	private State state;
	
	public Node(Node parent, Action previousAction, State state) {
		this.parentNode = parent;
		this.previousAction = previousAction;
		this.state = state;
		
		
	}

	public Node getParentNode() {
		return parentNode;
	}

	public Action getPreviousAction() {
		return previousAction;
	}

	public State getState() {
		return state;
	}
	
	public List<Node> getSuccessors(Vehicle vehicle){
		// TODO 
		
		LinkedList<Node> successors = new LinkedList<Node>();
		
		// Deliver
		
		// Pickup
		
		// Move
		
		return successors;
	}
	
	
}
