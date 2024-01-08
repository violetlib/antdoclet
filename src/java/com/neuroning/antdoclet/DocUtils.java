/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

package com.neuroning.antdoclet;

import com.sun.source.doctree.*;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**

*/

public class DocUtils
{
    public static @NotNull DocUtils create(@NotNull DocletEnvironment env, @NotNull Reporter reporter)
    {
        return new DocUtils(env, reporter);
    }

    private final @NotNull DocletEnvironment env;
    private final @NotNull DocTrees docTrees;
    private final @NotNull Elements elementUtils;
    private final @NotNull Types typeUtils;
    private final @NotNull Reporter reporter;

    private DocUtils(@NotNull DocletEnvironment env, @NotNull Reporter reporter)
    {
        this.env = env;
        this.docTrees = env.getDocTrees();
        this.elementUtils = env.getElementUtils();
        this.typeUtils = env.getTypeUtils();
        this.reporter = reporter;
    }

    public @NotNull Reporter getReporter()
    {
        return reporter;
    }

    public @Nullable TypeElement getType(@NotNull CharSequence name)
    {
        return elementUtils.getTypeElement(name);
    }

    public @NotNull String getTypeName(@NotNull TypeMirror t)
    {
        Element e = typeUtils.asElement(t);
        if (e != null) {
            return e.getSimpleName().toString();
        }
        return "Unknown";
    }

    public @Nullable TypeElement getType(@NotNull TypeMirror t)
    {
        Element e = typeUtils.asElement(t);
        if (e instanceof TypeElement) {
            return (TypeElement) e;
        }
        return null;
    }

    public @NotNull List<ExecutableElement> getMethods(@NotNull TypeElement type)
    {
        List<ExecutableElement> result = new ArrayList<>();
        for (Element member : elementUtils.getAllMembers(type)) {
            if (member instanceof ExecutableElement) {
                String name = member.getSimpleName().toString();
                if (!name.isEmpty() && !name.startsWith("<")) {
                    result.add((ExecutableElement) member);
                }
            }
        }
        return result;
    }

    public @NotNull List<ExecutableElement> getConstructors(@NotNull TypeElement type)
    {
        List<ExecutableElement> result = new ArrayList<>();
        for (Element member : elementUtils.getAllMembers(type)) {
            if (member instanceof ExecutableElement) {
                String name = member.getSimpleName().toString();
                if (name.equals("<init>")) {
                    result.add((ExecutableElement) member);
                }
            }
        }
        return result;
    }

    public boolean isSubtypeOf(@NotNull TypeMirror t1, @NotNull TypeMirror t2)
    {
        return typeUtils.isSubtype(t1, t2);
    }

    public long getLineNumber(Element e)
    {
        TreePath path = docTrees.getPath(e);
        if (path == null) {
            return 0;
        }
        CompilationUnitTree cu = path.getCompilationUnit();
        LineMap lineMap = cu.getLineMap();
        DocSourcePositions spos = docTrees.getSourcePositions();
        long pos = spos.getStartPosition(cu, path.getLeaf());
        return lineMap.getLineNumber(pos);
    }

    /**
      Return the textual content of a doc comment associated with an element.
      @param e The element.
      @return the doc comment text, or null if none.
    */

    public @Nullable String getComment(@Nullable Element e)
    {
        if (e == null) {
            return null;
        }
        DocCommentTree tree = docTrees.getDocCommentTree(e);
        if (tree == null) {
            return null;
        }
        List<? extends DocTree> body = tree.getFullBody();
        return getProcessedText(body);
    }

    /**
      Return the textual content of the first sentence of a doc comment associated with an element.
      @param e The element.
      @return the doc comment text, or null if none.
    */

    public @Nullable String getShortComment(@Nullable Element e)
    {
        if (e == null) {
            return null;
        }
        DocCommentTree tree = docTrees.getDocCommentTree(e);
        if (tree == null) {
            return null;
        }
        List<? extends DocTree> body = tree.getFirstSentence();
        return getProcessedText(body);
    }

    /**
      Returns the textual value of the designated attribute of the first (custom) javadoc tag with the given name.

      @return the text, or null if no tag with the specified name is present or not attribute with the
      specified name is present.
    */

    public @Nullable String tagAttributeValue(@Nullable Element e,
                                              @NotNull String tagName,
                                              @NotNull String attributeName)
    {
        String value = tagValue(e, tagName);
        if (value == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("(\\w+) *= *\"?([^\\s\",]+)\"?");
        Matcher matcher = pattern.matcher(value);

        while (matcher.find()) {
            String key = matcher.group(1);
            if (attributeName.equalsIgnoreCase(key)) {
                return matcher.group(2);
            }
        }

        return null;
    }

    /**
      Returns the textual value of the first (custom) javadoc tag with the given name.

      @return the text, or null if no tag with the specified name is present.
    */

    public @Nullable String tagValue(@Nullable Element e, @NotNull String tagName)
    {
        List<? extends DocTree> content = tagContent(e, tagName);
        if (content == null) {
            //System.err.println("No contents for tag " + tagName + " on " + e);
            return null;
        }

        if (content.isEmpty()) {
            return "";
        }

        return getProcessedText(content);
    }

    /**
      Returns the content of the first (custom) javadoc tag with the given name.

      @return the content, or null if no tag with the specified name is present.
    */

    public @Nullable List<? extends DocTree> tagContent(@Nullable Element e, @NotNull String tagName)
    {
        UnknownBlockTagTree t = getTag(e, tagName);
        if (t == null) {
            return null;
        }
        return t.getContent();
    }

    /**
      Returns the first (custom) javadoc tag with the given name.

      @return the tag definition, or null if no tag with the specified name is present.
    */

    public @Nullable UnknownBlockTagTree getTag(@Nullable Element e, @NotNull String tagName)
    {
        if (e == null) {
            return null;
        }

        DocCommentTree tree = docTrees.getDocCommentTree(e);
        if (tree == null) {
            return null;
        }

        List<? extends DocTree> tags = tree.getBlockTags();
        for (DocTree tag : tags) {
            if (tag instanceof UnknownBlockTagTree) {
                UnknownBlockTagTree u = (UnknownBlockTagTree) tag;
                if (u.getTagName().equals(tagName)) {
                    return u;
                }
            }
        }
        return null;
    }

    public @NotNull String getProcessedText(List<? extends DocTree> content)
    {
        StringBuilder sb = new StringBuilder();
        for (DocTree tag : content) {
            DocTree.Kind kind = tag.getKind();
            if (kind == DocTree.Kind.TEXT) {
                TextTree tt = (TextTree) tag;
                sb.append(tt.getBody());
            } else if (tag instanceof LiteralTree) {
                LiteralTree lt = (LiteralTree) tag;
                TextTree body = lt.getBody();
                sb.append("<code>");
                sb.append(body.getBody());
                sb.append("</code>");
            } else {
                // TBD
            }
        }
        return sb.toString();
    }
}
