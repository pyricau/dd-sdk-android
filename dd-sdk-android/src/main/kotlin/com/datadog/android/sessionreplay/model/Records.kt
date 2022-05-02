/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.model

enum class NodeType {
    Document,
    DocumentType,
    Element,
    Text,
    CDATA,
    Comment
}

enum class Tags(val tagValue: String) {
    SPAN("span"),
    DIV("div"),
    IMAGE("img"),
    BUTTON("button")
}

interface Node {
    val type: NodeType
    val attributes: Map<String, Any>?
    val id: Int?
}

data class DocumentNode(
    val childNodes: List<Node>,
    override val attributes: Map<String, Any>?,
    override val id: Int? = null
) : Node {
    override val type: NodeType = NodeType.Document
}

data class ElementNode(
    override val type: NodeType = NodeType.Element,
    val childNodes: List<Node> = emptyList(),
    val tagName: String? = Tags.DIV.tagValue,
    override val id: Int? = null,
    override val attributes: Map<String, Any>? = null,
    val isSvg: Boolean = false
) : Node {}

data class TextNode(
    val textContent: String,
    override val attributes: Map<String, Any>? = null,
    override val id: Int? = null
) : Node {
    override val type: NodeType = NodeType.Text
}

data class Offset(val top: Int, val left: Int)

data class RecordData(val node: Node, val initialOffset: Offset)
data class MetaData(val href: String, val width: Long, val height: Long) // where href is
// the RUM view id (identifier)
data class FocusData(val hasFocus: Boolean = true)
interface Record {
}

enum class RecordType(val type: Int) {
    FULL_SNAPSHOT(2),
    INCREMENTAL_SNAPSHOT(3),
    META(4),
    FOCUS(6),
    VIEW_END(7),
    VISUAL_VIEWPORT(8),
}

data class MetaRecord(
    val type: Int = RecordType.META.type,
    val timestamp: Long,
    val data: MetaData
):Record

data class FocusRecord(
    val type: Int = RecordType.FOCUS.type,
    val timestamp: Long,
    val focusData:FocusData = FocusData()
):Record

data class ViewEndRecord(
    val type: Int = RecordType.VIEW_END.type,
    val timestamp: Long
):Record

data class FullSnapshotRecord(
    val type: Int = RecordType.FULL_SNAPSHOT.type,
    val timestamp: Long,
    val data: RecordData
) : Record
