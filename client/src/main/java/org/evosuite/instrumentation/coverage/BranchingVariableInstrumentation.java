package org.evosuite.instrumentation.coverage;

import org.evosuite.PackageInfo;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

public class BranchingVariableInstrumentation implements MethodInstrumentation {
    private static final Logger logger = LoggerFactory.getLogger(BranchingVariableInstrumentation.class);
    private MethodNode methodNode;
    private String className;
    private int currentLine;

    @Override
    public void analyze(ClassLoader classLoader, MethodNode methodNode, String className, String methodName,
                        int access) {
        this.methodNode = methodNode;
        this.className = className;
        this.methodNode.localVariables.sort(Comparator.comparingInt(o -> o.index));
        ListIterator<AbstractInsnNode> iterator = this.methodNode.instructions.iterator();
        while (iterator.hasNext()) {
            AbstractInsnNode instruction = iterator.next();
            int opcode = instruction.getOpcode();

            if (instruction instanceof LineNumberNode) {
                currentLine = ((LineNumberNode) instruction).line;
            } else if (opcode == DCMPL || opcode == DCMPG) {
                logger.info("Comparing two double values");
                logBranchingVariables(instruction, 2);
            } else if (opcode == FCMPL || opcode == FCMPG) {
                logger.info("Comparing two float values");
                logBranchingVariables(instruction, 2);
            } else if (opcode == LCMP) {
                logger.info("Comparing two long values");
                logBranchingVariables(instruction, 2);
            } else if (opcode >= IF_ICMPEQ && opcode <= IF_ICMPLE) {
                logger.info("Comparing two byte/char/int/short/boolean values");
                logBranchingVariables(instruction, 2);
            } else if (opcode == IF_ACMPEQ || opcode == IF_ACMPNE) {
                logger.info("Comparing two references");
                logBranchingVariables(instruction, 2);
            } else if (opcode >= IFEQ && opcode <= IFLE) {
                logger.info("Comparing one byte/char/int/short/boolean against 0");
                logBranchingVariables(instruction, 1);
            } else if (opcode == IFNULL || opcode == IFNONNULL) {
                logger.info("Comparing one reference against NULL");
                logBranchingVariables(instruction, 1);
            }
        }
    }

    @Override
    public boolean executeOnMainMethod() {
        return false;
    }

    @Override
    public boolean executeOnExcludedMethods() {
        return false;
    }

    /**
     * If the local variable type is one of the primitive types, return a {@link MethodInsnNode} to call its
     * corresponding <b><i>valueOf</i></b> method, so it can be wrapped into an object. If it is already an object,
     * return a {@link Opcodes#NOP NOP} instruction to do nothing.
     *
     * @param variable The instruction node of the local variable.
     */
    private static AbstractInsnNode valueOf(LocalVariableNode variable) {
        switch (variable.desc) {
            case "B":
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            case "C":
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;",
                        false);
            case "D":
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            case "F":
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            case "I":
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;",
                        false);
            case "J":
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            case "S":
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            case "Z":
                return new MethodInsnNode(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;",
                        false);
            default:
                return new InsnNode(NOP);
        }
    }

    /**
     * Get the instrumentation to log a local variable into the {@link ExecutionTracer}.
     *
     * @param variable The instruction node of the local variable.
     *
     * @return A list of instructions to perform the logging process.
     */
    private InsnList getInstrumentation(LocalVariableNode variable) {
        InsnList insnList = new InsnList();
        insnList.add(new InsnNode(DUP));
        insnList.add(valueOf(variable));
        insnList.add(new LdcInsnNode(className));
        insnList.add(new LdcInsnNode(currentLine));
        insnList.add(new LdcInsnNode(variable.name));
        insnList.add(new MethodInsnNode(INVOKESTATIC, PackageInfo.getNameWithSlash(ExecutionTracer.class),
                "passedBranchingVariable", "(Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;)V", false));
        return insnList;
    }

    private void logBranchingVariables(AbstractInsnNode instruction, int numOfVariables) {
        int count = 0;
        AbstractInsnNode current = instruction;
        while (count < numOfVariables) {
            current = current.getPrevious();
            switch (current.getOpcode()) {
                // region The variable is a local variable, we log it into the ExecutionTracer
                case ILOAD:
                case LLOAD:
                case FLOAD:
                case DLOAD:
                case ALOAD:
                    LocalVariableNode variable = methodNode.localVariables.get(((VarInsnNode) current).var);
                    InsnList insnList = getInstrumentation(variable);
                    methodNode.instructions.insert(current, insnList);
                    methodNode.maxStack += 5;
                // endregion
                // region The variable is a field of the object or a static field of the class
                case GETSTATIC:
                case GETFIELD:
                    // todo For now we only increase the counter, but maybe they should be treated differently as they may be related to crashes
                // endregion
                // region The variable is a return value from a method call
                case INVOKEVIRTUAL:
                case INVOKESPECIAL:
                case INVOKESTATIC:
                case INVOKEINTERFACE:
                case INVOKEDYNAMIC:
                    // todo For now we only increase the counter, but maybe they should be treated differently as they may be related to crashes
                    // todo For now we haven't considered the arguments of the method call. They should be excluded as well.
                // endregion
                case NEW: // todo New object maybe should be logged as well?
                // region The variable is a constant, we don't care about it, just increase the counter.
                case ACONST_NULL:
                case ICONST_M1:
                case ICONST_0:
                case ICONST_1:
                case ICONST_2:
                case ICONST_3:
                case ICONST_4:
                case ICONST_5:
                case LCONST_0:
                case LCONST_1:
                case FCONST_0:
                case FCONST_1:
                case FCONST_2:
                case DCONST_0:
                case DCONST_1:
                case BIPUSH:
                case SIPUSH:
                case LDC:
                // endregion
                // region The variable is a result of a previous comparision, increase the counter and ignore it.
                case LCMP:
                case FCMPL:
                case FCMPG:
                case DCMPL:
                case DCMPG:
                // endregion
                    count++;
                // region We don't care about all the other instructions.
                default:
                    break;
                // endregion
            }
        }
    }
}
