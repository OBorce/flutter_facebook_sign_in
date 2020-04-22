/*
 * Copyright (c) 2020 Boris Onchev
 *
 * Distributed under the Boost Software License, Version 1.0.
 * (See accompanying file LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
 */
package com.boriso.facebooksignin.facebook_sign_in

import android.content.Intent
import androidx.annotation.NonNull
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque


/** FacebookSignInPlugin */
class FacebookSignInPlugin : FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener {


    /**
     * In case we receive a signIn and a signOut we need to wait for the signIn to finish before
     * signing out
     */
    private val resultQueue: BlockingDeque<Result> = LinkedBlockingDeque(1)
    private var callbackManager = CallbackManager.Factory.create()
    private var binding: ActivityPluginBinding? = null

    init {
        LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(loginResult: LoginResult) {
                        val result = resultQueue.pop()
                        loginResult.accessToken
                                .run {
                                    hashMapOf<String, Any>(
                                            ("access_token" to token),
                                            ("user_id" to userId),
                                            ("expires" to expires.time),
                                            ("permissions" to permissions.toList()),
                                            ("declined_permissions" to declinedPermissions.toList())
                                    )
                                }
                                .also { result.success(it) }
                    }

                    override fun onCancel() {
                        val result = resultQueue.pop()
                        result.error("cancelled", "User has cancelled the operation", "")
                    }

                    override fun onError(exception: FacebookException) {
                        val result = resultQueue.pop()
                        result.error("exception", "Exception while executing the operation", exception.message)
                    }
                }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(flutterPluginBinding.flutterEngine.dartExecutor, ChannelName)
        channel.setMethodCallHandler(this)
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), ChannelName)
            channel.setMethodCallHandler(FacebookSignInPlugin())
        }

        const val ChannelName = "facebook_sign_in"
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "sign_in" -> call.argument<List<String>>("permissions")
                    ?.also { permissions ->
                        resultQueue.put(result)
                        LoginManager.getInstance().logIn(binding?.activity, permissions)
                    }
                    ?: result.error("missing_permissions", "No permissions", "")
            "sign_out" -> resultQueue.put(result)
                    .also { LoginManager.getInstance().logOut() }
                    .also { resultQueue.pop() }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        LoginManager.getInstance().unregisterCallback(callbackManager)
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        this.binding = binding
        binding.addActivityResultListener(this)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.binding = binding
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        binding?.removeActivityResultListener(this)
        binding = null
    }

    override fun onDetachedFromActivityForConfigChanges() {
        binding?.removeActivityResultListener(this)
        binding = null
    }
}
