package com.example.util

import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

object BossSerializationHelper {
    fun serializeExtras(extras: List<ExtraPiece>): JSONArray {
        val arr = JSONArray()
        for (e in extras) {
            val o = JSONObject()
            o.put("id", e.id)
            o.put("section", e.section)
            o.put("lengthFt", e.lengthFt)
            o.put("qty", e.qty)
            o.put("gauge", e.gauge)
            arr.put(o)
        }
        return arr
    }

    fun deserializeExtras(arr: JSONArray?): List<ExtraPiece> {
        if (arr == null) return emptyList()
        val list = mutableListOf<ExtraPiece>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list.add(
                ExtraPiece(
                    section = o.optString("section", "D54A"),
                    lengthFt = o.optDouble("lengthFt", 16.0),
                    qty = o.optInt("qty", 1),
                    id = o.optString("id", java.util.UUID.randomUUID().toString()),
                    gauge = o.optString("gauge", "1.6")
                )
            )
        }
        return list
    }

    fun serializeWindows(items: List<WindowItem>): String {
        val array = JSONArray()
        for (w in items) {
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("width", w.width)
            obj.put("height", w.height)
            obj.put("qty", w.qty)
            obj.put("frameType", w.frameType)
            obj.put("outerSection", w.outerSection)
            obj.put("sectionGauge", w.sectionGauge)
            obj.put("glassGauge", w.glassGauge)
            obj.put("glassColor", w.glassColor)
            obj.put("glassEnabled", w.glassEnabled)
            obj.put("m24Qty", w.m24Qty)
            obj.put("m24aQty", w.m24aQty)
            obj.put("d29HeightQty", w.d29HeightQty)
            obj.put("d29WidthQty", w.d29WidthQty)
            obj.put("panelCount", w.panelCount)
            obj.put("lockType", w.lockType)
            obj.put("extras", serializeExtras(w.extras))
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeWindows(json: String?): List<WindowItem> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<WindowItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    WindowItem(
                        id = obj.optString("id", "w_$i"),
                        width = obj.optDouble("width", 36.0),
                        height = obj.optDouble("height", 48.0),
                        qty = obj.optInt("qty", 1),
                        frameType = obj.optString("frameType", "C"),
                        outerSection = obj.optString("outerSection", ""),
                        sectionGauge = obj.optString("sectionGauge", "1.6"),
                        glassGauge = obj.optString("glassGauge", "5"),
                        glassColor = obj.optString("glassColor", "CLEAR"),
                        glassEnabled = obj.optBoolean("glassEnabled", true),
                        m24Qty = obj.optInt("m24Qty", if (obj.optInt("panelCount", 2) == 3) 6 else 4).let {
                            if (obj.optInt("panelCount", 2) == 3 && (it == 4 || it == 2)) 6 else it
                        },
                        m24aQty = obj.optInt("m24aQty", 0),
                        panelCount = obj.optInt("panelCount", 2),
                        d29HeightQty = obj.optInt("d29HeightQty", if (obj.optInt("panelCount", 2) >= 3) 4 else 2).let {
                            if (obj.optInt("panelCount", 2) >= 3 && it == 2) 4 else it
                        },
                        d29WidthQty = obj.optInt("d29WidthQty", if (obj.optInt("panelCount", 2) >= 3) 4 else 2).let {
                            if (obj.optInt("panelCount", 2) >= 3 && it == 2) 4 else it
                        },
                        lockType = obj.optString("lockType", "LATCH"),
                        extras = deserializeExtras(obj.optJSONArray("extras"))
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeDoors(items: List<DoorItem>): String {
        val array = JSONArray()
        for (d in items) {
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("width", d.width)
            obj.put("height", d.height)
            obj.put("qty", d.qty)
            obj.put("outer", d.outer)
            obj.put("sectionGauge", d.sectionGauge)
            obj.put("glassGauge", d.glassGauge)
            obj.put("glassColor", d.glassColor)
            obj.put("glassEnabled", d.glassEnabled)
            obj.put("d59Qty", d.d59Qty)
            obj.put("d59aQty", d.d59aQty)
            obj.put("d52Qty", d.d52Qty)
            obj.put("extras", serializeExtras(d.extras))
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeDoors(json: String?): List<DoorItem> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<DoorItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DoorItem(
                        id = obj.optString("id", "d_$i"),
                        width = obj.optDouble("width", 36.0),
                        height = obj.optDouble("height", 84.0),
                        qty = obj.optInt("qty", 1),
                        outer = obj.optString("outer", "D54A"),
                        sectionGauge = obj.optString("sectionGauge", "1.6"),
                        glassGauge = obj.optString("glassGauge", "5"),
                        glassColor = obj.optString("glassColor", "CLEAR"),
                        glassEnabled = obj.optBoolean("glassEnabled", true),
                        d59Qty = obj.optInt("d59Qty", 0),
                        d59aQty = obj.optInt("d59aQty", 0),
                        d52Qty = obj.optInt("d52Qty", 0),
                        extras = deserializeExtras(obj.optJSONArray("extras"))
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeFixFrames(items: List<FixFrameItem>): String {
        val array = JSONArray()
        for (f in items) {
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("width", f.width)
            obj.put("height", f.height)
            obj.put("qty", f.qty)
            obj.put("outer", f.outer)
            obj.put("sectionGauge", f.sectionGauge)
            obj.put("glassGauge", f.glassGauge)
            obj.put("glassColor", f.glassColor)
            obj.put("glassEnabled", f.glassEnabled)
            obj.put("d59Qty", f.d59Qty)
            obj.put("d59aQty", f.d59aQty)
            obj.put("d52Qty", f.d52Qty)
            obj.put("extras", serializeExtras(f.extras))
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeFixFrames(json: String?): List<FixFrameItem> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<FixFrameItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FixFrameItem(
                        id = obj.optString("id", "f_$i"),
                        width = obj.optDouble("width", 36.0),
                        height = obj.optDouble("height", 48.0),
                        qty = obj.optInt("qty", 1),
                        outer = obj.optString("outer", "D54A"),
                        sectionGauge = obj.optString("sectionGauge", "1.6"),
                        glassGauge = obj.optString("glassGauge", "5"),
                        glassColor = obj.optString("glassColor", "CLEAR"),
                        glassEnabled = obj.optBoolean("glassEnabled", true),
                        d59Qty = obj.optInt("d59Qty", 0),
                        d59aQty = obj.optInt("d59aQty", 0),
                        d52Qty = obj.optInt("d52Qty", 0),
                        extras = deserializeExtras(obj.optJSONArray("extras"))
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeOpenableWindows(items: List<OpenableWindowItem>): String {
        val array = JSONArray()
        for (op in items) {
            val obj = JSONObject()
            obj.put("id", op.id)
            obj.put("width", op.width)
            obj.put("height", op.height)
            obj.put("qty", op.qty)
            obj.put("outer", op.outer)
            obj.put("sectionGauge", op.sectionGauge)
            obj.put("glassGauge", op.glassGauge)
            obj.put("glassColor", op.glassColor)
            obj.put("glassEnabled", op.glassEnabled)
            obj.put("d59Qty", op.d59Qty)
            obj.put("d59aQty", op.d59aQty)
            obj.put("d52Qty", op.d52Qty)
            obj.put("extras", serializeExtras(op.extras))
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeOpenableWindows(json: String?): List<OpenableWindowItem> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<OpenableWindowItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    OpenableWindowItem(
                        id = obj.optString("id", "op_$i"),
                        width = obj.optDouble("width", 36.0),
                        height = obj.optDouble("height", 48.0),
                        qty = obj.optInt("qty", 1),
                        outer = obj.optString("outer", "D54A"),
                        sectionGauge = obj.optString("sectionGauge", "1.6"),
                        glassGauge = obj.optString("glassGauge", "5"),
                        glassColor = obj.optString("glassColor", "CLEAR"),
                        glassEnabled = obj.optBoolean("glassEnabled", true),
                        d59Qty = obj.optInt("d59Qty", 0),
                        d59aQty = obj.optInt("d59aQty", 0),
                        d52Qty = obj.optInt("d52Qty", 0),
                        extras = deserializeExtras(obj.optJSONArray("extras"))
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeSlidingDoors(items: List<SlidingDoorItem>): String {
        val array = JSONArray()
        for (sd in items) {
            val obj = JSONObject()
            obj.put("id", sd.id)
            obj.put("width", sd.width)
            obj.put("height", sd.height)
            obj.put("qty", sd.qty)
            obj.put("outer", sd.outer)
            obj.put("m24Qty", sd.m24Qty)
            obj.put("m24aQty", sd.m24aQty)
            obj.put("d29HeightQty", sd.d29HeightQty)
            obj.put("d29WidthQty", sd.d29WidthQty)
            obj.put("sectionGauge", sd.sectionGauge)
            obj.put("glassGauge", sd.glassGauge)
            obj.put("glassColor", sd.glassColor)
            obj.put("glassEnabled", sd.glassEnabled)
            obj.put("panelCount", sd.panelCount)
            obj.put("lockType", sd.lockType)
            obj.put("extras", serializeExtras(sd.extras))
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeSlidingDoors(json: String?): List<SlidingDoorItem> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<SlidingDoorItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SlidingDoorItem(
                        id = obj.optString("id", "sd_$i"),
                        width = obj.optDouble("width", 48.0),
                        height = obj.optDouble("height", 84.0),
                        qty = obj.optInt("qty", 1),
                        outer = obj.optString("outer", "NORMAL"),
                        m24Qty = obj.optInt("m24Qty", if (obj.optInt("panelCount", 2) == 3) 6 else 4).let {
                            if (obj.optInt("panelCount", 2) == 3 && (it == 2 || it == 4)) 6 else it
                        },
                        m24aQty = obj.optInt("m24aQty", 0),
                        d29HeightQty = obj.optInt("d29HeightQty", if (obj.optInt("panelCount", 2) >= 3) 4 else 2),
                        d29WidthQty = obj.optInt("d29WidthQty", if (obj.optInt("panelCount", 2) >= 3) 4 else 2),
                        sectionGauge = obj.optString("sectionGauge", "1.6"),
                        glassGauge = obj.optString("glassGauge", "5"),
                        glassColor = obj.optString("glassColor", "CLEAR"),
                        glassEnabled = obj.optBoolean("glassEnabled", true),
                        panelCount = obj.optInt("panelCount", 2),
                        lockType = obj.optString("lockType", "LATCH"),
                        extras = deserializeExtras(obj.optJSONArray("extras"))
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeProjectExtras(extras: List<ExtraPiece>): String {
        return serializeExtras(extras).toString()
    }

    fun deserializeProjectExtras(json: String?): List<ExtraPiece> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            deserializeExtras(JSONArray(json))
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeSavedQuotations(items: List<SavedQuotation>): String {
        val array = JSONArray()
        for (sq in items) {
            val obj = JSONObject()
            obj.put("id", sq.id)
            obj.put("timestamp", sq.timestamp)
            obj.put("label", sq.label)
            obj.put("customerName", sq.customerName)
            obj.put("customerPhone", sq.customerPhone)
            obj.put("gauge", sq.gauge)
            obj.put("color", sq.color)
            obj.put("frameType", sq.frameType)
            obj.put("grandTotal", sq.grandTotal)
            obj.put("alumTotal", sq.alumTotal)
            obj.put("hardwareTotal", sq.hardwareTotal)
            obj.put("glassTotal", sq.glassTotal)
            obj.put("transportRent", sq.transportRent)
            obj.put("labourCost", sq.labourCost)
            obj.put("totalUnits", sq.totalUnits)
            obj.put("itemsSummary", sq.itemsSummary)
            obj.put("windowsJson", sq.windowsJson)
            obj.put("doorsJson", sq.doorsJson)
            obj.put("fixframesJson", sq.fixframesJson)
            obj.put("openableWindowsJson", sq.openableWindowsJson)
            obj.put("slidingDoorsJson", sq.slidingDoorsJson)
            obj.put("projectExtrasJson", sq.projectExtrasJson)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeSavedQuotations(json: String?): List<SavedQuotation> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<SavedQuotation>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                list.add(
                    SavedQuotation(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        timestamp = obj.optString("timestamp", ""),
                        label = obj.optString("label", "Saved Project"),
                        customerName = obj.optString("customerName", "Valued Client"),
                        customerPhone = obj.optString("customerPhone", ""),
                        gauge = obj.optString("gauge", "1.6"),
                        color = obj.optString("color", "DULL"),
                        frameType = obj.optString("frameType", "C"),
                        grandTotal = obj.optDouble("grandTotal", 0.0),
                        alumTotal = obj.optDouble("alumTotal", 0.0),
                        hardwareTotal = obj.optDouble("hardwareTotal", 0.0),
                        glassTotal = obj.optDouble("glassTotal", 0.0),
                        transportRent = obj.optDouble("transportRent", 0.0),
                        labourCost = obj.optDouble("labourCost", 0.0),
                        totalUnits = obj.optInt("totalUnits", 0),
                        itemsSummary = obj.optString("itemsSummary", ""),
                        windowsJson = obj.optString("windowsJson", ""),
                        doorsJson = obj.optString("doorsJson", ""),
                        fixframesJson = obj.optString("fixframesJson", ""),
                        openableWindowsJson = obj.optString("openableWindowsJson", ""),
                        slidingDoorsJson = obj.optString("slidingDoorsJson", ""),
                        projectExtrasJson = obj.optString("projectExtrasJson", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeGlassColorsByGauge(map: Map<String, List<String>>): String {
        val root = JSONObject()
        for ((gauge, colors) in map) {
            val arr = JSONArray()
            colors.forEach { arr.put(it) }
            root.put(gauge, arr)
        }
        return root.toString()
    }

    fun deserializeGlassColorsByGauge(
        jsonStr: String?,
        fallback: Map<String, List<String>>
    ): Map<String, List<String>> {
        if (jsonStr.isNullOrBlank()) return fallback
        val result = mutableMapOf<String, List<String>>()
        try {
            val root = JSONObject(jsonStr)
            val keys = root.keys()
            while (keys.hasNext()) {
                val g = keys.next()
                val arr = root.optJSONArray(g) ?: continue
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i)
                    if (v.isNotBlank()) list.add(v)
                }
                if (list.isNotEmpty()) {
                    result[g] = list
                }
            }
        } catch (_: Exception) {
            return fallback
        }
        return if (result.isEmpty()) fallback else result
    }

    private fun serializeDeductionDim(d: DeductionDim): JSONObject {
        val o = JSONObject()
        o.put("inch", d.inch)
        o.put("sutar", d.sutar)
        return o
    }

    private fun deserializeDeductionDim(o: JSONObject?, def: DeductionDim): DeductionDim {
        if (o == null) return def
        return DeductionDim(
            inch = o.optDouble("inch", def.inch),
            sutar = o.optDouble("sutar", def.sutar)
        )
    }

    fun serializeWindowFormulas(f: WindowFormulaSettings): String {
        val o = JSONObject()
        o.put("m23HeightDeduction", serializeDeductionDim(f.m23HeightDeduction))
        o.put("m24WidthDeduction", serializeDeductionDim(f.m24WidthDeduction))
        o.put("m24Panels", f.m24Panels)
        o.put("d29HeightDeduction", serializeDeductionDim(f.d29HeightDeduction))
        o.put("d29WidthDeduction", serializeDeductionDim(f.d29WidthDeduction))
        o.put("d29WidthOverlap", serializeDeductionDim(f.d29WidthOverlap))
        o.put("d29Panels", f.d29Panels)
        o.put("m23ApplyTo", f.m23ApplyTo)
        o.put("m23Qty", f.m23Qty)
        o.put("m28ApplyTo", f.m28ApplyTo)
        o.put("m28Qty", f.m28Qty)
        o.put("m24ApplyTo", f.m24ApplyTo)
        o.put("m24Qty", f.m24Qty)
        o.put("d29ApplyTo", f.d29ApplyTo)
        o.put("d29HeightQty", f.d29HeightQty)
        o.put("d29WidthQty", f.d29WidthQty)
        o.put("m24WidthCenterDeduction3", serializeDeductionDim(f.m24WidthCenterDeduction3))
        o.put("m24WidthSidesDeduction3", serializeDeductionDim(f.m24WidthSidesDeduction3))
        o.put("m24WidthDeduction3", serializeDeductionDim(f.m24WidthDeduction3))
        o.put("m24WidthOverlap3", serializeDeductionDim(f.m24WidthOverlap3))
        o.put("m23Qty3", f.m23Qty3)
        o.put("m28Qty3", f.m28Qty3)
        o.put("m24Qty3", f.m24Qty3)
        o.put("d29WidthDeduction3", serializeDeductionDim(f.d29WidthDeduction3))
        o.put("d29WidthOverlap3", serializeDeductionDim(f.d29WidthOverlap3))
        o.put("d29Panels3", f.d29Panels3)
        o.put("d29HeightQty3", f.d29HeightQty3)
        o.put("d29WidthQty3", f.d29WidthQty3)
        o.put("d29HeightDeduction3", serializeDeductionDim(f.d29HeightDeduction3))
        o.put("m24WidthDeduction4", serializeDeductionDim(f.m24WidthDeduction4))
        o.put("m23Qty4", f.m23Qty4)
        o.put("m28Qty4", f.m28Qty4)
        o.put("m24Qty4", f.m24Qty4)
        o.put("d29WidthDeduction4", serializeDeductionDim(f.d29WidthDeduction4))
        o.put("d29WidthOverlap4", serializeDeductionDim(f.d29WidthOverlap4))
        o.put("d29Panels4", f.d29Panels4)
        o.put("d29HeightQty4", f.d29HeightQty4)
        o.put("d29WidthQty4", f.d29WidthQty4)
        o.put("d29HeightDeduction4", serializeDeductionDim(f.d29HeightDeduction4))
        o.put("d29PushHandleDeduction", serializeDeductionDim(f.d29PushHandleDeduction))
        o.put("d31HeightDeduction4", serializeDeductionDim(f.d31HeightDeduction4))
        return o.toString()
    }

    fun deserializeWindowFormulas(jsonStr: String?, def: WindowFormulaSettings): WindowFormulaSettings {
        if (jsonStr.isNullOrBlank()) return def
        return try {
            val o = JSONObject(jsonStr)
            WindowFormulaSettings(
                m23HeightDeduction = deserializeDeductionDim(o.optJSONObject("m23HeightDeduction"), def.m23HeightDeduction),
                m24WidthDeduction = deserializeDeductionDim(o.optJSONObject("m24WidthDeduction"), def.m24WidthDeduction),
                m24Panels = o.optInt("m24Panels", def.m24Panels),
                d29HeightDeduction = deserializeDeductionDim(o.optJSONObject("d29HeightDeduction"), def.d29HeightDeduction),
                d29WidthDeduction = deserializeDeductionDim(o.optJSONObject("d29WidthDeduction"), def.d29WidthDeduction),
                d29WidthOverlap = deserializeDeductionDim(o.optJSONObject("d29WidthOverlap"), def.d29WidthOverlap),
                d29Panels = o.optInt("d29Panels", def.d29Panels),
                m23ApplyTo = o.optString("m23ApplyTo", def.m23ApplyTo),
                m23Qty = o.optInt("m23Qty", def.m23Qty),
                m28ApplyTo = o.optString("m28ApplyTo", def.m28ApplyTo),
                m28Qty = o.optInt("m28Qty", def.m28Qty),
                m24ApplyTo = o.optString("m24ApplyTo", def.m24ApplyTo),
                m24Qty = o.optInt("m24Qty", def.m24Qty),
                d29ApplyTo = o.optString("d29ApplyTo", def.d29ApplyTo),
                d29HeightQty = o.optInt("d29HeightQty", def.d29HeightQty),
                d29WidthQty = o.optInt("d29WidthQty", def.d29WidthQty),
                m24WidthCenterDeduction3 = deserializeDeductionDim(o.optJSONObject("m24WidthCenterDeduction3"), def.m24WidthCenterDeduction3),
                m24WidthSidesDeduction3 = deserializeDeductionDim(o.optJSONObject("m24WidthSidesDeduction3"), def.m24WidthSidesDeduction3),
                m24WidthDeduction3 = deserializeDeductionDim(o.optJSONObject("m24WidthDeduction3"), def.m24WidthDeduction3),
                m24WidthOverlap3 = deserializeDeductionDim(o.optJSONObject("m24WidthOverlap3"), def.m24WidthOverlap3),
                m23Qty3 = o.optInt("m23Qty3", def.m23Qty3),
                m28Qty3 = o.optInt("m28Qty3", def.m28Qty3),
                m24Qty3 = o.optInt("m24Qty3", def.m24Qty3),
                d29WidthDeduction3 = deserializeDeductionDim(o.optJSONObject("d29WidthDeduction3"), def.d29WidthDeduction3),
                d29WidthOverlap3 = deserializeDeductionDim(o.optJSONObject("d29WidthOverlap3"), def.d29WidthOverlap3),
                d29Panels3 = o.optInt("d29Panels3", def.d29Panels3),
                d29HeightQty3 = o.optInt("d29HeightQty3", def.d29HeightQty3),
                d29WidthQty3 = o.optInt("d29WidthQty3", def.d29WidthQty3),
                d29HeightDeduction3 = deserializeDeductionDim(o.optJSONObject("d29HeightDeduction3"), def.d29HeightDeduction3),
                m24WidthDeduction4 = deserializeDeductionDim(o.optJSONObject("m24WidthDeduction4"), def.m24WidthDeduction4),
                m23Qty4 = o.optInt("m23Qty4", def.m23Qty4),
                m28Qty4 = o.optInt("m28Qty4", def.m28Qty4),
                m24Qty4 = o.optInt("m24Qty4", def.m24Qty4),
                d29WidthDeduction4 = deserializeDeductionDim(o.optJSONObject("d29WidthDeduction4"), def.d29WidthDeduction4),
                d29WidthOverlap4 = deserializeDeductionDim(o.optJSONObject("d29WidthOverlap4"), def.d29WidthOverlap4),
                d29Panels4 = o.optInt("d29Panels4", def.d29Panels4),
                d29HeightQty4 = o.optInt("d29HeightQty4", def.d29HeightQty4),
                d29WidthQty4 = o.optInt("d29WidthQty4", def.d29WidthQty4),
                d29HeightDeduction4 = deserializeDeductionDim(o.optJSONObject("d29HeightDeduction4"), def.d29HeightDeduction4),
                d29PushHandleDeduction = deserializeDeductionDim(o.optJSONObject("d29PushHandleDeduction"), def.d29PushHandleDeduction),
                d31HeightDeduction4 = deserializeDeductionDim(o.optJSONObject("d31HeightDeduction4"), def.d31HeightDeduction4)
            )
        } catch (_: Exception) {
            def
        }
    }

    fun serializeSlidingDoorFormulas(f: SlidingDoorFormulaSettings): String {
        val o = JSONObject()
        o.put("m23HeightDeduction", serializeDeductionDim(f.m23HeightDeduction))
        o.put("m24WidthDeduction", serializeDeductionDim(f.m24WidthDeduction))
        o.put("m24Panels", f.m24Panels)
        o.put("m23ApplyTo", f.m23ApplyTo)
        o.put("m23Qty", f.m23Qty)
        o.put("m28ApplyTo", f.m28ApplyTo)
        o.put("m28Qty", f.m28Qty)
        o.put("m24ApplyTo", f.m24ApplyTo)
        o.put("m24Qty", f.m24Qty)
        o.put("m24WidthCenterDeduction3", serializeDeductionDim(f.m24WidthCenterDeduction3))
        o.put("m24WidthSidesDeduction3", serializeDeductionDim(f.m24WidthSidesDeduction3))
        o.put("m24WidthDeduction3", serializeDeductionDim(f.m24WidthDeduction3))
        o.put("m24WidthOverlap3", serializeDeductionDim(f.m24WidthOverlap3))
        o.put("m23Qty3", f.m23Qty3)
        o.put("m28Qty3", f.m28Qty3)
        o.put("m24Qty3", f.m24Qty3)
        o.put("m24WidthDeduction4", serializeDeductionDim(f.m24WidthDeduction4))
        o.put("m23Qty4", f.m23Qty4)
        o.put("m28Qty4", f.m28Qty4)
        o.put("m24Qty4", f.m24Qty4)
        o.put("d29HeightDeduction", serializeDeductionDim(f.d29HeightDeduction))
        o.put("d29WidthDeduction", serializeDeductionDim(f.d29WidthDeduction))
        o.put("d29WidthOverlap", serializeDeductionDim(f.d29WidthOverlap))
        o.put("d29Panels", f.d29Panels)
        o.put("d29ApplyTo", f.d29ApplyTo)
        o.put("d29HeightQty", f.d29HeightQty)
        o.put("d29WidthQty", f.d29WidthQty)
        o.put("d29WidthDeduction3", serializeDeductionDim(f.d29WidthDeduction3))
        o.put("d29WidthOverlap3", serializeDeductionDim(f.d29WidthOverlap3))
        o.put("d29Panels3", f.d29Panels3)
        o.put("d29HeightQty3", f.d29HeightQty3)
        o.put("d29WidthQty3", f.d29WidthQty3)
        o.put("d29HeightDeduction3", serializeDeductionDim(f.d29HeightDeduction3))
        o.put("d29WidthDeduction4", serializeDeductionDim(f.d29WidthDeduction4))
        o.put("d29WidthOverlap4", serializeDeductionDim(f.d29WidthOverlap4))
        o.put("d29Panels4", f.d29Panels4)
        o.put("d29HeightQty4", f.d29HeightQty4)
        o.put("d29WidthQty4", f.d29WidthQty4)
        o.put("d29HeightDeduction4", serializeDeductionDim(f.d29HeightDeduction4))
        o.put("d29PushHandleDeduction", serializeDeductionDim(f.d29PushHandleDeduction))
        o.put("d31HeightDeduction4", serializeDeductionDim(f.d31HeightDeduction4))
        return o.toString()
    }

    fun deserializeSlidingDoorFormulas(jsonStr: String?, def: SlidingDoorFormulaSettings): SlidingDoorFormulaSettings {
        if (jsonStr.isNullOrBlank()) return def
        return try {
            val o = JSONObject(jsonStr)
            SlidingDoorFormulaSettings(
                m23HeightDeduction = deserializeDeductionDim(o.optJSONObject("m23HeightDeduction"), def.m23HeightDeduction),
                m24WidthDeduction = deserializeDeductionDim(o.optJSONObject("m24WidthDeduction"), def.m24WidthDeduction),
                m24Panels = o.optInt("m24Panels", def.m24Panels),
                m23ApplyTo = o.optString("m23ApplyTo", def.m23ApplyTo),
                m23Qty = o.optInt("m23Qty", def.m23Qty),
                m28ApplyTo = o.optString("m28ApplyTo", def.m28ApplyTo),
                m28Qty = o.optInt("m28Qty", def.m28Qty),
                m24ApplyTo = o.optString("m24ApplyTo", def.m24ApplyTo),
                m24Qty = o.optInt("m24Qty", def.m24Qty),
                m24WidthCenterDeduction3 = deserializeDeductionDim(o.optJSONObject("m24WidthCenterDeduction3"), def.m24WidthCenterDeduction3),
                m24WidthSidesDeduction3 = deserializeDeductionDim(o.optJSONObject("m24WidthSidesDeduction3"), def.m24WidthSidesDeduction3),
                m24WidthDeduction3 = deserializeDeductionDim(o.optJSONObject("m24WidthDeduction3"), def.m24WidthDeduction3),
                m24WidthOverlap3 = deserializeDeductionDim(o.optJSONObject("m24WidthOverlap3"), def.m24WidthOverlap3),
                m23Qty3 = o.optInt("m23Qty3", def.m23Qty3),
                m28Qty3 = o.optInt("m28Qty3", def.m28Qty3),
                m24Qty3 = o.optInt("m24Qty3", def.m24Qty3),
                m24WidthDeduction4 = deserializeDeductionDim(o.optJSONObject("m24WidthDeduction4"), def.m24WidthDeduction4),
                m23Qty4 = o.optInt("m23Qty4", def.m23Qty4),
                m28Qty4 = o.optInt("m28Qty4", def.m28Qty4),
                m24Qty4 = o.optInt("m24Qty4", def.m24Qty4),
                d29HeightDeduction = deserializeDeductionDim(o.optJSONObject("d29HeightDeduction"), def.d29HeightDeduction),
                d29WidthDeduction = deserializeDeductionDim(o.optJSONObject("d29WidthDeduction"), def.d29WidthDeduction),
                d29WidthOverlap = deserializeDeductionDim(o.optJSONObject("d29WidthOverlap"), def.d29WidthOverlap),
                d29Panels = o.optInt("d29Panels", def.d29Panels),
                d29ApplyTo = o.optString("d29ApplyTo", def.d29ApplyTo),
                d29HeightQty = o.optInt("d29HeightQty", def.d29HeightQty),
                d29WidthQty = o.optInt("d29WidthQty", def.d29WidthQty),
                d29WidthDeduction3 = deserializeDeductionDim(o.optJSONObject("d29WidthDeduction3"), def.d29WidthDeduction3),
                d29WidthOverlap3 = deserializeDeductionDim(o.optJSONObject("d29WidthOverlap3"), def.d29WidthOverlap3),
                d29Panels3 = o.optInt("d29Panels3", def.d29Panels3),
                d29HeightQty3 = o.optInt("d29HeightQty3", def.d29HeightQty3),
                d29WidthQty3 = o.optInt("d29WidthQty3", def.d29WidthQty3),
                d29HeightDeduction3 = deserializeDeductionDim(o.optJSONObject("d29HeightDeduction3"), def.d29HeightDeduction3),
                d29WidthDeduction4 = deserializeDeductionDim(o.optJSONObject("d29WidthDeduction4"), def.d29WidthDeduction4),
                d29WidthOverlap4 = deserializeDeductionDim(o.optJSONObject("d29WidthOverlap4"), def.d29WidthOverlap4),
                d29Panels4 = o.optInt("d29Panels4", def.d29Panels4),
                d29HeightQty4 = o.optInt("d29HeightQty4", def.d29HeightQty4),
                d29WidthQty4 = o.optInt("d29WidthQty4", def.d29WidthQty4),
                d29HeightDeduction4 = deserializeDeductionDim(o.optJSONObject("d29HeightDeduction4"), def.d29HeightDeduction4),
                d29PushHandleDeduction = deserializeDeductionDim(o.optJSONObject("d29PushHandleDeduction"), def.d29PushHandleDeduction),
                d31HeightDeduction4 = deserializeDeductionDim(o.optJSONObject("d31HeightDeduction4"), def.d31HeightDeduction4)
            )
        } catch (_: Exception) {
            def
        }
    }

    fun serializeCategoryFormulas(f: FormulaCategorySettings): String {
        val root = JSONObject()
        val fObj = JSONObject()
        f.formulas.forEach { (sec, pair) ->
            val pObj = JSONObject()
            pObj.put("h", serializeDeductionDim(pair.h))
            pObj.put("w", serializeDeductionDim(pair.w))
            fObj.put(sec, pObj)
        }
        val aObj = JSONObject()
        f.acc.forEach { (sec, acc) ->
            val cObj = JSONObject()
            cObj.put("enabled", acc.enabled)
            cObj.put("which", acc.which)
            cObj.put("applyTo", acc.applyTo)
            cObj.put("qty", acc.qty)
            cObj.put("qtyH", acc.qtyH)
            cObj.put("qtyW", acc.qtyW)
            aObj.put(sec, cObj)
        }
        root.put("formulas", fObj)
        root.put("acc", aObj)
        return root.toString()
    }

    fun deserializeCategoryFormulas(jsonStr: String?, def: FormulaCategorySettings): FormulaCategorySettings {
        if (jsonStr.isNullOrBlank()) return def
        return try {
            val root = JSONObject(jsonStr)
            val fObj = root.optJSONObject("formulas")
            val formulas = def.formulas.toMutableMap()
            if (fObj != null) {
                val keys = fObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val pObj = fObj.optJSONObject(k) ?: continue
                    val defPair = formulas[k] ?: DeductionPair()
                    formulas[k] = DeductionPair(
                        h = deserializeDeductionDim(pObj.optJSONObject("h"), defPair.h),
                        w = deserializeDeductionDim(pObj.optJSONObject("w"), defPair.w)
                    )
                }
            }
            val aObj = root.optJSONObject("acc")
            val accMap = def.acc.toMutableMap()
            if (aObj != null) {
                val keys = aObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val cObj = aObj.optJSONObject(k) ?: continue
                    val defAcc = accMap[k] ?: AccessoryConfig()
                    accMap[k] = AccessoryConfig(
                        enabled = cObj.optBoolean("enabled", defAcc.enabled),
                        which = cObj.optString("which", defAcc.which),
                        applyTo = cObj.optString("applyTo", defAcc.applyTo),
                        qty = cObj.optInt("qty", defAcc.qty),
                        qtyH = cObj.optInt("qtyH", defAcc.qtyH),
                        qtyW = cObj.optInt("qtyW", defAcc.qtyW)
                    )
                }
            }
            FormulaCategorySettings(formulas = formulas, acc = accMap)
        } catch (_: Exception) {
            def
        }
    }

    fun serializeFSectionFormula(f: FSectionFormula): String {
        val o = JSONObject()
        o.put("enabled", f.enabled)
        o.put("applyTo", f.applyTo)
        o.put("heightDim", serializeDeductionDim(f.heightDim))
        o.put("widthDim", serializeDeductionDim(f.widthDim))
        o.put("heightIsAdd", f.heightIsAdd)
        o.put("widthIsAdd", f.widthIsAdd)
        o.put("qtyH", f.qtyH)
        o.put("qtyW", f.qtyW)
        return o.toString()
    }

    fun deserializeFSectionFormula(jsonStr: String?, def: FSectionFormula): FSectionFormula {
        if (jsonStr.isNullOrBlank()) return def
        return try {
            val o = JSONObject(jsonStr)
            FSectionFormula(
                enabled = o.optBoolean("enabled", def.enabled),
                applyTo = o.optString("applyTo", def.applyTo),
                heightDim = deserializeDeductionDim(o.optJSONObject("heightDim"), def.heightDim),
                widthDim = deserializeDeductionDim(o.optJSONObject("widthDim"), def.widthDim),
                heightIsAdd = o.optBoolean("heightIsAdd", def.heightIsAdd),
                widthIsAdd = o.optBoolean("widthIsAdd", def.widthIsAdd),
                qtyH = o.optInt("qtyH", def.qtyH),
                qtyW = o.optInt("qtyW", def.qtyW)
            )
        } catch (_: Exception) {
            def
        }
    }

    fun serializeGlassFormulas(map: Map<String, GlassFormula>): String {
        val root = JSONObject()
        map.forEach { (k, gf) ->
            val o = JSONObject()
            o.put("marginH", gf.marginH)
            o.put("marginW", gf.marginW)
            o.put("panels", gf.panels)
            o.put("marginHSutar", gf.marginHSutar)
            o.put("marginWSutar", gf.marginWSutar)
            o.put("marginH3", gf.marginH3)
            o.put("marginH3Sutar", gf.marginH3Sutar)
            o.put("marginW3Center", gf.marginW3Center)
            o.put("marginW3CenterSutar", gf.marginW3CenterSutar)
            o.put("marginW3Sides", gf.marginW3Sides)
            o.put("marginW3SidesSutar", gf.marginW3SidesSutar)
            o.put("marginW3", gf.marginW3)
            o.put("marginW3Sutar", gf.marginW3Sutar)
            o.put("overlapW3", gf.overlapW3)
            o.put("overlapW3Sutar", gf.overlapW3Sutar)
            o.put("marginH4", gf.marginH4)
            o.put("marginH4Sutar", gf.marginH4Sutar)
            o.put("marginW4", gf.marginW4)
            o.put("marginW4Sutar", gf.marginW4Sutar)
            root.put(k, o)
        }
        return root.toString()
    }

    fun deserializeGlassFormulas(jsonStr: String?, def: Map<String, GlassFormula>): Map<String, GlassFormula> {
        if (jsonStr.isNullOrBlank()) return def
        return try {
            val root = JSONObject(jsonStr)
            val result = def.toMutableMap()
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val o = root.optJSONObject(k) ?: continue
                val d = result[k] ?: GlassFormula()
                result[k] = GlassFormula(
                    marginH = o.optDouble("marginH", d.marginH),
                    marginW = o.optDouble("marginW", d.marginW),
                    panels = o.optInt("panels", d.panels),
                    marginHSutar = o.optDouble("marginHSutar", d.marginHSutar),
                    marginWSutar = o.optDouble("marginWSutar", d.marginWSutar),
                    marginH3 = o.optDouble("marginH3", d.marginH3),
                    marginH3Sutar = o.optDouble("marginH3Sutar", d.marginH3Sutar),
                    marginW3Center = o.optDouble("marginW3Center", d.marginW3Center),
                    marginW3CenterSutar = o.optDouble("marginW3CenterSutar", d.marginW3CenterSutar),
                    marginW3Sides = o.optDouble("marginW3Sides", d.marginW3Sides),
                    marginW3SidesSutar = o.optDouble("marginW3SidesSutar", d.marginW3SidesSutar),
                    marginW3 = o.optDouble("marginW3", d.marginW3),
                    marginW3Sutar = o.optDouble("marginW3Sutar", d.marginW3Sutar),
                    overlapW3 = o.optDouble("overlapW3", d.overlapW3),
                    overlapW3Sutar = o.optDouble("overlapW3Sutar", d.overlapW3Sutar),
                    marginH4 = o.optDouble("marginH4", d.marginH4),
                    marginH4Sutar = o.optDouble("marginH4Sutar", d.marginH4Sutar),
                    marginW4 = o.optDouble("marginW4", d.marginW4),
                    marginW4Sutar = o.optDouble("marginW4Sutar", d.marginW4Sutar)
                )
            }
            result
        } catch (_: Exception) {
            def
        }
    }
}

