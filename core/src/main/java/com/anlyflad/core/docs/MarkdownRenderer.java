package com.anlyflad.core.docs;
import java.util.ArrayList;
import java.util.List;
public final class MarkdownRenderer {
    private MarkdownRenderer() {
    }
    public static String render(String markdown) {
        if (markdown==null) {
            throw new IllegalArgumentException("markdown must not be null");
        }
        String[] lines=markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder html=new StringBuilder(markdown.length()*2);
        boolean inCode=false;
        boolean inList=false;
        boolean orderedList=false;
        boolean inTable=false;
        for (int index=0;index<lines.length;index++) {
            String line=lines[index];
            if (line.startsWith("```")) {
                if (inCode) {
                    html.append("</code></pre>\n");
                    inCode=false;
                } else {
                    closeList(html,inList,orderedList);
                    inList=false;
                    html.append("<pre><code>");
                    inCode=true;
                }
                continue;
            }
            if (inCode) {
                html.append(escape(line)).append('\n');
                continue;
            }
            String trimmed=line.trim();
            if (trimmed.isEmpty()) {
                closeList(html,inList,orderedList);
                inList=false;
                inTable=false;
                continue;
            }
            if (isHorizontalRule(trimmed)) {
                closeList(html,inList,orderedList);
                inList=false;
                inTable=false;
                html.append("<hr>\n");
                continue;
            }
            int heading=headingLevel(trimmed);
            if (heading>0) {
                closeList(html,inList,orderedList);
                inList=false;
                inTable=false;
                String text=trimmed.substring(heading).trim();
                html.append("<h").append(heading).append('>').append(inline(text)).append("</h").append(heading).append(">\n");
                continue;
            }
            if (trimmed.startsWith(">")) {
                closeList(html,inList,orderedList);
                inList=false;
                inTable=false;
                html.append("<blockquote>").append(inline(trimmed.substring(1).trim())).append("</blockquote>\n");
                continue;
            }
            if (isTableStart(lines,index)) {
                closeList(html,inList,orderedList);
                inList=false;
                inTable=true;
                html.append("<table>\n");
                appendTableRow(html,trimmed,true);
                index++;
                continue;
            }
            if (inTable) {
                if (!isTableRow(trimmed)) {
                    html.append("</table>\n");
                    inTable=false;
                } else {
                    appendTableRow(html,trimmed,false);
                    continue;
                }
            }
            if (trimmed.startsWith("- ")||trimmed.startsWith("* ")) {
                if (!inList||orderedList) {
                    closeList(html,inList,orderedList);
                    html.append("<ul>\n");
                    inList=true;
                    orderedList=false;
                }
                html.append("<li>").append(inline(trimmed.substring(2).trim())).append("</li>\n");
                continue;
            }
            if (isOrderedItem(trimmed)) {
                if (!inList||!orderedList) {
                    closeList(html,inList,orderedList);
                    html.append("<ol>\n");
                    inList=true;
                    orderedList=true;
                }
                int dot=trimmed.indexOf('.');
                html.append("<li>").append(inline(trimmed.substring(dot+1).trim())).append("</li>\n");
                continue;
            }
            closeList(html,inList,orderedList);
            inList=false;
            html.append("<p>").append(inline(trimmed)).append("</p>\n");
        }
        if (inCode) {
            html.append("</code></pre>\n");
        }
        if (inTable) {
            html.append("</table>\n");
        }
        closeList(html,inList,orderedList);
        return html.toString();
    }
    private static void closeList(StringBuilder html,boolean inList,boolean ordered) {
        if (inList) {
            html.append(ordered?"</ol>\n":"</ul>\n");
        }
    }
    private static boolean isHorizontalRule(String line) {
        if (line.length()<3) {
            return false;
        }
        for (int index=0;index<line.length();index++) {
            char current=line.charAt(index);
            if (current!='-'&&current!='*'&&current!='_') {
                return false;
            }
        }
        return true;
    }
    private static int headingLevel(String line) {
        int count=0;
        while (count<line.length()&&count<6&&line.charAt(count)=='#') {
            count++;
        }
        if (count==0||count>=line.length()||line.charAt(count)!=' ') {
            return 0;
        }
        return count;
    }
    private static boolean isOrderedItem(String line) {
        int index=0;
        while (index<line.length()&&Character.isDigit(line.charAt(index))) {
            index++;
        }
        return index>0&&index+1<line.length()&&line.charAt(index)=='.'&&line.charAt(index+1)==' ';
    }
    private static boolean isTableRow(String line) {
        return line.indexOf('|')>=0;
    }
    private static boolean isTableStart(String[] lines,int index) {
        if (!isTableRow(lines[index].trim())) {
            return false;
        }
        if (index+1>=lines.length||!isTableRow(lines[index+1].trim())) {
            return false;
        }
        String[] cells=splitRow(lines[index+1].trim());
        for (int cell=0;cell<cells.length;cell++) {
            if (!cells[cell].matches(":?-{2,}:?")) {
                return false;
            }
        }
        return cells.length>0;
    }
    private static void appendTableRow(StringBuilder html,String line,boolean header) {
        String[] cells=splitRow(line);
        html.append(header?"<thead><tr>":"<tr>");
        for (int cell=0;cell<cells.length;cell++) {
            html.append(header?"<th>":"<td>").append(inline(cells[cell])).append(header?"</th>":"</td>");
        }
        html.append(header?"</tr></thead>\n":"</tr>\n");
    }
    private static String[] splitRow(String line) {
        String value=line;
        if (value.startsWith("|")) {
            value=value.substring(1);
        }
        if (value.endsWith("|")) {
            value=value.substring(0,value.length()-1);
        }
        String[] raw=value.split("\\|",-1);
        List<String> cells=new ArrayList<String>(raw.length);
        for (int index=0;index<raw.length;index++) {
            cells.add(raw[index].trim());
        }
        return cells.toArray(new String[cells.size()]);
    }
    private static String inline(String text) {
        StringBuilder output=new StringBuilder(text.length()+16);
        int index=0;
        while (index<text.length()) {
            char current=text.charAt(index);
            if (current=='`') {
                int end=text.indexOf('`',index+1);
                if (end>index) {
                    output.append("<code>").append(escape(text.substring(index+1,end))).append("</code>");
                    index=end+1;
                    continue;
                }
            }
            if (text.startsWith("**",index)) {
                int end=text.indexOf("**",index+2);
                if (end>index) {
                    output.append("<strong>").append(escape(text.substring(index+2,end))).append("</strong>");
                    index=end+2;
                    continue;
                }
            }
            if (text.startsWith("![",index)) {
                int close=text.indexOf(']',index+2);
                int open=close<0?-1:text.indexOf('(',close);
                int end=open<0?-1:text.indexOf(')',open);
                if (close>index&&open==close+1&&end>open) {
                    output.append("<img src=\"").append(sanitizeUrl(text.substring(open+1,end))).append("\" alt=\"").append(escape(text.substring(index+2,close))).append("\">");
                    index=end+1;
                    continue;
                }
            }
            if (current=='[') {
                int close=text.indexOf(']',index+1);
                int open=close<0?-1:text.indexOf('(',close);
                int end=open<0?-1:text.indexOf(')',open);
                if (close>index&&open==close+1&&end>open) {
                    String label=inline(text.substring(index+1,close));
                    output.append("<a href=\"").append(sanitizeUrl(text.substring(open+1,end))).append("\">").append(label).append("</a>");
                    index=end+1;
                    continue;
                }
            }
            if (current=='*'&&index+1<text.length()&&text.charAt(index+1)!=' ') {
                int end=text.indexOf('*',index+1);
                if (end>index+1) {
                    output.append("<em>").append(escape(text.substring(index+1,end))).append("</em>");
                    index=end+1;
                    continue;
                }
            }
            output.append(escapeChar(current));
            index++;
        }
        return output.toString();
    }
    private static String sanitizeUrl(String url) {
        String trimmed=url.trim();
        String lower=trimmed.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("http://")||lower.startsWith("https://")||lower.startsWith("mailto:")||lower.startsWith("#")||lower.startsWith("./")||lower.startsWith("../")||lower.indexOf(':')<0) {
            return escape(trimmed);
        }
        return "#";
    }
    private static String escape(String value) {
        StringBuilder output=new StringBuilder(value.length()+16);
        for (int index=0;index<value.length();index++) {
            output.append(escapeChar(value.charAt(index)));
        }
        return output.toString();
    }
    private static String escapeChar(char value) {
        if (value=='&') {
            return "&amp;";
        }
        if (value=='<') {
            return "&lt;";
        }
        if (value=='>') {
            return "&gt;";
        }
        if (value=='"') {
            return "&quot;";
        }
        return Character.toString(value);
    }
}
