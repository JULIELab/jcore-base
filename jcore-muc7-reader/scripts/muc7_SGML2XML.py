# coding: utf-8

# - `<STORYID>`: values of attributes `cat` & `pri` need to be in quotation `"`
# - `<SLUG>`: value of attribute `fv` needs to be in quotation `"`
# - `<p>`: needs to be closed with `</p>`

import re
import sys


def close_paragraphs(line):
    global FIRST_P
    global IS_TEXT
    
    if not FIRST_P:
        FIRST_P = True
        return line
    else:
        return '</p>' + line


def quote_attr(line,r_find):
    compl_string = r_find.group(0)
    repl_string = r_find.group(1)
    sl = re.match('(.*?)=(.*?)( (.*?)=(.*?))?$', repl_string)
    if sl.group(3) == None:
        n_string = '{}="{}"'.format(
                       sl.group(1),
                       sl.group(2))
    else:
        n_string = '{}="{}" {}="{}"'.format(
                       sl.group(1),
                       sl.group(2),
                       sl.group(4),
                       sl.group(5))
    return re.sub(repl_string, n_string, compl_string)


if __name__ == "__main__":
    IS_TEXT = False
    FIRST_P = False

    p_reg = re.compile("<p>")
    o_txt_reg = re.compile("<TEXT>")
    c_txt_reg = re.compile("</TEXT>")
    story_reg = re.compile("<STORYID (.*?)>.*</STORYID>")
    slug_reg = re.compile("<SLUG (.*?)>.*</SLUG>")

    from_file = open(sys.argv[1], 'r')
    to_file = open(sys.argv[1]+".xml", 'w')

    to_file.write(
    """<?xml version="1.0" encoding="UTF-8"?>

<!DOCTYPE DOCS [
<!ENTITY LR "LR">
<!ENTITY UR "UR">
<!ENTITY MD "MD">
<!ENTITY QR "QR">
<!ENTITY QC "QC">
<!ENTITY HT "HT">
<!ENTITY AMP "&amp;amp;">
]>
<DOCS>""")
    for line in from_file.readlines():
        line = line.rstrip('\n')
        n_line = line

        # closes any open <p>
        p_find = p_reg.search(line)
        c_txt_find = c_txt_reg.search(line)
        if p_find or c_txt_find:
            n_line = close_paragraphs(line)

        # puts attribute values in quotations
        slug_find = slug_reg.search(line)
        if slug_find:
            n_line = quote_attr(line, slug_find)

        story_find = story_reg.search(line)
        if story_find:
            n_line = quote_attr(line, story_find)

        # resets <p> search
        c_txt_find = c_txt_reg.search(line)
        if c_txt_find:
            FIRST_P = False
        
        to_file.write(n_line+'\n')
    to_file.write("</DOCS>")
