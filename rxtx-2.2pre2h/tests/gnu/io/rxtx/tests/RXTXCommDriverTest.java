/*-------------------------------------------------------------------------
|   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
|   RXTX is a native interface to serial ports in java.
|   Copyright 2008 Martin Oberhuber (Wind River) and others who
|   actually wrote it.  See individual source files for more information.
|
|   A copy of the LGPL v 2.1 may be found at
|   http://www.gnu.org/licenses/lgpl.txt on March 4th 2007.  A copy is
|   here for your convenience.
|
|   This library is free software; you can redistribute it and/or
|   modify it under the terms of the GNU Lesser General Public
|   License as published by the Free Software Foundation; either
|   version 2.1 of the License, or (at your option) any later version.
|
|   This library is distributed in the hope that it will be useful,
|   but WITHOUT ANY WARRANTY; without even the implied warranty of
|   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
|   Lesser General Public License for more details.
|
|   An executable that contains no derivative of any portion of RXTX, but
|   is designed to work with RXTX by being dynamically linked with it,
|   is considered a "work that uses the Library" subject to the terms and
|   conditions of the GNU Lesser General Public License.
|
|   The following has been added to the RXTX License to remove
|   any confusion about linking to RXTX.   We want to allow in part what
|   section 5, paragraph 2 of the LGPL does not permit in the special
|   case of linking over a controlled interface.  The intent is to add a
|   Java Specification Request or standards body defined interface in the
|   future as another exception but one is not currently available.
|
|   http://www.fsf.org/licenses/gpl-faq.html#LinkingOverControlledInterface
|
|   As a special exception, the copyright holders of RXTX give you
|   permission to link RXTX with independent modules that communicate with
|   RXTX solely through the Sun Microsytems CommAPI interface version 2,
|   regardless of the license terms of these independent modules, and to copy
|   and distribute the resulting combined work under terms of your choice,
|   provided that every copy of the combined work is accompanied by a complete
|   copy of the source code of RXTX (the version of RXTX used to produce the
|   combined work), being distributed under the terms of the GNU Lesser General
|   Public License plus this exception.  An independent module is a
|   module which is not derived from or based on RXTX.
|
|   Note that people who make modified versions of RXTX are not obligated
|   to grant this special exception for their modified versions; it is
|   their choice whether to do so.  The GNU Lesser General Public License
|   gives permission to release a modified version without this exception; this
|   exception also makes it possible to release a modified version which
|   carries forward this exception.
|
|   You should have received a copy of the GNU Lesser General Public
|   License along with this library; if not, write to the Free
|   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
|   All trademarks belong to their respective owners.
--------------------------------------------------------------------------*/
package gnu.io.rxtx.tests;

import gnu.io.CommPortIdentifier;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import junit.framework.TestCase;

/**
 * Main class bundling all single specialized test suites into a
 * overall complete one.
 */
public class RXTXCommDriverTest extends TestCase {

	private String fOldPropSerial;
	private String fOldPropParallel;
	private String fPathSep;

	public RXTXCommDriverTest(String testName) {
		super(testName);
	}

	public void setUp() {
		fPathSep = System.getProperty("path.separator", ":");
		fOldPropSerial = System.getProperty("gnu.io.rxtx.SerialPorts");
		fOldPropParallel = System.getProperty("gnu.io.rxtx.ParallelPorts");
	}

	public void tearDown() {
		System.setProperty("gnu.io.rxtx.SerialPorts", fOldPropSerial == null ? "" : fOldPropSerial);
		System.setProperty("gnu.io.rxtx.ParallelPorts", fOldPropParallel == null ? "" : fOldPropParallel);
	}

	/**
	 * Check that ports can be specified (i.e. removed) by means of a Java
	 * Property
	 */
	public void testRegisterSpecifiedPorts() throws Exception {
		// First, find all serial ports
		List serialPorts = new ArrayList();
		Enumeration e = CommPortIdentifier.getPortIdentifiers();
		while (e.hasMoreElements()) {
			CommPortIdentifier port = (CommPortIdentifier) e.nextElement();
			if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				serialPorts.add(port.getName());
			}
		}
		System.out.println(serialPorts);
		// Now, get rid of the first one
		StringBuffer buf = new StringBuffer();
		for (int i = 1; i < serialPorts.size(); i++) {
			buf.append(serialPorts.get(i));
			buf.append(fPathSep);
		}
		System.setProperty("gnu.io.rxtx.SerialPorts", buf.toString());
		e = CommPortIdentifier.getPortIdentifiers();
		int nNew = 0;
		while (e.hasMoreElements()) {
			CommPortIdentifier port = (CommPortIdentifier) e.nextElement();
			if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				nNew++;
				assertTrue("hasPort", serialPorts.contains(port.getName()));
			}
		}
		assertEquals("1 port removed", serialPorts.size() - 1, nNew);
	}

}
