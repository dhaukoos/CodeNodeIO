package io.codenode.weatherforecast

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*
import io.codenode.weatherforecast.iptypes.ChartData
import io.codenode.weatherforecast.iptypes.Coordinates
import io.codenode.weatherforecast.iptypes.ForecastData
import io.codenode.weatherforecast.iptypes.ForecastDisplayList
import io.codenode.weatherforecast.iptypes.HttpResponse

val weatherForecastFlowGraph = flowGraph("WeatherForecast", version = "1.0.0") {
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    val triggerSource = codeNode("TriggerSource", nodeType = "SOURCE") {
        position(100.0, 300.0)
        output("coordinates", Coordinates::class)
        config("_codeNodeClass", "io.codenode.weatherforecast.nodes.TriggerSourceCodeNode")
        config("_codeNodeDefinition", "true")
        config("_genericType", "in0out1")
    }

    val httpFetcher = codeNode("HttpFetcher") {
        position(350.0, 300.0)
        input("coordinates", Coordinates::class)
        output("response", HttpResponse::class)
        config("_codeNodeClass", "io.codenode.weatherforecast.nodes.HttpFetcherCodeNode")
        config("_codeNodeDefinition", "true")
        config("_genericType", "in1out1")
    }

    val jsonParser = codeNode("JsonParser") {
        position(600.0, 300.0)
        input("response", HttpResponse::class)
        output("forecastData", ForecastData::class)
        config("_codeNodeClass", "io.codenode.weatherforecast.nodes.JsonParserCodeNode")
        config("_codeNodeDefinition", "true")
        config("_genericType", "in1out1")
    }

    val dataMapper = codeNode("DataMapper") {
        position(850.0, 300.0)
        input("forecastData", ForecastData::class)
        output("displayList", ForecastDisplayList::class)
        output("chartData", ChartData::class)
        config("_codeNodeClass", "io.codenode.weatherforecast.nodes.DataMapperCodeNode")
        config("_codeNodeDefinition", "true")
        config("_genericType", "in1out2")
    }

    val forecastDisplay = codeNode("ForecastDisplay", nodeType = "SINK") {
        position(1100.0, 300.0)
        input("displayList", ForecastDisplayList::class)
        input("chartData", ChartData::class)
        config("_codeNodeClass", "io.codenode.weatherforecast.nodes.ForecastDisplayCodeNode")
        config("_codeNodeDefinition", "true")
        config("_genericType", "in2anyout0")
    }

    // Connections with custom IP types
    triggerSource.output("coordinates") connect httpFetcher.input("coordinates") withType "ip_coordinates"
    httpFetcher.output("response") connect jsonParser.input("response") withType "ip_httpresponse"
    jsonParser.output("forecastData") connect dataMapper.input("forecastData") withType "ip_forecastdata"
    dataMapper.output("displayList") connect forecastDisplay.input("displayList") withType "ip_forecastdisplaylist"
    dataMapper.output("chartData") connect forecastDisplay.input("chartData") withType "ip_forecastchartdata"
}
