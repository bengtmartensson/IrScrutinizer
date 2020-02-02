# ROADMAP

This is an attempt to a high-level "roadmap" (or wishlist :-)) for IrScrutinizer.
The "low-level" issues are kept as [harctoolbox issues](https://github.com/bengtmartensson/IrScrutinizer/issues).

## Vocabulary
* _IrScrutinizer_ denotes the interactive program, and the source packages `org.harctoolbox.irscrutinizer` and subordinate packages,
* _IrpMaster_ denotes the IRP rendering engine irpmaster.jar, source package `org.harctoolbox.IrpMaster` and subordinate packages
 (discontinued; not used in IrScrutinizer 2),
* _IrpTransmogrifier_ is [this project](https://github.com/bengtmartensson/IrpTransmogrifier), that has replaced IrpMaster, DecodeIR, Analyzer, and much more,
* _HarcHardware_ denotes a collection of "drivers" and other software that accesses hardware more or less directly,
  source package `org.harctoolbox.harchardware` and subordinate packages,


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
selecting "scrutinize this", and a new "scrutinizer signal" internal frame comes up.
There may possibly also be more subtools, cf. [issue #74](https://github.com/bengtmartensson/IrScrutinizer/issues/74).
(Cf. [this remark from the manual](http://www.harctoolbox.org/IrScrutinizer.html#The+pane+interface+sucks.).)
Here is an example of programming [internal frames in Java Swing](https://docs.oracle.com/javase/tutorial/uiswing/components/internalframe.html).

## 2. An advanced "abstract remote" editor.
(By "abstract remote" I mean a collection of IR commands with (unique) names, but with no assignment of the commands to
buttons on a physical remote.) This covers the issues
[#89](https://github.com/bengtmartensson/IrScrutinizer/issues/89),
[#88](https://github.com/bengtmartensson/IrScrutinizer/issues/88),
[#87](https://github.com/bengtmartensson/IrScrutinizer/issues/87),
[#86](https://github.com/bengtmartensson/IrScrutinizer/issues/86),
[#85](https://github.com/bengtmartensson/IrScrutinizer/issues/85),
[#73](https://github.com/bengtmartensson/IrScrutinizer/issues/73),
[#72](https://github.com/bengtmartensson/IrScrutinizer/issues/72),
[#71](https://github.com/bengtmartensson/IrScrutinizer/issues/71),
[#52](https://github.com/bengtmartensson/IrScrutinizer/issues/52),
[#48](https://github.com/bengtmartensson/IrScrutinizer/issues/48).

## 5. Replace RXTX
See [#20](https://github.com/bengtmartensson/IrScrutinizer/issues/20).

## 6. Documentation system
See [#15](https://github.com/bengtmartensson/IrScrutinizer/issues/15). The
[article on the web site](http://harctoolbox.org/IrScrutinizer.html) should be a "cool" article,
not a "dry" reference manual.

## 7. Porting/packaging
... to Android ([#81](https://github.com/bengtmartensson/IrScrutinizer/issues/81)),
ARM/RPi ([#68](https://github.com/bengtmartensson/IrScrutinizer/issues/68)),
misc systems ([#68](https://github.com/bengtmartensson/IrScrutinizer/issues/68)).
Any way to build inno setups in Travis?

## 8. Hardware support
Have send- and capturing co-exist better;
[#54](https://github.com/bengtmartensson/IrScrutinizer/issues/54)
did not turn out to be a very good solution, new try: [#281](https://github.com/bengtmartensson/IrScrutinizer/issues/281).
New devices, see [HarcHardware](https://github.com/bengtmartensson/HarcHardwareBundle/issues?q=is%3Aopen+is%3Aissue+label%3A%22new+hardware+support%22)

## 9. Advanced command line support.
IrpTransmogrifier has a quite clean and powerful command line interface; should probably not touch or replace that.
Presently, import files in Girr format can be given on the command line.
Should incorporate hardware support, see [#11 of HarcHardware](https://github.com/bengtmartensson/HarcHardwareBundle/issues/11).
Should allow for some use cases, that naturally leads themselvs for non-interactive
exection, see [this discussion](https://github.com/bengtmartensson/IrScrutinizer/issues/26#issuecomment-431651739).
Still, a finished concept is missing.

## 10. Testing
I strive to have [TestNG](http://testng.org) based Java testing, integrated in Maven and Netbeans.

## 11. Bulk analyze functions
Should the functions of IrpTransmogrifier-GUI, in particular for bulk analyze of a set of functions, of
[IrpTransmogrifier-GUI](https://github.com/bengtmartensson/IrpTransmogrifier-GUI)
be integrated, or should it be a separate tool?

## -1. Internationalization
This is not at all an important issue for me, at least not for the moment.
(Despite being Swedish native speaker, and speaking German at work.) Sometimes, however,
that day may come. For the immediate future, no internationalization is planned.