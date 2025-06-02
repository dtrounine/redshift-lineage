/*
Redshift SQL grammar.

Based on PostgreSQL grammar: https://github.com/antlr/grammars-v4/blob/a87211c01c8de6a86e949f70e1934f765e8eef5a/sql/postgresql/PostgreSQLParser.g4

The MIT License (MIT).
Copyright (C) 2021-2023, Oleksii Kovalov (Oleksii.Kovalov@outlook.com).
Copyright (C) 2025, Dmitrii Trunin (dtrounine@gmail.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
parser grammar RedshiftSqlParser;

options {
    tokenVocab = RedshiftSqlLexer;
    superClass = io.github.dtrounine.lineage.sql.RedshiftSqlParserBase;
}

root
    : stmtblock EOF
    ;

stmtblock
    : stmtmulti
    ;

stmtmulti
    : stmt? (SEMI stmt?)*
    ;

stmt
    : createstmt        # CreateStatement
    | selectstmt        # SelectStatement
    | grantstmt         # GrantStatement
    | alterownerstmt    # AlterOwnerStatement
    | dropstmt          # DropStatement
    | insertstmt        # InsertStatement
    | deletestmt        # DeleteStatement
    | updatestmt        # UpdateStatement
    ;


grantstmt
    : GRANT privileges ON privilege_target TO grantee_list grant_grant_option_?
    ;

privileges
    : privilege_list
    | ALL
    | ALL PRIVILEGES
    ;

privilege_list
    : privilege (COMMA privilege)*
    ;

privilege
    : SELECT column_list_?
    | REFERENCES column_list_?
    | CREATE column_list_?
    | colid column_list_?
    ;

grantee_list
    : grantee (COMMA grantee)*
    ;

grantee
    : rolespec
    | GROUP_P rolespec
    | ROLE rolespec
    ;

rolespec
    : nonreservedword
    | CURRENT_USER
    | SESSION_USER
    ;

grant_grant_option_
    : WITH GRANT OPTION
    ;


deletestmt
    : with_clause? DELETE_P FROM relation_expr_opt_alias where_clause?
//    | with_clause? DELETE_P FROM relation_expr_opt_alias using_clause? where_or_current_clause? returning_clause?
    ;

relation_expr_opt_alias
    : relation_expr (AS? colid)?
    ;


updatestmt
    : with_clause? UPDATE relation_expr_opt_alias SET set_clause_list from_clause? where_clause?
    ;

set_clause_list
    : set_clause (COMMA set_clause)*
    ;

set_clause
    : set_target EQUAL a_expr
    | OPEN_PAREN set_target_list CLOSE_PAREN EQUAL a_expr
    ;

set_target
    : colid opt_indirection
    ;

set_target_list
    : set_target (COMMA set_target)*
    ;



/* Any not-fully-reserved word --- these names can be, eg, role names.
 */
nonreservedword
    : identifier
    | unreserved_keyword
    | col_name_keyword
    | type_func_name_keyword
    ;


column_list_
    : OPEN_PAREN columnlist CLOSE_PAREN

    ;

columnlist
    : columnElem (COMMA columnElem)*
    ;

columnElem
    : colid
    ;

privilege_target
    : qualified_name_list
    | TABLE qualified_name_list
    | SCHEMA name_list
    | ALL TABLES IN_P SCHEMA name_list
    ;

alterownerstmt
    : ALTER TABLE qualified_name OWNER TO rolespec
    ;


dropstmt
    : DROP TABLE ( IF_P EXISTS )? any_name_list_ drop_behavior_?
    ;

any_name_list_
    : any_name (COMMA any_name)*
    ;

drop_behavior_
    : CASCADE
    | RESTRICT
    ;

insertstmt
    : with_clause? INSERT INTO insert_target insert_rest
//    | with_clause_? INSERT INTO insert_target insert_rest on_conflict_? returning_clause?
    ;

insert_target
    : qualified_name (AS colid)? target_columns?
    ;

target_columns
    : OPEN_PAREN colid ( COMMA colid )* CLOSE_PAREN
    ;

insert_rest
    : selectstmt
//    | OVERRIDING override_kind VALUE_P selectstmt
//    | OPEN_PAREN insert_column_list CLOSE_PAREN (OVERRIDING override_kind VALUE_P)? selectstmt
//    | DEFAULT VALUES
    ;



createstmt
    : CREATE opttemp? TABLE (IF_P NOT EXISTS)? qualified_name createstmt_rest_
    ;

createstmt_rest_
    : OPEN_PAREN opttableelementlist? CLOSE_PAREN table_attributes?  # CreateStmtColumns
    | AS OPEN_PAREN selectstmt CLOSE_PAREN                           # CreateStmtAsSelect
    ;

opttemp
    : TEMPORARY
    | TEMP
    ;

opttableelementlist
    : tableelementlist
    ;

tableelementlist
    : tableelement (COMMA tableelement)*
    ;

tableelement
//    : tableconstraint
//    | tablelikeclause
    : columnDef
    ;

columnDef
    : colid typename ( ENCODE encoding )?
//    | colid typename create_generic_options? colquallist
    ;

encoding
    : AZ64
    | BYTEDICT
    | DELTA
    | DELTA32
    | LZO
    | MOSTLY8
    | MOSTLY16
    | MOSTLY32
    | RAW
    | RUNLENGTH
    | TEXT255
    | TEXT32
    | ZSTD
    ;


table_attributes
    : table_attribute ( table_attribute )*
    ;

table_attribute
    : DISTSTYLE diststyle_type
    | DISTKEY OPEN_PAREN colid CLOSE_PAREN
    | sortkey_modifier? SORTKEY OPEN_PAREN colid (COMMA colid)* CLOSE_PAREN
    | SORTKEY AUTO
    | ENCODE AUTO
    ;

diststyle_type
    : AUTO
    | EVEN
    | ALL
    | KEY
    ;

sortkey_modifier
    : INTERLEAVED
    | COMPOUND
    ;

//tableconstraint
//    : CONSTRAINT name constraintelem
//    | constraintelem
//    ;
//
//constraintelem
//    : CHECK OPEN_PAREN a_expr CLOSE_PAREN constraintattributespec
//    | UNIQUE (
//        OPEN_PAREN columnlist CLOSE_PAREN c_include_? definition_? optconstablespace? constraintattributespec
//        | existingindex constraintattributespec
//    )
//    | PRIMARY KEY (
//        OPEN_PAREN columnlist CLOSE_PAREN c_include_? definition_? optconstablespace? constraintattributespec
//        | existingindex constraintattributespec
//    )
//    | EXCLUDE access_method_clause? OPEN_PAREN exclusionconstraintlist CLOSE_PAREN c_include_? definition_? optconstablespace? exclusionwhereclause?
//        constraintattributespec
//    | FOREIGN KEY OPEN_PAREN columnlist CLOSE_PAREN REFERENCES qualified_name column_list_? key_match? key_actions? constraintattributespec
//    ;


expr_list
    : a_expr (COMMA a_expr)*
    ;

