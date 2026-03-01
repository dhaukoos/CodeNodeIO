package io.codenode.repositorypattern

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*
import io.codenode.repositorypattern.processingLogic.*

val repositoryPatternFlowGraph = flowGraph("RepositoryPattern", version = "1.0.0") {
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    val repoInputs = codeNode("RepoInputs", nodeType = "GENERIC") {
        position(14.125, 189.0)
        output("save", Any::class)
        output("update", Any::class)
        output("remove", Any::class)
    }

    val genericRepository = codeNode("GenericRepository", nodeType = "GENERIC") {
        position(266.6171875, 189.0)
        input("insert", Any::class)
        input("update", Any::class)
        input("delete", Any::class)
        output("getAll", Any::class)
    }

    val filterSort = codeNode("FilterSort", nodeType = "GENERIC") {
        position(510.546875, 77.14845275878906)
        input("filterBy", Any::class)
        input("sortBy", Any::class)
        input("observeAll", Any::class)
        output("filteredList", Any::class)
    }

    val selectInputs = codeNode("SelectInputs", nodeType = "GENERIC") {
        position(17.382843017578125, 46.87501525878906)
        output("filterBy", Any::class)
        output("sortBy", Any::class)
    }

    val repoDisplay = codeNode("RepoDisplay", nodeType = "GENERIC") {
        position(770.7265625, 77.390625)
        input("filteredList", Any::class)
    }

    repoInputs.output("save") connect genericRepository.input("insert")
    repoInputs.output("update") connect genericRepository.input("update")
    repoInputs.output("remove") connect genericRepository.input("delete")
    genericRepository.output("getAll") connect filterSort.input("observeAll")
    selectInputs.output("sortBy") connect filterSort.input("sortBy")
    selectInputs.output("filterBy") connect filterSort.input("filterBy")
    filterSort.output("filteredList") connect repoDisplay.input("filteredList")
}
