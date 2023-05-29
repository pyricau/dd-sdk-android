/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.internal.recorder

import com.datadog.android.sessionreplay.model.MobileSegment

internal class DelayedResolutionCallback(private val delayedCallbackInfo: DelayedCallbackInfo) {
    fun resolveNodeWithNewWireframe(newWireframe: MobileSegment.Wireframe) {

        println("yondbg resolveNodeWithNewWireframe called $newWireframe")
//        if (
//                delayedCallbackInfo.current?.children == null
//                || delayedCallbackInfo.root == null
//        ) return
//
//        val current = delayedCallbackInfo.current ?: return
//
//        println("yondbg current is ${current.wireframes}")
//
////        val children = nodeParent.children
////        val current = children[delayedCallbackInfo.indexInParent]
//
//        // replace wireframe in wireframes
//        val newList = ArrayList<MobileSegment.Wireframe>()
//        newList.addAll(current.wireframes)
//
//        println("yondbg newList is $newList")
//        println("yondbg newList size is ${newList.size}")
//        println("yondbg delayedCallbackInfo.wireframeIndex is ${delayedCallbackInfo.wireframeIndex}")
//
//        newList[delayedCallbackInfo.wireframeIndex] = newWireframe
//
//        println("yondbg newList is now $newList")
//        println("yondbg newList size is now ${newList.size}")
//
//        current.wireframes = newList.toList()

        // replace child in parent
//        val indexInParent = delayedCallbackInfo.indexInParent
//        val parent = delayedCallbackInfo.parent
//
//        val newChildList = ArrayList<Node>()
//        newChildList.addAll(parent?.children ?: emptyList())
//        newChildList[indexInParent] = current
//        delayedCallbackInfo.parent!!.children = newChildList.toList()

        // replace root in blockingQueueItem
        // delayedCallbackInfo.blockingQueueItem.setNodes(listOf(delayedCallbackInfo.root!!))

        // update handler
//        delayedCallbackInfo.blockingQueueHandler.update()
    }
}