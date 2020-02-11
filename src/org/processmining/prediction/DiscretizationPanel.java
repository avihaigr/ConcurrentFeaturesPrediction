package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.table.DefaultTableModel;

import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;
import org.processmining.framework.util.ui.widgets.ProMTable;
import org.processmining.framework.util.ui.widgets.ProMTextField;

import com.fluxicon.slickerbox.components.RoundedPanel;
import com.fluxicon.slickerbox.factory.SlickerFactory;

class MyTableModel extends DefaultTableModel
{

	private DiscretizationInterval[] intervals;

	public MyTableModel(DiscretizationInterval[] intervals) {
		this.intervals=intervals;
	}
	
	public int getRowCount() {
		if (intervals!=null)
			return intervals.length;
		else
			return 0;
	}
	
	public int getColumnCount() {
		return 3;
	}
	
	public Object getValueAt(int row, int column) {
		switch(column)
		{
			case 0: 
				return intervals[row].getFirst();
			case 1:
				return intervals[row].getSecond();
			case 2:
				return intervals[row].getName();
			default:
				assert(false);
				return null;
		}
	}
	
	public void setValueAt(Object value, int row, int column)
	{
		intervals[row].setName((String) value);
	}
	
	public boolean isCellEditable(int row, int column)
	{
		return column==2;
	}
	
	public String getColumnName(int column) {
		switch(column)
		{
			case 0: 
				return "From";
			case 1:
				return "To";
			case 2:
				return "Name";
			default:
				assert(false);
				return null;
		}
	}
	
	
}

public class DiscretizationPanel extends JPanel implements ViewInteractionPanel {


	public DiscretizationPanel(final DecisionTreePanel decisionTreeFrame,DiscretizationInterval[] intervals, int numIntervals, DiscrMethod method) {
		ProMTable table=new ProMTable(new MyTableModel(intervals));
		final ProMTextField field = new ProMTextField(String.valueOf(numIntervals));
		field.setMaximumSize(new Dimension(field.getMaximumSize().width/2,field.getMaximumSize().height));
		field.setPreferredSize(new Dimension(field.getPreferredSize().width/2,field.getPreferredSize().height));
		final JRadioButton equalWidthBtn=SlickerFactory.instance().createRadioButton("Equal Width");
		final JRadioButton equalFreqBtn=SlickerFactory.instance().createRadioButton("Equal Frequency");
		if (method==DiscrMethod.EQUAL_WIDTH)
			equalWidthBtn.setSelected(true);
		else
			equalFreqBtn.setSelected(true);
		ButtonGroup bgr=new ButtonGroup();
		bgr.add(equalWidthBtn);
		bgr.add(equalFreqBtn);
		field.setInputVerifier(new InputVerifier() {
			
			public boolean verify(JComponent input) {
				try
				{
					Integer.parseInt(field.getText());
				}
				catch(NumberFormatException err)
				{
					return false;
				}
				return true;
			}
		});
		this.setLayout(new BorderLayout());
		this.add(table,BorderLayout.CENTER);
		JButton button=SlickerFactory.instance().createButton("Update Decision Tree");
		button.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				try {
					decisionTreeFrame.setEnabled(false);
					
					TaskForProgressBar task1=new TaskForProgressBar(null,"Learning Decision Tree","",0,100) {

						protected Void doInBackground() throws Exception {
							decisionTreeFrame.createPanel(this);
							return null;
						}

						protected void done() {
							decisionTreeFrame.setEnabled(true);
						}

					};

					task1.execute();					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		});
		
		final JButton resampleBtn=SlickerFactory.instance().createButton("Change Number Intervals");
		resampleBtn.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				int numIntervals=Integer.parseInt(field.getText());
				decisionTreeFrame.resampleOutputAttribute(numIntervals,(equalWidthBtn.isSelected() ? DiscrMethod.EQUAL_WIDTH : DiscrMethod.EQUAL_FREQUENCY));
				
			}
		});
		
		JPanel southPnl=new JPanel();
		southPnl.add(button);
		this.add(southPnl, BorderLayout.SOUTH);
		JPanel northPnl=new JPanel();
		northPnl.add(field);
		RoundedPanel radioButtonPnl = new RoundedPanel();
		radioButtonPnl.setLayout(new GridLayout(0,1,3,3));
		radioButtonPnl.setBorder(BorderFactory.createTitledBorder("Discretization Method"));
		radioButtonPnl.add(equalWidthBtn);
		radioButtonPnl.add(equalFreqBtn);
		northPnl.add(radioButtonPnl);
		northPnl.add(resampleBtn);	
		this.add(northPnl, BorderLayout.NORTH);
	}

	public void updated() {
	}

	public String getPanelName() {
		return "Discretization Attributes";
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
		return this.getPreferredSize().getWidth();
	}

	public void willChangeVisibility(boolean to) {
		
	}

}
