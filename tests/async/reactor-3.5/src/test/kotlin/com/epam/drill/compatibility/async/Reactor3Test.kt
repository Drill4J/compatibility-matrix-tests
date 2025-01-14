/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.compatibility.async

import com.epam.drill.compatibility.matrix.AsyncMatrixTest
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers.parallel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future


class Reactor3Test : AsyncMatrixTest() {

    override fun callAsyncCommunication(task: () -> String): Future<String> {
        val future = CompletableFuture<String>()
        Mono.fromCallable {
            task()
        }.subscribeOn(parallel())
            .subscribe({ result -> future.complete(result) }, { error -> future.completeExceptionally(error) })
        return future
    }
}