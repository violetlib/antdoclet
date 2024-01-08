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

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;

/**
  An object of this class represents a Java class that is: an Ant Task, or an Ant Type.
  It provides information about the Task/Type's attributes, nested elements and more.
  It is intended to be used for documenting Ant Tasks/Types.

  @author Fernando Dobladez <dobladez@gmail.com>
*/
@SuppressWarnings("removal, deprecation")
public class AntDoc implements Comparable
{

    /**
      An IntrospectionHelper (from Ant) to interpret ant-specific conventions from Tasks and Types.
    */
    private IntrospectionHelper introHelper;

    /**
      Javadoc description for this type.
    */
    private final @NotNull TypeElement thisType;
    private final @NotNull DocletEnvironment env;
    private final @NotNull Class<?> clazz;
    private final @NotNull DocUtils docUtils;
    private final @NotNull Reporter reporter;
    private final @NotNull TypeInfo typeInfo;

    private static final List<String> antEntities = List.of("ant.task", "ant.type", "ant.prop", "ant.ref");

    private AntDoc(@NotNull IntrospectionHelper ih,
                   @NotNull DocletEnvironment env,
                   @NotNull TypeElement thisType,
                   @NotNull Class<?> clazz,
                   @NotNull Reporter reporter)
    {
        this.thisType = thisType;
        this.env = env;
        this.introHelper = ih;
        this.clazz = clazz;
        this.docUtils = DocUtils.create(env, reporter);
        this.reporter = reporter;
        this.typeInfo = Introspection.getInfo(thisType, docUtils);
    }

    public static @Nullable AntDoc getInstance(@NotNull String clazz,
                                               @NotNull DocletEnvironment rootdoc,
                                               @NotNull Reporter reporter)
    {
        Class<?> c = null;

        try {
            c = Class.forName(clazz);
        } catch (Throwable ee) {
            // try inner class (replacing last . for $)
            int lastdot = clazz.lastIndexOf(".");

            if (lastdot >= 0) {
                String newName = clazz.substring(0, lastdot) + "$" + clazz.substring(lastdot + 1);

                // System.out.println("trying inner:"+newName);

                try {
                    c = Class.forName(newName);
                } catch (Throwable e) {
                    System.err.println("WARNING: AntDoclet couldn't find '"
                      + clazz
                      + "'. Make sure it's in the CLASSPATH " + ee);
                    ee.printStackTrace();
                }
            }
        }

        return c != null ? getInstance(c, rootdoc, reporter) : null;
    }

    public static @Nullable AntDoc getInstance(@NotNull Class<?> clazz,
                                               @Nullable DocletEnvironment env,
                                               @NotNull Reporter reporter)
    {
        if (env == null) {
            return null;
        }

        if (!Task.class.isAssignableFrom(clazz) && !DataType.class.isAssignableFrom(clazz)) {
            return null;
        }

        IntrospectionHelper ih = IntrospectionHelper.getHelper(clazz);
        Elements es = env.getElementUtils();
        TypeElement type = es.getTypeElement(clazz.getName());

        if (type != null && !shouldIgnore(type, env, reporter)) {
            return new AntDoc(ih, env, type, clazz, reporter);
        }

        return null;
    }

