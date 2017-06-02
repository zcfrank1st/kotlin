/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.optimization

import org.jetbrains.kotlin.codegen.optimization.common.expect
import org.jetbrains.kotlin.codegen.optimization.common.matchInsns
import org.jetbrains.kotlin.codegen.optimization.common.opcode
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.JumpInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class PeepholeMethodTransformer : MethodTransformer() {
    private class PeepholerContext(val methodNode: MethodNode) {
        // TODO
    }

    private interface Peepholer {
        fun tryRewrite(instructions: InsnList, insn: AbstractInsnNode, context: PeepholerContext): AbstractInsnNode?
    }

    private val peepholers = listOf(IfNotPeepholer)

    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val instructions = methodNode.instructions
        val context = PeepholerContext(methodNode)

        do {
            var changes = false
            var insn: AbstractInsnNode? = instructions.first

            while (insn != null) {
                var rewrittenNext: AbstractInsnNode? = null
                for (peepholer in peepholers) {
                    rewrittenNext = peepholer.tryRewrite(instructions, insn, context) ?: continue
                    changes = true
                    break
                }
                insn = rewrittenNext ?: insn.next
            }
        } while (changes)
    }


    private object IfNotPeepholer : Peepholer {
        override fun tryRewrite(instructions: InsnList, insn: AbstractInsnNode, context: PeepholerContext): AbstractInsnNode? {
            insn.matchInsns {
                val iConst1Insn = opcode(Opcodes.ICONST_1)
                val iXorInsn = opcode(Opcodes.IXOR)
                val ifInsn = expect<JumpInsnNode> { opcode == Opcodes.IFEQ || opcode == Opcodes.IFNE }

                instructions.run {
                    remove(iConst1Insn)
                    remove(iXorInsn)
                    val invertedOpcode = Opcodes.IFNE + Opcodes.IFEQ - ifInsn.opcode
                    val newIfInsn = JumpInsnNode(invertedOpcode, ifInsn.label)
                    set(ifInsn, newIfInsn)
                    return newIfInsn
                }
            }

            return null
        }
    }

}