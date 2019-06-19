/*
 Copyright (C) 2013,2014 Bengt Martensson.

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
package org.harctoolbox.irscrutinizer.importer;

/**
 * This interface models a class that can import something, commands or a sequence.
 */
public interface IImporter {

    /**
     *
     * @return Name of the importer. May contain spaces and other "funny" characters. Should be one word, and not end with "Importer" or such.
     */
    public String getFormatName();
}

