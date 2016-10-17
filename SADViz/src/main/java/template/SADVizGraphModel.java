package template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.swing.Icon;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLQuantifiedRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.search.EntitySearcher;

import ca.uvic.cs.chisel.cajun.filter.FilterManager;
import ca.uvic.cs.chisel.cajun.graph.DefaultGraphModel;
import ca.uvic.cs.chisel.cajun.graph.arc.DefaultGraphArc;
import ca.uvic.cs.chisel.cajun.graph.arc.GraphArc;
import ca.uvic.cs.chisel.cajun.graph.node.DefaultGraphNode;
import ca.uvic.cs.chisel.cajun.graph.node.GraphNode;

public class SADVizGraphModel extends DefaultGraphModel{
	
	protected static final String DIRECT_SUBCLASS_SLOT_TYPE = "has a subclass";
	protected static final String DIRECT_INDIVIDUAL_SLOT_TYPE = "has an individual";
	protected static final String SUFFIX_DOMAIN_RANGE = " (ObjectProperty)";
	protected static final String SUB_CLASS_SOME_VALUE_OF = "(Subclass some)";
	protected static final String SUB_CLASS_ALL_VALUES = "(Subclass all)";
	protected static final String EQUIVALENT_CLASS_SOME_VALUE_OF = "(Equivalent class some)";
	protected static final String EQUIVALENT_CLASS_ALL_VALUES = "(Equivalent class all)";
	
	protected String restrictToArcType;
	
	public OWLModelManager manager;
	private OWLEditorKit kit;
	private Set<OWLOntology> ontologies;
	
	private OWLIconProviderImpl iconProvider;
	
	private FilterManager filterManager;
	private boolean allShown;
	
	private Collection<Object> arcTypes;
	private Map<OWLEntity, Set<GraphArc>> arcsForEntity;
	private Set<GraphArc> domainRangeRelationships;
	private Map<OWLNamedIndividual, Set<GraphArc>> artifactToUnreifiedRels;
	
	protected static final String UNKNOWN_TYPE = "unknown";
	protected static final String CLASS_TYPE = "class";
	protected static final String INDIVIDUAL_TYPE = "individual";
	
	public SADVizGraphModel(OWLEditorKit kit){
		super();
		iconProvider = new OWLIconProviderImpl(manager);
		manager = kit.getModelManager();
		this.kit = kit;
		allShown = false;
		
		ontologies = manager.getActiveOntologies();
		
		arcsForEntity = new HashMap<OWLEntity, Set<GraphArc>>();
	}

	/**
	 * Adds the specified OWLEntity to the model and displays it on the screen.
	 * 
	 * @param OWLEntity entity, FilterManager filterManager2. 
	 */
	public void show(OWLEntity entity, FilterManager filterManager2) {
		this.filterManager = filterManager2;
		
		List<GraphArc> arcs = new ArrayList<GraphArc>();
		
		addNode(entity);
		
		arcs.addAll(createIncomingRelationships(entity, true));
		arcs.addAll(createOutgoingRelationships(entity, true));

		addArcsToModel(arcs, false);
		recalculateArcStyles(); 
		
	}
	
	/**
	 * Adds all OWLEntities from the active ontology to the display.
	 */
	public void addAllNodes(){
		for (OWLEntity entity : manager.getActiveOntology().getClassesInSignature()){
			show(entity, filterManager);
		}
		
		for (OWLEntity entity : manager.getActiveOntology().getIndividualsInSignature()){
			show(entity, filterManager);
		}
		
		allShown = true;
	}
	
	public OWLModelManager getOwlModelManager() {
		return manager;
	}
	
	/**
	 * Adds the immediately related entities of the specified entity to the model,
	 * with a boolean value to determine whether to remove other nodes.
	 * 
	 * @param OWLEntity entity, boolean removeOldNodes. 
	 */
	public void showNeighbourhood(OWLEntity entity, boolean removeOldNodes) {
		List<OWLEntity> singleItemList = new ArrayList<OWLEntity>(1);
		singleItemList.add(entity);

		showNeighbourhood(singleItemList, removeOldNodes);
	}
	
