/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer;

import java.util.Set;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.SchemaUtils;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

/**
 * Abstract(base) serializer for ContainerNodes, serializes elements of type E.
 *
 * @param <E> type of serialized elements
 */
public abstract class ContainerNodeBaseSerializer<E> extends
        BaseDispatcherSerializer<E, ContainerNode, ContainerSchemaNode> {

    @Override
    protected final DataSchemaNode getSchemaForChild(ContainerSchemaNode schema,
            DataContainerChild<? extends InstanceIdentifier.PathArgument, ?> childNode) {
        return SchemaUtils.findSchemaForChild(schema, childNode.getNodeType());
    }

    @Override
    protected final AugmentationSchema getAugmentedCase(ContainerSchemaNode schema, AugmentationNode augmentationNode) {
        return SchemaUtils.findSchemaForAugment(schema, augmentationNode.getIdentifier().getPossibleChildNames());
    }

    @Override
    protected final Set<DataSchemaNode> getRealSchemasForAugment(ContainerSchemaNode schema, AugmentationSchema augmentationSchema) {
        return SchemaUtils.getRealSchemasForAugment((AugmentationTarget) schema, augmentationSchema);
    }
}
