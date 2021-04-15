package com.example.examplemod

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollUnit
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.shader.Framebuffer
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.GL_TEXTURE_2D_MULTISAMPLE
import org.lwjgl.opengl.GL32.glTexImage2DMultisample
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.CoroutineContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.texture.TextureUtil
import org.lwjgl.opengl.GL33.GL_SAMPLER_BINDING
import org.lwjgl.opengl.GL33.glBindSampler
import org.lwjgl.opengl.GL43.GL_SAMPLER
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.coroutines.resume


class MSAAFramebuffer(w: Int, h: Int, depth: Boolean) :
    Framebuffer(w, h, depth) {
    init {
        framebufferColor[0] = 0f
        framebufferColor[1] = 0f
        framebufferColor[2] = 0f
        framebufferColor[3] = 0f
    }

    override fun bindFramebufferTexture() {
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, framebufferTexture)
    }

    override fun unbindFramebufferTexture() {
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0)
    }

    override fun createFramebuffer(p_createFramebuffer_1_: Int, p_createFramebuffer_2_: Int) {
        framebufferWidth = p_createFramebuffer_1_
        framebufferHeight = p_createFramebuffer_2_
        framebufferTextureWidth = p_createFramebuffer_1_
        framebufferTextureHeight = p_createFramebuffer_2_

        framebufferObject = OpenGlHelper.glGenFramebuffers()
        framebufferTexture = TextureUtil.glGenTextures()


        bindFramebufferTexture()
        glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, 4, GL_RGBA8, framebufferTextureWidth, framebufferTextureHeight, false)
        unbindFramebufferTexture()

        bindFramebuffer(false)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D_MULTISAMPLE, framebufferTexture, 0)


        framebufferClear()
        unbindFramebufferTexture()
    }

}

data class TimedRequest(val timeMillis: Long, val continuation: CancellableContinuation<Unit>) {
}

@OptIn(ExperimentalFoundationApi::class)
class MyGui : GuiScreen() {


    val queue = ArrayDeque<Runnable>()
    val timedQueue = PriorityQueue<TimedRequest>(compareBy { it.timeMillis })

