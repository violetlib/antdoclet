/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

package org.violetlib.antdoclet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**

*/

public class AugmentedTagAttributeInfo
  extends TagAttributeInfo
{
    public static @NotNull AugmentedTagAttributeInfo create(@NotNull TagAttributeInfo base,
                                                            @NotNull ElementContentProcessing cp)
    {
        return new AugmentedTagAttributeInfo(base, cp);
    }

    private final @NotNull ElementContentProcessing cp;

    private @Nullable String _htmlValue;

    private AugmentedTagAttributeInfo(@NotNull TagAttributeInfo base, @NotNull ElementContentProcessing cp)
    {
        super(base);

        this.cp = cp;
    }

    public @NotNull String getHtmlValue()
    {
        if (_htmlValue == null) {
            _htmlValue = cp.toHTML(getValue());
        }
        return _htmlValue;
    }
}
