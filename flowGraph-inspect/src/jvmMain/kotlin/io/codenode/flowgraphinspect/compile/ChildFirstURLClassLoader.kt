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
        throw NotImplementedError("T025 will implement ChildFirstURLClassLoader.loadClass")
    }
}