    @OptIn(InternalCoroutinesApi::class)
    private val composeLayer = ComposeLayer(object : CoroutineDispatcher(), Delay {
        val creator = Thread.currentThread()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            if (creator != Thread.currentThread()) {
                error("!")
            }
            queue.addLast(block)
        }

        override fun isDispatchNeeded(context: CoroutineContext): Boolean {
            return true
        }

        override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
            timedQueue += TimedRequest(System.currentTimeMillis() + timeMillis, continuation)
        }
    })

    init {
        composeLayer.setContent {
            MaterialTheme {
                Surface(modifier = androidx.compose.ui.Modifier.fillMaxHeight().fillMaxWidth(0.25f), color = Color.White.copy(alpha = 0.7f)) {

                    val lines = remember { mutableStateListOf<String>() }

                    val scrollState = rememberScrollState(0f)


                    Box {
                        ScrollableColumn(scrollState = scrollState) {
                            Button(
                                onClick = { lines += "Line #${lines.size + 1}" },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Add new line")
                            }
                            for (line in lines) {
                                Text(line)
                            }
                        }
                        VerticalScrollbar(
                            rememberScrollbarAdapter(scrollState),
                            Modifier.align(Alignment.CenterEnd)
                        )
                    }
//                    LazyColumn {
//                        stickyHeader {
//                            Button(onClick = { lines += "Line #${lines.size + 1}" }, shape = RoundedCornerShape(10.dp)) {
//                                Text("Add new line")
//                            }
//                        }
//                        items(lines) {
//                            Text(it)
//                        }
//                    }

                }

            }
        }
    }

    override fun initGui() {

        glEnable(GL_MULTISAMPLE)
        msaaFbo = MSAAFramebuffer(mc.displayWidth, mc.displayHeight, false)
        glDisable(GL_MULTISAMPLE)

        targetFbo = Framebuffer(mc.displayWidth, mc.displayHeight, false)

        composeLayer.wrapped.setSize(mc.displayWidth, mc.displayHeight)
        composeLayer.needRedrawLayer()
        composeLayer.reinit()
//
//        composeLayer.renderer = object : ComposeLayer.Renderer {
//
//
//            val loader = FontLoader()
//
//            val p by lazy {
//                val pb = ParagraphBuilder(ParagraphStyle().apply {
//                    alignment = Alignment.START
//                    println("S: " + this.isHintingEnabled)
//                }, loader.fonts)
////                pb.
//                pb.pushStyle(org.jetbrains.skija.paragraph.TextStyle().apply {
////                    this.background = Paint().setARGB(255, 0, 0, 0);
//                    this.color = Color.makeARGB(255, 0, 0, 0)
//                    this.fontFamilies = listOf<String>().toTypedArray()
//                    this.fontStyle = FontStyle.NORMAL
//                    this.decorationStyle = DecorationStyle(false, false, false, false, this.color, DecorationLineStyle.SOLID, 1f)
//                    this.fontStyle = this.fontStyle.withWeight(400)
//                    this.addShadow(Shadow(Color.makeARGB(255, 0, 0, 0), 0f, 0f, 0.0))
//                    this.fontSize = 80f
//
////                    this.foreground = Paint().setARGB(255, 255, 0, 0)
//                })
//                pb.addText("AAADNISNIDI")
//                pb.popStyle()
//
//
//                pb.build()
//            }
//
//            override suspend fun onFrame(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
//
//                canvas.drawRRect(RRect.makeXYWH(0f, 0f, 300f, 300f, 5f), Paint().setARGB(255, 255, 255, 0))
////                canvas.drawString("AAAAAAAAAAAAA", 20f, 20f, org.jetbrains.skija.Font().setSize(25f), Paint().setARGB(255, 255, 0, 0))
//
//
//                p.layout(700f)
//                p.paint(canvas, 50f, 50f)
//
//                println("W: ${p.maxIntrinsicWidth} H: ${p.height}")
//
//
//
////                canvas.drawRect(Rect.makeXYWH(100f, 0f, 200f, 200f)!!, Paint().setARGB(255, 255, 0, 0))
////                canvas.drawRect(Rect.makeXYWH(300f, 0f, 200f, 200f)!!, Paint().setARGB(255, 0, 255, 0))
////                canvas.drawLine(0f, height.toFloat(), width.toFloat(), 0f, Paint().setARGB(255, 100, 100, 100))
////                canvas.drawLine(0f, 0f, width.toFloat(), height.toFloat(), Paint().setARGB(255, 100, 100, 100))
//
//            }
//
//        }
    }

    override fun onResize(p_onResize_1_: Minecraft, p_onResize_2_: Int, p_onResize_3_: Int) {
        msaaFbo.deleteFramebuffer()
        targetFbo.deleteFramebuffer()
//        composeLayer.reinit()
        super.onResize(p_onResize_1_, p_onResize_2_, p_onResize_3_)
    }


    class Context {
        var shader = 0
        var arrayBuffer = 0
        var activeTexture = 0
        var bindTexture = 0
        var bindSampler = 0
        var enableScissor = false


        val scissorBox = ByteBuffer.allocateDirect(4 * 16).order(ByteOrder.nativeOrder()).asIntBuffer()
        val enableVertexAttribArray: IntArray by lazy { IntArray(glGetInteger(GL_MAX_VERTEX_ATTRIBS)) }

    }

    val buf = ByteBuffer.allocateDirect(4 * 4).order(ByteOrder.nativeOrder())

    val mcContext = Context()
    val jetpackContext = Context()
    val renderContext = Context()


    fun checkGL() {
        glGetError().takeIf { it != 0 }?.let {
            error("BUG $it")
        }
    }

    fun saveGlContext(store: Context) {
        store.shader = glGetInteger(GL_CURRENT_PROGRAM)
        store.arrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        store.activeTexture = glGetInteger(GL_ACTIVE_TEXTURE)
        store.bindTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
        store.enableScissor = glGetBoolean(GL_SCISSOR_TEST)
        if (store.enableScissor) {
            glGetInteger(GL_SCISSOR_BOX, store.scissorBox.apply { clear() })
        }
        store.bindSampler = glGetInteger(GL_SAMPLER_BINDING)

        checkGL()

        for (index in store.enableVertexAttribArray.indices) {
            glGetVertexAttrib(index, GL_VERTEX_ATTRIB_ARRAY_ENABLED, buf.apply { clear() }.asIntBuffer())
            store.enableVertexAttribArray[index] = buf.getInt()
        }
    }

    fun restoreGlContext(load: Context) {
        for (index in load.enableVertexAttribArray.indices) {
            val v = load.enableVertexAttribArray[index]
            if (v == GL_FALSE)
                glDisableVertexAttribArray(index)
            else
                glEnableVertexAttribArray(index)
        }

        checkGL()


        glBindBuffer(GL_ARRAY_BUFFER, load.arrayBuffer)
        checkGL()
        glUseProgram(load.shader)
        checkGL()
        if (load.activeTexture != 0) glActiveTexture(load.activeTexture)
        checkGL()
        glBindTexture(GL_TEXTURE_2D, load.bindTexture)
        checkGL()

        if (load.enableScissor) {
            glEnable(GL_SCISSOR_TEST)
            glScissor(load.scissorBox[0], load.scissorBox[1], load.scissorBox[2], load.scissorBox[3])
        } else {
            glDisable(GL_SCISSOR_TEST)
        }
        checkGL()

        if (load.activeTexture != 0) glBindSampler(load.activeTexture - GL_TEXTURE0, load.bindSampler)

        checkGL()
    }



    lateinit var msaaFbo: Framebuffer
    lateinit var targetFbo: Framebuffer

    var pressed = false

    fun schedule(block: suspend CoroutineScope.() -> Unit) {
        GlobalScope.launch(composeLayer.dispatcher, block = block)
    }

    override fun handleMouseInput() {
        val x = Mouse.getEventX()
        val y = mc.displayHeight - Mouse.getEventY() - 1
        val k = Mouse.getEventButton()
        if (k != -1) {
            if (Mouse.getEventButton() == 0) {
                if (Mouse.getEventButtonState()) {
                    schedule { composeLayer.owners?.onMousePressed(x, y) }
                    pressed = true
                } else {
                    schedule { composeLayer.owners?.onMouseReleased(x, y) }
                    pressed = false
                }
            }
        } else if (Mouse.getEventDX() != 0 || Mouse.getEventDY() != 0) {
            if (pressed) {
                schedule { composeLayer.owners?.onMouseDragged(x, y) }
            }
            schedule { composeLayer.owners?.onMouseMoved(x, y) }
        } else if (Mouse.getEventDWheel() != 0) {
            schedule {
                composeLayer.owners?.onMouseScroll(
                    x,
                    y,
                    MouseScrollEvent(MouseScrollUnit.Page(Mouse.getEventDWheel() * -1f / mc.displayHeight), Orientation.Vertical)
                )
            }
        }
    }

    fun swapGlContextState(store: Context, load: Context) {
        saveGlContext(store)
        restoreGlContext(load)
    }

    override fun onGuiClosed() {
        composeLayer.dispose()
    }

    override fun drawScreen(p_drawScreen_1_: Int, p_drawScreen_2_: Int, p_drawScreen_3_: Float) {
        super.drawScreen(p_drawScreen_1_, p_drawScreen_2_, p_drawScreen_3_)

        val outputFb = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)

        checkGL()
        glDisable(GL_DEPTH_TEST)
        glDisable(GL_ALPHA_TEST)
        glDisable(GL_CULL_FACE)

        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)


        msaaFbo.framebufferClear()

        swapGlContextState(mcContext, jetpackContext)

        msaaFbo.bindFramebuffer(true)


        glEnable(GL_MULTISAMPLE)
        glEnable(GL_BLEND)


        checkGL()



        while(queue.isNotEmpty()) {
            val task = queue.removeFirst()
            task.run()
            checkGL()
        }

        val currentTime = System.currentTimeMillis()
        while(timedQueue.isNotEmpty()) {
            if (timedQueue.peek().timeMillis <= currentTime) {
                timedQueue.poll().continuation.resume(Unit)
            } else {
                break
            }
        }



        composeLayer.wrapped.draw()
        msaaFbo.unbindFramebuffer()

        checkGL()


        swapGlContextState(jetpackContext, mcContext)

        checkGL()

//        glDisable(GL_BLEND)

        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, targetFbo.framebufferObject)   // Make sure no FBO is set as the draw framebuffer
        glBindFramebuffer(GL_READ_FRAMEBUFFER, msaaFbo.framebufferObject) // Make sure your multisampled FBO is the read framebuffer
        glBlitFramebuffer(0, 0, mc.displayWidth, mc.displayHeight, 0, 0, mc.displayWidth, mc.displayHeight, GL_COLOR_BUFFER_BIT, GL_NEAREST)

        glDisable(GL_MULTISAMPLE)



        glBindFramebuffer(GL_FRAMEBUFFER, outputFb)
        targetFbo.framebufferRenderExt(mc.displayWidth, mc.displayHeight, false)

        checkGL()

        glEnable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
//        glEnable(GL_ALPHA_TEST)
        glEnable(GL_CULL_FACE)


    }

}