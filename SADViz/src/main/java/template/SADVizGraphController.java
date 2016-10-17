package template;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JToolBar;

import org.eclipse.zest.layouts.algorithms.DirectedGraphLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.HorizontalDirectedGraphLayoutAlgorithm;
import org.eclipse.zest.layouts.progress.ProgressEvent;
import org.eclipse.zest.layouts.progress.ProgressListener;
import org.protege.editor.owl.OWLEditorKit;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;

import ca.uvic.cs.chisel.cajun.actions.LayoutAction;
import ca.uvic.cs.chisel.cajun.constants.LayoutConstants;
import ca.uvic.cs.chisel.cajun.filter.FilterChangedEvent;
import ca.uvic.cs.chisel.cajun.filter.FilterChangedListener;
import ca.uvic.cs.chisel.cajun.graph.FlatGraph;
import ca.uvic.cs.chisel.cajun.graph.Graph;
import ca.uvic.cs.chisel.cajun.graph.arc.GraphArc;
import ca.uvic.cs.chisel.cajun.graph.node.DefaultGraphNode;
import ca.uvic.cs.chisel.cajun.graph.node.GraphNode;
import ca.uvic.cs.chisel.cajun.graph.ui.DefaultFlatGraphView;
import edu.umd.cs.piccolo.util.PBounds;

public class SADVizGraphController {
	
	public SADVizGraphModel model;
	private OWLClass owlClass;
	private int DURATION;
	private DefaultFlatGraphView view;
	private FlatGraph graph;
	public OWLEditorKit kit;
	
	/**
	 * Initializes the controller and sets style of nodes and arcs.
	 * 
	 * @param Container window, OWLEditorKit kit.
	 */
	public SADVizGraphController(Container window, OWLEditorKit kit){
		this.kit=kit;
		this.DURATION=50;
		
		model=new SADVizGraphModel(this.kit);
		
		this.graph = new FlatGraph(model);
		
		model.setFilterManager(graph.getFilterManager());
		
		SADVizNodeStyle nodeStyle = new SADVizNodeStyle();
		nodeStyle.setNodeTypes(model.getNodeTypes());
		this.graph.setGraphNodeStyle(nodeStyle);
		
		SADVizArcStyle arcStyle = new SADVizArcStyle() {
			public Stroke getStroke(GraphArc arc) {
				if (arc.getType().toString().contains(SADVizGraphModel.DIRECT_SUBCLASS_SLOT_TYPE) 
						|| arc.getType().toString().contains(SADVizGraphModel.DIRECT_INDIVIDUAL_SLOT_TYPE)) {
					setDashed(true);
				} else {
					setDashed(false);
					if (arc.getType().toString().contains("Equivalent")) {
						setDashedCapSquare(BasicStroke.CAP_ROUND);
						setDashWidth(2f);
					}else {
						setDashedCapSquare(BasicStroke.CAP_SQUARE);
						setDashWidth(10f);
					}
				}

				return super.getStroke(arc);
			}
		};
		arcStyle.setArcTypes(model.getArcTypes());
		this.graph.setGraphArcStyle(arcStyle);
		
		initialize(window);
		
		this.graph.addLayoutListener(new ProgressListener() {
			public void progressEnded(ProgressEvent arg0) {
				DefaultGraphNode node = (DefaultGraphNode)model.getNode(owlClass);
				panTo(node);
			}

			public void progressStarted(ProgressEvent arg0) {}
			public void progressUpdated(ProgressEvent arg0) {}
		});
		
		this.graph.getFilterManager().addFilterChangedListener(new FilterChangedListener() {
			public void filtersChanged(FilterChangedEvent fce) {
				model.resetNodeToArcCount();
			}
		});
	}
	
