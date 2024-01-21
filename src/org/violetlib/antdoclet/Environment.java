/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

package org.violetlib.antdoclet;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.TreePath;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**

*/

public class Environment
{
    public static @NotNull Environment create(@NotNull DocletEnvironment env, @NotNull Reporter reporter)
    {
        return new Environment(env, reporter);
    }

    private final @NotNull DocletEnvironment env;
    private final @NotNull DocUtils docUtils;
    private final @NotNull Reporter reporter;
    private final @NotNull LinkSupport linkSupport;
    private final @NotNull AnalysisCache analysisCache;
    private final @NotNull AntDocCache antDocCache;
    private final @NotNull AntRoot root;
    private final @NotNull Map<Element,AugmentedDocCommentInfo> docCommentCache = new HashMap<>();

    private Environment(@NotNull DocletEnvironment env, @NotNull Reporter reporter)
    {
        this.env = env;
        this.reporter = reporter;

        this.antDocCache = AntDocCache.create(this);
        this.linkSupport = LinkSupport.create(this);
        this.docUtils = DocUtils.create(env, reporter);
        this.analysisCache = AnalysisCache.create(docUtils);
        this.root = ProjectBuilder.build(antDocCache, env.getIncludedElements());
    }

    public @NotNull AntRoot getRoot()
    {
        return root;
    }

    public @NotNull Reporter getReporter()
    {
        return reporter;
    }

    public boolean isIncluded(@NotNull TypeMirror t)
    {
        TypeElement te = docUtils.getType(t);
        return te != null && isIncluded(te);
    }

    public boolean isIncluded(@NotNull TypeElement te)
    {
        return root.isIncluded(te);
    }

    public @Nullable AntDoc getAntDoc(@NotNull TypeElement te)
    {
        return antDocCache.get(te);
    }

    public @Nullable AntDoc getAntDoc(@NotNull String userName)
    {
        return antDocCache.get(userName);
    }

    public @Nullable TypeElement getTypeElement(@NotNull TypeMirror t)
    {
        return docUtils.getType(t);
    }

    public @Nullable TypeInfo getTypeInfo(@NotNull TypeElement te)
    {
        return analysisCache.getInfo(te);
    }

    /**
      Return the type element corresponding to the specified name, if the type is included in the documentation
      (i.e, it will have a documentation page).
      @param name A user name, simple type name, or fully qualified type name.
      @return the type element, or null if the name cannot be mapped to a type that is included in the documentation.
    */

    public @Nullable TypeElement getIncludedTypeElement(@NotNull String name)
    {
        Collection<AntDoc> ds = root.getAllDocumentedEntities();

        AntDoc named = antDocCache.get(name);
        if (named != null && ds.contains(named)) {
            return named.getTypeElement();
        }
        for (AntDoc d : ds) {
            TypeElement te = d.getTypeElement();
            if (te.getSimpleName().toString().equals(name)) {
                return te;
            }
            if (te.getQualifiedName().toString().equals(name)) {
                return te;
            }
        }
        return null;
    }

    public @Nullable TypeElement getTypeElement(@NotNull CharSequence name)
    {
        TypeElement te = env.getElementUtils().getTypeElement(name);
        if (te != null) {
            return te;
        }

        String ns = name.toString();
        for (Element e : env.getIncludedElements()) {
            if (e.getSimpleName().toString().equals(ns)) {
                if (e instanceof TypeElement type) {
                    return type;
                }
            }
        }

        return null;
    }

    public @Nullable TypeElement getTypeReference(@NotNull Element context, @NotNull ReferenceTree r)
    {
        Element ref = getReferenceElement(context, r);
        if (ref instanceof TypeElement te) {
            return te;
        }
        return getTypeElement(r.getSignature());
    }

    private @Nullable Element getReferenceElement(@NotNull Element context, @NotNull ReferenceTree r)
    {
        DocCommentTree ct = env.getDocTrees().getDocCommentTree(context);
        TreePath cp = env.getDocTrees().getPath(context);
        CompilationUnitTree cut = cp.getCompilationUnit();
        TreePath tp = new TreePath(cut);
        DocTreePath cdp = new DocTreePath(tp, ct);
        DocTreePath rp = DocTreePath.getPath(cdp, r);
        return env.getDocTrees().getElement(rp);
    }

    public @NotNull String getTypeName(@NotNull TypeMirror t)
    {
        return docUtils.getTypeName(t);
    }

    public @NotNull String getTypeNameLinked(@NotNull TypeMirror t)
    {
        TypeElement te = getTypeElement(t);
        return te != null ? getTypeNameLinked(te) : getTypeName(t);
    }

