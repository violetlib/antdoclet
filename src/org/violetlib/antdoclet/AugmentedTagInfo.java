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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**

*/

public class AugmentedTagInfo
  extends TagInfo
{
    public static @NotNull AugmentedTagInfo create(@NotNull TagInfo base, @NotNull ElementContentProcessing cp)
    {
        return new AugmentedTagInfo(base, cp);
    }

    private final @NotNull ElementContentProcessing cp;

    private @Nullable String _html_Content;

    private AugmentedTagInfo(@NotNull TagInfo base, @NotNull ElementContentProcessing cp)
    {
        super(base, createAugmentedAttributeMap(base, cp));

        this.cp = cp;
    }

    private static Map<String,TagAttributeInfo> createAugmentedAttributeMap(@NotNull TagInfo base,
                                                                            @NotNull ElementContentProcessing cp)
    {
        Map<String,TagAttributeInfo> result = new HashMap<>();
        for (String attributeName : base.getAttributeNames()) {
            TagAttributeInfo ab = base.getAttribute(attributeName);
            assert ab != null;
            AugmentedTagAttributeInfo aa = AugmentedTagAttributeInfo.create(ab, cp);
            result.put(attributeName, aa);
        }
        return result;
    }

    /**
      Return the information for an attribute.
    */

    @Override
    public @Nullable AugmentedTagAttributeInfo getAttribute(@NotNull String attributeName)
    {
        return (AugmentedTagAttributeInfo) super.getAttribute(attributeName);
    }

    /**
      Return the tag content as HTML.
    */

    public @NotNull String getHtmlContent()
    {
        if (_html_Content == null) {
            List<? extends DocTree> content = getContent();
            _html_Content = content.isEmpty() ? "" : cp.toHTML(content);
        }
        return _html_Content;
    }

    /**
      Return the value of an attribute as HTML.
      @param attributeName The name of the attribute.
      @return the attribute value, or null if the attribute is not defined.
    */

    public @Nullable String getHtmlAttributeValue(@NotNull String attributeName)
    {
        AugmentedTagAttributeInfo a = getAttribute(attributeName);
        return a != null ? a.getHtmlValue() : null;
    }
}
