package org.processmining.prediction.Augmentation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.prediction.Predictor;



public class ConcurrentInstances extends Augmentation implements ActivityLevelAugmentation {
	
//	private Map<Long,Integer> workload=new HashMap<Long,Integer>();
	
	//this object will contains for each activity timeStamp, the number of concurrent active traces
	private static Map<Date, Integer> occurrenceMap = new HashMap<Date, Integer>();
	//private static Map<Date, List<Long>> occurreneDurationTimeMap = new HashMap<Date, List<Long>>();
	//private static Map<Date, List<Long>> occurreneElapsedTimeMap = new HashMap<Date, List<Long>>();
	//private static Map<Date, List<Long>> occurreneRemainTimeMap = new HashMap<Date, List<Long>>();
	
	private static Map<String, List<Long>> occurreneDurationTimeMap1 = new HashMap<String, List<Long>>();
	private static Map<String, List<Long>> occurreneElapsedTimeMap1 = new HashMap<String, List<Long>>();
	private static Map<String, List<Long>> occurreneRemainTimeMap1 = new HashMap<String, List<Long>>();
	private static Map<String, Long[]> concurrentOccurreneData = new HashMap<String, Long[]>();
	
	//1_1_2019
	private Map<String, ArrayList<String>> caseAttributeValues;
	//structure: case attributes - attribute values - time stamp key - concurrent count
	private static Map<String, Map<String,Map<String,Integer>>> caseAttributeValuesConcurrentData;
	
	private Collection<String> activitiesToConsider;
	private boolean isSetactivitiesToConsiderChanged;
	
	public static enum ConcurrentType { Count_Concurrent_Instances(0), Mean_Remained_Time_Concurrent_Instances(1), Median_Remained_Time_Concurrent_Instances(2),
										Mean_Duration_Time_Concurrent_Instances(3), Median_Duration_Time_Concurrent_Instances(4),
										Anchor_Event_Mean_Elapsed_Time_Of_Concurrent_instances(5), Anchor_Event_Median_Elapsed_Time_Of_Concurrent_instances(6),
										Case_Attributes_Data(7);
										int id;
								        private ConcurrentType(int i){id = i;}
								
								        public int GetID(){return id;}
        }
//	Anchor_Event_Median_Elapsed_Time_Of_Concurrent_instances
//	Anchor_Event_Median_Elapsed_Time_Of_Concurrent_instances
	
	private XLog lastLog=null;
	private long lastTimeWindow;
	private static double timeMeasure=1;
	private static  String dateUnit;
	private static XLog log;
	private XTrace trace=null;
	private static boolean firstTimeInGetParameterNames=true;
	private boolean workloadToBeComputed;
	private static Map<String,Integer> timeUnits; 
	private long timeWindow=3600000;
	private static boolean usePastTimeWindow = true;
	private static boolean useFutureTimeWindow = true;
	private static String pastTimeWindowText ="Past Time Window";
	private static String futureTimeWindowText ="Future Time Window";
	private Collection<String> traceAttributeSet;
	private String caseAttribute;
	private String caseAttributeValue;
	private static String traceAttributeToConsider[];
	private static boolean notConsiderTraceAttributes;
	boolean isInGetPossibleValuesForParameterPhase=false;
	
	private ConcurrentType concurrentType;
	
	
	private void setTimeUnits(){
		timeUnits = new HashMap<>();
		timeUnits.put("Minutes", 60);
		timeUnits.put("Hours", 60*60);
		timeUnits.put("Days", 60*60*24);
		dateUnit=timeUnits.keySet().iterator().next();
		
	}
	
	public ConcurrentInstances(ConcurrentType concurrentType, Collection<String> traceAttributeSet,Map<String, ArrayList<String>> caseAttributeValues) {
		this(concurrentType.toString(),concurrentType,traceAttributeSet,null,null,caseAttributeValues);
		/*
		super(concurrentType.toString());
		this.concurrentType = concurrentType;
		this.traceAttributeSet = traceAttributeSet;

		//setTimeUnits();
		if (dateUnit== null){
			setTimeUnits();
		}
		*/
	}
	
