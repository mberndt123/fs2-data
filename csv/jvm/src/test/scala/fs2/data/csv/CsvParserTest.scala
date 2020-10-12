/*
 * Copyright 2019 Lucas Satabin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fs2.data.csv

import io.circe.parser._

import fs2._
import fs2.io._

import cats.effect._
import cats.implicits._

import weaver._

import java.nio.file.Paths

object CsvParserTest extends IOSuite {

  override type Res = Blocker
  def sharedResource: Resource[IO, Res] = Blocker[IO]

  private val testFileDir = Paths.get("csv/jvm/src/test/resources/csv-spectrum/csvs/")

  def allExpected(blocker: Blocker) =
    file
      .directoryStream[IO](blocker, testFileDir)
      .evalMap { path =>
        val name = path.getFileName.toFile.getName.stripSuffix(".csv")
        file
          .readAll[IO](Paths.get(s"csv/jvm/src/test/resources/csv-spectrum/json/$name.json"), blocker, 1024)
          .through(text.utf8Decode)
          .compile
          .string
          .flatMap { rawExpected =>
            parse(rawExpected)
              .flatMap(_.as[List[Map[String, String]]])
              .liftTo[IO]
          }
          .map(path -> _)
      }

  test("Standard test suite should pass") { blocker =>
    allExpected(blocker)
      .evalMap { case (path, expected) =>
        file
          .readAll[IO](path, blocker, 1024)
          .through(fs2.text.utf8Decode)
          .through(rows())
          .through(headers[IO, String])
          .compile
          .toList
          .map(_.map(_.toMap))
          .map(actual => expect(actual == expected, s"Invalid file $path").toExpectations)
      }
      .compile
      .foldMonoid
  }

  test("Standard test suite files should be encoded and parsed correctly") { blocker =>
    allExpected(blocker)
      .evalMap { case (path, expected) =>
        Stream
          .emits(expected)
          .map(m => CsvRow.fromListHeaders(m.toList))
          .unNone
          .through(encodeRowWithFirstHeaders)
          .through(toStrings())
          .through(rows[IO, String]())
          .through(headers[IO, String])
          .compile
          .toList
          .map(_.map(_.toMap))
          .map(reencoded => expect(reencoded == expected, s"Invalid file $path").toExpectations)
      }
      .compile
      .foldMonoid
  }

  test("Parser should handle literal quotes if specified") {
    val content =
      """name,age,description
        |John Doe,47,no quotes
        |Jane Doe,50,"entirely quoted"
        |Bob Smith,80,"starts with" a quote
        |Alice Grey,78,contains "a quote
        |""".stripMargin

    val expected = List(
      Map("name" -> "John Doe", "age" -> "47", "description" -> "no quotes"),
      Map("name" -> "Jane Doe", "age" -> "50", "description" -> "\"entirely quoted\""),
      Map("name" -> "Bob Smith", "age" -> "80", "description" -> "\"starts with\" a quote"),
      Map("name" -> "Alice Grey", "age" -> "78", "description" -> "contains \"a quote")
    )

    Stream
      .emit(content)
      .covary[IO]
      .through(rows[IO, String](',', QuoteHandling.Literal))
      .through(headers[IO, String])
      .compile
      .toList
      .map(_.map(_.toMap))
      .map(actual => expect(actual == expected).toExpectations)
  }
}