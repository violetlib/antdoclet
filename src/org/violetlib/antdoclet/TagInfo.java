/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

package org.violetlib.antdoclet;

import com.sun.source.doctree.DocTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
  Information about an @ant tag derived from analyzing a doc comment.
*/

public class TagInfo
{
    public static @NotNull TagInfo create(@NotNull Element owner,
                                          @NotNull String tagName,
                                          @NotNull Map<String,TagAttributeInfo> attributeMap,
                                          @Nullable List<? extends DocTree> content)
    {
        return new TagInfo(owner, tagName, attributeMap, content);
    }

    private final @NotNull Element owner;
    private final @NotNull String tagName;
    private final @NotNull Map<String,TagAttributeInfo> attributeMap;
    private final @NotNull List<? extends DocTree> content;

    private @Nullable Set<String> _attributeNames;

    private TagInfo(@NotNull Element owner,
                    @NotNull String tagName,
                    @NotNull Map<String,TagAttributeInfo> attributeMap,
                    @Nullable List<? extends DocTree> content)
    {
        this.owner = owner;
        this.tagName = tagName;
        this.attributeMap = attributeMap;
        this.content = content != null ? content : new ArrayList<>();
    }

    protected TagInfo(@NotNull TagInfo source,
                      @Nullable Map<String,TagAttributeInfo> attributeMap)
    {
        this.owner = source.owner;
        this.tagName = source.tagName;
        this.attributeMap = attributeMap != null ? attributeMap : source.attributeMap;
        this.content = source.content;
    }

    /**
      Return the source element that owns the documentation comment.
    */

    public @NotNull Element getOwner()
    {
        return owner;
    }

    /**
      Return the tag name.
    */

    public @NotNull String getName()
    {
        return tagName;
    }

    /**
      Return the names of the defined attributes.
    */

    public @NotNull Set<String> getAttributeNames()
    {
        if (_attributeNames == null) {
            _attributeNames = attributeMap.keySet();
        }
        return _attributeNames;
    }

    /**
      Return the information for an attribute.
    */

    public @Nullable TagAttributeInfo getAttribute(@NotNull String attributeName)
    {
        return attributeMap.get(attributeName);
    }

    /**
      Return the value of an attribute.
      @param attributeName The name of the attribute.
      @return the attribute value, or null if the attribute is not defined.
    */

    public @Nullable String getAttributeValue(@NotNull String attributeName)
    {
        TagAttributeInfo a = attributeMap.get(attributeName);
        return a != null ? a.getValue() : null;
    }

    /**
      Return the tag content.
    */

    public @NotNull List<? extends DocTree> getContent()
    {
        return content;
    }
}
