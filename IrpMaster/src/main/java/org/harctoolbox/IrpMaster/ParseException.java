/*
Copyright (C) 2011 Bengt Martensson.

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
package org.harctoolbox.IrpMaster;

/**
 * Thrown if something did not parse. Often used to encapsulate org,antlr.runtime.RecognitionException
 * in order not to export the latter, breaking encapsulation and requiring the use of antlr*.jar.
 *
 */

public class ParseException extends IrpMasterException {

    public ParseException() {
        super();
    }

    public ParseException(String string) {
        super(string);
    }

    public ParseException(String string, Throwable ex) {
        super(string, ex);
    }

    public ParseException(Throwable ex) {
        super(ex);
    }
}
