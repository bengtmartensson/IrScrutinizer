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

package org.harctoolbox.irscrutinizer.importer;

import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.girr.Command;

public class ProntoIrCode {

    private final static String[] prontoCharNames = {
        null, null, null, null, null, null, null, null, // 0x00-0x07
        null, null, null, null, null, null, null, null,// 0x08-0x0f
        null, null, null, null, null, null, null, null,// 0x10-0x17
        null, null, null, null, null, null, null, null,// 0x18-0x1f
        " ", "!", "\"", "#", "$", "%", "&", "'",
        "(", ")", "*", "+", ",", "-", ".", "/",
        "0", "1", "2", "3", "4", "5", "6", "7",
        "8", "9", ":", ";", "<", "=", ">", "?",
        "@", "A", "B", "C", "D", "E", "F", "G",
        "H", "I", "J", "K", "L", "M", "N", "O",
        "P", "Q", "R", "S", "T", "U", "V", "W",
        "X", "Y", "Z", "[", "\\", "]", "^", "_",
        "`", "a", "b", "c", "d", "e", "f", "g",
        "h", "i", "j", "k", "l", "m", "n", "o",
        "p", "q", "r", "s", "t", "u", "v", "w",
        "x", "y", "z", "{", "|", "}", "~", "PIP",
        "menu", "PIP_shift", "0x82", "PIP_swap", "0x84", "||", "stop", "play", // 0x80-0x87
        "0x88", "reverse", "0x8a", "still", "0x8c", "multipicture_display", "PIP_select", "key",
        "timer", "0x91", "0x92", "0x93", "0x94", "left", "previous_program", "right", // 0x90-0x97
        "0x98", "slow", "0x9a", "slow_reverse", "0x9c", "lamp", "teletext", "0x9f",
        "<<", "\u00a1", "||>", "focus_close", "I-II", "standby", "focus_far", "down", // 0xa0-0xa7
        "\u00a8", "record", "|<<", "eject", "up", "page_number_up", ">|", "page_enlargement", // 0xa8-0xaf
        "teletext_translucent", "subtitle", "./..", ">>", "\u00b4", "\u00b5", "|<", "picture_mute", // 0xb0-0xb7
        "\u00b8", "brightness", ">>|", "contrast", "brightness/contrast", "color", "<||", "\u00bf", // 0xb8-0xbf
        "split_screen_freeze", "split_screen_swap", "split_screen", "aspect_ratio", "personal_preferences", "PIP_freeze", "main_index_page", "still_mode", // 0xc0-0xc7
        "timer", "enter", "help", "clock", "EPG", "video_output", "read", "store", // 0xc8-0xcf
        "\u00d0", "\u00d1", "\u00d2", "\u00d3", "\u00d4", "\u00d5", "\u00d6", "page_number_down", // 0xd0-0xd7
        "\u00d8", "\u00d9", "\u00da", "\u00db", "\u00dc", "\u00dd", "\u00de", "\u00df", // 0xd8-0xdf
        "info", "active_control", "\u00e2", "page_hold", "\u00e4", "spatial_sound", "tv", "\u00e7", // 0xe0-0xe7
        "\u00e8", "\u00e9", "\u00ea", "\u00eb", "audio", "surround", "\u00ee", "\u00ef", // 0xe8-0xef
        "mute", "display", "\u00f2", "\u00f3", "\u00f4", "add_info", "\u00f6", "tape_direction", // 0xf0-0xf7
        "???", "\u00f9", "\u00fa", "\u00fb", "\u00fc", "???", "angle", "???", // 0xf8-0xff
    };
    private String ccf;
    private String name;
    private String comment;

    public String getCcf() {
        return ccf;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public Command toCommand(boolean generateRaw, boolean decode) throws IrpMasterException {
        return new Command(name, comment, ccf, generateRaw, decode);
    }

    // Not perfect, but better than nothing.
    public static String translateProntoFont(String name) {
        return name.length() == 1 ? prontoCharNames[(int) name.charAt(0)] : name;
    }

    private ProntoIrCode(String ccf, String name, String comment, boolean translateProntoFont) {
        this.ccf = ccf;
        this.name = translateProntoFont && name != null ? translateProntoFont(name) : name;
        this.comment = comment;
    }

    private ProntoIrCode(String ccf, String name) {
        this(ccf, name, null, true);
    }

    @Override
    public String toString() {
        return name
                + ((comment != null && !comment.isEmpty()) ? (" (" + comment + ")") : "")
                + " " + ccf;
    }
}
