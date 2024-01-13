/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

package org.violetlib.antdoclet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.*;

/**
  Analyze Java source code that implement Ant tasks and types.
*/

public class Analysis
{
    public static @NotNull TypeInfo getInfo(@NotNull TypeElement theClass, @NotNull DocUtils docUtils)
    {
        String name = theClass.getQualifiedName().toString();
        TypeInfo info = typeInfoMap.get(name);
        if (info == null) {
            info = new Analysis(theClass, docUtils).analyze();
            typeInfoMap.put(name, info);
        }

        return info;
    }

    private static final @NotNull Map<String,TypeInfo> typeInfoMap = new HashMap<>();

    private final @NotNull TypeElement theClass;
    private final @NotNull TypeMirror theType;
    private final @NotNull DocUtils docUtils;
    private final @NotNull TypeInfoBuilder builder;
    private final @NotNull List<ExecutableElement> methods;
    private final @NotNull List<ExecutableElement> constructors;
    private final @NotNull List<TypeElement> nestedClasses;

    private Analysis(@NotNull TypeElement theClass, @NotNull DocUtils docUtils)
    {
        this.theClass = theClass;
        this.theType = theClass.asType();
        this.docUtils = docUtils;
        this.builder = TypeInfoBuilder.create(theClass, docUtils);
        this.methods = docUtils.getMethods(theClass);
        this.constructors = docUtils.getConstructors(theClass);
        this.nestedClasses = docUtils.getNestedClasses(theClass);
    }

    private @NotNull TypeInfo analyze()
    {
        for (ExecutableElement method : methods) {
            analyze(method);
        }
        for (TypeElement te : nestedClasses) {
            analyze(te);
        }
        return builder.getInfo();
    }

    private void analyze(@NotNull TypeElement nc)
    {
        Set<Modifier> mods = nc.getModifiers();
        if (!mods.contains(Modifier.PUBLIC) || !mods.contains(Modifier.STATIC)) {
            return;
        }
        builder.foundNestedClass(nc);
    }

    private void analyze(@NotNull ExecutableElement method)
    {
        Set<Modifier> mods = method.getModifiers();
        if (!mods.contains(Modifier.PUBLIC)) {
            return;
        }

        String name = method.getSimpleName().toString();
        TypeMirror returnType = method.getReturnType();
        List<TypeMirror> parameterTypes = getMethodParameterTypes(method);

        // check of add[Configured](Class) pattern
        if (parameterTypes.size() == 1
          && isVoid(returnType)
          && (name.equals("add") || name.equals("addConfigured"))) {
            TypeMirror t = parameterTypes.get(0);
            builder.foundAddTypeMethod(t, method);
            return;
        }

        // not really user settable properties on tasks
        if (isSubtypeOf(theType, "org.apache.tools.ant.Task")
          && parameterTypes.size() == 1
          && isHiddenTaskSetMethod(name, parameterTypes.get(0))) {
            return;
        }

        // not really user settable properties on tasks/project components
        if (isSubtypeOf(theType, "org.apache.tools.ant.ProjectComponent")
          && parameterTypes.size() == 1
          && isHiddenTypeSetMethod(name, parameterTypes.get(0))) {
            return;
        }

        // addTask for TaskContainers
        if (isSubtypeOf(theType, "org.apache.tools.ant.TaskContainer")
          && parameterTypes.size() == 1
          && "addTask".equals(name)
          && isClass(parameterTypes.get(0), "org.apache.tools.ant.Task")) {
            builder.foundAddTaskMethod(method);
            return;
        }

        // addText
        if (name.equals("addText")
          && isVoid(returnType)
          && parameterTypes.size() == 1
          && isClass(parameterTypes.get(0), "java.lang.String")) {
            builder.foundAddTextMethod(method);
            return;
        }

        // set attribute
        if (name.startsWith("set")
          && isVoid(returnType)
          && parameterTypes.size() == 1
          && !isArray(parameterTypes.get(0))) {
            String propName = getPropertyName(name, "set");
            TypeMirror attributeType = parameterTypes.get(0);
            builder.foundAttributeSetter(propName, attributeType, method);
        }

        // create nested element
        if (name.startsWith("create") && !name.equals("create")
          && !isArray(returnType)
          && !isPrimitive(returnType)
          && parameterTypes.isEmpty()
        ) {
            String propName = getPropertyName(name, "create");
            builder.foundNestedTypeCreator(propName, returnType, method);
            return;
        }

        // create named nested element, configured
        if (name.startsWith("addConfigured")
          && isVoid(returnType)
          && parameterTypes.size() == 1
        ) {
            TypeMirror elementType = parameterTypes.get(0);
            if (!isClass(elementType, "java.lang.String")
              && !isArray(elementType)
              && !isPrimitive(elementType)) {

                ExecutableElement c = findNestedTypeConstructor(elementType);
                if (c != null) {
                    String propName = getPropertyName(name, "addConfigured");
                    builder.foundNestedTypeAddConfigured(propName, elementType, method, c);
                }
                return;
            }
        }

        // create named nested element
        if (name.startsWith("add")
          && isVoid(returnType)
          && parameterTypes.size() == 1
        ) {
            TypeMirror elementType = parameterTypes.get(0);
            if (!isClass(elementType, "java.lang.String")
              && !isArray(elementType)
              && !isPrimitive(elementType)) {

                ExecutableElement c = findNestedTypeConstructor(elementType);
                if (c != null) {
                    String propName = getPropertyName(name, "add");
                    builder.foundNestedTypeAdd(propName, elementType, method, c);
                }
                return;
            }
        }
    }

