package org.evosuite.instrumentation;

import org.evosuite.PackageInfo;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class StringIndexVisitor extends GeneratorAdapter {
    private static Logger logger = LoggerFactory.getLogger(StringIndexVisitor.class);

    private final String fullMethodName;
    private final String methodName;
    private final String className;

    private boolean hadInvokeSpecial = false;

    private List<Integer> skippedLines = new ArrayList<>();
    private int currentLine = 0;
    private boolean end = false;

    public StringIndexVisitor(MethodVisitor mv, String className, String methodName, String desc) {
        super(ASM6, mv, ACC_PUBLIC, methodName, desc);
        fullMethodName = methodName + desc;
        this.className = className;
        this.methodName = methodName;

        if (!methodName.equals("<init>"))
            hadInvokeSpecial = true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>An important part is that the stack cannot be changed after the execution of this method.</b>
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (opcode == INVOKEVIRTUAL && owner.equals(PackageInfo.getNameWithSlash(String.class)))
            if (name.equals("charAt") && desc.equals("(I)C")) {
                // when String.charAt(int index) is called
                // the stack should be like [..., StringRef, index]
                end = false;
                logIndexAndStringLength();
            } else if (name.equals("substring") && desc.equals("(II)Ljava/lang/String;")) {
                // when String.substring(int beginIndex, int endIndex) is called
                // the stack should be like    [..., StringRef, beginIndex, endIndex]
                int endIndex = newLocal(Type.INT_TYPE);
                storeLocal(endIndex);       // [..., StringRef, beginIndex]
                int beginIndex = newLocal(Type.INT_TYPE);
                storeLocal(beginIndex);     // [..., StringRef]
                int StringRef = newLocal(Type.getType(String.class));
                storeLocal(StringRef);      // [...]

                end = false;
                loadLocal(StringRef);       // [..., StringRef]
                dup();                      // [..., StringRef, StringRef]
                storeLocal(StringRef);      // [..., StringRef]
                loadLocal(beginIndex);      // [..., StringRef, beginIndex]
                dup();                      // [..., StringRef, beginIndex, beginIndex]
                storeLocal(beginIndex);     // [..., StringRef, beginIndex]
                logIndexAndStringLength();  // [..., StringRef, beginIndex]

                end = true;
                loadLocal(endIndex);        // [..., StringRef, beginIndex, endIndex]
                dup();                      // [..., StringRef, beginIndex, endIndex, endIndex]
                loadLocal(StringRef);       // [..., StringRef, beginIndex, endIndex, endIndex, StringRef]
                swap();                     // [..., StringRef, beginIndex, endIndex, StringRef, endIndex]
                logIndexAndStringLength();  // [..., StringRef, beginIndex, endIndex, StringRef, endIndex]
                pop2();                     // // [..., StringRef, beginIndex, endIndex]
            } else if (name.equals("substring") && desc.equals("(I)Ljava/lang/String;")) {
                // when String.substring(int beginIndex) is called
                // the stack should be like [..., StringRef, beginIndex]
                end = false;
                logIndexAndStringLength();
            }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    /**
     * When this method is called, the stack should be like [..., StringRef, index].
     * <p>
     * It will get the length of the String by calling {@link String#length()} and then call a static function to store
     * the index and the String length into the {@link ExecutionTracer}.
     */
    private void logIndexAndStringLength() {
        dup2();     // [..., StringRef, index, StringRef, index]
        swap();     // [..., StringRef, index, index, StringRef]
        mv.visitMethodInsn(INVOKEVIRTUAL, PackageInfo.getNameWithSlash(String.class), "length", "()I", false);
        // [..., StringRef, index, index, StringLength]
        visitLdcInsn(end ? 1 : 0);  // [..., StringRef, index, index, StringLength, layer]
        mv.visitMethodInsn(INVOKESTATIC, PackageInfo.getNameWithSlash(ExecutionTracer.class), "passedIndexedAccess",
                "(III)V", false);        // Stack returns back to what it was: [..., StringRef, index]
    }
}
