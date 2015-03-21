/*
Copyright (C) 2011, 2013 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
 */

grammar Irp;

options {
    language=Java;
    backtrack=true;
    memoize=true;

    output=AST;
    ASTLabelType=CommonTree;
}

tokens {
   PROTOCOL;
   GENERALSPEC;
   FREQUENCY;
   BITDIRECTION;
   DUTYCYCLE;
   UNIT;
   BITSPEC_IRSTREAM;
   BITSPEC;
   IRSTREAM;
   BARE_IRSTREAM;
   BITFIELD;
   INFINITE_BITFIELD;
   COMPLEMENT;
   REVERSE; 
   FLASH;
   GAP;
   EXTENT;
   REPEAT_MARKER;
   VARIATION;
   POWER;
   UMINUS;
   BITCOUNT;
   FLOAT;
   ASSIGNMENT;
   DEFINITIONS;
   DEFINITION;
   PARAMETER_SPECS;
   PARAMETER_SPEC;
   PARAMETER_SPEC_MEMORY;
}

@header {
package org.harctoolbox.IrpMaster;
}

@lexer::header {
package org.harctoolbox.IrpMaster;
}

@members {
public static CommonTree newIntegerTree(long val) {
    return new CommonTree(new CommonToken(INT, Long.toString(val)));
}

protected void mismatch(IntStream input, int ttype, BitSet follow) throws RecognitionException {
   throw new MismatchedTokenException(ttype, input);
}

public Object recoverFromMismatchedSet(IntStream input, RecognitionException e, BitSet follow) throws RecognitionException {
   throw e;
}

}

@rulecatch {
catch (RecognitionException e) {
   reportError(e);
   throw e;
}
}

// Due to the lexer, there can be no "name"s called k, u, m ,p, lsb, or msb.
// I am not sure if it is an issue, but it is ugly.

// 1.7
protocol:
        generalspec bitspec_irstream definitions* parameter_specs? // Difference: * instead of ?, parameterspec
                    ->  ^(PROTOCOL generalspec bitspec_irstream definitions* parameter_specs? )
;

// 2.2, simplified
// This is simpler than Graham in the sense that some silly input is not rejected.
// TODO: issues with {}
generalspec
	:
	'{'  generalspec_list?  '}' -> ^( GENERALSPEC generalspec_list?  )
	;
	
// Allowing empty makes ANTLR misbehave.
generalspec_list
	:
        generalspec_item (','! generalspec_item)*
    ;
	 
generalspec_item
	:	 frequency_item | unit_item | order_item | dutycycle_item
	;
	
frequency_item
	:	number_with_decimals 'k' -> ^(FREQUENCY number_with_decimals)
;
	
dutycycle_item
	:	number_with_decimals '%' -> ^(DUTYCYCLE number_with_decimals)
;
	
unit_item
	:	number_with_decimals (u='u' | u='p') ?     -> ^(UNIT number_with_decimals $u?)
	;
	
order_item
	:	 (o=LSB | o=MSB)                 -> ^(BITDIRECTION $o) 
	;

//3.2
duration:
        flash_duration | gap_duration
    ;

flash_duration
	:	name_or_number (u='m' | u='u' | u='p')? -> ^(FLASH name_or_number $u?)
	;
	
gap_duration 
	:	'-' name_or_number (u='m' | u='u' | u='p')? -> ^(GAP name_or_number $u?)
	;
	
name_or_number
	:	name | number_with_decimals // Diff: Graham allowed number (integers) only
	;

// 4.2
extent	:
        '^'  x=name_or_number (z='m' | z='u' | z='p')? -> ^(EXTENT name_or_number  $z? )
;

//  5.2
bitfield:
        t='~'? data=primary_item ':'  (m='-'? length=primary_item (':' chop=primary_item)?  -> ^(BITFIELD $t? $m? $data $length $chop?)
                                    |  ':'  chop=primary_item                               -> ^(INFINITE_BITFIELD $t? $data $chop))
	;

primary_item :
        name
        | DOLLAR_ID
	| number
	| expression
	;
	
// 6.2
irstream:
        '(' bare_irstream ')' repeat_marker?    -> ^(IRSTREAM bare_irstream repeat_marker?)
    ;

bare_irstream:
        /* Empty */                             -> ^(BARE_IRSTREAM)
        | irstream_item (','  irstream_item)*   -> ^(BARE_IRSTREAM irstream_item+)
	;

