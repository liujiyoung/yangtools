/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.sal.binding.generator.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.binding.generator.util.ReferencedTypeImpl;
import org.opendaylight.yangtools.binding.generator.util.Types;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.util.ClassLoaderUtils;
import org.opendaylight.yangtools.sal.binding.generator.util.CodeGenerationException;
import org.opendaylight.yangtools.sal.binding.model.api.ConcreteType;
import org.opendaylight.yangtools.sal.binding.model.api.Type;
import org.opendaylight.yangtools.sal.binding.model.api.type.builder.GeneratedTOBuilder;
import org.opendaylight.yangtools.sal.binding.model.api.type.builder.GeneratedTypeBuilder;
import org.opendaylight.yangtools.yang.binding.Augmentable;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.BindingCodec;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.codec.AugmentationCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.ChoiceCaseCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.ChoiceCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.opendaylight.yangtools.yang.data.impl.codec.DataContainerCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.DomCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.IdentifierCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.IdentityCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.InstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.ValueWithQName;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LazyGeneratedCodecRegistry implements //
        CodecRegistry, //
        SchemaContextListener, //
        GeneratorListener {

    private final static Logger LOG = LoggerFactory.getLogger(LazyGeneratedCodecRegistry.class);
    private final static LateMixinCodec NOT_READY_CODEC = new LateMixinCodec();

    private final InstanceIdentifierCodec instanceIdentifierCodec = new InstanceIdentifierCodecImpl(this);
    private final IdentityCompositeCodec identityRefCodec = new IdentityCompositeCodec();

    private TransformerGenerator generator;

    // Concrete class to codecs
    private static final Map<Class<?>, DataContainerCodec<?>> containerCodecs = Collections
            .synchronizedMap(new WeakHashMap<Class<?>, DataContainerCodec<?>>());
    private static final Map<Class<?>, IdentifierCodec<?>> identifierCodecs = Collections
            .synchronizedMap(new WeakHashMap<Class<?>, IdentifierCodec<?>>());
    private static final Map<Class<?>, ChoiceCodecImpl<?>> choiceCodecs = Collections
            .synchronizedMap(new WeakHashMap<Class<?>, ChoiceCodecImpl<?>>());
    private static final Map<Class<?>, ChoiceCaseCodecImpl<?>> caseCodecs = Collections
            .synchronizedMap(new WeakHashMap<Class<?>, ChoiceCaseCodecImpl<?>>());
    private static final Map<Class<?>, AugmentableCompositeCodec> augmentableCodecs = Collections
            .synchronizedMap(new WeakHashMap<Class<?>, AugmentableCompositeCodec>());
    private static final Map<Class<?>, AugmentationCodec<?>> augmentationCodecs = Collections
            .synchronizedMap(new WeakHashMap<Class<?>, AugmentationCodec<?>>());
    private static final Map<Class<?>, QName> identityQNames = Collections
            .synchronizedMap(new WeakHashMap<Class<?>, QName>());
    private static final Map<QName, Type> qnamesToIdentityMap = new ConcurrentHashMap<>();
    /** Binding type to encountered classes mapping **/
    @SuppressWarnings("rawtypes")
    private static final Map<Type, WeakReference<Class>> typeToClass = new ConcurrentHashMap<>();

    @SuppressWarnings("rawtypes")
    private static final ConcurrentMap<Type, ChoiceCaseCodecImpl> typeToCaseCodecs = new ConcurrentHashMap<>();

    private final CaseClassMapFacade classToCaseRawCodec = new CaseClassMapFacade();

    private static final Map<SchemaPath, GeneratedTypeBuilder> pathToType = new ConcurrentHashMap<>();
    private static final Map<List<QName>, Type> pathToInstantiatedType = new ConcurrentHashMap<>();
    private static final Map<Type, QName> typeToQname = new ConcurrentHashMap<>();
    private static final Map<AugmentationSchema, Type> augmentToType = new ConcurrentHashMap<>();

    private final SchemaLock lock;

    private SchemaContext currentSchema;

    private final ClassLoadingStrategy classLoadingStrategy;

    LazyGeneratedCodecRegistry(SchemaLock lock, ClassLoadingStrategy identityClassLoadingStrategy) {
        this.lock = Preconditions.checkNotNull(lock);
        this.classLoadingStrategy = identityClassLoadingStrategy;
    }

    public SchemaLock getLock() {
        return lock;
    }

    public TransformerGenerator getGenerator() {
        return generator;
    }

    public void setGenerator(TransformerGenerator generator) {
        this.generator = generator;
    }

    @Override
    public InstanceIdentifierCodec getInstanceIdentifierCodec() {
        return instanceIdentifierCodec;
    }

    @Override
    public <T extends Augmentation<?>> AugmentationCodec<T> getCodecForAugmentation(Class<T> object) {
        AugmentationCodec<T> codec = null;
        @SuppressWarnings("rawtypes")
        AugmentationCodec potentialCodec = augmentationCodecs.get(object);
        if (potentialCodec != null) {
            codec = potentialCodec;
        } else
            try {
                lock.waitForSchema(object);
                Class<? extends BindingCodec<Map<QName, Object>, Object>> augmentRawCodec = generator
                        .augmentationTransformerFor(object);
                BindingCodec<Map<QName, Object>, Object> rawCodec = augmentRawCodec.newInstance();
                codec = new AugmentationCodecWrapper<T>(rawCodec);
                augmentationCodecs.put(object, codec);
            } catch (InstantiationException e) {
                LOG.error("Can not instantiate raw augmentation codec {}", object.getSimpleName(), e);
            } catch (IllegalAccessException e) {
                LOG.debug(
                        "Run-time consistency issue: constructor {} is not available. This indicates either a code generation bug or a misconfiguration of JVM.",
                        object.getSimpleName(), e);
            }
        Class<? extends Augmentable<?>> objectSupertype = getAugmentableArgumentFrom(object);
        if (objectSupertype != null) {
            getAugmentableCodec(objectSupertype).addAugmentationCodec(object, codec);
        } else {
            LOG.warn("Could not find augmentation target for augmentation {}", object);
        }
        return codec;
    }

    @Override
    public QName getQNameForAugmentation(Class<?> cls) {
        Preconditions.checkArgument(Augmentation.class.isAssignableFrom(cls));
        return getCodecForAugmentation((Class<? extends Augmentation>) cls).getAugmentationQName();
    }

    private static Class<? extends Augmentable<?>> getAugmentableArgumentFrom(
            final Class<? extends Augmentation<?>> augmentation) {
        try {
            Class<? extends Augmentable<?>> ret = ClassLoaderUtils.withClassLoader(augmentation.getClassLoader(),
                    new Callable<Class<? extends Augmentable<?>>>() {
                        @Override
                        @SuppressWarnings("unchecked")
                        public Class<? extends Augmentable<?>> call() throws Exception {
                            for (java.lang.reflect.Type supertype : augmentation.getGenericInterfaces()) {
                                if (supertype instanceof ParameterizedType
                                        && Augmentation.class.equals(((ParameterizedType) supertype).getRawType())) {
                                    ParameterizedType augmentationGeneric = (ParameterizedType) supertype;
                                    return (Class<? extends Augmentable<?>>) augmentationGeneric
                                            .getActualTypeArguments()[0];
                                }
                            }
                            return null;
                        }
                    });
            return ret;
        } catch (Exception e) {
            LOG.debug("Could not find augmentable for {} using {}", augmentation, augmentation.getClassLoader(), e);
            return null;
        }
    }

    @Override
    public Class<?> getClassForPath(List<QName> names) {
        DataSchemaNode node = getSchemaNode(names);
        SchemaPath path = node.getPath();
        Type type = pathToType.get(path);
        if (type != null) {
            type = new ReferencedTypeImpl(type.getPackageName(), type.getName());
        } else {
            type = pathToInstantiatedType.get(names);
        }
        @SuppressWarnings("rawtypes")
        WeakReference<Class> weakRef = typeToClass.get(type);
        if (weakRef == null) {
            LOG.error("Could not find loaded class for path: {} and type: {}", path, type.getFullyQualifiedName());
        }
        return weakRef.get();
    }

    @Override
    public void putPathToClass(List<QName> names, Class<?> cls) {
        Type reference = Types.typeForClass(cls);
        pathToInstantiatedType.put(names, reference);
        bindingClassEncountered(cls);
    }

    @Override
    public IdentifierCodec<?> getKeyCodecForPath(List<QName> names) {
        @SuppressWarnings("unchecked")
        Class<? extends Identifiable<?>> cls = (Class<? extends Identifiable<?>>) getClassForPath(names);
        return getIdentifierCodecForIdentifiable(cls);
    }

    @Override
    public <T extends DataContainer> DataContainerCodec<T> getCodecForDataObject(Class<T> type) {
        @SuppressWarnings("unchecked")
        DataContainerCodec<T> ret = (DataContainerCodec<T>) containerCodecs.get(type);
        if (ret != null) {
            return ret;
        }
        Class<? extends BindingCodec<Map<QName, Object>, Object>> newType = generator.transformerFor(type);
        BindingCodec<Map<QName, Object>, Object> rawCodec = newInstanceOf(newType);
        DataContainerCodecImpl<T> newWrapper = new DataContainerCodecImpl<>(rawCodec);
        containerCodecs.put(type, newWrapper);
        return newWrapper;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void bindingClassEncountered(Class cls) {

        ConcreteType typeRef = Types.typeForClass(cls);
        if (typeToClass.containsKey(typeRef)) {
            return;
        }
        LOG.trace("Binding Class {} encountered.", cls);
        WeakReference<Class> weakRef = new WeakReference<>(cls);
        typeToClass.put(typeRef, weakRef);
        if (Augmentation.class.isAssignableFrom(cls)) {

        } else if (DataObject.class.isAssignableFrom(cls)) {
            @SuppressWarnings({ "unchecked", "unused" })
            Object cdc = getCodecForDataObject((Class<? extends DataObject>) cls);
        }
    }

    @Override
    public void onClassProcessed(Class<?> cls) {
        ConcreteType typeRef = Types.typeForClass(cls);
        if (typeToClass.containsKey(typeRef)) {
            return;
        }
        LOG.trace("Binding Class {} encountered.", cls);
        @SuppressWarnings("rawtypes")
        WeakReference<Class> weakRef = new WeakReference<Class>(cls);
        typeToClass.put(typeRef, weakRef);
    }

    private DataSchemaNode getSchemaNode(List<QName> path) {
        QName firstNode = path.get(0);
        DataNodeContainer previous = currentSchema.findModuleByNamespaceAndRevision(firstNode.getNamespace(),
                firstNode.getRevision());
        Iterator<QName> iterator = path.iterator();
        while (iterator.hasNext()) {
            QName arg = iterator.next();
            DataSchemaNode currentNode = previous.getDataChildByName(arg);
            if (currentNode == null && previous instanceof DataNodeContainer) {
                currentNode = searchInChoices(previous, arg);
            }
            if (currentNode instanceof DataNodeContainer) {
                previous = (DataNodeContainer) currentNode;
            } else if (currentNode instanceof LeafSchemaNode || currentNode instanceof LeafListSchemaNode) {
                Preconditions.checkState(!iterator.hasNext(), "Path tries to nest inside leaf node.");
                return currentNode;
            }
        }
        return (DataSchemaNode) previous;
    }

    private DataSchemaNode searchInChoices(DataNodeContainer node, QName arg) {
        Set<DataSchemaNode> children = node.getChildNodes();
        for (DataSchemaNode child : children) {
            if (child instanceof ChoiceNode) {
                ChoiceNode choiceNode = (ChoiceNode) child;
                DataSchemaNode potential = searchInCases(choiceNode, arg);
                if (potential != null) {
                    return potential;
                }
            }
        }
        return null;
    }

    private DataSchemaNode searchInCases(ChoiceNode choiceNode, QName arg) {
        Set<ChoiceCaseNode> cases = choiceNode.getCases();
        for (ChoiceCaseNode caseNode : cases) {
            DataSchemaNode node = caseNode.getDataChildByName(arg);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    private <T> T newInstanceOf(Class<?> newType) {
        try {
            @SuppressWarnings("unchecked")
            T ret = (T) newType.newInstance();
            return ret;
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T extends Identifiable<?>> IdentifierCodec<?> getIdentifierCodecForIdentifiable(Class<T> type) {
        IdentifierCodec<?> obj = identifierCodecs.get(type);
        if (obj != null) {
            return obj;
        }
        Class<? extends BindingCodec<Map<QName, Object>, Object>> newCodec = generator
                .keyTransformerForIdentifiable(type);
        BindingCodec<Map<QName, Object>, Object> newInstance;
        newInstance = newInstanceOf(newCodec);
        IdentifierCodecImpl<?> newWrapper = new IdentifierCodecImpl<>(newInstance);
        identifierCodecs.put(type, newWrapper);
        return newWrapper;
    }

    @Override
    public IdentityCodec<?> getIdentityCodec() {
        return identityRefCodec;
    }

    @Override
    public <T extends BaseIdentity> IdentityCodec<T> getCodecForIdentity(Class<T> codec) {
        bindingClassEncountered(codec);
        return identityRefCodec;
    }

    @Override
    public void onCodecCreated(Class<?> cls) {
        CodecMapping.setIdentifierCodec(cls, instanceIdentifierCodec);
        CodecMapping.setIdentityRefCodec(cls, identityRefCodec);
    }

    @Override
    public <T extends Identifier<?>> IdentifierCodec<T> getCodecForIdentifier(Class<T> object) {
        @SuppressWarnings("unchecked")
        IdentifierCodec<T> obj = (IdentifierCodec<T>) identifierCodecs.get(object);
        if (obj != null) {
            return obj;
        }
        Class<? extends BindingCodec<Map<QName, Object>, Object>> newCodec = generator
                .keyTransformerForIdentifier(object);
        BindingCodec<Map<QName, Object>, Object> newInstance;
        newInstance = newInstanceOf(newCodec);
        IdentifierCodecImpl<T> newWrapper = new IdentifierCodecImpl<>(newInstance);
        identifierCodecs.put(object, newWrapper);
        return newWrapper;
    }

    @SuppressWarnings("rawtypes")
    public ChoiceCaseCodecImpl getCaseCodecFor(Class caseClass) {
        ChoiceCaseCodecImpl<?> potential = caseCodecs.get(caseClass);
        if (potential != null) {
            return potential;
        }
        ConcreteType typeref = Types.typeForClass(caseClass);
        ChoiceCaseCodecImpl caseCodec = typeToCaseCodecs.get(typeref);

        Preconditions.checkState(caseCodec != null, "Case Codec was not created proactivelly for %s",
                caseClass.getName());
        Preconditions.checkState(caseCodec.getSchema() != null, "Case schema is not available for %s",
                caseClass.getName());
        @SuppressWarnings("unchecked")
        Class<? extends BindingCodec> newCodec = generator.caseCodecFor(caseClass, caseCodec.getSchema());
        BindingCodec newInstance = newInstanceOf(newCodec);
        caseCodec.setDelegate(newInstance);
        caseCodecs.put(caseClass, caseCodec);

        for (Entry<Class<?>, ChoiceCodecImpl<?>> choice : choiceCodecs.entrySet()) {
            if (choice.getKey().isAssignableFrom(caseClass)) {
                choice.getValue().cases.put(caseClass, caseCodec);
            }
        }
        return caseCodec;
    }

    public void onModuleContextAdded(SchemaContext schemaContext, Module module, ModuleContext context) {
        pathToType.putAll(context.getChildNodes());
        augmentToType.putAll(context.getTypeToAugmentation().inverse());
        qnamesToIdentityMap.putAll(context.getIdentities());
        for (Entry<QName, GeneratedTOBuilder> identity : context.getIdentities().entrySet()) {
            typeToQname.put(
                    new ReferencedTypeImpl(identity.getValue().getPackageName(), identity.getValue().getName()),
                    identity.getKey());
        }
        captureCases(context.getCases(), schemaContext);
    }

    private void captureCases(Map<SchemaPath, GeneratedTypeBuilder> cases, SchemaContext module) {
        for (Entry<SchemaPath, GeneratedTypeBuilder> caseNode : cases.entrySet()) {
            ReferencedTypeImpl typeref = new ReferencedTypeImpl(caseNode.getValue().getPackageName(), caseNode
                    .getValue().getName());

            pathToType.put(caseNode.getKey(), caseNode.getValue());

            ChoiceCaseNode node = (ChoiceCaseNode) SchemaContextUtil.findDataSchemaNode(module, caseNode.getKey());

            if (node == null) {
                LOG.warn("Failed to find YANG SchemaNode for {}, with path {} was not found in context.",
                        typeref.getFullyQualifiedName(), caseNode.getKey());
                @SuppressWarnings("rawtypes")
                ChoiceCaseCodecImpl value = new ChoiceCaseCodecImpl();
                typeToCaseCodecs.putIfAbsent(typeref, value);
                continue;
            }
            @SuppressWarnings("rawtypes")
            ChoiceCaseCodecImpl value = new ChoiceCaseCodecImpl(node);
            typeToCaseCodecs.putIfAbsent(typeref, value);
        }
    }

    @Override
    public void onGlobalContextUpdated(SchemaContext context) {
        currentSchema = context;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void onChoiceCodecCreated(Class<?> choiceClass,
            Class<? extends BindingCodec<Map<QName, Object>, Object>> choiceCodec, ChoiceNode schema) {
        ChoiceCodec<?> oldCodec = choiceCodecs.get(choiceClass);
        Preconditions.checkState(oldCodec == null);
        BindingCodec<Map<QName, Object>, Object> delegate = newInstanceOf(choiceCodec);
        ChoiceCodecImpl<?> newCodec = new ChoiceCodecImpl(delegate);
        choiceCodecs.put(choiceClass, newCodec);
        CodecMapping.setClassToCaseMap(choiceCodec, classToCaseRawCodec);
        CodecMapping.setCompositeNodeToCaseMap(choiceCodec, newCodec.getCompositeToCase());

        tryToCreateCasesCodecs(schema);

    }

    private void tryToCreateCasesCodecs(ChoiceNode schema) {
        for (ChoiceCaseNode choiceCase : schema.getCases()) {
            ChoiceCaseNode caseNode = choiceCase;
            if (caseNode.isAddedByUses()) {
                DataSchemaNode origCaseNode = SchemaContextUtil.findOriginal(caseNode, currentSchema);
                if (origCaseNode instanceof ChoiceCaseNode) {
                    caseNode = (ChoiceCaseNode) origCaseNode;
                }
            }
            SchemaPath path = caseNode.getPath();

            GeneratedTypeBuilder type;
            if (path != null && (type = pathToType.get(path)) != null) {
                ReferencedTypeImpl typeref = new ReferencedTypeImpl(type.getPackageName(), type.getName());
                ChoiceCaseCodecImpl partialCodec = typeToCaseCodecs.get(typeref);
                if (partialCodec.getSchema() == null) {
                    partialCodec.setSchema(caseNode);
                }
                try {
                    Class<?> caseClass = classLoadingStrategy.loadClass(type.getFullyQualifiedName());
                    getCaseCodecFor(caseClass);
                } catch (ClassNotFoundException e) {
                    LOG.trace("Could not proactivelly create case codec for {}", type, e);
                }
            }
        }

    }

    @Override
    public void onValueCodecCreated(Class<?> valueClass, Class<?> valueCodec) {
    }

    @Override
    public void onCaseCodecCreated(Class<?> choiceClass,
            Class<? extends BindingCodec<Map<QName, Object>, Object>> choiceCodec) {
    }

    @Override
    public void onDataContainerCodecCreated(Class<?> dataClass, Class<? extends BindingCodec<?, ?>> dataCodec) {
        if (Augmentable.class.isAssignableFrom(dataClass)) {
            AugmentableCompositeCodec augmentableCodec = getAugmentableCodec(dataClass);
            CodecMapping.setAugmentationCodec(dataCodec, augmentableCodec);
        }

    }

    public AugmentableCompositeCodec getAugmentableCodec(Class<?> dataClass) {
        AugmentableCompositeCodec ret = augmentableCodecs.get(dataClass);
        if (ret != null) {
            return ret;
        }
        ret = new AugmentableCompositeCodec(dataClass);
        augmentableCodecs.put(dataClass, ret);

        Map<Type, SchemaNode> typeToSchemaNode = generator.getTypeToSchemaNode();
        Type refType = new ReferencedTypeImpl(dataClass.getPackage().getName(), dataClass.getSimpleName());
        SchemaNode node = typeToSchemaNode.get(refType);
        tryToLoadAugmentations(node);

        return ret;
    }

    private void tryToLoadAugmentations(SchemaNode schemaNode) {
        if (schemaNode instanceof AugmentationTarget) {
            AugmentationTarget augmentationTarget = (AugmentationTarget) schemaNode;
            Set<AugmentationSchema> augments = augmentationTarget.getAvailableAugmentations();
            Set<Type> augmentTypes = new HashSet<>();
            if (augments != null) {
                for (AugmentationSchema augment : augments) {
                    Type augmentType = augmentToType.get(augment);
                    if (augmentType == null) {
                        LOG.warn("Failed to find type for augmentation of {}", augment);
                    } else {
                        augmentTypes.add(augmentType);
                    }
                }
                for (Type augmentType : augmentTypes) {
                    Class<? extends Augmentation<?>> clazz = null;
                    try {
                        clazz = (Class<? extends Augmentation<?>>) classLoadingStrategy.loadClass(augmentType);
                        getCodecForAugmentation(clazz);
                    } catch (ClassNotFoundException e) {
                        LOG.warn("Failed to find class for augmentation of {}, reason: {}", augmentType, e.toString());
                    } catch (CodeGenerationException e) {
                        LOG.warn("Failed to proactively generate augment coded for {}, reason: {}",  augmentType, e.toString());
                    }
                }
            }
        }

        if (schemaNode instanceof DataNodeContainer) {
            Set<DataSchemaNode> childNodes = ((DataNodeContainer) schemaNode).getChildNodes();
            for (DataSchemaNode child : childNodes) {
                tryToLoadAugmentations(child);
            }
        }
    }

    private static abstract class IntermediateCodec<T> implements //
            DomCodec<T>, Delegator<BindingCodec<Map<QName, Object>, Object>> {

        private final BindingCodec<Map<QName, Object>, Object> delegate;

        @Override
        public BindingCodec<Map<QName, Object>, Object> getDelegate() {
            return delegate;
        }

        public IntermediateCodec(BindingCodec<Map<QName, Object>, Object> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Node<?> serialize(ValueWithQName<T> input) {
            Map<QName, Object> intermediateOutput = delegate.serialize(input);
            return IntermediateMapping.toNode(intermediateOutput);
        }
    }

    private static class IdentifierCodecImpl<T extends Identifier<?>> //
            extends IntermediateCodec<T> //
            implements IdentifierCodec<T> {

        public IdentifierCodecImpl(BindingCodec<Map<QName, Object>, Object> delegate) {
            super(delegate);
        }

        @Override
        public ValueWithQName<T> deserialize(Node<?> input) {
            QName qname = input.getNodeType();
            @SuppressWarnings("unchecked")
            T value = (T) getDelegate().deserialize((Map<QName, Object>) input);
            return new ValueWithQName<T>(qname, value);
        }

        @Override
        public CompositeNode serialize(ValueWithQName<T> input) {
            return (CompositeNode) super.serialize(input);
        }
    }

    private static class DataContainerCodecImpl<T extends DataContainer> //
            extends IntermediateCodec<T> //
            implements DataContainerCodec<T> {

        public DataContainerCodecImpl(BindingCodec<Map<QName, Object>, Object> delegate) {
            super(delegate);
        }

        @Override
        public ValueWithQName<T> deserialize(Node<?> input) {
            if (input == null) {
                return null;
            }
            QName qname = input.getNodeType();
            @SuppressWarnings("unchecked")
            T value = (T) getDelegate().deserialize((Map<QName, Object>) input);
            return new ValueWithQName<T>(qname, value);
        }

        @Override
        public CompositeNode serialize(ValueWithQName<T> input) {
            return (CompositeNode) super.serialize(input);
        }
    }

    @SuppressWarnings("rawtypes")
    private static class ChoiceCaseCodecImpl<T extends DataContainer> implements ChoiceCaseCodec<T>, //
            Delegator<BindingCodec> {
        private boolean augmenting;
        private boolean uses;
        private BindingCodec delegate;

        private Set<String> validNames;
        private Set<QName> validQNames;
        private ChoiceCaseNode schema;

        public void setSchema(ChoiceCaseNode caseNode) {
            this.schema = caseNode;
            validNames = new HashSet<>();
            validQNames = new HashSet<>();
            for (DataSchemaNode node : caseNode.getChildNodes()) {
                QName qname = node.getQName();
                validQNames.add(qname);
                validNames.add(qname.getLocalName());
            }
            augmenting = caseNode.isAugmenting();
            uses = caseNode.isAddedByUses();
        }

        public ChoiceCaseCodecImpl() {
            this.delegate = NOT_READY_CODEC;
        }

        public ChoiceCaseCodecImpl(ChoiceCaseNode caseNode) {
            this.delegate = NOT_READY_CODEC;
            setSchema(caseNode);
        }

        @Override
        public ValueWithQName<T> deserialize(Node<?> input) {
            throw new UnsupportedOperationException("Direct invocation of this codec is not allowed.");
        }

        @Override
        public CompositeNode serialize(ValueWithQName<T> input) {
            throw new UnsupportedOperationException("Direct invocation of this codec is not allowed.");
        }

        @Override
        public BindingCodec getDelegate() {
            return delegate;
        }

        public void setDelegate(BindingCodec delegate) {
            this.delegate = delegate;
        }

        public ChoiceCaseNode getSchema() {
            return schema;
        }

        @Override
        public boolean isAcceptable(Node<?> input) {
            if (input instanceof CompositeNode) {
                if (augmenting && !uses) {
                    return checkAugmenting((CompositeNode) input);
                } else {
                    return checkLocal((CompositeNode) input);
                }
            }
            return false;
        }

        private boolean checkLocal(CompositeNode input) {
            QName parent = input.getNodeType();
            for (Node<?> childNode : input.getChildren()) {
                QName child = childNode.getNodeType();
                if (!Objects.equals(parent.getNamespace(), child.getNamespace())
                        || !Objects.equals(parent.getRevision(), child.getRevision())) {
                    continue;
                }
                if (validNames.contains(child.getLocalName())) {
                    return true;
                }
            }
            return false;
        }

        private boolean checkAugmenting(CompositeNode input) {
            for (Node<?> child : input.getChildren()) {
                if (validQNames.contains(child.getNodeType())) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class ChoiceCodecImpl<T> implements ChoiceCodec<T> {

        private final BindingCodec<Map<QName, Object>, Object> delegate;

        @SuppressWarnings("rawtypes")
        private final Map<Class, ChoiceCaseCodecImpl<?>> cases = Collections
                .synchronizedMap(new WeakHashMap<Class, ChoiceCaseCodecImpl<?>>());

        private final CaseCompositeNodeMapFacade CompositeToCase;

        public ChoiceCodecImpl(BindingCodec<Map<QName, Object>, Object> delegate) {
            this.delegate = delegate;
            this.CompositeToCase = new CaseCompositeNodeMapFacade(cases);
        }

        @Override
        public ValueWithQName<T> deserialize(Node<?> input) {
            throw new UnsupportedOperationException("Direct invocation of this codec is not allowed.");
        }

        @Override
        public Node<?> serialize(ValueWithQName<T> input) {
            throw new UnsupportedOperationException("Direct invocation of this codec is not allowed.");
        }

        public CaseCompositeNodeMapFacade getCompositeToCase() {
            return CompositeToCase;
        }

        public Map<Class, ChoiceCaseCodecImpl<?>> getCases() {
            return cases;
        }

        public BindingCodec<Map<QName, Object>, Object> getDelegate() {
            return delegate;
        }

    }

    @SuppressWarnings("rawtypes")
    private class CaseClassMapFacade extends MapFacadeBase {

        @Override
        public Set<Entry<Class, BindingCodec<Object, Object>>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public BindingCodec get(Object key) {
            if (key instanceof Class) {
                Class cls = (Class) key;
                // bindingClassEncountered(cls);
                ChoiceCaseCodecImpl caseCodec = getCaseCodecFor(cls);
                return caseCodec.getDelegate();
            }
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    private static class CaseCompositeNodeMapFacade extends MapFacadeBase<CompositeNode> {

        final Map<Class, ChoiceCaseCodecImpl<?>> choiceCases;

        public CaseCompositeNodeMapFacade(Map<Class, ChoiceCaseCodecImpl<?>> choiceCases) {
            this.choiceCases = choiceCases;
        }

        @Override
        public BindingCodec get(Object key) {
            if (!(key instanceof CompositeNode)) {
                return null;
            }
            for (Entry<Class, ChoiceCaseCodecImpl<?>> entry : choiceCases.entrySet()) {
                ChoiceCaseCodecImpl<?> codec = entry.getValue();
                if (codec.isAcceptable((CompositeNode) key)) {
                    return codec.getDelegate();
                }
            }
            return null;
        }

    }

    /**
     * This map is used as only facade for
     * {@link org.opendaylight.yangtools.yang.binding.BindingCodec} in different
     * classloaders to retrieve codec dynamicly based on provided key.
     *
     * @param <T>
     *            Key type
     */
    @SuppressWarnings("rawtypes")
    private static abstract class MapFacadeBase<T> implements Map<T, BindingCodec<?, ?>> {

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public void clear() {
            throw notModifiable();
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public BindingCodec remove(Object key) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Collection<BindingCodec<?, ?>> values() {
            return Collections.emptySet();
        }

        private UnsupportedOperationException notModifiable() {
            return new UnsupportedOperationException("Not externally modifiable.");
        }

        @Override
        public BindingCodec<Map<QName, Object>, Object> put(T key, BindingCodec<?, ?> value) {
            throw notModifiable();
        }

        @Override
        public void putAll(Map<? extends T, ? extends BindingCodec<?, ?>> m) {
            throw notModifiable();
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Set<T> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Set<Entry<T, BindingCodec<?, ?>>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private class AugmentableCompositeCodec implements BindingCodec {

        private final Class augmentableType;

        Map<Class, AugmentationCodec<?>> localAugmentationCodecs = Collections
                .synchronizedMap(new WeakHashMap<Class, AugmentationCodec<?>>());

        public AugmentableCompositeCodec(Class type) {
            Preconditions.checkArgument(Augmentable.class.isAssignableFrom(type));
            augmentableType = type;
        }

        @Override
        public Object serialize(Object input) {
            if (input instanceof Augmentable<?>) {

                Map<Class, Augmentation> augmentations = getAugmentations(input);
                return serializeImpl(augmentations);
            }
            return null;
        }

        private Map<Class, Augmentation> getAugmentations(Object input) {
            Field augmentationField;
            try {
                augmentationField = input.getClass().getDeclaredField("augmentation");
                augmentationField.setAccessible(true);
                Map<Class, Augmentation> augMap = (Map<Class, Augmentation>) augmentationField.get(input);
                return augMap;
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                LOG.debug("Could not read augmentations for {}", input, e);
            }
            return Collections.emptyMap();
        }

        private List serializeImpl(Map<Class, Augmentation> input) {
            List ret = new ArrayList<>();
            for (Entry<Class, Augmentation> entry : input.entrySet()) {
                AugmentationCodec codec = getCodecForAugmentation(entry.getKey());
                CompositeNode node = codec.serialize(new ValueWithQName(null, entry.getValue()));
                ret.addAll(node.getChildren());
            }
            return ret;
        }

        public synchronized <T extends Augmentation<?>> void addAugmentationCodec(Class<T> augmentationClass,
                AugmentationCodec<T> value) {
            localAugmentationCodecs.put(augmentationClass, value);
        }

        @Override
        public Map<Class, Augmentation> deserialize(Object input) {
            Map<Class, Augmentation> ret = new HashMap<>();
            if (input instanceof CompositeNode) {
                List<Entry<Class, AugmentationCodec<?>>> codecs = new ArrayList<>(localAugmentationCodecs.entrySet());
                for (Entry<Class, AugmentationCodec<?>> codec : codecs) {
                    ValueWithQName<?> value = codec.getValue().deserialize((CompositeNode) input);
                    if (value != null && value.getValue() != null) {
                        ret.put(codec.getKey(), (Augmentation) value.getValue());
                    }
                }
            }
            return ret;
        }

        public Class getAugmentableType() {
            return augmentableType;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static class LateMixinCodec implements BindingCodec, Delegator<BindingCodec> {

        private BindingCodec delegate;

        @Override
        public BindingCodec getDelegate() {
            if (delegate == null) {
                throw new IllegalStateException("Codec not initialized yet.");
            }
            return delegate;
        }

        @Override
        public Object deserialize(Object input) {
            return getDelegate().deserialize(input);
        }

        @Override
        public Object serialize(Object input) {
            return getDelegate().serialize(input);
        }
    }

    private static class AugmentationCodecWrapper<T extends Augmentation<?>> implements AugmentationCodec<T>,
            Delegator<BindingCodec> {

        private final BindingCodec delegate;
        private final QName augmentationQName;

        public AugmentationCodecWrapper(BindingCodec<Map<QName, Object>, Object> rawCodec) {
            this.delegate = rawCodec;
            this.augmentationQName = BindingReflections.findQName(rawCodec.getClass());
        }

        @Override
        public BindingCodec getDelegate() {
            return delegate;
        }

        @Override
        public CompositeNode serialize(ValueWithQName<T> input) {
            @SuppressWarnings("unchecked")
            List<Map<QName, Object>> rawValues = (List<Map<QName, Object>>) getDelegate().serialize(input);
            List<Node<?>> serialized = new ArrayList<>(rawValues.size());
            for (Map<QName, Object> val : rawValues) {
                serialized.add(IntermediateMapping.toNode(val));
            }
            return new CompositeNodeTOImpl(input.getQname(), null, serialized);
        }

        @Override
        @SuppressWarnings("unchecked")
        public ValueWithQName<T> deserialize(Node<?> input) {
            Object rawCodecValue = getDelegate().deserialize(input);
            return new ValueWithQName<T>(input.getNodeType(), (T) rawCodecValue);
        }

        @Override
        public QName getAugmentationQName() {
            return augmentationQName;
        }
    }

    private class IdentityCompositeCodec implements IdentityCodec {

        @Override
        public Object deserialize(Object input) {
            Preconditions.checkArgument(input instanceof QName);
            return deserialize((QName) input);
        }

        @Override
        public Class<?> deserialize(QName input) {
            Type type = qnamesToIdentityMap.get(input);
            if (type == null) {
                return null;
            }
            ReferencedTypeImpl typeref = new ReferencedTypeImpl(type.getPackageName(), type.getName());
            WeakReference<Class> softref = typeToClass.get(typeref);
            if (softref == null) {

                try {
                    Class<?> cls = classLoadingStrategy.loadClass(typeref.getFullyQualifiedName());
                    if (cls != null) {
                        serialize(cls);
                        return cls;
                    }
                } catch (Exception e) {
                    LOG.warn("Identity {} was not deserialized, because of missing class {}", input,
                            typeref.getFullyQualifiedName());
                }
                return null;
            }
            return softref.get();
        }

        @Override
        public QName serialize(Class input) {
            Preconditions.checkArgument(BaseIdentity.class.isAssignableFrom(input));
            bindingClassEncountered(input);
            QName qname = identityQNames.get(input);
            if (qname != null) {
                return qname;
            }
            ConcreteType typeref = Types.typeForClass(input);
            qname = typeToQname.get(typeref);
            if (qname != null) {
                identityQNames.put(input, qname);
            }
            return qname;
        }

        @Override
        public Object serialize(Object input) {
            Preconditions.checkArgument(input instanceof Class);
            return serialize((Class) input);
        }
    }

    public boolean isCodecAvailable(Class<? extends DataContainer> cls) {
        if (containerCodecs.containsKey(cls)) {
            return true;
        }
        if (identifierCodecs.containsKey(cls)) {
            return true;
        }
        if (choiceCodecs.containsKey(cls)) {
            return true;
        }
        if (caseCodecs.containsKey(cls)) {
            return true;
        }
        if (augmentableCodecs.containsKey(cls)) {
            return true;
        }
        if (augmentationCodecs.containsKey(cls)) {
            return true;
        }
        return false;
    }
}
