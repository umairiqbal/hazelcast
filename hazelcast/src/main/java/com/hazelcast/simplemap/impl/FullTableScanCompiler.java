package com.hazelcast.simplemap.impl;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.security.AccessController.doPrivileged;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport.closeQuietly;

public class FullTableScanCompiler {

    private final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
    private final File targetDirectory = new File(getUserDir(), "simplemap");
    private final ConcurrentMap<String, Class> classes = new ConcurrentHashMap<String, Class>();

    public synchronized Class compile(String compiledQueryUuid, String javacode) {
        ensureExistingDirectory(targetDirectory);

        String className = "FullTableScan_" + compiledQueryUuid.replace("-","");
        Class clazz = classes.get(compiledQueryUuid);
        if (clazz == null) {
            JavaFileObject file = createJavaFileObject(className, javacode);
            clazz = compile(javaCompiler, file, className);
            classes.put(compiledQueryUuid, clazz);
        }
        return clazz;
    }

    public Class<FullTableScan> load(String compileQueryUuid){
        return classes.get(compileQueryUuid);
    }

    private Class compile(JavaCompiler compiler, JavaFileObject file, final String className) {
        if (compiler == null) {
            throw new IllegalStateException("Could not get Java compiler."
                    + " You need to use a JDK! Version found: " + System.getProperty("java.version"));
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                null,
                diagnostics,
                asList("-d", targetDirectory.getAbsolutePath()),
                null,
                singletonList(file));

        boolean success = task.call();
        if (!success) {
            StringBuilder sb = new StringBuilder();
            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                sb.append("Error on line ")
                        .append(diagnostic.getLineNumber())
                        .append(" in ")
                        .append(diagnostic)
                        .append('\n');
            }
            throw new RuntimeException(sb.toString());
        }

        return (Class) doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    URLClassLoader classLoader = new URLClassLoader(new URL[]{targetDirectory.toURI().toURL()});
                    return (Class) classLoader.loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e.getMessage(), e);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        });
    }

    public static File getUserDir() {
        return new File(System.getProperty("user.dir"));
    }

    public static File ensureExistingDirectory(File dir) {
        if (dir.isDirectory()) {
            return dir;
        }

        if (dir.isFile()) {
            throw new IllegalArgumentException(format("File [%s] is not a directory", dir.getAbsolutePath()));
        }

        // we don't care about the result because multiple threads are allowed to call this method concurrently
        // and therefore mkdirs() can return false if the directory has been created by another thread
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();

        // we just need to make sure the directory is created
        if (!dir.exists()) {
            throw new RuntimeException("Could not create directory: " + dir.getAbsolutePath());
        }

        return dir;
    }

    private JavaFileObject createJavaFileObject(
            String className,
            String javaCode) {
        try {
            File javaFile = new File(targetDirectory, className + ".java");
            writeText(javaCode, javaFile);
            return new JavaSourceFromString(className, javaCode);
        } catch (Exception e) {
            throw new RuntimeException(className + " ran into a code generation problem: " + e.getMessage(), e);
        }
    }

    public static void writeText(String text, File file) {
//        checkNotNull(text, "Text can't be null");
//        checkNotNull(file, "File can't be null");

        FileOutputStream stream = null;
        OutputStreamWriter streamWriter = null;
        BufferedWriter writer = null;
        try {
            stream = new FileOutputStream(file);
            streamWriter = new OutputStreamWriter(stream);
            writer = new BufferedWriter(streamWriter);
            writer.write(text);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(writer);
            closeQuietly(streamWriter);
            closeQuietly(stream);
        }
    }

    private static class JavaSourceFromString extends SimpleJavaFileObject {

        private final String code;

        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}