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
import gnu.io.NoSuchPortException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import junit.framework.TestCase;

public class CommPortIdentifierTest extends TestCase {

	public CommPortIdentifierTest(String testName) {
		super(testName);
	}

	public List getPortIdentifiers() {
		Enumeration e = CommPortIdentifier.getPortIdentifiers();
		List l = new ArrayList();
		while (e.hasMoreElements()) {
			l.add(e.nextElement());
		}
		return l;
	}

	public void testGetPortIdentifiers() throws Exception {
		List l = getPortIdentifiers();
		assertFalse("has ports", l.isEmpty());
	}

	public void testGetPortIdentifier() throws Exception {
		List l = getPortIdentifiers();
		CommPortIdentifier first = (CommPortIdentifier) l.get(0);
		CommPortIdentifier last = (CommPortIdentifier) l.get(l.size() - 1);
		// first find by name
		CommPortIdentifier p = CommPortIdentifier.getPortIdentifier(first.getName());
		assertEquals("first found", p, first);
		p = CommPortIdentifier.getPortIdentifier(last.getName());
		assertEquals("last found", p, last);
		// now the non-existent case
		boolean exceptionThrown = false;
		try {
			p = CommPortIdentifier.getPortIdentifier("wuzziwuzz");
		} catch (NoSuchPortException e) {
			exceptionThrown = true;
		}
		assertTrue("invalid port", exceptionThrown);
	}

}
