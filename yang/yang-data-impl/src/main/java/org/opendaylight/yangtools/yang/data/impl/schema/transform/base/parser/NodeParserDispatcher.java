/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.impl.schema.transform.base.parser;

import java.util.List;

import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.ToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

import com.google.common.base.Preconditions;

/**
 *
 * Dispatches the parsing process of elements according to schema and returns the parsed Node.
 *
 * @param <E> type of elements parsed
 */
public interface NodeParserDispatcher<E> {

    DataContainerChild<?, ?> dispatchChildElement(Object schema, List<E> childNodes);

    /**
     * Abstract implementation that implements the dispatch conditions. Only requires parsers to be provided.
     * The same instance of parser can be provided in case it is immutable.
     */
    public static abstract class BaseNodeParserDispatcher<E> implements NodeParserDispatcher<E> {
        private final ToNormalizedNodeParserFactory<E> factory;

        protected BaseNodeParserDispatcher(final ToNormalizedNodeParserFactory<E> factory) {
            this.factory = Preconditions.checkNotNull(factory);
        }

        @Override
        public final DataContainerChild<?, ?> dispatchChildElement(Object schema, List<E> childNodes) {
            Preconditions.checkArgument(childNodes.isEmpty() == false);

            if (schema instanceof ContainerSchemaNode) {
                return factory.getContainerNodeParser().parse(childNodes, (ContainerSchemaNode) schema);
            } else if (schema instanceof LeafSchemaNode) {
                return factory.getLeafNodeParser().parse(childNodes, (LeafSchemaNode) schema);
            } else if (schema instanceof LeafListSchemaNode) {
                return factory.getLeafSetNodeParser().parse(childNodes, (LeafListSchemaNode) schema);
            } else if (schema instanceof ListSchemaNode) {
                return factory.getMapNodeParser().parse(childNodes, (ListSchemaNode) schema);
            } else if (schema instanceof ChoiceNode) {
                return factory.getChoiceNodeParser().parse(childNodes, (ChoiceNode) schema);
            } else if (schema instanceof AugmentationSchema) {
                return factory.getAugmentationNodeParser().parse(childNodes, (AugmentationSchema) schema);
            }

            throw new IllegalArgumentException("Unable to parse node, unknown schema type: " + schema.getClass());
        }
    }
}
