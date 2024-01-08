package com.neuroning.antdoclet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.Map;

/**
  Information about a type in a build script derived by analyzing source files and related class files,
  excluding documentation comments.
  The types of interest are tasks, data types, and other types used as attribute types.
*/

public class TypeInfo
{
    public static @NotNull TypeInfo create(@NotNull String qualifiedClassName,
                                           @NotNull String simpleClassName,
                                           boolean isTask,
                                           @NotNull Map<String,AttributeInfo> attributeMap,
                                           @NotNull Map<String,NestedElementInfo> nestedElementMap,
                                           @NotNull List<NestedElementInfo> nestedElementList,
                                           @Nullable ExecutableElement addTextMethod
                                           )
    {
        return new TypeInfo(qualifiedClassName, simpleClassName, isTask,
          attributeMap, nestedElementMap, nestedElementList, addTextMethod);
    }

    private final @NotNull String qualifiedClassName;
    private final @NotNull String simpleClassName;
    private final boolean isTask;
    private final @NotNull Map<String,AttributeInfo> attributeMap;
    private final @NotNull Map<String,NestedElementInfo> nestedElementMap;
    private final @NotNull List<NestedElementInfo> nestedElementList;
    private final @Nullable ExecutableElement addTextMethod;

    private TypeInfo(@NotNull String qualifiedClassName,
                     @NotNull String simpleClassName,
                     boolean isTask,
                     @NotNull Map<String,AttributeInfo> attributeMap,
                     @NotNull Map<String,NestedElementInfo> nestedElementMap,
                     @NotNull List<NestedElementInfo> nestedElementList,
                     @Nullable ExecutableElement addTextMethod)
    {
        this.qualifiedClassName = qualifiedClassName;
        this.simpleClassName = simpleClassName;
        this.isTask = isTask;
        this.attributeMap = attributeMap;
        this.nestedElementMap = nestedElementMap;
        this.nestedElementList = nestedElementList;
        this.addTextMethod = addTextMethod;
    }

    /**
      Return the fully qualified name of the Java class that implements the task or type.
    */

    public @NotNull String getQualifiedClassName()
    {
        return qualifiedClassName;
    }

    /**
      Return the simple name of the Java class that implements the task or type.
    */

    public @NotNull String getSimpleClassName()
    {
        return simpleClassName;
    }

    /**
      Indicate whether this type defines a task. A type that does not define a task is presumably used as an attribute
      or nested element type.

      @return true if the type defines a task, false otherwise.
    */

    public boolean isTask()
    {
        return isTask;
    }

    /**
      Return descriptions of the attributes supported by this type.
    */

    public @NotNull Map<String,AttributeInfo> getAttributes()
    {
        return attributeMap;
    }

    /**
      Return descriptions of the named nested elements supported by this type.
    */

    public @NotNull Map<String,NestedElementInfo> getNamedNestedElements()
    {
        return nestedElementMap;
    }

    /**
      Return descriptions of the unnamed nested elements supported by this type.
    */

    public @NotNull List<NestedElementInfo> getUnnamedNestedElements()
    {
        return nestedElementList;
    }

    public boolean supportsText()
    {
        return addTextMethod != null;
    }

    public @Nullable ExecutableElement getAddTextMethod()
    {
        return addTextMethod;
    }
}