    private boolean shouldIgnore(TypeElement doc)
    {
        for (String e : antEntities) {
            String s = docUtils.tagAttributeValue(doc, e, "ignore");
            if ("true".equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldIgnore(@NotNull TypeElement doc,
                                        @NotNull DocletEnvironment env,
                                        @NotNull Reporter reporter)
    {
        DocUtils utils = DocUtils.create(env, reporter);
        for (String e : antEntities) {
            String s = utils.tagAttributeValue(doc, e, "ignore");
            if ("true".equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    /**
      @return Whether this type represents an Ant Task
    */

    public boolean isTask()
    {
        return Task.class.isAssignableFrom(this.clazz);
    }

    /**
      @return Whether this type represents an Ant DataType
    */

    public boolean isType()
    {
        return DataType.class.isAssignableFrom(this.clazz);
    }

    /**
      @return Is this an Ant Task Container?
    */

    public boolean isTaskContainer()
    {
        return TaskContainer.class.isAssignableFrom(this.clazz);
    }

    /**
      @return Should this entity be excluded?
    */

    public boolean isIgnored()
    {
        return shouldIgnore(thisType);
    }

    /**
      @return Is the source code for this type included in this javadoc run?
    */

    public boolean sourceIncluded()
    {
        Set<? extends Element> included = env.getIncludedElements();
        return thisType != null && included.contains(thisType);
    }

    /**
      @return The source comment (description) for this class (task/type)
    */

    public String getComment()
    {
        return docUtils.getComment(thisType);
    }

    /**
      @return Short comment for this class (basically, the first sentence)
    */

    public String getShortComment()
    {
        return docUtils.getShortComment(thisType);
    }

    /**
      Get the attributes in this class from Ant's point of view.

      @return Collection of Ant attributes, excluding those inherited from
      org.apache.tools.ant.Task, or null if there are none
    */

    public Collection<String> getAttributes()
    {
        ArrayList<String> attrs = Collections.list(introHelper.getAttributes());

        if (attrs.isEmpty()) {
            return null;
        } else {
            // filter out all attributes inherited from Task, since they are
            // common to all Ant Tasks and tend to confuse
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(Task.class);
                PropertyDescriptor[] commonProps = beanInfo.getPropertyDescriptors();
                for (int i = 0; i < commonProps.length; i++) {
                    String propName = commonProps[i].getName().toLowerCase();
                    // System.out.println("Ignoring task property:"+propName);
                    attrs.remove(propName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            attrs.remove("refid");
            return sortAttributes(attrs);
        }
    }

    private Collection<String> sortAttributes(List<String> names)
    {
        names.sort((o1, o2) -> {
            long pos1 = getAttributeSourceLine(o1);
            long pos2 = getAttributeSourceLine(o2);
            return (int) (pos1 - pos2);
        });
        return names;
    }

    private long getAttributeSourceLine(String name)
    {
        ExecutableElement method = getMethodFor(this.thisType, name);
        if (method == null) {
            return 0;
        }
        return getSourceLine(method);
    }

    public List<String> getProperties()
    {
        List<VariableElement> l = getPropertyFieldsSorted();
        List<String> ss = new ArrayList<>();
        for (VariableElement f : l) {
            ss.add(f.getSimpleName().toString());
        }
        return ss;
    }

    public VariableElement getPropertyField(String fieldName)
    {
        List<VariableElement> fields = getPropertyFields();
        for (VariableElement field : fields) {
            if (field.getSimpleName().toString().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    public List<VariableElement> getPropertyFieldsSorted()
    {
        List<VariableElement> l = getPropertyFields();
        l.sort((o1, o2) -> {
            long pos1 = getSourceLine(o1);
            long pos2 = getSourceLine(o2);
            return (int) (pos1 - pos2);
        });
        return l;
    }

    public List<VariableElement> getPropertyFields()
    {
        List<VariableElement> l = new ArrayList<>();
        List<VariableElement> fields = getFields(thisType);
        for (VariableElement field : fields) {
            Set<Modifier> mods = field.getModifiers();
            if (mods.contains(Modifier.PUBLIC)
              && mods.contains(Modifier.STATIC)
              && mods.contains(Modifier.FINAL) && isProperty(field)) {
                l.add(field);
            }
        }
        return l;
    }

    public List<String> getReferences()
    {
        List<VariableElement> l = getReferenceFieldsSorted();
        List<String> ss = new ArrayList<>();
        for (VariableElement f : l) {
            ss.add(f.getSimpleName().toString());
        }
        return ss;
    }

    public VariableElement getReferenceField(String fieldName)
    {
        List<VariableElement> fields = getReferenceFields();
        for (VariableElement field : fields) {
            if (field.getSimpleName().toString().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    public List<VariableElement> getReferenceFieldsSorted()
    {
        List<VariableElement> l = getReferenceFields();
        l.sort((o1, o2) -> {
            long pos1 = getSourceLine(o1);
            long pos2 = getSourceLine(o2);
            return (int) (pos1 - pos2);
        });
        return l;
    }

    public List<VariableElement> getReferenceFields()
    {
        List<VariableElement> l = new ArrayList<>();
        List<VariableElement> fields = getFields(thisType);
        for (VariableElement field : fields) {
            Set<Modifier> mods = field.getModifiers();
            if (mods.contains(Modifier.PUBLIC)
              && mods.contains(Modifier.STATIC)
              && mods.contains(Modifier.FINAL) && isReference(field)) {
                l.add(field);
            }
        }
        return l;
    }

    private static List<VariableElement> getFields(TypeElement t)
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
      @return a collection of the "Nested Elements" that this Ant tasks accepts, or null if there are none
    */

    public @NotNull List<String> getNestedElements()
    {
        List<String> c = new ArrayList<>();

        Enumeration<String> elements = introHelper.getNestedElements();
        if (elements.hasMoreElements()) {
            while (elements.hasMoreElements()) {
                c.add(elements.nextElement());
            }
            c.sort((o1, o2) -> {
                long pos1 = getNestedElementSourceLine(o1);
                long pos2 = getNestedElementSourceLine(o2);
                return (int) (pos1 - pos2);
            });
        }
        return c;
    }

    private long getNestedElementSourceLine(String name)
    {
        ExecutableElement method = getMethodForType(this.thisType, name);
        if (method == null) {
            return 0;
        }
        return getSourceLine(method);
    }

    private long getSourceLine(@NotNull Element e)
    {
        return docUtils.getLineNumber(e);
    }

    /**
      Get the extension points for this class. Derived from the add(instance) or addConfigured(instance) methods. Each
      class is technically an Ant type, but typically you wont want to document it; just its concrete implementations.
      You can pass these strings to getImplementingClasses() to finds the available implementations. return The fully
      qualified class names, or null if there are none
    */

    public @NotNull Iterator<String> getNestedTypes()
    {
        List<Method> mm = introHelper.getExtensionPoints();
        if (mm.isEmpty()) {
            return null;
        } else {
            Collection<String> c = new HashSet<>();
            for (Method m : mm) {
                String classname = m.getParameterTypes()[0].getName();
                c.add(classname);
            }
            return c.iterator();
        }
    }

    /**
      Find all subclasses of the given abstract class or interface.
      Does NOT match the class itself.
    */

    public @NotNull Iterator<String> getImplementingClasses(@NotNull String className)
    {
        List<String> imps = new ArrayList<String>();
        Elements es = env.getElementUtils();
        TypeElement thisClass = es.getTypeElement(className);
        TypeMirror t = thisClass.asType();
        for (Element e : env.getIncludedElements()) {
            if (e instanceof TypeElement) {
                TypeElement te = (TypeElement) e;
                TypeMirror tm = te.asType();
                Types types = env.getTypeUtils();
                if (types.isSubtype(tm, t)) {
                    imps.add(te.getQualifiedName().toString());
                }
            }
        }
        return imps.iterator();
    }

    /**
      Get the AntDoc for the specified (arbitrary) class.

      @param className
      @return null if the class cannot be found on the classpath.
    */

    public @Nullable AntDoc getTypeDoc(String className)
    {
        return getInstance(className, env, reporter);
    }

    /**
      Get the comment for the add or addconfigured method for the specified class (extension).

      @return The source comment (description), or null if the class cannot be found.
    */
    public String getCommentForType(String type)
    {
        notNull(type, "type");
        ExecutableElement m = getMethodForType(thisType, type);
        return m == null ? null : env.getElementUtils().getDocComment(m);
    }

    public @NotNull String getFullClassName()
    {
        return clazz.getName();
    }

    /**
      @return true if this refers to an inner-class
    */

    public boolean isInnerClass()
    {
        if (thisType == null) {
            return false;
        }
        Elements es = env.getElementUtils();
        Element outer = es.getOutermostTypeElement(thisType);
        return outer != thisType;
    }

    /**
      Get the comment about the requirement of this attribute. The comment if extracted from the {@code ant.required} tag.
      @param attribute
      @return A comment. A null String if this attribute is not declared as required.
    */
    public @Nullable String getAttributeRequired(@NotNull String attribute)
    {
        ExecutableElement method = getMethodFor(this.thisType, attribute);
        if (method == null) {
            return null;
        }
        return docUtils.tagValue(method, "ant.required");
    }

    /**
      Get the comment about the "non-requirement" of this attribute. The comment if extracted from the {@code ant.not-required} tag.
      @param attribute
      @return A comment. A null String if this attribute is not declared as non-required.
    */

    public @Nullable String getAttributeNotRequired(@NotNull String attribute)
    {
        ExecutableElement method = getMethodFor(this.thisType, attribute);
        if (method == null) {
            return null;
        }
        return docUtils.tagValue(method, "ant.not-required");
    }

    public @NotNull String getPropertyName(@NotNull String fieldName)
    {
        VariableElement field = getPropertyField(fieldName);
        if (field != null) {
            String name = docUtils.tagAttributeValue(field, "ant.prop", "name");
            if (name != null) {
                return name;
            }
        }
        return "";
    }

    public @NotNull String getPropertyDescription(@NotNull String fieldName)
    {
        VariableElement field = getPropertyField(fieldName);
        if (field != null) {
            String d = docUtils.getComment(field);
            if (d != null) {
                return d;
            }
        }
        return "";
    }

    public @NotNull String getPropertyType(@NotNull String fieldName)
    {
        VariableElement field = getPropertyField(fieldName);
        if (field != null) {
            String type = docUtils.tagAttributeValue(field, "ant.prop", "type");
            if (type != null) {
                return type;
            }
        }
        return "String";
    }

    public @NotNull String getReferenceName(@NotNull String fieldName)
    {
        VariableElement field = getReferenceField(fieldName);
        if (field != null) {
            String name = docUtils.tagAttributeValue(field, "ant.ref", "name");
            if (name != null) {
                return name;
            }
        }
        return "";
    }

    public @NotNull String getReferenceDescription(@NotNull String fieldName)
    {
        VariableElement field = getReferenceField(fieldName);
        if (field != null) {
            String d = docUtils.getComment(field);
            if (d != null) {
                return d;
            }
        }
        return "";
    }

    public @NotNull String getReferenceType(@NotNull String fieldName)
    {
        VariableElement field = getReferenceField(fieldName);
        if (field != null) {
            String type = docUtils.tagAttributeValue(field, "ant.ref", "type");
            if (type != null) {
                return type;
            }
        }
        return "";
    }

    public @NotNull String getAntCategoryPrefix()
    {
        String category = getAntCategory();
        if (category != null) {
            return category + ": ";
        }
        return "";
    }

    /**
      Return the "category" of this Ant "task" or "type"

      @return The value of the "category" attribute of the ant entity.
    */

    public @Nullable String getAntCategory()
    {
        return getAntCategory(thisType);
    }

    public @Nullable String getAntCategory(@NotNull Element element)
    {
        for (String e : antEntities) {
            String s = docUtils.tagAttributeValue(element, e, "category");
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
        for (String e : antEntities) {
            String s = docUtils.tagAttributeValue(this.thisType, e, "name");
            if (s != null) {
                return true;
            }
        }

        return false;
    }

    public boolean isProperty(@NotNull Element e)
    {
        String antName = docUtils.tagAttributeValue(e, "ant.prop", "name");
        return antName != null;
    }

    public boolean isReference(@NotNull Element e)
    {
        String antName = docUtils.tagAttributeValue(e, "ant.ref", "name");
        return antName != null;
    }

    /**
      Return the name of this type from Ant's perspective.

      @return The value of name attribute of the ant tag, if it exists.
      Otherwise, the Java class name.
    */

    public @NotNull String getAntName()
    {
        return getAntName(thisType);
    }

    public @NotNull String getAntName(Element e)
    {
        for (String en : antEntities) {
            String name = docUtils.tagAttributeValue(e, en, "name");
            if (name != null) {
                return name;
            }
        }

        Element container = e.getEnclosingElement();
        if (container instanceof DeclaredType) {
            String en = getAntName(container);
            return en + "." + this.clazz.getName().substring(
              this.clazz.getName().lastIndexOf('$') + 1);
        }

        return typeToString(this.clazz);
    }

    /**
      @see #getNestedElements()
      @param elementName
      @return The java type for the specified element accepted by this task
    */

    public Class<?> getElementType(@NotNull String elementName)
    {
        return introHelper.getElementType(elementName);
    }

    /**
      Return a new AntDoc for the given "element"
    */

    public AntDoc getElementDoc(@NotNull String elementName)
    {
        Class<?> elementClass = getElementType(elementName);

        System.err.println("Class of " + elementName + " is " + elementClass.getName());

        return getInstance(elementClass, this.env, reporter);
    }

    /**
      Return a new AntDoc for the "container" of this type. Only makes sense for inner classes.
    */

    public @Nullable Element getContainerDoc()
    {
        if (!isInnerClass()) {
            return null;
        }
        return thisType.getEnclosingElement();
    }

    /**
      Return the display name for the specified attribute.
    */

    public @NotNull String getAttributeName(@NotNull String attribute)
    {
        ExecutableElement method = getMethodFor(this.thisType, attribute);
        if (method != null) {
            String name = docUtils.tagAttributeValue(method, "ant.prop", "name");
            if (name != null) {
                return name;
            }
        }
        return attribute;
    }

    /**
      Return the name of the type for the specified attribute.
    */

    public @NotNull String getAttributeType(String attribute)
    {
        return typeToString(introHelper.getAttributeType(attribute));
    }

    /**
      Retrieves the doc comment for the given attribute obtained from the getter or setter method.
      The comment of the setter is used preferably to the getter comment.

      @param attribute The attribute.
      @return The comment for the specified attribute, or null if none.
    */

    public @NotNull String getAttributeComment(@NotNull String attribute)
    {
        ExecutableElement method = getMethodFor(this.thisType, attribute);
        if (method == null) {
            return "";
        }
        String comment = docUtils.getComment(method);
        return comment != null ? comment : "";
    }

    /**
      Searches the given class for the appropriate setter or getter for the given attribute.
      This method only returns the getter if no setter is available.
      If the given class provides no method declaration, the superclasses are
      searched recursively.

      @param attribute
      @return The MethodDoc for the given attribute or null if not found
    */

    private @Nullable ExecutableElement getMethodFor(@Nullable TypeElement classDoc, @NotNull String attribute)
    {
        if (classDoc == null) {
            return null;
        }

        // we give priority to the documentation on the "setter" method of the attribute but if the documentation
        // is only on the "getter", use it we give priority to the documentation on the "setter" method of the
        // attribute but if the documentation is only on the "getter", use it.

        Elements elements = env.getElementUtils();
        List<? extends Element> members = elements.getAllMembers(classDoc);
        for (Element m : members) {
            if (m instanceof ExecutableElement) {
                ExecutableElement e = (ExecutableElement) m;
                String name = e.getSimpleName().toString();
                if (name.equalsIgnoreCase("set" + attribute)) {
                    return e;
                } else if (name.equalsIgnoreCase("get" + attribute)) {
                    return e;
                }
            }
        }
        TypeMirror superclass = classDoc.getSuperclass();
        if (superclass != null) {
            Element superType = env.getTypeUtils().asElement(superclass);
            if (superType instanceof TypeElement) {
                return getMethodFor((TypeElement) superType, attribute);
            }
        }
        return null;
    }

    /**
      Searches the given class for the appropriate create or add method for the given nested type. If the given class
      provides no suitable method declaration, the superclasses are searched recursively.

      @param nestedType
      @return The MethodDoc for the given attribute or null if not found
    */

    private @Nullable ExecutableElement getMethodForType(@NotNull TypeElement classDoc, @NotNull String nestedType)
    {
        notNull(classDoc, "classDoc");
        notNull(nestedType, "nestedType");

        Elements elements = env.getElementUtils();
        List<? extends Element> members = elements.getAllMembers(classDoc);
        for (Element m : members) {
            if (m instanceof ExecutableElement) {
                ExecutableElement e = (ExecutableElement) m;
                String name = e.getSimpleName().toString();

                if (name.equalsIgnoreCase("add") || name.equalsIgnoreCase("addConfigured")) {
                    List<? extends VariableElement> parameters = e.getParameters();
                    if (parameters.size() == 1) {
                        VariableElement p = parameters.getFirst();
                        TypeMirror pt = p.asType();
                        if (pt instanceof DeclaredType) {
                            DeclaredType dt = (DeclaredType) pt;
                            TypeElement pte = (TypeElement) dt.asElement();
                            String ptn = pte.getSimpleName().toString();
                            if (nestedType.endsWith(ptn)) {
                                return e;
                            }
                        }
                    }
                }
                String typeLC = null;
                String nameLC = name.toLowerCase();
                if (nameLC.startsWith("createconfigured")) {
                    typeLC = name.substring(16);
                } else if (nameLC.startsWith("create")) {
                    typeLC = name.substring(6);
                }
                if (typeLC != null && typeLC.equalsIgnoreCase(nestedType)) {
                    List<? extends VariableElement> parameters = e.getParameters();
                    if (parameters.isEmpty()) {
                        return e;
                    }
                }
            }
        }
        TypeMirror superclass = classDoc.getSuperclass();
        if (superclass != null) {
            Element superType = env.getTypeUtils().asElement(superclass);
            if (superType instanceof TypeElement) {
                return getMethodFor((TypeElement) superType, nestedType);
            }
        }
        return null;
    }

    private static String toCapitalized(String s)
    {
        if (s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /**
      @return true if this Ant Task/Type expects characters in the element body.
    */

    public boolean supportsCharacters()
    {
        return introHelper.supportsCharacters();
    }

    // Private helper methods:

    /**
      Create a "displayable" name for the given type

      @param clazz
      @return a string with the name for the given type
    */

    private static @NotNull String typeToString(@NotNull Class<?> clazz)
    {
        String fullName = clazz.getName();
        String name = fullName.lastIndexOf(".") >= 0
          ? fullName.substring(fullName.lastIndexOf(".") + 1)
          : fullName;

        String result = name.replace('$', '.'); // inners use dollar signs
        if (EnumeratedAttribute.class.isAssignableFrom(clazz)) {
            try {
                EnumeratedAttribute att = (EnumeratedAttribute) clazz.getDeclaredConstructor().newInstance();
                result = "String [";
                String[] values = att.getValues();
                result += "\"" + values[0] + "\"";
                for (int i = 1; i < values.length; i++) {
                    result += ", \"" + values[i] + "\"";
                }
                result += "]";
            } catch (java.lang.IllegalAccessException ignore) {
                // ignore, may a protected/private Enumeration
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public int compareTo(Object o)
    {
        AntDoc otherDoc = (AntDoc)o;
        String fullName1 = getAntCategory() +":" + getAntName();
        String fullName2 = otherDoc.getAntCategory() +":"+ otherDoc.getAntName();
        return fullName1.compareTo(fullName2);
    }

    /**
      Argument check for methods - not nullable.
      Typed, so you can use instancevar = notNull(arg,"arg");

      @param t
      @param msg Message for
      @throws NullPointerException if t is null
    */
    public static <T> T notNull(T t, String msg)
    {
        if (t == null) {
            throw new NullPointerException(msg);
        }
        return t;
    }
}
