
AntDoclet is a tool to automatically generate documentation from the source code of
[Ant](https://ant.apache.org) tasks and types.

AntDoclet uses [Velocity](https://velocity.apache.org/) template-based generation, to
simplify creating new formats or customizing the ones provided.  The provided
templates generate HTML and are located in the
`templates` directory.

AntDoclet is a [JavaDoc](https://docs.oracle.com/en/java/javase/21/javadoc) doclet.
It has been tested using JDK 21.
AntDoclet analyzes the source code directly; it does not require
class files for the Ant tasks and types.


Quick Start
-----------

The `example/example.build.xml` file illustrates running the doclet from
Ant. You must change some properties to fit your needs, such as the path to
your tasks source code.


Ant-specific JavaDoc tags
-------------------------

AntDoclet expects some ant-specific tags to build richer documentation:

* The JavaDoc comment of a class should have either the tag `@ant.task`
  (for Tasks) or tag `@ant.type` (for Types used from ant
  Tasks). Both accept three attributes: `category, name, ignored`. Examples:

        @ant.task name="copy" category="filesystem"
        @ant.type name="fileset" category="filesystem"
        @ant.task ignore="true"
 
  Categories may optionally be used to avoid long task menus.

  When `ignore` is true, the class will not be included in the
  documentation. This is useful for ignoring abstract classes or
  tasks/types that are not to be exposed in the docs.

* The documentation of task/type attributes is extracted from the corresponding
  setter methods. The preferred capitalization of the attribute name can be specified using
  the tag `@ant.prop`, for example:
  
        @ant.prop name="includeAntRuntime"

  Two additional tags may be used

        @ant.required 
        @ant.not-required

  to indicate that the attribute is (or not) required.

  An additional description can be added to the tag. Example:

        /**
         * Overwrite any existing destination file(s).
         *
         * If true force overwriting of destination file(s)
         * even if the destination file(s) are younger than
         * the corresponding source file.
         *
         * @ant.not-required Default is false.
         */
        public void setOverwrite(boolean overwrite) {
            this.forceOverwrite = overwrite;
        }

 * Global properties and referenced Ant elements used by a task can be documented by
 declaring a `public static final String` field whose value is the property name or the
 ID of the referenced element. JavaDoc comments for these fields must include
 the corresponding tag:
 
        @ant.prop name="document.title"
        @ant.ref name="default.classpath" type="Path"

The type of the property value or referenced element is specified using the type attribute.
The type attribute for a property defaults to `String`.

All JavaDoc comments must be valid HTML, otherwise, the template
output may be broken. Some suggestions:

* Use `{@code}` for variable and file names.
* For displaying source code in a nice box (like code examples) use
  `<pre> </pre>`. Avoid enclosing a trailing newline as it will create a blank line in the box.
* Remember to escape all necessary characters to make it valid HTML (`&lt;` instead of `<`, etc.)

<hr>

Based on the original [AntDoclet](https://github.com/dobladez/antdocle) by Fernando Dobladez.
