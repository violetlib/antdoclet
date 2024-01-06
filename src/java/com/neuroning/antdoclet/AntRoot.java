/**
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

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

import java.util.*;

/**
  An object of this class represents a set of Java classes that are an Ant Task and an Ant Types.

  It's mainly a wrapper around a RootDoc instance, adding methods for traversing the RootDoc objects sorted by
  Ant-specific features (task name, category, etc)

  @author Fernando Dobladez  <dobladez@gmail.com>
*/
@SuppressWarnings("removal, deprecation")
public class AntRoot
{
    private RootDoc rootDoc;
    private SortedSet<AntDoc> all;
    private SortedSet<AntDoc> allTypes;
    private SortedSet<AntDoc> allTasks;
    private SortedSet<String> categories;

    public AntRoot(RootDoc rootDoc)
    {
        this.rootDoc = rootDoc;
        all = new TreeSet<>();
        allTypes = new TreeSet<>();
        allTasks = new TreeSet<>();
        categories = new TreeSet<>();

        ClassDoc[] classes = rootDoc.classes();
        for (ClassDoc c : classes) {
            AntDoc d = AntDoc.getInstance(c.qualifiedName(), this.rootDoc);
            if (d != null) {
                all.add(d);
                if (d.getAntCategory() != null) {
                    categories.add(d.getAntCategory());
                }
                if (d.isTask()) {
                    allTasks.add(d);
                } else {
                    allTypes.add(d);
                }
            }
        }
    }

    public Iterator<String> getCategories()
    {
        return categories.iterator();
    }

    public Iterator<AntDoc> getAll()
    {
        return all.iterator();
    }

    public Iterator<AntDoc> getTypes()
    {
        return allTypes.iterator();
    }

    public Iterator<AntDoc> getTasks()
    {
        return allTasks.iterator();
    }

    public Iterator<AntDoc> getAllByCategory(String category)
    {
        // give category "all" a special meaning:
        if ("all".equals(category)) {
            return getAll();
        }
        return getByCategory(category, all);
    }

    public Iterator<AntDoc> getTypesByCategory(String category)
    {
        // give category "all" a special meaning:
        if ("all".equals(category)) {
            return getTypes();
        }
        return getByCategory(category, allTypes);
    }

    public Iterator<AntDoc> getTasksByCategory(String category)
    {
        // give category "all" a special meaning:
        if ("all".equals(category)) {
            return getTasks();
        }
        return getByCategory(category, allTasks);
    }

    private Iterator<AntDoc> getByCategory(String category, Set<AntDoc> antdocs)
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
