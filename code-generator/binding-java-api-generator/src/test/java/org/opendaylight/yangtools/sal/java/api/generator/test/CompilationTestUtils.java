/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.sal.java.api.generator.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class CompilationTestUtils {
    public static final String FS = File.separator;
    static final String BASE_PKG = "org.opendaylight.yang.gen.v1";

    static final String TEST_PATH = "target" + FS + "test";
    static final File TEST_DIR = new File(TEST_PATH);

    static final String GENERATOR_OUTPUT_PATH = TEST_PATH + FS + "src";
    static final File GENERATOR_OUTPUT_DIR = new File(GENERATOR_OUTPUT_PATH);
    static final String COMPILER_OUTPUT_PATH = TEST_PATH + FS + "bin";
    static final File COMPILER_OUTPUT_DIR = new File(COMPILER_OUTPUT_PATH);

    static final String BASE_PATH = "org" + FS + "opendaylight" + FS + "yang" + FS + "gen" + FS + "v1";
    static final String NS_TEST = BASE_PATH + FS + "urn" + FS + "opendaylight" + FS + "test" + FS + "rev131008";
    static final String NS_FOO = BASE_PATH + FS + "urn" + FS + "opendaylight" + FS + "foo" + FS + "rev131008";
    static final String NS_BAR = BASE_PATH + FS + "urn" + FS + "opendaylight" + FS + "bar" + FS + "rev131008";
    static final String NS_BAZ = BASE_PATH + FS + "urn" + FS + "opendaylight" + FS + "baz" + FS + "rev131008";

    /**
     * Method to clean resources. It is manually called at the end of each test
     * instead of marking it with @After annotation to prevent removing
     * generated code if test fails.
     */
    static void cleanUp(File... resourceDirs) {
        for (File resourceDir : resourceDirs) {
            if (resourceDir.exists()) {
                deleteTestDir(resourceDir);
            }
        }
    }

    /**
     * Asserts that class contains field with fiven name and type.
     *
     * @param clazz
     *            class to test
     * @param name
     *            field name
     * @param type
     *            field type
     * @return field with given name if present in class
     */
    static Field assertContainsField(Class<?> clazz, String name, Class<?> type) {
        try {
            Field f = clazz.getDeclaredField(name);
            assertEquals(type, f.getType());
            return f;
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Field " + name + " does not exists in class " + clazz.getSimpleName());
        }
    }

    /**
     * Asserts that class contains field with given name and value. Method tries
     * to create new instance of class and get value of field. If class
     * constructor contains any arguments, class is instantiated with null
     * values.
     *
     * @param clazz
     *            class to test
     * @param name
     *            name of field
     * @param returnType
     *            return type of field
     * @param expectedValue
     *            expected value of field
     * @param constructorArgs
     *            constructor arguments of class to test
     */
    static void assertContainsFieldWithValue(Class<?> clazz, String name, Class<?> returnType, Object expectedValue,
            Class<?>... constructorArgs) {
        Object[] initargs = null;
        if (constructorArgs != null && constructorArgs.length > 0) {
            initargs = new Object[constructorArgs.length];
            for (int i = 0; i < constructorArgs.length; i++) {
                initargs[i] = null;
            }
        }
        assertContainsFieldWithValue(clazz, name, returnType, expectedValue, constructorArgs, initargs);
    }

    /**
     * Asserts that class contains field with given name, return type and value.
     *
     * @param clazz
     *            class to test
     * @param name
     *            name of field
     * @param returnType
     *            return type of field
     * @param expectedValue
     *            expected value of field
     * @param constructorArgs
     *            array of constructor arguments classes
     * @param initargs
     *            array of constructor values
     */
    static void assertContainsFieldWithValue(Class<?> clazz, String name, Class<?> returnType, Object expectedValue,
            Class<?>[] constructorArgs, Object... initargs) {
        Field f = assertContainsField(clazz, name, returnType);
        try {
            Constructor<?> c = clazz.getDeclaredConstructor(constructorArgs);
            Object o = c.newInstance(initargs);
            assertEquals(expectedValue, f.get(o));
        } catch (Exception e) {
            throw new AssertionError("Failed to perform " + name + " field test", e);
        }
    }

    /**
     * Asserts that class contains constructor with parameter types.
     *
     * @param clazz
     *            class to test
     * @param args
     *            array of argument classes
     */
    static Constructor<?> assertContainsConstructor(Class<?> clazz, Class<?>... args) {
        try {
            return clazz.getDeclaredConstructor(args);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Constructor with args " + Arrays.toString(args) + " does not exists in class "
                    + clazz.getSimpleName());
        }
    }

    /**
     * Asserts that class contains method with given name, return type and
     * parameter types.
     *
     * @param clazz
     *            class to test
     * @param returnType
     *            method return type
     * @param name
     *            method name
     * @param args
     *            array of parameter type classes
     * @return method with given name, return type and parameter types
     */
    static Method assertContainsMethod(Class<?> clazz, Class<?> returnType, String name, Class<?>... args) {
        try {
            Method m = clazz.getDeclaredMethod(name, args);
            assertEquals(returnType, m.getReturnType());
            return m;
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Method " + name + " with args " + Arrays.toString(args)
                    + " does not exists in class " + clazz.getSimpleName());
        }
    }

    /**
     * Asserts that class contains method with given name and return type.
     *
     * @param clazz
     *            class to test
     * @param returnTypeStr
     *            name of method return type
     * @param name
     *            method name
     * @param loader
     *            current class loader
     */
    static void assertContainsMethod(Class<?> clazz, String returnTypeStr, String name, ClassLoader loader) {
        Class<?> returnType;
        try {
            returnType = Class.forName(returnTypeStr, true, loader);
            Method method = clazz.getMethod(name);
            assertEquals(returnType, method.getReturnType());
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Return type of method '" + name + "' not found");
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Method " + name + " does not exists in class " + clazz.getSimpleName());
        }
    }

    /**
     * Asserts that class contains hashCode, equals and toString methods.
     *
     * @param clazz
     *            class to test
     */
    static void assertContainsDefaultMethods(Class<?> clazz) {
        assertContainsMethod(clazz, Integer.TYPE, "hashCode");
        assertContainsMethod(clazz, Boolean.TYPE, "equals", Object.class);
        assertContainsMethod(clazz, String.class, "toString");
    }

    /**
     * Asserts that constructor contains check for illegal argument.
     *
     * @param constructor
     *            constructor to invoke
     * @param errorMsg
     *            expected error message
     * @param args
     *            constructor arguments
     * @throws Exception
     */
    static void assertContainsRestrictionCheck(Constructor<?> constructor, String errorMsg, Object... args)
            throws Exception {
        try {
            constructor.newInstance(args);
            fail("constructor invocation should fail");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalArgumentException);
            assertEquals(errorMsg, cause.getMessage());
        }
    }

    /**
     * Asserts that method contains check for illegal argument.
     *
     * @param obj
     *            object to test (can be null, if method is static)
     * @param method
     *            method to invoke
     * @param errorMsg
     *            expected error message
     * @param args
     *            constructor arguments
     * @throws Exception
     */
    static void assertContainsRestrictionCheck(Object obj, Method method, String errorMsg, Object... args)
            throws Exception {
        try {
            method.invoke(obj, args);
            fail("method invocation should fail");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalArgumentException);
            assertEquals(errorMsg, cause.getMessage());
        }
    }

    /**
     * Asserts that class contains 'public static
     * java.util.List<com.google.common.collect.Range<java.lang.Integer>>
     * getLength()' method.
     *
     * @param clazz
     *            class to test
     */
    static void assertContainsGetLengthOrRange(Class<?> clazz, boolean isLength) {
        try {
            Method m = clazz.getDeclaredMethod(isLength ? "length" : "range");
            java.lang.reflect.Type returnType = m.getGenericReturnType();
            assertTrue("Return type of getLength method must be ParameterizedType",
                    returnType instanceof ParameterizedType);
            ParameterizedType listType = (ParameterizedType) returnType;
            assertEquals("interface java.util.List", listType.getRawType().toString());

            java.lang.reflect.Type[] args = listType.getActualTypeArguments();
            assertEquals(1, args.length);
            java.lang.reflect.Type range = args[0];
            assertTrue(range instanceof ParameterizedType);
            ParameterizedType pRange = (ParameterizedType) range;
            assertEquals("class com.google.common.collect.Range", pRange.getRawType().toString());

            args = pRange.getActualTypeArguments();
            assertEquals(1, args.length);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Method length()/range() does not exists in class " + clazz.getSimpleName());
        }
    }

    /**
     * Asserts that class implements given interface.
     *
     * @param clazz
     *            source to test
     * @param ifc
     *            expected interface
     */
    static void assertImplementsIfc(Class<?> clazz, Class<?> ifc) {
        Class<?>[] interfaces = clazz.getInterfaces();
        List<Class<?>> ifcsList = Arrays.asList(interfaces);
        if (!ifcsList.contains(ifc)) {
            throw new AssertionError(clazz + " should implement " + ifc);
        }
    }

    /**
     * Test if interface generated from augment extends Augmentation interface
     * with correct generic type.
     *
     * @param clazz
     *            interface generated from augment
     * @param genericTypeName
     *            fully qualified name of expected parameter type
     */
    static void testAugmentation(Class<?> clazz, String genericTypeName) {
        final String ifcName = "interface org.opendaylight.yangtools.yang.binding.Augmentation";
        assertImplementsParameterizedIfc(clazz, ifcName, genericTypeName);
    }

    /**
     * Asserts that class implements interface with given name and generic type
     * parameter.
     *
     * @param clazz
     *            class to test
     * @param ifcName
     *            name of interface
     * @param genericTypeName
     *            name of generic type
     */
    static void assertImplementsParameterizedIfc(Class<?> clazz, String ifcName, String genericTypeName) {
        ParameterizedType ifcType = null;
        for (java.lang.reflect.Type ifc : clazz.getGenericInterfaces()) {
            if (ifc instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) ifc;
                if (ifcName.equals(pt.getRawType().toString())) {
                    ifcType = pt;
                }
            }
        }
        assertNotNull(ifcType);

        java.lang.reflect.Type[] typeArguments = ifcType.getActualTypeArguments();
        assertEquals(1, typeArguments.length);
        assertEquals("interface " + genericTypeName, typeArguments[0].toString());
    }

    /**
     * Test if source code is compilable.
     *
     * @param sourcesOutputDir
     *            directory containing source files
     * @param compiledOutputDir
     *            compiler output directory
     */
    static void testCompilation(File sourcesOutputDir, File compiledOutputDir) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        List<File> filesList = getJavaFiles(sourcesOutputDir);
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(filesList);
        Iterable<String> options = Arrays.asList("-d", compiledOutputDir.getAbsolutePath());
        boolean compiled = compiler.getTask(null, null, null, options, null, compilationUnits).call();
        assertTrue(compiled);
    }

    /**
     * Asserts that directory contains exactly given count of files.
     *
     * @param dir
     *            directory to test
     * @param count
     *            expected count of files in directory
     */
    static void assertFilesCount(File dir, int count) {
        File[] dirContent = dir.listFiles();
        if (dirContent == null) {
            throw new AssertionError("File " + dir + " doesn't exists or it's not a directory");
        } else {
            assertEquals("Unexpected count of generated files", count, dirContent.length);
        }
    }

    /**
     * Search recursively given directory for *.java files.
     *
     * @param directory
     *            directory to search
     * @return List of java files found
     */
    private static List<File> getJavaFiles(File directory) {
        List<File> result = new ArrayList<>();
        File[] filesToRead = directory.listFiles();
        if (filesToRead != null) {
            for (File file : filesToRead) {
                if (file.isDirectory()) {
                    result.addAll(getJavaFiles(file));
                } else {
                    String absPath = file.getAbsolutePath();
                    if (absPath.endsWith(".java")) {
                        result.add(file);
                    }
                }
            }
        }
        return result;
    }

    static List<File> getSourceFiles(String path) throws Exception {
        final URI resPath = BaseCompilationTest.class.getResource(path).toURI();
        final File sourcesDir = new File(resPath);
        if (sourcesDir.exists()) {
            final List<File> sourceFiles = new ArrayList<>();
            final File[] fileArray = sourcesDir.listFiles();
            if (fileArray == null) {
                throw new IllegalArgumentException("Unable to locate files in " + sourcesDir);
            }
            sourceFiles.addAll(Arrays.asList(fileArray));
            return sourceFiles;
        } else {
            throw new FileNotFoundException("Testing files were not found(" + sourcesDir.getName() + ")");
        }
    }

    static void deleteTestDir(File file) {
        if (file.isDirectory()) {
            File[] filesToDelete = file.listFiles();
            if (filesToDelete != null) {
                for (File f : filesToDelete) {
                    deleteTestDir(f);
                }
            }
        }
        if (!file.delete()) {
            throw new RuntimeException("Failed to clean up after test");
        }
    }

}
