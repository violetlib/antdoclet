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

import javax.lang.model.element.TypeElement;
import java.util.*;

/**
  This object provides access to the tasks and types being document.
  The methods of this object are invoked from templates.
  <p>
  @author Based on code by Fernando Dobladez <dobladez@gmail.com>
*/

public class AntRoot
{
    public static @NotNull AntRoot create(@NotNull AntDocCache docCache,
                                          @NotNull Set<String> categories,
                                          @NotNull Set<AntDoc> primaryTasks,
                                          @NotNull Set<AntDoc> primaryTypes,
                                          @NotNull Set<AntDoc> auxiliaryTypes,
                                          @NotNull Set<AntDoc> uncategorizedTasks,
                                          @NotNull Set<AntDoc> uncategorizedTypes
    )
    {
        return new AntRoot(docCache, categories, primaryTasks, primaryTypes, auxiliaryTypes, uncategorizedTasks,
          uncategorizedTypes);
    }

    private final @NotNull AntDocCache docCache;

    private final @NotNull Set<String> categories;
    private final @NotNull Set<String> extendedCategories;
    private final @NotNull Set<AntDoc> allPrimary;
    private final @NotNull Set<AntDoc> primaryTasks;
    private final @NotNull Set<AntDoc> primaryTypes;
    private final @NotNull Set<AntDoc> uncategorizedTasks;
    private final @NotNull Set<AntDoc> uncategorizedTypes;
    private final @NotNull Set<AntDoc> allUncategorized;
    private final @NotNull Set<AntDoc> auxiliaryTypes;
    private final @NotNull Set<AntDoc> allEntities;
    private final @NotNull Set<AntDoc> allTypes;
    private final @NotNull Map<String,List<AntDoc>> primaryByCategory = new HashMap<>();
    private final @NotNull Map<String,List<AntDoc>> primaryTasksByCategory = new HashMap<>();
    private final @NotNull Map<String,List<AntDoc>> primaryTypesByCategory = new HashMap<>();

    private AntRoot(@NotNull AntDocCache docCache,
                    @NotNull Set<String> categories,
                    @NotNull Set<AntDoc> primaryTasks,
                    @NotNull Set<AntDoc> primaryTypes,
                    @NotNull Set<AntDoc> auxiliaryTypes,
                    @NotNull Set<AntDoc> uncategorizedTasks,
                    @NotNull Set<AntDoc> uncategorizedTypes
    )
    {
        this.docCache = docCache;
        this.categories = categories;
        int uncategorizedEntityCount = uncategorizedTasks.size() + uncategorizedTypes.size();
        this.extendedCategories = createExtendedCategories(categories, uncategorizedEntityCount > 0);
        this.allPrimary = createAllPrimary(primaryTasks, primaryTypes);
        this.primaryTasks = primaryTasks;
        this.primaryTypes = primaryTypes;
        this.auxiliaryTypes = auxiliaryTypes;
        this.uncategorizedTasks = uncategorizedTasks;
        this.uncategorizedTypes = uncategorizedTypes;
        this.allUncategorized = createAllUncategorized(uncategorizedTasks, uncategorizedTypes);
        this.allEntities = combine(allPrimary, auxiliaryTypes);
        this.allTypes = combine(primaryTypes, auxiliaryTypes);
    }

    private @NotNull Set<AntDoc> createAllPrimary(@NotNull Set<AntDoc> primaryTasks,
                                                  @NotNull Set<AntDoc> primaryTypes)
    {
        Set<AntDoc> result = new HashSet<>();
        result.addAll(primaryTasks);
        result.addAll(primaryTypes);
        return Collections.unmodifiableSet(result);
    }

    private @NotNull Set<String> createExtendedCategories(@NotNull Set<String> categories, boolean includeOther)
    {
        Set<String> result = new HashSet<>(categories);
        if (includeOther) {
            result.add("none");
        }
        return Collections.unmodifiableSet(result);
    }

    private @NotNull Set<AntDoc> combine(@NotNull Set<AntDoc> primary, @NotNull Set<AntDoc> auxiliary)
    {
        Set<AntDoc> result = new TreeSet<>();
        result.addAll(primary);
        result.addAll(auxiliary);
        return Collections.unmodifiableSet(result);
    }

