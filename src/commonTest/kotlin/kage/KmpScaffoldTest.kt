/**
 * Copyright 2026 The kage Authors. All rights reserved. Use of this source code is governed by
 * either an Apache 2.0 or MIT license at your discretion, that can be found in the LICENSE-APACHE
 * or LICENSE-MIT files respectively.
 */
package kage

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class KmpScaffoldTest {
  @Test
  fun commonTestSourceSetRunsOnJvm() {
    assertThat("age".encodeToByteArray()).isEqualTo(byteArrayOf(0x61, 0x67, 0x65))
  }
}
