package org.evosuite.instrumentation;

import org.evosuite.PackageInfo;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class ArrayIndexVisitor extends GeneratorAdapter {
    private static Logger logger = LoggerFactory.getLogger(ArrayIndexVisitor.class);

    private final String fullMethodName;
    private final String methodName;
    private final String className;

    private boolean hadInvokeSpecial = false;

    private List<Integer> skippedLines = new ArrayList<>();
    private final int targetLine;
    private int currentLine = 0;
    private int arrayLayer = 0;

    public ArrayIndexVisitor(MethodVisitor mv, String className, String methodName, String desc, int targetLine) {
        super(ASM6, mv, ACC_PUBLIC, methodName, desc);
        fullMethodName = methodName + desc;
        this.className = className;
        this.methodName = methodName;
        this.targetLine = targetLine;

        if (!methodName.equals("<init>"))
            hadInvokeSpecial = true;
    }

    /**
     * Called before accessing an array.
     *
     * @param opcode the opcode of the instruction to be visited. This opcode is either IALOAD, LALOAD, FALOAD, DALOAD,
     *               AALOAD, BALOAD, CALOAD, SALOAD, IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, or
     *               SASTORE.
     */
    @Override
    public void visitInsn(int opcode) {
        if (currentLine == targetLine)
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
                logger.info("Loading an element from an array: \n\tclass:\t" + className + "\n\tline:\t:" + currentLine);

                logIndexAndArrayLength();
            } else if (opcode == IASTORE || opcode == LASTORE || opcode == FASTORE || opcode == DASTORE || opcode == AASTORE || opcode == BASTORE || opcode == CASTORE || opcode == SASTORE) {
                /*
                 * Byte code for storing an element to an array:
                 * ALOAD [arg]                                                          // load the array reference from the stack, where arg is the stack frame
                 * ILOAD [arg] / ICONST_* / BIPUSH [arg] / SIPUSH [arg] / LDC [arg]     // same way to deal with the index as in loading above
                 * *LOAD [arg] / *CONST_* / B*PUSH [arg] / S*PUSH [arg] / LDC [arg]     // element to store
                 * *ASTORE                                                              // store the element to array
                 */
                logger.info("Storing an element to an array: \n\tclass:\t" + className + "\n\tline:\t:" + currentLine);

                int element;        // create an temporary var to store the element to be saved
                if (opcode == IASTORE)
                    element = newLocal(Type.INT_TYPE);
                else if (opcode == BASTORE)
                    element = newLocal(Type.BYTE_TYPE);
                else if (opcode == CASTORE)
                    element = newLocal(Type.CHAR_TYPE);
                else if (opcode == SASTORE)
                    element = newLocal(Type.SHORT_TYPE);
                else if (opcode == LASTORE)
                    element = newLocal(Type.LONG_TYPE);
                else if (opcode == FASTORE)
                    element = newLocal(Type.LONG_TYPE);
                else if (opcode == DASTORE)
                    element = newLocal(Type.DOUBLE_TYPE);
                else
                    element = newLocal(Type.getType(Object.class));
                storeLocal(element);    // store the var temporarily

                logIndexAndArrayLength();

                loadLocal(element);     // recover the var
            }
        super.visitInsn(opcode);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        currentLine = line;
        arrayLayer = 0;
        if (methodName.equals("<clinit>"))
            return;
        if (!hadInvokeSpecial)
            skippedLines.add(line);
    }

    /**
     * When this method is called, the stack should be like [..., ref, index].
     * <p>
     * It will get the length of the array and call a static function to store the index and the array length into the
     * ExecutionTracer.
     */
    private void logIndexAndArrayLength() {
        dup2();         // duplicate the array reference and the query index.   Stack be like [..., ref, index, ref, index]
        swap();         // move array the reference to the top of the stack.    Stack be like [..., index, ref]
        arrayLength();  // replace array reference with array length.           Stack be like [..., index, length]

        LinePool.addLine(className, fullMethodName, currentLine);
        visitLdcInsn(arrayLayer++);
        mv.visitMethodInsn(INVOKESTATIC,
                           PackageInfo.getNameWithSlash(ExecutionTracer.class),
                           "passedArrayAccess",
                           "(III)V",
                           false);
    }
}
