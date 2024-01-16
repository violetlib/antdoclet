/*
 * Copyright (c) 2024 Alan Snyder.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the license agreement. For details see
 * accompanying license terms.
 */

/*
  Copyright (c) 2003-2005 Fernando Dobladez

  This file is part of AntDoclet.

  AntDoclet is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  AntDoclet is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with AntDoclet; if not, write to the Free Software
  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package org.violetlib.antdoclet;

import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.*;

/**
  This object determines the tasks and types that are to be documented. It also organizes them by category.
  <p>
  During the analysis, AntDoc instances may be created. The existence of an AntDoc does not imply that the corresponding
  task or type is documented.

  @author Fernando Dobladez <dobladez@gmail.com>
*/

public class AntRoot
{
    public static @NotNull AntRoot create(@NotNull AntDocCache docCache, @NotNull Set<? extends Element> elements)
    {
        if (false) {
            debug("Included elements");
            for (Element e : elements) {
                debug(e.getSimpleName().toString());
            }
        }

        return new AntRoot(docCache, elements);
    }

    private final @NotNull AntDocCache docCache;

    private final @NotNull SortedSet<AntDoc> all = new TreeSet<>();
    private final @NotNull SortedSet<AntDoc> allTypes = new TreeSet<>();
    private final @NotNull SortedSet<AntDoc> allTasks = new TreeSet<>();
    private final @NotNull SortedSet<String> categories = new TreeSet<>();
    private final @NotNull SortedSet<AntDoc> uncategorizedTasks = new TreeSet<>();
    private final @NotNull SortedSet<AntDoc> uncategorizedTypes = new TreeSet<>();
    private final @NotNull SortedSet<AntDoc> auxiliaryTypes = new TreeSet<>();

    private AntRoot(@NotNull AntDocCache docCache, @NotNull Set<? extends Element> elements)
    {
        this.docCache = docCache;

        List<TypeElement> types = getIncludedElements(elements);
        for (TypeElement e : types) {
            process(e);
        }
        extendTypes(types);
    }

    private void extendTypes(@NotNull List<TypeElement> types)
    {
        // Enlarge the set of documented elements to include types that are mentioned as unnamed nested elements of
        // documented types.

        for (TypeElement te : new ArrayList<>(types)) {
            AntDoc d = docCache.get(te);
            if (d != null) {
                for (TypeElement nte : d.getAllUnnamedNestedElementTypes()) {
                    if (!types.contains(nte)) {
                        AntDoc nd = docCache.getOrCreate(nte);
                        auxiliaryTypes.add(nd);
                    }
                }
            }
        }
    }

    private void process(@NotNull TypeElement e)
    {
        AntDoc d = docCache.getOrCreate(e);
        if (d != null) {
            all.add(d);
            if (d.getAntCategory() != null) {
                categories.add(d.getAntCategory());
            } else {
                if (d.isTask()) {
                    uncategorizedTasks.add(d);
                } else if (d.isType()){
                    debug("Adding uncategorized type: " + d.getAntName());
                    uncategorizedTypes.add(d);
                }
            }
            if (d.isTask()) {
                allTasks.add(d);
            } else if (d.isType()){
                allTypes.add(d);
            }
        }
    }

    /**
      Indicate whether the specified element has a documentation page
    */

    public boolean isIncluded(@NotNull TypeElement te)
    {
        AntDoc d = docCache.get(te);
        if (d != null) {
            return allTypes.contains(d) || auxiliaryTypes.contains(d);
        }
        return false;
    }

    /**
      Identify the elements to be included in the documentation.
    */

    private @NotNull List<TypeElement> getIncludedElements(@NotNull Set<? extends Element> elements)
    {
        List<TypeElement> result = new ArrayList<>();
        for (Element e : elements) {
            if (e instanceof TypeElement type) {
                if (shouldIncludeElement(type)) {
                    result.add(type);
                }
            }
        }
        return result;
    }

    /**
      Make an initial determination whether an element should be included in the documentation.
    */

