package com.lsh.plugin.entension

import java.io.File

open class ConfigExtension {

    var classPrefixName: Array<String> = arrayOf("")

    var dirPrefixName: Array<String> = arrayOf("")

    var resPrefixName: Array<String> = arrayOf("")

    var changeResDir: Array<String>? = null

    var junkPackage = "com.lsh.superplugin"

    var junkResPackage = "com.lsh.superplugin"

    var activityClassMethodCount = 0

    var activityClassCount = 0

    var normalClassCount = 0

    var normalClassMethodCount = 0

    var layoutClassCount = 0

    var layoutClassMethodCount = 0

    var drawableClassCount = 0

    var colorCount = 0

    var stringsCount = 0

    var colorPrefixName: Array<String> = arrayOf("")

    var stringsPrefixName: Array<String> = arrayOf("")

    var moveDir = HashMap<String, String>()

    var mappingFile: File? = null

}