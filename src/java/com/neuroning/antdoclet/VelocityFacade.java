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

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
  A "facade" to the Velocity template engine

  @author Fernando Dobladez <dobladez@gmail.com>
*/

public class VelocityFacade
{
    private VelocityEngine velocity;
    private File outputDir;
    private final Context context;

    /**
      @param outputDir directory for output
    */

    public VelocityFacade(@NotNull File outputDir, @NotNull String templatesDir)
      throws Exception
    {
        initVelocityEngine(templatesDir);
        this.outputDir = outputDir;
        this.context = new VelocityContext();
    }

    /**
      Create and initialize a VelocityEngine
    */

    private void initVelocityEngine(@NotNull String templatesDir)
      throws Exception
    {
        velocity = new VelocityEngine();
        velocity.setProperty("resource.loader", "file, class");
        velocity.setProperty("file.resource.loader.path", templatesDir); // default "file" loader
        velocity.init();
    }

    public @NotNull File getOutputDir()
    {
        return outputDir;
    }

    public void setOutputDir(@NotNull File outdir)
    {
        this.outputDir = outdir;
    }

    /**
      Get a Writer to the specified file
    */

    protected @NotNull FileWriter getFileWriter(@NotNull String fileName)
      throws IOException
    {
        File file = new File(getOutputDir(), fileName);
        file.getParentFile().mkdirs();
        return new FileWriter(file);
    }

    /**
      Get the evaluation-context used by this generator
    */

    public @NotNull Context getContext()
    {
        return context;
    }

    /**
      Add something to the Generator's evaluation-context
    */

    public void setAttribute(@NotNull String key, @Nullable Object value)
    {
        context.put(key, value);
    }

    /**
      Evaluate a template.
      @param templateName the name of the template
      @param writer output destination
      @param context merge context
    */

    void merge(@NotNull String templateName, @NotNull Writer writer, @NotNull Context context)
    {
        try {
            Template template = this.velocity.getTemplate(templateName);
            template.merge(context, writer);
            writer.flush();

        } catch (MethodInvocationException e) {
            Throwable cause = e.getWrappedThrowable();

            if (cause == null) {
                cause = e;
            }
            throw new RuntimeException("Error invoking $" + e.getReferenceName() +
              "." + e.getMethodName() + "() in \"" +
              templateName + "\"",
              cause);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing \"" + templateName + "\"", e);
        }
    }

    /**
      Evaluate a Velocity template.
      @param templateName name of the template
      @param writer output destination
    */

    public void eval(@NotNull String templateName, @NotNull Writer writer)
      throws IOException
    {
        merge(templateName, writer, getContext());
    }

    /**
      Evaluate a Velocity template.
      @param templateName name of the template
      @param fileName name of output file
    */

    public void eval(@NotNull String templateName, @NotNull String fileName)
      throws IOException
    {
        FileWriter writer = getFileWriter(fileName);
        eval(templateName, writer);
        writer.close();
    }

    public @NotNull Object create(@NotNull String className)
      throws Exception
    {
        return Class.forName(className).getDeclaredConstructor().newInstance();
    }
}
