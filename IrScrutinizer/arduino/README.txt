This is ArduinoGirsLite, Arduino software capable of sending and
analyzing IR signals. It is intended to be used with IrScrutinizer
version 1.1.0 and later. (It is not compatible with earlier versions.) It has
been tested on Arduino Uno, Nano, Leonardo, Micro, and Mega2560.

It consists of Chris Young's IRLib (http://tech.cyborg5.com/irlib/,
https://github.com/cyborg5/IRLib) in version 1.41, which is a major
rewrite of a previous library called IRremote, published by
Ken Shirriff in his blog at
http://www.righto.com/2009/08/multi-protocol-infrared-remote-library.html. The
current version is available at
https://github.com/shirriff/Arduino-IRremote.
The second component is Michael Dreher's IrWidget
(http://www.mikrocontroller.net/articles/High-Speed_capture_mit_ATmega_Timer
(in German), see also
http://www.hifi-remote.com/forums/viewtopic.php?p=111876#111876).

Michael's code has been somewhat reorganized.

Girs is a (rough) specification of a functionality of an "IR server",
see http://www.harctoolbox.org/Girs.html. The present software
implements the "Transmit" and the "Capture" modules. As the name
"ArduinoGirsLite" suggests, a more capable version is planned.

The present code is not really in a final state, however, it appears
as fairly reliable.

Licenses: Chris' as well as Ken's code is licensed under the LGPL
2.1-license. Michael's code carries the GPL2-license, although he is willing
to agree to "or later versions"
(http://www.hifi-remote.com/forums/viewtopic.php?p=112586#112586).

The complete package is, as part of IrScrutinizer, under the
GPL3-license.

The software assumes that the following pins are used for sending and
receiving. 

Hardware requirements: 

                          Sender Pin      Capture Pin
Uno/Nano (ATmega328P)          3             8
Leonardo/Micro (ATmega32U4)    9             4
Mega2560 (ATmega2560)          9            49
