/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.core.memory;

import org.apache.flink.annotation.Public;

import java.io.DataInput;
import java.io.IOException;

/**
 * This interface defines a view over some memory that can be used to sequentially read the contents
 * of the memory. The view is typically backed by one or more {@link
 * org.apache.flink.core.memory.MemorySegment}.
 */
//MemorySegment 解决了内存分块存储的问题,但如果要使用连续的MemorySegment存储数据，就需要借助DataInputView和DataOutputView
//DataInputView  和 DataOutputView 中定义了一组MemorySegement 视图,其中DataInputView用于顺序读取内存的数据,DataOutputView用于写

//  DataInputView 的主要实现类有三种 DataInputDeserializer,DataInputViewStreamWrapper,AbstractPagedInputView：
//  （1）DataInputDeserializer实现了 实现了简单高效的反序列化器,例如对 KafkaDeserializationSchema 中接入的二进制数据进行反序列化,转化为java对象
//  （2）DataInputViewStreamWarpper 对 DataInputStream 接口进行拓展，直接将InputStream 数据转为DataInputView 输入数据，
//  例如读取BinaryInputFormat类型的文件，直接将FSDataInputStream 转换为DataInputView
//  （3）AbstractPagedInputView 实现了基于多个内存页的数据输入，且具有多个子类

@Public
public interface DataInputView extends DataInput {

    /**
     * Skips {@code numBytes} bytes of memory. In contrast to the {@link #skipBytes(int)} method,
     * this method always skips the desired number of bytes or throws an {@link
     * java.io.EOFException}.
     *
     * @param numBytes The number of bytes to skip.
     * @throws IOException Thrown, if any I/O related problem occurred such that the input could not
     *     be advanced to the desired position.
     */
    void skipBytesToRead(int numBytes) throws IOException;

    /**
     * Reads up to {@code len} bytes of memory and stores it into {@code b} starting at offset
     * {@code off}. It returns the number of read bytes or -1 if there is no more data left.
     *
     * @param b byte array to store the data to
     * @param off offset into byte array
     * @param len byte length to read
     * @return the number of actually read bytes of -1 if there is no more data left
     * @throws IOException
     */
    int read(byte[] b, int off, int len) throws IOException;

    /**
     * Tries to fill the given byte array {@code b}. Returns the actually number of read bytes or -1
     * if there is no more data.
     *
     * @param b byte array to store the data to
     * @return the number of read bytes or -1 if there is no more data left
     * @throws IOException
     */
    // 入参用来存储 读进来的字节数组
    int read(byte[] b) throws IOException;
}
