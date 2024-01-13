package com.neuroning.antdoclet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.Map;

/**
  Cache the results of analyzing classes.
*/

public class AnalysisCache
{
    public static @NotNull AnalysisCache create(@NotNull DocUtils docUtils)
    {
        return new AnalysisCache(docUtils);
    }

    private final @NotNull DocUtils docUtils;
    private final @NotNull Map<TypeElement,TypeInfo> cache = new HashMap<>();

    private AnalysisCache(@NotNull DocUtils docUtils)
    {
        this.docUtils = docUtils;
    }

    public @Nullable TypeInfo getInfo(@NotNull TypeElement te)
    {
        TypeInfo info = cache.get(te);
        if (info != null) {
            return info;
        }

        info = Analysis.getInfo(te, docUtils);
        cache.put(te, info);
        return info;
    }
}
