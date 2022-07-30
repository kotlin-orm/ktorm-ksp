package org.ktorm.ksp.api

import java.nio.ByteBuffer

/**
 * Generate subclass byte code.
 */
internal class ByteCodeGenerator(superClass: Class<*>) {
    private val _className: ByteArray
    private val _superClassName: ByteArray

    init {
        _superClassName = superClass.name.replace('.', '/').toByteArray()
        _className = "\$${superClass.name.replace('.', '/')}\$KtormUndefined".toByteArray()
    }

    fun getBytes(): ByteArray {
        val buf = ByteBuffer.allocate(128)
        buf.putInt(0xCAFEBABE.toInt())                          // magic
        buf.putShort(0)                                         // minor version
        buf.putShort(52)                                        // major version, 52 for JDK1.8
        buf.putShort(5)                                         // constant pool count, totally 4 constants, so it's 5
        buf.put(1)                                              // #1, CONSTANT_Utf8_info
        buf.putShort(_className.size.toShort())                 // length
        buf.put(_className)                                     // class name
        buf.put(7)                                              // #2, CONSTANT_Class_info
        buf.putShort(1)                                         // name index, ref to constant #1
        buf.put(1)                                              // #3, CONSTANT_Utf8_info
        buf.putShort(_superClassName.size.toShort())            // length
        buf.put(_superClassName)                                // super class name
        buf.put(7)                                              // #4, CONSTANT_Class_info
        buf.putShort(3)                                         // name index, ref to constant #3
        buf.putShort((0x0001 or 0x0020 or 0x1000).toShort())    // access flags, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC
        buf.putShort(2)                                         // this class, ref to constant #2
        buf.putShort(4)                                         // super class, ref to constant #4
        buf.putShort(0)                                         // interfaces count
        buf.putShort(0)                                         // fields count
        buf.putShort(0)                                         // methods count
        buf.putShort(0)                                         // attributes count
        return ByteArray(buf.flip().limit()).also { buf.get(it) }
    }
}
