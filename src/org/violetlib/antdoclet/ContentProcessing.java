package org.violetlib.antdoclet;

import jdk.javadoc.doclet.Reporter;
import org.jetbrains.annotations.NotNull;
import com.sun.source.doctree.*;

import javax.lang.model.element.Element;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**

*/

public class ContentProcessing
{
    public static @NotNull ContentProcessing create(@NotNull LinkSupport linkSupport, @NotNull Reporter reporter)
    {
        return new ContentProcessing(linkSupport, reporter);
    }

    private final @NotNull LinkSupport linkSupport;
    private final @NotNull Reporter reporter;

    private ContentProcessing(@NotNull LinkSupport linkSupport, @NotNull Reporter reporter)
    {
        this.linkSupport = linkSupport;
        this.reporter = reporter;
    }

    public @NotNull String toRawText(@NotNull Element e, @NotNull DocTree content)
    {
        return toRawText(e, List.of(content));
    }

    public @NotNull String toRawText(@NotNull Element e, @NotNull List<? extends DocTree> content)
    {
        StringWriter sw = new StringWriter();
        ContentProcessor w = create(e, sw, true);
        w.write(content);
        w.flush();
        return sw.toString();
    }

    public @NotNull String toHTML(@NotNull Element e, @NotNull String content)
    {
        return toHTML(e, new TextNode(content));
    }

    public @NotNull String toHTML(@NotNull Element e, @NotNull DocTree content)
    {
        return toHTML(e, List.of(content));
    }

    public @NotNull String toHTML(@NotNull Element e, @NotNull List<? extends DocTree> content)
    {
        StringWriter sw = new StringWriter();
        ContentProcessor w = create(e, sw, false);
        w.write(content);
        w.flush();
        return sw.toString();
    }

    private @NotNull ContentProcessor create(@NotNull Element e, @NotNull Writer w, boolean isRaw)
    {
        return ContentProcessor.create(e, w, isRaw, linkSupport, reporter);
    }

    public static class TextNode
      implements TextTree
    {
        public final @NotNull String text;

        public TextNode(@NotNull String text)
        {
            this.text = text;
        }

        @Override
        public Kind getKind()
        {
            return Kind.TEXT;
        }

        @Override
        public <R,D> R accept(DocTreeVisitor<R,D> visitor, D data)
        {
            return visitor.visitText(this, data);
        }

        @Override
        public String getBody()
        {
            return text;
        }
    }
}
