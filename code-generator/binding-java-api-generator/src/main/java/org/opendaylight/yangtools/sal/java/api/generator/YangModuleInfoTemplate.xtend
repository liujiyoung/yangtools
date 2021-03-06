/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.sal.java.api.generator

import java.io.InputStream
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat

import java.util.Collections
import java.util.Date
import java.util.HashSet
import java.util.LinkedHashMap
import java.util.Map
import java.util.Set
import java.util.TreeMap

import org.opendaylight.yangtools.binding.generator.util.BindingGeneratorUtil
import org.opendaylight.yangtools.binding.generator.util.Types
import org.opendaylight.yangtools.sal.binding.model.api.ParameterizedType
import org.opendaylight.yangtools.sal.binding.model.api.Type
import org.opendaylight.yangtools.sal.binding.model.api.WildcardType
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yangtools.yang.model.api.Module
import org.opendaylight.yangtools.yang.model.api.SchemaContext

import com.google.common.collect.ImmutableSet
import static org.opendaylight.yangtools.yang.binding.BindingMapping.*
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider
import com.google.common.base.Preconditions

class YangModuleInfoTemplate {

    val Module module
    val SchemaContext ctx
    val Map<String, String> importMap = new LinkedHashMap()

    @Property
    val String packageName;

    @Property
    val String modelBindingProviderName;

    new(Module module, SchemaContext ctx) {
        Preconditions.checkArgument(module != null, "Module must not be null.");
        this.module = module
        this.ctx = ctx
        _packageName = BindingGeneratorUtil.moduleNamespaceToPackageName(module);
        _modelBindingProviderName = '''«packageName».«MODEL_BINDING_PROVIDER_CLASS_NAME»''';
    }

    def String generate() {
        val body = '''
            public final class «MODULE_INFO_CLASS_NAME» implements «YangModuleInfo.importedName» {

                private static final «YangModuleInfo.importedName» INSTANCE = new «MODULE_INFO_CLASS_NAME»();

                private final «String.importedName» name = "«module.name»";
                private final «String.importedName» namespace = "«module.namespace.toString»";
                «val DateFormat df = new SimpleDateFormat("yyyy-MM-dd")»
                private final «String.importedName» revision = "«df.format(module.revision)»";
                private final «String.importedName» resourcePath = "«sourcePath»";
                
                private final «Set.importedName»<YangModuleInfo> importedModules;

                public static «YangModuleInfo.importedName» getInstance() {
                    return INSTANCE;
                }

                «module.classBody»
            }
        '''
        return '''
            package «packageName» ;
            «imports»
            «body»
        '''.toString
    }

    def String generateModelProvider() {
        '''
            package «packageName»;

            public final class «MODEL_BINDING_PROVIDER_CLASS_NAME» implements «YangModelBindingProvider.name» {

                public «YangModuleInfo.name» getModuleInfo() {
                    return «MODULE_INFO_CLASS_NAME».getInstance();
                }
            }
        '''

    }

    private def CharSequence classBody(Module m) '''
        private «MODULE_INFO_CLASS_NAME»() {
            «IF m.imports.size != 0»
                «Set.importedName»<«YangModuleInfo.importedName»> set = new «HashSet.importedName»<>();
                «FOR imp : m.imports»
                    «val name = imp.moduleName»
                    «val rev = imp.revision»
                    «IF rev == null»
                        «val Set<Module> modules = ctx.modules»
                        «val TreeMap<Date, Module> sorted = new TreeMap()»
                        «FOR module : modules»
                            «IF module.name.equals(name)»
                                «sorted.put(module.revision, module)»
                            «ENDIF»
                        «ENDFOR»
                        set.add(«BindingGeneratorUtil.moduleNamespaceToPackageName(sorted.lastEntry().value)».«MODULE_INFO_CLASS_NAME».getInstance());
                    «ELSE»
                        set.add(«BindingGeneratorUtil.moduleNamespaceToPackageName(ctx.findModuleByName(name, rev))».«MODULE_INFO_CLASS_NAME».getInstance());
                    «ENDIF»
                «ENDFOR»
                importedModules = «ImmutableSet.importedName».copyOf(set);
            «ELSE»
                importedModules = «Collections.importedName».emptySet();
            «ENDIF»
            «InputStream.importedName» stream = «MODULE_INFO_CLASS_NAME».class.getResourceAsStream(resourcePath);
            if (stream == null) {
                throw new IllegalStateException("Resource '" + resourcePath + "' is missing");
            }
            try {
                stream.close();
            } catch («IOException.importedName» e) {
            // Resource leak, but there is nothing we can do
            }
        }

        @Override
        public «String.importedName» getName() {
            return name;
        }

        @Override
        public «String.importedName» getRevision() {
            return revision;
        }

        @Override
        public «String.importedName» getNamespace() {
            return namespace;
        }

        @Override
        public «InputStream.importedName» getModuleSourceStream() throws IOException {
            «InputStream.importedName» stream = «MODULE_INFO_CLASS_NAME».class.getResourceAsStream(resourcePath);
            if (stream == null) {
                throw new «IOException.importedName»("Resource " + resourcePath + " is missing");
            }
            return stream;
        }

        @Override
        public «Set.importedName»<«YangModuleInfo.importedName»> getImportedModules() {
            return importedModules;
        }

        @Override
        public «String.importedName» toString() {
            «StringBuilder.importedName» sb = new «StringBuilder.importedName»(this.getClass().getCanonicalName());
            sb.append("[");
            sb.append("name = " + name);
            sb.append(", namespace = " + namespace);
            sb.append(", revision = " + revision);
            sb.append(", resourcePath = " + resourcePath);
            sb.append(", imports = " + importedModules);
            sb.append("]");
            return sb.toString();
        }
    '''

