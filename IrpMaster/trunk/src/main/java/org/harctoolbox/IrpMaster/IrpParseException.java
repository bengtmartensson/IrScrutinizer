/*
Copyright (C) 2013 Bengt Martensson.

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

import org.antlr.runtime.RecognitionException;

/**
 * Thrown if an Irp protocol did not parse. Often used to encapsulate org,antlr.runtime.RecognitionException
 * in order not to export the latter, breaking encapsulation and requiring the use of antlr*.jar.
 * 
 */

public class IrpParseException extends ParseException {
    private static final long serialVersionUID = 1L;
    public int charPositionInLine = -1;
    public int line = -1;

    public IrpParseException(RecognitionException ex) {
        super(ex);
        charPositionInLine = ex.charPositionInLine;
        line = ex.line;
    }
}
