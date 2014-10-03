/*------------------------------------------------------------------
 * Copyright (c) 2014 Cisco Systems
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *------------------------------------------------------------------*/
/**
 * Contains classes representing MSCViewer view and data model.
 * 
 * <br>There are two type of models: Data Model and View Model.<br>
 * 
 * The Data Model consists primarily of {@link Entity}, {@link Event} and {@Interaction}
 * and it's build by loading a file through one of the loaders in the {@link com.cisco.mscviewer.io} 
 * package. <br>
 * 
 * The View Model represents a subset of a data model currently visualized in a frame. 
 * Typically the user opens in a view only a subset of the Entities. Each entity is
 * a column in the graphical view. columnns may be rearranged, resized, etc. 
 * This model maintains the information associated to this operation. <br>
 * 
 * While not yet supported, in the future we may allow multiple views (and hence
 * view models) for the same data model.
 *
 *
 * <ul>
 * <li> Data Model - this is the entire model
 * 
 * @author Roberto Attias
 * @since  Jan 2012

 */
package com.cisco.mscviewer.model;
