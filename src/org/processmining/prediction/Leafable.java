package org.processmining.prediction;

import java.util.List;

import org.processmining.framework.util.Pair;
import org.processmining.models.guards.Expression;

@SuppressWarnings("deprecation")
public interface Leafable {

	List<Pair<String,Expression>> getExpressionsAtLeaves();
	
	void balanceInstances();

}