    private boolean shouldIncludeElement(@NotNull TypeElement te)
    {
        AntDoc d = docCache.getOrCreate(te);
        if (d == null) {
            debug("Rejected: no AntDoc created: " + te.getQualifiedName());
            return false;
        }
        if (d.isIgnored()) {
            debug("Rejected: isIgnored: " + te.getQualifiedName());
            return false;
        }
        if (d.isTagged()) {
            return true;
        }
        // TBD: might want options to enable this behavior
        if (d.isSubtypeOf("org.apache.tools.ant.ProjectComponent")) {
            return true;
        }
        debug("Rejected: !isTagged or !isSubtype(ProjectComponent): " + te.getQualifiedName());
        return false;
    }

    public @NotNull List<String> getCategories()
    {
        return new ArrayList<>(categories);
    }

    public @NotNull List<String> getCategoriesExtended()
    {
        List<String> result = new ArrayList<>(categories);
        if (getUncategorizedElementCount() > 0) {
            result.add("none");
        }
        return result;
    }

    public @NotNull String getAntCategoryPrefix(@NotNull String category)
    {
        if (category.equals("none")) {
            return "Other ";
        }
        if (!category.equals("all")) {
            return capitalize(category) + " ";
        }
        return "";
    }

    private @NotNull String capitalize(@NotNull String s)
    {
        if (s.isBlank()) {
            return "";
        }
        String prefix = s.substring(0, 1);
        String suffix = s.substring(1);
        return prefix.toUpperCase() + suffix;
    }

    public @NotNull List<AntDoc> getAll()
    {
        return new ArrayList<>(all);
    }

    public @NotNull List<AntDoc> getTypes()
    {
        return new ArrayList<>(allTypes);
    }

    public @NotNull List<AntDoc> getTasks()
    {
        return new ArrayList<>(allTasks);
    }

    public @NotNull List<AntDoc> getAllUncategorized()
    {
        List<AntDoc> result = new ArrayList<>();
        result.addAll(uncategorizedTasks);
        result.addAll(uncategorizedTypes);
        return result;
    }

    public @NotNull List<AntDoc> getUncategorizedTypes()
    {
        return new ArrayList<>(uncategorizedTypes);
    }

    public @NotNull List<AntDoc> getUncategorizedTasks()
    {
        return new ArrayList<>(uncategorizedTasks);
    }

    public @NotNull List<AntDoc> getAuxiliaryTypes()
    {
        return new ArrayList<>(auxiliaryTypes);
    }

    public int getUncategorizedElementCount()
    {
        return uncategorizedTasks.size() + uncategorizedTypes.size();
    }

    public int getElementCount()
    {
        return allTasks.size() + allTypes.size();
    }

    public @NotNull List<AntDoc> getAllByCategory(@NotNull String category)
    {
        // give category "all" a special meaning:
        if ("all".equals(category)) {
            return getAll();
        }
        if ("none".equals(category)) {
            return getAllUncategorized();
        }
        return getByCategory(category, all);
    }

    public @NotNull List<AntDoc> getTypesByCategory(@NotNull String category)
    {
        // give category "all" a special meaning:
        if ("all".equals(category)) {
            return getTypes();
        }
        if ("none".equals(category)) {
            return getUncategorizedTypes();
        }
        return getByCategory(category, allTypes);
    }

    public @NotNull List<AntDoc> getTasksByCategory(@NotNull String category)
    {
        // give category "all" a special meaning:
        if ("all".equals(category)) {
            return getTasks();
        }
        if ("none".equals(category)) {
            return getUncategorizedTasks();
        }
        return getByCategory(category, allTasks);
    }

    private @NotNull List<AntDoc> getByCategory(@NotNull String category, @NotNull Set<AntDoc> antdocs)
    {
        List<AntDoc> filtered = new ArrayList<>();
        for (AntDoc d : antdocs) {
            if (category.equals(d.getAntCategory())) {
                filtered.add(d);
            }
        }
        return filtered;
    }

    private static void debug(@NotNull String message)
    {
        if (false) {
            System.err.println(message);
        }
    }
}
