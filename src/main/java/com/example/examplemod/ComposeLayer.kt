package com.example.examplemod


import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.runtime.*
import androidx.compose.runtime.dispatch.DefaultMonotonicFrameClock
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.Owner
import androidx.compose.ui.platform.*
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.selection.Selection
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.*
import org.jetbrains.skija.Canvas
import org.jetbrains.skiko.HardwareLayer
import org.jetbrains.skija.Picture
import org.jetbrains.skija.PictureRecorder
import org.jetbrains.skija.Rect
import org.jetbrains.skiko.SkiaRenderer
import java.awt.DisplayMode
import java.awt.Point
import java.awt.event.FocusEvent
import java.awt.event.InputMethodEvent
import java.awt.event.InputMethodListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.MouseWheelEvent
import java.awt.im.InputMethodRequests
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaMethod

internal class MutableResource<T : AutoCloseable> : AutoCloseable {
    @Volatile
    private var resource: T? = null
    @Volatile
    private var resourceToClose: T? = null
    @Volatile
    private var usingResource: T? = null

    /**
     * Close internal resource.
     *
     * If we close resource when it is using in [useWithoutClosing], we defer it's closing.
     *
     * If resource isn't using we close it immediately.
     *
     * After close we can use [set] (for set new internal resource)
     * and [useWithoutClosing] (it will be called with null resource)
     */
    override fun close() = set(null)

    /**
     * Change internal resource and close previous.
     *
     * If we set resource when previous is using in [useWithoutClosing], we defer it's closing.
     *
     * If previous isn't using we close it immediately.
     */
    fun set(resource: T?): Unit = synchronized(this) {
        val oldResource = this.resource
        this.resource = resource
        if (oldResource === usingResource) {
            resourceToClose = oldResource
        } else {
            oldResource?.close()
        }
    }

    /**
     * Can be called from the other thread.
     *
     * If we [set] new resource when we using previous, we close previous after using.
     */
    fun useWithoutClosing(use: (T?) -> Unit) {
        synchronized(this) {
            usingResource = resource
        }
        try {
            use(usingResource)
        } finally {
            synchronized(this) {
                usingResource = null
                resourceToClose?.close()
                resourceToClose = null
            }
        }
    }
}

internal class ComposeLayer(val dispatcher: CoroutineDispatcher) {

//    private val events = AWTDebounceEventQueue()

    var owners: DesktopOwners? = null
        set(value) {
            field = value
            renderer = value?.let(::OwnersRenderer)
        }

    var renderer: Renderer? = null

    private var isDisposed = false
    private var frameNanoTime = 0L
    private val frameDispatcher = FrameDispatcher(
        onFrame = { onFrame(it) },
        framesPerSecond = ::getFramesPerSecond,
        context = dispatcher
    )

    private val picture = MutableResource<Picture>()
    private val pictureRecorder = PictureRecorder()
    private val snapshotManager = GlSnapshotManager(dispatcher)

    private suspend fun onFrame(nanoTime: Long) {
        this.frameNanoTime = nanoTime
        preparePicture(frameNanoTime)
//        wrapped.redrawLayer()
    }

    var onDensityChanged: ((Density) -> Unit)? = null

    fun onDensityChanged(action: ((Density) -> Unit)?) {
        onDensityChanged = action
    }

    private var _density: Density? = null
    val density
        get() = _density ?: detectCurrentDensity().also {
            _density = it
        }

    inner class Wrapped : SkiaLayer(), DesktopComponent {
//        var currentInputMethodRequests: InputMethodRequests? = null

//        override fun getInputMethodRequests() = currentInputMethodRequests

        override fun enableInput(inputMethodRequests: InputMethodRequests) {
//            currentInputMethodRequests = inputMethodRequests
//            enableInputMethods(true)
//            val focusGainedEvent = FocusEvent(this, FocusEvent.FOCUS_GAINED)
//            inputContext.dispatchEvent(focusGainedEvent)
        }

        override fun disableInput() {
//            currentInputMethodRequests = null
        }

        @Suppress("ACCIDENTAL_OVERRIDE")
        override val locationOnScreen: Point
           get() = super.getLocationOnScreen()

        override val density: Density
            get() = this@ComposeLayer.density

        override fun scaleCanvas(dpi: Float) {}

        override val contentScale: Float
            get() = 1f
    }

    internal val wrapped = Wrapped()

    val component: HardwareLayer
        get() = wrapped

    init {
        wrapped.renderer = object : com.example.examplemod.SkiaRenderer {
            override fun onRender(canvas: Canvas, width: Int, height: Int) {
                try {
                    picture.useWithoutClosing {
                        it?.also(canvas::drawPicture)
                    }
                } catch (e: Throwable) {
                    e.printStackTrace(System.err)
                }
            }

            override fun onDispose() = Unit
            override fun onInit() = Unit
            override fun onReshape(width: Int, height: Int) = Unit
        }
        initCanvas()
    }

