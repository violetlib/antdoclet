/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

package org.violetlib.antdoclet;

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
import java.util.Set;

/**

*/

public class Environment
{
    public static @NotNull Environment create(@NotNull DocletEnvironment env,
                                              @NotNull Doclet doclet,
                                              @NotNull Reporter reporter,
                                              @NotNull LinkSupport linkSupport)
    {
        return new Environment(env, doclet, reporter, linkSupport);
    }

    private final @NotNull DocletEnvironment env;
    private final @NotNull DocUtils docUtils;
    private final @NotNull Reporter reporter;
    private final @NotNull LinkSupport linkSupport;
    private final @NotNull AnalysisCache analysisCache;
    private final @NotNull AntDocCache antDocCache;
    private final @NotNull AntRoot root;

    private Environment(@NotNull DocletEnvironment env,
                        @NotNull Doclet doclet,
                        @NotNull Reporter reporter,
                        @NotNull LinkSupport linkSupport
    )
    {
        this.env = env;
        this.reporter = reporter;
        this.linkSupport = linkSupport;
        this.docUtils = DocUtils.create(doclet, env, reporter);
        this.analysisCache = AnalysisCache.create(docUtils);
        this.antDocCache = AntDocCache.create(this);
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

    public boolean isInnerClass(@NotNull TypeElement te)
    {
        // TBD: not sure this works

        Element outer = env.getElementUtils().getOutermostTypeElement(te);
        return outer != null && outer != te;
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

    public @Nullable TypeElement getTypeElement(@NotNull TypeMirror t)
    {
        return docUtils.getType(t);
    }

    public @Nullable TypeInfo getTypeInfo(@NotNull TypeElement te)
    {
        return analysisCache.getInfo(te);
    }

    public @Nullable TypeElement getTypeElement(@NotNull CharSequence name)
    {
        return env.getElementUtils().getTypeElement(name);
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

    public @NotNull String getTypeNameLinked(@NotNull TypeElement te)
    {
        String typeName = te.getSimpleName().toString();
        if (isIncluded(te)) {
            return linkSupport.getTypeNameLinked(typeName, te);
        }
        return typeName;
    }

    public @NotNull String getSimpleTypeName(@NotNull TypeMirror t)
    {
        return docUtils.getSimpleTypeName(t);
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
            //System.err.println("Comment: " + s);
        }
        return s;
    }

    public @Nullable String tagValue(@Nullable Element e, @NotNull String tagName)
    {
        return docUtils.tagValue(e, tagName);
    }

    /**
      Returns the textual value of the designated attribute of the first (custom) javadoc tag with the given name.

      @return the text, or null if no tag with the specified name is present or not attribute with the
      specified name is present.
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