	/**
	 * Adds the immediately related entities of the specified entity to the model,
	 * with a boolean value to determine whether to remove other nodes.
	 * 
	 * @param OWLEntity entity, boolean removeOldNodes. 
	 */
	public void showNeighbourhood(Collection<? extends OWLEntity> nodes, boolean removeOldNodes) {
		List<GraphArc> arcs = new ArrayList<GraphArc>();
		for (OWLEntity entity : nodes) {
			arcs.addAll(createIncomingRelationships(entity, false));
			arcs.addAll(createOutgoingRelationships(entity, false));
		}

		addArcsToModel(arcs, removeOldNodes);
		recalculateArcStyles();
	}

	/**
	 * Loads the specified arcs to the model and displays it on the screen.
	 * 
	 * @param Collection<GraphArc> arcs, boolean b. 
	 */
	private void addArcsToModel(Collection<GraphArc> arcs, boolean b) {
		Set<GraphNode> newGraphNodes = new HashSet<GraphNode>();

		for (GraphArc arc : arcs) {
			GraphArc createdArc = addArc((OWLEntity) arc.getSource().getUserObject(), (OWLEntity) arc.getDestination().getUserObject(), arc.getType().toString(), arc.getIcon());
			if (createdArc != null) {
				newGraphNodes.add(createdArc.getSource());
				newGraphNodes.add(createdArc.getDestination());
				createdArc.setInverted(arc.isInverted());
			}
		}

		if (b) {
			GraphNode[] allNodes = getAllNodes().toArray(new GraphNode[getAllNodes().size()]);
			for (GraphNode node : allNodes) {
				if (!newGraphNodes.contains(node)) {
					removeNode(node.getUserObject());
				}
			}
		}
	}

	/**
	 * Creates and adds an arc to the model.
	 * 
	 * @param OWLEntity userObject, OWLEntity userObject2, String string, Icon icon. 
	 */
	protected GraphArc addArc(OWLEntity userObject, OWLEntity userObject2, String string, Icon icon) {
		if (!string.contains(restrictToArcType)) {
			return null;
		}

		boolean newNode = true;
		if (getNode(userObject) != null) {
			newNode = false;
		}

		GraphNode srcNode = addNode(userObject);
		GraphNode destNode = addNode(userObject2);

		if (newNode) {
			destNode.setLocation(srcNode.getBounds().getX(), srcNode.getBounds().getY());
		}

		String key = userObject.toString() + string + userObject2.toString();
		DefaultGraphArc arc = (DefaultGraphArc) addArc(key, srcNode, destNode, string, icon);
		arc.setInverted(true);
		return arc;
	}

	/**
	 * Creates and adds a node to the model.
	 * 
	 * @param OWLEntity entity. 
	 */
	protected GraphNode addNode(OWLEntity entity) {
		Icon icon = iconProvider.getIcon(entity);
		return addNode(entity, manager.getRendering(entity), icon, getNodeType(entity));
	}
	
	private Collection<Object> generateArcTypes() {
		Set<Object> types = new HashSet<Object>();
		types.add(SADVizGraphModel.DIRECT_SUBCLASS_SLOT_TYPE);
		types.add(SADVizGraphModel.DIRECT_INDIVIDUAL_SLOT_TYPE);
		types.addAll(super.getArcTypes());
		
		return types;
	}
	
	@Override
	public Collection<Object> getArcTypes() {
		if(arcTypes == null) {
			arcTypes = generateArcTypes();
		}
		else {
			Collection<Object> types = generateArcTypes();
			if(types.size() != arcTypes.size()) {
				arcTypes.addAll(types);
			}
		}
		return arcTypes;
	}

	protected String getNodeType(OWLEntity entity) {
		if(entity instanceof OWLClass) {
            return CLASS_TYPE;
        } else if(entity instanceof OWLIndividual) {
            return INDIVIDUAL_TYPE;
        }
		return UNKNOWN_TYPE;
	}

