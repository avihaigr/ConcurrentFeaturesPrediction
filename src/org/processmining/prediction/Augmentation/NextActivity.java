package org.processmining.prediction.Augmentation;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class NextActivity extends Augmentation implements ActivityLevelAugmentation {

	private XTrace trace=null;
	private int currPos=0;
	private String defaultValue="";
	private String activityNamesToConsider[];
	private String[] allActivityNames;
	
	public NextActivity(String[] allActivityNames) {
		super("NextActivity");
		this.allActivityNames=allActivityNames;
	}

	public void reset(XTrace trace) {
		this.trace=trace;
		currPos=0;
	}

	public Object returnAttribute(XEvent event) {
		String attrValue="NOTHING";
		if (currPos < trace.size())
		{
			int pos;
			for(pos = currPos+1;pos < trace.size();pos++)
			{
				XEvent nextEvent=trace.get(pos);
				attrValue=XConceptExtension.instance().extractName(nextEvent);
				if (activityNamesToConsider.length==0 || isInIgnoringCase(attrValue,activityNamesToConsider))
					break;
			}
			if (pos==trace.size())
				attrValue="NOTHING";
			currPos++;
		}
		return attrValue;
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
		return new String[] {"Activities to consider as next"};
	}
	
	@Override
	public boolean multipleValuesForParameter(int i)
	{
		return true;
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

	public void setLog(XLog log) {
		
	}
}
