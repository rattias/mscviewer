/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * @author Roberto Attias
 * @since  Jun 2011
 */
package com.cisco.mscviewer.model;

import java.util.ArrayList;
import java.util.Stack;

/**
 * An <code>Entity</code> represents a model element that can generate sequential events. 
 * For example, an <code>Entity</code> could be used to model a node, a process,
 * a thread, a state machine.<br>
 * 
 * Entities can be organized hierarchically. For example, an entity representing a node
 * might have a number of children entities representing processes. In turn, each
 * process might have children entities representing threads, etc.<br>
 * 
 * Entities are characterized by an ID and name. IDs of entites with the same parent 
 * have to be unique, while this constrains doesn't apply to names. a fully-qualified 
 * name/ID for an entity is defined as the slash-separated sequence of names/IDs of 
 * the entites along the path from root to the entity itself.<br>
 * 
 * An Entity can be a clock source, which means that children entites are sharing the
 * same clock with the parent.<br> 
 * 
 * Entites also have a description.
 * @author rattias
 */
public final class Entity {
    private final String id;
    private Entity parentEn;
    private String displayName;
    private final ArrayList<Entity> children;
    private int firstEventIndex, lastEventIndex;
    private final Stack<Entity> senderStack = new Stack<Entity>();
    private boolean isClockSource = false;
    private String description;

    /**
     * returns the fully-qualified ID of the parent to the entity
     * whose fully qualified name is passed as argument.
     * @param idPath
     * @return 
     */
    static String getParentId(String idPath) {
        int idx = idPath.lastIndexOf('/'); 
        return (idx>=0) ? idPath.substring(0, idx) : null;		
    }

    /**
     * returns last element from the slash-separated path passed as argument
     * @param idPath
     * @return 
     */
    static String getNameFromId(String idPath) {
        int idx = idPath.lastIndexOf('/'); 
        return (idx>=0) ? idPath.substring(idx+1) : idPath;		
    }

    /**
     * returns the fully qualified ID of the parent to this Entity
     * @return the fully qualified ID of the parent to this Entity
     */
    public String getParentId() {
        return getParentId(id);
    }

    /**
     * returns the fully qualified ID of this Entity.
     * @return the fully qualified ID of this Entity.
     */
    public String getId() {
        return id;
    }

    /**
     * returns <code>true</code> if the entity has events of its own,
     * <code>false</code> otherwise.
     * @return boolean
     */
    public boolean hasEvents() {
        return firstEventIndex > -1;
    }

    /**
     * Instantiates an Entity.
     * @param idPath the fully qualified ID of this entity. Must be unique.
     * @param parentEn the parent Entity
     * @param displayName the name to be used as alias for the ID when 
     * <code>getName()</code> is used. Doesn't need to be unique.
     */
    public Entity(String idPath, Entity parentEn, String displayName) {
        this.id = idPath;
        this.parentEn = parentEn;
        if (displayName != null)
            setName(displayName);
        else
            setName(getNameFromId(idPath));
        children = new ArrayList<Entity>();
        firstEventIndex = -1;
        lastEventIndex = -1;
        if (parentEn != null) {
            for(int i=0; i<parentEn.children.size(); i++) {
                if (parentEn.children.get(i).getName().compareTo(getName())>0) {
                    parentEn.children.add(i, this);
                    return;
                }
            }
            parentEn.children.add(this);
        }    
    }

    /**
     * returns the parent Entity to this Entity, or null if this is the
     * root entity.
     * @return 
     */
    public Entity getParentEntity() {
        return parentEn;
    }

    /**
     * returns <code>true</code> if the entity has no parent, <code>false</code>
     * otherwise.
     * @return
     */
    public boolean isRoot() {
        return parentEn != null;
    }
    
    /**
     * return the unqualified name of the Entity.
     * Note that IDs are unique, while names may not be.
     * @return the unqualified name of the Entity.
     */
    public String getName() {
        return displayName;
    }

    /**
     * return the fully-qualified name of the Entity.
     * Note that IDs are unique, while names may not be.
     * @return the fully-qualified name of the Entity.
     */
    public String getPath() {
        if (parentEn == null) {
            return getName();
        } else {
            return parentEn.getPath()+"/"+getName();
        }
    }

    /**
     * returns the same as <code>getPath()</code>
     * @return the same as <code>getPath()</code>
     */
    @Override
    public String toString() {
        return getPath();
    }

    /**
     * returns the <code>idx</code>-th child Entity of this entity
     * @param idx the index of the child entity to be returned
     * @return the <code>idx</code>-th child Entity of this entity
     */
    public Entity getChildAt(int idx) {
        return children.get(idx);		
    }

    /**
     * returns the number of children Entities to this Entity
     * @return the number of children Entities to this Entity
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * sets the index of the first event for this Entity in the
     * model
     * @param idx 
     */
    void setFirstEventIndex(int idx) {
        firstEventIndex = idx;
    }

    /**
     * returns the model index of the first event for this Entity
     * @return the index of the first event in for this Entity
     */
    public int getFirstEventIndex() {
        return firstEventIndex;
    }

    /**
     * sets the model index of the last event for this Entity
     * @param idx the model index
     */
    void setLastEventIndex(int idx) {
        lastEventIndex = idx;		
    }

    /**
     * returns the model index of the last event for this Entity
     * @return the model index of the last event for this Entity
     */
    public int getLastEventIndex() {
        return lastEventIndex;
    }

    public void pushSourceEntityForFromEvents(Entity en) {
        senderStack.push(en);
    }

    public void popSourceEntityForFromEvents() {
        if (!senderStack.isEmpty())
            senderStack.pop();
    }

    public Entity getSourceEntityForFromEvents() {
        if (senderStack.isEmpty() ) {
            return this;
        } else {
            return senderStack.peek();
        }
    }

//    public void setParent(Entity parentEntity) {
//        this.parentEn = parentEntity;
//
//    }

    /**
     * sets the unqualified name of this Entity
     * @param displayName 
     */
    public void setName(String displayName) {
        this.displayName = displayName;
    }


//    public void setClockSkew(long skewTime) {
//        if (skewTime<0)
//            throw new Error("Skew time should be >=0.");
//        Entity en = getClockSourceEntity();
//        if (en == this)
//            skew = skewTime;
//        else
//            en.setClockSkew(skewTime);
//    }
//
//    public long getClockSkew() {
//        Entity en = getClockSourceEntity();
//        if (en == this)
//            return skew;
//        else
//            return en.getClockSkew();
//    }

    /**
     * marks this Entity as a clock source.
     * An Entity is a clock source when descendant Entities are sharing
     * the same clock as this entity.
     * @param v <code>true</code> if the Entity is a clock source, <code>false</code> otherwise
     */
    public void setAsClockSource(boolean v) {
        isClockSource = v;
    }

    /**
     * returns <code>true</code> if this entity is a clock source
     * @return
     */
    public boolean isClockSource() {
        return isClockSource;
    }

    /**
     * returns the first clock source Entity traversing the hierarchy toward the
     * root.
     * @return this Entity, if the Entity is a clock source, or the first 
     * ancestor who is.
     */
    public Entity getClockSourceEntity() {
        // this should use a definition of 
        Entity en;
        for(en = this; en.getParentEntity() != null; en = en.getParentEntity())
            ;
        return en;	
    }

    /**
     * sets the description for this Entity.
     * @param descr the description
     */
    public void setDescription(String descr) {
        description = descr;
    }

    /**
     * returns the description for this Entity
     * @return the description for this Entity
     */
    public String getDescription() {
        return description;
    }
}
