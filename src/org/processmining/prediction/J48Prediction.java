package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;

import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.datapetrinets.utils.WekaTreeClassificationAdapter;
import org.processmining.datapetrinets.utils.WekaTreeClassificationAdapter.WekaLeafNode;
import org.processmining.framework.util.Pair;
import org.processmining.models.FunctionEstimator.DecisionTreeFunctionEstimator;
import org.processmining.models.FunctionEstimator.Type;
import org.processmining.models.guards.Expression;
import org.processmining.prediction.NewTreeVisualizer.MyTreeVisualizer;
import org.processmining.prediction.NewTreeVisualizer.PlaceNode2;
import org.processmining.prediction.newPrefuseTreeVis.NewPrefuseTreeVis;
import org.processmining.prediction.newPrefuseTreeVis.TreePanel;
import org.processmining.prediction.newPrefuseTreeVis.VisConfigurables;

import weka.classifiers.trees.J48;
import weka.core.Instance;

public class J48Prediction extends DecisionTreeFunctionEstimator implements Leafable{
	private Set<String> timeIntervalAttributes = new HashSet<String>();
	private boolean balanceInstances=false;
	private Map<Object,List> listInstances=new HashMap<Object,List>();

	public J48Prediction(Map<String, Type> map, Map<String, Set<String>> literalValues, Object[] outputValuesAsObjects,
			String name, int capacity, HashSet<String> timeIntervalAttributes) {
		super(map, literalValues, removeDuplicate(outputValuesAsObjects), name, capacity);
		this.timeIntervalAttributes = timeIntervalAttributes;
	}
	
	public J48Prediction(Map<String, Type> map, Map<String, Set<String>> literalValues, Object[] outputValuesAsObjects,
			String name, int capacity, HashSet<String> timeIntervalAttributes,boolean balanceInstances) {
		this(map, literalValues, removeDuplicate(outputValuesAsObjects), name, capacity,timeIntervalAttributes);
		this.balanceInstances = balanceInstances;
		if (balanceInstances)
		{
			for(Object o : outputValuesAsObjects)
				listInstances.put(o, new ArrayList<Instance>());
		}
	}	

	private static Object[] removeDuplicate(Object[] outputValuesAsObjects) {
		return new HashSet<Object>(Arrays.asList(outputValuesAsObjects)).toArray();
	}
	

