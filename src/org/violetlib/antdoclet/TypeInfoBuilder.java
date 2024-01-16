/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

package org.violetlib.antdoclet;

import jdk.javadoc.doclet.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**

*/

public class TypeInfoBuilder
{
    public static @NotNull TypeInfoBuilder create(@NotNull TypeElement theClass, @NotNull DocUtils docUtils)
    {
        return new TypeInfoBuilder(theClass, docUtils);
    }

    private final @NotNull TypeElement theClass;
    private final @NotNull TypeMirror theType;
    private final @NotNull String className;
    private final @NotNull DocUtils docUtils;
    private final @NotNull Reporter reporter;

    private final @NotNull Map<String,LocalAttributeInfo> attributeMap = new HashMap<>();
    private final @NotNull Map<String,NestedTypeInfo> namedTypeMap = new HashMap<>();
    private final @NotNull List<NestedTypeInfo> unnamedTypeList = new ArrayList<>();
    private @Nullable ExecutableElement addTaskMethod;
    private @Nullable ExecutableElement addTextMethod;
    private final @NotNull List<TypeElement> nestedClassList = new ArrayList<>();

    private static class LocalAttributeInfo
    {
        String name;
        TypeMirror type;
        List<TypeMirror> types = new ArrayList<>();
        List<ExecutableElement> methods = new ArrayList<>();

        public LocalAttributeInfo(@NotNull String name)
        {
            this.name = name;
        }
    }

    private static class NestedTypeInfo
    {
        String name;
        final @NotNull List<TypeMirror> types = new ArrayList<>();
        final List<ExecutableElement> methods = new ArrayList<>();
        ExecutableElement addTaskMethod;
        ExecutableElement addTextMethod;
        ExecutableElement constructor;

        /**
          Create a description of a named nested element.
        */

        public NestedTypeInfo(@NotNull String name)
        {
            this.name = name;
        }

        /**
          Create a description of a unnamed nested element.
        */

        public NestedTypeInfo(@NotNull TypeMirror type)
        {
            this.types.add(type);
        }
    }

    private TypeInfoBuilder(@NotNull TypeElement theClass, @NotNull DocUtils docUtils)
    {
        this.theClass = theClass;
        this.theType = theClass.asType();
        this.className = theClass.getSimpleName().toString();
        this.docUtils = docUtils;
        this.reporter = docUtils.getReporter();
    }

    public @NotNull TypeInfo getInfo()
    {
        String qualifiedClassName = theClass.getQualifiedName().toString();
        String simpleClassName = theClass.getSimpleName().toString();
        boolean isTask = isSubtypeOf(theType, "org.apache.tools.ant.Task");
        Map<String,AttributeInfo> amap = createAttributeMap();
        Map<String,NestedElementInfo> nmap = createNestedElementMap();
        List<NestedElementInfo> nel = createNestedElementList();
        List<TypeElement> ncl = new ArrayList<>(nestedClassList);

        return TypeInfo.create(qualifiedClassName, simpleClassName, isTask, amap, nmap, nel,
          addTaskMethod, addTextMethod, ncl);
    }

    private @NotNull Map<String,AttributeInfo> createAttributeMap()
    {
        Map<String,AttributeInfo> map = new HashMap<>();
        for (LocalAttributeInfo info : attributeMap.values()) {
            map.put(info.name, createAttributeInfo(info));
        }
        return map;
    }

    private @NotNull Map<String,NestedElementInfo> createNestedElementMap()
    {
        Map<String,NestedElementInfo> map = new HashMap<>();
        for (NestedTypeInfo info : namedTypeMap.values()) {
            map.put(info.name, createNamedTypeInfo(info));
        }
        return map;
    }

    private @NotNull List<NestedElementInfo> createNestedElementList()
    {
        List<NestedElementInfo> list = new ArrayList<>();
        for (NestedTypeInfo info : unnamedTypeList) {
            list.add(createUnnamedTypeInfo(info));
        }
        return list;
    }

    private @NotNull AttributeInfo createAttributeInfo(@NotNull LocalAttributeInfo info)
    {
        // TBD: priority of defining methods and types
        ExecutableElement definingMethod = info.methods.getFirst();
        List<TypeMirror> types = info.types;

        if (types.size() > 1) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s: Attribute %s has multiple types:", theClass.getSimpleName(), info.name));
            for (TypeMirror type : types) {
                sb.append(" ");
                sb.append(docUtils.getSimpleTypeName(type));
            }
            reporter.print(Diagnostic.Kind.WARNING, sb.toString());
        }

