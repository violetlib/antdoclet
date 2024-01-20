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
  Identify and categorize the tasks and types that are to be documented.
  <p>
  There are three dimensions of categorization:
  <ul>
  <li>Whether the entity is a task or a type.
  </li>
  <li>Whether or not the entity will appear in a menu. Some types are used only in certain tasks or types and do not
  need to clutter a menu. The entities that have menu items are called <em>primary</em>. The entities that do not have
  menu items are called <em>auxiliary</em>. Only types are candidates to be auxiliary entities.
  </li>
  <li>The assigned category of the entity. Some projects do not use categories. These projects will have a single menu
  containing all primary entities. A project that uses categories will display a category menu and a menu of the primary
  entities that are members of the selected category. A project that uses categories may have some primary entities that
  do not have a category. These entities belong to a implied category that appears as <em>other</em> in the category
  menu.
  </li>
  </ul>

  <p>
  During analysis, AntDoc instances may be created. The existence of an AntDoc does not imply that the corresponding
  task or type is documented.
*/

public class ProjectBuilder
{
    /**
      Build an AntRoot containing the identified and categorized tasks and types.
      @param docCache This object creates and caches AntDoc instances for the tasks and types. An AntDoc provides
      additional information about an entity based on documentation comments. The existence of an AntDoc does not
      imply that the corresponding task or type has an associated documentation page.
      @param elements The elements as defined by the arguments to javadoc.
      @return an AntRoot containing an organized collections of AntDocs.
    */

    public static @NotNull AntRoot build(@NotNull AntDocCache docCache, @NotNull Set<? extends Element> elements)
    {
        if (false) {
            debug("Included elements");
            for (Element e : elements) {
                debug(e.getSimpleName().toString());
            }
        }

        return new ProjectBuilder(docCache, elements).getRoot();
    }

    private final @NotNull AntDocCache docCache;

    private final @NotNull Set<TypeElement> candidates;
    private final @NotNull SortedSet<AntDoc> allPrimary = new TreeSet<>();
    private final @NotNull SortedSet<AntDoc> primaryTasks = new TreeSet<>();
    private final @NotNull SortedSet<AntDoc> primaryTypes = new TreeSet<>();
    private final @NotNull SortedSet<String> categories = new TreeSet<>();
    private final @NotNull SortedSet<AntDoc> uncategorizedTasks = new TreeSet<>();
    private final @NotNull SortedSet<AntDoc> uncategorizedTypes = new TreeSet<>();
    private final @NotNull SortedSet<AntDoc> auxiliaryTypes = new TreeSet<>();

    private ProjectBuilder(@NotNull AntDocCache docCache, @NotNull Set<? extends Element> elements)
    {
        this.docCache = docCache;

        this.candidates = identifyCandidates(elements);
        List<TypeElement> tes = identifyPrimaryEntities(candidates);
        for (TypeElement e : tes) {
            processCandidatePrimaryEntity(e);
        }
        discoverAuxiliaryTypes();
    }

    public @NotNull AntRoot getRoot()
    {
        return AntRoot.create(docCache,
          Collections.unmodifiableSet(categories),
          Collections.unmodifiableSet(primaryTasks),
          Collections.unmodifiableSet(primaryTypes),
          Collections.unmodifiableSet(auxiliaryTypes),
          Collections.unmodifiableSet(uncategorizedTasks),
          Collections.unmodifiableSet(uncategorizedTypes)
        );
    }

    /**
      Identify candidates for inclusion in the documentation. Only tasks and types in the packages provided to
      javadoc are candidates.
    */

    private @NotNull Set<TypeElement> identifyCandidates(@NotNull Set<? extends Element> elements)
    {
        Set<TypeElement> result = new HashSet<>();
        for (Element e : elements) {
            if (e instanceof TypeElement te) {
                result.add(te);
            }
        }
        return result;
    }

    /**
      Identify the elements to be included in the documentation.
    */

