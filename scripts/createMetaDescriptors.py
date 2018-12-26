#!/usr/bin/python
# -*- coding: utf-8 -*-
"""
This script created the component.meta files for a given UIMA
component root directory. Currently, the directory must obey
JCoRe conventions for this script to run successfully.

Parameters:
	-c: Create a new meta descriptor
	-i: Install the meta descriptor into a repository at ~/.jcore-pipeline-builder
		-r: The name of the repository
		-v: The version of the repository
		-u: If the repository does not yet exist: If is updateable or not
"""
import os
import sys
from os.path import expanduser
import json
import fnmatch
import xml.etree.ElementTree as ET
from xml.etree.ElementTree import ParseError
from collections import Counter

# For testing we define in and out names so we can create new versions and compare
META_DESC_IN_NAME = "component.meta"
META_DESC_OUT_NAME = "component.meta"

def getNodeText(nodelist):
	"""
	Meant to be used with nodelists that have zero or one elements.
	Returns the text content of the first element, if it exists.
	"""
	if len(nodelist) > 0:
		return nodelist[0].text
	return ""

def getArtifactInfo(pomFile):
		# POMs have a default namespace. We define it here for
		# the "d"(efault) prefix (the name is arbitrary) and
		# use it to provide the namespaces for XPath expressions
		ns = {"d":"http://maven.apache.org/POM/4.0.0"}
		root = ET.parse(pomFile)
		nameNodes = root.findall("./d:name", ns)
		descriptionNodes   = root.findall("./d:description", ns)
		artifactIdNodes    = root.findall("./d:artifactId", ns)
		groupIdNodes       = root.findall("./d:groupId", ns)
		versionNodes       = root.findall("./d:version", ns)
		parentVersionNodes = root.findall("./d:parent/d:version", ns)
		parentGroupIdNodes = root.findall("./d:parent/d:groupId", ns)

		name = ""
		description = ""
		artifactId = ""
		name = getNodeText(nameNodes)
		description = getNodeText(descriptionNodes)
		artifactId = getNodeText(artifactIdNodes)
		category = None
		if (artifactId.endswith("reader")):
			category = "reader"
		if (artifactId.endswith("ae")):
			category = "ae"
		if (artifactId.endswith("multiplier")):
			category = "multiplier"
		if (artifactId.endswith("consumer")):
			category = "consumer"
		if (artifactId.endswith("writer")):
			category = "consumer"

		artifact = {}
		artifact["artifactId"]  = artifactId
		artifact["groupId"]     = getNodeText(groupIdNodes)
		artifact["version"]     = getNodeText(versionNodes)
		if len(groupIdNodes) == 0:
			artifact["groupId"] = getNodeText(parentGroupIdNodes)
		if len(versionNodes) == 0:
			artifact["version"] = getNodeText(parentVersionNodes)

		
		return artifact, name, category, description

def getDescriptors(projectpath):
	"""
	This method returns all XML files that
	1. Are located in the JCoRe-conventional descriptor directory
	2. Look like a UIMA component descriptor on a quick glance
	"""
	ns = {"d":"http://uima.apache.org/resourceSpecifier"}
	descriptors = []
	for root, dirnames, filenames in os.walk(projectpath + os.path.sep + os.path.sep.join(["src", "main", "resources", "de", "julielab", "jcore"])):
		if (root.endswith("desc")):
			for filename in fnmatch.filter(filenames, '*.xml'):
				tree = None
				try:
					tree = ET.parse(root + os.path.sep + filename)
				except ParseError as e:
					print ("Could not parse file {}: {}".format(root + os.path.sep + filename, e))
				descriptorRoot = tree.getroot()
				outputsNewCASes = False
				outputsCasesNodes = descriptorRoot.findall(".//d:outputsNewCASes", ns)
				if len(outputsCasesNodes) > 0 and outputsCasesNodes[0].text.lower() == "true":
					outputsNewCASes = True
				category = None
				if descriptorRoot.tag.endswith("collectionReaderDescription"):
					category = "reader"
				if descriptorRoot.tag.endswith("analysisEngineDescription"):
					category = "ae"
					if outputsNewCASes:
						category = "multiplier"
					if "consumer" in filename.lower() or "writer" in filename.lower():
						category = "consumer"
				if descriptorRoot.tag.endswith("casConsumerDescription"):
					category = "consumer"
				if category != None:
					# From the complete file name, exclude the system dependent part. That is, make the path relative to the
					# project directory's src/main/resources directory.
					location = os.path.join(root, filename)[len(projectpath+os.path.sep+os.path.sep.join(["src", "main", "resources"]))+1:]
					# And then make it to be a lookup by name: Use a dot as the path separator and remove the file name extension
					location = location.replace(os.path.sep, ".")
					# Remove '.xml'
					location = location[:-4]
					descriptors.append({"location":location,"category":category})
	return descriptors