irstream_item
    :
        variation
    | bitfield // must come before duration!
    | assignment
    | extent
    | duration
    | irstream
    | bitspec_irstream
    ;

// 7.4
bitspec	:
    '<'  bare_irstream ('|'  bare_irstream)* '>' -> ^(BITSPEC bare_irstream+)
    ;

// 8.2
repeat_marker	:
	 '*'                -> ^(REPEAT_MARKER '*')
        | '+'               -> ^(REPEAT_MARKER '+')
        | INT '+'?          -> ^(REPEAT_MARKER INT '+'?)
;

bitspec_irstream
	:
	bitspec irstream -> ^(BITSPEC_IRSTREAM bitspec irstream)
;

// 9.2
expression
	:
	'('! bare_expression ')'!
	;
	
// Following rules rewritten to avoid left recursion	
bare_expression
	:
        inclusive_or_expression
	;
	
inclusive_or_expression	:	
        exclusive_or_expression ('|'^ exclusive_or_expression)*
	;
	
exclusive_or_expression:
        and_expression ('^'^ and_expression)*
	;

and_expression:
    additive_expression ('&'^  additive_expression)*
	;

additive_expression
	: multiplicative_expression (('+' | '-')^  multiplicative_expression)*
	;

multiplicative_expression
	: exponential_expression ( ('*' | '/' | '%')^  exponential_expression)*
	;
	
exponential_expression
         : unary_expression ('**'^ exponential_expression)?
	;
	
unary_expression
	: (bitfield | primary_item)
        | '-'   (  bitfield     -> ^(UMINUS bitfield)
                 | primary_item -> ^(UMINUS primary_item) )
        | '#'   (  bitfield     -> ^(BITCOUNT bitfield)
                 | primary_item -> ^(BITCOUNT primary_item) )
;

// 10.2
definitions	:
	'{' definitions_list? '}' -> ^(DEFINITIONS definitions_list?)
	;
	
definitions_list	:
	/* Empty */
	| definition (','!  definition)* // Diff: recursion removed
	;

definition	:
	name '=' bare_expression -> ^(DEFINITION name bare_expression)
	;	
	
// 11.2
assignment	:
    name '=' bare_expression      -> ^(ASSIGNMENT name bare_expression)
	;
	
// 12.2
variation:
        intro=alternative repetition=alternative ending=alternative? -> ^(VARIATION $intro $repetition $ending?)
    ;

alternative:
        '['! bare_irstream? ']'!
	;
	
// 13.2
number
    :
        INT
;

number_with_decimals
        :
        INT | float_number
    ;

name:	
    ID
    | 'k'
    | 'u'
    | 'p'
    ;
    
parameter_specs
    :
    '[' parameter_spec (',' parameter_spec )* ']' -> ^(PARAMETER_SPECS parameter_spec+)
    | '['  ']' -> ^(PARAMETER_SPECS )
    ;
	
parameter_spec
    :
	name ':' l=INT '.' '.' h=number ('=' i=bare_expression)?         -> ^(PARAMETER_SPEC name $l $h $i?)
	|	name  '@' ':' l=INT '.' '.' h=number '=' bare_expression -> ^(PARAMETER_SPEC_MEMORY name $l $h bare_expression)
    ;
	
float_number	
	:
           '.' INT                  -> ^(FLOAT {new CommonTree(new CommonToken(INT,"0"))} INT)
	|	 x=INT '.' y=INT    -> ^(FLOAT  $x $y)
	;

	
LSB	:
        'lsb'
    ;

MSB	:
        'msb'
    ;

// Diff: Allow C syntax identifiers; Graham disallowed lower case and underscores.
ID  :
	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
		//('A'..'Z' ('A'..'Z'|'0'..'9')*  // Graham's version
;

DOLLAR_ID : '$' ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
    ;

INT :
	( '0' .. '9')+
;

// Diff: Not present by Graham.
COMMENT
    :   '//' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}
    |   '/*' ( options {greedy=false;} : . )* '*/' {$channel=HIDDEN;}
;

WS
      :
   ( ' '
        | '\t'
        | '\r'
        | '\n'
    ) {$channel=HIDDEN;}
    ;

NEWLINE
    :
        '\r'? '\n'
    ;

