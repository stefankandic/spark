/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.catalyst.analysis

import org.apache.spark.sql.catalyst.expressions.{Cast, Expression, Literal}
import org.apache.spark.sql.catalyst.plans.logical.{AddColumns, AlterColumn, AlterViewAs, AppendData, ColumnDefinition, CreateView, InsertIntoStatement, LogicalPlan, QualifiedColType, ReplaceColumns, V1CreatePlan, V1WritePlan, V2CreateTablePlan}
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.types.{DataType, StringType}

/**
 * Resolves default string types in DDL commands. For DML commands, the default string type is
 * determined by the session's default string type. For DDL, the default string type is the
 * default type of the object (table -> schema -> catalog). However, this is not implemented yet.
 * So, we will just use UTF8_BINARY for now.
 */
object ResolveDefaultStringTypes extends Rule[LogicalPlan] {
  def apply(plan: LogicalPlan): LogicalPlan = {
    val newPlan = if (isDDLCommand(plan)) {
      transformDDL(plan)
    } else {
      val newType = stringTypeForDMLCommand
      transformPlan(plan, newType)
    }

    if (newPlan != plan) {
      val x = 3
    }
    newPlan
  }

  private def isDefaultSessionCollationUsed: Boolean = SQLConf.get.defaultStringType == StringType

  /**
   * Returns the default string type that should be used in a given DDL command (for now always
   * UTF8_BINARY).
   */
  private def stringTypeForDDLCommand(table: LogicalPlan): StringType =
    StringType("UTF8_BINARY")

  /** Returns the default string type that should be used in DML commands. */
  private def stringTypeForDMLCommand: StringType =
    if (isDefaultSessionCollationUsed) {
      StringType("UTF8_BINARY")
    } else {
      SQLConf.get.defaultStringType
    }

  private def isDDLCommand(plan: LogicalPlan): Boolean = plan exists {
    case _: AddColumns | _: ReplaceColumns | _: AlterColumn => true
    case _ => isCreateOrAlterPlan(plan) || isInsertInCtas(plan)
  }

  private def isCreateOrAlterPlan(plan: LogicalPlan): Boolean = plan match {
    case _: V2CreateTablePlan | _: CreateView | _: AlterViewAs | _: V1CreatePlan => true
    case _ => false
  }

  private def isInsertInCtas(plan: LogicalPlan): Boolean = plan match {
    case v1: V1WritePlan => v1.isCtas
    case append: AppendData => append.isCtas
    case insert: InsertIntoStatement => insert.isCtas
    case _ => false
  }

  private def transformDDL(plan: LogicalPlan): LogicalPlan = {
    val newType = stringTypeForDDLCommand(plan)
    if (newType == StringType) {
      return plan
    }

    plan resolveOperators {
      case oldPlan if isCreateOrAlterPlan(oldPlan) =>
        transformPlan(oldPlan, newType)

      case addCols: AddColumns =>
        addCols.copy(columnsToAdd = replaceColumnTypes(addCols.columnsToAdd, newType))

      case replaceCols: ReplaceColumns =>
        replaceCols.copy(columnsToAdd = replaceColumnTypes(replaceCols.columnsToAdd, newType))

      case alter: AlterColumn if alter.dataType.isDefined &&
          hasDefaultStringType(alter.dataType.get) =>
        alter.copy(dataType = Some(replaceDefaultStringType(alter.dataType.get, newType)))
    }
  }

  /**
   * Transforms the given plan, by transforming all expressions in its operators to use the given
   * new type instead of the default string type.
   */
  private def transformPlan(plan: LogicalPlan, newType: StringType): LogicalPlan = {
    plan resolveOperators { operator =>
      operator resolveExpressionsUp { expression =>
        transformExpression(expression, newType)
      }
    }
  }

  /**
   * Transforms the given expression, by changing all default string types to the given new type.
   */
  private def transformExpression(expression: Expression, newType: StringType): Expression = {
    expression match {
      case columnDef: ColumnDefinition if hasDefaultStringType(columnDef.dataType) =>
        columnDef.copy(dataType = replaceDefaultStringType(columnDef.dataType, newType))

      case cast: Cast if hasDefaultStringType(cast.dataType) =>
        cast.copy(dataType = replaceDefaultStringType(cast.dataType, newType))

      case Literal(value, dt) if hasDefaultStringType(dt) =>
        Literal(value, replaceDefaultStringType(dt, newType))

      case other => other
    }
  }

  private def hasDefaultStringType(dataType: DataType): Boolean =
    dataType.existsRecursively(isDefaultStringType)

  private def isDefaultStringType(dataType: DataType): Boolean = {
    dataType match {
      case st: StringType =>
        // should only return true for StringType object and not StringType("UTF8_BINARY")
        st.eq(StringType)
      case _ => false
    }
  }

  private def replaceDefaultStringType(dataType: DataType, newType: StringType): DataType = {
    dataType.transformRecursively {
      case currentType: StringType if isDefaultStringType(currentType) =>
        newType
    }
  }

  private def replaceColumnTypes(
      colTypes: Seq[QualifiedColType],
      newType: StringType): Seq[QualifiedColType] = {
    colTypes.map {
      case colWithDefault if hasDefaultStringType(colWithDefault.dataType) =>
        val replaced = replaceDefaultStringType(colWithDefault.dataType, newType)
        colWithDefault.copy(dataType = replaced)

      case col => col
    }
  }
}
