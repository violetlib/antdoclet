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

package com.neuroning.antdoclet;

import java.util.*;
import jdk.javadoc.doclet.*;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
  An object of this class represents a set of Java classes that are an Ant Task and an Ant Types.

  It's mainly a wrapper around a RootDoc instance, adding methods for traversing the RootDoc objects sorted by
  Ant-specific features (task name, category, etc)

  @author Fernando Dobladez  <dobladez@gmail.com>
*/
public class AntRoot
{
    private final @NotNull DocletEnvironment env;
    private final @NotNull SortedSet<AntDoc> all;
    private final @NotNull SortedSet<AntDoc> allTypes;
    private final @NotNull SortedSet<AntDoc> allTasks;
    private final @NotNull SortedSet<String> categories;

    public AntRoot(@NotNull DocletEnvironment env, @NotNull Reporter reporter)
    {
        this.env = env;
        all = new TreeSet<>();
        allTypes = new TreeSet<>();
        allTasks = new TreeSet<>();
        categories = new TreeSet<>();

        List<TypeElement> types = getIncludedTypes();
        for (TypeElement c : types) {
            String name = c.getQualifiedName().toString();
            AntDoc d = AntDoc.getInstance(name, this.env, reporter);
            if (d != null) {
                all.add(d);
                if (d.getAntCategory() != null) {
                    categories.add(d.getAntCategory());
                }
                if (d.isTask()) {
                    allTasks.add(d);
                } else if (d.isType()){
                    allTypes.add(d);
                }
            }
        }
    }

    private @NotNull List<TypeElement> getIncludedTypes()
    {
        List<TypeElement> result = new ArrayList<>();
        Set<? extends Element> elements = env.getIncludedElements();
        for (Element e : elements) {
            if (e instanceof TypeElement) {
                TypeElement type = (TypeElement) e;
                result.add(type);
            }
        }
        return result;
    }

    public @NotNull Iterator<String> getCategories()
    {
        return categories.iterator();
    }

    public @NotNull Iterator<AntDoc> getAll()
    {
        return all.iterator();
    }

    public @NotNull Iterator<AntDoc> getTypes()
    {
        return allTypes.iterator();
    }

    public @NotNull Iterator<AntDoc> getTasks()
    {
        return allTasks.iterator();
    }

    public @NotNull Iterator<AntDoc> getAllByCategory(@NotNull String category)
    {
        // give category "all" a special meaning:
        if ("all".equals(category)) {
            return getAll();
        }
        return getByCategory(category, all);
    }

    public @NotNull Iterator<AntDoc> getTypesByCategory(@NotNull String category)
    {
        // give category "all" a special meaning:
        if ("all".equals(category)) {
            return getTypes();
        }
        return getByCategory(category, allTypes);
    }

    public @NotNull Iterator<AntDoc> getTasksByCategory(@NotNull String category)
    {
        // give category "all" a special meaning:
        if ("all".equals(category)) {
            return getTasks();
        }
        return getByCategory(category, allTasks);
    }

    private @NotNull Iterator<AntDoc> getByCategory(@NotNull String category, @NotNull Set<AntDoc> antdocs)
    {
        List<AntDoc> filtered = new ArrayList<>();
        for (AntDoc d : antdocs) {
            if (category.equals(d.getAntCategory())) {
                filtered.add(d);
            }
        }

        return filtered.iterator();
    }
}
