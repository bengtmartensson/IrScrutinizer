# ROADMAP

This is an attempt to a high-level "roadmap" (or wishlist :-)) for IrScrutinizer.
The "low-level" issues are kept as [harctoolbox issues](https://github.com/bengtmartensson/harctoolboxbundle/issues).

## Vocabulary
* _IrScrutinizer_ denotes the interactive program, and the source packages `org.harctoolbox.irscrutinizer` and subordinate packages,
* _IrpMaster_ denotes the IRP rendering engine irpmaster.jar, source package `org.harctoolbox.IrpMaster` and subordinate packages,
* _HarcHardware_ denotes a collection of "drivers" and other software that accesses hardware more or less directly,
  source package `org.harctoolbox.harchardware` and subordinate packages,
* _harctoolboxbundle_ is a Github project containing the sources of IrScrutinizer, IrpMaster, HarcHardware and some the other packages.


## Demarkation (what _not_ to do (here!))
IrScrutinizer is a program for the interactive collection, analysis, editing and transformations of IR data,
and the sending of test signals. It should stay that way (cf. [Zawinski's law of software envelopment](https://en.wikipedia.org/wiki/Jamie_Zawinski#Zawinski.27s_law_of_software_envelopment)).
It should not be turned into a daemon or a production program for sending or receiving IR signals.
(This is not to say that I do not care about these use cases, see [Girs](http://www.harctoolbox.org/Girs.html),
and the projects [JGirs](https://github.com/bengtmartensson/JGirs), [AGirs](https://github.com/bengtmartensson/AGirs),
[dispatcher](https://github.com/bengtmartensson/dispatcher). Also note that
HarcHardware is intended to be useful for those use cases.)

## 1. "Object oriented" GUI with internal sub-frames.
The current panes "Scrutinize signal", "Scrutinize remote/parameteric", "Scrutinize remote/raw", generate, import,...
should instead be sub-panes of a "desktop pane", where they can be individually positioned, resized, minimized, maximized etc.
Also, there should then be the possibility of instantiating these "subtools" more than once, to the extent it makes sense.
They should also communicate, so that it can be possible  to right click on a signal in a table or tree,
selecting "scrutinize this", and a new "scerutinizer signal" internal frame comes up.
There may possibly also be more subtools, cf. [issue #74](https://github.com/bengtmartensson/harctoolboxbundle/issues/74).
(Cf. [this remark from the manual](http://www.harctoolbox.org/IrScrutinizer.html#The+pane+interface+sucks.).)
Here is an example of programming [internal frames in Java Swing](https://docs.oracle.com/javase/tutorial/uiswing/components/internalframe.html).

## 2. An advanced "abstract remote" editor.
("Abstract remote" denotes a collection of IR commands with (unique) names, but with no assignment of the commands to
buttons on a physical remote.) This covers the issues
[#89](https://github.com/bengtmartensson/harctoolboxbundle/issues/89),
[#88](https://github.com/bengtmartensson/harctoolboxbundle/issues/88),
[#87](https://github.com/bengtmartensson/harctoolboxbundle/issues/87),
[#86](https://github.com/bengtmartensson/harctoolboxbundle/issues/86),
[#85](https://github.com/bengtmartensson/harctoolboxbundle/issues/85),
[#73](https://github.com/bengtmartensson/harctoolboxbundle/issues/73),
[#72](https://github.com/bengtmartensson/harctoolboxbundle/issues/72),
[#71](https://github.com/bengtmartensson/harctoolboxbundle/issues/71),
[#52](https://github.com/bengtmartensson/harctoolboxbundle/issues/52),
[#48](https://github.com/bengtmartensson/harctoolboxbundle/issues/48).

## 3. Replace the repeatfinder and the analysis function.
See [#20](https://github.com/bengtmartensson/harctoolboxbundle/issues/20) and
[#23](https://github.com/bengtmartensson/harctoolboxbundle/issues/23).
The (third-party) code is unreliable and impossible to maintain.
See [IrpTransmogrifier](https://github.com/bengtmartensson/IrpTransmogrifier),
use case 5.

## 4. Modernize the IRP engine, couple to the decoding, thus replacing DecodeIR.
The decoding is unreliable, inflexible, completely decoupled with the IRP engine,
and constitutes a completely unnecessary JNI-library
(translating numbers to strings and numbers). See [IrpTransmogrifier](https://github.com/bengtmartensson/IrpTransmogrifier).
The decoding should be governed by the same data base (IrpProtocols.ini) that governs the generation.

## 5. Replace RXTX
See [#20](https://github.com/bengtmartensson/harctoolboxbundle/issues/20).
and also [#64](https://github.com/bengtmartensson/harctoolboxbundle/issues/64).

## 6. Documentation system
See [#15](https://github.com/bengtmartensson/harctoolboxbundle/issues/15).

## 7. Porting/packaging
... to Android ([#81](https://github.com/bengtmartensson/harctoolboxbundle/issues/81)),
ARM/RPi ([#68](https://github.com/bengtmartensson/harctoolboxbundle/issues/68)),
misc systems ([#68](https://github.com/bengtmartensson/harctoolboxbundle/issues/68)).
Any way to build inno setups in Travis?

## 8. Hardware support
Have send- and capturing co-exist better [#54](https://github.com/bengtmartensson/harctoolboxbundle/issues/54).
Tuning: [#58](https://github.com/bengtmartensson/harctoolboxbundle/issues/58). New devices:
[#53](https://github.com/bengtmartensson/harctoolboxbundle/issues/53). On Linux, possibly support reading from and writing to
[/dev/lirc0](http://lirc.org/html/lirc.html)?
To the extent possible, use [Girs as command language](http://www.harctoolbox.org/Girs.html) instead
of special hardware interfaces ([#24](https://github.com/bengtmartensson/harctoolboxbundle/issues/24)).

## 9. Testing
I strive to have [TestNG](http://testng.org) based Java testing, integrated in Maven and Netbeans.

## -1. Internationalization
This is not at all an important issue for me, at least not for the moment.
(Despite being Swedish native speaker, and speaking German at work.) Sometimes, however,
that day may come. For the immediate future, no internationalization is planned.