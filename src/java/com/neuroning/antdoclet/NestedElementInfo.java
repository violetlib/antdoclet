package com.neuroning.antdoclet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

/**
  Information about a type of nested element supported by a type or task.
*/

public class NestedElementInfo
{
    public static @NotNull NestedElementInfo create(@Nullable String name,
                                                    @NotNull TypeMirror type,
                                                    @NotNull ExecutableElement definingMethod)
    {
        return new NestedElementInfo(name, type, definingMethod);
    }

    public final @Nullable String name;
    public final @NotNull TypeMirror type;
    public final @NotNull ExecutableElement definingMethod;

    private NestedElementInfo(@Nullable String name,
                              @NotNull TypeMirror type,
                              @NotNull ExecutableElement definingMethod)
    {
        this.name = name;
        this.type = type;
        this.definingMethod = definingMethod;
    }
}
