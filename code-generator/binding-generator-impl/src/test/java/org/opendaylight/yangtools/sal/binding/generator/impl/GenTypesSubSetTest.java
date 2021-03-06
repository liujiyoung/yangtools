/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.sal.binding.generator.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.yangtools.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.yangtools.sal.binding.model.api.Type;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangModelParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class GenTypesSubSetTest {

    private final static List<File> yangModels = new ArrayList<>();
    private final static URL yangModelsFolder = AugmentedTypeTest.class
            .getResource("/leafref-test-models");

    @BeforeClass
    public static void loadTestResources() throws URISyntaxException {
        final File augFolder = new File(yangModelsFolder.toURI());

        for (final File fileEntry : augFolder.listFiles()) {
            if (fileEntry.isFile()) {
                yangModels.add(fileEntry);
            }
        }
    }

    @Test
    public void genTypesFromSubsetOfTwoModulesTest() {
        final YangModelParser parser = new YangParserImpl();
        final Set<Module> modules = parser.parseYangModels(yangModels);
        final SchemaContext context = parser.resolveSchemaContext(modules);

        final Set<Module> toGenModules = new HashSet<>();
        for (final Module module : modules) {
            if (module.getName().equals("abstract-topology")) {
                toGenModules.add(module);
            } else if (module.getName().equals("ietf-interfaces")) {
                toGenModules.add(module);
            }
        }

        assertEquals("Set of to Generate Modules must contain 2 modules", 2,
                toGenModules.size());
        assertNotNull("Schema Context is null", context);
        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context, toGenModules);
        assertNotNull("genTypes is null", genTypes);
        assertFalse("genTypes is empty", genTypes.isEmpty());
        assertEquals("Expected Generated Types from provided sub set of " +
                "modules should be 23!", 23,
                genTypes.size());
    }

    @Test
    public void genTypesFromSubsetOfThreeModulesTest() {
        final YangModelParser parser = new YangParserImpl();
        final Set<Module> modules = parser.parseYangModels(yangModels);
        final SchemaContext context = parser.resolveSchemaContext(modules);

        final Set<Module> toGenModules = new HashSet<>();
        for (final Module module : modules) {
            if (module.getName().equals("abstract-topology")) {
                toGenModules.add(module);
            } else if (module.getName().equals("ietf-interfaces")) {
                toGenModules.add(module);
            } else if (module.getName().equals("iana-if-type")) {
                toGenModules.add(module);
            }
        }

        assertEquals("Set of to Generate Modules must contain 3 modules", 3,
                toGenModules.size());

        assertNotNull("Schema Context is null", context);
        final BindingGenerator bindingGen = new BindingGeneratorImpl();
        final List<Type> genTypes = bindingGen.generateTypes(context, toGenModules);
        assertNotNull("genTypes is null", genTypes);
        assertFalse("genTypes is empty", genTypes.isEmpty());
        assertEquals("Expected Generated Types", 24, genTypes.size());
    }
}
