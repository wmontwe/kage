/**
 * Copyright 2026 The kage Authors. All rights reserved. Use of this source code is governed by
 * either an Apache 2.0 or MIT license at your discretion, that can be found in the LICENSE-APACHE
 * or LICENSE-MIT files respectively.
 */
package kage

import kotlin.test.Test
import kotlin.test.assertContentEquals

class KmpScaffoldTest {
  @Test
  fun commonTestSourceSetRunsOnJvm() {
    assertContentEquals(byteArrayOf(0x61, 0x67, 0x65), "age".encodeToByteArray())
  }
}