    private @NotNull Set<AntDoc> createAllUncategorized(@NotNull Set<AntDoc> tasks, @NotNull Set<AntDoc> types)
    {
        Set<AntDoc> result = new TreeSet<>();
        result.addAll(tasks);
        result.addAll(types);
        return Collections.unmodifiableSet(result);
    }

    /**
      Indicate whether the specified element has a documentation page
    */

    public boolean isIncluded(@NotNull TypeElement te)
    {
        AntDoc d = docCache.get(te);
        if (d != null) {
            return primaryTasks.contains(d) || primaryTypes.contains(d) || auxiliaryTypes.contains(d);
        }
        return false;
    }

    /**
      Return the names of all explicitly defined categories.
    */

    public @NotNull Set<String> getCategories()
    {
        return categories;
    }

    /**
      Return the names of all explicitly defined categories and the implicitly defined category containing
      entities with no defined category, if any.
    */

    public @NotNull Set<String> getCategoriesExtended()
    {
        return extendedCategories;
    }

    /**
      Return the prefix to apply when creating a title for a member of a category.
      @param category The category.
      @return the prefix.
    */

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

    public @NotNull Set<AntDoc> getAllDocumentedEntities()
    {
        return allEntities;
    }

    public @NotNull Set<AntDoc> getAllDocumentedTypes()
    {
        return allTypes;
    }

    public @NotNull Set<AntDoc> getAllPrimary()
    {
        return allPrimary;
    }

    public @NotNull Set<AntDoc> getPrimaryTypes()
    {
        return primaryTypes;
    }

    public @NotNull Set<AntDoc> getPrimaryTasks()
    {
        return primaryTasks;
    }

    public @NotNull Set<AntDoc> getAllUncategorized()
    {
        return allUncategorized;
    }

    public @NotNull Set<AntDoc> getUncategorizedTypes()
    {
        return uncategorizedTypes;
    }

    public @NotNull Set<AntDoc> getUncategorizedTasks()
    {
        return uncategorizedTasks;
    }

    public @NotNull Set<AntDoc> getAuxiliaryTypes()
    {
        return auxiliaryTypes;
    }

    public int getUncategorizedElementCount()
    {
        return allUncategorized.size();
    }

    public int getPrimaryElementCount()
    {
        return allPrimary.size();
    }

    public @NotNull Collection<AntDoc> getAllByCategory(@NotNull String category)
    {
        if ("all".equals(category)) {
            return getAllPrimary();
        }

        if ("none".equals(category)) {
            return getAllUncategorized();
        }

        List<AntDoc> ds = primaryByCategory.get(category);
        if (ds == null) {
            ds = getByCategory(category, allPrimary);
            primaryByCategory.put(category, ds);
        }
        return ds;
    }

    public @NotNull Collection<AntDoc> getTasksByCategory(@NotNull String category)
    {
        if ("all".equals(category)) {
            return getPrimaryTasks();
        }

        if ("none".equals(category)) {
            return getUncategorizedTasks();
        }

        List<AntDoc> ds = primaryTasksByCategory.get(category);
        if (ds == null) {
            ds = getByCategory(category, primaryTasks);
            primaryTasksByCategory.put(category, ds);
        }
        return ds;
    }

    public @NotNull Collection<AntDoc> getTypesByCategory(@NotNull String category)
    {
        // Special case for "all" when using categories. The assumption when using categories is that the "all"
        // menu is used as a substitute for a search box, so it should include auxiliary types. When not
        // using categories, the "all" list of entities is the only list that is displayed, so it should not
        // display types that have not been marked for display.

        if ("all".equals(category)) {
            if (categories.isEmpty()) {
                return getPrimaryTypes();
            } else {
                return getAllDocumentedTypes();
            }
        }

        if ("none".equals(category)) {
            return getUncategorizedTypes();
        }

        List<AntDoc> ds = primaryTypesByCategory.get(category);
        if (ds == null) {
            ds = getByCategory(category, primaryTypes);
            primaryTypesByCategory.put(category, ds);
        }
        return ds;
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
}
