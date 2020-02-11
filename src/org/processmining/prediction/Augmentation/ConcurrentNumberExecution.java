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

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.prediction.Predictor;


public class ConcurrentNumberExecution extends Augmentation implements ActivityLevelAugmentation {
	/*
	//this object will contains for each activity timeStamp, the number of concurrent active traces
	private static Map<Date, Map<String,Integer>> occurrenceMap; 
	private static Map<Date, Map<String,Integer>> resourceMap; 
	private static Map<Date, Map<String,Integer[]>> resourceGroupMap; 
	*/
	private static Map<String, Map<String,Integer>> occurrenceMap; 
	private static Map<String, Map<String,Integer>> resourceMap; 
	private static Map<String, Map<String,Integer>> resourceGroupMap; 
	
	private static Map<String, Map<String,String>> nextActivityOccurrenceMap1;
	private static Map<String, Map<String,String>> prevActivityOccurrenceMap1;
	private static Map<String, Map<String,Double>> meanDeltaFromMeanTimeFromActivityStartOccurrenceMap1;
	private static Map<String, Map<String,Integer>> medianDeltaFromMedianTimeFromActivityStartOccurrenceMap1;
	private static Map<String, Long[]> tracesMeanMeadinDeltaFromStart;
	
	public static enum ConcurrentType { Number_Of_Executions, Delta_From_Mean_Time_From_Start, Next_Activity,Prev_Activity,
		Resource_Overload, Resource_Group_Overload,Activity_Median_Of_Delta_From_Mean_Elapsed_Time, Activity_Mean_Of_Delta_From_Mean_Elapsed_Time, 
		Concurrent_Instances_Mean_Delays_From_Expected_Elapsed_Time,Concurrent_Instances_Median_Delays_From_Expected_Elapsed_Time}
	
	//Concurrent_Instances_Median_Delays_From_Expected_Elapsed_Time
	//Concurrent_Instances_Median_Delays_From_Expected_Elapsed_Time

	private XTrace trace=null;
	private XLog lastLog=null;
	private long lastTimeWindow;
	private static double timeMeasure=1;
	private long timeWindow=3600000;
	private static String dateUnit;
	private static boolean usePastTimeWindow = true;
	private static boolean useFutureTimeWindow = true;
	private static String pastTimeWindowText ="Past Time Window";
	private static String futureTimeWindowText ="Future Time Window";
	private Collection<String> traceAttributeSet;
	private static String traceAttributeToConsider[];
	private static boolean notConsiderTraceAttributes;
	
	private static XLog log;
	private static boolean firstTimeInGetParameterNames=true;
	private boolean workloadToBeComputed;
	private static Map<String,Integer> timeUnits; 
	private Collection<String> activitySet;
	private Collection<String> resources;
	private Collection<String> resourceGroups;
	
	private ConcurrentType concurrentType;
	private final String activityName;
	boolean isInGetPossibleValuesForParameterPhase=false;
	private Collection<String> activitiesToConsider;
	private boolean isSetactivitiesToConsiderChanged;
	
