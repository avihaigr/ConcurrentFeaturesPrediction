package org.processmining.prediction;

import java.awt.Dimension;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryBufferedImpl;
import org.deckfour.xes.model.XAttributable;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XEventImpl;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.Fun;
import org.mapdb.Serializer;
import org.processmining.framework.util.Pair;
import org.processmining.models.FunctionEstimator.AbstractDecisionTreeFunctionEstimator;
import org.processmining.models.FunctionEstimator.Type;
import org.processmining.models.guards.Expression;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.prediction.Augmentation.Augmentation;
import org.processmining.prediction.Augmentation.NumberExecutionCurrentActivity;
import org.processmining.prediction.Augmentation.TraceEndTime;
import org.processmining.prediction.Augmentation.TraceStartTime;
import org.processmining.xeslite.external.MapDBDatabaseImpl;
import org.processmining.xeslite.external.XFactoryExternalStore.MapDBDiskImpl;

import csplugins.id.mapping.ui.CheckComboBox;
import weka.classifiers.Evaluation;

class ObjectArraySerializer extends Serializer<Object[]>
{
	private static byte INTEGER=0;
	private static byte LONG=1;
	private static byte SHORT=2;
	private static byte FLOAT=3;
	private static byte STRING=4;
	private static byte DOUBLE=5;
	private static byte DATE=6;
	private static byte NULL=7;
	private int lengthArray;
	
	public ObjectArraySerializer(int lengthArray) {
		this.lengthArray=lengthArray;
	}
	
    @Override
    public void serialize(DataOutput out, Object[] value) throws IOException {
        for(Object c : value)
        {
        	if (c==null)
        		out.writeByte(NULL);
        	else if (c instanceof Integer)
        	{
        		out.writeByte(INTEGER);
        		out.writeInt((Integer) c);
        	}
        	else if (c instanceof Long)
        	{
        		out.writeByte(LONG);
        		out.writeLong((Long) c);
        	}
        	else if (c instanceof Short)
        	{
        		out.writeByte(SHORT);
        		out.writeShort((Short) c);
        	}
        	else if (c instanceof Float)
        	{
        		out.writeByte(FLOAT);
        		out.writeFloat((Float) c);
        	}
        	else if (c instanceof Double)
        	{
        		out.writeByte(DOUBLE);
        		out.writeDouble((Double) c);
        	}
        	else if (c instanceof Date)
        	{
        		out.writeByte(DATE);
        		out.writeLong(((Date) c).getTime());
        	} 
        	else if (c instanceof String)
        	{
        		out.writeByte(STRING);
        		out.writeUTF((String) c);
        	} 
        	else
        	{
        		System.err.println("The type "+c.getClass()+" is not supported");
        		out.writeByte(NULL);
        	}
        		
        }
    }

    @Override
    public Object[] deserialize(DataInput in, int available) throws IOException {
        Object[] ret = new Object[this.lengthArray];
        for(int i=0;i<lengthArray;i++)
        {
    		byte type = in.readByte();
    		if (type==NULL)
    			ret[i]=null;
        	if (type==INTEGER)
        		ret[i]=in.readInt();
        	else if (type==LONG)
        		ret[i]=in.readLong();
        	else if (type==SHORT)
        		ret[i]=in.readShort();
        	else if (type==FLOAT)
        		ret[i]=in.readFloat();
        	else if (type==DOUBLE)
        		ret[i]=in.readDouble();
        	else if (type==DATE)
        		ret[i]=new Date(in.readLong());
        	else if (type==STRING)
        		ret[i]=in.readUTF();

        }
        return ret;
    }

    @Override
    public boolean isTrusted() {
        return true;
    }

    @Override
    public boolean equals(Object[] a1, Object[] a2) {
        return Arrays.equals(a1,a2);
    }

    public int hashCode(Object[] bytes, int seed) {
        return Arrays.hashCode(bytes);
    }

    @Override
    public BTreeKeySerializer getBTreeKeySerializer(Comparator comparator) {
        if(comparator!=null && comparator!=Fun.COMPARATOR) {
            return super.getBTreeKeySerializer(comparator);
        }
        return BTreeKeySerializer.BASIC;
    }	
}


public class Predictor 
{

	private Map<String, Type> types;
	private Set<Object[]> instanceSet;
	private Augmentation[] augementationArray;
	private final XLog originalLog;
	private XLog baseLog;
	private Map<String, Set<String>> literalValues;
	protected AbstractDecisionTreeFunctionEstimator df=null;
	private Map<XTrace,Object[]> instanceOfATrace=new HashMap<XTrace,Object[]>();
	private boolean binarySplit=false;
	private float confidenceThreshold=-1;
	private int minNumInstancePerLeaf=2;
	private int numFoldErrorPruning=-1;
	private int numFoldCrossValidation=10;
	private boolean saveData=false;
	private boolean arffFile=false;
	private boolean considerFirstActivityOnly=false;
	private boolean unPruned=true;
	private int numIntervals=-1;
	private Collection<String> activitiesToConsider;
	private Augmentation outputAttribute;
	private DiscretizationInterval intervals[]=null;
	private final ArrayList<String> activityCollection=new ArrayList<String>();
	private final ArrayList<String> resourceCollection=new ArrayList<String>();
	private final ArrayList<String> resourceGruopCollection=new ArrayList<String>();
	
	private ResultReplay resReplay;
	//private ComplianceCheckerOutput compOut;
	private boolean isOutputAttributeChanged=true;
	private boolean isSetactivitiesToConsiderChanged;
	private DiscrMethod discrMethod=DiscrMethod.UNSET;
	private boolean hasLogBeenAugmented;
	private HashSet<String> timeIntervalAugmentations;
	private boolean regressionTree=false;
	private boolean hasAlgorithmChanged=true;
	private boolean discoveryParamChanged=true;
	private String attributeFilterName = null;
	private Object attributeFilterValue = null;
	