	public ConcurrentInstances(String name, ConcurrentType concurrentType, Collection<String> traceAttributeSet,String caseAttribute, String caseAttributeValue, Map<String, ArrayList<String>> caseAttributeValues) {
		super(name);
		//super(caseAttribute+"_"+caseAttributeValue);
		this.concurrentType = concurrentType;
		this.traceAttributeSet = traceAttributeSet;
		this.caseAttribute = caseAttribute;
		this.caseAttributeValue = caseAttributeValue; 
		this.caseAttributeValues = caseAttributeValues;;
		//setTimeUnits();
		if (dateUnit== null){
			setTimeUnits();
		}
	}

	public void reset(XTrace trace) {
		if (!firstTimeInGetParameterNames){
			firstTimeInGetParameterNames=true;
		}
		this.trace=trace;
	}

	public void setActivitiesToConsider(Collection<String> activitiesToConsider)
	{
		if (this.activitiesToConsider==null || this.activitiesToConsider.size()!=activitiesToConsider.size())
			isSetactivitiesToConsiderChanged=true;
		else
		{
			this.activitiesToConsider.removeAll(activitiesToConsider);
			if (this.activitiesToConsider.size()>0)
				isSetactivitiesToConsiderChanged=true;
			else
				isSetactivitiesToConsiderChanged=false;
		}
		this.activitiesToConsider=new HashSet<String>(activitiesToConsider);
	}
	
	public Object returnAttribute(XEvent event){
		//void augmentEvent(XEvent newEvent, XEvent event) {
		if(workloadToBeComputed)
			computeTimestamps();
		
		Date time = XTimeExtension.instance().extractTimestamp(event);
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSSZ");
		String traceAttributesValues = getTraceAttributesValues(trace);
		String key = sdf.format(time) + ":" + traceAttributesValues;
		Long activityDateEventAslong = time.getTime();
		
		Date firstEventTimeStamp=XTimeExtension.instance().extractTimestamp(trace.get(0));
		Date lastEventTimeStamp=XTimeExtension.instance().extractTimestamp(trace.get(trace.size()-1));							
		Long firstEventAslong = firstEventTimeStamp.getTime();
		Long lastEventAslong = lastEventTimeStamp.getTime();
		
		long elapsedTime=activityDateEventAslong-firstEventAslong;
		long remainingTime=lastEventAslong-activityDateEventAslong;
		long durationTime=lastEventAslong-firstEventAslong;
	
		if (time==null)
			return null;
		
		if (concurrentType==concurrentType.Case_Attributes_Data){
			Map<String,Map<String,Integer>> caseAttributeValues = caseAttributeValuesConcurrentData.get(caseAttribute);	
			if (caseAttributeValues == null){
				//not supposed to be null
				return null;
			}
			Map<String,Integer> counters = caseAttributeValues.get(caseAttributeValue);
			String keyDateOnly = sdf.format(time);
			
			//7_1_2019 - set the key as VA
			//Integer counter = counters.get(keyDateOnly);
			Integer counter = counters.get(key);
			
			//16_1_2019 - set this key as VI
			//Integer counter = counters.get(keyDateOnly);
			if (counter != null){
				return counter.intValue();
			}
			else
				return null;
		}
		else{
			Long[] occData = concurrentOccurreneData.get(key);
			if (occData != null){
				int concurrentTypeInt = concurrentType.GetID();		
				return occData[concurrentTypeInt];
			}
			else 
				return null;
		}
		

	}
	
	@Override
	public boolean multipleValuesForParameter(int i)
	{
		if (i==0 || i==2 || i==3)
			return true;
		else
			return false;
	}
	
