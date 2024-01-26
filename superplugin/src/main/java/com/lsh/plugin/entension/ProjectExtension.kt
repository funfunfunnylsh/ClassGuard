package com.lsh.plugin.entension

import com.android.build.gradle.BaseExtension
import com.lsh.plugin.model.ClassInfo
import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlParser
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import java.io.File

fun Project.javaDir(path: String = ""): File = file("src/main/java/$path")

fun Project.resDir(path: String = ""): File = file("src/main/res/$path")

fun Project.layoutDir(path: String = ""): File = file("src/main/res/layout/$path")

fun Project.manifestFile(): File = file("src/main/AndroidManifest.xml")

fun Project.findLayoutDirs(variantName: String) = findXmlDirs(variantName, "layout")
fun Project.findXmlDirs(variantName: String, vararg dirName: String): ArrayList<File> {
    return resDirs(variantName).flatMapTo(ArrayList()) { dir ->
        dir.listFiles { file, name ->
            //过滤res目录下xxx目录
            file.isDirectory && dirName.any { name.startsWith(it) }
        }?.toList() ?: emptyList()
    }
}


fun Project.resDirs(variantName: String): List<File> {
    val sourceSet = (extensions.getByName("android") as BaseExtension).sourceSets
    val nameSet = mutableSetOf<String>()
    nameSet.add("main")
    if (isAndroidProject()) {
        nameSet.addAll(variantName.splitWords())
    }
    val resDirs = mutableListOf<File>()
    sourceSet.names.forEach { name ->
        if (nameSet.contains(name)) {
            sourceSet.getByName(name).res.srcDirs.mapNotNullTo(resDirs) {
                if (it.exists()) it else null
            }
        }
    }
    return resDirs
}

fun findClassByManifest(text: String, classPaths: MutableList<String>, namespace: String?): String {
    val rootNode = XmlParser(false, false).parseText(text)
    val packageName = namespace ?: rootNode.attribute("package").toString()
    val nodeList = rootNode.get("application") as? NodeList ?: return packageName
    val applicationNode = nodeList.firstOrNull() as? Node ?: return packageName
    val application = applicationNode.attribute("android:name")?.toString()
    if (application != null) {
//        val classPath = if (application.startsWith(".")) packageName + application else application
        classPaths.add(application)
    }
    for (children in applicationNode.children()) {
        val childNode = children as? Node ?: continue
        val childName = childNode.name()
        if ("activity" == childName || "service" == childName ||
            "receiver" == childName || "provider" == childName
        ) {
            val name = childNode.attribute("android:name").toString()
//            val classPath = if (name.startsWith(".")) packageName + name else name
            classPaths.add(name)
        }
    }
    return packageName
}

fun Project.findDependencyAndroidProject(
    projects: MutableList<Project>,
    names: List<String> = mutableListOf("api", "implementation","compileOnly")
) {
    names.forEach { name ->
        val dependencyProjects = configurations.getByName(name).dependencies
            .filterIsInstance<DefaultProjectDependency>()
            .filter { it.dependencyProject.isAndroidProject() }
            .map { it.dependencyProject }
        projects.addAll(dependencyProjects)
        dependencyProjects.forEach {
            it.findDependencyAndroidProject(projects, names)
        }
    }
}

fun Project.isAndroidProject() =
    plugins.hasPlugin("com.android.application")
            || plugins.hasPlugin("com.android.library")


//查找dir所在的Project，dir不存在，返回null
fun Project.findLocationProject(dir: String, variantName: String): Project? {
    val packageName = dir.replace(".", File.separator)
    val javaDirs = javaDirs(variantName)
    if (javaDirs.any { File(it, packageName).exists() }) {
        return this
    }
    val dependencyProjects = mutableListOf<Project>()
    findDependencyAndroidProject(dependencyProjects)
    dependencyProjects.forEach {
        val project = it.findLocationProject(dir, variantName)
        if (project != null) return project
    }
    return null
}



fun Project.findPackage(): String {
    val namespace = (extensions.getByName("android") as BaseExtension).namespace
    if (namespace != null) {
        return namespace
    }
    val rootNode = XmlParser(false, false).parse(manifestFile())
    return rootNode.attribute("package").toString()
}


