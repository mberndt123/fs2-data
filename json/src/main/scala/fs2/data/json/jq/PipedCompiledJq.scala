/*
 * Copyright 2023 Lucas Satabin
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

package fs2
package data
package json
package jq

import cats.data.NonEmptyChain

/** Represents a sequence of piped compiled jq queries, applying every query to the result of the previous one. */
class PipedCompiledJq[F[_]](val jqs: NonEmptyChain[CompiledJq[F]]) extends CompiledJq[F] {

  override def apply(in: fs2.Stream[F, Token]): fs2.Stream[F, Token] =
    jqs.foldLeft(in)((base, jq) => base.through(jq))

  override def andThen(that: CompiledJq[F]): CompiledJq[F] =
    that match {
      case that: PipedCompiledJq[F] =>
        new PipedCompiledJq[F](this.jqs ++ that.jqs)
      case _ =>
        new PipedCompiledJq[F](this.jqs :+ that)
    }

}
