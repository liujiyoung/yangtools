/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.parser.AugmentationNodeBaseParser;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.parser.NodeParserDispatcher;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.DomUtils;
import org.w3c.dom.Element;

import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedListMultimap;

final class AugmentationNodeDomParser extends AugmentationNodeBaseParser<Element> {

    private final NodeParserDispatcher<Element> dispatcher;

    AugmentationNodeDomParser(final NodeParserDispatcher<Element> dispatcher) {
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
    }

    @Override
    protected LinkedListMultimap<QName, Element> mapChildElements(Iterable<Element> elements) {
        return DomUtils.mapChildElements(elements);
    }

    @Override
    protected NodeParserDispatcher<Element> getDispatcher() {
        return dispatcher;
    }

}
