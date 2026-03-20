package io.codenode.edgeartfilter

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val edgeArtFilterFlowGraph = flowGraph("EdgeArtFilter", version = "1.0.0") {
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    val imagePicker = codeNode("ImagePicker", nodeType = "SOURCE") {
        position(100.0, 300.0)
        output("image", Any::class)        // ImageData
        output("original", Any::class)     // ImageData (fan-out copy for ColorOverlay)
    }

    val grayscaleTransformer = codeNode("GrayscaleTransformer") {
        position(350.0, 200.0)
        input("image", Any::class)         // ImageData
        output("result", Any::class)       // ImageData (grayscale)
    }

    val edgeDetector = codeNode("EdgeDetector") {
        position(600.0, 200.0)
        input("image", Any::class)         // ImageData (grayscale)
        output("edges", Any::class)        // ImageData (edge map)
    }

    val colorOverlay = codeNode("ColorOverlay") {
        position(600.0, 400.0)
        input("original", Any::class)      // ImageData (original color)
        input("edges", Any::class)         // ImageData (edge map)
        output("composite", Any::class)    // ImageData (neon edge composite)
    }

    val imageViewer = codeNode("ImageViewer", nodeType = "SINK") {
        position(850.0, 300.0)
        input("image", Any::class)         // ImageData (composite)
    }

    // Connections: ImagePicker fans out to GrayscaleTransformer and ColorOverlay
    imagePicker.output("image") connect grayscaleTransformer.input("image") withType "ip_imagedata"
    imagePicker.output("original") connect colorOverlay.input("original") withType "ip_imagedata"
    grayscaleTransformer.output("result") connect edgeDetector.input("image") withType "ip_imagedata"
    edgeDetector.output("edges") connect colorOverlay.input("edges") withType "ip_imagedata"
    colorOverlay.output("composite") connect imageViewer.input("image") withType "ip_imagedata"
}
