package com.h0tk3y.kotlin.staticObjectNotation.language

import com.h0tk3y.kotlin.staticObjectNotation.analysis.DataType
import kotlinx.ast.common.ast.Ast

sealed interface LanguageTreeElement {
    val originAst: Ast
} 

sealed interface FunctionArgument : LanguageTreeElement {
    sealed interface ValueArgument : FunctionArgument {
        val expr: Expr
    }
    
    data class Positional(override val expr: Expr, override val originAst: Ast) : ValueArgument
    data class Named(val name: String, override val expr: Expr, override val originAst: Ast) : ValueArgument
    data class Lambda(val block: Block, override val originAst: Ast) : FunctionArgument
}

sealed interface DataStatement : LanguageTreeElement
sealed interface Expr : DataStatement

data class Block(val statements: List<DataStatement>, override val originAst: Ast) : LanguageTreeElement

data class Import(val name: AccessChain, override val originAst: Ast) : LanguageTreeElement 

data class AccessChain(val nameParts: List<String>, val originAst: Ast)

data class PropertyAccess(val receiver: Expr?, val name: String, override val originAst: Ast) : Expr
data class FunctionCall(val receiver: Expr?, val name: String, val args: List<FunctionArgument>, override val originAst: Ast) : Expr
data class Assignment(val lhs: PropertyAccess, val rhs: Expr, override val originAst: Ast) : DataStatement
data class LocalValue(val name: String, val rhs: Expr, override val originAst: Ast) : DataStatement

sealed interface Literal<T : Any> : Expr {
    val value: T
    val type: DataType.ConstantType<T>

    data class StringLiteral(
        override val value: String, override val originAst: Ast
    ) : Literal<String> {
        override val type: DataType.StringDataType get() = DataType.StringDataType
    }

    data class IntLiteral(
        override val value: Int, override val originAst: Ast
    ) : Literal<Int> {
        override val type: DataType.IntDataType get() = DataType.IntDataType
    }

    data class LongLiteral(
        override val value: Long, override val originAst: Ast
    ) : Literal<Long> {
        override val type: DataType.LongDataType get() = DataType.LongDataType
    }

    data class BooleanLiteral(
        override val value: Boolean, override val originAst: Ast
    ) : Literal<Boolean> {
        override val type: DataType.BooleanDataType get() = DataType.BooleanDataType
    }
}

data class Null(override val originAst: Ast) : Expr
data class This(override val originAst: Ast) : Expr
