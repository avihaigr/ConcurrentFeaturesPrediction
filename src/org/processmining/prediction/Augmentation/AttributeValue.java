package org.processmining.prediction.Augmentation;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class AttributeValue extends Augmentation{

	private Object value;
	private String originalName;

	public AttributeValue(String attributeName) {
		super(attributeName);
		originalName=attributeName;
	}

	public void reset(XTrace trace) {
		value=null;
	}

	public Object returnAttribute( XEvent event) {
		XAttribute attr=event.getAttributes().get(originalName);
		if (attr!=null)
			value=getAttributeValues(attr);
		return value;
	}

	public void setLog(XLog log) {
		// TODO Auto-generated method stub
		
	}
		
}