	private Date traceStartDate = null;
	private Date traceEndDate = null;
	
	// 1_1_2019
	private static Map<String, ArrayList<String>> caseAttributeValues = new HashMap<String, ArrayList<String>>();
	
	private ArrayList<String> originalLogAttributes=new ArrayList<String>();
	private ArrayList<String> originalLogTraceAttributes=new ArrayList<String>();
	private MapDBDatabaseImpl mapDBDatabaseImpl;
	public final static String CASE_ACTIVITY="Case";
	private static final String REMAINING = "Remaining Instances";
	
	public JComponent getPrefuseTreeVisualization()
	{
		return df.getPrefuseTreeVisualization();
	}

	public JComponent getNormalTreeVisualization()
	{
		return df.getVisualization();
	}
	
	
	public double classify(Map<String, Object> variableAssignment) throws Exception
	{
		return df.classify(variableAssignment);
	}
	

	@SuppressWarnings("deprecation")
	public Pair<String[], XLog[]> clusterLog(boolean onlyCorrectlyClassified, double maxDeviation) throws Exception
	{
		if (maxDeviation>1 && maxDeviation<=100)
			maxDeviation/=100.0;
		else if (maxDeviation>1 || maxDeviation<0)
			return null;	
		List<Pair<String, Expression>> listExpressions;
		if (df instanceof Leafable)
		{
			listExpressions = ((Leafable)df).getExpressionsAtLeaves();
		}
		else
		{
			Map<Object, Pair<Expression, Double>> values = df.getEstimation(null, false);
			listExpressions=new LinkedList<Pair<String,Expression>>();
			for(Entry<Object, Pair<Expression, Double>> entry : values.entrySet())
			{
				listExpressions.add(new Pair<String,Expression>(entry.getKey().toString(),entry.getValue().getFirst()));
			}
		}
		int size=listExpressions.size();
		if (onlyCorrectlyClassified)
			size++;
		String[] objectArray=new String[size];
		Expression[] exprArray=new Expression[size];
		XLog retValue[]=new XLog[size];


		int j=0;
		for(Pair<String, Expression> entry : listExpressions)
		{
			objectArray[j]=entry.getFirst();
			exprArray[j]=entry.getSecond();
			retValue[j++]=new XFactoryBufferedImpl().createLog();
		}
		if (onlyCorrectlyClassified)
		{
			objectArray[j]=REMAINING;
			exprArray[j]=null;
			retValue[j]=new XFactoryBufferedImpl().createLog();
		}
		for(XTrace trace : originalLog)
		{
			final Hashtable<String,Object> variableValues=new Hashtable<String, Object>();
			Object[] instance = instanceOfATrace.get(trace);
			
			for(int i=0;i<instance.length-1;i++)
				if (instance[i]!=null)
					variableValues.put(augementationArray[i].getAttributeName(), instance[i]);

			for(j=0;j<exprArray.length;j++)
			{
				if (exprArray[j]==null || exprArray[j].isTrue(variableValues))
				{
					boolean isOK=false;
					double valAsNumber=0;
					double secVal=-1;
					boolean isANumber=false;
					try
					{
						valAsNumber=Double.parseDouble(objectArray[j]);
						isANumber=true;
					}
					catch(NumberFormatException nfe) {}
					if (!isANumber)
					{
						try
						{
							String value[]=objectArray[j].replace('[', ' ').replace(']', ' ').replace(',', ' ').trim().split(" ");
									
							if (value.length==2)
							{
								valAsNumber=Double.parseDouble(value[0]);
								secVal=Double.parseDouble(value[1]);
							}
						}
						catch(NumberFormatException nfe) {}
					}
					if (onlyCorrectlyClassified && secVal>=valAsNumber && 
							variableValues.get(outputAttribute.getAttributeName()) instanceof Number)
					{
						double value=((Number)variableValues.get(outputAttribute.getAttributeName())).doubleValue();
						if (objectArray[j].indexOf(']')<0)
							isOK= (value>=valAsNumber && value< secVal);
						else
							isOK= (value>=valAsNumber && value <= secVal);
							
					}
					else if (onlyCorrectlyClassified && isANumber && 
							variableValues.get(outputAttribute.getAttributeName()) instanceof Number)
					{
						double actVal=((Number)variableValues.get(outputAttribute.getAttributeName())).doubleValue();
						if (Math.abs((actVal-valAsNumber)/actVal)<maxDeviation)
							isOK=true;
						else
							isOK=false;
								
					}
					
					if (!onlyCorrectlyClassified || objectArray[j].equals(REMAINING) || isOK || 
							objectArray[j].equals(variableValues.get(outputAttribute.getAttributeName())))
					{
						
						XTrace aNewTrace=new XFactoryBufferedImpl().createTrace(trace.getAttributes());
						
						for(XEvent event : trace)
							if (!CASE_ACTIVITY.equals(XConceptExtension.instance().extractName(event)))
								aNewTrace.add(new XFactoryBufferedImpl().createEvent(event.getAttributes()));
						retValue[j].add(aNewTrace);
						break;
					}
				}
			}
		}
		String[] description=new String[exprArray.length];
		for(int i=0;i<exprArray.length;i++)
		{
			if (objectArray[i]!=REMAINING)
				description[i]=objectArray[i].toString()+". Expression: "+exprArray[i];
			else
				description[i]=null;
		}
		return new Pair<String[],XLog[]>(description,retValue);
	}
	
