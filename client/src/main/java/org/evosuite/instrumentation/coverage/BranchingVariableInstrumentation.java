package org.evosuite.instrumentation.coverage;

import org.evosuite.PackageInfo;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

public class BranchingVariableInstrumentation implements MethodInstrumentation {
    private static final Logger logger = LoggerFactory.getLogger(BranchingVariableInstrumentation.class);

    @Override
    public void analyze(ClassLoader classLoader, MethodNode mn, String className, String methodName, int access) {
        mn.localVariables.sort(Comparator.comparingInt(o -> o.index));
        int currentLine = 0;
        ListIterator<AbstractInsnNode> iterator = mn.instructions.iterator();
        while (iterator.hasNext()) {
            AbstractInsnNode instruction = iterator.next();
            int opcode = instruction.getOpcode();

            if (instruction instanceof LineNumberNode) {
                currentLine = ((LineNumberNode) instruction).line;
            } else if (opcode == DCMPL || opcode == DCMPG) {
                // comparing two double values:
                int count = 0;
                AbstractInsnNode current = instruction;
                while (count < 2) {
                    current = current.getPrevious();
                    switch (current.getOpcode()) {
                        // region One of the doubles is a constant, which we don't care about. Just increase the
                        // counter.
                        case DCONST_0:
                        case DCONST_1:
                        case LDC:
                            count++;
                            break;
                        // endregion
                        // region One of the doubles is a field of the object or a static field of the class
                        case GETSTATIC:
                        case GETFIELD:
                            // todo for now we only increase the counter, but maybe they should be dealt with
                            //  differently as they may be related to crashes
                            count++;
                            break;
                        // endregion
                        // region One of the doubles is a return value from a method call
                        case INVOKEVIRTUAL:
                        case INVOKESPECIAL:
                        case INVOKESTATIC:
                        case INVOKEINTERFACE:
                        case INVOKEDYNAMIC:
                            // todo for now we only increase the counter, but maybe they should be dealt with
                            //  differently as they may be related to crashes
                            count++;
                            break;
                        // endregion
                        // region One of the doubles is a local variable, we log it into the ExecutionTracer
                        case DLOAD:
                            InsnList insnList = new InsnList();
                            insnList.add(new InsnNode(DUP));
                            insnList.add(new MethodInsnNode(INVOKESTATIC, PackageInfo.getNameWithSlash(Double.class),
                                    "valueOf", "(D)Ljava/lang/Double;", false));
                            insnList.add(new LdcInsnNode(className));
                            insnList.add(new LdcInsnNode(methodName));
                            insnList.add(new LdcInsnNode(currentLine));
                            insnList.add(new LdcInsnNode(mn.localVariables.get(((VarInsnNode) current).var).name));
                            insnList.add(new MethodInsnNode(INVOKESTATIC,
                                    PackageInfo.getNameWithSlash(ExecutionTracer.class), "passedBranchingVariable",
                                    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V",
                                    false));
                            mn.instructions.insert(current, insnList);
                            count++;
                            mn.maxStack += 5;
                            break;
                        // endregion
                        // region We don't care about all the other instructions.
                        default:
                            break;
                        // endregion
                    }
                }
            } else if (opcode == FCMPL || opcode == FCMPG) {
                // comparing two float values:
                int count = 0;
                AbstractInsnNode current = instruction;
                while (count < 2) {
                    current = current.getPrevious();
                    switch (current.getOpcode()) {
                        // region One of the floats is a constant, which we don't care about. Just increase the counter.
                        case FCONST_0:
                        case FCONST_1:
                        case FCONST_2:
                        case LDC:
                            count++;
                            break;
                        // endregion
                        // region One of the floats is a field of the object or a static field of the class
                        case GETSTATIC:
                        case GETFIELD:
                            // todo for now we only increase the counter, but maybe they should be dealt with
                            //  differently as they may be related to crashes
                            count++;
                            break;
                        // endregion
                        // region One of the floats is a return value from a method call
                        case INVOKEVIRTUAL:
                        case INVOKESPECIAL:
                        case INVOKESTATIC:
                        case INVOKEINTERFACE:
                        case INVOKEDYNAMIC:
                            // todo for now we only increase the counter, but maybe they should be dealt with
                            //  differently as they may be related to crashes
                            count++;
                            break;
                        // endregion
                        // region One of the longs is a local variable, we log it into the ExecutionTracer
                        case FLOAD:
                            InsnList insnList = new InsnList();
                            insnList.add(new InsnNode(DUP));
                            insnList.add(new MethodInsnNode(INVOKESTATIC, PackageInfo.getNameWithSlash(Float.class),
                                    "valueOf", "(F)Ljava/lang/Float;", false));
                            insnList.add(new LdcInsnNode(className));
                            insnList.add(new LdcInsnNode(methodName));
                            insnList.add(new LdcInsnNode(currentLine));
                            insnList.add(new LdcInsnNode(mn.localVariables.get(((VarInsnNode) current).var).name));
                            insnList.add(new MethodInsnNode(INVOKESTATIC,
                                    PackageInfo.getNameWithSlash(ExecutionTracer.class), "passedBranchingVariable",
                                    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V",
                                    false));
                            mn.instructions.insert(current, insnList);
                            count++;
                            mn.maxStack += 5;
                            break;
                        // endregion
                        // region We don't care about all the other instructions.
                        default:
                            break;
                        // endregion
                    }
                }
            } else if (opcode == LCMP) {
                // comparing two long values:
                int count = 0;
                AbstractInsnNode current = instruction;
                while (count < 2) {
                    current = current.getPrevious();
                    switch (current.getOpcode()) {
                        // region One of the longs is a constant, which we don't care about. Just increase the counter.
                        case LCONST_0:
                        case LCONST_1:
                        case LDC:
                            count++;
                            break;
                        // endregion
                        // region One of the longs is a field of the object or a static field of the class
                        case GETSTATIC:
                        case GETFIELD:
                            // todo for now we only increase the counter, but maybe they should be dealt with
                            //  differently as they may be related to crashes
                            count++;
                            break;
                        // endregion
                        // region One of the longs is a return value from a method call
                        case INVOKEVIRTUAL:
                        case INVOKESPECIAL:
                        case INVOKESTATIC:
                        case INVOKEINTERFACE:
                        case INVOKEDYNAMIC:
                            // todo for now we only increase the counter, but maybe they should be dealt with
                            //  differently as they may be related to crashes
                            count++;
                            break;
                        // endregion
                        // region One of the longs is a local variable, we log it into the ExecutionTracer
                        case LLOAD:
                            InsnList insnList = new InsnList();
                            insnList.add(new InsnNode(DUP));
                            insnList.add(new MethodInsnNode(INVOKESTATIC, PackageInfo.getNameWithSlash(Long.class),
                                    "valueOf", "(J)Ljava/lang/Long;", false));
                            insnList.add(new LdcInsnNode(className));
                            insnList.add(new LdcInsnNode(methodName));
                            insnList.add(new LdcInsnNode(currentLine));
                            insnList.add(new LdcInsnNode(mn.localVariables.get(((VarInsnNode) current).var).name));
                            insnList.add(new MethodInsnNode(INVOKESTATIC,
                                    PackageInfo.getNameWithSlash(ExecutionTracer.class), "passedBranchingVariable",
                                    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V",
                                    false));
                            mn.instructions.insert(current, insnList);
                            count++;
                            mn.maxStack += 5;
                            break;
                        // endregion
                        // region We don't care about all the other instructions.
                        default:
                            break;
                        // endregion
                    }
                }
            } else if (opcode >= IF_ICMPEQ && opcode <= IF_ICMPLE) {
                // comparing two byte/char/int/short/boolean values:
                int count = 0;
                AbstractInsnNode current = instruction;
                while (count < 2) {
                    current = current.getPrevious();
                    switch (current.getOpcode()) {
                        // region One of the integers is a constant, which we don't care about. Just increase the
                        // counter.
                        case ICONST_M1:
                        case ICONST_0:
                        case ICONST_1:
                        case ICONST_2:
                        case ICONST_3:
                        case ICONST_4:
                        case ICONST_5:
                        case BIPUSH:
                        case SIPUSH:
                        case LDC:
                            count++;
                            break;
                        // endregion
                        // region One of the integers is a field of the object or a static field of the class
                        case GETSTATIC:
                        case GETFIELD:
                            // todo for now we only increase the counter, but maybe they should be dealt with
                            //  differently as they may be related to crashes
                            count++;
                            break;
                        // endregion
                        // region One of the integers is a return value from a method call
                        case INVOKEVIRTUAL:
                        case INVOKESPECIAL:
                        case INVOKESTATIC:
                        case INVOKEINTERFACE:
                        case INVOKEDYNAMIC:
                            // todo for now we only increase the counter, but maybe they should be dealt with
                            //  differently as they may be related to crashes
                            count++;
                            break;
                        // endregion
                        // region One of the integers is a local variable, we log it into the ExecutionTracer
                        case ILOAD:
                            LocalVariableNode localVariable = mn.localVariables.get(((VarInsnNode) current).var);
                            String variableType = localVariable.desc;
                            String variableName = localVariable.name;

                            InsnList insnList = new InsnList();
                            insnList.add(new InsnNode(DUP));
                            // region Primitive types have to be turned into a wrapper class object
                            switch (variableType) {
                                case "B":
                                    insnList.add(new MethodInsnNode(INVOKESTATIC,
                                            PackageInfo.getNameWithSlash(Byte.class), "valueOf", "(B)Ljava/lang/Byte;"
                                            , false));
                                    break;
                                case "C":
                                    insnList.add(new MethodInsnNode(INVOKESTATIC,
                                            PackageInfo.getNameWithSlash(Character.class), "valueOf",
                                            "(C)Ljava/lang" + "/Character;", false));
                                    break;
                                case "S":
                                    insnList.add(new MethodInsnNode(INVOKESTATIC,
                                            PackageInfo.getNameWithSlash(Short.class), "valueOf", "(S)Ljava/lang" +
                                            "/Short;", false));
                                    break;
                                case "I":
                                    insnList.add(new MethodInsnNode(INVOKESTATIC,
                                            PackageInfo.getNameWithSlash(Integer.class), "valueOf",
                                            "(I)Ljava/lang" + "/Integer;", false));
                                    break;
                                case "Z":
                                    insnList.add(new MethodInsnNode(INVOKESTATIC,
                                            PackageInfo.getNameWithSlash(Boolean.class), "valueOf",
                                            "(Z)Ljava/lang" + "/Boolean;", false));
                                    break;
                            }
                            // endregion
                            insnList.add(new LdcInsnNode(className));
                            insnList.add(new LdcInsnNode(methodName));
                            insnList.add(new LdcInsnNode(currentLine));
                            insnList.add(new LdcInsnNode(variableName));
                            insnList.add(new MethodInsnNode(INVOKESTATIC,
                                    PackageInfo.getNameWithSlash(ExecutionTracer.class), "passedBranchingVariable",
                                    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V",
                                    false));
                            mn.instructions.insert(current, insnList);
                            count++;
                            mn.maxStack += 5;
                            break;
                        // endregion
                        // region We don't care about all the other instructions.
                        default:
                            break;
                        // endregion
                    }
                }
            } else if (opcode == IF_ACMPEQ || opcode == IF_ACMPNE) {
                // comparing two references
                int count = 0;
                AbstractInsnNode current = instruction;
                while (count < 2) {
                    current = current.getPrevious();
                    switch (current.getOpcode()) {
                        case ACONST_NULL:
                            count++;
                            break;
                        case GETSTATIC:
                        case GETFIELD:
                            count++;
                            break;
                        case INVOKEVIRTUAL:
                        case INVOKESPECIAL:
                        case INVOKESTATIC:
                        case INVOKEINTERFACE:
                        case INVOKEDYNAMIC:
                            count++;
                            break;
                        case ALOAD:
                            InsnList insnList = new InsnList();
                            insnList.add(new InsnNode(DUP));
                            insnList.add(new LdcInsnNode(className));
                            insnList.add(new LdcInsnNode(methodName));
                            insnList.add(new LdcInsnNode(currentLine));
                            insnList.add(new LdcInsnNode(mn.localVariables.get(((VarInsnNode) current).var).name));
                            insnList.add(new MethodInsnNode(INVOKESTATIC,
                                    PackageInfo.getNameWithSlash(ExecutionTracer.class), "passedBranchingVariable",
                                    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V",
                                    false));
                            mn.instructions.insert(current, insnList);
                            count++;
                            mn.maxStack += 5;
                            break;
                        case NEW:
                            // todo new object maybe should be logged as well?
                            count++;
                            break;
                    }
                }
            } else if (opcode >= IFEQ && opcode <= IFLE) {
                // comparing one integer with 0
                int count = 0;
                AbstractInsnNode current = instruction;
                while (count < 1) {
                    current = current.getPrevious();
                    switch (current.getOpcode()) {
                        // region The integer is a result of a previous comparision, increase the counter and ignore it.
                        case LCMP:
                        case FCMPL:
                        case FCMPG:
                        case DCMPL:
                        case DCMPG:
                            count++;
                            break;
                        // endregion
                        // region The integer is a field of the object or a static field of the class
                        case GETSTATIC:
                        case GETFIELD:
                            // todo for now we only increase the counter, but maybe they should be dealt with
                            //  differently as they may be related to crashes
                            count++;
                            break;
                        // endregion
                        // region The integer is a return value from a method call
                        case INVOKEVIRTUAL:
                        case INVOKESPECIAL:
                        case INVOKESTATIC:
                        case INVOKEINTERFACE:
                        case INVOKEDYNAMIC:
                            // todo for now we only increase the counter, but maybe they should be dealt with
                            //  differently as they may be related to crashes
                            count++;
                            break;
                        // endregion
                        // region The integer is a local variable, we log it into the ExecutionTracer
                        case ILOAD:
                            LocalVariableNode localVariable = mn.localVariables.get(((VarInsnNode) current).var);
                            String variableType = localVariable.desc;
                            String variableName = localVariable.name;

                            InsnList insnList = new InsnList();
                            insnList.add(new InsnNode(DUP));
                            // region Primitive types have to be turned into a wrapper class object
                            switch (variableType) {
                                case "B":
                                    insnList.add(new MethodInsnNode(INVOKESTATIC,
                                            PackageInfo.getNameWithSlash(Byte.class), "valueOf", "(B)Ljava/lang/Byte;"
                                            , false));
                                    break;
                                case "C":
                                    insnList.add(new MethodInsnNode(INVOKESTATIC,
                                            PackageInfo.getNameWithSlash(Character.class), "valueOf",
                                            "(C)Ljava/lang" + "/Character;", false));
                                    break;
                                case "S":
                                    insnList.add(new MethodInsnNode(INVOKESTATIC,
                                            PackageInfo.getNameWithSlash(Short.class), "valueOf", "(S)Ljava/lang" +
                                            "/Short;", false));
                                    break;
                                case "I":
                                    insnList.add(new MethodInsnNode(INVOKESTATIC,
                                            PackageInfo.getNameWithSlash(Integer.class), "valueOf",
                                            "(I)Ljava/lang" + "/Integer;", false));
                                    break;
                                case "Z":
                                    insnList.add(new MethodInsnNode(INVOKESTATIC,
                                            PackageInfo.getNameWithSlash(Boolean.class), "valueOf",
                                            "(Z)Ljava/lang" + "/Boolean;", false));
                                    break;
                            }
                            // endregion
                            insnList.add(new LdcInsnNode(className));
                            insnList.add(new LdcInsnNode(methodName));
                            insnList.add(new LdcInsnNode(currentLine));
                            insnList.add(new LdcInsnNode(variableName));
                            insnList.add(new MethodInsnNode(INVOKESTATIC,
                                    PackageInfo.getNameWithSlash(ExecutionTracer.class), "passedBranchingVariable",
                                    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V",
                                    false));
                            mn.instructions.insert(current, insnList);
                            count++;
                            mn.maxStack += 5;
                            break;
                        // endregion
                        // region We don't care about all the other instructions.
                        default:
                            break;
                        // endregion
                    }
                }
            } else if (opcode == IFNULL || opcode == IFNONNULL) {
                // comparing one references with null
                int count = 0;
                AbstractInsnNode current = instruction;
                while (count < 1) {
                    current = current.getPrevious();
                    switch (current.getOpcode()) {
                        case ACONST_NULL:
                            count++;
                            break;
                        case GETSTATIC:
                        case GETFIELD:
                            count++;
                            break;
                        case INVOKEVIRTUAL:
                        case INVOKESPECIAL:
                        case INVOKESTATIC:
                        case INVOKEINTERFACE:
                        case INVOKEDYNAMIC:
                            count++;
                            break;
                        case ALOAD:
                            InsnList insnList = new InsnList();
                            insnList.add(new InsnNode(DUP));
                            insnList.add(new LdcInsnNode(className));
                            insnList.add(new LdcInsnNode(methodName));
                            insnList.add(new LdcInsnNode(currentLine));
                            insnList.add(new LdcInsnNode(mn.localVariables.get(((VarInsnNode) current).var).name));
                            insnList.add(new MethodInsnNode(INVOKESTATIC,
                                    PackageInfo.getNameWithSlash(ExecutionTracer.class), "passedBranchingVariable",
                                    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V",
                                    false));
                            mn.instructions.insert(current, insnList);
                            count++;
                            mn.maxStack += 5;
                            break;
                        case NEW:
                            // todo new object maybe should be logged as well?
                            count++;
                            break;
                    }
                }
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
}
