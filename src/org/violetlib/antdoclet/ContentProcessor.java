package org.violetlib.antdoclet;

import com.sun.source.doctree.*;
import jdk.javadoc.doclet.Reporter;
import org.jetbrains.annotations.NotNull;

import javax.tools.Diagnostic;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.util.List;

/**

*/

public class ContentProcessor
{
    public static @NotNull ContentProcessor create(@NotNull Writer w,
                                                   boolean isRaw,
                                                   @NotNull LinkSupport linkSupport,
                                                   @NotNull Reporter reporter)
    {
        return new ContentProcessor(w, isRaw, linkSupport, reporter);
    }

    private final @NotNull PrintWriter w;
    private final boolean isRaw;
    private final @NotNull LinkSupport linkSupport;
    private final @NotNull Reporter reporter;

    public ContentProcessor(@NotNull Writer w,
                            boolean isRaw,
                            @NotNull LinkSupport linkSupport,
                            @NotNull Reporter reporter)
    {
        this.w = w instanceof PrintWriter pw ? pw : new PrintWriter(w);
        this.isRaw = isRaw;
        this.linkSupport = linkSupport;
        this.reporter = reporter;
    }

    public void write(@NotNull List<? extends DocTree> content)
    {
        for (DocTree tag : content) {
            DocTree.Kind kind = tag.getKind();
            if (kind == DocTree.Kind.TEXT) {
                TextTree tt = (TextTree) tag;
                String text = tt.getBody();
                debug("Text: " + text);
                w.write(text);
            } else if (kind == DocTree.Kind.CODE) {
                LiteralTree lt = (LiteralTree) tag;
                TextTree body = lt.getBody();
                String text = body.getBody();
                debug("Code: " + text);
                if (isRaw) {
                    w.write("{@code ");
                    w.write(text);
                    w.write("}");
                } else {
                    w.write("<code>");
                    w.write(text);
                    w.write("</code>");
                }
            } else if (tag instanceof LiteralTree) {
                LiteralTree lt = (LiteralTree) tag;
                TextTree body = lt.getBody();
                String text = body.getBody();
                debug("Literal text: " + text);
                if (isRaw) {
                    w.write("{@literal ");
                    w.write(text);
                    w.write("}");
                } else {
                    // TBD: need to escape HTML characters like <
                    w.write(text);
                }
            } else if (kind == DocTree.Kind.ENTITY) {
                EntityTree t = (EntityTree) tag;
                String name = t.getName().toString();
                debug("Character: " + name);
                w.write("&");
                w.write(name);
                w.write(";");
            } else if (kind == DocTree.Kind.START_ELEMENT) {
                StartElementTree t = (StartElementTree) tag;
                String name = t.getName().toString();
                debug("Start: " + name);
                w.write("<");
                w.write(name);
                write(t.getAttributes());
                if (t.isSelfClosing()) {
                    w.write("/");
                }
                w.write(">");
            } else if (kind == DocTree.Kind.ATTRIBUTE) {
                AttributeTree t = (AttributeTree) tag;
                String name = t.getName().toString();
                AttributeTree.ValueKind valueKind = t.getValueKind();
                debug("Attribute: " + name);
                writeAttribute(name, valueKind, t.getValue());
            } else if (kind == DocTree.Kind.END_ELEMENT) {
                EndElementTree t = (EndElementTree) tag;
                String name = t.getName().toString();
                debug("End: " + name);
                w.write("</");
                w.write(name);
                w.write(">");
            } else if (kind == DocTree.Kind.LINK) {
                LinkTree t = (LinkTree) tag;
                writeLink(t, false);
            } else if (kind == DocTree.Kind.LINK_PLAIN) {
                LinkTree t = (LinkTree) tag;
                writeLink(t, true);
            } else if (kind == DocTree.Kind.UNKNOWN_INLINE_TAG) {
                UnknownInlineTagTree t = (UnknownInlineTagTree) tag;
                String tagName = t.getTagName();
                debug("Inline tag: " + tagName);
                if (isRaw) {
                    w.write("@");
                    w.write(tagName);
                    w.write(" ");
                    write(t.getContent());
                } else {
                    // TBD
                    write(t.getContent());
                }
            } else if (kind == DocTree.Kind.ESCAPE) {
                EscapeTree t = (EscapeTree) tag;
                String body = t.getBody();
                debug("Escape: " + body);
                if (isRaw) {
                    w.write("@");
                    w.write(body);
                } else {
                    w.write(body);
                }
            } else {
                error("Unknown or unsupported doc tree element: " + kind);
            }
        }
    }

    protected void writeAttribute(@NotNull String name,
                                  @NotNull AttributeTree.ValueKind kind,
                                  @NotNull List<? extends DocTree> content)
    {
        w.write(" ");
        switch (kind) {
            case EMPTY:
                w.write(name);
                break;
            case UNQUOTED:
                w.write(name);
                w.write("=");
                write(content);
                break;
            case SINGLE:
                w.write(name);
                w.write("='");
                write(content);
                w.write("'");
                break;
            case DOUBLE:
                w.write(name);
                w.write("=\"");
                write(content);
                w.write("\"");
                break;
        }
    }

    private void writeLink(@NotNull LinkTree t, boolean isPlain)
    {
        String signature = t.getReference().getSignature();
        List<? extends DocTree> label = t.getLabel();
        if (isRaw) {
            w.write(isPlain ? "{@linkplain " : "{@link ");
            w.write(signature);
            if (!label.isEmpty()) {
                w.write(" ");
                write(t.getLabel());
            }
            w.write("}");
        } else {
            URI u = linkSupport.getLinkTarget(signature);
            if (u != null) {
                w.write("<a href=");
                w.write(u.toString());
                w.write(">");
                if (!isPlain) {
                    w.write("<code>");
                }
                if (!label.isEmpty()) {
                    write(label);
                } else {
                    w.write(signature);
                }
                if (!isPlain) {
                    w.write("</code>");
                }
                w.write("</a>");
            } else {
                error("Cannot create link to type: " + signature);
            }
        }
    }

    public void flush()
    {
        w.flush();
    }

    private void error(@NotNull String message)
    {
        reporter.print(Diagnostic.Kind.ERROR, message);
    }

    private void debug(@NotNull String message)
    {
        if (false) {
            reporter.print(Diagnostic.Kind.OTHER, message);
        }
    }
}
