package com.lsh.plugin

import com.android.build.gradle.AppExtension
import com.lsh.plugin.entension.ConfigExtension
import com.lsh.plugin.tasks.AddJunkFileGuardTask
import com.lsh.plugin.tasks.MoveDirTask
import com.lsh.plugin.tasks.RenameClassGuardTask
import com.lsh.plugin.tasks.RenameResGuardTask
import com.lsh.plugin.tasks.XmlClassGuardTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class ClassGuardPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        checkApplicationPlugin(project)
        val configExtension =
            project.extensions.create("classResGuard", ConfigExtension::class.java)
        project.tasks.create(
            "renameRes", RenameResGuardTask::class.java, configExtension
        )
        project.tasks.create("addJunkFile", AddJunkFileGuardTask::class.java, configExtension)
        project.tasks.create("renameClass", RenameClassGuardTask::class.java, configExtension)
//        project.tasks.create("renameDir", RenameDirGuardTask::class.java, configExtension)

        val android = project.extensions.getByName("android") as AppExtension
        project.afterEvaluate {
            android.applicationVariants.all { variant ->
                val variantName = variant.name.capitalize()
                project.tasks.create("xmlClassGuard$variantName", XmlClassGuardTask::class.java, configExtension,variantName)
                project.tasks.create("moveDir$variantName", MoveDirTask::class.java, configExtension,variantName)
            }
        }
    }


    private fun checkApplicationPlugin(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw  GradleException("Android Application plugin required")
        }
    }
}