	public Predictor(XLog log) 
	{
		baseLog = log;
		MapDBDiskImpl factory = new MapDBDiskImpl();
		originalLog=factory.createLog(log.getAttributes());
		HashSet<String> tempAttributeSet = new HashSet<String>();
		HashSet<String> tempResourceSet = new HashSet<String>();
		HashSet<String> tempResourceGroupSet = new HashSet<String>();
		
		HashSet<String> tempTraceAttributeSet = new HashSet<String>();
		HashSet<String> tempActivitySet = new HashSet<String>();
		
		for (XTrace trace : log)
		{
			XTrace newTrace = factory.createTrace(trace.getAttributes());
			tempTraceAttributeSet.addAll(trace.getAttributes().keySet());//
			
			for(String caseAttr:trace.getAttributes().keySet()){
				if (!caseAttr.startsWith("concept:") && !caseAttr.startsWith("time:") && !caseAttr.startsWith("resource:") && !caseAttr.startsWith("org:") && !caseAttr.startsWith("UNIT_GKEY"))
				{
					ArrayList<String> attrValues = caseAttributeValues.get(caseAttr);
					if (attrValues == null){
						attrValues = new ArrayList<String>();
						caseAttributeValues.put(caseAttr, attrValues);
					}
					XAttribute attributeObj = trace.getAttributes().get(caseAttr);
					if (attributeObj!= null){
						String attrVal =getAttributeValues(attributeObj).toString();
						if (!attrValues.contains(attrVal)){
							attrValues.add(attrVal);
						}
					}
				}
					
			}				
		
			
			XEvent startCaseEvent=new XEventImpl();
			startCaseEvent.setAttributes(trace.getAttributes());
			XConceptExtension.instance().assignName(startCaseEvent, CASE_ACTIVITY);
			Date initTimestamp = null;
			for(XEvent event : trace)
			{
				initTimestamp = XTimeExtension.instance().extractTimestamp(event);
				if (initTimestamp!=null)
				{
					XTimeExtension.instance().assignTimestamp(startCaseEvent, initTimestamp);
					break;
				}
			}
			if (initTimestamp==null)
				initTimestamp=new Date(0);
			XLifecycleExtension.instance().assignTransition(startCaseEvent, "start");
			XEvent endCaseEvent=new XEventImpl();
			XConceptExtension.instance().assignName(endCaseEvent, CASE_ACTIVITY);
			for(int i=trace.size()-1;i>=0;i--)
			{
				XEvent event = trace.get(i);
				Date timestamp = XTimeExtension.instance().extractTimestamp(event);
				if (timestamp!=null)
				{
					XTimeExtension.instance().assignTimestamp(endCaseEvent, timestamp);
					break;
				}
			}
			XLifecycleExtension.instance().assignTransition(endCaseEvent, "complete");
			newTrace.add(startCaseEvent);
			for(XEvent event : trace)
			{
				tempActivitySet.add(XConceptExtension.instance().extractName(event));
				String resource=XOrganizationalExtension.instance().extractResource(event);
				if (resource != null){
					tempResourceSet.add(resource);
				}
				String group=XOrganizationalExtension.instance().extractGroup(event);
				
				if (group != null){
					tempResourceGroupSet.add(group);
				}
				
				for(String attr : event.getAttributes().keySet())
				{
					if (!attr.startsWith("concept:") && !attr.startsWith("time:") && !attr.startsWith("resource:") && !attr.startsWith("org:"))
					{
						tempAttributeSet.add(attr);
					}					
				}

				XEvent newEvent=factory.createEvent(event.getAttributes());
				Date timestamp=XTimeExtension.instance().extractTimestamp(event);
				if (timestamp==null)
				{
					XTimeExtension.instance().assignTimestamp(newEvent, initTimestamp);
				}
				else
					initTimestamp=timestamp;
				newTrace.add(newEvent);
			}
			newTrace.add(endCaseEvent);
			originalLog.add(newTrace);
		}
		originalLogAttributes.addAll(tempAttributeSet);
		Collections.sort(originalLogAttributes);
		originalLogTraceAttributes.addAll(tempTraceAttributeSet);
		Collections.sort(originalLogTraceAttributes);
		activityCollection.addAll(tempActivitySet);
		Collections.sort(this.activityCollection);
		resourceCollection.addAll(tempResourceSet);
		Collections.sort(this.resourceCollection);
		
		resourceGruopCollection.addAll(tempResourceGroupSet);
		Collections.sort(this.resourceGruopCollection);
		augmentLog(new Augmentation[0],false,null);
		
	}
	
	public Predictor(XLog log, ResultReplay resReplay) {
		this(log);
		this.resReplay=resReplay;
	}
	

	public static String getName(XAttributable element) {
		XAttributeLiteral name = (XAttributeLiteral) element.getAttributes().get("concept:name");
		return name.getValue();
	}
	
	public boolean configureAugmentation(Augmentation[] augmentationCollection)
	{
		for(Augmentation aug : augmentationCollection)
		{
			aug.setLog(originalLog);
			String paramNames[]=aug.getParameterNames();
			for(int i=0;i<paramNames.length;i++)
			{
				String[] value;
				do
				{
					if (aug.getPossibleValuesForParameter(i)==null)
					{
						value=new String[1];
						value[0]=(String) JOptionPane.showInputDialog(null, "Please set the value for "+paramNames[i],
							"Attribute "+aug.getAttributeName(),JOptionPane.PLAIN_MESSAGE,null,null,aug.getDefaultValueForParameter(i)[0]);
					}
					else
						if (!aug.multipleValuesForParameter(i))
						{
							value=new String[1];
						value[0]=(String) JOptionPane.showInputDialog(null, "Please set the value for "+paramNames[i],
								"Attribute "+aug.getAttributeName(),JOptionPane.PLAIN_MESSAGE,null,aug.getPossibleValuesForParameter(i),aug.getDefaultValueForParameter(i)[0]);
						}
						else
						{
							CheckComboBox cbb=new CheckComboBox(aug.getPossibleValuesForParameter(i));
							Dimension dim=cbb.getPreferredSize();
							cbb.addSelectedItems(aug.getDefaultValueForParameter(i));
							dim.width*=2;
							cbb.setPreferredSize(dim);
							int yn=JOptionPane.showConfirmDialog(null, cbb,"Attribute "+aug.getAttributeName(),JOptionPane.YES_NO_OPTION);
							if (yn==JOptionPane.NO_OPTION)
								value=null;
							else
								value=(String[]) cbb.getSelectedItems().toArray(new String[0]);
							
						}
					if (value==null || value.length==0 || value[0]==null)
						return false;
				} while(!aug.setParameter(i, value));
			}
			if (aug.isTimeInterval())
			{
				timeIntervalAugmentations.add(aug.getAttributeName());
			}
		}
		return true;
	}

