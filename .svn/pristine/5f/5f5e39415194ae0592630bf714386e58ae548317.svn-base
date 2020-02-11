package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;

import org.processmining.models.FunctionEstimator.RepTreeEstimator;
import org.processmining.models.FunctionEstimator.Type;
import org.processmining.prediction.NewTreeVisualizer.MyTreeVisualizer;
import org.processmining.prediction.NewTreeVisualizer.PlaceNode2;
import org.processmining.prediction.newPrefuseTreeVis.NewPrefuseTreeVis;
import org.processmining.prediction.newPrefuseTreeVis.TreePanel;
import org.processmining.prediction.newPrefuseTreeVis.VisConfigurables;

import weka.classifiers.trees.REPTree;




public class RepTreePrediction extends RepTreeEstimator {
	
	private Set<String> timeIntervalAttributes=new HashSet<String>();
	private float maxValue=-1;
	
	public RepTreePrediction(Map<String, Type> map, Map<String, Set<String>> literalValues, String classAttributeName,
			String name, int capacity, Set<String> timeIntervalAttributes) {
		super(map, literalValues, classAttributeName, name, capacity);
		this.timeIntervalAttributes=timeIntervalAttributes;
	}
	
	

	@Override
	public synchronized void addInstance(Map<String,Object> variableAssignment,Object outputValue, float weight)
	{
		super.addInstance(variableAssignment, outputValue, weight);
		if (outputValue instanceof Number)
		{
			if (((Number) outputValue).floatValue()>maxValue)
				maxValue=((Number) outputValue).floatValue();
		}
	}

	public JPanel getVisualization() {
		if (tree==null)
			return null; 
		else
		{		
			HashMap<String, PredictionType> predictionTypes = new HashMap<String,PredictionType>();
			for (Entry<String, Type> entry : variableType.entrySet())
			{
				PredictionType p = null;
				switch(entry.getValue())
				{
					case BOOLEAN :
						p=PredictionType.BOOLEAN;
						break;
					case CONTINUOS :
						p=PredictionType.CONTINUOS;
						break;
					case DISCRETE :
						if (timeIntervalAttributes.contains(entry.getKey()))
							p=PredictionType.TIMEINTERVAL;
						else
							p=PredictionType.DISCRETE;
						break;
					case LITERAL :
						p=PredictionType.LITERAL;
						break;
					case TIMESTAMP :
						p=PredictionType.TIMESTAMP;							
						break;
					default :
						break;
				}
				predictionTypes.put(entry.getKey(), p);
			}
			try { 
				name=instances.classAttribute().name();

				return new MyTreeVisualizer(null,cleanTreeRepresentation(((REPTree) tree).graph()),new PlaceNode2(),VariablePanel.getColors(),predictionTypes,':',
						maxValue,predictionTypes.get(name));
			} catch (Exception e) {
				e.printStackTrace();
				JPanel panel=new JPanel(new BorderLayout());
				panel.add(new JLabel("Impossible to create the tree: "+e.getMessage()), BorderLayout.CENTER);
				return panel;
			}
		}
	}


	public JComponent getPrefuseTreeVisualization() {
		System.out.println("Drawing regression tree");
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
				String dotty = cleanTreeRepresentation(((REPTree) tree).graph());
				Map<String, Color> map = VariablePanel.getColors();
				String name= instances.classAttribute().name();
				NewPrefuseTreeVis ptv = new NewPrefuseTreeVis();
				VisConfigurables config = new VisConfigurables();
				PredictionType classType = predictionTypes.get(name);
				TreePanel panel = ptv.display(dotty, name, map, predictionTypes, classType,config);
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
									    "Double left-clicing on a node: Expand from that node to the leaves in that (sub)tree\n" ,
									    "Right-click center visualisation and resize to fit screen.\n",
									    "Ctrl leftclick expands multiple children",
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
			    button.setBounds(10, 10, 40, 40);
			    newPanel.add(button,0,0);			
				return newPanel;
			} catch (Exception e) {
				System.out.println(e);
				return null;
			}
		}
	}
	
	protected String cleanTreeRepresentation(String dotFile)
	{
		String[] split = dotFile.split("\\n");
		String[] cleanSplit=new String[split.length];
		int usedUntil=-1;
		for(String row : split)
		{
			if (!row.contains("(0/0)"))
				cleanSplit[++usedUntil]=row;
			else
				usedUntil--;
		}
		StringBuffer retValue=new StringBuffer();
		for(int i=0;i<=usedUntil;i++)
		{
			retValue.append(cleanSplit[i]);
			retValue.append('\n');
		}
		return retValue.toString();
	}
}
