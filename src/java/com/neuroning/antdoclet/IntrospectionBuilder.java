package com.neuroning.antdoclet;

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

public class IntrospectionBuilder
{
    public static @NotNull IntrospectionBuilder create(@NotNull TypeElement theClass,
                                                       @NotNull DocUtils docUtils)
    {
        return new IntrospectionBuilder(theClass, docUtils);
    }

    private final @NotNull TypeElement theClass;
    private final @NotNull TypeMirror theType;
    private final @NotNull String className;
    private final @NotNull DocUtils docUtils;
    private final @NotNull Reporter reporter;

    private final Map<String, LocalAttributeInfo> attributeMap = new HashMap<>();
    private final Map<String,NestedTypeInfo> namedTypeMap = new HashMap<>();
    private final List<NestedTypeInfo> unnamedTypeList = new ArrayList<>();
    private @Nullable ExecutableElement addTextMethod;

    private static class LocalAttributeInfo
    {
        String name;
        TypeMirror type;
        List<ExecutableElement> methods = new ArrayList<>();
        List<String> enumeratedValues = new ArrayList<>();

        public LocalAttributeInfo(String name)
        {
            this.name = name;
        }
    }

    private static class NestedTypeInfo
    {
        String name;
        TypeMirror type;
        List<ExecutableElement> methods = new ArrayList<>();
        ExecutableElement addTextMethod;

        public NestedTypeInfo(String name)
        {
            this.name = name;
        }

        public NestedTypeInfo(TypeMirror type)
        {
            this.type = type;
        }
    }

    private IntrospectionBuilder(@NotNull TypeElement theClass, @NotNull DocUtils docUtils)
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

        return TypeInfo.create(qualifiedClassName, simpleClassName, isTask, amap, nmap, nel, addTextMethod);
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
        // TBD: priority
        ExecutableElement definingMethod = info.methods.getFirst();
        return AttributeInfo.create(info.name, info.type, definingMethod, info.enumeratedValues);
    }

    private @NotNull NestedElementInfo createNamedTypeInfo(@NotNull NestedTypeInfo info)
    {
        // TBD: priority
        ExecutableElement definingMethod = info.methods.getFirst();
        return NestedElementInfo.create(info.name, info.type, definingMethod);
    }

    private @NotNull NestedElementInfo createUnnamedTypeInfo(@NotNull NestedTypeInfo info)
    {
        ExecutableElement definingMethod = info.methods.getFirst();
        return NestedElementInfo.create(info.name, info.type, definingMethod);
    }

    public void foundAddTypeMethod(@NotNull TypeMirror t, @NotNull ExecutableElement method)
    {
        debug("found nested type: " + docUtils.getTypeName(t));

        NestedTypeInfo info = createOrGetNestedTypeInfo(t);
        info.methods.add(method);
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

        // keep track of setters
        // manage priority of String and File setters (could be done last)

        LocalAttributeInfo info = createOrGetAttributeInfo(name);
        if (info.type == null) {
            info.type = attributeType;
        } else if (!info.type.equals(attributeType)) {
            reporter.print(Diagnostic.Kind.WARNING, method,
              "Inconsistent types for attribute " + name + " "
                + docUtils.getTypeName(info.type) + " and " + docUtils.getTypeName(attributeType));
            return;
        }
        info.methods.add(method);
    }

    public void foundNestedTypeCreator(@NotNull String name,
                                       @NotNull TypeMirror t,
                                       @NotNull ExecutableElement method)
    {
        debug("found nested type " + name + ": " + docUtils.getTypeName(t) + " [creator]");
        processAddMethod(name, t, method, false, false);
    }

    public void foundNestedTypeAddConfigured(@NotNull String name,
                                             @NotNull TypeMirror t,
                                             @NotNull ExecutableElement method)
    {
        debug("found nested type " + name + ": " + docUtils.getTypeName(t) + " [configured]");
        processAddMethod(name, t, method, true, true);
    }

    public void foundNestedTypeAdd(@NotNull String name,
                                   @NotNull TypeMirror t,
                                   @NotNull ExecutableElement method)
    {
        debug("found nested type " + name + ": " + docUtils.getTypeName(t));
        processAddMethod(name, t, method, true, false);
    }

    private void processAddMethod(@NotNull String name,
                                  @NotNull TypeMirror t,
                                  @NotNull ExecutableElement method,
                                  boolean isAdd,
                                  boolean isConfigured)
    {
        // manage priority

        NestedTypeInfo info = createOrGetNestedTypeInfo(name);
        if (info.type == null) {
            info.type = t;
        } else if (!info.type.equals(t)) {
            reporter.print(Diagnostic.Kind.WARNING, method,
              "Multiple types found for nested element " + name + " "
                + docUtils.getTypeName(info.type) + " and " + docUtils.getTypeName(t));
            return;
        }
        info.methods.add(method);
    }

    private @NotNull IntrospectionBuilder.LocalAttributeInfo createOrGetAttributeInfo(@NotNull String name)
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
            if (info.type.equals(t)) {
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
        System.err.println(className + ": " + message);
    }
}