	/**
	 * Returns a JPanel containing a visualization of the weka J48 tree using
	 * prefusetrees
	 * 
	 * @return <JPanel> containing a visualization of the decision tree.
	 */
	public JComponent getPrefuseTreeVisualization() {
		System.out.println("Drawing decision tree");
		if (tree == null)
			return null;
		else {
			HashMap<String, PredictionType> predictionTypes = new HashMap<String, PredictionType>();
			
			for (Entry<String, Type> entry : variableType.entrySet()) {
				PredictionType p = null;
				switch (entry.getValue()) {
					case BOOLEAN :
						p = PredictionType.BOOLEAN;
						break;
					case CONTINUOS :
						p = PredictionType.CONTINUOS;
						break;
					case DISCRETE :
						if (timeIntervalAttributes.contains(entry.getKey()))
							p = PredictionType.TIMEINTERVAL;
						else
							p = PredictionType.DISCRETE;
						break;
					case LITERAL :
						p = PredictionType.LITERAL;
						break;
					case TIMESTAMP :
						p = PredictionType.TIMESTAMP;
						break;
					default :
						break;
				}
				predictionTypes.put(entry.getKey(), p);
			}
			try {
				//PrefuseTreeVisualization ptv = new PrefuseTreeVisualization();
				String dotty = ((J48) tree).graph(); 
				//Map<String, Color> map = VariablePanel.getColors();
				String name = this.classAttributeName;
				NewPrefuseTreeVis ptv = new NewPrefuseTreeVis();
				VisConfigurables config = new VisConfigurables();
				PredictionType classType = predictionTypes.get(name);
				TreePanel panel = ptv.display(dotty, name, VariablePanel.getColors(), predictionTypes, classType,config);
				panel.setBackground(config.bgColor);     
				
				//Create a layered pane
				JLayeredPane newPanel = new JLayeredPane();
				//newPanel.setPreferredSize(new Dimension(1280, 800));
				
				//Add TreePanel
			    newPanel.add(panel, 0, -1);
				
			    //Add Helpwindow
			    JButton button = new JButton("?");
			    button.setFont(new Font("Serif", Font.BOLD, 12));
			    button.addActionListener( new ActionListener()
			    {
					public void actionPerformed(ActionEvent arg0) {
						EventQueue.invokeLater(new Runnable()
				        {
				            @Override
				            public void run()
				            {
				                JFrame frame = new JFrame("Help");
				                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				                JPanel panel = new JPanel();
							    String[] text = {
										"Left-Clicking not on a node: Move visualisation.\n",
									    "Left-Clicking on a node: Expand children / Collapse children\n " ,
									    "Double left-clicking on a node: Expand from that node to the leaves in that (sub)tree\n" ,
									    "Right-click center visualisation and resize to fit screen.\n",
									    "Scrolling the mousewheel zooms the tree",
									    "Hold and drag right click zooms the tree to the cursor"
									    
							    };
				                panel.setOpaque(true);
				                JList<String> win = new JList<String>(text);
				                win.setFont(new Font("Serif", Font.ITALIC, 16));
				                panel.add(win);
				                frame.getContentPane().add(BorderLayout.CENTER, panel);
				                frame.pack();
				                frame.setLocationByPlatform(true);
				                frame.setVisible(true);
				                frame.setResizable(false);
				            }
				        });
						
					}
			    });
			    button.setBounds(0,0, 40, 40);
			    newPanel.add(button,0,0);	
				return newPanel;
			} catch (Exception e) {
				System.out.println(e);
				return null;
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	public List<Pair<String,Expression>> getExpressionsAtLeaves()
	{
		LinkedList<Pair<String,Expression>> retValue=new LinkedList<Pair<String,Expression>>();
		try {
			WekaTreeClassificationAdapter wekaJ48Adapter = new WekaTreeClassificationAdapter((J48) tree, instances,
					variableType);
			for( WekaLeafNode leaf : wekaJ48Adapter.traverseLeafNodes())
			{
				GuardExpression guard=leaf.getExpression();
				String s=leaf.getClassName();
				retValue.add(new Pair<String,Expression>(s,new Expression(guard.toCanonicalString())));
			}
			return retValue;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}

	@Override
	public JPanel getVisualization() {
		if (tree == null)
			return null;
		else {
			HashMap<String, PredictionType> predictionTypes = new HashMap<String, PredictionType>();
			for (Entry<String, Type> entry : variableType.entrySet()) {
				PredictionType p = null;
				switch (entry.getValue()) {
					case BOOLEAN :
						p = PredictionType.BOOLEAN;
						break;
					case CONTINUOS :
						p = PredictionType.CONTINUOS;
						break;
					case DISCRETE :
						if (timeIntervalAttributes.contains(entry.getKey()))
							p = PredictionType.TIMEINTERVAL;
						else
							p = PredictionType.DISCRETE;
						break;
					case LITERAL :
						p = PredictionType.LITERAL;
						break;
					case TIMESTAMP :
						p = PredictionType.TIMESTAMP;
						break;
					default :
						break;
				}
				predictionTypes.put(entry.getKey(), p);
			}
			try {
				return new MyTreeVisualizer(null, ((J48) tree).graph(), new PlaceNode2(), VariablePanel.getColors(),
						predictionTypes, null, -1, predictionTypes.get(classAttributeName));
			} catch (Exception e) {
				return null;
			}
		}
	}

	public void balanceInstances()
	{
		int max=0;
		for(List list : listInstances.values())
		{
			if (list.size()>max)
			{
				max=list.size();
			}
		}
		for(Entry<Object, List> entry : listInstances.entrySet())
		{
			int toBeGenerated=max-entry.getValue().size();
			Random r=new Random();
			for(int i=0;i<toBeGenerated;i++)
			{
				Instance inst=(Instance) entry.getValue().get(r.nextInt(entry.getValue().size()-1));
				addWekaInstance((Instance) inst.copy(), entry.getKey(), 1);
			}
		}
		
	}

	public void addInstance(Map<String, Object> variableAssignment, Object outputValue, float weight) {
		Instance instance = createInstance(variableAssignment);
		addWekaInstance(instance, outputValue, weight);
		if (balanceInstances)
		{
			synchronized(listInstances)
			{
				listInstances.get(outputValue).add(instance);
			}
		}
			
	}
}
