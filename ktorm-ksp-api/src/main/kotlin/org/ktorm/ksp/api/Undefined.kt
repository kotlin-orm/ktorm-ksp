/*
 * Copyright 2018-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ktorm.ksp.api

import sun.misc.Unsafe
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.security.ProtectionDomain
import java.util.concurrent.ConcurrentHashMap

private val undefinedValuesCache = ConcurrentHashMap<Class<*>, Any>()
private val unsafe = getUnsafe()
private val defineClassMethod = getDefineClassMethod()

public inline fun <reified T> undefined(): T {
    return getUndefinedValue(T::class.java) as T
}

@PublishedApi
internal fun getUndefinedValue(cls: Class<*>): Any {
    return undefinedValuesCache.computeIfAbsent(cls) {
        if (cls.isArray) {
            java.lang.reflect.Array.newInstance(cls.componentType, 0)
        } else if (cls.isInterface) {
            createUndefinedValueByJdkProxy(cls)
        } else if (Modifier.isAbstract(cls.modifiers)) {
            createUndefinedValueBySubclassing(cls)
        } else {
            unsafe.allocateInstance(cls)
        }
    }
}

private fun getUnsafe(): Unsafe {
    val field = Unsafe::class.java.getDeclaredField("theUnsafe")
    field.isAccessible = true
    return field.get(null) as Unsafe
}

private fun createUndefinedValueByJdkProxy(cls: Class<*>): Any {
    return Proxy.newProxyInstance(cls.classLoader, arrayOf(cls)) { proxy, method, args ->
        when (method.declaringClass.kotlin) {
            Any::class -> {
                when (method.name) {
                    "equals" -> proxy === args!![0]
                    "hashCode" -> System.identityHashCode(proxy)
                    "toString" -> "Ktorm undefined value proxy for ${cls.name}"
                    else -> throw UnsupportedOperationException("Method not supported: $method")
                }
            }
            else -> {
                throw UnsupportedOperationException("Method not supported: $method")
            }
        }
    }
}

private fun createUndefinedValueBySubclassing(cls: Class<*>): Any {
    val superClassName = cls.name.replace(".", "/")

    var subclassName = "$superClassName\$KtormUndefined"
    if (superClassName.startsWith("java/")) {
        subclassName = "\$" + subclassName
    }

    val bytes = generateByteCode(subclassName.toByteArray(), superClassName.toByteArray())
    val subclass = defineClassMethod.invoke(cls.classLoader, null, bytes, null) as Class<*>
    return unsafe.allocateInstance(subclass)
}

private fun getDefineClassMethod(): Method {
    val parameterTypes = arrayOf(String::class.java, ByteBuffer::class.java, ProtectionDomain::class.java)
    val method = ClassLoader::class.java.getDeclaredMethod("defineClass", *parameterTypes)
    method.isAccessible = true
    return method
}

private fun generateByteCode(className: ByteArray, superClassName: ByteArray): ByteBuffer {
    val buf = ByteBuffer.allocate(1024)
    buf.putInt(0xCAFEBABE.toInt())                          // magic
    buf.putShort(0)                                         // minor version
    buf.putShort(52)                                        // major version, 52 for JDK1.8
    buf.putShort(5)                                         // constant pool count, totally 4 constants, so it's 5
    buf.put(1)                                              // #1, CONSTANT_Utf8_info
    buf.putShort(className.size.toShort())                  // length
    buf.put(className)                                      // class name
    buf.put(7)                                              // #2, CONSTANT_Class_info
    buf.putShort(1)                                         // name index, ref to constant #1
    buf.put(1)                                              // #3, CONSTANT_Utf8_info
    buf.putShort(superClassName.size.toShort())             // length
    buf.put(superClassName)                                 // super class name
    buf.put(7)                                              // #4, CONSTANT_Class_info
    buf.putShort(3)                                         // name index, ref to constant #3
    buf.putShort((0x0001 or 0x0020 or 0x1000).toShort())    // access flags, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC
    buf.putShort(2)                                         // this class, ref to constant #2
    buf.putShort(4)                                         // super class, ref to constant #4
    buf.putShort(0)                                         // interfaces count
    buf.putShort(0)                                         // fields count
    buf.putShort(0)                                         // methods count
    buf.putShort(0)                                         // attributes count
    buf.flip()
    return buf
}
