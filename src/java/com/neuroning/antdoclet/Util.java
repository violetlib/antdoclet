/**
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

import com.sun.javadoc.Doc;
import com.sun.javadoc.Tag;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
  Some utility methods. Mostly for String-handling

  @author Fernando Dobladez  <dobladez@gmail.com>
*/
@SuppressWarnings("removal")
public class Util
{
    public static String getProcessedText(Doc doc)
    {
        if (doc == null) {
            return null;
        }
        Tag[] tags = doc.inlineTags();
        return processTaggedText(tags);
    }

    public static String getProcessedText(Tag t)
    {
        if (t == null) {
            return null;
        }
        Tag[] tags = t.inlineTags();
        return processTaggedText(tags);
    }

    public static String processTaggedText(Tag[] tags)
    {
        StringBuilder sb = new StringBuilder();
        for (Tag tag : tags) {
            String kind = tag.kind();
            if (kind.equals("Text")) {
                sb.append(tag.text());
            } else {
                sb.append("<code>");
                sb.append(tag.text());
                sb.append("</code>");
            }
        }
        return sb.toString();
    }

    public static String getProcessedText(Doc doc, String tagname)
    {
        if (doc == null) {
            return null;
        }
        Tag[] tags = doc.tags(tagname);
        if (tags.length == 0) {
            return null;
        }
        Tag tag = tags[0];
        String text = tag.text();
        if (text == null) {
            return "-";
        }
        return getProcessedText(tag);
    }

    /**
      Returns the value of the first javadoc tag with the given name
    */
    static String tagValue(Doc doc, String tagname)
    {
        if(doc != null && doc.tags(tagname) != null && doc.tags(tagname).length > 0) {
            return (doc.tags(tagname)[0].text() == null ? "-" : doc.tags(tagname)[0].text());
        } else {
            return null;
        }
    }

    /**
      Returns the value of the value of the given tag's "attribute"
      Example: @mytag attribute1="value1" attribute2="value2"
    */
    static String tagAttributeValue(Doc doc, String tag, String attr)
    {
        String tagValue = tagValue(doc, tag);

        if (tagValue == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("(\\w+) *= *\"?([^\\s\",]+)\"?");
        Matcher matcher = pattern.matcher(tagValue);

        String attrValue = null;
        while (matcher.find()) {
            String key = matcher.group(1);
            if (attr.equalsIgnoreCase(key)) {
                attrValue = matcher.group(2);
            }
        }

        return attrValue;
    }

    /**
      Capitalize the give string. "myWord" -> "MyWord"
    */
    static String capitalize(String str)
    {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return str.substring(0,1).toUpperCase() + str.substring(1);
    }

    /**
      @param str
      @return true if str is either null or ""
    */
    static boolean empty(String str)
    {
        return str == null || str.isEmpty();
    }
}
