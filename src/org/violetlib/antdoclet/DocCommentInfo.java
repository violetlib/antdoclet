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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
  Information about a doc comment.
*/

public class DocCommentInfo
{
    public static @NotNull DocCommentInfo create(@NotNull Element owner,
                                                 @NotNull List<? extends DocTree> description,
                                                 @NotNull List<? extends DocTree> shortDescription,
                                                 @NotNull List<? extends DocTree> mediumDescription,
                                                 @NotNull Map<String,TagInfo> tagMap)
    {
        return new DocCommentInfo(owner, description, shortDescription, mediumDescription, tagMap);
    }

    private final @NotNull Element owner;
    private final @NotNull Map<String,TagInfo> tagMap;
    private final @NotNull List<? extends DocTree> description;
    private final @NotNull List<? extends DocTree> shortDescription;
    private final @NotNull List<? extends DocTree> mediumDescription;

    private @Nullable Set<String> _tagNames;

    private DocCommentInfo(@NotNull Element owner,
                           @NotNull List<? extends DocTree> description,
                           @NotNull List<? extends DocTree> shortDescription,
                           @NotNull List<? extends DocTree> mediumDescription,
                           @NotNull Map<String,TagInfo> tagMap)
    {
        this.owner = owner;
        this.description = description;
        this.shortDescription = shortDescription;
        this.mediumDescription = mediumDescription;
        this.tagMap = tagMap;
    }

    protected DocCommentInfo(@NotNull DocCommentInfo source, @Nullable Map<String,TagInfo> tagMap)
    {
        this.owner = source.owner;
        this.description = source.description;
        this.shortDescription = source.shortDescription;
        this.mediumDescription = source.mediumDescription;
        this.tagMap = tagMap != null ? tagMap : source.tagMap;
    }

    /**
      Return the source element that owns the documentation comment.
    */

    public @NotNull Element getOwner()
    {
        return owner;
    }

    /**
      Return the names of the defined tags.
    */

    public @NotNull Set<String> getTagNames()
    {
        if (_tagNames == null) {
            _tagNames = tagMap.keySet();
        }
        return _tagNames;
    }

    /**
      Indicate whether the tag with the specified name is defined.
      @param tagName The name of the tag.
      @return true if and only if a tag with that name is present.
    */

    public boolean hasTag(@NotNull String tagName)
    {
        return tagMap.containsKey(tagName);
    }

    /**
      Return information about a tag.
      @param tagName The name of the tag.
      @return the tag information, or null if the tag is not defined.
    */

    public @Nullable TagInfo getTag(@NotNull String tagName)
    {
        return tagMap.get(tagName);
    }

    /**
      Return the description content.
    */

    public @NotNull List<? extends DocTree> getDescription()
    {
        return description;
    }

    /**
      Return a short description content.
    */

    public @NotNull List<? extends DocTree> getShortDescription()
    {
        return shortDescription;
    }

    /**
      Return a medium length description content.
    */

    public @NotNull List<? extends DocTree> getMediumDescription()
    {
        return mediumDescription;
    }
}
