package com.h0tk3y.kotlin.staticObjectNotation.demo.demoPlugins

import com.h0tk3y.kotlin.staticObjectNotation.analysis.*
import com.h0tk3y.kotlin.staticObjectNotation.analysis.FunctionSemantics.AccessAndConfigure.ReturnType.UNIT
import com.h0tk3y.kotlin.staticObjectNotation.analysis.ParameterSemantics.StoreValueInProperty
import com.h0tk3y.kotlin.staticObjectNotation.demo.boolean
import com.h0tk3y.kotlin.staticObjectNotation.demo.string
import com.h0tk3y.kotlin.staticObjectNotation.demo.typeRef

fun demoSchema(): AnalysisSchema {
    val topLevelScopeRef = typeRef<TopLevelScope>()
    val pluginsBlockRef = typeRef<PluginsBlock>()
    val pluginDefinitionRef = typeRef<PluginDefinition>()
    
    val topLevelScopePlugins = DataProperty("plugins", pluginsBlockRef, isReadOnly = true, false)
    val pluginDefinitionId = DataProperty("id", string, isReadOnly = true, false)
    val pluginDefinitionVersion = DataProperty("version", string, isReadOnly = true, false)
    val pluginDefinitionApply = DataProperty("apply", boolean, isReadOnly = true, false)
    
    val topLevelScope = DataType.DataClass(
        TopLevelScope::class,
        properties = listOf(topLevelScopePlugins),
        memberFunctions = listOf(
            DataMemberFunction(
                topLevelScopeRef, "plugins", 
                parameters = emptyList(), 
                semantics = FunctionSemantics.AccessAndConfigure(
                    ConfigureAccessor.Property(topLevelScopeRef, topLevelScopePlugins), UNIT
                )
            )
        ),
        constructorSignatures = emptyList()
    )
    
    val pluginsBlock = DataType.DataClass(
        PluginsBlock::class,
        properties = emptyList(),
        memberFunctions = listOf(
            DataMemberFunction(
                pluginsBlockRef, "id",
                parameters = listOf(
                    DataParameter("identifier", string, isDefault = false, semantics = StoreValueInProperty(pluginDefinitionId))
                ),
                semantics = FunctionSemantics.AddAndConfigure(pluginDefinitionRef)
            )
        ),
        constructorSignatures = emptyList()
    )
    
    val pluginDefinition = DataType.DataClass(
        PluginDefinition::class,
        properties = listOf(
            pluginDefinitionId, pluginDefinitionVersion, pluginDefinitionApply
        ),
        memberFunctions = listOf(
            DataBuilderFunction(
                pluginDefinitionRef, pluginDefinitionVersion.name,
                DataParameter("newValue", string, isDefault = false, StoreValueInProperty(pluginDefinitionVersion))
            ),
            DataBuilderFunction(
                pluginDefinitionRef, pluginDefinitionApply.name,
                DataParameter("newValue", boolean, isDefault = false, StoreValueInProperty(pluginDefinitionApply))
            ),
        ),
        constructorSignatures = emptyList()
    )
    
    return AnalysisSchema(
        topLevelScope, 
        listOf(topLevelScope, pluginsBlock, pluginDefinition).associateBy { FqName.parse(it.kClass.java.name) },
        emptyMap(),
        emptyMap(),
        emptySet()
    )
}