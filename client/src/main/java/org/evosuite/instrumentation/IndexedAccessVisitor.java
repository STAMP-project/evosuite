package org.evosuite.instrumentation;

import org.evosuite.PackageInfo;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.objectweb.asm.Opcodes.*;

public class IndexedAccessVisitor extends GeneratorAdapter {
    private static Logger logger = LoggerFactory.getLogger(IndexedAccessVisitor.class);

    private final String fullMethodName;
    private final String className;

    private final int targetLine;
    private int currentLine = 0;
    private int layer = 0;

    IndexedAccessVisitor(MethodVisitor mv, String className, String methodName, String desc, int targetLine) {
        super(ASM6, mv, ACC_PUBLIC, methodName, desc);
        fullMethodName = methodName + desc;
        this.className = className;
        this.targetLine = targetLine;
    }

    /**
     * Called before accessing the target array that may throw an {@link ArrayIndexOutOfBoundsException}.
     *
     * @param opcode the opcode of the instruction to be visited. This opcode is either IALOAD, LALOAD, FALOAD, DALOAD,
     *               AALOAD, BALOAD, CALOAD, SALOAD, IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, or
     *               SASTORE.
     */
    @Override
    public void visitInsn(int opcode) {
        if (currentLine == targetLine) {
            if (opcode == IALOAD || opcode == LALOAD || opcode == FALOAD || opcode == DALOAD || opcode == AALOAD || opcode == BALOAD || opcode == CALOAD || opcode == SALOAD) {
                /*
                 * Byte code for loading an element from an array:
                 * ALOAD [arg]                                                          // load the array reference from the stack, where arg is the stack frame
                 * ILOAD [arg] / ICONST_* / BIPUSH [arg] / SIPUSH [arg] / LDC [arg]     // index of the element in the array
                 * *ALOAD                                                               // load the element to stack
                 *
                 * There are four different ways to represent the index:
                 *      1. ILOAD [arg]          // load an integer variable from the stack, arg is the index in the stack
                 *      2. ICONST_*             // If index is in range [0, 5], push an integer const to stack
                 *      3. BIPUSH/SIPUSH [arg]  // If index is in range [6, 32767], push an integer const to stack
                 *      4. LDC [arg]            // If index is larger, load it to stack
                 */

                // [..., arrayRef, index]
                logIndexAndArrayLength();
            } else if (opcode == IASTORE || opcode == LASTORE || opcode == FASTORE || opcode == DASTORE || opcode == AASTORE || opcode == BASTORE || opcode == CASTORE || opcode == SASTORE) {
                /*
                 * Byte code for storing an element to an array:
                 * ALOAD [arg]                                                          // load the array reference from the stack, where arg is the stack frame
                 * ILOAD [arg] / ICONST_* / BIPUSH [arg] / SIPUSH [arg] / LDC [arg]     // same way to deal with the index as in loading above
                 * *LOAD [arg] / *CONST_* / B*PUSH [arg] / S*PUSH [arg] / LDC [arg]     // element to store
                 * *ASTORE                                                              // store the element to array
                 */

                // [..., arrayRef, index, element]
                int element;
                if (opcode == IASTORE) {
                    element = newLocal(Type.INT_TYPE);
                } else if (opcode == BASTORE) {
                    element = newLocal(Type.BYTE_TYPE);
                } else if (opcode == CASTORE) {
                    element = newLocal(Type.CHAR_TYPE);
                } else if (opcode == SASTORE) {
                    element = newLocal(Type.SHORT_TYPE);
                } else if (opcode == LASTORE) {
                    element = newLocal(Type.LONG_TYPE);
                } else if (opcode == FASTORE) {
                    element = newLocal(Type.LONG_TYPE);
                } else if (opcode == DASTORE) {
                    element = newLocal(Type.DOUBLE_TYPE);
                } else {
                    element = newLocal(Type.getType(Object.class));
                }
                storeLocal(element);    // [..., arrayRef, index]
                logIndexAndArrayLength();
                loadLocal(element);     // [..., arrayRef, index, element]
            }
        }
        super.visitInsn(opcode);
    }

