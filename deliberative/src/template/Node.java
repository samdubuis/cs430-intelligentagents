package template;

import logist.plan.Action;

public class Node {
	private Node parentNode;
	private Action previousAction;
	private State state;
	
	public Node(Node parent, Action previousAction, State state) {
		this.parentNode = parent;
		this.previousAction = previousAction;
		this.state = state;
		
		
	}
}
