/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.concepts;

/**
 * Class representing a registration of an object. Such a registration is
 * a proper resource and should be cleaned up when no longer required, so
 * references to the object can be removed. This mechanism lies above the
 * usual Java reference mechanism, as the entity where the object is
 * registered may reside outside of the Java Virtual Machine.
 */
public interface Registration<T> extends AutoCloseable {
    /**
     * Return the object instance.
     *
     * @return Registered object.
     *
     * @deprecated This class is currently deprecated pending its rework for
     *             general-purpose registration. This rework will remove the
     *             getInstance() method, such that the registration is no
     *             longer tied to a particular object. Please use
     *             {@link ObjectRegistration} to ensure your code does not
     *             break when that happens.
     */
    @Deprecated
    T getInstance();

    /**
     * Unregisters the object. This operation is required not to invoke
     * blocking operations. Implementations which require interaction
     * with outside world must provide guarantees that any work is done
     * behind the scenes and the unregistration process looks as if it
     * has already succeeded once this method returns.
     */
    @Override
    void close() throws Exception;
}
