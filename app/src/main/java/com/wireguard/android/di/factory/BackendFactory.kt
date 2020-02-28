/*
 * Copyright © 2020 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.di.factory

import android.content.Context
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.ModuleLoader
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller

object BackendFactory {
    fun getBackend(context: Context, moduleLoader: ModuleLoader, rootShell: RootShell, toolsInstaller: ToolsInstaller): Backend {
        var backend: Backend? = null
        var didStartRootShell = false
        if (!ModuleLoader.isModuleLoaded() && moduleLoader.moduleMightExist()) {
            try {
                rootShell.start()
                didStartRootShell = true
                moduleLoader.loadModule()
            } catch (_: Exception) {
            }
        }
        if (ModuleLoader.isModuleLoaded()) {
            try {
                if (!didStartRootShell)
                    rootShell.start()
                backend = WgQuickBackend(context, rootShell, toolsInstaller)
            } catch (_: Exception) {
            }
        }
        if (backend == null) {
            backend = GoBackend(context)
        }
        return backend
    }
}