	/**
	 * Initializes the space within which the graph is displayed
	 * 
	 * @param Container window2.
	 */
	private void initialize(Container window2) {
				List<Object> layoutRelTypes = new ArrayList<Object>();
				layoutRelTypes.add(SADVizGraphModel.DIRECT_SUBCLASS_SLOT_TYPE);
				layoutRelTypes.add(SADVizGraphModel.DIRECT_INDIVIDUAL_SLOT_TYPE);
				for (LayoutAction layoutAction : graph.getLayouts()) {
					if (layoutAction.getName().equals(LayoutConstants.LAYOUT_TREE_HORIZONTAL)) {
						layoutAction.setLayout(new HorizontalDirectedGraphLayoutAlgorithm());
						this.graph.setLastLayout(layoutAction);
						layoutAction.setLayoutRelTypes(layoutRelTypes);
					} else if (layoutAction.getName().equals(LayoutConstants.LAYOUT_TREE_VERTICAL)) {
						layoutAction.setLayout(new DirectedGraphLayoutAlgorithm());
						layoutAction.setLayoutRelTypes(layoutRelTypes);
					}
				}

				view = new DefaultFlatGraphView(graph);

				window2.add(view, BorderLayout.CENTER);

				graph.addInputEventListener(new SADVizInputEventHandler(model, graph, this));
	}
	
	public Graph getGraph() {
		return graph;
	}
	
	/**
	 * If OWLClass passed as argument to this function is already displayed it pans to the existing
	 * node, otherwise adds the OWLClass as a node to the visualisation.
	 * 
	 * @param OWLClass (the class to be displayed).
	 */
	public void showOWLClass(OWLClass owlClass) {
		for(GraphNode node : model.getAllNodes()) {
			node.setFixedLocation(true);
		}
		
		boolean panToNode = true;
		if(model.getNode(owlClass) == null) {
			panToNode = false;
		}
		
		model.restrictToArcType = "";
		model.show(owlClass, graph.getFilterManager());
		
		Collection<GraphNode> matchingNodes = new ArrayList<GraphNode>();
		matchingNodes.add(model.getNode(owlClass));
		
		graph.setMatchingNodes(matchingNodes);
		graph.performLayout();
		
		for(GraphNode node : model.getAllNodes()) {
			node.setFixedLocation(false);
		}
		
		this.owlClass = owlClass;
		
		if(panToNode) {
			DefaultGraphNode node = (DefaultGraphNode)model.getNode(owlClass);
			panTo(node);
		}
	}
	
	public void showOWLEntity(OWLEntity entity) {
		model.restrictToArcType = "";
		model.show(entity, graph.getFilterManager());
	}
	
	/**
	 * Displays all nodes to screen.
	 */
	public void showAllNodes(){
		model.addAllNodes();
	}
	
	/**
	 * Performs layout on graph displayed
	 * 
	 * @param LayoutAction.
	 */
	public void peformLayout(LayoutAction layout){
		graph.setLastLayout(layout);
		graph.performLayout();
	}

	/**
	 * Places specified node at centre of view.
	 * 
	 * @param DefaultGraphNode
	 */
	public void panTo(DefaultGraphNode node) {
		if(node != null) {
			double x = node.getFullBoundsReference().getX();
			double y = node.getFullBoundsReference().getY();
			double w = node.getFullBoundsReference().getWidth();
			double h = node.getFullBoundsReference().getHeight();
			PBounds bounds = new PBounds(x - w * .01, y - h * .02, w + w * .02, h + h * .04);
			// only pan to the bounds if the node is not already visible
			if(!graph.getCamera().getViewBounds().contains(bounds.getBounds2D())) {
				graph.getRoot().getActivityScheduler().addActivity(
						graph.getCamera().animateViewToCenterBounds(bounds.getBounds2D(), false, DURATION)
				);
			}
		}
		
	}

	public JToolBar getToolBar() {
		return view.getToolBar();
	}
	
	/**
	 * Performs search action on all nodes looking for nodes that contain the searchString parameter.
	 * 
	 * @param String searchString.
	 */
	public int search(String searchString) {
		
		for(GraphNode node : model.getAllNodes()) node.setSelected(false);

		Collection<GraphNode> matchingNodes = new ArrayList<GraphNode>();
		Collection<? extends OWLEntity> searchResults = model.search(searchString, graph.getFilterManager());
		for (OWLEntity owlEntity : searchResults) {
			GraphNode node = model.getNode(owlEntity);
			if(node != null) {
				matchingNodes.add(model.getNode(owlEntity));
			}
		}

		graph.setSelectedNodes(matchingNodes);
		graph.setMatchingNodes(matchingNodes);
		graph.performLayout();

		return searchResults.size();
	}

	/**
	 * Clears the graph of all nodes.
	 */
	public void clear() {
		graph.clear();
		model.clear();
		model.restrictToArcType = "";
	}

}
