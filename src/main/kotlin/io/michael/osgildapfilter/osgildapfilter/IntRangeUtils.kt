package io.michael.osgildapfilter.osgildapfilter

import kotlin.math.max


val IntRange.length: Int
    get() = this.endInclusive - this.start

val IntRange.checkedEndExclusive: Int
    get() = max(this.start, this.endInclusive - 1)