//precendence accroding to Table 4.2. Operator Precedence (highest to lowest)

//https://www.postgresql.org/docs/12/sql-syntax-lexical.html#SQL-PRECEDENCE

/*
original version of a_expr, for info
 a_expr: c_expr
        //::	left	PostgreSQL-style typecast
       | a_expr TYPECAST typename -- 1
       | a_expr COLLATE any_name -- 2
       | a_expr AT TIME ZONE a_expr-- 3
       //right	unary plus, unary minus
       | (PLUS| MINUS) a_expr -- 4
        //left	exponentiation
       | a_expr CARET a_expr -- 5
        //left	multiplication, division, modulo
       | a_expr (STAR | SLASH | PERCENT) a_expr -- 6
        //left	addition, subtraction
       | a_expr (PLUS | MINUS) a_expr -- 7
        //left	all other native and user-defined operators
       | a_expr qual_op a_expr -- 8
       | qual_op a_expr -- 9
        //range containment, set membership, string matching BETWEEN IN LIKE ILIKE SIMILAR
       | a_expr NOT? (LIKE|ILIKE|SIMILAR TO|(BETWEEN SYMMETRIC?)) a_expr opt_escape -- 10
        //< > = <= >= <>	 	comparison operators
       | a_expr (LT | GT | EQUAL | LESS_EQUALS | GREATER_EQUALS | NOT_EQUALS) a_expr -- 11
       //IS ISNULL NOTNULL	 	IS TRUE, IS FALSE, IS NULL, IS DISTINCT FROM, etc
       | a_expr IS NOT?
            (
                NULL_P
                |TRUE_P
                |FALSE_P
                |UNKNOWN
                |DISTINCT FROM a_expr
                |OF OPEN_PAREN type_list CLOSE_PAREN
                |DOCUMENT_P
                |unicode_normal_form? NORMALIZED
            ) -- 12
       | a_expr (ISNULL|NOTNULL) -- 13
       | row OVERLAPS row -- 14
       //NOT	right	logical negation
       | NOT a_expr -- 15
        //AND	left	logical conjunction
       | a_expr AND a_expr -- 16
        //OR	left	logical disjunction
       | a_expr OR a_expr -- 17
       | a_expr (LESS_LESS|GREATER_GREATER) a_expr -- 18
       | a_expr qual_op -- 19
       | a_expr NOT? IN_P in_expr -- 20
       | a_expr subquery_Op sub_type (select_with_parens|OPEN_PAREN a_expr CLOSE_PAREN) -- 21
       | UNIQUE select_with_parens -- 22
       | DEFAULT -- 23
;
*/

a_expr
    : a_expr_qual
    ;

/*23*/

/*moved to c_expr*/

/*22*/

/*moved to c_expr*/

/*19*/

a_expr_qual
    : a_expr_lessless ({this.OnlyAcceptableOps()}? qual_op | )
    ;

/*18*/

a_expr_lessless
    : a_expr_or a_expr_lessless_rest_?
    ;

a_expr_lessless_rest_
    : ( (LESS_LESS | GREATER_GREATER) a_expr_or )+
    ;

/*17*/

a_expr_or
    : a_expr_and (OR a_expr_and)*
    ;

/*16*/

a_expr_and
    : a_expr_between (AND a_expr_between)*
    ;

/*21*/

a_expr_between
    : a_expr_in a_expr_between_rest_?
    ;

a_expr_between_rest_
    : NOT? BETWEEN SYMMETRIC? a_expr_in AND a_expr_in
    ;

/*20*/

a_expr_in
    : a_expr_unary_not (NOT? IN_P in_expr)?
    ;

/*15*/

a_expr_unary_not
    : NOT? a_expr_isnull
    ;

/*14*/

/*moved to c_expr*/

/*13*/

a_expr_isnull
    : a_expr_is_not (ISNULL | NOTNULL)?
    ;

/*12*/

a_expr_is_not
    : a_expr_compare (
        IS NOT? (
            NULL_P
            | TRUE_P
            | FALSE_P
            | UNKNOWN
            | DISTINCT FROM a_expr
            | OF OPEN_PAREN type_list CLOSE_PAREN
            | DOCUMENT_P
            | unicode_normal_form? NORMALIZED
        )
    )?
    ;

/*11*/

a_expr_compare
    : a_expr_like (
        (LT | GT | EQUAL | LESS_EQUALS | GREATER_EQUALS | NOT_EQUALS) a_expr_like
//        | subquery_Op sub_type (select_with_parens | OPEN_PAREN a_expr CLOSE_PAREN) /*21*/
    )?
    ;

/*10*/

a_expr_like
    : a_expr_qual_op (NOT? (LIKE | ILIKE | SIMILAR TO) a_expr_qual_op escape_?)?
    ;

/* 8*/

a_expr_qual_op
    : a_expr_unary_qualop (qual_op a_expr_unary_qualop)*
    ;

/* 9*/

a_expr_unary_qualop
    : qual_op? a_expr_add
    ;

/* 7*/

a_expr_add
    : a_expr_mul a_expr_add_rest_?
    ;

a_expr_add_rest_
    : a_expr_add_term_+
    ;

a_expr_add_term_
    : (MINUS | PLUS) a_expr_mul
    ;

/* 6*/

a_expr_mul
    : a_expr_caret a_expr_mul_rest_?
    ;

a_expr_mul_rest_
    : a_expr_mul_term_+
    ;

a_expr_mul_term_
    : (STAR | SLASH | PERCENT) a_expr_caret
    ;

/* 5*/

a_expr_caret
    : a_expr_unary_sign (CARET a_expr_unary_sign)?
    ;

/* 4*/

a_expr_unary_sign
    : (MINUS | PLUS)? a_expr_at_time_zone /* */
    ;

/* 3*/

a_expr_at_time_zone
    : a_expr_collate (AT TIME ZONE a_expr)?
    ;

/* 2*/

a_expr_collate
    : a_expr_typecast (COLLATE any_name)?
    ;

/* 1*/

a_expr_typecast
    : c_expr (TYPECAST typename)*
    ;


c_expr
    : EXISTS select_with_parens                                        # c_expr_exists
//    | ARRAY (select_with_parens | array_expr)                          # c_expr_expr
//    | PARAM opt_indirection                                            # c_expr_expr
//    | GROUPING OPEN_PAREN expr_list CLOSE_PAREN                        # c_expr_expr
//    | /*22*/ UNIQUE select_with_parens                                 # c_expr_expr
    | columnref                                                        # c_expr_columnref
    | aexprconst                                                       # c_expr_const
    | OPEN_PAREN a_expr_in_parens = a_expr CLOSE_PAREN opt_indirection # c_expr_in_parens
    | case_expr                                                        # c_expr_case
    | func_expr                                                        # c_expr_func
    | select_with_parens indirection?                                  # c_expr_select
//    | explicit_row                                                     # c_expr_expr
    | implicit_row                                                     # c_expr_implicit_row
