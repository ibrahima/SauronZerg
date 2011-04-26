package edu.berkeley.nlp.starcraft.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A digraph.
 * 
 * @author Eugene Ma, dlwh
 */
public class Digraph<T> {
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("digraph G {\n");
		for (Node n : dataToNodes.values()) {
			for (Node src : n.getIncomingNodes()) {
				b.append("  " + src.data + " -> " + n.data + "\r\n");
			}
			b.append(n.data + "\r\n");
		}
		b.append("}");
		return b.toString();
	}

	private Map<T, Node> dataToNodes = new HashMap<T, Node>();

	public Node getNode(T data) {
		return dataToNodes.get(data);
	}

	public boolean containsNodeFor(T data) {
		return dataToNodes.containsKey(data);
	}

	public Iterable<Node> allNodes() {
		return dataToNodes.values();
	}

	public Set<T> nodesWithParents(Set<T> data) {
		Set<T> ret = new HashSet<T>();
		for (Node n : allNodes()) {
			boolean unsatisfied = false;
			for (Node parent : n.incomingNodes) {
				if (!data.contains(parent.data)) {
					unsatisfied = true;
					break;
				}
			}
			if (!unsatisfied)
				ret.add(n.data);
		}

		return ret;
	}

	public class Node {
		public T data;
		public Set<Node> incomingNodes = new HashSet<Node>();

		@Override
		public String toString() {
			return "Node [data=" + data + ", incomingNodes=" + incomingNodes
					+ ", outgoingNodes=" + outgoingNodes + "]";
		}

		public Set<Node> outgoingNodes = new HashSet<Node>();

		public Node(T newData) {
			data = newData;
		}

		public T getData() {
			return data;
		}

		public Set<Node> getIncomingNodes() {
			return incomingNodes;
		}

		public Set<Node> getOutgoingNodes() {
			return outgoingNodes;
		}
	}

	public Digraph<T> subgraphWithSinks(Set<T> data) {
		Digraph<T> result = new Digraph<T>();

		for (T datum : data) {
			Node n = this.getNode(datum);
			result.ensurePredecessorOfNode(n);
		}

		return result;
	}

	public Digraph<T> subgraphWithSinksAndBlocks(Set<T> data, Set<T> blocks) {
		Digraph<T> result = new Digraph<T>();

		for (T datum : data) {
			Node n = this.getNode(datum);
			if(n == null) throw new RuntimeException("No node for " + datum);
			result.ensurePredecessorOfNode(n, blocks);
		}

		return result;
	}

	private Node ensurePredecessorOfNode(Node n) {
		if (containsNodeFor(n.data))
			return getNode(n.data);
		Node rn = addNode(n.data);
		for (Node pred : n.incomingNodes) {
			Node rp = ensurePredecessorOfNode(pred);
			addEdge(rp, rn);
		}
		return rn;
	}

	private Node ensurePredecessorOfNode(Node n, Set<T> blocks) {
		if (containsNodeFor(n.data))
			return getNode(n.data);
		Node rn = addNode(n.data);
		for (Node pred : n.incomingNodes) {
			if (!blocks.contains(pred.getData())) {
				Node rp = ensurePredecessorOfNode(pred, blocks);
				addEdge(rp, rn);
			}
		}
		return rn;
	}

	public Node addNode(T newData) {
		Node node;
		if (!dataToNodes.containsKey(newData)) {
			node = new Node(newData);
			dataToNodes.put(newData, node);
		} else {
			node = dataToNodes.get(newData);
		}
		return node;
	}

	public void removeNode(Node nodeToRemove) {
		// Remove references from the parents.
		for (Node incomingNode : nodeToRemove.incomingNodes) {
			incomingNode.outgoingNodes.remove(nodeToRemove);
		}

		// Remove references from the children.
		for (Node outgoingNode : nodeToRemove.outgoingNodes) {
			outgoingNode.incomingNodes.remove(nodeToRemove);
		}

		// Remove from the master list of dataToNodes.
		dataToNodes.remove(nodeToRemove.data);
	}

	public void addEdge(T origin, T target) {
		addEdge(addNode(origin), addNode(target));
	}

	public void addEdge(Node origin, Node target) {
		origin.outgoingNodes.add(target);
		target.incomingNodes.add(origin);
	}

	public void close(Collection<Node> dataToNodesToClose) {
		// Removes the "closed dataToNodes" and links the parents of the closed node
		// to the children of the closed node.

		for (Node nodeToClose : dataToNodesToClose) {
			closeNode(nodeToClose);
		}
	}

	private void closeNode(Node nodeToClose) {
		// Link the parents of the node to the children of the node.
		for (Node incomingNode : nodeToClose.incomingNodes) {
			for (Node outgoingNode : nodeToClose.outgoingNodes) {
				// Avoid self-loops.
				if (!incomingNode.equals(outgoingNode)) {
					addEdge(incomingNode, outgoingNode);
				}
			}
		}

		removeNode(nodeToClose);
	}

	public List<Node> createListOfSourceNodes() {
		List<Node> sourceNodes = new ArrayList<Node>();
		for (T data : dataToNodes.keySet()) {
			Node node = dataToNodes.get(data);
			if (node.incomingNodes.isEmpty()) {
				sourceNodes.add(node);
			}
		}
		return sourceNodes;
	}

	public List<Node> createListOfSinkNodes() {
		List<Node> sinkNodes = new ArrayList<Node>();
		for (T data : dataToNodes.keySet()) {
			Node node = dataToNodes.get(data);
			if (node.outgoingNodes.isEmpty()) {
				sinkNodes.add(node);
			}
		}
		return sinkNodes;
	}
}
