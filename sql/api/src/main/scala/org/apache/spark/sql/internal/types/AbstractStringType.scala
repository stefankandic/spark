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

package org.apache.spark.sql.internal.types

import org.apache.spark.sql.internal.SqlApiConf
import org.apache.spark.sql.types.{AbstractDataType, DataType, IndeterminateStringType, StringType}

/**
 * AbstractStringType is an abstract class for StringType with collation support.
 */
abstract class AbstractStringType extends AbstractDataType {
  override private[sql] def defaultConcreteType: DataType = SqlApiConf.get.defaultStringType
  override private[sql] def simpleString: String = "string"
}

/**
 * Use StringTypeBinary for expressions supporting only binary collation.
 */
case object StringTypeBinary extends AbstractStringType {
  override private[sql] def acceptsType(other: DataType): Boolean =
    other.isInstanceOf[StringType] && other.asInstanceOf[StringType].supportsBinaryEquality
}

/**
 * Use StringTypeBinaryLcase for expressions supporting only binary and lowercase collation.
 */
case object StringTypeBinaryLcase extends AbstractStringType {
  override private[sql] def acceptsType(other: DataType): Boolean =
    other.isInstanceOf[StringType] && (other.asInstanceOf[StringType].supportsBinaryEquality ||
      other.asInstanceOf[StringType].isUTF8LcaseCollation)
}

/**
 * Use StringTypeWithCaseAccentSensitivity for expressions supporting all collation types (binary
 * and ICU) but limited to using case and accent sensitivity specifiers.
 */
case object StringTypeWithCaseAccentSensitivity extends AbstractStringType {
  override private[sql] def acceptsType(other: DataType): Boolean = other.isInstanceOf[StringType]
}

/**
 * Use StringTypeNonCSAICollation for expressions supporting all possible collation types except
 * CS_AI collation types.
 */
case object StringTypeNonCSAICollation extends AbstractStringType {
  override private[sql] def acceptsType(other: DataType): Boolean = {
    true
//    other.isInstanceOf[StringType] && other.asInstanceOf[StringType].isNonCSAI
  }
}

case class MyAbstractStringType(collationType: CollationType, supportsIndeterminate: Boolean)
  extends AbstractDataType {

  override private[sql] def defaultConcreteType: DataType = SqlApiConf.get.defaultStringType
  override private[sql] def simpleString: String = "string"

  override private[sql] def acceptsType(other: DataType): Boolean = {
    other match {
      case IndeterminateStringType => supportsIndeterminate
      case StringType => collationType.acceptsType(other.asInstanceOf[StringType])
      case _ => false
    }
  }
}

sealed trait CollationType {
  def acceptsType(other: StringType): Boolean
}

object CollationType {

  /**
   * Use for expressions supporting only binary collation.
   */
  case object BinaryType extends CollationType {
    override def acceptsType(other: StringType): Boolean = other.supportsBinaryEquality
  }

  /**
   * Use for expressions supporting only binary and lowercase collation.
   */
  case object BinaryAndLowercaseType extends CollationType {
    override def acceptsType(other: StringType): Boolean =
      other.supportsBinaryEquality || other.isUTF8LcaseCollation
  }

  /**
   * Use for expressions supporting all collation types (binary
   * and ICU) but limited to using case and accent sensitivity specifiers.
   */
  case class ConfigurableCollationType(
      supportsCaseSpecifier: Boolean = true,
      supportsAccentSpecifier: Boolean = true,
      supportsTrimSpecifier: Boolean = false)
    extends CollationType {
    override def acceptsType(other: StringType): Boolean = {
      (supportsCaseSpecifier || !other.isCaseInsensitive) &&
        (supportsAccentSpecifier || !other.isAccentInsensitive) &&
        (supportsTrimSpecifier || !other.usesTrim)
    }
  }

  /**
   * Use for expressions supporting all possible collation types except
   * CS_AI collation types.
   */
  case object NonCaseSensitiveAccentInsensitiveType extends CollationType {
    override def acceptsType(other: StringType): Boolean =
      !other.isCaseInsensitive && other.isAccentInsensitive
  }
}