//    | row OVERLAPS row /* 14*/                                         # c_expr_expr
//    | DEFAULT                                                          # c_expr_expr
    ;

columnref
    : colid indirection?
    ;


slice_bound_
    : a_expr
    ;

opt_indirection
    : indirection_el*
    ;

escape_
    : ESCAPE a_expr
    ;


qual_op
    : Operator
    | OPERATOR OPEN_PAREN any_operator CLOSE_PAREN
    ;

any_operator
    : (colid DOT)* all_op
    ;

all_op
    : Operator
    | mathop
    ;

mathop
    : PLUS
    | MINUS
    | STAR
    | SLASH
    | PERCENT
    | CARET
    | LT
    | GT
    | EQUAL
    | LESS_EQUALS
    | GREATER_EQUALS
    | NOT_EQUALS
    ;

in_expr
    : select_with_parens               # in_expr_select
    | OPEN_PAREN expr_list CLOSE_PAREN # in_expr_list
    ;

case_expr
    : CASE case_arg? when_clause_list case_default? END_P
    ;

when_clause_list
    : when_clause+
    ;

when_clause
    : WHEN when_expr = a_expr THEN then_expr = a_expr
    ;

case_default
    : ELSE a_expr
    ;

case_arg
    : a_expr
    ;


attrs
    : (DOT attr_name)+
    ;

any_name
    : colid attrs?
    ;

array_expr
    : OPEN_BRACKET (expr_list | array_expr_list)? CLOSE_BRACKET
    ;

array_expr_list
    : array_expr (COMMA array_expr)*
    ;

func_name
    : type_function_name
    | colid indirection
    ;

func_arg_list
    : func_arg_expr (COMMA func_arg_expr)*
    ;

func_arg_expr
    : a_expr
    | param_name (COLON_EQUALS | EQUALS_GREATER) a_expr
    ;

param_name
    : type_function_name
    ;

func_application
    : func_name OPEN_PAREN (
        func_arg_list (COMMA VARIADIC func_arg_expr)? sort_clause?
        | VARIADIC func_arg_expr sort_clause?
        | (ALL | DISTINCT) func_arg_list sort_clause?
        | STAR
        |
    ) CLOSE_PAREN
    ;

func_expr
    : func_application within_group_clause? filter_clause? over_clause?
    | func_expr_common_subexpr
    ;

within_group_clause
    : WITHIN GROUP_P OPEN_PAREN sort_clause CLOSE_PAREN
    ;

filter_clause
    : FILTER OPEN_PAREN WHERE a_expr CLOSE_PAREN
    ;

over_clause
    : OVER (window_specification | colid)

    ;

window_specification
    : OPEN_PAREN existing_window_name_? partition_clause_? sort_clause? frame_clause_? CLOSE_PAREN
    ;

existing_window_name_
    : colid
    ;

partition_clause_
    : PARTITION BY expr_list
    ;

frame_clause_
    : RANGE frame_extent window_exclusion_clause_?
    | ROWS frame_extent window_exclusion_clause_?
    | GROUPS frame_extent window_exclusion_clause_?
    ;

frame_extent
    : frame_bound
    | BETWEEN frame_bound AND frame_bound
    ;

frame_bound
    : UNBOUNDED (PRECEDING | FOLLOWING)
    | CURRENT_P ROW
    | a_expr (PRECEDING | FOLLOWING)
    ;

window_exclusion_clause_
    : EXCLUDE (CURRENT_P ROW | GROUP_P | TIES | NO OTHERS)
    ;

row
    : ROW OPEN_PAREN expr_list? CLOSE_PAREN
    | OPEN_PAREN expr_list COMMA a_expr CLOSE_PAREN
    ;

explicit_row
    : ROW OPEN_PAREN expr_list? CLOSE_PAREN
    ;

/*
TODO:
for some reason v1
implicit_row: OPEN_PAREN expr_list COMMA a_expr CLOSE_PAREN;
works better than v2
implicit_row: OPEN_PAREN expr_list  CLOSE_PAREN;
while looks like they are almost the same, except v2 requieres at least 2 items in list
while v1 allows single item in list
*/

implicit_row
    : OPEN_PAREN expr_list COMMA a_expr CLOSE_PAREN
    ;


aexprconst
    : iconst
    | fconst
    | sconst
    | bconst
    | xconst
    | func_name (sconst | OPEN_PAREN func_arg_list sort_clause? CLOSE_PAREN sconst)
    | consttypename sconst
    | constinterval (sconst interval_? | OPEN_PAREN iconst CLOSE_PAREN sconst)
    | TRUE_P
    | FALSE_P
    | NULL_P
    ;

xconst
    : HexadecimalStringConstant
    ;

bconst
    : BinaryStringConstant
    ;

fconst
    : Numeric
    ;

iconst
    : Integral
    | BinaryIntegral
    | OctalIntegral
    | HexadecimalIntegral
    ;

sconst
    : anysconst uescape_?
    ;

anysconst
    : StringConstant
    | UnicodeEscapeStringConstant
    | BeginDollarStringConstant DollarText* EndDollarStringConstant
    | EscapeStringConstant
    ;

uescape_
    : UESCAPE anysconst
    ;


selectstmt
    : select_no_parens
    | select_with_parens
    ;

select_with_parens
    : OPEN_PAREN select_no_parens CLOSE_PAREN       # SelectNoParentheses
    | OPEN_PAREN select_with_parens CLOSE_PAREN     # SelectWithParentheses
    ;

select_no_parens
    : select_clause sort_clause?
    | with_clause select_clause sort_clause?
    ;

select_clause
    : simple_select select_combination_list
//    : simple_select_intersect ((UNION | EXCEPT) all_or_distinct? simple_select_intersect)*
    ;

select_combination_list
    : ( select_comb_op all_or_distinct? simple_select )*
    ;

select_comb_op
    : UNION
    | EXCEPT
    ;

//simple_select_intersect
//    : simple_select
////    | simple_select (INTERSECT all_or_distinct? simple_select)*
//    ;

simple_select
    :
        SELECT
	        distinct_clause?
	        target_list
		    into_clause?
		    from_clause?
		    where_clause?
		    group_clause?
		    having_clause?
		    window_clause?          # StandardSimpleSelect
//    | (
//        SELECT
//      	( all_clause_? target_list?
//      		into_clause? from_clause? where_clause?
//      		group_clause? having_clause? window_clause?
//      	| distinct_clause target_list
//      		into_clause? from_clause? where_clause?
//      		group_clause? having_clause? window_clause?
//        )
//      )
    | values_clause                 # ValuesSimpleSelect
//    | TABLE relation_expr
//    | select_with_parens
    ;

values_clause
    : VALUES OPEN_PAREN expr_list CLOSE_PAREN (COMMA OPEN_PAREN expr_list CLOSE_PAREN)*
    ;

distinct_clause
    : DISTINCT
//    | DISTINCT (ON OPEN_PAREN expr_list CLOSE_PAREN)?
    ;

where_clause
    : WHERE a_expr
    ;