fun findFragmentInfoList(text: String): List<ClassInfo> {
    val classInfoList = mutableListOf<ClassInfo>()
    val rootNode = XmlParser(false, false).parseText(text)
    for (children in rootNode.children()) {
        val childNode = children as? Node ?: continue
        val childName = childNode.name()
        if ("fragment" == childName) {
            val classPath = childNode.attribute("android:name").toString()
            classInfoList.add(ClassInfo(classPath, childNode.children().isNotEmpty()))
        }
    }
    return classInfoList
}

fun findClassByManifest(text: String, packageName: String): List<ClassInfo> {
    val classInfoList = mutableListOf<ClassInfo>()
    val rootNode = XmlParser(false, false).parseText(text)
    val nodeList = rootNode.get("application") as? NodeList ?: return classInfoList
    val applicationNode = nodeList.firstOrNull() as? Node ?: return classInfoList
    val application = applicationNode.attribute("android:name")?.toString()
    if (application != null) {
        val classPath = if (application.startsWith(".")) "$packageName$application" else application
        classInfoList.add(ClassInfo(classPath))
    }
    for (children in applicationNode.children()) {
        val childNode = children as? Node ?: continue
        val childName = childNode.name()
        if ("activity" == childName || "service" == childName ||
            "receiver" == childName || "provider" == childName
        ) {
            val name = childNode.attribute("android:name").toString()
            val classPath = if (name.startsWith(".")) "$packageName$name" else name
            classInfoList.add(ClassInfo(classPath))
        }
    }
    return classInfoList
}

fun findClassByLayoutXml(text: String, packageName: String): List<ClassInfo> {
    val classInfoList = mutableListOf<ClassInfo>()
    val childrenList = XmlParser(false, false).parseText(text).breadthFirst()
    val destAttributes =
        mutableListOf("tools:context", "app:layout_behavior", "app:layoutManager", "android:name")
    for (children in childrenList) {
        val childNode = children as? Node ?: continue
        destAttributes.forEach { attributeName ->
            val attributeValue = childNode.attribute(attributeName)?.toString()
            if (!attributeValue.isNullOrBlank()) {
                val classname =
                    if (attributeValue.startsWith(".")) "$packageName$attributeValue" else attributeValue
                classInfoList.add(ClassInfo(classname))
            }
        }
        val nodeName = childNode.name().toString()
        if (nodeName !in whiteList) {
            if (nodeName == "variable" || nodeName == "import") {
                val typeValue = childNode.attribute("type").toString()
                classInfoList.add(ClassInfo(typeValue, fromImportNode = nodeName == "import"))
            } else {
                classInfoList.add(ClassInfo(nodeName))
            }
        }
    }
    return classInfoList
}




//返回java/kotlin代码目录,可能有多个
fun Project.javaDirs(variantName: String): List<File> {
    val sourceSet = (extensions.getByName("android") as BaseExtension).sourceSets
    val nameSet = mutableSetOf<String>()
    nameSet.add("main")
    if (isAndroidProject()) {
        nameSet.addAll(variantName.splitWords())
    }
    val javaDirs = mutableListOf<File>()
    sourceSet.names.forEach { name ->
        if (nameSet.contains(name)) {
            sourceSet.getByName(name).java.srcDirs.mapNotNullTo(javaDirs) {
                if (it.exists()) it else null
            }
        }
    }
    return javaDirs
}

fun Project.aidlDirs(variantName: String): List<File> {
    val sourceSet = (extensions.getByName("android") as BaseExtension).sourceSets
    val nameSet = mutableSetOf<String>()
    nameSet.add("main")
    if (isAndroidProject()) {
        nameSet.addAll(variantName.splitWords())
    }
    val javaDirs = mutableListOf<File>()
    sourceSet.names.forEach { name ->
        if (nameSet.contains(name)) {
            sourceSet.getByName(name).aidl.srcDirs.mapNotNullTo(javaDirs) {
                if (it.exists()) it else null
            }
        }
    }
    return javaDirs
}



val whiteList = arrayListOf(
    "layout", "data", "merge", "ViewStub", "include",
    "LinearLayout", "RelativeLayout", "FrameLayout", "AbsoluteLayout",
    "Button", "TextView", "View", "ImageView", "EditText", "ProgressBar",
    "androidx.constraintlayout.widget.ConstraintLayout",
    "androidx.core.widget.NestedScrollView",
    "androidx.constraintlayout.widget.Group",
    "androidx.constraintlayout.widget.Guideline",
    "androidx.appcompat.widget.Toolbar",
    "com.google.android.material.button.MaterialButton",
    "GridLayout", "GridView",
)