    public @Nullable String getTypeNameLinked(@Nullable Element context, @NotNull String typeName)
    {
        TypeElement te = getTypeElement(typeName);

        if (te == null && context != null) {
            String qt = getQualifiedTypeName(context, typeName);
            if (qt != null) {
                te = getTypeElement(qt);
            }
        }

        return te != null ? getTypeNameLinked(te) : linkSupport.getTypeNameLink(typeName);
    }

    public @NotNull String getTypeNameLinked(@NotNull TypeElement te)
    {
        String typeName = te.getSimpleName().toString();
        return linkSupport.getTypeNameLinked(typeName, te);
    }

    public @NotNull String getSimpleTypeName(@NotNull TypeMirror t)
    {
        return docUtils.getSimpleTypeName(t);
    }

    public @Nullable String getQualifiedTypeName(@NotNull Element context, @NotNull String tn)
    {
        // A placeholder for possible future support for resolving type names in a lexical context.
        return null;
    }

    /**
      Return information obtained from the documentation comment associated with an element.
      @param e The element.
      @return the documentation comment information, or null if {@code e} is null or has no documentation comment.
    */

    public @Nullable AugmentedDocCommentInfo getDocCommentInfo(@Nullable Element e)
    {
        if (e == null) {
            return null;
        }
        AugmentedDocCommentInfo info = docCommentCache.get(e);
        if (info != null) {
            return info;
        }
        DocCommentTree dc = env.getDocTrees().getDocCommentTree(e);
        if (dc == null) {
            return null;
        }
        ElementContentProcessing ecp = ElementContentProcessing.create(e, linkSupport, reporter);
        info = DocCommentAnalyzer.analyze(dc, ecp);
        docCommentCache.put(e, info);
        return info;
    }

    /**
      Return the HTML description of an element.
      @param e The element.
      @return the description text, or null if none.
    */

    public @Nullable String getDescription(@Nullable Element e)
    {
        AugmentedDocCommentInfo info = getDocCommentInfo(e);
        return info != null ? info.getHtmlDescription() : null;
    }

    /**
      Return the HTML short description of an element.
      @param e The element.
      @return the description text, or null if none.
    */

    public @Nullable String getShortDescription(@NotNull Element e)
    {
        AugmentedDocCommentInfo info = getDocCommentInfo(e);
        return info != null ? info.getHtmlShortDescription() : null;
    }

    /**
      Return the HTML medium length description of an element.
      @param e The element.
      @return the description text, or null if none.
    */

    public @Nullable String getMediumDescription(@NotNull Element e)
    {
        AugmentedDocCommentInfo info = getDocCommentInfo(e);
        return info != null ? info.getHtmlMediumDescription() : null;
    }

    /**
      Indicate whether a tag with the specified name is present for the specified element.
      @param e The element.
      @param tagName The tag name.
      @return true if and only if the specified tag is present.
    */

    public boolean hasTag(@NotNull Element e, @NotNull String tagName)
    {
        AugmentedDocCommentInfo info = getDocCommentInfo(e);
        return info != null && info.hasTag(tagName);
    }

    /**
      Return the content of the specified tag for the specified element.
      @param e The element.
      @param tagName The tag name.
      @return the tag content, as HTML, or null if the tag is not present.
    */

    public @Nullable String tagContent(@Nullable Element e, @NotNull String tagName)
    {
        AugmentedDocCommentInfo info = getDocCommentInfo(e);
        if (info == null) {
            return null;
        }
        AugmentedTagInfo t = info.getTag(tagName);
        return t != null ? t.getHtmlContent() : null;
    }

    /**
      Returns the textual value of the designated attribute of the first (custom) javadoc tag with the given name.

      @return the text, or null if no tag with the specified name is present or no attribute with the specified name is
      present.
    */

    public @Nullable String tagAttributeValue(@Nullable Element e, @NotNull String tagName, @NotNull String attribute)
    {
        AugmentedDocCommentInfo info = getDocCommentInfo(e);
        if (info == null) {
            return null;
        }
        AugmentedTagInfo t = info.getTag(tagName);
        if (t == null) {
            return null;
        }
        return t.getAttributeValue(attribute);
    }

    public long getLineNumber(@NotNull Element e)
    {
        return docUtils.getLineNumber(e);
    }

    public boolean isSubtypeOf(@NotNull TypeMirror m, @NotNull String typeName)
    {
        TypeElement te = getTypeElement(typeName);
        if (te != null) {
            return docUtils.isSubtypeOf(m, te.asType());
        }
        reporter.print(Diagnostic.Kind.WARNING, "Type not found: " + typeName);
        return false;
    }

    public @NotNull String getAntCategoryPrefix(@NotNull String category)
    {
        return root.getAntCategoryPrefix(category);
    }
}
