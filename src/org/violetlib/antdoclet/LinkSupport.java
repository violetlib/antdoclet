/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

package org.violetlib.antdoclet;

import com.sun.source.doctree.ReferenceTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
  Create links to type documentation.
*/

public class LinkSupport
{
    public static @NotNull LinkSupport create(@NotNull Set<? extends Element> includedElements,
                                              @NotNull AntDocCache antDocs,
                                              @NotNull Environment env)
    {
        return new LinkSupport(includedElements, antDocs, env);
    }

    private final @NotNull Set<? extends Element> includedElements;
    private final @NotNull AntDocCache antDocs;
    private final @NotNull Environment env;

    private LinkSupport(@NotNull Set<? extends Element> includedElements,
                        @NotNull AntDocCache antDocs,
                        @NotNull Environment env)
    {
        this.includedElements = includedElements;
        this.antDocs = antDocs;
        this.env = env;  // warning: not initialized yet
    }

    /**
      Return HTML with a possible link for a type name.
      <p>
      The type name might be a simple Class name, a qualified Class name, or the user-visible name of
      an Ant type.
    */

    public @NotNull String getTypeNameLinked(@NotNull String typeName, @Nullable TypeElement te)
    {
        if (te != null) {
            URI link = getLinkTarget(te);
            if (link != null) {
                return getTextWithLink(typeName, link);
            }
        }
        return getTextWithLink(typeName, getLinkTarget(te, typeName));
    }

    public @NotNull String getTextWithLink(@NotNull String text, @Nullable URI target)
    {
        if (target != null) {
            String urlText = target.toString();
            return String.format("<a href=\"%s\">%s</a>", urlText, text);
        }
        return text;
    }

    public static class InvalidLinkException
      extends Exception
    {
        private final @NotNull String target;

        public InvalidLinkException(@NotNull String target)
        {
            this.target = target;
        }

        public @NotNull String getTarget()
        {
            return target;
        }
    }

    public @NotNull URI getReferenceLinkTarget(@NotNull Element context, @NotNull ReferenceTree r)
      throws InvalidLinkException
    {
        TypeElement te = env.getTypeReference(context, r);
        if (te != null) {
            URI u = getLinkTarget(te);
            if (u != null) {
                return u;
            }
        }

        URI u = getLinkTarget(context, r.getSignature());
        if (u != null) {
            return u;
        }

        if (te != null) {
            throw new InvalidLinkException(te.getQualifiedName().toString());
        } else {
            throw new InvalidLinkException(r.getSignature());
        }
    }

    /**
      Return a link destination for a type name.
      <p>
      The type name might be a simple Class name, a qualified Class name, or the user-visible name of
      an Ant type.
    */

    public @Nullable URI getLinkTarget(@Nullable Element context, @NotNull String typeName)
    {
        // Test to see if the type has a page in this documentation set
        for (Element e : includedElements) {
            if (e instanceof TypeElement te) {
                if (typeName.equals(te.getSimpleName().toString())) {
                    return getLinkTarget(te);
                }
                if (typeName.equals(te.getQualifiedName().toString())) {
                    return getLinkTarget(te);
                }
            }
        }

        // Test to see whether a "user" type name has been provided
        AntDoc d = antDocs.get(typeName);
        if (d != null && d.isType()) {
            String qn = d.getFullClassName();
            for (Element e : includedElements) {
                if (e instanceof TypeElement te) {
                    if (qn.equals(te.getQualifiedName().toString())) {
                        return getLinkTarget(te);
                    }
                }
            }
        }

        // Special case for testing
        if (typeName.equals("File")) {
            String link = "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/File.html";
            try {
                return new URI(link);
            } catch (URISyntaxException e) {
                System.out.println("Unexpected exception: " + e);
            }
        }

        return null;
    }

    public @Nullable URI getLinkTarget(@NotNull TypeElement te)
    {
        // Test to see if the type has a page in this documentation set
        if (includedElements.contains(te)) {
            String link = te.getQualifiedName() + ".html";
            try {
                return new URI(null, null, link, null);
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        // Special case for testing
        String qn = te.getQualifiedName().toString();
        if (qn.equals("java.io.File")) {
            String link = "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/File.html";
            try {
                return new URI(link);
            } catch (URISyntaxException e) {
                System.out.println("Unexpected exception: " + e);
            }
        }

        return null;
    }
}