group_clause
    : GROUP_P BY group_by_list
    ;

group_by_list
    : group_by_item (COMMA group_by_item)*
    ;

group_by_item
    : a_expr
//    | empty_grouping_set
//    | cube_clause
//    | rollup_clause
//    | grouping_sets_clause
//    | a_expr
    ;

having_clause
    : HAVING a_expr
    ;

window_clause
    : WINDOW window_definition_list

    ;

window_definition_list
    : window_definition (COMMA window_definition)*
    ;

window_definition
    : colid AS window_specification
    ;



all_clause_
    : ALL
    ;

into_clause
    : INTO opttempTableName
    ;

opttempTableName
    : (LOCAL | GLOBAL)? (TEMPORARY | TEMP) table_? qualified_name
    | UNLOGGED table_? qualified_name
    | TABLE qualified_name
    | qualified_name
    ;

table_
    : TABLE
    ;

from_clause
    : FROM from_list
    ;

from_list
    : table_ref (COMMA table_ref)*
    ;


//table_ref
//    : (
//        relation_expr alias_clause?
////        | relation_expr alias_clause? tablesample_clause?
////        | func_table func_alias_clause?
////        | xmltable alias_clause?
//        | select_with_parens alias_clause?
////        | LATERAL_P (
////            xmltable alias_clause?
////            | func_table func_alias_clause?
////            | select_with_parens alias_clause?
////        )
//        | OPEN_PAREN table_ref  CLOSE_PAREN alias_clause?
////        | OPEN_PAREN table_ref (
////            CROSS JOIN table_ref
////            | NATURAL join_type? JOIN table_ref
////            | join_type? JOIN table_ref join_qual
////        )? CLOSE_PAREN alias_clause?
//    ) (
//        CROSS JOIN table_ref
//        | NATURAL join_type? JOIN table_ref
//        | join_type? JOIN table_ref join_qual
//    )*
//    ;

table_ref
    : simple_table_ref join_clause_list?
    ;

simple_table_ref
    : relation_expr alias_clause?
    | select_with_parens alias_clause?
    | OPEN_PAREN table_ref  CLOSE_PAREN alias_clause?
    ;

join_clause_list
    : join_clause (join_clause)*
    ;

join_clause
    : CROSS JOIN table_ref                      # cross_join_clause
//    | NATURAL join_type? JOIN table_ref
    | join_type? JOIN table_ref join_qual       # qualified_join_clause
    ;

join_type
    : (FULL | LEFT | RIGHT | INNER_P) OUTER_P?
    ;

join_qual
    : USING OPEN_PAREN name_list CLOSE_PAREN
    | ON a_expr
    ;

alias_clause
    : AS? colid
//    | AS? colid (OPEN_PAREN name_list CLOSE_PAREN)?
    ;

relation_expr
    : qualified_name
//    | qualified_name STAR?
//    | ONLY (qualified_name | OPEN_PAREN qualified_name CLOSE_PAREN)
    ;

with_clause
    : WITH cte_list
//    : WITH RECURSIVE? cte_list
    ;

cte_list
    : common_table_expr (COMMA common_table_expr)*
    ;

common_table_expr
    : name AS OPEN_PAREN selectstmt CLOSE_PAREN
//    | name name_list_? AS materialized_? OPEN_PAREN preparablestmt CLOSE_PAREN
    ;


all_or_distinct
    : ALL
    | DISTINCT
    ;


sort_clause
    : ORDER BY sortby_list
    ;

sortby_list
    : sortby (COMMA sortby)*
    ;

sortby
    : a_expr asc_desc_?
//    | a_expr (USING qual_all_op | asc_desc_?) nulls_order_?
    ;

qual_all_op
    : all_op
    | OPERATOR OPEN_PAREN any_operator CLOSE_PAREN
    ;

asc_desc_
    : ASC
    | DESC
    ;

nulls_order_
    : NULLS_P FIRST_P
    | NULLS_P LAST_P
    ;

type_list
    : typename (COMMA typename)*
    ;

typename
    : SETOF? simpletypename
	( opt_array_bounds
	| ARRAY (OPEN_BRACKET iconst CLOSE_BRACKET)?
	)
    ;

opt_array_bounds
    : (OPEN_BRACKET iconst? CLOSE_BRACKET)*
    ;


func_expr_common_subexpr
    : COLLATION FOR OPEN_PAREN a_expr CLOSE_PAREN
    | CURRENT_DATE
    | CURRENT_TIME (OPEN_PAREN iconst CLOSE_PAREN)?
    | CURRENT_TIMESTAMP (OPEN_PAREN iconst CLOSE_PAREN)?
    | LOCALTIME (OPEN_PAREN iconst CLOSE_PAREN)?
    | LOCALTIMESTAMP (OPEN_PAREN iconst CLOSE_PAREN)?
    | CURRENT_ROLE
    | CURRENT_USER
    | SESSION_USER
    | SYSTEM_USER
    | USER
    | CURRENT_CATALOG
    | CURRENT_SCHEMA
    | CAST OPEN_PAREN a_expr AS typename CLOSE_PAREN
//    | EXTRACT OPEN_PAREN extract_list? CLOSE_PAREN
    | NORMALIZE OPEN_PAREN a_expr (COMMA unicode_normal_form)? CLOSE_PAREN
//    | OVERLAY OPEN_PAREN (overlay_list | func_arg_list? ) CLOSE_PAREN
//    | POSITION OPEN_PAREN position_list? CLOSE_PAREN
//    | SUBSTRING OPEN_PAREN (substr_list | func_arg_list?) CLOSE_PAREN
    | TREAT OPEN_PAREN a_expr AS typename CLOSE_PAREN
//    | TRIM OPEN_PAREN (BOTH | LEADING | TRAILING)? trim_list CLOSE_PAREN
    | NULLIF OPEN_PAREN a_expr COMMA a_expr CLOSE_PAREN
    | COALESCE OPEN_PAREN expr_list CLOSE_PAREN
    | GREATEST OPEN_PAREN expr_list CLOSE_PAREN
    | LEAST OPEN_PAREN expr_list CLOSE_PAREN
    | XMLCONCAT OPEN_PAREN expr_list CLOSE_PAREN
//    | XMLELEMENT OPEN_PAREN NAME_P colLabel (COMMA (xml_attributes | expr_list))? CLOSE_PAREN
//    | XMLEXISTS OPEN_PAREN c_expr xmlexists_argument CLOSE_PAREN
//    | XMLFOREST OPEN_PAREN xml_attribute_list CLOSE_PAREN
//    | XMLPARSE OPEN_PAREN document_or_content a_expr xml_whitespace_option? CLOSE_PAREN
    | XMLPI OPEN_PAREN NAME_P colLabel (COMMA a_expr)? CLOSE_PAREN
