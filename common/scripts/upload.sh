#!/bin/bash

set +x # Do not leak information

# Exit immediately if one of the files given as arguments is not there
# because we don't want to delete the existing release if we don't have
# the new files that should be uploaded
for file in "$@" ; do
    if [ ! -e "$file" ]
    then echo "$file is missing, giving up." >&2; exit 1
    fi
done

if [ $# -eq 0 ]; then
    echo "No artifacts to use for release, quitting."
    exit 1
fi
DIR="$(dirname -- "$(readlink -f -- "${0}")" )"

RELEASE_NAME="ci-build" # Do not use "latest" as it is reserved by GitHub

if [ ! -z "$GITHUB_ACTOR" ] ; then
  # Github action
  echo "Running as GitHub action"
  if [ -z "$REPO_SLUG" ] ; then
    REPO_SLUG=${GITHUB_REPOSITORY}
  fi
  if [ -z "$GITHUB_TOKEN" ] ; then
    echo "\$GITHUB_TOKEN missing, please set it in the Travis CI settings of this project"
    exit 1
  fi
else
  # Not Github action
  echo "Running interactively"
  if [ -z "$REPO_SLUG" ] ; then
    read -p "Repo Slug (GitHub CI username/reponame): " REPO_SLUG
  fi
  if [ -z "$GITHUB_TOKEN" ] ; then
    read -p "Token (https://github.com/settings/tokens): " GITHUB_TOKEN
  fi
fi

tag_url="https://api.github.com/repos/$REPO_SLUG/git/refs/tags/$RELEASE_NAME"
tag_infos=$(curl -XGET --header "Authorization: token ${GITHUB_TOKEN}" "${tag_url}")
echo "tag_infos: $tag_infos"
tag_sha=$(echo "$tag_infos" | grep '"sha":' | head -n 1 | cut -d '"' -f 4 | cut -d '{' -f 1)
echo "tag_sha: $tag_sha"

release_url="https://api.github.com/repos/$REPO_SLUG/releases/tags/$RELEASE_NAME"
echo "Getting the release ID..."
echo "release_url: $release_url"
release_infos=$(curl -XGET --header "Authorization: token ${GITHUB_TOKEN}" "${release_url}")
echo "release_infos: $release_infos"
release_id=$(echo "$release_infos" | grep "\"id\":" | head -n 1 | tr -s " " | cut -f 3 -d" " | cut -f 1 -d ",")
echo "release ID: $release_id"
upload_url=$(echo "$release_infos" | grep '"upload_url":' | head -n 1 | cut -d '"' -f 4 | cut -d '{' -f 1)
echo "upload_url: $upload_url"
release_url=$(echo "$release_infos" | grep '"url":' | head -n 1 | cut -d '"' -f 4 | cut -d '{' -f 1)
echo "release_url: $release_url"

echo "Deleting $RELEASE_NAME..."

if [ ! -z "$release_id" ]; then
    delete_url="https://api.github.com/repos/$REPO_SLUG/releases/$release_id"
    echo "Delete the release..."
    echo "delete_url: $delete_url"
    curl -XDELETE \
        --header "Authorization: token ${GITHUB_TOKEN}" \
        "${delete_url}"
fi

echo "Deleting the tag..."
delete_url="https://api.github.com/repos/$REPO_SLUG/git/refs/tags/$RELEASE_NAME"
echo "delete_url: $delete_url"
curl -XDELETE \
      --header "Authorization: token ${GITHUB_TOKEN}" \
      "${delete_url}"

echo "Create release..."

if [ -z "$TRAVIS_BRANCH" ] ; then
  TRAVIS_BRANCH="master"
fi

BODY="WARNING: This is a snapshot of the current development stand. "
BODY+="It may have different issues, and possibly does not work at all. "
BODY+="If this is not what you want, use the latest official release instead."

release_infos=$(curl -H "Authorization: token ${GITHUB_TOKEN}" \
       --data '{"tag_name": "'"$RELEASE_NAME"'","target_commitish": "'"$TRAVIS_BRANCH"'","name": "'"Continuous build"'","body": "'"$BODY"'","draft": false,"prerelease": true}' "https://api.github.com/repos/$REPO_SLUG/releases")

echo "$release_infos"

unset upload_url
upload_url=$(echo "$release_infos" | grep '"upload_url":' | head -n 1 | cut -d '"' -f 4 | cut -d '{' -f 1)
echo "upload_url: $upload_url"

unset release_url
release_url=$(echo "$release_infos" | grep '"url":' | head -n 1 | cut -d '"' -f 4 | cut -d '{' -f 1)
echo "release_url: $release_url"

echo "Upload binaries to the release..."

for FILE in "$@" ; do
  FULLNAME="${FILE}"
  BASENAME="$(basename "${FILE}")"
  curl -H "Authorization: token ${GITHUB_TOKEN}" \
       -H "Accept: application/vnd.github.manifold-preview" \
       -H "Content-Type: application/octet-stream" \
       --data-binary "@${FULLNAME}" \
       "$upload_url?name=${BASENAME}"
  echo ""
done

echo "Publish the release..."

release_infos=$(curl -H "Authorization: token ${GITHUB_TOKEN}" \
       --data '{"draft": false}' "${release_url}")

echo "${release_infos}"