    private @Nullable ExecutableElement findNestedTypeConstructor(@NotNull TypeMirror t)
    {
        TypeElement te = docUtils.getType(t);
        if (te == null) {
            return null;
        }
        List<ExecutableElement> constructors = docUtils.getConstructors(te);
        for (ExecutableElement cons : constructors) {
            List<TypeMirror> parameterTypes = getMethodParameterTypes(cons);
            if (parameterTypes.isEmpty()) {
                return cons;
            }
            if (parameterTypes.size() == 1) {
                TypeMirror pt = parameterTypes.get(0);
                if (isClass(pt, "org.apache.tools.ant.Project")) {
                    return cons;
                }
            }
        }
        return null;
    }

    private boolean isHiddenTaskSetMethod(@NotNull String name, @NotNull TypeMirror pt)
    {
        return "setLocation".equals(name) && isClass(pt, "org.apache.tools.ant.Location")
          || "setTaskType".equals(name) && isClass(pt, "java.lang.String")
          || "setProject".equals(name) && isClass(pt, "org.apache.tools.ant.Project")
          || "setOwningTarget".equals(name) && isClass(pt, "org.apache.tools.ant.Target")
          || "setTaskName".equals(name) && isClass(pt, "java.lang.String")
          || "setRuntimeConfigurableWrapper".equals(name) && isClass(pt, "org.apache.tools.ant.RuntimeConfigurable")
          ;
    }

    private boolean isHiddenTypeSetMethod(@NotNull String name, @NotNull TypeMirror pt)
    {
        return
             "setRefid".equals(name) && isClass(pt, "org.apache.tools.ant.types.Reference")
          || "setDescription".equals(name) && isClass(pt, "java.lang.String")
          || "setLocation".equals(name) && isClass(pt, "org.apache.tools.ant.Location")
          || "setProject".equals(name) && isClass(pt, "org.apache.tools.ant.Project")
          ;
    }

    private boolean isBoolean(@NotNull TypeMirror t)
    {
        return t.getKind() == TypeKind.BOOLEAN;
    }

    private boolean isClass(@NotNull TypeMirror t, @NotNull String name)
    {
        if (t instanceof DeclaredType) {
            DeclaredType dt = (DeclaredType) t;
            TypeElement e = (TypeElement) dt.asElement();
            return e.getQualifiedName().toString().equals(name);
        }
        return false;
    }

    private boolean isVoid(@NotNull TypeMirror t)
    {
        return t.getKind() == TypeKind.VOID;
    }

    private boolean isArray(@NotNull TypeMirror t)
    {
        return t.getKind() == TypeKind.ARRAY;
    }

    private boolean isPrimitive(@NotNull TypeMirror t)
    {
        return t.getKind().isPrimitive();
    }

    private boolean isSubtypeOf(@NotNull TypeMirror m, @NotNull String typeName)
    {
        TypeElement te = docUtils.getType(typeName);
        if (te != null) {
            return docUtils.isSubtypeOf(m, te.asType());
        }
        warn("Type not found: " + typeName);
        return false;
    }

    private @NotNull List<TypeMirror> getMethodParameterTypes(@NotNull ExecutableElement method)
    {
        List<TypeMirror> result = new ArrayList<>();
        for (VariableElement parm : method.getParameters()) {
            result.add(parm.asType());
        }
        return result;
    }

    private @NotNull String getPropertyName(@NotNull String methodName, @NotNull String prefix) {
        return methodName.substring(prefix.length()).toLowerCase(Locale.ENGLISH);
    }

    private void warn(@NotNull String message)
    {
        docUtils.getReporter().print(Diagnostic.Kind.WARNING, message);
    }
}