    private @NotNull List<TypeElement> identifyPrimaryEntities(@NotNull Set<TypeElement> tes)
    {
        List<TypeElement> result = new ArrayList<>();
        for (TypeElement te : tes) {
            if (isPrimaryEntity(te)) {
                result.add(te);
            }
        }
        return result;
    }

    private void processCandidatePrimaryEntity(@NotNull TypeElement e)
    {
        AntDoc d = docCache.getOrCreate(e);
        if (d != null) {
            allPrimary.add(d);
            if (d.getAntCategory() != null) {
                categories.add(d.getAntCategory());
            } else {
                if (d.isTask()) {
                    debug("Adding uncategorized task: " + d.getAntName());
                    uncategorizedTasks.add(d);
                } else if (d.isType()){
                    debug("Adding uncategorized type: " + d.getAntName());
                    uncategorizedTypes.add(d);
                }
            }
            if (d.isTask()) {
                primaryTasks.add(d);
            } else if (d.isType()){
                primaryTypes.add(d);
            }
        }
    }

    /**
      Determine whether the specified entity should be a primary entity.
    */

    private boolean isPrimaryEntity(@NotNull TypeElement te)
    {
        String qn = te.getQualifiedName().toString();

        AntDoc d = docCache.getOrCreate(te);
        if (d == null) {
            debug("Rejected: no AntDoc created: " + qn);
            return false;
        }

        if (d.isIgnored()) {
            debug("Rejected: isIgnored: " + qn);
            return false;
        }

        boolean isTagged = d.isTagged();
        if (isTagged) {
            return true;
        }

        // TBD: might want options to enable this behavior
        boolean isComponent = d.isSubtypeOf("org.apache.tools.ant.ProjectComponent");
        if (isComponent) {
            return true;
        }

        debug("Rejected: not tagged and not a project component: " + qn);
        return false;
    }

    /**
      Enlarge the set of documented elements to include types that are mentioned in the description of some other
      documented entity.
    */

    private void discoverAuxiliaryTypes()
    {
        // Enlarge the set of documented elements to include types that are mentioned in the description of some other
        // documented entity.

        Set<TypeElement> known = getPrimaryEntities(allPrimary);
        List<AntDoc> docsToProcess = new ArrayList<>(allPrimary);

        while (!docsToProcess.isEmpty()) {
            int lastIndex = docsToProcess.size() - 1;
            AntDoc d = docsToProcess.remove(lastIndex);
            for (TypeElement nte : d.getAllReferencedTypes()) {
                if (!known.contains(nte) && shouldIncludeAuxiliaryElement(nte)) {
                    AntDoc nd = docCache.getOrCreate(nte);
                    known.add(nte);
                    auxiliaryTypes.add(nd);
                }
            }
        }
    }

    private @NotNull Set<TypeElement> getPrimaryEntities(@NotNull Set<AntDoc> ds)
    {
        Set<TypeElement> result = new HashSet<>();
        for (AntDoc d : ds) {
            result.add(d.getTypeElement());
        }
        return result;
    }

    /**
      Determine whether an element should be included in the documentation as an auxiliary element.
    */

    private boolean shouldIncludeAuxiliaryElement(@NotNull TypeElement te)
    {
        String qn = te.getQualifiedName().toString();

        if (!candidates.contains(te)) {
            debug("Rejected as auxiliary type: not a project member");
            return false;
        }

        AntDoc d = docCache.getOrCreate(te);
        if (d == null) {
            debug("Rejected as auxiliary type: no AntDoc created: " + qn);
            return false;
        }

        if (d.isSubtypeOf("org.apache.tools.ant.Task")) {
            debug("Rejected as an auxiliary type: is a task");
        }

        if (d.isTagged()) {
            return true;
        }

        // TBD: might want options to enable this behavior
        if (d.isSubtypeOf("org.apache.tools.ant.ProjectComponent")) {
            return true;
        }

        debug("Rejected as an auxiliary type: not tagged and not a project component: " + qn);
        return false;
    }

    private static void debug(@NotNull String message)
    {
        if (false) {
            System.out.println(message);
        }
    }
}
