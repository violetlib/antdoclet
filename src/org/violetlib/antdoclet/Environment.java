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
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**

*/

public class Environment
{
    public static @NotNull Environment create(@NotNull DocletEnvironment env,
                                              @NotNull Doclet doclet,
                                              @NotNull Reporter reporter)
    {
        return new Environment(env, doclet, reporter);
    }

    private final @NotNull DocletEnvironment env;
    private final @NotNull DocUtils docUtils;
    private final @NotNull Reporter reporter;
    private final @NotNull LinkSupport linkSupport;
    private final @NotNull AnalysisCache analysisCache;
    private final @NotNull AntDocCache antDocCache;
    private final @NotNull AntRoot root;
    private final @NotNull ContentProcessing contentProcessing;

    private Environment(@NotNull DocletEnvironment env,
                        @NotNull Doclet doclet,
                        @NotNull Reporter reporter
    )
    {
        this.env = env;
        this.reporter = reporter;

        this.antDocCache = AntDocCache.create(this);
        this.linkSupport = LinkSupport.create(this);
        this.contentProcessing = ContentProcessing.create(linkSupport, reporter);
        this.docUtils = DocUtils.create(doclet, env, contentProcessing, reporter);
        this.analysisCache = AnalysisCache.create(docUtils);
        this.root = AntRoot.create(antDocCache, env.getIncludedElements());
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

    public @NotNull Set<Element> getIncludedElements()
    {
        return new HashSet<>(env.getIncludedElements());
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
        List<AntDoc> ds = root.getAllDocumentedEntities();

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
      Return the textual content of a doc comment associated with an element.
      @param e The element.
      @return the doc comment text, or null if none.
    */

    public @Nullable String getDescription(@Nullable Element e)
    {
        return comment(docUtils.getComment(e));
    }

    public @Nullable String getShortDescription(@NotNull Element e)
    {
        return comment(docUtils.getShortComment(e));
    }

    private @Nullable String comment(@Nullable String s)
    {
        if (s != null) {
            //System.out.println("Comment: " + s);
        }
        return s;
    }

    public @Nullable String tagRawValue(@Nullable Element e, @NotNull String tagName)
    {
        return docUtils.tagRawValue(e, tagName);
    }

    public @Nullable String tagValue(@Nullable Element e, @NotNull String tagName)
    {
        return docUtils.tagValue(e, tagName);
    }

    /**
      Returns the unprocessed textual value of the designated attribute of the first (custom) javadoc tag with the given
      name.

      @return the text, or null if no tag with the specified name is present or no attribute with the specified name is
      present.
    */

    public @Nullable String tagAttributeRawValue(@Nullable Element e,
                                                 @NotNull String tagName,
                                                 @NotNull String attribute)
    {
        return docUtils.tagAttributeRawValue(e, tagName, attribute);
    }

    /**
      Returns the textual value of the designated attribute of the first (custom) javadoc tag with the given name.

      @return the text, or null if no tag with the specified name is present or no attribute with the specified name is
      present.
    */

    public @Nullable String tagAttributeValue(@Nullable Element e, @NotNull String tagName, @NotNull String attribute)
    {
        return docUtils.tagAttributeValue(e, tagName, attribute);
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
