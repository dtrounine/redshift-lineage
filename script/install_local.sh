#!/bin/bash

script_dir=$(dirname ${BASH_SOURCE[0]})
echo "Script directory: $script_dir"

install_dir="$HOME/.local/share/redshift-lineage"
[[ -d $install_dir ]] && {
  echo "Removing existing installation at $install_dir"
  rm -rf $install_dir
}
parent_dir=$(dirname $install_dir)
[[ ! -d $parent_dir ]] && {
  echo "Creating parent directory at $parent_dir"
  mkdir -p $parent_dir
}

# array declaration
declare -A props
file="$script_dir/../gradle.properties"
while IFS='=' read -r key value; do
   props["$key"]="$value"
done < "$file"

version=${props["version"]}
echo "Installing Redshift Lineage version $version"

(
  cd $script_dir/..
  ./gradlew clean generateKotlinGrammarSource distZip

  dist_zip="build/distributions/redshift-lineage-$version.zip"
  [[ -z "$dist_zip" ]] && {
    echo "Failed to find distribution zip file."
    exit 1
  }
  tmp_unpack_dir="build/unpacked_zip"
  [[ -d $tmp_unpack_dir ]] && {
    echo "Removing existing unpacked zip directory at $tmp_unpack_dir"
    rm -rf $tmp_unpack_dir
  }
  mkdir -p $tmp_unpack_dir
  unzip -o "$dist_zip" -d "$tmp_unpack_dir"

  echo "Copying Redshift Lineage files from $tmp_unpack_dir/redshift-lineage-$version to $install_dir"
  mv "$tmp_unpack_dir/redshift-lineage-$version" "$install_dir/"
)
