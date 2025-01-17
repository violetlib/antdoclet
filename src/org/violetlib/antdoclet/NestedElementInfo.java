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
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
  Information about a type of nested element supported by a type or task.
*/

public class NestedElementInfo
{
    public static @NotNull NestedElementInfo create(@Nullable String name,
                                                    @NotNull List<TypeMirror> types,
                                                    @NotNull ExecutableElement definingMethod,
                                                    @Nullable ExecutableElement constructor)
    {
        return new NestedElementInfo(name, types, definingMethod, constructor);
    }

    /**
      The name deduced from the name of the defining method.
      Should not be shown to the user unless there is no name specified in an @ant tag.
      This field is null if the defining method defines unnamed nested types.
    */
    public final @Nullable String name;

    /**
      The type(s) of the nested element.
    */
    public final @NotNull List<TypeMirror> types;

    /**
      The defining method of the nested element.
      The defining method is an add or create method.
    */
    public final @NotNull ExecutableElement definingMethod;

    /**
      The constructor of the element type.
      A possible source of a description.
    */
    public final @Nullable ExecutableElement constructor;

    private NestedElementInfo(@Nullable String name,
                              @NotNull List<TypeMirror> types,
                              @NotNull ExecutableElement definingMethod,
                              @Nullable ExecutableElement constructor)
    {
        this.name = name;
        this.types = types;
        this.definingMethod = definingMethod;
        this.constructor = constructor;
    }
}
