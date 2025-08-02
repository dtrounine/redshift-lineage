# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a CLI tool that extracts table-level lineage information from Redshift SQL code using ANTLR-based parsing. The tool parses complex SQL statements to understand data flow relationships between tables, supporting CREATE TABLE AS SELECT, INSERT, DELETE, ALTER TABLE RENAME, and SELECT statements.

## Build System & Development Commands

**Build and Run:**
- `./gradlew build` - Build the project
- `./gradlew run` - Run the application 
- `./gradlew run --args="--help"` - Show CLI help
- `./gradlew test` - Run tests
- `./gradlew check` - Run all verification tasks

**Distribution:**
- `./gradlew distZip` - Create distributable ZIP archive
- `./gradlew installDist` - Install as local distribution

**Release Process:**
- `./gradlew release` - Full release workflow (version bump, tag, commit, GitHub release)

**Grammar Generation:**
- `./gradlew generateKotlinGrammarSource` - Generate ANTLR parser code from grammar files

## Architecture

**Core Components:**
- `Main.kt` - CLI entry point using Clikt framework
- `TableLineageExtractor.kt` - Main lineage extraction logic
- `sql/` package - ANTLR grammar definitions and AST handling
- `output/` package - Multiple output formats (JSON, YAML, OpenLineage)
- `model/` package - Data models for lineage information

**Key Architecture Patterns:**
- ANTLR4 for SQL parsing with custom Kotlin target
- Visitor pattern for AST traversal (`AbstractAstVisitor`, `AstVisitor`)
- Multiple output format support via strategy pattern
- Transitive lineage resolution for WITH clauses and CTEs

**SQL Statement Support:**
- SELECT with complex joins, subqueries, CTEs
- INSERT INTO ... SELECT 
- CREATE TABLE AS SELECT
- DELETE with WHERE clauses
- ALTER TABLE RENAME

## Testing

Tests are located in `src/test/kotlin/` and use Kotlin test framework. Test files include:
- `SelectLineageTests.kt` - Core SELECT statement lineage tests
- `InsertLineageTests.kt` - INSERT statement tests
- `DeleteLineageTests.kt` - DELETE statement tests
- `AlterTableTests.kt` - ALTER TABLE RENAME tests
- `SqlLineageTestUtils.kt` - Test utilities

Run tests with: 
- `./gradlew test` - to execute all tests.
- `./gradlew test` - tests "io.github.dtrounine.lineage.sql.SelectLineageTests" -- to run specific test classes.

## Grammar and Code Generation

The project uses ANTLR4 with Kotlin target for SQL parsing. Grammar files are in `src/main/antlr/`:
- `RedshiftSqlLexer.g4` - Lexer grammar
- `RedshiftSqlParser.g4` - Parser grammar

Generated code goes to `build/generatedAntlr/` and is automatically included in compilation.

## Version Management

Uses Gradle Release plugin for version management. Current version is tracked in `gradle.properties`. The release process automatically:
1. Updates version numbers
2. Creates Git tags  
3. Builds distribution artifacts
4. Creates GitHub releases

## Dependencies

Key dependencies:
- Kotlin 1.9.22 with JVM target 17
- ANTLR Kotlin runtime for parsing
- Clikt for CLI interface
- kotlinx-serialization for JSON output
- OpenLineage Java client for compatibility
- KAML for YAML output