def mergeWithOldMeta(projectPath, description):
	"""
	Reads potentially existing meta descriptor information
	and extracts information from it that can't be automatically
	derived from the POM and the descriptors.
	This is currently the group in which the component
	is manually inserted, the exposable attribute and the base
	project (only for jcore-projects).
	"""
	metaDescFileName = projectPath + os.path.sep + META_DESC_IN_NAME
	group = "general"
	exposable = description["descriptors"] != None and len(description["descriptors"]) > 0;
	baseProject = None
	if os.path.exists(metaDescFileName):
		with open(metaDescFileName, 'r') as metaDescFile:
			oldDescription = json.load(metaDescFile)
			group = oldDescription["group"]
			exposable = oldDescription["exposable"]
			if "base-project" in oldDescription:
				baseProject = oldDescription["base-project"]
	description["group"] = group
	description["exposable"] = exposable
	if baseProject != None:
		description["base-project"] = baseProject


if (__name__ == "__main__"):
	pPath = None
	validParameters = ["-c", "-i", "-r", "-v", "-u"]
	booleanParameters = ["-i", "-c"]
	cliParams = {}
	accountedParams = 0
	for i in range(len(sys.argv)):
		param = sys.argv[i]
		if param.startswith("-"):
			if param not in booleanParameters and (i+1 >= len(sys.argv) or sys.argv[i+1].startswith("-")):
				print("Missing value for parameter " + param)
				sys.exit(1)
			if param not in validParameters:
				print("Unknown parameter: " + param)
				sys.exit(2)
			if param in booleanParameters:
				cliParams[param] = True
				accountedParams = i
			else:
				cliParams[param] = sys.argv[i+1]
				accountedParams = i+1
			
	if accountedParams == len(sys.argv)-1:
		for line in sys.stdin:
			if pPath == None:
				pPath = line
	else:
		if len(sys.argv) > 1:
			pPath = sys.argv[-1]

	if pPath != None:
		if "-c" in cliParams.keys():
			print ("Creating or updating {} files in directory {}".format(META_DESC_OUT_NAME, pPath))
			numCreated = 0
			pomFile = pPath + os.path.sep + "pom.xml"

			if os.path.exists(pomFile):
				artifact, name, category, description = getArtifactInfo(pomFile)
				descriptors = getDescriptors(pPath)
				descriptorCategories = [d["category"] for d in descriptors]
				if category != None or len(descriptors) > 0:
					description = {
					 "name":name,
					 "maven-artifact":artifact,
					 "description": description,
					 "categories":[category]
					}
					# Ultimately, the descriptors determine in which categories the component
					# actually belongs.
					if len(descriptorCategories) > 0:
						categories = set()
						if category != None:
							categories.add(category)
						categories.update(descriptorCategories)
						description["categories"] = [c for c in categories]
					description["descriptors"] = descriptors
					mergeWithOldMeta(pPath, description)
					jsonDesc = json.dumps(description, sort_keys=True, indent=4, separators=(",", ": ")) + os.linesep
					with open(pPath + os.path.sep + META_DESC_OUT_NAME, 'w') as metaDescFile:
						metaDescFile.write(jsonDesc)
					numCreated = numCreated + 1
			print ("Created or updated {} {} files in {}.".format(numCreated, META_DESC_OUT_NAME, pPath))
		if "-i" in cliParams.keys():
			if "-r" not in cliParams.keys():
				print("You need to specify the repository name to install the component into (-r parameter)")
				sys.exit(3)
			if "-v" not in cliParams.keys():
				print("You need to specify the module version to install the component into (-v parameter)")
				sys.exit(4)
			repository = {}
			repository["name"]     = cliParams["-r"]
			repository["version"]  = cliParams["-v"]
			jcoreCacheDir        = expanduser("~") + os.path.sep + ".jcore-pipeline-builder"
			moduleDir            = jcoreCacheDir + os.path.sep + repository["name"] + os.path.sep + repository["version"]
			repositoriesListPath = jcoreCacheDir + os.path.sep + "repositories.json"
			componentlistPath    = moduleDir + os.path.sep + "componentlist.json"
			print("Installing meta description into " + componentlistPath + ". Note that the Maven artifact itself needs to be installed into your (local) repository as well.")
			if not os.path.isdir(moduleDir):
				os.makedirs(moduleDir)
			repositories    = None
			metaDescription = None
			componentlist   = None
			# Read existing repositories list
			if os.path.isfile(repositoriesListPath):
				with open(repositoriesListPath, "r") as repositoriesFile:
					repositories = json.load(repositoriesFile)
			else:
				repositories = []
			# Read the meta description to install
			metaDescriptorPath = None
			if os.path.isdir(pPath):
				metaDescriptorPath = pPath + os.path.sep + META_DESC_IN_NAME
				if not os.path.isfile(metaDescriptorPath):
					print("Could not find meta descriptor file " + metaDescriptorPath + " for installation")
					sys.exit(5)
			elif os.path.isfile(pPath):
				metaDescriptorPath = pPath
			else:
				print("Could not find meta descriptor file at directory or file " + metaDescriptorPath + " for installation")
				sys.exit(5)
			with open(metaDescriptorPath, "r") as metaDescriptorFile:
				metaDescription = json.load(metaDescriptorFile)
			# Read the component list of the repository to install into
			if (os.path.isfile(componentlistPath)):
				with open(componentlistPath, "r") as componentlistFile:
					componentlist = json.load(componentlistFile)
			else:
				componentlist = []
				
			# Find the specified repository in the list of existing repositories or add it
			repositoryIndex = -1
			for i in range(len(repositories)):
				rep = repositories[i]
				if rep["name"] == repository["name"] and rep["version"] == repository["version"]:
					repositoryIndex = i
			if repositoryIndex == -1:
				# The repository has not been created yet, add it
				if "-u" not in cliParams.keys():
					print("You need to specify whether the repository can be automatically updated or not (-u true/false)")
					sys.exit(6)
				repository["updateable"] = cliParams["-u"].lower() == "True"
				# The type is a name for the actual repository class in the pipeline builder. We also support
				# "GitHubRepository" but it is rather unprobably that this would created using this script.
				repository["type"] = "ComponentRepository"
				repositories.append(repository)
				# Write the updated repository list
				with open(repositoriesListPath, "w") as repositoriesFile:
					repositoriesFile.write(json.dumps(repositories, sort_keys=True, indent=4, separators=(",", ": ")) + os.linesep)
			
			# Add the meta description to the component list of the repository
			componentIndex = -1
			# First try to find a perhaps already existing entry for the current component
			for i in range(len(componentlist)):
				comp = componentlist[i]
				if comp["name"] == metaDescription["name"]:
					componentIndex = i
			if componentIndex == -1:
				componentlist.append(metaDescription)
			else:
				componentlist[componentIndex] = metaDescription
			with open(componentlistPath, "w") as componentlistFile:
				componentlistFile.write(json.dumps(componentlist, sort_keys=True, indent=4, separators=(",", ": ")) + os.linesep)
	else:
		print ("You need to pass UIMA component project directory as a parameter.")

