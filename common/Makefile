update: update-irp-protocols.xsd update-girr update-exportformats
	git commit -a

update-irp-protocols.xsd:
	wget -O schemas/irp-protocols.xsd https://raw.githubusercontent.com/bengtmartensson/IrpTransmogrifier/master/src/main/schemas/irp-protocols.xsd

update-girr:
	wget -O schemas/girr_ns-1.1.xsd https://raw.githubusercontent.com/bengtmartensson/Girr/master/src/main/schemas/girr_ns-1.1.xsd
	wget -O schemas/girr_ns-1.2.xsd https://raw.githubusercontent.com/bengtmartensson/Girr/master/src/main/schemas/girr_ns-1.2.xsd
	wget -O schemas/girr_ns.xsd https://raw.githubusercontent.com/bengtmartensson/Girr/master/src/main/schemas/girr_ns.xsd
	wget -O schemas/girr.xsd https://raw.githubusercontent.com/bengtmartensson/Girr/master/src/main/schemas/girr.xsd

update-exportformats:
	wget -O schemas/exportformats.xsd https://raw.githubusercontent.com/bengtmartensson/IrScrutinizer/master/src/main/schemas/exportformats.xsd