//    | XMLROOT OPEN_PAREN XML_P a_expr COMMA xml_root_version xml_root_standalone_? CLOSE_PAREN
//    | XMLSERIALIZE OPEN_PAREN document_or_content a_expr AS simpletypename CLOSE_PAREN
//    | JSON_OBJECT OPEN_PAREN (func_arg_list
//		| json_name_and_value_list
//		  json_object_constructor_null_clause?
//		  json_key_uniqueness_constraint?
//		  json_returning_clause?
//		| json_returning_clause? )
//		CLOSE_PAREN
//    | JSON_ARRAY OPEN_PAREN (json_value_expr_list
//		  json_array_constructor_null_clause?
//		  json_returning_clause?
//		| select_no_parens
//		  json_format_clause?
//		  json_returning_clause?
//		| json_returning_clause?
//		)
//		CLOSE_PAREN
//    | JSON '(' json_value_expr json_key_uniqueness_constraint? ')'
    | JSON_SCALAR '(' a_expr ')'
//    | JSON_SERIALIZE '(' json_value_expr json_returning_clause? ')'
    | MERGE_ACTION '(' ')'
//    | JSON_QUERY '('
//		json_value_expr ',' a_expr json_passing_clause?
//		json_returning_clause?
//		json_wrapper_behavior
//		json_quotes_clause?
//		json_behavior_clause?
//		')'
//    | JSON_EXISTS '('
//		json_value_expr ',' a_expr json_passing_clause?
//		json_on_error_clause?
//		')'
//    | JSON_VALUE '('
//		json_value_expr ',' a_expr json_passing_clause?
//		json_returning_clause?
//		json_behavior_clause?
//		')'
    ;




simpletypename
    : generictype
    | numeric
    | bit
    | character
    | constdatetime
    | constinterval (interval_? | OPEN_PAREN iconst CLOSE_PAREN)
    | jsonType
    ;

consttypename
    : numeric
    | constbit
    | constcharacter
    | constdatetime
    | jsonType
    ;

generictype
    : type_function_name attrs? type_modifiers_?
    ;

type_modifiers_
    : OPEN_PAREN expr_list CLOSE_PAREN
    ;

numeric
    : INT_P
    | INTEGER
    | SMALLINT
    | BIGINT
    | REAL
    | FLOAT_P float_?
    | DOUBLE_P PRECISION
    | DECIMAL_P type_modifiers_?
    | DEC type_modifiers_?
    | NUMERIC type_modifiers_?
    | BOOLEAN_P
    ;

float_
    : OPEN_PAREN iconst CLOSE_PAREN
    ;

bit
    : bitwithlength
    | bitwithoutlength
    ;

constbit
    : bitwithlength
    | bitwithoutlength
    ;

bitwithlength
    : BIT varying_? OPEN_PAREN expr_list CLOSE_PAREN
    ;

bitwithoutlength
    : BIT varying_?
    ;

constdatetime
    : (TIMESTAMP | TIME) (OPEN_PAREN iconst CLOSE_PAREN)? timezone_?
    ;

timezone_
    : WITH TIME ZONE
    | WITHOUT TIME ZONE
    ;

constinterval
    : INTERVAL
    ;

interval_
    : YEAR_P
    | MONTH_P
    | DAY_P
    | HOUR_P
    | MINUTE_P
    | interval_second
    | YEAR_P TO MONTH_P
    | DAY_P TO (HOUR_P | MINUTE_P | interval_second)
    | HOUR_P TO (MINUTE_P | interval_second)
    | MINUTE_P TO interval_second
    ;

interval_second
    : SECOND_P (OPEN_PAREN iconst CLOSE_PAREN)?
    ;

jsonType
    : JSON
    ;


/*****************************************************************************
 *
 *	target list for SELECT
 *
 *****************************************************************************/

target_list
    : target_el (COMMA target_el)*
    ;

target_el
    : a_expr (AS colLabel | bareColLabel |) # target_label
    | STAR                                  # target_star
    ;

qualified_name_list
    : qualified_name (COMMA qualified_name)*
    ;

qualified_name
    : colid indirection?
    ;

name_list
    : name (COMMA name)*
    ;

name
    : colid
    ;

/* Bare column label --- names that can be column labels without writing "AS".
 * This classification is orthogonal to the other keyword categories.
 */
bareColLabel
    : identifier
    | bare_label_keyword
    ;


/* Column identifier --- names that can be column, table, etc names.
 */
colid
    : identifier
    | unreserved_keyword
    | col_name_keyword
    ;

/* Type/function identifier --- names that can be type or function names.
 */
type_function_name
    : identifier
    | unreserved_keyword
    | type_func_name_keyword
    ;


indirection_el
    : DOT (attr_name | STAR)
    | OPEN_BRACKET (a_expr | slice_bound_? COLON slice_bound_?) CLOSE_BRACKET
    ;

indirection
    : indirection_el+
    ;

attr_name
    : colLabel
    ;

/* Column label --- allowed labels in "AS" clauses.
 * This presently includes *all* Postgres keywords.
 */
colLabel
    : identifier
    | unreserved_keyword
    | col_name_keyword
    | type_func_name_keyword
    | reserved_keyword
    | EXIT //NB: not in gram.y official source.
    ;

character
    : character_c (OPEN_PAREN iconst CLOSE_PAREN)?
    ;

constcharacter
    : character_c (OPEN_PAREN iconst CLOSE_PAREN)?
    ;

character_c
    : (CHARACTER | CHAR_P | NCHAR) varying_?
    | VARCHAR
    | NATIONAL (CHARACTER | CHAR_P) varying_?
    ;

varying_
    : VARYING
    ;

unicode_normal_form
    : NFC
    | NFD
    | NFKC
    | NFKD
    ;


/*
 * Keyword category lists.  Generally, every keyword present in
 * the Postgres grammar should appear in exactly one of these lists.
 *
 * Put a new keyword into the first list that it can go into without causing
 * shift or reduce conflicts.  The earlier lists define "less reserved"
 * categories of keywords.
 *
 * Make sure that each keyword's category in kwlist.h matches where
 * it is listed here.  (Someday we may be able to generate these lists and
 * kwlist.h's table from one source of truth.)
 */

/* "Unreserved" keywords --- available for use as any kind of name.
 */
