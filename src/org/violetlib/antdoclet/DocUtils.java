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
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.internal.doclets.formats.html.HtmlConfiguration;
import jdk.javadoc.internal.doclets.formats.html.HtmlDocletWriter;
import jdk.javadoc.internal.doclets.toolkit.DocletException;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**

*/

public class DocUtils
{
    public static @NotNull DocUtils create(@NotNull Doclet doclet,
                                           @NotNull DocletEnvironment env,
                                           @NotNull Reporter reporter)
    {
        return new DocUtils(doclet, env, reporter);
    }

    private final @NotNull DocletEnvironment env;
    private final @NotNull DocTrees docTrees;
    private final @NotNull Elements elementUtils;
    private final @NotNull Types typeUtils;
    private final @NotNull Reporter reporter;
    private final @NotNull HtmlDocletWriter writer;

    private DocUtils(@NotNull Doclet doclet, @NotNull DocletEnvironment env, @NotNull Reporter reporter)
    {
        System.err.println("Creating DocUtils");

        this.env = env;
        this.docTrees = env.getDocTrees();
        this.elementUtils = env.getElementUtils();
        this.typeUtils = env.getTypeUtils();
        this.reporter = reporter;

        MyHtmlConfiguration g = new MyHtmlConfiguration(doclet, Locale.ENGLISH, new NullReporter(), env);

        try {
            g.setOptions();
        } catch (DocletException ignore) {
        }

        this.writer = new HtmlDocletWriter(g, DocPath.empty);
    }

    private static class NullReporter
      implements Reporter
    {
        PrintWriter pw = new PrintWriter(Writer.nullWriter());

        @Override
        public void print(Diagnostic.Kind kind, String message)
        {
        }

        @Override
        public void print(Diagnostic.Kind kind, DocTreePath path, String message)
        {
        }

        @Override
        public void print(Diagnostic.Kind kind, Element element, String message)
        {
        }

        @Override
        public PrintWriter getStandardWriter()
        {
            return pw;
        }

        @Override
        public PrintWriter getDiagnosticWriter()
        {
            return pw;
        }
    }

    private static class MyHtmlConfiguration
      extends HtmlConfiguration
    {
        MyHtmlConfiguration(@NotNull Doclet doclet,
                            @NotNull Locale locale,
                            @NotNull Reporter reporter,
                            @NotNull DocletEnvironment env)
        {
            super(doclet, locale, reporter);
            initConfiguration(env, null);
        }

        @Override
        public void runDocLint(TreePath path)
        {
        }
    }

    public @NotNull DocletEnvironment getEnvironment()
    {
        return env;
    }

    public @NotNull Reporter getReporter()
    {
        return reporter;
    }

    public @Nullable TypeElement getType(@NotNull CharSequence name)
    {
        return elementUtils.getTypeElement(name);
    }

    public @NotNull String getSimpleTypeName(@NotNull TypeMirror t)
    {
        Element e = typeUtils.asElement(t);
        if (e != null) {
            return e.getSimpleName().toString();
        }
        return getBasicTypeName(t);
    }

    public @NotNull String getTypeName(@NotNull TypeMirror t)
    {
        Element e = typeUtils.asElement(t);
        if (e instanceof TypeElement te) {
            return te.getQualifiedName().toString();
        }
        if (e != null) {
            return e.getSimpleName().toString();
        }
        return getBasicTypeName(t);
    }

    private @NotNull String getBasicTypeName(@NotNull TypeMirror t)
    {
        TypeKind kind = t.getKind();
        return switch (kind) {
            case CHAR -> "char";
            case BOOLEAN -> "boolean";
            case INT -> "int";
            case BYTE -> "byte";
            case SHORT -> "short";
            case LONG -> "long";
            case FLOAT -> "float";
            case DOUBLE -> "double";
            default -> "Unknown";
        };
    }

    public @Nullable TypeElement getType(@NotNull TypeMirror t)
    {
        Element e = typeUtils.asElement(t);
        if (e instanceof TypeElement) {
            return (TypeElement) e;
        }
        return null;
    }

    /**
      Return the public lexically nested classes of the specified type.
    */

    public @NotNull List<TypeElement> getNestedClasses(@NotNull TypeElement type)
    {
        List<TypeElement> result = new ArrayList<>();
        for (Element member : elementUtils.getAllMembers(type)) {
            if (member instanceof TypeElement te) {
                if (te.getKind().equals(ElementKind.CLASS)
                  && te.getEnclosingElement().equals(type)
                  && te.getModifiers().contains(Modifier.PUBLIC)) {
                    result.add(te);
                }
            }
        }
        return result;
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
        return getText(e, body);
    }

    private @NotNull String getText(@NotNull Element e, @NotNull List<? extends DocTree> trees)
    {
        // return getProcessedText(trees);

        return writer.commentTagsToContent(e, trees, false).toString();
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
        return getText(e, body);
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
        if (tagName.startsWith("@")) {
            throw new AssertionError("Tag name should not start with @: " + tagName);
        }

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
//            System.err.println("No content for tag " + tagName + " on " + e);
//            DocCommentTree tree = docTrees.getDocCommentTree(e);
//            if (tree != null) {
//                Util.show(tree);
//            } else {
//                System.err.println("No tree");
//            }

            return null;
        }

        //System.err.println("Content: " + content.size());

        if (content.isEmpty()) {
            return "";
        }

        if (e == null) {
            return "";
        }

        return getText(e, content);
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

//    public @NotNull String getProcessedText(List<? extends DocTree> content)
//    {
//        StringBuilder sb = new StringBuilder();
//        for (DocTree tag : content) {
//            DocTree.Kind kind = tag.getKind();
//            if (kind == DocTree.Kind.TEXT) {
//                TextTree tt = (TextTree) tag;
//                String text = tt.getBody();
//                System.err.println("Text: " + text);
//                sb.append(text);
//            } else if (kind == DocTree.Kind.CODE) {
//                LiteralTree lt = (LiteralTree) tag;
//                TextTree body = lt.getBody();
//                String text = body.getBody();
//                System.err.println("Text: " + text);
//                sb.append("<code>");
//                sb.append(text);
//                sb.append("</code>");
//            } else if (tag instanceof LiteralTree) {
//                LiteralTree lt = (LiteralTree) tag;
//                TextTree body = lt.getBody();
//                String text = body.getBody();
//                System.err.println("Text: " + text);
//                sb.append(text);
//            } else if (kind == DocTree.Kind.ENTITY) {
//                EntityTree t = (EntityTree) tag;
//                sb.append("&");
//                sb.append(t.getName());
//                sb.append(";");
//            } else if (kind == DocTree.Kind.START_ELEMENT) {
//                StartElementTree t = (StartElementTree) tag;
//                sb.append("<");
//                sb.append(t.getName());
//                for (DocTree arg : t.getAttributes()) {
//                    // TBD
//                }
//                if (t.isSelfClosing()) {
//                    sb.append("/");
//                }
//                sb.append(">");
//            } else if (kind == DocTree.Kind.END_ELEMENT) {
//                EndElementTree t = (EndElementTree) tag;
//                sb.append("</");
//                sb.append(t.getName());
//                sb.append(">");
//            } else {
//                System.err.println("Unknown doc tree kind: " + kind);
//                // TBD
//            }
//        }
//        String result = sb.toString();
//        return result;
//    }
}
