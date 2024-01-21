package org.violetlib.antdoclet;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Set;

/**

*/

public class Util
{
    public static @Nullable String getTopLevelSimpleName(@NotNull Element e)
    {
        TypeElement te = getTopLevelClass(e);
        if (te != null) {
            return te.getSimpleName().toString();
        }
        return null;
    }

    public static @Nullable String getTopLevelQualifiedName(@NotNull Element e)
    {
        TypeElement te = getTopLevelClass(e);
        if (te != null) {
            return te.getQualifiedName().toString();
        }
        return null;
    }

    public static @Nullable TypeElement getTopLevelClass(@NotNull Element e)
    {
        for (;;) {
            Element parent = e.getEnclosingElement();
            if (parent == null || parent.getKind() == ElementKind.PACKAGE || parent.getKind() == ElementKind.MODULE) {
                return e instanceof TypeElement te ? te : null;
            }
            e = parent;
        }
    }

    public static @Nullable ModuleElement getModule(@NotNull Element e)
    {
        for (;;) {
            Element parent = e.getEnclosingElement();
            if (parent == null) {
                return null;
            }
            if (parent.getKind() == ElementKind.MODULE) {
                return parent instanceof ModuleElement m ? m : null;
            }
            e = parent;
        }
    }

    public static void show(@NotNull DocCommentInfo dc)
    {
        Element e = dc.getOwner();
        Set<String> tagNames = dc.getTagNames();

        System.out.println("-----------------------------------------------");
        String ts = tagNames.isEmpty() ? " [no tags]" : "";
        System.out.println("DocComment for element " + e.getSimpleName() + ts);
        for (String tagName : tagNames) {
            TagInfo tag = dc.getTag(tagName);
            assert tag != null;
            showIndented("  ", tag);
        }
        if (dc instanceof AugmentedDocCommentInfo adc) {
            showIndented("  description: ", adc.getHtmlDescription());
            showIndented("  short: ", adc.getHtmlShortDescription());
            showIndented("  medium: ", adc.getHtmlMediumDescription());
        }
    }

    private static void showIndented(@NotNull String indent, @NotNull TagInfo tagInfo)
    {
        String tagName = tagInfo.getName();
        List<? extends DocTree> content = tagInfo.getContent();
        Set<String> attributeNames = tagInfo.getAttributeNames();

        String ts = attributeNames.isEmpty() ? " [no attributes]" : "";
        if (content.isEmpty()) {
            ts = ts + " [no content]";
        }
        showIndented(indent, "Tag " + tagName + ts);
        for (String attributeName : attributeNames) {
            TagAttributeInfo at = tagInfo.getAttribute(attributeName);
            assert at != null;
            showIndented(indent + "  ", at);
        }
        if (!content.isEmpty() && tagInfo instanceof AugmentedTagInfo aa) {
            showIndented(indent + "  ", String.format("tag content [HTML]: \"%s\"", aa.getHtmlContent()));
        }
    }

    private static void showIndented(@NotNull String indent, @NotNull TagAttributeInfo at)
    {
        String value = at.getValue();
        showIndented(indent, String.format("%s = \"%s\"", at.getName(), value));
        if (at instanceof AugmentedTagAttributeInfo aa) {
            String htmlValue = aa.getHtmlValue();
            if (!htmlValue.equals(value)) {
                showIndented(indent, String.format("value [HTML] = \"%s\"", htmlValue));
            }
        }
    }

    private static void showIndented(@NotNull String indent, @NotNull String s)
    {
        System.out.println(indent + s);
    }

    public static void show(@NotNull DocTree tree)
    {
        showIndented("  ", tree);
    }

    private static void showIndented(@NotNull String indent, @NotNull List<? extends DocTree> trees)
    {
        for (DocTree tree : trees) {
            showIndented(indent, tree);
        }
    }

    private static void showIndented(@NotNull String indent, @NotNull DocTree tree)
    {
        System.out.print(indent);
        DocTree.Kind kind = tree.getKind();
        System.out.print(kind);

        if (kind == DocTree.Kind.DOC_COMMENT) {
            DocCommentTree d = (DocCommentTree) tree;
            List<? extends DocTree> body = d.getFullBody();
            List<? extends DocTree> tags = d.getBlockTags();
            showIndented(indent + "[body]", body);
            showIndented(indent + "[tag]", tags);
        } else if (kind == DocTree.Kind.UNKNOWN_BLOCK_TAG) {
            UnknownBlockTagTree t = (UnknownBlockTagTree) tree;
            String tagName = t.getTagName();
            System.out.println(" " + tagName);
            List<? extends DocTree> content = t.getContent();
            showIndented(indent + "[content]", content);
        } else if (kind == DocTree.Kind.TEXT) {
            TextTree t = (TextTree) tree;
            System.out.println(t.getBody());
        }
        System.out.println();
    }
}