	@Override
	public String[] getDefaultValueForParameter(int i)
	{
		if (i==0){
			String[] retVal = timeUnits.keySet().toArray(new String[timeUnits.size()]);
			String[] newstr = {retVal[0]};
			if (!dateUnit.isEmpty())
				newstr[0] = dateUnit;
			return newstr;
		}
		else if(i==1)
		{	
			String[] defaultValues=new String[] {String.valueOf(timeMeasure)};
			return(defaultValues);
		}
		else if(i==2)
		{	
			String dv = "";
			
			if (useFutureTimeWindow) dv = futureTimeWindowText;
			if (usePastTimeWindow) dv = String.join(",",dv,pastTimeWindowText);
		
			String[] defaultValues= dv.split(",");
			return(defaultValues);
		}
		else {//3
			if (traceAttributeToConsider  == null){
				traceAttributeToConsider = new String[]{"None"};
				notConsiderTraceAttributes = true;
			}
			return traceAttributeToConsider;
		}

	}
	
	@Override
	public String[] getPossibleValuesForParameter(int i)
	{
		if (!isInGetPossibleValuesForParameterPhase){
			isInGetPossibleValuesForParameterPhase = true;
		}
		
		if (i==0) {
			String[] retVal = timeUnits.keySet().toArray(new String[timeUnits.size()]);
			return retVal;
		}
		
		else if (i==2){	 
			String[] retVal = new String[] {pastTimeWindowText,futureTimeWindowText};
			return retVal;
		}
		
		else if (i==3){
			String[] retVal = new String[traceAttributeSet.size()+1];
			retVal[0]= "None";
			int j=1;
			for(String attribute : traceAttributeSet)
			{					
				retVal[j++] = attribute;
			}
					//String[] retVal =traceAttributeSet.toArray(new String[traceAttributeSet.size()+1]);
			return retVal;
		}
		else
			return(null);

	}	
	
	@Override
	public String[] getParameterNames()
	{
		if (firstTimeInGetParameterNames)
		{
			firstTimeInGetParameterNames = false;
			return new String[] {"Time Unit","Time Window","Time Window Type","Only Instances With The Same"};
		}
		else
			return new String[0];

	}
	
	@Override
	public boolean setParameter(int param, String value[])
	{
		if (param==0){
			if (dateUnit!=value[0])
			{
				dateUnit=value[0];
				
				int  unitSeconds = timeUnits.get(dateUnit);
				timeWindow=(long) (timeMeasure*(unitSeconds*1000L));
				
				workloadToBeComputed=true;
			}
		}
		else if (param==1)
		{	
			try {
				double newTime=Double.parseDouble(value[0]);
				if (newTime!=timeMeasure)
				{
					timeMeasure=newTime;
					
					int  unitSeconds = timeUnits.get(dateUnit);
					timeWindow=(long) (timeMeasure*(unitSeconds*1000L));
				
					workloadToBeComputed=true;
				}
			} 
			catch (Exception e) {
				return false;
			}
		}
		
		else if (param==2)
		{	
			boolean prevUsePastTimeWindow = usePastTimeWindow;
			boolean prevUseFutureTimeWindow = useFutureTimeWindow;
			usePastTimeWindow = false;
			useFutureTimeWindow = false;
			
			for(String str :value ){
				if (str.equals(pastTimeWindowText)) 
					usePastTimeWindow = true;
				if (str.equals(futureTimeWindowText)) 
					useFutureTimeWindow = true;			
		}
			if (prevUsePastTimeWindow != usePastTimeWindow || prevUseFutureTimeWindow != useFutureTimeWindow)
				workloadToBeComputed=true;
		}
		else if (param==3){				
			if (value.length>0)
			{
				boolean prevNotConsiderTraceAttributes = notConsiderTraceAttributes; 
				String prevAttributes =  arrayToString(traceAttributeToConsider);
				
				traceAttributeToConsider=value.clone();
				notConsiderTraceAttributes = false;
				for(String val : traceAttributeToConsider){
					if (val == "None"){
						notConsiderTraceAttributes = true;
					}
				}
				
				String newAttributes = arrayToString(traceAttributeToConsider);
				if (prevNotConsiderTraceAttributes != notConsiderTraceAttributes || !newAttributes.equals(prevAttributes))
					workloadToBeComputed=true;
				
				return true;
			}
			else
				return false;
		}
		
	return true;

	}
	
