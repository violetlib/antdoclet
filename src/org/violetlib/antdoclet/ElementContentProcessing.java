package org.violetlib.antdoclet;

import com.sun.source.doctree.DocTree;
import jdk.javadoc.doclet.Reporter;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**

*/

public class ElementContentProcessing
{
    public static @NotNull ElementContentProcessing create(@NotNull Element e,
                                                           @NotNull LinkSupport linkSupport,
                                                           @NotNull Reporter reporter)
    {
        return new ElementContentProcessing(e, linkSupport, reporter);
    }

    private final @NotNull Element e;
    private final @NotNull LinkSupport linkSupport;
    private final @NotNull Reporter reporter;

    private ElementContentProcessing(@NotNull Element e, @NotNull LinkSupport linkSupport, @NotNull Reporter reporter)
    {
        this.e = e;
        this.linkSupport = linkSupport;
        this.reporter = reporter;
    }

    public @NotNull Element getElement()
    {
        return e;
    }

    public @NotNull String toRawText(@NotNull DocTree content)
    {
        return toRawText(List.of(content));
    }

    public @NotNull String toRawText(@NotNull List<? extends DocTree> content)
    {
        StringWriter sw = new StringWriter();
        ContentProcessor w = create(sw, true);
        w.write(content);
        w.flush();
        return sw.toString();
    }

    public @NotNull String toHTML(@NotNull String content)
    {
        return toHTML(TextNode.create(content));
    }

    public @NotNull String toHTML(@NotNull DocTree content)
    {
        return toHTML(List.of(content));
    }

    public @NotNull String toHTML(@NotNull List<? extends DocTree> content)
    {
        StringWriter sw = new StringWriter();
        ContentProcessor w = create(sw, false);
        w.write(content);
        w.flush();
        return sw.toString();
    }

    private @NotNull ContentProcessor create(@NotNull Writer w, boolean isRaw)
    {
        return ContentProcessor.create(e, w, isRaw, linkSupport, reporter);
    }
}
