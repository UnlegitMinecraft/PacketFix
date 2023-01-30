package dev.sora.packetfix;

import by.radioegor146.nativeobfuscator.Native;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

@Native
public class FMLLoadingPlugin implements IFMLLoadingPlugin, IClassTransformer {

    private static byte[] hwid = null;
    private static byte[] userhwid = null;
    private static byte[] sig = null;
    private static String methodName = "";

    @Override
    public byte[] transform(final String name, final String transformedName, final byte[] basicClass) {
        if (transformedName.equals("net.minecraft.network.play.client.C08PacketPlayerBlockPlacement")) {
            final ClassNode classNode = readClass(basicClass);
            classNode.methods.stream().filter(it -> {
                MethodNode methodNode = (MethodNode)it;
                return (methodNode.name.equals("b") && methodNode.desc.equals("(Lem;)V")); // Notch Name
            })
                    .forEach(it -> {
                        MethodNode methodNode = (MethodNode)it;
                        for (int i = 0; i < methodNode.instructions.size(); ++i) {
                            final AbstractInsnNode abstractInsnNode = methodNode.instructions.get(i);
                            if (abstractInsnNode instanceof LdcInsnNode) {
                                final LdcInsnNode lin = (LdcInsnNode) abstractInsnNode;
                                if (lin.cst instanceof Float && lin.cst.equals(Float.valueOf(16f))) {
                                    methodNode.instructions.insertBefore(lin, new InsnNode(Opcodes.ICONST_1));
                                    methodNode.instructions.insertBefore(lin, new MethodInsnNode(Opcodes.INVOKESTATIC, FMLLoadingPlugin.class.getName().replaceAll("\\.", "/"), methodName, "(I)D"));
                                    methodNode.instructions.insertBefore(lin, new InsnNode(Opcodes.D2F));
                                    methodNode.instructions.remove(lin);
                                }
                            }
                        }
                    });
            return writeClass(classNode);
        } else if (transformedName.equals("net.minecraft.entity.EntityLivingBase")) {
            final ClassNode classNode = readClass(basicClass);
            for (Object it : classNode.methods) {
                MethodNode methodNode = (MethodNode) it;
                if (methodNode.name.equals("m") && methodNode.desc.equals("()V")) {
                    for (int i = 0; i < methodNode.instructions.size(); ++i) {
                        final AbstractInsnNode abstractInsnNode = methodNode.instructions.get(i);
                        if (abstractInsnNode instanceof LdcInsnNode) {
                            final LdcInsnNode lin = (LdcInsnNode) abstractInsnNode;
                            if (lin.cst instanceof Double && lin.cst.equals(Double.valueOf(0.005))) {
                                methodNode.instructions.insertBefore(lin, new InsnNode(Opcodes.ICONST_M1));
                                methodNode.instructions.insertBefore(lin, new MethodInsnNode(Opcodes.INVOKESTATIC, FMLLoadingPlugin.class.getName().replaceAll("\\.", "/"), methodName, "(I)D"));
                                methodNode.instructions.remove(lin);
                            }
                        }
                    }
                }
            }
            return writeClass(classNode);
        } else if (transformedName.equals("net.minecraft.client.entity.EntityPlayerSP")) {
            final ClassNode classNode = readClass(basicClass);
            for (Object it : classNode.methods) {
                MethodNode methodNode = (MethodNode) it;
                if (methodNode.name.equals("bw") && methodNode.desc.equals("()V")) {
                    for (int i = 0; i < methodNode.instructions.size(); ++i) {
                        final AbstractInsnNode abstractInsnNode = methodNode.instructions.get(i);
                        if (abstractInsnNode instanceof MethodInsnNode) {
                            final MethodInsnNode min = (MethodInsnNode) abstractInsnNode;
                            if (min.name.equals("a") && min.desc.equals("(Lff;)V")) {
                                methodNode.instructions.insertBefore(min, new InsnNode(Opcodes.POP));
                                methodNode.instructions.remove(min);
                            }
                        }
                    }
                }
            }
            return writeClass(classNode);
        } else if (transformedName.equals("net.minecraft.client.Minecraft")) {
            final ClassNode classNode = readClass(basicClass);
            for (Object it : classNode.methods) {
                MethodNode methodNode = (MethodNode) it;
                if (methodNode.name.equals("aw") && methodNode.desc.equals("()V")) {
                    FieldInsnNode thePlayer = null;
                    MethodInsnNode swingItem = null;
                    for (int i = 0; i < methodNode.instructions.size(); ++i) {
                        final AbstractInsnNode abstractInsnNode = methodNode.instructions.get(i);
                        if (thePlayer == null && abstractInsnNode instanceof VarInsnNode
                                && methodNode.instructions.get(i+1) instanceof FieldInsnNode
                                && methodNode.instructions.get(i+2) instanceof MethodInsnNode) {
                            thePlayer = (FieldInsnNode) methodNode.instructions.get(i+1);
                            swingItem = (MethodInsnNode) methodNode.instructions.get(i+2);
                            methodNode.instructions.remove(abstractInsnNode);
                            methodNode.instructions.remove(thePlayer);
                            methodNode.instructions.remove(swingItem);
                        } else if (thePlayer != null && abstractInsnNode instanceof InsnNode && abstractInsnNode.getOpcode() == Opcodes.RETURN
                                && !(methodNode.instructions.get(i-1) instanceof MethodInsnNode)) {
                            methodNode.instructions.insertBefore(abstractInsnNode, new VarInsnNode(Opcodes.ALOAD, 0));
                            methodNode.instructions.insertBefore(abstractInsnNode, thePlayer);
                            methodNode.instructions.insertBefore(abstractInsnNode, swingItem);
                        }
                    }
                }
            }
            return writeClass(classNode);
        }
        return basicClass;
    }

    public static double verify(int typ1) {
        if (typ1 == 1) {
            return 13 + Math.random();
        } else if (typ1 == -1) {
            return 0.003;
        } else {
            return Math.random() * 10;
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        boolean hasLabyMod = true;
        try {
            Class.forName("net.labymod.api.LabyModAPI");
        } catch(final Exception exception) {
            hasLabyMod = false;
        }
        if(hasLabyMod) {
            Display.setTitle()
        }
        verify(0);
        return new String[]{FMLLoadingPlugin.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> map) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    private ClassNode readClass(final byte[] classFile) {
        final ClassReader classReader = new ClassReader(classFile);
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        return classNode;
    }

    private byte[] writeClass(final ClassNode classNode) {
        final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
