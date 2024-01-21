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

import java.util.HashMap;
import java.util.Map;

/**

*/

public class AugmentedDocCommentInfo
  extends DocCommentInfo
{
    public static @NotNull AugmentedDocCommentInfo create(@NotNull DocCommentInfo base,
                                                          @NotNull ElementContentProcessing cp)
    {
        return new AugmentedDocCommentInfo(base, cp);
    }

    private final @NotNull ElementContentProcessing cp;

    private @Nullable String _html_description;
    private @Nullable String _html_short_description;
    private @Nullable String _html_medium_description;

    private AugmentedDocCommentInfo(@NotNull DocCommentInfo base, @NotNull ElementContentProcessing cp)
    {
        super(base, createAugmentedTagMap(base, cp));

        this.cp = cp;
    }

    private static Map<String,TagInfo> createAugmentedTagMap(@NotNull DocCommentInfo base,
                                                             @NotNull ElementContentProcessing cp)
    {
        Map<String,TagInfo> result = new HashMap<>();
        for (String tagName : base.getTagNames()) {
            TagInfo info = base.getTag(tagName);
            assert info != null;
            AugmentedTagInfo a = AugmentedTagInfo.create(info, cp);
            result.put(tagName, a);
        }
        return result;
    }

    @Override
    public @Nullable AugmentedTagInfo getTag(@NotNull String tagName)
    {
        return (AugmentedTagInfo) super.getTag(tagName);
    }

    /**
      Return the description content.
    */

    public @NotNull String getHtmlDescription()
    {
        if (_html_description == null) {
            _html_description = cp.toHTML(getDescription());
        }
        return _html_description;
    }

    /**
      Return a short description content.
    */

    public @NotNull String getHtmlShortDescription()
    {
        if (_html_short_description == null) {
            _html_short_description = cp.toHTML(getShortDescription());
        }
        return _html_short_description;
    }

    /**
      Return a medium length description content.
    */

    public @NotNull String getHtmlMediumDescription()
    {
        if (_html_medium_description == null) {
            _html_medium_description = cp.toHTML(getMediumDescription());
        }
        return _html_medium_description;
    }
}