	private String arrayToString(String array[])
	{
	    if (array.length == 0) return "";
	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < array.length; ++i)
	    {
	        sb.append(",'").append(array[i]).append("'");
	    }
	    return sb.substring(1);
	}
	
	private void computeTimestamps()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSSZ");
		
		//for debug
		System.out.println(sdf.format(new Date()) + "   start 'computeTimestamps' on ConcurrentInstances");

		workloadToBeComputed=false;
		PriorityQueue<Date> sortedTimestamps=new PriorityQueue<Date>();
		PriorityQueue<PairDateLong> sortedTimestamps1=new PriorityQueue<PairDateLong>();
			
		ArrayList<TraceData> traceDataList = new ArrayList<TraceData>(log.size());
		
		occurrenceMap.clear();
		
  	    occurreneDurationTimeMap1.clear();
	    occurreneElapsedTimeMap1.clear();
	    occurreneRemainTimeMap1.clear();
	    
	    concurrentOccurreneData.clear();
	    
		int i=0;
		
	    Date firstEventTimeStamp;
	    Date lastEventTimeStamp;
	    
	    long traceCounter = 0;
	    
	    caseAttributeValuesConcurrentData = new HashMap<String, Map<String,Map<String,Integer>>>();
	    //build the caseAttributeValuesConcurrentData object
		for(String caseAttr:caseAttributeValues.keySet()){
			Map<String,Map<String,Integer>> attrValuesMap = new  HashMap<String,Map<String,Integer>>();
			ArrayList<String> attrValues =  caseAttributeValues.get(caseAttr);
			for(String attrVal:attrValues){
				attrValuesMap.put(attrVal,new HashMap<String,Integer>());
			}
			caseAttributeValuesConcurrentData.put(caseAttr, attrValuesMap);
		}
		

	    
	    //loop on all the traces in the log
		for(XTrace trace : log)
		{
			traceCounter++;
			firstEventTimeStamp=XTimeExtension.instance().extractTimestamp(trace.get(0));
			lastEventTimeStamp=XTimeExtension.instance().extractTimestamp(trace.get(trace.size()-1));							
			Long firstEventAslong = firstEventTimeStamp.getTime();// - timeWindow ;
			Long lastEventAslong = lastEventTimeStamp.getTime();// + timeWindow ;
			//build the string that holds the needed trace attributes 
			String traceAttributesValues = getTraceAttributesValues(trace);
			TraceData td = new TraceData(traceCounter, firstEventAslong, lastEventAslong,traceAttributesValues,trace);
			traceDataList.add(td);
			
			//loop on all the events in the trace
			for (XEvent event : trace){
				String eventName  = XConceptExtension.instance().extractName(event);
				Date eventDate = XTimeExtension.instance().extractTimestamp(event);
				if(!Predictor.CASE_ACTIVITY.equals(eventName))
					if (eventDate !=null){
						//if this event is as the event that were chosen to be prediction point 
						if (activitiesToConsider.contains(eventName)){
							PairDateLong pdl = new PairDateLong(eventDate, traceCounter);
							//save the time of the activity in an array
							sortedTimestamps1.add(pdl);
						}
					}
			} 
		}
		//build array of all times in all the activities in the log
		PairDateLong[] array1 = sortedTimestamps1.toArray(new PairDateLong[0]);
		
		//sort the array of the traces digest data
		Collections.sort(traceDataList);
		
		
		System.out.println(sdf.format(new Date()) + "   end sorting all events of 'computeTimestamps' on ConcurrentInstances");
		
		  int kkk = 0;
		  //loop on the relevant events
	      for(PairDateLong pdl: array1){	  
			
	    	  Date activityDate = pdl.getDate();
	    	  long traceIdentifierOut = pdl.getIdentifier();
	    	  
	    	//for debug
				if (kkk%1000 == 0)
					System.out.println(sdf.format(new Date()) + " 'computeTimestamps'  Loop on relevant sorted activities. kkk="+kkk);
				kkk++;
			  
				//is there already values for this date?
	    	  Integer numOccurrence = occurrenceMap.get(activityDate);
			  Long activityDateEventAslong = activityDate.getTime();
				
	    	  //if we already compute this time stamp
	    	  if(numOccurrence != null){
	    		  continue;
	    	  }
	    	  
	  	    occurreneDurationTimeMap1.clear();
		    occurreneElapsedTimeMap1.clear();
		    occurreneRemainTimeMap1.clear();

	    	  numOccurrence=0;	    	  
	    	  occurrenceMap.put(activityDate, numOccurrence);

	    	  //loop on all the traces digest data
	    	  for(TraceData traceData: traceDataList){
	    		  long firstEventAslong = traceData.getFirstEventAslong();// - timeWindow ;
	    		  long lastEventAslong = traceData.getLastEventAslong();// + timeWindow ;
	    		  long traceIdentifier = traceData.getTraceIdentifier();
	    		  String traceAttributesValues = traceData.getTraceAttributesValues();
	    		  
	    		  //do not calculate the same trace data on itself
	    		  if (traceIdentifierOut == traceIdentifier)
	    			  continue;
	    		  
	    		  String key = sdf.format(activityDate) + ":" + traceAttributesValues;
	    		  
	    		  long firstEventAslongWithWindow =firstEventAslong; // - timeWindow ;
	    		  if (usePastTimeWindow)
	    			  firstEventAslongWithWindow -= timeWindow;
	    		  long lastEventAslongWithWindow = lastEventAslong; // + timeWindow ;
	    		  if (useFutureTimeWindow)
	    			  lastEventAslongWithWindow +=timeWindow;
	    		  
	    		  //because the list is sorted by the first event time there is no need to continue the traces loop if we encountered with
	    		  //trace that starts after our tested activity
	    		
	    		  if (activityDateEventAslong < firstEventAslongWithWindow)
	    			  break;
	    		  
	    		  if (activityDateEventAslong >= firstEventAslongWithWindow  && activityDateEventAslong <= lastEventAslongWithWindow) {
					
	    			  //1_1_2019 calculate the concurrent case date
	    			  String keyOnlyDate = sdf.format(activityDate);// + ":" + traceAttributesValues;
	    			  
	    			  //7_1_2019 - set the key as VA
	    			  for(String casteAttr:caseAttributeValuesConcurrentData.keySet()){
	    				  String attrValue = traceData.getCaseAttributeValue(casteAttr);
	    				  Map<String,Integer> valueDateCounter = caseAttributeValuesConcurrentData.get(casteAttr).get(attrValue);
	    				  
	    				  //Integer counter = valueDateCounter.get(keyOnlyDate);
	    				  //7_1_2019
	    				  
	    				  //16_01_2019 - set this feature as VI
	    				 // Integer counter = valueDateCounter.get(keyOnlyDate);
	    				  
	    				  //12_08_2019
	    				  Integer counter = null;
	    				  if (valueDateCounter != null)
	    					  counter = valueDateCounter.get(key);
	    				  else //the attribute is not exists in this trace
	    					  continue;
	    				  if (counter == null)
	    					  counter = 0;
	    				  counter++;
	    				  //16_01_2019
	    				  //valueDateCounter.put(keyOnlyDate, counter); 				  
	    				  valueDateCounter.put(key, counter);
	    			  }
	    			  
	    			  //TBD - to count every trace only once
	    			  numOccurrence++; 
						occurrenceMap.put(activityDate, numOccurrence);
						/*
						//only calculate the duration parameters if they have the same attributes as the outer trace attributes
						//if the user selected to not used the attributes "NONE" then  the value of this parameters is equal 
				    	//	if (traceAttributesValuesOut.equals(traceAttributesValues)){
						*/
						
					    	  List<Long> durationArray1 = occurreneDurationTimeMap1.get(key);
					    	  	if (durationArray1 ==null){
					    	  		durationArray1 = new ArrayList<Long>();
					    	  		occurreneDurationTimeMap1.put(key,durationArray1);
					    	  	}
							  List<Long> elapsedArray1 = occurreneElapsedTimeMap1.get(key);
							  if (elapsedArray1 ==null){
								  elapsedArray1 = new ArrayList<Long>();
								  occurreneElapsedTimeMap1.put(key, elapsedArray1);
							  }
							  List<Long> remainedArray1 = occurreneRemainTimeMap1.get(key);
							  if (remainedArray1 ==null){
								  remainedArray1 = new ArrayList<Long>();
								  occurreneRemainTimeMap1.put(key, remainedArray1);
							  }
												    	  
							long elapsedTime=activityDateEventAslong-firstEventAslong;
							long remainingTime=lastEventAslong-activityDateEventAslong;
							long durationTime=lastEventAslong-firstEventAslong;
							
							durationArray1.add(durationTime);
							remainedArray1.add(remainingTime);
							
							// if the event is in the future then do not relate it 
							if (elapsedTime > 0){
								elapsedArray1.add(elapsedTime);
							}
					}
	    	  }
	    	  for(String key1 : occurreneDurationTimeMap1.keySet()){
		    	  Long[] occDate = new Long[ConcurrentType.values().length];
		    	  occDate[ConcurrentType.Count_Concurrent_Instances.GetID()] = (long) numOccurrence;
		    	  
		    	  //Anchor_Event_Mean_Elapsed_Time_Of_Concurrent_instances
		    	  List<Long> elapsedArray=this.occurreneElapsedTimeMap1.get(key1);
		    	  List<Long> remainedArray=this.occurreneRemainTimeMap1.get(key1);
		    	  List<Long> durationArray=this.occurreneDurationTimeMap1.get(key1);
		    	 
		    	  long meanElapsed = mean(elapsedArray);
		    	  long medianElapsed = median(elapsedArray);
		    	  long meanRemained = mean(remainedArray);
		    	  long medianRemined = median(remainedArray);
		    	  long meanDuration = mean(durationArray);
		    	  long medianDuration = median(durationArray);
		    	 		    		
		    	  occDate[ConcurrentType.Mean_Duration_Time_Concurrent_Instances.GetID()] = meanDuration;
		    	  occDate[ConcurrentType.Median_Duration_Time_Concurrent_Instances.GetID()] = medianDuration;
		    	  occDate[ConcurrentType.Mean_Remained_Time_Concurrent_Instances.GetID()] = meanRemained;
		    	  occDate[ConcurrentType.Median_Remained_Time_Concurrent_Instances.GetID()] = medianRemined;
		    	  occDate[ConcurrentType.Anchor_Event_Mean_Elapsed_Time_Of_Concurrent_instances.GetID()] = meanElapsed;
		    	  occDate[ConcurrentType.Anchor_Event_Median_Elapsed_Time_Of_Concurrent_instances.GetID()] = medianElapsed;
		    	 		    			 		    	  
		    	  concurrentOccurreneData.put(key1, occDate);
	    	  }
	      }	
	      System.out.println(sdf.format(new Date()) + "   start 'computeTimestamps' on ConcurrentInstances");
	}
	
	private String getTraceAttributesValues(XTrace trace){
		String traceAttributesValues = "None";
		if (!notConsiderTraceAttributes){
			String value;
			//String attributesValueStr;
			String attributesValue[] = new String[traceAttributeToConsider.length];
			int atri = 0;
			for(String attribute : traceAttributeToConsider){
				XAttribute attributeObj = trace.getAttributes().get(attribute);
				if (attributeObj==null)
					value="";
				else
					value=getAttributeValues(attributeObj).toString();
				attributesValue[atri++] = attribute.concat(" : ").concat(value);
			}
			traceAttributesValues = arrayToString(attributesValue);
		}
		return traceAttributesValues;
	}
	
	public static long mean1(List<Long> list,Long exceptElement) {
		//list.toArray(a)
	    double sum = 0;
	    
	    if (list.size() == 0){
	    	return 0;
	    }
	    // size =1 when there is only one trace date in the array 
	    if (list.size() == 1){
	    	return 0;
	    }
	    else
	    {
		    for (int i = 0; i < list.size(); i++) {
		        sum += list.get(i);
		    }
		    sum -= exceptElement;
		    return (long) sum / (list.size() -1);
	    }
	}
	
	public static long mean(List<Long> list) {
		//list.toArray(a)
	    double sum = 0;
	    
	    if (list == null ||list.size() == 0){
	    	return 0;
	    }
	    else
	    {
		    for (int i = 0; i < list.size(); i++) {
		        sum += list.get(i);
		    }
		    return (long) sum / list.size();
	    }
	}
	
	// the array double[] m MUST BE SORTED
	public static long median(List<Long> list) {
		
		if (list == null || list.size() == 0){
	    	return 0;
	    }
		Collections.sort(list);
	    int middle = list.size()/2;
	    if (list.size()%2 == 1) {
	        return list.get(middle);
	    } else {
	        return (long) ((list.get(middle-1) + list.get(middle)) / 2.0);
	    }
	}
	
	
	public void setLog(XLog log) {
		if (this.log!=log){
			this.log=log;
			workloadToBeComputed=true;
			//firstTimeInGetParameterNames=false;
		}
		else{
			//not need to recalculate the neighbors data
			workloadToBeComputed=false;
			//not need to show the parameters wizard
			//startEventsPresent=true;
		}
		/*this.log=log;
		workloadToBeComputed=true;
		startEventsPresent=false;*/
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
	
	private class TraceData implements Comparable{
		private long firstEventAslong;
		private long lastEventAslong;
		private String traceAttributesValues;
		private long traceIdentifier;
		private XTrace trace;
		
		TraceData(long traceIdentifier, long firstEventAslong,long lastEventAslong,String traceAttributesValues,XTrace trace ){
			this.setFirstEventAslong(firstEventAslong);
			this.setLastEventAslong(lastEventAslong);
			this.traceAttributesValues=traceAttributesValues;
	        this.traceIdentifier = traceIdentifier;
	        this.trace =  trace;
		}
		
		public String getCaseAttributeValue(String caseAttribute){
			String value;
			XAttribute attributeObj = trace.getAttributes().get(caseAttribute);
			if (attributeObj==null)
				value=null;
			else
				value=getAttributeValues(attributeObj).toString();
			return value;
		}
		
		String getTraceAttributesValues() {
			return traceAttributesValues;
		}
	    public long getTraceIdentifier() {return traceIdentifier;}
	    
		Long getFirstEventAslong() {
			return firstEventAslong;
		}
		void setFirstEventAslong(long firstEventAslong) {
			this.firstEventAslong = firstEventAslong;
		}
		Long getLastEventAslong() {
			return lastEventAslong;
		}
		void setLastEventAslong(long lastEventAslong) {
			this.lastEventAslong = lastEventAslong;
		}
		
		@Override
	    public int compareTo(Object  p1) {
			
			final int BEFORE = -1;
		    final int EQUAL = 0;
		    final int AFTER = 1;
		    
			if (firstEventAslong < ((TraceData)p1).getFirstEventAslong())
				return BEFORE;
			else if (firstEventAslong == ((TraceData)p1).getFirstEventAslong())
				return EQUAL;
			else
				return AFTER;
	    }
	}


	private class PairDateLong implements Comparable{
		private Date date;
		private long identifier;
		
		PairDateLong(Date date,Long identifier ){
			this.date = date;//(firstEventAslong);
			this.identifier=identifier;
		}
		Date getDate() {
			return date;
		}

		public long getIdentifier() {return identifier;}

		
		@Override
	    public int compareTo(Object  p1) {
			if (date.before(((PairDateLong)p1).getDate()))
				return -1;
			else
				return 1;
	    }
	}
	
	
	private class PairDateString implements Comparable{
		private Date date;
		private String traceAttributesValues;
		
		PairDateString(Date date,String traceAttributesValues ){
			this.date = date;//(firstEventAslong);
			this.traceAttributesValues=traceAttributesValues;
		}
		Date getDate() {
			return date;
		}

		String getTraceAttributesValues() {
			return traceAttributesValues;
		}

		
		@Override
	    public int compareTo(Object  p1) {
			if (date.before(((PairDateString)p1).getDate()))
				return -1;
			else
				return 1;
	    }
	}
}