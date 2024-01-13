/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

package org.violetlib.antdoclet;

import org.jetbrains.annotations.NotNull;

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
                                                @NotNull List<TypeMirror> allTypes,
                                                @NotNull ExecutableElement definingMethod)
    {
        return new AttributeInfo(name, type, allTypes, definingMethod);
    }

    public final @NotNull String name;
    public final @NotNull TypeMirror type;
    public final @NotNull List<TypeMirror> allTypes;
    public final @NotNull ExecutableElement definingMethod;

    private AttributeInfo(@NotNull String name,
                          @NotNull TypeMirror type,
                          @NotNull List<TypeMirror> allTypes,
                          @NotNull ExecutableElement definingMethod)
    {
        this.name = name;
        this.type = type;
        this.allTypes = allTypes;
        this.definingMethod = definingMethod;
    }
}
