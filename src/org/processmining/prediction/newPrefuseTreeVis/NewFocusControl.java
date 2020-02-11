package org.processmining.prediction.newPrefuseTreeVis;

import java.awt.event.MouseEvent;
import java.util.Iterator;

import prefuse.Visualization;
import prefuse.controls.FocusControl;
import prefuse.data.tuple.TupleSet;
import prefuse.util.ui.UILib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

/**
 * Prefuse focus control - on double click, adds / removes node and children to
 * focus set.
 */
public class NewFocusControl extends FocusControl {
	int cclicks;
	public NewFocusControl(int clicks, String act) {
		super(clicks, act);
		cclicks = clicks;
	}

	public NewFocusControl(int clicks) {
		super(clicks);
		cclicks = clicks;
	}

	public NewFocusControl() {
		super(1);
		cclicks = 1;
	}
	protected boolean filterCheck(VisualItem item){
		try{
			String label = (String)item.get("name");
			label = label.split(" ")[0];
			return !(label.equals(">")||label.equals("<")||label.equals("<=")||label.equals("=")||label.equals(">=")||label.equals("!="));	
		} catch(Exception e){
			//System.out.println(e);
			return false;
		}

	}
	public void itemClicked(VisualItem item, MouseEvent e) {
		if(!filterCheck(item)) return;
/*		if(e.getClickCount() == 2){
			ArrayList<VisualItem> subtree = expandSubtree( (NodeItem) item, new ArrayList<VisualItem>() );
			MouseEvent newE = new MouseEvent(e.getComponent(), e.getID(),e.getWhen(),InputEvent.CTRL_DOWN_MASK,e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),e.getButton());
			for(VisualItem i : subtree){
				super.itemClicked(i, newE);
			}
		} else {
			super.itemClicked(item, e);
		}*/
        if ( UILib.isButtonPressed(e, button) &&
                e.getClickCount() == ccount )
        {
            Visualization vis = item.getVisualization();
            TupleSet ts = vis.getFocusGroup(Visualization.FOCUS_ITEMS);
            if(e.getClickCount()==2)
         	   addSubTree(((NodeItem) item),ts);
            else
            if ( ts.containsTuple(item) )
            {
         	   removeSubTree(((NodeItem) item),ts);
         	   ts.removeTuple(item);
         	   if (((NodeItem) item).getParent()!=null)
         		   ts.removeTuple(((NodeItem) item).getParent());
            }                	   
            else 
         	   ts.addTuple(item);
            vis.run(activity);
            
    }
}


private void addSubTree(NodeItem nodeItem, TupleSet ts) {
	Iterator<NodeItem> children = nodeItem.children();
	while (children.hasNext()) {
		addSubTree(children.next(),ts);
	}
	if (!ts.containsTuple(nodeItem))
		ts.addTuple(nodeItem);
}	

private void removeSubTree(NodeItem nodeItem, TupleSet ts) {
	Iterator<NodeItem> children = nodeItem.children();
	while (children.hasNext()) {
		removeSubTree(children.next(),ts);
	}
	ts.removeTuple(nodeItem);
}

}