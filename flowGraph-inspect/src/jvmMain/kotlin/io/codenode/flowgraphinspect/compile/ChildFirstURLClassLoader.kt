/*
 * ChildFirstURLClassLoader - URLClassLoader that loads owned-package classes locally first
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import java.net.URL
import java.net.URLClassLoader

/**
 * URLClassLoader that overrides the default parent-first delegation for FQCNs whose
 * package matches an entry in [ownedPackages]. For owned classes, the local URLs are
 * tried FIRST so freshly-compiled session output supersedes any same-FQCN class on
 * the parent (launch-time) classloader. All other classes delegate to parent normally.
 *
 * Critical for hot-reload: without child-first delegation for owned packages, a
 * recompiled session class would still resolve to the launch-time JAR's stale identity.
 */
class ChildFirstURLClassLoader(
    urls: Array<URL>,
    parent: ClassLoader,
    val ownedPackages: Set<String>
) : URLClassLoader(urls, parent) {

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            // 1) If already loaded by this loader, return it.
            findLoadedClass(name)?.let {
                if (resolve) resolveClass(it)
                return it
            }

            if (isOwned(name)) {
                // 2) Owned package: try LOCAL URLs first, fall back to parent on miss.
                try {
                    val local = findClass(name)
                    if (resolve) resolveClass(local)
                    return local
                } catch (_: ClassNotFoundException) {
                    // not in local URLs — fall through to parent delegation
                }
            }

            // 3) Default delegation: parent first, then local (for non-owned classes
            //    we preserve the standard ClassLoader contract by delegating up).
            return super.loadClass(name, resolve)
        }
    }

    /** True iff [fqcn]'s package matches any prefix in [ownedPackages]. */
    private fun isOwned(fqcn: String): Boolean {
        val pkg = fqcn.substringBeforeLast('.', missingDelimiterValue = "")
        if (pkg.isEmpty()) return false
        return ownedPackages.any { owner ->
            pkg == owner || pkg.startsWith("$owner.")
        }
    }
}