unreserved_keyword
    : ABORT_P
    | ABSENT
    | ABSOLUTE_P
    | ACCESS
    | ACTION
    | ADD_P
    | ADMIN
    | AFTER
    | AGGREGATE
    | ALSO
    | ALTER
    | ALWAYS
    | ASENSITIVE
    | ASSERTION
    | ASSIGNMENT
    | AT
    | ATOMIC
    | ATTACH
    | ATTRIBUTE
    | BACKWARD
    | BEFORE
    | BEGIN_P
    | BREADTH
    | BY
    | CACHE
    | CALL
    | CALLED
    | CASCADE
    | CASCADED
    | CATALOG
    | CHAIN
    | CHARACTERISTICS
    | CHECKPOINT
    | CLASS
    | CLOSE
    | CLUSTER
    | COLUMNS
    | COMMENT
    | COMMENTS
    | COMMIT
    | COMMITTED
    | COMPRESSION
    | CONDITIONAL
    | CONFIGURATION
    | CONFLICT
    | CONNECTION
    | CONSTRAINTS
    | CONTENT_P
    | CONTINUE_P
    | CONVERSION_P
    | COPY
    | COST
    | CSV
    | CUBE
    | CURRENT_P
    | CURSOR
    | CYCLE
    | DATA_P
    | DATABASE
    | DAY_P
    | DEALLOCATE
    | DECLARE
    | DEFAULTS
    | DEFERRED
    | DEFINER
    | DELETE_P
    | DELIMITER
    | DELIMITERS
    | DEPENDS
    | DEPTH
    | DETACH
    | DICTIONARY
    | DISABLE_P
    | DISCARD
    | DOCUMENT_P
    | DOMAIN_P
    | DOUBLE_P
    | DROP
    | EACH
    | EMPTY_P
    | ENABLE_P
    | ENCODING
    | ENCRYPTED
    | ENUM_P
    | ERROR
    | ESCAPE
    | EVENT
    | EXCLUDE
    | EXCLUDING
    | EXCLUSIVE
    | EXECUTE
    | EXPLAIN
    | EXPRESSION
    | EXTENSION
    | EXTERNAL
    | FAMILY
    | FILTER
    | FINALIZE
    | FIRST_P
    | FOLLOWING
    | FORCE
    | FORMAT
    | FORWARD
    | FUNCTION
    | FUNCTIONS
    | GENERATED
    | GLOBAL
    | GRANTED
    | GROUPS
    | HANDLER
    | HEADER_P
    | HOLD
    | HOUR_P
    | IDENTITY_P
    | IF_P
    | IMMEDIATE
    | IMMUTABLE
    | IMPLICIT_P
    | IMPORT_P
    | INCLUDE
    | INCLUDING
    | INCREMENT
    | INDENT
    | INDEX
    | INDEXES
    | INHERIT
    | INHERITS
    | INLINE_P
    | INPUT_P
    | INSENSITIVE
    | INSERT
    | INSTEAD
    | INVOKER
    | ISOLATION
    | KEEP
    | KEY
    | KEYS
    | LABEL
    | LANGUAGE
    | LARGE_P
    | LAST_P
    | LEAKPROOF
    | LEVEL
    | LISTEN
    | LOAD
    | LOCAL
    | LOCATION
    | LOCK_P
    | LOCKED
    | LOGGED
    | MAPPING
    | MATCH
    | MATCHED
    | MATERIALIZED
    | MAXVALUE
    | MERGE
    | METHOD
    | MINUTE_P
    | MINVALUE
    | MODE
    | MONTH_P
    | MOVE
    | NAME_P
    | NAMES
    | NESTED
    | NEW
    | NEXT
    | NFC
    | NFD
    | NFKC
    | NFKD
    | NO
    | NORMALIZED
    | NOTHING
    | NOTIFY
    | NOWAIT
    | NULLS_P
    | OBJECT_P
    | OF
    | OFF
    | OIDS
    | OLD
    | OMIT
    | OPERATOR
    | OPTION
    | OPTIONS
    | ORDINALITY
    | OTHERS
    | OVER
    | OVERRIDING
    | OWNED
    | OWNER
    | PARALLEL
    | PARAMETER
    | PARSER
    | PARTIAL
    | PARTITION
    | PASSING
    | PASSWORD
    | PATH
    | PERIOD
    | PLAN
    | PLANS
    | POLICY
    | PRECEDING
    | PREPARE
    | PREPARED
    | PRESERVE
    | PRIOR
    | PRIVILEGES
    | PROCEDURAL
    | PROCEDURE
    | PROCEDURES
    | PROGRAM
    | PUBLICATION
    | QUOTE
    | QUOTES
    | RANGE
    | READ
    | REASSIGN
//    | RECHECK
    | RECURSIVE
    | REF
    | REFERENCING
    | REFRESH
    | REINDEX
    | RELATIVE_P
    | RELEASE
    | RENAME
    | REPEATABLE
    | REPLACE
    | REPLICA
    | RESET
    | RESTART
    | RESTRICT
    | RETURN
    | RETURNS
    | REVOKE
    | ROLE
    | ROLLBACK
    | ROLLUP
    | ROUTINE
    | ROUTINES
    | ROWS
    | RULE
    | SAVEPOINT
    | SCALAR
    | SCHEMA
    | SCHEMAS
    | SCROLL
    | SEARCH
    | SECOND_P
    | SECURITY
    | SEQUENCE
    | SEQUENCES
    | SERIALIZABLE
    | SERVER
    | SESSION
    | SET
    | SETS
    | SHARE
    | SHOW
    | SIMPLE
    | SKIP_P
    | SNAPSHOT
    | SOURCE
    | SQL_P
    | STABLE
    | STANDALONE_P
    | START
    | STATEMENT
    | STATISTICS
    | STDIN
    | STDOUT
    | STORAGE
    | STORED
    | STRICT_P
    | STRING_P
    | STRIP_P
    | SUBSCRIPTION
    | SUPPORT
    | SYSID
    | SYSTEM_P
    | TABLES
    | TABLESPACE
    | TARGET
    | TEMP
    | TEMPLATE
    | TEMPORARY
    | TEXT_P
    | TIES
    | TRANSACTION
    | TRANSFORM
    | TRIGGER
    | TRUNCATE
    | TRUSTED
    | TYPE_P
    | TYPES_P
    | UESCAPE
    | UNBOUNDED
    | UNCOMMITTED
    | UNCONDITIONAL
    | UNENCRYPTED
    | UNKNOWN
    | UNLISTEN
    | UNLOGGED
    | UNTIL
    | UPDATE
    | VACUUM
    | VALID
    | VALIDATE
    | VALIDATOR
    | VALUE_P
    | VARYING
    | VERSION_P
    | VIEW
    | VIEWS
    | VOLATILE
    | WHITESPACE_P
    | WITHIN
    | WITHOUT
    | WORK
    | WRAPPER
    | WRITE
    | XML_P
    | YEAR_P
    | YES_P
    | ZONE
    ;


/* Column identifier --- keywords that can be column, table, etc names.
 *
 * Many of these keywords will in fact be recognized as type or function
 * names too; but they have special productions for the purpose, and so
 * can't be treated as "generic" type or function names.
 *
 * The type names appearing here are not usable as function names
 * because they can be followed by '(' in typename productions, which
 * looks too much like a function call for an LR(1) parser.
 */