	public ConcurrentNumberExecution(String activityName,Collection<String> activitySet,Collection<String> traceAttributeSet,Collection<String> resources,Collection<String> resourceGroups, ConcurrentType concurrentType)
	{
		super(concurrentType.toString()+" " +activityName);
		this.activityName=activityName;
		this.activitySet = activitySet;
		this.resources = resources;
		this.resourceGroups = resourceGroups;		
		this.traceAttributeSet = traceAttributeSet;
		this.concurrentType = concurrentType;
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
		
		private void setTimeUnits(){
			timeUnits = new HashMap<>();
			timeUnits.put("Minutes", 60);
			timeUnits.put("Hours", 60*60);
			timeUnits.put("Days", 60*60*24);
			dateUnit=timeUnits.keySet().iterator().next();
			
		}
			
		public Object returnAttribute(XEvent event){
		
			if(workloadToBeComputed)
				computeTimestamps();
			Date time = XTimeExtension.instance().extractTimestamp(event);
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSSZ");
			String traceAttributesValues = getTraceAttributesValues(trace);
			String key = sdf.format(time) + ":" + traceAttributesValues;
			String keyDateOnly = sdf.format(time);
			
			if (time==null)
				return null;

			if (concurrentType == ConcurrentType.Number_Of_Executions){
				Map<String,Integer> neighours=this.occurrenceMap.get(key);//time);
				int val;
				if (neighours == null)
					val= -1;
				else{
					if (neighours.containsKey(activityName))
						val = neighours.get(activityName);
					else
						val = -1;
				}
				return val;
			}
			if (concurrentType == ConcurrentType.Resource_Overload){
				Map<String,Integer> neighours=this.resourceMap.get(key);//time);
				//16_01_2019 - use this feature as VI
				//Map<String,Integer> neighours=this.resourceMap.get(keyDateOnly);//time);
				int val;
				if (neighours == null)
					val= -1;
				else{
					if (neighours.containsKey(activityName))
						val = neighours.get(activityName);
					else
						val = -1;
				}
				return val;
			}
			if (concurrentType == ConcurrentType.Resource_Group_Overload){
				Map<String,Integer> neighours=this.resourceGroupMap.get(key);
				//16_01_2019 - use this feature as VI
				//Map<String,Integer> neighours=this.resourceGroupMap.get(keyDateOnly);
				int val;
				if (neighours == null)
					val= -1;
				else{
					if (neighours.containsKey(activityName))
						val = neighours.get(activityName);
					else
						val = -1;
				}
				return val;
			}
			else if (concurrentType == ConcurrentType.Next_Activity){
				Map<String,String> neighours=this.nextActivityOccurrenceMap1.get(key);
				String val;
				if (neighours == null)
					val= "";
				else{
					val = neighours.get(activityName);
					if (val == null)
						val = "";
				}
				return val;
			}
			else if (concurrentType == ConcurrentType.Prev_Activity){
				//Map<String,String> neighours=this.prevActivityOccurrenceMap.get(time);
				Map<String,String> neighours=this.prevActivityOccurrenceMap1.get(key);
				String val;
				if (neighours == null)
					val= "";
				else{
					val = neighours.get(activityName);
					if (val == null)
						val = "";
				}
				return val;
			}
			else if (concurrentType == ConcurrentType.Activity_Mean_Of_Delta_From_Mean_Elapsed_Time){
				Map<String,Double> neighours=this.meanDeltaFromMeanTimeFromActivityStartOccurrenceMap1.get(key);
				Double val;
				if (neighours == null)
					val= null;
				else{
					val = neighours.get(activityName);
					if (val == null)
						val = null;
				}
				return val;
			}
			
			else if (concurrentType == ConcurrentType.Activity_Median_Of_Delta_From_Mean_Elapsed_Time){
				Map<String,Integer> neighours=this.medianDeltaFromMedianTimeFromActivityStartOccurrenceMap1.get(key);
				Integer val;
				if (neighours == null)
					val= null;
				else{
					val = neighours.get(activityName);
					if (val == null)
						val = null;
				}
				return val;
			}
			
			else if (concurrentType == ConcurrentType.Concurrent_Instances_Mean_Delays_From_Expected_Elapsed_Time){
				Long[] neighours=this.tracesMeanMeadinDeltaFromStart.get(key);
				Long val;
				if (neighours == null)
					val= null;
				else{
					val = neighours[0];
					if (val == null)
						val = null;
				}
				return val;
			}
			else if (concurrentType == ConcurrentType.Concurrent_Instances_Median_Delays_From_Expected_Elapsed_Time){
				Long[] neighours=this.tracesMeanMeadinDeltaFromStart.get(key);
				Long val;
				if (neighours == null)
					val= null;
				else{
					val = neighours[1];
					if (val == null)
						val = null;
				}
				return val;
			}
			//Activity_Mean_Of_Delta_From_Mean_Elapsed_Time, Concurrent_Instances_Mean_Delays_From_Expected_Elapsed_Time,Concurrent_Instances_Median_Delays_From_Expected_Elapsed_Time}
			
			/**else if (concurrentType == ConcurrentType.Remained_Time){
				Map<String,Integer> neighours=this.occurrenceMap.get(time);
				int val = neighours.get(activityName);
				return val;
			}*/
			return null;
			//
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
		
		private String getTraceAttributesValues(XTrace trace){
			String traceAttributesValues = "None";
			if (!notConsiderTraceAttributes){
				String value;
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
			
		/**
		 * 
		 */
		private void computeTimestamps()
		{			
			//set the format of the date
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSSZ");
			
			System.out.println(sdf.format(new Date()) +  " Start ConcurrentNumberExecution in computeTimestamps....");
			
			//flag to set calculation of all the concurrent traces activities to be only onces
			workloadToBeComputed=false;
/*
			//init the class level arrays
			occurrenceMap = new HashMap<Date, Map<String,Integer>>();
			resourceMap = new HashMap<Date, Map<String,Integer>>();			
			resourceGroupMap = new HashMap<Date, Map<String,Integer[]>>();
		*/	
			occurrenceMap = new HashMap<String, Map<String,Integer>>();
			resourceMap = new HashMap<String, Map<String,Integer>>();			
			resourceGroupMap = new HashMap<String, Map<String,Integer>>();
			
			nextActivityOccurrenceMap1 = new HashMap<String, Map<String,String>>();
			prevActivityOccurrenceMap1 = new HashMap<String, Map<String,String>>();
			meanDeltaFromMeanTimeFromActivityStartOccurrenceMap1 = new HashMap<String, Map<String,Double>>();
			medianDeltaFromMedianTimeFromActivityStartOccurrenceMap1 = new HashMap<String, Map<String,Integer>>();
			tracesMeanMeadinDeltaFromStart = new  HashMap<String, Long[]>();
			List<Pair> pairList = new ArrayList<Pair>(log.size());
		    int traceCounter = -1;
		    
		    HashMap<String,HashMap<String,HashMap<Integer,ArrayList<Long>>>> logActivitiesElapsedTimeStatisticBuilder =  new  HashMap<String,HashMap<String,HashMap<Integer,ArrayList<Long>>>> (activitySet.size());
		    HashMap<String,HashMap<String,HashMap<Integer,Long[]>>> logActivitiesElapsedTimeStatisticResults =  new HashMap<String,HashMap<String,HashMap<Integer,Long[]>>>(activitySet.size());
		    
		    //loop on all the traces in the log
			for(XTrace trace : log)
			{				
				//the current trace number
				traceCounter++;
				
				if (traceCounter % 1000 == 0)
					System.out.println(sdf.format(new Date()) +  "   for(XTrace trace : log)... trace num = " + traceCounter);
								
				//build the string that holds the trace attributes that declare the "type" of the trace
				String traceAttributesValues = getTraceAttributesValues(trace);
				
				Date firstEventTimeStamp=XTimeExtension.instance().extractTimestamp(trace.get(0));
				HashMap<String,Integer> traceActivitiesCounter = new HashMap<String,Integer>(trace.size());

				//loop on all the trace activities
				for (int eventNumber =0;  eventNumber < trace.size() ;eventNumber++ ){ 
					XEvent event = trace.get(eventNumber);
					String transitionName=XLifecycleExtension.instance().extractTransition(event);
					
					//we are looking only for events of type "complete"
					if (transitionName!=null && transitionName.equals("start"))
						continue;
					
					//if this is a real activity 
					if(!Predictor.CASE_ACTIVITY.equals(XConceptExtension.instance().extractName(event)))
						if (XTimeExtension.instance().extractTimestamp(event)!=null){
							Date d = XTimeExtension.instance().extractTimestamp(event);
							String s = XConceptExtension.instance().extractName(event);
							/*
							//prev event name calculation
							String prevEventName ="";
							int intPrevCounter =1;
							XEvent prevEvent = trace.get(eventNumber-intPrevCounter);							
							String prevEventTransitionName =XLifecycleExtension.instance().extractTransition(prevEvent);

							//we are looking only for events of type "complete"
							while (prevEventTransitionName!=null && prevEventTransitionName.equals("start") && eventNumber-intPrevCounter >1){
								intPrevCounter++;
								prevEvent = trace.get(eventNumber-intPrevCounter);
								prevEventTransitionName=XLifecycleExtension.instance().extractTransition(prevEvent);
							}														
							if (eventNumber >0){
								String tempPrevEventName = XConceptExtension.instance().extractName(prevEvent);
								if(!Predictor.CASE_ACTIVITY.equals(tempPrevEventName))
									prevEventName = tempPrevEventName;
							}
							*/
							// ***************to enter THIS code instead the code above (need to check a transition data)
							//prev event name calculation - option B
							String prevEventName ="";
							if (pairList.size() >0 && eventNumber >1){
								prevEventName = pairList.get(pairList.size()-1).getActivityName();
							}
							/*
							if (prevEventName.equals("")){
								if (!s.equals("UNIT_CREATE")){
									int dfgdgdg=34343;
								}
							}
							*/
							/*
							//test
							if (!prevEventNameB.equals(prevEventName)){
								System.out.println(sdf.format(new Date()) +  " Strange......  ");
							}
							*/
							
							//next event name calculation
							String nextEventName ="";
							int intNextCounter =1;
							XEvent nextEvent = trace.get(eventNumber+intNextCounter);
							String nextEventTransitionName =XLifecycleExtension.instance().extractTransition(nextEvent);
							//we are looking only for events of type "complete"
							while (nextEventTransitionName!=null && nextEventTransitionName.equals("start") && eventNumber+intNextCounter < trace.size()-3){
								intNextCounter++;
								nextEvent = trace.get(eventNumber+intNextCounter);
								nextEventTransitionName=XLifecycleExtension.instance().extractTransition(nextEvent);
							}	
							if (eventNumber <trace.size()-2){
								String tempNextEventName = XConceptExtension.instance().extractName(nextEvent);
								if(!Predictor.CASE_ACTIVITY.equals(tempNextEventName))
									nextEventName = tempNextEventName;
							}
							
							//resource calculation
							String resource=XOrganizationalExtension.instance().extractResource(event);
							//resource group calculation
							String resourceGroup=XOrganizationalExtension.instance().extractGroup(event);
							/*
							//resource group calculation
							//May be removed
							Map<String,Integer> resourceClustersData = new HashMap<String,Integer>();
							for (String resourceGroup : resourceGroups)
							{
								XAttribute attr=event.getAttributes().get(resourceGroup);
								Integer val= null;
								if (attr!=null){
									try
									{val=Integer.parseInt(getAttributeValues(attr).toString());}
									catch(Exception e){}
									resourceClustersData.put(resourceGroup, val);
								}
							}
							*/
							//calculate the activity number (the counter of the current activity in the trace)
							Integer activityNumber;
							activityNumber =  traceActivitiesCounter.get(s);
							if (activityNumber == null) activityNumber = 0;
							activityNumber++;
							traceActivitiesCounter.put(s, activityNumber);
							
							//build statistics about the elapsed time for each activity
							long elapsedTime = d.getTime()-firstEventTimeStamp.getTime();
							//String key = sdf.format(d) + ":" + traceAttributesValues;
							HashMap<String,HashMap<Integer,ArrayList<Long>>> activityStatistics = logActivitiesElapsedTimeStatisticBuilder.get(s);
							if (activityStatistics == null){
								activityStatistics = new HashMap<String,HashMap<Integer,ArrayList<Long>>>();
								HashMap<String,HashMap<Integer,Long[]>> activityStatistics1 =  new HashMap<String,HashMap<Integer,Long[]>>();
								logActivitiesElapsedTimeStatisticResults.put(s,activityStatistics1);
							}
							//HashMap<Integer,ArrayList<Long>> activityStatistics = logActivitiesElapsedTimeStatisticBuilder.get(s);
							HashMap<Integer,ArrayList<Long>> activityTraceAttributesStatistics= activityStatistics.get(traceAttributesValues);
							if (activityTraceAttributesStatistics == null){
								activityTraceAttributesStatistics = new HashMap<Integer,ArrayList<Long>>();
								activityStatistics.put(traceAttributesValues, activityTraceAttributesStatistics);
								HashMap<Integer,Long[]>  activityStatisticsResults= new HashMap<Integer,Long[]>();
								logActivitiesElapsedTimeStatisticResults.get(s).put(traceAttributesValues, activityStatisticsResults);
							}
							logActivitiesElapsedTimeStatisticBuilder.put(s,activityStatistics);
							ArrayList<Long> occuranceStatistic = activityTraceAttributesStatistics.get(activityNumber);
							if (occuranceStatistic == null) {
								occuranceStatistic = new ArrayList<Long>();
								logActivitiesElapsedTimeStatisticResults.get(s).get(traceAttributesValues).put(activityNumber, new Long[2]);
							}
							occuranceStatistic.add(elapsedTime);
							activityTraceAttributesStatistics.put(activityNumber, occuranceStatistic);
							
							//build the trace digest data object
							Pair pair = new Pair(traceCounter, s, d, eventNumber,activityNumber,  traceAttributesValues, prevEventName,nextEventName,elapsedTime,resource,resourceGroup);							
							//Pair pair = new Pair(traceCounter, s, d, traceAttributesValues, prevEventName,nextEventName,resource);
							pairList.add(pair);
						}
				}

			}
			
			//sort all the events by time
			Collections.sort(pairList);
			
			//convert the List into a regular array
			Pair[] pairArr = new Pair[pairList.size()];
			pairArr = pairList.toArray(pairArr);
			pairList = null;
			
			//built the statistic
			for (String activity : logActivitiesElapsedTimeStatisticBuilder.keySet()){
				HashMap<String,HashMap<Integer,ArrayList<Long>>> activityStatistics1 = logActivitiesElapsedTimeStatisticBuilder.get(activity);
				
				for(String traceAttributesValues: activityStatistics1.keySet()){
					
					HashMap<Integer,ArrayList<Long>> activityStatistics = activityStatistics1.get(traceAttributesValues);
					for (Integer activityCounter : activityStatistics.keySet()){
						ArrayList<Long> occuranceStatistic = activityStatistics.get(activityCounter);
						long mean = mean(occuranceStatistic);
						long median = median(occuranceStatistic);
						Long[] statistics = logActivitiesElapsedTimeStatisticResults.get(activity).get(traceAttributesValues).get(activityCounter);
						statistics[0] = mean;
						statistics[1] = median;
					}
				}
			}
			logActivitiesElapsedTimeStatisticBuilder = null;
			
			
			
			
			System.out.println(sdf.format(new Date()) +  "ConcurrentNumberExecution in computeTimestamps : finish to sort all events");
			
			//loop on all the events and set an array of the location of the activities that need to be considered
			ArrayList<Integer> activitiesLocations = new ArrayList<Integer>(trace.size());
			for (int j=0; j<pairArr.length;j++){
				Pair pair = pairArr[j];//.get(j);
				String activity = pair.getActivityName();
				if (activitiesToConsider.contains(activity))
					activitiesLocations.add(j);
				
				//set the results of the statistical values of elapsed time in the activities
				String traceAttributesValues = pair.traceAttributesValues;
				int activityCounter = pair.activityNumber;
				//get the statistic for the specified activity with the specified attribute with the specified counter (ordinal number of this activity in the trace)
				Long[] statistics = logActivitiesElapsedTimeStatisticResults.get(activity).get(traceAttributesValues).get(activityCounter);
				pair.calculateDeltaFromMean(statistics[0]);
				pair.calculateDeltaFromMedian(statistics[1]);
				
			}
			Integer[] locations = activitiesLocations.toArray(new Integer[activitiesLocations.size()]);
			activitiesLocations = null;
			
			
			int j;
			int j_skeep =0;
			int j_store=0;
			
			//loop on all the events that need to be consider 
			for (int jLoc=0;jLoc< locations.length;jLoc++){
								
				//for debug
				if (jLoc%1000 == 0)
					System.out.println(sdf.format(new Date()) + "   Loop on pairArr 1. jLoc="+jLoc);
				
				//the location of the considered activities in the All activities array
				j = locations[jLoc];				
				Pair pair = pairArr[j];
				String activity = pair.getActivityName();
				String attributesValue = pair.getAttributesValues();
				
				String key = sdf.format(pair.getActivityDate()) + ":" + attributesValue;
				String keyDateOnly = sdf.format(pair.getActivityDate());
				
				//if this key has been already calculated 
				//(the check is with the nextActivityOccurrenceMap1 hash but it could be any of the hashes that store the key / time)
				if (nextActivityOccurrenceMap1.containsKey(key)){
					//for debug
					if (j_skeep%1000 == 0)
						System.out.println(sdf.format(new Date()) + "   Loop on pairArr 1. j_skeep="+j_skeep);
					
					j_skeep++;
					continue;
				}
				
				if (!activitiesToConsider.contains(activity)){
					System.out.println(sdf.format(new Date()) + "   Very Very Strange....");
					continue;//not supposed to be here at all. consider to delete
				}
				
				//this is a TBD code
				//set the next pair activity to consider				
				Pair nextPair = null;
				{
				if (jLoc < locations.length-1)
					nextPair = pairArr[locations[jLoc+1]];
				else
					nextPair = pair;//dummy assign. It will not be used since this occur only in the last trace				
				if (pair.isInOverlapWindowsOfPrevPair){}
				}
				
				// build the time windows related to the current event
				Long beginTimeWindows = pair.getActivityDate().getTime();
				Long beginTimeWindowsNextPair = nextPair.getActivityDate().getTime();
				if (usePastTimeWindow){
					beginTimeWindows -= timeWindow;
					beginTimeWindowsNextPair -= timeWindow;
				}
				Long endTimeWindows = pair.getActivityDate().getTime();
				Long endTimeWindowsNextPair  = pair.getActivityDate().getTime();
				if (useFutureTimeWindow){
					endTimeWindows +=timeWindow;
					endTimeWindowsNextPair +=timeWindow;					
				}					
				
				// ******************only test *****************
				if ((pair.getActivityDate().getDate() == 23) && (pair.getActivityDate().getMonth() == 7)){
					int rrrr = 111;
					if (attributesValue.contains("EXPRT")){
						int eeeeee= 33;
					}
				}
				// look for events in the left side of the sliding window 
				int j1 =j-1;
				while(j1 >= 0 && pairArr[j1].getActivityDate().getTime() >= beginTimeWindows){
					Pair neighbour = pairArr[j1]; 
					
					//TDB code
					boolean isNeighbourInNextPairBeginTimeWindow = false;
					if (neighbour.getActivityDate().getTime() >= beginTimeWindowsNextPair)
						isNeighbourInNextPairBeginTimeWindow = true;
					
					//set the data from the neighbour into the currrent event
					setPairNeighbourData( pair,  attributesValue,  neighbour,nextPair ,isNeighbourInNextPairBeginTimeWindow);
					j1--;
				}
				
				//look for events in the right side of the sliding windows
				int j2 =j+1;
				while(j2 <= pairArr.length -1 && pairArr[j2].getActivityDate().getTime() <= endTimeWindows){
					Pair neighbour = pairArr[j2]; 		
					
					///TDB code
					boolean isNeighbourInNextPairEndTimeWindow = false;
					if (neighbour.getActivityDate().getTime() <= endTimeWindowsNextPair)
						isNeighbourInNextPairEndTimeWindow = true;
					
					setPairNeighbourData( pair,  attributesValue,  neighbour,nextPair, isNeighbourInNextPairEndTimeWindow);
					j2++;
				}
				
				if (!activitiesToConsider.contains(pair.getActivityName())){
					System.out.println(sdf.format(new Date()) + "   Very Very Strange..2..");
					continue; //not suppose to enter here
				}
						
				//for debug
				if (j_store%1000 == 0){
					System.out.println(sdf.format(new Date()) + "   Loop on pairArr 1. j_store="+j_store);
				}
				j_store++;
				/*
				occurrenceMap.put(pair.getActivityDate(),pair.getNeighboursData());
				resourceMap.put(pair.getActivityDate(),pair.getNeighboursResourceData());
				resourceGroupMap.put(pair.getActivityDate(),pair.getNeighboursResourceGroupData());
				*/	
				
				occurrenceMap.put(key,pair.getNeighboursData());
				/*
				//16_01_2019 - set the resource features to be VI
				resourceMap.put(keyDateOnly,pair.getNeighboursResourceData());
				resourceGroupMap.put(keyDateOnly,pair.getNeighboursResourceGroupData());
			    */
				//
				
				resourceMap.put(key,pair.getNeighboursResourceData());
				resourceGroupMap.put(key,pair.getNeighboursResourceGroupData());
			    
				
				pair.calculateFrequentNextActivity();				
				nextActivityOccurrenceMap1.put(key,pair.getNeighboursNextActivityData());				
				
				pair.calculateFrequentPrevActivity();
				prevActivityOccurrenceMap1.put(key,pair.getNeighboursPrevActivityData());
				
				pair.calculateMeanDelta();
				meanDeltaFromMeanTimeFromActivityStartOccurrenceMap1.put(key,pair.getNeighboursMeanDeltaFromMeanStartTimeData());
				medianDeltaFromMedianTimeFromActivityStartOccurrenceMap1.put(key,pair.getNeighboursMedianDeltaFromMeanStartTimeData());
				
				Long[] tracesElapsedTime= new Long[]{pair.tracesMeanDelta,pair.tracesMedianDelta};
				tracesMeanMeadinDeltaFromStart.put(key, tracesElapsedTime);
				
				pair.clearTempData();						
			}

		}
	
		private void setPairNeighbourData(Pair pair,String attributesValue,Pair neighbour,Pair nextPairToConsider, boolean isNeighbourInNextPairTimeWindow){
			// if the two events are from the same trace then return
			if (pair.getTraceIdentifier() == neighbour.getTraceIdentifier()){
				return;
			}	
			
			String neighbourActivity = neighbour.getActivityName();
			String neighbourAttributesValue = neighbour.getAttributesValues();
			String neighbourResource = neighbour.getResource();
			String neighbourResourceGroup = neighbour.getResourceGroup();
			
			//Do not delete
			//this code move temporary to the addTempNeighbours method in order to relate these feature as VA
			/*
			//count for each activity the total number of its occurrences
			Integer val =pair.getNeighboursData().get(neighbourActivity);
			if (val == null) val = 0;
			val++;									
			pair.getNeighboursData().put(neighbourActivity, val);
			
			//count for each resource the total number of its occurrences
			Integer resourceVal =pair.getNeighboursResourceData().get(neighbourResource);
			if (resourceVal == null) {resourceVal = 0;}
			resourceVal++;			
			pair.getNeighboursResourceData().put(neighbourResource, resourceVal);
			
			//count for each resource group the total number of its occurrences
			Integer resourceGroupVal =pair.getNeighboursResourceGroupData().get(neighbourResourceGroup);
			if (resourceGroupVal == null) {resourceGroupVal = 0;}
			resourceGroupVal++;			
			pair.getNeighboursResourceGroupData().put(neighbourResourceGroup, resourceGroupVal);

			*/
			
			
			// take into acount the neighbours data only if the with the same attributes of the current trace (as the user selected)		
			if (attributesValue.equals(neighbourAttributesValue)){
				String neighbourNextActivity = neighbour.nextEventName;
				String neighbourPrevActivity = neighbour.prevEventName;
				Integer neighbourDeltaFromMean = neighbour.deltaFromMean;
				Integer neighbourDeltaFromMedian = neighbour.deltaFromMedian; 
				long neighbourTraceIdentifier = neighbour.traceIdentifier;
				int neighbourActivityNumber = neighbour.activityNumber;
				int neighbourEventNumber = neighbour.eventNumber;
				//logActivitiesElapsedTimeStatisticResults.get(neighbour.getActivityName());
				pair.addTempNeighbours(neighbourActivity, neighbourPrevActivity, neighbourNextActivity,neighbourDeltaFromMean,neighbourDeltaFromMedian, neighbourTraceIdentifier, neighbourActivityNumber,neighbourEventNumber,neighbourResource,neighbourResourceGroup);
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

		
		private long mean(List<Long> list) {
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
		private long median(List<Long> list) {
			
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
		
		//String, Date
		private class Pair implements Comparable<Pair> {
		    private String activityName;
		    private Date activityDate;
		    //collection that holds for each concurrent trace the data about the delta from mean/median for each activity. 
		    //only the last ordinal activity is considered
		    private Map<Long, Map<String,Integer[]>> neighboursDeltaFromMeanAndMedianStartTimeTempData= new HashMap<>();
		    private Map<String,Double> neighboursMeanDeltaFromMeanStartTimeData = new HashMap<>();
		    private Map<String,Integer> neighboursMedianDeltaFromMeanStartTimeData = new HashMap<>();
		    
		    private Map<Long, Integer[]> tracesDeltaFromMeanStartTimeTempData= new HashMap<>();
		    //private Map<String, Long[]> tracesDeltaFromMeanStartTimeData= new HashMap<>();
		    long tracesMeanDelta;
	    	long tracesMedianDelta;
		    
		    private Map<String,Integer> neighboursData;
		    private Map<String,Integer> neighboursResourceData;
		    private Map<String,Integer> neighboursResourceGroupData;
		    
		    private Map<String,String> neighboursNextActivityData = new HashMap<>();
		    private Map<String,String> neighboursPrevActivityData = new HashMap<>();
		    //collection counter for each next activity
		    private Map<String,List<String>> neighboursNextActivityTempData = new HashMap<>();	    	
		    //collection counter for each prev activity 
		    private Map<String,List<String>> neighboursPrevActivityTempData = new HashMap<>();
		    private boolean isInOverlapWindowsOfPrevPair = false;
		    
		    String prevEventName;
		    String nextEventName;
		    Integer deltaFromMean;
		    Integer deltaFromMedian;
		    long elapsedTimeFromStart; 
		    private String resource;
		    private String resourceGroup;		    
		    private Map<String,Integer> resourceClustersData;
		    private String traceAttributesValues;
		    private long traceIdentifier;
		    private int eventNumber;	
		    private int activityNumber;
		    
		    public Pair(long traceIdentifier, String activityName, Date activityDate, int eventNumber, int activityNumber, String traceAttributesValues, String prevEventName,String nextEventName,long elapsedTimeFromStart,String resource,String resourceGroup){
		        this.activityName = activityName;
		        this.activityDate = activityDate;
		        this.eventNumber = eventNumber;
		        this.activityNumber = activityNumber;
		        this.prevEventName = prevEventName;
		        this.nextEventName =nextEventName;
		        this.elapsedTimeFromStart=elapsedTimeFromStart;
		        this.setResource(resource);
		        this.setResourceGroup(resourceGroup);
		        
		        this.traceAttributesValues=traceAttributesValues;
		        this.traceIdentifier = traceIdentifier;
		        
		        neighboursData = new HashMap<>(activitySet.size()+1);
		        neighboursResourceData = new HashMap<>(resources.size()+1);
		        neighboursResourceGroupData = new HashMap<>(resourceGroups.size()+1);
		        /*
		        //May be Removed
		        neighboursResourceGroupData = new HashMap<String,Integer[]>(resourceGroups.size()+1);
		        for (String group : resourceGroups){
		        	int numOfClusters = 0;
		        	try
		        	{numOfClusters = Integer.parseInt(group.replaceAll("\\D+",""));}
		        	catch(Exception e){}
		        	if (numOfClusters > 0)
		        		neighboursResourceGroupData.put(group, new Integer[numOfClusters]);
		        }
		        */
		    }
		  
		    @Override
		    public int compareTo(Pair  p1) {
		    	int comp =  activityDate.compareTo(p1.getActivityDate());
		    	if (comp == 0){
		    		comp = ((Long)traceIdentifier).compareTo(p1.getTraceIdentifier());
		    		if (comp == 0){
		    			comp = ((Integer)eventNumber).compareTo(p1.getEventNumber());
		    		}
		    	}
		    	return comp;
		    }
		    
		    public String getAttributesValues(){return traceAttributesValues;}
		    public String getActivityName(){ return activityName; }
		    public Date getActivityDate(){ return activityDate; }
		    public int getEventNumber(){ return eventNumber; }
		    public long getTraceIdentifier() {return traceIdentifier;}
		    
			//collection that holds for each activity the total number of its occurrences
		    public Map<String,Integer> getNeighboursData(){
		    	return neighboursData;
		    }
		    
		    //collection that holds for each resource the total number of its occurrences
		    public Map<String,Integer> getNeighboursResourceData(){
		    	return neighboursResourceData;
		    }
		    
			//collection the holds for each resource group the total number of its occurrences
		    public Map<String,Integer> getNeighboursResourceGroupData(){
		    	return neighboursResourceGroupData;
		    }		   
		    
		    public void clearTempData(){

		    	neighboursPrevActivityTempData.clear();
		    	neighboursNextActivityTempData.clear();
		    	neighboursDeltaFromMeanAndMedianStartTimeTempData.clear();		    	
			    tracesDeltaFromMeanStartTimeTempData.clear();
		    }
		    public void addTempNeighbours(String activityName, String prevActivity, String nextActivity,Integer neighbourDeltaFromMean,Integer neighbourDeltaFromMedian, long neighbourTraceIdentifier ,int activityNumber,int neighbourEventNumber,String neighbourResource,String neighbourResourceGroup){
		    	
				Integer val =getNeighboursData().get(activityName);
				if (val == null) val = 0;
				val++;									
				getNeighboursData().put(activityName, val);
				
				//count for each resource the total number of its occurrences
				Integer resourceVal =getNeighboursResourceData().get(neighbourResource);
				if (resourceVal == null) {resourceVal = 0;}
				resourceVal++;			
				getNeighboursResourceData().put(neighbourResource, resourceVal);
				
				//count for each resource group the total number of its occurrences
				Integer resourceGroupVal =getNeighboursResourceGroupData().get(neighbourResourceGroup);
				if (resourceGroupVal == null) {resourceGroupVal = 0;}
				resourceGroupVal++;			
				getNeighboursResourceGroupData().put(neighbourResourceGroup, resourceGroupVal);

				
		    	//build the collection set for each prev activity 
		    	List<String> prevList = neighboursPrevActivityTempData.get(activityName);
		    	if (prevList == null){
		    		prevList = new ArrayList<String>();
		    		neighboursPrevActivityTempData.put(activityName,prevList);
		    	}
		    	prevList.add(prevActivity);
		    	
		    	//build the collection set for each next activity
		    	List<String> nextList = neighboursNextActivityTempData.get(activityName);
		    	if (nextList == null){
		    		nextList = new ArrayList<String>();
		    		neighboursNextActivityTempData.put(activityName, nextList);
		    	}
		    	nextList.add(nextActivity);
		    	
	    		//set for each concurrent trace the delta from the mean elapsed time of each activity (last occurrence of each activity in the time window)
		    	Map<String,Integer[]> traceActivityData = neighboursDeltaFromMeanAndMedianStartTimeTempData.get(neighbourTraceIdentifier);
		    	if (traceActivityData == null){
		    		traceActivityData = new HashMap<String,Integer[]>();
		    		neighboursDeltaFromMeanAndMedianStartTimeTempData.put(neighbourTraceIdentifier, traceActivityData);
		    	}
		    	Integer[] activityCounterData = traceActivityData.get(activityName);
		    	if (activityCounterData == null){
		    		activityCounterData = new Integer[]{activityNumber,neighbourDeltaFromMean,neighbourDeltaFromMedian}; 
		    		traceActivityData.put(activityName, activityCounterData);
		    	}
		    	
		    	//if this activity number is higher in the ordinal number within the specified trace
	    		if (activityNumber > activityCounterData[0]){
	    			activityCounterData[0] =activityNumber;
	    			activityCounterData[1] = neighbourDeltaFromMean; 
	    			activityCounterData[2] = neighbourDeltaFromMedian;
	    		}
		    	
		    	//set for each trace in the concurrent windows the delta from the mean elapsed time of the last activity in the trace (that in the window)
		    	Integer[] traceLastActivity = tracesDeltaFromMeanStartTimeTempData.get(neighbourTraceIdentifier);
		    	if (traceLastActivity == null){
		    		traceLastActivity = new Integer[]{neighbourEventNumber,neighbourDeltaFromMean,neighbourDeltaFromMedian}; 
		    		tracesDeltaFromMeanStartTimeTempData.put(neighbourTraceIdentifier, traceLastActivity);
		    	}
		    	if (neighbourEventNumber > traceLastActivity[0]){
		    		traceLastActivity[0] =neighbourEventNumber;
		    		traceLastActivity[1] = neighbourDeltaFromMean;
		    		traceLastActivity[2] = neighbourDeltaFromMedian;
	    		}	
		    }
		   		
		    public void calculateDeltaFromMean(long mean){
		    	deltaFromMean = (int) (elapsedTimeFromStart - mean);
		    }
		    
		    public void calculateDeltaFromMedian(long median){
		    	deltaFromMedian =(int)( elapsedTimeFromStart - median);
		    }

		    public Map<String,Double> getNeighboursMeanDeltaFromMeanStartTimeData(){
		    	return neighboursMeanDeltaFromMeanStartTimeData;
		    }
		    
		    public Map<String,Integer> getNeighboursMedianDeltaFromMeanStartTimeData(){
		    	return neighboursMedianDeltaFromMeanStartTimeData;
		    }
		    
		    public Map<String,String> getNeighboursNextActivityData(){
		    	return neighboursNextActivityData;
		    }
		    
		    public Map<String,String> getNeighboursPrevActivityData(){
		    	return neighboursPrevActivityData;
		    }
		    
		    public void calculateMeanDelta(){ 
		    	
		    	//help collection to store for each activity the set of data about the delta for statistic
		    	HashMap<String,List<Integer>[]> activitiesDeltaList = new HashMap<String,List<Integer>[]>(); 
		    	
		    	//loop on all the concurrent traces 
		    	for (Long traceKey : neighboursDeltaFromMeanAndMedianStartTimeTempData.keySet()){
		    		//get the array of the delta data for each activity in the concurrent trace
		    		Map<String,Integer[]> traceActivityData = neighboursDeltaFromMeanAndMedianStartTimeTempData.get(traceKey);
		    		for (String activity: traceActivityData.keySet()){
		    			Integer[] activityLastOccuranceData = traceActivityData.get(activity);
		    			
		    			List<Integer>[] activityDeltas = activitiesDeltaList.get(activity);
		    			if (activityDeltas == null){
		    				activityDeltas = new List[2];
		    				activityDeltas[0] = new ArrayList<Integer>();
		    				activityDeltas[1] = new ArrayList<Integer>();
		    				activitiesDeltaList.put(activity, activityDeltas);
		    			}
		    			activityDeltas[0].add(activityLastOccuranceData[1]);//mean
		    			activityDeltas[1].add(activityLastOccuranceData[2]);//median
		    		}
		    	}
		    	
		    	for (String key : activitiesDeltaList.keySet()){
		    		List<Integer> deltaDataMean = activitiesDeltaList.get(key)[0];
		    		List<Integer> deltaDataMedian = activitiesDeltaList.get(key)[1];
		    		//Double meanDelta = calculateMean(deltaData);
		    		Double meanDelta = calculateMean(deltaDataMean);
		    		int medianDelta = calculateMedian(deltaDataMedian);
		    		//temporal change
		    		
		    		//set the 
		    		neighboursMeanDeltaFromMeanStartTimeData.put(key, meanDelta);
		    		neighboursMedianDeltaFromMeanStartTimeData.put(key, medianDelta);
		    	}
		    	
		    	ArrayList<Long> allTracMean = new ArrayList<Long>();
		    	ArrayList<Long> allTracMedian = new ArrayList<Long>();
		    	for (Long key : tracesDeltaFromMeanStartTimeTempData.keySet()){
		    		Integer[] traceDeltaData = tracesDeltaFromMeanStartTimeTempData.get(key);
		    		long deltaMean = traceDeltaData[1];		    		
		    		long deltaMedian = traceDeltaData[2];
		    		allTracMean.add(deltaMean);	
		    		allTracMedian.add(deltaMedian);
		    	}
		    	 tracesMeanDelta = mean(allTracMean);
		    	 tracesMedianDelta = median(allTracMedian);

		    }
		    public void calculateFrequentNextActivity(){	    	
		    	for (String key : neighboursNextActivityTempData.keySet()){
		    		List<String> nextTempData = neighboursNextActivityTempData.get(key);
		    		String frequentNextActivity = findPopular(nextTempData);
		    		neighboursNextActivityData.put(key, frequentNextActivity);
		    	}
		    }
		    
		    public void calculateFrequentPrevActivity(){	    	
		    	for (String key : neighboursPrevActivityTempData.keySet()){
		    		List<String> prevTempData = neighboursPrevActivityTempData.get(key);
		    		String frequentPrevActivity = findPopular(prevTempData);
		    		neighboursPrevActivityData.put(key, frequentPrevActivity);
		    	}
		    }
		    
		    public Double calculateMean(List<Integer> list) {

		        double total = 0;
		        for (Integer element : list) {
		            total += element.doubleValue();
		        }
		        double average = total / list.size();
		        return average;
		    }
		  
			private int calculateMedian(List<Integer> list) {
				
				if (list == null || list.size() == 0){
			    	return 0;
			    }
				Collections.sort(list);
			    int middle = list.size()/2;
			    if (list.size()%2 == 1) {
			        return list.get(middle);
			    } else {
			        return (int) ((list.get(middle-1) + list.get(middle)) / 2.0);
			    }
			}
			
		    
		    private String findPopular (List<String> list) {
		        Map<String, Integer> stringsCount = new HashMap<String, Integer>();
		        for(String string: list)
		        {
		            if (string.length() > 0) {
		                Integer count = stringsCount.get(string);
		                if(count == null) count = new Integer(0);
		                count++;
		                stringsCount.put(string,count);
		            }
		        }
		        Map.Entry<String,Integer> mostRepeated = null;
		        for(Map.Entry<String, Integer> e: stringsCount.entrySet())
		        {
		            if(mostRepeated == null || mostRepeated.getValue()<e.getValue())
		                mostRepeated = e;
		        }
		        try {
		            return mostRepeated.getKey();
		        } catch (NullPointerException e) {
		            //System.out.println("Cannot find most popular value at the List. Maybe all strings are empty");
		            return "None";
		        }

		    }

			private boolean isInOverlapWindowsOfPrevPair() {
				return isInOverlapWindowsOfPrevPair;
			}

			private void setInOverlapWindowsOfPrevPair(boolean isInOverlapWindowsOfPrevPair) {
				this.isInOverlapWindowsOfPrevPair = isInOverlapWindowsOfPrevPair;
			}

			String getResource() {
				return resource;
			}
			
			String getResourceGroup() {
				return resourceGroup;
			}
			/*
			//May be removed
			Map<String,Integer> getResourceGroups() {
				return resourceClustersData;
			}
*/

			void setResource(String resource) {
				this.resource = resource;
			}
			
			void setResourceGroup(String resourceGroup) {
				this.resourceGroup = resourceGroup;
			}
			/*
			//May be removed
			void setResourceClusters(Map<String,Integer> resourceClustersData) {
				this.resourceClustersData = resourceClustersData;
			}
			*/
			
		}			
	}
