/*
 * Copyright (C) 2020 Bengt Martensson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.harctoolbox.irscrutinizer.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameUniquefier {

    private final static String DEFAULT_SEPARATOR = "#";
    private static final int INIT_CAPACITY = 64;

    public static List<String> mkUnique(Iterable<String> names, String separator) {
        NameUniquefier uniquefier = new NameUniquefier(separator);
        return uniquefier.uniquefy(names);
    }

    public static List<String> mkUnique(Iterable<String> names) {
        NameUniquefier uniquefier = new NameUniquefier();
        return uniquefier.uniquefy(names);
    }

    private final String separator;
    private final Pattern pattern;
    private final List<String> names;

    public NameUniquefier(String separator) {
        this(new ArrayList<String>(0), separator);
    }
    
    public NameUniquefier(Collection<String> oldNames, String separator) {
        this.separator = separator;
        pattern = Pattern.compile(separator + "\\d+$");
        names = new ArrayList<>(oldNames);
    }
    
    public NameUniquefier() {
        this(DEFAULT_SEPARATOR);
    }

    public boolean isOk(String name) {
        return !names.contains(name);
    }

    public String uniquefy(String name) {
        if (isOk(name)) {
            names.add(name);
            return name;
        }

        Matcher matcher = pattern.matcher(name);
        boolean hasExtension = matcher.find();
        int number;
        String stem;
        if (hasExtension) {
            String extension = matcher.group();
            number = Integer.parseInt(extension.substring(separator.length()));
            stem = name.substring(0, matcher.start());
        } else {
            number = 1;
            stem = name;
        }
        return uniq(stem, number);
    }

    public List<String> uniquefy(Iterable<String> names) {
        List<String> result = new ArrayList<>(INIT_CAPACITY);
        for (String name : names)
            result.add(uniquefy(name));

        return result;
    }

    private String uniq(String stem, int number) {
        for (int n = number;; n++) {
            String candidate = decorate(stem, n);
            if (isOk(candidate)) {
                names.add(candidate);
                return candidate;
            }
        }
    }

    private String decorate(String stem, int number) {
        return stem + separator + Integer.toString(number);
    }
}
