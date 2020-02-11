package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;

import com.fluxicon.slickerbox.components.RoundedPanel;
import com.fluxicon.slickerbox.factory.SlickerFactory;

class MyModel extends DefaultListModel
{
	private final List<String> activityGroups;

	public MyModel(List<String> activities)
	{
		this.activityGroups=activities;
	}
	
	public Object getElementAt(int index) 
	{
		return activityGroups.get(index);
	}

	public int getSize() {
		return activityGroups.size();
	}

	public void fireChanges(int index0, int index1) {
		super.fireContentsChanged(this, index0, index1);
	}
	
	public void fireAdditions(int index0, int index1) {
		super.fireIntervalAdded(this, index0, index1);
	}

	public void fireRemovals(int index0, int index1) {
		super.fireIntervalRemoved(this, index0, index1);
	}
	
	
}

public class ActivityPanel extends RoundedPanel implements ActionListener, ActivitiesGrouper
{
	
	private static final String ADD = "A";
	private static final String REMOVE = "R";
	private static final String RESET = "RESET";
	private static final String CASELEVEL="C";
	
	private JButton addButton=SlickerFactory.instance().createButton("Add Activity");
	private JButton removeButton=SlickerFactory.instance().createButton("Remove Activity");
	private JButton resetButton=SlickerFactory.instance().createButton("Reset to Include All Activities");	
	private JToggleButton caseLevelButton=new JToggleButton("Case Level",false);
	private final TreeSet<String> activitiesToAdd;
	private MyProMList aList;
	private LinkedList<String> selectedActivities;
	private VariablePanel varPanel;
	
	public ActivityPanel(Collection<String> collection,VariablePanel varPanel) {
		//SlickerDecorator.instance().decorate(caseLevelButton);
		this.activitiesToAdd=new TreeSet<String>();
		this.varPanel=varPanel;
		this.selectedActivities=new LinkedList<String>(collection);
		selectedActivities.remove(Predictor.CASE_ACTIVITY);
		Collections.sort(selectedActivities);
		aList=new MyProMList("Activities to Consider:",new MyModel(selectedActivities));
		aList.setToolTipText("All events referring to activities in this list are retained");
		this.setLayout(new BorderLayout());
		this.add(aList,BorderLayout.CENTER);
		JPanel buttonPnl=new JPanel();
		buttonPnl.add(addButton);
		addButton.addActionListener(this);
		addButton.setActionCommand(ADD);
		removeButton.addActionListener(this);
		removeButton.setActionCommand(REMOVE);
		buttonPnl.add(removeButton);
		resetButton.addActionListener(this);
		resetButton.setActionCommand(RESET);
		buttonPnl.add(resetButton);
		caseLevelButton.addActionListener(this);
		caseLevelButton.setActionCommand(CASELEVEL);
		buttonPnl.add(caseLevelButton);
		this.add(buttonPnl, BorderLayout.SOUTH);
	}

	public void actionPerformed(ActionEvent arg0) {
		String actionCommand=arg0.getActionCommand();
		int[] indices=aList.getSelectedIndices();
		if (actionCommand == ADD)
		{
			if (activitiesToAdd.size() == 0)
			{
				JOptionPane.showMessageDialog(null, "There is no activity to add");
				return;
			}
			String activity=(String) JOptionPane.showInputDialog(null,"Select Activity","Add Activity",JOptionPane.PLAIN_MESSAGE,null,activitiesToAdd.toArray(),null);
			if (activity==null)
				return;
			selectedActivities.add(activity);
			activitiesToAdd.remove(activity);
			Collections.sort(selectedActivities);
			((MyModel)aList.getModel()).fireAdditions(selectedActivities.size()-1, selectedActivities.size()-1);
			if (selectedActivities.size()>1)
				((MyModel)aList.getModel()).fireChanges(0, selectedActivities.size()-2);
		}
		else if (actionCommand == REMOVE)
		{
			if (indices.length==0)
				return;
			for(int index : indices)
			{
				String activity=selectedActivities.get(index);
				activitiesToAdd.add(activity);
				selectedActivities.remove(index);
				((MyModel)aList.getModel()).fireRemovals(index, index);
			}
		}
		else if (actionCommand == RESET)
		{
			int preSize=selectedActivities.size();
			selectedActivities.addAll(activitiesToAdd);
			Collections.sort(selectedActivities);
			((MyModel)aList.getModel()).fireAdditions(preSize, selectedActivities.size()-1);
			if (preSize>1)
				((MyModel)aList.getModel()).fireChanges(0, preSize-1);
			
		}
		else if (actionCommand == CASELEVEL)
		{
			aList.setEnabled(!caseLevelButton.isSelected());
			addButton.setEnabled(!caseLevelButton.isSelected());
			resetButton.setEnabled(!caseLevelButton.isSelected());
			removeButton.setEnabled(!caseLevelButton.isSelected());
			varPanel.setPanel(!caseLevelButton.isSelected());
			
		}
		aList.setSelectedIndices(indices);
	}

	public void updated() {
		
	}

	public String getPanelName() {
		return "Activities";
	}

	public JComponent getComponent() {
		return this;
	}

	public void setScalableComponent(ScalableComponent scalable) {
		
	}

	public void setParent(ScalableViewPanel viewPanel) {
		
	}

	public double getHeightInView() {
		return this.getPreferredSize().getHeight();
	}

	public double getWidthInView() {
		return 2*this.getPreferredSize().getWidth();
	}

	public void willChangeVisibility(boolean to) {
		
	}

	public Collection<String> getActivitiesToConsider() {
		if (caseLevelButton.isSelected())
		{
			ArrayList<String> retValue=new ArrayList<String>();
			retValue.add(Predictor.CASE_ACTIVITY);
			return retValue;
		}
		else
			return selectedActivities;
	}

}