	public boolean augmentLog(Augmentation[] augmentationCollection,boolean useMapDB, TaskForProgressBar task) 
	{
		/*
		types=new HashMap<String, Type>();
		literalValues=new HashMap<String, Set<String>>();
*/
		
		types=new LinkedHashMap<String, Type>();
		literalValues=new LinkedHashMap<String, Set<String>>();

		try {
			if (mapDBDatabaseImpl!=null)
				mapDBDatabaseImpl.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!useMapDB)
		{
			//10_1_2019 in order to work with stable order --restored at(21_01_2019)-- temporary re-restored in order to get ordered results (28_01_2019)
			//instanceSet=new HashSet<Object[]>();
			instanceSet=new LinkedHashSet<Object[]>();
			
			mapDBDatabaseImpl=null;
		}
		else
		{
			try
			{
				mapDBDatabaseImpl = new MapDBDatabaseImpl();
				mapDBDatabaseImpl.createDB();
				DB db = mapDBDatabaseImpl.getDB();
				instanceSet=db.hashSet("instanceSet",new ObjectArraySerializer(augmentationCollection.length+1));
			}
			catch(IOException err)
			{
				err.printStackTrace();
				return false;
			}
		}
		
		long start=System.currentTimeMillis();
		//log=factory.createLog();
		this.augementationArray=augmentationCollection;
		timeIntervalAugmentations=new HashSet<String>();

		Object[] newInstance=null;
		int numTraces=originalLog.size();
		int elaboratedTrace=0;
		for(XTrace trace : originalLog)
		{
			for(Augmentation aug : augmentationCollection)
			{
				aug.reset(trace);
			}
			//XTrace newTrace=factory.createTrace(trace.getAttributes());
			for(XEvent event : trace)
			{
				//XEvent newEvent=factory.createEvent();
				//XConceptExtension.instance().assignName(newEvent, XConceptExtension.instance().extractName(event));
				newInstance = new Object[augementationArray.length+1];
				for(int i=0;i<augementationArray.length;i++)
				{
					try
					{
						augementationArray[i].setActivitiesToConsider(activitiesToConsider);
						newInstance[i]=augementationArray[i].returnAttribute(event);
						//if (types.get(augementationArray[i].getAttributeName())==null)// 16_01_2019 - want to see all the attribute in the arff.(this code may be restore) && newInstance[i]!=null)
						if (types.get(augementationArray[i].getAttributeName())==null && newInstance[i]!=null)
							types.put(augementationArray[i].getAttributeName(), generateDataElement(newInstance[i]));
						if (newInstance[i] instanceof String)
						{
							Set<String> valueSet=literalValues.get(augementationArray[i].getAttributeName());
							if (valueSet==null)
							{
								valueSet=new HashSet<String>();
								literalValues.put(augementationArray[i].getAttributeName(),valueSet);
							}
						//	newInstance.
							valueSet.add((String) newInstance[i]);
								
						}

					}
					catch(Exception err)
					{
						err.printStackTrace();
					}
				}
				String transition=XLifecycleExtension.instance().extractTransition(event);
				if (transition==null || transition.equalsIgnoreCase("complete"))
				{
					/*
					//int indexOfNumberExecutionCurrentActivityField=-1;
					int index=0;
					Integer numberExecutionCurrentActivity = null;
					for(;index<augementationArray.length;index++){
					//	indexOfNumberExecutionCurrentActivityField=index;
						if (augementationArray[index].getAttributeName().equals("NumExecution_Current_Activity")){
						numberExecutionCurrentActivity = (Integer) newInstance[index];
						break;
						}
					}
					*/
					//in case we want to consider the activity only in the first time it occure
				//	if (considerFirstActivityOnly && numberExecutionCurrentActivity != 1) 
				//		break;
					
					newInstance[newInstance.length-1]=XConceptExtension.instance().extractName(event);
					instanceSet.add(newInstance);
				}
			}
			
			//java.util.Arrays.
			instanceOfATrace.put(trace, newInstance);
			if (task!=null)
				task.myProgress((++elaboratedTrace*100)/numTraces);
		}
		//test
		for(Object[] instance : instanceSet)
		{
			String activityName = (String) instance[instance.length-1];
		}
		
		//types=extractAttributeInformation(log);
		//literalValues=getLiteralValuesMap(log);
		System.out.println(System.currentTimeMillis()-start);
		hasLogBeenAugmented=true;
		return true;
	}
	
	private static Map<String, Set<String>> getLiteralValuesMap(XLog log) {
		
		Map<String, Set<String>> retValue=new HashMap<String, Set<String>>();
		
		for(XTrace trace : log) {
			
			
			for(XEvent event : trace) {
				
				for(XAttribute attributeEntry : event.getAttributes().values()) {
					
					if (attributeEntry instanceof XAttributeLiteral) {
						
						String value = ((XAttributeLiteral)attributeEntry).getValue();
						String varName=attributeEntry.getKey();
						Set<String> literalValues = retValue.get(varName);

						if (literalValues == null) {
							literalValues = new HashSet<String>();
							retValue.put(varName, literalValues);
						}
						
						literalValues.add(value);
					}
				}
			}
		}
		return retValue;
	}

