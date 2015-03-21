/*
Copyright (C) 2013, 2014 Bengt Martensson.

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

// It would probably be a good idea to make the line breaks part of the grammar,
// allowing to parse the Dish_Network files, using decimal command numbers,
// and to allow remote names to contain spaces.
// Another problem is a key name "end" is not accepted (remotes/sharp/LC-20V1RU).

// It is assumed that a 8-bit character set is used, similar to ISO-8859-1 (see ID).

grammar ConfigFile;

options {
    language=Java;
    output=template;
}

tokens {
}

@header {
package org.harctoolbox.jirc;
import java.util.LinkedHashMap;
import java.math.BigInteger;
}

@lexer::header {
package org.harctoolbox.jirc;
}

@members {
    private static class Bucket {
        String name = null;
        ArrayList<String> flags = new ArrayList<String>();
        HashMap<String, Long>unaryParameters = new HashMap<String, Long>();
        HashMap<String, IrRemote.XY>binaryParameters = new HashMap<String, IrRemote.XY>();

        public void add(String name, long x) {
            unaryParameters.put(name, x);
        }
        public void add(String name, long x, long y) {
            binaryParameters.put(name, new IrRemote.XY(x,y));
        }
    }

    private static long parseUnsignedLongHex(String s) {
        if (s.length() == 16) {
            long value = new BigInteger(s, 16).longValue();
            return value;
        }
        return Long.parseLong(s, 16);
    }
}

@rulecatch {
catch (RecognitionException e) {
   reportError(e);
   throw e;
}
}

remotes returns [LinkedHashMap<String, IrRemote> remotes = new LinkedHashMap<String, IrRemote>()]:
    (remote { $remotes.put($remote.irRemote.getName(), $remote.irRemote); })+
;

remote returns [IrRemote irRemote]:
    BEGIN_REMOTE parameters codes END_REMOTE { $irRemote = new IrRemote($parameters.bucket.name,
 $parameters.bucket.flags, $parameters.bucket.unaryParameters, $parameters.bucket.binaryParameters,
 $codes.codes); }
;

codes returns [ArrayList<IrNCode> codes]:
     cooked_codes { $codes = (ArrayList<IrNCode>)$cooked_codes.codes; }
     | raw_codes { $codes = (ArrayList<IrNCode>)$raw_codes.codes; }
;

cooked_codes returns [ArrayList<IrNCode> codes = new ArrayList<IrNCode>()]: BEGIN_CODES (cooked_button {$codes.add($cooked_button.irNCode);})* END_CODES
;

raw_codes returns [ArrayList<IrNCode> codes = new ArrayList<IrNCode>()]: BEGIN_RAW_CODES (raw_button {$codes.add($raw_button.irNCode); } )+ END_RAW_CODES
;

cooked_button returns [IrNCode irNCode]: name commandno_list {$irNCode = new IrNCode($name.text, $commandno_list.commandnos); }
;

raw_button returns [IrNCode irNCode]: 'name' name integer_list { $irNCode = new IrNCode($name.text, 0, $integer_list.codes); }
;

integer_list returns [ArrayList<Integer> codes = new ArrayList<Integer>()] : ( integer {$codes.add((int)$integer.value); } )+
;

commandno_list returns [ArrayList<Long> commandnos = new ArrayList<Long>()] : ( HEXINT {$commandnos.add(parseUnsignedLongHex($HEXINT.text.substring(2))); } )+
;

parameters returns [Bucket bucket = new Bucket()] :
 ( flags_decl { $bucket.flags = $flags_decl.flags; }
 | name_assignment { $bucket.name = $name_assignment.name; }
 | unary_parameter_assignment { $bucket.add($unary_parameter_assignment.name, $unary_parameter_assignment.number); }
 | binary_parameter_assignment { $bucket.add($binary_parameter_assignment.name, $binary_parameter_assignment.x, $binary_parameter_assignment.y); }
)*
;

name_assignment returns [String name]: 'name' name { $name = $name.text; }
;

unary_parameter_assignment returns [String name, long number]: unary_int_parameter integer { $name = $unary_int_parameter.text; $number = $integer.value; }
;

binary_parameter_assignment returns [String name, long x, long y]:  binary_parameter i=integer j=integer { $name = $binary_parameter.text; $x = $i.value; $y = $j.value; }
;

flags_decl returns [ArrayList<String> flags = new ArrayList<String>()]: 'flags' fl=flag {$flags.add($fl.text);} ('|' f=flag {$flags.add($f.text);} )*
;

flag: ID
;

unary_int_parameter: ID
;

binary_parameter: ID
;

BEGIN_REMOTE : 'begin' ' '+ 'remote';
END_REMOTE : 'end' ' '+ 'remote';
BEGIN_CODES : 'begin' ' '+ 'codes';
END_CODES : 'end' ' '+ 'codes';
BEGIN_RAW_CODES : 'begin' ' '+ 'raw_codes';
END_RAW_CODES : 'end' ' '+ 'raw_codes';

integer returns [long value]:
   INT { $value = Long.parseLong($INT.text); }
 | HEXINT { $value = parseUnsignedLongHex($HEXINT.text.substring(2)); }
;

name: INT | ID
;

INT:
	( '0' .. '9')+
;

HEXINT:
	'0' ('x' | 'X') ( '0' .. '9' | 'A' .. 'F' | 'a' .. 'f')+
;

ID:
        ('!' .. '{' | '}' .. '~' | '\u00A1' .. '\u00FF')+
;

COMMENT:
   '#' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}
;

WS:
   ( ' '
        | '\t'
        | '\r'
        | '\n'
    ) {$channel=HIDDEN;}
;
