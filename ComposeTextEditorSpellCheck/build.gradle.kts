import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.android.library)
	alias(libs.plugins.mavenPublish)
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
			}
		}

		// SymSpell implementation (used by wasmJs, desktop, and android)
		val symSpellMain by creating {
			dependsOn(commonMain)
			dependencies {
				implementation(libs.symspellkt)
			}
		}
		// Native/Platform spell checker implementation (desktop + android)
		val platformSpellMain by creating {
			dependsOn(commonMain)
			dependencies {
				implementation(libs.platform.spellchecker)
			}
		}

		val desktopMain by getting {
			dependsOn(symSpellMain)
			dependsOn(platformSpellMain)
			dependencies {
				implementation(compose.desktop.currentOs)
				implementation(libs.kotlinx.coroutines.swing)
			}
		}
		val androidMain by getting {
			dependsOn(symSpellMain)
			dependsOn(platformSpellMain)
		}
		val desktopTest by getting {
			dependencies {
				implementation(libs.jetbrains.kotlin.test)
				implementation(libs.jetbrains.kotlin.test.junit)
				implementation(libs.mockk)
				implementation(libs.kotlinx.coroutines.test)
				implementation(libs.kotlinx.coroutines.test.jvm)

				implementation(libs.symspellkt)
				implementation(libs.platform.spellchecker)
			}
		}
		val wasmJsMain by getting {
			dependsOn(symSpellMain)
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

group = "com.darkrockstudios"
version = providers.gradleProperty("library.version").getOrElse("0.0.0-SNAPSHOT")

mavenPublishing {
	publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
	signAllPublications()

	pom {
		name.set("Compose Text Editor Spell Check")
		description.set("Spell checking addon for Compose Text Editor.")
		url.set("https://github.com/Wavesonics/ComposeTextEditorLibrary")

		licenses {
			license {
				name.set("MIT")
				url.set("https://opensource.org/licenses/MIT")
			}
		}
		issueManagement {
			system.set("Github")
			url.set("https://github.com/Wavesonics/ComposeTextEditorLibrary/issues")
		}
		scm {
			connection.set("scm:git:git://github.com/Wavesonics/ComposeTextEditorLibrary.git")
			developerConnection.set("scm:git:ssh://github.com/Wavesonics/ComposeTextEditorLibrary.git")
			url.set("https://github.com/Wavesonics/ComposeTextEditorLibrary")
		}
		developers {
			developer {
				name.set("Adam Brown")
				id.set("Wavesonics")
				email.set("adamwbrown@gmail.com")
			}
		}
	}
}