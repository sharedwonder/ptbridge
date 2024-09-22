/*
 * Copyright (C) 2024 sharedwonder (Liu Baihao).
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

package net.sharedwonder.lightproxy.util

import java.io.StringWriter
import java.io.Writer
import com.google.gson.stream.JsonWriter

open class JsonBuilder(val writer: Writer = StringWriter()) : JsonWriter(writer) {
    override fun name(name: String): JsonBuilder {
        super.name(name)
        return this
    }

    inline fun objectValue(block: JsonBuilder.() -> Unit): JsonBuilder {
        beginObject()
        block()
        endObject()
        return this
    }

    inline fun arrayValue(block: JsonBuilder.() -> Unit): JsonBuilder {
        beginArray()
        block()
        endArray()
        return this
    }

    fun entry(name: String, value: String?) {
        name(name).value(value)
    }

    fun entry(name: String, value: Long) {
        name(name).value(value)
    }

    fun entry(name: String, value: Float) {
        name(name).value(value)
    }

    fun entry(name: String, value: Double) {
        name(name).value(value)
    }

    fun entry(name: String, value: Number?) {
        name(name).value(value)
    }

    fun entry(name: String, value: Boolean) {
        name(name).value(value)
    }

    fun entry(name: String, value: Boolean?) {
        name(name).value(value)
    }

    fun entryWithNullValue(name: String) {
        name(name).nullValue()
    }

    fun entryWithJsonValue(name: String, value: String) {
        name(name).jsonValue(value)
    }

    inline infix fun String.objectValue(block: JsonBuilder.() -> Unit) {
        name(this).objectValue(block)
    }

    inline infix fun String.arrayValue(block: JsonBuilder.() -> Unit) {
        name(this).arrayValue(block)
    }

    infix fun String.value(value: String?) {
        name(this).value(value)
    }

    infix fun String.value(value: Long) {
        name(this).value(value)
    }

    infix fun String.value(value: Float) {
        name(this).value(value)
    }

    infix fun String.value(value: Double) {
        name(this).value(value)
    }

    infix fun String.value(value: Number?) {
        name(this).value(value)
    }

    infix fun String.value(value: Boolean) {
        name(this).value(value)
    }

    infix fun String.value(value: Boolean?) {
        name(this).value(value)
    }

    fun String.nullValue() {
        name(this).nullValue()
    }

    infix fun String.jsonValue(value: String?) {
        name(this).value(value)
    }

    override fun toString(): String = writer.toString()
}