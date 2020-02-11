package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;
import org.processmining.models.FunctionEstimator.Type;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.prediction.Augmentation.ActivityDuration;
import org.processmining.prediction.Augmentation.ActivityName;
import org.processmining.prediction.Augmentation.AttributeValue;
import org.processmining.prediction.Augmentation.Augmentation;
import org.processmining.prediction.Augmentation.ConcurrentInstances;
import org.processmining.prediction.Augmentation.ConcurrentNumberExecution;
import org.processmining.prediction.Augmentation.DayOfWeek;
import org.processmining.prediction.Augmentation.ElapsedTime;
import org.processmining.prediction.Augmentation.EventNumber;
import org.processmining.prediction.Augmentation.Executor;
import org.processmining.prediction.Augmentation.Fitness;
import org.processmining.prediction.Augmentation.Group;
import org.processmining.prediction.Augmentation.Hour;
import org.processmining.prediction.Augmentation.NextActivity;
import org.processmining.prediction.Augmentation.NumberExecution;
import org.processmining.prediction.Augmentation.NumberExecutionCurrentActivity;
import org.processmining.prediction.Augmentation.PreAttributeValue;
import org.processmining.prediction.Augmentation.PreviousActivity;
import org.processmining.prediction.Augmentation.PreviousPath;
import org.processmining.prediction.Augmentation.RemainingTime;
import org.processmining.prediction.Augmentation.RemainingTimeToActiviy;
import org.processmining.prediction.Augmentation.ResourceWorkload;
import org.processmining.prediction.Augmentation.Role;
import org.processmining.prediction.Augmentation.Timestamp;
import org.processmining.prediction.Augmentation.TotalResourceWorkload;
import org.processmining.prediction.Augmentation.TraceAttributeValue;
import org.processmining.prediction.Augmentation.TraceStartTime;

import com.fluxicon.slickerbox.factory.SlickerFactory;

class MLTreeSelectionModel extends DefaultTreeSelectionModel {
	private static final long serialVersionUID = -4270031800448415780L;
	private VariablePanel variablePanel;

	public MLTreeSelectionModel(VariablePanel variablePanel) {
		this.variablePanel=variablePanel;
	}

	@Override
	public void addSelectionPath(TreePath path) {
		// Don't do overriding logic here because addSelectionPaths is ultimately called.
		super.addSelectionPath(path);
	}

	@Override
	public void addSelectionPaths(TreePath[] paths) {
		if(paths != null) {
			for(TreePath path : paths) {

				if (path.getLastPathComponent() instanceof Augmentation)
				{
					TreePath[] aux=new TreePath[]{path};

						if (!isPathSelected(path))
							super.addSelectionPaths(aux);
						else
							super.removeSelectionPaths(aux);
				}
				else
				{
					TreeNode node=(TreeNode) path.getLastPathComponent();
					ArrayList<TreePath> subPaths=new ArrayList<TreePath>();
					for(int i=0;i<node.getChildCount();i++)
					{
						TreeNode subNode=node.getChildAt(i);
						Object[] aux=Arrays.copyOf(path.getPath(), path.getPathCount()+1);
						aux[path.getPathCount()]=subNode;
						subPaths.add(new TreePath(aux));
					}
					addSelectionPaths(subPaths.toArray(new TreePath[0]));
				}

			}
		}
	}
}

class MyJTree extends JTree
{
	public MyJTree(DefaultMutableTreeNode root) {
		super(root);
		
	}

	@Override
	public void setSelectionPath(TreePath path) {
		
		addSelectionPath(path);
	}

	@Override
	public void setSelectionPaths(TreePath[] paths) {

		addSelectionPaths(paths);

		return;
	}

	@Override
	public void setSelectionRow(int row) {
		addSelectionRow(row);
	}
}


public class VariablePanel extends JPanel implements ViewInteractionPanel {

	private static final Color PURPLE = new Color(102, 0, 153);
	private JTree attributesTree;
	private JButton decUpdateBtn=SlickerFactory.instance().createButton("Create Augmented Event Log");
	private JButton fitnessBtn=SlickerFactory.instance().createButton("Open the fitness frame");
	private Collection<String> attributeSet;
	private Collection<String> resources;
	private Collection<String> resourceGroups;	
	private Collection<String> activitySet;
	private Collection<String> traceAttributeSet;
	private Map<String, ArrayList<String>> caseAttributeValues;
	//May be Removed
	private ArrayList<String> eventAttributes;

	
	private ConfigurationPanel configurationFrame;
	private JPanel northPanel=null;
	private JCheckBox fitnessCBox=SlickerFactory.instance().createCheckBox("Consider Fitness as Characteristic", false);
	private JCheckBox mapDBCBox=SlickerFactory.instance().createCheckBox("Swapping to Disk if unsufficient memory", false);

