package io.codenode.geolocations

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*
import io.codenode.geolocations.iptypes.GeoLocation

val geoLocationsFlowGraph = flowGraph("GeoLocations", version = "1.0.0") {
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    val geoLocationRepository = codeNode("GeoLocationRepository") {
        position(445.75, 398.0)
        input("save", GeoLocation::class)
        input("update", GeoLocation::class)
        input("remove", GeoLocation::class)
        output("result", String::class)
        output("error", String::class)
    }

    val geoLocationCUD = codeNode("GeoLocationCUD", nodeType = "SOURCE") {
        position(118.0, 394.25)
        output("save", GeoLocation::class)
        output("update", GeoLocation::class)
        output("remove", GeoLocation::class)
    }

    val geoLocationsDisplay = codeNode("GeoLocationsDisplay", nodeType = "SINK") {
        position(799.5, 398.0)
        input("result", String::class)
        input("error", String::class)
    }

    geoLocationCUD.output("save") connect geoLocationRepository.input("save") withType "ip_geolocation"
    geoLocationCUD.output("update") connect geoLocationRepository.input("update") withType "ip_geolocation"
    geoLocationCUD.output("remove") connect geoLocationRepository.input("remove") withType "ip_geolocation"
    geoLocationRepository.output("result") connect geoLocationsDisplay.input("result")
    geoLocationRepository.output("error") connect geoLocationsDisplay.input("error")
}
