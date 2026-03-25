package io.codenode.addresses

import io.codenode.addresses.iptypes.Address
import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val addressesFlowGraph = flowGraph("Addresses", version = "1.0.0") {
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    val addressRepository = codeNode("AddressRepository") {
        position(445.75, 398.0)
        input("save", Address::class)
        input("update", Address::class)
        input("remove", Address::class)
        output("result", String::class)
        output("error", String::class)
    }

    val addressCUD = codeNode("AddressCUD", nodeType = "SOURCE") {
        position(118.0, 394.25)
        output("save", Address::class)
        output("update", Address::class)
        output("remove", Address::class)
    }

    val addressesDisplay = codeNode("AddressesDisplay", nodeType = "SINK") {
        position(799.5, 398.0)
        input("result", String::class)
        input("error", String::class)
    }

    addressCUD.output("save") connect addressRepository.input("save") withType "ip_address"
    addressCUD.output("update") connect addressRepository.input("update") withType "ip_address"
    addressCUD.output("remove") connect addressRepository.input("remove") withType "ip_address"
    addressRepository.output("result") connect addressesDisplay.input("result")
    addressRepository.output("error") connect addressesDisplay.input("error")
}
