package template;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;

import ca.uvic.cs.chisel.cajun.graph.FlatGraph;
import ca.uvic.cs.chisel.cajun.graph.node.GraphNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;

public class SADVizInputEventHandler extends PBasicInputEventHandler {
	private static final int DOUBLE_CLICK = 2;

	private SADVizGraphModel graphModel;
	private FlatGraph graph;
	private SADVizGraphController controlla;

	public SADVizInputEventHandler(SADVizGraphModel graphModel, FlatGraph graph, SADVizGraphController controller) {
		this.graphModel = graphModel;
		this.graph = graph;
		this.controlla = controller;

		PInputEventFilter filter = new PInputEventFilter();
		filter.rejectAllEventTypes();
		filter.setAcceptsMousePressed(true);
		filter.setAcceptsMouseMoved(true);
		
		this.setEventFilter(filter);
	}

	/**
	 * Listens for a double click event to expand or collapse node.
	 * 
	 * @param PInputEvent 
	 */
	public void mousePressed(PInputEvent event) {
		
		if (event.isLeftMouseButton()) {
			if (event.getClickCount() == DOUBLE_CLICK) {
				if (event.getPickedNode() instanceof GraphNode) {
					expandCollapseNode((GraphNode) event.getPickedNode());
				}
			}
		}
	}
	
	/**
	 * Expands a node if it is not already expanded, otherwise it collapses it. In this case
	 * expansion is equivalent to displaying only the nodes directly related to the one being
	 * expanded, and collapsing an expanded node is to revert to all nodes shown.
	 * 
	 * @param graphNode The node to expand or collapse.
	 */
	private void expandCollapseNode(GraphNode graphNode) {
		graphNode.setHighlighted(false);
		graphNode.moveToFront();
		if (graphModel.isExpanded(graphNode)) {
			graphModel.collapseNode(graphNode);
			if (graphNode.getUserObject() instanceof OWLClass){
				controlla.showOWLClass((OWLClass) graphNode.getUserObject());
			}else{
				controlla.showOWLEntity((OWLEntity) graphNode.getUserObject());
			}
			graph.setLastLayout(graph.getLayout("Tree - Horizontal"));
			graph.performLayout();
		} else {
			graphModel.expandNode(graphNode, true);
			if (graphNode.getUserObject() instanceof OWLClass){
				controlla.showOWLClass((OWLClass) graphNode.getUserObject());
			}else{
				controlla.showOWLEntity((OWLEntity) graphNode.getUserObject());
			}
			graph.setLastLayout(graph.getLayout("Tree - Horizontal"));
			graph.performLayout();
		}
	}

}
