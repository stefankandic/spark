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
package org.apache.spark.sql.catalyst.expressions.csv

import com.univocity.parsers.csv.CsvParser

import org.apache.spark.sql.catalyst.csv.{CSVInferSchema, CSVOptions}
import org.apache.spark.sql.types.{DataType, NullType, StructType}
import org.apache.spark.unsafe.types.UTF8String

case class SchemaOfCsvEvaluator(options: Map[String, String]) {

  @transient
  private lazy val csvOptions: CSVOptions = {
    // 'lineSep' is a plan-wise option so we set a noncharacter, according to
    // the unicode specification, which should not appear in Java's strings.
    // See also SPARK-38955 and https://www.unicode.org/charts/PDF/UFFF0.pdf.
    // scalastyle:off nonascii
    val exprOptions = options ++ Map("lineSep" -> '\uFFFF'.toString)
    // scalastyle:on nonascii
    new CSVOptions(exprOptions, true, "UTC")
  }

  @transient
  private lazy val csvParser: CsvParser = new CsvParser(csvOptions.asParserSettings)

  @transient
  private lazy val csvInferSchema = new CSVInferSchema(csvOptions)

  final def evaluate(csv: UTF8String): Any = {
    val row = csvParser.parseLine(csv.toString)
    assert(row != null, "Parsed CSV record should not be null.")
    val header = row.zipWithIndex.map { case (_, index) => s"_c$index" }
    val startType: Array[DataType] = Array.fill[DataType](header.length)(NullType)
    val fieldTypes = csvInferSchema.inferRowType(startType, row)
    val st = StructType(csvInferSchema.toStructFields(fieldTypes, header))
    UTF8String.fromString(st.sql)
  }
}
