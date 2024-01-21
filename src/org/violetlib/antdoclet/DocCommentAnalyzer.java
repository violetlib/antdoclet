/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

package org.violetlib.antdoclet;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**

*/

public class DocCommentAnalyzer
{
    public static @NotNull AugmentedDocCommentInfo analyze(@NotNull DocCommentTree dc,
                                                           @NotNull ElementContentProcessing cp)
    {
        return new DocCommentAnalyzer(dc, cp).getResult();
    }

    private final @NotNull DocCommentTree dc;
    private final @NotNull ElementContentProcessing cp;

    private DocCommentAnalyzer(@NotNull DocCommentTree dc, @NotNull ElementContentProcessing cp)
    {
        this.dc = dc;
        this.cp = cp;
    }

    private @NotNull AugmentedDocCommentInfo getResult()
    {
        List<? extends DocTree> description = dc.getFullBody();
        List<? extends DocTree> shortDescription = dc.getFirstSentence();
        List<? extends DocTree> mediumDescription = shortDescription; // TBD

        Map<String,TagInfo> tagMap = collectTags(dc);
        DocCommentInfo basic
          = DocCommentInfo.create(cp.getElement(), description, shortDescription, mediumDescription, tagMap);
        return AugmentedDocCommentInfo.create(basic, cp);
    }

    private @NotNull Map<String,TagInfo> collectTags(@NotNull DocCommentTree dc)
    {
        Map<String,TagInfo> tagMap = new HashMap<>();

        List<? extends DocTree> blockTags = dc.getBlockTags();
        for (DocTree tag : blockTags) {
            if (tag instanceof UnknownBlockTagTree bt) {
                String tagName = bt.getTagName();
                if (tagName.startsWith("ant.")) {
                    List<? extends DocTree> tagContent = bt.getContent();
                    AugmentedTagInfo t = analyzeTag(tagName, (List) tagContent);
                    tagMap.put(tagName, t);
                }
            }
        }

        return tagMap;
    }

    private @NotNull AugmentedTagInfo analyzeTag(@NotNull String tagName, @NotNull List<DocTree> fullContent)
    {
        Map<String,TagAttributeInfo> attributeMap = new HashMap<>();

        // If there are attributes, they should not contain any inline tags. Therefore, they should all be in
        // the first DocTree, which should be text.

        if (!fullContent.isEmpty()) {
            DocTree first = fullContent.get(0);
            if (first instanceof TextTree tt) {
                String firstText = tt.getBody();
                int charCount = parseAttributes(tagName, firstText, attributeMap);
                if (charCount == firstText.length()) {
                    fullContent = fullContent.subList(1, fullContent.size());
                } else if (charCount > 0) {
                    String remainingText = firstText.substring(charCount);
                    fullContent = new ArrayList<>(fullContent);
                    fullContent.set(0, TextNode.create(remainingText));
                }
            }
        }

        TagInfo basic = TagInfo.create(cp.getElement(), tagName, attributeMap, fullContent);
        return AugmentedTagInfo.create(basic, cp);
    }

    /**
      Parse attributes at the beginning of the specified text and add them to the map.
      @return the number of characters to remove from the text to get the content.
    */

    private int parseAttributes(@NotNull String tagName,
                                @NotNull String text,
                                @NotNull Map<String,TagAttributeInfo> attributeMap)
    {
        // This pattern requires an attribute value to contain non-space characters other than double quote.
        // It allows the value to be surrounded by double quote (without requiring the start and the end
        // to use the same convention?). It allows separation using space characters only.

        Pattern pattern = Pattern.compile(" *(\\w+) *= *\"?([^\\s\"]+)\"? *");
        Matcher matcher = pattern.matcher(text);

        int lastEnd = 0;
        while (matcher.lookingAt()) {
            String name = matcher.group(1);
            String value = matcher.group(2);
            TagAttributeInfo a = TagAttributeInfo.create(cp.getElement(), name, value);
            attributeMap.put(name, AugmentedTagAttributeInfo.create(a, cp));
            int end = matcher.end();
            matcher.region(end, text.length());
            lastEnd = end;
        }

        return lastEnd;
    }
}