	/**
	 * Creates and adds outgoing relationships to an entity.
	 * 
	 * @param OWLEntity entity, boolean b. 
	 */
	protected Set<GraphArc> createOutgoingRelationships(OWLEntity entity, boolean b) {
		Set<GraphArc> outgoingRels = new HashSet<GraphArc>();

		outgoingRels.addAll(loadChildren(entity, b));
		outgoingRels.addAll(loadDomainRangeRels(entity, true, b));
		outgoingRels.addAll(findOutgoingIndividualRelationships(entity, b));
		outgoingRels.addAll(findOutgoingConditionsRelationships(entity, b));

		return outgoingRels;
	}
	
	private Set<GraphArc> findOutgoingConditionsRelationships(OWLEntity entity, boolean b) {
		Set<GraphArc> arcs = new HashSet<GraphArc>();

		if (!(entity instanceof OWLClass)) {
			return arcs;
		}

		OWLClass owlClass = (OWLClass) entity;
		
        convertOWLClassExpressionsToArcs(owlClass,
                EntitySearcher.getSuperClasses(owlClass, ontologies), arcs,
                null, b);
		
		OWLIconProviderImpl iconProvider = new OWLIconProviderImpl(manager);
		Icon icon = iconProvider.getIcon(owlClass);
        convertOWLClassExpressionsToArcs(owlClass,
                EntitySearcher.getEquivalentClasses(owlClass, ontologies),
                arcs, icon, b);
		
		return arcs;
	}

