import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.android.library)
	id("module.publication")
}

kotlin {
	applyDefaultHierarchyTemplate()
	jvm("desktop")
	androidTarget {
		publishLibraryVariants("release")
		compilerOptions {
			jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
		}
	}
	@OptIn(ExperimentalWasmDsl::class)
	wasmJs {
		outputModuleName = "ComposeTextEditorSpellCheck"
		browser {
			commonWebpackConfig {
				outputFileName = "composeTextEditorSpellCheckLibrary.js"
			}
		}
		binaries.library()
	}

	sourceSets {
		val commonMain by getting {
			dependencies {
				api(projects.composeTextEditor)

				implementation(compose.runtime)
				implementation(compose.foundation)
				implementation(compose.material3)
				implementation(compose.materialIconsExtended)
				implementation(compose.ui)
				implementation(compose.components.resources)
				implementation(compose.components.uiToolingPreview)
				implementation(libs.androidx.lifecycle.viewmodel)
				implementation(libs.androidx.lifecycle.runtime.compose)
				implementation(libs.symspellkt)
			}
		}

		val desktopMain by getting {
			dependencies {
				implementation(compose.desktop.currentOs)
				implementation(libs.kotlinx.coroutines.swing)
			}
		}

		val desktopTest by getting {
			dependencies {
				implementation(libs.jetbrains.kotlin.test)
				implementation(libs.jetbrains.kotlin.test.junit)
				implementation(libs.mockk)
				implementation(libs.kotlinx.coroutines.test)
				implementation(libs.kotlinx.coroutines.test.jvm)

				implementation(libs.symspellkt)
			}
		}
	}
}

android {
	namespace = "com.darkrockstudios.texteditor.spellcheck"
	compileSdk = libs.versions.android.compileSdk.get().toInt()
	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
	}
	compileOptions {
		sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
		targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
	}
}