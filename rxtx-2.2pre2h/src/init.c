
/* init.c is used for compiling dll's with lcc.  More info in Makefile.lcc */

#ifdef _WIN32
#include <windows.h>
#include <jni.h>

/**
 * Lcc requuires that there should be LibraryMain function and seeks
 * by default for LibMain. This might be someway not usefull for jni
 * interface it satisfies lcc linker.
 */
BOOL WINAPI __declspec(dllexport) LibMain(HINSTANCE hDLLInst, DWORD fdwReason, LPVOID lpvReserved) {
    return TRUE;
}

/**
 * This is the standart implementation of Java 2 OnLoad and OnUnload native
 * library calls. This template defines them empty functions
 *
 * Now in SerialImp.c
 */
/*
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)   { return JNI_VERSION_1_2; }
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {}
*/

#endif
