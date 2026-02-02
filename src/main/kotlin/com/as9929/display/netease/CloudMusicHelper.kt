package com.as9929.display.netease

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary

object CloudMusicHelper {

    private interface MyUser32 : StdCallLibrary {
        fun EnumWindows(lpEnumFunc: Callback, data: Pointer?): Boolean
        fun GetWindowThreadProcessId(hWnd: Pointer, lpdwProcessId: IntByReference): Int
        fun GetWindowTextW(hWnd: Pointer, lpString: CharArray, nMaxCount: Int): Int
        fun IsWindowVisible(hWnd: Pointer): Boolean
        fun GetWindowTextLengthW(hWnd: Pointer): Int

        interface Callback : com.sun.jna.Callback {
            fun invoke(hWnd: Pointer, data: Pointer?): Boolean
        }
    }

    private interface MyKernel32 : StdCallLibrary {
        fun OpenProcess(dwDesiredAccess: Int, bInheritHandle: Boolean, dwProcessId: Int): Pointer?
        fun CloseHandle(hObject: Pointer): Boolean
        fun QueryFullProcessImageNameW(hProcess: Pointer, dwFlags: Int, lpExeName: CharArray, lpdwSize: IntByReference): Boolean
    }

    // --- FIX: Use 'by lazy' to prevent loading on Linux/Mac ---
    // This code block will NOT run until you actually try to use 'user32'
    private val user32: MyUser32 by lazy {
        Native.load("user32", MyUser32::class.java)
    }

    private val kernel32: MyKernel32 by lazy {
        Native.load("kernel32", MyKernel32::class.java)
    }

    private const val PROCESS_QUERY_INFORMATION = 0x0400
    private const val PROCESS_VM_READ = 0x0010

    fun getCloudMusicTitle(): String? {
        // 1. Strict OS Check FIRST
        if (!System.getProperty("os.name").lowercase().contains("win")) {
            return null
        }

        // 2. Now it is safe to access 'user32', triggering the lazy load
        var foundTitle: String? = null
        val targetProcess = "cloudmusic.exe"

        try {
            user32.EnumWindows(object : MyUser32.Callback {
                override fun invoke(hWnd: Pointer, data: Pointer?): Boolean {
                    if (foundTitle != null) return false

                    if (user32.IsWindowVisible(hWnd)) {
                        val pidRef = IntByReference()
                        user32.GetWindowThreadProcessId(hWnd, pidRef)
                        val pid = pidRef.value

                        if (getPName(pid).equals(targetProcess, ignoreCase = true)) {
                            val title = getWText(hWnd)
                            if (title.isNotEmpty() && title != "DesktopLyrics" && title != "GDI+ Window") {
                                foundTitle = title
                                return false
                            }
                        }
                    }
                    return true
                }
            }, null)
        } catch (e: Throwable) {
            // Failsafe for any unexpected JNA errors
            return null
        }

        return foundTitle
    }

    private fun getWText(hWnd: Pointer): String {
        val length = user32.GetWindowTextLengthW(hWnd) + 1
        val buffer = CharArray(length)
        user32.GetWindowTextW(hWnd, buffer, length)
        return Native.toString(buffer)
    }

    private fun getPName(pid: Int): String {
        // Safe access to kernel32 via lazy load
        val hProcess = kernel32.OpenProcess(PROCESS_QUERY_INFORMATION or PROCESS_VM_READ, false, pid) ?: return ""
        try {
            val buffer = CharArray(1024)
            val size = IntByReference(buffer.size)
            if (kernel32.QueryFullProcessImageNameW(hProcess, 0, buffer, size)) {
                val fullPath = Native.toString(buffer)
                return fullPath.substringAfterLast('\\')
            }
        } finally {
            kernel32.CloseHandle(hProcess)
        }
        return ""
    }
}