col_name_keyword
    : BETWEEN
    | BIGINT
    | BIT
    | BOOLEAN_P
    | CHAR_P
    | character
    | COALESCE
    | DEC
    | DECIMAL_P
    | EXISTS
    | EXTRACT
    | FLOAT_P
    | GREATEST
    | GROUPING
    | INOUT
    | INT_P
    | INTEGER
    | INTERVAL
    | JSON
    | JSON_ARRAY
    | JSON_ARRAYAGG
    | JSON_EXISTS
    | JSON_OBJECT
    | JSON_OBJECTAGG
    | JSON_QUERY
    | JSON_SCALAR
    | JSON_SERIALIZE
    | JSON_TABLE
    | JSON_VALUE
    | LEAST
    | MERGE_ACTION
    | NATIONAL
    | NCHAR
    | NONE
    | NORMALIZE
    | NULLIF
    | NUMERIC
    | OUT_P
    | OVERLAY
    | POSITION
    | PRECISION
    | REAL
    | ROW
    | SETOF
    | SMALLINT
    | SUBSTRING
    | TIME
    | TIMESTAMP
    | TREAT
    | TRIM
    | VALUES
    | VARCHAR
    | XMLATTRIBUTES
    | XMLCONCAT
    | XMLELEMENT
    | XMLEXISTS
    | XMLFOREST
    | XMLNAMESPACES
    | XMLPARSE
    | XMLPI
    | XMLROOT
    | XMLSERIALIZE
    | XMLTABLE
    ;

/* Type/function identifier --- keywords that can be type or function names.
 *
 * Most of these are keywords that are used as operators in expressions;
 * in general such keywords can't be column names because they would be
 * ambiguous with variables, but they are unambiguous as function identifiers.
 *
 * Do not include POSITION, SUBSTRING, etc here since they have explicit
 * productions in a_expr to support the goofy SQL9x argument syntax.
 * - thomas 2000-11-28
 */
type_func_name_keyword
    : AUTHORIZATION
    | BINARY
    | COLLATION
    | CONCURRENTLY
    | CROSS
    | CURRENT_SCHEMA
    | FREEZE
    | FULL
    | ILIKE
    | INNER_P
    | IS
    | ISNULL
    | JOIN
    | LEFT
    | LIKE
    | NATURAL
    | NOTNULL
    | OUTER_P
    | OVERLAPS
    | RIGHT
    | SIMILAR
    | TABLESAMPLE
    | VERBOSE
    ;

/* Reserved keyword --- these keywords are usable only as a ColLabel.
 *
 * Keywords appear here if they could not be distinguished from variable,
 * type, or function names in some contexts.  Don't put things here unless
 * forced to.
 */
reserved_keyword
    : ALL
    | ANALYSE
    | ANALYZE
    | AND
    | ANY
    | ARRAY
    | AS
    | ASC
    | ASYMMETRIC
    | BOTH
    | CASE
    | CAST
    | CHECK
    | COLLATE
    | COLUMN
    | CONSTRAINT
    | CREATE
    | CURRENT_CATALOG
    | CURRENT_DATE
    | CURRENT_ROLE
    | CURRENT_TIME
    | CURRENT_TIMESTAMP
    | CURRENT_USER
    | DEFAULT
    | DEFERRABLE
    | DESC
    | DISTINCT
    | DO
    | ELSE
    | END_P
    | EXCEPT
    | FALSE_P
    | FETCH
    | FOR
    | FOREIGN
    | FROM
    | GRANT
    | GROUP_P
    | HAVING
    | IN_P
    | INITIALLY
    | INTERSECT
    | INTO
    | LATERAL_P
    | LEADING
    | LIMIT
    | LOCALTIME
    | LOCALTIMESTAMP
    | NOT
    | NULL_P
    | OFFSET
    | ON
    | ONLY
    | OR
    | ORDER
    | PLACING
    | PRIMARY
    | REFERENCES
    | RETURNING
    | SELECT
    | SESSION_USER
    | SOME
    | SYMMETRIC
    | SYSTEM_USER
    | TABLE
    | THEN
    | TO
    | TRAILING
    | TRUE_P
    | UNION
    | UNIQUE
    | USER
    | USING
    | VARIADIC
    | WHEN
    | WHERE
    | WINDOW
    | WITH
    ;


/*
 * While all keywords can be used as column labels when preceded by AS,
 * not all of them can be used as a "bare" column label without AS.
 * Those that can be used as a bare label must be listed here,
 * in addition to appearing in one of the category lists above.
 *
 * Always add a new keyword to this list if possible.  Mark it BARE_LABEL
 * in kwlist.h if it is included here, or AS_LABEL if it is not.
 */
