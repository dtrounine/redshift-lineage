#!/bin/bash

script_dir=$(dirname ${BASH_SOURCE[0]})

sql_file="$1"
if [[ -z "$sql_file" ]]; then
  echo "Usage: $0 <path_to_sql_file>"
  exit 1
fi

antlr4-parse \
  $script_dir/../src/main/antlr/io/github/dtrounine/lineage/sql/RedshiftSqlParser.g4 \
  $script_dir/../src/main/antlr/io/github/dtrounine/lineage/sql/RedshiftSqlLexer.g4 \
  root -gui \
  $sql_file
