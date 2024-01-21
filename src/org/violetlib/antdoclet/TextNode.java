/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

package org.violetlib.antdoclet;

import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.TextTree;
import org.jetbrains.annotations.NotNull;

/**

*/

public class TextNode
  implements TextTree
{
    public static @NotNull TextTree create(@NotNull String text)
    {
        return new TextNode(text);
    }

    private final @NotNull String text;

    private TextNode(@NotNull String text)
    {
        this.text = text;
    }

    @Override
    public Kind getKind()
    {
        return Kind.TEXT;
    }

    @Override
    public <R,D> R accept(DocTreeVisitor<R,D> visitor, D data)
    {
        return visitor.visitText(this, data);
    }

    @Override
    public @NotNull String getBody()
    {
        return text;
    }
}
