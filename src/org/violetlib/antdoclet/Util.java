package org.violetlib.antdoclet;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.*;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**

*/

public class Util
{
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
