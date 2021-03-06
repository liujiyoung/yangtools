/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.builder.api;

import java.util.List;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

/**
 * Basic implementation of SchemaNodeBuilder.
 */
public abstract class AbstractSchemaNodeBuilder extends AbstractBuilder implements SchemaNodeBuilder {
    protected final QName qname;
    protected SchemaPath schemaPath;

    protected AbstractSchemaNodeBuilder(final String moduleName, final int line, final QName qname) {
        super(moduleName, line);
        this.qname = qname;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((parentBuilder == null) ? 0 : parentBuilder.hashCode());
        result = prime * result + ((schemaPath == null) ? 0 : schemaPath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        AbstractSchemaNodeBuilder other = (AbstractSchemaNodeBuilder) obj;
        if (parentBuilder == null) {
            if (other.parentBuilder != null) {
                return false;
            }
        } else if (!parentBuilder.equals(other.parentBuilder)) {
            return false;
        }
        if (schemaPath == null) {
            if (other.schemaPath != null) {
                return false;
            }
        } else if (!schemaPath.equals(other.schemaPath)) {
            return false;
        }
        return true;
    }

    @Override
    public QName getQName() {
        return qname;
    }

    public void setUnknownNodes(List<UnknownSchemaNode> unknownNodes) {
        this.unknownNodes.addAll(unknownNodes);
    }

}
