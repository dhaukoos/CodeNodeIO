/*
 * HttpResponse - IP type for HTTP response data
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.models

data class HttpResponse(
    val statusCode: Int,
    val body: String
)
