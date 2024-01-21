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
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
  Create links to type documentation.
*/

public class LinkSupport
{
    public static @NotNull LinkSupport create(@NotNull Environment env)
    {
        return new LinkSupport(env);
    }

    private final @NotNull Environment env;
    private final @NotNull Map<String,String> antLinks;

    private LinkSupport(@NotNull Environment env)
    {
        this.env = env;  // warning: not initialized yet
        this.antLinks = getAntDocumentationLinks();
    }

    /**
      Return HTML with a link for a type name.
      @param typeName The type name, which might be a simple Class name, a qualified Class name, or the user-visible
      name of an Ant type.
      @return a link to the documentation of the named type, or null if the type name could not be resolved.
    */

    public @Nullable String getTypeNameLink(@NotNull String typeName)
    {
        URI u = getLinkTarget(null, typeName);
        return u != null ? getTextWithLink(typeName, u) : null;
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
        private final @Nullable TypeElement te;

        public InvalidLinkException(@NotNull String target)
        {
            this.target = target;
            this.te = null;
        }

        public InvalidLinkException(@NotNull TypeElement te)
        {
            this.target = te.getQualifiedName().toString();
            this.te = te;
        }

        public @Nullable TypeElement getTypeElement()
        {
            return te;
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
            throw new InvalidLinkException(te);
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
        TypeElement te = env.getIncludedTypeElement(typeName);
        if (te != null) {
            return createLinkTargetForIncludedType(te);
        }

        if (context != null) {
            String qn = env.getQualifiedTypeName(context, typeName);
            if (qn != null) {
                TypeElement te1 = env.getIncludedTypeElement(qn);
                if (te1 != null) {
                    return createLinkTargetForIncludedType(te1);
                }
            }
        }

        return getSpecialLinkTarget(typeName);
    }

    private @Nullable URI getSpecialLinkTarget(@NotNull String typeName)
    {
        // Special cases for testing and utility
        if (typeName.equals("File")) {
            return getJavaLink("java.io", typeName);
        }
        if (typeName.equals("String")) {
            return getJavaLink("java.lang", typeName);
        }
        URI antLink = getAntLink(typeName);
        if (antLink != null) {
            return antLink;
        }
        return null;
    }

    private @NotNull URI getJavaLink(@NotNull String packageName, @NotNull String type)
    {
        String ps = packageName.replace(".", "/");
        String link = String.format("https://docs.oracle.com/en/java/javase/21/docs/api/java.base/%s/%s.html",
          ps, type);
        try {
            return new URI(link);
        } catch (URISyntaxException e) {
            throw new AssertionError("Unexpected exception: " + e);
        }
    }

    private @Nullable URI getAntLink(@NotNull String type)
    {
        String link = antLinks.get(type);
        if (link != null) {
            try {
                return new URI(link);
            } catch (URISyntaxException e) {
                throw new AssertionError("Unexpected exception: " + e);
            }
        }
        return null;
    }

    /**
      Return a link destination for a type.
    */

    public @Nullable URI getLinkTarget(@NotNull TypeElement te)
    {
        // Test to see if the type has a page in this documentation set
        if (env.getIncludedTypeElement(te.getQualifiedName().toString()) != null) {
            return createLinkTargetForIncludedType(te);
        }

        // Special case for Java symbols
        ModuleElement me = Util.getModule(te);
        if (me != null) {
            String mn = me.getQualifiedName().toString();
            if (mn.startsWith("java.") || mn.startsWith("jdk.")) {
                String qn = te.getQualifiedName().toString().replace(".", "/");
                String link = "https://docs.oracle.com/en/java/javase/21/docs/api/" + mn + "/" + qn + ".html";
                try {
                    return new URI(link);
                } catch (URISyntaxException e) {
                    System.out.println("Unexpected exception: " + e);
                }
            }
        }

        return null;
    }

    private @NotNull URI createLinkTargetForIncludedType(@NotNull TypeElement te)
    {
        String link = te.getQualifiedName() + ".html";
        try {
            return new URI(null, null, link, null);
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception: " + e);
        }
    }

    private @NotNull Map<String,String> getAntDocumentationLinks()
    {
        Map<String,String> m = new HashMap<>();
        m.put("Reference", "https://ant.apache.org/manual/using.html#references");
        m.put("FileSet", "https://ant.apache.org/manual/Types/classfileset.html");
        m.put("PatternSet", "https://ant.apache.org/manual/Types/patternset.html");
        m.put("DirSet", "https://ant.apache.org/manual/Types/dirset.html");
        m.put("FileList", "https://ant.apache.org/manual/Types/filelist.html");
        m.put("ClassFileSet", "https://ant.apache.org/manual/Types/classfileset.html");
        m.put("FilterSet", "https://ant.apache.org/manual/Types/filterset.html");
        m.put("MultiRootFileSet", "https://ant.apache.org/manual/Types/multirootfileset.html");
        m.put("PropertySet", "https://ant.apache.org/manual/Types/propertyset.html");
        m.put("Regexp", "https://ant.apache.org/manual/Types/regexp.html");
        m.put("Resource", "https://ant.apache.org/manual/Types/resources.html#basic");
        m.put("FileResource", "https://ant.apache.org/manual/Types/resources.html#file");
        m.put("JavaResource", "https://ant.apache.org/manual/Types/resources.html#javaresource");
        m.put("JavaConstantResource", "https://ant.apache.org/manual/Types/resources.html#javaconstant");
        m.put("PropertyResource", "https://ant.apache.org/manual/Types/resources.html#propertyresource");
        m.put("StringResource", "https://ant.apache.org/manual/Types/resources.html#string");
        m.put("URLResource", "https://ant.apache.org/manual/Types/resources.html#url");
        m.put("ResourceCollection", "https://ant.apache.org/manual/Types/resources.html#collection");
        m.put("Path", "https://ant.apache.org/manual/using.html#path");
        m.put("Commandline.Argument", "https://ant.apache.org/manual/using.html#arg");
        m.put("Argument", "https://ant.apache.org/manual/using.html#arg");
        return m;
    }
}
