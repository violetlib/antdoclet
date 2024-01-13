package com.neuroning.antdoclet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
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
                                           @Nullable ExecutableElement addTaskMethod,
                                           @Nullable ExecutableElement addTextMethod,
                                           @NotNull List<TypeElement> nestedClassList
    )
    {
        return new TypeInfo(qualifiedClassName, simpleClassName, isTask,
          attributeMap, nestedElementMap, nestedElementList, addTaskMethod, addTextMethod, nestedClassList);
    }

    private final @NotNull String qualifiedClassName;
    private final @NotNull String simpleClassName;
    private final boolean isPlaceholder;
    private final boolean isTask;
    private final @NotNull Map<String,AttributeInfo> attributeMap;
    private final @NotNull Map<String,NestedElementInfo> nestedElementMap;
    private final @NotNull List<NestedElementInfo> nestedElementList;
    private final @Nullable ExecutableElement addTaskMethod;
    private final @Nullable ExecutableElement addTextMethod;
    private final @NotNull List<TypeElement> nestedClassList;

    private TypeInfo(@NotNull String qualifiedClassName,
                     @NotNull String simpleClassName,
                     boolean isTask,
                     @NotNull Map<String,AttributeInfo> attributeMap,
                     @NotNull Map<String,NestedElementInfo> nestedElementMap,
                     @NotNull List<NestedElementInfo> nestedElementList,
                     @Nullable ExecutableElement addTaskMethod,
                     @Nullable ExecutableElement addTextMethod,
                     @NotNull List<TypeElement> nestedClassList)
    {
        this.qualifiedClassName = qualifiedClassName;
        this.simpleClassName = simpleClassName;
        this.isPlaceholder = false;
        this.isTask = isTask;
        this.attributeMap = attributeMap;
        this.nestedElementMap = nestedElementMap;
        this.nestedElementList = nestedElementList;
        this.addTaskMethod = addTaskMethod;
        this.addTextMethod = addTextMethod;
        this.nestedClassList = nestedClassList;
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
      Indicate whether this description is a placeholder for a nested type that has not been analyzed.
    */

    public boolean isPlaceholder()
    {
        return isPlaceholder;
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
      Return a description of the attribute with the specified name.
    */

    public @Nullable AttributeInfo getAttribute(@NotNull String name)
    {
        return attributeMap.get(name);
    }

    /**
      Return descriptions of the named nested elements supported by this type.
    */

    public @NotNull Map<String,NestedElementInfo> getNamedNestedElements()
    {
        return nestedElementMap;
    }

    /**
      Return a description of a named nested element.
      @param name The name of the nested element.
      @return the description, or null if the name is not the name of a supported named nested element.
    */

    public @Nullable NestedElementInfo getNamedElement(@NotNull String name)
    {
        return nestedElementMap.get(name);
    }

    /**
      Return descriptions of the unnamed nested elements supported by this type.
    */

    public @NotNull List<NestedElementInfo> getUnnamedNestedElements()
    {
        return nestedElementList;
    }

    /**
      Return a description of an unnamed nested element type.
      @param t The type of the nested element.
      @return the description, or null if the type is not a supported nested element type.
    */

    public @Nullable NestedElementInfo getUnnamedElement(@NotNull TypeMirror t)
    {
        for (NestedElementInfo info : nestedElementList) {
            if (info.types.contains(t)) {
                return info;
            }
        }
        return null;
    }

    /**
      Return the addTask method, if defined.
    */

    public @Nullable ExecutableElement getAddTaskMethod()
    {
        return addTaskMethod;
    }

    /**
      Indicate whether this type supports text content.
    */

    public boolean supportsText()
    {
        return addTextMethod != null;
    }

    /**
      Return the addText method, if defined.
    */

    public @Nullable ExecutableElement getAddTextMethod()
    {
        return addTextMethod;
    }

    /**
      Return nested types.
    */

    public @NotNull List<TypeElement> getNestedClasses()
    {
        return new ArrayList<>(nestedClassList);
    }
}
