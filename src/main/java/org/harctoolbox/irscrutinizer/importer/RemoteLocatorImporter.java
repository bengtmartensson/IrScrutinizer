/*
Copyright (C) 2021 Bengt Martensson.

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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.harctoolbox.remotelocator.Girrable;
import org.harctoolbox.remotelocator.NotFoundException;
import org.harctoolbox.remotelocator.RemoteDatabase;
import org.harctoolbox.remotelocator.RemoteLink;
import org.xml.sax.SAXException;

public final class RemoteLocatorImporter extends DatabaseImporter implements IRemoteSetImporter {

    public static final String CATALOG = "http://www.harctoolbox.org/downloads/remotelocator";
    // public static final String CATALOG = "http://www.harctoolbox.org/downloads/remotelocator.html";
    public static final String CATALOG_URL = CATALOG + ".xml";
    public static final String CATALOG_HTML = CATALOG + ".html";

    private static String catalog = CATALOG_URL;

    public static void setCatalog(String newCatalog) {
        catalog = newCatalog;
    }

    public static URI getHomeUri() {
        return URI.create(CATALOG_HTML);
    }

    private final boolean verbose;
    private RemoteDatabase remoteDatabase;
    private Remote remote;
    private RemoteLink remoteLink;
    private RemoteSet remoteSet;

    public RemoteLocatorImporter(boolean verbose) throws IOException {
        super(catalog);
        this.verbose = verbose;
        remoteDatabase = null;
        clear();
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void assertDatabase() throws IOException, SAXException {
        if (remoteDatabase == null) {
            try {
                remoteDatabase = new RemoteDatabase(catalog);
                if (verbose)
                    System.err.println("Successfully loaded RemoteLocator catalog from " + catalog);
            } catch (RemoteDatabase.FormatVersionMismatchException ex) {
                throw new IOException(ex.getLocalizedMessage());
            }
        }
    }

    public void clear() {
        remote = null;
        remoteLink = null;
        remoteSet = null;
    }

    public String[] getManufacturers() throws IOException, SAXException {
        assertDatabase();
        List<String> manufacturer = remoteDatabase.getManufacturers();
        return manufacturer.toArray(new String[0]);
    }

    @Override
    public RemoteSet getRemoteSet() {
        return remoteSet;
    }

    public boolean isLoadable() {
        return remoteLink != null ? remoteLink.isLoadable() : null;
    }

    @Override
    public String getFormatName() {
        return "RemoteLocator";
    }

    @Override
    public Remote.MetaData getMetaData() {
        return remote != null ? remote.getMetaData() : null;
    }

    public String[] getDeviceClasses(String manufacturer) throws NotFoundException {
        List<String> deviceClasses = remoteDatabase.getDeviceTypes(manufacturer);
        return deviceClasses.toArray(new String[0]);
    }

    public String[] getRemotes(String manufacturer, String deviceType) throws NotFoundException {
        List<String> remotes = remoteDatabase.getRemotes(manufacturer, deviceType);
        return remotes.toArray(new String[0]);
    }

    public void load(String manufacturer, String deviceType, String remoteName) throws NotFoundException {
        remoteLink = remoteDatabase.get(manufacturer, deviceType, remoteName);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void loadRemote() throws IOException, Girrable.NotGirrableException, NotFoundException {
        if (remoteLink == null)
            throw new ThisCannotHappenException();
        if (remoteLink.isLoadable()) {
            remote = remoteLink.getRemote();
            if (verbose)
                System.err.println("Remote successfully loaded from " + remoteLink.getUrl());
            remoteSet = new RemoteSet(null, null, remote);
        } else {
            remote = null;
            remoteSet = null;
        }
    }

    public URL getRemoteURL() {
        return remoteLink != null ? remoteLink.getUrl() : null;
    }

    public String getkind() {
        return remoteLink != null ? remoteLink.getKind().toString() : "";
    }
}