    private fun initCanvas() {
//        wrapped.addInputMethodListener(object : InputMethodListener {
//            override fun caretPositionChanged(p0: InputMethodEvent?) {
//                TODO("Implement input method caret change")
//            }
//
//            override fun inputMethodTextChanged(event: InputMethodEvent) = events.post {
//                owners?.onInputMethodTextChanged(event)
//            }
//        })
//
//        wrapped.addMouseListener(object : MouseAdapter() {
//            override fun mouseClicked(event: MouseEvent) = Unit
//
//            override fun mousePressed(event: MouseEvent) = events.post {
//                owners?.onMousePressed(
//                    (event.x * density.density).toInt(),
//                    (event.y * density.density).toInt()
//                )
//            }
//
//            override fun mouseReleased(event: MouseEvent) = events.post {
//                owners?.onMouseReleased(
//                    (event.x * density.density).toInt(),
//                    (event.y * density.density).toInt()
//                )
//            }
//        })
//        wrapped.addMouseMotionListener(object : MouseMotionAdapter() {
//            override fun mouseDragged(event: MouseEvent) = events.post {
//                owners?.onMouseDragged(
//                    (event.x * density.density).toInt(),
//                    (event.y * density.density).toInt()
//                )
//            }
//
//            override fun mouseMoved(event: MouseEvent) = events.post {
//                owners?.onMouseMoved(
//                    (event.x * density.density).toInt(),
//                    (event.y * density.density).toInt()
//                )
//            }
//        })
//        wrapped.addMouseWheelListener { event ->
//            events.post {
//                owners?.onMouseScroll(
//                    (event.x * density.density).toInt(),
//                    (event.y * density.density).toInt(),
//                    event.toComposeEvent()
//                )
//            }
//        }
//        wrapped.addKeyListener(object : KeyAdapter() {
//            override fun keyPressed(event: KeyEvent) = events.post {
//                owners?.onKeyPressed(event)
//            }
//
//            override fun keyReleased(event: KeyEvent) = events.post {
//                owners?.onKeyReleased(event)
//            }
//
//            override fun keyTyped(event: KeyEvent) = events.post {
//                owners?.onKeyTyped(event)
//            }
//        })
    }

    private class OwnersRenderer(private val owners: DesktopOwners) : ComposeLayer.Renderer {
        override suspend fun onFrame(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
            owners.onFrame(canvas, width, height, nanoTime)
        }
    }

    private fun MouseWheelEvent.toComposeEvent() = MouseScrollEvent(
        delta = if (scrollType == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
            MouseScrollUnit.Page((scrollAmount * preciseWheelRotation).toFloat())
        } else {
            MouseScrollUnit.Line((scrollAmount * preciseWheelRotation).toFloat())
        },

        // There are no other way to detect horizontal scrolling in AWT
        orientation = if (isShiftDown) {
            Orientation.Horizontal
        } else {
            Orientation.Vertical
        }
    )

    // We draw into picture, because SkiaLayer.draw can be called from the other thread,
    // but onRender should be called in AWT thread. Picture doesn't add any visible overhead on
    // CPU/RAM.
    private suspend fun preparePicture(frameTimeNanos: Long) {
        val bounds = Rect.makeWH(wrapped.width * density.density, wrapped.height * density.density)
        val pictureCanvas = pictureRecorder.beginRecording(bounds)
        renderer?.onFrame(
            pictureCanvas,
            (wrapped.width * density.density).toInt(),
            (wrapped.height * density.density).toInt(),
            frameTimeNanos
        )
        picture.set(pictureRecorder.finishRecordingAsPicture())
    }

    fun reinit() {
        val currentDensity = detectCurrentDensity()
        if (_density != currentDensity) {
            _density = currentDensity
            onDensityChanged?.invoke(density)
        }
        check(!isDisposed)
        wrapped.reinit()
    }


    private fun detectCurrentDensity(): Density {

        return Density(1f, 1f)
    }

    private fun getFramesPerSecond(): Float {
        return 30f
    }

    fun updateLayer() {
        check(!isDisposed)
        wrapped.updateLayer()
    }

    fun dispose() {
//        events.cancel()
        check(!isDisposed)
        frameDispatcher.cancel()
//        wrapped.disposeLayer()
        picture.close()
        pictureRecorder.close()
        isDisposed = true
        snapshotManager.dispose()
    }

    internal fun needRedrawLayer() {
        check(!isDisposed)
        frameDispatcher.scheduleFrame()
    }

    interface Renderer {
        suspend fun onFrame(canvas: Canvas, width: Int, height: Int, nanoTime: Long)
    }