	public Map<String, Type> getTypes() {
		return Collections.unmodifiableMap(types);
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
	
	public void setRegression(boolean regressionTree)
	{
		if (this.regressionTree!=regressionTree)
		{
			hasAlgorithmChanged=true;
			this.regressionTree=regressionTree;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void makePrediction(TaskForProgressBar task) throws Exception
	{
		boolean toDiscover=false;
		if (isOutputAttributeChanged || isSetactivitiesToConsiderChanged || hasLogBeenAugmented || hasAlgorithmChanged)
		{
			//Set<String> varToConsider = types.keySet();
			
			Object[] outputValuesAsObjects=determineValues(outputAttribute);
			Map<String,Type> dfTypes=new HashMap<String, Type>(types);
			dfTypes.remove(outputAttribute);
			if (regressionTree)
				df=new RepTreePrediction(types, literalValues, outputAttribute.getAttributeName(), "predictor", 100,timeIntervalAugmentations);
			else
				df=new J48Prediction(types, literalValues, outputValuesAsObjects, "predictor", 100,timeIntervalAugmentations);

			int numInstances=instanceSet.size()*3;
			int instanceNumber=0;
			/*
			int indexOfNumberExecutionActivityAttribute = -1;
			for(int index=0;index<augementationArray.length;index++){
				if (augementationArray[index] instanceof  NumberExecutionCurrentActivity){
					indexOfNumberExecutionActivityAttribute = index;
				break;
				}
			}
			*/
			int indexOfNumberExecutionActivityAttribute = getAugmantatinFieldIndex(augementationArray, new NumberExecutionCurrentActivity());
			int indexOfTraceStartTime = getAugmantatinFieldIndex(augementationArray, new TraceStartTime());
			int indexOfTraceEndTime = getAugmantatinFieldIndex(augementationArray, new TraceEndTime());
			
			//find the index of the filter attribute
			int indexOfAttributeFilterName =0;
			boolean isFilterByTraceAttribute = false;		
			if (attributeFilterName != null){
				isFilterByTraceAttribute = true;
				for(;!augementationArray[indexOfAttributeFilterName].toString().equals(attributeFilterName);indexOfAttributeFilterName++);
			}
			
			Object filterAttributeValue;
			
			for(Object[] instance : instanceSet)
			{
				String activityName = (String) instance[instance.length-1];
				
				//find the number of execution of the activity in the trace
				int numberExecutionCurrentActivity = -1;
				if(indexOfNumberExecutionActivityAttribute > -1){
					numberExecutionCurrentActivity = (int) instance[indexOfNumberExecutionActivityAttribute];
				}
				//boolean considerFirstActivityFailedCheck = false; 
				boolean instanceNeedToBeConsider = true;
				//in case we want to consider the activity only in the first time it occur
				if (considerFirstActivityOnly && numberExecutionCurrentActivity != 1) 
					instanceNeedToBeConsider = false;
				
				if (traceStartDate != null){
					Date startDate = (Date) instance[indexOfTraceStartTime];
					if (startDate.getTime() < traceStartDate.getTime())
						instanceNeedToBeConsider = false;
				}
				
				if (traceEndDate != null){
					//Date endDate = (Date) instance[indexOfTraceEndTime];
					Date startDate = (Date) instance[indexOfTraceStartTime];
					if (startDate.getTime() > traceEndDate.getTime())
						instanceNeedToBeConsider = false;
				}
/*
				//in case we want to consider the activity only in the first time it occur
				if (considerFirstActivityOnly && numberExecutionCurrentActivity != 1) 
					considerFirstActivityFailedCheck = true;
				*/								
				if (activitiesToConsider.contains(activityName) && instanceNeedToBeConsider)
				{
					
					boolean instanceNeedToBeConsider1 = true;
					
					if (isFilterByTraceAttribute){
						//find the value of the filtered attribute
						filterAttributeValue = instance[indexOfAttributeFilterName];
						if (!filterAttributeValue.equals(attributeFilterValue)){
							instanceNeedToBeConsider1 = false;
							continue;
						}
					}
					
					HashMap<String,Object> variableValues=new HashMap<String, Object>();
					for(int i=0;i<instance.length-1;i++){
						if (augementationArray[i].isIncludInPrediction())
							variableValues.put(augementationArray[i].getAttributeName(), instance[i]);
					}
					Object outputValue=variableValues.get(outputAttribute.getAttributeName());
					if (outputValue!=null)
						if (!regressionTree && types.get(outputAttribute.getAttributeName())!= Type.BOOLEAN && types.get(outputAttribute.getAttributeName()) != Type.LITERAL)
						{
							if (outputValue instanceof Date)
								outputValue=((Date) outputValue).getTime();
							if (outputValue instanceof Number)
								outputValue=determineInterval((Number)outputValue,(DiscretizationInterval[]) outputValuesAsObjects,types.get(outputAttribute.getAttributeName()));
							else
								continue;
						}
					
					/*
					 * place to remove unwanted fields
					 */
					variableValues.remove(outputAttribute.getAttributeName());
					try
					{
						df.addInstance(variableValues, outputValue,1);
					}
					catch(NullPointerException err)
					{
						System.err.println("Instance "+df+" with outputValue "+outputValue+" skipped because of "+err.getMessage());
					}

				}
				if (task!=null)
					task.myProgress((++instanceNumber*100)/numInstances);

			}
			isOutputAttributeChanged=false;
			isSetactivitiesToConsiderChanged=false;
			hasLogBeenAugmented=false;
			hasAlgorithmChanged=false;
			toDiscover = true;
		}
		else
			System.out.println("Instances not created again!");
		
		//test
		
		if (arffFile){
		//	File f = new File("test123.arff");
		//	df.saveInstances(f);
			
			//discoveryParamChanged = false;
			//toDiscover = false;
		//	originalLog.
			Thread t1=new Thread(new Runnable() {
				
				public void run() {
					try {
						String timeStamp = new SimpleDateFormat("MM_dd_HH_mm_ss").format(new Date());
						File f = new File(timeStamp+".arff");
						df.saveInstances(f);						
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
				}	
			});
			t1.start();
			
			}
		
		
		if (discoveryParamChanged || toDiscover)
		{
			df.setBinarySplit(binarySplit);
			if (confidenceThreshold!=-1)
				df.setConfidenceFactor(confidenceThreshold);
			df.setMinNumObj(minNumInstancePerLeaf);
			if (numFoldErrorPruning!=-1)
				df.setNumFolds(numFoldErrorPruning);
			df.setNumFoldCrossValidation(numFoldCrossValidation);
			df.setSaveData(saveData);
			df.setUnpruned(unPruned);
			df.setCrossValidate(true);
			Thread t=new Thread(new Runnable() {
				
				public void run() {
					try {
						df.createAndSetTree(null);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			t.start();
			if (task!=null)
			{
				while(t.isAlive())
				{
					Thread.currentThread().sleep(3000);
					if (task.getProgress()<99)
						task.myProgress(task.getProgress()+1);
				}
				task.myProgress(100);
			}
			else
				t.join();

		}
		else
			System.out.println("New tree not created!");
		discoveryParamChanged=false;
	}
		
	private Object[] determineValues(Augmentation outputAttribute) {
		switch(types.get(outputAttribute.getAttributeName()))
		{
			case BOOLEAN :
				return new Boolean[] {false,true};
			case CONTINUOS :
			case DISCRETE :
			case TIMESTAMP :
				return intervals;
			case LITERAL :
				return literalValues.get(outputAttribute.getAttributeName()).toArray();
		}
		return null;
	}

	private Object determineInterval(Number outputValue, DiscretizationInterval[] outputValuesAsObjects, Type type) {

		for(DiscretizationInterval interval : outputValuesAsObjects)
		{
			boolean aux;
			if (interval.isSecExtremeIncluded())
				aux=(outputValue.doubleValue() <= interval.getSecond());
			else
				aux=(outputValue.doubleValue() < interval.getSecond());
			if (outputValue.doubleValue() >= interval.getFirst() && aux)
				return interval;
		}
		return null;
	}

	public DiscretizationInterval[] setOutputAttribute(Augmentation attribute, int numberIntervals, DiscrMethod method, boolean regressionTree)
	{
		//assert(attribute!=null);
		if (attribute!=outputAttribute || numberIntervals!=numIntervals || discrMethod!=method)
			isOutputAttributeChanged=true;
		this.outputAttribute=attribute;
		this.numIntervals=numberIntervals;
		this.discrMethod=method;
		if (attribute==null)
		{
			intervals=null;
			return null;
		}
		if (regressionTree)
			intervals=null;
		else
			switch(types.get(attribute.getAttributeName()))
			{
			case CONTINUOS :
			{
				if (method==DiscrMethod.EQUAL_WIDTH)
				{
					Pair<Double, Double> pair=determineSmallestGreatest(attribute.getAttributeName());
					double range=pair.getSecond()-pair.getFirst();
					double intervalSize=range/numberIntervals;
					intervals=new DiscretizationInterval[numberIntervals];
					intervals[0]=new DiscretizationInterval(Double.NEGATIVE_INFINITY,pair.getFirst()+intervalSize);
					for(int i=1;i<numberIntervals-1;i++)
						intervals[i]=new DiscretizationInterval(pair.getFirst()+i*intervalSize,pair.getFirst()+(i+1)*intervalSize);
					intervals[numberIntervals-1]=new DiscretizationInterval(pair.getFirst()+(numberIntervals-1)*intervalSize,Double.POSITIVE_INFINITY);
					return intervals;	
				}
				else
				{
					ArrayList<DiscretizationInterval> intervalList=new ArrayList<DiscretizationInterval>(numberIntervals);
					Pair<TreeMap<Double,Integer>,Integer> frequencyMap=determineFrequency(attribute);
					double frequencyPerInterval=frequencyMap.getSecond()/numberIntervals;
					int intervalFrequency=0;
					double from=frequencyMap.getFirst().firstKey();
					double to=from;
					for(Entry<Double, Integer> entry : frequencyMap.getFirst().entrySet())
					{
						if (intervalFrequency < frequencyPerInterval)
						{
							to=entry.getKey();
							intervalFrequency+=entry.getValue();
						}
						else
						{
							intervalList.add(new DiscretizationInterval(from, to));
							from=to;
							intervalFrequency=0;
						}
					}
					intervalList.add(new DiscretizationInterval(from,to));
					intervals=intervalList.toArray(new DiscretizationInterval[0]);
				}
				return(intervals);
			}
			case DISCRETE :
			case TIMESTAMP :
			{
				Pair<Double, Double> pair=determineSmallestGreatest(attribute.getAttributeName());
				double range=pair.getSecond()-pair.getFirst();
				double intervalSize=range/numberIntervals;
				if (intervalSize>=1)
				{
					if (method==DiscrMethod.EQUAL_WIDTH)
					{
						intervals=new DiscretizationInterval[numberIntervals];
						for(int i=0;i<numberIntervals-1;i++)
							intervals[i]=new DiscretizationInterval(pair.getFirst()+i*intervalSize,pair.getFirst()+(i+1)*intervalSize);
						intervals[numberIntervals-1]=new DiscretizationInterval(pair.getFirst()+(numberIntervals-1)*intervalSize,pair.getSecond(),true);
					}
					else
					{
						ArrayList<DiscretizationInterval> intervalList=new ArrayList<DiscretizationInterval>(numberIntervals);
						Pair<TreeMap<Double,Integer>,Integer> frequencyMap=determineFrequency(attribute);
						double frequencyPerInterval=frequencyMap.getSecond()/numberIntervals;
						int intervalFrequency=0;
						double from=frequencyMap.getFirst().firstEntry().getKey();
						double to=0;
						for(Entry<Double, Integer> entry : frequencyMap.getFirst().entrySet())
						{
							if (intervalFrequency < frequencyPerInterval)
							{
								to=entry.getKey();
								intervalFrequency+=entry.getValue();
							}
							else
							{
								intervalList.add(new DiscretizationInterval(from, to));
								from=to;
								intervalFrequency=0;
							}
						}
						if (intervalFrequency>0)
							intervalList.add(new DiscretizationInterval(from, to));							
						intervals=intervalList.toArray(new DiscretizationInterval[0]);
					}
				}
				else
				{

					if (pair.getSecond()!=Double.NEGATIVE_INFINITY)
					{
						intervals=new DiscretizationInterval[(int) (pair.getSecond()-pair.getFirst())+1];

						for(int i=0;i<intervals.length;i++)
							intervals[i]=new DiscretizationInterval(pair.getFirst()+i,pair.getFirst()+i,true);
					}
					else
					{
						intervals=new DiscretizationInterval[1];
						intervals[0]=new DiscretizationInterval(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);
					}
					
				}
				return intervals;
			}
			case LITERAL:
				intervals=null;
			default :
				break;	
			}
		return null;

	}

	private int getAugmantatinFieldIndex(Augmentation[] array, Augmentation aug){
		//find the index of the aug field within the array
		for(int index1=0;index1<array.length;index1++){
			if (array[index1].getClass().equals(aug.getClass())){
				return index1;
			}
		}
		return -1;
	}
	
	private Pair<TreeMap<Double, Integer>, Integer> determineFrequency(Augmentation outputAttribute) {
		
		//find the index of the output variable
		int index=0;
		for(;!augementationArray[index].equals(outputAttribute);index++);
		
		//find the index of the filter attribute
		int indexOfAttributeFilterName =0;
		boolean isFilterByTraceAttribute = false;		
		if (attributeFilterName != null){
			isFilterByTraceAttribute = true;
			for(;!augementationArray[indexOfAttributeFilterName].toString().equals(attributeFilterName);indexOfAttributeFilterName++);
		}
		
		/*
		//find the index of the num of execution field
		int indexOfNumberExecutionActivityAttribute = -1;
		for(int index1=0;index1<augementationArray.length;index1++){
			if (augementationArray[index1] instanceof  NumberExecutionCurrentActivity){
				indexOfNumberExecutionActivityAttribute = index1;
			break;
			}
		}
		*/
		int indexOfNumberExecutionActivityAttribute = getAugmantatinFieldIndex(augementationArray, new NumberExecutionCurrentActivity());
		int indexOfTraceStartTime = getAugmantatinFieldIndex(augementationArray, new TraceStartTime());
		int indexOfTraceEndTime = getAugmantatinFieldIndex(augementationArray, new TraceEndTime());
		
		int totalOccurrence=0;
		TreeMap<Double,Integer> retValue=new TreeMap<Double, Integer>();
		Object value;
		String atributeName;
		Object filterAttributeValue;
		int lastInd = augementationArray.length;

		for(Object[] instance : instanceSet)
		{
			value=instance[index];
			atributeName = (String) instance[lastInd];
			
			if (value!=null && value instanceof Number)
			{												
				if (activitiesToConsider.contains(atributeName)){
					
					boolean instanceNeedToBeConsider = true;
					
					if (isFilterByTraceAttribute){
						//find the value of the filtered attribute
						filterAttributeValue = instance[indexOfAttributeFilterName];
						if (!filterAttributeValue.equals(attributeFilterValue)){
							instanceNeedToBeConsider = false;
						}
					}
					
					//find the number of execution of the activity in the trace
					int numberExecutionCurrentActivity = -1;
					if(indexOfNumberExecutionActivityAttribute > -1){
						numberExecutionCurrentActivity = (int) instance[indexOfNumberExecutionActivityAttribute];
						
						//in case we want to consider the activity only in the first time it occur
						if (considerFirstActivityOnly && numberExecutionCurrentActivity != 1) 
							instanceNeedToBeConsider = false;
					}
					
					if (traceStartDate != null){
						Date startDate = (Date) instance[indexOfTraceStartTime];
						if (startDate.getTime() < traceStartDate.getTime())
							instanceNeedToBeConsider = false;
					}
					
					if (traceEndDate != null){
						//Date endDate = (Date) instance[indexOfTraceEndTime];
						Date startDate = (Date) instance[indexOfTraceStartTime];
						if (startDate.getTime() > traceEndDate.getTime())
							instanceNeedToBeConsider = false;
					}
					
					if (instanceNeedToBeConsider){
						Integer numOccurrences=retValue.get(((Number)value).doubleValue());
						if (numOccurrences==null) 
							numOccurrences=0;
						
						retValue.put(((Number) value).doubleValue(), numOccurrences+1);
						totalOccurrence++;
					}
				}
			}


		}
		return new Pair<TreeMap<Double,Integer>, Integer>(retValue, totalOccurrence);
	}

	private Pair<Double,Double> determineSmallestGreatest(String outputAttribute) {
		if (outputAttribute.endsWith("'"))
			outputAttribute=outputAttribute.substring(0, outputAttribute.length()-1);
		double smallest=Double.POSITIVE_INFINITY;
		double greatest=Double.NEGATIVE_INFINITY;
		int index=0;
		while(!augementationArray[index].getAttributeName().equals(outputAttribute))
			index++;
		Object value;


		for(Object[] instance : instanceSet)
		{
			value=instance[index];
			{
				if (value!=null && value instanceof Number)
				{
					smallest=Math.min(((Number) value).doubleValue(), smallest);
					greatest=Math.max(((Number) value).doubleValue(), greatest);
				}
				if (value!=null && value instanceof Date)
				{
					smallest=Math.min(((Date)value).getTime(), smallest);
					greatest=Math.max(((Date)value).getTime(), greatest);
				}
			} 
		}
		return new Pair<Double, Double>(smallest, greatest);
	}

	private Object getAttributeValues(XAttribute xAttrib) 
	{
		if (xAttrib instanceof XAttributeBoolean)
			return((XAttributeBoolean)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeContinuous)
			return((XAttributeContinuous)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeDiscrete)
			return((XAttributeDiscrete)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeTimestamp)
			return((XAttributeTimestamp)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeLiteral)
			return((XAttributeLiteral)xAttrib).getValue();

		return null;
	}

	public void setBinarySplit(boolean binarySplit) {
		if (this.binarySplit != binarySplit)
		{
			this.binarySplit=binarySplit;
			discoveryParamChanged=true;
		}
	}

	public void setConfidenceThreshold(float confidenceThreshold) {
		if (this.confidenceThreshold != confidenceThreshold)
		{
			this.confidenceThreshold = confidenceThreshold;
			discoveryParamChanged=true;
		}	
	}

	public void setMinNumInstancePerLeaf(int minNumInstancePerLeaf) {
		if (this.minNumInstancePerLeaf != minNumInstancePerLeaf)
		{
			this.minNumInstancePerLeaf = minNumInstancePerLeaf;
			discoveryParamChanged=true;
		}

	}

	public void setSaveData(boolean saveData) {
		if (this.saveData != saveData)
		{
			this.saveData=saveData;
			discoveryParamChanged=true;
		}
	}
	
	public void setArffFile(boolean arffFile) {
		if (this.arffFile != arffFile)
		{
			this.arffFile=arffFile;
			discoveryParamChanged=true;
		}
	}

	
	public void setConsiderFirstActivityOnly(boolean considerFirstActivityOnly) {
		if (this.considerFirstActivityOnly != considerFirstActivityOnly)
		{
			this.considerFirstActivityOnly=considerFirstActivityOnly;
			discoveryParamChanged=true;
		}
	}
	
	public void setAttributeFilterName(String attributeFilterName){
		this.attributeFilterName = attributeFilterName;
	}
	
	public void setAttributeFilterValue(Object attributeFilterValue){
		this.attributeFilterValue = attributeFilterValue;
	}
	
	public void setTraceStartTime(Date traceStartTime){
		this.traceStartDate = traceStartTime; 
	}
	
	public void setTraceEndTime(Date traceEndTime){
		this.traceEndDate =  traceEndTime;
	}	
	
	public void setUnPruned(boolean unPruned) {
		if(this.unPruned != unPruned)
		{
			this.unPruned = unPruned;
			discoveryParamChanged=true;
		}
	}

	public XLog getLog() {
		return baseLog;
	}
	
	public int getNumInstances()
	{
		return df.getNumInstances();
	}

	public void setNumFolds(int numFoldErrorPruning) {
		if(this.numFoldErrorPruning != numFoldErrorPruning)
		{
			this.numFoldErrorPruning = numFoldErrorPruning;
			discoveryParamChanged=true;
		}
	}
	
	
	public void setNumFoldCrossValidation(int numFoldCrossValidation) {
		if(this.numFoldCrossValidation != numFoldCrossValidation)
		{
			this.numFoldCrossValidation = numFoldCrossValidation;
			discoveryParamChanged=true;
		}
	}
	
	public Evaluation getEvaluation() { 
		return df.getEvaluation();
	}

	public Set<String> getLiteralValues(String attribute) {
		return Collections.unmodifiableSet(literalValues.get(attribute));
	}

	public Collection<String> getActivities() {
		return Collections.unmodifiableCollection(activityCollection);
	}

	
	
	public Collection<String> getResources() {
		return Collections.unmodifiableCollection(resourceCollection);
	}
	
	public Collection<String> getResourceGroups() {
		return Collections.unmodifiableCollection(resourceGruopCollection);
	}
	
	
	
	public XLog getOriginalLog() {
		return originalLog;
	}

	public ResultReplay getResReplay() {
		return resReplay;
	}
	
	private static Type generateDataElement(Object value) {

		if (value instanceof Boolean) {
			return Type.BOOLEAN;
		} else if (value instanceof Long || value instanceof Integer) {
			return Type.DISCRETE;
		} else if (value instanceof Double || value instanceof Float) {
			return Type.CONTINUOS;
		} else if (value instanceof Date) {
			return Type.TIMESTAMP;
		} else if (value instanceof String) {
			return Type.LITERAL;
		}
		
		return null;	
	}

	public boolean isRegressionTree() {
		return regressionTree;
	}

	public static Map<String, Type> extractAttributeInformation(XLog log) {
		HashMap<String, Type> retValue = new HashMap<String, Type>();
		for (XTrace trace : log) {
			
			for (XEvent event : trace)
			{
				for(XAttribute attr : event.getAttributes().values())
				{
					if (!attr.getKey().startsWith("concept:") && !attr.getKey().startsWith("time:") && !attr.getKey().startsWith("resource:"))
					{
						Type classType = generateDataElement(attr);
						if (classType != null)
							retValue.put(attr.getKey(), classType);
					}
				}
			}
			
		}
		/*
		 * return: Mapping of Attribute name to the Attribute Data Type in a HashMap<String, Type>
		 */
		return retValue;
	}
	
	public List<String> getAttributes() {
		return(originalLogAttributes);
	}
	
	public List<String> getTraceAttributes() {
		return(originalLogTraceAttributes);
	}
	
	public Map<String, ArrayList<String>> getCaseAttributeValues(){
		return caseAttributeValues;
	}
	




}
