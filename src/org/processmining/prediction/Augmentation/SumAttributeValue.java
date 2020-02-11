package org.processmining.prediction.Augmentation;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class SumAttributeValue extends Augmentation{

	private Number value;
	private String originalName;

	public SumAttributeValue(String attributeName) {
		super(attributeName);
		originalName=attributeName;
	}

	public void reset(XTrace trace) {
		value=0;
	}

	public Object returnAttribute( XEvent event) {
		XAttribute attr=event.getAttributes().get(originalName);
		Object aValue=getAttributeValues(attr);
		if (attr!=null && aValue instanceof Number)
		{
			value=value.doubleValue()+((Number)aValue).doubleValue();
		}
		return value;
	}

	public void setLog(XLog log) {
		
	}
		
}
