#!/usr/bin/env python

import urllib, json, os, sys, re
import requests # apt-get install python-requests
import magic # apt-get install python-magic seems to install a wrong version
from time import gmtime, strftime

__author__ = 'probonopd'
release_name = "ci-build"

git_config = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".git/config")
print git_config
config = open(git_config).read()
gits = None
try:
    gits = re.findall("https:.*.git", config)[0].split("/")
except:
    gits = re.findall("git:.*.git", config)[0].split("/")
username = gits[3]
repo = gits[4].replace(".git", "")

if os.environ.get('GITHUB_TOKEN')== None:
    print("GITHUB_TOKEN needs to be set in Travis CI Repository Settings")
    print("Skipping upload to GitHub Releases")
    exit(1)
else:
    token = os.environ.get('GITHUB_TOKEN')

mime = magic.Magic(mime=True)

url = 'https://api.github.com/repos/' + username + '/' + repo + '/releases'
print url

# TODO: MAKE A RELEASE AND DELETE THE PREVIOUS ONE INSTEAD OF HARDCODING ONE
# IF ONE THAT MATCHES THE CURRENT SOURCE IS NOT HERE
# THIS WAY THE SOURCE WILL MATCH TO THE BINARY...

# Get the release with the release_name
headers = {'Authorization': 'token ' + token}
response = requests.get(url, headers=headers)
print response
assert(response.status_code==200)
data = json.loads(response.content)

release = None
for candidate in data:
    if candidate['tag_name'] == release_name:
        release = candidate

if (release != None):
    print("Release with name " + release_name + " found, deleting...")
    # TODO: Check if deleting and creating a release is necessary because the git sha has changed since
    # Delete this release
    print release["url"]
    headers = {'Authorization': 'token ' + token}
    response = requests.delete(release["url"], headers=headers)
    print response
    assert(response.status_code==204)
    # Nuke the corresponding tag
    #resp = requests.delete('https://api.github.com/repos/' + username + '/' + repo + '/git/refs/tags/' + release_name, headers=headers)
    #print resp

# Create new release with the same name
print("Creating new release with name " + release_name + "...")
url = "https://api.github.com/repos/" + username + "/" + repo + "/releases"
headers = {'Authorization': 'token ' + token}
payload = {
  "tag_name": release_name,
  "target_commitish": "master",
  "name": release_name,
  "body": "These are the latest binaries built by Travis continuous integration  on " + strftime("%Y-%m-%d %H:%M:%S", gmtime()) + " GMT.\n\nPlease ignore the \"release date\" and the sources in this \"release\".",
  "draft": False,
  "prerelease": True
}
print url
response = requests.post(url, data=json.dumps(payload), headers=headers)
print response
print response.content
# assert(response.status_code==201)
release = json.loads(response.content)

# # Delete all binary assets of that release with the corresponding filename
# for asset in release["assets"]:
#     for arg in sys.argv[1:]:
#         filename = arg
#         if(asset["name"] == os.path.basename(filename)):
#             print("Deleting asset " + str(asset["id"]) + " ...")
#             headers = {'Authorization': 'token ' + token}
#             url = asset["url"]
#             response = requests.delete(url, headers=headers)
#             print response

for arg in sys.argv[1:]:
    filename = arg
    content_type = mime.from_file(filename)
    # Upload binary asset
    print("Uploading " + os.path.basename(filename) + "...")
    url = "https://uploads.github.com/repos/" + username + "/" + repo + "/releases/" + str(release["id"]) + "/assets?name=" + os.path.basename(filename)
    headers = {'Authorization': 'token ' + token,
               'Content-Type': content_type}
    data = open(filename, 'rb').read()
    response = requests.post(url, data=data, headers=headers)
    print response
    data = json.loads(response.content)
    print data["browser_download_url"]