    /**
     * Called before methods in the {@link String} class that may throw a {@link StringIndexOutOfBoundsException}.
     *
     * @param opcode {@link Opcodes#INVOKEINTERFACE} for {@link CharSequence#charAt(int)}, {@link Opcodes#INVOKESPECIAL}
     *               for constructors of the {@link String} class and {@link Opcodes#INVOKEVIRTUAL} for the rest methods
     *               in the {@link String} class.
     * @param owner  It is either {@link String} or {@link CharSequence}.
     * @param name   All methods in the {@link} String class that may throw a {@link StringIndexOutOfBoundsException}.
     * @param desc   Signature of the method.
     * @param itf    Whether it is a interface method.
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (currentLine == targetLine) {
            if (owner.equals(PackageInfo.getNameWithSlash(String.class))) {
                switch (name) {
                    case "<init>":
                        if ("([BIILjava/nio/charset/Charset;)V".equals(desc) || "([BIILjava/lang/String;)V".equals(desc)) {
                            // [..., arrayRef, offset, counter, charset]
                            int charset = newLocal(Type.getType(Object.class));
                            storeLocal(charset);    // [..., arrayRef, offset, counter]
                            logStringCheckBounds();
                            loadLocal(charset);     // [..., arrayRef, offset, counter, charset]
                        } else if ("([BII)V".equals(desc) || "([CII)V".equals(desc) || "([III)V".equals(desc)) {
                            // [..., arrayRef, offset, length]
                            logStringCheckBounds();
                        }
                        break;
                    case "codePointAt":
                    case "charAt":
                        // [..., StringRef, index]
                        logIndexAndStringLength();
                        break;
                    case "codePointBefore":
                        // [..., StringRef, indexToCheck+1]
                        mv.visitInsn(ICONST_M1);    // [..., StringRef, indexToCheck+1, -1]
                        mv.visitInsn(IADD);         // [..., StringRef, indexToCheck]
                        logIndexAndStringLength();
                        mv.visitInsn(ICONST_1);     // [..., StringRef, indexToCheck, 1]
                        mv.visitInsn(IADD);         // [..., StringRef, indexToCheck+1]
                        break;
                    case "substring":
                        if (desc.equals("(I)Ljava/lang/String;")) {
                            // [..., StringRef, strIndex]
                            logIndexAndStringLength();
                        } else {
                            // [..., StringRef, strIndex, endIndex]
                            dup2X1();   // [..., strIndex, endIndex, StringRef, strIndex, endIndex]
                            pop();      // [..., strIndex, endIndex, StringRef, strIndex]
                            logIndexAndStringLength();
                            pop();      // [..., strIndex, endIndex, StringRef]

                            dupX2();    // [..., StringRef, strIndex, endIndex, StringRef]
                            swap();     // [..., StringRef, strIndex, StringRef, endIndex]
                            dupX1();    // [..., StringRef, strIndex, endIndex, StringRef, endIndex]
                            logIndexAndStringLength();
                            pop2();     // [..., StringRef, strIndex, endIndex]
                        }
                        break;
                    case "getChars":
                        // [..., StringRef, strIndex, endIndex,  dstArray, dstStrIndex]
                        int dstStrIndex = newLocal(Type.INT_TYPE);
                        storeLocal(dstStrIndex);    // [..., StringRef, strIndex, endIndex,  dstArray]
                        dup2X2();                   // [..., endIndex,  dstArray, StringRef, strIndex, endIndex, dstArray]
                        pop();                      // [..., endIndex,  dstArray, StringRef, strIndex, endIndex]
                        int endIndex = newLocal(Type.INT_TYPE);
                        storeLocal(endIndex);       // [..., endIndex,  dstArray, StringRef, strIndex]
                        dup2X2();                   // [..., StringRef, strIndex, endIndex,  dstArray, StringRef, strIndex]
                        swap();                     // [..., StringRef, strIndex, endIndex,  dstArray, strIndex, StringRef]
                        dupX1();                    // [..., StringRef, strIndex, endIndex,  dstArray, StringRef, strIndex, StringRef]
                        loadLocal(endIndex);        // [..., StringRef, strIndex, endIndex,  dstArray, StringRef, strIndex, StringRef, endIndex]
                        logIndexAndStringLength();
                        pop2();                     // [..., StringRef, strIndex, endIndex,  dstArray, StringRef, strIndex]
                        logIndexAndStringLength();
                        pop2();                     // [..., StringRef, strIndex, endIndex,  dstArray]
                        loadLocal(dstStrIndex);     // [..., StringRef, strIndex, endIndex,  dstArray, dstStrIndex]
                        break;
                }
            } else if (owner.equals(PackageInfo.getNameWithSlash(CharSequence.class))) {
                if (name.equals("charAt")) {
                    // [..., StringRef, index]
                    logIndexAndStringLength();
                }
            }
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        currentLine = line;
        layer = 0;
    }

    /**
     * When this method is called, the stack should be like [..., arrayRef, index]. <b>It's important that afterwards,
     * the stack maintains the same.</b>
     * <p>
     * It will get the length of the array and call a static function to store the index and the array length into the
     * {@link ExecutionTracer}.
     */
    private void logIndexAndArrayLength() {
        dup2();         // [..., arrayRef, index, arrayRef, index]
        swap();         // [..., arrayRef, index, index, arrayRef]
        arrayLength();  // [..., arrayRef, index, index, length]

        LinePool.addLine(className, fullMethodName, currentLine);
        visitLdcInsn(layer++);
        visitLdcInsn(className);
        visitLdcInsn(fullMethodName);
        mv.visitMethodInsn(INVOKESTATIC, PackageInfo.getNameWithSlash(ExecutionTracer.class), "passedIndexedAccess",
                "(IIILjava/lang/String;Ljava/lang/String;)V", false);
        // [..., arrayRef, index]
    }

