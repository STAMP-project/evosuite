package org.evosuite.instrumentation.coverage;

import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.evosuite.runtime.instrumentation.AnnotatedLabel;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class BranchingVariableInstrumentation implements MethodInstrumentation {
    private static final Logger logger = LoggerFactory.getLogger(BranchingVariableInstrumentation.class);

    public static final String EXECUTION_TRACER = Type.getInternalName(ExecutionTracer.class);
    public static final String CHARACTER = Type.getInternalName(Character.class);
    public static final String INTEGER = Type.getInternalName(Integer.class);
    public static final String BOOLEAN = Type.getInternalName(Boolean.class);
    public static final String DOUBLE = Type.getInternalName(Double.class);
    public static final String SHORT = Type.getInternalName(Short.class);
    public static final String FLOAT = Type.getInternalName(Float.class);
    public static final String LONG = Type.getInternalName(Long.class);
    public static final String BYTE = Type.getInternalName(Byte.class);

    static final String LOG_METHOD_NAME = "passedBranchingVariable";
    static final String LOG_METHOD_DESC = "(Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;)V";
    static final String VALUE_OF = "valueOf";

    private MethodNode methodNode;
    private String className;

    private Map<LabelNode, Integer> labelLineMap = new HashMap<>();
    private Map<AbstractInsnNode, String> variableNameMap = new HashMap<>();
    private Map<String, String> variableTypeMap = new HashMap<>();
    private Map<Integer, Set<LocalVariableNode>> indexVariableMap = new HashMap<>();

    @Override
    public void analyze(ClassLoader classLoader, MethodNode methodNode, String className, String methodName,
                        int access) {
        this.methodNode = methodNode;
        this.className = className;

        RawControlFlowGraph graph = GraphPool.getInstance(classLoader).getRawCFG(className, methodName);
        boolean ignore = false;
        ListIterator<AbstractInsnNode> j = methodNode.instructions.iterator();
        while (j.hasNext()) {
            AbstractInsnNode in = j.next();
            for (BytecodeInstruction v : graph.vertexSet())
                if (in.equals(v.getASMNode()))
                    if (v.isBranch() && in.getOpcode() != JSR) {
                        if (in.getPrevious() instanceof LabelNode) {
                            LabelNode label = (LabelNode) in.getPrevious();
                            if (label.getLabel() instanceof AnnotatedLabel) {
                                AnnotatedLabel aLabel = (AnnotatedLabel) label.getLabel();
                                if (aLabel.isStartTag())
                                    if (aLabel.shouldIgnore())
                                        continue;
                            }
                        }
                        instrument(graph, v);
                    } else if (v.isLocalVariableUse() && !v.isIINC()) {
                        int lineNumber = v.getLineNumber();
                        int variableIndex = ((VarInsnNode) in).var;
                        for (LocalVariableNode localVariable : this.methodNode.localVariables)
                            if (localVariable.index == variableIndex) {
                                Integer start = labelLineMap.get(localVariable.start);
                                Integer end = labelLineMap.get(localVariable.end);
                                if (start != null && start <= lineNumber && (end == null || lineNumber <= end)) {
                                    variableNameMap.put(in, localVariable.name);
                                    variableTypeMap.put(localVariable.name, localVariable.desc);
                                    break;
                                }
                            }
                    } else if (v.isLabel())
                        if (graph.getInstruction(v.getInstructionId() + 1).isLineNumber()) {
                            labelLineMap.put((LabelNode) v.getASMNode(),
                                    graph.getInstruction(v.getInstructionId() + 1).getLineNumber());
                            ignore = false;
                        } else
                            ignore = true;
                    else
                        break;
        }
    }

    private void instrument(RawControlFlowGraph graph, BytecodeInstruction instruction) {
        int instructionId = instruction.getInstructionId();

        int opcode = instruction.getASMNode().getOpcode();
        if (opcode >= IF_ICMPEQ && opcode <= IF_ICMPLE) {
            logger.info("Comparing two byte/char/int/short/boolean values");
            logBranchingVariables(graph, instructionId, 2);
        } else if (opcode == IF_ACMPEQ || opcode == IF_ACMPNE) {
            logger.info("Comparing two references");
            logBranchingVariables(graph, instructionId, 2);
        } else if (opcode >= IFEQ && opcode <= IFLE) {
            logger.info("Comparing one byte/char/int/short/boolean against 0");
            BytecodeInstruction current = graph.getInstruction(instructionId--);
            if (current.isLocalVariableUse() && !current.isIINC()) {
                methodNode.instructions.insert(current.getASMNode(), getInstrumentation(current.getLineNumber(),
                        variableNameMap.get(current.getASMNode())));
            } else if (current.getASMNode().getOpcode() == DCMPL || current.getASMNode().getOpcode() == DCMPG || current.getASMNode() instanceof MethodInsnNode && (((MethodInsnNode) current.getASMNode()).name.equals("doubleSubL") || ((MethodInsnNode) current.getASMNode()).name.equals("doubleSubG")))
                logBranchingVariables(graph, instructionId, 2);
            else if (current.getASMNode().getOpcode() == FCMPL || current.getASMNode().getOpcode() == FCMPG || current.getASMNode() instanceof MethodInsnNode && (((MethodInsnNode) current.getASMNode()).name.equals("floatSubL") || ((MethodInsnNode) current.getASMNode()).name.equals("floatSubG")))
                logBranchingVariables(graph, instructionId, 2);
            else if (current.getASMNode().getOpcode() == LCMP || current.getASMNode() instanceof MethodInsnNode && (((MethodInsnNode) current.getASMNode()).name.equals("longSub")))
                logBranchingVariables(graph, instructionId, 2);
        } else if (opcode == IFNULL || opcode == IFNONNULL) {
            logger.info("Comparing one reference against NULL");
            logBranchingVariables(graph, instructionId, 1);
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
     * @param type The type of the local variable.
     */
    private static AbstractInsnNode valueOf(String type) {
        switch (type) {
            case "B":
                return new MethodInsnNode(INVOKESTATIC, BYTE, VALUE_OF, "(B)Ljava/lang/Byte;", false);
            case "C":
                return new MethodInsnNode(INVOKESTATIC, CHARACTER, VALUE_OF, "(C)Ljava/lang/Character;", false);
            case "D":
                return new MethodInsnNode(INVOKESTATIC, DOUBLE, VALUE_OF, "(D)Ljava/lang/Double;", false);
            case "F":
                return new MethodInsnNode(INVOKESTATIC, FLOAT, VALUE_OF, "(F)Ljava/lang/Float;", false);
            case "I":
                return new MethodInsnNode(INVOKESTATIC, INTEGER, VALUE_OF, "(I)Ljava/lang/Integer;", false);
            case "J":
                return new MethodInsnNode(INVOKESTATIC, LONG, VALUE_OF, "(J)Ljava/lang/Long;", false);
            case "S":
                return new MethodInsnNode(INVOKESTATIC, SHORT, VALUE_OF, "(S)Ljava/lang/Short;", false);
            case "Z":
                return new MethodInsnNode(INVOKESTATIC, BOOLEAN, VALUE_OF, "(Z)Ljava/lang/Boolean;", false);
            default:
                return new InsnNode(NOP);
        }
    }

    /**
     * Get the instrumentation to log a local variable into the {@link ExecutionTracer}.
     *
     * @return A list of instructions to perform the logging process.
     */
    private InsnList getInstrumentation(int currentLine, String variableName) {
        InsnList insnList = new InsnList();
        if (variableName != null) {
            insnList.add(new InsnNode(DUP));
            insnList.add(valueOf(variableTypeMap.get(variableName)));
            insnList.add(new LdcInsnNode(className));
            insnList.add(new LdcInsnNode(currentLine));
            insnList.add(new LdcInsnNode(variableName));
            methodNode.maxStack += 4;
            insnList.add(new MethodInsnNode(INVOKESTATIC, EXECUTION_TRACER, LOG_METHOD_NAME, LOG_METHOD_DESC, false));
        }
        return insnList;
    }

    private void logBranchingVariables(RawControlFlowGraph graph, int instructionId, int numberOfVariables) {
        int count;
        BytecodeInstruction current;
        count = 0;
        while (count < numberOfVariables) {
            current = graph.getInstruction(instructionId--);
            if (current.isConstant() || current.isFieldNodeUse() || current.getASMNode().getOpcode() == NEW)
                count++;
            else if (current.isMethodCall() || current.getASMNode().getOpcode() == INVOKEDYNAMIC)
                break;
            else if (current.isLocalVariableUse() && !current.isIINC()) {
                count++;
                methodNode.instructions.insert(current.getASMNode(), getInstrumentation(current.getLineNumber(),
                        variableNameMap.get(current.getASMNode())));
            } else if (current.loadsReferenceToThis()) {
                count++;
            }
        }
    }
}