        return AttributeInfo.create(info.name, info.type, types, definingMethod);
    }

    private @NotNull NestedElementInfo createNamedTypeInfo(@NotNull NestedTypeInfo info)
    {
        // TBD: priority

//        System.err.println("Creating named type info " + info.name + " " + info.methods.size());
//        for (ExecutableElement e : info.methods) {
//            System.err.println("  " + e.getSimpleName());
//        }
//        if (info.constructor != null) {
//            System.err.println("  Constructor: " + info.constructor.getSimpleName());
//        }

        ExecutableElement definingMethod = info.methods.getFirst();

        List<TypeMirror> types = info.types;
        if (types.size() > 1) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s: Nested element %s has multiple types:", theClass.getSimpleName(), info.name));
            for (TypeMirror type : types) {
                sb.append(" ");
                sb.append(docUtils.getSimpleTypeName(type));
            }
            reporter.print(Diagnostic.Kind.WARNING, sb.toString());
        }

        return NestedElementInfo.create(info.name, info.types, definingMethod, info.constructor);
    }

    private @NotNull NestedElementInfo createUnnamedTypeInfo(@NotNull NestedTypeInfo info)
    {
        ExecutableElement definingMethod = info.methods.getFirst();
        return NestedElementInfo.create(info.name, info.types, definingMethod, info.constructor);
    }

    public void foundNestedClass(@NotNull TypeElement te)
    {
        debug("found nested class: " + te.getSimpleName());

        nestedClassList.add(te);
    }

    public void foundAddTypeMethod(@NotNull TypeMirror t, @NotNull ExecutableElement method)
    {
        debug("found nested element type: " + docUtils.getTypeName(t));

        NestedTypeInfo info = createOrGetNestedTypeInfo(t);
        info.methods.add(method);
    }

    public void foundAddTaskMethod(@NotNull ExecutableElement method)
    {
        debug("found addTask");

        if (addTaskMethod != null) {
            reporter.print(Diagnostic.Kind.ERROR, method, "Duplicate addTask method");
        } else {
            addTaskMethod = method;
        }
    }

    public void foundAddTextMethod(@NotNull ExecutableElement method)
    {
        debug("found addText");

        if (addTextMethod != null) {
            reporter.print(Diagnostic.Kind.ERROR, method, "Duplicate addText method");
        } else {
            addTextMethod = method;
        }
    }

    public void foundAttributeSetter(@NotNull String name,
                                     @NotNull TypeMirror attributeType,
                                     @NotNull ExecutableElement method)
    {
        debug("found attribute: " + name + " : " + docUtils.getTypeName(attributeType));

        // keep track of setters and types
        // manage priority of String and File setters (could be done last)

        LocalAttributeInfo info = createOrGetAttributeInfo(name);
        if (info.type == null) {
            info.type = attributeType;
        }
        if (!info.types.contains(attributeType)) {
            info.types.add(attributeType);
        }
        info.methods.add(method);
    }

    public void foundNestedTypeCreator(@NotNull String name,
                                       @NotNull TypeMirror t,
                                       @NotNull ExecutableElement method)
    {
        debug("found named nested element type " + name + ": " + docUtils.getTypeName(t) + " [creator]");
        processAddMethod(name, t, method, false, false, null);
    }

    public void foundNestedTypeAddConfigured(@NotNull String name,
                                             @NotNull TypeMirror t,
                                             @NotNull ExecutableElement method,
                                             @NotNull ExecutableElement constructor)
    {
        debug("found name nested element type " + name + ": " + docUtils.getTypeName(t) + " [configured]");
        processAddMethod(name, t, method, true, true, constructor);
    }

    public void foundNestedTypeAdd(@NotNull String name,
                                   @NotNull TypeMirror t,
                                   @NotNull ExecutableElement method,
                                   @NotNull ExecutableElement constructor)
    {
        debug("found named nested element type " + name + ": " + docUtils.getTypeName(t));
        processAddMethod(name, t, method, true, false, constructor);
    }

    private void processAddMethod(@NotNull String name,
                                  @NotNull TypeMirror t,
                                  @NotNull ExecutableElement method,
                                  boolean isAdd,
                                  boolean isConfigured,
                                  @Nullable ExecutableElement constructor
    )
    {
        // manage priority

        NestedTypeInfo info = createOrGetNestedTypeInfo(name);
        if (!info.types.contains(t)) {
            info.types.add(t);
        }
        info.methods.add(method);
        if (constructor != null) {
            info.constructor = constructor;
        }
    }

    private @NotNull TypeInfoBuilder.LocalAttributeInfo createOrGetAttributeInfo(@NotNull String name)
    {
        LocalAttributeInfo info = attributeMap.get(name);
        if (info != null) {
            return info;
        }
        info = new LocalAttributeInfo(name);
        attributeMap.put(name, info);
        return info;
    }

    private @NotNull NestedTypeInfo createOrGetNestedTypeInfo(@NotNull String name)
    {
        NestedTypeInfo info = namedTypeMap.get(name);
        if (info != null) {
            return info;
        }

        info = new NestedTypeInfo(name);
        namedTypeMap.put(name, info);
        return info;
    }

    private @NotNull NestedTypeInfo createOrGetNestedTypeInfo(@NotNull TypeMirror t)
    {
        for (NestedTypeInfo info : unnamedTypeList) {
            if (info.types.contains(t)) {
                return info;
            }
        }

        NestedTypeInfo info = new NestedTypeInfo(t);
        unnamedTypeList.add(info);
        return info;
    }

    private boolean isSubtypeOf(@NotNull TypeMirror m, @NotNull String typeName)
    {
        TypeElement te = docUtils.getType(typeName);
        if (te != null) {
            return docUtils.isSubtypeOf(m, te.asType());
        }
        reporter.print(Diagnostic.Kind.WARNING, "Type not found: " + typeName);
        return false;
    }

    private void debug(@NotNull String message)
    {
        if (false) {
            reporter.print(Diagnostic.Kind.NOTE, className + ": " + message);
        }
    }
}