    /**
     * When this method is called, the stack should be like [..., arrayRef, offset, counter]. <b>It's important that
     * afterwards, the stack maintains the same.</b>
     * <p>
     * It will use {@link #logIndexAndArrayLength()} to log three index-length pairs into the {@link ExecutionTracer}:
     * <ol>
     *     <li>(offset + counter) to array length.</li>
     *     <li>counter to array length.</li>
     *     <li>offset to array length.</li>
     * </ol>
     *
     * @see #logIndexAndArrayLength()
     */
    private void logStringCheckBounds() {
        dup2X1();           // [..., offset, counter, arrayRef, offset, counter]
        mv.visitInsn(IADD); // [..., offset, counter, arrayRef, offset+counter]
        logIndexAndStringLength();
        pop();              // [..., offset, counter, arrayRef]
        dupX2();            // [..., arrayRef, offset, counter, arrayRef]
        swap();             // [..., arrayRef, offset, arrayRef, counter]
        logIndexAndStringLength();
        dupX2();            // [..., arrayRef, counter, offset, arrayRef, counter]
        pop();              // [..., arrayRef, counter, offset, arrayRef]
        swap();             // [..., arrayRef, counter, arrayRef, offset]
        logIndexAndStringLength();
        swap();             // [..., arrayRef, counter, offset, arrayRef]
        pop();              // [..., arrayRef, counter, offset]
    }

    /**
     * When this method is called, the stack should be like [..., StringRef, index]. <b>It's important that afterwards,
     * the stack maintains the same.</b>
     * <p>
     * It will get the length of the String by calling {@link String#length()} and then call a static function to store
     * the index and the String length into the {@link ExecutionTracer}.
     */
    private void logIndexAndStringLength() {
        dup2();     // [..., StringRef, index, StringRef, index]
        swap();     // [..., StringRef, index, index, StringRef]
        mv.visitMethodInsn(INVOKEVIRTUAL, PackageInfo.getNameWithSlash(String.class), "length", "()I", false);
        // [..., StringRef, index, index, StringLength]
        visitLdcInsn(layer++);
        visitLdcInsn(className);
        visitLdcInsn(fullMethodName);
        mv.visitMethodInsn(INVOKESTATIC, PackageInfo.getNameWithSlash(ExecutionTracer.class), "passedIndexedAccess",
                "(IIILjava/lang/String;Ljava/lang/String;)V", false);
        // [..., StringRef, index]
    }
}