    internal fun setContent(
        parent: Any? = null,
        invalidate: () -> Unit = this::needRedrawLayer,
        content: @Composable () -> Unit
    ): Composition {
        check(owners == null) {
            "Cannot setContent twice."
        }
        val desktopOwners = DesktopOwners(wrapped, invalidate)
        val desktopOwner = DesktopOwner(desktopOwners, density)

        owners = desktopOwners
        val composition = desktopOwner.setContent(snapshotManager, content)

        onDensityChanged(desktopOwner::density::set)

//        when (parent) {
//            is AppFrame -> parent.onDispose = desktopOwner::dispose
//            is ComposePanel -> parent.onDispose = desktopOwner::dispose
//        }

        return composition
    }
}


//@OptIn(ExperimentalKeyInput::class)
@Composable
private fun ProvideDesktopAmbients(owner: DesktopOwner, content: @Composable () -> Unit) {
    val animationClock = owner.container::class.members.find { it.name == "animationClock" }!!.call(owner.container)
    val selectionManagerTracker = owner::class.memberProperties.single { it.name == "selectionManager" }.call(owner)
    Providers(
        DesktopOwnersAmbient provides owner.container,
        SelectionManagerTrackerAmbient provides selectionManagerTracker as SelectionManagerTracker
    ) {
        ProvideCommonAmbients(
            owner = owner,
            animationClock = animationClock as AnimationClockObservable,
            uriHandler = DesktopUriHandler(),
            content = content
        )
    }
}

//@OptIn(ExperimentalFocusocus::class)
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ProvideCommonAmbients(
    owner: Owner,
    animationClock: AnimationClockObservable,
    uriHandler: UriHandler,
    content: @Composable () -> Unit
) {
    Providers(
        AmbientAnimationClock provides animationClock,
        AmbientAutofill provides owner.autofill,
        AmbientAutofillTree provides owner.autofillTree,
        AmbientClipboardManager provides owner.clipboardManager,
        AmbientDensity provides owner.density,
        AmbientFocusManager provides owner.focusManager,
        AmbientFontLoader provides owner.fontLoader,
        AmbientHapticFeedback provides owner.hapticFeedBack,
        AmbientLayoutDirection providesDefault owner.layoutDirection,
        AmbientTextInputService provides owner.textInputService,
        AmbientTextToolbar provides owner.textToolbar,
        AmbientUriHandler provides uriHandler,
        AmbientViewConfiguration provides owner.viewConfiguration,
        AmbientWindowManager provides owner.windowManager,
        content = content
    )
}

@Composable
fun DesktopSelectionContainer(content: @Composable () -> Unit) {
    val selection = remember { mutableStateOf<Selection?>(null) }
    DesktopSelectionContainer(
        selection = selection.value,
        onSelectionChange = { selection.value = it },
        content = content
    )
}

@OptIn(ExperimentalComposeApi::class)
internal fun DesktopOwner.setContent(snapshotManager: GlSnapshotManager, content: @Composable () -> Unit): Composition {
    snapshotManager.ensureStarted()


    val recomposer = run {
        val mainScope = CoroutineScope(
            NonCancellable + snapshotManager.dispatcher + DefaultMonotonicFrameClock
        )

        Recomposer(mainScope.coroutineContext).also {
            // NOTE: Launching undispatched so that compositions created with the
            // Recomposer.current() singleton instance can assume the recomposer is running
            // when they perform initial composition. The relevant Recomposer code is
            // appropriately thread-safe for this.
            mainScope.launch {
                it.runRecomposeAndApplyChanges()
            }
        }

    }

    val composition = compositionFor(root, DesktopUiApplier(root), recomposer)
    composition.setContent {
        ProvideDesktopAmbients(this) {
            DesktopSelectionContainer(content)
        }
    }

//    keyboard?.setShortcut(copyToClipboardKeySet) {
//        selectionManager.recentManager?.let { selector ->
//            selector.getSelectedText()?.let {
//                clipboardManager.setText(it)
//            }
//        }
//    }

    return composition
}

fun KFunction<*>.unreflect() = MethodHandles.lookup().unreflect(this.javaMethod)


internal class DesktopUiApplier(
    root: LayoutNode
) : AbstractApplier<LayoutNode>(root) {
    override fun insertTopDown(index: Int, instance: LayoutNode) {
        // ignored. Building tree bottom-up
    }

    inline fun <reified N> r() = object : ReadOnlyProperty<N, MethodHandle> {
        lateinit var handle: MethodHandle
        var thisSubj: Any? = null
        override fun getValue(thisRef: N, property: KProperty<*>): MethodHandle {
            if (thisSubj != thisRef) {
                handle = N::class.memberFunctions.single { it.name == property.name }.unreflect().bindTo(thisRef)
                thisSubj = thisRef
            }
            return handle
        }
    }


    val LayoutNode.insertAt by r()
    val LayoutNode.move by r()
    val LayoutNode.removeAt by r()
    val LayoutNode.removeAll by r()

    override fun insertBottomUp(index: Int, instance: LayoutNode) {
        current.insertAt(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        current.removeAt(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.move(from, to, count)
    }

    override fun onClear() {
        root.removeAll()
    }
}