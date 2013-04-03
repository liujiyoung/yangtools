/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.util;

import java.util.List;

import org.opendaylight.controller.model.api.type.RangeConstraint;
import org.opendaylight.controller.model.api.type.UnsignedIntegerTypeDefinition;
import org.opendaylight.controller.yang.common.QName;

/**
 * Implementation of Yang uint32 built-in type. <br>
 * uint32 represents integer values between 0 and 4294967295, inclusively. The
 * Java counterpart of Yang uint32 built-in type is {@link Long}.
 * 
 */
public class Uint32 extends AbstractUnsignedInteger {

    private static final QName name = BaseTypes.constructQName("uint32");
    private Long defaultValue = null;
    private static final String description = "uint32 represents integer values between 0 and 4294967295, inclusively.";

    public Uint32() {
        super(name, description, Short.MIN_VALUE, Short.MAX_VALUE, "");
    }

    public Uint32(final Long defaultValue) {
        super(name, description, Short.MIN_VALUE, Short.MAX_VALUE, "");
        this.defaultValue = defaultValue;
    }

    public Uint32(final List<RangeConstraint> rangeStatements,
            final String units, final Long defaultValue) {
        super(name, description, rangeStatements, units);
        this.defaultValue = defaultValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.controller.yang.model.api.TypeDefinition#getBaseType()
     */
    @Override
    public UnsignedIntegerTypeDefinition getBaseType() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.controller.yang.model.api.TypeDefinition#getDefaultValue
     * ()
     */
    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((defaultValue == null) ? 0 : defaultValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Uint32 other = (Uint32) obj;
        if (defaultValue == null) {
            if (other.defaultValue != null) {
                return false;
            }
        } else if (!defaultValue.equals(other.defaultValue)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Uint32 [defaultValue=");
        builder.append(defaultValue);
        builder.append(", AbstractInteger=");
        builder.append(super.toString());
        builder.append("]");
        return builder.toString();
    }
}