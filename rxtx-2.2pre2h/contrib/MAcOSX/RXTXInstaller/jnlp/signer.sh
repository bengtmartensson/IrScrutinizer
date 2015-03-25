#!/bin/bash
jar cf libnative.lib.jar libAuthKit.jnilib
jarsigner  -storepass <put_your_data_here> libnative.lib.jar <put_your_data_here>
jarsigner  -storepass <put_your_data_here> RXTXInstaller.jar <put_your_data_here>
