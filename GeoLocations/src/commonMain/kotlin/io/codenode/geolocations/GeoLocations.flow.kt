package io.codenode.geolocations

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val geoLocationsFlowGraph = flowGraph("GeoLocations", version = "1.0.0") {
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    val geoLocationRepository = codeNode("GeoLocationRepository", nodeType = "GENERIC") {
        position(445.75, 398.0)
        input("save", Any::class)
        input("update", Any::class)
        input("remove", Any::class)
        output("result", Any::class)
        output("error", Any::class)
    }

    val geoLocationCUD = codeNode("GeoLocationCUD", nodeType = "GENERIC") {
        position(118.0, 394.25)
        output("save", Any::class)
        output("update", Any::class)
        output("remove", Any::class)
    }

    val geoLocationsDisplay = codeNode("GeoLocationsDisplay", nodeType = "GENERIC") {
        position(799.5, 398.0)
        input("result", Any::class)
        input("error", Any::class)
    }

    geoLocationCUD.output("save") connect geoLocationRepository.input("save")
    geoLocationCUD.output("update") connect geoLocationRepository.input("update")
    geoLocationCUD.output("remove") connect geoLocationRepository.input("remove")
    geoLocationRepository.output("result") connect geoLocationsDisplay.input("result")
    geoLocationRepository.output("error") connect geoLocationsDisplay.input("error")
}
