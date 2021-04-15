package com.example.examplemod

import net.minecraft.launchwrapper.IClassTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode


class Transform : IClassTransformer {
    override fun transform(name: String, transformedName: String?, basicClass: ByteArray): ByteArray {
        return when (name) {
            "net.minecraftforge.client.ForgeHooksClient" -> {
                val node = ClassNode()
                ClassReader(basicClass).accept(node, 0)
                for (methodNode in node.methods) {
                    val q = methodNode
                    for (insn in q.instructions) {
                        if (insn is MethodInsnNode && insn.name == "setClientActiveTexture") {
                            insn.name = "setActiveTexture"
                        }
                    }
                }
                ClassWriter(0).also { node.accept(it) }.toByteArray()
            }
            "net.minecraft.client.model.ModelBoat" -> {
                val node = ClassNode()
                ClassReader(basicClass).accept(node, 0)
                val q = node.methods.single { it.name == "<init>" }
                for (insn in q.instructions) {
                    if (insn is MethodInsnNode && insn.name == "generateDisplayLists") {
                        q.instructions.remove(insn.previous)
                        q.instructions.insertBefore(insn, LdcInsnNode(0))
                        q.instructions.remove(insn)
                        break
                    }
                }
                ClassWriter(0).also { node.accept(it) }.toByteArray()
            }
            else -> {
                basicClass
            }
        }
    }
}