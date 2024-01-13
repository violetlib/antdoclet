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

import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.Map;

/**
  Cache the results of analyzing classes. Probably good for performance, but essential to avoid recursion.
*/

public class AntDocCache
{
    public static @NotNull AntDocCache create(@NotNull Environment env)
    {
        return new AntDocCache(env);
    }

    private final @NotNull Environment env;
    private final @NotNull Map<TypeElement,AntDoc> cache = new HashMap<>();

    private AntDocCache(@NotNull Environment env)
    {
        this.env = env;
    }

    public boolean isDefined(@NotNull TypeElement te)
    {
        return cache.containsKey(te);
    }

    public @Nullable AntDoc get(@NotNull TypeElement te)
    {
        return cache.get(te);
    }

    public @Nullable AntDoc getOrCreate(@NotNull TypeElement te)
    {
        AntDoc d = cache.get(te);
        if (d != null) {
            return d;
        }
        d = AntDoc.create(env, te);
        cache.put(te, d);
        return d;
    }
}
