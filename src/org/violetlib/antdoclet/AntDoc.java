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
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.*;

/**
  An object that represents a documented Java source file in templates.
  Source files of interest are classes that implement Ant tasks or types.
*/
@SuppressWarnings("removal, deprecation")
public class AntDoc
  implements Comparable
{
    // for use only by AntDocCache
    public static @NotNull AntDoc create(@NotNull Environment env, @NotNull TypeElement te)
    {
        return new AntDoc(env, te);
    }

    private final @NotNull Environment env;
    private final @NotNull TypeElement thisType;
    private final @NotNull TypeInfo typeInfo;
    private @Nullable List<AntDoc> nestedClasses;
    private @NotNull List<Property> properties;
    private @NotNull List<Reference> references;

    private static final List<String> antEntities = List.of("ant.task", "ant.type", "ant.prop", "ant.ref");

    private AntDoc(@NotNull Environment env, @NotNull TypeElement thisType)
    {
        this.env = env;
        this.thisType = thisType;
        TypeInfo ti = env.getTypeInfo(thisType);
        if (ti == null) {
            String message = String.format("Should not create AntDoc for %s until type information is available",
              thisType.getQualifiedName());
            throw new IllegalStateException(message);
        }
        this.typeInfo = ti;
        properties = discoverProperties();
        references = discoverReferences();
    }

    public @NotNull List<AntDoc> getNestedClasses()
    {
        if (nestedClasses == null) {
            nestedClasses = createNestedClassDocs();
        }
        return nestedClasses;
    }

    private boolean shouldIgnore(@NotNull TypeElement doc)
    {
        for (String en : antEntities) {
            String s = env.tagAttributeValue(doc, en, "ignore");
            if ("true".equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldIgnore(@NotNull Environment env, @NotNull TypeElement type)
    {
        for (String en : antEntities) {
            String s = env.tagAttributeValue(type, en, "ignore");
            if ("true".equalsIgnoreCase(s)) {
                // debug
                System.out.println("Ignoring: " + env.getTypeName(type.asType()));
                return true;
            }
        }
        return false;
    }

    /**
      @return w this type represents an Ant Task
    */

    public boolean isTask()
    {
        return typeInfo.isTask();
    }

    /**
      @return whether this type represents an Ant DataType
    */

    public boolean isType()
    {
        return !isTask();
    }

    /**
      @return whether this type is an Ant Task Container?
    */

    // For template use
    public boolean isTaskContainer()
    {
        return env.isSubtypeOf(thisType.asType(), "org.apache.tools.ant.TaskContainer");
    }

    /**
      @return Should this entity be excluded?
    */

    public boolean isIgnored()
    {
        return shouldIgnore(thisType);
    }

    /**
      @return Is this element included in documentation?
    */

    public boolean isIncluded()
    {
        return env.isIncluded(thisType);
    }

    /**
      @return the description for this ant entity.
    */

    // For template use
    public @NotNull String getDescription()
    {
        String description = env.getDescription(thisType);
        return description != null ? description : "";
    }

    /**
      @return a short description for this ant entity (basically, the first sentence).
    */

    // For template use
    public @Nullable String getShortDescription()
    {
        return env.getShortDescription(thisType);
    }

    /**
      Return HTML text that documents the behavior of nested tasks.
      @return text, or null to allow the template to supply default text.
    */

    // For template use
    public @Nullable String getNestedTaskDescription()
    {
        ExecutableElement m = typeInfo.getAddTaskMethod();
        if (m != null) {
            return env.getDescription(m);
        }
        return null;
    }

    /**
      Create AntDocs for nested (inner) classes.
    */

    private @NotNull List<AntDoc> createNestedClassDocs()
    {
        List<AntDoc> result = new ArrayList<>();
        for (TypeElement te : typeInfo.getNestedClasses()) {
            if (!shouldIgnore(te)) {
                TypeInfo ti = env.getTypeInfo(te);
                if (ti == null) {
                    String message = String.format("Info for nested class %s of %s is unavailable",
                      te.getQualifiedName(), thisType.getQualifiedName());
                    env.getReporter().print(Diagnostic.Kind.ERROR, message);
                    continue;
                }
                AntDoc doc = env.getAntDoc(te);
                result.add(doc);
            }
        }
        return result;
    }

    /**
      An opaque type used by templates to refer to an attribute supported by this entity.
    */

    private static class Attribute
    {
        private final @NotNull AttributeInfo info;

        public Attribute(@NotNull AttributeInfo info)
        {
            this.info = info;
        }

        @Override
        public @NotNull String toString()
        {
            return String.format("<Attribute %s>", info.name);
        }
    }

    /**
      Identify the attributes supported by this entity.

      @return a list of attribute names.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull List<Attribute> getAttributes()
    {
        Map<String,AttributeInfo> am = typeInfo.getAttributes();
        List<AttributeInfo> as = sortAttributes(am.values());
        return toAttributes(as);
    }

    private @NotNull List<Attribute> toAttributes(@NotNull List<AttributeInfo> as)
    {
        List<Attribute> result = new ArrayList<>();
        for (AttributeInfo a : as) {
            result.add(new Attribute(a));
        }
        return result;
    }

    private @NotNull List<AttributeInfo> sortAttributes(@NotNull Collection<AttributeInfo> as)
    {
        List<AttributeInfo> result = new ArrayList<>(as);
        result.sort(new AttributeComparator());
        return result;
    }

    private class AttributeComparator
      implements Comparator<AttributeInfo>
    {
        @Override
        public int compare(AttributeInfo o1, AttributeInfo o2)
        {
            long n1 = getSourceLine(o1.definingMethod);
            long n2 = getSourceLine(o2.definingMethod);
            return (int) (n1 - n2);
        }
    }

    /**
      Return the display name for the specified attribute.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull String getAttributeName(@NotNull Attribute a)
    {
        return a.info.name;
    }

    /**
      Return a description of the type of the specified name attribute.
      @return the type name, or null if there is no supported named attribute with the specified name.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @Nullable String getAttributeType(@NotNull Attribute a)
    {
        return typeToString(a.info.type);
    }

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @Nullable String getAttributeTypeLinked(@NotNull Attribute a)
    {
        return typeToLink(a.info.type);
    }

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @Nullable String getAttributeTypes(@NotNull Attribute a)
    {
        return getTypeNames(a.info.allTypes);
    }

    private @NotNull String getTypeNames(@NotNull List<TypeMirror> types)
    {
        StringBuilder sb = new StringBuilder();
        for (TypeMirror type : types) {
            sb.append(", ");
            sb.append(typeToString(type));
        }
        return sb.substring(2);
    }

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @Nullable String getAttributeTypesLinked(@NotNull Attribute a)
    {
        List<TypeMirror> types = a.info.allTypes;
        StringBuilder sb = new StringBuilder();
        for (TypeMirror type : types) {
            sb.append(", ");
            sb.append(typeToLink(type));
        }
        return sb.substring(2);
    }

    private @NotNull String typeToLink(@NotNull TypeMirror t)
    {
        return env.getTypeNameLinked(t);
    }

    /**
      Return HTML text that describes the specified attribute.
      @return The description, or an empty string if none.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull String getAttributeDescription(@NotNull Attribute a)
    {
        String d = env.getDescription(a.info.definingMethod);
        return d != null ? d : "";
    }

    /**
      Return HTML text that describes the required nature of an attribute.
      This text is extracted from the {@code ant.required} tag.
      @return The descriptive text. Returns null if this attribute is not declared as required.
      Returns an empty string if no documentation is available.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @Nullable String getAttributeRequired(@NotNull Attribute a)
    {
        return env.tagValue(a.info.definingMethod, "ant.required");
    }

    /**
      Get HTML text that describes the optional nature of an attribute.
      The text is extracted from the {@code ant.optional} tag.
      @return The descriptive text. Returns null if this attribute is not declared as optional.
      Returns an empty string if no documentation is available.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @Nullable String getAttributeNotRequired(@NotNull Attribute a)
    {
        String s = env.tagValue(a.info.definingMethod, "ant.optional");
        if (s == null) {
            // backward compatibility
            s = env.tagValue(a.info.definingMethod, "ant.not-required");
        }
        return s;
    }

    /**
      An opaque type used by templates to refer to a global property.
    */

    private static class Property
      implements Comparable<Property>
    {
        private final @NotNull VariableElement field;
        private final @NotNull String propertyName;
        private final @NotNull String propertyType;
        private final long sortKey;

        public Property(@NotNull VariableElement field,
                        @NotNull String propertyName,
                        @NotNull String propertyType,
                        long sortKey)
        {
            this.field = field;
            this.propertyName = propertyName;
            this.propertyType = propertyType;
            this.sortKey = sortKey;
        }

        @Override
        public int compareTo(@NotNull AntDoc.Property o)
        {
            return (int) (sortKey - o.sortKey);
        }
    }

    /**
      Identify the global properties that this entity depends upon.
      @return objects that identify properties.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull List<Property> getProperties()
    {
        return new ArrayList<>(properties);
    }

    /**
      Return the property name of a property used by this entity.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull String getPropertyName(@NotNull Property p)
    {
        return p.propertyName;
    }

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull String getPropertyDescription(@NotNull Property p)
    {
        String d = env.getDescription(p.field);
        if (d != null) {
            return d;
        }
        return "";
    }

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull String getPropertyType(@NotNull Property p)
    {
        return p.propertyType;
    }

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull String getPropertyTypeLinked(@NotNull Property p)
    {
        String typeName = getPropertyType(p);
        return getTypeNameLinked(p.field, typeName);
    }

    private @Nullable TypeElement getPropertyTypeElement(@NotNull Property p)
    {
        String typeName = getPropertyType(p);
        return env.getTypeElement(typeName);
    }

    private @NotNull List<Property> discoverProperties()
    {
        List<Property> l = new ArrayList<>();
        List<VariableElement> fields = getFields(thisType);
        for (VariableElement field : fields) {
            Set<Modifier> mods = field.getModifiers();
            if (mods.contains(Modifier.PUBLIC)
              && mods.contains(Modifier.STATIC)
              && mods.contains(Modifier.FINAL) && isProperty(field)) {
                Object value = field.getConstantValue();
                if (value instanceof String name) {
                    long line = getSourceLine(field);
                    String type = env.tagAttributeValue(field, "ant.prop", "type");
                    if (type == null) {
                        type = "String";
                    }
                    Property p = new Property(field, name, type, line);
                    l.add(p);
                }
            }
        }
        l.sort(null);
        return l;
    }

    /**
      An opaque type used by templates to refer to a references.
    */

    private static class Reference
      implements Comparable<Reference>
    {
        private final @NotNull VariableElement field;
        private final @NotNull String id;
        private final @NotNull String referenceType;
        private final long sortKey;

        public Reference(@NotNull VariableElement field,
                         @NotNull String id,
                         @NotNull String referenceType,
                         long sortKey)
        {
            this.field = field;
            this.id = id;
            this.referenceType = referenceType;
            this.sortKey = sortKey;
        }

        @Override
        public int compareTo(@NotNull AntDoc.Reference o)
        {
            return (int) (sortKey - o.sortKey);
        }
    }

    /**
      Identify the referenced entities that this entity depends upon.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull List<Reference> getReferences()
    {
        return new ArrayList<>(references);
    }

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull String getReferenceName(@NotNull Reference r)
    {
        return r.id;
    }

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull String getReferenceDescription(@NotNull Reference r)
    {
        String d = env.getDescription(r.field);
        if (d != null) {
            return d;
        }
        return "";
    }

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull String getReferenceType(@NotNull Reference r)
    {
        return r.referenceType;
    }

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull String getReferenceTypeLinked(@NotNull Reference r)
    {
        String typeName = getReferenceType(r);
        return getTypeNameLinked(r.field, typeName);
    }

    /**
      Attempt to create a link to documentation for a type known only by a name (not an element or type mirror).
    */

    private @NotNull String getTypeNameLinked(@Nullable Element context, @NotNull String typeName)
    {
        String link = env.getTypeNameLinked(context, typeName);

        if (link == null) {
            String message = String.format("%s: Type %s cannot be linked [type not found in sources]",
              getClassName(), typeName);
            env.getReporter().print(Diagnostic.Kind.WARNING, message);
            return typeName;
        }

        return link;
    }

    private @Nullable TypeElement getReferenceTypeElement(@NotNull Reference r)
    {
        String typeName = getReferenceType(r);
        return env.getTypeElement(typeName);
    }

    private @NotNull List<Reference> discoverReferences()
    {
        List<Reference> l = new ArrayList<>();
        List<VariableElement> fields = getFields(thisType);
        for (VariableElement field : fields) {
            Set<Modifier> mods = field.getModifiers();
            if (mods.contains(Modifier.PUBLIC)
              && mods.contains(Modifier.STATIC)
              && mods.contains(Modifier.FINAL) && isReference(field)) {
                Object value = field.getConstantValue();
                if (value instanceof String id) {
                    long line = getSourceLine(field);
                    String type = env.tagAttributeValue(field, "ant.ref", "type");
                    if (type == null) {
                        env.getReporter().print(Diagnostic.Kind.ERROR, "Reference lacks type: " + id);
                        type = "Object";
                    }
                    Reference r = new Reference(field, id, type, line);
                    l.add(r);
                }
            }
        }
        l.sort(null);
        return l;
    }

    private static @NotNull List<VariableElement> getFields(TypeElement t)
    {
        List<VariableElement> result = new ArrayList<>();
        for (Element e : t.getEnclosedElements()) {
            if (e instanceof VariableElement) {
                result.add((VariableElement) e);
            }
        }
        return result;
    }

    /**
      An opaque type used by templates to refer to nested elements supported by this entity.
    */

    private static class NestedElement
    {
        private final @NotNull NestedElementInfo info;
        private final @Nullable String typeNames;

        public NestedElement(@NotNull NestedElementInfo info, @Nullable String typeNames)
        {
            this.info = info;
            this.typeNames = typeNames;
        }

        @Override
        public @NotNull String toString()
        {
            if (info.name != null) {
                return String.format("<NestedElement %s>", info.name);
            } else {
                return String.format("<NestedElement %s>", typeNames);
            }
        }
    }

    /**
      Identify the named nested elements supported by this entity.
      @return a sorted list of nested elements.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull List<NestedElement> getNamedElements()
    {
        List<NestedElementInfo> es = new ArrayList<>(typeInfo.getNamedNestedElements().values());
        es.sort(new NestedElementComparator());
        List<NestedElement> result = new ArrayList<>();
        for (NestedElementInfo e : es) {
            result.add(toNestedElement(e));
        }
        return result;
    }

    /**
      Identify the documented unnamed nested element types supported by this entity.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull List<NestedElement> getNestedElementTypes()
    {
        List<NestedElementInfo> es = new ArrayList<>(typeInfo.getUnnamedNestedElements());
        es.sort(new NestedElementComparator());
        List<NestedElement> result = new ArrayList<>();
        for (NestedElementInfo e : es) {
            if (env.isIncluded(e.types.getFirst())) {
                result.add(toNestedElement(e));
            }
        }
        return result;
    }

    /**
      Return all referenced types, without checking for their inclusion status.
    */

    public @NotNull Set<TypeElement> getAllReferencedTypes()
    {
        Set<TypeElement> result = new HashSet<>();
        result.addAll(getAllNamedNestedElementTypes());
        result.addAll(getAllUnnamedNestedElementTypes());
        result.addAll(getAllAttributeTypes());
        result.addAll(getAllPropertyTypes());
        result.addAll(getAllReferenceTypes());
        return result;
    }

    /**
      Return all property types, without checking for their inclusion status.
    */

    public @NotNull Set<TypeElement> getAllPropertyTypes()
    {
        Set<TypeElement> result = new HashSet<>();
        for (Property p : properties) {
            TypeElement nte = getPropertyTypeElement(p);
            if (nte != null) {
                result.add(nte);
            }
        }
        return result;
    }

    /**
      Return all reference types, without checking for their inclusion status.
    */

    public @NotNull Set<TypeElement> getAllReferenceTypes()
    {
        Set<TypeElement> result = new HashSet<>();
        for (Reference p : references) {
            TypeElement nte = getReferenceTypeElement(p);
            if (nte != null) {
                result.add(nte);
            }
        }
        return result;
    }

    /**
      Return all attribute types, without checking for their inclusion status.
    */

    public @NotNull Set<TypeElement> getAllAttributeTypes()
    {
        Set<TypeElement> result = new HashSet<>();
        for (AttributeInfo info : typeInfo.getAttributes().values()) {
            TypeElement nte = env.getTypeElement(info.type);
            if (nte != null) {
                result.add(nte);
            }
        }
        return result;
    }

    /**
      Return all unnamed nested element types, without checking for their inclusion status.
    */

    public @NotNull Set<TypeElement> getAllUnnamedNestedElementTypes()
    {
        Set<TypeElement> result = new HashSet<>();
        for (NestedElementInfo info : typeInfo.getUnnamedNestedElements()) {
            TypeElement nte = env.getTypeElement(info.types.getFirst());
            if (nte != null) {
                result.add(nte);
            }
        }
        return result;
    }

    /**
      Return all named nested element types, without checking for their inclusion status.
    */

    public @NotNull Set<TypeElement> getAllNamedNestedElementTypes()
    {
        Set<TypeElement> result = new HashSet<>();
        for (NestedElementInfo info : typeInfo.getNamedNestedElements().values()) {
            TypeElement nte = env.getTypeElement(info.types.getFirst());
            if (nte != null) {
                result.add(nte);
            }
        }
        return result;
    }

    /**
      Return the display name for a named nested element.
      @param e The name returned by getNestedElements().
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull String getNamedElementName(@NotNull NestedElement e)
    {
        String name = env.tagAttributeValue(e.info.definingMethod, "ant.type", "name");
        if (name != null) {
            return name;
        }
        name = e.info.name;
        if (name != null) {
            return name;
        }
        throw new IllegalArgumentException("Not a named element");
    }

    /**
      Get the AntDoc for the type of the specified nested element.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @Nullable AntDoc getNestedElementTypeDoc(@NotNull NestedElement e)
    {
        TypeElement te = env.getTypeElement(e.info.types.getFirst());
        if (te == null) {
            String name = env.getTypeName(e.info.types.getFirst());
            env.getReporter().print(Diagnostic.Kind.ERROR, "Unknown type: " + name);
            return null;
        }

        return env.getAntDoc(te);
    }

    /**
      Return HTML text that describes a named nested element type.

      @return description, or null no description is available.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @Nullable String getNamedNestedElementDescription(@NotNull NestedElement e)
    {
        String d = env.getDescription(e.info.definingMethod);
        if (d != null) {
            return d;
        }
        if (e.info.constructor != null) {
            return env.getDescription(e.info.constructor);
        }
        return null;
    }

    /**
      Return HTML text that describes an unnamed nested element type.

      @return description, or null no description is available.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @Nullable String getNestedElementTypeDescription(@NotNull NestedElement e)
    {
        String d = env.getDescription(e.info.definingMethod);
        if (d == null) {
            TypeElement te = env.getTypeElement(e.info.types.getFirst());
            if (te != null) {
                d = env.getDescription(te);
                if (d == null) {
                    String thisName = env.getSimpleTypeName(thisType.asType());
                    // debug
                    env.getReporter().print(Diagnostic.Kind.WARNING,
                      "No description found for nested element " + thisName
                        + " in " + thisName + "." + show(e.info.definingMethod));
                }
            }
        }
        return d;
    }

    /**
      Return HTML text that displays the type name of a nested element.
      If possible, the HTML text should provide the text in the form of a link to type documentation.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull String getNestedElementTypeLinked(@NotNull NestedElement e)
    {
        return env.getTypeNameLinked(e.info.types.getFirst());
    }

    /**
      Return HTML text that displays the type name of a nested element.
      If possible, the HTML text should provide the text in the form of a link to type documentation.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @NotNull String getNestedElementTypesLinked(@NotNull NestedElement e)
    {
        // The nested types must be referenced in this class because the defining method refers to the type, as a
        // method argument type.

        List<TypeMirror> types = e.info.types;
        StringBuilder sb = new StringBuilder();
        for (TypeMirror type : types) {
            sb.append(", ");
            sb.append(typeToLink(type));
        }
        return sb.substring(2);
    }

    private @NotNull NestedElement toNestedElement(@NotNull NestedElementInfo info)
    {
        List<TypeMirror> types = info.types;
        String typeNames = getTypeNames(types);
        return new NestedElement(info, typeNames);
    }

    private class NestedElementComparator
      implements Comparator<NestedElementInfo>
    {
        @Override
        public int compare(@NotNull NestedElementInfo o1, @NotNull NestedElementInfo o2)
        {
            long n1 = getSourceLine(o1.definingMethod);
            long n2 = getSourceLine(o2.definingMethod);
            return (int) (n1 - n2);
        }
    }

    private long getSourceLine(@NotNull Element e)
    {
        return env.getLineNumber(e);
    }

//    /**
//      Find all subclasses of the given abstract class or interface.
//      Does NOT match the class itself.
//    */
//
//    // TBD: is this needed?
//
//    public @NotNull List<String> getImplementingClasses(@NotNull String className)
//    {
//        List<String> imps = new ArrayList<String>();
//        Elements es = env.getElementUtils();
//        TypeElement thisClass = es.getTypeElement(className);
//        TypeMirror t = thisClass.asType();
//        for (Element e : env.getIncludedElements()) {
//            if (e instanceof TypeElement) {
//                TypeElement te = (TypeElement) e;
//                TypeMirror tm = te.asType();
//                Types types = env.getTypeUtils();
//                if (types.isSubtype(tm, t)) {
//                    imps.add(te.getQualifiedName().toString());
//                }
//            }
//        }
//        return imps;
//    }

    /**
      Return the fully qualified name of the defining class of this entity.
    */

    // For template use
    public @NotNull String getFullClassName()
    {
        return thisType.getQualifiedName().toString();
    }

    /**
      Return the simple name of the defining class of this entity.
      @see #getAntName()
    */

    // For template use
    public @NotNull String getClassName()
    {
        return thisType.getSimpleName().toString();
    }

    /**
      Return an optional category prefix for use as a title in a menu.
    */

    // For template use
    public @NotNull String getAntCategoryPrefix()
    {
        String category = getAntCategory();
        return category != null ? env.getAntCategoryPrefix(category) : "";
    }

    /**
      Return the "category" of this Ant "task" or "type"

      @return The value of the "category" attribute of the ant entity.
    */

    // For template use
    public @Nullable String getAntCategory()
    {
        return getAntCategory(thisType);
    }

    private @Nullable String getAntCategory(@NotNull Element element)
    {
        for (String e : antEntities) {
            String s = env.tagAttributeValue(element, e, "category");
            if (s != null) {
                return s;
            }
        }
        Element container = element.getEnclosingElement();
        return container != null ? getAntCategory(container) : null;
    }

    /**
      @return true if the class has an ant tag in it
    */

    public boolean isTagged()
    {
        return isTagged(thisType);
    }

    private boolean isTagged(@NotNull Element e)
    {
        for (String en : antEntities) {
            String s = env.tagRawValue(e, en);
            if (s != null) {
                return true;
            }
        }

        return false;
    }

    private boolean isProperty(@NotNull VariableElement e)
    {
        String tag = env.tagRawValue(e, "ant.prop");
        if (tag != null) {
            Object value = e.getConstantValue();
            if (value instanceof String name) {
                String dn = env.tagAttributeValue(e, "ant.prop", "name");
                if (dn != null && !dn.equals(name)) {
                    env.getReporter().print(Diagnostic.Kind.WARNING,
                      String.format("Inconsistent property names %s and %s", name, dn));
                }
                return true;
            }
        }
        return false;
    }

    private boolean isReference(@NotNull VariableElement e)
    {
        String tag = env.tagRawValue(e, "ant.ref");
        if (tag != null) {
            Object value = e.getConstantValue();
            if (value instanceof String id) {
                String dd = env.tagAttributeValue(e, "ant.ref", "name");
                if (dd != null && !dd.equals(id)) {
                    env.getReporter().print(Diagnostic.Kind.WARNING,
                      String.format("Inconsistent reference IDs %s and %s", id, dd));
                }
                return true;
            }
        }
        return false;
    }

    // For template use
    public @NotNull String getAntKind()
    {
        return isTask() ? "Task" : "Type";
    }

    /**
      Return the name of this type from Ant's perspective.

      @return The value of name attribute of the ant tag, if it exists.
      Otherwise, the Java class name.
    */

    // For template use
    public @NotNull String getAntName()
    {
        return getAntName(thisType);
    }

    /**
      Return an AntDoc for the type of a named nested element.
    */

    // For template use
    @SuppressWarnings("ClassEscapesDefinedScope")
    public @Nullable AntDoc getNamedElementDoc(@NotNull NestedElement e)
    {
        TypeElement te = env.getTypeElement(e.info.types.getFirst());
        if (te == null) {
            env.getReporter().print(Diagnostic.Kind.WARNING,
              "No type element for type of nested element with name " + e.info.name);
            return null;
        }
        return env.getAntDoc(te);
    }

    private <T> T debug(T t)
    {
        System.out.println(t != null ? t : "NULL");
        return t;
    }

    /**
      @return true if this Ant Task/Type expects text in the element body.
    */

    // For template use
    public boolean supportsText()
    {
        return typeInfo.supportsText();
    }

    /**
      Return HTML text that describes the behavior of embedded text content.
      @return text, or null to allow the template to supply default text.
    */

    // For template use
    public @Nullable String getTextDescription()
    {
        ExecutableElement m = typeInfo.getAddTextMethod();
        if (m != null) {
            return env.getDescription(m);
        }
        return null;
    }

    private @NotNull String show(@NotNull ExecutableElement e)
    {
        // Avoid verbose descriptions that include qualified parameter type names including annotations.

        StringBuilder sb = new StringBuilder();
        sb.append(e.getSimpleName());
        sb.append("(");
        List<? extends VariableElement> ps = e.getParameters();
        boolean isFirst = true;
        for (VariableElement p : ps) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(", ");
            }
            TypeMirror pt = p.asType();
            sb.append(env.getSimpleTypeName(pt));
        }
        sb.append(")");
        return sb.toString();
    }

    private @NotNull String getAntName(@NotNull Element e)
    {
        String name = getDeclaredName(e);
        if (name != null) {
            return name;
        }

        Element container = e.getEnclosingElement();
        if (container instanceof DeclaredType) {
            String en = getAntName(container);
            String qn = thisType.getQualifiedName().toString();
            return en + "." + qn.substring(qn.lastIndexOf('$') + 1);
        }

        return thisType.getSimpleName().toString();
    }

    private @Nullable String getDeclaredName(@NotNull Element e)
    {
        for (String en : antEntities) {
            String name = env.tagAttributeValue(e, en, "name");
            if (name != null) {
                return name;
            }
        }
        return null;
    }

    // Private helper methods:

    /**
      Create a "displayable" description of a type

      @return a string with the name for the given type
    */

    private @NotNull String typeToString(@NotNull TypeMirror t)
    {
        TypeElement te = env.getTypeElement(t);
        if (te == null) {
            return env.getTypeName(t);
        }

        String name = te.getSimpleName().toString();
        return name.replace('$', '.'); // inners use dollar signs
    }

//    private @NotNull String createEnumeratedValuesDescription(@NotNull List<String> values)
//    {
//        StringBuilder sb = new StringBuilder();
//        for (String value : values) {
//            sb.append(String.format(", \"%s\"", value));
//        }
//        String s = sb.substring(2);
//        return String.format("String [%s]", s);
//    }

    public int compareTo(Object o)
    {
        AntDoc otherDoc = (AntDoc)o;
        String fullName1 = getAntCategory() +":" + getAntName();
        String fullName2 = otherDoc.getAntCategory() +":"+ otherDoc.getAntName();
        return fullName1.compareTo(fullName2);
    }

    public @NotNull TypeElement getTypeElement()
    {
        return thisType;
    }

    public boolean isSubtypeOf(@NotNull String typeName)
    {
        return env.isSubtypeOf(thisType.asType(), typeName);
    }

    @Override
    public @NotNull String toString()
    {
        return super.toString() + " " + getFullClassName();
    }
}
