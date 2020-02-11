package org.processmining.prediction.Augmentation;

import java.util.HashMap;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class NumberExecutionCurrentActivity extends Augmentation {

	//private final String activityName;
	//private int numExecution=0;
	private Map<String, Integer> valuesMap;
	public NumberExecutionCurrentActivity()
	{
		super("NumExecution_Current_Activity");
	//	Map<String, Integer> valuesMap=new HashMap<String, Integer>();
		//this.activityName=activityName;
	}
	
	public void reset(XTrace trace) {
		//numExecution=0;
		valuesMap=new HashMap<String, Integer>();
	}

	public Object returnAttribute(XEvent event) {
		String eventName = XConceptExtension.instance().extractName(event);
		String transition=XLifecycleExtension.instance().extractTransition(event);
		
		if (transition==null || transition.equalsIgnoreCase("complete"))
			{
			
			if (valuesMap.containsKey(eventName))
			{
				int numExecution = valuesMap.get(eventName);
				numExecution++;
				valuesMap.put(eventName, numExecution);
				return(numExecution);
			}
			else{
				int numExecution = 1;
				valuesMap.put(eventName, numExecution);
				return(numExecution);
			}
		}
		else
			return 0;
	}

	public void setLog(XLog log) {
		// TODO Auto-generated method stub
		
	}

	public boolean isIncludInPrediction(){
		return false;
	}
}
