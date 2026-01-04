import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.android.kmp.library)
	alias(libs.plugins.mavenPublish)
}

kotlin {
	applyDefaultHierarchyTemplate()
	jvm("desktop")
	androidLibrary {
		namespace = "com.darkrockstudios.texteditor.find"
		compileSdk = libs.versions.android.compileSdk.get().toInt()
		minSdk = libs.versions.android.minSdk.get().toInt()

		compilerOptions {
			jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
		}
	}
	@OptIn(ExperimentalWasmDsl::class)
	wasmJs {
		outputModuleName = "ComposeTextEditorFind"
		browser {
			commonWebpackConfig {
				outputFileName = "composeTextEditorFindLibrary.js"
			}
		}
		binaries.library()
	}

	iosX64()
	iosArm64()
	iosSimulatorArm64()

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
			}
		}
	}
}

group = "com.darkrockstudios"
version = providers.gradleProperty("library.version").getOrElse("0.0.0-SNAPSHOT")

mavenPublishing {
	coordinates(artifactId = "composetexteditor-find")
	publishToMavenCentral(automaticRelease = true)
	signAllPublications()

	pom {
		name.set("Compose Text Editor Find")
		description.set("Find functionality addon for Compose Text Editor.")
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
