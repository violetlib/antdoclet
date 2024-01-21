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

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.SourceVersion;
import java.io.File;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
  AntDoclet Main class

  This doclet generates documentation and other deliverables from the source code of ant Tasks and Types.

  It uses template-based generation to make it easy to create new deliverables or modify the ones provided.

  @author Fernando Dobladez <dobladez@gmail.com>
*/

public class AntDoclet
  implements Doclet
{
    private @Nullable Reporter reporter;

    private @Nullable String docTitle = "Ant Tasks";
    private @Nullable String[] templates;
    private @NotNull String templatesDir = ".";
    private @NotNull String[] outputDirs = new String[] { "." };

    public AntDoclet()
    {
    }

    /**
      Processes the JavaDoc documentation.
      @return True if processing was successful.
    */

    private boolean start(@NotNull DocletEnvironment docletEnvironment)
    {
        // Init Velocity-template Generator
        VelocityFacade velocity;
        try {
            velocity = new VelocityFacade(new File("."), templatesDir);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        assert reporter != null;

        Environment env = Environment.create(docletEnvironment, reporter);

        // Set global parameters to the templates
        velocity.setAttribute("velocity", velocity);
        velocity.setAttribute("title", docTitle);
        velocity.setAttribute("antroot", env.getRoot());

        if (templates != null) {
            for (int i = 0; i < templates.length; i++) {
                try {
                    if (outputDirs.length > i) {
                        velocity.setOutputDir(new File(outputDirs[i]));
                    }
                    velocity.eval(templates[i], new OutputStreamWriter(System.out));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    private abstract class MyOption
      implements Option
    {
        private final String name;
        private final int argCount;

        protected MyOption(String name, int argCount)
        {
            this.name = name;
            this.argCount = argCount;
        }

        @Override
        public String getDescription()
        {
            return name;
        }

        @Override
        public Kind getKind()
        {
            return Kind.STANDARD;
        }

        @Override
        public List<String> getNames()
        {
            return List.of(name);
        }

        @Override
        public String getParameters()
        {
            return "";
        }

        @Override
        public String toString()
        {
            return name;
        }

        @Override
        public int getArgumentCount()
        {
            return argCount;
        }
    }

    private @NotNull Option createOutputOption()
    {
        return new MyOption("-output", 1) {
            @Override
            public boolean process(String opt, List<String> args) {
                outputDirs = args.get(0).split(",");
                return true;
            }
        };
    }

    private @NotNull Option createOutputDirectoryOption()
    {
        return new MyOption("-d", 1) {
            @Override
            public boolean process(String opt, List<String> args) {
                outputDirs = args.get(0).split(",");
                return true;
            }
        };
    }

    private @NotNull Option createDocTitleOption()
    {
        return new MyOption("-doctitle", 1) {
            @Override
            public boolean process(String opt, List<String> args) {
                docTitle = args.get(0);
                return true;
            }
        };
    }

    private @NotNull Option createTemplatesOption()
    {
        return new MyOption("-templates", 1) {
            @Override
            public boolean process(String opt, List<String> args) {
                templates = args.get(0).split(","); // comma-separated
                return true;
            }
        };
    }

    private @NotNull Option createTemplatesDirOption()
    {
        return new MyOption("-templatesdir", 1) {
            @Override
            public boolean process(String opt, List<String> args) {
                templatesDir = args.get(0); // comma-separated filenames
                return true;
            }
        };
    }

    @Override
    public void init(Locale locale, Reporter reporter)
    {
        this.reporter = reporter;
    }

    @Override
    public @NotNull String getName()
    {
        return "AntDoclet";
    }

    @Override
    public @NotNull Set<? extends Option> getSupportedOptions()
    {
        Set<Option> options = new HashSet<>();
        options.add(createOutputOption());
        //options.add(createOutputDirectoryOption());
        options.add(createDocTitleOption());
        options.add(createTemplatesOption());
        options.add(createTemplatesDirOption());
        return options;
    }

    @Override
    public @NotNull SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.RELEASE_21;
    }

    @Override
    public boolean run(@NotNull DocletEnvironment env)
    {
        return start(env);
    }
}
