package io.codenode.userprofiles

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*
import io.codenode.userprofiles.processingLogic.*

val userProfilesFlowGraph = flowGraph("UserProfiles", version = "1.0.0") {
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    val userProfileRepository = codeNode("UserProfileRepository", nodeType = "GENERIC") {
        description = "UserProfile repository with save/update/remove operations"
        position(445.75, 398.0)
        input("save", Any::class)
        input("update", Any::class)
        input("remove", Any::class)
        output("result", Any::class)
        output("error", Any::class)
    }

    val userProfileCUD = codeNode("UserProfileCUD", nodeType = "GENERIC") {
        description = "Generic processing node with no inputs and 3 outputs"
        position(118.0, 394.25)
        output("save", Any::class)
        output("update", Any::class)
        output("remove", Any::class)
    }

    val userProfilesDisplay = codeNode("UserProfilesDisplay", nodeType = "GENERIC") {
        description = "Generic processing node with 2 inputs and no outputs"
        position(799.5, 398.0)
        input("result", Any::class)
        input("error", Any::class)
    }

    userProfileCUD.output("save") connect userProfileRepository.input("save") withType "ip_userprofile_8364e1e1"
    userProfileCUD.output("update") connect userProfileRepository.input("update") withType "ip_userprofile_8364e1e1"
    userProfileCUD.output("remove") connect userProfileRepository.input("remove") withType "ip_userprofile_8364e1e1"
    userProfileRepository.output("result") connect userProfilesDisplay.input("result")
    userProfileRepository.output("error") connect userProfilesDisplay.input("error")
}
