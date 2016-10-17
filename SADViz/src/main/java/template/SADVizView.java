package template;

import java.awt.*;

import javax.swing.*;

import org.protege.editor.owl.ui.view.cls.AbstractOWLClassViewComponent;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;

import ca.uvic.cs.chisel.cajun.graph.AbstractGraph;
import ca.uvic.cs.chisel.cajun.graph.node.GraphNode;
import ca.uvic.cs.chisel.cajun.graph.node.GraphNodeCollectionEvent;
import ca.uvic.cs.chisel.cajun.graph.node.GraphNodeCollectionListener;
import ca.uvic.cs.chisel.cajun.util.GradientPainter;
import ca.uvic.cs.chisel.cajun.util.GradientPanel;

import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.ActionEvent;

public class SADVizView extends AbstractOWLClassViewComponent {
	private static final long serialVersionUID = 1505057428784911280L;
	
	private SADVizGraphController controlla;
	private JLabel searchResults;
	private JTextField searchField;
	
	private static final Color BACKGROUND_COLOR = new Color(85, 19, 195);
	private boolean cancelSelectionUpdate;
	
	/**
	 * Creates the container for visualisation and the controller for manipulating the visualisation
	 *  as well as adding the toolbar and displaying all classes and individuals within ontology as nodes with 
	 *  correct relationships.
	 */
	public void initialiseClassView() throws Exception {
		setLayout(new BorderLayout());
		
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				super.componentResized(e);
				
			}
		});
		
		controlla = new SADVizGraphController(this, this.getOWLEditorKit());
		
		controlla.getGraph().addNodeSelectionListener(new GraphNodeCollectionListener() {
			public void collectionChanged(GraphNodeCollectionEvent arg0) {
				syncNodeSelection();
			}
		});

		Dimension d = new Dimension(800, 600);
		setPreferredSize(d);
		setSize(d);
		setLocation(100, 50);
		
		setVisible(true);
		
		add(getSearchPanel(), BorderLayout.SOUTH);
		
		makeToolBar();
		
		updateView(getOWLModelManager().getOWLDataFactory().getOWLThing());
		controlla.showAllNodes();
		
		controlla.peformLayout(((AbstractGraph) controlla.getGraph()).getLayout("Tree - Horizontal"));
	}
	
	/**
	 * Syncs the class selected in the ontology to the node the user has selected.
	 */
	private void syncNodeSelection() {
		GraphNode node = ((AbstractGraph)controlla.getGraph()).getFirstSelectedNode();
		if(node != null) {
			cancelSelectionUpdate = true;
			setGlobalSelection((OWLEntity)node.getUserObject());
		}
	}
	
	/**
	 * Adds two buttons to the tool bar - one to display all nodes, and one to clear the visualisation.
	 */
	private void makeToolBar(){
		JToolBar toolBar = controlla.getToolBar();
		
		toolBar.addSeparator();
		
		JButton btn = new JButton();
		btn.setText("SHOW ALL");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controlla.showAllNodes();
				controlla.peformLayout(((AbstractGraph) controlla.getGraph()).getLayout("Tree - Horizontal"));
			}
		});
		
		JButton btn2 = new JButton();
		btn2.setText("CLEAR");
		btn2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controlla.clear();
				searchResults.setText("");
			}
		});
		
		toolBar.add(btn);
		toolBar.addSeparator();
		toolBar.add(btn2);
	}

	/**
	 * If OWLClass passed as argument to this function is already displayed it pans to the existing
	 * node, otherwise adds the OWLClass as a node to the visualisation.
	 * 
	 * @param OWLClass (the class to be displayed).
	 */
	@Override
	protected OWLClass updateView(OWLClass owlClass) {
		if(owlClass != null && !cancelSelectionUpdate) {
			controlla.showOWLClass(owlClass);
		}
		
		cancelSelectionUpdate = false;
		
		return null;
	}


	@Override
	public void disposeView() {
	}
	
	/**
	 * Adds a search panel to the window beneath the visualisation container.
	 */
	private JPanel getSearchPanel() {
		JPanel searchPanel = new GradientPanel(GradientPanel.BG_START, BACKGROUND_COLOR, GradientPainter.TOP_TO_BOTTOM);

		searchField = new JTextField();
		searchField.setMinimumSize(new Dimension(300, 22));
		searchField.setSize(new Dimension(300, 22));
		searchField.setPreferredSize(new Dimension(300, 22));
		searchField.setFocusable(true);
		searchField.requestFocus();

		JButton searchButton = new JButton("Search");
		searchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performSearch();
			}
		});
		searchButton.setMinimumSize(new Dimension(80, 22));
		searchButton.setSize(new Dimension(80, 22));
		searchButton.setPreferredSize(new Dimension(80, 22));

		searchField.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {}

			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					performSearch();
				}
			}
		});

		searchResults = new JLabel();
		searchResults.setFont(searchResults.getFont().deriveFont(Font.BOLD));
		searchResults.setForeground(Color.white);
		searchResults.setOpaque(false);

		searchPanel.add(searchField,0);
		searchPanel.add(searchButton,1);
		searchPanel.add(searchResults,2);
		
		return searchPanel;
	}
	
	/**
	 * Searches for nodes that contain the string specified in the search field.
	 */
	private void performSearch() {
		if (searchField.getText().length() > 0) {
			this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			int numOfResults = controlla.search(searchField.getText());
			searchResults.setText(numOfResults + " result(s) found.");
			this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			
			syncNodeSelection();
		} else {
			JOptionPane.showMessageDialog(this, "You must enter a valid search term", "Invalid search term", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
}
