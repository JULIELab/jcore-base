
import os
import sys

import xml.etree.ElementTree as ET

new_version = sys.argv[1]

print("Trying to update to version", new_version)

path = sys.argv[2]

print('Searching in', path)


def get_xmlns(tag_name):
    parts = tag_name.split('}')
    if len(parts) > 1:
        return parts[0][1:]
    else:
        return ''


def uima_file_type(tag_name):
    uima_files = {
        "{http://uima.apache.org/resourceSpecifier}collectionReaderDescription": 1,
        "{http://uima.apache.org/resourceSpecifier}analysisEngineDescription": 2,
        "{http://uima.apache.org/resourceSpecifier}casConsumerDescription": 3,
        "{http://uima.apache.org/resourceSpecifier}typeSystemDescription": 4,
        "{http://uima.apache.org/resourceSpecifier}typePriorities": 5,
    }
    return uima_files.get(tag_name, None)

def get_uima_version_parent(root):
    type = uima_file_type(root.tag)
    if type == 4 or type == 5:
        return root
    else:
        element_names = {
            1: "{http://uima.apache.org/resourceSpecifier}processingResourceMetaData",
            2: "{http://uima.apache.org/resourceSpecifier}analysisEngineMetaData",
            3: "{http://uima.apache.org/resourceSpecifier}processingResourceMetaData",
        }
        name = element_names.get(type)
        return root.find(name)


def process_directory(dirname):
    for entry in os.scandir(dirname):
        if entry.is_dir():
            process_directory(entry.path)
        elif os.path.splitext(entry.name)[1] == ".xml":
            # print(entry.path)
            try:
                tree = ET.parse(entry.path)
                root = tree.getroot()
                # print(root.tag)
                xmlns = get_xmlns(root.tag)
                # print(xmlns)
                if xmlns:
                    ET.register_namespace("", xmlns)
                else:
                    ET.register_namespace("", "")

                modified = False

                version_tag_name = "{http://uima.apache.org/resourceSpecifier}version"
                desc_tag_name = "{http://uima.apache.org/resourceSpecifier}description"

                if uima_file_type(root.tag):
                    parent = get_uima_version_parent(root)
                    if parent:
                        elem = parent.find(version_tag_name)
                        descElem = parent.find(desc_tag_name)
                        if elem is not None:
                            old_version = elem.text
                            elem.text = new_version
                            elem.tail = '\n        '
                            print("changed version", old_version, "to", new_version, "in", entry.path)
                            versionIndex = list(parent).index(elem)
                            if versionIndex > 2:
                                parent.remove(elem)
                                if descElem is not None:
                                    descIndex = list(parent).index(descElem)
                                    parent.insert(descIndex+1,elem)
                                else:
                                    parent.insert(1,elem)
                        else:
                            elem = ET.Element(version_tag_name)
                            # This adds a newline after the new version element
                            # and the indentation for the next line
                            elem.tail = '\n        '
                            elem.text = new_version
                            descIndex = list(parent).index(descElem)
                            if descIndex > 0:
                                parent.insert(descIndex+1,elem)
                            else:
                                parent.insert(1,elem)
                            print("added version", new_version, "in", entry.path)
                        modified = True
                    else:
                        print("Error: don't understand structure of", entry.path)

                if modified:
                    tree.write(entry.path, xml_declaration=True, encoding="UTF-8")

            except Exception as e:
                print(e)
                print("occured in: ", entry.path)


process_directory(path)

