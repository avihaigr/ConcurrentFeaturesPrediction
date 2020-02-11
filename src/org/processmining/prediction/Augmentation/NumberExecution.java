package org.processmining.prediction.Augmentation;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;


public class NumberExecution extends Augmentation {

	private final String activityName;
	private int numExecution=0;

	public NumberExecution(String activityName)
	{
		super("NumExecution_"+activityName);
		this.activityName=activityName;
	}
	
	public void reset(XTrace trace) {
		numExecution=0;
	}

	public Object returnAttribute(XEvent event) {
		if (activityName.equals(XConceptExtension.instance().extractName(event)))
		{
			return(numExecution++);
		}
		else
			return(numExecution);
	}

	public void setLog(XLog log) {
		// TODO Auto-generated method stub
		
	}


}
