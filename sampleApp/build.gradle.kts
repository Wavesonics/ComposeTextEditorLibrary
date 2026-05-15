@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.android.application)
}

kotlin {
	applyDefaultHierarchyTemplate()
	jvm("desktop")
	androidTarget {
		compilerOptions {
			jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
		}
	}
	wasmJs {
		outputModuleName = "sampleApp"
		browser {
			commonWebpackConfig {
				outputFileName = "sampleApp.js"
			}
		}
		binaries.executable()
		compilerOptions {
			freeCompilerArgs.add("-Xwasm-use-new-exception-proposal")
		}
	}

	iosX64()
	iosArm64()
	iosSimulatorArm64()

	listOf(
		iosX64(),
		iosArm64(),
		iosSimulatorArm64()
	).forEach { iosTarget ->
		iosTarget.binaries.framework {
			baseName = "SampleApp"
			isStatic = true
		}
	}

	sourceSets {

		val commonMain by getting {
			dependencies {
				implementation(projects.composeTextEditor)
				implementation(projects.composeTextEditorSpellCheck)
				implementation(projects.composeTextEditorFind)
				implementation(compose.runtime)
				implementation(compose.foundation)
				implementation(compose.material3)
				implementation(compose.materialIconsExtended)
				implementation(compose.ui)
				implementation(compose.components.resources)
				implementation(compose.components.uiToolingPreview)
				implementation(compose.components.resources)
			}
		}

		val desktopMain by getting {
			dependencies {
				implementation(libs.kotlinx.coroutines.swing)

				implementation(libs.androidx.lifecycle.viewmodel)
				implementation(libs.androidx.lifecycle.runtime.compose)

				implementation(compose.desktop.currentOs)
				implementation(compose.runtime)
				implementation(compose.foundation)
				implementation(compose.material3)
				implementation(compose.materialIconsExtended)
				implementation(compose.ui)
				implementation(compose.components.resources)
				implementation(compose.components.uiToolingPreview)
				implementation(libs.platform.spellchecker)
			}
		}

		val desktopTest by getting {
			dependencies {
				implementation(libs.jetbrains.kotlin.test)
				implementation(libs.jetbrains.kotlin.test.junit)
				implementation(libs.mockk)
				implementation(libs.kotlinx.coroutines.test)
				implementation(libs.kotlinx.coroutines.test.jvm)
			}
		}

		val androidMain by getting {
			dependencies {
				implementation(libs.activity.ktx)
				implementation(compose.runtime)
				implementation(compose.foundation)
				implementation(compose.material3)
				implementation(compose.ui)
				implementation(compose.components.resources)
				implementation(libs.platform.spellchecker)
			}
		}

		val wasmJsMain by getting {
			dependencies {
				implementation(libs.symspellkt)
				implementation(libs.symspellkt.fdic)
			}
		}

		val iosMain by getting {
			dependencies {
				implementation(libs.platform.spellchecker)
			}
		}
	}
}

compose.desktop {
	application {
		mainClass = "MainKt"

		nativeDistributions {
			targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
			packageName = "com.darkrockstudios.texteditor"
			packageVersion = "1.0.0"
		}
	}
}

android {
	namespace = "com.darkrockstudios.texteditor.sample"
	compileSdk = libs.versions.android.compileSdk.get().toInt()
	defaultConfig {
		applicationId = "com.darkrockstudios.texteditor.sample"
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetSdk = libs.versions.android.compileSdk.get().toInt()
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}
	buildFeatures {
		compose = true
	}
	compileOptions {
		sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
		targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
	}

	buildTypes {
		debug {
		}

		release {
		}
	}
}

tasks.register<Copy>("updateDemo") {
	description = "Builds the WASM distribution and copies it to the docs directory for GitHub Pages"
	group = "distribution"

	dependsOn("wasmJsBrowserDistribution")

	from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
	into(layout.projectDirectory.dir("../docs"))

	doLast {
		val docsDir = layout.projectDirectory.dir("../docs").asFile
		val buildDir = layout.buildDirectory.dir("dist/wasmJs/productionExecutable").get().asFile

		// Get list of WASM files in build output
		val newWasmFiles =
			buildDir.listFiles { file -> file.extension == "wasm" }?.map { it.name }?.toSet() ?: emptySet()

		// Remove old WASM files that are no longer in the build
		docsDir.listFiles { file -> file.extension == "wasm" }?.forEach { oldFile ->
			if (oldFile.name !in newWasmFiles) {
				println("Removing old WASM file: ${oldFile.name}")
				oldFile.delete()
			}
		}

		println("Demo updated in docs/ directory")
	}
}
dependencies {
	implementation(libs.lifecycle.runtime.ktx)
	implementation(libs.activity.compose)
	implementation(platform(libs.compose.bom))
	implementation(libs.ui)
	implementation(libs.ui.graphics)
	implementation(libs.ui.tooling.preview)
	implementation(libs.material3)
	implementation(libs.activity.ktx)
	androidTestImplementation(platform(libs.compose.bom))
	androidTestImplementation(libs.ui.test.junit4)
	debugImplementation(libs.ui.tooling)
	debugImplementation(libs.ui.test.manifest)
}
