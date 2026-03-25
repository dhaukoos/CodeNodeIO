/*
 * HttpResponse - Custom IP Type
 * @IPType
 * @TypeName HttpResponse
 * @TypeId ip_httpresponse
 * @Color rgb(255, 152, 0)
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.iptypes

data class HttpResponse(
    val statusCode: Int,
    val body: String
)
