package org.processmining.prediction;

import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;

import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.components.RoundedPanel;
import com.fluxicon.slickerbox.factory.SlickerFactory;

public class LogClusterPanel extends RoundedPanel implements ViewInteractionPanel {

	private JCheckBox onlyCorrectClassified=SlickerFactory.instance().createCheckBox("Only correctly classified", true);
	private NiceIntegerSlider threshold=SlickerFactory.instance().
			createNiceIntegerSlider("Absolute Error Threshold to Be Considered Correctly Classified", 0, 100, 10, Orientation.HORIZONTAL);

	public LogClusterPanel(PluginContext proMContext, Predictor predictor) {
		setLayout(new GridLayout(3,1));
		JButton button=SlickerFactory.instance().createButton("Generate Log Clusters");
		button.addActionListener(new LogClusterListener(onlyCorrectClassified,threshold,proMContext,predictor));
		this.add(onlyCorrectClassified);
		this.add(threshold);
		this.add(button);
		threshold.setToolTipText("This is only relevant when the output is a regression tree and the dependent characteristic is numeric");
	}

	public void updated() {

	}

	public String getPanelName() {
		return "Log Clusters";
	}

	public JComponent getComponent() {
		return this;
	}

	public void setScalableComponent(ScalableComponent scalable) {
		// TODO Auto-generated method stub

	}

	public void setParent(ScalableViewPanel viewPanel) {
		// TODO Auto-generated method stub

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
