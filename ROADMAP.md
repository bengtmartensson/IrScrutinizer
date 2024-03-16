# ROADMAP

This is an attempt to a high-level "roadmap" (or wishlist :-)) for IrScrutinizer.
The "low-level" issues are kept as [issues](https://github.com/bengtmartensson/IrScrutinizer/issues).

## Vocabulary
* _IrScrutinizer_ denotes the interactive program, and the source packages `org.harctoolbox.irscrutinizer` and subordinate packages,
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

## 6. Documentation system
See [#15](https://github.com/bengtmartensson/IrScrutinizer/issues/15). The
[article on the web site](http://harctoolbox.org/IrScrutinizer.html) should be a "cool" article,
not a "dry" reference manual.

## -1. Internationalization
This is not at all an important issue for me, at least not for the moment.
(Despite being Swedish native speaker, and speaking German at work.) Sometimes, however,
that day may come. For the immediate future, no internationalization is planned.