/*
 * UIComposableParser - Parses a Compose UI file to extract ViewModel interface
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.parser

class UIComposableParser {

    private val packagePattern = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
    private val composableFunPattern = Regex(
        """fun\s+(\w+)\s*\(\s*viewModel\s*:\s*(\w+)"""
    )
    private val collectAsStatePattern = Regex(
        """viewModel\.(\w+)\.collectAsState\(\)"""
    )
    private val viewModelCallPattern = Regex(
        """viewModel\.(\w+)\("""
    )
    private val importPattern = Regex(
        """^import\s+([\w.]+)""", RegexOption.MULTILINE
    )
    private val valTypePattern = Regex(
        """val\s+(\w+)\s+by\s+viewModel\.\w+\.collectAsState\(\)"""
    )
    private val toTypeOrNullPattern = Regex(
        """(\w+)\.to(\w+)OrNull\(\)"""
    )

    private val lifecycleMethods = setOf("start", "stop", "pause", "resume", "reset")
    private val composeMethods = setOf("collectAsState", "collectAsLazyPagingItems")

    fun parse(fileContent: String): UIFBPParseResult {
        val packageMatch = packagePattern.find(fileContent)
            ?: return UIFBPParseResult(errorMessage = "No package declaration found")
        val uiPackage = packageMatch.groupValues[1]
        val basePackage = if (uiPackage.endsWith(".userInterface")) {
            uiPackage.removeSuffix(".userInterface")
        } else {
            uiPackage
        }

        val composableMatch = composableFunPattern.find(fileContent)
            ?: return UIFBPParseResult(errorMessage = "No composable function with a viewModel parameter found")
        val moduleName = composableMatch.groupValues[1]
        val viewModelTypeName = composableMatch.groupValues[2]

        val sinkInputs = extractSinkInputs(fileContent)
        val sourceOutputs = extractSourceOutputs(fileContent)
        val ipTypeImports = extractIPTypeImports(fileContent, basePackage)

        return UIFBPParseResult(
            spec = UIFBPSpec(
                moduleName = moduleName,
                viewModelTypeName = viewModelTypeName,
                packageName = basePackage,
                sourceOutputs = sourceOutputs,
                sinkInputs = sinkInputs,
                ipTypeImports = ipTypeImports
            )
        )
    }

    private fun extractSinkInputs(fileContent: String): List<PortInfo> {
        val sinkProperties = collectAsStatePattern.findAll(fileContent)
            .map { it.groupValues[1] }
            .distinct()
            .toList()

        return sinkProperties.map { propName ->
            val typeName = inferSinkType(fileContent, propName)
            val isNullable = inferSinkNullability(fileContent, propName)
            PortInfo(name = propName, typeName = typeName, isNullable = isNullable)
        }
    }

    private fun inferSinkType(fileContent: String, propName: String): String {
        val typedValPattern = Regex("""val\s+$propName\s*:\s*([\w<>?,\s]+)\s+by""")
        val typedMatch = typedValPattern.find(fileContent)
        if (typedMatch != null) {
            return cleanTypeName(typedMatch.groupValues[1])
        }

        val paramTypePattern = Regex("""$propName\s*:\s*(\w+)\??""")
        val paramMatch = paramTypePattern.find(fileContent)
        if (paramMatch != null) {
            val candidate = paramMatch.groupValues[1]
            if (candidate[0].isUpperCase() && candidate != "Modifier" && candidate != "Unit") {
                return candidate
            }
        }

        val importForType = findImportForUsage(fileContent, propName)
        if (importForType != null) {
            return importForType.substringAfterLast(".")
        }

        return "Any"
    }

    private fun inferSinkNullability(fileContent: String, propName: String): Boolean {
        val nullCheckPattern = Regex("""$propName\s*==\s*null|$propName\s*!=\s*null|$propName\?""")
        return nullCheckPattern.containsMatchIn(fileContent)
    }

    private fun extractSourceOutputs(fileContent: String): List<PortInfo> {
        val methodCalls = viewModelCallPattern.findAll(fileContent)
            .map { it.groupValues[1] }
            .filter { it !in lifecycleMethods && it !in composeMethods }
            .distinct()
            .toList()

        if (methodCalls.isEmpty()) return emptyList()

        val emitMethod = methodCalls.first()
        return extractEmitParameters(fileContent, emitMethod)
    }

    private fun extractEmitParameters(fileContent: String, methodName: String): List<PortInfo> {
        val callPattern = Regex("""viewModel\.$methodName\(([^)]+)\)""")
        val callMatch = callPattern.find(fileContent) ?: return emptyList()
        val args = callMatch.groupValues[1].split(",").map { it.trim() }

        return args.map { argExpr ->
            val argName = argExpr.trim()
            val typeName = inferArgumentType(fileContent, argName)
            PortInfo(name = argName, typeName = typeName)
        }
    }

    private fun inferArgumentType(fileContent: String, argName: String): String {
        val explicitTypePattern = Regex("""val\s+$argName\s*:\s*(\w+\??)""")
        val explicitMatch = explicitTypePattern.find(fileContent)
        if (explicitMatch != null) {
            return explicitMatch.groupValues[1].removeSuffix("?")
        }

        val conversionPattern = Regex("""\w+\.to(\w+)OrNull\(\)[\s\S]{0,50}val\s+$argName|val\s+$argName\s*=\s*\w+\.to(\w+)OrNull\(\)""")
        val conversionMatch = conversionPattern.find(fileContent)
        if (conversionMatch != null) {
            val typeName = conversionMatch.groupValues[1].ifEmpty { conversionMatch.groupValues[2] }
            return typeName
        }

        val assignPattern = Regex("""val\s+$argName\s*=\s*(\w+)\.to(\w+)OrNull""")
        val assignMatch = assignPattern.find(fileContent)
        if (assignMatch != null) {
            return assignMatch.groupValues[2]
        }

        return "Any"
    }

    private fun extractIPTypeImports(fileContent: String, basePackage: String): List<String> {
        return importPattern.findAll(fileContent)
            .map { it.groupValues[1] }
            .filter { it.contains(".iptypes.") }
            .toList()
    }

    private fun findImportForUsage(fileContent: String, typeName: String): String? {
        val imports = importPattern.findAll(fileContent)
            .map { it.groupValues[1] }
            .toList()

        val capitalizedName = typeName.replaceFirstChar { it.uppercase() }
        return imports.firstOrNull { it.endsWith(".$capitalizedName") }
            ?: imports.firstOrNull { it.endsWith(".$typeName") }
    }

    private fun cleanTypeName(rawType: String): String {
        return rawType.trim()
            .removeSuffix("?")
            .removePrefix("StateFlow<")
            .removeSuffix(">")
            .trim()
    }
}
