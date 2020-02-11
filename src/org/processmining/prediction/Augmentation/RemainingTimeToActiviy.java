package org.processmining.prediction.Augmentation;

import java.util.Date;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class RemainingTimeToActiviy extends Augmentation implements ActivityLevelAugmentation {

	private Date lastEventTimeStamp;
	private String[] allActivityNames;
	private String activityNamesToConsider[];
	private XTrace trace=null;
	private int currPos=0;

	public RemainingTimeToActiviy(String[] allActivityNames) {
		super("RemainingTimeToActiviy");
		this.allActivityNames=allActivityNames;
	}

	public void reset(XTrace trace) {
		this.trace=trace;
		currPos=0;
		//lastEventTimeStamp=XTimeExtension.instance().extractTimestamp(trace.get(trace.size()-1));
	}

	public Object returnAttribute(XEvent event) {
		Date timestamp=XTimeExtension.instance().extractTimestamp(event);
		long remainingTime = -1;
		if (currPos < trace.size())
		{
			int pos;
			for(pos = currPos+1;pos < trace.size();pos++)
			{
				XEvent nextEvent=trace.get(pos);
				String attrValue=XConceptExtension.instance().extractName(nextEvent);
				if (activityNamesToConsider.length==0 || isInIgnoringCase(attrValue,activityNamesToConsider)){
					Date lastEventTimeStamp=XTimeExtension.instance().extractTimestamp(nextEvent);
					if (lastEventTimeStamp!=null && timestamp!=null)
					{
						remainingTime=lastEventTimeStamp.getTime()-timestamp.getTime();
						break;
					} 				
				}
			}
			if (pos==trace.size())
				remainingTime=-1;
			currPos++;
		}
		return remainingTime;
		/*
		//Date timestamp=XTimeExtension.instance().extractTimestamp(event);
		if (lastEventTimeStamp!=null && timestamp!=null)
		{
			long remainingTime=lastEventTimeStamp.getTime()-timestamp.getTime();
			return remainingTime;
		} 
		return null;
		*/
	}

	public void setLog(XLog log) {
		
	}

	@Override
	public boolean isTimeInterval() {
		return true;
	}
	
	private boolean isInIgnoringCase(String value, String[] array) {
		for(String aValue : array)
		{
			if (value.equalsIgnoreCase(aValue))
				return true;
		}
		return false;
	}

	@Override
	public String[] getParameterNames() {
		return new String[] {"Remaining time until Activity:"};
	}
	
	@Override
	public boolean multipleValuesForParameter(int i)
	{
		return false;
	}
	
	@Override
	public String[] getDefaultValueForParameter(int i)
	{
		return(allActivityNames);
	}

	@Override
	public String[] getPossibleValuesForParameter(int i)
	{
		return(allActivityNames);
	}

	@Override
	public boolean setParameter(int i,String value[]) {
		if (value.length>0)
		{
			activityNamesToConsider=value.clone();
			return true;
		}
		else
			return false;
	}
}
