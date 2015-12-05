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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlExport {
    public static final String flashTagName = "flash";
    public static final String gapTagName = "gap";
    public static final String decodeTagName = "decode";
    public static final String decodesTagName = "decodes";
    public static final String protocolAttributeName = "protocol";
    public static final String textTagName = "text";
    public static final String prontoTagName = "pronto";
    public static final String introTagName = "intro";
    public static final String repeatTagName = "repeat";
    public static final String endingTagName = "ending";
    public static final String parametersTagName = "parameters";
    public static final String parameterTagName = "parameter";
    public static final String parameterNameAttributeName = "name";
    public static final String parameterValueAttributeName = "value";
    public static final String irSignalTagName = "irsignal";
    public static final String nameAttributeName = "name";
    public static final String frequencyAttributeName = "frequency";
    public static final String dutyCycleAttributeName = "dutycycle";
    public static final String rawTagName = "raw";
    public static final String analyzerTagName = "analyzer";
    public static final String introBurstsLengthName = "nointrobursts";
    public static final String repeatBurstsLengthName = "norepeatbursts";
    public static final String endingBurstsLengthName = "noendingbursts";
    public static final String burstLengthAttributeName = "burstlength";
    public static final String commentAttributeName = "comment";

    private Document doc;

    public XmlExport(Document doc) {
        this.doc = doc;
    }

    public XmlExport() {
        doc = newDocument();
    }

    public static Document newDocument() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        Document doc = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
        } catch (ParserConfigurationException e) {
        }
        return doc;
    }


    public void printDOM(OutputStream ostr, Document stylesheet, String doctypeSystemid) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer tr;
            if (stylesheet == null) {
                tr = factory.newTransformer();

                tr.setOutputProperty(OutputKeys.METHOD, "xml");

            } else {
                tr = factory.newTransformer(new DOMSource(stylesheet));
            }
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            if (doctypeSystemid != null)
                tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctypeSystemid);
            tr.transform(new DOMSource(doc), new StreamResult(ostr));
        } catch (TransformerConfigurationException e) {
            System.err.println(e.getMessage());
        } catch (TransformerException e) {
            System.err.println(e.getMessage());
        }
    }

    public void printDOM(OutputStream ostr, String doctypeSystemid) {
        printDOM(ostr, null, doctypeSystemid);
    }

    public void printDOM(File file, String doctypeSystemid) throws FileNotFoundException, IOException {
        if (file == null)
            printDOM(System.out, doctypeSystemid);
        else {
            try (FileOutputStream stream = new FileOutputStream(file)) {
                printDOM(stream, doctypeSystemid);
                System.err.println("File " + file + " written.");
            }
        }
    }

    public void printDOM(File file) throws FileNotFoundException, IOException {
        printDOM(file, null);
    }

    public void setRoot(Element el) {
        doc.appendChild(el);
    }

    public Element toElement(String s, String tagname) {
        Element el = doc.createElement(tagname);
        el.setTextContent(s);
        return el;
    }

    public Element toElement(IrSequence irSequence, String tagname) {
        Element el = doc.createElement(tagname);
        el.setAttribute(burstLengthAttributeName, Integer.toString(irSequence.getNumberBursts()));
        for (int i = 0; i < irSequence.getNumberBursts(); i++) {
            double time = irSequence.get(2*i);
            el.appendChild(toElement(Long.toString(Math.round(Math.abs(time))), flashTagName));
            time = irSequence.get(2*i+1);
            el.appendChild(toElement(Long.toString(Math.round(Math.abs(time))), gapTagName));
        }
        return el;
    }

    public Element toElement(DecodeIR.DecodedSignal decode) {
        Element el = doc.createElement(decodeTagName);
        el.setAttribute(protocolAttributeName, decode.getProtocol());
        el.appendChild(toElement(decode.toString(), textTagName));
        Element parametersEl = doc.createElement(parametersTagName);
        el.appendChild(parametersEl);
        for (Entry<String, Long>kvp : decode.getParameters().entrySet()) {
            Element paramEl = doc.createElement(parameterTagName);
            paramEl.setAttribute(parameterNameAttributeName, kvp.getKey());
            paramEl.setAttribute(parameterValueAttributeName, Long.toString(kvp.getValue()));
            parametersEl.appendChild(paramEl);
        }
        return el;
    }

    public Element toElement(DecodeIR.DecodedSignal[] decodes) {
        if (decodes == null || decodes.length == 0)
            return null;
        Element el = doc.createElement(decodesTagName);
        for (DecodeIR.DecodedSignal decode : decodes)
            el.appendChild(toElement(decode));
        return el;
    }

    public Element toElement(IrSignal irSignal, String name, String comment, DecodeIR.DecodedSignal[] decodes, String analyze) {
        Element el = doc.createElement(irSignalTagName);
        el.setAttribute(nameAttributeName, name);
        el.setAttribute(frequencyAttributeName, Long.toString(Math.round(irSignal.getFrequency())));
        el.setAttribute(dutyCycleAttributeName, Double.toString(irSignal.getDutyCycle()));
        el.appendChild(toElement(decodes));
        el.appendChild(toElement(analyze, analyzerTagName));

        Element rawEl = doc.createElement(rawTagName);
        rawEl.setAttribute(introBurstsLengthName, Integer.toString(irSignal.getIntroBursts()));
        rawEl.setAttribute(repeatBurstsLengthName, Integer.toString(irSignal.getRepeatBursts()));
        rawEl.setAttribute(endingBurstsLengthName, Integer.toString(irSignal.getEndingBursts()));
        el.appendChild(rawEl);
        if (irSignal.getIntroLength() > 0)
            rawEl.appendChild(toElement(irSignal.getIntroSequence(), introTagName));
        if (irSignal.getRepeatLength() > 0)
            rawEl.appendChild(toElement(irSignal.getRepeatSequence(), repeatTagName));
        if (irSignal.getEndingLength() > 0)
            rawEl.appendChild(toElement(irSignal.getEndingSequence(), endingTagName));

        Element prontoEl = doc.createElement(prontoTagName);
        prontoEl.setTextContent(Pronto.toPrintString(irSignal));
        el.appendChild(prontoEl);
        return el;
    }

    public Element toElement(String protocolName, HashMap<String, Long>parameters, String name, String comment) {
        Element el = doc.createElement(irSignalTagName);
        el.setAttribute(nameAttributeName, name);
        el.setAttribute(commentAttributeName, comment);
        el.setAttribute(protocolAttributeName, protocolName);
        Element parametersEl = doc.createElement(parametersTagName);
        el.appendChild(parametersEl);
        for (Entry<String, Long>kvp : parameters.entrySet()) {
            Element paramEl = doc.createElement(parameterTagName);
            paramEl.setAttribute(parameterNameAttributeName, kvp.getKey());
            paramEl.setAttribute(parameterValueAttributeName, Long.toString(kvp.getValue()));
            parametersEl.appendChild(paramEl);
        }
        return el;
    }
}
