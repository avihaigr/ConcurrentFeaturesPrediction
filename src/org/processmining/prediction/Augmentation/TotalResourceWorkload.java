package org.processmining.prediction.Augmentation;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.prediction.Predictor;

public class TotalResourceWorkload extends Augmentation implements ActivityLevelAugmentation {
	private long timeWindow=0;
	private Map<Long,Integer> workload=new HashMap<Long,Integer>();
	private XLog lastLog=null;
	private long lastTimeWindow;
	private double days=0;
	private XLog log;
	private boolean startEventsPresent=false;
	private boolean workloadToBeComputed;

	public TotalResourceWorkload() {
		super("Total-Resource-Workload");
	}

	public void reset(XTrace trace) {

	}

	public Object returnAttribute(XEvent event) {
		if(workloadToBeComputed)
			computeTimestamps();
		Date time = XTimeExtension.instance().extractTimestamp(event);
		if (time==null)
			return null;
		long timestamp=	time.getTime();
		Integer workload=this.workload.get(timestamp);
		if (workload!=null)
			return workload;
		return null;
	}
	
	@Override
	public String[] getDefaultValueForParameter(int i)
	{
		String[] defaultValues=new String[] {String.valueOf(days)};
		return(defaultValues);
	}
	
	@Override
	public String[] getPossibleValuesForParameter(int i)
	{
		return(null);
	}	
	
	@Override
	public String[] getParameterNames()
	{
		if (!startEventsPresent)
		{
			return new String[] {"Time Window (in days)"};
		}
		else
			return new String[0];
	}
	
	@Override
	public boolean setParameter(int param, String value[])
	{
		try {
			double newdays=Double.parseDouble(value[0]);
			if (newdays!=days)
			{
				days=newdays;
				timeWindow=(long) (days*(24L*60L*60L*1000L));
				workloadToBeComputed=true;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	private void computeTimestamps()
	{
		workloadToBeComputed=false;
		PriorityQueue<Long> sortedTimestamps=new PriorityQueue<Long>();
		int i=0;
		for(XTrace trace : log)
		{
			for (XEvent event : trace)
				if(!Predictor.CASE_ACTIVITY.equals(XConceptExtension.instance().extractName(event)))
					if (XTimeExtension.instance().extractTimestamp(event)!=null)
						sortedTimestamps.add(XTimeExtension.instance().extractTimestamp(event).getTime());
		}
		Long[] array=sortedTimestamps.toArray(new Long[0]);
		int j,k;
		for(i=0;i<array.length;i++)
		{
			for(j=(i-1);j>=0;j--)
			{
				if (array[i]-array[j]>timeWindow)
					break;
			}
			for(k=(i+1);k < array.length ;k++)
			{
				if (array[k]-array[i]>timeWindow)
					break;
			}
			workload.put(array[i], k-j-1);
		}
	}
	
	public void setLog(XLog log) {
		this.log=log;
		workloadToBeComputed=true;
		startEventsPresent=false;
	}

	public long getTimeWindow() {
		return timeWindow;
	}

	public XLog getLastLog() {
		return lastLog;
	}

	public long getLastTimeWindow() {
		return lastTimeWindow;
	}

	public boolean isStartEventPresent() {
		// TODO Auto-generated method stub
		return false;
	}

}
