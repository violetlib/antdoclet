/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

package org.violetlib.antdoclet;

import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;

/**
  Information about an attribute of an @ant tag derived from analyzing a doc comment.
  <p>
  Currently, all attribute values are plain text.
*/

public class TagAttributeInfo
{
    public static @NotNull TagAttributeInfo create(@NotNull Element owner,
                                                   @NotNull String attributeName,
                                                   @NotNull String value)
    {
        return new TagAttributeInfo(owner, attributeName, value);
    }

    private final @NotNull Element owner;
    private final @NotNull String attributeName;
    private final @NotNull String value;

    private TagAttributeInfo(@NotNull Element owner,
                             @NotNull String attributeName,
                             @NotNull String value)
    {
        this.owner = owner;
        this.attributeName = attributeName;
        this.value = value;
    }

    protected TagAttributeInfo(@NotNull TagAttributeInfo source)
    {
        this.owner = source.owner;
        this.attributeName = source.attributeName;
        this.value = source.value;
    }

    /**
      Return the source element that owns the documentation comment.
    */

    public @NotNull Element getOwner()
    {
        return owner;
    }

    /**
      Return the attribute name.
    */

    public @NotNull String getName()
    {
        return attributeName;
    }

    /**
      Return the attribute value.
    */

    public @NotNull String getValue()
    {
        return value;
    }
}
