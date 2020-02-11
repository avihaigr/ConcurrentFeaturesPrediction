package org.processmining.prediction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import org.deckfour.uitopia.api.model.ResourceType;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIContext;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.model.ProMPOResource;
import org.processmining.contexts.uitopia.model.ProMTask;
import org.processmining.framework.plugin.GlobalContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.providedobjects.ProvidedObjectID;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.utils.ProvidedObjectHelper;

import com.fluxicon.slickerbox.components.NiceIntegerSlider;

public class LogClusterListener implements ActionListener {

	private PluginContext proMContext;
	private Predictor predictor;
	private JCheckBox onlyCorrectlyClassified;
	private NiceIntegerSlider thresholdComponent;


	public LogClusterListener(JCheckBox onlyCorrectlyClassified, NiceIntegerSlider threshold, PluginContext proMContext,
			Predictor predictor) {
		this.proMContext=proMContext;
		this.predictor=predictor;
		this.onlyCorrectlyClassified=onlyCorrectlyClassified;
		this.thresholdComponent=threshold;
	}

	public void actionPerformed(ActionEvent arg0) {
		try {
			Pair<String[], XLog[]> retValue = 
					predictor.clusterLog(onlyCorrectlyClassified.isSelected(),thresholdComponent.getValue());
			int generatedLog=0;
			int wronglyClassified=0;
			for(int i=0;i<retValue.getFirst().length;i++)
			{
				if (retValue.getSecond()[i].size()>0)
				{
					String val=retValue.getFirst()[i];
					if (val==null)
					{
						val="Wrongly-classified Traces";
						wronglyClassified=retValue.getSecond()[i].size();
					}
					XConceptExtension.instance().assignName(retValue.getSecond()[i], val);
					publish(proMContext, "Cluster for "+val, retValue.getSecond()[i], XLog.class, true);
					generatedLog++;
				}
			}
			JOptionPane.showMessageDialog(null, 
					generatedLog+" event log(s) have been generated and added in the favorite tab.\n" +
							wronglyClassified+" instance(s) have been wrongly classified");
		} catch(NullPointerException e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Please, generate a decision/regression tree, first!");			
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private static <T> void publish(final PluginContext context, final String name, final T object,
			final Class<? super T> clazz, final boolean favorite) {
		final ProvidedObjectID id = context.getProvidedObjectManager().createProvidedObject(name, object, clazz,
				context);
		if (context instanceof UIPluginContext) {
			final GlobalContext gcontext = ((UIPluginContext) context).getGlobalContext();
			if (gcontext instanceof UIContext) {
				final UIContext uicontext = (UIContext) gcontext;
				final ResourceType resType = uicontext.getResourceManager().getResourceTypeFor(clazz);
				if (resType != null) {
					ProMTask task = null;
					try {
						final Field taskField = context.getClass().getDeclaredField("task");
						taskField.setAccessible(true);
						task = (ProMTask) taskField.get(context);
					} catch (final Exception _) {
						// Guess it wasn't meant to be, then...
					}
					final List<Collection<ProMPOResource>> lst = Collections.emptyList();
					ProMPOResource res = new ProMPOResource(uicontext, task == null ? null : task.getAction(), resType,
							id, lst);
					res = uicontext.getResourceManager().addResource(id, res);
				}
			}
		}
		if (favorite) {
			ProvidedObjectHelper.setFavorite(context, object);
		}
	}

}
