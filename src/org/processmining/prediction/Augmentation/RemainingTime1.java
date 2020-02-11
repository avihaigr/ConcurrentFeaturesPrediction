package org.processmining.prediction.Augmentation;

import java.util.Date;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class RemainingTime1 extends Augmentation implements ActivityLevelAugmentation {

	private Date lastEventTimeStamp;

	public RemainingTime1() {
		super("RemainingTime1");
	}

	public void reset(XTrace trace) {
		lastEventTimeStamp=XTimeExtension.instance().extractTimestamp(trace.get(trace.size()-1));
	}

	public Object returnAttribute(XEvent event) {
		Date timestamp=XTimeExtension.instance().extractTimestamp(event);
		if (lastEventTimeStamp!=null && timestamp!=null)
		{
			long remainingTime=lastEventTimeStamp.getTime()-timestamp.getTime();
			return remainingTime;
		} 
		return null;
	}

	public void setLog(XLog log) {
		
	}

	@Override
	public boolean isTimeInterval() {
		return true;
	}
}
