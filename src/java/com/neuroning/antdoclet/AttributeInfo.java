package com.neuroning.antdoclet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
  Information about an attribute of a type or task.
*/

public class AttributeInfo
{
    public static @NotNull AttributeInfo create(@NotNull String name,
                                                @NotNull TypeMirror type,
                                                @NotNull ExecutableElement definingMethod,
                                                @Nullable List<String> enumeratedValues)
    {
        return new AttributeInfo(name, type, definingMethod, enumeratedValues);
    }

    public final @NotNull String name;
    public final @NotNull TypeMirror type;
    public final @NotNull ExecutableElement definingMethod;
    public final @Nullable List<String> enumeratedValues;

    private AttributeInfo(@NotNull String name,
                          @NotNull TypeMirror type,
                          @NotNull ExecutableElement definingMethod,
                          @Nullable List<String> enumeratedValues)
    {
        this.name = name;
        this.type = type;
        this.definingMethod = definingMethod;
        this.enumeratedValues = enumeratedValues;
    }
}
