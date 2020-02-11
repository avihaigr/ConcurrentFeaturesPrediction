package org.processmining.prediction.Augmentation;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;


public class TraceAttributeValue extends Augmentation implements ActivityLevelAugmentation {
	private Object value;
	private String attribute=null;

	public TraceAttributeValue(String attribute)
	{
		super(attribute);
		this.attribute=attribute;
	}
	
	public void reset(XTrace trace) {
		XAttribute attributeObj = trace.getAttributes().get(attribute);
		if (attributeObj==null)
			value=null;
		else
			value=getAttributeValues(attributeObj);
	}

	public Object returnAttribute(XEvent event) {
		return value;
	}

	public void setLog(XLog log) {
		
	}

}
