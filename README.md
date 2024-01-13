
AntDoclet is a tool to automatically generate documentation from the source code of
[Ant](https://ant.apache.org) tasks and types.

AntDoclet uses template-based generation, using
[Velocity](https://velocity.apache.org/). This makes it simple to
create new deliverables or modify the ones provided.  The example
templates generate HTML and are located under the
`templates/` directory.

AntDoclet is a [Javadoc](https://docs.oracle.com/en/java/javase/21/javadoc) doclet.
This release of AntDoclet has been upgraded to use the current javadoc
doclet API. It has been tested using JDK 21.

This release analyzes the source code directly. It does not require
class files for the Ant tasks and types.


Quick Start
-----------

The `example.build.xml` file is an example of how to run the doclet from
Ant. You must change some properties to fit your needs, such as the path to
your tasks source code.


Ant-specific Javadoc tags
-------------------------

AntDoclet expects some ant-specific tags to build richer documentation:

* The javadoc comment of a class should have either the tag `@ant.task`
  (for Tasks) or tag `@ant.type` (for Types used from ant
  Tasks). Both accept three "attributes": `category, name, ignored`. Examples:

        @ant.task name="copy" category="filesystem"
        @ant.type name="fileset" category="filesystem"
        @ant.task ignore="true"
 
  When `ignore` is true, the class will not be included in the
  documentation. This is useful for ignoring abstract classes or
  tasks/types that are not to be exposed in the docs.

* The tasks/types properties documentation is extracted from the
  properties setter/getter methods' comments (the setter comment has
  precedence). Two additional tags are accepted here:

        @ant.required 
        @ant.not-required

  used to indicate that the property is (or not) required.

  Additional comment can be added to the tag. Example:

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



The javadoc comments must be valid HTML, otherwise, the template
output may be broken. Some suggestions:

* Use `{@code}` for variable and file names.
* For displaying source code in a nice box (like code examples) use
  `<pre> </pre>`.
* Remember to escape all necessary characters to make it valid HTML (`&lt;` instead of `<`, etc.)

<hr>

Based on the original AntDoclet by Fernando Dobladez (<http://code54.com>)