	private AlignmentFrame frame=null;
	private Augmentation fitness=null;
	private Map<String, Type> attributeType;
	private static Map<String, Color> colorMap=Collections.synchronizedMap(new HashMap<String,Color>());
	
	// 1_1_2019 add the caseAttributeValues
	public VariablePanel(Collection<String> attributeSet,Collection<String> traceAttributeSet, Collection<String> activitySet, Collection<String> resources,Collection<String> resourceGroups, Map<String, ArrayList<String>> caseAttributeValues,
			final ConfigurationPanel configurationFrame, 	ResultReplay resReplay, Map<String, Type> map) {
		this.attributeSet=attributeSet;
		this.resources = resources;
		this.resourceGroups = resourceGroups;
		this.traceAttributeSet = traceAttributeSet;
		this.activitySet=activitySet;
		this.configurationFrame=configurationFrame;
		this.attributeType=map;
		this.caseAttributeValues =caseAttributeValues;
		//May be Removed
		eventAttributes = new ArrayList<String>();  
				
		if (resReplay!=null)
		{
			frame=new AlignmentFrame(activitySet,resReplay);
			fitness=new Fitness(resReplay);
		}
		fitnessBtn.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(true);
			}
		});

		decUpdateBtn.addActionListener(new ActionListener() {
			
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent arg0) {
				try {
					configurationFrame.setAttributeAugmentation(getSelectedItems().toArray(new Augmentation[0]));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		setPanel(true);
	}
	
	public static Map<String, Color> getColors()
	{
		return Collections.unmodifiableMap(colorMap);
	}

	
	public void setPanel(boolean activityLevel)
	{
		attributesTree=generateTree(attributeSet,activitySet,activityLevel);
		if (northPanel!=null)
			northPanel.removeAll();
		else
		{
			this.setLayout(new BorderLayout());
			northPanel=new JPanel(new BorderLayout());
			this.add(northPanel,BorderLayout.NORTH);
		}
		northPanel.add(SlickerFactory.instance().createLabel("Characteristics to consider when building the decision/regression tree:"),
				BorderLayout.NORTH);
		northPanel.add(new JScrollPane(attributesTree),BorderLayout.CENTER);
		if (frame!=null)
		{
			JPanel centerPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
			centerPanel.add(fitnessCBox);
			centerPanel.add(fitnessBtn);
			this.add(centerPanel,BorderLayout.CENTER);
		}
		JPanel southPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
		southPanel.add(mapDBCBox);
		southPanel.add(decUpdateBtn);
		configurationFrame.setAttributeAugmentation(new Augmentation[0]);
		this.add(southPanel,BorderLayout.SOUTH);
		this.validate();
		this.repaint();
	}
	
	

	private JTree generateTree(Collection<String> attributeSet, Collection<String> activitySet, boolean activityLevel) {
		DefaultMutableTreeNode root=new DefaultMutableTreeNode("Available Features");
		//Green Color
		DefaultMutableTreeNode dataPerspective=new DefaultMutableTreeNode("Data Perspective");
		root.add(dataPerspective);
		DefaultMutableTreeNode attribValues=new DefaultMutableTreeNode("Attribute Post-Values");
		dataPerspective.add(attribValues);
		DefaultMutableTreeNode attribPreValues=new DefaultMutableTreeNode("Attribute Pre-Values");
		dataPerspective.add(attribPreValues);
		DefaultMutableTreeNode traceAttribValues=new DefaultMutableTreeNode("Case Attribute Values");
		dataPerspective.add(traceAttribValues);
		//DefaultMutableTreeNode sumAttributeValues=new DefaultMutableTreeNode("SumAttributeValues");
		//dataPerspective.add(sumAttributeValues);

		//Blue Color
		DefaultMutableTreeNode timePerspective=new DefaultMutableTreeNode("Time Perspective");
		root.add(timePerspective);
		//DefaultMutableTreeNode declarePerspective=new DefaultMutableTreeNode("Declare Violations");
		//root.add(declarePerspective);
		//Cyan Color
		DefaultMutableTreeNode resourcePerspective=new DefaultMutableTreeNode("Resource Perspective");
		if(activityLevel)
			root.add(resourcePerspective);
		//Purple Color
		DefaultMutableTreeNode controlPerspective=new DefaultMutableTreeNode("Control-flow Perspective");
		DefaultMutableTreeNode numExecActivities=new DefaultMutableTreeNode("Number Executions of Activities");
		root.add(controlPerspective);
		controlPerspective.add(numExecActivities);
		
		//DefaultMutableTreeNode environmentPerspective=new DefaultMutableTreeNode("Environment Perspective");
		DefaultMutableTreeNode environmentPerspective=new DefaultMutableTreeNode("Peer Instances Properties");
		DefaultMutableTreeNode concurrentActivities=new DefaultMutableTreeNode("Count Concurrent Activities");
		DefaultMutableTreeNode concurrentActivitiesNextActivity=new DefaultMutableTreeNode("Frequent Next Activity of Concurrent Activities");
		//DefaultMutableTreeNode deltaFromMeanTimeFromStart=new DefaultMutableTreeNode("Delta From The Mean Time From Start of Concurrent Activities");
		//DefaultMutableTreeNode deltaFromMedianTimeFromStart=new DefaultMutableTreeNode("Delta From The Median Time From Start of Concurrent Activities");
		//6.	Concurrent Instances Delays from Expected elapsed time - Mean
	//	Activities Delay from expected elapsed Time - Mean
		DefaultMutableTreeNode deltaFromMeanTimeFromStart=new DefaultMutableTreeNode("Activities Delay from expected elapsed Time - Mean");
		DefaultMutableTreeNode deltaFromMedianTimeFromStart=new DefaultMutableTreeNode("Activities Delay from expected elapsed Time - Median");
	
	//	DefaultMutableTreeNode meanDeltaFromTraceMeanTimeFromStart=new DefaultMutableTreeNode("Mean of the concurrent traces delta from the mean elapsed time");
	//	DefaultMutableTreeNode medianDeltaFromTraceMedianTimeFromStart=new DefaultMutableTreeNode("Median of the concurrent traces delta from the median elapsed time");
		
		DefaultMutableTreeNode concurrentActivitiesPrevActivity=new DefaultMutableTreeNode("Frequent Previous Activity of Concurrent Activities");
		DefaultMutableTreeNode concurrentResources=new DefaultMutableTreeNode("Resources Overload");//("Count Resources Activities");
		DefaultMutableTreeNode concurrentResourceCluster=new DefaultMutableTreeNode("Resource Groups Overload");//("Count Resource Group Activities");
		DefaultMutableTreeNode concurrentTimePerspective=new DefaultMutableTreeNode("Time Perspective");
		
		root.add(environmentPerspective);
		DefaultMutableTreeNode environmentDataPerspective=new DefaultMutableTreeNode("Data Perspective");
		DefaultMutableTreeNode environmentDesourcePerspective=new DefaultMutableTreeNode("Resource Perspective");
		DefaultMutableTreeNode environmentControlPerspective=new DefaultMutableTreeNode("Control-flow Perspective");

		environmentPerspective.add(environmentDataPerspective);
		environmentPerspective.add(environmentDesourcePerspective);
		environmentPerspective.add(environmentControlPerspective);
		
		
		//environmentPerspective.add(concurrentActivities);
		environmentControlPerspective.add(concurrentActivities);
	//	environmentControlPerspective.add(concurrentActivitiesNextActivity);
		//environmentPerspective.add(concurrentActivitiesPrevActivity);
		environmentControlPerspective.add(concurrentActivitiesPrevActivity);
		environmentDesourcePerspective.add(concurrentResources);
		
		//May be Removed
		environmentDesourcePerspective.add(concurrentResourceCluster);
		
		environmentPerspective.add(concurrentTimePerspective);
		concurrentTimePerspective.add(deltaFromMeanTimeFromStart);
		concurrentTimePerspective.add(deltaFromMedianTimeFromStart);
		
		String[] attributeArray=attributeSet.toArray(new String[0]);
		Arrays.sort(attributeArray);
		Augmentation aug;
		for(String attribute : attributeArray)
		{
			if (!attribute.startsWith("concept:") && !attribute.startsWith("lifecycle:") && !attribute.startsWith("time:"))
			{
				//May be Removed
				eventAttributes.add(attribute);
				
				
				aug=new AttributeValue(attribute);
				attribValues.add(aug);
				colorMap.put(aug.getAttributeName(), Color.GREEN);
				if (activityLevel)
				{
					aug=new PreAttributeValue(attribute);
					attribPreValues.add(aug);
					colorMap.put(aug.getAttributeName(), Color.GREEN);
				}
				//if (attributeType.get(attribute)==Type.CONTINUOS || attributeType.get(attribute)==Type.DISCRETE)
				//	sumAttributeValues.add(new SumAttributeValue(attribute));
			}
		}
		
		

		for(String attribute : traceAttributeSet)
		{
			aug =new TraceAttributeValue(attribute);
			traceAttribValues.add(aug);
			colorMap.put(aug.getAttributeName(), Color.GREEN);
		}
		
		aug=new ElapsedTime();
		timePerspective.add(aug);
		colorMap.put(aug.getAttributeName(), Color.BLUE);
		
		if (activityLevel)
		{
			aug=new ConcurrentInstances(ConcurrentInstances.ConcurrentType.Count_Concurrent_Instances,traceAttributeSet,caseAttributeValues);
			environmentDataPerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);
			
			for(String caseAttr:caseAttributeValues.keySet()){
				DefaultMutableTreeNode concurrentCaseAttr=new DefaultMutableTreeNode(caseAttr);
				environmentDataPerspective.add(concurrentCaseAttr);
				ArrayList<String> attrValues =   caseAttributeValues.get(caseAttr);
				for(String attrVal:attrValues){
					String name = caseAttr+"_"+attrVal;
					aug=new ConcurrentInstances(name, ConcurrentInstances.ConcurrentType.Case_Attributes_Data,traceAttributeSet,caseAttr,attrVal,caseAttributeValues);
					concurrentCaseAttr.add(aug);
				}
			//environmentDataPerspective
			//caseAttributeValues
			}
			
			
			//meanDeltaFromTraceMeanTimeFromStart
			
			aug=new ConcurrentNumberExecution(null,activitySet, traceAttributeSet,resources,eventAttributes,ConcurrentNumberExecution.ConcurrentType.Concurrent_Instances_Mean_Delays_From_Expected_Elapsed_Time);
			concurrentTimePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);
			
			aug=new ConcurrentNumberExecution(null,activitySet, traceAttributeSet,resources,eventAttributes,ConcurrentNumberExecution.ConcurrentType.Concurrent_Instances_Median_Delays_From_Expected_Elapsed_Time);
			concurrentTimePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);
			
			aug=new ConcurrentInstances(ConcurrentInstances.ConcurrentType.Anchor_Event_Mean_Elapsed_Time_Of_Concurrent_instances,traceAttributeSet,caseAttributeValues);
			concurrentTimePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);
			
			aug=new ConcurrentInstances(ConcurrentInstances.ConcurrentType.Anchor_Event_Median_Elapsed_Time_Of_Concurrent_instances,traceAttributeSet,caseAttributeValues);
			concurrentTimePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);
			
			aug=new ConcurrentInstances(ConcurrentInstances.ConcurrentType.Mean_Remained_Time_Concurrent_Instances,traceAttributeSet,caseAttributeValues);
		//	concurrentTimePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);
			
			aug=new ConcurrentInstances(ConcurrentInstances.ConcurrentType.Median_Remained_Time_Concurrent_Instances,traceAttributeSet,caseAttributeValues);
		//	concurrentTimePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);
			
			aug=new ConcurrentInstances(ConcurrentInstances.ConcurrentType.Mean_Duration_Time_Concurrent_Instances,traceAttributeSet,caseAttributeValues);
		//	concurrentTimePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);
			
			aug=new ConcurrentInstances(ConcurrentInstances.ConcurrentType.Median_Duration_Time_Concurrent_Instances,traceAttributeSet,caseAttributeValues);
		//	concurrentTimePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);
			
		}
		
		if (activityLevel)
		{
			aug=new RemainingTime();
			timePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);

			aug=new RemainingTimeToActiviy(activitySet.toArray(new String[0]));
			timePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);
		
		}
		if (activityLevel)
		{
			aug=new ActivityDuration();
			timePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.BLUE);
		}
		aug=new Timestamp();
		timePerspective.add(aug);
		colorMap.put(aug.getAttributeName(), Color.BLUE);
		/*
		aug=new TraceEndTime();
		timePerspective.add(aug);
		colorMap.put(aug.getAttributeName(), Color.BLUE);
		
		aug=new TraceStartTime();
		timePerspective.add(aug);
		colorMap.put(aug.getAttributeName(), Color.BLUE);
		*/
		
		aug=new DayOfWeek();
		timePerspective.add(aug);
		colorMap.put(aug.getAttributeName(), Color.BLUE);
		
		aug=new Hour();
		timePerspective.add(aug);
		colorMap.put(aug.getAttributeName(), Color.BLUE);
		
		if (activityLevel)
		{
			aug=new Executor();
			resourcePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.CYAN);

		}
		if (activityLevel)
		{
			aug=new Role();
			resourcePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.CYAN);

		}
		if (activityLevel)
		{
			aug=new Group();
			resourcePerspective.add(aug);

		}
		if (activityLevel)
		{
			TotalResourceWorkload trw=new TotalResourceWorkload();
			resourcePerspective.add(trw);
			colorMap.put(trw.getAttributeName(), Color.CYAN);
			aug=new ResourceWorkload(trw);
			resourcePerspective.add(aug);
			colorMap.put(aug.getAttributeName(), Color.CYAN);

		}
		
		for (String activity : activitySet)
		{
			if (!activity.equals(Predictor.CASE_ACTIVITY))
			{
				aug=new NumberExecution(activity);
				numExecActivities.add(aug);
				colorMap.put(aug.getAttributeName(), PURPLE);
			}
			
		}
		if (activityLevel)
		{
			aug=new NextActivity(activitySet.toArray(new String[0]));
			colorMap.put(aug.getAttributeName(), PURPLE);
			controlPerspective.add(aug);
			aug=new PreviousActivity();
			colorMap.put(aug.getAttributeName(), PURPLE);
			controlPerspective.add(aug);
				/*		
			aug=new NumberExecutionCurrentActivity();
			colorMap.put(aug.getAttributeName(), PURPLE);
			controlPerspective.add(aug);
			*/
			aug=new PreviousPath();
			colorMap.put(aug.getAttributeName(), PURPLE);
			controlPerspective.add(aug);
		}
		if (activityLevel)
		{
			aug=new ActivityName();
			controlPerspective.add(aug);
			colorMap.put(aug.getAttributeName(), PURPLE);

		}
		aug=new EventNumber(!activityLevel);
		controlPerspective.add(aug);
		colorMap.put(aug.getAttributeName(), PURPLE);
		
		for (String activity : activitySet)
		{
			if (!activity.equals(Predictor.CASE_ACTIVITY))
			{
				aug=new ConcurrentNumberExecution(activity,activitySet, traceAttributeSet,resources,resourceGroups,ConcurrentNumberExecution.ConcurrentType.Number_Of_Executions);
				//aug=new ConcurrentNumberExecution(activity,activitySet, traceAttributeSet,resources,ConcurrentNumberExecution.ConcurrentType.Number_Of_Executions);				
				concurrentActivities.add(aug);
				colorMap.put(aug.getAttributeName(), PURPLE);
			}
			
		}
		
		for (String activity : activitySet)
		{
			if (!activity.equals(Predictor.CASE_ACTIVITY))
			{
				aug=new ConcurrentNumberExecution(activity,activitySet, traceAttributeSet,resources,resourceGroups,ConcurrentNumberExecution.ConcurrentType.Activity_Mean_Of_Delta_From_Mean_Elapsed_Time);
				//aug=new ConcurrentNumberExecution(activity,activitySet,traceAttributeSet,resources,ConcurrentNumberExecution.ConcurrentType.Next_Activity);
				deltaFromMeanTimeFromStart.add(aug);
				colorMap.put(aug.getAttributeName(), PURPLE);
			}			
		}
		
		for (String activity : activitySet)
		{
			if (!activity.equals(Predictor.CASE_ACTIVITY))
			{
				aug=new ConcurrentNumberExecution(activity,activitySet, traceAttributeSet,resources,resourceGroups,ConcurrentNumberExecution.ConcurrentType.Activity_Median_Of_Delta_From_Mean_Elapsed_Time);
				//aug=new ConcurrentNumberExecution(activity,activitySet,traceAttributeSet,resources,ConcurrentNumberExecution.ConcurrentType.Next_Activity);
				deltaFromMedianTimeFromStart.add(aug);
				colorMap.put(aug.getAttributeName(), PURPLE);
			}			
		}
		
		for (String activity : activitySet)
		{
			if (!activity.equals(Predictor.CASE_ACTIVITY))
			{
				aug=new ConcurrentNumberExecution(activity,activitySet, traceAttributeSet,resources,resourceGroups,ConcurrentNumberExecution.ConcurrentType.Next_Activity);
				//aug=new ConcurrentNumberExecution(activity,activitySet,traceAttributeSet,resources,ConcurrentNumberExecution.ConcurrentType.Next_Activity);
				concurrentActivitiesNextActivity.add(aug);
				colorMap.put(aug.getAttributeName(), PURPLE);
			}			
		}
		
		for (String activity : activitySet)
		{
			if (!activity.equals(Predictor.CASE_ACTIVITY))
			{
				aug=new ConcurrentNumberExecution(activity,activitySet, traceAttributeSet,resources,resourceGroups,ConcurrentNumberExecution.ConcurrentType.Prev_Activity);
				//aug=new ConcurrentNumberExecution(activity,activitySet,traceAttributeSet,resources,ConcurrentNumberExecution.ConcurrentType.Prev_Activity);
				concurrentActivitiesPrevActivity.add(aug);
				colorMap.put(aug.getAttributeName(), PURPLE);
			}			
		}
			
		for (String resource : resources)
		{
			aug=new ConcurrentNumberExecution(resource,activitySet, traceAttributeSet,resources,resourceGroups,ConcurrentNumberExecution.ConcurrentType.Resource_Overload);
			concurrentResources.add(aug);
			colorMap.put(aug.getAttributeName(), PURPLE);		
		}
		
		
		for (String resourceGroup : resourceGroups)
		{
			aug=new ConcurrentNumberExecution(resourceGroup,activitySet, traceAttributeSet,resources,resourceGroups,ConcurrentNumberExecution.ConcurrentType.Resource_Group_Overload);
			concurrentResourceCluster.add(aug);
			colorMap.put(aug.getAttributeName(), PURPLE);		
		}
		/*
		for (String resourceGroup : eventAttributes)
		{
			DefaultMutableTreeNode clusterNode =new DefaultMutableTreeNode(resourceGroup);
			concurrentResourceCluster.add(clusterNode);
			
			int numOfClusters = 0;
			
			try
			{numOfClusters = Integer.parseInt(resourceGroup.replaceAll("\\D+",""));}
			catch(Exception e){}
			
			for (Integer i=0;i<numOfClusters;i++){
				aug=new ConcurrentNumberExecution(resourceGroup + " " + i.toString(),activitySet, traceAttributeSet,resources,eventAttributes,ConcurrentNumberExecution.ConcurrentType.Resource_Group_Overload);
				clusterNode.add(aug);
				colorMap.put(aug.getAttributeName(), PURPLE);
			}
		}
		*/
		JTree retValue=new MyJTree(root);
		retValue.setSelectionModel(new MLTreeSelectionModel(this));

		return retValue;
	}

	public JComponent getComponent() {
		return this;
	}

	public double getHeightInView() {
		return this.getPreferredSize().getHeight();
	}

	public String getPanelName() {
		return "Attributes";
	}

	public double getWidthInView() {
		return this.getPreferredSize().getWidth();
	}

	public void setParent(ScalableViewPanel viewPanel) {

	}

	public void setScalableComponent(ScalableComponent scalable) {

	}

	public void willChangeVisibility(boolean to) {

	}

	public void updated() {

	}
	

	@SuppressWarnings("unchecked")
	public Collection<Augmentation> getSelectedItems() {
		//Set<Augmentation> selectedAugmentations=new HashSet<Augmentation>();
		Set<Augmentation> selectedAugmentations=new LinkedHashSet<Augmentation>();
		TreePath paths[]=attributesTree.getSelectionPaths();
		if (paths!=null)
			for (TreePath path : paths)
				if (path.getLastPathComponent() instanceof Augmentation)
					selectedAugmentations.add((Augmentation) path.getLastPathComponent());
		if (frame!=null)
		{
			selectedAugmentations.addAll(frame.createAugmentation());
			if (fitnessCBox.isSelected())
			{
				selectedAugmentations.add(fitness);
			}
		}
		//add the help fields -> that are not included in the prediction process
		selectedAugmentations.add(new TraceStartTime());
		selectedAugmentations.add(new NumberExecutionCurrentActivity());


		return selectedAugmentations;
	}

	public boolean isMapDBSelected() {
		return mapDBCBox.isSelected();
	}

}
