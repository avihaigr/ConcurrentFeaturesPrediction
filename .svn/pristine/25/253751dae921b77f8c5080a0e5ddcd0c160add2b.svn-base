package org.processmining.prediction.Augmentation;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.prediction.Predictor;

public class PreviousActivity extends Augmentation implements ActivityLevelAugmentation {

	private XTrace trace=null;
	private int currPos=0;
	
	public PreviousActivity() {
		super("PreviousActivity");
	}

	public void reset(XTrace trace) {
		this.trace=trace;
		currPos=0;
	}

	public Object returnAttribute(XEvent event) {
		String attrValue;
		if (currPos == 0 )
		{
			attrValue="NOTHING";
		}
		else
		{
			XEvent previousEvent=trace.get(currPos-1);		
			attrValue=XConceptExtension.instance().extractName(previousEvent);
			if (attrValue==null)
				attrValue="";
			if (attrValue.equals(Predictor.CASE_ACTIVITY))
				attrValue="NOTHING";
		}
		currPos++;
		return attrValue;
	}

	public void setLog(XLog log) {
		
	}

}
