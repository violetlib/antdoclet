
AntDoclet is a tool to automatically generate documentation from the source code of
[Ant](https://ant.apache.org) tasks and types.

AntDoclet uses [Velocity](https://velocity.apache.org/) template-based generation, to
simplify creating new formats or customizing the ones provided.  The provided
templates generate HTML and are located in the
`templates` directory.

AntDoclet is a [JavaDoc](https://docs.oracle.com/en/java/javase/21/javadoc) doclet.
It has been tested using JDK 21.
AntDoclet analyzes the source code directly; it does not require
class files for the Ant tasks and types, although class files for
compile-time dependencies are
required as the analysis is partly performed by the Java compiler.


Quick Start
-----------

The file `example/example.build.xml` illustrates running the doclet from
Ant. You must change some properties to fit your needs, such as the path to
your source code.


Ant-specific JavaDoc tags
-------------------------

AntDoclet expects some ant-specific tags to build richer documentation:

* The JavaDoc comment of a class that implements an Ant task or type
  should have either the tag `@ant.task`
  (for a task) or tag `@ant.type` (for a type).
  Both tags accept these attributes: `category, name, ignore`. Examples:

        @ant.task name="copy" category="filesystem"
        @ant.type name="fileset" category="filesystem"
        @ant.task ignore="true"
  The `name` attribute specifies the name by which the task or type is typically defined
  (most likely in an `antlib.xml` file provided with the class library).
  Categories may optionally be used to avoid long task menus when the library
  contains a large number of tasks and types.
  When `ignore` is true, the class will not be included in the
  task or type menus. (The task or type may still be referenced in the descriptions of
  other tasks or types.)

* The documentation of task/type attributes is extracted from the JavaDoc comment of the
  corresponding
  setter methods. The preferred capitalization of the attribute name can be specified using
  the tag `@ant.prop`, for example:
  
        @ant.prop name="includeAntRuntime"

  Two additional tags may be used

        @ant.required 
        @ant.optional

  to indicate that the attribute is required or optional. An additional description can
  be added, for example:

        /**
         * Overwrite any existing destination file(s).
         *
         * If true, force overwriting of destination file(s)
         * even if the destination file(s) are nwer than
         * the corresponding source file.
         *
         * @ant.optional Default is false.
         */
        public void setOverwrite(boolean overwrite) {
            this.forceOverwrite = overwrite;
        }

 * The documentation of nested elements is extracted from the corresponding `add`,
   `addConfigured`, `addXXX`, `addConfiguredXXX` or `createXXX` methods.
   The `@ant.type` tag can be used on these methods, with the `name` attribute
   specifying the preferred capitalization of a named nested element.

 * Global properties and referenced Ant elements used by a task or type can be documented by
   declaring a `public static final String` field whose value is the property name or the
   ID of the referenced element. (The name of the field does not matter.)
   JavaDoc comments for these fields must include
   the `@ant.prop` tag (for properties) or the `@ant.ref` tag (for references),
   as in these examples:
 
        /**
         * @ant.prop type="String"
         */
        public static final String DEFAULT_WIDTH_NAME = "default.width";

        /**
         * @ant.ref type="Path"
         */
        public static final String DEFAULT_PATH_ID = "default.path";

   The type of the property value or referenced element is specified using the `type` attribute.
   The `type` attribute for a property defaults to `String`.

All JavaDoc comments must be valid HTML, otherwise, the template
output may be broken. Some suggestions:

* Use `{@code}` for variable and file names.
* For displaying source code in a box (like code examples) use `{@snippet}`. This is a stripped down
  version of the standard doclet `@snippet` tag that supports no attributes or inline tags. All it does
  is remove excess indentation and escape HTML metacharacters as needed.

The syntax of `@snippet` is not obvious. Here is an example:

    {@snippet :
    if (x < 3 && y > 6) {
        System.err.println("Bad arguments");
    }
    }

Things to note:
 * The colon is required.
 * Content cannot be included in the`@snippet` line or the line with the
   closing `}`.
 * Braces in the content must be balanced.
 * The indentation that is removed is the indentation of the line containing the closing `}`.
 * The indentation to be stripped must be spaces, no tabs.

<hr>

Based on the original [AntDoclet](https://github.com/dobladez/antdocle) by Fernando Dobladez.
