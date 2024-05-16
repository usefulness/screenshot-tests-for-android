/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.usefulness.testing.screenshot

import android.annotation.SuppressLint
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowManager
import androidx.core.content.getSystemService
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.WeakHashMap

@SuppressLint("PrivateApi")
object WindowAttachment {
    /**
     * Keep track of all the attached windows here so that we don't double attach them.
     */
    private val sAttachments = WeakHashMap<View, Boolean>()

    private var sAttachInfo: Any? = null

    private val sInvocationHandler = InvocationHandler { _, method: Method, _ ->
        if ("getCoverStateSwitch" == method.name) {
            // needed for Samsung version of Android 8.0
            return@InvocationHandler false
        }
        null
    }

    /**
     * Dispatch onAttachedToWindow to all the views in the view hierarchy.
     *
     *
     * Detach the view by calling `detach()` on the returned `Detacher`.
     *
     *
     * Note that if the view is already attached (either via WindowAttachment or to a real window),
     * then both the attach and the corresponding detach will be no-ops.
     *
     *
     * Note that this is hacky, after these calls the views will still say that
     * isAttachedToWindow() is false and getWindowToken() == null.
     */
    @JvmStatic
    fun dispatchAttach(view: View): Detacher {
        if (view.windowToken != null || sAttachments.containsKey(view)) {
            // Screenshot tests can often be run against a View that's
            // attached to a real activity, in which case we have nothing to
            // do
            Log.i("WindowAttachment", "Skipping window attach hack since it's really attached")
            return NoopDetacher()
        }

        sAttachments[view] = true
        sAttachInfo = generateAttachInfo(view)
        setAttachInfo(view)

        return RealDetacher(view)
    }

    /**
     * Similar to dispatchAttach, except dispatches to the corresponding detach.
     */
    private fun dispatchDetach(view: View) {
        invokeUnchecked(view, "dispatchDetachedFromWindow")
    }

    private fun invokeUnchecked(view: View, methodName: String) {
        val method = View::class.java.getDeclaredMethod(methodName)
        method.isAccessible = true
        method.invoke(view)
    }

    @JvmStatic
    fun setAttachInfo(view: View) {
        if (sAttachInfo == null) {
            sAttachInfo = generateAttachInfo(view)
        }

        val dispatch = View::class.java.getDeclaredMethod(
            "dispatchAttachedToWindow",
            Class.forName("android.view.View\$AttachInfo"),
            Int::class.javaPrimitiveType,
        )
        dispatch.isAccessible = true
        dispatch.invoke(view, sAttachInfo, 0)
    }

    /**
     * Simulates the view as being attached.
     */
    fun generateAttachInfo(view: View): Any {
        sAttachInfo?.let { return it }

        val cAttachInfo = Class.forName("android.view.View\$AttachInfo")

        val cViewRootImpl = Class.forName("android.view.ViewRootImpl")

        val cIWindowSession = Class.forName("android.view.IWindowSession")
        val cIWindow = Class.forName("android.view.IWindow")
        val cICallbacks = Class.forName("android.view.View\$AttachInfo\$Callbacks")

        val context = view.context
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            val wm = context.getSystemService<WindowManager>()
            @Suppress("DEPRECATION")
            wm?.defaultDisplay
        }
            .let(::checkNotNull)

        val window = createIWindow()

        val viewRootImpl: Any
        val viewRootCtorParams: Array<Class<*>>
        val viewRootCtorValues: Array<Any>

        if (Build.VERSION.SDK_INT >= 26) {
            viewRootImpl =
                cViewRootImpl
                    .getConstructor(Context::class.java, Display::class.java)
                    .newInstance(context, display)

            viewRootCtorParams =
                arrayOf(
                    cIWindowSession,
                    cIWindow,
                    Display::class.java,
                    cViewRootImpl,
                    Handler::class.java,
                    cICallbacks,
                    Context::class.java,
                )

            viewRootCtorValues =
                arrayOf(
                    stub(cIWindowSession),
                    window,
                    display,
                    viewRootImpl,
                    Handler(),
                    stub(cICallbacks),
                    context,
                )
        } else {
            viewRootImpl =
                cViewRootImpl
                    .getConstructor(Context::class.java, Display::class.java)
                    .newInstance(context, display)

            viewRootCtorParams =
                arrayOf(
                    cIWindowSession,
                    cIWindow,
                    Display::class.java,
                    cViewRootImpl,
                    Handler::class.java,
                    cICallbacks,
                )

            viewRootCtorValues =
                arrayOf(
                    stub(cIWindowSession),
                    window,
                    display,
                    viewRootImpl,
                    Handler(),
                    stub(cICallbacks),
                )
        }

        val attachInfo = invokeConstructor(cAttachInfo, viewRootCtorParams, viewRootCtorValues)

        setField(attachInfo, "mHasWindowFocus", true)
        setField(attachInfo, "mWindowVisibility", View.VISIBLE)
        setField(attachInfo, "mInTouchMode", false)
        setField(attachInfo, "mHardwareAccelerated", false)

        return attachInfo
    }

    @Throws(Exception::class)
    private fun invokeConstructor(clazz: Class<*>, params: Array<Class<*>>, values: Array<Any>): Any {
        val cons = clazz.getDeclaredConstructor(*params)
        cons.isAccessible = true
        return cons.newInstance(*values)
    }

    @Throws(Exception::class)
    private fun createIWindow(): Any {
        val cIWindow = Class.forName("android.view.IWindow")

        // Since IWindow is an interface, I don't need dexmaker for this
        val handler = InvocationHandler { proxy: Any?, method: Method, args: Array<Any?>? ->
            if (method.name == "asBinder") {
                return@InvocationHandler Binder()
            }
            null
        }

        return Proxy.newProxyInstance(cIWindow.classLoader, arrayOf(cIWindow), handler)
    }

    private fun stub(klass: Class<*>): Any {
        require(klass.isInterface) { "Cannot stub an non-interface" }

        return Proxy.newProxyInstance(klass.classLoader, arrayOf(klass), sInvocationHandler)
    }

    @Throws(Exception::class)
    private fun setField(o: Any, fieldName: String, value: Any) {
        val clazz: Class<*> = o.javaClass
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field[o] = value
    }

    interface Detacher {
        fun detach()
    }

    private class NoopDetacher : Detacher {
        override fun detach() = Unit
    }

    private class RealDetacher(private val mView: View) : Detacher {
        override fun detach() {
            dispatchDetach(mView)
            sAttachments.remove(mView)
        }
    }
}