	private void convertOWLClassExpressionsToArcs(OWLClass owlClass,
            Collection<OWLClassExpression> expressions, Set<GraphArc> arcs,
            Icon icon, boolean mustBeVisible) {
		for(OWLClassExpression expression : expressions) {
			if(expression.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM) 
					|| expression.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)) {
				convertOWLClassExpressionToArcs(owlClass, expression, arcs, icon, mustBeVisible);
			}
			else if(expression.getClassExpressionType().equals(ClassExpressionType.OBJECT_INTERSECTION_OF)) {
				for(OWLClassExpression e : expression.asConjunctSet()) {
					convertOWLClassExpressionToArcs(owlClass, e, arcs, icon, mustBeVisible);
				}
			}
		}
	}
	
	private void convertOWLClassExpressionToArcs(OWLClass owlClass, OWLClassExpression expression, Set<GraphArc> arcs, Icon icon, boolean mustBeVisible) {
		boolean isSubClass = true;
		if(icon != null) {
			isSubClass = false; 
		}
		
		for(OWLClassExpression e : expression.asConjunctSet()) {
			if(e instanceof OWLQuantifiedRestriction) {
				OWLQuantifiedRestriction restriction = (OWLQuantifiedRestriction)e;
				if(restriction.getFiller() instanceof OWLClass) {
					String relType = manager.getRendering(restriction.getProperty());
					if(isSubClass) {
						if(restriction instanceof OWLObjectSomeValuesFrom) {
                            relType += SUB_CLASS_SOME_VALUE_OF;
                        } else {
                            relType += SUB_CLASS_ALL_VALUES;
                        }
					}
					else {
						if(restriction instanceof OWLObjectSomeValuesFrom) {
                            relType += EQUIVALENT_CLASS_SOME_VALUE_OF;
                        } else {
                            relType += EQUIVALENT_CLASS_ALL_VALUES;
                        }
					}
					
					if(!filterManager.isArcTypeVisible(relType)) {
                        continue;
                    }
					
					if(isDisplayableNode( (OWLClass)restriction.getFiller(), mustBeVisible)) {
						arcs.add(createArc(owlClass, (OWLClass)restriction.getFiller(), relType, icon));
					}
				}
			}
		}
	}

	protected boolean isDisplayableNode(OWLEntity filler, boolean mustBeVisible) {
		return !mustBeVisible || mustBeVisible && getNode(filler) != null;
	}

	protected GraphArc createArc(OWLEntity srcCls, OWLEntity targetCls, String relType) {
		return createArc(srcCls, targetCls, relType, null);
	}
	
	protected GraphArc createArc(OWLEntity srcCls, OWLEntity targetCls, String relType, Icon icon) {
		GraphNode srcNode = new DefaultGraphNode(srcCls);
		GraphNode destNode = new DefaultGraphNode(targetCls);

		return createArc(srcNode, destNode, relType, icon);
	}
	
	protected GraphArc createArc(GraphNode srcNode, GraphNode destNode, String relType, Icon icon) {
		String key = srcNode.getUserObject().toString() + relType + destNode.getUserObject().toString();

		return new DefaultGraphArc(key, srcNode, destNode, icon, relType);
	}

	private Set<GraphArc> findOutgoingIndividualRelationships(OWLEntity entity, boolean b) {
		Set<GraphArc> incomingInstanceRels = new HashSet<GraphArc>();

		if (!(entity instanceof OWLClass)) {
            return incomingInstanceRels;
        }
		if(!filterManager.isArcTypeVisible(DIRECT_INDIVIDUAL_SLOT_TYPE)) {
            return incomingInstanceRels;
        }

		OWLClass owlClass = (OWLClass) entity;
        for (OWLIndividual individual : EntitySearcher.getIndividuals(owlClass,
                ontologies)) {
			if(individual instanceof OWLNamedIndividual) {
				OWLNamedIndividual namedIndividual = (OWLNamedIndividual)individual;
				if(isDisplayableNode(namedIndividual, b)) {
					String relType = DIRECT_INDIVIDUAL_SLOT_TYPE;
					GraphArc arc = createArc(owlClass, namedIndividual, relType);
					incomingInstanceRels.add(arc);
				}
			}
		}

		return incomingInstanceRels;
	}

	private Set<GraphArc> loadDomainRangeRels(OWLEntity entity, boolean b, boolean b2) {
		Set<GraphArc> domainRangeArcs = new HashSet<GraphArc>();
		
		getDomainRangeRelationships(); // ensures that domain range rels are created
		
		for (GraphArc relationship : domainRangeRelationships) {
			if(!filterManager.isArcTypeVisible(relationship.getType())) {
                continue;
            }
			
			OWLEntity sourceObject = (OWLEntity) relationship.getSource().getUserObject();
			OWLEntity destObject = (OWLEntity) relationship.getDestination().getUserObject();

			if (!isDisplayableNode(sourceObject, b2) || !isDisplayableNode(destObject, b2)) {
				continue;
			}
			if (b && sourceObject.equals(entity) || destObject.equals(entity)) {
				if(b) {
                    relationship.setInverted(false);
                }
				
				domainRangeArcs.add(relationship);
			}
			
		}

		return domainRangeArcs;
	}
	
	private void createDomainRangeRels() {
domainRangeRelationships = new HashSet<GraphArc>();
		
		for (OWLOntology owlOntology : ontologies) {
    		Set<OWLObjectProperty> properties = owlOntology.getObjectPropertiesInSignature();
    		
    		for(OWLObjectProperty property : properties) {
    			for(OWLObjectProperty owlObjectProperty : property.getObjectPropertiesInSignature()) {
                    Collection<OWLClassExpression> domainVals = EntitySearcher
                            .getDomains(owlObjectProperty, owlOntology);
                    Collection<OWLClassExpression> rangeVals = EntitySearcher
                            .getRanges(owlObjectProperty, owlOntology);
    				
    				if (domainVals.isEmpty() && !rangeVals.isEmpty()) {
    					domainVals.add(manager.getOWLEntityFinder().getOWLClass("Thing"));
    				} else if (rangeVals.isEmpty() && !domainVals.isEmpty()) {
    					rangeVals.add(manager.getOWLEntityFinder().getOWLClass("Thing"));
    				}
    				
    				Set<OWLEntity> domains = getOWLClasses(domainVals);
    				Set<OWLEntity> ranges = getOWLClasses(rangeVals);
    				
    				createDomainRangeRels(domains, ranges, owlObjectProperty);
    			}
    		}
		}
	}

	private void createDomainRangeRels(Set<OWLEntity> domains, Set<OWLEntity> ranges,
			OWLObjectProperty owlObjectProperty) {
		for(OWLEntity domainClass : domains) {
			GraphNode srcNode = new DefaultGraphNode(domainClass);
			for (OWLEntity rangeClass : ranges) {
				GraphNode destNode = new DefaultGraphNode(rangeClass);
				String relType = manager.getRendering(owlObjectProperty) + SUFFIX_DOMAIN_RANGE;
				
				GraphArc arc = createArc(srcNode, destNode, relType, null);
				if(!domainRangeRelationships.contains(arc)) {
					domainRangeRelationships.add(arc);
				}
			}
		}
	}

	private Set<OWLEntity> getOWLClasses(Collection<OWLClassExpression> domainVals) {
			Set<OWLEntity> domains = new HashSet<OWLEntity>();
			for(OWLClassExpression expression : domainVals) {
				if(expression instanceof OWLClass) {
					domains.add((OWLClass)expression);
				}
			}
			
		return domains;
	}

	private Set<GraphArc> getDomainRangeRelationships() {
		if (domainRangeRelationships == null) {
			createDomainRangeRels();
		}
		return domainRangeRelationships;
	}

	/**
	 * Loads and returns child entities of specified entity.
	 * 
	 * @param OWLEntity entity, boolean b. 
	 */
	protected Set<GraphArc> loadChildren(OWLEntity entity, boolean b) {
		Set<GraphArc> children = new HashSet<GraphArc>();
		
		if(!(entity instanceof OWLClass)) {
            return children;
        }
		if(!filterManager.isArcTypeVisible(DIRECT_SUBCLASS_SLOT_TYPE)) {
            return children;
        }
		
		OWLClass clsOfInterest = (OWLClass)entity;
		
		for(OWLClass childCls : kit.getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider().getChildren(clsOfInterest)) {
			if (isDisplayableNode(childCls, b)) {
				GraphArc arc = createArc(clsOfInterest, childCls, DIRECT_SUBCLASS_SLOT_TYPE);
				children.add(arc);
			}
		}

		return children;
	}

	protected Set<GraphArc> createIncomingRelationships(OWLEntity entity, boolean b) {
		Set<GraphArc> incomingArcs = new HashSet<GraphArc>();

		incomingArcs.addAll(loadParents(entity, b));
		incomingArcs.addAll(loadDomainRangeRels(entity, false, b));
		incomingArcs.addAll(findIncomingIndividualRelationships(entity, b));
		incomingArcs.addAll(loadUnreifiedRelations(entity, b));
		incomingArcs.addAll(findIncomingConditionsRelationships(entity, b));
		
		return incomingArcs;
	}

	private Set<GraphArc> findIncomingConditionsRelationships(OWLEntity entity, boolean b) {
		Set<GraphArc> arcs = new HashSet<GraphArc>();

		if (!(entity instanceof OWLClass)) {
			return arcs;
		}

		for (OWLOntology owlOntology : ontologies) {
    		OWLClass owlClass = (OWLClass) entity;
            Collection<OWLAxiom> axioms = EntitySearcher.getReferencingAxioms(
                    owlClass, owlOntology, true);
    		for(OWLAxiom axiom : axioms) {
    			if(axiom.getAxiomType().equals(AxiomType.SUBCLASS_OF)) {
    				OWLSubClassOfAxiom subClassAxiom = (OWLSubClassOfAxiom)axiom;
    				OWLClassExpression subClassExpression = subClassAxiom.getSubClass();
    				
    				if(subClassExpression instanceof OWLClass) {
    					OWLClassExpression superClassExpression = subClassAxiom.getSuperClass();
    					if(superClassExpression instanceof OWLQuantifiedRestriction) {
    						OWLQuantifiedRestriction restriction = (OWLQuantifiedRestriction)superClassExpression;
    						if(restriction.getFiller() instanceof OWLClass) {
    							String relType = manager.getRendering(restriction.getProperty());
    							if(restriction instanceof OWLObjectSomeValuesFrom) {
                                    relType += SUB_CLASS_SOME_VALUE_OF;
                                } else {
                                    relType += SUB_CLASS_ALL_VALUES;
                                }
    							
    							if(!filterManager.isArcTypeVisible(relType)) {
                                    continue;
                                }
    							
    							OWLEntity source = (OWLClass)subClassExpression;
    							OWLEntity target = (OWLClass)restriction.getFiller();
    							
    							if(isDisplayableNode(source, b) && isDisplayableNode(target, b)) {
    								arcs.add(createArc(source, target, relType));
    							}
    						}
    					}
    				}
    			}
    		}
		}
		
		return arcs;
	}

	private Set<GraphArc> loadUnreifiedRelations(OWLEntity entity, boolean b) {
		unreifyRelationInstances();
		
		Set<GraphArc> unreifiedRels = artifactToUnreifiedRels.get(entity);
		if(unreifiedRels != null) {
			for(GraphArc arc : unreifiedRels) {
				if(!filterManager.isArcTypeVisible(arc.getType())) {
					unreifiedRels.remove(arc);
				}
				else {
					if(!isDisplayableNode((OWLEntity)arc.getDestination().getUserObject(), b)) {
						unreifiedRels.remove(arc);
					}
				}
			}
		}
		return unreifiedRels == null ? new HashSet<GraphArc>() : unreifiedRels;
	}

	private void unreifyRelationInstances() {
		if (artifactToUnreifiedRels != null) {
			return;
		}

		artifactToUnreifiedRels = new HashMap<OWLNamedIndividual, Set<GraphArc>>();

		for (OWLOntology owlOntology : ontologies) {
			for(OWLNamedIndividual individual : owlOntology.getIndividualsInSignature()) {
                for (Entry<OWLObjectPropertyExpression, Collection<OWLIndividual>> entry : EntitySearcher
                        .getObjectPropertyValues(individual, owlOntology).asMap()
                        .entrySet()) {
    				for(OWLIndividual refIndividual : entry.getValue()) {
    					GraphArc arc = createArc(individual, (OWLNamedIndividual)refIndividual, manager.getRendering(entry.getKey()));
    					
    					Set<GraphArc> outgoingUnreifiedRels = artifactToUnreifiedRels.get(individual);
    					if (outgoingUnreifiedRels == null) {
    						outgoingUnreifiedRels = new HashSet<GraphArc>();
    						artifactToUnreifiedRels.put(individual, outgoingUnreifiedRels);
    					}
    					outgoingUnreifiedRels.add(arc);
    					
    					Set<GraphArc> incomingUnreifiedRels = artifactToUnreifiedRels.get(refIndividual);
    					if (incomingUnreifiedRels == null) {
    						incomingUnreifiedRels = new HashSet<GraphArc>();
    						artifactToUnreifiedRels.put((OWLNamedIndividual)refIndividual, incomingUnreifiedRels);
    					}
    					incomingUnreifiedRels.add(arc);
    				}
    			}
    		}
		}
	}

	/**
	 * Returns arcs leading to individuals.
	 * 
	 * @param OWLEntity entity, boolean b. 
	 */
	private Set<GraphArc> findIncomingIndividualRelationships(OWLEntity entity, boolean b) {
		Set<GraphArc> arcs = new HashSet<GraphArc>();

		if (!(entity instanceof OWLNamedIndividual)) {
            return arcs;
        }
		if(!filterManager.isArcTypeVisible(DIRECT_INDIVIDUAL_SLOT_TYPE)) {
            return arcs;
        }

		OWLNamedIndividual destIndiv = (OWLNamedIndividual) entity;
        for (OWLClassExpression refNode : EntitySearcher.getTypes(destIndiv,
                ontologies)) {
			if(refNode instanceof OWLClass) {
				OWLClass clsOwner = (OWLClass)refNode;
				if (isDisplayableNode(clsOwner, b)) {
					String relType = DIRECT_INDIVIDUAL_SLOT_TYPE;
	
					arcs.add(createArc(clsOwner, destIndiv, relType));
				}
			}
		}
		
		return arcs;
	}

	/**
	 * Returns arcs leading to superclasses.
	 * 
	 * @param OWLEntity entity, boolean b. 
	 */
	protected Set<GraphArc> loadParents(OWLEntity entity, boolean b) {
		Set<GraphArc> parents = new HashSet<GraphArc>();
		
		if(!(entity instanceof OWLClass)) {
            return parents;
        }
		if(!filterManager.isArcTypeVisible(DIRECT_SUBCLASS_SLOT_TYPE)) {
            return parents;
        }
		
		OWLClass clsOfInterest = (OWLClass)entity;
		
		for(OWLClass parentCls : kit.getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider().getParents(clsOfInterest)) {
			if (isDisplayableNode(parentCls, b)) {
				GraphArc arc = createArc(parentCls, clsOfInterest, DIRECT_SUBCLASS_SLOT_TYPE);
				arc.setInverted(false);
				parents.add(arc);
			}
		}

		return parents;
	}

	public void resetNodeToArcCount() {
		for(OWLEntity owlEntity : arcsForEntity.keySet()) {
			arcsForEntity.put(owlEntity, (Set<GraphArc>)getArcsForEntity(owlEntity));
		}
	}

	/**
	 * Returns true if number of arcs displayed on node is equal to number of arcs for that node.
	 * 
	 * @param GraphNode graphNode. 
	 */
	private Set<GraphArc> getArcsForEntity(OWLEntity owlEntity) {
		Set<GraphArc> arcs = new HashSet<GraphArc>();

		arcs.addAll(createIncomingRelationships(owlEntity, false));
		arcs.addAll(createOutgoingRelationships(owlEntity, false));

		return arcs;
	}

	/**
	 * Returns true if number of arcs displayed on node is equal to number of arcs for that node.
	 * 
	 * @param GraphNode graphNode. 
	 */
	public boolean isExpanded(GraphNode graphNode) {
		if (allShown==true){
			return false;
		}
		
		int modelArcsSize = graphNode.getArcs().size();
		int frameArcsSize = getNodeToArcCount((OWLEntity) graphNode.getUserObject());

		return modelArcsSize >= frameArcsSize;
	}

	/**
	 * Returns number of arcs for this object.
	 * 
	 * @param OWLEntity userObject. 
	 */
	private int getNodeToArcCount(OWLEntity userObject) {
		Set<GraphArc> arcs = getCachedArcsForEntity(userObject);

		return arcs.size();
	}

	/**
	 * Returns arcs already stored in arcsForEntity, otherwise creates them,
	 * stores them and returns them.
	 * 
	 * @param OWLEntity userObject. 
	 */
	public Set<GraphArc> getCachedArcsForEntity(OWLEntity userObject) {
		Set<GraphArc> arcs = arcsForEntity.get(userObject);
		if (arcs == null) {
			arcs = (Set<GraphArc>)getArcsForEntity(userObject);
			arcsForEntity.put(userObject, arcs);
		}

		return arcs;
	}
	
	/**
	 * Expands a node by displaying all nodes immediately connected to it.
	 * 
	 * @param GraphNode graphNode, boolean b. 
	 */
	public void expandNode(GraphNode graphNode, boolean b) {
		allShown=false;
		showNeighbourhood((OWLEntity) graphNode.getUserObject(), b);
	}

	/**
	 * Collapses an expanded node by reverting to the view where all nodes are displayed.
	 */
	public void collapseNode(GraphNode graphNode) {
		addAllNodes();
	}
	
	/**
	 * Performs a search function over the ontology, displaying the required nodes
	 * and their immediate connections.
	 * 
	 * @param String searchString, FilterManager filterManager. 
	 */
	public Collection<? extends OWLEntity> search(String searchString, FilterManager filterManager) {
		restrictToArcType = "";
		this.filterManager = filterManager;
		
		Set<OWLEntity> searchResults = new HashSet<OWLEntity>();
		Set<? extends OWLEntity> matchingClasses = manager.getOWLEntityFinder().getMatchingOWLClasses(searchString, true, Pattern.CASE_INSENSITIVE);
		Set<? extends OWLEntity> matchingIndividuals = manager.getOWLEntityFinder().getMatchingOWLIndividuals(searchString, true, Pattern.CASE_INSENSITIVE);

		searchResults.addAll(matchingClasses);
		searchResults.addAll(matchingIndividuals);
		
		if (searchResults != null) {
			showNeighbourhood(searchResults, true);
		}
		
		show((OWLEntity)searchResults.toArray()[0], filterManager);

		return searchResults;
	}
	
	public void setFilterManager(FilterManager filterManager2) {
		this.filterManager=filterManager2;
	}
}
