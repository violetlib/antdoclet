/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

package org.violetlib.antdoclet;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;

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

    public long getLineNumber(@NotNull Element e)
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
}
