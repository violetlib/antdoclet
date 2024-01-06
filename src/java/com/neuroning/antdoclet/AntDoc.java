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

import com.sun.javadoc.*;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.types.EnumeratedAttribute;

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
    private ClassDoc doc;

    /**
      Javadoc "root node"
    */
    private RootDoc rootdoc;

    /**
      The java Class for this type
    */
    private Class clazz;

    private static final List<String> antEntities = List.of("ant.task", "ant.type", "ant.prop", "ant.ref");

    private AntDoc(IntrospectionHelper ih, RootDoc rootdoc, ClassDoc doc, Class clazz)
    {
        this.doc = doc;
        this.rootdoc = rootdoc;
        this.introHelper = ih;
        this.clazz = clazz;
    }

    public static AntDoc getInstance(String clazz)
    {
        return getInstance(clazz, null);
    }

    public static AntDoc getInstance(Class<?> clazz)
    {
        return getInstance(clazz, null);
    }

    public static AntDoc getInstance(String clazz, RootDoc rootdoc)
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

        return c != null ? getInstance(c, rootdoc) : null;
    }

    public static AntDoc getInstance(Class<?> clazz, RootDoc rootdoc)
    {
        AntDoc d = null;
        IntrospectionHelper ih = IntrospectionHelper.getHelper(clazz);
        ClassDoc doc = null;

        if (rootdoc != null) {
            doc = rootdoc.classNamed(clazz.getName());
        }

        if (!shouldIgnore(doc)) {
            d = new AntDoc(ih, rootdoc, doc, clazz);
        }

        return d;
    }

    private static boolean shouldIgnore(Doc doc)
    {
        for (String e : antEntities) {
            String s = Util.tagAttributeValue(doc, e, "ignore");
            if ("true".equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    /**
      @return Whether this represents an Ant Task (otherwise, it is assumed as a Type)
    */
    public boolean isTask()
    {
        return Task.class.isAssignableFrom(this.clazz);
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
        return shouldIgnore(doc);
    }

    /**
      @return Is the source code for this type included in this javadoc run?
    */
    public boolean sourceIncluded()
    {
        return doc != null && doc.isIncluded();
    }

    /**
      @return The source comment (description) for this class (task/type)
    */
    public String getComment()
    {
        return Util.getProcessedText(doc);
    }

    /**
      @return Short comment for this class (basically, the first sentence)
    */
    public String getShortComment()
    {
        if (doc == null) {
            return null;
        }

        Tag[] firstTags = doc.firstSentenceTags();
        if (firstTags.length > 0 && firstTags[0] != null) {
            return firstTags[0].text();
        }
        return null;
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
            int pos1 = getAttributeSourceLine(o1);
            int pos2 = getAttributeSourceLine(o2);
            return pos1 - pos2;
        });
        return names;
    }

    private int getAttributeSourceLine(String name)
    {
        MethodDoc method = getMethodFor(this.doc, name);
        if (method == null) {
            return 0;
        }
        return getSourceLine(method);
    }

    public List<String> getProperties()
    {
        List<FieldDoc> l = getPropertyFieldsSorted();
        List<String> ss = new ArrayList<>();
        for (FieldDoc f : l) {
            ss.add(f.name());
            System.err.println("Property: " + f.name());
        }
        return ss;
    }

    public FieldDoc getPropertyField(String fieldName)
    {
        List<FieldDoc> fields = getPropertyFields();
        for (FieldDoc field : fields) {
            if (field.name().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    public List<FieldDoc> getPropertyFieldsSorted()
    {
        List<FieldDoc> l = getPropertyFields();
        l.sort((o1, o2) -> {
            int pos1 = getSourceLine(o1);
            int pos2 = getSourceLine(o2);
            return pos1 - pos2;
        });
        return l;
    }

    public List<FieldDoc> getPropertyFields()
    {
        List<FieldDoc> l = new ArrayList<>();
        FieldDoc[] fields = doc.fields();
        for (FieldDoc field : fields) {
            if (field.isPublic() && field.isStatic() && field.isFinal() && isProperty(field)) {
                l.add(field);
            }
        }
        return l;
    }

    public List<String> getReferences()
    {
        List<FieldDoc> l = getReferenceFieldsSorted();
        List<String> ss = new ArrayList<>();
        for (FieldDoc f : l) {
            ss.add(f.name());
            System.err.println("Reference: " + f.name());
        }
        return ss;
    }

    public FieldDoc getReferenceField(String fieldName)
    {
        List<FieldDoc> fields = getReferenceFields();
        for (FieldDoc field : fields) {
            if (field.name().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    public List<FieldDoc> getReferenceFieldsSorted()
    {
        List<FieldDoc> l = getReferenceFields();
        l.sort((o1, o2) -> {
            int pos1 = getSourceLine(o1);
            int pos2 = getSourceLine(o2);
            return pos1 - pos2;
        });
        return l;
    }

    public List<FieldDoc> getReferenceFields()
    {
        List<FieldDoc> l = new ArrayList<>();
        FieldDoc[] fields = doc.fields();
        for (FieldDoc field : fields) {
            if (field.isPublic() && field.isStatic() && field.isFinal() && isReference(field)) {
                l.add(field);
            }
        }
        return l;
    }

    /**
      @return a collection of the "Nested Elements" that this Ant tasks accepts, or null if there are none
    */
    public Iterator<String> getNestedElements()
    {
        Enumeration<String> elements = introHelper.getNestedElements();
        if (elements.hasMoreElements()) {
            List<String> c = new ArrayList<>();
            while (elements.hasMoreElements()) {
                c.add(elements.nextElement());
            }
            c.sort((o1, o2) -> {
                int pos1 = getNestedElementSourceLine(o1);
                int pos2 = getNestedElementSourceLine(o2);
                return pos1 - pos2;
            });
            return c.iterator();
        } else {
            return null;
        }
    }

    private int getNestedElementSourceLine(String name)
    {
        MethodDoc method = getMethodForType(this.doc, name);
        if (method == null) {
            return 0;
        }
        return getSourceLine(method);
    }

    private int getSourceLine(Doc doc)
    {
        com.sun.javadoc.SourcePosition pos = doc.position();
        if (pos == null) {
            return 0;
        }
        return pos.line();
    }

    /**
      Get the extension points for this class. Derived from the add(instance) or addConfigured(instance) methods. Each
      class is technically an Ant type, but typically you wont want to document it; just its concrete implementations.
      You can pass these strings to getImplementingClasses() to finds the available implementations. return The fully
      qualified class names, or null if there are none
    */
    public Iterator<String> getNestedTypes()
    {
        List<Method> mm = introHelper.getExtensionPoints();
        if (mm.isEmpty()) {
            return null;
        } else {
            Collection c = new HashSet<String>();
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
    public Iterator<String> getImplementingClasses(String className)
    {
        List<String> imps = new ArrayList<String>();
        ClassDoc thisClass = rootdoc.classNamed(className);
        for (ClassDoc cd : rootdoc.classes()) {
            if (cd.subclassOf(thisClass) && !cd.qualifiedName().equals(className)) {
                imps.add(cd.qualifiedName());
            }
        }
        return imps.iterator();
    }

    /**
      Get the AntDoc for the specified (arbitrary) class.

      @param className
      @return null if the class cannot be found on the classpath.
    */
    public AntDoc getTypeDoc(String className)
    {
        return getInstance(className, rootdoc);
    }

    /**
      Get the comment for the add or addconfigured method for the specified class (extension).

      @return The source comment (description), or null if the class cannot be found.
    */
    public String getCommentForType(String type)
    {
        notNull(type, "type");
        MethodDoc m = getMethodForType(doc, type);
        return m==null ? null : m.commentText();
    }

    public String getFullClassName()
    {
        return clazz.getName();
    }

    /**
      @return true if this refers to an inner-class
    */
    public boolean isInnerClass()
    {
        return doc != null && (doc.containingClass() != null);
    }

    /**
      Get the comment about the requirement of this attribute. The comment if extracted from the {@code ant.required} tag.
      @param attribute
      @return A comment. A null String if this attribute is not declared as required.
    */
    public String getAttributeRequired(String attribute)
    {
        MethodDoc method = getMethodFor(this.doc, attribute);
        if (method == null) {
            return null;
        }
        return Util.getProcessedText(method, "ant.required");
    }

    /**
      Get the comment about the "non-requirement" of this attribute. The comment if extracted from the {@code ant.not-required} tag.
      @param attribute
      @return A comment. A null String if this attribute is not declared as non-required.
    */
    public String getAttributeNotRequired(String attribute)
    {
        MethodDoc method = getMethodFor(this.doc, attribute);
        if (method == null) {
            return null;
        }
        return Util.getProcessedText(method, "ant.not-required");
    }

    public String getPropertyName(String fieldName)
    {
        FieldDoc field = getPropertyField(fieldName);
        if (field != null) {
            String name = Util.tagAttributeValue(field, "ant.prop", "name");
            if (name != null) {
                return name;
            }
        }
        return "";
    }

    public String getPropertyDescription(String fieldName)
    {
        FieldDoc field = getPropertyField(fieldName);
        if (field != null) {
            String d = Util.getProcessedText(field);
            if (d != null) {
                return d;
            }
        }
        return "";
    }

    public String getPropertyType(String fieldName)
    {
        FieldDoc field = getPropertyField(fieldName);
        if (field != null) {
            // type needs to be a tag attribute, defaults to String
        }
        return "";
    }

    public String getReferenceName(String fieldName)
    {
        FieldDoc field = getReferenceField(fieldName);
        if (field != null) {
            String name = Util.tagAttributeValue(field, "ant.ref", "name");
            if (name != null) {
                return name;
            }
        }
        return "";
    }

    public String getReferenceDescription(String fieldName)
    {
        FieldDoc field = getReferenceField(fieldName);
        if (field != null) {
            String d = Util.getProcessedText(field);
            if (d != null) {
                return d;
            }
        }
        return "";
    }

    public String getReferenceType(String fieldName)
    {
        FieldDoc field = getReferenceField(fieldName);
        if (field != null) {
            // type needs to be a tag attribute, defaults to String
        }
        return "";
    }

    public String getAntCategoryPrefix()
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
    public String getAntCategory()
    {
        for (String e : antEntities) {
            String s = Util.tagAttributeValue(this.doc, e, "category");
            if (s != null) {
                return s;
            }
        }
        if (getContainerDoc() != null)
            return getContainerDoc().getAntCategory();

        return null;
    }

    /**
      @return true if the class has an ant tag in it
    */
    public boolean isTagged()
    {
        for (String e : antEntities) {
            String s = Util.tagAttributeValue(this.doc, e, "name");
            if (s != null) {
                return true;
            }
        }

        return false;
    }

    public boolean isProperty(Doc doc)
    {
        String antName = Util.tagAttributeValue(doc, "ant.prop", "name");
        return antName != null;
    }

    public boolean isReference(Doc doc)
    {
        String antName = Util.tagAttributeValue(doc, "ant.ref", "name");
        return antName != null;
    }

    /**
      Return the name of this type from Ant's perspective.

      @return The value of name attribute of the ant tag, if it exists.
      Otherwise, the Java class name.
    */
    public String getAntName()
    {
        for (String e : antEntities) {
            String name = Util.tagAttributeValue(this.doc, e, "name");
            if (name != null) {
                return name;
            }
        }

        // Handle inner class case
        if (getContainerDoc() != null) {
            String name = getContainerDoc().getAntName();
            if (name != null) {
                return name
                  + "."
                  + this.clazz.getName().substring(
                  this.clazz.getName().lastIndexOf('$') + 1);
            }
        }

        return typeToString(this.clazz);
    }

    /**
      @see #getNestedElements()
      @param elementName
      @return The java type for the specified element accepted by this task
    */
    public Class<?> getElementType(String elementName)
    {
        return introHelper.getElementType(elementName);
    }

    /**
      Return a new AntDoc for the given "element"
    */
    public AntDoc getElementDoc(String elementName)
    {
        return getInstance(getElementType(elementName), this.rootdoc);
    }

    /**
      Return a new AntDoc for the "container" of this type. Only makes sense for inner classes.
    */
    public AntDoc getContainerDoc()
    {
        if (!isInnerClass()) {
            return null;
        }
        return getInstance(this.doc.containingClass().qualifiedName(), this.rootdoc);
    }

    /**
      Return the name of the type for the specified attribute
    */
    public String getAttributeType(String attributeName)
    {
        return typeToString(introHelper.getAttributeType(attributeName));
    }

    /**
      Retrieves the method comment for the given attribute.
      The comment of the setter is used preferably to the getter comment.

      @param attribute
      @return The comment for the specified attribute
    */
    public String getAttributeComment(String attribute)
    {
        MethodDoc method = getMethodFor(this.doc, attribute);
        if (method == null) {
            return new String();
        }
        return method.commentText();
    }

    /**
      Searches the given class for the appropriate setter or getter for the given attribute.
      This method only returns the getter if no setter is available.
      If the given class provides no method declaration, the superclasses are
      searched recursively.

      @param attribute
      @return The MethodDoc for the given attribute or null if not found
    */
    private static MethodDoc getMethodFor(ClassDoc classDoc, String attribute)
    {
        if (classDoc == null) {
            return null;
        }
        MethodDoc result = null;
        MethodDoc[] methods = classDoc.methods();
        for (MethodDoc method : methods) {

            // we give priority to the documentation on the "setter" method of the attribute but if the documentation
            // is only on the "getter", use it we give priority to the documentation on the "setter" method of the
            // attribute but if the documentation is only on the "getter", use it
            if (method.name().equalsIgnoreCase("set" + attribute)) {
                return method;
            } else if (method.name().equalsIgnoreCase("get" + attribute)) {
                result = method;
            }
        }
        if (result == null) {
            return getMethodFor(classDoc.superclass(), attribute);
        }
        return result;
    }

    /**
      Searches the given class for the appropriate setter or getter for the given attribute. This method only returns
      the getter if no setter is available. If the given class provides no method declaration, the superclasses are
      searched recursively.

      @param nestedType
      @return The MethodDoc for the given attribute or null if not found
    */
    private static MethodDoc getMethodForType(ClassDoc classDoc, String nestedType)
    {
        notNull(classDoc, "classDoc");
        notNull(nestedType, "nestedType");

        String addName = "add" + toCapitalized(nestedType);
        String addConfiguredName = "addConfigured" + toCapitalized(nestedType);

        MethodDoc result = null;
        MethodDoc[] methods = classDoc.methods();
        for (MethodDoc method : methods) {
            if (method.name().equalsIgnoreCase("add")||method.name().equalsIgnoreCase("addConfigured")) {
                com.sun.javadoc.Parameter[] params = method.parameters();
                if (params.length == 1) {
                    // Ugly. I have the method, why can't Javadoc give me the comment directly?
                    if (nestedType.endsWith(params[0].type().typeName())) {
                        result = method;
                        break;
                    }
                }
            } else if (method.name().equals(addName) || method.name().equals(addConfiguredName)) {
                com.sun.javadoc.Parameter[] params = method.parameters();
                if (params.length == 1) {
                    return method;
                }
            }
        }
        if (result == null && classDoc.superclass() != null) {
            return getMethodForType(classDoc.superclass(), nestedType);
        }
        return result;
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
    private static String typeToString(Class<?> clazz)
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
