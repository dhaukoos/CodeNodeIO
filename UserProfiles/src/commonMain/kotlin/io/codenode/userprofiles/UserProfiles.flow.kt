package io.codenode.userprofiles

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*
import io.codenode.userprofiles.iptypes.UserProfile

val userProfilesFlowGraph = flowGraph("UserProfiles", version = "1.0.0") {
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    val userProfileRepository = codeNode("UserProfileRepository") {
        position(445.75, 398.0)
        input("save", UserProfile::class)
        input("update", UserProfile::class)
        input("remove", UserProfile::class)
        output("result", String::class)
        output("error", String::class)
    }

    val userProfileCUD = codeNode("UserProfileCUD", nodeType = "SOURCE") {
        position(118.0, 394.25)
        output("save", UserProfile::class)
        output("update", UserProfile::class)
        output("remove", UserProfile::class)
    }

    val userProfilesDisplay = codeNode("UserProfilesDisplay", nodeType = "SINK") {
        position(799.5, 398.0)
        input("result", String::class)
        input("error", String::class)
    }

    userProfileCUD.output("save") connect userProfileRepository.input("save") withType "ip_userprofile"
    userProfileCUD.output("update") connect userProfileRepository.input("update") withType "ip_userprofile"
    userProfileCUD.output("remove") connect userProfileRepository.input("remove") withType "ip_userprofile"
    userProfileRepository.output("result") connect userProfilesDisplay.input("result")
    userProfileRepository.output("error") connect userProfilesDisplay.input("error")
}
