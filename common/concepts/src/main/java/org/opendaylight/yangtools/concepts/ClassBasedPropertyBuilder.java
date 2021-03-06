/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.concepts;

public interface ClassBasedPropertyBuilder<P,T extends ClassBasedPropertyBuilder<P,T>> extends Builder<P> {

    /**
     * Sets a value of property uniquely identified by it's
     * class.
     * 
     * @param type Type of property to set
     * @param value Value of property 
     * @return
     */
    <V> T set(Class<V> type,V value);
    
    /**
     * Gets a value of property based on it's type.
     * 
     * @param type
     * @return
     */
    <V> V get(Class<V> type);

}