bare_label_keyword
    : ABORT_P
    | ABSENT
    | ABSOLUTE_P
    | ACCESS
    | ACTION
    | ADD_P
    | ADMIN
    | AFTER
    | AGGREGATE
    | ALL
    | ALSO
    | ALTER
    | ALWAYS
    | ANALYSE
    | ANALYZE
    | AND
    | ANY
    | ASC
    | ASENSITIVE
    | ASSERTION
    | ASSIGNMENT
    | ASYMMETRIC
    | AT
    | ATOMIC
    | ATTACH
    | ATTRIBUTE
    | AUTHORIZATION
    | BACKWARD
    | BEFORE
    | BEGIN_P
    | BETWEEN
    | BIGINT
    | BINARY
    | BIT
    | BOOLEAN_P
    | BOTH
    | BREADTH
    | BY
    | CACHE
    | CALL
    | CALLED
    | CASCADE
    | CASCADED
    | CASE
    | CAST
    | CATALOG
    | CHAIN
    | CHARACTERISTICS
    | CHECK
    | CHECKPOINT
    | CLASS
    | CLOSE
    | CLUSTER
    | COALESCE
    | COLLATE
    | COLLATION
    | COLUMN
    | COLUMNS
    | COMMENT
    | COMMENTS
    | COMMIT
    | COMMITTED
    | COMPRESSION
    | CONCURRENTLY
    | CONDITIONAL
    | CONFIGURATION
    | CONFLICT
    | CONNECTION
    | CONSTRAINT
    | CONSTRAINTS
    | CONTENT_P
    | CONTINUE_P
    | CONVERSION_P
    | COPY
    | COST
    | CROSS
    | CSV
    | CUBE
    | CURRENT_CATALOG
    | CURRENT_DATE
    | CURRENT_P
    | CURRENT_ROLE
    | CURRENT_SCHEMA
    | CURRENT_TIME
    | CURRENT_TIMESTAMP
    | CURRENT_USER
    | CURSOR
    | CYCLE
    | DATA_P
    | DATABASE
    | DEALLOCATE
    | DEC
    | DECIMAL_P
    | DECLARE
    | DEFAULT
    | DEFAULTS
    | DEFERRABLE
    | DEFERRED
    | DEFINER
    | DELETE_P
    | DELIMITER
    | DELIMITERS
    | DEPENDS
    | DEPTH
    | DESC
    | DETACH
    | DICTIONARY
    | DISABLE_P
    | DISCARD
    | DISTINCT
    | DO
    | DOCUMENT_P
    | DOMAIN_P
    | DOUBLE_P
    | DROP
    | EACH
    | ELSE
    | EMPTY_P
    | ENABLE_P
    | ENCODING
    | ENCRYPTED
    | END_P
    | ENUM_P
    | ERROR
    | ESCAPE
    | EVENT
    | EXCLUDE
    | EXCLUDING
    | EXCLUSIVE
    | EXECUTE
    | EXISTS
    | EXPLAIN
    | EXPRESSION
    | EXTENSION
    | EXTERNAL
    | EXTRACT
    | FALSE_P
    | FAMILY
    | FINALIZE
    | FIRST_P
    | FLOAT_P
    | FOLLOWING
    | FORCE
    | FOREIGN
    | FORMAT
    | FORWARD
    | FREEZE
    | FULL
    | FUNCTION
    | FUNCTIONS
    | GENERATED
    | GLOBAL
    | GRANTED
    | GREATEST
    | GROUPING
    | GROUPS
    | HANDLER
    | HEADER_P
    | HOLD
    | IDENTITY_P
    | IF_P
    | ILIKE
    | IMMEDIATE
    | IMMUTABLE
    | IMPLICIT_P
    | IMPORT_P
    | IN_P
    | INCLUDE
    | INCLUDING
    | INCREMENT
    | INDENT
    | INDEX
    | INDEXES
    | INHERIT
    | INHERITS
    | INITIALLY
    | INLINE_P
    | INNER_P
    | INOUT
    | INPUT_P
    | INSENSITIVE
    | INSERT
    | INSTEAD
    | INT_P
    | INTEGER
    | INTERVAL
    | INVOKER
    | IS
    | ISOLATION
    | JOIN
    | JSON
    | JSON_ARRAY
    | JSON_ARRAYAGG
    | JSON_EXISTS
    | JSON_OBJECT
    | JSON_OBJECTAGG
    | JSON_QUERY
    | JSON_SCALAR
    | JSON_SERIALIZE
    | JSON_TABLE
    | JSON_VALUE
    | KEEP
    | KEY
    | KEYS
    | LABEL
    | LANGUAGE
    | LARGE_P
    | LAST_P
    | LATERAL_P
    | LEADING
    | LEAKPROOF
    | LEAST
    | LEFT
    | LEVEL
    | LIKE
    | LISTEN
    | LOAD
    | LOCAL
    | LOCALTIME
    | LOCALTIMESTAMP
    | LOCATION
    | LOCK_P
    | LOCKED
    | LOGGED
    | MAPPING
    | MATCH
    | MATCHED
    | MATERIALIZED
    | MAXVALUE
    | MERGE
    | MERGE_ACTION
    | METHOD
    | MINVALUE
    | MODE
    | MOVE
    | NAME_P
    | NAMES
    | NATIONAL
    | NATURAL
    | NCHAR
    | NESTED
    | NEW
    | NEXT
    | NFC
    | NFD
    | NFKC
    | NFKD
    | NO
    | NONE
    | NORMALIZE
    | NORMALIZED
    | NOT
    | NOTHING
    | NOTIFY
    | NOWAIT
    | NULL_P
    | NULLIF
    | NULLS_P
    | NUMERIC
    | OBJECT_P
    | OF
    | OFF
    | OIDS
    | OLD
    | OMIT
    | ONLY
    | OPERATOR
    | OPTION
    | OPTIONS
    | OR
    | ORDINALITY
    | OTHERS
    | OUT_P
    | OUTER_P
    | OVERLAY
    | OVERRIDING
    | OWNED
    | OWNER
    | PARALLEL
    | PARAMETER
    | PARSER
    | PARTIAL
    | PARTITION
    | PASSING
    | PASSWORD
    | PATH
    | PERIOD
    | PLACING
    | PLAN
    | PLANS
    | POLICY
    | POSITION
    | PRECEDING
    | PREPARE
    | PREPARED
    | PRESERVE
    | PRIMARY
    | PRIOR
    | PRIVILEGES
    | PROCEDURAL
    | PROCEDURE
    | PROCEDURES
    | PROGRAM
    | PUBLICATION
    | QUOTE
    | QUOTES
    | RANGE
    | READ
    | REAL
    | REASSIGN
    | RECURSIVE
    | REF
    | REFERENCES
    | REFERENCING
    | REFRESH
    | REINDEX
    | RELATIVE_P
    | RELEASE
    | RENAME
    | REPEATABLE
    | REPLACE
    | REPLICA
    | RESET
    | RESTART
    | RESTRICT
    | RETURN
    | RETURNS
    | REVOKE
    | RIGHT
    | ROLE
    | ROLLBACK
    | ROLLUP
    | ROUTINE
    | ROUTINES
    | ROW
    | ROWS
    | RULE
    | SAVEPOINT
    | SCALAR
    | SCHEMA
    | SCHEMAS
    | SCROLL
    | SEARCH
    | SECURITY
    | SELECT
    | SEQUENCE
    | SEQUENCES
    | SERIALIZABLE
    | SERVER
    | SESSION
    | SESSION_USER
    | SET
    | SETOF
    | SETS
    | SHARE
    | SHOW
    | SIMILAR
    | SIMPLE
    | SKIP_P
    | SMALLINT
    | SNAPSHOT
    | SOME
    | SOURCE
    | SQL_P
    | STABLE
    | STANDALONE_P
    | START
    | STATEMENT
    | STATISTICS
    | STDIN
    | STDOUT
    | STORAGE
    | STORED
    | STRICT_P
    | STRING_P
    | STRIP_P
    | SUBSCRIPTION
    | SUBSTRING
    | SUPPORT
    | SYMMETRIC
    | SYSID
    | SYSTEM_P
    | SYSTEM_USER
    | TABLE
    | TABLES
    | TABLESAMPLE
    | TABLESPACE
    | TARGET
    | TEMP
    | TEMPLATE
    | TEMPORARY
    | TEXT_P
    | THEN
    | TIES
    | TIME
    | TIMESTAMP
    | TRAILING
    | TRANSACTION
    | TRANSFORM
    | TREAT
    | TRIGGER
    | TRIM
    | TRUE_P
    | TRUNCATE
    | TRUSTED
    | TYPE_P
    | TYPES_P
    | UESCAPE
    | UNBOUNDED
    | UNCOMMITTED
    | UNCONDITIONAL
    | UNENCRYPTED
    | UNIQUE
    | UNKNOWN
    | UNLISTEN
    | UNLOGGED
    | UNTIL
    | UPDATE
    | USER
    | USING
    | VACUUM
    | VALID
    | VALIDATE
    | VALIDATOR
    | VALUE_P
    | VALUES
    | VARCHAR
    | VARIADIC
    | VERBOSE
    | VERSION_P
    | VIEW
    | VIEWS
    | VOLATILE
    | WHEN
    | WHITESPACE_P
    | WORK
    | WRAPPER
    | WRITE
    | XML_P
    | XMLATTRIBUTES
    | XMLCONCAT
    | XMLELEMENT
    | XMLEXISTS
    | XMLFOREST
    | XMLNAMESPACES
    | XMLPARSE
    | XMLPI
    | XMLROOT
    | XMLSERIALIZE
    | XMLTABLE
    | YES_P
    | ZONE
    ;


identifier
    : Identifier
    | QuotedIdentifier
    | UnicodeQuotedIdentifier
    | PLSQLVARIABLENAME
    ;