    def getSourcePath() {
        return "/" + module.moduleSourcePath.replace(java.io.File.separatorChar, '/')
    }

    private def imports() ''' 
        «IF !importMap.empty»
            «FOR entry : importMap.entrySet»
                «IF entry.value != BindingGeneratorUtil.moduleNamespaceToPackageName(module)»
                    import «entry.value».«entry.key»;
                «ENDIF»
            «ENDFOR»
        «ENDIF»
    '''

    final protected def importedName(Class<?> cls) {
        val Type intype = Types.typeForClass(cls)
        putTypeIntoImports(intype);
        getExplicitType(intype)
    }

    final def void putTypeIntoImports(Type type) {
        val String typeName = type.getName();
        val String typePackageName = type.getPackageName();
        if (typePackageName.startsWith("java.lang") || typePackageName.isEmpty()) {
            return;
        }
        if (!importMap.containsKey(typeName)) {
            importMap.put(typeName, typePackageName);
        }
        if (type instanceof ParameterizedType) {
            val ParameterizedType paramType = (type as ParameterizedType)
            val Type[] params = paramType.getActualTypeArguments()
            if (params != null) {
                for (Type param : params) {
                    putTypeIntoImports(param);
                }
            }
        }
    }

    final def String getExplicitType(Type type) {
        val String typePackageName = type.getPackageName();
        val String typeName = type.getName();
        val String importedPackageName = importMap.get(typeName);
        var StringBuilder builder;
        if (typePackageName.equals(importedPackageName)) {
            builder = new StringBuilder(type.getName());
            addActualTypeParameters(builder, type);
            if (builder.toString().equals("Void")) {
                return "void";
            }
        } else {
            builder = new StringBuilder();
            if (typePackageName.startsWith("java.lang")) {
                builder.append(type.getName());
            } else {
                if (!typePackageName.isEmpty()) {
                    builder.append(typePackageName + Constants.DOT + type.getName());
                } else {
                    builder.append(type.getName());
                }
            }
            if (type.equals(Types.voidType())) {
                return "void";
            }
            addActualTypeParameters(builder, type);
        }
        return builder.toString();
    }

    final def StringBuilder addActualTypeParameters(StringBuilder builder, Type type) {
        if (type instanceof ParameterizedType) {
            val ParameterizedType pType = (type as ParameterizedType)
            val Type[] pTypes = pType.getActualTypeArguments();
            builder.append("<");
            builder.append(getParameters(pTypes));
            builder.append(">");
        }
        return builder;
    }

    final def String getParameters(Type[] pTypes) {
        if (pTypes == null || pTypes.length == 0) {
            return "?";
        }
        val StringBuilder builder = new StringBuilder();

        var int i = 0;
        for (pType : pTypes) {
            val Type t = pTypes.get(i)

            var String separator = ",";
            if (i == (pTypes.length - 1)) {
                separator = "";
            }

            var String wildcardParam = "";
            if (t.equals(Types.voidType())) {
                builder.append("java.lang.Void" + separator);
            } else {

                if (t instanceof WildcardType) {
                    wildcardParam = "? extends ";
                }

                builder.append(wildcardParam + getExplicitType(t) + separator);
                i = i + 1
            }
        }
        return builder.toString();
    }

}
