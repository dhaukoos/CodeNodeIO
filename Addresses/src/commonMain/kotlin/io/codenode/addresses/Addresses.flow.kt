package io.codenode.addresses

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val addressesFlowGraph = flowGraph("Addresses", version = "1.0.0") {
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    val addressRepository = codeNode("AddressRepository", nodeType = "GENERIC") {
        position(445.75, 398.0)
        input("save", Any::class)
        input("update", Any::class)
        input("remove", Any::class)
        output("result", Any::class)
        output("error", Any::class)
    }

    val addressCUD = codeNode("AddressCUD", nodeType = "GENERIC") {
        position(118.0, 394.25)
        output("save", Any::class)
        output("update", Any::class)
        output("remove", Any::class)
    }

    val addressesDisplay = codeNode("AddressesDisplay", nodeType = "GENERIC") {
        position(799.5, 398.0)
        input("result", Any::class)
        input("error", Any::class)
    }

    addressCUD.output("save") connect addressRepository.input("save")
    addressCUD.output("update") connect addressRepository.input("update")
    addressCUD.output("remove") connect addressRepository.input("remove")
    addressRepository.output("result") connect addressesDisplay.input("result")
    addressRepository.output("error") connect addressesDisplay.input("error")
}
